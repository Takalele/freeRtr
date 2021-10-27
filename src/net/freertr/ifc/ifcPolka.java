package net.freertr.ifc;

import java.math.BigInteger;
import net.freertr.addr.addrMac;
import net.freertr.cry.cryHashCrc16;
import net.freertr.cry.cryPoly;
import net.freertr.ip.ipFwd;
import net.freertr.ip.ipMpls;
import net.freertr.pack.packHolder;
import net.freertr.util.bits;
import net.freertr.util.counter;
import net.freertr.util.debugger;
import net.freertr.util.logger;
import net.freertr.util.state;

/**
 * polka encapsulation handler
 *
 * @author matecsaba
 */
public class ifcPolka implements ifcUp {

    /**
     * ethertype
     */
    public final static int type = 0x8842;

    /**
     * size
     */
    public final static int size = 20;

    /**
     * counter of this interface
     */
    public counter cntr = new counter();

    /**
     * server that sends our packets
     */
    public ifcDn lower = new ifcNull();

    /**
     * hardware address
     */
    public addrMac hwaddr = addrMac.getBroadcast();

    /**
     * local id
     */
    public final int localId;

    /**
     * base of crc values
     */
    public final int crcBase;

    /**
     * base of crc values
     */
    public final int crcMax;

    /**
     * crc calculator
     */
    public final cryHashCrc16 hasher;

    /**
     * crc calculator
     */
    public final cryPoly[] coeffs;

    /**
     * ipv4 forwarder
     */
    public ipFwd fwd4;

    /**
     * ipv6 forwarder
     */
    public ipFwd fwd6;

    /**
     * create instance
     *
     * @param id local id
     * @param bas crc base
     * @param max crc max
     */
    public ifcPolka(int id, int bas, int max) {
        localId = id;
        crcBase = bas;
        crcMax = max;
        coeffs = generateIrreducible(bas, max);
        hasher = new cryHashCrc16(coeffs[id].getCoeff().intValue(), 0, 0, false);
    }

    private static boolean checkIrreducible(cryPoly[] s, int o, cryPoly f) {
        for (int i = 0; i < o; i++) {
            cryPoly[] r = s[i].modInv(f);
            if (r == null) {
                return true;
            }
            if (r[0] == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * generate irreducible polynominals
     *
     * @param f first polynominal
     * @param n number of entries
     * @return entries generated
     */
    public static cryPoly[] generateIrreducible(int f, int n) {
        cryPoly[] s = new cryPoly[n];
        s[0] = new cryPoly(f);
        for (int o = 1; o < s.length; o++) {
            for (;;) {
                f++;
                if (checkIrreducible(s, o, new cryPoly(f))) {
                    continue;
                }
                break;
            }
            s[o] = new cryPoly(f);
        }
        return s;
    }

    /**
     * encode routeid polynomial
     *
     * @param s switch coefficients
     * @param o switches to visit
     * @return routeid
     */
    public static byte[] encodeRouteId(cryPoly[] s, int[] o) {
        cryPoly M = new cryPoly(1);
        for (int i = 0; i < o.length; i++) {
            M = M.mul(s[o[i]]);
        }
        cryPoly[] m = new cryPoly[o.length];
        cryPoly[] n = new cryPoly[o.length];
        for (int i = 0; i < o.length; i++) {
            m[i] = M.div(s[o[i]])[0];
            n[i] = m[i].modInv(s[o[i]])[0];
        }
        cryPoly r = new cryPoly(BigInteger.valueOf(0));
        for (int i = 0; i < o.length; i++) {
            int p = 0;
            if (i < (o.length - 1)) {
                p = o[i + 1];
            }
            r = r.add(new cryPoly(p).mul(m[i]).mul(n[i]));
        }
        r = r.div(M)[1];
        byte[] b = r.getCoeff().toByteArray();
        byte[] p = new byte[16 - b.length];
        return bits.byteConcat(p, b);
    }

    /**
     * decode routeid polynomial
     *
     * @param s switch coefficients
     * @param v routeid
     * @return next node id
     */
    public static int[] decodeRouteId(cryPoly[] s, byte[] v) {
        cryPoly r = new cryPoly(new BigInteger(v));
        int[] o = new int[s.length];
        for (int i = 0; i < s.length; i++) {
            cryPoly c = r.div(s[i])[1];
            o[i] = c.getCoeff().intValue();
        }
        return o;
    }

    /**
     * parse polka header
     *
     * @param pck packet to parse
     * @return false on success, true on error
     */
    public static boolean parsePolkaHeader(packHolder pck) {
        if (pck.dataSize() < size) {
            return true;
        }
        if (pck.getByte(0) != 0) { // version
            return true;
        }
        pck.NSHttl = pck.getByte(1); // ttl
        pck.IPprt = pck.msbGetW(2); // protocol
        pck.BIERbs = new byte[16];
        pck.getCopy(pck.BIERbs, 0, 4, pck.BIERbs.length);
        pck.getSkip(size);
        return false;
    }

    /**
     * create polka header
     *
     * @param pck packet to parse
     */
    public static void createPolkaHeader(packHolder pck) {
        pck.putByte(0, 0); // version
        pck.putByte(1, pck.NSHttl); // ttl
        pck.msbPutW(2, pck.IPprt); // protocol
        pck.putCopy(pck.BIERbs, 0, 4, pck.BIERbs.length);
        pck.putSkip(size);
        pck.merge2beg();
    }

    public void setParent(ifcDn parent) {
        lower = parent;
        try {
            hwaddr = (addrMac) lower.getHwAddr();
        } catch (Exception e) {
        }
    }

    public counter getCounter() {
        return cntr;
    }

    public void closeUp() {
    }

    public void setState(state.states stat) {
    }

    public String toString() {
        return "polka on " + lower;
    }

    public void recvPack(packHolder pck) {
        cntr.rx(pck);
        if (pck.msbGetW(0) != type) {
            return;
        }
        pck.getSkip(2);
        if (parsePolkaHeader(pck)) {
            cntr.drop(pck, counter.reasons.badHdr);
            return;
        }
        if (debugger.ifcPolkaEvnt) {
            logger.debug("rx ttl=" + pck.NSHttl + " proto=" + pck.IPprt + " route=" + bits.byteDump(pck.BIERbs, 0, -1));
        }
        ipMpls.gotPolkaPack(this, fwd4, fwd6, pck);
    }

    /**
     * send one packet
     *
     * @param pck packet to send
     */
    public void send2eth(packHolder pck) {
        if (debugger.ifcPolkaEvnt) {
            logger.debug("tx ttl=" + pck.NSHttl + " proto=" + pck.IPprt + " route=" + bits.byteDump(pck.BIERbs, 0, -1));
        }
        lower.sendPack(pck);
    }

}
