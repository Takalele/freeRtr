package user;

import cfg.cfgAll;
import cfg.cfgSensor;
import java.util.ArrayList;
import java.util.List;
import pipe.pipeLine;
import pipe.pipeSetting;
import pipe.pipeSide;
import util.bits;
import util.cmds;
import util.debugger;
import util.extMrkLng;
import util.extMrkLngEntry;
import util.logger;
import util.verCore;

/**
 * netconf (rfc6241) handler
 *
 * @author matecsaba
 */
public class userNetconf {

    /**
     * header separator
     */
    public final static String headerEnd = "]]>]]>";

    /**
     * get-filter
     */
    public final static String getFilter = "/?xml/rpc/get/filter/";

    /**
     * get-config
     */
    public final static String getConfig = "/?xml/rpc/get-config/filter/config";

    /**
     * edit-config
     */
    public final static String editConfig = "/?xml/rpc/edit-config/config/config";

    /**
     * reply-data
     */
    public final static String replyData = "/rpc-reply/data";

    /**
     * namespace
     */
    public final static String namespace = "xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\"";

    /**
     * port
     */
    public final static int port = 830;

    private final pipeSide conn;

    private final boolean privi;

    private final boolean form;

    private final boolean echo;

    private final int sessId;

    private int currVer; // 10, 11

    private boolean need2run;

    /**
     * create handler
     *
     * @param pipe pipe to use
     * @param prv privileged
     * @param frm format response
     * @param ech echo input
     */
    public userNetconf(pipeSide pipe, boolean prv, boolean frm, boolean ech) {
        conn = pipe;
        privi = prv;
        form = frm;
        echo = ech;
        sessId = bits.randomD();
        need2run = true;
    }

    /**
     * make yang
     *
     * @param lst list to use
     * @param beg beginning
     * @param end ending
     * @return help
     */
    public static List<String> makeYang(List<String> lst, int beg, int end) {
        String path = lst.get(beg + 0);
        String prefix = lst.get(beg + 1);
        pipeLine pl = new pipeLine(65535, false);
        pipeSide pip = pl.getSide();
        pip.lineTx = pipeSide.modTyp.modeCRLF;
        pip.lineRx = pipeSide.modTyp.modeCRorLF;
        userReader rdr = new userReader(pip, null);
        pip.settingsPut(pipeSetting.tabMod, userFormat.tableMode.raw);
        pip.settingsPut(pipeSetting.height, 0);
        userConfig cfg = new userConfig(pip, rdr);
        pip.setTime(60000);
        int pos;
        for (pos = beg + 2; pos < end; pos++) {
            String a = lst.get(pos);
            if (a.equals("!")) {
                break;
            }
            userHelping hlp = cfg.getHelping(true, true);
            rdr.setContext(hlp, "");
            String b = hlp.repairLine(a);
            if (b.length() < 1) {
                pip.linePut("bad: " + a);
                continue;
            }
            cfg.executeCommand(b);
        }
        pos++;
        userHelping ned = cfg.getHelping(false, false);
        for (; pos < end; pos++) {
            String a = lst.get(pos);
            userHelping hlp = cfg.getHelping(true, true);
            rdr.setContext(hlp, "");
            String b = hlp.repairLine(a);
            if (b.length() < 1) {
                pip.linePut("bad: " + a);
                continue;
            }
            cfg.executeCommand(b);
        }
        pip = pl.getSide();
        pl.setClose();
        String a = pip.strGet(65535);
        lst = ned.getYang(path, prefix);
        if (a == null) {
            return lst;
        }
        if (a.length() < 1) {
            return lst;
        }
        logger.error("error happened: " + a);
        return lst;
    }

