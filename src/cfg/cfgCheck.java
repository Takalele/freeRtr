package cfg;

import addr.addrIP;
import clnt.clntDns;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pack.packDnsRec;
import pack.packNrpe;
import pipe.pipeLine;
import pipe.pipeSide;
import tab.tabGen;
import user.userExec;
import user.userFilter;
import user.userFormat;
import user.userHelping;
import user.userReader;
import util.bits;
import util.cmds;

/**
 * check exporter
 *
 * @author matecsaba
 */
public class cfgCheck implements Comparator<cfgCheck>, cfgGeneric {

    /**
     * name of check
     */
    public final String name;

    /**
     * time to answer
     */
    public int tim;

    /**
     * ok answers
     */
    public int okNum;

    /**
     * error answers
     */
    public int errNum;

    /**
     * last ok
     */
    public long okTim;

    /**
     * last error
     */
    public long errTim;

    /**
     * command to execute
     */
    public String command;

    /**
     * send out command after error text
     */
    public boolean sendCmds;

    /**
     * send out my name after error text
     */
    public boolean sendMyId;

    /**
     * don't send out error/ok
     */
    public boolean noState;

    /**
     * inherit parameters
     */
    public cfgCheck inherit;

    /**
     * description
     */
    public String dsc;

    /**
     * error string
     */
    public String err;

    /**
     * ignore all regexp
     */
    public final List<String> allR;

    /**
     * ignore one regexp
     */
    public final List<String> ignR;

    /**
     * require one regexp
     */
    public final List<String> reqR;

    /**
     * ignore all text
     */
    public final List<String> allT;

    /**
     * ignore one text
     */
    public final List<String> ignT;

    /**
     * require one text
     */
    public final List<String> reqT;

    /**
     * list of resolvers
     */
    public tabGen<cfgCheckRes> ress = new tabGen<cfgCheckRes>();

    /**
     * list of replacers
     */
    public tabGen<cfgCheckRep> reps = new tabGen<cfgCheckRep>();

    /**
     * alternate result
     */
    public boolean alternate;

    /**
     * last hash
     */
    public int lastHash = 1;

