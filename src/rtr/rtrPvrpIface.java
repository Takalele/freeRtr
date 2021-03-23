package rtr;

import addr.addrIP;
import addr.addrIPv4;
import auth.authLocal;
import cfg.cfgAll;
import cfg.cfgCert;
import cfg.cfgKey;
import cfg.cfgPrfxlst;
import cfg.cfgRoump;
import cfg.cfgRouplc;
import cry.cryKeyDSA;
import cry.cryKeyECDSA;
import cry.cryKeyRSA;
import ip.ipFwdIface;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import pack.packHolder;
import prt.prtGenConn;
import prt.prtServP;
import serv.servGeneric;
import tab.tabGen;
import tab.tabListing;
import tab.tabPrfxlstN;
import tab.tabRoute;
import tab.tabRtrmapN;
import tab.tabRtrplcN;
import user.userFlash;
import user.userFormat;
import user.userHelping;
import util.bits;
import util.cmds;
import util.counter;
import util.debugger;
import util.logger;
import util.state;

/**
 * pvrp interface
 *
 * @author matecsaba
 */
public class rtrPvrpIface implements Comparator<rtrPvrpIface>, Runnable, prtServP {

    /**
     * hello interval
     */
    public int helloTimer = 5000;

    /**
     * dead interval
     */
    public int deadTimer = 15000;

    /**
     * default distance
     */
    public int distance = 80;

    /**
     * default incoming metric
     */
    public int metricIn = 10;

    /**
     * default outgoing metric
     */
    public int metricOut = 0;

    /**
     * stub flag
     */
    public boolean stub = false;

    /**
     * unstub flag
     */
    public boolean unstub = false;

    /**
     * bfd enabled
     */
    public boolean bfdTrigger;

    /**
     * passive interface
     */
    public boolean passiveInt;

    /**
     * accept metric
     */
    public boolean acceptMetric = false;

    /**
     * advertise default route
     */
    public boolean defOrigin = false;

    /**
     * advertise pop labels
     */
    public boolean labelPop = false;

    /**
     * not advertise routes learned from interface back
     */
    public boolean splitHorizon = true;

    /**
     * suppress interface address
     */
    public boolean suppressAddr = false;

    /**
     * unsuppress interface address
     */
    public boolean unsuppressAddr = false;

    /**
     * authentication string
     */
    public String authentication = null;

    /**
     * ingress label filter
     */
    public tabListing<tabPrfxlstN, addrIP> labelIn = null;

    /**
     * egress label filter
     */
    public tabListing<tabPrfxlstN, addrIP> labelOut = null;

    /**
     * ingress prefix list
     */
    public tabListing<tabPrfxlstN, addrIP> prflstIn = null;

    /**
     * egress prefix list
     */
    public tabListing<tabPrfxlstN, addrIP> prflstOut = null;

    /**
     * ingress route map
     */
    public tabListing<tabRtrmapN, addrIP> roumapIn = null;

    /**
     * egress route map
     */
    public tabListing<tabRtrmapN, addrIP> roumapOut = null;

    /**
     * ingress route policy
     */
    public tabListing<tabRtrplcN, addrIP> roupolIn = null;

    /**
     * egress route policy
     */
    public tabListing<tabRtrplcN, addrIP> roupolOut = null;

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
     * security method
     */
    public int encryptionMethod = 0;

    /**
     * dump file
     */
    public String dumpFile = null;

    /**
     * dump backup time
     */
    public int dumpTime = 0;

    /**
     * name of backup file
     */
    public String dumpBackup = null;

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
    protected rtrPvrp lower;

    /**
     * list of neighbors
     */
    protected tabGen<rtrPvrpNeigh> neighs;

    /**
     * routes needed to advertise
     */
    protected tabRoute<addrIP> need2adv;

    private FileOutputStream dumpHandle1 = null;

    private PrintStream dumpHandle2 = null;

    private long dumpStarted = 0;

    private boolean need2run;

