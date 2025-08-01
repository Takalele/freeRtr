package org.freertr.rtr;

import java.util.List;
import org.freertr.addr.addrIP;
import org.freertr.addr.addrIPv4;
import org.freertr.auth.authLocal;
import org.freertr.cfg.cfgAll;
import org.freertr.cfg.cfgCert;
import org.freertr.cfg.cfgKey;
import org.freertr.cry.cryKeyDSA;
import org.freertr.cry.cryKeyECDSA;
import org.freertr.cry.cryKeyMLDSA;
import org.freertr.cry.cryKeyRSA;
import org.freertr.ip.ipFwdIface;
import org.freertr.pack.packHolder;
import org.freertr.prt.prtGenConn;
import org.freertr.prt.prtServP;
import org.freertr.sec.secInfoCfg;
import org.freertr.sec.secInfoUtl;
import org.freertr.serv.servGeneric;
import org.freertr.tab.tabAverage;
import org.freertr.tab.tabGen;
import org.freertr.user.userFormat;
import org.freertr.user.userHelp;
import org.freertr.util.bits;
import org.freertr.util.cmds;
import org.freertr.util.counter;
import org.freertr.util.debugger;
import org.freertr.util.logFil;
import org.freertr.util.logger;
import org.freertr.util.state;

/**
 * lsrp interface
 *
 * @author matecsaba
 */
public class rtrLsrpIface implements Comparable<rtrLsrpIface>, Runnable, prtServP {

    /**
     * ipinfo config
     */
    public secInfoCfg ipInfoCfg;

    /**
     * hello interval
     */
    public int helloTimer = 5000;

    /**
     * dead interval
     */
    public int deadTimer = 15000;

    /**
     * echo interval
     */
    public int echoTimer = 60000;

    /**
     * echo parameters
     */
    public tabAverage echoParam = new tabAverage(1, 100000);

    /**
     * default metric
     */
    public int metric = 10;

    /**
     * stub flag
     */
    public boolean stub = false;

    /**
     * unstub flag
     */
    public boolean unstub = false;

    /**
     * segment rou index
     */
    public int segrouIdx = -1;

    /**
     * segment rou pop
     */
    public boolean segrouPop = false;

    /**
     * bier index
     */
    public int bierIdx = -1;

    /**
     * bier subdomain
     */
    public int bierSub = -1;

    /**
     * affinity
     */
    public int affinity = 0;

    /**
     * srlg
     */
    public int srlg = 0;

    /**
     * bfd enabled
     */
    public int bfdTrigger = 0;

    /**
     * passive interface
     */
    public boolean passiveInt = false;

    /**
     * accept metric
     */
    public boolean acceptMetric = false;

    /**
     * dynamic metric
     */
    public int dynamicMetric = 0;

    /**
     * dynamic forbidden
     */
    public boolean dynamicForbid = false;

    /**
     * ldp metric syncrhonization
     */
    public boolean ldpSync = false;

    /**
     * suppress interface address
     */
    public boolean suppressAddr = false;

    /**
     * unsuppress interface address
     */
    public boolean unsuppressAddr = false;

    /**
     * check neighbor address is connected
     */
    public boolean connectedCheck = true;

    /**
     * tos used to send
     */
    public int sendingTos = -1;

    /**
     * ttl used to send
     */
    public int sendingTtl = -1;

    /**
     * authentication string
     */
    public String authentication = null;

    /**
     * disable authentication
     */
    public boolean authenDisable = false;

    /**
     * split horizon
     */
    public boolean splitHorizon = true;

    /**
     * database filter
     */
    public boolean databaseFilter = false;

    /**
     * rsa key to use
     */
    public cfgKey<cryKeyRSA> keyRsa = null;

    /**
     * dsa key to use
     */
    public cfgKey<cryKeyDSA> keyDsa = null;

    /**
     * ecdsa key to use
     */
    public cfgKey<cryKeyECDSA> keyEcDsa = null;

    /**
     * mldsa key to use
     */
    public cfgKey<cryKeyMLDSA> keyMlDsa = null;

