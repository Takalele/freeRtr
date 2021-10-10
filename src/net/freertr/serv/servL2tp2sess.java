package net.freertr.serv;

import java.util.Comparator;
import net.freertr.addr.addrEmpty;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrType;
import net.freertr.cfg.cfgIfc;
import net.freertr.ifc.ifcDn;
import net.freertr.ifc.ifcNull;
import net.freertr.ifc.ifcUp;
import net.freertr.ip.ipFwd;
import net.freertr.pack.packHolder;
import net.freertr.pack.packL2tp2;
import net.freertr.util.counter;
import net.freertr.util.state;

/**
 * layer two tunneling protocol (rfc2661) session
 *
 * @author matecsaba
 */
public class servL2tp2sess implements ifcDn, Comparator<servL2tp2sess> {

    /**
     * local session id
     */
    protected int sesLoc;

    /**
     * remote session id
     */
    protected int sesRem;

    private ifcUp upper = new ifcNull();

    private counter cntr = new counter();

    private cfgIfc ifc;

    private servL2tp2conn lower;

    /**
     * create instance
     *
     * @param parent parent
     */
    public servL2tp2sess(servL2tp2conn parent) {
        lower = parent;
    }

    public String toString() {
        return lower + "/" + sesLoc;
    }

    public int compare(servL2tp2sess o1, servL2tp2sess o2) {
        if (o1.sesLoc < o2.sesLoc) {
            return -1;
        }
        if (o1.sesLoc > o2.sesLoc) {
            return +1;
        }
        return 0;
    }

    /**
     * start upper layer
     */
    public void doStartup() {
        upper = new ifcNull();
        ifc = lower.lower.clnIfc.cloneStart(this);
    }

    public addrType getHwAddr() {
        return new addrEmpty();
    }

    public void setFilter(boolean promisc) {
    }

    public state.states getState() {
        return state.states.up;
    }

    public void closeDn() {
        lower.enQueue(packL2tp2.createCDN(sesRem, sesLoc));
        lower.sesDel(sesLoc);
        upper.closeUp();
        if (ifc != null) {
            ifc.cloneStop();
        }
    }

    public void flapped() {
        closeDn();
    }

    public void setUpper(ifcUp server) {
        upper = server;
        upper.setParent(this);
    }

    public counter getCounter() {
        return cntr;
    }

    public int getMTUsize() {
        return 1400;
    }

    public long getBandwidth() {
        return 8000000;
    }

    public void sendPack(packHolder pck) {
        cntr.tx(pck);
        pck.putDefaults();
        lower.sesData(this, pck);
    }

    /**
     * send to upper layer
     *
     * @param pck packet to send
     */
    public void send2upper(packHolder pck) {
        cntr.rx(pck);
        upper.recvPack(pck);
    }

    /**
     * get forwarder
     *
     * @return forwarder used
     */
    public ipFwd getFwder() {
        return lower.lower.srvVrf.getFwd(lower.conn.peerAddr);
    }

    /**
     * get local address
     *
     * @return address
     */
    public addrIP getAddrLoc() {
        return lower.conn.iface.addr.copyBytes();
    }

    /**
     * get remote address
     *
     * @return address
     */
    public addrIP getAddrRem() {
        return lower.conn.peerAddr.copyBytes();
    }

    /**
     * get local port number
     *
     * @return session id, 0 if no session
     */
    public int getPortLoc() {
        return lower.conn.portLoc;
    }

    /**
     * get remote port number
     *
     * @return session id, 0 if no session
     */
    public int getPortRem() {
        return lower.conn.portRem;
    }

    /**
     * get remote session id
     *
     * @return session id, 0 if no session
     */
    public int getSessRem() {
        return sesRem;
    }

    /**
     * get remote tunn id
     *
     * @return session id, 0 if no tunnel
     */
    public int getTunnRem() {
        return lower.tunRem;
    }

}
