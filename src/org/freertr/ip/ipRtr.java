package org.freertr.ip;

import java.util.List;
import org.freertr.addr.addrIP;
import org.freertr.addr.addrIPv4;
import org.freertr.cfg.cfgRtr;
import org.freertr.tab.tabGen;
import org.freertr.tab.tabIndex;
import org.freertr.tab.tabLabelEntry;
import org.freertr.tab.tabListing;
import org.freertr.tab.tabPrfxlstN;
import org.freertr.tab.tabRoute;
import org.freertr.tab.tabRouteAttr;
import org.freertr.tab.tabRouteUtil;
import org.freertr.user.userHelping;
import org.freertr.util.cmds;

/**
 * ip routers have to use it to be able to work with ip forwarder
 *
 * @author matecsaba
 */
public abstract class ipRtr implements Comparable<ipRtr> {

    /**
     * create instance
     */
    public ipRtr() {
    }

    /**
     * protocol id of this routing protocol
     */
    protected int routerProtoNum;

    /**
     * process id of this routing protocol
     */
    protected int routerProcNum;

    /**
     * protocol type of this routing protocol
     */
    protected tabRouteAttr.routeType routerProtoTyp;

    /**
     * ecmp enabled
     */
    public boolean routerEcmp;

    /**
     * vpn instance
     */
    public boolean routerVpn;

    /**
     * igp instance
     */
    public boolean routerIgp;

    /**
     * number of times redist changed
     */
    public int routerRedistChg;

    /**
     * last time redist changed
     */
    public long routerRedistTim;

    /**
     * number of times computed changed
     */
    public int routerComputeChg;

    /**
     * last time computed changed
     */
    public long routerComputeTim;

    /**
     * the unicast routes computed from protocol
     */
    public tabRoute<addrIP> routerComputedU = new tabRoute<addrIP>("computed");

    /**
     * the multicast routes computed from protocol
     */
    public tabRoute<addrIP> routerComputedM = new tabRoute<addrIP>("computed");

    /**
     * the flowspec routes computed from protocol
     */
    public tabRoute<addrIP> routerComputedF = new tabRoute<addrIP>("computed");

    /**
     * the index to prefix computed from protocol
     */
    public tabGen<tabIndex<addrIP>> routerComputedI = new tabGen<tabIndex<addrIP>>();

    /**
     * the imported unicast routes
     */
    public tabRoute<addrIP> routerRedistedU = new tabRoute<addrIP>("imported");

    /**
     * the imported multicast routes
     */
    public tabRoute<addrIP> routerRedistedM = new tabRoute<addrIP>("imported");

    /**
     * the imported flowspec routes
     */
    public tabRoute<addrIP> routerRedistedF = new tabRoute<addrIP>("imported");

    /**
     * the unicast routes changed from protocol
     */
    public tabRoute<addrIP> routerChangedU = null;

    /**
     * the multicast routes changed from protocol
     */
    public tabRoute<addrIP> routerChangedM = null;

    /**
     * the flowspec routes changed from protocol
     */
    public tabRoute<addrIP> routerChangedF = null;

    /**
     * list of route imports
     */
    public tabGen<ipRtrRed> routerRedisting = new tabGen<ipRtrRed>();

    /**
     * list of prefix imports
     */
    public tabGen<ipRtrAdv> routerAdverting = new tabGen<ipRtrAdv>();

    /**
     * list of prefix redistributions
     */
    public tabGen<ipRtrAdv> routerReadvrtng = new tabGen<ipRtrAdv>();

    /**
     * list of interface imports
     */
    public tabGen<ipRtrInt> routerAdvInter = new tabGen<ipRtrInt>();

    /**
     * list of aggregates
     */
    public tabGen<ipRtrAgr> routerAggregating = new tabGen<ipRtrAgr>();

    /**
     * auto mesh prefix list
     */
    public tabListing<tabPrfxlstN, addrIP> routerAutoMesh;

    /**
     * auto summary computed prefixes
     */
    public boolean routerAutoSummary;

    /**
     * filter for the auto summarization
     */
    public tabListing<tabPrfxlstN, addrIP> routerAutoSumPfx;

    public int compareTo(ipRtr o) {
        if (routerProtoNum < o.routerProtoNum) {
            return -1;
        }
        if (routerProtoNum > o.routerProtoNum) {
            return +1;
        }
        return 0;
    }

