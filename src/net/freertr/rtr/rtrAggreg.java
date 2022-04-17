package net.freertr.rtr;

import java.util.List;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrIPv4;
import net.freertr.addr.addrIPv6;
import net.freertr.ip.ipCor4;
import net.freertr.ip.ipCor6;
import net.freertr.ip.ipFwd;
import net.freertr.ip.ipRtr;
import net.freertr.tab.tabGen;
import net.freertr.tab.tabIndex;
import net.freertr.tab.tabRoute;
import net.freertr.tab.tabRouteAttr;
import net.freertr.tab.tabRouteEntry;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;

/**
 * auto aggregate creator
 *
 * @author matecsaba
 */
public class rtrAggreg extends ipRtr {

    /**
     * distance
     */
    public int distance;

    /**
     * address family
     */
    protected int afi;

    /**
     * nexthop
     */
    public addrIP nextHop;

    /**
     * netmask
     */
    public int netMask;

    /**
     * netmask addition
     */
    public final int maskAdd;

    /**
     * the forwarder protocol
     */
    public final ipFwd fwdCore;

    /**
     * route type
     */
    protected final tabRouteAttr.routeType rouTyp;

    /**
     * router number
     */
    protected final int rtrNum;

    /**
     * create aggregator process
     *
     * @param forwarder forwarder to update
     * @param id process id
     */
    public rtrAggreg(ipFwd forwarder, int id) {
        fwdCore = forwarder;
        rtrNum = id;
        switch (fwdCore.ipVersion) {
            case ipCor4.protocolVersion:
                rouTyp = tabRouteAttr.routeType.aggreg4;
                maskAdd = new addrIP().maxBits() - new addrIPv4().maxBits();
                break;
            case ipCor6.protocolVersion:
                rouTyp = tabRouteAttr.routeType.aggreg6;
                maskAdd = new addrIP().maxBits() - new addrIPv6().maxBits();
                break;
            default:
                rouTyp = null;
                maskAdd = 0;
                break;
        }
        afi = 1;
        routerComputedU = new tabRoute<addrIP>("rx");
        routerComputedM = new tabRoute<addrIP>("rx");
        routerComputedF = new tabRoute<addrIP>("rx");
        routerComputedI = new tabGen<tabIndex<addrIP>>();
        distance = 254;
        nextHop = new addrIP();
        netMask = 0;
        routerCreateComputed();
        fwdCore.routerAdd(this, rouTyp, id);
    }

    /**
     * convert to string
     *
     * @return string
     */
    public String toString() {
        return "aggreg on " + fwdCore;
    }

    private void doPrefix(tabRouteEntry<addrIP> ntry, tabRoute<addrIP> tab) {
        if (ntry == null) {
            return;
        }
        if (ntry.prefix.maskLen <= (maskAdd + netMask)) {
            return;
        }
        ntry = ntry.copyBytes(tabRoute.addType.notyet);
        ntry.best.rouTyp = rouTyp;
        ntry.best.protoNum = rtrNum;
        ntry.prefix.setMask(maskAdd + netMask);
        if (distance > 0) {
            ntry.best.distance = distance;
        }
        if (!nextHop.isEmpty()) {
            ntry.best.nextHop = nextHop.copyBytes();
        }
        tab.add(tabRoute.addType.better, ntry, true, false);
    }

    /**
     * create computed
     */
    public synchronized void routerCreateComputed() {
        tabRoute<addrIP> resU = new tabRoute<addrIP>("computed");
        tabRoute<addrIP> resM = new tabRoute<addrIP>("computed");
        switch (afi) {
            case 1:
                for (int i = 0; i < routerRedistedU.size(); i++) {
                    doPrefix(routerRedistedU.get(i), resU);
                }
                break;
            case 2:
                for (int i = 0; i < routerRedistedM.size(); i++) {
                    doPrefix(routerRedistedM.get(i), resM);
                }
                break;
        }
        routerDoAggregates(rtrBgpUtil.sfiUnicast, resU, resU, fwdCore.commonLabel, null, 0);
        routerDoAggregates(rtrBgpUtil.sfiMulticast, resM, resM, fwdCore.commonLabel, null, 0);
        boolean same = resU.preserveTime(routerComputedU);
        same &= resM.preserveTime(routerComputedM);
        if (same) {
            return;
        }
        routerComputedU = resU;
        routerComputedM = resM;
        fwdCore.routerChg(this, false);
    }

    /**
     * redistribution changed
     */
    public void routerRedistChanged() {
        routerCreateComputed();
    }

    /**
     * others changed
     */
    public void routerOthersChanged() {
    }

    /**
     * get help
     *
     * @param l list
     */
    public void routerGetHelp(userHelping l) {
        l.add(null, "1 2   distance                    specify default distance");
        l.add(null, "2 .     <num>                     distance");
        l.add(null, "1 2   nexthop                     specify default nexthop");
        l.add(null, "2 .     <addr>                    nexthop");
        l.add(null, "1 2   netmask                     specify netmask to use");
        l.add(null, "2 .     <num>                     mask bits");
        l.add(null, "1 2   afi                         set address family");
        l.add(null, "2 .     unicast                   select unicast");
        l.add(null, "2 .     multicast                 select multicast");
    }

    /**
     * get config
     *
     * @param l list
     * @param beg beginning
     * @param filter filter
     */
    public void routerGetConfig(List<String> l, String beg, int filter) {
        l.add(beg + "afi " + rtrLogger.afi2str(afi));
        l.add(beg + "netmask " + netMask);
        l.add(beg + "distance " + distance);
        l.add(beg + "nexthop " + nextHop);
    }

    /**
     * configure
     *
     * @param cmd command
     * @return false if success, true if error
     */
    public boolean routerConfigure(cmds cmd) {
        String s = cmd.word();
        boolean negated = false;
        if (s.equals("no")) {
            s = cmd.word();
            negated = true;
        }
        if (s.equals("afi")) {
            if (negated) {
                afi = 1;
                return false;
            }
            afi = rtrLogger.str2afi(cmd.word());
            return false;
        }
        if (s.equals("distance")) {
            distance = bits.str2num(cmd.word());
            return false;
        }
        if (s.equals("nexthop")) {
            nextHop = new addrIP();
            if (negated) {
                return false;
            }
            nextHop.fromString(cmd.word());
            return false;
        }
        if (s.equals("netmask")) {
            netMask = bits.str2num(cmd.word());
            return false;
        }
        return true;
    }

    /**
     * stop work
     */
    public void routerCloseNow() {
    }

    /**
     * get neighbor count
     *
     * @return count
     */
    public int routerNeighCount() {
        return 0;
    }

    /**
     * get neighbor list
     *
     * @param tab list
     */
    public void routerNeighList(tabRoute<addrIP> tab) {
    }

    /**
     * get interface count
     *
     * @return count
     */
    public int routerIfaceCount() {
        return 0;
    }

    /**
     * maximum recursion depth
     *
     * @return allowed number
     */
    public int routerRecursions() {
        return 1;
    }

    /**
     * get list of link states
     *
     * @param tab table to update
     * @param par parameter
     * @param asn asn
     * @param adv advertiser
     */
    public void routerLinkStates(tabRoute<addrIP> tab, int par, int asn, addrIPv4 adv) {
    }

}
