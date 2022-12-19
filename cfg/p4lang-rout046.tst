description p4lang: bundle vlan evpn/cmac with bgp

addrouter r1
int eth1 eth 0000.0000.1111 $1a$ $1b$
int eth2 eth 0000.0000.1111 $2b$ $2a$
!
vrf def v1
 rd 1:1
 label-mode per-prefix
 exit
bridge 1
 rd 1:1
 rt-both 1:1
 mac-learn
 exit
bundle 1
 exit
vrf def v9
 rd 1:1
 exit
int lo9
 vrf for v9
 ipv4 addr 10.10.10.227 255.255.255.255
 exit
int eth1
 vrf for v9
 ipv4 addr 10.11.12.254 255.255.255.0
 exit
int eth2
 exit
server dhcp4 eth1
 pool 10.11.12.1 10.11.12.99
 gateway 10.11.12.254
 netmask 255.255.255.0
 dns-server 10.10.10.227
 domain-name p4l
 static 0000.0000.2222 10.11.12.111
 interface eth1
 vrf v9
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.101 255.255.255.255
 ipv6 addr 4321::101 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int sdn1
 no autostat
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.0
 ipv6 addr 1234:1::1 ffff:ffff::
 ipv6 ena
 mpls enable
 mpls ldp4
 mpls ldp6
 exit
int sdn2
 no autostat
 vrf for v1
 ipv4 addr 1.1.2.1 255.255.255.0
 ipv6 addr 1234:2::1 ffff:ffff::
 ipv6 ena
 mpls enable
 mpls ldp4
 mpls ldp6
 exit
int sdn3
 no autostat
 bundle-gr 1
 exit
int sdn4
 no autostat
 bundle-gr 1
 exit
int bun1.111
 bridge-gr 1
 exit
router bgp4 1
 vrf v1
 address evpn
 local-as 1
 router-id 4.4.4.1
 temp a remote-as 1
 temp a update lo0
 temp a send-comm both
 temp a pmsi
 temp a route-reflect
 neigh 2.2.2.103 temp a
 neigh 2.2.2.104 temp a
 afi-evpn 101 bridge 1
 afi-evpn 101 update lo0
 afi-evpn 101 encap cmac
 exit
router bgp6 1
 vrf v1
 address evpn
 local-as 1
 router-id 6.6.6.1
 temp a remote-as 1
 temp a update lo0
 temp a send-comm both
 temp a pmsi
 temp a route-reflect
 neigh 4321::103 temp a
 neigh 4321::104 temp a
 exit
server p4lang p4
 interconnect eth2
 export-vrf v1
 export-br 1
 export-port sdn1 1 10
 export-port sdn2 2 10
 export-port sdn3 3 10
 export-port sdn4 4 10
 export-port bun1 dynamic
 vrf v9
 exit
ipv4 route v1 2.2.2.103 255.255.255.255 1.1.1.2
ipv4 route v1 2.2.2.104 255.255.255.255 1.1.2.2
ipv6 route v1 4321::103 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:1::2
ipv6 route v1 4321::104 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:2::2
ipv6 route v1 4444:3:: ffff:ffff:: 1234:1::2
ipv6 route v1 4444:4:: ffff:ffff:: 1234:2::2
!

addother r2 controller r1 v9 9080 - feature bundle vlan evpn mpls
int eth1 eth 0000.0000.2222 $1b$ $1a$
int eth2 eth 0000.0000.2222 $2a$ $2b$
int eth3 eth 0000.0000.2222 $3a$ $3b$
int eth4 eth 0000.0000.2222 $4a$ $4b$
int eth5 eth 0000.0000.2222 $5a$ $5b$
int eth6 eth 0000.0000.2222 $6a$ $6b$
!
!

addrouter r3
int eth1 eth 0000.0000.3333 $3b$ $3a$
!
vrf def v1
 rd 1:1
 label-mode per-prefix
 exit