    /**
     * last status
     */
    public int lastStat = packNrpe.coWar;

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "check .*! no description",
        "check .*! no inherit",
        "check .*! no error-text",
        "check .*! no alternate",
        "check .*! no error-states",
        "check .*! no error-commands",
        "check .*! no error-hostname",};

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    /**
     * create new check
     *
     * @param n name
     */
    public cfgCheck(String n) {
        name = n;
        allR = new ArrayList<String>();
        ignR = new ArrayList<String>();
        reqR = new ArrayList<String>();
        allT = new ArrayList<String>();
        ignT = new ArrayList<String>();
        reqT = new ArrayList<String>();
    }

    public String toString() {
        return name;
    }

    public int compare(cfgCheck o1, cfgCheck o2) {
        return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
    }

    public String getPrompt() {
        return "check";
    }

    public void getHelp(userHelping l) {
        l.add("1 2      resolve                  resolve the regexp group a to hostname");
        l.add("2 3        <name>                 regexp of checks");
        l.add("3 3,.    <str>                    text to resolve");
        l.add("1 2      replace                  replace from one string to another");
        l.add("2 3        <name>                 regexp of checks");
        l.add("3 4          <str>                source string");
        l.add("4 4,.          <str>              target string");
        l.add("1 2,.    train                    train command to current result");
        l.add("2 2,.      <str>                  text");
        l.add("1 .      alternate                alternate reported state on diff change");
        l.add("1 2      inherit                  inherit parameters");
        l.add("2 .        <str>                  name of check");
        l.add("1 2      command                  specify command to execute");
        l.add("2 2,.      <str>                  command");
        l.add("1 2      description              specify description");
        l.add("2 2,.      <str>                  description");
        l.add("1 2      error-text               specify error text");
        l.add("2 2,.      <str>                  description");
        l.add("1 .      error-states             remove state of messages");
        l.add("1 .      error-commands           include commands in states");
        l.add("1 .      error-hostname           include local hostname in states");
        l.add("1 2      require-regexp           require one regexp line");
        l.add("2 2,.      <str>                  text");
        l.add("1 2      ignore-regexp            ignore one regexp line");
        l.add("2 2,.      <str>                  text");
        l.add("1 2      ignorall-regexp          ignore all regexp line");
        l.add("2 2,.      <str>                  text");
        l.add("1 2      require-text             require one text line");
        l.add("2 2,.      <str>                  text");
        l.add("1 2      ignore-text              ignore one text line");
        l.add("2 2,.      <str>                  text");
        l.add("1 2      ignorall-text            ignore all text line");
        l.add("2 2,.      <str>                  text");
    }

    public List<String> getShRun(boolean filter) {
        List<String> l = new ArrayList<String>();
        l.add("check " + name);
        cmds.cfgLine(l, command == null, cmds.tabulator, "command", "" + command);
        cmds.cfgLine(l, inherit == null, cmds.tabulator, "inherit", "" + inherit);
        for (int i = 0; i < ress.size(); i++) {
            l.add(cmds.tabulator + "resolve " + ress.get(i));
        }
        for (int i = 0; i < reps.size(); i++) {
            l.add(cmds.tabulator + "replace " + reps.get(i));
        }
        cmds.cfgLine(l, dsc == null, cmds.tabulator, "description", dsc);
        cmds.cfgLine(l, err == null, cmds.tabulator, "error-text", err);
        cmds.cfgLine(l, !alternate, cmds.tabulator, "alternate", "");
        cmds.cfgLine(l, !sendCmds, cmds.tabulator, "error-commands", "");
        cmds.cfgLine(l, !sendMyId, cmds.tabulator, "error-hostname", "");
        cmds.cfgLine(l, !noState, cmds.tabulator, "error-states", "");
        for (int i = 0; i < ignT.size(); i++) {
            l.add(cmds.tabulator + "ignore-text " + ignT.get(i));
        }
        for (int i = 0; i < allT.size(); i++) {
            l.add(cmds.tabulator + "ignorall-text " + allT.get(i));
        }
        for (int i = 0; i < ignR.size(); i++) {
            l.add(cmds.tabulator + "ignore-regexp " + ignR.get(i));
        }
        for (int i = 0; i < allR.size(); i++) {
            l.add(cmds.tabulator + "ignorall-regexp " + allR.get(i));
        }
        for (int i = 0; i < reqT.size(); i++) {
            l.add(cmds.tabulator + "require-text " + reqT.get(i));
        }
        for (int i = 0; i < reqR.size(); i++) {
            l.add(cmds.tabulator + "require-regexp " + reqR.get(i));
        }
        l.add(cmds.tabulator + cmds.finish);
        l.add(cmds.comment);
        if (!filter) {
            return l;
        }
        return userFilter.filterText(l, defaultF);
    }

    public void doCfgStr(cmds cmd) {
        String s = cmd.word();
        boolean negated = s.equals("no");
        if (negated) {
            s = cmd.word();
        }
        if (s.equals("resolve")) {
            s = cmd.getRemaining();
            if (negated) {
                ress.del(new cfgCheckRes(s));
            } else {
                ress.add(new cfgCheckRes(s));
            }
            return;
        }
        if (s.equals("replace")) {
            String a = cmd.word();
            s = cmd.getRemaining();
            if (negated) {
                reps.del(new cfgCheckRep(a, s));
            } else {
                reps.add(new cfgCheckRep(a, s));
            }
            return;
        }
        if (s.equals("command")) {
            command = cmd.getRemaining();
            if (negated) {
                command = null;
            }
            return;
        }
        if (s.equals("description")) {
            dsc = cmd.getRemaining();
            if (negated) {
                dsc = null;
            }
            return;
        }
        if (s.equals("error-text")) {
            err = cmd.getRemaining();
            if (negated) {
                err = null;
            }
            return;
        }
        if (s.equals("error-commands")) {
            sendCmds = !negated;
            return;
        }
        if (s.equals("error-hostname")) {
            sendMyId = !negated;
            return;
        }
        if (s.equals("error-states")) {
            noState = !negated;
            return;
        }
        if (s.equals("alternate")) {
            alternate = !negated;
            return;
        }
        if (s.equals("require-regexp")) {
            s = cmd.getRemaining();
            if (negated) {
                reqR.remove(s);
            } else {
                reqR.add(s);
            }
            return;
        }
        if (s.equals("ignore-regexp")) {
            s = cmd.getRemaining();
            if (negated) {
                ignR.remove(s);
            } else {
                ignR.add(s);
            }
            return;
        }
        if (s.equals("ignorall-regexp")) {
            s = cmd.getRemaining();
            if (negated) {
                allR.remove(s);
            } else {
                allR.add(s);
            }
            return;
        }
        if (s.equals("require-text")) {
            s = cmd.getRemaining();
            if (negated) {
                reqT.remove(s);
            } else {
                reqT.add(s);
            }
            return;
        }
        if (s.equals("ignore-text")) {
            s = cmd.getRemaining();
            if (negated) {
                ignT.remove(s);
            } else {
                ignT.add(s);
            }
            return;
        }
        if (s.equals("ignorall-text")) {
            s = cmd.getRemaining();
            if (negated) {
                allT.remove(s);
            } else {
                allT.add(s);
            }
            return;
        }
        if (s.equals("inherit")) {
            inherit = cfgAll.checkFind(cmd.word(), false);
            if (inherit == null) {
                cmd.error("no such check");
                return;
            }
            if (negated) {
                inherit = null;
            }
            return;
        }
        if (s.equals("train")) {
            s = cmd.getRemaining();
            if (s.length() < 1) {
                doTrain();
            } else {
                doTrain(s);
            }
            return;
        }
        cmd.badCmd();
    }

    private List<String> getResult() {
        pipeLine pl = new pipeLine(1024 * 1024, false);
        pipeSide pip = pl.getSide();
        pip.lineTx = pipeSide.modTyp.modeCRLF;
        pip.lineRx = pipeSide.modTyp.modeCRorLF;
        userReader rdr = new userReader(pip, null);
        rdr.tabMod = userFormat.tableMode.raw;
        rdr.height = 0;
        userExec exe = new userExec(pip, rdr);
        exe.privileged = true;
        pip.setTime(120000);
        String a = exe.repairCommand(command);
        exe.executeCommand(a);
        pip = pl.getSide();
        pl.setClose();
        pip.lineTx = pipeSide.modTyp.modeCRLF;
        pip.lineRx = pipeSide.modTyp.modeCRtryLF;
        List<String> lst = new ArrayList<String>();
        for (;;) {
            if (pip.ready2rx() < 1) {
                break;
            }
            a = pip.lineGet(1);
            if (a.length() < 1) {
                continue;
            }
            lst.add(a);
        }
        return lst;
    }

    private void delReg(List<String> lst) {
        for (int o = 0; o < reqR.size(); o++) {
            String s = reqR.get(o);
            for (int i = 0; i < lst.size(); i++) {
                if (!lst.get(i).matches(s)) {
                    continue;
                }
                lst.remove(i);
                break;
            }
        }
    }

    private void delIgn(List<String> lst) {
        for (int o = 0; o < ignT.size(); o++) {
            String s = ignT.get(o);
            for (int i = 0; i < lst.size(); i++) {
                if (!lst.get(i).equals(s)) {
                    continue;
                }
                lst.remove(i);
                break;
            }
        }
        for (int o = 0; o < allT.size(); o++) {
            String s = allT.get(o);
            for (int i = lst.size() - 1; i >= 0; i--) {
                if (!lst.get(i).equals(s)) {
                    continue;
                }
                lst.remove(i);
            }
        }
        for (int o = 0; o < ignR.size(); o++) {
            String s = ignR.get(o);
            for (int i = 0; i < lst.size(); i++) {
                if (!lst.get(i).matches(s)) {
                    continue;
                }
                lst.remove(i);
                break;
            }
        }
        for (int o = 0; o < allR.size(); o++) {
            String s = allR.get(o);
            for (int i = lst.size() - 1; i >= 0; i--) {
                if (!lst.get(i).matches(s)) {
                    continue;
                }
                lst.remove(i);
            }
        }
    }

    private String makeFancy(String l) {
        for (int i = 0; i < ress.size(); i++) {
            cfgCheckRes r = ress.get(i);
            if (r == null) {
                continue;
            }
            l = r.doWork(l);
        }
        for (int i = 0; i < reps.size(); i++) {
            cfgCheckRep r = reps.get(i);
            if (r == null) {
                continue;
            }
            l = l.replaceAll(r.src, r.trg);
        }
        if (inherit == null) {
            return l;
        }
        return inherit.makeFancy(l);
    }

    private void doCheckMiss(List<String> lst, List<String> res) {
        for (int o = 0; o < reqT.size(); o++) {
            String s = reqT.get(o);
            boolean ok = false;
            for (int i = 0; i < lst.size(); i++) {
                if (!lst.get(i).equals(s)) {
                    continue;
                }
                lst.remove(i);
                ok = true;
                break;
            }
            if (ok) {
                continue;
            }
            res.add("- " + makeFancy(s));
        }
        for (int o = 0; o < reqR.size(); o++) {
            String s = reqR.get(o);
            boolean ok = false;
            for (int i = 0; i < lst.size(); i++) {
                if (!lst.get(i).matches(s)) {
                    continue;
                }
                lst.remove(i);
                ok = true;
                break;
            }
            if (ok) {
                continue;
            }
            res.add("- " + makeFancy(s));
        }
    }

    private void doCheckExtra(List<String> lst, List<String> res) {
        for (int i = 0; i < lst.size(); i++) {
            res.add("+ " + makeFancy(lst.get(i)));
        }
    }

    private String getHeadLine(List<String> lst) {
        String s = "";
        boolean b = noState;
        if (inherit != null) {
            b |= inherit.noState;
        }
        if (!b) {
            s += "ERROR ";
        }
        s += lst.size() + " ";
        String a = err;
        if ((a == null) && (inherit != null)) {
            a = inherit.err;
        }
        if (a == null) {
            a = "lines(s) changed";
        }
        s += a + " ";
        b = sendMyId;
        if (inherit != null) {
            b |= inherit.sendMyId;
        }
        if (b) {
            s += " - " + cfgAll.hostName + "#";
        }
        b = sendCmds;
        if (inherit != null) {
            b |= inherit.sendCmds;
        }
        if (b) {
            s += command + " ";
        }
        return s;
    }

    /**
     * do the checking
     *
     * @return result
     */
    public List<String> doCheck() {
        List<String> lst = getResult();
        List<String> res = new ArrayList<String>();
        delIgn(lst);
        if (inherit != null) {
            inherit.delIgn(lst);
            inherit.doCheckMiss(lst, res);
        }
        doCheckMiss(lst, res);
        doCheckExtra(lst, res);
        return res;
    }

    /**
     * get nrpe report
     *
     * @param pck packet to update
     */
    public void getReportNrpe(packNrpe pck) {
        pck.typ = packNrpe.tyRep;
        long tim = bits.getTime();
        List<String> lst = doCheck();
        tim = (int) (bits.getTime() - tim);
        if (lst.size() < 1) {
            okNum++;
            okTim = tim;
            pck.cod = packNrpe.coOk;
            pck.str = "OK";
            if (dsc != null) {
                pck.str += " " + dsc;
            } else if (inherit != null) {
                if (inherit.dsc != null) {
                    pck.str += " " + inherit.dsc;
                }
            }
            return;
        }
        errNum++;
        errTim = tim;
        pck.cod = packNrpe.coCri;
        pck.str = getHeadLine(lst).trim();
        for (int i = 0; i < lst.size(); i++) {
            pck.str += new String(pck.sep) + lst.get(i).trim();
        }
        if (!alternate) {
            if (inherit == null) {
                return;
            }
            if (!inherit.alternate) {
                return;
            }
        }
        int i = pck.str.hashCode();
        if (i == lastHash) {
            pck.cod = lastStat;
        } else {
            lastHash = i;
            if (lastStat == packNrpe.coCri) {
                i = packNrpe.coWar;
            } else {
                i = packNrpe.coCri;
            }
            lastStat = i;
            pck.cod = i;
        }
    }

    /**
     * train the check
     *
     * @param ned line to train
     */
    public void doTrain(String ned) {
        List<String> lst = getResult();
        delIgn(lst);
        delReg(lst);
        if (inherit != null) {
            inherit.delIgn(lst);
            inherit.delReg(lst);
            inherit.doCheckMiss(lst, new ArrayList<String>());
        }
        for (int i = reqT.size() - 1; i >= 0; i--) {
            if (reqT.get(i).indexOf(ned) >= 0) {
                reqT.remove(i);
            }
        }
        for (int i = 0; i < lst.size(); i++) {
            String a = lst.get(i);
            if (a.indexOf(ned) < 0) {
                continue;
            }
            reqT.add(a);
        }
    }

    /**
     * train the check
     */
    public void doTrain() {
        List<String> lst = getResult();
        delIgn(lst);
        delReg(lst);
        if (inherit != null) {
            inherit.delIgn(lst);
            inherit.delReg(lst);
            inherit.doCheckMiss(lst, new ArrayList<String>());
        }
        reqT.clear();
        for (int i = 0; i < lst.size(); i++) {
            reqT.add(lst.get(i));
        }
    }

    /**
     * get show
     *
     * @return result
     */
    public List<String> getShow() {
        List<String> res = new ArrayList<String>();
        res.add("name=" + name);
        res.add("inherit=" + inherit);
        res.add("description=" + dsc);
        res.add("command=" + command);
        res.add("error=" + err);
        res.add("alternate=" + alternate);
        res.add("asked=" + (okNum + errNum) + " times");
        res.add("reply=" + tim + " ms");
        res.add("passed=" + okNum + " times, last " + bits.time2str(cfgAll.timeZoneName, okTim + cfgAll.timeServerOffset, 3) + " (" + bits.timePast(okTim) + " ago)");
        res.add("failed=" + errNum + " times, last " + bits.time2str(cfgAll.timeZoneName, errTim + cfgAll.timeServerOffset, 3) + " (" + bits.timePast(errTim) + " ago)");
        res.add("output:");
        res.addAll(getResult());
        res.add("result:");
        res.addAll(doCheck());
        packNrpe nrp = new packNrpe();
        getReportNrpe(nrp);
        res.add("nrpe:" + nrp.cod + " " + nrp.str);
        return res;
    }

}

