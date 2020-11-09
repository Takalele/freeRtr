package user;

import java.util.ArrayList;
import java.util.List;
import util.bits;
import util.cmds;
import util.verCore;

/**
 * help system
 *
 * @author matecsaba
 */
public class userHelping {

    private final static int maxVal = 0x100000;

    /**
     * lines of data
     */
    public List<userHelpingData> lines = new ArrayList<userHelpingData>();

    /**
     * get generic config help
     *
     * @param l help text
     */
    protected static void getCfgGen(userHelping l) {
        l.add("1 .    exit                go back to previous mode");
        l.add("1 2,.  end                 close this config session");
        l.add("2 2,.    <cmd>             parameters");
        l.add("1 2    do                  execute one exec command");
        l.add("2 2,.    <cmd>             exec command");
        l.add("1 1    no                  negate a command");
    }

    /**
     * get generic config help
     *
     * @param l help text
     */
    protected static void getCfgHelp(userHelping l) {
        l.add("1 2    show                running system information");
        userExec.getHelpShow(l, true);
        userExec.getHelpPipes(l, 110, true);
    }

    /**
     * collect lines starts with
     *
     * @param l list to read
     * @param s starting string
     * @return list of lines starting with s
     */
    public static List<String> startsWith(List<String> l, String s) {
        List<String> r = new ArrayList<String>();
        for (int i = 0; i < l.size(); i++) {
            String a = l.get(i);
            if (a.startsWith(s)) {
                r.add(a);
            }
        }
        return r;
    }

    /**
     * get size of menu lines
     *
     * @return number of lines
     */
    public int size() {
        return lines.size();
    }

    /**
     * get one menu line
     *
     * @param i sequence to get
     * @return value of line
     */
    public userHelpingData get(int i) {
        if (i < 0) {
            return null;
        }
        if (i >= lines.size()) {
            return null;
        }
        return lines.get(i);
    }

    /**
     * add one menu line
     *
     * @param s string to add
     */
    public void add(String s) {
        userHelpingData d = new userHelpingData();
        d.set(s);
        lines.add(d);
    }

    /**
     * get lines from an other help
     *
     * @param src source
     */
    public void addOther(userHelping src) {
        lines.addAll(src.lines);
    }

    /**
     * delete one menu line
     *
     * @param s string to remove
     */
    public void del(String s) {
        userHelpingData d = new userHelpingData();
        d.set(s);
        for (int i = lines.size() - 1; i >= 0; i--) {
            userHelpingData r = lines.get(i);
            if (r.level != d.level) {
                continue;
            }
            if (!r.command.equals(d.command)) {
                continue;
            }
            r.level = 666;
        }
    }

    /**
     * collect menu lines in one direction
     *
     * @param lin starting line number
     * @param skip skip current level
     * @param dir direction, + or - 1
     * @param levMin minimum level to scan
     * @param levMax maximum level to scan
     * @param levReq required level to collect
     * @return line numbers meets requirements
     */
    private userHelpingList collectDirection(int lin, boolean skip, int dir, int levMin, int levMax, int levReq) {
        userHelpingList d = new userHelpingList();
        if (skip) {
            int cur = lines.get(lin).level;
            for (;; lin += dir) {
                if (lin >= lines.size()) {
                    return d;
                }
                if (lin < 0) {
                    return d;
                }
                if (lines.get(lin).level != cur) {
                    break;
                }
            }
        }
        for (;; lin += dir) {
            if (lin >= lines.size()) {
                return d;
            }
            if (lin < 0) {
                return d;
            }
            int lv = lines.get(lin).level;
            if (lv == levReq) {
                d.add(lin);
                continue;
            }
            if (lv < levMin) {
                return d;
            }
            if (lv > levMax) {
                return d;
            }
        }
    }

