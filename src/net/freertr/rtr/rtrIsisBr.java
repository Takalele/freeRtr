package net.freertr.rtr;

import net.freertr.addr.addrIP;
import net.freertr.pack.packHolder;
import net.freertr.tab.tabLabelBier;
import net.freertr.tab.tabRouteEntry;
import net.freertr.util.bits;
import net.freertr.enc.encTlv;

/**
 * isis bier
 *
 * @author matecsaba
 */
public class rtrIsisBr {

    private rtrIsisBr() {
    }

    /**
     * bier info
     */
    public static final int typBier = 32;

    /**
     * generate br prefix
     *
     * @param lower lower layer to use
     * @param idx index
     * @return bytes generated
     */
    protected static byte[] putPref(rtrIsis lower, int idx) {
        if (lower.bierLab == null) {
            return new byte[0];
        }
        packHolder pck = new packHolder(true, true);
        encTlv tlv = rtrIsis.getTlv();
        tlv.valDat[0] = 0; // algorithm
        tlv.valDat[1] = 0; // ipa
        tlv.valDat[2] = 0; // subdomain
        bits.msbPutW(tlv.valDat, 3, idx); // bfr id
        tlv.valDat[5] = 1; // type: mpls encap
        tlv.valDat[6] = 4; // length
        bits.msbPutD(tlv.valDat, 7, lower.bierLab[0].label | (tabLabelBier.num2bsl(lower.bierLen) << 20)); // bsl, label
        tlv.valDat[7] = (byte) lower.bierLab.length; // label range
        tlv.valSiz = 11;
        tlv.valTyp = typBier;
        tlv.putThis(pck);
        pck.merge2beg();
        return pck.getCopy();
    }

    /**
     * read br prefix
     *
     * @param tlv data
     * @param prf prefix
     */
    protected static void getPref(encTlv tlv, tabRouteEntry<addrIP> prf) {
        if (tlv.valTyp != typBier) {
            return;
        }
        prf.best.bierIdx = bits.msbGetW(tlv.valDat, 3); // bfr id
        if (tlv.valDat[5] != 1) { // type
            return;
        }
        if (tlv.valDat[6] != 4) { // length
            return;
        }
        int i = bits.msbGetD(tlv.valDat, 7);
        prf.best.bierBeg = i & 0xfffff; // label
        prf.best.bierHdr = (i >>> 20) & 0xf; // bsl
    }

}
