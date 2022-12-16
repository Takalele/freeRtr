package net.freertr.rtr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.freertr.addr.addrIP;
import net.freertr.cfg.cfgAll;
import net.freertr.clnt.clntEcho;
import net.freertr.clnt.clntPing;
import net.freertr.clnt.clntTwamp;
import net.freertr.enc.encThrift;
import net.freertr.enc.encThriftEntry;
import net.freertr.ip.ipFwdIface;
import net.freertr.pack.packHolder;
import net.freertr.prt.prtGenConn;
import net.freertr.prt.prtServP;
import net.freertr.tab.tabAverage;
import net.freertr.tab.tabGen;
import net.freertr.user.userFormat;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;
import net.freertr.util.counter;
import net.freertr.util.debugger;
import net.freertr.util.logger;
import net.freertr.util.notifier;
import net.freertr.util.state;

/**
 * rift interface
 *
 * @author matecsaba
 */
public class rtrRiftIface implements Comparator<rtrRiftIface>, Runnable, rtrBfdClnt, prtServP {

    /**
     * hello interval
     */
    public int helloTimer = 1000;

    /**
     * dead interval
     */
    public int deadTimer = 3000;

    /**
     * echo interval
     */
    public int echoTimer;

    /**
     * echo parameters
     */
    public tabAverage echoParam;

    /**
     * dynamic metric
     */
    public int dynamicMetric;

    /**
     * default metric
     */
    public int metric = 10;

    /**
     * bfd enabled
     */
    public boolean bfdTrigger;

    /**
     * passive interface
     */
    public boolean passiveInt;

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
     * the interface this works on
     */
    protected final ipFwdIface iface;

    /**
     * the udp connection it uses to multicast
     */
    protected prtGenConn connM;

    /**
     * the udp connection it uses to unicast
     */
    protected prtGenConn connU;

    /**
     * the lower layer
     */
    protected final rtrRift lower;

    /**
     * tie database
     */
    protected final tabGen<rtrRiftTie> advert;

    /**
     * notified on route change
     */
    protected notifier notif = new notifier();

    /**
     * time echo sent
     */
    protected long echoTime;

    /**
     * peer ready
     */
    public boolean ready;

    /**
     * transport address of peer
     */
    public addrIP peer;

    /**
     * hostname of peer
     */
    public String name;

    /**
     * router id of peer
     */
    public long rtrId;

    /**
     * link id of peer
     */
    public int lnkId;

    /**
     * level of peer
     */
    public int level;

    /**
     * flood port of peer
     */
    public int flood;

    /**
     * time last heard
     */
    public long lastHeard;

    private int nonceL;

    private int nonceR;

    private int lieCnt;

    private int tieCnt;

    private long upTime;

    private boolean need2run;

    /**
     * create one instance
     *
     * @param parent the rip protocol
     * @param ifc the ip interface to work on
     */
    public rtrRiftIface(rtrRift parent, ipFwdIface ifc) {
        lower = parent;
        iface = ifc;
        advert = new tabGen<rtrRiftTie>();
        echoTimer = 60000;
        echoParam = new tabAverage(1, 65530);
    }

    /**
     * unregister from udp
     */
    public void unregister2udp() {
        lower.udpCore.listenStop(iface, rtrRift.portL, null, 0);
        lower.udpCore.listenStop(iface, rtrRift.portT, null, 0);
        connM.setClosing();
        if (connU != null) {
            connU.setClosing();
        }
        need2run = false;
    }

    /**
     * register to udp
     */
    public void register2udp() {
        addrIP adr = new addrIP();
        if (iface.addr.isIPv4()) {
            adr.fromString("224.0.0.120");
        } else {
            adr.fromString("ff02::a1f7");
        }
        lower.udpCore.packetListen(this, iface, rtrRift.portL, null, 0, "rift", -1, null, -1, -1);
        lower.udpCore.packetListen(this, iface, rtrRift.portT, null, 0, "rift", -1, null, -1, -1);
        connM = lower.udpCore.packetConnect(this, iface, rtrRift.portL, adr, rtrRift.portL, "rift", -1, null, -1, -1);
        if (connM == null) {
            return;
        }
        connM.timeout = 0;
        nonceL = bits.randomW();
        need2run = true;
        new Thread(this).start();
    }

