package org.freertr.cfg;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.freertr.auth.authLocal;
import org.freertr.clnt.clntDns;
import org.freertr.ifc.ifcThread;
import org.freertr.ifc.ifcUdpInt;
import org.freertr.ip.ipFwdTab;
import org.freertr.line.lineTcpLine;
import org.freertr.pipe.pipeConnect;
import org.freertr.pipe.pipeConsole;
import org.freertr.pipe.pipeImage;
import org.freertr.pipe.pipeLine;
import org.freertr.pipe.pipeReader;
import org.freertr.pipe.pipeSetting;
import org.freertr.pipe.pipeSide;
import org.freertr.pipe.pipeWindow;
import org.freertr.prt.prtLocTcp;
import org.freertr.prt.prtRedun;
import org.freertr.prt.prtWatch;
import org.freertr.serv.servAmt;
import org.freertr.serv.servBmp2mrt;
import org.freertr.serv.servBstun;
import org.freertr.serv.servCharGen;
import org.freertr.serv.servDaytime;
import org.freertr.serv.servDcp;
import org.freertr.serv.servDhcp4;
import org.freertr.serv.servDhcp6;
import org.freertr.serv.servDiscard;
import org.freertr.serv.servDns;
import org.freertr.serv.servEchoP;
import org.freertr.serv.servEchoS;
import org.freertr.serv.servEtherIp;
import org.freertr.serv.servForwarder;
import org.freertr.serv.servFtp;
import org.freertr.serv.servGeneric;
import org.freertr.serv.servGeneve;
import org.freertr.serv.servGopher;
import org.freertr.serv.servGre;
import org.freertr.serv.servGtp;
import org.freertr.serv.servHoneyPot;
import org.freertr.serv.servHttp;
import org.freertr.serv.servImap4;
import org.freertr.serv.servIrc;
import org.freertr.serv.servIscsi;
import org.freertr.serv.servL2f;
import org.freertr.serv.servL2tp2;
import org.freertr.serv.servL2tp3;
import org.freertr.serv.servLoadBalancer;
import org.freertr.serv.servLpd;
import org.freertr.serv.servModem;
import org.freertr.serv.servMplsIp;
import org.freertr.serv.servMplsOam;
import org.freertr.serv.servMplsUdp;
import org.freertr.serv.servMultiplexer;
import org.freertr.serv.servNetflow;
import org.freertr.serv.servNrpe;
import org.freertr.serv.servNtp;
import org.freertr.serv.servOpenflow;
import org.freertr.serv.servPktmux;
import org.freertr.serv.servP4lang;
import org.freertr.serv.servPcep;
import org.freertr.serv.servPckOdtls;
import org.freertr.serv.servPckOtcp;
import org.freertr.serv.servPckOtxt;
import org.freertr.serv.servPckOudp;
import org.freertr.serv.servPop3;
import org.freertr.serv.servPptp;
import org.freertr.serv.servPrometheus;
import org.freertr.serv.servQuote;
import org.freertr.serv.servRadius;
import org.freertr.serv.servRfb;
import org.freertr.serv.servRpki;
import org.freertr.serv.servSdwan;
import org.freertr.serv.servSip;
import org.freertr.serv.servSmtp;
import org.freertr.serv.servSnmp;
import org.freertr.serv.servSocks;
import org.freertr.serv.servStreamingMdt;
import org.freertr.serv.servStun;
import org.freertr.serv.servSyslog;
import org.freertr.serv.servTacacs;
import org.freertr.serv.servTelnet;
import org.freertr.serv.servTftp;
import org.freertr.serv.servTime;
import org.freertr.serv.servTwamp;
import org.freertr.serv.servUdpFwd;
import org.freertr.serv.servUdptn;
import org.freertr.serv.servUni2multi;
import org.freertr.serv.servUpnpFwd;
import org.freertr.serv.servUpnpHub;
import org.freertr.serv.servVoice;
import org.freertr.serv.servVxlan;
import org.freertr.enc.encUrl;
import org.freertr.ip.ipRtr;
import org.freertr.serv.servMrt2bgp;
import org.freertr.serv.servPlan9;
import org.freertr.serv.servStack;
import org.freertr.serv.servUni2uni;
import org.freertr.serv.servWhois;
import org.freertr.serv.servXotPad;
import org.freertr.tab.tabGen;
import org.freertr.tab.tabRouteAttr;
import org.freertr.tab.tabRouteIface;
import org.freertr.user.userConfig;
import org.freertr.user.userExec;
import org.freertr.user.userFilter;
import org.freertr.user.userFlash;
import org.freertr.user.userFonts;
import org.freertr.user.userHelping;
import org.freertr.user.userHwdet;
import org.freertr.user.userNetconf;
import org.freertr.user.userReader;
import org.freertr.user.userScreen;
import org.freertr.util.bits;
import org.freertr.util.cmds;
import org.freertr.util.counter;
import org.freertr.util.debugger;
import org.freertr.util.history;
import org.freertr.util.logBuf;
import org.freertr.util.logger;
import org.freertr.util.version;

/**
 * hardware configuration
 *
 * @author matecsaba
 */
public class cfgInit implements Runnable {

    /**
     * create instance
     */
    private cfgInit() {
    }

    /**
     * sw config end
     */
    public final static String swCfgEnd = "sw.txt";

    /**
     * hw config end
     */
    public final static String hwCfgEnd = "hw.txt";

    /**
     * redundancy priority
     */
    public static int redunPrio;

    /**
     * set until boot completes
     */
    public static boolean booting = true;

    /**
     * time when started
     */
    public static long started = -1;

    /**
     * read-write path name
     */
    public static String rwPath;

    /**
     * hw config file in use
     */
    public static String cfgFileHw;

    /**
     * sw config file in use
     */
    public static String cfgFileSw;

    /**
     * state save file in use
     */
    public static String stateFile;

    /**
     * hardware serial number
     */
    public static String hwIdNum;

    /**
     * hardware serial number
     */
    public static String hwSnNum;

    /**
     * hostname of parent
     */
    public static String prntNam;

    /**
     * jvm parameters
     */
    public static String jvmParam = "";

    /**
     * timer history
     */
    public static history timerHistory;

    /**
     * memory history
     */
    public static history memoryHistory;

    /**
     * loaded snmp mibs
     */
    public final static tabGen<userFilter> snmpMibs = new tabGen<userFilter>();

    /**
     * list of physical interfaces
     */
    public final static tabGen<cfgVdcIfc> ifaceLst = new tabGen<cfgVdcIfc>();

    /**
     * list of started vdcs
     */
    public final static tabGen<cfgVdc> vdcLst = new tabGen<cfgVdc>();

    /**
     * list of started vnets
     */
    public final static tabGen<cfgVnet> vnetLst = new tabGen<cfgVnet>();

    /**
     * no stall check
     */
    public static boolean noStallCheck = false;

    /**
     * vdc port range
     */
    public static int vdcPortBeg = 10240;

    /**
     * vdc port range
     */
    public static int vdcPortEnd = 32768;

    /**
     * interface names
     */
    public static userHelping ifaceNames = new userHelping();

    private static List<String> stateLast = new ArrayList<String>();

    private final static tabGen<cfgInitMime> types = new tabGen<cfgInitMime>();

    private static long jvmStarted = -1;

    private static boolean jvmSetup = false;

