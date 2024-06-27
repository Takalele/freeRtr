void str2mac(__u8 *dst, char *src) {
    sscanf(src, "%hhx:%hhx:%hhx:%hhx:%hhx:%hhx", &dst[0], &dst[1], &dst[2], &dst[3], &dst[4], &dst[5]);
}

int str2key(char *str, unsigned char *key) {
    unsigned char buf[4];
    int s = 0;
    for (int i=0;;) {
        memmove(&buf, &str[i], 2);
        buf[2] = 0;
        if (str[i] == 0) break;
        sscanf((char*)buf, "%hhx", &key[s]);
        s++;
        i += 2;
    }
    return s;
}

void crc16mktab(int *tab, int poly) {
    for (int o=0; o < 256; o++) {
        int v = o << 8;
        for (int i = 0; i < 8; ++i) {
            v <<= 1;
            if ((v & 0x10000) != 0) {
                v ^= poly;
            }
        }
        tab[o] = v & 0xffff;
    }
}

void doStatRound(FILE *commands, int round) {
    __u32 i = 1;
    __u32 o = 10;
    if (bpf_map_update_elem(cpu_port_fd, &i, &o, BPF_ANY) != 0) err("error setting cpuport");
    if ((round % 10) != 0) return;
    struct port_res prt1;
    struct port_res prt2;
    i = -1;
    for (;;) {
        if (bpf_map_get_next_key(tx_ports_fd, &i, &o) != 0) break;
        i = o;
        struct port_res* prtp = &prt2;
        if (bpf_map_lookup_elem(tx_ports_fd, &i, prtp) != 0) continue;
        i = prt2.idx;
        prtp = &prt1;
        if (bpf_map_lookup_elem(rx_ports_fd, &i, prtp) != 0) continue;
        i = o;
        fprintf(commands, "state %i 1\r\n", i);
        fprintf(commands, "counter %i %li %li %li %li 0 0\r\n", i, (long)prt1.pack, (long)prt1.byte, (long)prt2.pack, (long)prt2.byte);
    }
    struct bundle_res bunr;
    i = -1;
    for (;;) {
        if (bpf_map_get_next_key(bundles_fd, &i, &o) != 0) break;
        i = o;
        struct bundle_res* bunp = &bunr;
        if (bpf_map_lookup_elem(bundles_fd, &i, bunp) != 0) continue;
        fprintf(commands, "counter %i 0 0 %li %li 0 0\r\n", i, (long)bunp->pack, (long)bunp->byte);
    }
    if ((round % 150) != 0) {
        fflush(commands);
        return;
    }
    struct label_res labr;
    i = -1;
    for (;;) {
        if (bpf_map_get_next_key(labels_fd, &i, &o) != 0) break;
        i = o;
        struct label_res* labp = &labr;
        if (bpf_map_lookup_elem(labels_fd, &i, labp) != 0) continue;
        fprintf(commands, "mpls_cnt %i %li %li\r\n", i, (long)labp->pack, (long)labp->byte);
    }
    fflush(commands);
}