    /**
     * get peer metric
     *
     * @return metric
     */
    public int getMetric() {
        if (!ready) {
            return metric;
        }
        int met = metric;
        if (dynamicMetric < 1) {
            return met;
        }
        return echoParam.getResult(met);
    }

    /**
     * list of neighbors
     *
     * @param brief only briefly
     * @param res list to update
     */
    protected void showNeighs(userFormat res, boolean brief) {
        if (!ready) {
            return;
        }
        if (brief) {
            res.add(rtrId + "|" + name + "|" + bits.timePast(upTime));
        } else {
            res.add(iface + "|" + rtrId + "|" + name + "|" + peer + "|" + bits.timePast(upTime));
        }
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
        cmds.cfgLine(l, !passiveInt, cmds.tabulator, beg + "passive", "");
        cmds.cfgLine(l, !bfdTrigger, cmds.tabulator, beg + "bfd", "");
        cmds.cfgLine(l, !suppressAddr, cmds.tabulator, beg + "suppress-prefix", "");
        cmds.cfgLine(l, !unsuppressAddr, cmds.tabulator, beg + "unsuppress-prefix", "");
        cmds.cfgLine(l, !connectedCheck, cmds.tabulator, beg + "verify-source", "");
        l.add(cmds.tabulator + beg + "metric " + metric);
        l.add(cmds.tabulator + beg + "hello-time " + helloTimer);
        l.add(cmds.tabulator + beg + "dead-time " + deadTimer);
        String a;
        switch (dynamicMetric) {
            case 0:
                a = "disabled";
                break;
            case 1:
                a = "icmpecho";
                break;
            case 2:
                a = "udpecho";
                break;
            case 3:
                a = "twamp";
                break;
            default:
                a = "unknown=" + dynamicMetric;
        }
        cmds.cfgLine(l, dynamicMetric < 1, cmds.tabulator, beg + "dynamic-metric mode", a);
        l.add(cmds.tabulator + beg + "dynamic-metric time " + echoTimer);
        l.add(cmds.tabulator + beg + "dynamic-metric size " + echoParam.buckets);
        l.add(cmds.tabulator + beg + "dynamic-metric minimum " + echoParam.minimum);
        l.add(cmds.tabulator + beg + "dynamic-metric maximum " + echoParam.maximum);
        l.add(cmds.tabulator + beg + "dynamic-metric divisor " + echoParam.divisor);
        l.add(cmds.tabulator + beg + "dynamic-metric multiply " + echoParam.multiply);
        l.add(cmds.tabulator + beg + "dynamic-metric ignore " + echoParam.ignorer);
        l.add(cmds.tabulator + beg + "dynamic-metric skip-min " + echoParam.discardLo);
        l.add(cmds.tabulator + beg + "dynamic-metric skip-max " + echoParam.discardHi);
        l.add(cmds.tabulator + beg + "dynamic-metric algo " + echoParam.getAlgoName());
    }

