package net.freertr.rtr;

import java.util.ArrayList;
import java.util.List;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrIPv4;
import net.freertr.addr.addrPrefix;
import net.freertr.auth.authLocal;
import net.freertr.cfg.cfgAll;
import net.freertr.cfg.cfgIfc;
import net.freertr.clnt.clntPmtudCfg;
import net.freertr.ip.ipFwd;
import net.freertr.ip.ipFwdIface;
import net.freertr.ip.ipFwdMcast;
import net.freertr.ip.ipFwdTab;
import net.freertr.ip.ipRtr;
import net.freertr.prt.prtTcp;
import net.freertr.tab.tabGen;
import net.freertr.tab.tabIndex;
import net.freertr.tab.tabRoute;
import net.freertr.tab.tabRouteEntry;
import net.freertr.user.userFormat;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;

/**
 * multicast source discovery (rfc3618) protocol
 *
 * @author matecsaba
 */
public class rtrMsdp extends ipRtr {

    /**
     * port to use
     */
    public final static int port = 639;

    /**
     * the forwarder protocol
     */
    protected ipFwd fwdCore;

    /**
     * the tcp protocol
     */
    protected prtTcp tcpCore;

    /**
     * list of neighbors
     */
    protected tabGen<rtrMsdpNeigh> neighs = new tabGen<rtrMsdpNeigh>();

    /**
     * router number
     */
    protected int rtrNum;

    /**
     * accepted sas
     */
    public tabGen<ipFwdMcast> cache = new tabGen<ipFwdMcast>();

    /**
     * create bgp process
     *
     * @param forwarder forwarder to update
     * @param protocol tcp protocol to use
     * @param id process id
     */
    public rtrMsdp(ipFwd forwarder, prtTcp protocol, int id) {
        fwdCore = forwarder;
        tcpCore = protocol;
        rtrNum = id;
    }

    /**
     * convert to string
     *
     * @return string
     */
    public String toString() {
        return "msdp on " + fwdCore;
    }

    /**
     * get neighbor count
     *
     * @return count
     */
    public int routerNeighCount() {
        return neighs.size();
    }

