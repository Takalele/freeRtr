package net.freertr.cfg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.freertr.tab.tabGen;
import net.freertr.tab.tabSession;
import net.freertr.user.userFilter;
import net.freertr.user.userHelping;
import net.freertr.util.cmds;

/**
 * stateful session configuration
 *
 * @author matecsaba
 */
public class cfgSessn implements Comparator<cfgSessn>, cfgGeneric {

    /**
     * name of session
     */
    public String name;

    /**
     * sessions
     */
    public tabSession connects = new tabSession(true, 60000);

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "session .*! no timeout",
        "session .*! no mac",
        "session .*! no before",
        "session .*! no after",
        "session .*! no dropped",
        "session .*! no allow-routing",
        "session .*! no allow-linklocal",
        "session .*! no allow-multicast",
        "session .*! no allow-broadcast",
        "session .*! no allow-list",
        "session .*! no drop-rx",
        "session .*! no drop-tx",
        "session .*! no drop-frg",
        "session .*! no member"
    };

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public int compare(cfgSessn o1, cfgSessn o2) {
        return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
    }

    /**
     * create new certificate
     *
     * @param nam name of interface
     */
    public cfgSessn(String nam) {
        name = nam.trim();
    }

    public String toString() {
        return name;
    }

    public void getHelp(userHelping l) {
        l.add("1 2  timeout                      set timeout");
        l.add("2 .    <num>                      timeout in ms");
        l.add("1 .  before                       log on session start");
        l.add("1 .  after                        log on session stop");
        l.add("1 .  dropped                      log on session stop");
    }

    public String getPrompt() {
        return "session";
    }

    public void doCfgStr(cmds cmd) {
        connects.fromString(cmd);
    }

    public List<String> getShRun(int filter) {
        List<String> lst = new ArrayList<String>();
        lst.add("session " + name);
        connects.getConfig(lst, cmds.tabulator);
        lst.add(cmds.tabulator + cmds.finish);
        lst.add(cmds.comment);
        if ((filter & 1) == 0) {
            return lst;
        }
        return userFilter.filterText(lst, defaultF);
    }

}
