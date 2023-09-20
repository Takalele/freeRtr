package net.freertr.rtr;

import java.util.Comparator;
import java.util.List;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrIPv4;
import net.freertr.addr.addrIPv6;
import net.freertr.auth.authLocal;
import net.freertr.cfg.cfgIfc;
import net.freertr.ip.ipFwdIface;
import net.freertr.ip.ipFwdMcast;
import net.freertr.ip.ipFwdTab;
import net.freertr.pack.packHolder;
import net.freertr.pipe.pipeDiscard;
import net.freertr.pipe.pipeLine;
import net.freertr.pipe.pipeSide;
import net.freertr.prt.prtAccept;
import net.freertr.prt.prtPmtud;
import net.freertr.tab.tabGen;
import net.freertr.util.bits;
import net.freertr.util.cmds;
import net.freertr.util.debugger;
import net.freertr.util.logger;

/**
 * msdp neighbor
 *
 * @author matecsaba
 */
public class rtrMsdpNeigh implements Runnable, rtrBfdClnt, Comparator<rtrMsdpNeigh> {

    /**
     * peer address
     */
    public final addrIP peer = new addrIP();

    /**
     * remote description
     */
    public String description;

    /**
     * source interface
     */
    public cfgIfc srcIface;

    /**
     * pmtud min
     */
    public int pmtudMin;

    /**
     * pmtud max
     */
    public int pmtudMax;

    /**
     * pmtud timeout
     */
    public int pmtudTim;

    /**
     * pmtud result
     */
    public int pmtudRes;

    /**
     * keep alive
     */
    public int keepAlive = 30000;

    /**
     * hold timer
     */
    public int holdTimer = 75000;

    /**
     * advertisement timer
     */
    public int freshTimer = 60000;

    /**
     * advertisement timer
     */
    public int flushTimer = 120000;

    /**
     * set to true to shutdown peer
     */
    public boolean shutdown;

    /**
     * session password
     */
    public String passwd = null;

    /**
     * bfd enabled
     */
    public boolean bfdTrigger;

    /**
     * learned sas
     */
    public tabGen<ipFwdMcast> learned = new tabGen<ipFwdMcast>();

    /**
     * header size
     */
    public final static int size = 3;

    /**
     * source active
     */
    public final static int typSAact = 1;

    /**
     * source request
     */
    public final static int typSAreq = 2;

    /**
     * source response
     */
    public final static int typSArep = 3;

    /**
     * keepalive
     */
    public final static int typKeep = 4;

    /**
     * notification
     */
    public final static int typNtfy = 5;

    /**
     * traceroute request
     */
    public final static int typTRreq = 6;

    /**
     * traceroute reply
     */
    public final static int typTRrep = 7;

    private pipeSide pipe;

    private final rtrMsdp parent;

    private boolean need2run;

    private long lastAdv;

    private ipFwdIface usedIfc;

    /**
     * uptime
     */
    protected long upTime;

    /**
     * convert type to string
     *
     * @param i type
     * @return string
     */
    public static String type2string(int i) {
        switch (i) {
            case typSAact:
                return "saAct";
            case typSAreq:
                return "saReq";
            case typSArep:
                return "saRep";
            case typKeep:
                return "keepalive";
            case typNtfy:
                return "notify";
            case typTRreq:
                return "traceReq";
            case typTRrep:
                return "traceRep";
            default:
                return "unknown=" + i;
        }
    }

    /**
     * create new peer
     *
     * @param lower lower to use
     */
    public rtrMsdpNeigh(rtrMsdp lower) {
        parent = lower;
    }

    public int compare(rtrMsdpNeigh o1, rtrMsdpNeigh o2) {
        return o1.peer.compare(o1.peer, o2.peer);
    }

    /**
     * close this session
     */
    public void closeNow() {
        learned = new tabGen<ipFwdMcast>();
        if (pipe != null) {
            logger.error("neighbor " + peer + " down");
            pipe.setClose();
            pipe = null;
        }
        if (usedIfc != null) {
            usedIfc.bfdDel(peer, this);
            usedIfc = null;
        }
    }

