package net.freertr.rtr;

import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrIPv4;
import net.freertr.addr.addrIPv6;
import net.freertr.ip.ipFwdIface;
import net.freertr.ip.ipPrt;
import net.freertr.pack.packHolder;
import net.freertr.tab.tabAverage;
import net.freertr.tab.tabGen;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;
import net.freertr.util.counter;
import net.freertr.util.debugger;
import net.freertr.util.logger;
import net.freertr.util.state;

/**
 * ospfv3 interface
 *
 * @author matecsaba
 */
public class rtrOspf6iface implements Comparator<rtrOspf6iface>, ipPrt {

    /**
     * list of neighbors
     */
    protected tabGen<rtrOspf6neigh> neighs;

    /**
     * the interface this works on
     */
    protected final ipFwdIface iface;

    /**
     * areas this interface belongs
     */
    protected tabGen<rtrOspf6area> areas = new tabGen<rtrOspf6area>();

    /**
     * counter
     */
    protected counter cntr;

    /**
     * interface type
     */
    public int networkType;

    private final rtrOspf6 lower;

    private Timer keepTimer;

    /**
     * suppress interface address
     */
    public boolean suppressAddr;

    /**
     * unsuppress interface address
     */
    public boolean unsuppressAddr;

    /**
     * check neighbor address is connected
     */
    public boolean connectedCheck;

    /**
     * passive interface
     */
    public boolean passiveInt;

    /**
     * hello timer
     */
    public int helloTimer;

    /**
     * dead timer
     */
    public int deadTimer;

    /**
     * retransmit timer
     */
    public int retransTimer;

    /**
     * dr priority
     */
    public int drPriority;

    /**
     * traffic eng suppression
     */
    public boolean teSuppress;

    /**
     * traffic eng metric
     */
    public int teMetric;

    /**
     * traffic eng bandwidth
     */
    public long teBandwidth;

    /**
     * traffic eng affinity
     */
    public int teAffinity;

    /**
     * traffic eng srlg
     */
    public int teSrlg;

    /**
     * segment rou index
     */
    public int srIndex;

    /**
     * bier index
     */
    public int brIndex;

    /**
     * segment rou node
     */
    public boolean srNode;

    /**
     * segment rou pop
     */
    public boolean srPop;

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
     * dr address
     */
    public addrIPv4 drAddr = new addrIPv4();

    /**
     * bdr address
     */
    public addrIPv4 bdrAddr = new addrIPv4();

    /**
     * interface metric
     */
    public int metric;

    /**
     * bfd enabled
     */
    public boolean bfdTrigger;

    /**
     * interface instance
     */
    public int instance = 0;

    /**
     * point to point
     */
    public final static int netypP2p = 1;

    /**
     * point to multipoint
     */
    public final static int netypP2mp = 2;

    /**
     * point to multipoint, non broadcast
     */
    public final static int netypP2nb = 3;

    /**
     * broadcast
     */
    public final static int netypBrdct = 4;

    /**
     * non broadcast, multiple access
     */
    public final static int netypNbma = 5;

    /**
     * master/slave
     */
    public final static int dscrMstr = 0x01;

    /**
     * more descriptors
     */
    public final static int dscrMore = 0x02;

    /**
     * initialization
     */
    public final static int dscrInit = 0x04;

    /**
     * create one instance
     *
     * @param parent the ospf protocol
     * @param ara area where this interface belongs
     * @param ifc the ip interface to work on
     */
    public rtrOspf6iface(rtrOspf6 parent, rtrOspf6area ara, ipFwdIface ifc) {
        lower = parent;
        iface = ifc;
        areas.add(ara);
        networkType = netypP2p;
        setDefaultTimers();
        neighs = new tabGen<rtrOspf6neigh>();
        cntr = new counter();
        connectedCheck = true;
        drPriority = 0;
        metric = 10;
        teMetric = 10;
        echoTimer = 60000;
        echoParam = new tabAverage(1, 65530);
        if (iface != null) {
            teBandwidth = iface.lower.getBandwidth();
        }
    }

