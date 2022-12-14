package net.freertr.cfg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrPrefix;
import net.freertr.ip.ipFwd;
import net.freertr.ip.ipRtr;
import net.freertr.ip.ipRtrAdv;
import net.freertr.ip.ipRtrAgr;
import net.freertr.ip.ipRtrInt;
import net.freertr.ip.ipRtrRed;
import net.freertr.rtr.rtrAggreg;
import net.freertr.rtr.rtrBabel;
import net.freertr.rtr.rtrBgp;
import net.freertr.rtr.rtrBlackhole;
import net.freertr.rtr.rtrDeaggr;
import net.freertr.rtr.rtrDownload;
import net.freertr.rtr.rtrEigrp;
import net.freertr.rtr.rtrFlowspec;
import net.freertr.rtr.rtrGhosthunt;
import net.freertr.rtr.rtrIsis;
import net.freertr.rtr.rtrLogger;
import net.freertr.rtr.rtrLsrp;
import net.freertr.rtr.rtrMobile;
import net.freertr.rtr.rtrMsdp;
import net.freertr.rtr.rtrOlsr;
import net.freertr.rtr.rtrOspf4;
import net.freertr.rtr.rtrOspf6;
import net.freertr.rtr.rtrPvrp;
import net.freertr.rtr.rtrRift;
import net.freertr.rtr.rtrRip4;
import net.freertr.rtr.rtrRip6;
import net.freertr.rtr.rtrUni2flow;
import net.freertr.rtr.rtrUni2multi;
import net.freertr.tab.tabGen;
import net.freertr.tab.tabIntUpdater;
import net.freertr.tab.tabRouteAttr;
import net.freertr.user.userFilter;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;

/**
 * router configuration
 *
 * @author matecsaba
 */
public class cfgRtr implements Comparator<cfgRtr>, cfgGeneric {

    /**
     * description of this dialpeer
     */
    public String description = null;

    /**
     * type of routing process
     */
    public tabRouteAttr.routeType type;

    /**
     * number of routing process
     */
    public int number;

    /**
     * vrf of this router
     */
    public cfgVrf vrf;

    /**
     * forwarder of this router
     */
    public ipFwd fwd;

    /**
     * rip handler
     */
    public rtrRip4 rip4;

    /**
     * rip handler
     */
    public rtrRip6 rip6;

    /**
     * babel handler
     */
    public rtrBabel babel;

    /**
     * blackhole handler
     */
    public rtrBlackhole blackhole;

    /**
     * olsr handler
     */
    public rtrOlsr olsr;

    /**
     * ospf handler
     */
    public rtrOspf4 ospf4;

    /**
     * ospf handler
     */
    public rtrOspf6 ospf6;

    /**
     * isis handler
     */
    public rtrIsis isis;

    /**
     * rift handler
     */
    public rtrRift rift;

    /**
     * pvrp handler
     */
    public rtrPvrp pvrp;

    /**
     * lsrp handler
     */
    public rtrLsrp lsrp;

    /**
     * eigrp handler
     */
    public rtrEigrp eigrp;

    /**
     * bgp handler
     */
    public rtrBgp bgp;

    /**
     * msdp handler
     */
    public rtrMsdp msdp;

    /**
     * flowspec handler
     */
    public rtrFlowspec flwspc;

    /**
     * ghost hunter
     */
    public rtrGhosthunt ghosthunt;

    /**
     * unicast to multicast handler
     */
    public rtrUni2multi uni2multi;

    /**
     * unicast to flowspec handler
     */
    public rtrUni2flow uni2flow;

    /**
     * logger handler
     */
    public rtrLogger logger;

    /**
     * downloader handler
     */
    public rtrDownload download;

    /**
     * deaggregate handler
     */
    public rtrDeaggr deaggr;

    /**
     * aggregate handler
     */
    public rtrAggreg aggreg;

    /**
     * mobile handler
     */
    public rtrMobile mobile;