    /**
     * open new connection
     *
     * @return false on success, true on error
     */
    public boolean openConn() {
        if (srcIface == null) {
            usedIfc = ipFwdTab.findSendingIface(parent.fwdCore, peer);
        } else {
            usedIfc = srcIface.getFwdIfc(peer);
        }
        if (usedIfc == null) {
            return true;
        }
        if (usedIfc.addr == null) {
            return true;
        }
        if (shutdown) {
            return true;
        }
        bits.sleep(bits.random(1000, 5000));
        if (peer.compare(peer, usedIfc.addr) > 0) {
            pipe = parent.tcpCore.streamConnect(new pipeLine(65536, false), usedIfc, 0, peer, rtrMsdp.port, "msdp", -1, passwd, -1, -1);
        } else {
            prtAccept ac = new prtAccept(parent.tcpCore, new pipeLine(65536, false), usedIfc, rtrMsdp.port, peer, 0, "msdp", -1, passwd, -1, -1);
            ac.wait4conn(30000);
            pipe = ac.getConn(true);
        }
        if (pipe == null) {
            return true;
        }
        pipe.setTime(holdTimer);
        pipe.setReady();
        pipe.wait4ready(holdTimer);
        if (pmtudTim > 0) {
            logger.warn("testing pmtud to " + peer + " from " + usedIfc.addr);
            pipeLine pl = new pipeLine(65536, true);
            prtPmtud pm = new prtPmtud(pl.getSide(), peer, parent.fwdCore, usedIfc.addr);
            pm.min = pmtudMin;
            pm.max = pmtudMax;
            pm.timeout = pmtudTim;
            int[] res = pm.doer();
            if (res == null) {
                logger.warn("pmtud failed to " + peer);
                pipeDiscard.logLines("pmtud failure to " + peer, pl.getSide(), true, null);
                pipe.setClose();
                return false;
            }
            logger.warn("pmtud measured " + pm.last + " bytes to " + peer);
            pmtudRes = pm.last;
        }
        if (pipe.isReady() != 3) {
            closeNow();
            return true;
        }
        if (bfdTrigger) {
            usedIfc.bfdAdd(peer, this, "msdp");
        }
        logger.warn("neighbor " + peer + " up");
        sendKeep();
        return false;
    }

    private addrIP getAddr(packHolder pck) {
        if (peer.isIPv4()) {
            return rtrBgpUtil.readAddress(rtrBgpUtil.afiIpv4, pck);
        } else {
            return rtrBgpUtil.readAddress(rtrBgpUtil.afiIpv6, pck);
        }
    }

    private void putAddr(packHolder pck, addrIP adr) {
        if (peer.isIPv4()) {
            rtrBgpUtil.writeAddress(rtrBgpUtil.afiIpv4, pck, adr);
        } else {
            rtrBgpUtil.writeAddress(rtrBgpUtil.afiIpv6, pck, adr);
        }
    }

    /**
     * receive one packet
     *
     * @return false on success, true on error
     */
    public boolean packRecv() {
        packHolder pck = new packHolder(true, true);
        if (pipe == null) {
            return true;
        }
        if (pck.pipeRecv(pipe, 0, size, 144) != size) {
            return true;
        }
        int typ = pck.getByte(0); // type
        int len = pck.msbGetW(1) - size; // length
        if (len < 0) {
            return true;
        }
        if (debugger.rtrMsdpEvnt) {
            logger.debug("rx " + type2string(typ));
        }
        if (len < 1) {
            return false;
        }
        pck.clear();
        if (pck.pipeRecv(pipe, 0, len, 144) != len) {
            return true;
        }
        switch (typ) {
            case typSAact:
                break;
            case typKeep:
                return false;
            default:
                logger.info("got unknown type (" + typ + ") from " + peer);
                return true;
        }
        int grps = pck.getByte(0); // number of groups
        pck.getSkip(1);
        addrIP rp = getAddr(pck);
        long tim = bits.getTime();
        for (int i = 0; i < grps; i++) {
            pck.getSkip(4); // reserved + preflen
            addrIP grp = getAddr(pck);
            addrIP src = getAddr(pck);
            ipFwdMcast g = new ipFwdMcast(grp, src);
            g.upstream = rp.copyBytes();
            g.created = tim;
            learned.put(g);
            if (debugger.rtrMsdpTraf) {
                logger.debug("rx " + g);
            }
        }
        return false;
    }

    private void sendPack(packHolder pck, int typ) {
        if (pipe == null) {
            return;
        }
        if (debugger.rtrMsdpEvnt) {
            logger.debug("sending " + type2string(typ));
        }
        pck.merge2beg();
        pck.putByte(0, typ);
        pck.msbPutW(1, pck.dataSize() + size);
        pck.putSkip(size);
        pck.merge2beg();
        pck.pipeSend(pipe, 0, pck.dataSize(), 2);
    }

