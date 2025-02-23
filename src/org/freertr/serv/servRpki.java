package org.freertr.serv;

import java.util.List;
import org.freertr.addr.addrIP;
import org.freertr.cfg.cfgAll;
import org.freertr.cfg.cfgRtr;
import org.freertr.ip.ipRtr;
import org.freertr.pack.packHolder;
import org.freertr.rtr.rtrRpkiSpeak;
import org.freertr.pipe.pipeLine;
import org.freertr.pipe.pipeSide;
import org.freertr.prt.prtGenConn;
import org.freertr.prt.prtServS;
import org.freertr.rtr.rtrRpki;
import org.freertr.tab.tabGen;
import org.freertr.tab.tabRoautNtry;
import org.freertr.tab.tabRouteAttr;
import org.freertr.user.userFilter;
import org.freertr.user.userFormat;
import org.freertr.user.userHelping;
import org.freertr.util.bits;
import org.freertr.util.cmds;
import org.freertr.util.counter;
import org.freertr.util.debugger;
import org.freertr.util.logger;

/**
 * resource public key infrastructure (rfc6810) server
 *
 * @author matecsaba
 */
public class servRpki extends servGeneric implements prtServS {

    /**
     * create instance
     */
    public servRpki() {
    }

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "server rpki .*!" + cmds.tabulator + "port " + rtrRpkiSpeak.portNum,
        "server rpki .*!" + cmds.tabulator + "protocol " + proto2string(protoAllStrm),
        "server rpki .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "json",
        "server rpki .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "rpki"
    };

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    /**
     * configured ipv4 prefixes
     */
    public final tabGen<tabRoautNtry> cfged4 = new tabGen<tabRoautNtry>();

    /**
     * configured ipv6 prefixes
     */
    public final tabGen<tabRoautNtry> cfged6 = new tabGen<tabRoautNtry>();

    /**
     * sequence
     */
    protected int sequence;

    /**
     * rpki type configured
     */
    protected tabRouteAttr.routeType rpkiT;

    /**
     * rpki number configured
     */
    protected int rpkiN;

    /**
     * json file
     */
    protected String jsonN;

    /**
     * connected neighbors
     */
    protected tabGen<servRpkiConn> neighs = new tabGen<servRpkiConn>();

    public tabGen<userFilter> srvDefFlt() {
        return defaultF;
    }

    public String srvName() {
        return "rpki";
    }

    public int srvPort() {
        return rtrRpkiSpeak.portNum;
    }

    public int srvProto() {
        return protoAllStrm;
    }

    public boolean srvInit() {
        return genStrmStart(this, new pipeLine(32768, false), 0);
    }

    public boolean srvDeinit() {
        return genericStop(0);
    }

    public void srvShRun(String beg, List<String> lst, int filter) {
        if (rpkiT == null) {
            lst.add(beg + "no rpki");
        } else {
            lst.add(beg + "rpki " + cfgRtr.num2name(rpkiT) + " " + rpkiN);
        }
        cmds.cfgLine(lst, jsonN == null, beg, "json", jsonN);
        for (int i = 0; i < cfged4.size(); i++) {
            lst.add(beg + "prefix " + cfged4.get(i).toConfig());
        }
        for (int i = 0; i < cfged6.size(); i++) {
            lst.add(beg + "prefix " + cfged6.get(i).toConfig());
        }
    }

    public boolean srvCfgStr(cmds cmd) {
        String s = cmd.word();
        if (s.equals("prefix")) {
            tabRoautNtry prf = new tabRoautNtry();
            if (prf.fromString(cmd)) {
                cmd.error("bad prefix");
                return false;
            }
            prf.srcIP = new addrIP();
            if (prf.prefix.network.isIPv4()) {
                cfged4.put(prf);
            } else {
                cfged6.put(prf);
            }
            sequence++;
            return false;
        }
        if (s.equals("json")) {
            jsonN = cmd.getRemaining();
            sequence++;
            return false;
        }
        if (s.equals("rpki")) {
            rpkiT = cfgRtr.name2num(cmd.word());
            rpkiN = bits.str2num(cmd.word());
            if (ipRtr.isRPKI(rpkiT) > 0) {
                return false;
            }
            cmd.error("not an rpki process");
            rpkiT = null;
            rpkiN = 0;
            sequence++;
            return false;
        }
        if (!s.equals(cmds.negated)) {
            return true;
        }
        s = cmd.word();
        if (s.equals("prefix")) {
            tabRoautNtry prf = new tabRoautNtry();
            if (prf.fromString(cmd)) {
                cmd.error("bad prefix");
                return false;
            }
            if (prf.prefix.network.isIPv4()) {
                cfged4.del(prf);
            } else {
                cfged6.del(prf);
            }
            sequence++;
            return false;
        }
        if (s.equals("json")) {
            jsonN = null;
            sequence++;
            return false;
        }
        if (s.equals("rpki")) {
            rpkiT = null;
            rpkiN = 0;
            sequence++;
            return false;
        }
        return true;
    }

    public void srvHelp(userHelping l) {
        l.add(null, "1 2  prefix                       setup a prefix");
        l.add(null, "2 3    <net/mask>                 network in perfix/mask format");
        l.add(null, "3 4      <num>                    maximum prefix length");
        l.add(null, "4 4,.      <num>                  as number");
        l.add(null, "1 2   json                        setup a json file");
        l.add(null, "2 .     <str>                     name of file to use");
        l.add(null, "1 2   rpki                        setup resource public key infrastructure");
        cfgRtr.getRouterList(l, 0, "");
        l.add(null, "3 .         <num>                 process number");
    }

    public boolean srvAccept(pipeSide pipe, prtGenConn id) {
        pipe.setTime(120000);
        new servRpkiConn(this, pipe, id.peerAddr);
        return false;
    }

    /**
     * get show
     *
     * @return result
     */
    public userFormat getShow() {
        userFormat l = new userFormat("|", "neighbor|rx|tx|rx|tx", "1|2pack|2byte");
        for (int i = 0; i < neighs.size(); i++) {
            servRpkiConn ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            l.add(ntry.peer + "|" + ntry.cntr.packRx + "|" + ntry.cntr.packTx + "|" + ntry.cntr.byteRx + "|" + ntry.cntr.byteTx);
        }
        return l;
    }

}

