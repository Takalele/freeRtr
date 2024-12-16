package org.freertr.cfg;

import java.util.ArrayList;
import java.util.List;
import org.freertr.addr.addrIP;
import org.freertr.tab.tabGen;
import org.freertr.tab.tabListing;
import org.freertr.tab.tabObjnetN;
import org.freertr.user.userFilter;
import org.freertr.user.userHelping;
import org.freertr.util.bits;
import org.freertr.util.cmds;

/**
 * network object group configuration
 *
 * @author matecsaba
 */
public class cfgObjnet implements Comparable<cfgObjnet>, cfgGeneric {

    /**
     * name of access list
     */
    public String name;

    /**
     * description of access list
     */
    public String description;

    /**
     * list of statements
     */
    public tabListing<tabObjnetN<addrIP>, addrIP> objgrp;

    /**
     * create object group
     *
     * @param s name
     */
    public cfgObjnet(String s) {
        objgrp = new tabListing<tabObjnetN<addrIP>, addrIP>();
        name = s;
        objgrp.listName = s;
    }

    /**
     * defaults text
     */
    public final static String[] defaultL = {};

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public int compareTo(cfgObjnet o) {
        return name.toLowerCase().compareTo(o.name.toLowerCase());
    }

    public void getHelp(userHelping l) {
        l.add(null, "1  2   sequence              sequence number of an entry");
        l.add(null, "2  1     <num>               sequence number");
        l.add(null, "1  3,. description           specify description");
        l.add(null, "3  3,.   <str>               text");
        l.add(null, "1  2   rename                rename this object group");
        l.add(null, "2  .     <str>               set new name");
        l.add(null, "1  3   <addr>                address of network");
        l.add(null, "3  .     <mask>              mask of network");
        l.add(null, "1  2,. reindex               reindex access list");
        l.add(null, "2  3,.   [num]               initial number to start with");
        l.add(null, "3  .       [num]             increment number");
    }

    public List<String> getShRun(int filter) {
        List<String> l = new ArrayList<String>();
        l.add("object-group network " + name);
        if (description != null) {
            l.add(cmds.tabulator + "description " + description);
        }
        l.addAll(objgrp.dump(cmds.tabulator, filter));
        l.add(cmds.tabulator + cmds.finish);
        l.add(cmds.comment);
        if ((filter & 1) == 0) {
            return l;
        }
        return userFilter.filterText(l, defaultF);
    }

    public void doCfgStr(cmds cmd) {
        String a = cmd.word();
        if (a.equals(cmds.negated)) {
            a = cmd.word();
            if (a.equals("description")) {
                description = null;
                return;
            }
            if (a.equals("sequence")) {
                tabObjnetN<addrIP> ntry = new tabObjnetN<addrIP>(new addrIP());
                ntry.sequence = bits.str2num(cmd.word());
                if (objgrp.del(ntry)) {
                    cmd.error("invalid sequence");
                    return;
                }
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("description")) {
            description = cmd.getRemaining();
            return;
        }
        if (a.equals("rename")) {
            a = cmd.word();
            cfgObjnet v = cfgAll.objnetFind(a, false);
            if (v != null) {
                cmd.error("already exists");
                return;
            }
            name = a;
            objgrp.listName = a;
            return;
        }
        if (a.equals("reindex")) {
            int i = bits.str2num(cmd.word());
            objgrp.reindex(i, bits.str2num(cmd.word()));
            return;
        }
        int seq = objgrp.nextseq();
        if (a.equals("sequence")) {
            seq = bits.str2num(cmd.word());
            a = cmd.word();
        }
        tabObjnetN<addrIP> ntry = new tabObjnetN<addrIP>(new addrIP());
        if (tabObjnetN.fromString(ntry, a + " " + cmd.getRemaining())) {
            cmd.error("invalid network");
            return;
        }
        ntry.sequence = seq;
        objgrp.add(ntry);
    }

    public String getPrompt() {
        return "objgrp";
    }

}
