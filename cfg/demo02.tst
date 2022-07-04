description addressed demo network

addrouter r1
int eth1 eth 0000.1111.0001 $1a$ $1b$
int eth2 eth 0000.1111.0002 $2a$ $2b$
!
vrf def v1
 rd 1:1
 exit
proxy-profile p1
 vrf v1
 exit
server telnet tel
 security protocol tel
 vrf v1
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.1 255.255.255.255
 ipv6 addr 4321::1 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1
 desc r2 e1
 lldp ena
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.252
 ipv6 addr 1234:1::1 ffff:ffff::
 exit
int eth2
 desc r3 e2
 lldp ena
 vrf for v1
 ipv4 addr 1.1.1.5 255.255.255.252
 ipv6 addr 1234:2::1 ffff:ffff::
 exit
client proxy p1
!

addrouter r2
int eth1 eth 0000.2222.0001 $1b$ $1a$
int eth2 eth 0000.2222.0002 $3a$ $3b$
!
vrf def v1
 rd 1:1
 exit
proxy-profile p1
 vrf v1
 exit
server telnet tel
 security protocol tel
 vrf v1
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.2 255.255.255.255
 ipv6 addr 4321::2 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1
 desc r1 e1
 lldp ena
 vrf for v1
 ipv4 addr 1.1.1.2 255.255.255.252
 ipv6 addr 1234:1::2 ffff:ffff::
 exit
int eth2
 desc r4 e2
 lldp ena
 vrf for v1
 ipv4 addr 1.1.1.9 255.255.255.252
 ipv6 addr 1234:3::1 ffff:ffff::
 exit
client proxy p1
!

addrouter r3
int eth1 eth 0000.3333.0001 $4a$ $4b$
int eth2 eth 0000.3333.0002 $2b$ $2a$
!
vrf def v1
 rd 1:1
 exit
proxy-profile p1
 vrf v1
 exit
server telnet tel
 security protocol tel
 vrf v1
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.3 255.255.255.255
 ipv6 addr 4321::3 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1
 desc r4 e1
 lldp ena
 vrf for v1
 ipv4 addr 1.1.1.13 255.255.255.252
 ipv6 addr 1234:4::1 ffff:ffff::
 exit
int eth2
 desc r1 e2
 lldp ena
 vrf for v1
 ipv4 addr 1.1.1.6 255.255.255.252
 ipv6 addr 1234:2::2 ffff:ffff::
 exit
client proxy p1
!

addrouter r4
int eth1 eth 0000.4444.0001 $4b$ $4a$
int eth2 eth 0000.4444.0002 $3b$ $3a$
!
vrf def v1
 rd 1:1
 exit
proxy-profile p1
 vrf v1
 exit
server telnet tel
 security protocol tel
 vrf v1
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.4 255.255.255.255
 ipv6 addr 4321::4 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1
 desc r3 e1
 lldp ena
 vrf for v1
 ipv4 addr 1.1.1.14 255.255.255.252
 ipv6 addr 1234:4::2 ffff:ffff::
 exit
int eth2
 desc r2 e2
 lldp ena
 vrf for v1
 ipv4 addr 1.1.1.10 255.255.255.252
 ipv6 addr 1234:3::2 ffff:ffff::
 exit
client proxy p1
!

exit


  r1----e1----r2
  |            |
  |            |
  e2          e2
  |            |
  |            |
  r3----e1----r4
