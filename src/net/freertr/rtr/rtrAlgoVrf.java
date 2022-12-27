package net.freertr.rtr;

import java.util.List;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrIPv4;
import net.freertr.ip.ipRtr;
import net.freertr.tab.tabRoute;
import net.freertr.user.userHelping;
import net.freertr.util.cmds;

/**
 * one flexible algorithm
 *
 * @author matecsaba
 */
public class rtrAlgoVrf extends ipRtr {

    private final rtrAlgo parent;

    /**
     * unregister from ip
     */
    public void unregister2ip() {
        parent.fwd.routerDel(this);
    }

    /**
     * register to ip
     */
    public void register2ip() {
        parent.fwd.routerAdd(this, parent.typ, parent.prc);
    }

    /**
     * update routes
     *
     * @param rou route table
     */
    public void update2ip(tabRoute<addrIP> rou) {
        rou.setProto(routerProtoTyp, routerProcNum);
        if (rou.preserveTime(routerComputedU)) {
            return;
        }
        routerComputedU = rou;
        routerComputedM = rou;
        parent.fwd.routerChg(this, false);
    }

    /**
     * create new instance
     *
     * @param p parent to use
     */
    public rtrAlgoVrf(rtrAlgo p) {
        parent = p;
    }

    /**
     * convert to string
     *
     * @return string
     */
    public String toString() {
        return "flexalgo on " + parent.fwd;
    }

    public synchronized void routerCreateComputed() {
    }

    public void routerRedistChanged() {
    }

    public void routerOthersChanged() {
    }

    public void routerGetHelp(userHelping l) {
    }

    public void routerGetConfig(List<String> l, String beg, int filter) {
    }

    public boolean routerConfigure(cmds cmd) {
        return true;
    }

    public void routerCloseNow() {
    }

    public int routerNeighCount() {
        return 0;
    }

    public void routerNeighList(tabRoute<addrIP> tab) {
    }

    public int routerIfaceCount() {
        return 0;
    }

    public int routerRecursions() {
        return 1;
    }

    public void routerLinkStates(tabRoute<addrIP> tab, int par, int asn, addrIPv4 adv) {
    }

}
