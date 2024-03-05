package org.freertr.spf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.freertr.addr.addrIP;
import org.freertr.addr.addrPrefix;
import org.freertr.addr.addrType;

/**
 * spf prefix
 *
 * @param <Ta> type of nodes
 * @author matecsaba
 */
public class spfPrefix<Ta extends addrType> implements Comparator<spfPrefix<Ta>> {

    /**
     * prefix
     */
    protected final addrPrefix<addrIP> prefix;

    /**
     * nodes
     */
    protected final List<spfNode<Ta>> nodes = new ArrayList<spfNode<Ta>>();

    /**
     * create instance
     *
     * @param pfx prefix
     */
    public spfPrefix(addrPrefix<addrIP> pfx) {
        prefix = pfx;
    }

    public String toString() {
        String a = "";
        for (int i = 0; i < nodes.size(); i++) {
            a += " " + nodes.get(i);
        }
        return addrPrefix.ip2str(prefix) + "|" + a;
    }

    public int compare(spfPrefix<Ta> o1, spfPrefix<Ta> o2) {
        return o1.prefix.compare(o1.prefix, o2.prefix);
    }

}