    public int compare(rtrOspf6iface o1, rtrOspf6iface o2) {
        if (o1.iface.ifwNum < o2.iface.ifwNum) {
            return -1;
        }
        if (o1.iface.ifwNum > o2.iface.ifwNum) {
            return +1;
        }
        return 0;
    }

    public String toString() {
        return "" + iface;
    }

    /**
     * get configuration
     *
     * @param l list to add
     * @param beg beginning
     */
    public void routerGetConfig(List<String> l, String beg) {
        l.add(cmds.tabulator + beg + "enable");
        String a = "";
        for (int i = 0; i < areas.size(); i++) {
            a = a + " " + areas.get(i).area;
        }
        l.add(cmds.tabulator + beg + "area" + a);
        cmds.cfgLine(l, !passiveInt, cmds.tabulator, beg + "passive", "");
        switch (networkType) {
            case netypP2p:
                a = "point2point";
                break;
            case netypP2mp:
                a = "point2multipoint";
                break;
            case netypP2nb:
                a = "point2nonbroadcast";
                break;
            case netypBrdct:
                a = "broadcast";
                break;
            case netypNbma:
                a = "nonbroadcast";
                break;
            default:
                a = "unknown=" + networkType;
                break;
        }
        l.add(cmds.tabulator + beg + "network " + a);
        cmds.cfgLine(l, !bfdTrigger, cmds.tabulator, beg + "bfd", "");
        cmds.cfgLine(l, !suppressAddr, cmds.tabulator, beg + "suppress-prefix", "");
        cmds.cfgLine(l, !unsuppressAddr, cmds.tabulator, beg + "unsuppress-prefix", "");
        cmds.cfgLine(l, !connectedCheck, cmds.tabulator, beg + "verify-source", "");
        l.add(cmds.tabulator + beg + "instance " + instance);
        l.add(cmds.tabulator + beg + "cost " + metric);
        l.add(cmds.tabulator + beg + "priority " + drPriority);
        l.add(cmds.tabulator + beg + "hello-time " + helloTimer);
        l.add(cmds.tabulator + beg + "dead-time " + deadTimer);
        l.add(cmds.tabulator + beg + "retransmit-time " + retransTimer);
        if (areas.get(0).traffEng) {
            a = beg + "traffeng ";
            cmds.cfgLine(l, !teSuppress, cmds.tabulator, a + "suppress", "");
            l.add(cmds.tabulator + a + "metric " + teMetric);
            l.add(cmds.tabulator + a + "bandwidth " + teBandwidth);
            l.add(cmds.tabulator + a + "affinity " + teAffinity);
            l.add(cmds.tabulator + a + "srlg " + teSrlg);
        }
        if (areas.get(0).segrouEna) {
            a = beg + "segrout ";
            cmds.cfgLine(l, srIndex < 1, cmds.tabulator, a + "index", "" + srIndex);
            cmds.cfgLine(l, !srNode, cmds.tabulator, a + "node", "");
            cmds.cfgLine(l, !srPop, cmds.tabulator, a + "pop", "");
        }
        if (areas.get(0).bierEna) {
            a = beg + "bier ";
            cmds.cfgLine(l, brIndex < 1, cmds.tabulator, a + "index", "" + brIndex);
        }
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
        cmds.cfgLine(l, dynamicMetric < 1, cmds.tabulator, beg + "dynamic-metric", a);
        l.add(cmds.tabulator + beg + "dynamic-time " + echoTimer);
        l.add(cmds.tabulator + beg + "dynamic-size " + echoParam.buckets);
        l.add(cmds.tabulator + beg + "dynamic-minimum " + echoParam.minimum);
        l.add(cmds.tabulator + beg + "dynamic-maximum " + echoParam.maximum);
        l.add(cmds.tabulator + beg + "dynamic-divisor " + echoParam.divisor);
        l.add(cmds.tabulator + beg + "dynamic-multiply " + echoParam.multiply);
        l.add(cmds.tabulator + beg + "dynamic-ignore " + echoParam.ignorer);
        l.add(cmds.tabulator + beg + "dynamic-skip-min " + echoParam.discardLo);
        l.add(cmds.tabulator + beg + "dynamic-skip-max " + echoParam.discardHi);
        l.add(cmds.tabulator + beg + "dynamic-algo " + echoParam.getAlgoName());
        for (int i = 0; i < neighs.size(); i++) {
            rtrOspf6neigh ntry = neighs.get(i);
            if (!ntry.statNeigh) {
                continue;
            }
            l.add(cmds.tabulator + beg + "neighbor " + ntry.peer);
        }
    }