    private final static String[] needInit = {
        "interface .*",
        "aaa .*",
        "vrf definition .*",
        "access-list .*",
        "prefix-list .*",
        "policy-map .*",
        "route-map .*",
        "route-policy .*",
        "proxy-profile .*",
        "vdc definition .*",};

    private final static String[] needFull = {
        "vnet .*",};

    private final static String[] needIface = {
        "interface .*!" + cmds.tabulator + "vrf forwarding .*",
        "interface .*!" + cmds.tabulator + "ipv4 address .*",
        "interface .*!" + cmds.tabulator + "ipv6 address .*"
    };

    private final static String[] jvmMagic = {
        "java.net.preferIPv4Stack=true",
        "java.net.preferIPv6Addresses=false"
    };

    private final static int bootLogo = 0x1fd;

    /**
     * get mime type of an extesion
     *
     * @param s extension possibly starting with dot.
     * @return mime type
     */
    public final static String findMimeType(String s) {
        if (s.startsWith("//")) {
            return s.substring(2, s.length());
        }
        s = s.trim().toLowerCase();
        int i = s.lastIndexOf(".");
        if (i >= 0) {
            s = s.substring(i + 1, s.length());
        }
        cfgInitMime ntry = new cfgInitMime(s);
        ntry = types.find(ntry);
        if (ntry != null) {
            return ntry.mime;
        }
        ntry = new cfgInitMime("*");
        ntry = types.find(ntry);
        if (ntry != null) {
            return ntry.mime;
        }
        return "*/*";
    }

    /**
     * name of backup configuration
     *
     * @return null if disabled, file name
     */
    public final static String getBackupCfgName() {
        if (cfgAll.configBackup == null) {
            return null;
        }
        String a = cfgAll.configBackup;
        if (a.length() > 0) {
            return a;
        }
        a = cfgFileSw;
        int i = a.lastIndexOf(".");
        if (i > 0) {
            a = a.substring(0, i);
        }
        a = a + ".bak";
        return a;
    }