    /**
     * create one instance
     *
     * @param parent the rip protocol
     * @param ifc the ip interface to work on
     */
    public rtrPvrpIface(rtrPvrp parent, ipFwdIface ifc) {
        lower = parent;
        iface = ifc;
        neighs = new tabGen<rtrPvrpNeigh>();
    }

    /**
     * unregister from udp
     */
    public void unregister2udp() {
        lower.udpCore.listenStop(iface, rtrPvrp.port, null, 0);
        conn.setClosing();
        need2run = false;
    }

    /**
     * register to udp
     */
    public void register2udp() {
        addrIP adr = new addrIP();
        if (iface.addr.isIPv4()) {
            adr.fromString("224.0.0.227");
        } else {
            adr.fromString("ff02::227");
        }
        lower.udpCore.packetListen(this, iface, rtrPvrp.port, null, 0, "pvrp", null, -1);
        conn = lower.udpCore.packetConnect(this, iface, rtrPvrp.port, adr, rtrPvrp.port, "pvrp", null, -1);
        if (conn == null) {
            return;
        }
        conn.timeout = 0;
        need2run = true;
        new Thread(this).start();
    }

    /**
     * dump one line
     *
     * @param dir direction: false=rx, true=tx
     * @param dat line
     */
    protected void dumpLine(boolean dir, String dat) {
        if (dumpHandle2 == null) {
            return;
        }
        synchronized (dumpFile) {
            if (dumpTime > 0) {
                if ((bits.getTime() - dumpStarted) > dumpTime) {
                    dumpStarted = bits.getTime();
                    try {
                        dumpHandle2.flush();
                        dumpHandle1.close();
                    } catch (Exception e) {
                        logger.error("unable to close file");
                    }
                    dumpHandle2 = null;
                    dumpHandle1 = null;
                    userFlash.rename(dumpFile, dumpBackup, true, true);
                    try {
                        dumpHandle1 = new FileOutputStream(dumpFile);
                        dumpHandle2 = new PrintStream(dumpHandle1);
                    } catch (Exception e) {
                        logger.error("unable to open file");
                    }
                }
            }
            try {
                dumpHandle2.print(logger.getTimestamp());
                if (dir) {
                    dumpHandle2.print(" tx ");
                } else {
                    dumpHandle2.print(" rx ");
                }
                dumpHandle2.println(dat);
                dumpHandle2.flush();
                return;
            } catch (Exception e) {
                logger.error("unable to write file");
            }
            try {
                dumpHandle2.close();
            } catch (Exception e) {
            }
            dumpHandle1 = null;
            dumpHandle2 = null;
        }
    }

