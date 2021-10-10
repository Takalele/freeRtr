package net.freertr.cfg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.freertr.tab.tabGen;
import net.freertr.tab.tabIntMatcher;
import net.freertr.tab.tabListingEntry;
import net.freertr.tab.tabTime;
import net.freertr.user.userFilter;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;

/**
 * route map configuration
 *
 * @author matecsaba
 */
public class cfgTime implements Comparator<cfgTime>, cfgGeneric {

    /**
     * name of routemap
     */
    public String name;

    /**
     * description of access list
     */
    public String description;

    /**
     * current sequence number
     */
    public int seq;

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "time-map .*! no description",
        "time-map .*! sequence .* match year all",
        "time-map .*! sequence .* match month all",
        "time-map .*! sequence .* match day all",
        "time-map .*! sequence .* match dow all",
        "time-map .*! sequence .* match hour all",
        "time-map .*! sequence .* match minute all",
        "time-map .*! sequence .* match second all"};

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    /**
     * time list
     */
    public tabGen<tabTime> timemap;

    /**
     * create new route map
     */
    public cfgTime() {
        timemap = new tabGen<tabTime>();
        seq = nextseq();
    }

    /**
     * get next sequence
     *
     * @return seq number
     */
    public int nextseq() {
        if (timemap.size() < 1) {
            return 10;
        }
        return timemap.get(timemap.size() - 1).seq + 10;
    }

    /**
     * get current entry
     *
     * @return current entry
     */
    public synchronized tabTime getCurr() {
        tabTime ntry = new tabTime();
        ntry.seq = seq;
        ntry = timemap.find(ntry);
        if (ntry != null) {
            return ntry;
        }
        ntry = new tabTime();
        ntry.seq = seq;
        ntry.act = tabListingEntry.actionType.actPermit;
        timemap.add(ntry);
        return ntry;
    }

    public List<String> getShRun(int filter) {
        List<String> l = new ArrayList<String>();
        l.add("time-map " + name);
        cmds.cfgLine(l, description == null, cmds.tabulator, "description", "" + description);
        for (int i = 0; i < timemap.size(); i++) {
            l.addAll(timemap.get(i).dump(cmds.tabulator));
        }
        l.add(cmds.tabulator + cmds.finish);
        l.add(cmds.comment);
        if ((filter & 1) == 0) {
            return l;
        }
        return userFilter.filterText(l, defaultF);
    }

    public void getHelp(userHelping l) {
        l.add("1 2   sequence              sequence number of an entry");
        l.add("2 1,.   <num>               sequence number");
        l.add("1 3,. description           specify description");
        l.add("3 3,.   <str>               text");
        l.add("1 2,. reindex               reindex route map");
        l.add("2 3,.   [num]               initial number to start with");
        l.add("3 4,.     [num]             increment number");
        l.add("1 2   action                set action to do");
        l.add("2 .     deny                specify to forbid");
        l.add("2 .     permit              specify to allow");
        l.add("1 2   match                 match values");
        l.add("2 3     year                match year");
        l.add("3 .       <num>             value");
        l.add("3 .       all               any value");
        l.add("2 3     month               match month");
        l.add("3 .       <num>             value");
        l.add("3 .       all               any value");
        l.add("2 3     day                 match day");
        l.add("3 .       <num>             value");
        l.add("3 .       all               any value");
        l.add("2 3     dow                 match day of week");
        l.add("3 .       <num>             value");
        l.add("3 .       all               any value");
        l.add("2 3     hour                match hour");
        l.add("3 .       <num>             value");
        l.add("3 .       all               any value");
        l.add("2 3     minute              match minute");
        l.add("3 .       <num>             value");
        l.add("3 .       all               any value");
        l.add("2 3     second              match second");
        l.add("3 .       <num>             value");
        l.add("3 .       all               any value");
    }

    public synchronized void doCfgStr(cmds cmd) {
        String a = cmd.word();
        if (a.equals("description")) {
            description = cmd.getRemaining();
            return;
        }
        if (a.equals("sequence")) {
            seq = bits.str2num(cmd.word());
            a = cmd.word();
            if (a.length() < 1) {
                return;
            }
        }
        if (a.equals("action")) {
            tabTime ntry = getCurr();
            ntry.act = tabListingEntry.string2action(cmd.word());
            return;
        }
        if (a.equals("match")) {
            a = cmd.word();
            tabTime ntry = getCurr();
            if (a.equals("year")) {
                ntry.year.fromString(cmd.getRemaining());
                return;
            }
            if (a.equals("month")) {
                ntry.month.fromString(cmd.getRemaining());
                return;
            }
            if (a.equals("day")) {
                ntry.day.fromString(cmd.getRemaining());
                return;
            }
            if (a.equals("dow")) {
                ntry.dow.fromString(cmd.getRemaining());
                return;
            }
            if (a.equals("hour")) {
                ntry.hour.fromString(cmd.getRemaining());
                return;
            }
            if (a.equals("minute")) {
                ntry.min.fromString(cmd.getRemaining());
                return;
            }
            if (a.equals("second")) {
                ntry.sec.fromString(cmd.getRemaining());
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("reindex")) {
            int o = bits.str2num(cmd.word());
            int p = bits.str2num(cmd.word());
            if (o < 1) {
                o = 10;
            }
            if (p < 1) {
                p = 10;
            }
            for (int i = 0; i < timemap.size(); i++) {
                tabTime t = timemap.get(i);
                t.seq = (p * i) + o;
            }
            return;
        }
        if (!a.equals("no")) {
            cmd.badCmd();
            return;
        }
        a = cmd.word();
        if (a.equals("description")) {
            description = null;
            return;
        }
        if (a.equals("match")) {
            a = cmd.word();
            tabTime ntry = getCurr();
            if (a.equals("year")) {
                ntry.year = new tabIntMatcher();
                return;
            }
            if (a.equals("month")) {
                ntry.month = new tabIntMatcher();
                return;
            }
            if (a.equals("day")) {
                ntry.day = new tabIntMatcher();
                return;
            }
            if (a.equals("dow")) {
                ntry.dow = new tabIntMatcher();
                return;
            }
            if (a.equals("hour")) {
                ntry.hour = new tabIntMatcher();
                return;
            }
            if (a.equals("minute")) {
                ntry.min = new tabIntMatcher();
                return;
            }
            if (a.equals("second")) {
                ntry.sec = new tabIntMatcher();
                return;
            }
            cmd.badCmd();
            return;
        }
        if (a.equals("sequence")) {
            tabTime ntry = new tabTime();
            ntry.seq = bits.str2num(cmd.word());
            if (timemap.del(ntry) == null) {
                cmd.error("invalid sequence");
                return;
            }
            return;
        }
        cmd.badCmd();
    }

    public int compare(cfgTime o1, cfgTime o2) {
        return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
    }

    public String getPrompt() {
        return "time";
    }

    public String toString() {
        return name;
    }

    /**
     * check if time matches
     *
     * @param tim time to check
     * @return false if matches, true if not
     */
    public boolean matches(long tim) {
        for (int i = 0; i < timemap.size(); i++) {
            tabTime ntry = timemap.get(i);
            if (!ntry.matches(cfgAll.timeZoneName, tim)) {
                continue;
            }
            return ntry.act != tabListingEntry.actionType.actPermit;
        }
        return true;
    }

}
