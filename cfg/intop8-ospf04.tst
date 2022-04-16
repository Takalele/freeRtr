description interop8: ospf transit area

addrouter r1
int eth1 eth 0000.0000.1111 $1a$ $1b$
!
vrf def v1
 rd 1:1
 exit
router ospf4 1
 vrf v1
 router 4.4.4.1
 area 1 ena
 no area 1 host
 red conn
 exit
router ospf6 1
 vrf v1
 router 6.6.6.1
 area 1 ena
 no area 1 host
 red conn
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.0
 ipv6 addr fe80::1 ffff::
 router ospf4 1 ena
 router ospf6 1 ena
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.1 255.255.255.255
 ipv6 addr 4321::1 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
!

addother r2
int eth1 eth 0000.0000.2222 $1b$ $1a$
!
ip forwarding
ipv6 forwarding
interface lo
 ip addr 2.2.2.2/32
 ipv6 addr 4321::2/128
 exit
router ospf
 redistribute connected
 exit
router ospf6
 redistribute connected
 interface ens3 area 0.0.0.1
 exit
interface ens3
 ip address 1.1.1.2/24
 ip ospf area 1
 ip ospf network point-to-point
 ipv6 ospf6 network point-to-point
 no shutdown
 exit
!


r1 tping 100 10 1.1.1.2 vrf v1
r1 tping 100 60 2.2.2.2 vrf v1 sou lo0
r1 tping 100 60 4321::2 vrf v1 sou lo0
