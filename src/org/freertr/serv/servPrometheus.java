package org.freertr.serv;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import org.freertr.cfg.cfgAll;
import org.freertr.cfg.cfgSensor;
import org.freertr.pipe.pipeLine;
import org.freertr.pipe.pipeSide;
import org.freertr.prt.prtGenConn;
import org.freertr.prt.prtServS;
import org.freertr.enc.encUrl;
import org.freertr.tab.tabGen;
import org.freertr.user.userFilter;
import org.freertr.user.userFlash;
import org.freertr.user.userHelp;
import org.freertr.util.bits;
import org.freertr.util.cmds;
import org.freertr.util.debugger;
import org.freertr.util.logger;

/**
 * prometheus server
 *
 * @author matecsaba
 */
public class servPrometheus extends servGeneric implements prtServS {

    /**
     * create instance
     */
    public servPrometheus() {
    }

    /**
     * default port
     */
    public final static int port = 9001;

    /**
     * list of metrics
     */
    public tabGen<cfgSensor> sensors = new tabGen<cfgSensor>();

    /**
     * all the metrics
     */
    public String allMets = "metrics";

    /**
     * number of queries
     */
    public int allNum;

    /**
     * time to respond
     */
    public int allTim;

    /**
     * last response
     */
    public long allLast;

    /**
     * defaults text
     */
    public final static userFilter[] defaultF = {
        new userFilter("server prometheus .*", cmds.tabulator + "port " + port, null),
        new userFilter("server prometheus .*", cmds.tabulator + "protocol " + proto2string(protoAllStrm), null),
        new userFilter("server prometheus .*", cmds.tabulator + "all-metrics metrics", null)
    };

    public userFilter[] srvDefFlt() {
        return defaultF;
    }

    public String srvName() {
        return "prometheus";
    }

    public int srvPort() {
        return port;
    }

    public int srvProto() {
        return protoAllStrm;
    }

    public boolean srvInit() {
        return genStrmStart(this, new pipeLine(65536, false), 0);
    }

    public boolean srvDeinit() {
        return genericStop(0);
    }

    public void srvShRun(String beg, List<String> lst, int filter) {
        lst.add(beg + "all-metrics " + allMets);
        for (int p = 0; p < sensors.size(); p++) {
            cfgSensor met = sensors.get(p);
            if (met == null) {
                continue;
            }
            lst.add(beg + "sensor " + met.name);
        }
    }

    public boolean srvCfgStr(cmds cmd) {
        String s = cmd.word();
        boolean negated = s.equals(cmds.negated);
        if (negated) {
            s = cmd.word();
        }
        if (s.equals("all-metrics")) {
            allMets = cmd.word();
            return false;
        }
        if (s.equals("sensor")) {
            cfgSensor tl = cfgAll.sensorFind(cmd.word(), false);
            if (tl == null) {
                cmd.error("no such sensor");
                return false;
            }
            if (negated) {
                sensors.del(tl);
            } else {
                sensors.add(tl);
            }
            return false;
        }
        return true;
    }

    public void srvHelp(userHelp l) {
        l.add(null, false, 1, new int[]{2}, "all-metrics", "configure whole exporter");
        l.add(null, false, 2, new int[]{-1}, "<str>", "name to use");
        l.add(null, false, 1, new int[]{2}, "sensor", "configure one metric");
        l.add(null, false, 2, new int[]{-1}, "<name:sns>", "name of metric");
    }

    public boolean srvAccept(pipeSide pipe, prtGenConn id) {
        pipe.setTime(120000);
        new servPrometheusConn(this, pipe);
        return false;
    }

}

class servPrometheusConn implements Runnable {

    private servPrometheus lower;

    private pipeSide conn;

    private boolean gotCompr;

    public servPrometheusConn(servPrometheus parent, pipeSide pipe) {
        lower = parent;
        conn = pipe;
        new Thread(this).start();
    }

