package org.freertr.rtr;

import java.util.List;
import org.freertr.addr.addrEui;
import org.freertr.addr.addrIP;
import org.freertr.addr.addrIPv4;
import org.freertr.addr.addrPrefix;
import org.freertr.ip.ipCor4;
import org.freertr.ip.ipCor6;
import org.freertr.ip.ipFwd;
import org.freertr.ip.ipFwdIface;
import org.freertr.ip.ipRtr;
import org.freertr.pack.packHolder;
import org.freertr.prt.prtGenConn;
import org.freertr.prt.prtServP;
import org.freertr.prt.prtUdp;
import org.freertr.tab.tabGen;
import org.freertr.tab.tabIndex;
import org.freertr.tab.tabRoute;
import org.freertr.tab.tabRouteAttr;
import org.freertr.tab.tabRouteEntry;
import org.freertr.user.userFormat;
import org.freertr.user.userHelping;
import org.freertr.util.bits;
import org.freertr.util.cmds;
import org.freertr.util.counter;
import org.freertr.util.debugger;
import org.freertr.util.logger;
import org.freertr.util.state;
import org.freertr.enc.encTlv;

/**
 * babel routing protocol (rfc6126) version 2
 *
 * @author matecsaba
 */
public class rtrBabel extends ipRtr implements prtServP {

    /**
     * port number
     */
    public final static int port = 6696;

    /**
     * version number
     */
    public final static int version = 2;

    /**
     * magic number
     */
    public final static int magic = 42;

    /**
     * header size
     */
    public final static int size = 4;

    /**
     * pad1 tlv
     */
    public final static int tlvPad1 = 0;

    /**
     * padN tlv
     */
    public final static int tlvPadN = 1;

    /**
     * ack request tlv
     */
    public final static int tlvAckReq = 2;

    /**
     * ack response tlv
     */
    public final static int tlvAckRep = 3;

    /**
     * hello tlv
     */
    public final static int tlvHello = 4;

    /**
     * ihu tlv
     */
    public final static int tlvIhu = 5;

    /**
     * router id tlv
     */
    public final static int tlvRtrId = 6;

    /**
     * next hop tlv
     */
    public final static int tlvNxtHop = 7;

    /**
     * update tlv
     */
    public final static int tlvUpdate = 8;

    /**
     * route req tlv
     */
    public final static int tlvRouReq = 9;

    /**
     * seqno req tlv
     */
    public final static int tlvSeqReq = 10;

    /**
     * the udp protocol
     */
    protected prtUdp udpCore;

    /**
     * forwarding core
     */
    public final ipFwd fwdCore;

    /**
     * router id
     */
    public addrEui routerID = new addrEui();

    /**
     * suppress interface addresses
     */
    public boolean suppressAddr;

    /**
     * neighbors
     */
    protected tabGen<rtrBabelNeigh> neighs;

    /**
     * interfaces
     */
    protected tabGen<rtrBabelIface> ifaces;

    /**
     * sequence number
     */
    protected int seqno;

    /**
     * create one babel process
     *
     * @param forwarder the ip protocol
     * @param protocol the udp protocol
     * @param id process id
     */
    public rtrBabel(ipFwd forwarder, prtUdp protocol, int id) {
        if (debugger.rtrBabelEvnt) {
            logger.debug("startup");
        }
        fwdCore = forwarder;
        udpCore = protocol;
        tabRouteAttr.routeType rouTyp = null;
        switch (fwdCore.ipVersion) {
            case ipCor4.protocolVersion:
                rouTyp = tabRouteAttr.routeType.babel4;
                break;
            case ipCor6.protocolVersion:
                rouTyp = tabRouteAttr.routeType.babel6;
                break;
            default:
                break;
        }
        ifaces = new tabGen<rtrBabelIface>();
        neighs = new tabGen<rtrBabelNeigh>();
        seqno = bits.randomW();
        routerCreateComputed();
        fwdCore.routerAdd(this, rouTyp, id);
    }

    /**
     * get neighbor count
     *
     * @return count
     */
    public int routerNeighCount() {
        return neighs.size();
    }

