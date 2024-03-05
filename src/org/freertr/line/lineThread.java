package org.freertr.line;

import org.freertr.pipe.pipeLine;
import org.freertr.pipe.pipeSide;
import org.freertr.util.debugger;
import org.freertr.util.logger;

/**
 * one line handler thread
 *
 * @author matecsaba
 */
public abstract class lineThread {

    /**
     * create instance
     */
    public lineThread() {
    }

    /**
     * data carrier detect
     */
    public final static int signalDCDi = 0x1;

    /**
     * ring indicator
     */
    public final static int signalRIi = 0x2;

    /**
     * data set ready
     */
    public final static int signalDSRi = 0x10;

    /**
     * data terminal ready
     */
    public final static int signalDTRo = 0x20;

    /**
     * clear to send
     */
    public final static int signalCTSi = 0x100;

    /**
     * ready to send
     */
    public final static int signalRTSo = 0x200;

    /**
     * all input signals
     */
    public final static int ctrlAllIn = signalDSRi | signalCTSi | signalDCDi | signalRIi;

    /**
     * all output signals
     */
    public final static int ctrlAllOut = signalDTRo | signalRTSo;

    /**
     * presence signals
     */
    public final static int ctrlDtrRsr = signalDTRo | signalDSRi;

    /**
     * flow control signals
     */
    public final static int ctrlRtsCts = signalRTSo | signalCTSi;

    /**
     * all signals
     */
    public final static int ctrlAll = ctrlAllIn | ctrlAllOut;

    /**
     * pipe line handler
     */
    protected pipeLine pipe;

    /**
     * server side of pipe
     */
    protected pipeSide pipS;

    /**
     * client side of pipe
     */
    protected pipeSide pipC;

    /**
     * set control bits
     *
     * @param ctrl control bits
     */
    protected abstract void txCtrlBit(int ctrl);

    /**
     * set flow control
     *
     * @param ctrl control bits
     */
    protected abstract void txFlowCtrl(int ctrl);

    /**
     * close the connection
     */
    protected abstract void rxtxClose();

    /**
     * open the connection
     */
    protected abstract void rxtxOpen();

    /**
     * make new pipelines
     *
     * @return pipe side of client
     */
    public synchronized pipeSide getPipe() {
        if (debugger.lineThreadTraf) {
            logger.debug("reopening session");
        }
        rxtxClose();
        pipe = new pipeLine(65536, false);
        pipS = pipe.getSide();
        pipC = pipe.getSide();
        pipS.setReady();
        rxtxOpen();
        return pipC;
    }

}
