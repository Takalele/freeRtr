package net.freertr.clnt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.freertr.addr.addrEmpty;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrType;
import net.freertr.cfg.cfgIfc;
import net.freertr.ifc.ifcDn;
import net.freertr.ifc.ifcNull;
import net.freertr.ifc.ifcUp;
import net.freertr.ip.ipFwdIface;
import net.freertr.pack.packHolder;
import net.freertr.prt.prtGenConn;
import net.freertr.prt.prtServP;
import net.freertr.user.userFormat;
import net.freertr.util.bits;
import net.freertr.util.cmds;
import net.freertr.util.counter;
import net.freertr.util.debugger;
import net.freertr.util.logger;
import net.freertr.util.state;

/**
 * sdwan connection
 *
 * @author matecsaba
 */
public class clntSdwanConn implements Runnable, prtServP, Comparator<clntSdwanConn> {

    /**
     * parent
     */
    public final clntSdwan lower;

    /**
     * ip version
     */
    public int ver;

    /**
     * remote address
     */
    public addrIP addr;

    /**
     * remote port
     */
    public int port;

    /**
     * neighbor id
     */
    public int peerId;

    /**
     * inner ipv4
     */
    public addrIP peer4;

    /**
     * inner ipv6
     */
    public addrIP peer6;

    /**
     * peer name
     */
    public String name;

    /**
     * list of protocols
     */
    public List<clntSdwan.protoTyp> protol;

    /**
     * selected protocol
     */
    public clntSdwan.protoTyp protos;

    private cfgIfc ifc;

    private ifcUp upper = new ifcNull();

    private counter cntr = new counter();

    private prtGenConn conn;

    private boolean noMacsec;

    private boolean noSgt;

    private int frags;

    private boolean need2work = true;

    private prtServP wrkrPrtCl;

    private clntL2tp2 prtL2tp2;

    /**
     * create instance
     *
     * @param parent lower layer
     */
    public clntSdwanConn(clntSdwan parent) {
        lower = parent;
    }

    public String toString() {
        return "sdwan to " + addr;
    }

    public int compare(clntSdwanConn o1, clntSdwanConn o2) {
        if (o1.port < o2.port) {
            return -1;
        }
        if (o1.port > o2.port) {
            return +1;
        }
        return o1.addr.compare(o1.addr, o2.addr);
    }

    /**
     * read string
     *
     * @param cmd string
     */
    public void fromString(cmds cmd) {
        ver = bits.str2num(cmd.word());
        addr = new addrIP();
        addr.fromString(cmd.word());
        port = bits.str2num(cmd.word());
        peerId = bits.str2num(cmd.word());
        peer4 = new addrIP();
        peer4.fromString(cmd.word());
        peer6 = new addrIP();
        peer6.fromString(cmd.word());
        name = cmd.word();
        protol = new ArrayList<clntSdwan.protoTyp>();
        for (;;) {
            String a = cmd.word();
            if (a.length() < 1) {
                break;
            }
            if (a.equals("nomacsec")) {
                noMacsec = true;
                continue;
            }
            if (a.equals("nosgt")) {
                noSgt = true;
                continue;
            }
            if (a.equals("frag")) {
                frags = bits.str2num(cmd.word());
                continue;
            }
            if (!a.startsWith(clntSdwan.protoBeg)) {
                continue;
            }
            a = a.substring(clntSdwan.protoLen, a.length());
            clntSdwan.protoTyp p = clntSdwan.string2proto(a);
            if (p == null) {
                continue;
            }
            protol.add(p);
        }
        if (protol.size() < 1) {
            protol.add(clntSdwan.protoTyp.l2tp);
        }
        if (lower.myNum < peerId) {
            protos = clntSdwan.selectProto(lower.protol, protol);
        } else {
            protos = clntSdwan.selectProto(protol, lower.protol);
        }
    }

    /**
     * stop connection
     */
    public synchronized void workStop() {
        if (debugger.clntSdwanTraf) {
            logger.debug("stopping peer " + addr + " " + port);
        }
        need2work = false;
        if (ifc != null) {
            ifc.cloneStop();
        }
        lower.udpCor.listenStop(lower.fwdIfc, lower.dataPort, addr, 0);
        if (conn != null) {
            conn.setClosing();
        }
        ifc = null;
        conn = null;
    }

