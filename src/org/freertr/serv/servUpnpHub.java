package org.freertr.serv;

import java.util.Comparator;
import java.util.List;
import org.freertr.pack.packHolder;
import org.freertr.pack.packUpnpFwd;
import org.freertr.pipe.pipeLine;
import org.freertr.pipe.pipeSide;
import org.freertr.prt.prtGenConn;
import org.freertr.prt.prtServS;
import org.freertr.tab.tabGen;
import org.freertr.user.userFilter;
import org.freertr.user.userHelping;
import org.freertr.util.cmds;
import org.freertr.util.logger;

/**
 * upnp hub
 *
 * @author matecsaba
 */
public class servUpnpHub extends servGeneric implements prtServS {

    /**
     * create instance
     */
    public servUpnpHub() {
    }

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "server upnphub .*! port " + packUpnpFwd.portNum,
        "server upnphub .*! protocol " + proto2string(protoNets + protoUdp)
    };

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public tabGen<userFilter> srvDefFlt() {
        return defaultF;
    }

    /**
     * connections
     */
    protected tabGen<servUpnpHubConn> conns = new tabGen<servUpnpHubConn>();

    public boolean srvAccept(pipeSide pipe, prtGenConn id) {
        pipe.setTime(120000);
        new servUpnpHubConn(this, pipe, id);
        return false;
    }

    public void srvShRun(String beg, List<String> lst, int filter) {
    }

    public boolean srvCfgStr(cmds cmd) {
        return true;
    }

    public void srvHelp(userHelping l) {
    }

    public String srvName() {
        return "upnphub";
    }

    public int srvPort() {
        return packUpnpFwd.portNum;
    }

    public int srvProto() {
        return protoNets + protoUdp;
    }

    public boolean srvInit() {
        return genStrmStart(this, new pipeLine(32768, true), 0);
    }

    public boolean srvDeinit() {
        return genericStop(0);
    }

    /**
     * got packet
     *
     * @param conn connection
     * @param pck packet
     */
    protected void doPacket(servUpnpHubConn conn, packHolder pck) {
        packUpnpFwd pckF = new packUpnpFwd();
        pckF.parsePacket(pck);
        pckF.createPacket(pck);
        if (pckF.typ != packUpnpFwd.typData) {
            conn.doPack(pck);
            return;
        }
        for (int i = conns.size() - 1; i >= 0; i--) {
            servUpnpHubConn ntry = conns.get(i);
            if (conn.compare(conn, ntry) == 0) {
                continue;
            }
            ntry.doPack(pck);
        }
    }

}

class servUpnpHubConn implements Runnable, Comparator<servUpnpHubConn> {

    private servUpnpHub parent;

    private pipeSide pipe;

    private prtGenConn conn;

    public servUpnpHubConn(servUpnpHub prnt, pipeSide pip, prtGenConn id) {
        parent = prnt;
        pipe = pip;
        conn = id;
        parent.conns.add(this);
        new Thread(this).start();
    }

    public int compare(servUpnpHubConn o1, servUpnpHubConn o2) {
        return o1.conn.compare(o1.conn, o2.conn);
    }

    public void run() {
        logger.info("forwarder " + conn.peerAddr + " up");
        try {
            pipe.wait4ready(120000);
            for (;;) {
                packHolder pck = pipe.readPacket(true);
                if (pck == null) {
                    break;
                }
                parent.doPacket(this, pck);
            }
        } catch (Exception e) {
            logger.traceback(e);
        }
        pipe.setClose();
        logger.info("forwarder " + conn.peerAddr + " down");
        parent.conns.del(this);
    }

    public void doPack(packHolder pck) {
        pck.pipeSend(pipe, 0, pck.dataSize(), 2);
    }

}
