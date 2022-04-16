description interop8: bgp origin

addrouter r1
int eth1 eth 0000.0000.1111 $1a$ $1b$
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
route-map rm1
 sequence 10 act deny
  match origin 2
 sequence 20 act permit
 exit
router bgp4 1
 vrf v1
 address uni
 local-as 1
 router-id 4.4.4.1
 neigh 1.1.1.2 remote-as 1
 neigh 1.1.1.2 route-map-in rm1
 red conn
 exit
router bgp6 1
 vrf v1
 address uni
 local-as 1
 router-id 6.6.6.1
 neigh 1234::2 remote-as 1
 neigh 1234::2 route-map-in rm1
 red conn
 exit
!

addother r2
int eth1 eth 0000.0000.2222 $1b$ $1a$
!
ip forwarding
ipv6 forwarding
interface lo
 ip addr 2.2.2.2/32
 ip addr 2.2.2.3/32
 ip addr 2.2.2.4/32
 ipv6 addr 4321::2/128
 ipv6 addr 4321::3/128
 ipv6 addr 4321::4/128
 exit
interface ens3
 ip address 1.1.1.2/24
 ipv6 address 1234::2/64
 no shutdown
 exit
ip prefix-list pl1 seq 5 permit 2.2.2.3/32
route-map rm1 permit 10
 match ip address prefix-list pl1
 set origin incomplete
route-map rm1 permit 20
 set origin igp
ipv6 prefix-list pl2 seq 5 permit 4321::3/128
route-map rm2 permit 10
 match ipv6 address prefix-list pl2
 set origin incomplete
route-map rm2 permit 20
 set origin igp
router bgp 1
 neighbor 1.1.1.1 remote-as 1
 neighbor 1234::1 remote-as 1
 address-family ipv4 unicast
  neighbor 1.1.1.1 activate
  no neighbor 1234::1 activate
  redistribute connected route-map rm1
 address-family ipv6 unicast
  no neighbor 1.1.1.1 activate
  neighbor 1234::1 activate
  redistribute connected route-map rm2
 exit
!


r1 tping 100 10 1.1.1.2 vrf v1
r1 tping 100 10 1234::2 vrf v1
r1 tping 100 60 2.2.2.2 vrf v1 sou lo0
r1 tping 100 60 4321::2 vrf v1 sou lo0
r1 tping 0 60 2.2.2.3 vrf v1 sou lo0
r1 tping 0 60 4321::3 vrf v1 sou lo0
r1 tping 100 60 2.2.2.4 vrf v1 sou lo0
r1 tping 100 60 4321::4 vrf v1 sou lo0
