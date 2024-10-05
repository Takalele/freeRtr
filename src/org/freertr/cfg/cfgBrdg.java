package org.freertr.cfg;

import java.util.ArrayList;
import java.util.List;
import org.freertr.ifc.ifcBridge;
import org.freertr.tab.tabGen;
import org.freertr.user.userFilter;
import org.freertr.user.userHelping;
import org.freertr.util.bits;
import org.freertr.util.cmds;

/**
 * one bridge configuration
 *
 * @author matecsaba
 */
public class cfgBrdg implements Comparable<cfgBrdg>, cfgGeneric {

    /**
     * number of this bridge
     */
    public final int number;

    /**
     * bridge handler
     */
    public ifcBridge bridgeHed;

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "bridge .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "description",
        "bridge .*!" + cmds.tabulator + "rd 0:0",
        "bridge .*!" + cmds.tabulator + "rt-import 0:0",
        "bridge .*!" + cmds.tabulator + "rt-export 0:0",
        "bridge .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "mac-learn",
        "bridge .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "mac-move",
        "bridge .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "inspect",
        "bridge .*!" + cmds.tabulator + "stp-mode none",
        "bridge .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "mac-address",
        "bridge .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "private-bridge",
        "bridge .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "block-unicast",
        "bridge .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "block-multicast",
        "bridge .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "padup-small",
        "bridge .*!" + cmds.tabulator + "mac-age 600000",
        "bridge .*!" + cmds.tabulator + "mac-limit 0",
        "bridge .*!" + cmds.tabulator + "stp-priority 32768",
        "bridge .*!" + cmds.tabulator + "stp-time 2000 20000 15000"
    };

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public int compareTo(cfgBrdg o) {
        if (number < o.number) {
            return -1;
        }
        if (number > o.number) {
            return +1;
        }
        return 0;
    }

    public String toString() {
        return "brdg " + number;
    }

    /**
     * create new bridge instance
     *
     * @param nam name of bridge
     */
    public cfgBrdg(String nam) {
        number = bits.str2num(nam);
    }

    /**
     * close this bridge
     */
    public void closeUp() {
    }

    /**
     * get name of interface represents this bridge
     *
     * @return interface name
     */
    public String getIntName() {
        return "bvi" + number;
    }

    public List<String> getShRun(int filter) {
        List<String> l = new ArrayList<String>();
        l.add("bridge " + number);
        bridgeHed.getConfig(l, cmds.tabulator);
        l.add(cmds.tabulator + cmds.finish);
        l.add(cmds.comment);
        if ((filter & 1) == 0) {
            return l;
        }
        return userFilter.filterText(l, defaultF);
    }

    public void getHelp(userHelping l) {
        ifcBridge.getHelp(l);
    }

    public void doCfgStr(cmds cmd) {
        bridgeHed.doConfig(cmd);
    }

    public String getPrompt() {
        return "brdg";
    }

}