    /**
     * get help text
     *
     * @param l list to update
     */
    public static void routerGetHelp(userHelping l) {
        l.add(null, "4 .         enable                      enable protocol processing");
        l.add(null, "4 .         bfd                         enable bfd triggered down");
        l.add(null, "4 .         passive                     do not form neighborship");
        l.add(null, "4 .         suppress-prefix             do not advertise interface");
        l.add(null, "4 .         unsuppress-prefix           do advertise interface");
        l.add(null, "4 .         verify-source               check source address of updates");
        l.add(null, "4 5         metric                      metric of the interface");
        l.add(null, "5 .           <num>                     metric");
        l.add(null, "4 5         hello-time                  time between hellos");
        l.add(null, "5 .           <num>                     time in ms");
        l.add(null, "4 5         dead-time                   time before neighbor down");
        l.add(null, "5 .           <num>                     time in ms");
        l.add(null, "4 5         dynamic-metric            dynamic peer metric");
        l.add(null, "5 6           mode                    dynamic peer metric");
        l.add(null, "6 .             disabled              forbid echo requests");
        l.add(null, "6 .             icmpecho              icmp echo requests");
        l.add(null, "6 .             udpecho               udp echo requests");
        l.add(null, "6 .             twamp                 twamp echo requests");
        l.add(null, "5 6           time                      measurement interval");
        l.add(null, "6 .             <num>                   time in ms");
        l.add(null, "5 6           size                      number of measurement");
        l.add(null, "6 .             <num>                   number of values");
        l.add(null, "5 6           minimum                   lowest result");
        l.add(null, "6 .             <num>                   value");
        l.add(null, "5 6           maximum                   highest result");
        l.add(null, "6 .             <num>                   value");
        l.add(null, "5 6           divisor                   divide result");
        l.add(null, "6 .             <num>                   value");
        l.add(null, "5 6           multiply                  multiply result");
        l.add(null, "6 .             <num>                   value");
        l.add(null, "5 6           ignore                    ignore small differences");
        l.add(null, "6 .             <num>                   value");
        l.add(null, "5 6           skip-min                  discard small measures");
        l.add(null, "6 .             <num>                   number of values");
        l.add(null, "5 6           skip-max                  discard big measures");
        l.add(null, "6 .             <num>                   number of values");
        l.add(null, "5 6           algo                      calculation to do");
        l.add(null, "6 .             none                    nothing");
        l.add(null, "6 .             minimum                 take lowest");
        l.add(null, "6 .             average                 take average");
        l.add(null, "6 .             maximum                 take highest");
        l.add(null, "6 .             summary                 take summary");
        l.add(null, "6 .             dif-min                 take lowest of differences");
        l.add(null, "6 .             dif-avg                 take average of differences");
        l.add(null, "6 .             dif-max                 take highest of differences");
        l.add(null, "6 .             dif-sum                 take summary of differences");
    }

