package org.freertr.rtr;

import java.util.List;
import org.freertr.addr.addrIP;
import org.freertr.addr.addrPrefix;
import org.freertr.cfg.cfgAll;
import org.freertr.cfg.cfgPrfxlst;
import org.freertr.cfg.cfgRoump;
import org.freertr.cfg.cfgRouplc;
import org.freertr.ip.ipFwdIface;
import org.freertr.pack.packHolder;
import org.freertr.prt.prtGenConn;
import org.freertr.tab.tabListing;
import org.freertr.tab.tabPrfxlstN;
import org.freertr.tab.tabRoute;
import org.freertr.tab.tabRouteAttr;
import org.freertr.tab.tabRouteEntry;
import org.freertr.tab.tabRtrmapN;
import org.freertr.tab.tabRtrplcN;
import org.freertr.user.userHelp;
import org.freertr.util.bits;
import org.freertr.util.cmds;
import org.freertr.util.debugger;
import org.freertr.util.logger;

/**
 * ripv6 interface
 *
 * @author matecsaba
 */
public class rtrRip6iface implements Comparable<rtrRip6iface> {

    /**
     * advertisement interval
     */
    public int updateTimer = 30000;

    /**
     * hold time
     */
    public int holdTimer = 180000;

    /**
     * invalid time
     */
    public int flushTimer = 240000;

    /**
     * time to wait between packets
     */
    public int interPackTime = 20;

    /**
     * default distance
     */
    public int distance = 120;

    /**
     * default input metric
     */
    public int metricIn = 0;

    /**
     * default output metric
     */
    public int metricOut = 1;

    /**
     * bfd enabled
     */
    public boolean bfdTrigger;

    /**
     * not advertise routes learned from interface back
     */
    public boolean splitHorizon = true;

    /**
     * advertise back best routes with infinity metric
     */
    public boolean poisonReverse = true;

    /**
     * check neighbor address is connected
     */
    public boolean connectedCheck = true;

    /**
     * allow receive of routes
     */
    public boolean allowRx = true;

    /**
     * allow transmit of routes
     */
    public boolean allowTx = true;

    /**
     * advertise default route
     */
    public boolean defOrigin = false;

    /**
     * suppress interface address
     */
    public boolean suppressAddr = false;

    /**
     * unsuppress interface address
     */
    public boolean unsuppressAddr = false;

    /**
     * ingress prefix list
     */
    public tabListing<tabPrfxlstN, addrIP> prflstIn;

    /**
     * egress prefix list
     */
    public tabListing<tabPrfxlstN, addrIP> prflstOut;

    /**
     * ingress route map
     */
    public tabListing<tabRtrmapN, addrIP> roumapIn;

    /**
     * egress route map
     */
    public tabListing<tabRtrmapN, addrIP> roumapOut;

    /**
     * ingress route map
     */
    public tabListing<tabRtrplcN, addrIP> roupolIn;

    /**
     * egress route map
     */
    public tabListing<tabRtrplcN, addrIP> roupolOut;

    /**
     * the interface this works on
     */
    protected final ipFwdIface iface;

    /**
     * the udp connection it uses to multicast
     */
    protected prtGenConn conn;

    /**
     * the lower layer
     */
    protected final rtrRip6 lower;

    /**
     * create one instance
     *
     * @param parent the rip protocol
     * @param ifc the ip interface to work on
     */
    public rtrRip6iface(rtrRip6 parent, ipFwdIface ifc) {
        lower = parent;
        iface = ifc;
    }

    /**
     * unregister from udp
     */
    public void unregister2udp() {
        lower.udpCore.listenStop(iface, rtrRip6.port, null, 0);
        conn.setClosing();
    }

    /**
     * register to udp
     */
    public void register2udp() {
        addrIP adr = new addrIP();
        adr.fromString("ff02::9");
        lower.udpCore.packetListen(lower, iface, rtrRip6.port, null, 0, "rip", -1, null, -1, -1);
        conn = lower.udpCore.packetConnect(lower, iface, rtrRip6.port, adr, rtrRip6.port, "rip", -1, null, -1, -1);
        if (conn == null) {
            return;
        }
        conn.timeout = 0;
    }

    public String toString() {
        return "rip on " + iface;
    }

    public int compareTo(rtrRip6iface o) {
        if (iface.ifwNum < o.iface.ifwNum) {
            return -1;
        }
        if (iface.ifwNum > o.iface.ifwNum) {
            return +1;
        }
        return 0;
    }