    /**
     * schedule work
     *
     * @param wrk work
     */
    protected void schedWork(int wrk) {
        for (int i = 0; i < areas.size(); i++) {
            rtrOspf6area ara = areas.get(i);
            if (ara == null) {
                continue;
            }
            ara.schedWork(wrk);
        }
    }

    /**
     * do configuration
     *
     * @param a command
     * @param cmd parameters
     */
    public void routerDoConfig(String a, cmds cmd) {
        if (a.equals("network")) {
            int i = -1;
            a = cmd.word();
            if (a.equals("point2point")) {
                i = netypP2p;
            }
            if (a.equals("point2multipoint")) {
                i = netypP2mp;
            }
            if (a.equals("point2nonbroadcast")) {
                i = netypP2nb;
            }
            if (a.equals("broadcast")) {
                i = netypBrdct;
            }
            if (a.equals("nonbroadcast")) {
                i = netypNbma;
            }
            if (i < 0) {
                return;
            }
            networkType = i;
            setDefaultTimers();
            schedWork(3);
            return;
        }
        if (a.equals("area")) {
            tabGen<rtrOspf6area> lst = new tabGen<rtrOspf6area>();
            for (;;) {
                a = cmd.word();
                if (a.length() < 1) {
                    break;
                }
                rtrOspf6area ar = new rtrOspf6area(lower, bits.str2num(a));
                ar = lower.areas.find(ar);
                if (ar == null) {
                    continue;
                }
                lst.add(ar);
            }
            if (lst.size() < 1) {
                return;
            }
            schedWork(7);
            areas = lst;
            schedWork(7);
            return;
        }
        if (a.equals("neighbor")) {
            addrIPv6 adr = new addrIPv6();
            if (adr.fromString(cmd.word())) {
                return;
            }
            rtrOspf6neigh nei = new rtrOspf6neigh(lower, areas.get(0), this, adr);
            rtrOspf6neigh old = neighs.add(nei);
            if (old != null) {
                nei = old;
            } else {
                nei.startNow();
            }
            nei.statNeigh = true;
            return;
        }
        if (a.equals("passive")) {
            passiveInt = true;
            return;
        }
        if (a.equals("bfd")) {
            bfdTrigger = true;
            return;
        }
        if (a.equals("suppress-prefix")) {
            suppressAddr = true;
            schedWork(1);
            return;
        }
        if (a.equals("unsuppress-prefix")) {
            unsuppressAddr = true;
            schedWork(1);
            return;
        }
        if (a.equals("verify-source")) {
            connectedCheck = true;
            return;
        }
        if (a.equals("hello-time")) {
            helloTimer = bits.str2num(cmd.word());
            restartTimer(false);
            return;
        }
        if (a.equals("dead-time")) {
            deadTimer = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("retransmit-time")) {
            retransTimer = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("priority")) {
            drPriority = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("cost")) {
            metric = bits.str2num(cmd.word());
            schedWork(3);
            return;
        }
        if (a.equals("instance")) {
            instance = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("traffeng")) {
            a = cmd.word();
            if (a.equals("suppress")) {
                teSuppress = true;
                schedWork(1);
                return;
            }
            if (a.equals("metric")) {
                teMetric = bits.str2num(cmd.word());
                schedWork(1);
                return;
            }
            if (a.equals("bandwidth")) {
                teBandwidth = bits.str2long(cmd.word());
                schedWork(1);
                return;
            }
            if (a.equals("affinity")) {
                teAffinity = bits.str2num(cmd.word());
                schedWork(1);
                return;
            }
            if (a.equals("srlg")) {
                teSrlg = bits.str2num(cmd.word());
                schedWork(1);
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("segrout")) {
            a = cmd.word();
            if (a.equals("index")) {
                srIndex = bits.str2num(cmd.word());
                schedWork(3);
                return;
            }
            if (a.equals("node")) {
                srNode = true;
                schedWork(3);
                return;
            }
            if (a.equals("pop")) {
                srPop = true;
                schedWork(3);
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("bier")) {
            a = cmd.word();
            if (a.equals("index")) {
                brIndex = bits.str2num(cmd.word());
                schedWork(3);
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("dynamic-metric")) {
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
            schedWork(3);
            return;
        }
        if (a.equals("dynamic-time")) {
            echoTimer = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("dynamic-size")) {
            echoParam.buckets = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("dynamic-minimum")) {
            echoParam.minimum = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("dynamic-maximum")) {
            echoParam.maximum = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("dynamic-divisor")) {
            echoParam.divisor = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("dynamic-multiply")) {
            echoParam.multiply = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("dynamic-ignore")) {
            echoParam.ignorer = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("dynamic-skip-min")) {
            echoParam.discardLo = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("dynamic-skip-max")) {
            echoParam.discardHi = bits.str2num(cmd.word());
            return;
        }
        if (a.equals("dynamic-algo")) {
            echoParam.string2algo(cmd.word());
            schedWork(3);
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
        if (a.equals("neighbor")) {
            addrIPv6 adr = new addrIPv6();
            if (adr.fromString(cmd.word())) {
                return;
            }
            rtrOspf6neigh nei = new rtrOspf6neigh(lower, areas.get(0), this, adr);
            nei = neighs.find(nei);
            if (nei == null) {
                return;
            }
            nei.statNeigh = false;
            return;
        }
        if (a.equals("cost")) {
            metric = 10;
            schedWork(3);
            return;
        }
        if (a.equals("passive")) {
            passiveInt = false;
            return;
        }
        if (a.equals("bfd")) {
            bfdTrigger = false;
            return;
        }
        if (a.equals("suppress-prefix")) {
            suppressAddr = false;
            schedWork(1);
            return;
        }
        if (a.equals("unsuppress-prefix")) {
            unsuppressAddr = false;
            schedWork(1);
            return;
        }
        if (a.equals("verify-source")) {
            connectedCheck = false;
            return;
        }
        if (a.equals("traffeng")) {
            a = cmd.word();
            if (a.equals("suppress")) {
                teSuppress = false;
                schedWork(1);
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("segrout")) {
            a = cmd.word();
            if (a.equals("index")) {
                srIndex = 0;
                schedWork(3);
                return;
            }
            if (a.equals("node")) {
                srNode = false;
                schedWork(3);
                return;
            }
            if (a.equals("pop")) {
                srPop = false;
                schedWork(3);
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("bier")) {
            a = cmd.word();
            if (a.equals("index")) {
                brIndex = 0;
                schedWork(3);
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("dynamic-metric")) {
            dynamicMetric = 0;
            schedWork(3);
            return;
        }
        cmd.badCmd();
    }

    /**
     * get help text
     *
     * @param l list to update
     */
    public static void routerGetHelp(userHelping l) {
        l.add(null, "4 .         enable                  enable protocol processing");
        l.add(null, "4 5         area                    specify area number");
        l.add(null, "5 6,.         <num>                 area number");
        l.add(null, "6 6,.           <num>               secondary area number");
        l.add(null, "4 5         network                 specify network type");
        l.add(null, "5 .           point2point           point to point");
        l.add(null, "5 .           point2multipoint      point to multipoint");
        l.add(null, "5 .           point2nonbroadcast    point to multipoint, non broadcast");
        l.add(null, "5 .           broadcast             broadcast");
        l.add(null, "5 .           nonbroadcast          non broadcast, multiple access");
        l.add(null, "4 .         passive                 do not process packets");
        l.add(null, "4 .         bfd                     enable bfd triggered down");
        l.add(null, "4 .         suppress-prefix         do not advertise interface");
        l.add(null, "4 .         unsuppress-prefix       do advertise interface");
        l.add(null, "4 .         verify-source           check source address of updates");
        l.add(null, "4 5         instance                interface instance");
        l.add(null, "5 .           <num>                 instance");
        l.add(null, "4 5         cost                    interface cost");
        l.add(null, "5 .           <num>                 cost");
        l.add(null, "4 5         priority                router priority");
        l.add(null, "5 .           <num>                 priority 0=disable");
        l.add(null, "4 5         hello-time              time between hellos");
        l.add(null, "5 .           <num>                 time in ms");
        l.add(null, "4 5         dead-time               time before neighbor down");
        l.add(null, "5 .           <num>                 time in ms");
        l.add(null, "4 5         retransmit-time         time before retarnsmitting");
        l.add(null, "5 .           <num>                 time in ms");
        l.add(null, "4 5         neighbor                specify static neighbor");
        l.add(null, "5 .           <addr>                address of peer interface");
        l.add(null, "4 5         traffeng                traffic engineering parameters");
        l.add(null, "5 .           suppress              do not advertise interface");
        l.add(null, "5 6           metric                set metric");
        l.add(null, "6 .             <num>               cost");
        l.add(null, "5 6           bandwidth             set bandwidth");
        l.add(null, "6 .             <num>               bandwidth");
        l.add(null, "5 6           affinity              set affinity");
        l.add(null, "6 .             <num>               affinity");
        l.add(null, "5 6           srlg                  set srlg");
        l.add(null, "6 .             <num>               affinity");
        l.add(null, "4 5         segrout                 segment routing parameters");
        l.add(null, "5 6           index                 set index");
        l.add(null, "6 .             <num>               index");
        l.add(null, "5 .           node                  set node flag");
        l.add(null, "5 .           pop                   request php");
        l.add(null, "4 5         bier                    bier parameters");
        l.add(null, "5 6           index                 set index");
        l.add(null, "6 .             <num>               index");
        l.add(null, "4 5         dynamic-metric          dynamic peer metric");
        l.add(null, "5 .           disabled              forbid echo requests");
        l.add(null, "5 .           icmpecho              icmp echo requests");
        l.add(null, "5 .           udpecho               udp echo requests");
        l.add(null, "5 .           twamp                 twamp echo requests");
        l.add(null, "4 5         dynamic-time            measurement interval");
        l.add(null, "5 .           <num>                 time in ms");
        l.add(null, "4 5         dynamic-size            number of measurement");
        l.add(null, "5 .           <num>                 number of values");
        l.add(null, "4 5         dynamic-minimum         lowest result");
        l.add(null, "5 .           <num>                 minimum");
        l.add(null, "4 5         dynamic-maximum         highest result");
        l.add(null, "5 .           <num>                 maximum");
        l.add(null, "4 5         dynamic-divisor         divide result");
        l.add(null, "5 .           <num>                 divisor");
        l.add(null, "4 5         dynamic-multiply        multiply result");
        l.add(null, "5 .           <num>                 multiplier");
        l.add(null, "4 5         dynamic-ignore          ignore small differences");
        l.add(null, "5 .           <num>                 maximum unreported change");
        l.add(null, "4 5         dynamic-skip-min        discard small measures");
        l.add(null, "5 .           <num>                 number of values");
        l.add(null, "4 5         dynamic-skip-max        discard big measures");
        l.add(null, "5 .           <num>                 number of values");
        l.add(null, "4 5         dynamic-algo            calculation to do");
        l.add(null, "5 .           none                  nothing");
        l.add(null, "5 .           minimum               take lowest");
        l.add(null, "5 .           average               take average");
        l.add(null, "5 .           maximum               take highest");
        l.add(null, "5 .           summary               take summary");
        l.add(null, "5 .           dif-min               take lowest of differences");
        l.add(null, "5 .           dif-avg               take average of differences");
        l.add(null, "5 .           dif-max               take highest of differences");
        l.add(null, "5 .           dif-sum               take summary of differences");
    }

    /**
     * close all neighbors
     *
     * @param shutdown complete shutdown
     */
    protected void closeNeighbors(boolean shutdown) {
        for (int i = neighs.size(); i >= 0; i--) {
            rtrOspf6neigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.statNeigh &= !shutdown;
            ntry.stopNow();
        }
        schedWork(7);
    }

    /**
     * setup timer thread
     *
     * @param shutdown set true to shut down
     */
    public void restartTimer(boolean shutdown) {
        try {
            keepTimer.cancel();
        } catch (Exception e) {
        }
        keepTimer = null;
        if (shutdown) {
            return;
        }
        keepTimer = new Timer();
        rtrOspf6ifaceHello task = new rtrOspf6ifaceHello(this);
        keepTimer.schedule(task, 500, helloTimer);
    }

    /**
     * set default timers
     */
    public void setDefaultTimers() {
        switch (networkType) {
            case netypP2p:
            case netypBrdct:
                helloTimer = 10000;
                break;
            case netypP2mp:
            case netypP2nb:
            case netypNbma:
                helloTimer = 30000;
                break;
        }
        deadTimer = helloTimer * 4;
        retransTimer = 3000;
    }

    /**
     * check if i've to peer with
     *
     * @param peer pere to check
     * @return true if yes, false if not
     */
    protected boolean shouldIpeer(addrIPv4 peer) {
        switch (networkType) {
            case netypP2p:
            case netypP2mp:
            case netypP2nb:
                return true;
            case netypBrdct:
            case netypNbma:
                break;
            default:
                return false;
        }
        if (amIdr()) {
            return true;
        }
        if (drAddr.compare(drAddr, peer) == 0) {
            return true;
        }
        if (bdrAddr.compare(bdrAddr, peer) == 0) {
            return true;
        }
        return false;
    }

    private addrIPv4 findDR(boolean drNeeded) {
        rtrOspf6neigh dr = new rtrOspf6neigh(lower, areas.get(0), this, iface.addr.toIPv6());
        dr.rtrPri = drPriority;
        dr.rtrID = lower.routerID.copyBytes();
        for (int i = 0; i < neighs.size(); i++) {
            rtrOspf6neigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            if (drNeeded) {
                if (ntry.rtrID.compare(ntry.peerDR, ntry.rtrID) != 0) {
                    continue;
                }
            } else {
                if (ntry.rtrID.compare(ntry.rtrID, drAddr) == 0) {
                    continue;
                }
                if (ntry.rtrID.compare(ntry.peerBDR, ntry.rtrID) != 0) {
                    continue;
                }
            }
            if (dr.otherBetterDR(ntry)) {
                dr = ntry;
            }
        }
        if (dr.rtrPri < 1) {
            return new addrIPv4();
        }
        return dr.rtrID.copyBytes();
    }

    /**
     * check if need dr
     *
     * @return true if yes, false if no
     */
    protected boolean needDR() {
        switch (networkType) {
            case netypBrdct:
            case netypNbma:
                return true;
            default:
                return false;
        }
    }

    /**
     * elect drs for this interface
     */
    protected void electDRs() {
        if (!needDR()) {
            return;
        }
        addrIPv4 old = drAddr.copyBytes();
        drAddr = findDR(true);
        bdrAddr = findDR(false);
        if (old.compare(old, drAddr) == 0) {
            return;
        }
        if (debugger.rtrOspf6evnt) {
            logger.debug("dr change, dr=" + drAddr + " bdr=" + bdrAddr);
        }
        schedWork(7);
    }

    /**
     * get dr interface id
     *
     * @return interface id
     */
    protected int DRintId() {
        if (drAddr.compare(drAddr, lower.routerID) == 0) {
            return iface.ifwNum;
        }
        for (int i = 0; i < neighs.size(); i++) {
            rtrOspf6neigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            if (drAddr.compare(drAddr, ntry.rtrID) != 0) {
                continue;
            }
            return ntry.rtrInt;
        }
        return -1;
    }

    /**
     * get dr interface id
     *
     * @return interface id
     */
    protected addrIP DRintAdr() {
        if (drAddr.compare(drAddr, lower.routerID) == 0) {
            return iface.addr.copyBytes();
        }
        for (int i = 0; i < neighs.size(); i++) {
            rtrOspf6neigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            if (drAddr.compare(drAddr, ntry.rtrID) != 0) {
                continue;
            }
            addrIP adr = new addrIP();
            adr.fromIPv6addr(ntry.peer);
            return adr;
        }
        return null;
    }

    /**
     * check if i'm dr on the net
     *
     * @return true means yes, false means no
     */
    protected boolean amIdr() {
        if (!needDR()) {
            return false;
        }
        if (drAddr.compare(drAddr, lower.routerID) == 0) {
            return true;
        }
        if (bdrAddr.compare(bdrAddr, lower.routerID) == 0) {
            return true;
        }
        return false;
    }

    /**
     * unregister from ip
     */
    public void unregister2ip() {
        lower.fwdCore.protoDel(this, iface, null);
    }

    /**
     * register to ip
     */
    public void register2ip() {
        lower.fwdCore.protoAdd(this, iface, null);
    }

    /**
     * make packet header
     *
     * @param pck packet to update
     * @param area area
     * @param typ type of packet
     */
    protected void mkPackHead(packHolder pck, rtrOspf6area area, int typ) {
        pck.merge2beg();
        if (debugger.rtrOspf6traf) {
            logger.debug("sending " + rtrOspf6neigh.msgTyp2string(typ) + " on " + iface);
        }
        pck.putByte(0, rtrOspf6.verNum); // version
        pck.putByte(1, typ); // message type
        pck.msbPutW(2, pck.dataSize() + rtrOspf6.sizeHead); // size
        pck.putAddr(4, lower.routerID); // router id
        pck.msbPutD(8, area.area); // area id
        pck.msbPutW(12, 0); // checksum
        pck.putByte(14, instance); // instance id
        pck.putByte(15, 0); // reserved
        int i = pck.pseudoIPsum(pck.dataSize() + rtrOspf6.sizeHead);
        i = pck.putIPsum(0, rtrOspf6.sizeHead, i);
        i = pck.getIPsum(0, pck.dataSize(), i);
        pck.lsbPutW(12, 0xffff - i); // checksum
        pck.putSkip(rtrOspf6.sizeHead);
        pck.merge2beg();
    }

    /**
     * make hello packet
     *
     * @param pck packet to update
     * @param area area
     */
    protected void mkHelloPack(packHolder pck, rtrOspf6area area) {
        pck.merge2beg();
        pck.msbPutD(0, iface.ifwNum); // interface id
        pck.msbPutD(4, area.getCapabilities()); // optional capabilities
        pck.putByte(4, drPriority); // dr priority
        pck.msbPutW(8, helloTimer / 1000); // hello interval
        pck.msbPutW(10, deadTimer / 1000); // dead interval
        pck.putAddr(12, drAddr); // designated router address
        pck.putAddr(16, bdrAddr); // backup designated router address
        pck.putSkip(20);
        for (int i = 0; i < neighs.size(); i++) {
            rtrOspf6neigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            if (ntry.area.area != area.area) {
                continue;
            }
            if (ntry.rtrID.isEmpty()) {
                continue;
            }
            pck.putAddr(0, ntry.rtrID);
            pck.putSkip(addrIPv4.size);
        }
    }

    /**
     * make database descriptor packet
     *
     * @param pck packet to update
     * @param area area
     * @param flags descriptor flags
     * @param seq sequence number
     */
    protected void mkDescrPack(packHolder pck, rtrOspf6area area, int flags, int seq) {
        pck.merge2beg();
        pck.msbPutD(0, area.getCapabilities()); // optional capabilities
        pck.msbPutW(4, iface.lower.getMTUsize()); // interface mtu
        pck.msbPutW(6, flags); // db flags
        pck.msbPutD(8, seq); // sequence number
        pck.putSkip(12);
    }

    /**
     * make link state update packet
     *
     * @param pck packet to update
     * @param lsa lsa to put
     */
    protected void mkLSupdate(packHolder pck, rtrOspf6lsa lsa) {
        pck.msbPutD(0, 1);
        pck.putSkip(4);
        int i = lsa.writeData(pck, 0, true);
        pck.putSkip(i);
    }

    /**
     * send one packet on this interface
     *
     * @param pck packet to send
     * @param area area
     * @param justDR send just to dr
     * @param typ type of packet
     */
    protected void packSend(packHolder pck, rtrOspf6area area, boolean justDR, int typ) {
        pck.IPdf = false;
        pck.IPfrg = 0;
        pck.IPalrt = -1;
        pck.IPttl = 255;
        pck.IPtos = 0;
        pck.IPid = 0;
        pck.IPprt = rtrOspf6.protoNum;
        pck.IPsrc.setAddr(iface.addr);
        if (justDR) {
            pck.IPtrg.fromString("ff02::6");
        } else {
            pck.IPtrg.fromString("ff02::5");
        }
        mkPackHead(pck, area, typ);
        lower.fwdCore.protoPack(iface, null, pck);
    }

    /**
     * send hello packet
     *
     * @param area area
     */
    protected void sendHello(rtrOspf6area area) {
        if (passiveInt) {
            return;
        }
        packHolder pck = new packHolder(true, true);
        mkHelloPack(pck, area);
        switch (networkType) {
            case netypP2mp:
            case netypP2p:
            case netypBrdct:
                packSend(pck, area, false, rtrOspf6neigh.msgTypHello);
                return;
            case netypNbma:
            case netypP2nb:
                break;
            default:
                return;
        }
        for (int i = 0; i < neighs.size(); i++) {
            rtrOspf6neigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            if (ntry.area.area != area.area) {
                continue;
            }
            ntry.packSend(pck.copyBytes(true, true), rtrOspf6neigh.msgTypHello);
        }
    }

    /**
     * get protocol number
     *
     * @return number
     */
    public int getProtoNum() {
        return rtrOspf6.protoNum;
    }

    /**
     * get counter
     *
     * @return counter
     */
    public counter getCounter() {
        return cntr;
    }

    /**
     * close interface
     *
     * @param iface interface
     */
    public void closeUp(ipFwdIface iface) {
        restartTimer(true);
        unregister2ip();
        closeNeighbors(true);
    }

    /**
     * set state
     *
     * @param iface interface
     * @param stat state
     */
    public void setState(ipFwdIface iface, state.states stat) {
        if (stat == state.states.up) {
            return;
        }
        closeNeighbors(false);
    }

    /**
     * received packet
     *
     * @param iface interface
     * @param pck packet
     */
    public void recvPack(ipFwdIface iface, packHolder pck) {
        cntr.rx(pck);
        if (passiveInt) {
            cntr.drop(pck, counter.reasons.notUp);
            return;
        }
        if ((connectedCheck) && (!iface.network.matches(pck.IPsrc))) {
            logger.info("got from out of subnet peer " + pck.IPsrc);
            cntr.drop(pck, counter.reasons.badAddr);
            return;
        }
        rtrOspf6area ara = new rtrOspf6area(lower, pck.msbGetD(8));
        ara = areas.find(ara);
        if (ara == null) {
            logger.info("got invalid area from " + pck.IPsrc);
            iface.cntr.drop(pck, counter.reasons.badID);
            return;
        }
        rtrOspf6neigh nei = new rtrOspf6neigh(lower, ara, this, pck.IPsrc.toIPv6());
        rtrOspf6neigh old = neighs.add(nei);
        boolean sndHll = false;
        if (old != null) {
            nei = old;
        } else {
            nei.startNow();
            sndHll = true;
        }
        try {
            nei.recvPack(pck);
            if (sndHll) {
                sendHello(ara);
            }
        } catch (Exception e) {
            logger.traceback(e);
        }
    }

    /**
     * alert packet
     *
     * @param rxIfc interface
     * @param pck packet
     * @return false if success, true if error
     */
    public boolean alertPack(ipFwdIface rxIfc, packHolder pck) {
        return true;
    }

    /**
     * error packet
     *
     * @param err error code
     * @param rtr address
     * @param rxIfc interface
     * @param pck packet
     */
    public void errorPack(counter.reasons err, addrIP rtr, ipFwdIface rxIfc, packHolder pck) {
    }

}

class rtrOspf6ifaceHello extends TimerTask {

    private final rtrOspf6iface lower;

    public rtrOspf6ifaceHello(rtrOspf6iface parent) {
        lower = parent;
    }

    public void run() {
        try {
            lower.electDRs();
            for (int i = 0; i < lower.areas.size(); i++) {
                rtrOspf6area area = lower.areas.get(i);
                if (area == null) {
                    continue;
                }
                lower.sendHello(area);
            }
        } catch (Exception e) {
            logger.traceback(e);
        }
    }

}