    /**
     * do one config
     *
     * @param a command
     * @param cmd parameters
     */
    public void routerDoConfig(String a, cmds cmd) {
        if (a.equals("dynamic-metric")) {
            a = cmd.word();
            if (a.equals("mode")) {
                a = cmd.word();
                dynamicMetric = 0;
                if (a.equals("disabled")) {
                    dynamicMetric = 0;
                }
                if (a.equals("icmpecho")) {
                    dynamicMetric = 1;
                }
                if (a.equals("udpecho")) {
                    dynamicMetric = 2;
                }
                if (a.equals("twamp")) {
                    dynamicMetric = 3;
                }
                lower.notif.wakeup();
                return;
            }
            if (a.equals("time")) {
                echoTimer = bits.str2num(cmd.word());
                return;
            }
            if (a.equals("size")) {
                echoParam.buckets = bits.str2num(cmd.word());
                return;
            }
            if (a.equals("minimum")) {
                echoParam.minimum = bits.str2num(cmd.word());
                return;
            }
            if (a.equals("maximum")) {
                echoParam.maximum = bits.str2num(cmd.word());
                return;
            }
            if (a.equals("divisor")) {
                echoParam.divisor = bits.str2num(cmd.word());
                return;
            }
            if (a.equals("multiply")) {
                echoParam.multiply = bits.str2num(cmd.word());
                return;
            }
            if (a.equals("ignore")) {
                echoParam.ignorer = bits.str2num(cmd.word());
                return;
            }
            if (a.equals("skip-min")) {
                echoParam.discardLo = bits.str2num(cmd.word());
                return;
            }
            if (a.equals("skip-max")) {
                echoParam.discardHi = bits.str2num(cmd.word());
                return;
            }
            if (a.equals("algo")) {
                echoParam.string2algo(cmd.word());
                lower.notif.wakeup();
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("bfd")) {
            bfdTrigger = true;
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
        if (a.equals("metric")) {
            metric = bits.str2num(cmd.word());
            lower.notif.wakeup();
            return;
        }
    }

    /**
     * undo one config
     *
     * @param a command
     * @param cmd parameters
     */
    public void routerUnConfig(String a, cmds cmd) {
        if (a.equals("dynamic-metric")) {
            a = cmd.word();
            if (a.equals("mode")) {
                dynamicMetric = 0;
                lower.notif.wakeup();
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("bfd")) {
            bfdTrigger = false;
            return;
        }
        if (a.equals("verify-source")) {
            connectedCheck = false;
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
        if (a.equals("passive")) {
            passiveInt = false;
            return;
        }
    }

    public String toString() {
        return "rift on " + iface;
    }

    public int compare(rtrRiftIface o1, rtrRiftIface o2) {
        if (o1.iface.ifwNum < o2.iface.ifwNum) {
            return -1;
        }
        if (o1.iface.ifwNum > o2.iface.ifwNum) {
            return +1;
        }
        return 0;
    }

    private encThrift putMyHeader() {
        encThrift th1 = new encThrift();
        encThriftEntry th2 = new encThriftEntry();
        th2.putField(1, encThriftEntry.tpI8, rtrRift.version);
        th2.putField(2, encThriftEntry.tpI16, 1); // minor
        th2.putField(3, encThriftEntry.tpI64, lower.nodeID);
        th2.putField(4, encThriftEntry.tpI8, lower.level);
        th1.putField(1, encThriftEntry.tpStr, th2.elm); // header
        return th1;
    }

    private void putPackHead(packHolder pck, int cnt, int life) {
        pck.merge2end();
        pck.msbPutW(0, rtrRift.magic);
        pck.msbPutW(2, cnt);
        pck.msbPutW(4, rtrRift.version);
        pck.msbPutW(6, 0); // key id, length
        pck.msbPutW(8, nonceL);
        pck.msbPutW(10, nonceR);
        pck.msbPutD(12, life); // lifetime
        pck.putSkip(16);
        if (life != -1) {
            pck.msbPutD(0, 0); // tie key
            pck.putSkip(4);
        }
        pck.merge2beg();
    }

    private void sendLie() {
        if (debugger.rtrRiftEvnt) {
            logger.debug("tx lie " + connM);
        }
        encThrift th1 = putMyHeader();
        encThriftEntry th2 = new encThriftEntry();
        encThriftEntry th3 = new encThriftEntry();
        th3.putField(1, encThriftEntry.tpBin, (cfgAll.hostName + ":" + iface).getBytes());
        th3.putField(2, encThriftEntry.tpI32, iface.ifwNum);
        th3.putField(3, encThriftEntry.tpI16, rtrRift.portT);
        th3.putField(4, encThriftEntry.tpI32, iface.mtu + lower.ipSize);
        if (ready) {
            encThriftEntry th4 = new encThriftEntry();
            th4.putField(1, encThriftEntry.tpI64, rtrId);
            th4.putField(2, encThriftEntry.tpI32, lnkId);
            th3.putField(6, encThriftEntry.tpStr, th4.elm); // reflected
        }
        th3.putField(22, encThriftEntry.tpI8, 1); // flood repeater
        encThriftEntry th4 = new encThriftEntry();
        th4.putField(1, encThriftEntry.tpI16, 1); // minor
        th3.putField(10, encThriftEntry.tpStr, th4.elm); // nodecapa
        th3.putField(12, encThriftEntry.tpI16, deadTimer / 1000);
        th2.putField(1, encThriftEntry.tpStr, th3.elm); // lie
        th1.putField(2, encThriftEntry.tpStr, th2.elm); // content
        th2 = new encThriftEntry();
        th1.putField(0, encThriftEntry.tpEnd, 0); // end
        packHolder pck = new packHolder(true, true);
        th1.toPacket(pck);
        if (debugger.rtrRiftTraf) {
            logger.debug("tx thrift=" + th1.show());
        }
        lieCnt++;
        putPackHead(pck, lieCnt, -1);
        connM.send2net(pck);
    }

    private boolean openUniConn() {
        if (connU != null) {
            return false;
        }
        connU = lower.udpCore.packetConnect(this, iface, 0, peer, flood, "rift", -1, null, -1, -1);
        if (connU == null) {
            return true;
        }
        connU.timeout = 0;
        return false;
    }

    private void sendTie() {
        if (openUniConn()) {
            return;
        }
        for (int i = 0; i < lower.ties.size(); i++) {
            rtrRiftTie tie = lower.ties.get(i);
            if (tie == null) {
                continue;
            }
            if (!tie.differs(advert.find(tie))) {
                continue;
            }
            if ((level < lower.level) && (tie.direct != 1)) {
                continue;
            }
            if (debugger.rtrRiftEvnt) {
                logger.debug("tx tie " + connU);
            }
            encThriftEntry th2 = new encThriftEntry();
            th2.putField(1, encThriftEntry.tpStr, tie.putHeader2().elm);
            th2.putField(2, encThriftEntry.tpStr, tie.elements.elm);
            encThriftEntry th3 = new encThriftEntry();
            th3.putField(4, encThriftEntry.tpStr, th2.elm);
            encThrift th1 = putMyHeader();
            th1.putField(2, encThriftEntry.tpStr, th3.elm);
            th1.putField(0, encThriftEntry.tpEnd, 0); // end
            packHolder pck = new packHolder(true, true);
            th1.toPacket(pck);
            if (debugger.rtrRiftTraf) {
                logger.debug("tx thrift=" + th1.show());
            }
            tieCnt++;
            putPackHead(pck, tieCnt, tie.getRemain());
            connU.send2net(pck);
            return;
        }
    }

    private void sendTire(rtrRiftTie tie) {
        if (openUniConn()) {
            return;
        }
        if (debugger.rtrRiftEvnt) {
            logger.debug("tx tire " + connU);
        }
        encThriftEntry th3 = new encThriftEntry();
        th3.putField(0, encThriftEntry.tpStr, tie.putHeader3().elm);
        encThriftEntry th4 = new encThriftEntry();
        th4.putField(1, encThriftEntry.tpSet, th3.elm);
        th4.putTypKV(encThriftEntry.tpStr, encThriftEntry.tpStr);
        encThriftEntry th5 = new encThriftEntry();
        th5.putField(3, encThriftEntry.tpStr, th4.elm);
        encThrift th1 = putMyHeader();
        th1.putField(2, encThriftEntry.tpStr, th5.elm);
        th1.putField(0, encThriftEntry.tpEnd, 0); // end
        packHolder pck = new packHolder(true, true);
        th1.toPacket(pck);
        if (debugger.rtrRiftTraf) {
            logger.debug("tx thrift=" + th1.show());
        }
        tieCnt++;
        putPackHead(pck, tieCnt, -1);
        connU.send2net(pck);
    }

    private void sendTide(rtrRiftTie tie) {
        if (openUniConn()) {
            return;
        }
        if (debugger.rtrRiftEvnt) {
            logger.debug("tx tide " + connU);
        }
        encThriftEntry th2 = new encThriftEntry();
        th2.putField(1, encThriftEntry.tpStr, tie.putHeader1().elm);
        th2.putField(2, encThriftEntry.tpStr, tie.putHeader1().elm);
        th2.putField(3, encThriftEntry.tpLst, new ArrayList<encThriftEntry>());
        encThriftEntry th3 = new encThriftEntry();
        th3.putField(2, encThriftEntry.tpStr, th2.elm);
        encThrift th1 = putMyHeader();
        th1.putField(2, encThriftEntry.tpStr, th3.elm);
        th1.putField(0, encThriftEntry.tpEnd, 0); // end
        packHolder pck = new packHolder(true, true);
        th1.toPacket(pck);
        if (debugger.rtrRiftTraf) {
            logger.debug("tx thrift=" + th1.show());
        }
        tieCnt++;
        putPackHead(pck, tieCnt, tie.getRemain());
        connU.send2net(pck);
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
        if ((connectedCheck) && (!iface.network.matches(id.peerAddr))) {
            logger.info("got from out of subnet peer " + id);
            return true;
        }
        if (pck.msbGetW(0) != rtrRift.magic) {
            return true;
        }
        if (pck.getByte(5) != rtrRift.version) {
            return true;
        }
        if (debugger.rtrRiftEvnt) {
            logger.debug("rx " + (id.portLoc == rtrRift.portL ? "lie" : "tie") + " " + id);
        }
        peer = id.peerAddr.copyBytes();
        nonceR = pck.msbGetW(8);
        if (pck.msbGetW(10) == nonceL) {
            lastHeard = bits.getTime();
        } else {
            sendLie();
            ready = false;
        }
        if (id.portLoc == rtrRift.portL) {
            pck.getSkip(16);
            encThrift th1 = new encThrift();
            if (th1.fromPacket(pck)) {
                return false;
            }
            if (debugger.rtrRiftTraf) {
                logger.debug("rx thrift=" + th1.show());
            }
            encThriftEntry th2 = th1.getField(1, 0); // header
            if (th2 == null) {
                return false;
            }
            encThriftEntry th3 = th2.getField(1, 0);
            if (th3 == null) {
                return false;
            }
            if (th3.val != rtrRift.version) {
                return false;
            }
            th3 = th2.getField(3, 0);
            if (th3 == null) {
                return false;
            }
            rtrId = th3.val;
            th3 = th2.getField(4, 0);
            if (th3 == null) {
                return false;
            }
            level = (int) th3.val;
            th2 = th1.getField(2, 0); // contents
            if (th2 == null) {
                return false;
            }
            th2 = th2.getField(1, 0); // liepack
            if (th2 == null) {
                return false;
            }
            th3 = th2.getField(1, 0); // name
            if (th3 == null) {
                return false;
            }
            if (th3.dat == null) {
                return false;
            }
            name = new String(th3.dat);
            th3 = th2.getField(2, 0); // link id
            if (th3 == null) {
                return false;
            }
            lnkId = (int) th3.val;
            th3 = th2.getField(3, 0); // flood port
            if (th3 == null) {
                return false;
            }
            flood = (int) th3.val;
            if (ready) {
                return false;
            }
            if ((level < (lower.level - 1)) || (level > (lower.level + 1))) {
                logger.error("neighbor " + name + " (" + peer + ") miscabled");
                return false;
            }
            logger.warn("neighbor " + name + " (" + peer + ") up");
            upTime = bits.getTime();
            tieCnt = 0;
            advert.clear();
            ready = true;
            lower.notif.wakeup();
            if (bfdTrigger) {
                iface.bfdAdd(peer, this, "rift");
            }
            return false;
        }
        if (!ready) {
            return false;
        }
        int life = pck.msbGetD(12);
        if (life == -1) {
            pck.getSkip(16);
        } else {
            pck.getSkip(20);
        }
        encThrift th1 = new encThrift();
        if (th1.fromPacket(pck)) {
            return false;
        }
        if (debugger.rtrRiftTraf) {
            logger.debug("rx thrift=" + th1.show());
        }
        encThriftEntry th2 = th1.getField(1, 0); // header
        if (th2 == null) {
            return false;
        }
        th2 = th1.getField(2, 0); // content
        if (th2 == null) {
            return false;
        }
        encThriftEntry th3 = th2.getField(2, 0);
        if (th3 != null) { // tide
            if (debugger.rtrRiftEvnt) {
                logger.debug("rx tide " + id);
            }
            encThriftEntry th4 = th3.getField(3, 0);
            if (th4 == null) {
                return false;
            }
            if (th4.elm == null) {
                return false;
            }
            for (int i = 0; i < th4.elm.size(); i++) {
                encThriftEntry th5 = th4.elm.get(i);
                encThriftEntry th6 = th3.getField(1, 0);
                if (th6 == null) {
                    continue;
                }
                rtrRiftTie tie = new rtrRiftTie();
                if (tie.getHeader(th5.getField(1, 0))) {
                    continue;
                }
                if (!tie.better(lower.ties.find(tie))) {
                    continue;
                }
                sendTide(tie);
            }
            return false;
        }
        th3 = th2.getField(3, 0);
        if (th3 != null) { // tire
            if (debugger.rtrRiftEvnt) {
                logger.debug("rx tire " + id);
            }
            encThriftEntry th4 = th3.getField(1, 0);
            if (th4 == null) {
                return false;
            }
            if (th4.elm == null) {
                return false;
            }
            for (int i = 0; i < th4.elm.size(); i++) {
                encThriftEntry th5 = th4.elm.get(i);
                rtrRiftTie tie = new rtrRiftTie();
                if (tie.getHeader(th5.getField(1, 0))) {
                    continue;
                }
                advert.put(tie.copyHead());
            }
            notif.wakeup();
            return false;
        }
        th3 = th2.getField(4, 0);
        if (th3 != null) { // tie
            if (debugger.rtrRiftEvnt) {
                logger.debug("rx tie " + id);
            }
            rtrRiftTie tie = new rtrRiftTie();
            tie.elements = th3.getField(2, 0);
            if (tie.elements == null) {
                logger.error("got empty tie element from " + peer);
                return false;
            }
            if (tie.getHeader(th3.getField(1, 0))) {
                logger.error("got invalid tie header from " + peer);
                return false;
            }
            tie.expire = bits.getTime() + (life * 1000);
            if (tie.differs(advert.find(tie))) {
                advert.put(tie.copyHead());
            }
            if (tie.better(lower.ties.find(tie))) {
                lower.ties.put(tie);
                lower.notif.wakeup();
            }
            sendTire(tie.copyHead());
            return false;
        }
        return false;
    }

    public void run() {
        for (;;) {
            if (!need2run) {
                return;
            }
            notif.sleep(helloTimer);
            try {
                sendLie();
                if (!ready) {
                    continue;
                }
                if ((echoTime + echoTimer) < bits.getTime()) {
                    switch (dynamicMetric) {
                        case 1:
                            clntPing png = new clntPing();
                            png.meas = echoParam;
                            png.fwd = lower.fwdCore;
                            png.src = iface;
                            png.trg = new addrIP();
                            png.trg = peer;
                            png.doWork();
                            break;
                        case 2:
                            clntEcho ech = new clntEcho();
                            ech.meas = echoParam;
                            ech.udp = lower.udpCore;
                            ech.src = iface;
                            ech.trg = new addrIP();
                            ech.trg = peer;
                            ech.doWork();
                            break;
                        case 3:
                            clntTwamp twm = new clntTwamp();
                            twm.meas = echoParam;
                            twm.udp = lower.udpCore;
                            twm.src = iface;
                            twm.trg = new addrIP();
                            twm.trg = peer;
                            twm.doWork();
                            break;
                    }
                    echoTime = bits.getTime() - 1;
                }
                sendTie();
                long tim = bits.getTime();
                if ((tim - lastHeard) < deadTimer) {
                    continue;
                }
                logger.error("neighbor " + name + " (" + peer + ") down");
                lower.notif.wakeup();
                iface.bfdDel(peer, this);
                if (connU != null) {
                    connU.setClosing();
                    connU = null;
                }
                ready = false;
            } catch (Exception e) {
                logger.traceback(e);
            }
        }
    }

    public void bfdPeerDown() {
        if (debugger.rtrRiftEvnt) {
            logger.debug("stopping peer " + rtrId + " (" + peer + ")");
        }
        ready = false;
        nonceL = bits.randomW();
        nonceR = 0;
        lastHeard = 0;
    }

}
