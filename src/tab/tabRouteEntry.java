package tab;

import addr.addrIP;
import addr.addrPrefix;
import addr.addrType;
import ip.ipFwd;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import user.userFormat;
import util.bits;
import util.counter;

/**
 * represents one route table entry
 *
 * @param <T> class of address
 * @author matecsaba
 */
public class tabRouteEntry<T extends addrType> implements Comparator<tabRouteEntry<T>> {

    /**
     * route distinguisher
     */
    public long rouDst;

    /**
     * prefix
     */
    public addrPrefix<T> prefix;

    /**
     * nlri
     */
    public byte[] nlri;

    /**
     * best path
     */
    public tabRouteAttr<T> best;

    /**
     * alternate instances
     */
    public List<tabRouteAttr<T>> alts;

    /**
     * counter
     */
    public counter cntr;

    /**
     * hardware counter
     */
    public counter hwCntr;

    /**
     * create instance
     */
    public tabRouteEntry() {
        best = new tabRouteAttr<T>();
        alts = new ArrayList<tabRouteAttr<T>>();
        alts.add(best);
    }
    
    public String toString() {
        return "" + prefix;
    }

    /**
     * select best route
     */
    public void selectBest() {
        best = alts.get(0);
        for (int i = 1; i < alts.size(); i++) {
            tabRouteAttr<T> ntry = alts.get(i);
            if (best.isOtherBetter(ntry, true)) {
                best = ntry;
            }
        }
    }

    /**
     * select best route
     */
    public void hashBest() {
        best = alts.get(prefix.network.getHash() % alts.size());
    }

