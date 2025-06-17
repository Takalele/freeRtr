package org.freertr.cfg;

import java.util.ArrayList;
import java.util.List;
import org.freertr.auth.authConstant;
import org.freertr.auth.authGeneric;
import org.freertr.auth.authList;
import org.freertr.auth.authLocal;
import org.freertr.auth.authRadius;
import org.freertr.auth.authTacacs;
import org.freertr.tab.tabGen;
import org.freertr.user.userFilter;
import org.freertr.user.userHelping;
import org.freertr.util.cmds;

/**
 * one user list configuration
 *
 * @author matecsaba
 */
public class cfgAuther implements Comparable<cfgAuther>, cfgGeneric {

    /**
     * name of this user list
     */
    public String name;

    /**
     * description of this dialpeer
     */
    public String description = null;

    /**
     * method type
     */
    public enum methodType {

        /**
         * local userlist
         */
        local,
        /**
         * radius
         */
        radius,
        /**
         * tacacs
         */
        tacacs,
        /**
         * list of methods
         */
        listing,
        /**
         * never succeed
         */
        never,
        /**
         * always succeed
         */
        always

    }

    private authLocal loc;

    private authRadius rad;

    private authTacacs tac;

    private authList lst;

    private authConstant cnst;

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "aaa userlist .*!" + cmds.tabulator + "username .* autocommand ",
        "aaa userlist .*!" + cmds.tabulator + "username .* privilege 15",
        "aaa radius .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "secret",
        "aaa radius .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "proxy",
        "aaa radius .*!" + cmds.tabulator + "privilege 15",
        "aaa radius .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "port",
        "aaa tacacs .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "secret",
        "aaa tacacs .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "proxy",
        "aaa tacacs .*!" + cmds.tabulator + "privilege 15",
        "aaa tacacs .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "port",
        "aaa .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "description",
        "aaa .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "log-error",
        "aaa .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "log-failure",
        "aaa .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "log-success",
        "aaa .*!" + cmds.tabulator + cmds.negated + cmds.tabulator + "log-password"
    };

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public int compareTo(cfgAuther o) {
        return name.toLowerCase().compareTo(o.name.toLowerCase());
    }

    public String toString() {
        return "authenticator " + name;
    }

    /**
     * get authenticator
     *
     * @return authenticator to use
     */
    public authGeneric getAuther() {
        if (loc != null) {
            return loc;
        }
        if (rad != null) {
            return rad;
        }
        if (tac != null) {
            return tac;
        }
        if (lst != null) {
            return lst;
        }
        if (cnst != null) {
            return cnst;
        }
        return null;
    }

    /**
     * create new vrf
     *
     * @param nam name of vrf
     * @param typ type
     */
    public cfgAuther(String nam, methodType typ) {
        name = nam.trim();
        if (typ == null) {
            return;
        }
        switch (typ) {
            case local:
                loc = new authLocal();
                break;
            case radius:
                rad = new authRadius();
                break;
            case tacacs:
                tac = new authTacacs();
                break;
            case listing:
                lst = new authList();
                break;
            case never:
                cnst = new authConstant(false);
                break;
            case always:
                cnst = new authConstant(true);
                break;
        }
        authGeneric aut = getAuther();
        if (aut != null) {
            aut.autName = name;
        }
    }

    /**
     * convert string to type
     *
     * @param s string
     * @return type
     */
    public static methodType string2auther(String s) {
        if (s.equals("userlist")) {
            return methodType.local;
        }
        if (s.equals("radius")) {
            return methodType.radius;
        }
        if (s.equals("tacacs")) {
            return methodType.tacacs;
        }
        if (s.equals("list")) {
            return methodType.listing;
        }
        if (s.equals("never")) {
            return methodType.never;
        }
        if (s.equals("always")) {
            return methodType.always;
        }
        return null;
    }

    public List<String> getShRun(int filter) {
        List<String> l = new ArrayList<String>();
        authGeneric aut = getAuther();
        if (aut == null) {
            return l;
        }
        l.add("aaa " + aut.getCfgName() + " " + name);
        cmds.cfgLine(l, description == null, cmds.tabulator, "description", description);
        cmds.cfgLine(l, !aut.logFail, cmds.tabulator, "log-failure", "");
        cmds.cfgLine(l, !aut.logErr, cmds.tabulator, "log-error", "");
        cmds.cfgLine(l, !aut.logOk, cmds.tabulator, "log-success", "");
        cmds.cfgLine(l, !aut.logPass, cmds.tabulator, "log-password", "");
        l.addAll(aut.getShRun(cmds.tabulator, filter));
        l.add(cmds.tabulator + cmds.finish);
        l.add(cmds.comment);
        if ((filter & 1) == 0) {
            return l;
        }
        return userFilter.filterText(l, defaultF);
    }

    public void getHelp(userHelping l) {
        l.add(null, false, 1, new int[]{2}, "rename", "rename this authenticator");
        l.add(null, false, 2, new int[]{-1}, "<str>", "set new name");
        l.add(null, false, 1, new int[]{2}, "description", "specify description");
        l.add(null, false, 2, new int[]{2, -1}, "<str>", "description");
        l.add(null, false, 1, new int[]{-1}, "log-failure", "log failure");
        l.add(null, false, 1, new int[]{-1}, "log-success", "log success");
        l.add(null, false, 1, new int[]{-1}, "log-error", "log error");
        l.add(null, false, 1, new int[]{-1}, "log-password", "log password");
        authGeneric aut = getAuther();
        if (aut != null) {
            aut.getHelp(l);
        }
    }

    public synchronized void doCfgStr(cmds cmd) {
        boolean b = true;
        authGeneric aut = getAuther();
        cmds c = cmd.copyBytes(false);
        String a = c.word();
        boolean neg = a.equals(cmds.negated);
        if (neg) {
            a = c.word();
        }
        if (aut == null) {
            cmd.badCmd();
            return;
        }
        if (a.equals("rename")) {
            a = c.word();
            cfgAuther v = cfgAll.autherFind(a, null);
            if (v != null) {
                cmd.error("already exists");
                return;
            }
            name = a;
            aut.autName = a;
            return;
        }
        if (a.equals("description")) {
            description = c.getRemaining();
            if (neg) {
                description = null;
            }
            return;
        }
        if (a.equals("log-failure")) {
            aut.logFail = !neg;
            return;
        }
        if (a.equals("log-error")) {
            aut.logErr = !neg;
            return;
        }
        if (a.equals("log-success")) {
            aut.logOk = !neg;
            return;
        }
        if (a.equals("log-password")) {
            aut.logPass = !neg;
            return;
        }
        b = aut.fromString(cmd);
        if (b) {
            cmd.badCmd();
        }
    }

    public String getPrompt() {
        return "auther";
    }

}
