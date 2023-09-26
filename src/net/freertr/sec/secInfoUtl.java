package net.freertr.sec;

import java.util.ArrayList;
import java.util.List;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrPrefix;
import net.freertr.cfg.cfgAll;
import net.freertr.cfg.cfgRtr;
import net.freertr.cfg.cfgVrf;
import net.freertr.clnt.clntPmtud;
import net.freertr.enc.enc7bit;
import net.freertr.ip.ipFwd;
import net.freertr.ip.ipRtr;
import net.freertr.pipe.pipeLine;
import net.freertr.pipe.pipeSide;
import net.freertr.tab.tabRoute;
import net.freertr.tab.tabRouteAttr;
import net.freertr.tab.tabRouteEntry;
import net.freertr.tab.tabRouteUtil;
import net.freertr.user.userFormat;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;
import net.freertr.util.logger;
import net.freertr.util.verCore;

/**
 * generic information utils
 *
 * @author matecsaba
 */
public class secInfoUtl {

    private secInfoUtl() {
    }

    /**
     * find one route
     *
     * @param adr address to look up
     * @param rtr router to use
     * @param fwd forwarder to use
     * @return route entry, null if nothing
     */
    public final static tabRouteEntry<addrIP> findOneRoute(addrIP adr, ipRtr rtr, ipFwd fwd) {
        if (adr == null) {
            return null;
        }
        if (fwd == null) {
            return null;
        }
        if (rtr == null) {
            return null;
        }
        tabRouteEntry<addrIP> ntry = rtr.routerComputedU.route(adr);
        if (ntry == null) {
            logger.warn("no route " + rtr + " " + adr);
            return null;
        }
        ntry = ntry.copyBytes(tabRoute.addType.alters);
        return ntry;
    }

    /**
     * do config knob
     *
     * @param cfg config to use
     * @param cmd command to interpret
     * @param negated command was negated
     * @return null if cleared, config on success
     */
    public final static secInfoCfg doCfgStr(secInfoCfg cfg, cmds cmd, boolean negated) {
        if (cfg == null) {
            cfg = new secInfoCfg();
        }
        String s = cmd.word();
        if (s.equals("script")) {
            if (negated) {
                cfg.script = null;
                doSanityChecks(cfg);
                return cfg;
            }
            cfg.script = cfgAll.scrptFind(cmd.word(), false);
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("pmtud")) {
            if (negated) {
                cfg.pmtudMin = 0;
                cfg.pmtudMax = 0;
                cfg.pmtudTim = 0;
                return cfg;
            }
            cfg.pmtudMin = bits.str2num(cmd.word());
            cfg.pmtudMax = bits.str2num(cmd.word());
            cfg.pmtudTim = bits.str2num(cmd.word());
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("resolve")) {
            cfg.resolve = !negated;
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("others")) {
            cfg.others = !negated;
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("tinyhttp")) {
            cfg.tinyHttp = !negated;
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("details")) {
            cfg.details = !negated;
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("single")) {
            cfg.single = !negated;
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("hacked")) {
            cfg.hacked = !negated;
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("plain")) {
            cfg.plain = !negated;
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("justip")) {
            cfg.justip = !negated;
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("style")) {
            if (negated) {
                cfg.style = null;
                return cfg;
            }
            cfg.style = cmd.getRemaining();
            return cfg;
        }
        if (s.equals("format")) {
            if (negated) {
                cfg.format = userFormat.tableMode.normal;
                return cfg;
            }
            s = cmd.word();
            cfg.format = userFormat.str2tabmod(s);
            return cfg;
        }
        if (s.equals("router4")) {
            if (negated) {
                cfg.router4 = null;
                cfg.fwder4 = null;
                doSanityChecks(cfg);
                return cfg;
            }
            tabRouteAttr.routeType o = cfgRtr.name2num(cmd.word());
            int i = bits.str2num(cmd.word());
            cfgRtr rtr = cfgAll.rtrFind(o, i, false);
            if (rtr == null) {
                cmd.error("no such router process");
                return cfg;
            }
            if (rtr.fwd == null) {
                cmd.error("router have no vrf");
                return cfg;
            }
            cfg.router4 = rtr.getRouter();
            cfg.fwder4 = rtr.fwd;
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("router6")) {
            if (negated) {
                cfg.router6 = null;
                cfg.fwder6 = null;
                doSanityChecks(cfg);
                return cfg;
            }
            tabRouteAttr.routeType o = cfgRtr.name2num(cmd.word());
            int i = bits.str2num(cmd.word());
            cfgRtr rtr = cfgAll.rtrFind(o, i, false);
            if (rtr == null) {
                cmd.error("no such router process");
                return cfg;
            }
            if (rtr.fwd == null) {
                cmd.error("router have no vrf");
                return cfg;
            }
            cfg.router6 = rtr.getRouter();
            cfg.fwder6 = rtr.fwd;
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("rd")) {
            if (negated) {
                cfg.rd = 0;
                doSanityChecks(cfg);
                return cfg;
            }
            s = cmd.word();
            cfg.rd = tabRouteUtil.string2rd(s);
            doSanityChecks(cfg);
            return cfg;
        }
        if (s.equals("vrf")) {
            if (negated) {
                cfg.rd = 0;
                doSanityChecks(cfg);
                return cfg;
            }
            s = cmd.word();
            cfgVrf ntry = cfgAll.vrfFind(s, false);
            if (ntry == null) {
                cmd.error("no such vrf");
                return cfg;
            }
            cfg.fwder4 = ntry.fwd4;
            cfg.fwder6 = ntry.fwd6;
            cfg.rd = cfg.fwder4.rd;
            cfg.rd = cfg.fwder6.rd;
            doSanityChecks(cfg);
            return cfg;
        }
        cmd.badCmd();
        return cfg;
    }