    /**
     * get http url
     *
     * @param url url
     * @return text read
     */
    public final static List<String> httpGet(String url) {
        if (url == null) {
            url = "";
        }
        if (encUrl.parseOne(url).proto.length() < 1) {
            return bits.txt2buf(url);
        }
        setupJVM();
        try {
            List<String> res = new ArrayList<String>();
            InputStream strm = new URI(url).toURL().openStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(strm));
            for (;;) {
                String a = rd.readLine();
                if (a == null) {
                    break;
                }
                res.add(a);
            }
            rd.close();
            strm.close();
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    private final static void setupJVM() {
        if (jvmSetup) {
            return;
        }
        jvmSetup = true;
        try {
            Thread.setDefaultUncaughtExceptionHandler(new cfgInitHandler());
        } catch (Exception e) {
            logger.error("error catching jvm");
        }
        for (int o = 0; o < jvmMagic.length; o++) {
            String s = jvmMagic[o];
            int i = s.indexOf("=");
            String c = s.substring(i + 1, s.length());
            s = s.substring(0, i);
            try {
                System.setProperty(s, c);
            } catch (Exception e) {
                logger.error("error setting jvm:" + s + "=" + c);
            }
        }
    }

    private static String doTrimmer(String s) {
        byte[] b = s.getBytes();
        for (int i = 0; i < s.length(); i++) {
            int o = b[i] & 0xff;
            if ((o <= 31) || (o >= 127)) {
                o = 95;
            }
            b[i] = (byte) o;
        }
        return new String(b).trim();
    }

    /**
     * execute hw commands
     *
     * @param read commands
     * @param defs defaults
     * @param inhs inheritables
     * @param cfgs configs
     */
    public final static void executeHWcommands(List<String> read, List<String> defs, List<String> inhs, List<String> cfgs) {
        if (read == null) {
            return;
        }
        for (int cnt = 0; cnt < read.size(); cnt++) {
            String s = doTrimmer(read.get(cnt));
            if (s.length() < 1) {
                continue;
            }
            if (s.startsWith(cmds.comment)) {
                continue;
            }
            if (debugger.cfgInitHw) {
                logger.debug("cmd " + s);
            }
            cmds cmd = new cmds("hw", s);
            s = cmd.word().toLowerCase();
            if (s.equals("limited")) {
                cfgAll.invdc = true;
                continue;
            }
            if (s.equals("save")) {
                stateFile = cmd.getRemaining();
                continue;
            }
            if (s.equals("hwid")) {
                hwIdNum = cmd.getRemaining();
                continue;
            }
            if (s.equals("hwsn")) {
                hwSnNum = cmd.getRemaining();
                continue;
            }
            if (s.equals("prnt")) {
                prntNam = cmd.getRemaining();
                continue;
            }
            if (s.equals("jvm")) {
                jvmParam = cmd.getRemaining();
                continue;
            }
            if (s.equals("debug")) {
                debugger.setByName(cmd, true);
                continue;
            }
            if (s.equals("url")) {
                cfgAll.upgradeServer = cmd.getRemaining();
                continue;
            }
            if (s.equals("key")) {
                cfgAll.upgradePubKey = cmd.getRemaining();
                continue;
            }
            if (s.equals("enc")) {
                cfgAll.passEnh = authLocal.passwdDecode(cmd.word());
                continue;
            }
            if (s.equals("hidevrf")) {
                cfgVrf vrf = cfgAll.vrfFind(cmd.word(), true);
                if (vrf == null) {
                    continue;
                }
                vrf.hidden = true;
                continue;
            }
            if (s.equals("hideifc")) {
                cfgIfc ifc = cfgAll.ifcFind(cmd.word(), 0);
                if (ifc == null) {
                    continue;
                }
                ifc.hidden = true;
                continue;
            }
            if (s.equals("tcp2vrf")) {
                int loc = bits.str2num(cmd.word());
                cfgVrf vrf = cfgAll.vrfFind(cmd.word(), true);
                int rem = bits.str2num(cmd.word());
                String bind = cmd.word();
                String fake = cmd.word();
                prtLocTcp.startServer(loc, vrf, rem, bind, fake);
                continue;
            }
            if (s.equals("prio")) {
                redunPrio = bits.str2num(cmd.word());
                continue;
            }
            if (s.equals("def")) {
                s = cmd.getRemaining();
                defs.add(s);
                cfgAll.defaultF.add(new userFilter("", s, null));
                continue;
            }
            if (s.equals("cfg")) {
                s = cmd.getRemaining();
                cfgs.add(s);
                continue;
            }
            if (s.equals("dcfg")) {
                s = cmd.getRemaining();
                cfgs.add(s);
                defs.add(s);
                cfgAll.defaultF.add(new userFilter("", s, null));
                continue;
            }
            if (s.equals("port")) {
                vdcPortBeg = bits.str2num(cmd.word());
                vdcPortEnd = bits.str2num(cmd.word());
                continue;
            }
            if (s.equals("nostall")) {
                noStallCheck = true;
                continue;
            }
            if (s.equals("rwpath")) {
                rwPath = cmd.getRemaining();
                if (!rwPath.endsWith("/")) {
                    rwPath += "/";
                }
                continue;
            }
            if (s.equals("prcpar")) {
                cfgPrcss prc = new cfgPrcss(cmd.word());
                prc = cfgAll.prcs.find(prc);
                if (prc == null) {
                    continue;
                }
                for (;;) {
                    s = cmd.word();
                    if (s.length() < 1) {
                        break;
                    }
                    boolean neg = s.startsWith(cmds.negated);
                    if (neg) {
                        s = s.substring(2, s.length());
                    }
                    if (s.equals("hid")) {
                        prc.hidden = !neg;
                        continue;
                    }
                    if (s.equals("act")) {
                        prc.logAct = !neg;
                        continue;
                    }
                    if (s.equals("con")) {
                        prc.logCon = !neg;
                        continue;
                    }
                    if (s.equals("col")) {
                        if (neg) {
                            prc.logCol = null;
                            continue;
                        }
                        prc.logCol = new logBuf(bits.str2num(cmd.word()));
                        continue;
                    }
                }
                continue;
            }
            if (s.equals("proc")) {
                cfgPrcss prc = new cfgPrcss(cmd.word());
                prc.hidden = true;
                prc.logAct = true;
                prc.execName = cmd.getRemaining();
                cfgPrcss old = cfgAll.prcs.put(prc);
                prc.startNow();
                if (old == null) {
                    continue;
                }
                old.stopNow();
                continue;
            }
            if (s.equals("vnet")) {
                cfgVnet prc = new cfgVnet(cmd.word());
                prc.hidden = true;
                prc.side1.logAct = true;
                prc.side2.logAct = true;
                prc.side1.ifcTyp = userHwdet.string2type(cmd.word());
                prc.side2.ifcTyp = prc.side1.ifcTyp;
                s = cmd.word().trim();
                if (s.length() > 0) {
                    s = cfgIfc.dissectName(s)[0];
                    if (s.length() < 1) {
                        continue;
                    }
                    prc.side1.locNam = s;
                }
                s = cmd.word().trim();
                if (s.length() > 0) {
                    prc.side2.conNam = s;
                }
                cfgAll.vnets.put(prc);
                continue;
            }
            if (s.equals("int")) {
                String old = cmd.getRemaining();
                String nam = cmd.word();
                String pnm[] = cfgIfc.dissectName(nam);
                if (pnm[0].length() < 1) {
                    continue;
                }
                if (pnm[1].length() > 0) {
                    continue;
                }
                s = cmd.word().toLowerCase();
                int stat = 0;
                if (s.equals("stat")) {
                    stat = 1;
                }
                if (s.equals("red")) {
                    stat = 2;
                }
                if (s.equals("dog")) {
                    stat = 3;
                }
                if (stat != 0) {
                    s = cmd.word().toLowerCase();
                }
                tabRouteIface.ifaceType typ = cfgIfc.string2type(s);
                if (typ == null) {
                    continue;
                }
                String mac = cmd.word();
                String loop = cmd.word();
                int loc = bits.str2num(cmd.word());
                String peer = cmd.word();
                int rem = bits.str2num(cmd.word());
                s = cmd.word();
                int thrd = bits.str2num(s);
                ifcUdpInt hdr = new ifcUdpInt(loop, loc, peer, rem, mac,
                        typ != tabRouteIface.ifaceType.ether, stat == 1);
                switch (stat) {
                    case 2:
                        hdr.booter = true;
                        prtRedun.ifcAdd(nam, hdr, s);
                        break;
                    case 3:
                        hdr.booter = true;
                        prtWatch.ifcAdd(nam, hdr, mac);
                        break;
                    default:
                        cfgIfc ifc = cfgAll.ifcAdd(nam, typ, hdr, thrd);
                        if (ifc == null) {
                            continue;
                        }
                        cfgVdcIfc ntry = new cfgVdcIfc(ifc.name, old);
                        ntry.portL = loc;
                        ntry.portR = rem;
                        ifaceLst.add(ntry);
                        ifc.initPhysical();
                        if (debugger.cfgInitHw) {
                            logger.debug("iface " + hdr);
                        }
                        break;
                }
                continue;
            }
            if (s.equals("line")) {
                String nam = cmd.word();
                boolean nomon = nam.equals("nomon");
                if (nomon) {
                    nam = cmd.word();
                }
                String loop = cmd.word();
                int loc = bits.str2num(cmd.word());
                String peer = cmd.word();
                int rem = bits.str2num(cmd.word());
                lineTcpLine hdr = new lineTcpLine(loop, loc, peer, rem);
                cfgLin lin = cfgAll.linAdd(nam, hdr);
                if (debugger.cfgInitHw) {
                    logger.debug("line " + hdr);
                }
                if (nomon) {
                    continue;
                }
                List<String> logo = version.shLogo(bootLogo);
                for (int i = 0; i < logo.size(); i++) {
                    lin.sendLine(logo.get(i));
                }
                lin.runner.setMon(true);
                continue;
            }
            if (s.equals("netconf")) {
                s = cmd.getRemaining();
                List<String> txt = httpGet(s);
                if (txt == null) {
                    continue;
                }
                inhs.add(cmd.getOriginal());
                int bg = -1;
                int md = 0;
                for (int p = 0; p < txt.size(); p++) {
                    String a = txt.get(p);
                    if (a.startsWith("sensor ")) {
                        bg = p;
                        md = 1;
                        continue;
                    }
                    if (a.startsWith("config ")) {
                        bg = p;
                        md = 2;
                        continue;
                    }
                    if (!a.equals(".")) {
                        continue;
                    }
                    cmd = new cmds("", txt.get(bg));
                    cmd.word();
                    switch (md) {
                        case 1:
                            cfgSensor tl = new cfgSensor(cmd.getRemaining());
                            tl.hidden = true;
                            for (int i = bg + 1; i < p; i++) {
                                cmd = new cmds("", txt.get(i));
                                tl.doCfgStr(cmd);
                            }
                            cfgAll.sensors.put(tl);
                            if (debugger.cfgInitHw) {
                                logger.debug("netconf sensor " + tl.name);
                            }
                            break;
                        case 2:
                            a = cmd.word();
                            userNetconf.makeYang(txt, bg + 1, p);
                            if (debugger.cfgInitHw) {
                                logger.debug("netconf config " + a);
                            }
                            break;
                        default:
                            break;
                    }
                    md = 0;
                    continue;
                }
                continue;
            }
            if (s.equals("snmp")) {
                s = cmd.getRemaining();
                List<String> txt = httpGet(s);
                if (txt == null) {
                    continue;
                }
                inhs.add(cmd.getOriginal());
                int bg = -1;
                for (int p = 0; p < txt.size(); p++) {
                    String a = txt.get(p);
                    if (a.startsWith("oid ")) {
                        bg = p;
                    }
                    if (!a.equals(".")) {
                        continue;
                    }
                    cmd = new cmds("", txt.get(bg));
                    cmd.word();
                    a = cmd.word();
                    userFilter sn = new userFilter(a, cmd.getRemaining(), new ArrayList<String>());
                    sn.listing.addAll(txt.subList(bg + 1, p));
                    snmpMibs.put(sn);
                    if (debugger.cfgInitHw) {
                        logger.debug("snmp " + sn);
                    }
                }
                continue;
            }
            logger.info((cnt + 1) + ":" + cmd.getOriginal());
        }
    }

    /**
     * execute sw commands
     *
     * @param cs commands
     * @param quiet do not log errors
     * @return number of errors
     */
    public final static int executeSWcommands(List<String> cs, boolean quiet) {
        if (cs == null) {
            return 0;
        }
        if (debugger.cfgInitSw) {
            logger.debug("applying sw config");
        }
        int err = 0;
        pipeLine pl = new pipeLine(65536, false);
        pipeSide psS = pl.getSide();
        pipeSide psC = pl.getSide();
        userReader rd = new userReader(psC, null);
        psC.settingsPut(pipeSetting.height, 0);
        userConfig uc = new userConfig(psC, rd);
        psS.lineRx = pipeSide.modTyp.modeCRorLF;
        psC.lineTx = pipeSide.modTyp.modeCRLF;
        psS.setTime(100000);
        for (int o = 0; o < cs.size(); o++) {
            String a = cs.get(o);
            int i = a.indexOf(cmds.comment);
            if (i >= 0) {
                a = a.substring(0, i);
            }
            a = doTrimmer(a);
            if (a.length() < 1) {
                continue;
            }
            if (debugger.cfgInitSw) {
                logger.debug("cmd " + a);
            }
            String beg = "line " + (o + 1) + ": \"" + a + "\" : ";
            userHelping hl = uc.getHelping(false, true, true);
            rd.setContext(hl, "");
            String b = hl.repairLine(a);
            if (b.length() < 1) {
                err++;
                if (quiet) {
                    continue;
                }
                logger.info(beg + "no such command");
                continue;
            }
            try {
                uc.executeCommand(b);
            } catch (Exception e) {
                err++;
                logger.info(beg + logger.dumpException(e, " at line " + err));
            }
            i = psS.ready2rx();
            if (i < 1) {
                continue;
            }
            err++;
            byte[] buf = new byte[i];
            i = psS.nonBlockGet(buf, 0, buf.length);
            b = new String(buf, 0, i);
            b = b.replaceAll("\r", " ");
            b = b.replaceAll("\n", " ");
            if (quiet) {
                continue;
            }
            logger.info(beg + b);
        }
        return err;
    }

    private final static void doInit(List<String> hw, List<String> sw, pipeSide cons) {
        if (jvmStarted > 0) {
            logger.info("overlapping boot eliminated");
            return;
        }
        jvmStarted = bits.getTime();
        started = bits.getTime();
        logger.info("booting");
        setupJVM();
        if (hw == null) {
            logger.info("no hw config found");
            hw = new ArrayList<String>();
        }
        if (sw == null) {
            logger.info("no sw config found");
            sw = new ArrayList<String>();
        }
        types.add(new cfgInitMime("html", "text/html"));
        types.add(new cfgInitMime("htm", "text/html"));
        types.add(new cfgInitMime("css", "text/css"));
        types.add(new cfgInitMime("rtf", "text/richtext"));
        types.add(new cfgInitMime("text", "text/plain"));
        types.add(new cfgInitMime("txt", "text/plain"));
        types.add(new cfgInitMime("csv", "text/csv"));
        types.add(new cfgInitMime("md", "text/markdown"));
        types.add(new cfgInitMime("*", "text/plain"));
        types.add(new cfgInitMime("webp", "image/webp"));
        types.add(new cfgInitMime("gif", "image/gif"));
        types.add(new cfgInitMime("jpeg", "image/jpeg"));
        types.add(new cfgInitMime("jpg", "image/jpeg"));
        types.add(new cfgInitMime("tiff", "image/tiff"));
        types.add(new cfgInitMime("tif", "image/tiff"));
        types.add(new cfgInitMime("bmp", "image/bmp"));
        types.add(new cfgInitMime("png", "image/png"));
        types.add(new cfgInitMime("svg", "image/svg+xml"));
        types.add(new cfgInitMime("ico", "image/x-icon"));
        types.add(new cfgInitMime("pbm", "image/x-portable-bitmap"));
        types.add(new cfgInitMime("pgm", "image/x-portable-graymap"));
        types.add(new cfgInitMime("pnm", "image/x-portable-anymap"));
        types.add(new cfgInitMime("ppm", "image/x-portable-pixmap"));
        types.add(new cfgInitMime("xbm", "image/x-xbitmap"));
        types.add(new cfgInitMime("xpm", "image/x-xpixmap"));
        types.add(new cfgInitMime("webm", "video/webm"));
        types.add(new cfgInitMime("mjpeg", "video/x-motion-jpeg"));
        types.add(new cfgInitMime("avi", "video/msvideo"));
        types.add(new cfgInitMime("mov", "video/quicktime"));
        types.add(new cfgInitMime("qt", "video/quicktime"));
        types.add(new cfgInitMime("mpeg", "video/mpeg"));
        types.add(new cfgInitMime("mpg", "video/mpeg"));
        types.add(new cfgInitMime("mp4", "video/mp4"));
        types.add(new cfgInitMime("mkv", "video/x-matroska"));
        types.add(new cfgInitMime("3gp", "video/3gpp"));
        types.add(new cfgInitMime("3g2", "video/3gpp2"));
        types.add(new cfgInitMime("ogv", "video/ogg"));
        types.add(new cfgInitMime("weba", "audio/weba"));
        types.add(new cfgInitMime("aif", "audio/x-aiff"));
        types.add(new cfgInitMime("aiff", "audio/x-aiff"));
        types.add(new cfgInitMime("wav", "audio/wav"));
        types.add(new cfgInitMime("midi", "audio/midi"));
        types.add(new cfgInitMime("mid", "audio/midi"));
        types.add(new cfgInitMime("rmi", "audio/midi"));
        types.add(new cfgInitMime("ram", "audio/x-pn-realaudio"));
        types.add(new cfgInitMime("rpm", "audio/x-pn-realaudio-plugin"));
        types.add(new cfgInitMime("ra", "audio/x-realaudio"));
        types.add(new cfgInitMime("rm", "audio/x-pn-realaudio"));
        types.add(new cfgInitMime("mp3", "audio/mpeg"));
        types.add(new cfgInitMime("oga", "audio/ogg"));
        types.add(new cfgInitMime("flac", "audio/flac"));
        types.add(new cfgInitMime("aac", "audio/aac"));
        types.add(new cfgInitMime("bin", "application/octet-stream"));
        types.add(new cfgInitMime("jar", "application/java-archive"));
        types.add(new cfgInitMime("doc", "application/msword"));
        types.add(new cfgInitMime("docx", "application/msword"));
        types.add(new cfgInitMime("dvi", "application/x-dvi"));
        types.add(new cfgInitMime("eps", "application/postscript"));
        types.add(new cfgInitMime("ps", "application/postscript"));
        types.add(new cfgInitMime("gz", "application/x-gzip"));
        types.add(new cfgInitMime("bz2", "application/x-bzip2"));
        types.add(new cfgInitMime("js", "application/javascript"));
        types.add(new cfgInitMime("latex", "application/x-latex"));
        types.add(new cfgInitMime("lzh", "application/x-lzh"));
        types.add(new cfgInitMime("pdf", "application/pdf"));
        types.add(new cfgInitMime("epub", "application/epub+zip"));
        types.add(new cfgInitMime("swf", "application/x-shockwave-flash"));
        types.add(new cfgInitMime("tar", "application/tar"));
        types.add(new cfgInitMime("tcl", "application/x-tcl"));
        types.add(new cfgInitMime("tex", "application/x-tex"));
        types.add(new cfgInitMime("tgz", "application/x-gzip"));
        types.add(new cfgInitMime("zip", "application/zip"));
        types.add(new cfgInitMime("xml", "application/xml"));
        types.add(new cfgInitMime("ogg", "application/ogg"));
        types.add(new cfgInitMime("wml", "text/vnd.wap.wml"));
        types.add(new cfgInitMime("wbmp", "image/vnd.wap.wbmp"));
        ifaceNames.add(null, "1 . loopback      ifc");
        ifaceNames.add(null, "1 . null          ifc");
        ifaceNames.add(null, "1 . template      ifc");
        ifaceNames.add(null, "1 . dialer        ifc");
        ifaceNames.add(null, "1 . sdn           ifc");
        ifaceNames.add(null, "1 . pwether       ifc");
        ifaceNames.add(null, "1 . virtualppp    ifc");
        ifaceNames.add(null, "1 . access        ifc");
        ifaceNames.add(null, "1 . bvi           ifc");
        ifaceNames.add(null, "1 . bundle        ifc");
        ifaceNames.add(null, "1 . tunnel        ifc");
        ifaceNames.add(null, "1 . hairpin       ifc");
        ifaceNames.add(null, "1 . atm           ifc");
        ifaceNames.add(null, "1 . arcnet        ifc");
        ifaceNames.add(null, "1 . infiniband    ifc");
        ifaceNames.add(null, "1 . ethernet      ifc");
        ifaceNames.add(null, "1 . serial        ifc");
        ifaceNames.add(null, "1 . cellular      ifc");
        ifaceNames.add(null, "1 . wireless      ifc");
        cfgIfc.notemplF = createFilter(cfgIfc.notemplL);
        cfgIfc.nocloneF = createFilter(cfgIfc.nocloneL);
        userReader.linedefF = createFilter(userReader.linedefL);
        cfgMenuK.defaultF = createFilter(cfgMenuK.defaultL);
        cfgMenuT.defaultF = createFilter(cfgMenuT.defaultL);
        cfgAll.defaultF = createFilter(cfgAll.defaultL);
        cfgIpsec.defaultF = createFilter(cfgIpsec.defaultL);
        cfgAuther.defaultF = createFilter(cfgAuther.defaultL);
        cfgVdc.defaultF = createFilter(cfgVdc.defaultL);
        cfgPrcss.defaultF = createFilter(cfgPrcss.defaultL);
        cfgVrf.defaultF = createFilter(cfgVrf.defaultL);
        cfgAlias.defaultF = createFilter(cfgAlias.defaultL);
        cfgCert.defaultF = createFilter(cfgCert.defaultL);
        cfgChat.defaultF = createFilter(cfgChat.defaultL);
        cfgHrpn.defaultF = createFilter(cfgHrpn.defaultL);
        cfgKey.defaultF = createFilter(cfgKey.defaultL);
        cfgAceslst.defaultF = createFilter(cfgAceslst.defaultL);
        cfgObjnet.defaultF = createFilter(cfgObjnet.defaultL);
        cfgObjprt.defaultF = createFilter(cfgObjprt.defaultL);
        cfgPrfxlst.defaultF = createFilter(cfgPrfxlst.defaultL);
        cfgBndl.defaultF = createFilter(cfgBndl.defaultL);
        cfgBrdg.defaultF = createFilter(cfgBrdg.defaultL);
        cfgTrnsltn.defaultF = createFilter(cfgTrnsltn.defaultL);
        cfgDial.defaultF = createFilter(cfgDial.defaultL);
        cfgSessn.defaultF = createFilter(cfgSessn.defaultL);
        cfgCheck.defaultF = createFilter(cfgCheck.defaultL);
        cfgSensor.defaultF = createFilter(cfgSensor.defaultL);
        cfgRoump.defaultF = createFilter(cfgRoump.defaultL);
        cfgRouplc.defaultF = createFilter(cfgRouplc.defaultL);
        cfgTime.defaultF = createFilter(cfgTime.defaultL);
        cfgPlymp.defaultF = createFilter(cfgPlymp.defaultL);
        cfgRtr.defaultF = createFilter(cfgRtr.defaultL);
        cfgIfc.defaultF = createFilter(cfgIfc.defaultL);
        cfgLin.defaultF = createFilter(cfgLin.defaultL, userReader.linedefF);
        cfgCons.defaultF = createFilter(cfgCons.defaultL, userReader.linedefF);
        cfgSched.defaultF = createFilter(cfgSched.defaultL);
        cfgScrpt.defaultF = createFilter(cfgScrpt.defaultL);
        cfgTlmtry.defaultF = createFilter(cfgTlmtry.defaultL);
        cfgEvntmgr.defaultF = createFilter(cfgEvntmgr.defaultL);
        cfgTrack.defaultF = createFilter(cfgTrack.defaultL);
        cfgMtrack.defaultF = createFilter(cfgMtrack.defaultL);
        cfgProxy.defaultF = createFilter(cfgProxy.defaultL);
        cfgVpdn.defaultF = createFilter(cfgVpdn.defaultL);
        cfgVnet.defaultF = createFilter(cfgVnet.defaultL);
        cfgXconn.defaultF = createFilter(cfgXconn.defaultL);
        tabGen<userFilter> srvdefsF = createFilter(servGeneric.srvdefsL);
        servBstun.defaultF = createFilter(servBstun.defaultL, srvdefsF, userReader.linedefF);
        servMrt2bgp.defaultF = createFilter(servMrt2bgp.defaultL, srvdefsF);
        servRpki.defaultF = createFilter(servRpki.defaultL, srvdefsF);
        servNrpe.defaultF = createFilter(servNrpe.defaultL, srvdefsF);
        servPrometheus.defaultF = createFilter(servPrometheus.defaultL, srvdefsF);
        servStreamingMdt.defaultF = createFilter(servStreamingMdt.defaultL, srvdefsF);
        servCharGen.defaultF = createFilter(servCharGen.defaultL, srvdefsF);
        servOpenflow.defaultF = createFilter(servOpenflow.defaultL, srvdefsF);
        servPktmux.defaultF = createFilter(servPktmux.defaultL, srvdefsF);
        servP4lang.defaultF = createFilter(servP4lang.defaultL, srvdefsF);
        servStack.defaultF = createFilter(servStack.defaultL, srvdefsF);
        servDaytime.defaultF = createFilter(servDaytime.defaultL, srvdefsF);
        servDcp.defaultF = createFilter(servDcp.defaultL, srvdefsF);
        servSdwan.defaultF = createFilter(servSdwan.defaultL, srvdefsF);
        servPcep.defaultF = createFilter(servPcep.defaultL, srvdefsF);
        servIrc.defaultF = createFilter(servIrc.defaultL, srvdefsF);
        servDhcp4.defaultF = createFilter(servDhcp4.defaultL, srvdefsF);
        servDhcp6.defaultF = createFilter(servDhcp6.defaultL, srvdefsF);
        servDiscard.defaultF = createFilter(servDiscard.defaultL, srvdefsF);
        servDns.defaultF = createFilter(servDns.defaultL, srvdefsF);
        servNetflow.defaultF = createFilter(servNetflow.defaultL, srvdefsF);
        servUdpFwd.defaultF = createFilter(servUdpFwd.defaultL, srvdefsF);
        servUpnpFwd.defaultF = createFilter(servUpnpFwd.defaultL, srvdefsF);
        servUpnpHub.defaultF = createFilter(servUpnpHub.defaultL, srvdefsF);
        servEchoP.defaultF = createFilter(servEchoP.defaultL, srvdefsF);
        servEchoS.defaultF = createFilter(servEchoS.defaultL, srvdefsF);
        servForwarder.defaultF = createFilter(servForwarder.defaultL, srvdefsF);
        servFtp.defaultF = createFilter(servFtp.defaultL, srvdefsF);
        servGopher.defaultF = createFilter(servGopher.defaultL, srvdefsF);
        servPlan9.defaultF = createFilter(servPlan9.defaultL, srvdefsF);
        servGtp.defaultF = createFilter(servGtp.defaultL, srvdefsF);
        servHoneyPot.defaultF = createFilter(servHoneyPot.defaultL, srvdefsF);
        servWhois.defaultF = createFilter(servWhois.defaultL, srvdefsF);
        servHttp.defaultF = createFilter(servHttp.defaultL, srvdefsF);
        servIscsi.defaultF = createFilter(servIscsi.defaultL, srvdefsF);
        servBmp2mrt.defaultF = createFilter(servBmp2mrt.defaultL, srvdefsF);
        servVxlan.defaultF = createFilter(servVxlan.defaultL, srvdefsF);
        servGeneve.defaultF = createFilter(servGeneve.defaultL, srvdefsF);
        servL2f.defaultF = createFilter(servL2f.defaultL, srvdefsF);
        servL2tp2.defaultF = createFilter(servL2tp2.defaultL, srvdefsF);
        servL2tp3.defaultF = createFilter(servL2tp3.defaultL, srvdefsF);
        servEtherIp.defaultF = createFilter(servEtherIp.defaultL, srvdefsF);
        servGre.defaultF = createFilter(servGre.defaultL, srvdefsF);
        servMplsIp.defaultF = createFilter(servMplsIp.defaultL, srvdefsF);
        servMplsUdp.defaultF = createFilter(servMplsUdp.defaultL, srvdefsF);
        servMplsOam.defaultF = createFilter(servMplsOam.defaultL, srvdefsF);
        servTwamp.defaultF = createFilter(servTwamp.defaultL, srvdefsF);
        servAmt.defaultF = createFilter(servAmt.defaultL, srvdefsF);
        servUni2multi.defaultF = createFilter(servUni2multi.defaultL, srvdefsF);
        servUni2uni.defaultF = createFilter(servUni2uni.defaultL, srvdefsF);
        servLoadBalancer.defaultF = createFilter(servLoadBalancer.defaultL, srvdefsF);
        servMultiplexer.defaultF = createFilter(servMultiplexer.defaultL, srvdefsF);
        servLpd.defaultF = createFilter(servLpd.defaultL, srvdefsF);
        servNtp.defaultF = createFilter(servNtp.defaultL, srvdefsF);
        servPckOdtls.defaultF = createFilter(servPckOdtls.defaultL, srvdefsF);
        servPckOtcp.defaultF = createFilter(servPckOtcp.defaultL, srvdefsF);
        servPckOtxt.defaultF = createFilter(servPckOtxt.defaultL, srvdefsF);
        servPckOudp.defaultF = createFilter(servPckOudp.defaultL, srvdefsF);
        servPop3.defaultF = createFilter(servPop3.defaultL, srvdefsF);
        servImap4.defaultF = createFilter(servImap4.defaultL, srvdefsF);
        servPptp.defaultF = createFilter(servPptp.defaultL, srvdefsF);
        servQuote.defaultF = createFilter(servQuote.defaultL, srvdefsF);
        servRadius.defaultF = createFilter(servRadius.defaultL, srvdefsF);
        servRfb.defaultF = createFilter(servRfb.defaultL, srvdefsF, userReader.linedefF);
        servModem.defaultF = createFilter(servModem.defaultL, srvdefsF, userReader.linedefF);
        servVoice.defaultF = createFilter(servVoice.defaultL, srvdefsF);
        servSip.defaultF = createFilter(servSip.defaultL, srvdefsF);
        servSmtp.defaultF = createFilter(servSmtp.defaultL, srvdefsF);
        servSnmp.defaultF = createFilter(servSnmp.defaultL, srvdefsF);
        servSocks.defaultF = createFilter(servSocks.defaultL, srvdefsF);
        servStun.defaultF = createFilter(servStun.defaultL, srvdefsF);
        servSyslog.defaultF = createFilter(servSyslog.defaultL, srvdefsF);
        servTacacs.defaultF = createFilter(servTacacs.defaultL, srvdefsF);
        servTelnet.defaultF = createFilter(servTelnet.defaultL, srvdefsF, userReader.linedefF);
        servXotPad.defaultF = createFilter(servXotPad.defaultL, srvdefsF, userReader.linedefF);
        servTftp.defaultF = createFilter(servTftp.defaultL, srvdefsF);
        servTime.defaultF = createFilter(servTime.defaultL, srvdefsF);
        servUdptn.defaultF = createFilter(servUdptn.defaultL, srvdefsF, userReader.linedefF);
        List<String> sdefs = new ArrayList<String>();
        for (int i = 0; i < cfgAll.defaultF.size(); i++) {
            userFilter ntry = cfgAll.defaultF.get(i);
            if (ntry.section.length() > 0) {
                continue;
            }
            sdefs.add(ntry.command);
        }
        List<String> inis = new ArrayList<String>();
        List<userFilter> secs = userFilter.text2section(sw);
        for (int i = 0; i < needInit.length; i++) {
            inis.addAll(userFilter.getSecList(secs, needInit[i], cmds.tabulator + cmds.finish));
        }
        for (int i = 0; i < needFull.length; i++) {
            inis.addAll(userFilter.section2text(userFilter.getSection(secs, needFull[i], true, false, false), true));
        }
        List<String> ints = userFilter.section2text(userFilter.filter2text(secs, createFilter(needIface)), true);
        List<String> hcfgs = new ArrayList<String>();
        List<String> hdefs = new ArrayList<String>();
        List<String> inhs = new ArrayList<String>();
        logger.info("initializing hardware");
        try {
            executeHWcommands(hw, hdefs, inhs, hcfgs);
        } catch (Exception e) {
            logger.exception(e);
        }
        logger.info("applying defaults");
        try {
            executeSWcommands(sdefs, false);
        } catch (Exception e) {
            logger.traceback(e);
        }
        try {
            executeSWcommands(hdefs, false);
        } catch (Exception e) {
            logger.traceback(e);
        }
        try {
            executeSWcommands(inis, true);
        } catch (Exception e) {
            logger.traceback(e);
        }
        try {
            executeSWcommands(ints, true);
        } catch (Exception e) {
            logger.traceback(e);
        }
        for (int i = 0; i < cfgAll.vnets.size(); i++) {
            cfgVnet ntry = cfgAll.vnets.get(i).copyBytes();
            ntry.startNow(vdcPortBeg + (i * 4));
            vnetLst.add(ntry);
        }
        vdcPortBeg += (vnetLst.size() * 4);
        logger.info("applying configuration");
        int res = 0;
        try {
            res = executeSWcommands(sw, false);
        } catch (Exception e) {
            logger.traceback(e);
        }
        if (res > 0) {
            logger.error(res + " errors found");
        }
        try {
            executeSWcommands(hcfgs, true);
        } catch (Exception e) {
            logger.traceback(e);
        }
        int p = cfgAll.vdcs.size();
        if (p > 0) {
            p = (vdcPortEnd - vdcPortBeg) / p;
        } else {
            p = 1024;
        }
        for (int i = 0; i < cfgAll.vdcs.size(); i++) {
            cfgVdc ntry = cfgAll.vdcs.get(i).copyBytes();
            vdcLst.add(ntry);
            int o = (i * p) + vdcPortBeg;
            ntry.startNow(hdefs, inhs, o, o + p);
        }
        cfgAll.con0.line.execTimeOut = 0;
        try {
            prtRedun.doInit(cons);
        } catch (Exception e) {
            logger.exception(e);
        }
        stateLoad();
        started = bits.getTime();
        booting = false;
        new Thread(new cfgInit()).start();
        logger.info("boot completed");
    }

    private final static void stateLoad() {
        List<String> txt = bits.txt2buf(version.myStateFile());
        if (txt == null) {
            return;
        }
        userFlash.delete(version.myStateFile());
        int o = 0;
        for (int i = 0; i < txt.size(); i++) {
            cmds cmd = new cmds("rst", txt.get(i));
            tabRouteAttr.routeType t = cfgRtr.name2num(cmd.word());
            if (t == null) {
                continue;
            }
            cfgRtr c = cfgAll.rtrFind(t, bits.str2num(cmd.word()), false);
            if (c == null) {
                continue;
            }
            ipRtr r = c.getRouter();
            if (r == null) {
                continue;
            }
            boolean b = true;
            try {
                b = r.routerStateSet(cmd);
            } catch (Exception e) {
                logger.traceback(e);
            }
            if (b) {
                continue;
            }
            o++;
        }
        logger.info("restored " + o + " of " + txt.size());
    }

    /**
     * save state
     */
    public final static void stateSave() {
        List<String> res = new ArrayList<String>();
        for (int i = 0; i < cfgAll.routers.size(); i++) {
            cfgRtr c = cfgAll.routers.get(i);
            if (c == null) {
                continue;
            }
            ipRtr e = c.getRouter();
            if (e == null) {
                continue;
            }
            e.routerStateGet(res);
        }
        boolean e = res.size() == stateLast.size();
        if (e) {
            for (int i = 0; i < res.size(); i++) {
                e = res.get(i).equals(stateLast.get(i));
                if (!e) {
                    break;
                }
            }
        }
        if (e) {
            return;
        }
        stateLast = res;
        bits.buf2txt(true, res, version.myStateFile());
        prtRedun.doState();
    }

    private final static tabGen<userFilter> createFilter(String[] lst) {
        tabGen<userFilter> res = new tabGen<userFilter>();
        for (int o = 0; o < lst.length; o++) {
            String s = lst[o];
            int i = s.indexOf("!");
            String c = s.substring(i + 1, s.length());
            s = s.substring(0, i);
            userFilter ntry = new userFilter(s, c, null);
            ntry.optimize4lookup();
            res.add(ntry);
        }
        return res;
    }

    private final static void addFilters(tabGen<userFilter> trg, tabGen<userFilter> src) {
        for (int i = 0; i < src.size(); i++) {
            userFilter ntry = src.get(i);
            trg.add(ntry);
        }
    }

    private final static tabGen<userFilter> createFilter(String[] lst, tabGen<userFilter> s1) {
        tabGen<userFilter> r = createFilter(lst);
        addFilters(r, s1);
        return r;
    }

    private final static tabGen<userFilter> createFilter(String[] lst, tabGen<userFilter> s1, tabGen<userFilter> s2) {
        tabGen<userFilter> r = createFilter(lst);
        addFilters(r, s1);
        addFilters(r, s2);
        return r;
    }

    /**
     * stop router
     *
     * @param clean clean exit
     * @param code exit code, negative just updates reload file, 22 already used
     * @param reason reason string
     */
    public final static void stopRouter(boolean clean, int code, String reason) {
        boolean fake = code < 0;
        if (fake) {
            code = -code;
        }
        try {
            bits.buf2txt(true, bits.str2lst("code#" + code + "=" + reason), version.myReloadFile());
        } catch (Exception e) {
        }
        if (fake) {
            return;
        }
        try {
            debugger.setAll(false);
        } catch (Exception e) {
        }
        if (clean && cfgAll.graceReload) {
            for (int i = 0; i < cfgAll.vrfs.size(); i++) {
                try {
                    cfgAll.vrfs.get(i).closeAllConns(true);
                } catch (Exception e) {
                }
            }
            prtRedun.doShut();
            prtWatch.doShut();
            bits.sleep(100);
        }
        for (int i = 0; i < vnetLst.size(); i++) {
            try {
                vnetLst.get(i).stopNow();
            } catch (Exception e) {
            }
        }
        for (int i = 0; i < vdcLst.size(); i++) {
            try {
                vdcLst.get(i).stopNow();
            } catch (Exception e) {
            }
        }
        for (int i = 0; i < cfgAll.prcs.size(); i++) {
            try {
                cfgAll.prcs.get(i).stopNow();
            } catch (Exception e) {
            }
        }
        logger.error("shutdown code=" + code + " reason=" + reason);
        logger.fileName(null);
        System.exit(code);
    }

    /**
     * start applet
     *
     * @param url config url
     * @return image
     */
    public final static pipeImage doApplet(String url) {
        pipeLine pl = new pipeLine(65536, false);
        pipeImage img = new pipeImage(pl.getSide(), 80, 25, userFonts.font8x16(), userFonts.colorData);
        pipeSide ps = pl.getSide();
        ps.lineTx = pipeSide.modTyp.modeCRLF;
        ps.lineRx = pipeSide.modTyp.modeCRorLF;
        ps.setTime(0);
        logger.pipeStart(ps);
        List<String> logo = version.shLogo(bootLogo);
        for (int i = 0; i < logo.size(); i++) {
            ps.linePut(logo.get(i));
        }
        doInit(null, httpGet(url), null);
        cfgAll.con0.line.createHandler(ps, "applet", 2);
        img.scr.doRound(true);
        img.doImage();
        return img;
    }

    /**
     * do main task
     *
     * @param args parameters
     */
    public final static void doMain(String[] args) {
        String s = "";
        if (args.length > 0) {
            s = args[0];
        }
        if (s.startsWith("router")) {
            boolean det = false;
            boolean con = false;
            boolean win = false;
            String hwN = args[1];
            String swN = null;
            for (int i = 6; i < s.length(); i++) {
                String a = "" + s.charAt(i);
                if (a.equals("s")) {
                    hwN = args[1];
                    swN = args[2];
                    continue;
                }
                if (a.equals("a")) {
                    hwN = null;
                    swN = args[1];
                    continue;
                }
                if (a.equals("c")) {
                    con = true;
                    continue;
                }
                if (a.equals("w")) {
                    win = true;
                    continue;
                }
                if (a.equals("d")) {
                    det = true;
                    continue;
                }
            }
            pipeSide pipCon = null;
            pipeSide pipWin = null;
            if (con) {
                pipCon = pipeConsole.create();
                logger.pipeStart(pipCon);
            }
            if (win) {
                pipWin = pipeWindow.createOne(80, 25, userFonts.font8x16(), userFonts.colorData);
                logger.pipeStart(pipWin);
            }
            if (swN == null) {
                swN = hwN + swCfgEnd;
                hwN += hwCfgEnd;
            }
            List<String> logo = version.shLogo(bootLogo);
            for (int i = 0; i < logo.size(); i++) {
                if (pipCon != null) {
                    pipCon.linePut(logo.get(i));
                }
                if (pipWin != null) {
                    pipWin.linePut(logo.get(i));
                }
            }
            cfgFileHw = hwN;
            cfgFileSw = swN;
            List<String> hwT = httpGet(cfgFileHw);
            List<String> swT = httpGet(cfgFileSw);
            doInit(hwT, swT, pipCon);
            if (pipCon != null) {
                if (det) {
                    userScreen.updtSiz(pipCon);
                }
                cfgAll.con0.line.createHandler(pipCon, "console", 2);
            }
            if (pipWin != null) {
                cfgAll.con0.line.createHandler(pipWin, "window", 2);
            }
            return;
        }
        if (s.startsWith("cfgexec")) {
            boolean det = false;
            for (int i = 7; i < s.length(); i++) {
                String a = "" + s.charAt(i);
                if (a.equals("d")) {
                    det = true;
                    continue;
                }
            }
            cfgFileSw = args[1];
            s = "";
            for (int i = 2; i < args.length; i++) {
                s += " " + args[i];
            }
            pipeSide pip = pipeConsole.create();
            logger.pipeStart(pip);
            List<String> logo = version.shLogo(bootLogo);
            for (int i = 0; i < logo.size(); i++) {
                pip.linePut(logo.get(i));
            }
            List<String> swT = httpGet(cfgFileSw);
            doInit(null, swT, pip);
            logger.pipeStart(pip);
            userReader rdr = new userReader(pip, null);
            pip.settingsPut(pipeSetting.height, 0);
            if (det) {
                userScreen.updtSiz(pip);
            }
            userExec exe = new userExec(pip, rdr);
            exe.privileged = true;
            s = exe.repairCommand(s);
            try {
                exe.executeCommand(s);
            } catch (Exception e) {
                logger.exception(e);
            }
            stopRouter(true, 1, "finished");
            return;
        }
        if (s.equals("show")) {
            s = "";
            for (int i = 0; i < args.length; i++) {
                s += " " + args[i];
            }
            pipeLine pl = new pipeLine(1024 * 1024, false);
            pipeSide pip = pl.getSide();
            pip.lineTx = pipeSide.modTyp.modeCRLF;
            pip.lineRx = pipeSide.modTyp.modeCRorLF;
            userReader rdr = new userReader(pip, null);
            pip.settingsPut(pipeSetting.height, 0);
            userExec exe = new userExec(pip, rdr);
            exe.privileged = true;
            s = exe.repairCommand(s);
            try {
                exe.executeCommand(s);
            } catch (Exception e) {
                logger.exception(e);
            }
            pip = pl.getSide();
            pl.setClose();
            pipeReader rd = new pipeReader();
            rd.setLineMode(pipeSide.modTyp.modeCRtryLF);
            pipeConnect.connect(pip, rd.getPipe(), true);
            rd.waitFor();
            List<String> res = rd.getResult();
            for (int i = 0; i < res.size(); i++) {
                putln(res.get(i));
            }
            return;
        }
        boolean b = s.startsWith("exec");
        if (b || s.startsWith("test")) {
            boolean det = false;
            for (int i = 4; i < s.length(); i++) {
                String a = "" + s.charAt(i);
                if (a.equals("d")) {
                    det = true;
                    continue;
                }
            }
            s = "";
            for (int i = b ? 1 : 0; i < args.length; i++) {
                s += " " + args[i];
            }
            pipeSide pip = pipeConsole.create();
            logger.pipeStart(pip);
            userReader rdr = new userReader(pip, null);
            pip.settingsPut(pipeSetting.height, 0);
            if (det) {
                userScreen.updtSiz(pip);
            }
            userExec exe = new userExec(pip, rdr);
            exe.privileged = true;
            s = exe.repairCommand(s);
            try {
                exe.executeCommand(s);
            } catch (Exception e) {
                logger.exception(e);
            }
            stopRouter(true, 18, "finished");
            return;
        }
        putln("java -jar " + version.getFileName() + " <parameters>");
        putln("parameters:");
        userHelping hlp = new userHelping();
        hlp.add(null, "1 2 router         start router background");
        hlp.add(null, "2 .   <cfg>        config url");
        hlp.add(null, "1 2 routerc        start router with console");
        hlp.add(null, "2 .   <cfg>        config url");
        hlp.add(null, "1 2 routerw        start router with window");
        hlp.add(null, "2 .   <cfg>        config url");
        hlp.add(null, "1 2 routercw       start router with console and window");
        hlp.add(null, "2 .   <cfg>        config url");
        hlp.add(null, "1 2 routers        start router from separate configs");
        hlp.add(null, "2 3   <hwcfg>      config url");
        hlp.add(null, "3 .     <swcfg>    config url");
        hlp.add(null, "1 2 routera        start router with sw config only");
        hlp.add(null, "2 .   <swcfg>      config url");
        hlp.add(null, "1 2 test           execute test command");
        hlp.add(null, "2 .   <cmd>        command to execute");
        hlp.add(null, "1 2 show           execute show command");
        hlp.add(null, "2 .   <cmd>        command to execute");
        hlp.add(null, "1 2 exec           execute exec command");
        hlp.add(null, "2 .   <cmd>        command to execute");
        hlp.add(null, "1 2 cfgexec        execute exec command");
        hlp.add(null, "2 3   <swcfg>      config url");
        hlp.add(null, "3 3,.   <cmd>      command to execute");
        List<String> res = hlp.getUsage();
        for (int i = 0; i < res.size(); i++) {
            putln(res.get(i));
        }
    }

    private final static void putln(String s) {
        System.out.println(s);
    }

    public void run() {
        int rnd = 0;
        counter cntr = new counter();
        cntr.byteRx = bits.getTime() / 8;
        timerHistory = new history(cntr);
        Runtime rt = Runtime.getRuntime();
        long oldM = rt.freeMemory() / 8;
        cntr.byteRx = oldM;
        memoryHistory = new history(cntr);
        for (;;) {
            try {
                rnd += 1;
                bits.sleep(1000);
                if (debugger.prtWatchEvnt) {
                    logger.debug("health check");
                }
                ifcThread.checkIfaces();
                ipFwdTab.checkVrfs();
                cntr.byteRx = bits.getTime() / 8;
                timerHistory.update(cntr);
                oldM += rt.freeMemory() / 8;
                cntr.byteRx = oldM;
                memoryHistory.update(cntr);
                if ((rnd % 120) != 0) {
                    continue;
                }
                clntDns.purgeLocalCache(false);
                stateSave();
            } catch (Exception e) {
                logger.exception(e);
            }
        }
    }

}

class cfgInitMime implements Comparable<cfgInitMime> {

    protected final String ext;

    protected final String mime;

    public cfgInitMime(String e) {
        ext = e;
        mime = "*";
    }

    public cfgInitMime(String e, String m) {
        ext = e;
        mime = m;
    }

    public int compareTo(cfgInitMime o) {
        return ext.toLowerCase().compareTo(o.ext.toLowerCase());
    }

}

class cfgInitHandler implements UncaughtExceptionHandler {

    public void uncaughtException(Thread t, Throwable e) {
        logger.exception(e);
    }

}
