description interop1: ethernet tunneling with l2tp3

addrouter r1
int eth1 eth 0000.0000.1111 $per1$
!
vrf def v1
 rd 1:1
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.252
 exit
bridge 1
 mac-learn
 exit
int bvi1
 vrf for v1
 ipv4 addr 2.2.2.1 255.255.255.0
 ipv6 addr 4321::1 ffff:ffff::
 exit
server l2tp3 l2tp
 bridge 1
 vrf v1
 exit
!

addpersist r2
int eth1 eth 0000.0000.2222 $per1$
int eth2 eth 0000.0000.2211 $per2$
!
ip routing
ipv6 unicast-routing
interface gigabit1
 ip address 1.1.1.2 255.255.255.0
 no shutdown
 exit
vpdn enable
l2tp-class l2tpc
 exit
pseudowire-class l2tp
 encapsulation l2tpv3
 protocol l2tpv3ietf l2tpc
 ip local interface gigabit1
 exit
interface gigabit2
 xconnect 1.1.1.1 1234 pw-class l2tp
 no shutdown
 exit
!

addrouter r3
int eth1 eth 0000.0000.4444 $per2$
!
vrf def v1
 rd 1:1
 exit
int eth1
 vrf for v1
 ipv4 addr 2.2.2.2 255.255.255.0
 ipv6 addr 4321::2 ffff::
 exit
!

r1 tping 100 10 1.1.1.2 vrf v1
r1 tping 100 60 2.2.2.2 vrf v1
r1 tping 100 60 4321::2 vrf v1
r3 tping 100 60 2.2.2.1 vrf v1
r3 tping 100 60 4321::1 vrf v1
