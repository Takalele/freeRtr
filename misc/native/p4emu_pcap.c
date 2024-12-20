#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <pthread.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <pcap.h>


#include "p4emu_hdr.h"

pcap_t *ifacePcap[maxPorts];

void sendPack(unsigned char *bufD, int bufS, int port) {
    pcap_sendpacket(ifacePcap[port], bufD, bufS);
}

void setMtu(int port, int mtu) {
}

void setState(int port, int sta) {
}

int getState(int port) {
    return 1;
}


void getStats(int port, unsigned char*buf, unsigned char*pre, int*len) {
    struct pcap_stat stat;
    if (pcap_stats(ifacePcap[port], &stat) != 0) return;
    *len += snprintf((char*)&buf[*len], 128, "%s ps_recv %i\r\n", (char*)pre, stat.ps_recv);
    *len += snprintf((char*)&buf[*len], 128, "%s ps_drop %i\r\n", (char*)pre, stat.ps_drop);
    *len += snprintf((char*)&buf[*len], 128, "%s ps_ifdrop %i\r\n", (char*)pre, stat.ps_ifdrop);
}



void err(char*buf) {
    printf("%s\n", buf);
    _exit(1);
}




int commandSock;
int ifaceId[maxPorts];



#define packGet                                     \
    if (fail++ > 1024) break;                       \
    pack = pcap_next(ifacePcap[port], &head);       \
    if (pack == NULL) continue;                     \
    bufS = head.caplen;                             \
    if (bufS < 1) continue;                         \
    memcpy(&bufD[preBuff], pack, bufS);             \
    fail = 0;



void doIfaceLoop(int * param) {
    int port = *param;
    const unsigned char *pack;
    int bufS;
    int fail = 0;
    struct pcap_pkthdr head;
    struct packetContext ctx;
    if (initContext(&ctx) != 0) err("error initializing context");
    unsigned char *bufD = ctx.bufD;
    ctx.port = port;
    if (port == cpuPort) {
        for (;;) {
            packGet;
            processCpuPack(&ctx, bufS);
        }
    } else {
        for (;;) {
            packGet;
            processDataPacket(&ctx, bufS, port);
        }
    }
    err("port thread exited");
}



void doSockLoop() {
    struct packetContext ctx;
    if (initContext(&ctx) != 0) err("error initializing context");
    FILE *commands = fdopen(commandSock, "r");
    if (commands == NULL) err("failed to open file");
    unsigned char buf[16384];
    for (;;) {
        memset(&buf, 0, sizeof(buf));
        if (fgets((char*)&buf[0], sizeof(buf), commands) == NULL) break;
        if (doOneCommand(&ctx, &buf[0]) != 0) break;
    }
    err("command thread exited");
}



void doStatLoop() {
    FILE *commands = fdopen(commandSock, "w");
    if (commands == NULL) err("failed to open file");
    fprintf(commands, "platform %spcap\r\n", platformBase);
    fprintf(commands, "capabilities %s\r\n", getCapas());
    for (int i = 0; i < dataPorts; i++) fprintf(commands, "portname %i %s\r\n", i, ifaceName[i]);
    fprintf(commands, "cpuport %i\r\n", cpuPort);
    fprintf(commands, "dynrange %i 1073741823\r\n", maxPorts);
    fprintf(commands, "vrfrange 1 1073741823\r\n");
    fprintf(commands, "neirange 4096 1073741823\r\n");
    fprintf(commands, "nomore\r\n");
    fflush(commands);
    int rnd = 0;
    for (;;) {
        doStatRound(commands, rnd);
        rnd++;
        usleep(100000);
    }
    err("stat thread exited");
}




void doMainLoop() {
    unsigned char buf[1024];

    for (;;) {
        printf("> ");
        buf[0] = 0;
        int i = scanf("%1023s", buf);
        if (i < 1) {
            sleep(1);
            continue;
        }
        if (doConsoleCommand(&buf[0]) != 0) break;
        printf("\n");
    }
    err("main thread exited");
}




int main(int argc, char **argv) {
    char errbuf[PCAP_ERRBUF_SIZE + 1];

    dataPorts = 0;
    for (int i = 4; i < argc; i++) {
        initIface(dataPorts, argv[i]);
        dataPorts++;
    }
    if (dataPorts < 2) err("using: dp <addr> <port> <cpuport> <ifc0> <ifc1> [ifcN]");
    if (dataPorts > maxPorts) dataPorts = maxPorts;
    printf("pcap version: %s\n", pcap_lib_version());
    if (initTables() != 0) err("error initializing tables");
    int port = atoi(argv[2]);
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof (addr));
    if (inet_aton(argv[1], &addr.sin_addr) == 0) err("bad addr address");
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    printf("connecting %s %i.\n", inet_ntoa(addr.sin_addr), port);
    commandSock = socket(AF_INET, SOCK_STREAM, 0);
    if (commandSock < 0) err("unable to open socket");
    if (connect(commandSock, (struct sockaddr*)&addr, sizeof(addr)) < 0) err("failed to connect socket");
    cpuPort = atoi(argv[3]);
    printf("cpu port is #%i of %i...\n", cpuPort, dataPorts);

    for (int i = 0; i < dataPorts; i++) {
        printf("opening interface %s\n", ifaceName[i]);
        ifacePcap[i] = pcap_create(ifaceName[i], errbuf);
        if (ifacePcap[i] == NULL) err("unable to open interface");
        if (pcap_set_snaplen(ifacePcap[i], 65536) < 0) err("unable to set snaplen");
        if (pcap_set_promisc(ifacePcap[i], 1) < 0) err("unable to set promisc");
        if (pcap_set_immediate_mode(ifacePcap[i], 1) < 0) err("unable to set immediate");
        if (pcap_activate(ifacePcap[i]) < 0) {
            if (i < (dataPorts-1)) err("activation failed");
            dataPorts--;
            break;
        }
        if (pcap_setdirection(ifacePcap[i], PCAP_D_IN) < 0) err("unable to set direction");
        ifaceId[i] = i;
    }

    setgid(1);
    setuid(1);
    pthread_t threadSock;
    if (pthread_create(&threadSock, NULL, (void*) & doSockLoop, NULL)) err("error creating socket thread");
    pthread_t threadStat;
    if (pthread_create(&threadStat, NULL, (void*) & doStatLoop, NULL)) err("error creating status thread");
    pthread_t threadRaw[maxPorts];
    for (int i=0; i < dataPorts; i++) {
        if (pthread_create(&threadRaw[i], NULL, (void*) & doIfaceLoop, &ifaceId[i])) err("error creating port thread");
    }

    doMainLoop();
}