    /**
     * list neighbors
     *
     * @param tab list
     */
    public void routerNeighList(tabRoute<addrIP> tab) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrMsdpNeigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            tabRouteEntry<addrIP> ntry = new tabRouteEntry<addrIP>();
            ntry.prefix = new addrPrefix<addrIP>(nei.peer, addrIP.size * 8);
            tabRoute.addUpdatedEntry(tabRoute.addType.better, tab, rtrBgpUtil.sfiUnicast, 0, ntry, true, null, null, routerAutoMesh);
        }
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

    private int getIface(addrIP adr) {
        ipFwdIface ifc = ipFwdTab.findSendingIface(fwdCore, adr);
        if (ifc == null) {
            return 0;
        }
        return ifc.ifwNum;
    }

    /**
     * create computed
     */
    public synchronized void routerCreateComputed() {
        tabGen<ipFwdMcast> lst = new tabGen<ipFwdMcast>();
        for (int o = 0; o < neighs.size(); o++) {
            rtrMsdpNeigh nei = neighs.get(o);
            if (nei == null) {
                continue;
            }
            int ifc = getIface(nei.peer);
            if (ifc == 0) {
                continue;
            }
            for (int i = 0; i < nei.learned.size(); i++) {
                ipFwdMcast ntry = nei.learned.get(i);
                if (getIface(ntry.upstream) != ifc) {
                    continue;
                }
                lst.add(ntry);
            }
        }
        cache = lst;
        routerComputedU = new tabRoute<addrIP>("rx");
        routerComputedM = new tabRoute<addrIP>("rx");
        routerComputedF = new tabRoute<addrIP>("rx");
        routerComputedI = new tabGen<tabIndex<addrIP>>();
    }

    /**
     * redistribution changed
     */
    public void routerRedistChanged() {
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
        List<String> neis = new ArrayList<String>();
        for (int i = 0; i < neighs.size(); i++) {
            rtrMsdpNeigh ntry = neighs.get(i);
            neis.add("" + ntry.peer);
        }
        l.add(null, "1 2   neighbor                    specify neighbor parameters");
        l.add(neis, "2 3     <addr:loc>                address of peer");
        l.add(null, "3 .       enable                  enable this peer");
        l.add(null, "3 4       description             describe this neighbor");
        l.add(null, "4 4,.       <text>                description of neighbor");
        l.add(null, "3 4       password                set session password");
        l.add(null, "4 .         <text>                tcp password");
        l.add(null, "3 4       update-source           connection source for this peer");
        l.add(null, "4 .         <name:ifc>            name of interface");
        l.add(null, "3 4       timer                   neighbor keepalive times");
        l.add(null, "4 5         <num>                 keepalive in ms");
        l.add(null, "5 6           <num>               hold time in ms");
        l.add(null, "6 7             <num>             refresh time in ms");
        l.add(null, "7 .               <num>           flush time in ms");
        l.add(null, "3  4       pmtud                  test pmtud before accepting");
        l.add(null, "4  5         <num>                min mtu");
        l.add(null, "5  6           <num>              max mtu");
        l.add(null, "6  .             <num>            timeout per round");
        l.add(null, "3 .       shutdown                connection disabled for this peer");
        l.add(null, "3 .       bfd                     enable bfd triggered down");
    }

    /**
     * get config
     *
     * @param l list
     * @param beg beginning
     * @param filter filter
     */
    public void routerGetConfig(List<String> l, String beg, int filter) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrMsdpNeigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.getCfg(l, beg, filter);
        }
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
        if (!s.equals("neighbor")) {
            return true;
        }
        rtrMsdpNeigh ntry = new rtrMsdpNeigh(this);
        if (ntry.peer.fromString(cmd.word())) {
            cmd.error("bad address");
            return false;
        }
        s = cmd.word();
        if (s.equals("enable")) {
            if (negated) {
                ntry = neighs.del(ntry);
                if (ntry == null) {
                    return false;
                }
                ntry.stopNow();
                return false;
            }
            if (neighs.add(ntry) != null) {
                return false;

            }
            ntry.startNow();
            return false;
        }
        ntry = neighs.find(ntry);
        if (ntry == null) {
            cmd.error("no such neighbor");
            return false;
        }
        if (s.equals("update-source")) {
            if (negated) {
                ntry.srcIface = null;
                return false;
            }
            cfgIfc res = cfgAll.ifcFind(cmd.word(), 0);
            if (res == null) {
                cmd.error("no such interface");
                return false;
            }
            if (res.vrfFor == null) {
                cmd.error("not in vrf");
                return false;
            }
            if (res.vrfFor.getFwd(ntry.peer) != fwdCore) {
                cmd.error("in other vrf");
                return false;
            }
            ntry.srcIface = res;
            return false;
        }
        if (s.equals("password")) {
            if (negated) {
                ntry.passwd = null;
                return false;
            }
            ntry.passwd = authLocal.passwdDecode(cmd.getRemaining());
            return false;
        }
        if (s.equals("description")) {
            if (negated) {
                ntry.description = null;
                return false;
            }
            ntry.description = cmd.getRemaining();
            return false;
        }
        if (s.equals("timer")) {
            ntry.keepAlive = bits.str2num(cmd.word());
            ntry.holdTimer = bits.str2num(cmd.word());
            ntry.freshTimer = bits.str2num(cmd.word());
            ntry.flushTimer = bits.str2num(cmd.word());
            return false;
        }
        if (s.equals("pmtud")) {
            ntry.pmtudCfg = new clntPmtudCfg();
            clntPmtudCfg.doCfgStr(ntry.pmtudCfg, cmd, negated);
            return false;
        }
        if (s.equals("shutdown")) {
            ntry.shutdown = !negated;
            if (!negated) {
                ntry.closeNow();
            }
            return false;
        }
        if (s.equals("bfd")) {
            ntry.bfdTrigger = !negated;
            return false;
        }
        return true;
    }

    /**
     * stop work
     */
    public void routerCloseNow() {
        for (int i = 0; i < neighs.size(); i++) {
            rtrMsdpNeigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.stopNow();
            ntry.closeNow();
        }
        fwdCore.routerDel(this);
    }

    /**
     * get neighbor show
     *
     * @return list of neighbors
     */
    public userFormat getNeighShow() {
        userFormat l = new userFormat("|", "address|learned|uptime");
        for (int i = 0; i < neighs.size(); i++) {
            rtrMsdpNeigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            l.add(ntry.peer + "|" + ntry.learned.size() + "|" + bits.timePast(ntry.upTime));
        }
        return l;
    }

    /**
     * get sources show
     *
     * @return list of sources
     */
    public userFormat getSourcesShow() {
        userFormat l = new userFormat("|", "source|group|upstream");
        for (int i = 0; i < cache.size(); i++) {
            ipFwdMcast ntry = cache.get(i);
            if (ntry == null) {
                continue;
            }
            l.add(ntry.source + "|" + ntry.group + "|" + ntry.upstream);
        }
        return l;
    }

    /**
     * find neighbor
     *
     * @param adr address
     * @return neighbor, null if not found
     */
    public rtrMsdpNeigh findPeer(addrIP adr) {
        rtrMsdpNeigh nei = new rtrMsdpNeigh(this);
        nei.peer.setAddr(adr);
        return neighs.find(nei);
    }

}
