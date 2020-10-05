package rtr;

import addr.addrIP;
import addr.addrPrefix;
import cfg.cfgAll;
import cfg.cfgPrfxlst;
import ip.ipCor4;
import ip.ipCor6;
import ip.ipFwd;
import ip.ipRtr;
import java.util.List;
import tab.tabListing;
import tab.tabPrfxlstN;
import tab.tabRoute;
import tab.tabRouteAttr;
import tab.tabRouteEntry;
import user.userHelping;
import util.bits;
import util.cmds;
import util.logger;

/**
 * blackhole generator
 *
 * @author matecsaba
 */
public class rtrBlackhole extends ipRtr implements Runnable {

    /**
     * the forwarder protocol
     */
    public final ipFwd fwdCore;

    /**
     * route type
     */
    public final tabRouteAttr.routeType rouTyp;

    /**
     * router number
     */
    public final int rtrNum;

    /**
     * protocol version
     */
    public final int proto;

    /**
     * distance to give
     */
    protected int distance;

    private tabListing<tabPrfxlstN, addrIP> whitelist;

    private int penalty = 60 * 1000;

    private final tabRoute<addrIP> entries = new tabRoute<addrIP>("ntry");

    private boolean working = true;

    /**
     * create blackhole process
     *
     * @param forwarder forwarder to update
     * @param id process id
     */
    public rtrBlackhole(ipFwd forwarder, int id) {
        fwdCore = forwarder;
        rtrNum = id;
        switch (fwdCore.ipVersion) {
            case ipCor4.protocolVersion:
                rouTyp = tabRouteAttr.routeType.blackhole4;
                proto = 4;
                break;
            case ipCor6.protocolVersion:
                rouTyp = tabRouteAttr.routeType.blackhole6;
                proto = 6;
                break;
            default:
                rouTyp = null;
                proto = 0;
                break;
        }
        distance = 254;
        routerComputedU = new tabRoute<addrIP>("rx");
        routerComputedM = new tabRoute<addrIP>("rx");
        routerComputedF = new tabRoute<addrIP>("rx");
        routerCreateComputed();
        fwdCore.routerAdd(this, rouTyp, id);
        new Thread(this).start();
    }

    /**
     * convert to string
     *
     * @return string
     */
    public String toString() {
        return "blackhole on " + fwdCore;
    }

    /**
     * create computed
     */
    public synchronized void routerCreateComputed() {
        tabRoute<addrIP> res = new tabRoute<addrIP>("computed");
        res.mergeFrom(tabRoute.addType.better, entries, null, true, tabRouteAttr.distanLim);
        routerDoAggregates(rtrBgpUtil.safiUnicast, res, null, fwdCore.commonLabel, 0, null, 0);
        res.preserveTime(routerComputedU);
        routerComputedU = res;
        fwdCore.routerChg(this);
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
        l.add("1  2      distance                   specify default distance");
        l.add("2  .        <num>                    distance");
        l.add("1  2      penalty                    specify time between runs");
        l.add("2  .        <num>                    milliseconds before aging");
        l.add("1  2      whitelist                  specify whitelist");
        l.add("2  .        <name>                   name of prefix list");
    }

    /**
     * get config
     *
     * @param l list
     * @param beg beginning
     * @param filter filter
     */
    public void routerGetConfig(List<String> l, String beg, boolean filter) {
        l.add(beg + "distance " + distance);
        l.add(beg + "penalty " + penalty);
        if (whitelist == null) {
            l.add(beg + "no whitelist");
        } else {
            l.add(beg + "whitelist " + whitelist.listName);
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
        if (s.equals("whitelist")) {
            if (negated) {
                whitelist = null;
                return false;
            }
            cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such list");
                return false;
            }
            whitelist = ntry.prflst;
            return false;
        }
        if (s.equals("penalty")) {
            penalty = bits.str2num(cmd.word());
            return false;
        }
        if (s.equals("distance")) {
            distance = bits.str2num(cmd.word());
            return false;
        }
        return true;
    }

    /**
     * stop work
     */
    public void routerCloseNow() {
        working = false;
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
     * neighbor list
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

    private void doRound() {
        long tim = bits.getTime() - penalty;
        int del = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            tabRouteEntry<addrIP> ntry = entries.get(i);
            if (ntry == null) {
                continue;
            }
            if (ntry.best.time > tim) {
                continue;
            }
            entries.del(ntry);
            del++;
        }
        if (del < 1) {
            return;
        }
        routerCreateComputed();
    }

    public void run() {
        for (;;) {
            if (!working) {
                return;
            }
            bits.sleep(penalty / 2);
            try {
                doRound();
            } catch (Exception e) {
                logger.traceback(e);
            }
        }
    }

    private boolean isWhitelisted(addrIP adr) {
        if (whitelist == null) {
            return false;
        }
        return whitelist.matches(rtrBgpUtil.safiUnicast, 0, new addrPrefix<addrIP>(adr, adr.maxBits()));
    }

    /**
     * check one address
     *
     * @param adr address
     * @return true if blocked, false if allowed
     */
    public boolean checkAddr(addrIP adr) {
        if (isWhitelisted(adr)) {
            return false;
        }
        tabRouteEntry<addrIP> ntry = entries.route(adr);
        if (ntry != null) {
            ntry.best.time = bits.getTime();
            return true;
        }
        ntry = fwdCore.actualU.route(adr);
        return ntry != null;
    }

    /**
     * block one address
     *
     * @param adr address
     */
    public void blockAddr(addrIP adr) {
        if (isWhitelisted(adr)) {
            return;
        }
        addrPrefix<addrIP> prf;
        if (proto == 4) {
            prf = new addrPrefix<addrIP>(adr, cfgAll.accessSubnet4);
        } else {
            prf = new addrPrefix<addrIP>(adr, cfgAll.accessSubnet6);
        }
        tabRouteEntry<addrIP> ntry = new tabRouteEntry<addrIP>();
        ntry.prefix = prf;
        ntry.best.time = bits.getTime();
        ntry.best.rouTyp = rouTyp;
        ntry.best.protoNum = rtrNum;
        ntry.best.distance = distance;
        entries.add(tabRoute.addType.always, ntry, false, false);
        routerCreateComputed();
    }

}
