package net.freertr.serv;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import net.freertr.cfg.cfgAll;
import net.freertr.cfg.cfgProxy;
import net.freertr.clnt.clntProxy;
import net.freertr.cry.cryHashCrc32;
import net.freertr.pipe.pipeLine;
import net.freertr.pipe.pipeSide;
import net.freertr.prt.prtGenConn;
import net.freertr.prt.prtServS;
import net.freertr.enc.encUrl;
import net.freertr.tab.tabGen;
import net.freertr.user.userFilter;
import net.freertr.user.userFormat;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;

/**
 * hypertext transfer protocol (rfc2616) server
 *
 * @author matecsaba
 */
public class servHttp extends servGeneric implements prtServS {

    /**
     * create instance
     */
    public servHttp() {
    }

    /**
     * port number
     */
    public static final int clearPort = 80;

    /**
     * secure port
     */
    public static final int securePort = 443;

    /**
     * html 401 transitive
     */
    public static final String htmlHead = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n";

    /**
     * list of hosts
     */
    protected tabGen<servHttpHost> hosts = new tabGen<servHttpHost>();

    /**
     * proxy to use
     */
    protected clntProxy proxy;

    /**
     * second port to use
     */
    protected int secondPort = -1;

    /**
     * error message
     */
    protected String error;

    /**
     * single request
     */
    protected boolean singleRequest;

    /**
     * default server path
     */
    protected String defPath = defHostPat;

    /**
     * default subconnect
     */
    protected int defSubcon;

    /**
     * buffer size
     */
    protected int bufSiz = 65536;

