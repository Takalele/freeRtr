package net.freertr.clnt;

import net.freertr.addr.addrIP;
import net.freertr.cfg.cfgSensor;
import net.freertr.pack.packHolder;
import net.freertr.pack.packStreamingMdt;
import net.freertr.pipe.pipeSide;
import net.freertr.serv.servGeneric;
import net.freertr.serv.servStreamingMdt;
import net.freertr.tab.tabGen;
import net.freertr.user.userTerminal;
import net.freertr.util.bits;
import net.freertr.util.counter;
import net.freertr.util.logger;

/**
 * telemetry sender
 *
 * @author matecsaba
 */
public class clntTelemetry implements Runnable {

    /**
     * create instance
     */
    public clntTelemetry() {
    }

    /**
     * target
     */
    public String target;

    /**
     * interval
     */
    public int interval = 5000;

    /**
     * sensors
     */
    public tabGen<cfgSensor> sensors = new tabGen<cfgSensor>();

    /**
     * port
     */
    public int port = servStreamingMdt.port;

    /**
     * proxy
     */
    public clntProxy proxy;

    /**
     * running
     */
    public boolean need2run;

    /**
     * counter
     */
    public counter cntr = new counter();

    private pipeSide pipe;

    /**
     * stop working
     */
    public void stopWork() {
        if (!need2run) {
            return;
        }
        need2run = false;
        if (pipe != null) {
            pipe.setClose();
        }
        pipe = null;
    }

    /**
     * stop working
     */
    public void startWork() {
        if (need2run) {
            return;
        }
        need2run = true;
        new Thread(this).start();
    }

    private void doWork() {
        if (pipe != null) {
            pipe.setClose();
        }
        pipe = null;
        if (proxy == null) {
            return;
        }
        if (target == null) {
            return;
        }
        addrIP trg = userTerminal.justResolv(target, proxy.prefer);
        if (trg == null) {
            return;
        }
        pipe = proxy.doConnect(servGeneric.protoTcp, trg, port, "telemetry");
        if (pipe == null) {
            return;
        }
        pipe.setTime(120000);
        for (;;) {
            bits.sleep(interval);
            if (!need2run) {
                break;
            }
            if (pipe.isClosed() != 0) {
                break;
            }
            for (int i = 0; i < sensors.size(); i++) {
                cfgSensor ntry = sensors.get(i);
                packHolder pck = ntry.getReportKvGpb();
                if (pck == null) {
                    logger.warn("sensor " + ntry.name + " returned nothing");
                    continue;
                }
                sendReport(pck);
            }
        }
    }

    public void run() {
        try {
            for (;;) {
                if (!need2run) {
                    break;
                }
                doWork();
                bits.sleep(1000);
            }
        } catch (Exception e) {
            logger.traceback(e);
        }
    }

    /**
     * send one report
     *
     * @param pck packet, header prepended
     */
    public void sendReport(packHolder pck) {
        if (pipe == null) {
            cntr.drop(pck, counter.reasons.notUp);
            return;
        }
        if (pipe.isClosed() != 0) {
            cntr.drop(pck, counter.reasons.notUp);
            return;
        }
        cntr.tx(pck);
        packStreamingMdt pckPb = new packStreamingMdt(pipe, pck);
        pckPb.typ = 1;
        pckPb.encap = 1;
        pckPb.vers = 1;
        pckPb.flags = 0;
        pckPb.sendPack();
    }

}