    /**
     * state of this process
     */
    public boolean running;

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        // router *
        "router .*! no automesh",
        "router .*! no description",
        // router rift
        "router rift[46] .*! no suppress-prefix",
        "router rift[46] .*! level 24",
        "router rift[46] .*! distance 100",
        "router rift[46] .*! lifetime 604800000",
        // router pvrp
        "router pvrp[46] .*! no suppress-prefix",
        "router pvrp[46] .*! no labels",
        "router pvrp[46] .*! no stub",
        // router lsrp
        "router lsrp[46] .*! spf-log 0",
        "router lsrp[46] .*! no spf-topolog",
        "router lsrp[46] .*! no spf-bidir",
        "router lsrp[46] .*! no spf-hops",
        "router lsrp[46] .*! no spf-ecmp",
        "router lsrp[46] .*! no database-password",
        "router lsrp[46] .*! distance 70",
        "router lsrp[46] .*! no default-originate",
        "router lsrp[46] .*! no stub",
        "router lsrp[46] .*! no suppress-prefix",
        "router lsrp[46] .*! no prefix-list",
        "router lsrp[46] .*! no route-map",
        "router lsrp[46] .*! no route-policy",
        "router lsrp[46] .*! no segrout",
        "router lsrp[46] .*! no bier",
        "router lsrp[46] .*! refresh 2400000",
        "router lsrp[46] .*! lifetime 3600000",
        // router eigrp
        "router eigrp[46] .*! kvals 1 0 1 0 0",
        "router eigrp[46] .*! stub",
        "router eigrp[46] .*! no suppress-prefix",
        // router babel
        "router babel[46] .*! no suppress-prefix",
        // router olsr
        "router olsr[46] .*! no suppress-prefix",
        // router rip
        "router rip[46] .*! no suppress-prefix",
        // router ospf
        "router ospf[46] .*! distance 110 110 110",
        "router ospf[46] .*! no segrout",
        "router ospf[46] .*! no bier",
        "router ospf[46] .*! area .* spf-log 0",
        "router ospf[46] .*! no area .* spf-topolog",
        "router ospf[46] .*! no area .* spf-bidir",
        "router ospf[46] .*! no area .* spf-hops",
        "router ospf[46] .*! no area .* spf-ecmp",
        "router ospf[46] .*! no area .* max-metric",
        "router ospf[46] .*! no area .* stub",
        "router ospf[46] .*! no area .* nssa",
        "router ospf[46] .*! no area .* traffeng",
        "router ospf[46] .*! no area .* segrout",
        "router ospf[46] .*! no area .* srv6",
        "router ospf[46] .*! no area .* bier",
        "router ospf[46] .*! no area .* suppress-prefix",
        "router ospf4 .*! area .* hostname",
        "router ospf6 .*! no area .* hostname",
        "router ospf[46] .*! no area .* default-originate",
        "router ospf[46] .*! no area .* prefix-list-from",
        "router ospf[46] .*! no area .* prefix-list-into",
        "router ospf[46] .*! no area .* route-map-from",
        "router ospf[46] .*! no area .* route-map-into",
        "router ospf[46] .*! no area .* route-policy-from",
        "router ospf[46] .*! no area .* route-policy-into",
        // router isis
        "router isis[46] .*! max-area-addrs 3",
        "router isis[46] .*! distance 115 115",
        "router isis[46] .*! no afi-other enable",
        "router isis[46] .*! afi-other distance 115 115",
        "router isis[46] .*! metric-wide",
        "router isis[46] .*! no multi-topology",
        "router isis[46] .*! no segrout",
        "router isis[46] .*! no bier",
        "router isis[46] .*! level[12] spf-log 0",
        "router isis[46] .*! no level[12] spf-topolog",
        "router isis[46] .*! no level[12] spf-bidir",
        "router isis[46] .*! no level[12] spf-hops",
        "router isis[46] .*! no level[12] spf-ecmp",
        "router isis[46] .*! level2 clear-attached",
        "router isis[46] .*! no level2 allow-attached",
        "router isis[46] .*! no level1 clear-attached",
        "router isis[46] .*! level1 allow-attached",
        "router isis[46] .*! no level[12] set-overload",
        "router isis[46] .*! no level[12] set-attached",
        "router isis[46] .*! no level[12] traffeng",
        "router isis[46] .*! no level[12] segrout",
        "router isis[46] .*! no level[12] srv6",
        "router isis[46] .*! no level[12] bier",
        "router isis[46] .*! no level[12] suppress-prefix",
        "router isis[46] .*! no level[12] other-suppress-prefix",
        "router isis[46] .*! level[12] hostname",
        "router isis[46] .*! level[12] inter-level",
        "router isis[46] .*! no level[12] default-originate",
        "router isis[46] .*! no level[12] other-default-originate",
        "router isis[46] .*! level[12] lsp-mtu 1024",
        "router isis[46] .*! no level[12] lsp-password",
        "router isis[46] .*! level[12] authen-type clear",
        "router isis[46] .*! level[12] lsp-refresh 400000",
        "router isis[46] .*! level[12] lsp-lifetime 1200000",
        "router isis[46] .*! no level[12] prefix-list-from",
        "router isis[46] .*! no level[12] prefix-list-into",
        "router isis[46] .*! no level[12] route-map-from",
        "router isis[46] .*! no level[12] route-map-into",
        "router isis[46] .*! no level[12] route-policy-from",
        "router isis[46] .*! no level[12] route-policy-into",
        "router isis[46] .*! no level[12] other-prefix-list-from",
        "router isis[46] .*! no level[12] other-prefix-list-into",
        "router isis[46] .*! no level[12] other-route-map-from",
        "router isis[46] .*! no level[12] other-route-map-into",
        "router isis[46] .*! no level[12] other-route-policy-from",
        "router isis[46] .*! no level[12] other-route-policy-into",
        // router bgp
        "router bgp[46] .*! distance 20 200 200",
        "router bgp[46] .*! scantime 1000",
        "router bgp[46] .*! scandelay 1000",
        "router bgp[46] .*! nexthop recursion 1",
        "router bgp[46] .*! incremental 1000",
        "router bgp[46] .*! no conquer",
        "router bgp[46] .*! safe-ebgp",
        "router bgp[46] .*! no flapstat",
        "router bgp[46] .*! no flowspec-advert",
        "router bgp[46] .*! no flowspec-install",
        "router bgp[46] .*! no segrout",
        "router bgp[46] .*! no bier",
        "router bgp[46] .*! graceful-restart 60000",
        "router bgp[46] .*! no nexthop route-map",
        "router bgp[46] .*! no nexthop route-policy",
        "router bgp[46] .*! no nexthop prefix-list",
        "router bgp[46] .*! no (template|neighbor) .* template",
        "router bgp[46] .*! no (template|neighbor) .* description",
        "router bgp[46] .*! (template|neighbor) .* dmz-link-bw -1",
        "router bgp[46] .*! (template|neighbor) .* timer 60000 180000",
        "router bgp[46] .*! (template|neighbor) .* connection-mode both",
        "router bgp[46] .*! (template|neighbor) .* buffer-size 65536",
        "router bgp[46] .*! (template|neighbor) .* ttl-security -1",
        "router bgp[46] .*! (template|neighbor) .* tos-value -1",
        "router bgp[46] .*! (template|neighbor) .* role disabled",
        "router bgp[46] .*! (template|neighbor) .* additional-path-rx",
        "router bgp[46] .*! (template|neighbor) .* additional-path-tx",
        "router bgp[46] .*! (template|neighbor) .* authen-type md5",
        "router bgp[46] .*! no (template|neighbor) .* ungroup-remoteas",
        "router bgp[46] .*! no (template|neighbor) .* password",
        "router bgp[46] .*! no (template|neighbor) .* shutdown",
        "router bgp[46] .*! no (template|neighbor) .* update-source",
        "router bgp[46] .*! no (template|neighbor) .* compression",
        "router bgp[46] .*! no (template|neighbor) .* bfd",
        "router bgp[46] .*! no (template|neighbor) .* backup-peer",
        "router bgp[46] .*! no (template|neighbor) .* proxy-profile",
        "router bgp[46] .*! no (template|neighbor) .* fall-over",
        "router bgp[46] .*! no (template|neighbor) .* hostname",
        "router bgp[46] .*! no (template|neighbor) .* unidirection",
        "router bgp[46] .*! no (template|neighbor) .* extended-open",
        "router bgp[46] .*! no (template|neighbor) .* extended-update",
        "router bgp[46] .*! (template|neighbor) .* graceful-restart",
        "router bgp[46] .*! (template|neighbor) .* multiple-labels",
        "router bgp[46] .*! (template|neighbor) .* extended-nexthop-current",
        "router bgp[46] .*! (template|neighbor) .* extended-nexthop-other",
        "router bgp[46] .*! (template|neighbor) .* capability-negotiation",
        "router bgp[46] .*! (template|neighbor) .* track-next-hop",
        "router bgp[46] .*! (template|neighbor) .* advertisement-interval-rx 0",
        "router bgp[46] .*! (template|neighbor) .* advertisement-interval-tx 0",
        "router bgp[46] .*! no (template|neighbor) .* aigp",
        "router bgp[46] .*! no (template|neighbor) .* traffeng",
        "router bgp[46] .*! no (template|neighbor) .* pmsitun",
        "router bgp[46] .*! no (template|neighbor) .* tunenc",
        "router bgp[46] .*! no (template|neighbor) .* linkstate",
        "router bgp[46] .*! no (template|neighbor) .* attribset",
        "router bgp[46] .*! no (template|neighbor) .* segrout",
        "router bgp[46] .*! no (template|neighbor) .* bier",
        "router bgp[46] .*! no (template|neighbor) .* egress-engineering",
        "router bgp[46] .*! no (template|neighbor) .* monitor",
        "router bgp[46] .*! no (template|neighbor) .* dump",
        "router bgp[46] .*! no (template|neighbor) .* default-originate",
        "router bgp[46] .*! no (template|neighbor) .* other-default-originate",
        "router bgp[46] .*! no (template|neighbor) .* internal-vpn-client",
        "router bgp[46] .*! no (template|neighbor) .* allow-as-in",
        "router bgp[46] .*! (template|neighbor) .* allow-as-out",
        "router bgp[46] .*! no (template|neighbor) .* route-target-filter-in",
        "router bgp[46] .*! no (template|neighbor) .* route-target-filter-out",
        "router bgp[46] .*! no (template|neighbor) .* enforce-first-as",
        "router bgp[46] .*! no (template|neighbor) .* route-server-client",
        "router bgp[46] .*! no (template|neighbor) .* soft-reconfiguration",
        "router bgp[46] .*! no (template|neighbor) .* remove-private-as-out",
        "router bgp[46] .*! no (template|neighbor) .* remove-private-as-in",
        "router bgp[46] .*! no (template|neighbor) .* override-peer-as-out",
        "router bgp[46] .*! no (template|neighbor) .* override-peer-as-in",
        "router bgp[46] .*! no (template|neighbor) .* route-reflector-client",
        "router bgp[46] .*! no (template|neighbor) .* confederation-peer",
        "router bgp[46] .*! no (template|neighbor) .* next-hop-unchanged",
        "router bgp[46] .*! no (template|neighbor) .* next-hop-self",
        "router bgp[46] .*! no (template|neighbor) .* next-hop-peer",
        "router bgp[46] .*! no (template|neighbor) .* send-community",
        "router bgp[46] .*! no (template|neighbor) .* label-pop",
        "router bgp[46] .*! no (template|neighbor) .* maximum-prefix-in",
        "router bgp[46] .*! no (template|neighbor) .* maximum-prefix-out",
        "router bgp[46] .*! no (template|neighbor) .* attribute-filter",
        "router bgp[46] .*! no (template|neighbor) .* unknowns-in",
        "router bgp[46] .*! no (template|neighbor) .* unknowns-out",
        "router bgp[46] .*! no (template|neighbor) .* dampening",
        "router bgp[46] .*! no (template|neighbor) .* prefix-list-in",
        "router bgp[46] .*! no (template|neighbor) .* prefix-list-out",
        "router bgp[46] .*! no (template|neighbor) .* route-map-in",
        "router bgp[46] .*! no (template|neighbor) .* route-map-out",
        "router bgp[46] .*! no (template|neighbor) .* route-policy-in",
        "router bgp[46] .*! no (template|neighbor) .* route-policy-out",
        "router bgp[46] .*! no (template|neighbor) .* other-prefix-list-in",
        "router bgp[46] .*! no (template|neighbor) .* other-prefix-list-out",
        "router bgp[46] .*! no (template|neighbor) .* other-route-map-in",
        "router bgp[46] .*! no (template|neighbor) .* other-route-map-out",
        "router bgp[46] .*! no (template|neighbor) .* other-route-policy-in",
        "router bgp[46] .*! no (template|neighbor) .* other-route-policy-out",
        "router bgp[46] .*! no (template|neighbor) .* other-address",
        "router bgp[46] .*! no (template|neighbor) .* vpn-route-map-in",
        "router bgp[46] .*! no (template|neighbor) .* vpn-route-map-out",
        "router bgp[46] .*! no (template|neighbor) .* vpn-route-policy-in",
        "router bgp[46] .*! no (template|neighbor) .* vpn-route-policy-out",
        "router bgp[46] .*! no (template|neighbor) .* ovpn-route-map-in",
        "router bgp[46] .*! no (template|neighbor) .* ovpn-route-map-out",
        "router bgp[46] .*! no (template|neighbor) .* ovpn-route-policy-in",
        "router bgp[46] .*! no (template|neighbor) .* ovpn-route-policy-out",
        "router bgp[46] .*! no afi-other enable",
        "router bgp[46] .*! afi-other vpn-mode",
        "router bgp[46] .*! afi-other distance -1",
        "router bgp[46] .*! afi-vrf .* distance -1",
        "router bgp[46] .*! no afi-vrf .* default-originate",
        "router bgp[46] .*! no afi-vrf .* flowspec-advert",
        "router bgp[46] .*! no afi-vrf .* flowspec-install",
        "router bgp[46] .*! no afi-vrf .* mvpn",
        "router bgp[46] .*! afi-ovrf .* distance -1",
        "router bgp[46] .*! no afi-ovrf .* default-originate",
        "router bgp[46] .*! no afi-ovrf .* flowspec-advert",
        "router bgp[46] .*! no afi-ovrf .* flowspec-install",
        "router bgp[46] .*! no afi-ovrf .* mvpn",
        "router bgp[46] .*! afi-vpls .* ve-id 0 0",
        // router msdp
        "router msdp[46] .*! neighbor .* timer 30000 75000 60000 120000",
        "router msdp[46] .*! no neighbor .* description",
        "router msdp[46] .*! no neighbor .* shutdown",
        "router msdp[46] .*! no neighbor .* update-source",
        "router msdp[46] .*! no neighbor .* password",
        "router msdp[46] .*! no neighbor .* bfd",
        // router flowspec
        "router flowspec[46] .*! distance 254",
        // router ghosthunt
        "router ghosthunt[46] .*! distance 10",
        "router ghosthunt[46] .*! afi unicast",
        "router ghosthunt[46] .*! grace 0 0",
        "router ghosthunt[46] .*! mode observer",
        "router ghosthunt[46] .*! lookup vrf",
        "router ghosthunt[46] .*! no send-map",
        "router ghosthunt[46] .*! no send-policy",
        "router ghosthunt[46] .*! no recv-map",
        "router ghosthunt[46] .*! no recv-policy",
        "router ghosthunt[46] .*! no ignore",
        "router ghosthunt[46] .*! no logging",
        // router uni2multi
        "router uni2multi[46] .*! distance 254",
        // router deaggr
        "router deaggr[46] .*! afi unicast",
        "router deaggr[46] .*! distance 254 254",
        "router deaggr[46] .*! nexthop :: ::",
        // router aggreg
        "router aggreg[46] .*! afi unicast",
        "router aggreg[46] .*! distance 254",
        "router aggreg[46] .*! nexthop ::",
        "router aggreg[46] .*! netmask 0",
        // router uni2flow
        "router uni2flow[46] .*! distance 254",
        "router uni2flow[46] .*! direction target",
        "router uni2flow[46] .*! as 0",
        "router uni2flow[46] .*! rate 0",
        // router download
        "router download[46] .*! no hidden",
        "router download[46] .*! no log",
        "router download[46] .*! url ",
        "router download[46] .*! no range",
        "router download[46] .*! respawn",
        "router download[46] .*! random-time 0",
        "router download[46] .*! random-delay 0",
        "router download[46] .*! delay 0",
        "router download[46] .*! time 0",
        // router blackhole
        "router blackhole[46] .*! no negate",
        "router blackhole[46] .*! penalty 60000",
        "router blackhole[46] .*! distance 254",
        "router blackhole[46] .*! no whitelist",
        // router mobile
        "router mobile[46] .*! distance 254",
        // router logger
        "router logger[46] .*! afi unicast",
        "router logger[46] .*! no flapstat",
        "router logger[46] .*! no logging",};

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    /**
     * convert name to number
     *
     * @param a name to convert
     * @return value, negative on error
     */
    public static tabRouteAttr.routeType name2num(String a) {
        if (a.equals("rip4")) {
            return tabRouteAttr.routeType.rip4;
        }
        if (a.equals("rip6")) {
            return tabRouteAttr.routeType.rip6;
        }
        if (a.equals("babel4")) {
            return tabRouteAttr.routeType.babel4;
        }
        if (a.equals("babel6")) {
            return tabRouteAttr.routeType.babel6;
        }
        if (a.equals("blackhole4")) {
            return tabRouteAttr.routeType.blackhole4;
        }
        if (a.equals("blackhole6")) {
            return tabRouteAttr.routeType.blackhole6;
        }
        if (a.equals("olsr4")) {
            return tabRouteAttr.routeType.olsr4;
        }
        if (a.equals("olsr6")) {
            return tabRouteAttr.routeType.olsr6;
        }
        if (a.equals("ospf4")) {
            return tabRouteAttr.routeType.ospf4;
        }
        if (a.equals("ospf6")) {
            return tabRouteAttr.routeType.ospf6;
        }
        if (a.equals("isis4")) {
            return tabRouteAttr.routeType.isis4;
        }
        if (a.equals("isis6")) {
            return tabRouteAttr.routeType.isis6;
        }
        if (a.equals("rift4")) {
            return tabRouteAttr.routeType.rift4;
        }
        if (a.equals("rift6")) {
            return tabRouteAttr.routeType.rift6;
        }
        if (a.equals("pvrp4")) {
            return tabRouteAttr.routeType.pvrp4;
        }
        if (a.equals("pvrp6")) {
            return tabRouteAttr.routeType.pvrp6;
        }
        if (a.equals("lsrp4")) {
            return tabRouteAttr.routeType.lsrp4;
        }
        if (a.equals("lsrp6")) {
            return tabRouteAttr.routeType.lsrp6;
        }
        if (a.equals("eigrp4")) {
            return tabRouteAttr.routeType.eigrp4;
        }
        if (a.equals("eigrp6")) {
            return tabRouteAttr.routeType.eigrp6;
        }
        if (a.equals("bgp4")) {
            return tabRouteAttr.routeType.bgp4;
        }
        if (a.equals("bgp6")) {
            return tabRouteAttr.routeType.bgp6;
        }
        if (a.equals("static")) {
            return tabRouteAttr.routeType.staticRoute;
        }
        if (a.equals("defgw")) {
            return tabRouteAttr.routeType.defpref;
        }
        if (a.equals("local")) {
            return tabRouteAttr.routeType.local;
        }
        if (a.equals("remote")) {
            return tabRouteAttr.routeType.remote;
        }
        if (a.equals("connected")) {
            return tabRouteAttr.routeType.conn;
        }
        if (a.equals("msdp4")) {
            return tabRouteAttr.routeType.msdp4;
        }
        if (a.equals("msdp6")) {
            return tabRouteAttr.routeType.msdp6;
        }
        if (a.equals("flowspec4")) {
            return tabRouteAttr.routeType.flwspc4;
        }
        if (a.equals("flowspec6")) {
            return tabRouteAttr.routeType.flwspc6;
        }
        if (a.equals("ghosthunt4")) {
            return tabRouteAttr.routeType.ghosthunt4;
        }
        if (a.equals("ghosthunt6")) {
            return tabRouteAttr.routeType.ghosthunt6;
        }
        if (a.equals("uni2multi4")) {
            return tabRouteAttr.routeType.uni2multi4;
        }
        if (a.equals("uni2multi6")) {
            return tabRouteAttr.routeType.uni2multi6;
        }
        if (a.equals("uni2flow4")) {
            return tabRouteAttr.routeType.uni2flow4;
        }
        if (a.equals("uni2flow6")) {
            return tabRouteAttr.routeType.uni2flow6;
        }
        if (a.equals("logger4")) {
            return tabRouteAttr.routeType.logger4;
        }
        if (a.equals("logger6")) {
            return tabRouteAttr.routeType.logger6;
        }
        if (a.equals("download4")) {
            return tabRouteAttr.routeType.download4;
        }
        if (a.equals("download6")) {
            return tabRouteAttr.routeType.download6;
        }
        if (a.equals("deaggr4")) {
            return tabRouteAttr.routeType.deaggr4;
        }
        if (a.equals("deaggr6")) {
            return tabRouteAttr.routeType.deaggr6;
        }
        if (a.equals("aggreg4")) {
            return tabRouteAttr.routeType.aggreg4;
        }
        if (a.equals("aggreg6")) {
            return tabRouteAttr.routeType.aggreg6;
        }
        if (a.equals("mobile4")) {
            return tabRouteAttr.routeType.mobile4;
        }
        if (a.equals("mobile6")) {
            return tabRouteAttr.routeType.mobile6;
        }
        return null;
    }

    /**
     * convert number to name
     *
     * @param i number to convert
     * @return name of protocol
     */
    public static String num2name(tabRouteAttr.routeType i) {
        switch (i) {
            case rip4:
                return "rip4";
            case rip6:
                return "rip6";
            case babel4:
                return "babel4";
            case babel6:
                return "babel6";
            case blackhole4:
                return "blackhole4";
            case blackhole6:
                return "blackhole6";
            case olsr4:
                return "olsr4";
            case olsr6:
                return "olsr6";
            case ospf4:
                return "ospf4";
            case ospf6:
                return "ospf6";
            case isis4:
                return "isis4";
            case isis6:
                return "isis6";
            case rift4:
                return "rift4";
            case rift6:
                return "rift6";
            case pvrp4:
                return "pvrp4";
            case pvrp6:
                return "pvrp6";
            case lsrp4:
                return "lsrp4";
            case lsrp6:
                return "lsrp6";
            case eigrp4:
                return "eigrp4";
            case eigrp6:
                return "eigrp6";
            case bgp4:
                return "bgp4";
            case bgp6:
                return "bgp6";
            case msdp4:
                return "msdp4";
            case msdp6:
                return "msdp6";
            case flwspc4:
                return "flowspec4";
            case flwspc6:
                return "flowspec6";
            case ghosthunt4:
                return "ghosthunt4";
            case ghosthunt6:
                return "ghosthunt6";
            case uni2multi4:
                return "uni2multi4";
            case uni2multi6:
                return "uni2multi6";
            case uni2flow4:
                return "uni2flow4";
            case uni2flow6:
                return "uni2flow6";
            case logger4:
                return "logger4";
            case logger6:
                return "logger6";
            case download4:
                return "download4";
            case download6:
                return "download6";
            case deaggr4:
                return "deaggr4";
            case deaggr6:
                return "deaggr6";
            case aggreg4:
                return "aggreg4";
            case aggreg6:
                return "aggreg6";
            case mobile4:
                return "mobile4";
            case mobile6:
                return "mobile6";
            case staticRoute:
                return "static";
            case defpref:
                return "defgw";
            case local:
                return "local";
            case remote:
                return "remote";
            case conn:
                return "connected";
            default:
                return "";
        }
    }

    /**
     * test if need process number
     *
     * @param i router type
     * @return true if needed, false if not
     */
    public static boolean num2proc(tabRouteAttr.routeType i) {
        switch (i) {
            case staticRoute:
            case conn:
            case defpref:
            case local:
            case remote:
                return false;
            default:
                return true;
        }
    }

    /**
     * get redistribution config
     *
     * @param lst list to update
     * @param beg beginning
     * @param rtr router to use
     */
    public static void getShRedist(List<String> lst, String beg, ipRtr rtr) {
        for (int i = 0; i < rtr.routerRedisting.size(); i++) {
            ipRtrRed ntry = rtr.routerRedisting.get(i);
            if (ntry == null) {
                continue;
            }
            lst.add(beg + "redistribute " + redist2str(ntry));
        }
        for (int i = 0; i < rtr.routerAdverting.size(); i++) {
            ipRtrAdv ntry = rtr.routerAdverting.get(i);
            if (ntry == null) {
                continue;
            }
            lst.add(beg + "advertise " + advert2str(ntry));
        }
        for (int i = 0; i < rtr.routerReadvrtng.size(); i++) {
            ipRtrAdv ntry = rtr.routerReadvrtng.get(i);
            if (ntry == null) {
                continue;
            }
            lst.add(beg + "readvertise " + advert2str(ntry));
        }
        for (int i = 0; i < rtr.routerAdvInter.size(); i++) {
            ipRtrInt ntry = rtr.routerAdvInter.get(i);
            if (ntry == null) {
                continue;
            }
            lst.add(beg + "justadvert " + advint2str(ntry));
        }
        for (int i = 0; i < rtr.routerAggregating.size(); i++) {
            ipRtrAgr ntry = rtr.routerAggregating.get(i);
            if (ntry == null) {
                continue;
            }
            lst.add(beg + "aggregate " + aggreg2str(ntry));
        }
        if (rtr.routerEcmp) {
            lst.add(beg + "ecmp");
        }
    }

    /**
     * do redistribution config
     *
     * @param rtr router to update
     * @param fwd forwarder to update
     * @param neg negated
     * @param a command word
     * @param cmd command to parse
     * @return false on success, true on error
     */
    public static boolean doCfgRedist(ipRtr rtr, ipFwd fwd, boolean neg, String a, cmds cmd) {
        if (a.equals("ecmp")) {
            rtr.routerEcmp = !neg;
            fwd.routerConfigChg();
            return false;
        }
        if (a.equals("redistribute")) {
            ipRtrRed ntry = str2redist(cmd);
            if (ntry == null) {
                return true;
            }
            if (neg) {
                rtr.routerRedisting.del(ntry);
            } else {
                rtr.routerRedisting.put(ntry);
            }
            fwd.routerConfigChg();
            return false;
        }
        if (a.equals("justadvert")) {
            ipRtrInt ntry = str2advint(cmd);
            if (ntry == null) {
                return true;
            }
            try {
                if (neg) {
                    rtr.routerAdvInter.del(ntry);
                } else {
                    rtr.routerAdvInter.put(ntry);
                }
            } catch (Exception e) {
                cmd.error("no such iface");
            }
            fwd.routerConfigChg();
            return false;
        }
        if (a.equals("advertise")) {
            ipRtrAdv ntry = str2advert(cmd);
            if (ntry == null) {
                return true;
            }
            if (neg) {
                rtr.routerAdverting.del(ntry);
            } else {
                rtr.routerAdverting.put(ntry);
            }
            fwd.routerConfigChg();
            return false;
        }
        if (a.equals("readvertise")) {
            ipRtrAdv ntry = str2advert(cmd);
            if (ntry == null) {
                return true;
            }
            if (neg) {
                rtr.routerReadvrtng.del(ntry);
            } else {
                rtr.routerReadvrtng.put(ntry);
            }
            fwd.routerConfigChg();
            return false;
        }
        if (a.equals("aggregate")) {
            ipRtrAgr ntry = str2aggreg(cmd);
            if (ntry == null) {
                return true;
            }
            if (neg) {
                rtr.routerAggregating.del(ntry);
            } else {
                rtr.routerAggregating.put(ntry);
            }
            fwd.routerConfigChg();
            return false;
        }
        return true;
    }

    /**
     * convert redistribution to string
     *
     * @param red redistribution
     * @return string
     */
    public static String redist2str(ipRtrRed red) {
        String a = num2name(red.typ);
        if (num2proc(red.typ)) {
            a += " " + red.num;
        }
        if (red.limit > 0) {
            a += " limit " + red.limit;
        }
        if (red.metric != null) {
            a += " metric " + red.metric;
        }
        if (red.tag != null) {
            a += " tag " + red.tag;
        }
        if (red.prflst != null) {
            a += " prefix-list " + red.prflst.listName;
        }
        if (red.roumap != null) {
            a += " route-map " + red.roumap.listName;
        }
        if (red.rouplc != null) {
            a += " route-policy " + red.rouplc.listName;
        }
        if (red.ecmp) {
            a += " ecmp";
        }
        return a;
    }

    /**
     * convert advertisement to string
     *
     * @param adv advertisement
     * @return string
     */
    public static String advert2str(ipRtrAdv adv) {
        String a = "" + addrPrefix.ip2str(adv.prefix);
        if (adv.metric != null) {
            a += " metric " + adv.metric;
        }
        if (adv.tag != null) {
            a += " tag " + adv.tag;
        }
        if (adv.roumap != null) {
            a += " route-map " + adv.roumap.listName;
        }
        if (adv.rouplc != null) {
            a += " route-policy " + adv.rouplc.listName;
        }
        if (adv.ecmp) {
            a += " ecmp";
        }
        return a;
    }

    /**
     * convert aggregating to string
     *
     * @param agr aggregating
     * @return string
     */
    public static String aggreg2str(ipRtrAgr agr) {
        String a = "" + addrPrefix.ip2str(agr.prefix);
        if (agr.metric != null) {
            a += " metric " + agr.metric;
        }
        if (agr.tag != null) {
            a += " tag " + agr.tag;
        }
        if (agr.prflst != null) {
            a += " prefix-list " + agr.prflst.listName;
        }
        if (agr.roumap != null) {
            a += " route-map " + agr.roumap.listName;
        }
        if (agr.rouplc != null) {
            a += " route-policy " + agr.rouplc.listName;
        }
        if (agr.aspath) {
            a += " as-set";
        }
        if (agr.summary) {
            a += " summary-only";
        }
        return a;
    }

    /**
     * convert interface to string
     *
     * @param ifc interface
     * @return string
     */
    public static String advint2str(ipRtrInt ifc) {
        String a = "" + ifc.iface.name;
        if (ifc.metric != null) {
            a += " metric " + ifc.metric;
        }
        if (ifc.tag != null) {
            a += " tag " + ifc.tag;
        }
        if (ifc.roumap != null) {
            a += " route-map " + ifc.roumap.listName;
        }
        if (ifc.rouplc != null) {
            a += " route-policy " + ifc.rouplc.listName;
        }
        return a;
    }

    /**
     * parse up redistribution
     *
     * @param cmd command to read
     * @return redistribution
     */
    public static ipRtrRed str2redist(cmds cmd) {
        tabRouteAttr.routeType i = name2num(cmd.word());
        if (i == null) {
            return null;
        }
        int o = -1;
        if (num2proc(i)) {
            o = bits.str2num(cmd.word());
        }
        ipRtrRed red = new ipRtrRed(i, o);
        for (;;) {
            String s = cmd.word();
            if (s.length() < 1) {
                break;
            }
            if (s.equals("ecmp")) {
                red.ecmp = true;
                continue;
            }
            if (s.equals("limit")) {
                red.limit = bits.str2num(cmd.word());
                continue;
            }
            if (s.equals("metric")) {
                red.metric = new tabIntUpdater();
                s = cmd.word();
                if (red.metric.fromString(s + " " + cmd.word())) {
                    return null;
                }
                continue;
            }
            if (s.equals("tag")) {
                red.tag = new tabIntUpdater();
                s = cmd.word();
                if (red.tag.fromString(s + " " + cmd.word())) {
                    return null;
                }
                continue;
            }
            if (s.equals("prefix-list")) {
                cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
                if (ntry == null) {
                    return null;
                }
                red.prflst = ntry.prflst;
                continue;
            }
            if (s.equals("route-map")) {
                cfgRoump ntry = cfgAll.rtmpFind(cmd.word(), false);
                if (ntry == null) {
                    return null;
                }
                red.roumap = ntry.roumap;
                continue;
            }
            if (s.equals("route-policy")) {
                cfgRouplc ntry = cfgAll.rtplFind(cmd.word(), false);
                if (ntry == null) {
                    return null;
                }
                red.rouplc = ntry.rouplc;
                continue;
            }
            return null;
        }
        return red;
    }

    /**
     * parse up advertisement
     *
     * @param cmd command to read
     * @return advertisement
     */
    public static ipRtrAdv str2advert(cmds cmd) {
        addrPrefix<addrIP> prf = addrPrefix.str2ip(cmd.word());
        if (prf == null) {
            return null;
        }
        ipRtrAdv adv = new ipRtrAdv(prf);
        for (;;) {
            String s = cmd.word();
            if (s.length() < 1) {
                break;
            }
            if (s.equals("metric")) {
                adv.metric = new tabIntUpdater();
                s = cmd.word();
                if (adv.metric.fromString(s + " " + cmd.word())) {
                    return null;
                }
                continue;
            }
            if (s.equals("tag")) {
                adv.tag = new tabIntUpdater();
                s = cmd.word();
                if (adv.tag.fromString(s + " " + cmd.word())) {
                    return null;
                }
                continue;
            }
            if (s.equals("ecmp")) {
                adv.ecmp = true;
                continue;
            }
            if (s.equals("route-map")) {
                cfgRoump ntry = cfgAll.rtmpFind(cmd.word(), false);
                if (ntry == null) {
                    return null;
                }
                adv.roumap = ntry.roumap;
                continue;
            }
            if (s.equals("route-policy")) {
                cfgRouplc ntry = cfgAll.rtplFind(cmd.word(), false);
                if (ntry == null) {
                    return null;
                }
                adv.rouplc = ntry.rouplc;
                continue;
            }
            return null;
        }
        return adv;
    }

    /**
     * parse up aggregation
     *
     * @param cmd command to read
     * @return aggregation
     */
    public static ipRtrAgr str2aggreg(cmds cmd) {
        addrPrefix<addrIP> prf = addrPrefix.str2ip(cmd.word());
        if (prf == null) {
            return null;
        }
        ipRtrAgr agr = new ipRtrAgr(prf);
        for (;;) {
            String s = cmd.word();
            if (s.length() < 1) {
                break;
            }
            if (s.equals("metric")) {
                agr.metric = new tabIntUpdater();
                s = cmd.word();
                if (agr.metric.fromString(s + " " + cmd.word())) {
                    return null;
                }
                continue;
            }
            if (s.equals("tag")) {
                agr.tag = new tabIntUpdater();
                s = cmd.word();
                if (agr.tag.fromString(s + " " + cmd.word())) {
                    return null;
                }
                continue;
            }
            if (s.equals("prefix-list")) {
                cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
                if (ntry == null) {
                    return null;
                }
                agr.prflst = ntry.prflst;
                continue;
            }
            if (s.equals("route-map")) {
                cfgRoump ntry = cfgAll.rtmpFind(cmd.word(), false);
                if (ntry == null) {
                    return null;
                }
                agr.roumap = ntry.roumap;
                continue;
            }
            if (s.equals("route-policy")) {
                cfgRouplc ntry = cfgAll.rtplFind(cmd.word(), false);
                if (ntry == null) {
                    return null;
                }
                agr.rouplc = ntry.rouplc;
                continue;
            }
            if (s.equals("as-set")) {
                agr.aspath = true;
                continue;
            }
            if (s.equals("summary-only")) {
                agr.summary = true;
                continue;
            }
            return null;
        }
        return agr;
    }

    /**
     * parse up interface
     *
     * @param cmd command to read
     * @return advertisement
     */
    public static ipRtrInt str2advint(cmds cmd) {
        cfgIfc iface = cfgAll.ifcFind(cmd.word(), 0);
        if (iface == null) {
            return null;
        }
        ipRtrInt adv = new ipRtrInt(iface);
        for (;;) {
            String s = cmd.word();
            if (s.length() < 1) {
                break;
            }
            if (s.equals("metric")) {
                adv.metric = new tabIntUpdater();
                s = cmd.word();
                if (adv.metric.fromString(s + " " + cmd.word())) {
                    return null;
                }
                continue;
            }
            if (s.equals("tag")) {
                adv.tag = new tabIntUpdater();
                s = cmd.word();
                if (adv.tag.fromString(s + " " + cmd.word())) {
                    return null;
                }
                continue;
            }
            if (s.equals("route-map")) {
                cfgRoump ntry = cfgAll.rtmpFind(cmd.word(), false);
                if (ntry == null) {
                    return null;
                }
                adv.roumap = ntry.roumap;
                continue;
            }
            if (s.equals("route-policy")) {
                cfgRouplc ntry = cfgAll.rtplFind(cmd.word(), false);
                if (ntry == null) {
                    return null;
                }
                adv.rouplc = ntry.rouplc;
                continue;
            }
            return null;
        }
        return adv;
    }

    public int compare(cfgRtr o1, cfgRtr o2) {
        int i = o1.type.compareTo(o2.type);
        if (i != 0) {
            return i;
        }
        if (o1.number < o2.number) {
            return -1;
        }
        if (o1.number > o2.number) {
            return +1;
        }
        return 0;
    }

    public String toString() {
        return "rtr " + number;
    }

    /**
     * create new bridge instance
     *
     * @param typ type of routing process
     * @param num number of bridge
     */
    public cfgRtr(tabRouteAttr.routeType typ, int num) {
        type = typ;
        number = num;
    }

    /**
     * destroy this router
     */
    public synchronized void closeUp() {
        running = false;
        if (rip4 != null) {
            rip4.routerCloseNow();
            rip4 = null;
        }
        if (rip6 != null) {
            rip6.routerCloseNow();
            rip6 = null;
        }
        if (babel != null) {
            babel.routerCloseNow();
            babel = null;
        }
        if (blackhole != null) {
            blackhole.routerCloseNow();
            blackhole = null;
        }
        if (olsr != null) {
            olsr.routerCloseNow();
            olsr = null;
        }
        if (ospf4 != null) {
            ospf4.routerCloseNow();
            ospf4 = null;
        }
        if (ospf6 != null) {
            ospf6.routerCloseNow();
            ospf6 = null;
        }
        if (isis != null) {
            isis.routerCloseNow();
            isis = null;
        }
        if (rift != null) {
            rift.routerCloseNow();
            rift = null;
        }
        if (pvrp != null) {
            pvrp.routerCloseNow();
            pvrp = null;
        }
        if (lsrp != null) {
            lsrp.routerCloseNow();
            lsrp = null;
        }
        if (eigrp != null) {
            eigrp.routerCloseNow();
            eigrp = null;
        }
        if (bgp != null) {
            bgp.routerCloseNow();
            bgp = null;
        }
        if (msdp != null) {
            msdp.routerCloseNow();
            msdp = null;
        }
        if (flwspc != null) {
            flwspc.routerCloseNow();
            flwspc = null;
        }
        if (ghosthunt != null) {
            ghosthunt.routerCloseNow();
            ghosthunt = null;
        }
        if (uni2multi != null) {
            uni2multi.routerCloseNow();
            uni2multi = null;
        }
        if (uni2flow != null) {
            uni2flow.routerCloseNow();
            uni2flow = null;
        }
        if (logger != null) {
            logger.routerCloseNow();
            logger = null;
        }
        if (download != null) {
            download.routerCloseNow();
            download = null;
        }
        if (deaggr != null) {
            deaggr.routerCloseNow();
            deaggr = null;
        }
        if (aggreg != null) {
            aggreg.routerCloseNow();
            aggreg = null;
        }
        if (mobile != null) {
            mobile.routerCloseNow();
            mobile = null;
        }
    }

    /**
     * get routing process
     *
     * @return routing process
     */
    public synchronized ipRtr getRouter() {
        switch (type) {
            case rip4:
                return rip4;
            case rip6:
                return rip6;
            case babel4:
            case babel6:
                return babel;
            case blackhole4:
            case blackhole6:
                return blackhole;
            case olsr4:
            case olsr6:
                return olsr;
            case ospf4:
                return ospf4;
            case ospf6:
                return ospf6;
            case isis4:
            case isis6:
                return isis;
            case rift4:
            case rift6:
                return rift;
            case pvrp4:
            case pvrp6:
                return pvrp;
            case lsrp4:
            case lsrp6:
                return lsrp;
            case eigrp4:
            case eigrp6:
                return eigrp;
            case bgp4:
            case bgp6:
                return bgp;
            case msdp4:
            case msdp6:
                return msdp;
            case flwspc4:
            case flwspc6:
                return flwspc;
            case ghosthunt4:
            case ghosthunt6:
                return ghosthunt;
            case uni2multi4:
            case uni2multi6:
                return uni2multi;
            case uni2flow4:
            case uni2flow6:
                return uni2flow;
            case logger4:
            case logger6:
                return logger;
            case download4:
            case download6:
                return download;
            case deaggr4:
            case deaggr6:
                return deaggr;
            case aggreg4:
            case aggreg6:
                return aggreg;
            case mobile4:
            case mobile6:
                return mobile;
            default:
                return null;
        }
    }

    /**
     * setup this router
     *
     * @return true on error, false if successful
     */
    public synchronized boolean setup2run() {
        if (vrf == null) {
            return true;
        }
        fwd = null;
        running = true;
        switch (type) {
            case rip4:
                fwd = vrf.fwd4;
                rip4 = new rtrRip4(vrf.fwd4, vrf.udp4, number);
                break;
            case rip6:
                fwd = vrf.fwd6;
                rip6 = new rtrRip6(vrf.fwd6, vrf.udp6, number);
                break;
            case babel4:
                fwd = vrf.fwd4;
                babel = new rtrBabel(vrf.fwd4, vrf.udp4, number);
                break;
            case babel6:
                fwd = vrf.fwd6;
                babel = new rtrBabel(vrf.fwd6, vrf.udp6, number);
                break;
            case blackhole4:
                fwd = vrf.fwd4;
                blackhole = new rtrBlackhole(vrf.fwd4, number);
                break;
            case blackhole6:
                fwd = vrf.fwd6;
                blackhole = new rtrBlackhole(vrf.fwd6, number);
                break;
            case olsr4:
                fwd = vrf.fwd4;
                olsr = new rtrOlsr(vrf.fwd4, vrf.udp4, number);
                break;
            case olsr6:
                fwd = vrf.fwd6;
                olsr = new rtrOlsr(vrf.fwd6, vrf.udp6, number);
                break;
            case ospf4:
                fwd = vrf.fwd4;
                ospf4 = new rtrOspf4(vrf.fwd4, vrf.udp4, number);
                break;
            case ospf6:
                fwd = vrf.fwd6;
                ospf6 = new rtrOspf6(vrf.fwd6, vrf.udp6, number);
                break;
            case isis4:
                fwd = vrf.fwd4;
                isis = new rtrIsis(vrf.fwd4, vrf.fwd6, vrf.udp4, number);
                break;
            case isis6:
                fwd = vrf.fwd6;
                isis = new rtrIsis(vrf.fwd6, vrf.fwd4, vrf.udp6, number);
                break;
            case rift4:
                fwd = vrf.fwd4;
                rift = new rtrRift(vrf.fwd4, vrf.udp4, number);
                break;
            case rift6:
                fwd = vrf.fwd6;
                rift = new rtrRift(vrf.fwd6, vrf.udp6, number);
                break;
            case pvrp4:
                fwd = vrf.fwd4;
                pvrp = new rtrPvrp(vrf.fwd4, vrf.udp4, vrf.tcp4, number);
                break;
            case pvrp6:
                fwd = vrf.fwd6;
                pvrp = new rtrPvrp(vrf.fwd6, vrf.udp6, vrf.tcp6, number);
                break;
            case lsrp4:
                fwd = vrf.fwd4;
                lsrp = new rtrLsrp(vrf.fwd4, vrf.udp4, vrf.tcp4, number);
                break;
            case lsrp6:
                fwd = vrf.fwd6;
                lsrp = new rtrLsrp(vrf.fwd6, vrf.udp6, vrf.tcp6, number);
                break;
            case eigrp4:
                fwd = vrf.fwd4;
                eigrp = new rtrEigrp(vrf.fwd4, number);
                break;
            case eigrp6:
                fwd = vrf.fwd6;
                eigrp = new rtrEigrp(vrf.fwd6, number);
                break;
            case bgp4:
                fwd = vrf.fwd4;
                bgp = new rtrBgp(vrf.fwd4, vrf, vrf.tcp4, number);
                break;
            case bgp6:
                fwd = vrf.fwd6;
                bgp = new rtrBgp(vrf.fwd6, vrf, vrf.tcp6, number);
                break;
            case msdp4:
                fwd = vrf.fwd4;
                msdp = new rtrMsdp(vrf.fwd4, vrf.tcp4, number);
                break;
            case msdp6:
                fwd = vrf.fwd6;
                msdp = new rtrMsdp(vrf.fwd6, vrf.tcp6, number);
                break;
            case flwspc4:
                fwd = vrf.fwd4;
                flwspc = new rtrFlowspec(vrf.fwd4, number);
                break;
            case flwspc6:
                fwd = vrf.fwd6;
                flwspc = new rtrFlowspec(vrf.fwd6, number);
                break;
            case ghosthunt4:
                fwd = vrf.fwd4;
                ghosthunt = new rtrGhosthunt(vrf.fwd4, number);
                break;
            case ghosthunt6:
                fwd = vrf.fwd6;
                ghosthunt = new rtrGhosthunt(vrf.fwd6, number);
                break;
            case uni2multi4:
                fwd = vrf.fwd4;
                uni2multi = new rtrUni2multi(vrf.fwd4, number);
                break;
            case uni2multi6:
                fwd = vrf.fwd6;
                uni2multi = new rtrUni2multi(vrf.fwd6, number);
                break;
            case uni2flow4:
                fwd = vrf.fwd4;
                uni2flow = new rtrUni2flow(vrf.fwd4, number);
                break;
            case uni2flow6:
                fwd = vrf.fwd6;
                uni2flow = new rtrUni2flow(vrf.fwd6, number);
                break;
            case logger4:
                fwd = vrf.fwd4;
                logger = new rtrLogger(vrf.fwd4, number);
                break;
            case logger6:
                fwd = vrf.fwd6;
                logger = new rtrLogger(vrf.fwd6, number);
                break;
            case download4:
                fwd = vrf.fwd4;
                download = new rtrDownload(vrf.fwd4, number);
                break;
            case download6:
                fwd = vrf.fwd6;
                download = new rtrDownload(vrf.fwd6, number);
                break;
            case deaggr4:
                fwd = vrf.fwd4;
                deaggr = new rtrDeaggr(vrf.fwd4, number);
                break;
            case deaggr6:
                fwd = vrf.fwd6;
                deaggr = new rtrDeaggr(vrf.fwd6, number);
                break;
            case aggreg4:
                fwd = vrf.fwd4;
                aggreg = new rtrAggreg(vrf.fwd4, number);
                break;
            case aggreg6:
                fwd = vrf.fwd6;
                aggreg = new rtrAggreg(vrf.fwd6, number);
                break;
            case mobile4:
                fwd = vrf.fwd4;
                mobile = new rtrMobile(vrf.fwd4, number);
                break;
            case mobile6:
                fwd = vrf.fwd6;
                mobile = new rtrMobile(vrf.fwd6, number);
                break;
            default:
                return true;
        }
        return false;
    }

    private synchronized List<String> getShRun(int mode, int filter) {
        boolean need2nd;
        switch (type) {
            case bgp4:
            case bgp6:
            case msdp4:
            case msdp6:
                need2nd = true;
                break;
            default:
                need2nd = false;
                break;
        }
        List<String> l = new ArrayList<String>();
        switch (mode) {
            case 1:
                break;
            case 2:
                if (need2nd) {
                    return l;
                }
                break;
            case 3:
                if (!need2nd) {
                    return l;
                }
                break;
            default:
                return l;
        }
        l.add("router " + num2name(type) + " " + number);
        cmds.cfgLine(l, description == null, cmds.tabulator, "description", description);
        if (vrf == null) {
            l.add(cmds.tabulator + "no vrf");
        } else {
            l.add(cmds.tabulator + "vrf " + vrf.name);
        }
        ipRtr rtr = getRouter();
        if (rtr != null) {
            rtr.routerGetConfig(l, cmds.tabulator, filter);
            getShRedist(l, cmds.tabulator, rtr);
            if (rtr.routerAutoMesh == null) {
                l.add(cmds.tabulator + "no automesh");
            } else {
                l.add(cmds.tabulator + "automesh " + rtr.routerAutoMesh.listName);
            }
        }
        l.add(cmds.tabulator + cmds.finish);
        l.add(cmds.comment);
        if ((filter & 1) == 0) {
            return l;
        }
        return userFilter.filterText(l, defaultF);
    }

    public List<String> getShRun(int filter) {
        return getShRun(1, filter);
    }

    /**
     * get configuration
     *
     * @param filter filter defaults
     * @return configuration
     */
    public List<String> getShRun1(int filter) {
        return getShRun(2, filter);
    }

    /**
     * get configuration
     *
     * @param filter filter defaults
     * @return configuration
     */
    public List<String> getShRun2(int filter) {
        return getShRun(3, filter);
    }

    /**
     * get help string
     *
     * @param l list to update
     * @param p number start
     * @param e ending
     */
    public static void getRouterList(userHelping l, int p, String e) {
        l.add(null, (p + 2) + " " + (p + 3) + "     bgp4                  border gateway protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     bgp6                  border gateway protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     ospf4                 open shortest path first" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     ospf6                 open shortest path first" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     isis4                 intermediate system intermediate system" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     isis6                 intermediate system intermediate system" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     rift4                 routing in fat trees" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     rift6                 routing in fat trees" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     pvrp4                 path vector routing protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     pvrp6                 path vector routing protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     lsrp4                 link state routing protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     lsrp6                 link state routing protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     eigrp4                enhanced interior gateway routing protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     eigrp6                enhanced interior gateway routing protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     rip4                  routing information protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     rip6                  routing information protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     babel4                babel routing protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     babel6                babel routing protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     blackhole4            blackhole collector" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     blackhole6            blackhole collector" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     olsr4                 optimized link state routing protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     olsr6                 optimized link state routing protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     msdp4                 multicast source discovery protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     msdp6                 multicast source discovery protocol" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     flowspec4             flowspec to flowspec rewriter" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     flowspec6             flowspec to flowspec rewriter" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     ghosthunt4            ghost/zombie route hunter" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     ghosthunt6            ghost/zombie route hunter" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     uni2multi4            unicast to multicast converter" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     uni2multi6            unicast to multicast converter" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     uni2flow4             unicast to flowspec converter" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     uni2flow6             unicast to flowspec converter" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     logger4               route logger" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     logger6               route logger" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     download4             route download" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     download6             route download" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     deaggr4               deaggregate creator" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     deaggr6               deaggregate creator" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     aggreg4               auto aggregate creator" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     aggreg6               auto aggregate creator" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     mobile4               mobile route creator" + e);
        l.add(null, (p + 2) + " " + (p + 3) + "     mobile6               mobile route creator" + e);
    }

    /**
     * get help string
     *
     * @param l list to update
     * @param p number start
     * @param e ending
     */
    public static void getRouterList(userHelping l, String p, String e) {
        l.add(null, p + "   connected             connected routes" + e);
        l.add(null, p + "   static                static routes" + e);
        l.add(null, p + "   defgw                 routes through default gateway" + e);
        l.add(null, p + "   local                 local interface addresses" + e);
        l.add(null, p + "   remote                remote interface addresses" + e);
    }

    /**
     * get help string
     *
     * @param l list to update
     * @param p number start
     */
    public static void getRedistHelp(userHelping l, int p) {
        l.add(null, (p + 1) + " .  ecmp                    enable ecmp export to rib");
        l.add(null, (p + 1) + " " + (p + 2) + "   redistribute            redistribute prefixes from other protocols");
        getRouterList(l, (p + 2) + " " + (p + 4) + ",.", "");
        getRouterList(l, p, " routes");
        l.add(null, (p + 3) + " " + (p + 4) + ",.     <num>               process id");
        l.add(null, (p + 4) + " " + (p + 4) + ",.       ecmp              process ecmp alternatives also");
        l.add(null, (p + 4) + " " + (p + 5) + "         route-map         process prefixes on importing");
        l.add(null, (p + 5) + " " + (p + 4) + ",.         <name:rm>       name of route map");
        l.add(null, (p + 4) + " " + (p + 5) + "         route-policy      process prefixes on importing");
        l.add(null, (p + 5) + " " + (p + 4) + ",.         <name:rpl>      name of route policy");
        l.add(null, (p + 4) + " " + (p + 5) + "         prefix-list       filter prefixes on importing");
        l.add(null, (p + 5) + " " + (p + 4) + ",.         <name:pl>       name of prefix list");
        l.add(null, (p + 4) + " " + (p + 5) + "         metric            set metric");
        l.add(null, (p + 5) + " " + (p + 4) + ",.         <num>           value");
        l.add(null, (p + 4) + " " + (p + 5) + "         limit             limit number of routes to import");
        l.add(null, (p + 5) + " " + (p + 4) + ",.         <num>           value");
        l.add(null, (p + 4) + " " + (p + 5) + "         tag               set tag");
        l.add(null, (p + 5) + " " + (p + 4) + ",.         <num>           value");
        l.add(null, (p + 1) + " " + (p + 2) + "   advertise               advertise one prefix of mine");
        l.add(null, (p + 1) + " " + (p + 2) + "   readvertise             readvertise one prefix from anywhere");
        l.add(null, (p + 2) + " " + (p + 3) + ",.   <pref>                prefix");
        l.add(null, (p + 3) + " " + (p + 3) + ",.     ecmp                process ecmp alternatives also");
        l.add(null, (p + 3) + " " + (p + 4) + "       route-map           set properties of advertisement");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <name:rm>         name of route map");
        l.add(null, (p + 3) + " " + (p + 4) + "       route-policy        set properties of advertisement");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <name:rpl>        name of route policy");
        l.add(null, (p + 3) + " " + (p + 4) + "       metric              set metric");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <num>             value");
        l.add(null, (p + 3) + " " + (p + 4) + "       tag                 set tag");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <num>             value");
        l.add(null, (p + 1) + " " + (p + 2) + "   justadvert              advertise interface");
        l.add(null, (p + 2) + " " + (p + 3) + ",.   <name:ifc>            name of interface");
        l.add(null, (p + 3) + " " + (p + 4) + "       route-map           set properties of advertisement");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <name:rm>         name of route map");
        l.add(null, (p + 3) + " " + (p + 4) + "       route-policy        set properties of advertisement");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <name:rpl>        name of route policy");
        l.add(null, (p + 3) + " " + (p + 4) + "       metric              set metric");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <num>             value");
        l.add(null, (p + 3) + " " + (p + 4) + "       tag                 set tag");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <num>             value");
        l.add(null, (p + 1) + " " + (p + 2) + "   aggregate               aggregate more prefixes");
        l.add(null, (p + 2) + " " + (p + 3) + ",.   <pref>                prefix");
        l.add(null, (p + 3) + " " + (p + 4) + "       route-map           set properties of advertisement");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <name:rm>         name of route map");
        l.add(null, (p + 3) + " " + (p + 4) + "       route-policy        set properties of advertisement");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <name:rpl>        name of route policy");
        l.add(null, (p + 3) + " " + (p + 4) + "       prefix-list         filter prefixes for aggregation");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <name:pl>         name of prefix list");
        l.add(null, (p + 3) + " " + (p + 4) + "       metric              set metric");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <num>             value");
        l.add(null, (p + 3) + " " + (p + 4) + "       tag                 set tag");
        l.add(null, (p + 4) + " " + (p + 3) + ",.       <num>             value");
        l.add(null, (p + 3) + " " + (p + 3) + ",.     as-set              generate as path information");
        l.add(null, (p + 3) + " " + (p + 3) + ",.     summary-only        filter more specific prefixes");
    }

    public void getHelp(userHelping l) {
        l.add(null, "1 2   description             specify description");
        l.add(null, "2 2,.   <str>                 description");
        l.add(null, "1 2   vrf                     specify vrf to use");
        l.add(null, "2 .     <name:vrf>            name of table");
        l.add(null, "1 2   automesh                specify auto mesh te tunnels");
        l.add(null, "2 .     <name:pl>             name of prefix list");
        getRedistHelp(l, 0);
        ipRtr rtr = getRouter();
        if (rtr != null) {
            rtr.routerGetHelp(l);
        }
    }

    public synchronized void doCfgStr(cmds cmd) {
        ipRtr rtr = getRouter();
        if (rtr != null) {
            if (!rtr.routerConfigure(cmd.copyBytes(false))) {
                return;
            }
        }
        String a = cmd.word();
        boolean neg = a.equals("no");
        if (neg) {
            a = cmd.word();
        }
        if (a.equals("description")) {
            description = cmd.getRemaining();
            if (neg) {
                description = null;
            }
            return;
        }
        if (a.equals("vrf")) {
            if (neg) {
                closeUp();
                vrf = null;
                return;
            }
            if (vrf != null) {
                cmd.error("already initialized");
                return;
            }
            a = cmd.word();
            cfgVrf v = cfgAll.vrfFind(a, false);
            if (v == null) {
                cmd.error("no such vrf");
                return;
            }
            vrf = v;
            setup2run();
            return;
        }
        if (rtr == null) {
            cmd.error("not initialized");
            return;
        }
        if (a.equals("automesh")) {
            if (neg) {
                rtr.routerAutoMesh = null;
                return;
            }
            cfgPrfxlst prf = cfgAll.prfxFind(cmd.word(), false);
            if (prf == null) {
                cmd.error("no such prefixlist");
                return;
            }
            rtr.routerAutoMesh = prf.prflst;
            return;
        }
        if (doCfgRedist(rtr, fwd, neg, a, cmd)) {
            cmd.badCmd();
        }
    }

    public String getPrompt() {
        return "rtr";
    }

}