    /**
     * get router name
     *
     * @return name
     */
    public String routerGetName() {
        return cfgRtr.num2name(routerProtoTyp) + " " + routerProcNum;
    }

    /**
     * check if this is a rpki process
     *
     * @param t check if rpki or not
     * @return -1 if not, ip version if yes
     */
    public static int isRPKI(tabRouteAttr.routeType t) {
        if (t == null) {
            return -1;
        }
        switch (t) {
            case rpki4:
                return ipCor4.protocolVersion;
            case rpki6:
                return ipCor6.protocolVersion;
            default:
                return -1;
        }
    }

    /**
     * check if this is a bgp process
     *
     * @return 0=no, 1=bgp, 2=vpn
     */
    public int isBGP() {
        switch (routerProtoTyp) {
            case msdp4:
            case msdp6:
            case rpki4:
            case rpki6:
            case flwspc4:
            case flwspc6:
            case ghosthunt4:
            case ghosthunt6:
            case uni2multi4:
            case uni2multi6:
            case uni2flow4:
            case uni2flow6:
            case logger4:
            case logger6:
            case download4:
            case download6:
            case deaggr4:
            case deaggr6:
            case aggreg4:
            case aggreg6:
            case mobile4:
            case mobile6:
                return 1;
            case bgp4:
            case bgp6:
                if (routerIgp) {
                    return 0;
                }
                if (routerVpn) {
                    return 2;
                }
                return 1;
            default:
                return 0;
        }
    }

    /**
     * get add mode
     *
     * @return add mode
     */
    public tabRoute.addType getAddMode() {
        return routerEcmp ? tabRoute.addType.altEcmp : tabRoute.addType.better;
    }

    /**
     * do aggregates
     *
     * @param afi address family
     * @param src source table
     * @param trg target table
     * @param lab label to use
     * @param agrR aggregator router
     * @param agrA aggregator as
     */
    public void routerDoAggregates(int afi, tabRoute<addrIP> src, tabRoute<addrIP> trg, tabLabelEntry lab, addrIPv4 agrR, int agrA) {
        for (int i = 0; i < routerAggregating.size(); i++) {
            ipRtrAgr ntry = routerAggregating.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.filter(afi, src, trg, lab, agrR, agrA, routerProtoTyp, routerProcNum);
        }
        if (routerAutoSummary) {
            tabRouteUtil.compressTable(afi, src, routerAutoSumPfx);
        }
    }

    /**
     * create a computed table from protocol information
     */
    public abstract void routerCreateComputed();

    /**
     * somebody has updated imported table
     */
    public abstract void routerRedistChanged();

    /**
     * somebody has updated other parts of routing table
     */
    public abstract void routerOthersChanged();

    /**
     * get help text
     *
     * @param l text to update
     */
    public abstract void routerGetHelp(userHelping l);

    /**
     * get configuration
     *
     * @param l list to update
     * @param beg beginning string
     * @param filter defaults
     */
    public abstract void routerGetConfig(List<String> l, String beg, int filter);

    /**
     * parse configuration
     *
     * @param cmd command to parse
     * @return false on success, true on error
     */
    public abstract boolean routerConfigure(cmds cmd);

    /**
     * protocol should close now
     */
    public abstract void routerCloseNow();

    /**
     * count number of neighbors
     *
     * @return number of neighbors
     */
    public abstract int routerNeighCount();

    /**
     * get list of neighbors
     *
     * @param tab table to update
     */
    public abstract void routerNeighList(tabRoute<addrIP> tab);

    /**
     * count number of interfaces
     *
     * @return number of interfaces
     */
    public abstract int routerIfaceCount();

    /**
     * maximum recursion depth
     *
     * @return allowed number
     */
    public abstract int routerRecursions();

    /**
     * get list of link states
     *
     * @param tab table to update
     * @param par parameter
     * @param asn asn
     * @param adv advertiser
     */
    public abstract void routerLinkStates(tabRoute<addrIP> tab, int par, int asn, addrIPv4 adv);

    /**
     * get state information
     *
     * @param lst list to append
     */
    public abstract void routerStateGet(List<String> lst);

    /**
     * set state information
     *
     * @param cmd string to append
     * @return true on error, false on success
     */
    public abstract boolean routerStateSet(cmds cmd);

}