    /**
     * default host path
     */
    public final static String defHostPat = "/data/notfound/";

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "server http .*! port " + clearPort,
        "server http .*! protocol " + proto2string(protoAllStrm),
        "server http .*! no proxy",
        "server http .*! no error",
        "server http .*! no single-request",
        "server http .*! def-path " + defHostPat,
        "server http .*! def-subconn",
        "server http .*! buffer 65536",
        "server http .*! no second-port",};

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    /**
     * get defaults filter
     *
     * @return filter
     */
    public tabGen<userFilter> srvDefFlt() {
        return defaultF;
    }

    /**
     * get gzip header
     *
     * @return header
     */
    public static byte[] getGzipHdr() {
        byte[] res = new byte[10];
        res[0] = 0x1f; // magic
        res[1] = (byte) 0x8b; // magic
        res[2] = Deflater.DEFLATED; // deflate
        res[3] = 0; // flags
        res[4] = 0; // mtime
        res[5] = 0; // mtime
        res[6] = 0; // mtime
        res[7] = 0; // mtime
        res[8] = 0; // extra flags
        res[9] = (byte) 0xff; // os
        return res;
    }

    /**
     * get gzip trailer
     *
     * @param unc uncompressed data
     * @return trailer
     */
    public static byte[] getGzipTrl(byte[] unc) {
        byte[] res = new byte[8];
        cryHashCrc32 crc = new cryHashCrc32(cryHashCrc32.polyCrc32i);
        crc.init();
        crc.update(unc);
        bits.lsbPutD(res, 0, bits.msbGetD(crc.finish(), 0));
        bits.lsbPutD(res, 4, unc.length);
        return res;
    }

    /**
     * find one host
     *
     * @param s host name
     * @return host descriptor, null if not found
     */
    protected servHttpHost findHost(String s) {
        servHttpHost ntry = new servHttpHost(s);
        ntry = hosts.find(ntry);
        if (ntry != null) {
            return ntry;
        }
        for (int i = hosts.size() - 1; i >= 0; i--) {
            ntry = hosts.get(i);
            if (!ntry.host.startsWith("*")) {
                continue;
            }
            if (!s.endsWith(ntry.host.substring(1, ntry.host.length()))) {
                continue;
            }
            return ntry;
        }
        return null;
    }

    /**
     * start connection
     *
     * @param pipe pipeline
     * @param id connection
     * @return false on success, true on error
     */
    public boolean srvAccept(pipeSide pipe, prtGenConn id) {
        pipe.setTime(120000);
        new servHttpConn(this, pipe, id);
        return false;
    }

    /**
     * get config
     *
     * @param beg beginning
     * @param l list
     * @param filter default filter
     */
    public void srvShRun(String beg, List<String> l, int filter) {
        if (proxy == null) {
            l.add(beg + "no proxy");
        } else {
            l.add(beg + "proxy " + proxy.name);
        }
        if (error == null) {
            l.add(beg + "no error");
        } else {
            l.add(beg + "error " + error);
        }
        cmds.cfgLine(l, secondPort < 0, beg, "second-port", "" + secondPort);
        l.add(beg + "buffer " + bufSiz);
        l.add(beg + "def-path " + defPath);
        l.add(beg + "def-subconn" + servHttpHost.subconn2string(defSubcon));
        cmds.cfgLine(l, !singleRequest, beg, "single-request", "");
        for (int hn = 0; hn < hosts.size(); hn++) {
            servHttpHost ntry = hosts.get(hn);
            if (ntry == null) {
                continue;
            }
            ntry.getConfig(beg, l, filter);
        }
    }

    /**
     * configure
     *
     * @param cmd command
     * @return false on success, true on error
     */
    public boolean srvCfgStr(cmds cmd) {
        String a = cmd.word();
        boolean negated = false;
        if (a.equals("no")) {
            negated = true;
            a = cmd.word();
        }
        if (a.equals("second-port")) {
            if (negated) {
                secondPort = -1;
                return false;
            }
            secondPort = bits.str2num(cmd.word());
            return false;
        }
        if (a.equals("buffer")) {
            bufSiz = bits.str2num(cmd.word());
            return false;
        }
        if (a.equals("single-request")) {
            singleRequest = !negated;
            return false;
        }
        if (a.equals("def-path")) {
            if (negated) {
                defPath = defHostPat;
            } else {
                defPath = cmd.word();
            }
            return false;
        }
        if (a.equals("def-subconn")) {
            defSubcon = servHttpHost.string2subconn(negated, cmd);
            return false;
        }
        if (a.equals("proxy")) {
            if (negated) {
                proxy = null;
                return false;
            }
            cfgProxy prx = cfgAll.proxyFind(cmd.word(), false);
            if (prx == null) {
                cmd.error("no such proxy");
                return false;
            }
            proxy = prx.proxy;
            return false;
        }
        if (a.equals("error")) {
            error = cmd.getRemaining();
            if (negated) {
                error = null;
                return false;
            }
            return false;
        }
        if (!a.equals("host")) {
            return true;
        }
        servHttpHost ntry = new servHttpHost(cmd.word());
        servHttpHost old = hosts.add(ntry);
        if (old != null) {
            ntry = old;
        }
        a = cmd.word();
        if (a.length() < 1) {
            return false;
        }
        if (a.equals("path")) {
            if (negated) {
                hosts.del(ntry);
                return false;
            }
            ntry.path = "/" + encUrl.normalizePath(cmd.word() + "/");
            return false;
        }
        if (ntry.path == null) {
            ntry.path = defPath;
            ntry.subconn = defSubcon;
        }
        return ntry.doConfig(negated, a, cmd);
    }

    private static final void getSubconnHelp(String b, userHelping l) {
        l.add(null, b + "     strip-path               strip path");
        l.add(null, b + "     strip-name               strip filename");
        l.add(null, b + "     strip-ext                strip extension");
        l.add(null, b + "     strip-param              strip parameters");
        l.add(null, b + "     keep-cred                keep credentinals");
        l.add(null, b + "     keep-host                keep hostname");
        l.add(null, b + "     keep-path                append path");
    }

    /**
     * get help
     *
     * @param l list
     */
    public void srvHelp(userHelping l) {
        l.add(null, "1 .  single-request                 one request per connection");
        l.add(null, "1 2  buffer                         set buffer size on connection");
        l.add(null, "2 .    <num>                        buffer in bytes");
        l.add(null, "1 2  proxy                          enable proxy support");
        l.add(null, "2 .    <name:prx>                   proxy profile");
        l.add(null, "1 2  second-port                    enable dual binding");
        l.add(null, "2 .    <num>                        secure port");
        l.add(null, "1 2  def-path                       set host default path");
        l.add(null, "2 .    <str>                        path on the disk");
        l.add(null, "1 2  def-subconn                    set host default subconnect");
        getSubconnHelp("2 2,. ", l);
        l.add(null, "1 2  error                          set error message");
        l.add(null, "2 2,.  <str>                        error message");
        l.add(null, "1 2  host                           define one virtual server");
        List<String> lst = new ArrayList<String>();
        for (int i = 0; i < hosts.size(); i++) {
            lst.add(hosts.get(i).host);
        }
        l.add(lst, "2 3     <name:loc>                   name of server, * for any");
        l.add(null, "3 4      path                       set server root");
        l.add(null, "4 .        <str>                    root directory of server");
        l.add(null, "3 4      redir                      set redirect path");
        l.add(null, "4 .        <url>                    url to redirect to");
        l.add(null, "3 .      logging                    log to syslog");
        l.add(null, "3 4      reconn                     reconnect to server");
        l.add(null, "4 5        <name:prx>               proxy profile");
        l.add(null, "5 .          <str>                  server to redirect to");
        l.add(null, "3 4      translate                  translate the url");
        l.add(null, "4 4,.      <num:trn>                translation rule to use");
        l.add(null, "3 4      subconn                    reconnect only to the url");
        getSubconnHelp("4 4,. ", l);
        l.add(null, "3 4      stream                     stream from server");
        l.add(null, "4 5        <str>                    content type");
        l.add(null, "5 6          <name:prx>             proxy profile");
        l.add(null, "6 .            <str>                server to stream from");
        l.add(null, "3 4      multiacc                   access multiple servers");
        l.add(null, "4 5        <name:prx>               proxy profile");
        l.add(null, "5 5,.        <str>                  server to access");
        l.add(null, "3 .      markdown                   allow markdown conversion");
        l.add(null, "3 .      noindex                    disallow index for directory");
        l.add(null, "3 4      speed-limit                limit download speeds");
        l.add(null, "4 .        <num>                    bytes per second");
        l.add(null, "3 4,.    dirlist                    allow directory listing");
        l.add(null, "4 4,.      readme                   put readme in front of listing");
        l.add(null, "4 4,.      stats                    put statistics after listing");
        l.add(null, "3 4,.    script                     allow script running");
        l.add(null, "4 4,.      exec                     allow exec commands");
        l.add(null, "4 4,.      config                   allow config commands");
        l.add(null, "3 4,.    api                        allow api calls");
        l.add(null, "4 4,.      exec                     allow exec commands");
        l.add(null, "4 4,.      config                   allow config commands");
        l.add(null, "4 4,.      ipinfo                   allow ip info commands");
        l.add(null, "3 4      search-script              allow scripts defined in configuration");
        l.add(null, "4 .        <str>                    prefix");
        l.add(null, "3 .      imagemap                   allow image map processing");
        l.add(null, "3 .      websock                    allow websocket processing");
        l.add(null, "3 .      webdav                     allow webdav processing");
        l.add(null, "3 .      mediastream                allow media streaming");
        l.add(null, "3 .      class                      allow class running");
        l.add(null, "3 .      upload                     allow upload files");
        l.add(null, "3 4      backup                     backup uploaded files");
        l.add(null, "4 5        <num>                    number of backups to keep");
        l.add(null, "5 .          <str>                  root directory of backup");
        l.add(null, "3 4      sstp                       allow sstp clients");
        l.add(null, "4 .        <name:ifc>               name of interface");
        l.add(null, "3 4      anyconn                    allow anyconnect clients");
        l.add(null, "4 .        <name:ifc>               name of interface");
        l.add(null, "3 4      forti                      allow fortinet clients");
        l.add(null, "4 .        <name:ifc>               name of interface");
        l.add(null, "3 4      authentication             require authentication to access");
        l.add(null, "4 .        <name:aaa>               authentication list");
        l.add(null, "3 4      access-class               require ip to access");
        l.add(null, "4 .        <name:acl>               access list");
        l.add(null, "3 4      style                      set page style tags");
        l.add(null, "4 4,.      <text>                   text to send");
    }

    /**
     * get name
     *
     * @return name
     */
    public String srvName() {
        return "http";
    }

    /**
     * get port
     *
     * @return port
     */
    public int srvPort() {
        return clearPort;
    }

    /**
     * get protocol
     *
     * @return protocol
     */
    public int srvProto() {
        return protoAllStrm;
    }

    /**
     * initialize
     *
     * @return false on success, true on error
     */
    public boolean srvInit() {
        if (secondPort > 0) {
            if (genStrmStart(this, new pipeLine(bufSiz, false), secondPort)) {
                return true;
            }
        }
        return genStrmStart(this, new pipeLine(bufSiz, false), 0);
    }

    /**
     * deinitialize
     *
     * @return false on success, true on error
     */
    public boolean srvDeinit() {
        if (secondPort > 0) {
            if (genericStop(secondPort)) {
                return true;
            }
        }
        return genericStop(0);
    }

    /**
     * get show
     *
     * @return result
     */
    public userFormat getShow() {
        userFormat res = new userFormat("|", "host|hit|last");
        for (int i = 0; i < hosts.size(); i++) {
            servHttpHost ntry = hosts.get(i);
            res.add(ntry.host + "|" + ntry.askNum + "|" + bits.timePast(ntry.askTim));
        }
        return res;
    }

}