bridge 1
 rd 1:1
 rt-both 1:1
 mac-learn
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.103 255.255.255.255
 ipv6 addr 4321::103 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int lo1
 vrf for v1
 ipv4 addr 3.3.3.103 255.255.255.255
 ipv6 addr 3333::103 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int bvi1
 vrf for v1
 ipv4 addr 1.1.3.3 255.255.255.0
 ipv6 addr 1234:3::3 ffff:ffff::
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.2 255.255.255.0
 ipv6 addr 1234:1::2 ffff:ffff::
 mpls enable
 mpls ldp4
 mpls ldp6
 exit
router bgp4 1
 vrf v1
 address evpn
 local-as 1
 router-id 4.4.4.3
 neigh 2.2.2.101 remote-as 1
 neigh 2.2.2.101 update lo0
 neigh 2.2.2.101 send-comm both
 neigh 2.2.2.101 pmsi
 afi-evpn 101 bridge 1
 afi-evpn 101 update lo0
 afi-evpn 101 encap cmac
 exit
router bgp6 1
 vrf v1
 address evpn
 local-as 1
 router-id 6.6.6.3
 neigh 4321::101 remote-as 1
 neigh 4321::101 update lo0
 neigh 4321::101 send-comm both
 neigh 4321::101 pmsi
 exit
ipv4 route v1 1.1.2.0 255.255.255.0 1.1.1.1
ipv6 route v1 1234:2:: ffff:ffff:: 1234:1::1
ipv4 route v1 2.2.2.101 255.255.255.255 1.1.1.1
ipv4 route v1 2.2.2.104 255.255.255.255 1.1.1.1
ipv6 route v1 4321::101 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:1::1
ipv6 route v1 4321::104 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:1::1
ipv4 route v1 3.3.3.104 255.255.255.255 1.1.3.4
ipv4 route v1 3.3.3.105 255.255.255.255 1.1.3.5
ipv6 route v1 3333::104 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:3::4
ipv6 route v1 3333::105 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:3::5
ipv6 route v1 4444:1:: ffff:ffff:: 1234:1::1
ipv6 route v1 4444:4:: ffff:ffff:: 1234:1::1
!

addrouter r4
int eth1 eth 0000.0000.4444 $4b$ $4a$
!
vrf def v1
 rd 1:1
 label-mode per-prefix
 exit
bridge 1
 rd 1:1
 rt-both 1:1
 mac-learn
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.104 255.255.255.255
 ipv6 addr 4321::104 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int lo1
 vrf for v1
 ipv4 addr 3.3.3.104 255.255.255.255
 ipv6 addr 3333::104 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int bvi1
 vrf for v1
 ipv4 addr 1.1.3.4 255.255.255.0
 ipv6 addr 1234:3::4 ffff:ffff::
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.2.2 255.255.255.0
 ipv6 addr 1234:2::2 ffff:ffff::
 mpls enable
 mpls ldp4
 mpls ldp6
 exit
router bgp4 1
 vrf v1
 address evpn
 local-as 1
 router-id 4.4.4.4
 neigh 2.2.2.101 remote-as 1
 neigh 2.2.2.101 update lo0
 neigh 2.2.2.101 send-comm both
 neigh 2.2.2.101 pmsi
 afi-evpn 101 bridge 1
 afi-evpn 101 update lo0
 afi-evpn 101 encap cmac
 exit
router bgp6 1
 vrf v1
 address evpn
 local-as 1
 router-id 6.6.6.4
 neigh 4321::101 remote-as 1
 neigh 4321::101 update lo0
 neigh 4321::101 send-comm both
 neigh 4321::101 pmsi
 exit
ipv4 route v1 1.1.1.0 255.255.255.0 1.1.2.1
ipv6 route v1 1234:1:: ffff:ffff:: 1234:2::1
ipv4 route v1 2.2.2.101 255.255.255.255 1.1.2.1
ipv4 route v1 2.2.2.103 255.255.255.255 1.1.2.1
ipv6 route v1 4321::101 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:2::1
ipv6 route v1 4321::103 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:2::1
ipv4 route v1 3.3.3.103 255.255.255.255 1.1.3.3
ipv4 route v1 3.3.3.105 255.255.255.255 1.1.3.5
ipv6 route v1 3333::103 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:3::3
ipv6 route v1 3333::105 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:3::5
ipv6 route v1 4444:1:: ffff:ffff:: 1234:2::1
ipv6 route v1 4444:3:: ffff:ffff:: 1234:2::1
!