    /**
     * find possible next words for a line
     *
     * @param lin line number to process
     * @return list of possible next lines
     */
    private userHelpingList nextWords(int lin) {
        userHelpingList d = new userHelpingList();
        if (lin < 0) {
            d = collectDirection(0, false, 1, -maxVal, maxVal, 1);
            return d;
        }
        userHelpingList curNxt = lines.get(lin).after;
        int curLvl = lines.get(lin).level;
        for (int i = 0; i < curNxt.num(); i++) {
            int req = curNxt.val(i);
            if (req < 0) {
                d.add(req);
                continue;
            }
            if (req > 100) {
                d.addMore(collectDirection(lin, false, 1, 0, maxVal, req));
                continue;
            }
            if (req > curLvl) {
                d.addMore(collectDirection(lin, true, 1, curLvl + 1, maxVal, req));
                continue;
            }
            d.addMore(collectDirection(lin, false, -1, req, maxVal, req));
            d.addMore(collectDirection(lin + 1, false, 1, req, maxVal, req));
        }
        return d;
    }

    /**
     * match one string to given lines
     *
     * @param lns list of lines
     * @param s string to match
     * @return match probability for lines level set to best one or a negative
     * error code of the following: -2 more best matches, have to type more
     * chars -3 string does not match to any of lines -4 generic error
     */
    private userHelpingList matchStr(userHelpingList lns, String s) {
        byte[] b1 = s.trim().toLowerCase().getBytes();
        userHelpingList d = new userHelpingList();
        for (int o = 0; o < lns.num(); o++) {
            int i = lns.val(o);
            if (i < 0) {
                d.add(0);
                continue;
            }
            userHelpingData dat = lines.get(i);
            if (dat.variable) {
                d.add(-1);
                continue;
            }
            byte[] b2 = dat.command.trim().toLowerCase().getBytes();
            for (i = 0;; i++) {
                if (i >= b1.length) {
                    break;
                }
                if (i >= b2.length) {
                    break;
                }
                if (b1[i] != b2[i]) {
                    break;
                }
            }
            d.add(i);
        }
        int num = -4;
        int max = -1;
        int vld = -1;
        for (int i = 0; i < d.num(); i++) {
            int cur = d.val(i);
            if (cur < 0) {
                vld = i;
                continue;
            }
            if (cur < max) {
                continue;
            }
            if (cur == max) {
                num = -2;
                continue;
            }
            max = cur;
            num = i;
        }
        if (max != b1.length) {
            num = -3;
        }
        if ((num < 0) && (vld >= 0)) {
            num = vld;
        }
        d.level = num;
        return d;
    }

    /**
     * find longest matching string
     *
     * @param lns list of lines
     * @param s string to match
     * @return string
     */
    public String matchLong(userHelpingList lns, String s) {
        s = s.trim().toLowerCase();
        String m = null;
        for (int o = 0; o < lns.num(); o++) {
            int i = lns.val(o);
            if (i < 0) {
                continue;
            }
            userHelpingData dat = lines.get(i);
            if (dat.variable) {
                continue;
            }
            if (!dat.command.trim().toLowerCase().startsWith(s)) {
                continue;
            }
            if (m == null) {
                m = dat.command.trim().toLowerCase();
                continue;
            }
            String a = dat.command.trim().toLowerCase();
            for (i = 0;; i++) {
                if (i >= m.length()) {
                    break;
                }
                if (i >= a.length()) {
                    break;
                }
                if (a.substring(i, i + 1).equals(m.substring(i, i + 1))) {
                    continue;
                }
                m = a.substring(0, i);
                break;
            }
        }
        return m;
    }

    /**
     * create a list of lines to a command string
     *
     * @param s command string to process
     * @return line numbers over wich i get to the command level set to a last
     * one, or error from matchStr
     */
    private userHelpingList whereAm(String s) {
        s = s.trim();
        userHelpingList res = new userHelpingList();
        res.level = -1;
        String b = "";
        for (; s.length() > 0;) {
            userHelpingList lns = nextWords(res.level);
            int i = s.indexOf(" ");
            String a;
            if (i < 0) {
                a = s;
                s = "";
            } else {
                a = s.substring(0, i).trim();
                s = s.substring(i, s.length()).trim();
            }
            userHelpingList sel = matchStr(lns, a);
            if (sel.level < 0) {
                res.level = sel.level;
                if (s.length() > 0) {
                    return res;
                }
                s = matchLong(lns, a);
                if (s == null) {
                    return res;
                }
                res.str = b + s;
                return res;
            }
            i = lns.val(sel.level);
            res.add(i);
            if (lines.get(i).variable) {
                b += a + " ";
            } else {
                b += lines.get(i).command + " ";
            }
            res.level = i;
        }
        res.str = b;
        return res;
    }

