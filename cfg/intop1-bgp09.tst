description interop1: bgp addpath

addrouter r1
int eth1 eth 0000.0000.1111 $per1$
!
vrf def v1
 rd 1:1
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.0
 ipv6 addr 1234::1 ffff::
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.1 255.255.255.255
 ipv6 addr 4321::1 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
router bgp4 1
 vrf v1
 address uni
 local-as 1
 router-id 4.4.4.1
 neigh 1.1.1.2 remote-as 1
 neigh 1.1.1.2 additional-path-tx uni
 neigh 1.1.1.2 additional-path-rx uni
 red conn
 exit
router bgp6 1
 vrf v1
 address uni
 local-as 1
 router-id 6.6.6.1
 neigh 1234::2 remote-as 1
 neigh 1234::2 additional-path-tx uni
 neigh 1234::2 additional-path-rx uni
 red conn
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
interface gigabit1
 ip address 1.1.1.2 255.255.255.0
 ipv6 address 1234::2/64
 no shutdown
 exit
router bgp 1
 address-family ipv4 unicast
  bgp additional-paths send receive
  neighbor 1.1.1.1 remote-as 1
  redistribute connected
 address-family ipv6 unicast
  bgp additional-paths send receive
  neighbor 1234::1 remote-as 1
  redistribute connected
 exit
!


r1 tping 100 10 1.1.1.2 vrf v1
r1 tping 100 10 1234::2 vrf v1
r1 tping 100 120 2.2.2.2 vrf v1 sou lo0
r1 tping 100 120 4321::2 vrf v1 sou lo0