    private void sendReply(String hdr, List<String> res) {
        if (debugger.servPrometheusTraf) {
            logger.debug("tx " + hdr);
        }
        conn.linePut("HTTP/1.1 " + hdr);
        conn.linePut("Content-Type: text/plain");
        conn.linePut("Date: " + bits.time2str(cfgAll.timeZoneName, bits.getTime() + cfgAll.timeServerOffset, 4));
        if (res == null) {
            conn.linePut("Content-Length: 0");
            conn.linePut("");
            return;
        }
        byte[] buf1 = new byte[0];
        byte[] buf3 = new byte[1];
        buf3[0] = 10;
        for (int i = 0; i < res.size(); i++) {
            buf1 = bits.byteConcat(buf1, res.get(i).getBytes());
            buf1 = bits.byteConcat(buf1, buf3);
        }
        if (!gotCompr) {
            conn.linePut("Content-Length: " + buf1.length);
            conn.linePut("");
            conn.morePut(buf1, 0, buf1.length);
            return;
        }
        byte[] buf2 = new byte[buf1.length];
        Deflater cmp = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        cmp.setInput(buf1);
        cmp.finish();
        int i = cmp.deflate(buf2);
        if (i >= buf2.length) {
            conn.linePut("Content-Length: " + buf1.length);
            conn.linePut("");
            conn.morePut(buf1, 0, buf1.length);
            return;
        }
        buf3 = userFlash.getGzipHdr();
        byte[] buf4 = userFlash.getGzipTrl(buf1);
        conn.linePut("Content-Encoding: gzip");
        conn.linePut("Content-Length: " + (buf3.length + i + buf4.length));
        conn.linePut("");
        conn.morePut(buf3, 0, buf3.length);
        conn.morePut(buf2, 0, i);
        conn.morePut(buf4, 0, buf4.length);
    }

    private boolean doWork() {
        conn.lineRx = pipeSide.modTyp.modeCRtryLF;
        conn.lineTx = pipeSide.modTyp.modeCRLF;
        String gotCmd = conn.lineGet(1);
        gotCompr = false;
        if (debugger.servPrometheusTraf) {
            logger.debug("rx " + gotCmd);
        }
        if (gotCmd.length() < 1) {
            return true;
        }
        for (;;) {
            String a = conn.lineGet(1);
            if (a.length() < 1) {
                break;
            }
            a = a.toLowerCase();
            if (a.startsWith("accept-encoding")) {
                if (a.indexOf("gzip") >= 0) {
                    gotCompr = true;
                }
                continue;
            }
        }
        encUrl gotUrl = new encUrl();
        int i = gotCmd.toLowerCase().lastIndexOf(" http/");
        if (i > 0) {
            gotCmd = gotCmd.substring(0, i);
        }
        i = gotCmd.indexOf(" ");
        if (i < 0) {
            sendReply("501 bad request", null);
            return true;
        }
        String s = gotCmd.substring(i + 1, gotCmd.length());
        gotCmd = gotCmd.substring(0, i);
        gotUrl.fromString(s);
        cfgSensor ntry = cfgAll.sensorFind(gotUrl.filName, false);
        if (ntry != null) {
            List<String> res = ntry.getReportProm();
            sendReply("200 ok", res);
            return false;
        }
        if (!gotUrl.filName.equals(lower.allMets)) {
            sendReply("404 no such metric", null);
            return false;
        }
        long tim = bits.getTime();
        List<String> res = new ArrayList<String>();
        for (i = 0; i < lower.sensors.size(); i++) {
            ntry = lower.sensors.get(i);
            res.addAll(ntry.getReportProm());
        }
        lower.allTim = (int) (bits.getTime() - tim);
        lower.allLast = tim;
        lower.allNum++;
        sendReply("200 ok", res);
        return false;
    }

    public void run() {
        try {
            for (;;) {
                if (doWork()) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.traceback(e);
        }
        conn.setClose();

    }

}