    /**
     * list neighbors
     *
     * @param tab list
     */
    public void routerNeighList(tabRoute<addrIP> tab) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrBabelNeigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            if (nei.iface.iface.lower.getState() != state.states.up) {
                continue;
            }
            tabRouteEntry<addrIP> ntry = new tabRouteEntry<addrIP>();
            ntry.prefix = new addrPrefix<addrIP>(nei.conn.peerAddr, addrIP.size * 8);
            tabRoute.addUpdatedEntry(tabRoute.addType.better, tab, rtrBgpUtil.sfiUnicast, 0, ntry, true, null, null, routerAutoMesh);
        }
    }

    /**
     * get interface count
     *
     * @return count
     */
    public int routerIfaceCount() {
        return ifaces.size();
    }

    /**
     * maximum recursion depth
     *
     * @return allowed number
     */
    public int routerRecursions() {
        return 1;
    }

    /**
     * get list of link states
     *
     * @param tab table to update
     * @param par parameter
     * @param asn asn
     * @param adv advertiser
     */
    public void routerLinkStates(tabRoute<addrIP> tab, int par, int asn, addrIPv4 adv) {
    }

    /**
     * update sequence number
     */
    protected void incSeq() {
        seqno = (seqno + 1) & 0xffff;
    }

    /**
     * get tlv handler
     *
     * @return handler
     */
    protected static encTlv getTlv() {
        return new encTlv(0, 8, 8, 8, 1, 0, 2, 1, 0, 512, true);
    }

    /**
     * convert to string
     *
     * @return string
     */
    public String toString() {
        return "babel on " + fwdCore;
    }

    /**
     * add one interface to work on
     *
     * @param ifc ip forwarder interface
     * @return false if successful, true if error happened
     */
    public rtrBabelIface addInterface(ipFwdIface ifc) {
        if (debugger.rtrBabelEvnt) {
            logger.debug("add iface " + ifc);
        }
        if (ifc == null) {
            return null;
        }
        rtrBabelIface ntry = new rtrBabelIface(this, ifc);
        rtrBabelIface old = ifaces.add(ntry);
        if (old != null) {
            ntry = old;
        }
        ntry.register2udp();
        routerCreateComputed();
        return ntry;
    }

    /**
     * close interface
     *
     * @param iface interface
     */
    public void closedInterface(ipFwdIface iface) {
        rtrBabelIface ifc = new rtrBabelIface(this, iface);
        ifc = ifaces.del(ifc);
        if (ifc == null) {
            return;
        }
        ifc.unregister2udp();
        for (int i = neighs.size() - 1; i >= 0; i--) {
            rtrBabelNeigh nei = neighs.get(i);
            if (nei.iface.iface.ifwNum != iface.ifwNum) {
                continue;
            }
            neighs.del(nei);
            nei.unregister2udp();
        }
        routerCreateComputed();
    }

    /**
     * accept connection
     *
     * @param id connection
     * @return false if success, true if error
     */
    public boolean datagramAccept(prtGenConn id) {
        rtrBabelIface ifc = new rtrBabelIface(this, id.iface);
        ifc = ifaces.find(ifc);
        if (ifc == null) {
            logger.warn("no interface " + id);
            return true;
        }
        if ((ifc.connectedCheck) && (!ifc.iface.network.matches(id.peerAddr))) {
            logger.info("got from out of subnet peer " + id);
            return true;
        }
        logger.warn("neighbor " + id.peerAddr + " up");
        rtrBabelNeigh ntry = new rtrBabelNeigh(id, ifc);
        rtrBabelNeigh old = neighs.add(ntry);
        if (old != null) {
            ntry = old;
        }
        ntry.conn = id;
        if (ifc.bfdTrigger) {
            ifc.iface.bfdAdd(id.peerAddr, ntry, "babel");
        }
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
     * close connection
     *
     * @param id connection
     */
    public void datagramClosed(prtGenConn id) {
        rtrBabelNeigh ntry = new rtrBabelNeigh(id, null);
        ntry = neighs.del(ntry);
        if (ntry == null) {
            return;
        }
        logger.error("neighbor " + id.peerAddr + " down");
        id.iface.bfdDel(id.peerAddr, ntry);
        routerCreateComputed();
    }

    /**
     * work connection
     *
     * @param id connection
     */
    public void datagramWork(prtGenConn id) {
        rtrBabelNeigh nei = new rtrBabelNeigh(id, null);
        nei = neighs.find(nei);
        if (nei != null) {
            if (nei.doWork()) {
                routerCreateComputed();
            }
            return;
        }
        rtrBabelIface ifc = new rtrBabelIface(this, id.iface);
        ifc = ifaces.find(ifc);
        if (ifc != null) {
            ifc.doWork();
            return;
        }
        id.setClosing();
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
        rtrBabelNeigh ntry = new rtrBabelNeigh(id, null);
        ntry = neighs.find(ntry);
        if (ntry == null) {
            return false;
        }
        ntry.bfdPeerDown();
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
        rtrBabelNeigh ntry = new rtrBabelNeigh(id, null);
        ntry = neighs.find(ntry);
        if (ntry == null) {
            id.setClosing();
            return false;
        }
        if (ntry.gotPack(pck)) {
            return false;
        }
        routerCreateComputed();
        return false;
    }

    /**
     * create computed table
     */
    public synchronized void routerCreateComputed() {
        if (debugger.rtrBabelEvnt) {
            logger.debug("create table");
        }
        tabRoute<addrIP> tab = new tabRoute<addrIP>("babel");
        tabRouteEntry<addrIP> ntry;
        for (int i = 0; i < ifaces.size(); i++) {
            rtrBabelIface ifc = ifaces.get(i);
            if (ifc == null) {
                continue;
            }
            if (ifc.iface.lower.getState() != state.states.up) {
                continue;
            }
            if ((suppressAddr || ifc.suppressAddr) && (!ifc.unsuppressAddr)) {
                continue;
            }
            ntry = tab.add(tabRoute.addType.better, ifc.iface.network, null);
            ntry.best.rouTyp = tabRouteAttr.routeType.conn;
            ntry.best.iface = ifc.iface;
            ntry.best.distance = tabRouteAttr.distanIfc;
        }
        for (int i = 0; i < neighs.size(); i++) {
            rtrBabelNeigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            if (nei.iface.iface.lower.getState() != state.states.up) {
                continue;
            }
            tab.mergeFrom(tabRoute.addType.ecmp, nei.learned, tabRouteAttr.distanLim);
        }
        routerDoAggregates(rtrBgpUtil.sfiUnicast, tab, tab, fwdCore.commonLabel, null, 0);
        tab.setProto(routerProtoTyp, routerProcNum);
        if (tab.preserveTime(routerComputedU)) {
            return;
        }
        routerComputedU = tab;
        routerComputedM = tab;
        routerComputedF = new tabRoute<addrIP>("rx");
        routerComputedI = new tabGen<tabIndex<addrIP>>();
        fwdCore.routerChg(this, false);
    }

    /**
     * redistribution changed
     */
    public void routerRedistChanged() {
        routerCreateComputed();
    }

    /**
     * others changed
     */
    public void routerOthersChanged() {
    }

    /**
     * stop work
     */
    public void routerCloseNow() {
        for (int i = ifaces.size() - 1; i >= 0; i--) {
            rtrBabelIface ntry = ifaces.get(i);
            if (ntry == null) {
                continue;
            }
            ifaces.del(ntry);
            ntry.unregister2udp();
        }
        for (int i = neighs.size() - 1; i >= 0; i--) {
            rtrBabelNeigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            neighs.del(ntry);
            ntry.unregister2udp();
        }
        fwdCore.routerDel(this);
    }

    /**
     * get help
     *
     * @param l list
     */
    public void routerGetHelp(userHelping l) {
        l.add(null, "1 2   router-id                   specify router id");
        l.add(null, "2 .     <addr>                    router id");
        l.add(null, "1 .   suppress-prefix             do not advertise interfaces");
    }

    /**
     * get config
     *
     * @param l list
     * @param beg beginning
     * @param filter filter
     */
    public void routerGetConfig(List<String> l, String beg, int filter) {
        l.add(beg + "router-id " + routerID);
        cmds.cfgLine(l, !suppressAddr, beg, "suppress-prefix", "");
    }

    /**
     * configure
     *
     * @param cmd command
     * @return false if success, true if error
     */
    public boolean routerConfigure(cmds cmd) {
        String s = cmd.word();
        boolean negated = false;
        if (s.equals(cmds.negated)) {
            s = cmd.word();
            negated = true;
        }
        if (s.equals("router-id")) {
            routerID.fromString(cmd.word());
            if (negated) {
                routerID = new addrEui();
            }
            return false;
        }
        if (s.equals("suppress-prefix")) {
            suppressAddr = !negated;
            return false;
        }
        return true;
    }

    /**
     * list neighbors
     *
     * @return list of neighbors
     */
    public userFormat showNeighs() {
        userFormat l = new userFormat("|", "interface|learn|neighbor|uptime");
        for (int i = 0; i < neighs.size(); i++) {
            rtrBabelNeigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            l.add(ntry.iface.iface + "|" + ntry.learned.size() + "|" + ntry.conn.peerAddr + "|" + bits.timePast(ntry.upTime));
        }
        return l;
    }

    /**
     * list interfaces
     *
     * @return list of interfaces
     */
    public userFormat showIfaces() {
        userFormat l = new userFormat("|", "interface|neighbors");
        for (int i = 0; i < ifaces.size(); i++) {
            rtrBabelIface ifc = ifaces.get(i);
            l.add(ifc.iface + "|" + countNeighs(ifc.iface.ifwNum));
        }
        return l;
    }

    private int countNeighs(int ifc) {
        int o = 0;
        for (int i = 0; i < neighs.size(); i++) {
            rtrBabelNeigh nei = neighs.get(i);
            if (nei.iface.iface.ifwNum == ifc) {
                o++;
            }
        }
        return o;
    }

    /**
     * find peer
     *
     * @param addr address to find
     * @return neighbor, null if not found
     */
    public rtrBabelNeigh findPeer(addrIP addr) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrBabelNeigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            if (addr.compare(addr, ntry.conn.peerAddr) != 0) {
                continue;
            }
            return ntry;
        }
        return null;
    }

}
