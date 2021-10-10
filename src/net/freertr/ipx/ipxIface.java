package net.freertr.ipx;

import net.freertr.addr.addrIpx;
import net.freertr.addr.addrMac;
import net.freertr.addr.addrPrefix;
import net.freertr.ifc.ifcDn;
import net.freertr.ifc.ifcEthTyp;
import net.freertr.ifc.ifcNull;
import net.freertr.ifc.ifcUp;
import net.freertr.pack.packHolder;
import net.freertr.tab.tabRouteIface;
import net.freertr.util.counter;
import net.freertr.util.state.states;

/**
 * ipx interface
 * 
 * @author matecsaba
 */
public class ipxIface extends tabRouteIface implements ifcUp {

    /**
     * packet type
     */
    public final static int type = 0x8137;

    /**
     * packet handler
     */
    public final ifcEthTyp ethtyp;

    /**
     * ipx handler
     */
    public final ipxFwd upper;

    /**
     * ipx address
     */
    public addrIpx addr;

    /**
     * hardware address
     */
    public addrMac hwaddr = addrMac.getRandom();

    /**
     * prefix of network
     */
    public addrPrefix<addrIpx> network;

    /**
     * ready for use, true if yes, false if no
     */
    public boolean ready = true;

    /**
     * counter of interface
     */
    public final counter cntr = new counter();

    private ifcDn lower = new ifcNull();

    /**
     * create new instance
     *
     * @param num number assigned to it
     * @param prn upper handler
     * @param hnd packet handler
     */
    public ipxIface(int num, ipxFwd prn, ifcEthTyp hnd) {
        ifwNum = num;
        ethtyp = hnd;
        upper = prn;
    }

    /**
     * convert to string
     *
     * @return string
     */
    public String toString() {
        return "" + ethtyp;
    }

    /**
     * send one packet
     *
     * @param pck packet to send
     */
    public void sendPack(packHolder pck) {
        pck.merge2beg();
        pck.msbPutW(0, type);
        pck.putSkip(2);
        pck.merge2beg();
        lower.sendPack(pck);
    }

    /**
     * received packet
     *
     * @param pck packet
     */
    public void recvPack(packHolder pck) {
        cntr.rx(pck);
        int typ = pck.msbGetW(0); // ethertype
        if (typ != type) {
            cntr.drop(pck, counter.reasons.badEthTyp);
            return;
        }
        pck.getSkip(2);
        upper.ifacePack(this, pck);
    }

    /**
     * set parent
     *
     * @param parent parent
     */
    public void setParent(ifcDn parent) {
        lower = parent;
        try {
            hwaddr = (addrMac) lower.getHwAddr().copyBytes();
        } catch (Exception e) {
        }
    }

    /**
     * set state
     *
     * @param stat state
     */
    public void setState(states stat) {
        cntr.stateChange(stat);
        upper.ifaceState(this, stat);
    }

    /**
     * close interface
     */
    public void closeUp() {
        upper.ifaceDel(this);
    }

    /**
     * get counter
     *
     * @return counter
     */
    public counter getCounter() {
        return cntr;
    }

}
