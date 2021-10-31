/*
 * Copyright 2019-present GT RARE project
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

#ifndef _TABLE_SIZE_P4_
#define _TABLE_SIZE_P4_

/*
on routing only profile we can have this
#define IPV4_LPM_TABLE_SIZE                    34816
#define IPV6_LPM_TABLE_SIZE                    4096
*/
#ifdef HAVE_SRV6
#define IPV4_LPM_TABLE_SIZE                    2048
#define IPV6_LPM_TABLE_SIZE                    2048
#else
#define IPV4_LPM_TABLE_SIZE                    2048
#define IPV6_LPM_TABLE_SIZE                    2048
#endif

#define IPV4_HOST_TABLE_SIZE                   512
#define IPV6_HOST_TABLE_SIZE                   512
#define IPV4_MCAST_TABLE_SIZE                  512
#define IPV6_MCAST_TABLE_SIZE                  512

#define MPLS_TABLE_SIZE                        1024
#define POLKA_TABLE_SIZE                       512
#define NSH_TABLE_SIZE                         512
#define PORT_TABLE_SIZE                        512
#define MAC_TABLE_SIZE                         512
#define BUNDLE_TABLE_SIZE                      512
#define VLAN_TABLE_SIZE                        512

#define NEXTHOP_TABLE_SIZE                     1024
#define PPPOE_TABLE_SIZE                       512
#define IPV4_TUNNEL_TABLE_SIZE                 512
#define IPV6_TUNNEL_TABLE_SIZE                 512

#define IPV4_NATTRNS_TABLE_SIZE                512
#define IPV6_NATTRNS_TABLE_SIZE                512
#define IPV4_NATACL_TABLE_SIZE                 512
#define IPV6_NATACL_TABLE_SIZE                 512
#define IPV4_PBRACL_TABLE_SIZE                 512
#define IPV6_PBRACL_TABLE_SIZE                 512

#define IPV4_COPP_TABLE_SIZE                   512
#define IPV6_COPP_TABLE_SIZE                   512
#define IPV4_INACL_TABLE_SIZE                  512
#define IPV6_INACL_TABLE_SIZE                  512
#define IPV4_OUTACL_TABLE_SIZE                 512
#define IPV6_OUTACL_TABLE_SIZE                 512

#define IPV4_INQOS_TABLE_SIZE                  512
#define IPV6_INQOS_TABLE_SIZE                  512
#define IPV4_OUTQOS_TABLE_SIZE                 512
#define IPV6_OUTQOS_TABLE_SIZE                 512
#define IPV4_FLOWSPEC_TABLE_SIZE               512
#define IPV6_FLOWSPEC_TABLE_SIZE               512

#endif // _TABLE_SIZE_P4_
