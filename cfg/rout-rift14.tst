description rift change in tag

addrouter r1
int eth1 eth 0000.0000.1111 $1a$ $1b$
!
vrf def v1
 rd 1:1
 exit
route-map rm1
 set tag 1000
 exit
router rift4 1
 vrf v1
 router 41
 red conn route-map rm1
 exit
router rift6 1
 vrf v1
 router 61
 red conn route-map rm1
 exit
int lo1
 vrf for v1
 ipv4 addr 2.2.2.1 255.255.255.255
 ipv6 addr 4321::1 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.252
 ipv6 addr 1234:1::1 ffff:ffff::
 router rift4 1 ena
 router rift6 1 ena
 exit
!

addrouter r2
int eth1 eth 0000.0000.2222 $1b$ $1a$
int eth2 eth 0000.0000.2222 $2a$ $2b$
!
vrf def v1
 rd 1:1
 exit
route-map rm1
 sequence 10 act deny
 sequence 10 match tag 2000-4000
 sequence 20 act perm
 exit
router rift4 1
 vrf v1
 router 42
 red conn
 route-map rm1
 exit
router rift6 1
 vrf v1
 router 62
 red conn
 route-map rm1
 exit
int lo1
 vrf for v1
 ipv4 addr 2.2.2.2 255.255.255.255
 ipv6 addr 4321::2 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.2 255.255.255.252
 ipv6 addr 1234:1::2 ffff:ffff::
 router rift4 1 ena
 router rift6 1 ena
 exit
int eth2
 vrf for v1
 ipv4 addr 1.1.1.5 255.255.255.252
 ipv6 addr 1234:2::1 ffff:ffff::
 router rift4 1 ena
 router rift6 1 ena
 exit
!

addrouter r3
int eth1 eth 0000.0000.3333 $2b$ $2a$
!
vrf def v1
 rd 1:1
 exit
router rift4 1
 vrf v1
 router 43
 red conn
 exit
router rift6 1
 vrf v1
 router 63
 red conn
 exit
int lo1
 vrf for v1
 ipv4 addr 2.2.2.3 255.255.255.255
 ipv6 addr 4321::3 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.6 255.255.255.252
 ipv6 addr 1234:2::2 ffff:ffff::
 router rift4 1 ena
 router rift6 1 ena
 exit
!



r1 tping 100 40 2.2.2.2 vrf v1
r1 tping 100 40 2.2.2.3 vrf v1
r1 tping 100 40 4321::2 vrf v1
r1 tping 100 40 4321::3 vrf v1

r2 tping 100 40 2.2.2.1 vrf v1
r2 tping 100 40 2.2.2.3 vrf v1
r2 tping 100 40 4321::1 vrf v1
r2 tping 100 40 4321::3 vrf v1

r3 tping 100 40 2.2.2.1 vrf v1
r3 tping 100 40 2.2.2.2 vrf v1
r3 tping 100 40 4321::1 vrf v1
r3 tping 100 40 4321::2 vrf v1

r1 send conf t
r1 send route-map rm1
r1 send set tag 3000
r1 send end
r1 send clear ipv4 route v1
r1 send clear ipv6 route v1

r1 tping 100 40 2.2.2.2 vrf v1
r1 tping 100 40 2.2.2.3 vrf v1
r1 tping 100 40 4321::2 vrf v1
r1 tping 100 40 4321::3 vrf v1

r2 tping 0 40 2.2.2.1 vrf v1
r2 tping 100 40 2.2.2.3 vrf v1
r2 tping 0 40 4321::1 vrf v1
r2 tping 100 40 4321::3 vrf v1

r3 tping 0 40 2.2.2.1 vrf v1
r3 tping 100 40 2.2.2.2 vrf v1
r3 tping 0 40 4321::1 vrf v1
r3 tping 100 40 4321::2 vrf v1

r1 send conf t
r1 send route-map rm1
r1 send set tag 5000
r1 send end
r1 send clear ipv4 route v1
r1 send clear ipv6 route v1

r1 tping 100 40 2.2.2.2 vrf v1
r1 tping 100 40 2.2.2.3 vrf v1
r1 tping 100 40 4321::2 vrf v1
r1 tping 100 40 4321::3 vrf v1

r2 tping 100 40 2.2.2.1 vrf v1
r2 tping 100 40 2.2.2.3 vrf v1
r2 tping 100 40 4321::1 vrf v1
r2 tping 100 40 4321::3 vrf v1

r3 tping 100 40 2.2.2.1 vrf v1
r3 tping 100 40 2.2.2.2 vrf v1
r3 tping 100 40 4321::1 vrf v1
r3 tping 100 40 4321::2 vrf v1

r2 output show ipv4 rift 1 nei
r2 output show ipv6 rift 1 nei
r2 output show ipv4 rift 1 dat
r2 output show ipv6 rift 1 dat
r2 output show ipv4 rift 1 tre n
r2 output show ipv6 rift 1 tre n
r2 output show ipv4 route v1
r2 output show ipv6 route v1