class servRpkiConn implements Runnable, Comparable<servRpkiConn> {

    public final servRpki lower;

    public final pipeSide conn;

    public final addrIP peer;

    public final int sess;

    public final counter cntr;

    public servRpkiConn(servRpki parent, pipeSide pipe, addrIP rem) {
        lower = parent;
        conn = pipe;
        peer = rem;
        sess = bits.randomW();
        cntr = new counter();
        new Thread(this).start();
    }

    public int compareTo(servRpkiConn o) {
        return peer.compareTo(o.peer);
    }

    public void run() {
        if (debugger.servRpkiTraf) {
            logger.debug("starting " + peer);
        }
        lower.neighs.put(this);
        try {
            rtrRpkiSpeak pck = new rtrRpkiSpeak(new packHolder(true, true), conn, cntr);
            for (;;) {
                rtrRpki rpkiR = null;
                if (lower.rpkiT != null) {
                    cfgRtr rtrCfg = cfgAll.rtrFind(lower.rpkiT, lower.rpkiN, false);
                    if (rtrCfg != null) {
                        rpkiR = (rtrRpki) rtrCfg.getRouter();
                    }
                }
                if (pck.doOneServRnd(lower.sequence, sess, lower.cfged4, lower.cfged6, rpkiR, lower.jsonN)) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.traceback(e);
        }
        conn.setClose();
        lower.neighs.del(this);
        if (debugger.servRpkiTraf) {
            logger.debug("stoping " + peer);
        }
    }

}