class cfgCheckRep implements Comparator<cfgCheckRep> {

    public final String src;

    public final String trg;

    public cfgCheckRep(String s, String t) {
        src = s;
        trg = t;
    }

    public String toString() {
        return src + " " + trg;
    }

    public int compare(cfgCheckRep o1, cfgCheckRep o2) {
        return o1.src.toLowerCase().compareTo(o2.src.toLowerCase());
    }

}

class cfgCheckRes implements Comparator<cfgCheckRes> {

    public final String nam;

    public cfgCheckRes(String s) {
        nam = s;
    }

    public String toString() {
        return nam;
    }

    public int compare(cfgCheckRes o1, cfgCheckRes o2) {
        return o1.nam.toLowerCase().compareTo(o2.nam.toLowerCase());
    }

    public String doWork(String l) {
        String as;
        try {
            Pattern pat = Pattern.compile(nam);
            Matcher mat = pat.matcher(l);
            if (!mat.find()) {
                return l;
            }
            as = mat.group("a");
        } catch (Exception e) {
            as = null;
        }
        if (as == null) {
            return l;
        }
        addrIP ad = new addrIP();
        if (ad.fromString(as)) {
            return l;
        }
        clntDns clnt = new clntDns();
        clnt.doResolvList(cfgAll.nameServerAddr, packDnsRec.generateReverse(ad), false, packDnsRec.typePTR);
        String dn = clnt.getPTR();
        if (dn == null) {
            return l;
        }
        return l.replaceAll(as, dn);
    }

}