    /**
     * do hello
     *
     * @return true on error, false on success
     */
    public boolean doHello() {
        currVer = 10;
        extMrkLng x = new extMrkLng();
        x.data.add(new extMrkLngEntry(null, "/hello", "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"", ""));
        x.data.add(new extMrkLngEntry(null, "/hello/capabilities", "", ""));
        x.data.add(new extMrkLngEntry(null, "/hello/capabilities/capability", "", "urn:ietf:params:netconf:base:1.0"));
        x.data.add(new extMrkLngEntry(null, "/hello/capabilities", "", ""));
        x.data.add(new extMrkLngEntry(null, "/hello/capabilities/capability", "", "urn:ietf:params:netconf:base:1.1"));
        x.data.add(new extMrkLngEntry(null, "/hello/capabilities", "", ""));
        x.data.add(new extMrkLngEntry(null, "/hello/capabilities/capability", "", "urn:ietf:params:netconf:capability:writable-running:1.0"));
        x.data.add(new extMrkLngEntry(null, "/hello/capabilities", "", ""));
        x.data.add(new extMrkLngEntry(null, "/hello/capabilities/capability", "", "urn:ietf:params:netconf:capability:startup:1.0"));
        x.data.add(new extMrkLngEntry(null, "/hello/capabilities", "", ""));
        for (int i = 0; i < cfgAll.sensors.size(); i++) {
            cfgSensor ntry = cfgAll.sensors.get(i);
            x.data.add(new extMrkLngEntry(null, "/hello/capabilities/capability", "", verCore.homeUrl + "yang/" + ntry.prefix + "?module=" + ntry.prefix));
            x.data.add(new extMrkLngEntry(null, "/hello/capabilities", "", ""));
        }
        x.data.add(new extMrkLngEntry(null, "/hello/session-id", "", "" + sessId));
        x.data.add(new extMrkLngEntry(null, "/hello", "", ""));
        doSend(x);
        x = doRead();
        if (x == null) {
            return true;
        }
        for (int i = 0; i < x.data.size(); i++) {
            extMrkLngEntry ntry = x.data.get(i);
            String a = getName(ntry, false);
            if (!a.equals("/?xml/hello/capabilities/capability")) {
                continue;
            }
            if (ntry.value.equals("urn:ietf:params:netconf:base:1.1")) {
                currVer = 11;
                continue;
            }
        }
        if (debugger.userNetconfEvnt) {
            logger.debug("ver: " + currVer);
        }
        return false;
    }

    private void addError(extMrkLng rep, String path, String msg) {
        rep.data.add(new extMrkLngEntry(null, "/rpc-reply/rpc-error/error-type", "", "application"));
        rep.data.add(new extMrkLngEntry(null, "/rpc-reply/rpc-error/error-tag", "", "invalid-value"));
        rep.data.add(new extMrkLngEntry(null, "/rpc-reply/rpc-error/error-severity", "", "error"));
        rep.data.add(new extMrkLngEntry(null, "/rpc-reply/rpc-error/error-path", "", path));
        rep.data.add(new extMrkLngEntry(null, "/rpc-reply/rpc-error/error-message", "", msg));
        rep.data.add(new extMrkLngEntry(null, "/rpc-reply", "", ""));
    }

    /**
     * do request
     *
     * @param req request
     * @return response, null if error
     */
    public extMrkLng doRequest(extMrkLng req) {
        extMrkLng rep = new extMrkLng();
        String rpc = null;
        int mod = 1;
        for (int p = 0; p < (req.data.size() - 1); p++) {
            extMrkLngEntry ntry = req.data.get(p);
            String a = getName(ntry, false);
            if ((rpc == null) && (a.equals("/?xml/rpc"))) {
                rpc = ntry.param;
            }
            String n = getName(req.data.get(p + 1), false);
            if (mod == 1) {
                if (n.length() > a.length()) {
                    continue;
                }
            } else {
                if (n.length() < a.length()) {
                    continue;
                }
            }
            mod = 3 - mod;
            if (mod != 2) {
                continue;
            }
            if (a.startsWith(editConfig)) {
                if (!privi) {
                    addError(rep, a, "not enough privileges");
                    continue;
                }
                n = a;
                a = getName(ntry, true);
                if (!a.startsWith(editConfig)) {
                    addError(rep, a, "error in encoding");
                    continue;
                }
                a = a.substring(editConfig.length(), a.length());
                a = a.replaceAll("/", " ").trim();
                pipeLine pl = new pipeLine(65535, false);
                pipeSide pip = pl.getSide();
                pip.lineTx = pipeSide.modTyp.modeCRLF;
                pip.lineRx = pipeSide.modTyp.modeCRorLF;
                userReader rdr = new userReader(pip, null);
                pip.settingsPut(pipeSetting.tabMod, userFormat.tableMode.raw);
                pip.settingsPut(pipeSetting.height, 0);
                pip.setTime(60000);
                userExec.doSetUnset(pip, rdr, new cmds("cm", a), false);
                pip = pl.getSide();
                pl.setClose();
                a = pip.strGet(65535);
                if (a == null) {
                    a = "";
                }
                a = a.trim();
                if (a.length() > 0) {
                    addError(rep, n, a);
                    continue;
                }
                rep.data.add(new extMrkLngEntry(null, "/rpc-reply/ok", "", ""));
                rep.data.add(new extMrkLngEntry(null, "/rpc-reply", "", ""));
                continue;
            }
            if (a.startsWith(getConfig)) {
                List<String> cfg = cfgAll.getShRun(false);
                List<userFilter> sec = userFilter.text2section(cfg);
                n = a.substring(getConfig.length(), a.length());
                List<userFilter> res = new ArrayList<userFilter>();
                for (; n.length() > 0;) {
                    a = extMrkLng.unescId(n).replaceAll("/", " ");
                    res = userFilter.getSection(sec, a.trim(), false, false, true);
                    if (res.size() > 0) {
                        break;
                    }
                    int i = n.lastIndexOf("/");
                    if (i < 0) {
                        n = "";
                        break;
                    }
                    n = n.substring(0, i);
                }
                if (res.size() < 1) {
                    addError(rep, n, "got empty config");
                    continue;
                }
                userFilter.section2xml(rep, "/rpc-reply/data/config" + n, res);
                rep.data.add(new extMrkLngEntry(null, "/rpc-reply", "", ""));
                continue;
            }
            if (a.startsWith(getFilter)) {
                cfgSensor tl = getSensor(a.substring(getFilter.length(), a.length()));
                if (tl == null) {
                    addError(rep, a, "no such sensor");
                    continue;
                }
                rep.data.add(new extMrkLngEntry(null, replyData, "", ""));
                a = tl.path;
                int o = a.indexOf("/");
                rep.data.add(new extMrkLngEntry(null, replyData + "/" + a.substring(0, o), "xmlns=\"" + verCore.homeUrl + "yang/" + tl.prefix + "\"", ""));
                tl.getReportNetConf(rep, replyData + "/");
                rep.data.add(new extMrkLngEntry(null, "/rpc-reply", "", ""));
                continue;
            }
            if (a.startsWith("/?xml/rpc/close-session")) {
                rep.data.add(new extMrkLngEntry(null, "/rpc-reply/ok", "", ""));
                rep.data.add(new extMrkLngEntry(null, "/rpc-reply", "", ""));
                need2run = false;
                continue;
            }
        }
        if (rpc == null) {
            rpc = "";
        }
        rep.data.add(0, new extMrkLngEntry(null, "/rpc-reply", rpc, ""));
        rep.data.add(new extMrkLngEntry(null, "/rpc-reply", "", ""));
        return rep;
    }

    private static cfgSensor getSensor(String a) {
        for (int i = 0; i < cfgAll.sensors.size(); i++) {
            cfgSensor tl = cfgAll.sensors.get(i);
            if (a.startsWith(tl.path)) {
                return tl;
            }
        }
        return null;
    }

    private static String getName(extMrkLngEntry ntry, boolean unesc) {
        String a;
        if (unesc) {
            a = ntry.getUnesc();
        } else {
            a = "" + ntry.name;
        }
        return a.replaceAll("/nc:", "/");
    }

    private static void dumpXml(cmds cmd, extMrkLng x) {
        List<String> l = x.show();
        for (int i = 0; i < l.size(); i++) {
            cmd.error(l.get(i));
        }
    }

    /**
     * read request
     *
     * @return request, null if error
     */
    public extMrkLng doRead() {
        String s = "";
        if (currVer < 11) {
            for (;;) {
                if (conn.isClosed() != 0) {
                    return null;
                }
                String a = conn.strGet(1);
                if (a == null) {
                    continue;
                }
                if (a.length() < 1) {
                    continue;
                }
                if (echo) {
                    conn.strPut(a);
                }
                s += a;
                if (s.endsWith(headerEnd)) {
                    break;
                }
            }
            if (debugger.userNetconfEvnt) {
                logger.debug("rx: " + s);
            }
            s = s.substring(0, s.length() - headerEnd.length());
        } else {
            pipeSide.modTyp sav = conn.lineRx;
            for (;;) {
                if (conn.isClosed() != 0) {
                    return null;
                }
                conn.lineRx = pipeSide.modTyp.modeLF;
                String a = conn.lineGet(echo ? 0x32 : 1);
                conn.lineRx = sav;
                if (a.length() < 1) {
                    continue;
                }
                if (a.equals("##")) {
                    break;
                }
                if (!a.startsWith("#")) {
                    continue;
                }
                int i = bits.str2num(a.substring(1, a.length()));
                a = conn.strGet(i);
                if (debugger.userNetconfEvnt) {
                    logger.debug("rx: " + a);
                }
                if (a == null) {
                    continue;
                }
                s += a.replaceAll("\r", "\n");
            }
        }
        extMrkLng x = new extMrkLng();
        if (x.fromString(s)) {
            return null;
        }
        return x;
    }

    private void doSend(String a) {
        if (debugger.userNetconfEvnt) {
            logger.debug("tx: " + a);
        }
        if (currVer > 10) {
            conn.linePut("");
            conn.linePut("#" + (a.length() + 1));
        }
        conn.linePut(a);
    }

    /**
     * send response
     *
     * @param x xml
     */
    public void doSend(extMrkLng x) {
        pipeSide.modTyp sav = conn.lineTx;
        conn.lineTx = pipeSide.modTyp.modeLF;
        doSend(extMrkLng.header);
        if (form) {
            List<String> l = x.toXMLlst();
            for (int i = 0; i < l.size(); i++) {
                doSend(l.get(i));
            }
        } else {
            doSend(x.toXMLstr());
        }
        if (currVer > 10) {
            conn.linePut("");
            conn.linePut("##");
        } else {
            conn.strPut(headerEnd);
        }
        conn.lineTx = sav;
    }

    /**
     * do work
     */
    public void doClose() {
        extMrkLng x = new extMrkLng();
        x.data.add(new extMrkLngEntry(null, "/rpc", namespace + " message-id=\"" + bits.randomD() + "\"", ""));
        x.data.add(new extMrkLngEntry(null, "/rpc/close-session", "", ""));
        doSend(x);
        doRead();
    }

    /**
     * do work
     */
    public void doServer() {
        if (doHello()) {
            return;
        }
        for (;;) {
            if (!need2run) {
                break;
            }
            extMrkLng x = doRead();
            if (x == null) {
                break;
            }
            x = doRequest(x);
            doSend(x);
        }
    }

    /**
     * do work
     *
     * @param cmd console
     * @param mod mode
     * @param path path
     * @param ns namespace
     * @return true on error, false on success
     */
    public boolean doClient(cmds cmd, String mod, String path, String ns) {
        extMrkLng x = new extMrkLng();
        x.data.add(new extMrkLngEntry(null, "/rpc", namespace + " message-id=\"" + bits.randomD() + "\"", ""));
        int i = path.indexOf("/");
        x.data.add(new extMrkLngEntry(null, "/rpc/" + mod + "/" + path.substring(0, i), "xmlns=\"" + ns + "\"", ""));
        x.data.add(new extMrkLngEntry(null, "/rpc/" + mod + "/" + path, "", ""));
        cmd.error("request");
        dumpXml(cmd, x);
        doSend(x);
        x = doRead();
        if (x == null) {
            return true;
        }
        cmd.error("reply");
        dumpXml(cmd, x);
        return false;
    }

}