    /**
     * repair commands in one line
     *
     * @param a string to process
     * @return repaired line, variables preserved, empty if no guess
     */
    public String repairLine(String a) {
        userHelpingList d = whereAm(a);
        if (d.level < 0) {
            return "";
        }
        return d.str;
    }

    /**
     * guess commands in one line
     *
     * @param a string to process
     * @return guessed line, variables preserved, null if no guess
     */
    public String guessLine(String a) {
        userHelpingList d = whereAm(a);
        return d.str;
    }

    /**
     * test if at end of a valid command
     *
     * @param s command to test
     * @return false if valid command, true if an invalid command
     */
    public boolean endOfCmd(String s) {
        userHelpingList d = whereAm(s);
        if (d.level < 0) {
            return true;
        }
        d = nextWords(d.level);
        for (int i = 0; i < d.num(); i++) {
            if (d.val(i) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * format help text to user displayable format for a line
     *
     * @param lin line number to generate help for
     * @return array of strings that user should read
     */
    public List<String> formatHelp(int lin) {
        final String begin = "  ";
        final String enter = "<cr>";
        userHelpingList d = nextWords(lin);
        List<String> s = new ArrayList<String>();
        int max = -1;
        for (int i = 0; i < d.num(); i++) {
            int o = d.val(i);
            if (o < 0) {
                o = enter.length();
            } else {
                o = lines.get(o).command.length();
            }
            if (o > max) {
                max = o;
            }
        }
        for (int i = 0; i < d.num(); i++) {
            int o = d.val(i);
            if (o < 0) {
                s.add(begin + enter);
                continue;
            }
            userHelpingData cur = lines.get(o);
            s.add(begin + bits.padEnd(cur.command, max, " ") + " - " + cur.description);
        }
        return s;
    }

    /**
     * find usage of a line
     *
     * @param dat data to append
     * @param beg beginning
     * @param hlp help text
     * @param lin line to get
     * @param lev current level
     * @return found entries
     */
    protected int formatUsage(userHelping dat, String beg, String hlp, int lin, int lev) {
        int fnd = 0;
        for (int o = lin; o < lines.size(); o++) {
            userHelpingData ntry = lines.get(o);
            if ((fnd > 0) && (ntry.level < lev)) {
                break;
            }
            if (ntry.level != lev) {
                continue;
            }
            fnd++;
            int q = 0;
            boolean b = false;
            for (int i = 0; i < ntry.after.num(); i++) {
                int p = ntry.after.val(i);
                if (p < 0) {
                    b = true;
                    continue;
                }
                if (p <= lev) {
                    continue;
                }
                q += formatUsage(dat, beg + " " + ntry.command, hlp + ", " + ntry.description, o + 1, p);
            }
            if ((!b) && (q > 0)) {
                continue;
            }
            userHelpingData res = new userHelpingData();
            String a = hlp + ", " + ntry.description;
            res.description = a.substring(2, a.length());
            a = beg + " " + ntry.command;
            res.command = a.substring(1, a.length());
            res.level = 1;
            dat.lines.add(res);
        }
        return fnd;
    }

    protected void formatYang(List<String> dat, String id, int lin, int lev) {
        int fnd = 0;
        for (int o = lin; o < lines.size(); o++) {
            userHelpingData ntry = lines.get(o);
            if ((fnd > 0) && (ntry.level < lev)) {
                break;
            }
            if (ntry.level != lev) {
                continue;
            }
            fnd++;
            List<Integer> nxt = new ArrayList<Integer>();
            boolean b = false;
            for (int i = 0; i < ntry.after.num(); i++) {
                int p = ntry.after.val(i);
                if (p < 0) {
                    b = true;
                    continue;
                }
                if (p <= lev) {
                    continue;
                }
                nxt.add(p);
            }
            String vn = ntry.command;
            if (ntry.variable) {
                vn = vn.substring(1, vn.length() - 1);
            }
            if (nxt.size() < 1) {
                if (ntry.variable) {
                    dat.add(id + "leaf " + vn + " {");
                    dat.add(id + "  description \"" + ntry.description + "\";");
                    dat.add(id + "  type string;");
                    dat.add(id + "}");
                    continue;
                }
                dat.add(id + "leaf " + vn + " {");
                dat.add(id + "  description \"" + ntry.description + "\";");
                dat.add(id + "  type empty;");
                dat.add(id + "}");
                continue;
            }
            if (!ntry.variable) {
                dat.add(id + "container " + vn + " {");
                dat.add(id + "  description \"" + ntry.description + "\";");
                if (b) {
                    dat.add(id + "  leaf " + userNetconf.specialIgnore + " {");
                    dat.add(id + "    description \"" + ntry.description + "\";");
                    dat.add(id + "    type empty;");
                    dat.add(id + "  }");
                }
                for (int i = 0; i < nxt.size(); i++) {
                    formatYang(dat, id + "  ", o + 1, nxt.get(i));
                }
                dat.add(id + "}");
                continue;
            }
            dat.add(id + "list " + userNetconf.specialIgnore + o + " {");
            dat.add(id + "  key \"" + userNetconf.specialIgnore + o + "\";");
            dat.add(id + "  leaf " + userNetconf.specialIgnore + o + " {");
            dat.add(id + "    description \"" + ntry.description + "\";");
            dat.add(id + "    type string;");
            dat.add(id + "  }");
            if (b) {
                dat.add(id + "  leaf " + userNetconf.specialIgnore + " {");
                dat.add(id + "    description \"" + ntry.description + "\";");
                dat.add(id + "    type empty;");
                dat.add(id + "  }");
            }
            for (int i = 0; i < nxt.size(); i++) {
                formatYang(dat, id + "  ", o + 1, nxt.get(i));
            }
            dat.add(id + "}");
        }
    }

    /**
     * get help text for a given command line
     *
     * @param s the command line
     * @param oneLine set true to return only one line response
     * @return array of strings that user should read
     */
    public List<String> getHelp(String s, boolean oneLine) {
        userHelpingList d = whereAm(s);
        String cmd = "";
        for (int i = 0; i < d.num(); i++) {
            cmd += " " + lines.get(d.val(i)).command;
        }
        if (cmd.length() > 0) {
            cmd = cmd.substring(1, cmd.length());
        }
        if (d.level == -2) {
            d = whereAm(cmd);
            s = s.trim();
            int i = s.lastIndexOf(" ") + 1;
            if (i < 1) {
                i = 0;
            }
            s = s.substring(i, s.length());
            if (oneLine) {
                return bits.str2lst("ambigous, try " + s + "?");
            }
            List<String> lst = formatHelp(d.level);
            lst = startsWith(lst, "  " + s);
            return lst;
        }
        if (d.level >= -1) {
            if (oneLine) {
                return bits.str2lst("incomplete, try " + cmd + " ?");
            }
            if (d.level == -1) {
                return formatHelp(d.level);
            }
            if (s.lastIndexOf(" ") >= s.length() - 1) {
                return formatHelp(d.level);
            }
            userHelpingData r = lines.get(d.level);
            return bits.str2lst("type " + r.command + " to " + r.description);
        }
        return bits.str2lst("invalid, try " + cmd + " ?");
    }

    /**
     * get usage text for a given command
     *
     * @return usage text
     */
    public List<String> getUsage() {
        userHelping d = new userHelping();
        formatUsage(d, "", "", 0, 1);
        return d.formatHelp(-1);
    }

    /**
     * get yang for a given command
     *
     * @param path path
     * @param prefix prefix
     * @return yang
     */
    public List<String> getYang(String path, String prefix) {
        List<String> res = new ArrayList<String>();
        res.add("module " + prefix + " {");
        res.add("  namespace \"" + verCore.homeUrl + "yang/" + prefix + "\";");
        res.add("  prefix \"" + prefix + "\";");
        cmds cp = new cmds("ya", path);
        String id = "  ";
        for (;;) {
            if (cp.size() < 1) {
                break;
            }
            String a = cp.word("/");
            if (a.equals("" + bits.str2num(a))) {
                a = userNetconf.specialEscape + a;
            }
            res.add(id + "container " + a + " {");
            id += "  ";
        }
        formatYang(res, id, 0, 1);
        for (; id.length() > 0;) {
            id = id.substring(0, id.length() - 2);
            res.add(id + "}");
        }
        return res;
    }

    /**
     * get list of commands
     *
     * @return list of commands
     */
    public List<String> getList() {
        List<String> s = new ArrayList<String>();
        String[] l = new String[64];
        for (int i = 0; i < l.length; i++) {
            l[i] = "";
        }
        for (int o = 0; o < lines.size(); o++) {
            userHelpingData ntry = lines.get(o);
            l[ntry.level] = "" + ntry.command;
            String a = "";
            for (int i = 0; i <= ntry.level; i++) {
                a = a + l[i] + " ";
            }
            s.add(a.trim());
        }
        return s;
    }

    /**
     * dump out the menu lines
     *
     * @return string showing menu lines
     */
    public String dump() {
        String s = "";
        for (int i = 0; i < lines.size(); i++) {
            s += i + ":" + lines.get(i).dump() + "\n";
        }
        return s;
    }

    /**
     * add possibility to lines
     *
     * @param lvl source level to add
     * @param nxt next level to add
     */
    public void possible(int lvl, int nxt) {
        for (int i = 0; i < lines.size(); i++) {
            userHelpingData lns = lines.get(i);
            if (lns.after.find(lvl) < 0) {
                continue;
            }
            lns.after.add(nxt);
        }
    }

}

class userHelpingList {

    public List<Integer> data = new ArrayList<Integer>(); // values

    public int level; // which level we are on

    public String str; // repaired line

    public int num() {
        return data.size();
    }

    public int val(int i) {
        return data.get(i);
    }

    public void add(int i) {
        data.add(i);
    }

    public void addMore(userHelpingList d) {
        data.addAll(d.data);
    }

    public int find(int lvl) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) == lvl) {
                return i;
            }
        }
        return -1;
    }

    public userHelpingList copyBytes() {
        userHelpingList n = new userHelpingList();
        n.data.addAll(data);
        return n;
    }

    public String dump() {
        String s = level + "|";
        for (int i = 0; i < data.size(); i++) {
            s += " " + data.get(i);
        }
        return s;
    }

}