    /**
     * find one router
     *
     * @param adr address to check
     * @param rtr4 ipv4 candidate
     * @param rtr6 ipv6 candidate
     * @return proper one, null if nothing
     */
    public final static ipRtr findOneRtr(addrIP adr, ipRtr rtr4, ipRtr rtr6) {
        if (adr == null) {
            return null;
        }
        if (adr.isIPv4()) {
            return rtr4;
        } else {
            return rtr6;
        }
    }

    /**
     * perform the work
     *
     * @param cfg configuration to use
     * @param vrf vrf to use
     * @param trg target address
     * @param src source address
     * @return work done, null if error happened
     */
    public final static clntPmtud doPmtud(secInfoCfg cfg, ipFwd vrf, addrIP trg, addrIP src) {
        if (cfg == null) {
            return null;
        }
        if (vrf == null) {
            return null;
        }
        if (cfg.pmtudTim < 1) {
            return null;
        }
        if (cfg.pmtudMin >= cfg.pmtudMax) {
            return null;
        }
        logger.info("pmtuding " + vrf + " " + trg + " " + src);
        pipeLine pl = new pipeLine(65536, true);
        pipeSide ps = pl.getSide();
        clntPmtud pm = new clntPmtud(ps, trg, vrf, src);
        pm.min = cfg.pmtudMin;
        pm.max = cfg.pmtudMax;
        pm.delay = 100;
        pm.timeout = cfg.pmtudTim;
        pm.doer();
        if (pm.doer() == null) {
            logger.error("failed pmtud " + vrf + " " + trg + " " + src);
            return null;
        }
        logger.info("pmtuded " + vrf + " " + trg + " " + src + " " + pm);
        return pm;
    }

    /**
     * one liner of the route
     *
     * @param fwd forwarder to use
     * @param rtr router to use
     * @param ntry route entry
     * @return one liner of the route
     */
    public final static String getRoute1liner(ipFwd fwd, ipRtr rtr, tabRouteEntry<addrIP> ntry) {
        if (ntry == null) {
            return noRoute;
        }
        return fwd.vrfName + " " + rtr.routerComputedU.size()
                + " pfx=" + addrPrefix.ip2str(ntry.prefix)
                + " rd=" + tabRouteUtil.rd2string(ntry.rouDst)
                + " pth=" + ntry.best.asPathStr().trim()
                + " inf=" + ntry.best.asInfoStr().trim()
                + " nam=" + ntry.best.asNameStr().trim();
    }

    private final static String noRoute = "route not found";

