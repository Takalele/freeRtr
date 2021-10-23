package net.freertr.tab;

import java.util.Comparator;
import net.freertr.addr.addrPrefix;
import net.freertr.addr.addrType;

/**
 * one index to prefix entry
 *
 * @param <T> type of address
 * @author matecsaba
 */
public class tabIndex<T extends addrType> implements Comparator<tabIndex<T>> {

    /**
     * index
     */
    public final int index;

    /**
     * prefix
     */
    public final addrPrefix<T> prefix;

    /**
     * create entry
     *
     * @param idx index
     * @param prf prefix
     */
    public tabIndex(int idx, addrPrefix<T> prf) {
        index = idx;
        prefix = prf;
    }

    public String toString() {
        return index + "|" + prefix;
    }

    /**
     * copy entry
     *
     * @return copy
     */
    public tabIndex<T> copyBytes() {
        tabIndex<T> n = new tabIndex<T>(index, prefix.copyBytes());
        return n;
    }

    public int compare(tabIndex<T> o1, tabIndex<T> o2) {
        if (o1.index < o2.index) {
            return -1;
        }
        if (o1.index > o2.index) {
            return +1;
        }
        return 0;
    }

    /**
     * is better than other
     *
     * @param o other entry
     * @return true if better, false if not
     */
    public boolean isBetter(tabIndex<T> o) {
        if (prefix.maskLen < o.prefix.maskLen) {
            return false;
        }
        if (prefix.maskLen > o.prefix.maskLen) {
            return true;
        }
        return prefix.compare(prefix, o.prefix) < 0;
    }

    /**
     * merge tables
     *
     * @param <T> type of address
     * @param trg target table
     * @param src source entry
     * @return true if added, false if not
     */
    public static <T extends addrType> boolean add2table(tabGen<tabIndex<T>> trg, tabIndex<T> src) {
        tabIndex<T> old = trg.add(src);
        if (old == null) {
            return true;
        }
        if (!src.isBetter(old)) {
            return false;
        }
        trg.put(src);
        return true;
    }

    /**
     * merge tables
     *
     * @param <T> type of address
     * @param trg target table
     * @param src source table
     * @return entries added
     */
    public static <T extends addrType> int mergeTable(tabGen<tabIndex<T>> trg, tabGen<tabIndex<T>> src) {
        if (src == null) {
            return -1;
        }
        int cnt = 0;
        for (int i = 0; i < src.size(); i++) {
            tabIndex<T> ntry = src.get(i);
            if (add2table(trg, ntry)) {
                cnt++;
            }
        }
        return cnt;
    }

}