    /**
     * rsa certificate to use
     */
    public cfgCert certRsa = null;

    /**
     * dsa certificate to use
     */
    public cfgCert certDsa = null;

    /**
     * ecdsa certificate to use
     */
    public cfgCert certEcDsa = null;

    /**
     * mldsa certificate to use
     */
    public cfgCert certMlDsa = null;

    /**
     * security method
     */
    public int encryptionMethod = 0;

    /**
     * dump file
     */
    public logFil dumpFile = null;

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
    protected final rtrLsrp lower;

    /**
     * list of neighbors
     */
    protected tabGen<rtrLsrpNeigh> neighs;

    private boolean need2run;

    /**
     * create one instance
     *
     * @param parent the rip protocol
     * @param ifc the ip interface to work on
     */
    public rtrLsrpIface(rtrLsrp parent, ipFwdIface ifc) {
        lower = parent;
        iface = ifc;
        neighs = new tabGen<rtrLsrpNeigh>();
    }

    /**
     * unregister from udp
     */
    public void unregister2udp() {
        lower.udpCore.listenStop(iface, rtrLsrp.port, null, 0);
        conn.setClosing();
        need2run = false;
    }

    /**
     * register to udp
     */
    public void register2udp() {
        addrIP adr = new addrIP();
        if (iface.addr.isIPv4()) {
            adr.fromString("224.0.0.228");
        } else {
            adr.fromString("ff02::228");
        }
        lower.udpCore.packetListen(this, iface, rtrLsrp.port, null, 0, "lsrp", -1, null, -1, -1);
        conn = lower.udpCore.packetConnect(this, iface, rtrLsrp.port, adr, rtrLsrp.port, "lsrp", -1, null, -1, -1);
        if (conn == null) {
            return;
        }
        conn.timeout = 0;
        need2run = true;
        new Thread(this).start();
    }

