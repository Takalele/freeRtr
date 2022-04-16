description interop2: isis narrow metric

addrouter r1
int eth1 eth 0000.0000.1111 $per1$
int eth2 eth 0000.0000.1112 $per2$
!
vrf def v1
 rd 1:1
 exit
router isis4 1
 vrf v1
 net 48.4444.0000.1111.00
 no metric-wide
 red conn
 exit
router isis6 1
 vrf v1
 net 48.6666.0000.1111.00
 no metric-wide
 multi-topology
 red conn
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.0
 router isis4 1 ena
 exit
int eth2
 vrf for v1
 ipv6 addr fe80::1 ffff::
 router isis6 1 ena
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.1 255.255.255.255
 ipv6 addr 4321::1 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
!

addpersist r2
int eth1 eth 0000.0000.2222 $per1$
int eth2 eth 0000.0000.2223 $per2$
!
interface loopback0
 ipv4 addr 2.2.2.2 255.255.255.255
 ipv6 addr 4321::2/128
 exit
interface gigabit0/0/0/0
 ipv4 address 1.1.1.2 255.255.255.0
 no shutdown
 exit
interface gigabit0/0/0/1
 ipv6 enable
 no shutdown
 exit
router isis 1
 net 48.0000.0000.1234.00
 address-family ipv4 unicast
  redistribute connected
 address-family ipv6 unicast
  redistribute connected
 interface gigabit0/0/0/0
  point-to-point
  address-family ipv4 unicast
 interface gigabit0/0/0/1
  point-to-point
  address-family ipv6 unicast
root
commit
!


r1 tping 100 10 1.1.1.2 vrf v1
r1 tping 100 60 2.2.2.2 vrf v1 sou lo0
r1 tping 100 60 4321::2 vrf v1 sou lo0
