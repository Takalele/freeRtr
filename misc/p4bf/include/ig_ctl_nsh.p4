
/*
 * Copyright 2019-present GEANT RARE project
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

#ifndef _IG_CTL_NSH_P4_
#define _IG_CTL_NSH_P4_

#ifdef HAVE_NSH

control IngressControlNSH(inout headers hdr,
                          inout ingress_metadata_t ig_md,
                          in ingress_intrinsic_metadata_t ig_intr_md) {

    DirectCounter< bit<64> > (CounterType_t.PACKETS_AND_BYTES) stats;

    action act_route(switch_vrf_t vrf) {
        stats.count();
        ig_md.vrf = vrf;
        ig_md.nsh_remove = 1;
    }

    action act_forward(SubIntId_t port, mac_addr_t src, mac_addr_t dst, bit<24> sp, bit<8> si) {
        stats.count();
        ig_md.nsh_remove = 0;
        ig_md.target_id = port;
#ifdef HAVE_MPLS
        ig_md.mpls0_valid = 0;
        ig_md.mpls1_valid = 0;
#endif
        ig_md.ipv4_valid = 0;
        ig_md.ipv6_valid = 0;
        hdr.ethernet.dst_mac_addr = dst;
        hdr.ethernet.src_mac_addr = src;
        hdr.nsh.sp = sp;
        hdr.nsh.si = si;
    }

    table tbl_nsh {
        key = {
hdr.nsh.sp:
            exact;
hdr.nsh.si:
            exact;
        }
        actions = {
            act_forward;
            act_route;
            @defaultonly NoAction;
        }
        size = NSH_TABLE_SIZE;
        default_action = NoAction();
        counters = stats;
    }

    apply {
        if (ig_md.nsh_valid==1)  {
            tbl_nsh.apply();
            if (ig_md.nsh_remove==1) {
                if (hdr.nsh.next_proto == 1) ig_md.ethertype = ETHERTYPE_IPV4;
                else if (hdr.nsh.next_proto == 2) ig_md.ethertype = ETHERTYPE_IPV6;
                else if (hdr.nsh.next_proto == 5) ig_md.ethertype = ETHERTYPE_MPLS_UCAST;
            }
        }
    }
}

#endif

#endif // _IG_CTL_IPv4_P4_
