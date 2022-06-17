package net.freertr.clnt;

import net.freertr.addr.addrEmpty;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrIPv4;
import net.freertr.addr.addrIPv6;
import net.freertr.addr.addrType;
import net.freertr.cfg.cfgIfc;
import net.freertr.cfg.cfgInit;
import net.freertr.cfg.cfgVrf;
import net.freertr.ifc.ifcDn;
import net.freertr.ifc.ifcUp;
import net.freertr.ip.ipFwd;
import net.freertr.ip.ipFwdIface;
import net.freertr.ip.ipFwdTab;
import net.freertr.pack.packHolder;
import net.freertr.pipe.pipeLine;
import net.freertr.pipe.pipeSide;
import net.freertr.prt.prtTcp;
import net.freertr.prt.prtUdp;
import net.freertr.sec.secClient;
import net.freertr.serv.servGeneric;
import net.freertr.serv.servSdwan;
import net.freertr.tab.tabGen;
import net.freertr.user.userFormat;
import net.freertr.user.userTerminal;
import net.freertr.util.bits;
import net.freertr.util.cmds;
import net.freertr.util.counter;
import net.freertr.util.debugger;
import net.freertr.util.logger;
import net.freertr.util.state;
import net.freertr.util.version;

/**
 * sdwan client
 *
 * @author matecsaba
 */
public class clntSdwan implements Runnable, ifcDn {

    /**
     * create instance
     */
    public clntSdwan() {
    }

    /**
     * target of tunnel
     */
    public String ctrlAddr = null;

    /**
     * target of tunnel
     */
    public int ctrlPort = 0;

    /**
     * source of tunnel
     */
    public int dataPort = 0;

    /**
     * randomize source port
     */
    public boolean dataRand = false;

    /**
     * vrf of target
     */
    public cfgVrf srcVrf = null;

    /**
     * source interface
     */
    public cfgIfc srcIfc = null;

    /**
     * clone interface
     */
    public cfgIfc clonIfc = null;

    /**
     * control protocol preference
     */
    public int prefer;

    /**
     * pubkey to use
     */
    public byte[] pubkey = null;

    /**
     * username to use
     */
    public String username = null;

    /**
     * password to use
     */
    public String password = null;

    /**
     * sending ttl value, -1 means maps out
     */
    public int sendingTTL = 255;

    /**
     * sending tos value, -1 means maps out
     */
    public int sendingTOS = -1;

    /**
     * sending df value, -1 means maps out
     */
    public int sendingDFN = -1;

    /**
     * sending flow value, -1 means maps out
     */
    public int sendingFLW = -1;

    /**
     * timeout
     */
    public int timeout = 120000;

    /**
     * counter
     */
    public counter cntr = new counter();

    private boolean working = true;

    private pipeSide conn;

    private tabGen<clntSdwanConn> peers = new tabGen<clntSdwanConn>();

    /**
     * forwarding core
     */
    protected ipFwd fwdCor;

    /**
     * udp core
     */
    protected prtUdp udpCor;

    /**
     * tcp core
     */
    protected prtTcp tcpCor;

    /**
     * forwarding interface
     */
    protected ipFwdIface fwdIfc;

    /**
     * my number
     */
    protected int myNum;

    /**
     * my inner address
     */
    protected addrIPv4 myAddr4;

    /**
     * my inner address
     */
    protected addrIPv6 myAddr6;

    public String toString() {
        return "sdwan to " + ctrlAddr;
    }

    /**
     * get hw address
     *
     * @return hw address
     */
    public addrType getHwAddr() {
        return new addrEmpty();
    }

    /**
     * set filter
     *
     * @param promisc promiscous mode
     */
    public void setFilter(boolean promisc) {
    }

    /**
     * get state
     *
     * @return state
     */
    public state.states getState() {
        return state.states.up;
    }

    /**
     * close interface
     */
    public void closeDn() {
        clearState();
    }

    /**
     * flap interface
     */
    public void flapped() {
        clearState();
    }

