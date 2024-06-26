package org.freertr.serv;

import java.util.Comparator;
import java.util.List;
import org.freertr.addr.addrIP;
import org.freertr.cfg.cfgAll;
import org.freertr.cfg.cfgIfc;
import org.freertr.cfg.cfgProxy;
import org.freertr.clnt.clntProxy;
import org.freertr.ip.ipFwdIface;
import org.freertr.pipe.pipeConnect;
import org.freertr.pipe.pipeLine;
import org.freertr.pipe.pipeSide;
import org.freertr.prt.prtGen;
import org.freertr.prt.prtGenConn;
import org.freertr.prt.prtServS;
import org.freertr.tab.tabGen;
import org.freertr.user.userFilter;
import org.freertr.user.userHelping;
import org.freertr.util.bits;
import org.freertr.util.cmds;
import org.freertr.util.logger;

/**
 * load balancer
 *
 * @author matecsaba
 */
public class servLoadBalancer extends servGeneric implements prtServS {

    /**
     * create instance
     */
    public servLoadBalancer() {
    }

    /**
     * port number
     */
    public final static int port = 1;

    /**
     * target proxy
     */
    public clntProxy proxy;

    /**
     * source interface
     */
    public cfgIfc originate;

    /**
     * timeout on connection
     */
    public int timeOut = 60 * 1000;

    /**
     * buffer size
     */
    public int bufSiz = 65536;

    /**
     * logging
     */
    public boolean logging = false;

    /**
     * list of servers
     */
    public tabGen<servLoadBalancerEntry> servLst = new tabGen<servLoadBalancerEntry>();