    /**
     * list of neighbors
     *
     * @param res list to update
     * @param brief only briefly
     */
    protected void showNeighs(userFormat res, boolean brief) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrLsrpNeigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            if (brief) {
                res.add(nei.rtrId + "|" + nei.name + "|" + nei.isReady() + "|" + bits.timePast(nei.upTime));
            } else {
                res.add(iface + "|" + nei.rtrId + "|" + nei.name + "|" + nei.inam + "|" + nei.peer + "|" + nei.isReady() + "|" + bits.timePast(nei.upTime));
            }
        }
    }

    /**
     * list of neighbors
     *
     * @param res list to update
     */
    protected void showMetrics(userFormat res) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrLsrpNeigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            res.add(iface + "|" + nei.rtrId + "|" + nei.name + "|" + nei.peer + "|" + nei.getMetric() + "|" + nei.gotMetric + "|" + nei.echoCalc);
        }
    }

    /**
     * find one neighbor
     *
     * @param adr address of peer
     * @return neighbor, null if not found
     */
    protected rtrLsrpNeigh findNeigh(addrIP adr) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrLsrpNeigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            if (adr.compareTo(nei.peer) == 0) {
                return nei;
            }
        }
        return null;
    }

    /**
     * get configuration
     *
     * @param l list to add
     * @param beg beginning
     * @param filter filter defaults
     */
    public void routerGetConfig(List<String> l, String beg, int filter) {
        l.add(cmds.tabulator + beg + "enable");
        if (dumpFile == null) {
            l.add(cmds.tabulator + cmds.negated + cmds.tabulator + beg + "dump");
        } else {
            String a = dumpFile.rotate2();
            if (a == null) {
                a = "";
            } else {
                a = " " + a;
            }
            l.add(cmds.tabulator + beg + "dump " + dumpFile.name() + a);
        }
        String a = "";
        if (segrouPop) {
            a = " pop";
        }
        cmds.cfgLine(l, segrouIdx < 0, cmds.tabulator, beg + "segrout", "" + segrouIdx + a);
        cmds.cfgLine(l, bierIdx < 0, cmds.tabulator, beg + "bier", bierIdx + " " + bierSub);
        cmds.cfgLine(l, !splitHorizon, cmds.tabulator, beg + "split-horizon", "");
        cmds.cfgLine(l, !databaseFilter, cmds.tabulator, beg + "database-filter", "");
        cmds.cfgLine(l, !passiveInt, cmds.tabulator, beg + "passive", "");
        cmds.cfgLine(l, !acceptMetric, cmds.tabulator, beg + "accept-metric", "");
        a = "";
        if (bfdTrigger == 2) {
            a = "strict";
        }
        cmds.cfgLine(l, bfdTrigger < 1, cmds.tabulator, beg + "bfd", a);
        cmds.cfgLine(l, !stub, cmds.tabulator, beg + "stub", "");
        cmds.cfgLine(l, !unstub, cmds.tabulator, beg + "unstub", "");
        cmds.cfgLine(l, !suppressAddr, cmds.tabulator, beg + "suppress-prefix", "");
        cmds.cfgLine(l, !unsuppressAddr, cmds.tabulator, beg + "unsuppress-prefix", "");
        cmds.cfgLine(l, !connectedCheck, cmds.tabulator, beg + "verify-source", "");
        cmds.cfgLine(l, encryptionMethod <= 0, cmds.tabulator, beg + "encryption", servGeneric.proto2string(encryptionMethod) + " " + keyRsa + " " + keyDsa + " " + keyEcDsa + " " + keyMlDsa + " " + certRsa + " " + certDsa + " " + certEcDsa + " " + certMlDsa);
        cmds.cfgLine(l, authentication == null, cmds.tabulator, beg + "password", authLocal.passwdEncode(authentication, (filter & 2) != 0));
        cmds.cfgLine(l, !authenDisable, cmds.tabulator, beg + "disable-password", "");
        l.add(cmds.tabulator + beg + "sending-tos " + sendingTos);
        l.add(cmds.tabulator + beg + "sending-ttl " + sendingTtl);
        l.add(cmds.tabulator + beg + "metric " + metric);
        l.add(cmds.tabulator + beg + "affinity " + affinity);
        l.add(cmds.tabulator + beg + "srlg " + srlg);
        l.add(cmds.tabulator + beg + "hello-time " + helloTimer);
        l.add(cmds.tabulator + beg + "dead-time " + deadTimer);
        secInfoUtl.getConfig(l, ipInfoCfg, cmds.tabulator + beg + "ipinfo ");
        cmds.cfgLine(l, !ldpSync, cmds.tabulator, beg + "ldp-sync", "");
        cmds.cfgLine(l, !dynamicForbid, cmds.tabulator, beg + "dynamic-metric forbid", "");
        switch (dynamicMetric) {
            case 0:
                a = "disabled";
                break;
            case 1:
                a = "inband";
                break;
            case 2:
                a = "icmpecho";
                break;
            case 3:
                a = "udpecho";
                break;
            case 4:
                a = "twamp";
                break;
            default:
                a = "unknown=" + dynamicMetric;
        }
        cmds.cfgLine(l, dynamicMetric < 1, cmds.tabulator, beg + "dynamic-metric mode", a);
        l.add(cmds.tabulator + beg + "dynamic-metric time " + echoTimer);
        echoParam.getConfig(l, beg);
    }

    /**
     * get help text
     *
     * @param l list to update
     */
    public static void routerGetHelp(userHelp l) {
        l.add(null, false, 4, new int[]{-1}, "enable", "enable protocol processing");
        l.add(null, false, 4, new int[]{-1}, "split-horizon", "dont advertise back on rx interface");
        l.add(null, false, 4, new int[]{-1}, "database-filter", "advertise only own data");
        l.add(null, false, 4, new int[]{5, -1}, "bfd", "enable bfd triggered down");
        l.add(null, false, 5, new int[]{-1}, "strict", "enable strict mode");
        l.add(null, false, 4, new int[]{-1}, "passive", "do not form neighborship");
        l.add(null, false, 4, new int[]{-1}, "accept-metric", "accept peer metric");
        l.add(null, false, 4, new int[]{-1}, "stub", "do not route traffic");
        l.add(null, false, 4, new int[]{-1}, "unstub", "do route traffic");
        l.add(null, false, 4, new int[]{5}, "segrout", "set segment routing parameters");
        l.add(null, false, 5, new int[]{6, -1}, "<num>", "index");
        l.add(null, false, 6, new int[]{6, -1}, "pop", "advertise pop label");
        l.add(null, false, 4, new int[]{5}, "bier", "set bier parameters");
        l.add(null, false, 5, new int[]{6, -1}, "<num>", "index");
        l.add(null, false, 6, new int[]{-1}, "<num>", "subdomain");
        l.add(null, false, 4, new int[]{-1}, "disable-password", "disable authentications");
        l.add(null, false, 4, new int[]{-1}, "suppress-prefix", "do not advertise interface");
        l.add(null, false, 4, new int[]{-1}, "unsuppress-prefix", "do advertise interface");
        l.add(null, false, 4, new int[]{-1}, "verify-source", "check source address of updates");
        l.add(null, false, 4, new int[]{5}, "sending-tos", "tos used for sending");
        l.add(null, false, 5, new int[]{-1}, "<num>", "value");
        l.add(null, false, 4, new int[]{5}, "sending-ttl", "ttl used for sending");
        l.add(null, false, 5, new int[]{-1}, "<num>", "value");
        l.add(null, false, 4, new int[]{5}, "encryption", "select encryption method");
        l.add(null, false, 5, new int[]{6}, "ssh", "select secure shell");
        l.add(null, false, 5, new int[]{6}, "tls", "select transport layer security");
        l.add(null, false, 6, new int[]{7}, "<name:rsa>", "rsa key");
        l.add(null, false, 7, new int[]{8}, "<name:dsa>", "dsa key");
        l.add(null, false, 8, new int[]{9}, "<name:ecd>", "ecdsa key");
        l.add(null, false, 9, new int[]{10}, "<name:mld>", "mldsa key");
        l.add(null, false, 10, new int[]{11}, "<name:crt>", "rsa certificate");
        l.add(null, false, 11, new int[]{12}, "<name:crt>", "dsa certificate");
        l.add(null, false, 12, new int[]{13}, "<name:crt>", "ecdsa certificate");
        l.add(null, false, 13, new int[]{-1}, "<name:crt>", "mldsa certificate");
        l.add(null, false, 4, new int[]{5}, "dump", "setup dump file");
        l.add(null, false, 5, new int[]{6, -1}, "<file>", "name of file");
        l.add(null, false, 6, new int[]{7}, "<num>", "ms between backup");
        l.add(null, false, 7, new int[]{8, -1}, "<file>", "name of backup");
        l.add(null, false, 8, new int[]{-1}, "<num>", "maximum size of backup");
        l.add(null, false, 4, new int[]{5}, "password", "password for authentication");
        l.add(null, false, 5, new int[]{-1}, "<text>", "set password");
        l.add(null, false, 4, new int[]{5}, "metric", "interface metric");
        l.add(null, false, 5, new int[]{-1}, "<num>", "metric");
        l.add(null, false, 4, new int[]{5}, "affinity", "set affinity");
        l.add(null, false, 5, new int[]{-1}, "<num>", "affinity");
        l.add(null, false, 4, new int[]{5}, "srlg", "set srlg");
        l.add(null, false, 5, new int[]{-1}, "<num>", "srlg");
        l.add(null, false, 4, new int[]{5}, "hello-time", "time between hellos");
        l.add(null, false, 5, new int[]{-1}, "<num>", "time in ms");
        l.add(null, false, 4, new int[]{5}, "dead-time", "time before neighbor down");
        l.add(null, false, 5, new int[]{-1}, "<num>", "time in ms");
        secInfoUtl.getHelp(l, 4, "ipinfo", "check peers");
        l.add(null, false, 4, new int[]{-1}, "ldp-sync", "synchronize metric to ldp");
        l.add(null, false, 4, new int[]{5}, "dynamic-metric", "dynamic peer metric");
        l.add(null, false, 5, new int[]{-1}, "forbid", "forbid peer measurement");
        l.add(null, false, 5, new int[]{6}, "mode", "measurement mode");
        l.add(null, false, 6, new int[]{-1}, "disabled", "forbid echo requests");
        l.add(null, false, 6, new int[]{-1}, "inband", "inband echo requests");
        l.add(null, false, 6, new int[]{-1}, "icmpecho", "icmp echo requests");
        l.add(null, false, 6, new int[]{-1}, "udpecho", "udp echo requests");
        l.add(null, false, 6, new int[]{-1}, "twamp", "twamp echo requests");
        tabAverage.getHelp(l);
    }

    /**
     * do one config
     *
     * @param a command
     * @param cmd parameters
     */
    public void routerDoConfig(String a, cmds cmd) {
        if (a.equals("bfd")) {
            bfdTrigger = 1;
            for (;;) {
                a = cmd.word();
                if (a.length() < 1) {
                    break;
                }
                if (a.equals("strict")) {
                    bfdTrigger = 2;
                    continue;
                }
            }
            return;
        }
        if (a.equals("stub")) {
            stub = true;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("unstub")) {
            unstub = true;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("disable-password")) {
            authenDisable = true;
            return;
        }
        if (a.equals("segrout")) {
            segrouIdx = bits.str2num(cmd.word());
            segrouPop = false;
            for (;;) {
                a = cmd.word();
                if (a.length() < 1) {
                    break;
                }
                if (a.equals("pop")) {
                    segrouPop = true;
                    continue;
                }
            }
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("bier")) {
            bierIdx = bits.str2num(cmd.word());
            bierSub = bits.str2num(cmd.word());
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("split-horizon")) {
            splitHorizon = true;
            return;
        }
        if (a.equals("sending-tos")) {
            sendingTos = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("sending-ttl")) {
            sendingTtl = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("verify-source")) {
            connectedCheck = true;
            return;
        }
        if (a.equals("database-filter")) {
            databaseFilter = true;
            return;
        }
        if (a.equals("dump")) {
            try {
                dumpFile.close();
            } catch (Exception e) {
            }
            dumpFile = new logFil(cmd.word());
            int tim = bits.str2num(cmd.word());
            String bck = cmd.word();
            int siz = bits.str2num(cmd.word());
            dumpFile.rotate(bck, siz, tim, 0);
            dumpFile.open(true);
            return;
        }
        if (a.equals("password")) {
            authentication = authLocal.passwdDecode(cmd.word());
            return;
        }
        if (a.equals("encryption")) {
            encryptionMethod = servGeneric.string2proto(cmd.word());
            keyRsa = cfgAll.keyFind(cfgAll.rsakeys, cmd.word(), false);
            keyDsa = cfgAll.keyFind(cfgAll.dsakeys, cmd.word(), false);
            keyEcDsa = cfgAll.keyFind(cfgAll.ecdsakeys, cmd.word(), false);
            keyMlDsa = cfgAll.keyFind(cfgAll.mldsakeys, cmd.word(), false);
            certRsa = cfgAll.certFind(cmd.word(), false);
            certDsa = cfgAll.certFind(cmd.word(), false);
            certEcDsa = cfgAll.certFind(cmd.word(), false);
            certMlDsa = cfgAll.certFind(cmd.word(), false);
            return;
        }
        if (a.equals("suppress-prefix")) {
            suppressAddr = true;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("unsuppress-prefix")) {
            unsuppressAddr = true;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("accept-metric")) {
            acceptMetric = true;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("ldp-sync")) {
            ldpSync = true;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("dynamic-metric")) {
            a = cmd.word();
            if (a.equals("forbid")) {
                dynamicForbid = true;
                lower.todo.set(0);
                lower.notif.wakeup();
                return;
            }
            if (a.equals("mode")) {
                a = cmd.word();
                dynamicMetric = 0;
                if (a.equals("disabled")) {
                    dynamicMetric = 0;
                }
                if (a.equals("inband")) {
                    dynamicMetric = 1;
                }
                if (a.equals("icmpecho")) {
                    dynamicMetric = 2;
                }
                if (a.equals("udpecho")) {
                    dynamicMetric = 3;
                }
                if (a.equals("twamp")) {
                    dynamicMetric = 4;
                }
                lower.todo.set(0);
                lower.notif.wakeup();
                return;
            }
            if (a.equals("time")) {
                echoTimer = bits.str2num(cmd.word());
                return;
            }
            if (echoParam.doConfig(a, cmd)) {
                return;
            }
            if (a.equals("algo")) {
                echoParam.string2algo(cmd.word());
                lower.todo.set(0);
                lower.notif.wakeup();
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("passive")) {
            passiveInt = true;
            return;
        }
        if (a.equals("hello-time")) {
            helloTimer = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("ipinfo")) {
            ipInfoCfg = secInfoUtl.doCfgStr(ipInfoCfg, cmd, false);
            return;
        }
        if (a.equals("dead-time")) {
            deadTimer = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("metric")) {
            metric = bits.str2num(cmd.word());
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("affinity")) {
            affinity = bits.str2num(cmd.word());
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("srlg")) {
            srlg = bits.str2num(cmd.word());
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        cmd.badCmd();
    }

    /**
     * undo one config
     *
     * @param a command
     * @param cmd parameters
     */
    public void routerUnConfig(String a, cmds cmd) {
        if (a.equals("ipinfo")) {
            ipInfoCfg = secInfoUtl.doCfgStr(ipInfoCfg, cmd, true);
            return;
        }
        if (a.equals("metric")) {
            metric = 10;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("bfd")) {
            bfdTrigger = 0;
            return;
        }
        if (a.equals("stub")) {
            stub = false;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("unstub")) {
            unstub = false;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("disable-password")) {
            authenDisable = false;
            return;
        }
        if (a.equals("segrout")) {
            segrouIdx = -1;
            segrouPop = false;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("bier")) {
            bierIdx = -1;
            bierSub = -1;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("split-horizon")) {
            splitHorizon = false;
            return;
        }
        if (a.equals("sending-tos")) {
            sendingTos = -1;
            return;
        }
        if (a.equals("sending-ttl")) {
            sendingTtl = -1;
            return;
        }
        if (a.equals("verify-source")) {
            connectedCheck = false;
            return;
        }
        if (a.equals("database-filter")) {
            databaseFilter = false;
            return;
        }
        if (a.equals("dump")) {
            try {
                dumpFile.close();
            } catch (Exception e) {
            }
            dumpFile = null;
            return;
        }
        if (a.equals("password")) {
            authentication = null;
            return;
        }
        if (a.equals("suppress-prefix")) {
            suppressAddr = false;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("unsuppress-prefix")) {
            unsuppressAddr = false;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("ldp-sync")) {
            ldpSync = false;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("dynamic-metric")) {
            a = cmd.word();
            if (a.equals("forbid")) {
                dynamicForbid = false;
                lower.todo.set(0);
                lower.notif.wakeup();
                return;
            }
            if (a.equals("mode")) {
                dynamicMetric = 0;
                lower.todo.set(0);
                lower.notif.wakeup();
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("accept-metric")) {
            acceptMetric = false;
            lower.todo.set(0);
            lower.notif.wakeup();
            return;
        }
        if (a.equals("passive")) {
            passiveInt = false;
            return;
        }
        if (a.equals("encryption")) {
            encryptionMethod = 0;
            return;
        }
        cmd.badCmd();
    }

    public String toString() {
        return "lsrp on " + iface;
    }

    public int compareTo(rtrLsrpIface o) {
        if (iface.ifwNum < o.iface.ifwNum) {
            return -1;
        }
        if (iface.ifwNum > o.iface.ifwNum) {
            return +1;
        }
        return 0;
    }

    private void sendHello(prtGenConn id) {
        if (debugger.rtrLsrpEvnt) {
            logger.debug("tx hello " + id);
        }
        packHolder pck = new packHolder(true, true);
        pck.putFill(0, 16, 255);
        pck.putSkip(16);
        pck.putAddr(0, lower.routerID);
        pck.putSkip(4);
        if (!passiveInt) {
            for (int i = 0; i < neighs.size(); i++) {
                rtrLsrpNeigh nei = neighs.get(i);
                pck.putAddr(0, nei.rtrId);
                pck.putSkip(4);
            }
        }
        pck.merge2beg();
        id.send2net(pck);
    }

    /**
     * close interface
     *
     * @param ifc interface
     */
    public void closedInterface(ipFwdIface ifc) {
    }

    /**
     * connection ready
     *
     * @param id connection
     */
    public void datagramReady(prtGenConn id) {
    }

    /**
     * start connection
     *
     * @param id connection
     * @return false if success, true if error
     */
    public boolean datagramAccept(prtGenConn id) {
        id.timeout = deadTimer;
        return false;
    }

    /**
     * stop connection
     *
     * @param id connection
     */
    public void datagramClosed(prtGenConn id) {
    }

    /**
     * work connection
     *
     * @param id connection
     */
    public void datagramWork(prtGenConn id) {
    }

    /**
     * received error
     *
     * @param id connection
     * @param pck packet
     * @param rtr reporting router
     * @param err error happened
     * @param lab error label
     * @return false on success, true on error
     */
    public boolean datagramError(prtGenConn id, packHolder pck, addrIP rtr, counter.reasons err, int lab) {
        return false;
    }

    /**
     * notified that state changed
     *
     * @param id id number to reference connection
     * @param stat state
     * @return return false if successful, true if error happened
     */
    public boolean datagramState(prtGenConn id, state.states stat) {
        if (stat == state.states.up) {
            return false;
        }
        closeNeighbors();
        return false;
    }

    /**
     * received packet
     *
     * @param id connection
     * @param pck packet
     * @return false if success, true if error
     */
    public boolean datagramRecv(prtGenConn id, packHolder pck) {
        id.setClosing();
        if ((connectedCheck) && (!iface.network.matches(id.peerAddr))) {
            logger.info("got from out of subnet peer " + id);
            return true;
        }
        if (passiveInt) {
            return true;
        }
        for (int i = 0; i < 16; i++) {
            if (pck.getByte(i) != 255) {
                return true;
            }
        }
        addrIPv4 peer = new addrIPv4();
        pck.getAddr(peer, 16);
        pck.getSkip(20);
        int seen = 0;
        for (;;) {
            if (pck.dataSize() < 1) {
                break;
            }
            addrIPv4 adr = new addrIPv4();
            pck.getAddr(adr, 0);
            pck.getSkip(4);
            if (adr.compareTo(lower.routerID) == 0) {
                seen++;
            }
        }
        if (debugger.rtrLsrpEvnt) {
            logger.debug("rx hello " + id);
        }
        rtrLsrpNeigh nei = new rtrLsrpNeigh(lower, this, peer, id.peerAddr);
        rtrLsrpNeigh old = neighs.add(nei);
        if (old == null) {
            sendHello(conn);
            nei.startWork();
        } else {
            nei = old;
        }
        if (seen > 0) {
            nei.lastHeard = bits.getTime();
        } else {
            sendHello(conn);
        }
        return false;
    }

    /**
     * close all neighbors
     */
    public void closeNeighbors() {
        for (int i = neighs.size(); i >= 0; i--) {
            rtrLsrpNeigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            nei.stopWork();
        }
    }

    /**
     * got better advertisement
     *
     * @param dat advertisement
     */
    public void gotAdvert(rtrLsrpData dat) {
        if (!splitHorizon) {
            return;
        }
        for (int i = 0; i < neighs.size(); i++) {
            rtrLsrpNeigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.advert.put(dat.copyHead());
        }
    }

    public void run() {
        for (;;) {
            if (!need2run) {
                return;
            }
            try {
                sendHello(conn);
                long tim = bits.getTime();
                for (int i = neighs.size() - 1; i >= 0; i--) {
                    rtrLsrpNeigh nei = neighs.get(i);
                    if (nei == null) {
                        continue;
                    }
                    if ((tim - nei.lastHeard) < deadTimer) {
                        continue;
                    }
                    nei.stopWork();
                }
            } catch (Exception e) {
                logger.traceback(e);
            }
            bits.sleep(helloTimer);
        }
    }

}