    /**
     * find alternate by id
     *
     * @param id id to find
     * @return entry number, -1 if not found
     */
    public int findId(int id) {
        for (int i = 0; i < alts.size(); i++) {
            if (alts.get(i).ident == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * reduce to best route
     */
    public void reduce2best() {
        alts.clear();
        alts.add(best);
    }

    /**
     * find same forwarder
     *
     * @param other attribute to find
     * @return instance, null if not found
     */
    public tabRouteAttr<T> sameFwder(tabRouteAttr<T> other) {
        if (other.nextHop == null) {
            return null;
        }
        for (int i = 0; i < alts.size(); i++) {
            tabRouteAttr<T> ntry = alts.get(i);
            if (ntry.iface != other.iface) {
                continue;
            }
            if (ntry.nextHop == null) {
                continue;
            }
            if (other.nextHop.getSize() != ntry.nextHop.getSize()) {
                continue;
            }
            if (other.nextHop.compare(other.nextHop, ntry.nextHop) == 0) {
                return ntry;
            }
        }
        return null;
    }

    /**
     * clone this table entry
     *
     * @param mod mode to use
     * @return new instance containing same data
     */
    public tabRouteEntry<T> copyBytes(tabRoute.addType mod) {
        tabRouteEntry<T> prf = new tabRouteEntry<T>();
        prf.rouDst = rouDst;
        prf.cntr = cntr;
        prf.hwCntr = hwCntr;
        if (nlri != null) {
            prf.nlri = new byte[nlri.length];
            bits.byteCopy(nlri, 0, prf.nlri, 0, nlri.length);
        }
        if (prefix != null) {
            prf.prefix = prefix.copyBytes();
        }
        switch (mod) {
            case altEcmp:
                prf.alts.clear();
                for (int i = 0; i < alts.size(); i++) {
                    tabRouteAttr<T> ntry = alts.get(i);
                    tabRouteAttr<T> attr = new tabRouteAttr<T>();
                    ntry.copyBytes(attr, true);
                    prf.alts.add(attr);
                }
                prf.hashBest();
                return prf;
            case ecmp:
                prf.alts.clear();
                for (int i = 0; i < alts.size(); i++) {
                    tabRouteAttr<T> ntry = alts.get(i);
                    if (ntry.isOtherBetter(best, false)) {
                        continue;
                    }
                    tabRouteAttr<T> attr = new tabRouteAttr<T>();
                    ntry.copyBytes(attr, true);
                    prf.alts.add(attr);
                }
                prf.hashBest();
                return prf;
            case alters:
                prf.alts.clear();
                for (int i = 0; i < alts.size(); i++) {
                    tabRouteAttr<T> ntry = alts.get(i);
                    tabRouteAttr<T> attr = new tabRouteAttr<T>();
                    ntry.copyBytes(attr, true);
                    prf.alts.add(attr);
                }
                prf.selectBest();
                return prf;
            case lnkEcmp:
                prf.alts.clear();
                for (int i = 0; i < alts.size(); i++) {
                    tabRouteAttr<T> ntry = alts.get(i);
                    if (ntry.isOtherBetter(best, false)) {
                        continue;
                    }
                    prf.alts.add(ntry);
                }
                prf.hashBest();
                return prf;
            case lnkAlters:
                prf.alts.clear();
                for (int i = 0; i < alts.size(); i++) {
                    prf.alts.add(alts.get(i));
                }
                prf.selectBest();
                return prf;
            default:
                best.copyBytes(prf.best, true);
                return prf;
        }
    }

    /**
     * add attribute
     *
     * @param other attribute to add
     */
    public void addAlt(tabRouteAttr<T> other) {
        alts.add(other);
    }

    /**
     * add attribute
     *
     * @param idx index
     * @param other attribute to add
     */
    public void setAlt(int idx, tabRouteAttr<T> other) {
        alts.set(idx, other);
    }

    /**
     * add attribute
     *
     * @param others attributes to add
     */
    public void addAlt(List<tabRouteAttr<T>> others) {
        alts.addAll(others);
    }

    /**
     * add attribute
     *
     * @param idx index
     */
    public void delAlt(int idx) {
        alts.remove(idx);
    }

    /**
     * check if differs from other
     *
     * @param mod mode to use
     * @param other other to test
     * @return false on equals, true on differs
     */
    public boolean differs(tabRoute.addType mod, tabRouteEntry<T> other) {
        if (other == null) {
            return true;
        }
        if (compare(this, other) != 0) {
            return true;
        }
        switch (mod) {
            case alters:
                if (alts.size() != other.alts.size()) {
                    return true;
                }
                for (int i = 0; i < alts.size(); i++) {
                    if (alts.get(i).differs(other.alts.get(i))) {
                        return true;
                    }
                }
                return false;
            default:
                return best.differs(other.best);
        }
    }

    /**
     * need to update with this prefix
     *
     * @param imp new prefix
     * @return true if yes, false if not
     */
    public boolean isOtherBetter(tabRouteEntry<T> imp) {
        return best.isOtherBetter(imp.best, true);
    }

    /**
     * convert to route format
     *
     * @param l list to append
     * @param prf entry to dump
     */
    public static void toShSrRoute(userFormat l, tabRouteEntry<addrIP> prf) {
        String a = addrPrefix.ip2str(prf.prefix);
        for (int i = 0; i < prf.alts.size(); i++) {
            tabRouteAttr<addrIP> attr = prf.alts.get(i);
            if (attr.segrouIdx < 1) {
                continue;
            }
            l.add(a + "|" + attr.toShSrRoute());
        }
    }

    /**
     * convert to route format
     *
     * @param l list to append
     * @param prf entry to dump
     */
    public static void toShBrRoute(userFormat l, tabRouteEntry<addrIP> prf) {
        String a = addrPrefix.ip2str(prf.prefix);
        for (int i = 0; i < prf.alts.size(); i++) {
            tabRouteAttr<addrIP> attr = prf.alts.get(i);
            if (attr.bierIdx < 1) {
                continue;
            }
            l.add(a + "|" + attr.toShBrRoute());
        }
    }

    /**
     * convert to route format
     *
     * @param l list to append
     * @param prf entry to dump
     */
    public static void toShRoute(userFormat l, tabRouteEntry<addrIP> prf) {
        String a = addrPrefix.ip2str(prf.prefix);
        for (int i = 0; i < prf.alts.size(); i++) {
            tabRouteAttr<addrIP> attr = prf.alts.get(i);
            l.add(tabRouteAttr.rouTyp2string(attr) + "|" + a + "|" + attr.toShRoute());
        }
    }

    /**
     * convert to bgp format
     *
     * @param l list to append
     * @param prf entry to dump
     */
    public static void toShBgp(userFormat l, tabRouteEntry<addrIP> prf) {
        String a = toShBgpFirst(prf);
        for (int i = 0; i < prf.alts.size(); i++) {
            tabRouteAttr<addrIP> attr = prf.alts.get(i);
            l.add(a + attr.toShBgpLast());
        }
    }

    /**
     * convert to evpn format
     *
     * @param l list to append
     * @param prf entry to dump
     */
    public static void toShEvpn(userFormat l, tabRouteEntry<addrIP> prf) {
        String a = addrPrefix.ip2evpn(prf.prefix) + " " + tabRtrmapN.rd2string(prf.rouDst);
        for (int i = 0; i < prf.alts.size(); i++) {
            tabRouteAttr<addrIP> attr = prf.alts.get(i);
            l.add(a + attr.toShBgpLast());
        }
    }

    /**
     * convert to bgp format
     *
     * @param l list to append
     * @param prf entry to dump
     * @param evpn evpn
     */
    public static void toShBgpLabels(userFormat l, tabRouteEntry<addrIP> prf, boolean evpn) {
        String a;
        if (evpn) {
            a = addrPrefix.ip2evpn(prf.prefix);
        } else {
            a = toShBgpFirst(prf);
        }
        for (int i = 0; i < prf.alts.size(); i++) {
            tabRouteAttr<addrIP> attr = prf.alts.get(i);
            l.add(a + "|" + attr.toShBgpLabels());
        }
    }

    /**
     * convert to counter format
     *
     * @param l list to append
     * @param prf entry to dump
     */
    public static void toShCntr(userFormat l, tabRouteEntry<addrIP> prf) {
        if (prf.cntr == null) {
            return;
        }
        String a = "";
        String s = "";
        if (prf.hwCntr != null) {
            a = "+" + prf.hwCntr.packTx;
            s = "+" + prf.hwCntr.byteTx;
        }
        l.add(addrPrefix.ip2str(prf.prefix) + "|" + prf.cntr.packTx + a + "|" + prf.cntr.byteTx + s + "|" + bits.timePast(prf.best.time));
    }

    /**
     * convert to bgp format
     *
     * @param prf entry to dump
     * @return converted
     */
    public static String toShBgpFirst(tabRouteEntry<addrIP> prf) {
        String s = "";
        if (prf.rouDst != 0) {
            s = " " + tabRtrmapN.rd2string(prf.rouDst);
        }
        return addrPrefix.ip2str(prf.prefix) + s;
    }

    /**
     * convert to ldp format
     *
     * @param l list to append
     * @param prf entry to dump
     */
    public static void toShLdp(userFormat l, tabRouteEntry<addrIP> prf) {
        String a = addrPrefix.ip2str(prf.prefix);
        for (int i = 0; i < prf.alts.size(); i++) {
            tabRouteAttr<addrIP> attr = prf.alts.get(i);
            l.add(a + "|" + attr.toShLdp());
        }
    }

    /**
     * convert to rpki format
     *
     * @param l list to append
     * @param prf entry to dump
     */
    public static void toShRpki(userFormat l, tabRouteEntry<addrIP> prf) {
        String a = addrPrefix.ip2str(prf.prefix);
        for (int i = 0; i < prf.alts.size(); i++) {
            tabRouteAttr<addrIP> attr = prf.alts.get(i);
            l.add(a + "|" + attr.toShRpki());
        }
    }

    /**
     * convert to ecmp format
     *
     * @param l list to append
     * @param prf entry to dump
     * @param evpn evpn
     */
    public static void toShEcmp(userFormat l, tabRouteEntry<addrIP> prf, boolean evpn) {
        String a;
        if (evpn) {
            a = addrPrefix.ip2evpn(prf.prefix);
        } else {
            a = toShBgpFirst(prf);
        }
        for (int i = 0; i < prf.alts.size(); i++) {
            tabRouteAttr<addrIP> attr = prf.alts.get(i);
            l.add(a + "|" + prf.alts.size() + "|" + (!attr.isOtherBetter(prf.best, false)) + "|" + (attr == prf.best) + "|" + attr.toShEcmp());
        }
    }

    /**
     * full dump of this prefix
     *
     * @param fwd forwarding core to use
     * @return list describes this prefix
     */
    public List<String> fullDump(ipFwd fwd) {
        List<String> l = new ArrayList<String>();
        if (fwd != null) {
            l.add("vrf = " + fwd.vrfName);
            l.add("ipver = " + fwd.ipVersion);
        }
        l.add("rd = " + tabRtrmapN.rd2string(rouDst));
        l.add("prefix = " + prefix);
        l.add("prefix network = " + prefix.network);
        l.add("prefix broadcast = " + prefix.broadcast);
        l.add("prefix wildcard = " + prefix.wildcard);
        l.add("prefix netmask = " + prefix.mask);
        l.add("nlri = " + bits.byteDump(nlri, 0, -1));
        l.add("alternates = " + alts.size());
        for (int i = 0; i < alts.size(); i++) {
            tabRouteAttr<T> ntry = alts.get(i);
            l.add("alternate #" + i + " attributes: ecmp=" + (!ntry.isOtherBetter(best, false)) + " best=" + (ntry == best));
            ntry.fullDump(l);
        }
        l.add("counter = " + counter.getShStat(cntr));
        l.add("hardware counter = " + counter.getShStat(hwCntr));
        return l;
    }
    
    public int compare(tabRouteEntry<T> o1, tabRouteEntry<T> o2) {
        if (o1.rouDst < o2.rouDst) {
            return -1;
        }
        if (o1.rouDst > o2.rouDst) {
            return +1;
        }
        return prefix.compare(o1.prefix, o2.prefix);
    }
    
}