    /**
     * list of neighbors
     *
     * @param res list to update
     */
    protected void showNeighs(userFormat res) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrPvrpNeigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            res.add(iface + "|" + nei.rtrId + "|" + nei.name + "|" + nei.inam + "|" + nei.peer + "|" + nei.learned.size() + "|" + nei.adverted.size() + "|" + bits.timePast(nei.upTime));
        }
    }

    /**
     * find one neighbor
     *
     * @param adr address of peer
     * @return neighbor, null if not found
     */
    protected rtrPvrpNeigh findNeigh(addrIP adr) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrPvrpNeigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            if (adr.compare(adr, nei.peer) == 0) {
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
            l.add(cmds.tabulator + "no " + beg + "dump");
        } else {
            String a = "";
            if (dumpTime != 0) {
                a = " " + dumpTime + " " + dumpBackup;
            }
            l.add(cmds.tabulator + beg + "dump " + dumpFile + a);
        }
        cmds.cfgLine(l, !splitHorizon, cmds.tabulator, beg + "split-horizon", "");
        cmds.cfgLine(l, !passiveInt, cmds.tabulator, beg + "passive", "");
        cmds.cfgLine(l, !acceptMetric, cmds.tabulator, beg + "accept-metric", "");
        cmds.cfgLine(l, !bfdTrigger, cmds.tabulator, beg + "bfd", "");
        cmds.cfgLine(l, !defOrigin, cmds.tabulator, beg + "default-originate", "");
        cmds.cfgLine(l, !labelPop, cmds.tabulator, beg + "label-pop", "");
        cmds.cfgLine(l, !stub, cmds.tabulator, beg + "stub", "");
        cmds.cfgLine(l, !unstub, cmds.tabulator, beg + "unstub", "");
        cmds.cfgLine(l, !suppressAddr, cmds.tabulator, beg + "suppress-prefix", "");
        cmds.cfgLine(l, !unsuppressAddr, cmds.tabulator, beg + "unsuppress-prefix", "");
        cmds.cfgLine(l, encryptionMethod <= 0, cmds.tabulator, beg + "encryption", servGeneric.proto2string(encryptionMethod) + " " + keyRsa + " " + keyDsa + " " + keyEcDsa + " " + certRsa + " " + certDsa + " " + certEcDsa);
        cmds.cfgLine(l, authentication == null, cmds.tabulator, beg + "password", authLocal.passwdEncode(authentication, (filter & 2) != 0));
        l.add(cmds.tabulator + beg + "distance " + distance);
        l.add(cmds.tabulator + beg + "metric-in " + metricIn);
        l.add(cmds.tabulator + beg + "metric-out " + metricOut);
        l.add(cmds.tabulator + beg + "hello-time " + helloTimer);
        l.add(cmds.tabulator + beg + "dead-time " + deadTimer);
        cmds.cfgLine(l, labelIn == null, cmds.tabulator, beg + "label-in", "" + labelIn);
        cmds.cfgLine(l, labelOut == null, cmds.tabulator, beg + "label-out", "" + labelOut);
        cmds.cfgLine(l, prflstIn == null, cmds.tabulator, beg + "prefix-list-in", "" + prflstIn);
        cmds.cfgLine(l, prflstOut == null, cmds.tabulator, beg + "prefix-list-out", "" + prflstOut);
        cmds.cfgLine(l, roumapIn == null, cmds.tabulator, beg + "route-map-in", "" + roumapIn);
        cmds.cfgLine(l, roumapOut == null, cmds.tabulator, beg + "route-map-out", "" + roumapOut);
        cmds.cfgLine(l, roupolIn == null, cmds.tabulator, beg + "route-policy-in", "" + roupolIn);
        cmds.cfgLine(l, roupolOut == null, cmds.tabulator, beg + "route-policy-out", "" + roupolOut);
    }

    /**
     * get help text
     *
     * @param l list to update
     */
    public static void routerGetHelp(userHelping l) {
        l.add("4 .         enable                  enable protocol processing");
        l.add("4 .         bfd                     enable bfd triggered down");
        l.add("4 .         default-originate       send default route to peer");
        l.add("4 .         label-pop               advertise php");
        l.add("4 .         split-horizon           dont advertise back on rx interface");
        l.add("4 .         passive                 do not form neighborship");
        l.add("4 .         accept-metric           accept peer metric");
        l.add("4 .         stub                    do not route traffic");
        l.add("4 .         unstub                  do route traffic");
        l.add("4 .         suppress-prefix         do not advertise interface");
        l.add("4 .         unsuppress-prefix       do advertise interface");
        l.add("4 5         encryption              select encryption method");
        l.add("5 6           ssh                   select secure shell");
        l.add("5 6           tls                   select transport layer security");
        l.add("6 7             <name>              rsa key");
        l.add("7 8               <name>            dsa key");
        l.add("8 9                 <name>          ecdsa key");
        l.add("9 10                  <name>        rsa certificate");
        l.add("10 11                   <name>      dsa certificate");
        l.add("11 .                      <name>    ecdsa certificate");
        l.add("4 5         dump                    setup dump file");
        l.add("5 6,.         <file>                name of file");
        l.add("6 7             <num>               ms between backup");
        l.add("7 .               <file>            name of backup");
        l.add("4 5         password                password for authentication");
        l.add("5 .           <text>                set password");
        l.add("4 5         metric-in               metric of incoming routes");
        l.add("5 .           <num>                 metric");
        l.add("4 5         metric-out              metric of outgoing routes");
        l.add("5 .           <num>                 metric");
        l.add("4 5         distance                administrative distance of routes");
        l.add("5 .           <num>                 set administrative distance");
        l.add("4 5         hello-time              time between hellos");
        l.add("5 .           <num>                 time in ms");
        l.add("4 5         dead-time               time before neighbor down");
        l.add("5 .           <num>                 time in ms");
        l.add("4 5         route-map-in            process prefixes in ingress updates");
        l.add("5 .           <name>                name of route map");
        l.add("4 5         route-map-out           process prefixes in egress updates");
        l.add("5 .           <name>                name of route map");
        l.add("4 5         route-policy-in         process prefixes in ingress updates");
        l.add("5 .           <name>                name of route policy");
        l.add("4 5         route-policy-out        process prefixes in egress updates");
        l.add("5 .           <name>                name of route policy");
        l.add("4 5         prefix-list-in          filter prefixes in ingress updates");
        l.add("5 .           <name>                name of prefix list");
        l.add("4 5         prefix-list-out         filter prefixes in egress updates");
        l.add("5 .           <name>                name of prefix list");
        l.add("4 5         label-in                filter label in ingress updates");
        l.add("5 .           <name>                name of prefix list");
        l.add("4 5         label-out               filter label in egress updates");
        l.add("5 .           <name>                name of prefix list");
    }

    /**
     * do one config
     *
     * @param a command
     * @param cmd parameters
     */
    public void routerDoConfig(String a, cmds cmd) {
        if (a.equals("bfd")) {
            bfdTrigger = true;
            return;
        }
        if (a.equals("label-pop")) {
            labelPop = true;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("default-originate")) {
            defOrigin = true;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("split-horizon")) {
            splitHorizon = true;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("dump")) {
            try {
                dumpHandle2.flush();
                dumpHandle1.close();
            } catch (Exception e) {
            }
            dumpHandle2 = null;
            dumpHandle1 = null;
            dumpFile = cmd.word();
            dumpTime = bits.str2num(cmd.word());
            dumpBackup = cmd.word();
            dumpStarted = bits.getTime();
            if (dumpTime > 0) {
                userFlash.rename(dumpFile, dumpBackup, true, true);
            }
            try {
                dumpHandle1 = new FileOutputStream(dumpFile);
                dumpHandle2 = new PrintStream(dumpHandle1);
            } catch (Exception e) {
                logger.error("unable to open file");
            }
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
            certRsa = cfgAll.certFind(cmd.word(), false);
            certDsa = cfgAll.certFind(cmd.word(), false);
            certEcDsa = cfgAll.certFind(cmd.word(), false);
            return;
        }
        if (a.equals("stub")) {
            stub = true;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("unstub")) {
            unstub = true;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("suppress-prefix")) {
            suppressAddr = true;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("unsuppress-prefix")) {
            unsuppressAddr = true;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("accept-metric")) {
            acceptMetric = true;
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
        if (a.equals("dead-time")) {
            deadTimer = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("metric-in")) {
            metricIn = bits.str2num(cmd.word());
            lower.notif.wakeup();
            return;
        }
        if (a.equals("metric-out")) {
            metricOut = bits.str2num(cmd.word());
            lower.notif.wakeup();
            return;
        }
        if (a.equals("distance")) {
            distance = bits.str2num(cmd.word());
            lower.notif.wakeup();
            return;
        }
        if (a.equals("label-in")) {
            cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such prefix list");
                return;
            }
            labelIn = ntry.prflst;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("label-out")) {
            cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such prefix list");
                return;
            }
            labelOut = ntry.prflst;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("prefix-list-in")) {
            cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such prefix list");
                return;
            }
            prflstIn = ntry.prflst;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("prefix-list-out")) {
            cfgPrfxlst ntry = cfgAll.prfxFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such prefix list");
                return;
            }
            prflstOut = ntry.prflst;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("route-map-in")) {
            cfgRoump ntry = cfgAll.rtmpFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such route map");
                return;
            }
            roumapIn = ntry.roumap;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("route-map-out")) {
            cfgRoump ntry = cfgAll.rtmpFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such route map");
                return;
            }
            roumapOut = ntry.roumap;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("route-policy-in")) {
            cfgRouplc ntry = cfgAll.rtplFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such route policy");
                return;
            }
            roupolIn = ntry.rouplc;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("route-policy-out")) {
            cfgRouplc ntry = cfgAll.rtplFind(cmd.word(), false);
            if (ntry == null) {
                cmd.error("no such route policy");
                return;
            }
            roupolOut = ntry.rouplc;
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
        if (a.equals("bfd")) {
            bfdTrigger = false;
            return;
        }
        if (a.equals("label-pop")) {
            labelPop = false;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("default-originate")) {
            defOrigin = false;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("split-horizon")) {
            splitHorizon = false;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("dump")) {
            try {
                dumpHandle1.flush();
                dumpHandle2.close();
            } catch (Exception e) {
            }
            dumpHandle2 = null;
            dumpHandle1 = null;
            dumpFile = null;
            dumpTime = 0;
            dumpBackup = null;
            return;
        }
        if (a.equals("password")) {
            authentication = null;
            return;
        }
        if (a.equals("stub")) {
            stub = false;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("unstub")) {
            unstub = false;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("suppress-prefix")) {
            suppressAddr = false;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("unsuppress-prefix")) {
            unsuppressAddr = false;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("accept-metric")) {
            acceptMetric = false;
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
        if (a.equals("label-in")) {
            labelIn = null;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("label-out")) {
            labelOut = null;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("prefix-list-in")) {
            prflstIn = null;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("prefix-list-out")) {
            prflstOut = null;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("route-map-in")) {
            roumapIn = null;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("route-map-out")) {
            roumapOut = null;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("route-policy-in")) {
            roupolIn = null;
            lower.notif.wakeup();
            return;
        }
        if (a.equals("route-policy-out")) {
            roupolOut = null;
            lower.notif.wakeup();
            return;
        }
        cmd.badCmd();
    }

    public String toString() {
        return "pvrp on " + iface;
    }

    public int compare(rtrPvrpIface o1, rtrPvrpIface o2) {
        if (o1.iface.ifwNum < o2.iface.ifwNum) {
            return -1;
        }
        if (o1.iface.ifwNum > o2.iface.ifwNum) {
            return +1;
        }
        return 0;
    }

    private void sendHello(prtGenConn id) {
        if (debugger.rtrPvrpEvnt) {
            logger.debug("tx hello " + id);
        }
        packHolder pck = new packHolder(true, true);
        pck.putFill(0, 16, 255);
        pck.putSkip(16);
        pck.putAddr(0, lower.routerID);
        pck.putSkip(4);
        if (!passiveInt) {
            for (int i = 0; i < neighs.size(); i++) {
                rtrPvrpNeigh nei = neighs.get(i);
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
        if (passiveInt) {
            return true;
        }
        if (!iface.network.matches(id.peerAddr)) {
            logger.info("got from out of subnet peer " + id);
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
            if (adr.compare(adr, lower.routerID) == 0) {
                seen++;
            }
        }
        if (debugger.rtrPvrpEvnt) {
            logger.debug("rx hello " + id);
        }
        rtrPvrpNeigh nei = new rtrPvrpNeigh(lower, this, peer, id.peerAddr);
        rtrPvrpNeigh old = neighs.add(nei);
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
            rtrPvrpNeigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            nei.stopWork();
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
                    rtrPvrpNeigh nei = neighs.get(i);
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
