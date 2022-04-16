description interop9: bgp with labels

addrouter r1
int eth1 eth 0000.0000.1111 $per1$
!
vrf def v1
 rd 1:1
 label-mode per-prefix
 exit
access-list test4
 deny 1 any all any all
 permit all any all any all
 exit
access-list test6
 deny 58 4321:: ffff:: all 4321:: ffff:: all
 permit all any all any all
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.0
 ipv6 addr 1234:1::1 ffff:ffff::
 mpls enable
 ipv4 access-group-in test4
 ipv6 access-group-in test6
! ipv4 access-group-out test4
! ipv6 access-group-out test6
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.1 255.255.255.255
 ipv6 addr 4321::1 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
router bgp4 1
 vrf v1
 no safe-ebgp
 address lab
 local-as 1
 router-id 4.4.4.1
 neigh 1.1.1.2 remote-as 2
 red conn
 exit
router bgp6 1
 vrf v1
 no safe-ebgp
 address lab
 local-as 1
 router-id 6.6.6.1
 neigh 1234:1::2 remote-as 2
 red conn
 exit
int pweth1
 vrf for v1
 ipv4 addr 3.3.3.1 255.255.255.252
 pseudo v1 lo0 pweompls 2.2.2.3 1234
 exit
int pweth2
 vrf for v1
 ipv4 addr 3.3.3.5 255.255.255.252
 pseudo v1 lo0 pweompls 4321::3 1234
 exit
!

addpersist r2
int eth1 eth 0000.0000.2222 $per1$
int eth2 eth 0000.0000.2223 $per2$
!
set interfaces ge-0/0/0.0 family inet address 1.1.1.2/24
set interfaces ge-0/0/0.0 family inet6 address 1234:1::2/64
set interfaces ge-0/0/0.0 family mpls
set interfaces ge-0/0/1.0 family inet address 1.1.2.2/24
set interfaces ge-0/0/1.0 family inet6 address 1234:2::2/64
set interfaces ge-0/0/1.0 family mpls
set interfaces lo0.0 family inet address 2.2.2.2/32
set interfaces lo0.0 family inet6 address 4321::2/128
set routing-options autonomous-system 2
set policy-options policy-statement ps1 from protocol direct
set policy-options policy-statement ps1 then accept
set protocols bgp export ps1
set protocols bgp group peer4 type external
set protocols bgp group peer4 family inet labeled-unicast
set protocols bgp group peer4 neighbor 1.1.1.1 peer-as 1
set protocols bgp group peer4 neighbor 1.1.2.1 peer-as 3
set protocols bgp group peer6 type external
set protocols bgp group peer6 family inet6 labeled-unicast
set protocols bgp group peer6 neighbor 1234:1::1 peer-as 1
set protocols bgp group peer6 neighbor 1234:2::1 peer-as 3
commit
!

addrouter r3
int eth1 eth 0000.0000.1131 $per2$
!
vrf def v1
 rd 1:1
 label-mode per-prefix
 exit
access-list test4
 deny 1 any all any all
 permit all any all any all
 exit
access-list test6
 deny 58 4321:: ffff:: all 4321:: ffff:: all
 permit all any all any all
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.2.1 255.255.255.0
 ipv6 addr 1234:2::1 ffff:ffff::
 mpls enable
 ipv4 access-group-in test4
 ipv6 access-group-in test6
! ipv4 access-group-out test4
! ipv6 access-group-out test6
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.3 255.255.255.255
 ipv6 addr 4321::3 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
router bgp4 1
 vrf v1
 no safe-ebgp
 address lab
 local-as 3
 router-id 4.4.4.3
 neigh 1.1.2.2 remote-as 2
 red conn
 exit
router bgp6 1
 vrf v1
 no safe-ebgp
 address lab
 local-as 3
 router-id 6.6.6.3
 neigh 1234:2::2 remote-as 2
 red conn
 exit
int pweth1
 vrf for v1
 ipv4 addr 3.3.3.2 255.255.255.252
 pseudo v1 lo0 pweompls 2.2.2.1 1234
 exit
int pweth2
 vrf for v1
 ipv4 addr 3.3.3.6 255.255.255.252
 pseudo v1 lo0 pweompls 4321::1 1234
 exit
!


r1 tping 0 10 1.1.1.2 vrf v1
r1 tping 100 10 1234:1::2 vrf v1
r3 tping 0 10 1.1.2.2 vrf v1
r3 tping 100 10 1234:2::2 vrf v1

r1 tping 100 60 2.2.2.2 vrf v1 sou lo0
!r1 tping 100 60 4321::2 vrf v1 sou lo0
r3 tping 100 60 2.2.2.2 vrf v1 sou lo0
!r3 tping 100 60 4321::2 vrf v1 sou lo0

r1 tping 100 60 2.2.2.3 vrf v1 sou lo0
r1 tping 100 60 4321::3 vrf v1 sou lo0
r3 tping 100 60 2.2.2.1 vrf v1 sou lo0
r3 tping 100 60 4321::1 vrf v1 sou lo0

r1 tping 100 40 3.3.3.2 vrf v1
r3 tping 100 40 3.3.3.1 vrf v1
r1 tping 100 40 3.3.3.6 vrf v1
r3 tping 100 40 3.3.3.5 vrf v1