    /**
     * get configuration
     *
     * @param l list to add
     * @param beg beginning
     */
    public void routerGetConfig(List<String> l, String beg) {
        l.add(cmds.tabulator + beg + "enable");
        cmds.cfgLine(l, !allowRx, cmds.tabulator, beg + "allow-rx", "");
        cmds.cfgLine(l, !allowTx, cmds.tabulator, beg + "allow-tx", "");
        cmds.cfgLine(l, !bfdTrigger, cmds.tabulator, beg + "bfd", "");
        cmds.cfgLine(l, !connectedCheck, cmds.tabulator, beg + "verify-source", "");
        cmds.cfgLine(l, !poisonReverse, cmds.tabulator, beg + "poison-reverse", "");
        cmds.cfgLine(l, !splitHorizon, cmds.tabulator, beg + "split-horizon", "");
        cmds.cfgLine(l, !defOrigin, cmds.tabulator, beg + "default-originate", "");
        cmds.cfgLine(l, !suppressAddr, cmds.tabulator, beg + "suppress-prefix", "");
        cmds.cfgLine(l, !unsuppressAddr, cmds.tabulator, beg + "unsuppress-prefix", "");
        l.add(cmds.tabulator + beg + "distance " + distance);
        l.add(cmds.tabulator + beg + "metric-in " + metricIn);
        l.add(cmds.tabulator + beg + "metric-out " + metricOut);
        l.add(cmds.tabulator + beg + "packet-timer " + interPackTime);
        l.add(cmds.tabulator + beg + "update-timer " + updateTimer);
        l.add(cmds.tabulator + beg + "hold-time " + holdTimer);
        l.add(cmds.tabulator + beg + "flush-time " + flushTimer);
        cmds.cfgLine(l, prflstIn == null, cmds.tabulator, beg + "prefix-list-in", "" + prflstIn);
        cmds.cfgLine(l, prflstOut == null, cmds.tabulator, beg + "prefix-list-out", "" + prflstOut);
        cmds.cfgLine(l, roumapIn == null, cmds.tabulator, beg + "route-map-in", "" + roumapIn);
        cmds.cfgLine(l, roumapOut == null, cmds.tabulator, beg + "route-map-out", "" + roumapOut);
        cmds.cfgLine(l, roupolIn == null, cmds.tabulator, beg + "route-policy-in", "" + roupolIn);
        cmds.cfgLine(l, roupolOut == null, cmds.tabulator, beg + "route-policy-out", "" + roupolOut);
    }

