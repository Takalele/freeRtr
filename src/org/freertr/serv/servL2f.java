package org.freertr.serv;

import java.util.List;
import org.freertr.addr.addrIP;
import org.freertr.auth.authLocal;
import org.freertr.cfg.cfgAll;
import org.freertr.cfg.cfgIfc;
import org.freertr.pack.packHolder;
import org.freertr.pack.packL2f;
import org.freertr.pipe.pipeSide;
import org.freertr.prt.prtGenConn;
import org.freertr.prt.prtServP;
import org.freertr.tab.tabGen;
import org.freertr.tab.tabRouteIface;
import org.freertr.user.userFilter;
import org.freertr.user.userFormat;
import org.freertr.user.userHelp;
import org.freertr.util.bits;
import org.freertr.util.cmds;
import org.freertr.util.counter;
import org.freertr.util.state;

/**
 * layer two forwarding protocol (rfc2341) server
 *
 * @author matecsaba
 */
public class servL2f extends servGeneric implements prtServP {

    /**
     * create instance
     */
    public servL2f() {
    }

    /**
     * interface to use
     */
    public cfgIfc clnIfc;

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
     * password
     */
    public String password;

    /**
     * ticks after send a hello
     */
    public int helloTicks = 5;

    /**
     * ticks after give up retry
     */
    public int retryTicks = 8;

    /**
     * list of connections
     */
    public tabGen<servL2fConn> conns = new tabGen<servL2fConn>();

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "server l2f .*!" + cmds.tabulator + "port " + packL2f.port,
        "server l2f .*!" + cmds.tabulator + "protocol " + proto2string(protoAllDgrm),
        "server l2f .*!" + cmds.tabulator + "timer 5 8",
        "server l2f .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "password"
    };

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public tabGen<userFilter> srvDefFlt() {
        return defaultF;
    }

    /**
     * find one connection
     *
     * @param id connection id
     * @param create set true to create if none found
     * @return connection entry
     */
    public servL2fConn connFind(prtGenConn id, boolean create) {
        servL2fConn ntry = new servL2fConn(id, this);
        if (!create) {
            return conns.find(ntry);
        }
        servL2fConn old = conns.add(ntry);
        if (old != null) {
            return old;
        }
        if (password != null) {
            ntry.chlLoc = new byte[16];
            for (int i = 0; i < ntry.chlLoc.length; i++) {
                ntry.chlLoc[i] = (byte) bits.randomB();
            }
        }
        ntry.tunLoc = bits.randomW();
        ntry.created = bits.getTime();
        return ntry;
    }

    /**
     * delete one connection
     *
     * @param id connection id
     * @return connection entry
     */
    public servL2fConn connDel(prtGenConn id) {
        servL2fConn ntry = new servL2fConn(id, this);
        return conns.del(ntry);
    }

    public void srvShRun(String beg, List<String> l, int filter) {
        l.add(beg + "timer " + helloTicks + " " + retryTicks);
        if (clnIfc == null) {
            l.add(beg + "no clone");
        } else {
            l.add(beg + "clone " + clnIfc.name);
        }
        cmds.cfgLine(l, password == null, cmds.tabulator, "password", authLocal.passwdEncode(password, (filter & 2) != 0));
    }

    public boolean srvCfgStr(cmds cmd) {
        String s = cmd.word();
        if (s.equals("timer")) {
            helloTicks = bits.str2num(cmd.word());
            retryTicks = bits.str2num(cmd.word());
            return false;
        }
        if (s.equals("clone")) {
            clnIfc = cfgAll.ifcFind(cmd.word(), 0);
            if (clnIfc == null) {
                cmd.error("no such interface");
                return false;
            }
            if (clnIfc.type != tabRouteIface.ifaceType.dialer) {
                cmd.error("not dialer interface");
                clnIfc = null;
                return false;
            }
            return false;
        }
        if (s.equals("password")) {
            password = authLocal.passwdDecode(cmd.getRemaining());
            return false;
        }
        if (!s.equals(cmds.negated)) {
            return true;
        }
        s = cmd.word();
        if (s.equals("clone")) {
            clnIfc = null;
            return false;
        }
        if (s.equals("password")) {
            password = null;
            return false;
        }
        return true;
    }

    public void srvHelp(userHelp l) {
        l.add(null, false, 1, new int[]{2}, "timer", "set timers");
        l.add(null, false, 2, new int[]{3}, "<num>", "hello ticks");
        l.add(null, false, 3, new int[]{-1}, "<num>", "retry ticks");
        l.add(null, false, 1, new int[]{2}, "clone", "set interface to clone");
        l.add(null, false, 2, new int[]{-1}, "<name:ifc>", "name of interface");
        l.add(null, false, 1, new int[]{2}, "password", "set password");
        l.add(null, false, 2, new int[]{-1}, "<str>", "password");
    }

    public String srvName() {
        return "l2f";
    }

    public int srvPort() {
        return packL2f.port;
    }

    public int srvProto() {
        return protoAllDgrm;
    }

    public boolean srvInit() {
        return genDgrmStart(this, 0);
    }

    public boolean srvDeinit() {
        return genericStop(0);
    }

    public boolean srvAccept(pipeSide pipe, prtGenConn id) {
        id.timeout = timeout;
        id.sendTTL = sendingTTL;
        id.sendTOS = sendingTOS;
        id.sendDFN = sendingDFN;
        id.sendFLW = sendingFLW;
        connFind(id, true);
        return false;
    }

    /**
     * connection ready
     *
     * @param id connection
     */
    public void datagramReady(prtGenConn id) {
    }

    /**
     * stop connection
     *
     * @param id connection
     */
    public void datagramClosed(prtGenConn id) {
        servL2fConn ntry = connDel(id);
        if (ntry == null) {
            return;
        }
        ntry.setClosed();
    }

    /**
     * work connection
     *
     * @param id connection
     */
    public void datagramWork(prtGenConn id) {
        servL2fConn ntry = connFind(id, false);
        if (ntry == null) {
            id.setClosing();
            return;
        }
        ntry.doWork();
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
        return false;
    }

    /**
     * received packet
     *
     * @param id connection
     * @param pck packet
     * @return false on success, true on error
     */
    public boolean datagramRecv(prtGenConn id, packHolder pck) {
        servL2fConn ntry = connFind(id, false);
        if (ntry == null) {
            id.setClosing();
            return false;
        }
        ntry.doRecv(pck);
        return false;
    }

    /**
     * get show
     *
     * @return result
     */
    public userFormat getShow() {
        userFormat res = new userFormat("|", "addr|port|tunloc|tunrem|sess|for|since");
        for (int i = 0; i < conns.size(); i++) {
            servL2fConn ntry = conns.get(i);
            if (ntry == null) {
                continue;
            }
            res.add(ntry.conn.peerAddr + "|" + ntry.conn.portRem + "|" + ntry.tunLoc + "|" + ntry.tunRem + "|" + ntry.session.size() + "|" + bits.timePast(ntry.created) + "|" + bits.time2str(cfgAll.timeZoneName, ntry.created + cfgAll.timeServerOffset, 3));
        }
        return res;
    }

    /**
     * do clear
     *
     * @param peer peer ip
     */
    public void doClear(addrIP peer) {
        for (int i = 0; i < conns.size(); i++) {
            servL2fConn ntry = conns.get(i);
            if (ntry == null) {
                continue;
            }
            if (peer.compareTo(ntry.conn.peerAddr) != 0) {
                continue;
            }
            ntry.setClosed();
        }
    }

}
