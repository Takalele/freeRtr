package net.freertr.serv;

import java.util.List;
import net.freertr.pipe.pipeConnect;
import net.freertr.pipe.pipeLine;
import net.freertr.pipe.pipeSide;
import net.freertr.prt.prtGenConn;
import net.freertr.prt.prtServS;
import net.freertr.tab.tabGen;
import net.freertr.user.userFilter;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;

/**
 * echo (rfc862) server - stream mode
 *
 * @author matecsaba
 */
public class servEchoS extends servGeneric implements prtServS {

    /**
     * create instance
     */
    public servEchoS() {
    }

    /**
     * port number
     */
    public final static int port = 7;

    /**
     * delay
     */
    public int delay = 0;

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "server echo .*! port " + port,
        "server echo .*! protocol " + proto2string(protoAll),
        "server echo .*! delay 0"
    };

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public tabGen<userFilter> srvDefFlt() {
        return defaultF;
    }

    public boolean srvAccept(pipeSide pipe, prtGenConn id) {
        pipe.setTime(10000);
        pipeConnect.loopback(pipe, delay);
        return false;
    }

    public void srvShRun(String beg, List<String> l, int filter) {
        l.add(beg + "delay " + delay);
    }

    public boolean srvCfgStr(cmds cmd) {
        String a = cmd.word();
        if (a.equals("delay")) {
            delay = bits.str2num(cmd.word());
            return false;
        }
        if (!a.equals("no")) {
            return true;
        }
        a = cmd.word();
        if (a.equals("delay")) {
            delay = 0;
            return false;
        }
        return true;
    }

    public void srvHelp(userHelping l) {
        l.add(null, "1 2        delay          reply delay");
        l.add(null, "2 .          <num>        time in ms");
    }

    public String srvName() {
        return "echo";
    }

    public int srvPort() {
        return port;
    }

    public int srvProto() {
        return protoAll;
    }

    public boolean srvInit() {
        return genStrmStart(this, new pipeLine(32768, false), 0);
    }

    public boolean srvDeinit() {
        return genericStop(0);
    }

}
