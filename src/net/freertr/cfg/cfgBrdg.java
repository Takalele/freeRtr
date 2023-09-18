package net.freertr.cfg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.freertr.ifc.ifcBridge;
import net.freertr.tab.tabGen;
import net.freertr.user.userFilter;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;

/**
 * one bridge configuration
 *
 * @author matecsaba
 */
public class cfgBrdg implements Comparator<cfgBrdg>, cfgGeneric {

    /**
     * name of this bridge
     */
    public String name;

    /**
     * number of this bridge
     */
    public int num;

    /**
     * bridge handler
     */
    public ifcBridge bridgeHed;

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "bridge .*! no description",
        "bridge .*! rd 0:0",
        "bridge .*! rt-import 0:0",
        "bridge .*! rt-export 0:0",
        "bridge .*! no mac-learn",
        "bridge .*! no mac-move",
        "bridge .*! no inspect",
        "bridge .*! stp-mode none",
        "bridge .*! no mac-address",
        "bridge .*! no private-bridge",
        "bridge .*! no block-unicast",
        "bridge .*! no block-multicast",
        "bridge .*! no padup-small",
        "bridge .*! mac-age 600000",
        "bridge .*! mac-limit 0",
        "bridge .*! stp-priority 32768",
        "bridge .*! stp-time 2000 20000 15000"
    };

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public int compare(cfgBrdg o1, cfgBrdg o2) {
        return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
    }

    public String toString() {
        return "brdg " + name;
    }

    /**
     * create new bridge instance
     *
     * @param nam name of bridge
     */
    public cfgBrdg(String nam) {
        num = bits.str2num(nam);
        name = "" + num;
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
        return "bvi" + name;
    }

    public List<String> getShRun(int filter) {
        List<String> l = new ArrayList<String>();
        l.add("bridge " + name);
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