class userHelpingData {

    protected int level;

    protected userHelpingList after = new userHelpingList();

    protected String command = "";

    protected String description = "";

    protected boolean variable;

    public void set(String s) {
        boolean hidden = false;
        s = s.trim();
        if (s.startsWith(".")) {
            hidden = true;
            s = s.substring(1, s.length());
        }
        int i = s.indexOf(" ");
        level = bits.str2num(s.substring(0, i).trim());
        s = s.substring(i, s.length()).trim();
        i = s.indexOf(" ");
        String a = s.substring(0, i).trim() + ",";
        s = s.substring(i, s.length()).trim();
        for (; a.length() > 0;) {
            i = a.indexOf(",");
            String b = a.substring(0, i).trim();
            a = a.substring(i + 1, a.length()).trim();
            if (b.equals(".")) {
                after.add(-1);
            } else {
                after.add(bits.str2num(b));
            }
        }
        i = s.indexOf(" ");
        description = s.substring(i + 1, s.length()).trim();
        command = s.substring(0, i).trim().toLowerCase();
        if (command.equals("?")) {
            command = "";
        }
        variable = (command.indexOf("<") == 0) || (command.indexOf("[") == 0);
        if (hidden) {
            description = "!!!EXPERiMENTAL!!! " + description;
            if (verCore.release) {
                level = 666;
            }
        }
    }

    public userHelpingData copyBytes() {
        userHelpingData n = new userHelpingData();
        n.level = level;
        n.after = after.copyBytes();
        n.command = command;
        n.description = description;
        return n;
    }

    public String dump() {
        return variable + " lev=" + level + " aft=" + after.dump() + " cmd=" + command + " desc=" + description;
    }

}