addrouter r5
int eth1 eth 0000.0000.5555 $5b$ $5a$
int eth2 eth 0000.0000.6666 $6b$ $6a$
!
vrf def v1
 rd 1:1
 exit
int lo0
 vrf for v1
 ipv4 addr 3.3.3.105 255.255.255.255
 ipv6 addr 3333::105 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
bundle 1
 exit
int eth1
 bundle-gr 1
 exit
int eth2
 bundle-gr 1
 exit
int bun1.111
 vrf for v1
 ipv4 addr 1.1.3.5 255.255.255.0
 ipv6 addr 1234:3::5 ffff:ffff::
 exit
ipv4 route v1 3.3.3.103 255.255.255.255 1.1.3.3
ipv4 route v1 3.3.3.104 255.255.255.255 1.1.3.4
ipv6 route v1 3333::103 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:3::3
ipv6 route v1 3333::104 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 1234:3::4
!


r1 tping 100 10 2.2.2.101 vrf v1 sou lo0
r1 tping 100 10 4321::101 vrf v1 sou lo0
r1 tping 100 10 2.2.2.103 vrf v1 sou lo0
r1 tping 100 10 4321::103 vrf v1 sou lo0
r1 tping 100 10 2.2.2.104 vrf v1 sou lo0
r1 tping 100 10 4321::104 vrf v1 sou lo0

r3 tping 100 10 2.2.2.101 vrf v1 sou lo0
r3 tping 100 10 4321::101 vrf v1 sou lo0
r3 tping 100 10 2.2.2.103 vrf v1 sou lo0
r3 tping 100 10 4321::103 vrf v1 sou lo0
r3 tping 100 10 2.2.2.104 vrf v1 sou lo0
r3 tping 100 10 4321::104 vrf v1 sou lo0

r4 tping 100 10 2.2.2.101 vrf v1 sou lo0
r4 tping 100 10 4321::101 vrf v1 sou lo0
r4 tping 100 10 2.2.2.103 vrf v1 sou lo0
r4 tping 100 10 4321::103 vrf v1 sou lo0
r4 tping 100 10 2.2.2.104 vrf v1 sou lo0
r4 tping 100 10 4321::104 vrf v1 sou lo0

r5 tping 100 10 3.3.3.103 vrf v1 sou lo0
r5 tping 100 10 3333::103 vrf v1 sou lo0
r5 tping 100 10 3.3.3.104 vrf v1 sou lo0
r5 tping 100 10 3333::104 vrf v1 sou lo0
r5 tping 100 10 3.3.3.105 vrf v1 sou lo0
r5 tping 100 10 3333::105 vrf v1 sou lo0

r3 tping 100 10 3.3.3.103 vrf v1 sou lo1
r3 tping 100 10 3333::103 vrf v1 sou lo1
r3 tping 100 10 3.3.3.104 vrf v1 sou lo1
r3 tping 100 10 3333::104 vrf v1 sou lo1
r3 tping 100 10 3.3.3.105 vrf v1 sou lo1
r3 tping 100 10 3333::105 vrf v1 sou lo1

r4 tping 100 10 3.3.3.103 vrf v1 sou lo1
r4 tping 100 10 3333::103 vrf v1 sou lo1
r4 tping 100 10 3.3.3.104 vrf v1 sou lo1
r4 tping 100 10 3333::104 vrf v1 sou lo1
r4 tping 100 10 3.3.3.105 vrf v1 sou lo1
r4 tping 100 10 3333::105 vrf v1 sou lo1

r1 dping sdn . r5 3.3.3.103 vrf v1 sou lo0
r1 dping sdn . r5 3333::103 vrf v1 sou lo0
