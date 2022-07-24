package net.freertr.clnt;

import net.freertr.addr.addrIP;
import net.freertr.ip.ipFwdIface;
import net.freertr.pipe.pipeLine;
import net.freertr.pipe.pipeSide;
import net.freertr.prt.prtUdp;
import net.freertr.serv.servEchoS;
import net.freertr.tab.tabAverage;
import net.freertr.util.bits;
import net.freertr.util.logger;
import net.freertr.util.notifier;

/**
 * echo (rfc862) client
 *
 * @author matecsaba
 */
public class clntEcho implements Runnable {

    /**
     * create instance
     */
    public clntEcho() {
    }

    /**
     * measurement
     */
    public tabAverage meas;

    /**
     * notifier
     */
    public notifier notif;

    /**
     * udp
     */
    public prtUdp udp;

    /**
     * source
     */
    public ipFwdIface src;

    /**
     * target
     */
    public addrIP trg;

    /**
     * timeout
     */
    public int timeout = 1000;

    /**
     * size
     */
    public int size = 64;

    /**
     * ttl
     */
    public int tim2liv = 255;

    /**
     * tos
     */
    public int typOsrv = 0;

    /**
     * data pattern
     */
    public int datPat = 0;

    /**
     * do work
     */
    public void doWork() {
        new Thread(this).start();
    }

    public void run() {
        try {
            pipeSide pip = udp.streamConnect(new pipeLine(65536, true), src, 0, trg, servEchoS.port, "echo", -1, null, tim2liv, typOsrv);
            if (pip == null) {
                return;
            }
            pip.wait4ready(timeout);
            pip.setTime(timeout);
            byte[] buf = new byte[size];
            bits.byteFill(buf, 0, buf.length, datPat);
            long beg = bits.getTime();
            pip.blockingPut(buf, 0, buf.length);
            int len = pip.blockingGet(buf, 0, buf.length);
            pip.setClose();
            beg = bits.getTime() - beg;
            if (len != buf.length) {
                return;
            }
            if (meas != null) {
                meas.addValue((int) beg);
            }
            if (notif != null) {
                notif.wakeup();
            }
        } catch (Exception e) {
            logger.traceback(e);
        }
    }

}
