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

#ifndef _INGRESS_CONTROL_P4_
#define _INGRESS_CONTROL_P4_

/*------------------ I N G R E S S - M A T C H - A C T I O N ---------------- */

control ig_ctl(inout headers hdr, inout ingress_metadata_t ig_md,
               in ingress_intrinsic_metadata_t ig_intr_md,
               in ingress_intrinsic_metadata_from_parser_t ig_prsr_md,
               inout ingress_intrinsic_metadata_for_deparser_t ig_dprsr_md,
               inout ingress_intrinsic_metadata_for_tm_t ig_tm_md)
{

#ifdef HAVE_NOHW

    apply {
        if (ig_intr_md.ingress_port == CPU_PORT) {
            ig_tm_md.ucast_egress_port =(PortId_t) hdr.cpu.port;
            ig_tm_md.bypass_egress = 1;
            hdr.cpu.setInvalid();
        } else {
            hdr.cpu.setValid();
            hdr.cpu.port = ig_md.ingress_id;
            ig_tm_md.ucast_egress_port = CPU_PORT;
            ig_tm_md.bypass_egress = 1;
        }
    }

#else


    IngressControlBundle() ig_ctl_bundle;

#ifdef HAVE_MPLS
    IngressControlMPLS()ig_ctl_mpls;
#endif
#ifdef HAVE_PPPOE
    IngressControlPPPOE() ig_ctl_pppoe;
#endif
    IngressControlPktPreEmit()ig_ctl_pkt_pre_emit;
#ifdef HAVE_BRIDGE
    IngressControlBridge()ig_ctl_bridge;
#endif
    IngressControlIPv4()ig_ctl_ipv4;
    IngressControlIPv6()ig_ctl_ipv6;
#ifdef HAVE_SRV6
    IngressControlIPv4b() ig_ctl_ipv4b;
    IngressControlIPv6b() ig_ctl_ipv6b;
#endif
#ifdef HAVE_TUN
    IngressControlTunnel() ig_ctl_tunnel;
#endif
    IngressControlCoPP()ig_ctl_copp;
    IngressControlAclIn() ig_ctl_acl_in;
    IngressControlAclOut() ig_ctl_acl_out;
    IngressControlNexthop()ig_ctl_nexthop;
    IngressControlVlanIn()ig_ctl_vlan_in;
    IngressControlVlanOut()ig_ctl_vlan_out;
    IngressControlVRF()ig_ctl_vrf;
#ifdef HAVE_NAT
    IngressControlNAT() ig_ctl_nat;
#endif

    Counter< bit<64>, SubIntId_t> ((MAX_PORT+1), CounterType_t.PACKETS_AND_BYTES) pkt_out_stats;

    apply {

        ig_dprsr_md.drop_ctl = 0; /// hack for odd/even ports

        ig_ctl_vlan_in.apply(hdr, ig_md, ig_intr_md);

        if (ig_intr_md.ingress_port == CPU_PORT) {
            pkt_out_stats.count(ig_md.source_id);
            ig_tm_md.ucast_egress_port = (PortId_t)hdr.cpu.port;
            ig_tm_md.bypass_egress = 1;
            hdr.cpu.setInvalid();
        } else {
            if (ig_intr_md.ingress_port == RECIR_PORT) {
                hdr.cpu.setInvalid();
            }
#ifdef HAVE_PPPOE
            ig_ctl_pppoe.apply(hdr,ig_md,ig_intr_md, ig_dprsr_md, ig_tm_md);
#endif

            ig_ctl_acl_in.apply(hdr, ig_md, ig_intr_md, ig_dprsr_md, ig_tm_md);
            ig_ctl_vrf.apply(hdr, ig_md);
#ifdef HAVE_MPLS
            ig_ctl_mpls.apply(hdr, ig_md, ig_intr_md);
#endif
#ifdef HAVE_NAT
            ig_ctl_nat.apply(hdr,ig_md,ig_intr_md);
#endif
#ifdef HAVE_BRIDGE
            ig_ctl_bridge.apply(hdr, ig_md, ig_intr_md);
#endif
            if (ig_md.ipv4_valid == 1) {
                ig_ctl_ipv4.apply(hdr, ig_md, ig_intr_md, ig_dprsr_md, ig_tm_md);
            } else if (ig_md.ipv6_valid == 1) {
                ig_ctl_ipv6.apply(hdr, ig_md, ig_intr_md, ig_dprsr_md, ig_tm_md);
            } else if (ig_md.arp_valid == 1) {
                ig_md.nexthop_id = CPU_PORT;
            }
#ifdef HAVE_SRV6
            if (ig_md.srv_op_type == 4) {
                ig_ctl_ipv4b.apply(hdr, ig_md, ig_intr_md, ig_dprsr_md, ig_tm_md);
            } else if (ig_md.srv_op_type == 6) {
                ig_ctl_ipv6b.apply(hdr, ig_md, ig_intr_md, ig_dprsr_md, ig_tm_md);
            }
#endif
            ig_ctl_pkt_pre_emit.apply(hdr, ig_md, ig_intr_md, ig_tm_md);

            if (ig_md.nexthop_id == CPU_PORT) {
#ifdef HAVE_TUN
                ig_ctl_tunnel.apply(hdr,ig_md,ig_intr_md, ig_dprsr_md, ig_tm_md);
#endif
                ig_ctl_copp.apply(hdr, ig_md, ig_intr_md, ig_dprsr_md, ig_tm_md);
            } else {
                if (hdr.vlan.isValid()) hdr.vlan.setInvalid();
#ifdef HAVE_PPPOE
                if (hdr.pppoeD.isValid()) hdr.pppoeD.setInvalid();
#endif
                ig_ctl_nexthop.apply(hdr, ig_md, ig_dprsr_md);
                ig_ctl_acl_out.apply(hdr, ig_md, ig_intr_md, ig_dprsr_md, ig_tm_md);
                ig_ctl_vlan_out.apply(hdr, ig_md, ig_tm_md);
                ig_ctl_bundle.apply (hdr, ig_md, ig_dprsr_md, ig_tm_md);
            }
        }
    }

#endif

}



#endif // _INGRESS_CONTROL_P4_