    /**
     * do configuration
     *
     * @param a command
     * @param cmd parameters
     */
    public void routerDoConfig(String a, cmds cmd) {
        if (a.equals("allow-rx")) {
            allowRx = true;
            return;
        }
        if (a.equals("allow-tx")) {
            allowTx = true;
            return;
        }
        if (a.equals("bfd")) {
            bfdTrigger = true;
            return;
        }
        if (a.equals("default-originate")) {
            defOrigin = true;
            return;
        }
        if (a.equals("suppress-prefix")) {
            suppressAddr = true;
            return;
        }
        if (a.equals("unsuppress-prefix")) {
            unsuppressAddr = true;
            return;
        }
        if (a.equals("verify-source")) {
            connectedCheck = true;
            return;
        }
        if (a.equals("poison-reverse")) {
            poisonReverse = true;
            return;
        }
        if (a.equals("split-horizon")) {
            splitHorizon = true;
            return;
        }
        if (a.equals("distance")) {
            distance = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("metric-in")) {
            metricIn = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("metric-out")) {
            metricOut = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("packet-timer")) {
            interPackTime = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("update-timer")) {
            updateTimer = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("hold-time")) {
            holdTimer = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("flush-time")) {
            flushTimer = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("prefix-list-in")) {
            cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such prefix list");
                return;
            }
            prflstIn = ntry.prflst;
            return;
        }
        if (a.equals("prefix-list-out")) {
            cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such prefix list");
                return;
            }
            prflstOut = ntry.prflst;
            return;
        }
        if (a.equals("route-map-in")) {
            cfgRoump ntry = cfgAll.rtmpFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such route map");
                return;
            }
            roumapIn = ntry.roumap;
            return;
        }
        if (a.equals("route-map-out")) {
            cfgRoump ntry = cfgAll.rtmpFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such route map");
                return;
            }
            roumapOut = ntry.roumap;
            return;
        }
        if (a.equals("route-policy-in")) {
            cfgRouplc ntry = cfgAll.rtplFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such route policy");
                return;
            }
            roupolIn = ntry.rouplc;
            return;
        }
        if (a.equals("route-policy-out")) {
            cfgRouplc ntry = cfgAll.rtplFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such route policy");
                return;
            }
            roupolOut = ntry.rouplc;
            return;
        }
        cmd.badCmd();
    }

    /**
     * undo configuration
     *
     * @param a command
     * @param cmd parameters
     */
    public void routerUnConfig(String a, cmds cmd) {
        if (a.equals("allow-rx")) {
            allowRx = false;
            return;
        }
        if (a.equals("allow-tx")) {
            allowTx = false;
            return;
        }
        if (a.equals("bfd")) {
            bfdTrigger = false;
            return;
        }
        if (a.equals("default-originate")) {
            defOrigin = false;
            return;
        }
        if (a.equals("suppress-prefix")) {
            suppressAddr = false;
            return;
        }
        if (a.equals("unsuppress-prefix")) {
            unsuppressAddr = false;
            return;
        }
        if (a.equals("verify-source")) {
            connectedCheck = false;
            return;
        }
        if (a.equals("poison-reverse")) {
            poisonReverse = false;
            return;
        }
        if (a.equals("split-horizon")) {
            splitHorizon = false;
            return;
        }
        if (a.equals("prefix-list-in")) {
            prflstIn = null;
            return;
        }
        if (a.equals("prefix-list-out")) {
            prflstOut = null;
            return;
        }
        if (a.equals("route-map-in")) {
            roumapIn = null;
            return;
        }
        if (a.equals("route-map-out")) {
            roumapOut = null;
            return;
        }
        if (a.equals("route-policy-in")) {
            roupolIn = null;
            return;
        }
        if (a.equals("route-policy-out")) {
            roupolOut = null;
            return;
        }
        cmd.badCmd();
    }

    /**
     * get help text
     *
     * @param l list to update
     */
    public static void routerGetHelp(userHelp l) {
        l.add(null, false, 4, new int[]{-1}, "enable", "enable/disable rip updates");
        l.add(null, false, 4, new int[]{-1}, "allow-rx", "suppress processing routing updates");
        l.add(null, false, 4, new int[]{-1}, "allow-tx", "suppress sending routing updates");
        l.add(null, false, 4, new int[]{-1}, "bfd", "enable bfd triggered down");
        l.add(null, false, 4, new int[]{-1}, "default-originate", "send default route to peer");
        l.add(null, false, 4, new int[]{-1}, "suppress-prefix", "do not advertise interface");
        l.add(null, false, 4, new int[]{-1}, "unsuppress-prefix", "do advertise interface");
        l.add(null, false, 4, new int[]{-1}, "verify-source", "check source address of updates");
        l.add(null, false, 4, new int[]{-1}, "poison-reverse", "advertise back best routes");
        l.add(null, false, 4, new int[]{-1}, "split-horizon", "dont advertise back on rx interface");
        l.add(null, false, 4, new int[]{5}, "distance", "administrative distance of routes");
        l.add(null, false, 5, new int[]{-1}, "<num>", "set administrative distance");
        l.add(null, false, 4, new int[]{5}, "metric-out", "metric of outgoing routes");
        l.add(null, false, 5, new int[]{-1}, "<num>", "set metric");
        l.add(null, false, 4, new int[]{5}, "metric-in", "metric of incoming routes");
        l.add(null, false, 5, new int[]{-1}, "<num>", "set metric");
        l.add(null, false, 4, new int[]{5}, "packet-timer", "inter packet gap time");
        l.add(null, false, 5, new int[]{-1}, "<num>", "time in ms");
        l.add(null, false, 4, new int[]{5}, "update-timer", "time between updates");
        l.add(null, false, 5, new int[]{-1}, "<num>", "time in ms");
        l.add(null, false, 4, new int[]{5}, "hold-time", "hold prefixes in routing table");
        l.add(null, false, 5, new int[]{-1}, "<num>", "time in ms");
        l.add(null, false, 4, new int[]{5}, "flush-time", "flush prefix after inactivity");
        l.add(null, false, 5, new int[]{-1}, "<num>", "time in ms");
        l.add(null, false, 4, new int[]{5}, "route-map-in", "process prefixes in ingress updates");
        l.add(null, false, 5, new int[]{-1}, "<name:rm>", "name of route map");
        l.add(null, false, 4, new int[]{5}, "route-map-out", "process prefixes in egress updates");
        l.add(null, false, 5, new int[]{-1}, "<name:rm>", "name of route map");
        l.add(null, false, 4, new int[]{5}, "route-policy-in", "process prefixes in ingress updates");
        l.add(null, false, 5, new int[]{-1}, "<name:rpl>", "name of route policy");
        l.add(null, false, 4, new int[]{5}, "route-policy-out", "process prefixes in egress updates");
        l.add(null, false, 5, new int[]{-1}, "<name:rpl>", "name of route policy");
        l.add(null, false, 4, new int[]{5}, "prefix-list-in", "filter prefixes in ingress updates");
        l.add(null, false, 5, new int[]{-1}, "<name:pl>", "name of prefix list");
        l.add(null, false, 4, new int[]{5}, "prefix-list-out", "filter prefixes in egress updates");
        l.add(null, false, 5, new int[]{-1}, "<name:pl>", "name of prefix list");
    }

    private void createRIPheader(int cmd, packHolder pck) {
        pck.clear();
        pck.putStart();
        pck.putByte(0, cmd); // command
        pck.putByte(1, rtrRip6.version); // version
        pck.msbPutW(2, 0); // reserved
        pck.putSkip(rtrRip6.sizeHead); // size of header
    }

    private void createRIPupdate(tabRouteEntry<addrIP> ntry, packHolder pck) {
        pck.putAddr(0, ntry.prefix.network.toIPv6()); // network
        pck.msbPutW(16, ntry.best.tag); // route tag
        pck.putByte(18, ntry.prefix.mask.toIPv6().toNetmask()); // subnet mask
        int i = ntry.best.metric + metricOut;
        if (i > rtrRip6.metricMax) {
            i = rtrRip6.metricMax;
        }
        if (i < rtrRip6.metricMin) {
            i = rtrRip6.metricMin;
        }
        ntry.best.metric = i;
        pck.putByte(19, i); // metric
        pck.putSkip(rtrRip6.sizeNtry);
        if (debugger.rtrRip6traf) {
            logger.debug("txnet " + ntry);
        }
    }

    /**
     * send one update over a connection
     *
     * @param conn connection to use
     */
    protected void sendOutUpdates(prtGenConn conn) {
        packHolder pck = new packHolder(true, true);
        tabRoute<addrIP> tab1 = new tabRoute<addrIP>("copy");
        if (defOrigin) {
            tab1.add(tabRoute.addType.better, addrPrefix.ip6toIP(addrPrefix.defaultRoute6()), new addrIP());
        }
        tab1.mergeFrom(tabRoute.addType.better, lower.routerComputedU, tabRouteAttr.distanLim);
        if (splitHorizon) {
            if (poisonReverse) {
                for (int i = 0; i < tab1.size(); i++) {
                    tabRouteEntry<addrIP> ntry = tab1.get(i);
                    if (ntry.best.iface != conn.iface) {
                        continue;
                    }
                    ntry.best.metric = rtrRip6.metricMax;
                }
            } else {
                tab1.delIface(conn.iface);
            }
        }
        tab1.mergeFrom(tabRoute.addType.better, lower.routerRedistedU, tabRouteAttr.distanLim);
        tabRoute<addrIP> tab2 = new tabRoute<addrIP>("copy");
        tabRoute.addUpdatedTable(tabRoute.addType.better, rtrBgpUtil.sfiUnicast, 0, tab2, tab1, true, roumapOut, roupolOut, prflstOut);
        if (debugger.rtrRip6traf) {
            logger.debug("tx " + conn);
        }
        createRIPheader(2, pck);
        int entries = 0;
        for (int i = 0; i < tab2.size(); i++) {
            tabRouteEntry<addrIP> ntry = tab2.get(i);
            if (ntry == null) {
                continue;
            }
            if (ntry.best.iface != conn.iface) {
                ntry.best.nextHop = null;
            }
            createRIPupdate(ntry, pck);
            entries++;
            if (entries < rtrRip6.entryPerUpdate) {
                continue;
            }
            conn.send2net(pck);
            createRIPheader(2, pck);
            entries = 0;
            bits.sleep(interPackTime);
        }
        if (entries > 0) {
            conn.send2net(pck);
        }
    }

    /**
     * do some work round
     */
    public synchronized void doWork() {
        if (conn == null) {
            return;
        }
        conn.workInterval = updateTimer;
        conn.timeout = 0;
        if (!allowTx) {
            return;
        }
        sendOutUpdates(conn);
    }

}