    /**
     * send sa active
     *
     * @param grp group to advertise
     */
    public void sendSAact(ipFwdMcast grp) {
        packHolder pck = new packHolder(true, true);
        pck.putByte(0, 1); // number of groups
        pck.putSkip(1);
        putAddr(pck, grp.upstream);
        if (peer.isIPv4()) {
            pck.msbPutD(0, new addrIPv4().maxBits());
        } else {
            pck.msbPutD(0, new addrIPv6().maxBits());
        }
        pck.putSkip(4);
        putAddr(pck, grp.group);
        putAddr(pck, grp.source);
        sendPack(pck, typSAact);
        if (debugger.rtrMsdpTraf) {
            logger.debug("tx " + grp);
        }
    }

    /**
     * send keepalive
     */
    public void sendKeep() {
        sendPack(new packHolder(true, true), typKeep);
    }

    /**
     * start this neighbor
     */
    public void startNow() {
        if (debugger.rtrMsdpEvnt) {
            logger.debug("starting " + peer);
        }
        need2run = true;
        upTime = bits.getTime();
        new Thread(this).start();
        new rtrMsdpNeighTx(this);
    }

    /**
     * stop this neighbor
     */
    public void stopNow() {
        if (debugger.rtrMsdpEvnt) {
            logger.debug("stopping " + peer);
        }
        need2run = false;
        closeNow();
    }

    /**
     * get configuration
     *
     * @param l list to append
     * @param beg beginning
     * @param filter filter defaults
     */
    public void getCfg(List<String> l, String beg, int filter) {
        String a = "neighbor " + peer + " ";
        l.add(beg + a + "enable");
        cmds.cfgLine(l, description == null, beg, a + "description", description);
        if (srcIface == null) {
            l.add(beg + "no " + a + "update-source");
        } else {
            l.add(beg + a + "update-source " + srcIface.name);
        }
        cmds.cfgLine(l, passwd == null, beg, a + "password", authLocal.passwdEncode(passwd, (filter & 2) != 0));
        l.add(beg + a + "pmtud " + pmtudMin + " " + pmtudMax + " " + pmtudTim);
        l.add(beg + a + "timer " + keepAlive + " " + holdTimer + " " + freshTimer + " " + flushTimer);
        cmds.cfgLine(l, !shutdown, beg, a + "shutdown", "");
        cmds.cfgLine(l, !bfdTrigger, beg, a + "bfd", "");
    }

    public void run() {
        try {
            for (;;) {
                if (doWorkRx()) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.traceback(e);
        }
    }

    /**
     * do rx work
     *
     * @return false on continue, true on success
     */
    protected boolean doWorkRx() {
        if (!need2run) {
            return true;
        }
        boolean b = pipe == null;
        if (!b) {
            b = pipe.isClosed() != 0;
        }
        if (b) {
            bits.sleep(1000);
            closeNow();
            openConn();
            return false;
        }
        if (packRecv()) {
            closeNow();
            return false;
        }
        return false;
    }

    /**
     * do tx work
     *
     * @return false on continue, true on success
     */
    protected boolean doWorkTx() {
        if (!need2run) {
            return true;
        }
        bits.sleep(keepAlive);
        if (pipe == null) {
            return false;
        }
        sendKeep();
        long tim = bits.getTime();
        if ((tim - lastAdv) < freshTimer) {
            return false;
        }
        lastAdv = tim;
        for (int i = 0; i < parent.cache.size(); i++) {
            ipFwdMcast ntry = parent.cache.get(i);
            if (ntry == null) {
                continue;
            }
            if (learned.find(ntry) != null) {
                continue;
            }
            sendSAact(ntry);
        }
        for (int i = learned.size(); i >= 0; i--) {
            ipFwdMcast ntry = learned.get(i);
            if (ntry == null) {
                continue;
            }
            if ((tim - ntry.created) < flushTimer) {
                continue;
            }
            learned.del(ntry);
        }
        parent.routerCreateComputed();
        sendKeep();
        return false;
    }

    public void bfdPeerDown() {
        closeNow();
    }

}

class rtrMsdpNeighTx implements Runnable {

    public final rtrMsdpNeigh lower;

    public rtrMsdpNeighTx(rtrMsdpNeigh prnt) {
        lower = prnt;
        new Thread(this).start();
    }

    public void run() {
        try {
            for (;;) {
                if (lower.doWorkTx()) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.traceback(e);
        }
    }

}