    private void doReconnect() {
        if (conn != null) {
            conn.setClosing();
            conn = null;
        }
        if (bits.random(1, 100) < lower.passPerc) {
            logger.warn("accepting " + addr + " " + port);
            lower.udpCor.packetListen(this, lower.fwdIfc, lower.dataPort, addr, 0, "sdwan", -1, null, -1, -1);
            int max = bits.random(10, 30);
            for (int i = 0; i < max; i++) {
                bits.sleep(1000);
                if (conn != null) {
                    break;
                }
            }
            lower.udpCor.listenStop(lower.fwdIfc, lower.dataPort, addr, 0);
        } else {
            logger.warn("reconnecting " + addr + " " + port);
            conn = lower.udpCor.packetConnect(this, lower.fwdIfc, lower.dataPort, addr, port, "sdwan", -1, null, -1, -1);
        }
        if (conn == null) {
            logger.error("failed to establish " + addr + " " + port);
            return;
        }
        conn.timeout = 120000;
        conn.sendFLW = lower.sendingFLW;
        conn.sendTOS = lower.sendingTOS;
        conn.sendDFN = lower.sendingDFN;
        conn.sendTTL = lower.sendingTTL;
        switch (protos) {
            case l2tp:
                prtL2tp2.setConnection(conn, lower.fwdCor, lower.myNum, peerId, peerId, lower.myNum);
                break;
        }
    }

    /**
     * stop connection
     */
    public void workStart() {
        if (debugger.clntSdwanTraf) {
            logger.debug("starting peer " + addr + " " + port);
        }
        upper = new ifcNull();
        ifcDn wrkrIfDn = null;
        switch (protos) {
            case l2tp:
                prtL2tp2 = new clntL2tp2();
                wrkrIfDn = prtL2tp2;
                wrkrPrtCl = prtL2tp2;
                break;
            default:
                return;
        }
        doReconnect();
        ifc = lower.clonIfc.cloneStart(wrkrIfDn);
        ifc.addr4changed(lower.myAddr4, ifc.mask4, peer4.toIPv4());
        ifc.addr6changed(lower.myAddr6, ifc.mask6, peer6.toIPv6());
        if (noMacsec) {
            ifc.disableMacsec = true;
            ifc.ethtyp.macSec = null;
            ifc.ethtyp.timerUpdate();
        }
        if (noSgt) {
            ifc.disableSgt = true;
            ifc.ethtyp.sgtHnd = null;
        }
        if ((ifc.ppp != null) && (frags > 0)) {
            if (ifc.ppp.fragLen > frags) {
                ifc.ppp.fragLen = frags;
            }
        }
        if ((ifc.frmrly != null) && (frags > 0)) {
            if (ifc.frmrly.fragLen > frags) {
                ifc.frmrly.fragLen = frags;
            }
        }
        upper = ifc.getEncapProto();
        switch (protos) {
            case l2tp:
                prtL2tp2.setUpper(upper);
                break;
        }
        new Thread(this).start();
    }

    public void run() {
        for (;;) {
            bits.sleep(1000);
            if (!need2work) {
                break;
            }
            if (conn == null) {
                doReconnect();
                continue;
            }
            if (conn.txBytesFree() < 0) {
                doReconnect();
                continue;
            }
        }
    }

    /**
     * get show
     *
     * @param l list to update
     */
    protected void getShow(userFormat l) {
        l.add(name + "|" + protos + "|" + addr + "|" + port + "|" + peerId + "|" + ifc.name + "|" + peer4 + "|" + peer6);
    }

    public boolean datagramRecv(prtGenConn id, packHolder pck) {
        if (conn == null) {
            cntr.drop(pck, counter.reasons.notUp);
            return false;
        }
        wrkrPrtCl.datagramRecv(id, pck);
        return false;
    }

    public void closedInterface(ipFwdIface ifc) {
    }

    public boolean datagramAccept(prtGenConn id) {
        if (conn != null) {
            return true;
        }
        conn = id;
        return false;
    }

    public void datagramReady(prtGenConn id) {
    }

    public void datagramClosed(prtGenConn id) {
    }

    public void datagramWork(prtGenConn id) {
    }

    public boolean datagramError(prtGenConn id, packHolder pck, addrIP rtr, counter.reasons err, int lab) {
        if (conn == null) {
            cntr.drop(pck, counter.reasons.notUp);
            return false;
        }
        wrkrPrtCl.datagramError(id, pck, rtr, err, lab);
        return false;
    }

    public boolean datagramState(prtGenConn id, state.states stat) {
        return false;
    }

}
