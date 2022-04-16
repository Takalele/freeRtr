description interop1: rip

addrouter r1
int eth1 eth 0000.0000.1111 $per1$
!
vrf def v1
 rd 1:1
 exit
router rip4 1
 vrf v1
 red conn
 exit
router rip6 1
 vrf v1
 red conn
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.0
 ipv6 addr fe80::1 ffff::
 router rip4 1 ena
 router rip6 1 ena
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.1 255.255.255.255
 ipv6 addr 4321::1 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
!

addpersist r2
int eth1 eth 0000.0000.2222 $per1$
!
ip routing
ipv6 unicast-routing
interface loopback0
 ip addr 2.2.2.2 255.255.255.255
 ipv6 addr 4321::2/128
 exit
router rip
 version 2
 redistribute connected
 no auto-summary
 network 1.0.0.0
 exit
ipv6 router rip 1
 redistribute connected
 exit
interface gigabit1
 ip address 1.1.1.2 255.255.255.0
 ipv6 enable
 ipv6 rip 1 enable
 no shutdown
 exit
!


r1 tping 100 10 1.1.1.2 vrf v1
r1 tping 100 120 2.2.2.2 vrf v1 sou lo0
r1 tping 100 120 4321::2 vrf v1 sou lo0
