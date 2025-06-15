description multicast routing with mgre

addrouter r1
int eth1 eth 0000.0000.1111 $1a$ $1b$
!
vrf def v1
 rd 1:1
 exit
vrf def v2
 rd 1:2
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.252
 ipv6 addr 1234:1::1 ffff:ffff::
 ipv4 pim ena
 ipv6 pim ena
 exit
int tun1
 tunnel vrf v1
 tunnel mode mgre
 tunnel source ethernet1
 tunnel domain 1.1.1.5 1.1.1.9
 tunnel destination 232.2.2.2
 vrf for v2
 ipv4 addr 2.2.2.1 255.255.255.0
 no ipv4 resend-packet
 exit
int tun2
 tunnel vrf v1
 tunnel mode mgre
 tunnel source ethernet1
 tunnel domain 1234:2::1 1234:3::1
 tunnel destination ff06::1
 vrf for v2
 ipv6 addr 4321::1 ffff:ffff::
 no ipv6 resend-packet
 exit
ipv4 route v1 0.0.0.0 0.0.0.0 1.1.1.2
ipv6 route v1 :: :: 1234:1::2
ipv4 mroute v1 0.0.0.0 0.0.0.0 1.1.1.2
ipv6 mroute v1 :: :: 1234:1::2
!

addrouter r2
int eth1 eth 0000.0000.2222 $1b$ $1a$
int eth2 eth 0000.0000.2222 $2a$ $2b$
int eth3 eth 0000.0000.2222 $3a$ $3b$
!
vrf def v1
 rd 1:1
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.2 255.255.255.252
 ipv6 addr 1234:1::2 ffff:ffff::
 ipv4 pim ena
 ipv6 pim ena
 exit
int eth2
 vrf for v1
 ipv4 addr 1.1.1.6 255.255.255.252
 ipv6 addr 1234:2::2 ffff:ffff::
 ipv4 pim ena
 ipv6 pim ena
 exit
int eth3
 vrf for v1
 ipv4 addr 1.1.1.10 255.255.255.252
 ipv6 addr 1234:3::2 ffff:ffff::
 ipv4 pim ena
 ipv6 pim ena
 exit
!

addrouter r3
int eth1 eth 0000.0000.3333 $2b$ $2a$
!
vrf def v1
 rd 1:1
 exit
vrf def v2
 rd 1:2
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.5 255.255.255.252
 ipv6 addr 1234:2::1 ffff:ffff::
 ipv4 pim ena
 ipv6 pim ena
 exit
int tun1
 tunnel vrf v1
 tunnel mode mgre
 tunnel source ethernet1
 tunnel domain 1.1.1.1 1.1.1.9
 tunnel destination 232.2.2.2
 vrf for v2
 ipv4 addr 2.2.2.3 255.255.255.0
 no ipv4 resend-packet
 exit
int tun2
 tunnel vrf v1
 tunnel mode mgre
 tunnel source ethernet1
 tunnel domain 1234:1::1 1234:3::1
 tunnel destination ff06::1
 vrf for v2
 ipv6 addr 4321::3 ffff:ffff::
 no ipv6 resend-packet
 exit
ipv4 route v1 0.0.0.0 0.0.0.0 1.1.1.6
ipv6 route v1 :: :: 1234:2::2
ipv4 mroute v1 0.0.0.0 0.0.0.0 1.1.1.6
ipv6 mroute v1 :: :: 1234:2::2
!

addrouter r4
int eth1 eth 0000.0000.4444 $3b$ $3a$
!
vrf def v1
 rd 1:1
 exit
vrf def v2
 rd 1:2
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.9 255.255.255.252
 ipv6 addr 1234:3::1 ffff:ffff::
 ipv4 pim ena
 ipv6 pim ena
 exit
int tun1
 tunnel vrf v1
 tunnel mode mgre
 tunnel source ethernet1
 tunnel domain 1.1.1.1 1.1.1.5
 tunnel destination 232.2.2.2
 vrf for v2
 ipv4 addr 2.2.2.4 255.255.255.0
 no ipv4 resend-packet
 exit
int tun2
 tunnel vrf v1
 tunnel mode mgre
 tunnel source ethernet1
 tunnel domain 1234:1::1 1234:2::1
 tunnel destination ff06::1
 vrf for v2
 ipv6 addr 4321::4 ffff:ffff::
 no ipv6 resend-packet
 exit
ipv4 route v1 0.0.0.0 0.0.0.0 1.1.1.10
ipv6 route v1 :: :: 1234:3::2
ipv4 mroute v1 0.0.0.0 0.0.0.0 1.1.1.10
ipv6 mroute v1 :: :: 1234:3::2
!

r2 tping 100 5 1.1.1.9 vrf v1
r2 tping 100 5 1.1.1.5 vrf v1
r2 tping 100 5 1.1.1.1 vrf v1
r2 tping 100 5 1234:3::1 vrf v1
r2 tping 100 5 1234:2::1 vrf v1
r2 tping 100 5 1234:1::1 vrf v1

r1 tping 100 5 1.1.1.9 vrf v1
r1 tping 100 5 1.1.1.5 vrf v1
r1 tping 100 5 1234:3::1 vrf v1
r1 tping 100 5 1234:2::1 vrf v1

r3 tping 100 5 1.1.1.1 vrf v1
r3 tping 100 5 1234:1::1 vrf v1
r4 tping 100 5 1.1.1.1 vrf v1
r4 tping 100 5 1234:1::1 vrf v1

r1 tping 200 5 232.2.2.2 vrf v1 sou eth1 multi
r1 tping 200 5 ff06::1 vrf v1 sou eth1 multi

r3 tping 200 5 232.2.2.2 vrf v1 sou eth1 multi
r3 tping 200 5 ff06::1 vrf v1 sou eth1 multi

r4 tping 200 5 232.2.2.2 vrf v1 sou eth1 multi
r4 tping 200 5 ff06::1 vrf v1 sou eth1 multi

r1 tping 100 5 2.2.2.3 vrf v2
r1 tping 100 5 4321::3 vrf v2
r1 tping 100 5 2.2.2.4 vrf v2
r1 tping 100 5 4321::4 vrf v2

r3 tping 100 5 2.2.2.1 vrf v2
r3 tping 100 5 4321::1 vrf v2
r3 tping 100 5 2.2.2.4 vrf v2
r3 tping 100 5 4321::4 vrf v2

r4 tping 100 5 2.2.2.1 vrf v2
r4 tping 100 5 4321::1 vrf v2
r4 tping 100 5 2.2.2.3 vrf v2
r4 tping 100 5 4321::3 vrf v2