int doOneCommand(unsigned char* buf) {
    unsigned char buf2[1024];
    char* arg[128];
    int cnt;
    cnt = 0;
    arg[0] = (char*)&buf[0];
    __u32 i = 0;
    __u32 o = 0;
    for (;;) {
        if (cnt >= 128) break;
        switch (buf[i]) {
        case 0:
        case 10:
        case 13:
            o = 1;
            break;
        case ' ':
        case '/':
        case '_':
            buf[i] = 0;
            cnt++;
            arg[cnt] = (char*)&buf[i + 1];
            break;
        }
        if (o > 0) break;
        i++;
    }
    printf("rx: ");
    for (int i=0; i < cnt; i++) printf("'%s' ",arg[i]);
    printf("\n");
    int del = strcmp(arg[1], "del");
    if (del != 0) del = 1;
    struct vrfp_res vrfp;
    memset(&vrfp, 0, sizeof(vrfp));
    vrfp.sgtSet = -1;
    struct vrfp_res* vrfr = &vrfp;
    struct neigh_res neir;
    memset(&neir, 0, sizeof(neir));
    struct route4_key rou4;
    memset(&rou4, 0, sizeof(rou4));
    struct route6_key rou6;
    memset(&rou6, 0, sizeof(rou6));
    struct routes_res rour;
    memset(&rour, 0, sizeof(rour));
    struct label_res labr;
    memset(&labr, 0, sizeof(labr));
    struct bridge_key brdk;
    memset(&brdk, 0, sizeof(brdk));
    struct bridge_res brdr;
    memset(&brdr, 0, sizeof(brdr));
#ifdef HAVE_NOHW
    return 0;
#else
    if (strcmp(arg[0], "portvrf") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->cmd = 1;
        vrfr->vrf = atoi(arg[3]);
        if (del == 0) vrfr->cmd = 0;
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "sgtset") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->sgtSet = atoi(arg[3]);
        if (del == 0) vrfr->sgtSet = -1;
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "sgttag") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        if (del == 0) vrfr->sgtTag = 0;
        else vrfr->sgtTag = 1;
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "verify4") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->verify4 = atoi(arg[3]);
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "verify6") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->verify6 = atoi(arg[3]);
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "pmtud4in") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->pmtud4 = atoi(arg[3]);
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "pmtud6in") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->pmtud6 = atoi(arg[3]);
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "mplsttl4") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->pttl4 = atoi(arg[3]);
        if (vrfr->pttl4 == 0) vrfr->pttl4 = 0xff;
        else vrfr->pttl4 = 0;
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "mplsttl6") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->pttl6 = atoi(arg[3]);
        if (vrfr->pttl6 == 0) vrfr->pttl6 = 0xff;
        else vrfr->pttl6 = 0;
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "mplspack") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->mpls = atoi(arg[3]);
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "nshpack") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->nsh = atoi(arg[3]);
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "loconnifc") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->cmd = 4;
        vrfr->label1 = atoi(arg[3]);
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "loconnnei") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->cmd = 5;
        vrfr->label1 = atoi(arg[3]);
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "nshconn") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->cmd = 6;
        vrfr->label1 = atoi(arg[3]);
        vrfr->label2 = atoi(arg[4]);
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "xconnect") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->cmd = 3;
        vrfr->hop = atoi(arg[4]);
        vrfr->label1 = atoi(arg[5]);
        vrfr->label2 = atoi(arg[7]);
        o = atoi(arg[6]);
        labr.port = i;
        labr.cmd= 4;
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        if (del == 0) {
            vrfr->cmd = 0;
            if (bpf_map_delete_elem(labels_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(labels_fd, &o, &labr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "portbridge") == 0) {
        i = atoi(arg[2]);
        bpf_map_lookup_elem(vrf_port_fd, &i, vrfr);
        vrfr->cmd = 2;
        vrfr->brdg = atoi(arg[3]);
        if (del == 0) vrfr->cmd = 0;
        if (bpf_map_update_elem(vrf_port_fd, &i, vrfr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "bridgemac") == 0) {
        brdk.id = atoi(arg[2]);
        str2mac(brdk.mac, arg[3]);
        brdr.port = atoi(arg[4]);
        brdr.cmd = 1;
        if (del == 0) {
            if (bpf_map_delete_elem(bridges_fd, &brdk) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(bridges_fd, &brdk, &brdr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "routedmac") == 0) {
        brdk.id = atoi(arg[2]);
        str2mac(brdk.mac, arg[3]);
        brdr.hop = atoi(arg[4]);
        brdr.cmd = 3;
        if (del == 0) {
            if (bpf_map_delete_elem(bridges_fd, &brdk) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(bridges_fd, &brdk, &brdr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "bridgepckoudp4") == 0) {
        struct tunnel4_key tunk;
        memset(&tunk, 0, sizeof(tunk));
        struct tunnel_res tunr;
        memset(&tunr, 0, sizeof(tunr));
        brdk.id = atoi(arg[2]);
        str2mac(brdk.mac, arg[3]);
        inet_pton(AF_INET, arg[4], buf2);
        memcpy(&tunk.trgAddr, &buf2, sizeof(tunk.trgAddr));
        memcpy(&brdr.srcAddr, &buf2, sizeof(tunk.trgAddr));
        inet_pton(AF_INET, arg[5], buf2);
        memcpy(&tunk.srcAddr, &buf2, sizeof(tunk.srcAddr));
        memcpy(&brdr.trgAddr, &buf2, sizeof(tunk.srcAddr));
        tunk.trgPort = brdr.srcPort = atoi(arg[6]);
        tunk.srcPort = brdr.trgPort = atoi(arg[7]);
        brdr.hop = atoi(arg[8]);
        brdr.cmd = 4;
        tunk.vrf = atoi(arg[9]);
        tunr.aclport = atoi(arg[10]);
        tunk.prot = IP_PROTOCOL_UDP;
        tunr.cmd = 5;
        if (del == 0) {
            if (bpf_map_delete_elem(tunnel4_fd, &tunk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(bridges_fd, &brdk) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(tunnel4_fd, &tunk, &tunr, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(bridges_fd, &brdk, &brdr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "bridgepckoudp6") == 0) {
        struct tunnel6_key tunk;
        memset(&tunk, 0, sizeof(tunk));
        struct tunnel_res tunr;
        memset(&tunr, 0, sizeof(tunr));
        brdk.id = atoi(arg[2]);
        str2mac(brdk.mac, arg[3]);
        inet_pton(AF_INET6, arg[4], buf2);
        memcpy(&tunk.trgAddr, &buf2, sizeof(tunk.trgAddr));
        memcpy(&brdr.srcAddr, &buf2, sizeof(tunk.trgAddr));
        inet_pton(AF_INET6, arg[5], buf2);
        memcpy(&tunk.srcAddr, &buf2, sizeof(tunk.srcAddr));
        memcpy(&brdr.trgAddr, &buf2, sizeof(tunk.srcAddr));
        tunk.trgPort = brdr.srcPort = atoi(arg[6]);
        tunk.srcPort = brdr.trgPort = atoi(arg[7]);
        brdr.hop = atoi(arg[8]);
        brdr.cmd = 5;
        tunk.vrf = atoi(arg[9]);
        tunr.aclport = atoi(arg[10]);
        tunk.prot = IP_PROTOCOL_UDP;
        tunr.cmd = 5;
        if (del == 0) {
            if (bpf_map_delete_elem(tunnel6_fd, &tunk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(bridges_fd, &brdk) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(tunnel6_fd, &tunk, &tunr, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(bridges_fd, &brdk, &brdr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "bridgevpls") == 0) {
        brdk.id = atoi(arg[2]);
        str2mac(brdk.mac, arg[3]);
        brdr.hop = atoi(arg[5]);
        brdr.label1 = atoi(arg[6]);
        brdr.label2 = atoi(arg[7]);
        brdr.cmd = 2;
        if (del == 0) {
            if (bpf_map_delete_elem(bridges_fd, &brdk) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(bridges_fd, &brdk, &brdr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "bridgelabel") == 0) {
        i = atoi(arg[3]);
        labr.brdg = atoi(arg[2]);
        labr.cmd = 5;
        if (del == 0) {
            if (bpf_map_delete_elem(labels_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(labels_fd, &i, &labr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "neigh4") == 0) {
        inet_pton(AF_INET, arg[3], buf2);
        rou4.vrf = atoi(arg[5]);
        memcpy(rou4.addr, buf2, sizeof(rou4.addr));
        rou4.bits = routes_bits + (sizeof(rou4.addr) * 8);
        rour.cmd = 1;
        i = rour.hop = atoi(arg[2]);
        str2mac(&neir.macs[0], arg[4]);
        str2mac(&neir.macs[6], arg[6]);
        neir.aclport = neir.port = atoi(arg[7]);
        neir.cmd = 1;
        if (del == 0) {
            if (bpf_map_delete_elem(route4_fd, &rou4) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route4_fd, &rou4, &rour, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &i, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "neigh6") == 0) {
        inet_pton(AF_INET6, arg[3], buf2);
        rou6.vrf = atoi(arg[5]);
        memcpy(rou6.addr, buf2, sizeof(rou6.addr));
        rou6.bits = routes_bits + (sizeof(rou6.addr) * 8);
        rour.cmd = 1;
        i = rour.hop = atoi(arg[2]);
        str2mac(&neir.macs[0], arg[4]);
        str2mac(&neir.macs[6], arg[6]);
        neir.aclport = neir.port = atoi(arg[7]);
        neir.cmd = 1;
        if (del == 0) {
            if (bpf_map_delete_elem(route6_fd, &rou6) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route6_fd, &rou6, &rour, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &i, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "myaddr4") == 0) {
        inet_pton(AF_INET, arg[2], buf2);
        rou4.vrf = atoi(arg[5]);
        memcpy(rou4.addr, buf2, sizeof(rou4.addr));
        rou4.bits = routes_bits + atoi(arg[3]);
        rour.cmd = 2;
        if (del == 0) {
            if (bpf_map_delete_elem(route4_fd, &rou4) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route4_fd, &rou4, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "myaddr6") == 0) {
        inet_pton(AF_INET6, arg[2], buf2);
        rou6.vrf = atoi(arg[5]);
        memcpy(rou6.addr, buf2, sizeof(rou6.addr));
        rou6.bits = routes_bits + atoi(arg[3]);
        rour.cmd = 2;
        if (del == 0) {
            if (bpf_map_delete_elem(route6_fd, &rou6) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route6_fd, &rou6, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "route4") == 0) {
        inet_pton(AF_INET, arg[2], buf2);
        rou4.vrf = atoi(arg[6]);
        memcpy(rou4.addr, buf2, sizeof(rou4.addr));
        rou4.bits = routes_bits + atoi(arg[3]);
        rour.cmd = 1;
        rour.hop = atoi(arg[4]);
        if (del == 0) {
            if (bpf_map_delete_elem(route4_fd, &rou4) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route4_fd, &rou4, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "route6") == 0) {
        inet_pton(AF_INET6, arg[2], buf2);
        rou6.vrf = atoi(arg[6]);
        memcpy(rou6.addr, buf2, sizeof(rou6.addr));
        rou6.bits = routes_bits + atoi(arg[3]);
        rour.cmd = 1;
        rour.hop = atoi(arg[4]);
        if (del == 0) {
            if (bpf_map_delete_elem(route6_fd, &rou6) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route6_fd, &rou6, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "labroute4") == 0) {
        inet_pton(AF_INET, arg[2], buf2);
        rou4.vrf = atoi(arg[6]);
        memcpy(rou4.addr, buf2, sizeof(rou4.addr));
        rou4.bits = routes_bits + atoi(arg[3]);
        rour.cmd = 3;
        rour.hop = atoi(arg[4]);
        rour.label1 = atoi(arg[7]);
        if (del == 0) {
            if (bpf_map_delete_elem(route4_fd, &rou4) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route4_fd, &rou4, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "labroute6") == 0) {
        inet_pton(AF_INET6, arg[2], buf2);
        rou6.vrf = atoi(arg[6]);
        memcpy(rou6.addr, buf2, sizeof(rou6.addr));
        rou6.bits = routes_bits + atoi(arg[3]);
        rour.cmd = 3;
        rour.hop = atoi(arg[4]);
        rour.label1 = atoi(arg[7]);
        if (del == 0) {
            if (bpf_map_delete_elem(route6_fd, &rou6) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route6_fd, &rou6, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "vpnroute4") == 0) {
        inet_pton(AF_INET, arg[2], buf2);
        rou4.vrf = atoi(arg[6]);
        memcpy(rou4.addr, buf2, sizeof(rou4.addr));
        rou4.bits = routes_bits + atoi(arg[3]);
        rour.cmd = 4;
        rour.hop = atoi(arg[4]);
        rour.label1 = atoi(arg[7]);
        rour.label2 = atoi(arg[8]);
        if (del == 0) {
            if (bpf_map_delete_elem(route4_fd, &rou4) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route4_fd, &rou4, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "vpnroute6") == 0) {
        inet_pton(AF_INET6, arg[2], buf2);
        rou6.vrf = atoi(arg[6]);
        memcpy(rou6.addr, buf2, sizeof(rou6.addr));
        rou6.bits = routes_bits + atoi(arg[3]);
        rour.cmd = 4;
        rour.hop = atoi(arg[4]);
        rour.label1 = atoi(arg[7]);
        rour.label2 = atoi(arg[8]);
        if (del == 0) {
            if (bpf_map_delete_elem(route6_fd, &rou6) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route6_fd, &rou6, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "droproute4") == 0) {
        inet_pton(AF_INET, arg[2], buf2);
        rou4.vrf = atoi(arg[4]);
        memcpy(rou4.addr, buf2, sizeof(rou4.addr));
        rou4.bits = routes_bits + atoi(arg[3]);
        rour.cmd = 5;
        if (del == 0) {
            if (bpf_map_delete_elem(route4_fd, &rou4) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route4_fd, &rou4, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "droproute6") == 0) {
        inet_pton(AF_INET6, arg[2], buf2);
        rou6.vrf = atoi(arg[4]);
        memcpy(rou6.addr, buf2, sizeof(rou6.addr));
        rou6.bits = routes_bits + atoi(arg[3]);
        rour.cmd = 5;
        if (del == 0) {
            if (bpf_map_delete_elem(route6_fd, &rou6) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route6_fd, &rou6, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "polroute4") == 0) {
        inet_pton(AF_INET, arg[2], buf2);
        rou4.vrf = atoi(arg[6]);
        memcpy(rou4.addr, buf2, sizeof(rou4.addr));
        rou4.bits = routes_bits + atoi(arg[3]);
        rour.hop = atoi(arg[4]);
        str2key(arg[7], rour.polka);
        rour.cmd = 6;
        if (del == 0) {
            if (bpf_map_delete_elem(route4_fd, &rou4) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route4_fd, &rou4, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "polroute6") == 0) {
        inet_pton(AF_INET6, arg[2], buf2);
        rou6.vrf = atoi(arg[6]);
        memcpy(rou6.addr, buf2, sizeof(rou6.addr));
        rou6.bits = routes_bits + atoi(arg[3]);
        rour.hop = atoi(arg[4]);
        str2key(arg[7], rour.polka);
        rour.cmd = 6;
        if (del == 0) {
            if (bpf_map_delete_elem(route6_fd, &rou6) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(route6_fd, &rou6, &rour, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "mylabel4") == 0) {
        i = atoi(arg[2]);
        labr.vrf = atoi(arg[3]);
        labr.ver = 4;
        labr.cmd = 1;
        if (del == 0) {
            if (bpf_map_delete_elem(labels_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(labels_fd, &i, &labr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "mylabel6") == 0) {
        i = atoi(arg[2]);
        labr.vrf = atoi(arg[3]);
        labr.ver = 6;
        labr.cmd = 1;
        if (del == 0) {
            if (bpf_map_delete_elem(labels_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(labels_fd, &i, &labr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "unlabel4") == 0) {
        i = atoi(arg[2]);
        labr.hop = atoi(arg[3]);
        labr.ver = 4;
        labr.cmd = 2;
        if (del == 0) {
            if (bpf_map_delete_elem(labels_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(labels_fd, &i, &labr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "unlabel6") == 0) {
        i = atoi(arg[2]);
        labr.hop = atoi(arg[3]);
        labr.ver = 6;
        labr.cmd = 2;
        if (del == 0) {
            if (bpf_map_delete_elem(labels_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(labels_fd, &i, &labr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "label4") == 0) {
        i = atoi(arg[2]);
        labr.hop = atoi(arg[3]);
        labr.swap = atoi(arg[5]);
        labr.ver = 4;
        labr.cmd = 3;
        if (del == 0) {
            if (bpf_map_delete_elem(labels_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(labels_fd, &i, &labr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "label6") == 0) {
        i = atoi(arg[2]);
        labr.hop = atoi(arg[3]);
        labr.swap = atoi(arg[5]);
        labr.ver = 6;
        labr.cmd = 3;
        if (del == 0) {
            if (bpf_map_delete_elem(labels_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(labels_fd, &i, &labr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "hairpin") == 0) {
        struct bundle_res bunn;
        memset(&bunn, 0, sizeof(bunn));
        o = atoi(arg[2]);
        bunn.cmd = 2;
        __u32 p = atoi(arg[3]);
        for (int i = 0; i < 16; i++) bunn.out[i] = p;
        if (del == 0) {
            if (bpf_map_delete_elem(bundles_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(bundles_fd, &o, &bunn, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "portbundle") == 0) {
        o = atoi(arg[2]);
        if (del == 0) {
            if (bpf_map_delete_elem(bundles_fd, &o) != 0) warn("error removing entry");
            return 0;
        }
        struct bundle_res bunn;
        memset(&bunn, 0, sizeof(bunn));
        struct bundle_res* bunr = &bunn;
        bpf_map_update_elem(bundles_fd, &o, &bunn, BPF_NOEXIST);
        if (bpf_map_lookup_elem(bundles_fd, &o, bunr) != 0) err("error getting entry");
        i = atoi(arg[3]);
        bunr->out[i] = atoi(arg[4]);
        bunr->cmd = 1;
        if (bpf_map_update_elem(bundles_fd, &o, bunr, BPF_ANY) != 0) warn("error setting entry");
        return 0;
    }
    if (strcmp(arg[0], "portvlan") == 0) {
        struct vlan_key vlnk;
        memset(&vlnk, 0, sizeof(vlnk));
        struct vlan_res vlnr;
        memset(&vlnr, 0, sizeof(vlnr));
        i = atoi(arg[2]);
        o = atoi(arg[3]);
        vlnk.vlan = vlnr.vlan = atoi(arg[4]);
        vlnr.port = o;
        vlnk.port = o;
        if (del == 0) {
            if (bpf_map_delete_elem(vlan_in_fd, &vlnk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(vlan_out_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(vlan_in_fd, &vlnk, &i, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(vlan_out_fd, &i, &vlnr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "bundlevlan") == 0) {
        struct vlan_key vlnk;
        memset(&vlnk, 0, sizeof(vlnk));
        i = atoi(arg[4]);
        o = atoi(arg[2]);
        vlnk.vlan = atoi(arg[3]);
        vlnk.port = o;
        if (del == 0) {
            if (bpf_map_delete_elem(vlan_in_fd, &vlnk) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(vlan_in_fd, &vlnk, &i, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "nshifc") == 0) {
        struct nsh_key nshk;
        memset(&nshk, 0, sizeof(nshk));
        struct nsh_res nshr;
        memset(&nshr, 0, sizeof(nshr));
        nshk.sp = atoi(arg[2]);
        nshk.si = atoi(arg[3]);
        nshr.cmd = 1;
        nshr.port = atoi(arg[4]);
        str2mac(&nshr.macs[6], arg[5]);
        str2mac(&nshr.macs[0], arg[6]);
        nshr.trg = (atoi(arg[7]) << 8) | atoi(arg[8]);
        if (del == 0) {
            if (bpf_map_delete_elem(nsh_fd, &nshk) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(nsh_fd, &nshk, &nshr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "nshnei") == 0) {
        struct nsh_key nshk;
        memset(&nshk, 0, sizeof(nshk));
        struct nsh_res nshr;
        memset(&nshr, 0, sizeof(nshr));
        nshk.sp = atoi(arg[2]);
        nshk.si = atoi(arg[3]);
        nshr.cmd = 3;
        nshr.port = atoi(arg[4]);
        nshr.trg = (atoi(arg[5]) << 8) | atoi(arg[6]);
        if (del == 0) {
            if (bpf_map_delete_elem(nsh_fd, &nshk) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(nsh_fd, &nshk, &nshr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "nshloc") == 0) {
        struct nsh_key nshk;
        memset(&nshk, 0, sizeof(nshk));
        struct nsh_res nshr;
        memset(&nshr, 0, sizeof(nshr));
        nshk.sp = atoi(arg[2]);
        nshk.si = atoi(arg[3]);
        nshr.cmd = 2;
        nshr.vrf = atoi(arg[4]);
        if (del == 0) {
            if (bpf_map_delete_elem(nsh_fd, &nshk) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(nsh_fd, &nshk, &nshr, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "polkaidx") == 0) {
        struct polidx_key polk;
        memset(&polk, 0, sizeof(polk));
        polk.idx = atoi(arg[2]);
        polk.vrf = atoi(arg[3]);
        o = atoi(arg[4]);
        if (del == 0) {
            if (bpf_map_delete_elem(polidx_fd, &polk) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(polidx_fd, &polk, &o, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "polkapoly") == 0) {
        struct polpol_res res;
        memset(&res, 0, sizeof(res));
        i = atoi(arg[2]);
        o = atoi(arg[3]);
        crc16mktab(res.tab, o);
        if (del == 0) {
            if (bpf_map_delete_elem(polpol_fd, &i) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(polpol_fd, &i, &res, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "pppoe") == 0) {
        struct pppoe_key pppoe;
        memset(&pppoe, 0, sizeof(pppoe));
        neir.aclport = i = atoi(arg[2]);
        pppoe.port = atoi(arg[3]);
        pppoe.sess = atoi(arg[6]);
        o = atoi(arg[4]);
        neir.port = pppoe.port;
        neir.sess = pppoe.sess;
        neir.cmd = 2;
        str2mac(&neir.macs[0], arg[7]);
        str2mac(&neir.macs[6], arg[8]);
        if (del == 0) {
            if (bpf_map_delete_elem(pppoes_fd, &pppoe) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(pppoes_fd, &pppoe, &i, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &o, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "gre4") == 0) {
        struct tunnel4_key tunk;
        memset(&tunk, 0, sizeof(tunk));
        struct tunnel_res tunr;
        memset(&tunr, 0, sizeof(tunr));
        o = atoi(arg[2]);
        neir.aclport = tunr.aclport = atoi(arg[3]);
        neir.port = atoi(arg[4]);
        inet_pton(AF_INET, arg[5], buf2);
        memcpy(&tunk.trgAddr, &buf2, sizeof(tunk.trgAddr));
        memcpy(&neir.srcAddr, &buf2, sizeof(tunk.trgAddr));
        inet_pton(AF_INET, arg[6], buf2);
        memcpy(&tunk.srcAddr, &buf2, sizeof(tunk.srcAddr));
        memcpy(&neir.trgAddr, &buf2, sizeof(tunk.srcAddr));
        tunk.vrf = atoi(arg[8]);
        str2mac(&neir.macs[0], arg[7]);
        str2mac(&neir.macs[6], arg[9]);
        neir.cmd = 3;
        tunk.srcPort = 0;
        tunk.trgPort = 0;
        tunk.prot = IP_PROTOCOL_GRE;
        tunr.cmd = 1;
        if (del == 0) {
            if (bpf_map_delete_elem(tunnel4_fd, &tunk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(tunnel4_fd, &tunk, &tunr, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &o, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "gre6") == 0) {
        struct tunnel6_key tunk;
        memset(&tunk, 0, sizeof(tunk));
        struct tunnel_res tunr;
        memset(&tunr, 0, sizeof(tunr));
        o = atoi(arg[2]);
        neir.aclport = tunr.aclport = atoi(arg[3]);
        neir.port = atoi(arg[4]);
        inet_pton(AF_INET6, arg[5], buf2);
        memcpy(&tunk.trgAddr, &buf2, sizeof(tunk.trgAddr));
        memcpy(&neir.srcAddr, &buf2, sizeof(tunk.trgAddr));
        inet_pton(AF_INET6, arg[6], buf2);
        memcpy(&tunk.srcAddr, &buf2, sizeof(tunk.srcAddr));
        memcpy(&neir.trgAddr, &buf2, sizeof(tunk.srcAddr));
        tunk.vrf = atoi(arg[8]);
        str2mac(&neir.macs[0], arg[7]);
        str2mac(&neir.macs[6], arg[9]);
        neir.cmd = 4;
        tunk.srcPort = 0;
        tunk.trgPort = 0;
        tunk.prot = IP_PROTOCOL_GRE;
        tunr.cmd = 1;
        if (del == 0) {
            if (bpf_map_delete_elem(tunnel6_fd, &tunk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(tunnel6_fd, &tunk, &tunr, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &o, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "l2tp4") == 0) {
        struct tunnel4_key tunk;
        memset(&tunk, 0, sizeof(tunk));
        struct tunnel_res tunr;
        memset(&tunr, 0, sizeof(tunr));
        o = atoi(arg[2]);
        neir.aclport = tunr.aclport = atoi(arg[3]);
        neir.port = atoi(arg[4]);
        inet_pton(AF_INET, arg[5], buf2);
        memcpy(&tunk.trgAddr, &buf2, sizeof(tunk.trgAddr));
        memcpy(&neir.srcAddr, &buf2, sizeof(tunk.trgAddr));
        inet_pton(AF_INET, arg[6], buf2);
        memcpy(&tunk.srcAddr, &buf2, sizeof(tunk.srcAddr));
        memcpy(&neir.trgAddr, &buf2, sizeof(tunk.srcAddr));
        tunk.vrf = atoi(arg[8]);
        str2mac(&neir.macs[0], arg[7]);
        str2mac(&neir.macs[6], arg[9]);
        neir.cmd = 5;
        neir.srcPort = tunk.trgPort = atoi(arg[10]);
        neir.trgPort = tunk.srcPort = atoi(arg[11]);
        neir.sess = atoi(arg[12]);
        tunk.prot = IP_PROTOCOL_UDP;
        tunr.cmd = 2;
        if (del == 0) {
            if (bpf_map_delete_elem(tunnel4_fd, &tunk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(tunnel4_fd, &tunk, &tunr, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &o, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "l2tp6") == 0) {
        struct tunnel6_key tunk;
        memset(&tunk, 0, sizeof(tunk));
        struct tunnel_res tunr;
        memset(&tunr, 0, sizeof(tunr));
        o = atoi(arg[2]);
        neir.aclport = tunr.aclport = atoi(arg[3]);
        neir.port = atoi(arg[4]);
        inet_pton(AF_INET6, arg[5], buf2);
        memcpy(&tunk.trgAddr, &buf2, sizeof(tunk.trgAddr));
        memcpy(&neir.srcAddr, &buf2, sizeof(tunk.trgAddr));
        inet_pton(AF_INET6, arg[6], buf2);
        memcpy(&tunk.srcAddr, &buf2, sizeof(tunk.srcAddr));
        memcpy(&neir.trgAddr, &buf2, sizeof(tunk.srcAddr));
        tunk.vrf = atoi(arg[8]);
        str2mac(&neir.macs[0], arg[7]);
        str2mac(&neir.macs[6], arg[9]);
        neir.cmd = 6;
        neir.srcPort = tunk.trgPort = atoi(arg[10]);
        neir.trgPort = tunk.srcPort = atoi(arg[11]);
        neir.sess = atoi(arg[12]);
        tunk.prot = IP_PROTOCOL_UDP;
        tunr.cmd = 2;
        if (del == 0) {
            if (bpf_map_delete_elem(tunnel6_fd, &tunk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(tunnel6_fd, &tunk, &tunr, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &o, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "l3tp4") == 0) {
        struct tunnel4_key tunk;
        memset(&tunk, 0, sizeof(tunk));
        struct tunnel_res tunr;
        memset(&tunr, 0, sizeof(tunr));
        o = atoi(arg[2]);
        neir.aclport = tunr.aclport = atoi(arg[3]);
        neir.port = atoi(arg[4]);
        inet_pton(AF_INET, arg[5], buf2);
        memcpy(&tunk.trgAddr, &buf2, sizeof(tunk.trgAddr));
        memcpy(&neir.srcAddr, &buf2, sizeof(tunk.trgAddr));
        inet_pton(AF_INET, arg[6], buf2);
        memcpy(&tunk.srcAddr, &buf2, sizeof(tunk.srcAddr));
        memcpy(&neir.trgAddr, &buf2, sizeof(tunk.srcAddr));
        tunk.vrf = atoi(arg[8]);
        str2mac(&neir.macs[0], arg[7]);
        str2mac(&neir.macs[6], arg[9]);
        neir.cmd = 7;
        tunk.trgPort = 0;
        tunk.srcPort = 0;
        neir.sess = atoi(arg[10]);
        tunk.prot = IP_PROTOCOL_L2TP;
        tunr.cmd = 3;
        if (del == 0) {
            if (bpf_map_delete_elem(tunnel4_fd, &tunk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(tunnel4_fd, &tunk, &tunr, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &o, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "l3tp6") == 0) {
        struct tunnel6_key tunk;
        memset(&tunk, 0, sizeof(tunk));
        struct tunnel_res tunr;
        memset(&tunr, 0, sizeof(tunr));
        o = atoi(arg[2]);
        neir.aclport = tunr.aclport = atoi(arg[3]);
        neir.port = atoi(arg[4]);
        inet_pton(AF_INET6, arg[5], buf2);
        memcpy(&tunk.trgAddr, &buf2, sizeof(tunk.trgAddr));
        memcpy(&neir.srcAddr, &buf2, sizeof(tunk.trgAddr));
        inet_pton(AF_INET6, arg[6], buf2);
        memcpy(&tunk.srcAddr, &buf2, sizeof(tunk.srcAddr));
        memcpy(&neir.trgAddr, &buf2, sizeof(tunk.srcAddr));
        tunk.vrf = atoi(arg[8]);
        str2mac(&neir.macs[0], arg[7]);
        str2mac(&neir.macs[6], arg[9]);
        neir.cmd = 8;
        tunk.trgPort = 0;
        tunk.srcPort = 0;
        neir.sess = atoi(arg[10]);
        tunk.prot = IP_PROTOCOL_L2TP;
        tunr.cmd = 3;
        if (del == 0) {
            if (bpf_map_delete_elem(tunnel6_fd, &tunk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(tunnel6_fd, &tunk, &tunr, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &o, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "gtp4") == 0) {
        struct tunnel4_key tunk;
        memset(&tunk, 0, sizeof(tunk));
        struct tunnel_res tunr;
        memset(&tunr, 0, sizeof(tunr));
        o = atoi(arg[2]);
        neir.aclport = tunr.aclport = atoi(arg[3]);
        neir.port = atoi(arg[4]);
        inet_pton(AF_INET, arg[5], buf2);
        memcpy(&tunk.trgAddr, &buf2, sizeof(tunk.trgAddr));
        memcpy(&neir.srcAddr, &buf2, sizeof(tunk.trgAddr));
        inet_pton(AF_INET, arg[6], buf2);
        memcpy(&tunk.srcAddr, &buf2, sizeof(tunk.srcAddr));
        memcpy(&neir.trgAddr, &buf2, sizeof(tunk.srcAddr));
        tunk.vrf = atoi(arg[8]);
        str2mac(&neir.macs[0], arg[7]);
        str2mac(&neir.macs[6], arg[9]);
        neir.cmd = 9;
        neir.srcPort = tunk.trgPort = atoi(arg[10]);
        neir.trgPort = tunk.srcPort = atoi(arg[11]);
        neir.sess = atoi(arg[12]);
        tunk.prot = IP_PROTOCOL_UDP;
        tunr.cmd = 4;
        if (del == 0) {
            if (bpf_map_delete_elem(tunnel4_fd, &tunk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(tunnel4_fd, &tunk, &tunr, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &o, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    if (strcmp(arg[0], "gtp6") == 0) {
        struct tunnel6_key tunk;
        memset(&tunk, 0, sizeof(tunk));
        struct tunnel_res tunr;
        memset(&tunr, 0, sizeof(tunr));
        o = atoi(arg[2]);
        neir.aclport = tunr.aclport = atoi(arg[3]);
        neir.port = atoi(arg[4]);
        inet_pton(AF_INET6, arg[5], buf2);
        memcpy(&tunk.trgAddr, &buf2, sizeof(tunk.trgAddr));
        memcpy(&neir.srcAddr, &buf2, sizeof(tunk.trgAddr));
        inet_pton(AF_INET6, arg[6], buf2);
        memcpy(&tunk.srcAddr, &buf2, sizeof(tunk.srcAddr));
        memcpy(&neir.trgAddr, &buf2, sizeof(tunk.srcAddr));
        tunk.vrf = atoi(arg[8]);
        str2mac(&neir.macs[0], arg[7]);
        str2mac(&neir.macs[6], arg[9]);
        neir.cmd = 10;
        neir.srcPort = tunk.trgPort = atoi(arg[10]);
        neir.trgPort = tunk.srcPort = atoi(arg[11]);
        neir.sess = atoi(arg[12]);
        tunk.prot = IP_PROTOCOL_UDP;
        tunr.cmd = 4;
        if (del == 0) {
            if (bpf_map_delete_elem(tunnel6_fd, &tunk) != 0) warn("error removing entry");
            if (bpf_map_delete_elem(neighs_fd, &o) != 0) warn("error removing entry");
        } else {
            if (bpf_map_update_elem(tunnel6_fd, &tunk, &tunr, BPF_ANY) != 0) warn("error setting entry");
            if (bpf_map_update_elem(neighs_fd, &o, &neir, BPF_ANY) != 0) warn("error setting entry");
        }
        return 0;
    }
    return 0;
#endif
}
