/*
 * Copyright 2019-present GÉANT RARE project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed On an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _IG_CTL_BRIDGE_P4_
#define _IG_CTL_BRIDGE_P4_

#ifdef HAVE_BRIDGE

control IngressControlBridge(inout headers hdr, inout ingress_metadata_t ig_md,
                             in ingress_intrinsic_metadata_t ig_intr_md)
{

    action act_set_bridge_port() {
        ig_md.bridge_src = 1;
    }

    action act_bridge_miss() {
        ig_md.bridge_src = 0;
        ig_md.nexthop_id = CPU_PORT;
        // Packets sent to the controller needs to be prepended with the
        // packet-in header. By setting it valid we make sure it will be
        // deparsed on the wire (see c_deparser).
    }

    table tbl_bridge_learn {
        key = {
ig_md.bridge_id:
            exact;
hdr.ethernet.src_mac_addr:
            exact;
        } actions = {
            act_set_bridge_port;
            act_bridge_miss;
        }
        size = VRF_TABLE_SIZE;
        default_action = act_bridge_miss();
    }

    action act_set_bridge_out(SubIntId_t port) {
        ig_md.bridge_trg = port;
        ig_md.target_id = port;
    }

#ifdef HAVE_TAP


    action act_set_bridge_routed(NextHopId_t nexthop) {
        ig_md.bridge_trg = MAX_PORT;
        ig_md.vrf = 0;
#ifdef HAVE_MPLS
        ig_md.mpls0_valid = 0;
        ig_md.mpls1_valid = 0;
#endif
        ig_md.arp_valid = 0;
        ig_md.ipv4_valid = 0;
        ig_md.ipv6_valid = 0;
        hdr.eth4.setValid();
        hdr.eth4.dst_mac_addr = hdr.ethernet.dst_mac_addr;
        hdr.eth4.src_mac_addr = hdr.ethernet.src_mac_addr;
        hdr.eth4.ethertype = ig_md.ethertype + ig_md.always_zero; // hack
        ig_md.nexthop_id = nexthop;
        ig_md.ethertype = ETHERTYPE_ROUTEDMAC;
#ifdef NEED_PKTLEN
        ig_md.pktlen = ig_md.pktlen + 14;
#endif
    }
#endif

#ifdef HAVE_MPLS
    action act_set_bridge_vpls(NextHopId_t port, label_t lab_tun, label_t lab_svc) {
        ig_md.bridge_trg = MAX_PORT;
        ig_md.mpls0_remove = 0;
        ig_md.mpls1_remove = 0;
        ig_md.mpls_encap_egress_label = lab_tun;
        ig_md.mpls_encap_svc_label = lab_svc;
        ig_md.mpls_encap_decap_sap_type = 2;
        ig_md.mpls_encap_l2vpn_valid = 1;
        ig_md.nexthop_id = port;
        ig_md.mpls_op_type = 3;
    }
#endif

#ifdef HAVE_VXLAN
    action act_set_bridge_vxlan4(NextHopId_t nexthop, ipv4_addr_t dst_ip_addr, ipv4_addr_t src_ip_addr, bit<24> instance) {
        ig_md.bridge_trg = MAX_PORT;
        ig_md.vrf = 0;
#ifdef HAVE_MPLS
        ig_md.mpls0_valid = 0;
        ig_md.mpls1_valid = 0;
#endif
        ig_md.arp_valid = 0;
        ig_md.ipv4_valid = 0;
        ig_md.ipv6_valid = 0;
        hdr.vlan.setInvalid();
        hdr.eth2.setValid();
        hdr.eth2 = hdr.ethernet;
        hdr.eth2.ethertype = ig_md.ethertype;
        ig_md.nexthop_id = nexthop;

        hdr.vxlan2.setValid();
        hdr.vxlan2.flags = 0x800;
        hdr.vxlan2.policy = 0;
        hdr.vxlan2.instance = instance;
        hdr.vxlan2.reserved = 0;

        hdr.udp2.setValid();
        hdr.udp2.src_port = 4789;
        hdr.udp2.dst_port = 4789;
        hdr.udp2.length = ig_md.pktlen + 30;
        hdr.udp2.checksum = 0;

        hdr.ipv4d.setValid();
        hdr.ipv4d.version = 4;
        hdr.ipv4d.ihl = 5;
        hdr.ipv4d.diffserv = 0;
        hdr.ipv4d.total_len = ig_md.pktlen + 50;
        hdr.ipv4d.identification = 0;
        hdr.ipv4d.flags = 0;
        hdr.ipv4d.frag_offset = 0;
        hdr.ipv4d.ttl = 255;
        hdr.ipv4d.protocol = IP_PROTOCOL_UDP;
        hdr.ipv4d.hdr_checksum = 0;
        hdr.ipv4d.src_addr = src_ip_addr;
        hdr.ipv4d.dst_addr = dst_ip_addr;
        ig_md.ethertype = ETHERTYPE_IPV4;
    }
#endif

#ifdef HAVE_VXLAN
    action act_set_bridge_vxlan6(NextHopId_t nexthop, ipv6_addr_t dst_ip_addr, ipv6_addr_t src_ip_addr, bit<24> instance) {
        ig_md.bridge_trg = MAX_PORT;
        ig_md.vrf = 0;
#ifdef HAVE_MPLS
        ig_md.mpls0_valid = 0;
        ig_md.mpls1_valid = 0;
#endif
        ig_md.arp_valid = 0;
        ig_md.ipv4_valid = 0;
        ig_md.ipv6_valid = 0;
        hdr.vlan.setInvalid();
        hdr.eth2.setValid();
        hdr.eth2 = hdr.ethernet;
        hdr.eth2.ethertype = ig_md.ethertype;
        ig_md.nexthop_id = nexthop;

        hdr.vxlan2.setValid();
        hdr.vxlan2.flags = 0x800;
        hdr.vxlan2.policy = 0;
        hdr.vxlan2.instance = instance;
        hdr.vxlan2.reserved = 0;

        hdr.udp2.setValid();
        hdr.udp2.src_port = 4789;
        hdr.udp2.dst_port = 4789;
        hdr.udp2.length = ig_md.pktlen + 30;
        hdr.udp2.checksum = 0;

        hdr.ipv6d.setValid();
        hdr.ipv6d.version = 6;
        hdr.ipv6d.traffic_class = 0;
        hdr.ipv6d.flow_label = 0;
        hdr.ipv6d.payload_len = ig_md.pktlen + 30;
        hdr.ipv6d.next_hdr = IP_PROTOCOL_UDP;
        hdr.ipv6d.hop_limit = 255;
        hdr.ipv6d.src_addr = src_ip_addr;
        hdr.ipv6d.dst_addr = dst_ip_addr;
        ig_md.ethertype = ETHERTYPE_IPV6;
    }
#endif


#ifdef HAVE_PCKOUDP
    action act_set_bridge_pckoudp4(NextHopId_t nexthop, ipv4_addr_t dst_ip_addr, ipv4_addr_t src_ip_addr, bit<16> src_port, bit<16> dst_port) {
        ig_md.bridge_trg = MAX_PORT;
        ig_md.vrf = 0;
#ifdef HAVE_MPLS
        ig_md.mpls0_valid = 0;
        ig_md.mpls1_valid = 0;
#endif
        ig_md.arp_valid = 0;
        ig_md.ipv4_valid = 0;
        ig_md.ipv6_valid = 0;
        hdr.vlan.setInvalid();
        hdr.eth2.setValid();
        hdr.eth2 = hdr.ethernet;
        hdr.eth2.ethertype = ig_md.ethertype;
        ig_md.nexthop_id = nexthop;

        hdr.udp2.setValid();
        hdr.udp2.src_port = src_port;
        hdr.udp2.dst_port = dst_port;
        hdr.udp2.length = ig_md.pktlen + 22;
        hdr.udp2.checksum = 0;

        hdr.ipv4d.setValid();
        hdr.ipv4d.version = 4;
        hdr.ipv4d.ihl = 5;
        hdr.ipv4d.diffserv = 0;
        hdr.ipv4d.total_len = ig_md.pktlen + 42;
        hdr.ipv4d.identification = 0;
        hdr.ipv4d.flags = 0;
        hdr.ipv4d.frag_offset = 0;
        hdr.ipv4d.ttl = 255;
        hdr.ipv4d.protocol = IP_PROTOCOL_UDP;
        hdr.ipv4d.hdr_checksum = 0;
        hdr.ipv4d.src_addr = src_ip_addr;
        hdr.ipv4d.dst_addr = dst_ip_addr;
        ig_md.ethertype = ETHERTYPE_IPV4;
    }
#endif


#ifdef HAVE_PCKOUDP
    action act_set_bridge_pckoudp6(NextHopId_t nexthop, ipv6_addr_t dst_ip_addr, ipv6_addr_t src_ip_addr, bit<16> src_port, bit<16> dst_port) {
        ig_md.bridge_trg = MAX_PORT;
        ig_md.vrf = 0;
#ifdef HAVE_MPLS
        ig_md.mpls0_valid = 0;
        ig_md.mpls1_valid = 0;
#endif
        ig_md.arp_valid = 0;
        ig_md.ipv4_valid = 0;
        ig_md.ipv6_valid = 0;
        hdr.vlan.setInvalid();
        hdr.eth2.setValid();
        hdr.eth2 = hdr.ethernet;
        hdr.eth2.ethertype = ig_md.ethertype;
        ig_md.nexthop_id = nexthop;

        hdr.udp2.setValid();
        hdr.udp2.src_port = src_port;
        hdr.udp2.dst_port = dst_port;
        hdr.udp2.length = ig_md.pktlen + 22;
        hdr.udp2.checksum = 0;

        hdr.ipv6d.setValid();
        hdr.ipv6d.version = 6;
        hdr.ipv6d.traffic_class = 0;
        hdr.ipv6d.flow_label = 0;
        hdr.ipv6d.payload_len = ig_md.pktlen + 22;
        hdr.ipv6d.next_hdr = IP_PROTOCOL_UDP;
        hdr.ipv6d.hop_limit = 255;
        hdr.ipv6d.src_addr = src_ip_addr;
        hdr.ipv6d.dst_addr = dst_ip_addr;
        ig_md.ethertype = ETHERTYPE_IPV6;
    }
#endif


    action act_bridge_punt() {
        ig_md.bridge_trg = 0;
        ig_md.nexthop_id = CPU_PORT;
        // Packets sent to the controller needs to be prepended with the
        // packet-in header. By setting it valid we make sure it will be
        // deparsed on the wire (see c_deparser).
    }

    table tbl_bridge_target {
        key = {
ig_md.bridge_id:
            exact;
hdr.ethernet.dst_mac_addr:
            exact;
        } actions = {
            act_set_bridge_out;
#ifdef HAVE_TAP
            act_set_bridge_routed;
#endif
#ifdef HAVE_MPLS
            act_set_bridge_vpls;
#endif
#ifdef HAVE_VXLAN
            act_set_bridge_vxlan4;
            act_set_bridge_vxlan6;
#endif
#ifdef HAVE_PCKOUDP
            act_set_bridge_pckoudp4;
            act_set_bridge_pckoudp6;
#endif
            act_bridge_punt;
        }
        size = VRF_TABLE_SIZE;
        default_action = act_bridge_punt();
    }

    apply {

        if (ig_md.bridge_id != 0) {
            ig_md.vrf = 0;
#ifdef HAVE_MPLS
            ig_md.mpls0_valid = 0;
            ig_md.mpls1_valid = 0;
#endif
            ig_md.arp_valid = 0;
            ig_md.ipv4_valid = 0;
            ig_md.ipv6_valid = 0;

#ifdef HAVE_TAP
            if (hdr.eth6.isValid()) {
                hdr.ethernet.dst_mac_addr = hdr.eth6.dst_mac_addr;
                hdr.ethernet.src_mac_addr = hdr.eth6.src_mac_addr;
            }
#endif

            if (tbl_bridge_learn.apply().hit) if (tbl_bridge_target.apply().hit) {

#ifdef HAVE_TAP
                    if (hdr.eth6.isValid()) {
                        ig_md.ethertype = hdr.eth6.ethertype;
                        hdr.eth6.setInvalid();
                    }
#endif

#ifdef HAVE_MPLS
                    if (hdr.mpls1.isValid() && (ig_md.mpls_op_type != 3)) {
                        hdr.eth2.setInvalid();
                        hdr.mpls1.setInvalid();
                        hdr.mpls0.setInvalid();
                        hdr.vlan.setInvalid();
                        ig_md.mpls0_remove = 0;
                        ig_md.mpls1_remove = 0;
                    }
#endif
                }
        }

    }
}

#endif

#endif // _IG_CTL_BRIDGE_P4_
