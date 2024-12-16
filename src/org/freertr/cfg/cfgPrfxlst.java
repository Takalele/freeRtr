package org.freertr.cfg;

import java.util.ArrayList;
import java.util.List;
import org.freertr.addr.addrIP;
import org.freertr.tab.tabGen;
import org.freertr.tab.tabListing;
import org.freertr.tab.tabListingEntry;
import org.freertr.tab.tabPrfxlstN;
import org.freertr.user.userFilter;
import org.freertr.user.userHelping;
import org.freertr.util.bits;
import org.freertr.util.cmds;

/**
 * prefix list configuration
 *
 * @author matecsaba
 */
public class cfgPrfxlst implements Comparable<cfgPrfxlst>, cfgGeneric {

    /**
     * name of prefixlist
     */
    public String name;

    /**
     * description of access list
     */
    public String description;

    /**
     * list of prefixes
     */
    public tabListing<tabPrfxlstN, addrIP> prflst;

    /**
     * create new prefix list
     *
     * @param s name
     */
    public cfgPrfxlst(String s) {
        prflst = new tabListing<tabPrfxlstN, addrIP>();
        name = s;
        prflst.listName = s;
    }

    /**
     * defaults text
     */
    public final static String[] defaultL = {};

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public List<String> getShRun(int filter) {
        List<String> l = new ArrayList<String>();
        l.add("prefix-list " + name);
        if (description != null) {
            l.add(cmds.tabulator + "description " + description);
        }
        l.addAll(prflst.dump(cmds.tabulator, filter));
        l.add(cmds.tabulator + cmds.finish);
        l.add(cmds.comment);
        if ((filter & 1) == 0) {
            return l;
        }
        return userFilter.filterText(l, defaultF);
    }

    public void getHelp(userHelping l) {
        l.add(null, "1 2   sequence              sequence number of an entry");
        l.add(null, "2 1     <num>               sequence number");
        l.add(null, "1 3,. description           specify description");
        l.add(null, "3 3,.   <str>               text");
        l.add(null, "1 2   rename                rename this prefix list");
        l.add(null, "2 .     <str>               set new name");
        l.add(null, "1 3   evaluate              evaluate another list");
        l.add(null, "3 4     permit              specify list to allow");
        l.add(null, "3 4     deny                specify list to forbid");
        l.add(null, "4 .       <name:pl>         name of list");
        l.add(null, "1 3   permit                specify networks to allow");
        l.add(null, "1 3   deny                  specify networks to forbid");
        l.add(null, "3 4,.   <net/mask>          network in perfix/mask format");
        l.add(null, "4 5       ge                minimum prefix length to be matched");
        l.add(null, "5 4,.       <num>           minimum prefix length");
        l.add(null, "4 5       le                maximum prefix length to be matched");
        l.add(null, "5 4,.       <num>           maximum prefix length");
        l.add(null, "4 4,.     log               set logging on match");
        l.add(null, "1 2,. reindex               reindex prefix list");
        l.add(null, "2 3,.   [num]               initial number to start with");
        l.add(null, "3 .       [num]             increment number");
    }

    public synchronized void doCfgStr(cmds cmd) {
        String a = cmd.word();
        if (a.equals(cmds.negated)) {
            a = cmd.word();
            if (a.equals("description")) {
                description = null;
                return;
            }
            if (a.equals("sequence")) {
                tabPrfxlstN ntry = new tabPrfxlstN();
                ntry.sequence = bits.str2num(cmd.word());
                if (prflst.del(ntry)) {
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
            cfgPrfxlst v = cfgAll.prfxFind(a, false);
            if (v != null) {
                cmd.error("already exists");
                return;
            }
            name = a;
            prflst.listName = a;
            return;
        }
        if (a.equals("reindex")) {
            int i = bits.str2num(cmd.word());
            prflst.reindex(i, bits.str2num(cmd.word()));
            return;
        }
        int seq = prflst.nextseq();
        if (a.equals("sequence")) {
            seq = bits.str2num(cmd.word());
            a = cmd.word();
        }
        tabPrfxlstN ntry = new tabPrfxlstN();
        ntry.sequence = seq;
        if (a.equals("evaluate")) {
            ntry.action = tabListingEntry.string2action(cmd.word());
            cfgPrfxlst res = cfgAll.prfxFind(cmd.word(), false);
            if (res == null) {
                cmd.error("no such list");
                return;
            }
            ntry.evaluate = res.prflst;
            prflst.add(ntry);
            return;
        }
        ntry.action = tabListingEntry.string2action(a);
        if (ntry.fromString(cmd.getRemaining())) {
            cmd.error("invalid network");
            return;
        }
        prflst.add(ntry);
    }

    public int compareTo(cfgPrfxlst o) {
        return name.toLowerCase().compareTo(o.name.toLowerCase());
    }

    public String getPrompt() {
        return "prfx";
    }

    public String toString() {
        return name;
    }

}