    /**
     * get route in details
     *
     * @param fwd forwarder to use
     * @param ntry route entry
     * @param tm table mode
     * @param hck hacker voiced
     * @return text representing the route
     */
    public final static List<String> getRouteDetails(ipFwd fwd, tabRouteEntry<addrIP> ntry, userFormat.tableMode tm, boolean hck) {
        if (ntry == null) {
            return bits.str2lst(noRoute);
        }
        userFormat res = ntry.fullDump("", fwd);
        List<String> lst = res.formatAll(tm);
        if (!hck) {
            return lst;
        }
        lst = enc7bit.toHackedLst(lst);
        return lst;
    }

    /**
     * get route as bytes
     *
     * @param lst list to convert
     * @return converted list
     */
    public final static byte[] getRouteAscii(List<String> lst) {
        if (lst == null) {
            return new byte[0];
        }
        int lss = lst.size();
        List<Integer> res = new ArrayList<Integer>();
        byte[] buf = null;
        for (int o = 0; o < lss; o++) {
            String a = lst.get(o);
            if (a == null) {
                a = "";
            }
            a = enc7bit.decodeExtStr(a);
            buf = a.getBytes();
            for (int i = 0; i < buf.length; i++) {
                int p = (int) buf[i];
                res.add(p);
            }
            res.add(13);
            res.add(10);
        }
        buf = new byte[res.size()];
        for (int i = 0; i < buf.length; i++) {
            int o = res.get(i);
            buf[i] = (byte) o;
        }
        return buf;
    }

    /**
     * get configuration
     *
     * @param lst list to append
     * @param cfg config to use
     * @param beg beginning
     */
    public final static void getConfig(List<String> lst, secInfoCfg cfg, String beg) {
        if (cfg == null) {
            return;
        }
        if (cfg.pmtudTim > 1) {
            lst.add(beg + "pmtud " + cfg.pmtudMin + " " + cfg.pmtudMax + " " + cfg.pmtudTim);
        }
        if (cfg.router4 != null) {
            lst.add(beg + "router4 " + cfg.router4.routerGetName());
        }
        if (cfg.router6 != null) {
            lst.add(beg + "router6 " + cfg.router6.routerGetName());
        }
        if (cfg.details) {
            lst.add(beg + "details");
        }
        if (cfg.single) {
            lst.add(beg + "single");
        }
        if (cfg.hacked) {
            lst.add(beg + "hacked");
        }
        if (cfg.plain) {
            lst.add(beg + "plain");
        }
        if (cfg.justip) {
            lst.add(beg + "justip");
        }
        if (cfg.style != null) {
            lst.add(beg + "style " + cfg.style);
        }
        if (cfg.format != userFormat.tableMode.normal) {
            lst.add(beg + "format " + userFormat.tabmod2str(cfg.format));
        }
        if (cfg.rd != 0) {
            lst.add(beg + "rd " + tabRouteUtil.rd2string(cfg.rd));
        }
        if (cfg.script != null) {
            lst.add(beg + "script " + cfg.script.name);
        }
        if (cfg.resolve) {
            lst.add(beg + "resolve");
        }
        if (cfg.tinyHttp) {
            lst.add(beg + "tinyhttp");
        }
        if (cfg.others) {
            lst.add(beg + "others");
        }
    }

