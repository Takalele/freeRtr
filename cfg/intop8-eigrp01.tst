description interop8: eigrp

addrouter r1
int eth1 eth 0000.0000.1111 $1a$ $1b$
!
vrf def v1
 rd 1:1
 exit
router eigrp4 1
 vrf v1
 router 4.4.4.1
 as 1
 red conn
 exit
router eigrp6 1
 vrf v1
 router 6.6.6.1
 as 1
 red conn
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.0
 ipv6 addr fe80::1 ffff::
 router eigrp4 1 ena
 router eigrp6 1 ena
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
router eigrp 1
 network 1.1.1.0/24
 network 2.2.2.0/24
 exit
interface ens3
 ip address 1.1.1.2/24
 no shutdown
 exit
!


r1 tping 100 10 1.1.1.2 vrf v1
r1 tping 100 60 2.2.2.2 vrf v1 sou lo0
!r1 tping 100 60 4321::2 vrf v1 sou lo0
