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

#ifndef _EGRESS_CONTROL_P4_
#define _EGRESS_CONTROL_P4_

control eg_ctl(
    inout headers hdr, inout ingress_metadata_t eg_md,
    in egress_intrinsic_metadata_t eg_intr_md,
    in egress_intrinsic_metadata_from_parser_t eg_prsr_md,
    inout egress_intrinsic_metadata_for_deparser_t eg_dprsr_md,
    inout egress_intrinsic_metadata_for_output_port_t eg_oport_md)
{
#ifdef HAVE_NOHW

    apply {
    }

#else


#ifdef HAVE_MCAST
    EgressControlMcast() eg_ctl_mcast;
#endif
    EgressControlVlanOut() eg_ctl_vlan_out;
    EgressControlHairpin() eg_ctl_hairpin;
    EgressControlNexthop()eg_ctl_nexthop;
#ifdef HAVE_OUTACL
    EgressControlAclOut() eg_ctl_acl_out;
#endif
#ifdef HAVE_OUTQOS
    EgressControlQosOut() eg_ctl_qos_out;
#endif



    apply {

#ifdef NEED_PKTLEN
        eg_md.pktlen = hdr.internal.pktlen;
#endif
        eg_md.ethertype = hdr.ethernet.ethertype;
#ifdef HAVE_TAP
        if (hdr.ethernet.ethertype == ETHERTYPE_ROUTEDMAC_INT) {
            eg_md.ethertype = ETHERTYPE_ROUTEDMAC;
        }
#endif

        if (hdr.internal.reason == INTREAS_RECIR) {
            hdr.cpu.setValid();
            hdr.cpu.port = hdr.internal.port_id;
        }

#ifdef HAVE_MCAST
        eg_ctl_mcast.apply(hdr, eg_md, eg_intr_md, eg_dprsr_md);
#endif

        eg_ctl_nexthop.apply(hdr, eg_md, eg_intr_md, eg_dprsr_md);
#ifdef HAVE_OUTACL
        eg_ctl_acl_out.apply(hdr, eg_md, eg_intr_md, eg_dprsr_md);
#endif
#ifdef HAVE_OUTQOS
        eg_ctl_qos_out.apply(hdr, eg_md, eg_intr_md, eg_dprsr_md);
#endif
        eg_ctl_vlan_out.apply(hdr, eg_md, eg_intr_md, eg_dprsr_md);
        eg_ctl_hairpin.apply(hdr, eg_md, eg_intr_md, eg_dprsr_md);

        hdr.internal.setInvalid();
    }

#endif

}

#endif // _EGRESS_CONTROL_P4_
