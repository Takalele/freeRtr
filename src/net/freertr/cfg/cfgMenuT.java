package net.freertr.cfg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.freertr.pipe.pipeSetting;
import net.freertr.pipe.pipeSide;
import net.freertr.tab.tabGen;
import net.freertr.user.userExec;
import net.freertr.user.userFilter;
import net.freertr.user.userHelping;
import net.freertr.user.userReader;
import net.freertr.user.userScreen;
import net.freertr.util.cmds;
import net.freertr.util.logger;
import net.freertr.util.version;

/**
 * one tui based menu configuration
 *
 * @author matecsaba
 */
public class cfgMenuT implements Comparator<cfgMenuT>, cfgGeneric {

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "menu tui .*! no description",};

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    /**
     * name of menu
     */
    public String name;

    /**
     * description
     */
    public String description;

    /**
     * groups of menu
     */
    public List<cfgMenuTgroup> group = new ArrayList<cfgMenuTgroup>();

    /**
     * create new instance
     *
     * @param s name of menu
     */
    public cfgMenuT(String s) {
        name = s;
    }

    public int compare(cfgMenuT o1, cfgMenuT o2) {
        return o1.name.compareTo(o2.name);
    }

    public void getHelp(userHelping l) {
        l.add(null, "1 3,. description                   specify description");
        l.add(null, "3 3,.   <str>                       text");
        l.add(null, "1 2   rename                        rename this menu");
        l.add(null, "2 .     <str>                       set new name");
        l.add(null, "1 2   entry                         add an entry");
        l.add(null, "2 3     <str>                       group name");
        l.add(null, "3 4       <str>                     entry name");
        l.add(null, "4 4,.       <str>                   command to execute");
    }

    public List<String> getShRun(int filter) {
        List<String> l = new ArrayList<String>();
        l.add("menu tui " + name);
        cmds.cfgLine(l, description == null, cmds.tabulator, "description", description);
        for (int o = 0; o < group.size(); o++) {
            cfgMenuTgroup grp = group.get(o);
            for (int i = 0; i < grp.entry.size(); i++) {
                cfgMenuTentry ent = grp.entry.get(i);
                l.add(cmds.tabulator + "entry " + grp.name + " " + ent.name + " " + ent.exec);
            }
        }
        l.add(cmds.tabulator + cmds.finish);
        l.add(cmds.comment);
        if ((filter & 1) == 0) {
            return l;
        }
        return userFilter.filterText(l, defaultF);
    }

    public void doCfgStr(cmds cmd) {
        String a = cmd.word();
        boolean negated = a.equals("no");
        if (negated) {
            a = cmd.word();
        }
        if (a.equals("description")) {
            if (negated) {
                description = null;
            } else {
                description = cmd.getRemaining();
            }
            return;
        }
        if (a.equals("rename")) {
            a = cmd.word();
            cfgMenuT v = cfgAll.menuTfind(a, false);
            if (v != null) {
                cmd.error("already exists");
                return;
            }
            name = a;
            return;
        }
        if (!a.equals("entry")) {
            cmd.badCmd();
            return;
        }
        cfgMenuTgroup grp = new cfgMenuTgroup(cmd.word());
        a = cmd.word();
        cfgMenuTentry ent = new cfgMenuTentry(a, cmd.getRemaining());
        if (!negated) {
            int i = findGrp(grp.name);
            if (i >= 0) {
                grp = group.get(i);
            } else {
                group.add(grp);
            }
            grp.entry.add(ent);
            return;
        }
        int o = findGrp(grp.name);
        if (o < 0) {
            return;
        }
        grp = group.get(o);
        int i = grp.findEnt(ent.name);
        if (i < 0) {
            return;
        }
        grp.entry.remove(i);
        if (grp.entry.size() > 0) {
            return;
        }
        group.remove(o);
    }

    public String getPrompt() {
        return "menut";
    }

    private int findGrp(String n) {
        for (int i = 0; i < group.size(); i++) {
            if (n.equals(group.get(i).name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * do menu
     *
     * @param pipe pipe to use
     * @param rdr reader
     * @param prv privileged
     */
    public void doMenu(pipeSide pipe, userReader rdr, boolean prv) {
        cfgMenuTdoer d = new cfgMenuTdoer(this, new userScreen(pipe), rdr, prv);
        d.doWork();
    }

}

class cfgMenuTgroup {

    public final String name;

    public final List<cfgMenuTentry> entry = new ArrayList<cfgMenuTentry>();

    public cfgMenuTgroup(String n) {
        name = n;
    }

    public int findEnt(String n) {
        for (int i = 0; i < entry.size(); i++) {
            if (n.equals(entry.get(i).name)) {
                return i;
            }
        }
        return -1;
    }

}

class cfgMenuTentry {

    public final String name;

    public final String exec;

    public String group;

    public boolean mark;

    public cfgMenuTentry(String n, String e) {
        name = n;
        exec = e;
    }

}

class cfgMenuTdoer {

    private final cfgMenuT lower;

    private final userScreen console;

    private final userReader reader;

    private final boolean privileged;

    private int beg;

    private int cur;

    private String flt;

    private List<cfgMenuTentry> buf;

    private int max;

    public cfgMenuTdoer(cfgMenuT prn, userScreen pip, userReader rdr, boolean prv) {
        lower = prn;
        console = pip;
        reader = rdr;
        privileged = prv;
    }

    public void doWork() {
        doReset();
        doFilter();
        for (;;) {
            doRange();
            doDraw(false);
            if (doKey()) {
                break;
            }
        }
        doClear();
    }

    private boolean doKey() {
        int i = userScreen.getKey(console.pipe);
        switch (i) {
            case -1: // end
                return true;
            case 0x0020: // space
                doKeySp();
                return false;
            case 0x0261: // ctrl+a
                doKeyUp();
                return false;
            case 0x027a: // ctrl+z
                doKeyDn();
                return false;
            case 0x0268: // ctrl+h
                doKeyBs();
                return false;
            case 0x0270: // ctrl+p
                doKeyPgUp();
                return false;
            case 0x026e: // ctrl+n
                doKeyPgDn();
                return false;
            case 0x0272: // ctrl+r
                doDraw(true);
                return false;
            case 0x026c: // ctrl+l
                doDraw(true);
                return false;
            case 0x0273: // ctrl+s
                doKeyF1();
                return false;
            case 0x0271: // ctrl+q
                return true;
            case 0x0278: // ctrl+x
                return true;
            case 0x0263: // ctrl+c
                return true;
            case 0x0276: // ctrl+v
                doKeyF3();
                return false;
            case 0x0277: // ctrl+w
                doKeyClr();
                return false;
            case 0x8003: // backspace
                doKeyBs();
                return false;
            case 0x8004: // enter
                doKeyEnter();
                return false;
            case 0x8008: // home
                doKeyHom();
                return false;
            case 0x8009: // end
                doKeyEnd();
                return false;
            case 0x800a: // pgup
                doKeyPgUp();
                return false;
            case 0x800b: // pgdn
                doKeyPgDn();
                return false;
            case 0x800c: // up
                doKeyUp();
                return false;
            case 0x800d: // down
                doKeyDn();
                return false;
            case 0x8014: // f1
                doKeyF1();
                return false;
            case 0x8016: // f3
                doKeyF3();
                return false;
            case 0x801d: // f10
                return true;
            default:
                doKeyChr(i);
                return false;
        }
    }

    private void doKeyEnter() {
        if (cur >= buf.size()) {
            return;
        }
        doClear();
        int o = 0;
        for (int i = 0; i < buf.size(); i++) {
            cfgMenuTentry ent = buf.get(i);
            if (!ent.mark) {
                continue;
            }
            ent.mark = false;
            doExecOne(ent);
            o++;
        }
        if (o < 1) {
            doExecOne(buf.get(cur));
        }
        doClear();
        doDraw(true);
    }

    private void doExecOne(cfgMenuTentry ent) {
        userScreen.sendCol(console.pipe, userScreen.colBrGreen);
        console.pipe.linePut(ent.group + " - " + ent.name);
        userScreen.sendCol(console.pipe, userScreen.colWhite);
        userExec exe = new userExec(console.pipe, reader);
        exe.privileged = privileged;
        String s = exe.repairCommand(ent.exec);
        if (console.pipe.settingsGet(pipeSetting.logging, false)) {
            logger.info("command menu:" + s + " from " + console.pipe.settingsGet(pipeSetting.origin, "?"));
        }
        exe.executeCommand(s);
    }

    private void doKeyF1() {
        List<String> l = new ArrayList<String>();
        l.add("f1 - help");
        l.add("f3 - view command");
        l.add("f10 - exit");
        l.add("space  - select");
        l.add("ctrl+s - help");
        l.add("ctrl+v - view command");
        l.add("ctrl+a - move up");
        l.add("ctrl+z - move down");
        l.add("ctrl+w - erase filter");
        l.add("ctrl+p - move page up");
        l.add("ctrl+n - move page down");
        l.add("ctrl+r - redraw screen");
        l.add("ctrl+l - redraw screen");
        l.add("ctrl+q - exit");
        l.add("ctrl+x - exit");
        l.add("ctrl+c - exit");
        console.helpWin(userScreen.colBlue, userScreen.colWhite, userScreen.colBrWhite, -1, -1, -1, -1, l);
    }

    private void doKeyF3() {
        if (!privileged) {
            return;
        }
        if (cur > buf.size()) {
            return;
        }
        cfgMenuTentry ent = buf.get(cur);
        console.askUser("command to execute", userScreen.colBlue, userScreen.colWhite, userScreen.colBrYellow, userScreen.colBrWhite, -1, -1, -1, ent.exec);
    }

    private void doKeyUp() {
        cur--;
    }

    private void doKeyDn() {
        cur++;
    }

    private void doKeyHom() {
        cur = 0;
    }

    private void doKeyEnd() {
        cur = buf.size();
    }

    private void doKeyPgDn() {
        cur += console.sizY / 4;
    }

    private void doKeyPgUp() {
        cur -= console.sizY / 4;
    }

    private void doKeySp() {
        if (cur >= buf.size()) {
            return;
        }
        cfgMenuTentry ent = buf.get(cur);
        ent.mark = !ent.mark;
        cur++;
    }

    private void doKeyBs() {
        int i = flt.length();
        if (i < 1) {
            return;
        }
        flt = flt.substring(0, i - 1);
        doFilter();
    }

    private void doKeyClr() {
        flt = "";
        doFilter();
    }

    private void doKeyChr(int k) {
        if (k < 0x20) {
            return;
        }
        if (k > 0x7f) {
            return;
        }
        flt += (char) k;
        doFilter();
    }

    private void doReset() {
        beg = 0;
        cur = 0;
        flt = "";
    }

    private void doClear() {
        console.putCls();
        console.putCur(0, 0);
        console.refresh();
    }

    private void doFilter() {
        buf = new ArrayList<cfgMenuTentry>();
        max = 0;
        for (int o = 0; o < lower.group.size(); o++) {
            cfgMenuTgroup grp = lower.group.get(o);
            int i = grp.name.length();
            if (max < i) {
                max = i;
            }
            boolean all = flt.length() < 1;
            if (!all) {
                all = grp.name.indexOf(flt) >= 0;
            }
            for (i = 0; i < grp.entry.size(); i++) {
                cfgMenuTentry ent = grp.entry.get(i);
                boolean ned = all;
                if (!ned) {
                    ned = ent.name.indexOf(flt) >= 0;
                }
                if (!ned) {
                    continue;
                }
                ent = new cfgMenuTentry(ent.name, ent.exec);
                ent.group = grp.name;
                buf.add(ent);
            }
        }
    }

    private void doRange() {
        int bs = buf.size();
        if (cur >= bs) {
            cur = bs - 1;
        }
        if (cur < 0) {
            cur = 0;
        }
        int i = cur - console.sizY + 3;
        if (beg < i) {
            beg = i;
        }
        i = bs - console.sizY + 2;
        if (beg > i) {
            beg = i;
        }
        if (beg > cur) {
            beg = cur;
        }
        if (beg < 0) {
            beg = 0;
        }
        if (beg > bs) {
            beg = bs;
        }
    }

    private void doDraw(boolean clr) {
        if (clr) {
            console.putCls();
            console.refresh();
            for (int i = 0; i < console.sizY; i++) {
                putFill(i, userScreen.colWhite, userScreen.colBlack, 32);
            }
            console.refresh();
        }
        putHeader();
        putFooter();
        for (int i = 0; i < console.sizY - 2; i++) {
            putLine(i);
        }
        console.putCur(8 + flt.length(), console.sizY - 1);
        console.refresh();
    }

    private void putHeader() {
        putFill(0, userScreen.colGreen, userScreen.colWhite, 32);
        console.putStr(0, 0, userScreen.colGreen, userScreen.colBrYellow, false, version.namVer);
    }

    private void putFooter() {
        putFill(console.sizY - 1, userScreen.colBlue, userScreen.colWhite, 32);
        console.putStr(0, console.sizY - 1, userScreen.colBlue, userScreen.colBrWhite, false, (cur + 1) + "/" + buf.size());
        console.putStr(8, console.sizY - 1, userScreen.colBlue, userScreen.colBrWhite, false, flt);
    }

    private void putLine(int ln) {
        int lin = ln + beg;
        int bg;
        int fg;
        if (lin == cur) {
            bg = userScreen.colMagenta;
            fg = userScreen.colBrYellow;
        } else {
            bg = userScreen.colBlack;
            fg = userScreen.colWhite;
        }
        putFill(ln + 1, bg, fg, 32);
        if (lin < 0) {
            return;
        }
        if (lin >= buf.size()) {
            return;
        }
        cfgMenuTentry ent = buf.get(lin);
        console.putStr(0, ln + 1, bg, fg, false, ent.group);
        console.putStr(max + 3, ln + 1, bg, fg, false, ent.name);
        if (!ent.mark) {
            return;
        }
        console.putStr(max + 1, ln + 1, bg, fg, false, "*");
    }

    private void putFill(int ln, int bg, int fg, int ch) {
        for (int i = 0; i < console.sizX; i++) {
            console.putInt(i, ln, bg, fg, false, ch);
        }
    }

}