    /**
     * get help messages
     *
     * @param lst help text to update
     * @param tab base level
     * @param beg beginning
     */
    public final static void getHelp(userHelping lst, int tab, String beg) {
        lst.add(null, (tab + 0) + " " + (tab + 1) + "  " + beg + "                  test ip info before accepting");
        lst.add(null, (tab + 1) + " " + (tab + 2) + "  router4                      lookup addresses");
        cfgRtr.getRouterList(lst, tab, "");
        lst.add(null, (tab + 3) + " .         <num:rtr>       process id");
        lst.add(null, (tab + 1) + " " + (tab + 2) + "  router6                      lookup addresses");
        cfgRtr.getRouterList(lst, tab, "");
        lst.add(null, (tab + 3) + " .         <num:rtr>       process id");
        lst.add(null, (tab + 1) + " " + (tab + 2) + "  rd                           rd to use");
        lst.add(null, (tab + 2) + " .    <rd>                       rd in ASnum:IDnum format");
        lst.add(null, (tab + 1) + " " + (tab + 2) + "  vrf                          vrf to use");
        lst.add(null, (tab + 2) + " .    <name:vrf>                 name of table");
        lst.add(null, (tab + 1) + " " + (tab + 2) + "  script                       script to execute");
        lst.add(null, (tab + 2) + " .    <name:scr>                 script name");
        lst.add(null, (tab + 1) + " " + (tab + 2) + "  style                        colorize prefix details");
        lst.add(null, (tab + 2) + " .    <str>                      string to send");
        lst.add(null, (tab + 1) + " " + (tab + 2) + "  format                       format prefix details");
        lst.add(null, (tab + 2) + " .    normal                     select normal mode");
        lst.add(null, (tab + 2) + " .    table                      select table mode");
        lst.add(null, (tab + 2) + " .    fancy                      select fancy mode");
        lst.add(null, (tab + 2) + " .    csv                        select csv mode");
        lst.add(null, (tab + 2) + " .    raw                        select raw mode");
        lst.add(null, (tab + 2) + " .    html                       select html mode");
        lst.add(null, (tab + 1) + " " + (tab + 2) + "  pmtud                    test pmtud before accepting");
        lst.add(null, (tab + 2) + " " + (tab + 3) + "  <num>                      min mtu");
        lst.add(null, (tab + 3) + " " + (tab + 4) + "    <num>                    max mtu");
        lst.add(null, (tab + 4) + " .    <num>                    timeout per round");
        lst.add(null, (tab + 1) + " .  details                      print prefix details");
        lst.add(null, (tab + 1) + " .  single                       print prefix summary");
        lst.add(null, (tab + 1) + " .  hacked                       hackerize prefix details");
        lst.add(null, (tab + 1) + " .  plain                        plain prefix details");
        lst.add(null, (tab + 1) + " .  justip                       just address headline");
        lst.add(null, (tab + 1) + " .  resolve                      resolve addresses");
        lst.add(null, (tab + 1) + " .  tinyhttp                     pretend http server");
        lst.add(null, (tab + 1) + " .  others                       allow any addresses");
    }

    /**
     * preform style checks
     *
     * @param a string
     * @return null on error, style if ok
     */
    public final static String doSanityStyle(String a) {
        if (a == null) {
            return null;
        }
        if (a.indexOf("\"") >= 0) {
            return null;
        }
        if (a.indexOf("\\") >= 0) {
            return null;
        }
        if (a.indexOf(">") >= 0) {
            return null;
        }
        return a;
    }

    /**
     * perform sanity checks
     *
     * @param cfg config to repair
     * @return changes made
     */
    public final static int doSanityChecks(secInfoCfg cfg) {
        int chg = 0;
        if (cfg.pmtudMin >= cfg.pmtudMax) {
            cfg.pmtudMin = 0;
            cfg.pmtudMax = 0;
            cfg.pmtudTim = 0;
            chg++;
        }
        if (cfg.router4 == null) {
            cfg.fwder4 = null;
            chg++;
        }
        if (cfg.router6 == null) {
            cfg.fwder6 = null;
            chg++;
        }
        if ((cfg.router4 != null) && (cfg.router6 == null)) {
            cfg.rd = 0;
            chg++;
        }
        if (cfg.style != null) {
            String a = doSanityStyle(cfg.style);
            if (a.length() < 1) {
                cfg.style = null;
                chg++;
                a = null;
            }
            cfg.style = a;
        }
        if (!verCore.release) {
            return chg;
        }
        cfg.script = null;
        chg++;
        cfg.others = false;
        chg++;
        return chg;
    }

    /**
     * find one forwarder
     *
     * @param adr address to check
     * @param fwd4 ipv4 candidate
     * @param fwd6 ipv6 candidate
     * @return proper one, null if nothing
     */
    public final static ipFwd findOneFwd(addrIP adr, ipFwd fwd4, ipFwd fwd6) {
        if (adr == null) {
            return null;
        }
        if (adr.isIPv4()) {
            return fwd4;
        } else {
            return fwd6;
        }
    }

}