    /**
     * set upper layer
     *
     * @param server upper layer
     */
    public void setUpper(ifcUp server) {
        server.setParent(this);
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
     * get mtu size
     *
     * @return mtu size
     */
    public int getMTUsize() {
        return 1500;
    }

    /**
     * get bandwidth
     *
     * @return bandwidth
     */
    public long getBandwidth() {
        return 8000000;
    }

    /**
     * send packet
     *
     * @param pck packet
     */
    public void sendPack(packHolder pck) {
    }

    private void clearState() {
        if (conn != null) {
            conn.setClose();
        }
        myAddr4 = new addrIPv4();
        myAddr6 = new addrIPv6();
        for (int i = peers.size() - 1; i >= 0; i--) {
            clntSdwanConn ntry = peers.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.workStop();
        }
        peers.clear();
        if (dataRand) {
            dataPort = bits.random(1024, 8191);
        }
    }

    public void run() {
        for (;;) {
            if (!working) {
                break;
            }
            try {
                clearState();
                workDoer();
            } catch (Exception e) {
                logger.traceback(e);
            }
            clearState();
            bits.sleep(1000);
        }
    }

    /**
     * start connection
     */
    public void workStart() {
        if (ctrlPort < 1) {
            ctrlPort = servSdwan.port;
        }
        if (dataPort < 1) {
            dataPort = ctrlPort;
        }
        if (prefer < 1) {
            prefer = clntDns.getPriPref();
        }
        new Thread(this).start();
    }

    /**
     * stop connection
     */
    public void workStop() {
        working = false;
        clearState();
    }

    private void sendLn(String s) {
        if (debugger.clntSdwanTraf) {
            logger.debug("tx: " + s);
        }
        conn.linePut(s);
    }

    private String readLn() {
        String s = conn.lineGet(1);
        if (debugger.clntSdwanTraf) {
            logger.debug("rx: " + s);
        }
        return s;
    }

    private void workDoer() {
        if (debugger.clntSdwanTraf) {
            logger.debug("resolving " + ctrlAddr + " for ipv" + prefer);
        }
        addrIP trg = userTerminal.justResolv(ctrlAddr, prefer);
        if (trg == null) {
            logger.error("unable to resolve " + ctrlAddr);
            return;
        }
        fwdCor = srcVrf.getFwd(trg);
        tcpCor = srcVrf.getTcp(trg);
        udpCor = srcVrf.getUdp(trg);
        fwdIfc = null;
        if (srcIfc != null) {
            fwdIfc = srcIfc.getFwdIfc(trg);
        } else {
            fwdIfc = ipFwdTab.findSendingIface(fwdCor, trg);
        }
        if (fwdIfc == null) {
            return;
        }
        if (!fwdIfc.ready) {
            return;
        }
        if (fwdCor.ipVersion != prefer) {
            logger.error("unable to resolve " + ctrlAddr);
            return;
        }
        if (fwdIfc.addr == null) {
            logger.error("unable to resolve " + ctrlAddr);
            return;
        }
        if (debugger.clntSdwanTraf) {
            logger.debug("connecting " + trg);
        }
        conn = tcpCor.streamConnect(new pipeLine(65536, false), fwdIfc, 0, trg, ctrlPort, "sdwan", null, -1);
        if (conn == null) {
            logger.error("unable to connect " + trg);
            return;
        }
        conn.setTime(timeout);
        conn.lineRx = pipeSide.modTyp.modeCRtryLF;
        conn.lineTx = pipeSide.modTyp.modeCRLF;
        if (conn.wait4ready(timeout)) {
            logger.error("failed to connect " + trg);
            return;
        }
        sendLn("sdwan");
        if (!readLn().equals("okay")) {
            logger.error("unable to validate " + trg);
            return;
        }
        conn = secClient.openSec(conn, servGeneric.protoSsh, pubkey, username, password);
        if (conn == null) {
            logger.error("unable to authenticate " + trg);
            return;
        }
        conn.setTime(timeout);
        conn.lineRx = pipeSide.modTyp.modeCRtryLF;
        conn.lineTx = pipeSide.modTyp.modeCRLF;
        sendLn("hello");
        sendLn("username " + username);
        sendLn("software " + version.headLine);
        sendLn("middleware " + version.getVMname());
        sendLn("kernel " + version.getKernelName());
        sendLn("hardware " + cfgInit.hwIdNum + " " + version.getCPUname() + " " + version.getMemoryInfo());
        sendLn("needaddr " + (clonIfc.addr4 != null) + " " + (clonIfc.addr6 != null));
        sendLn("myaddr " + clonIfc.addr4 + " " + clonIfc.addr6);
        String a = "";
        if (clonIfc.disableMacsec) {
            a += " nomacsec";
        }
        if (clonIfc.disableSgt) {
            a += " nosgt";
        }
        sendLn("myendpoint " + prefer + " " + fwdIfc.addr + " " + dataPort + " " + a);
        sendLn("nomore");
        for (;;) {
            a = readLn();
            cmds cmd = new cmds("sdw", a);
            a = cmd.word();
            if (a.length() < 1) {
                if (conn.isClosed() != 0) {
                    return;
                }
                continue;
            }
            if (a.equals("nomore")) {
                break;
            }
            if (a.equals("hello")) {
                continue;
            }
            if (a.equals("youraddr")) {
                myAddr4.fromString(cmd.word());
                myAddr6.fromString(cmd.word());
                continue;
            }
            if (a.equals("yourid")) {
                myNum = bits.str2num(cmd.word());
                continue;
            }
            logger.warn("got unknown command: " + cmd.getOriginal());
        }
        logger.info("neighbor " + trg + " up");
        for (;;) {
            if (doRound()) {
                break;
            }
        }
        logger.warn("neighbor " + trg + " down");
    }

    private boolean doRound() {
        String a = readLn();
        cmds cmd = new cmds("sdw", a);
        a = cmd.word();
        if (a.length() < 1) {
            return conn.isClosed() != 0;
        }
        if (a.equals("echo")) {
            sendLn("echoed " + cmd.getRemaining());
            return false;
        }
        if (a.equals("endpoint_add")) {
            clntSdwanConn ntry = new clntSdwanConn(this);
            ntry.fromString(cmd);
            if (ntry.ver != prefer) {
                return false;
            }
            clntSdwanConn old = peers.del(ntry);
            if (old != null) {
                old.workStop();
            }
            ntry.workStart();
            peers.put(ntry);
            return false;
        }
        if (a.equals("endpoint_del")) {
            clntSdwanConn ntry = new clntSdwanConn(this);
            ntry.fromString(cmd);
            if (ntry.ver != prefer) {
                return false;
            }
            ntry = peers.del(ntry);
            if (ntry == null) {
                return false;
            }
            ntry.workStop();
            return false;
        }
        return false;
    }

    /**
     * get show
     *
     * @return state
     */
    public userFormat getShow() {
        userFormat l = new userFormat("|", "user|peer|port|num|iface|addr4|addr6");
        for (int i = 0; i < peers.size(); i++) {
            clntSdwanConn ntry = peers.get(i);
            if (ntry == null) {
                continue;
            }
            ntry.getShow(l);
        }
        return l;
    }

}