    /**
     * next server
     */
    public int servNxt = 0;

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "server loadbalancer .*! port " + port,
        "server loadbalancer .*! protocol " + proto2string(protoAllStrm),
        "server loadbalancer .*! no proxy",
        "server loadbalancer .*! no source",
        "server loadbalancer .*! timeout 60000",
        "server loadbalancer .*! buffer 65536",
        "server loadbalancer .*! no logging",};

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public tabGen<userFilter> srvDefFlt() {
        return defaultF;
    }

    public void srvShRun(String beg, List<String> l, int filter) {
        cmds.cfgLine(l, !logging, beg, "logging", "");
        if (proxy == null) {
            l.add(beg + "no proxy");
        } else {
            l.add(beg + "proxy " + proxy.name);
        }
        if (originate == null) {
            l.add(beg + "no source");
        } else {
            l.add(beg + "source " + originate.name);
        }
        for (int i = 0; i < servLst.size(); i++) {
            l.add(beg + "server " + servLst.get(i));
        }
        l.add(beg + "timeout " + timeOut);
        l.add(beg + "buffer " + bufSiz);
    }

    public boolean srvCfgStr(cmds cmd) {
        String a = cmd.word();
        if (a.equals("logging")) {
            logging = true;
            return false;
        }
        if (a.equals("timeout")) {
            timeOut = bits.str2num(cmd.word());
            return false;
        }
        if (a.equals("buffer")) {
            bufSiz = bits.str2num(cmd.word());
            return false;
        }
        if (a.equals("proxy")) {
            cfgProxy p = cfgAll.proxyFind(cmd.word(), false);
            if (p == null) {
                cmd.error("no such proxy");
                return false;
            }
            proxy = p.proxy;
            return false;
        }
        if (a.equals("source")) {
            cfgIfc i = cfgAll.ifcFind(cmd.word(), 0);
            if (i == null) {
                cmd.error("no such interface");
                return false;
            }
            originate = i;
            return false;
        }
        if (a.equals("server")) {
            servLoadBalancerEntry ntry = new servLoadBalancerEntry();
            ntry.num = bits.str2num(cmd.word());
            if (ntry.addr.fromString(cmd.word())) {
                return true;
            }
            ntry.port = bits.str2num(cmd.word());
            servLst.put(ntry);
            return false;
        }
        if (!a.equals(cmds.negated)) {
            return true;
        }
        a = cmd.word();
        if (a.equals("logging")) {
            logging = false;
            return false;
        }
        if (a.equals("proxy")) {
            proxy = null;
            return false;
        }
        if (a.equals("source")) {
            originate = null;
            return false;
        }
        if (a.equals("server")) {
            servLoadBalancerEntry ntry = new servLoadBalancerEntry();
            ntry.num = bits.str2num(cmd.word());
            servLst.del(ntry);
            return false;
        }
        return false;
    }

    public void srvHelp(userHelping l) {
        l.add(null, "1 .  logging                      set logging");
        l.add(null, "1 2  timeout                      set timeout on connection");
        l.add(null, "2 .    <num>                      timeout in ms");
        l.add(null, "1 2  buffer                       set buffer size on connection");
        l.add(null, "2 .    <num>                      buffer in bytes");
        l.add(null, "1 2  proxy                        set proxy to use");
        l.add(null, "2 .    <name:prx>                 name of proxy");
        l.add(null, "1 2  source                       set source interface");
        l.add(null, "2 .    <name:ifc>                 name of interface");
        l.add(null, "1 2  server                       name of server");
        l.add(null, "2 3    <num>                      number of server");
        l.add(null, "3 4      <addr>                   address of server");
        l.add(null, "4 .        <port>                 port on server");
    }

    public String srvName() {
        return "loadbalancer";
    }

    public int srvPort() {
        return port;
    }

    public int srvProto() {
        return protoAllStrm;
    }

    public boolean srvInit() {
        dynBlckMod = true;
        return genStrmStart(this, new pipeLine(bufSiz, false), 0);
    }

    public boolean srvDeinit() {
        return genericStop(0);
    }

    public boolean srvAccept(pipeSide pipe, prtGenConn id) {
        if (logging) {
            logger.info("connection from " + id.peerAddr);
        }
        pipe.setTime(timeOut);
        new servLoadBalancerDoer(this, pipe);
        return false;
    }

    /**
     * start one connection
     *
     * @param con1 incoming connection
     * @return false on success, true on error
     */
    public boolean doConnStart(pipeSide con1) {
        con1.setTime(timeOut);
        con1.wait4ready(timeOut);
        servLoadBalancerEntry ntry = null;
        int o = servLst.size();
        long p = bits.getTime();
        for (int i = 0; i < o; i++) {
            servNxt = (servNxt + 1) % o;
            ntry = servLst.get(servNxt);
            if (ntry.bad == 0) {
                break;
            }
            if ((p - ntry.bad) < timeOut) {
                ntry = null;
                continue;
            }
            ntry.bad = 0;
            break;
        }
        if (ntry == null) {
            logger.warn("no available server found");
            return true;
        }
        pipeSide con2 = null;
        if (proxy != null) {
            con2 = proxy.doConnect(srvProto, ntry.addr, ntry.port, srvName());
        } else {
            if (srvVrf == null) {
                return true;
            }
            prtGen prt = getProtocol(srvVrf, srvProto, ntry.addr);
            if (prt == null) {
                return true;
            }
            ipFwdIface ifc = null;
            if (originate != null) {
                ifc = originate.getFwdIfc(ntry.addr);
            }
            con2 = prt.streamConnect(new pipeLine(bufSiz, con1.isBlockMode()), ifc, 0, ntry.addr, ntry.port, srvName(), -1, null, -1, -1);
        }
        if (con2 == null) {
            logger.warn("server " + ntry.num + " marked bad");
            ntry.bad = bits.getTime();
            return true;
        }
        con2.setTime(timeOut);
        if (con2.wait4ready(timeOut)) {
            ntry.bad = bits.getTime();
            return true;
        }
        pipeConnect.connect(con1, con2, true);
        return false;
    }

}

class servLoadBalancerEntry implements Comparator<servLoadBalancerEntry> {

    public int num;

    public addrIP addr = new addrIP();

    public int port;

    public long bad = 0;

    public String toString() {
        return num + " " + addr + " " + port;
    }

    public int compare(servLoadBalancerEntry o1, servLoadBalancerEntry o2) {
        if (o1.num < o2.num) {
            return -1;
        }
        if (o1.num > o2.num) {
            return +1;
        }
        return 0;
    }

}

class servLoadBalancerDoer implements Runnable {

    private pipeSide pipe;

    private servLoadBalancer parent;

    public servLoadBalancerDoer(servLoadBalancer prnt, pipeSide stream) {
        parent = prnt;
        pipe = stream;
        new Thread(this).start();
    }

    public void run() {
        try {
            if (parent.doConnStart(pipe)) {
                pipe.setClose();
            }
        } catch (Exception e) {
            pipe.setClose();
            logger.traceback(e);
        }
    }

}
