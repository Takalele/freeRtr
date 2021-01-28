package prt;

import addr.addrMac;
import cfg.cfgAll;
import cfg.cfgInit;
import ifc.ifcDn;
import ifc.ifcThread;
import ifc.ifcUp;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import pack.packHolder;
import pack.packRedundancy;
import pipe.pipeSide;
import user.userFlash;
import user.userFormat;
import util.bits;
import util.counter;
import util.debugger;
import util.logger;
import util.notifier;
import util.state;
import util.syncInt;
import util.version;

/**
 * redundancy protocol
 *
 * @author matecsaba
 */
public class prtRedun implements Runnable {

    /**
     * my magic number
     */
    protected static int magic = (bits.randomD() & 0x3fffffff) + 1;

    /**
     * current state
     */
    protected static int state = packRedundancy.statInit;

    /**
     * current uptime
     */
    protected static int uptime = 0;

    private final static List<prtRedunIfc> ifaces = new ArrayList<prtRedunIfc>();

    public void run() {
        try {
            packHolder pck = new packHolder(true, true);
            for (;;) {
                long tim = bits.getTime();
                uptime = (int) ((tim - cfgInit.started) / 1000);
                for (int i = 0; i < ifaces.size(); i++) {
                    pck.clear();
                    prtRedunIfc ifc = ifaces.get(i);
                    ifc.doPack(packRedundancy.typHello, pck);
                    if ((tim - ifc.heard) < cfgAll.redundancyHold) {
                        continue;
                    }
                    ifc.reach.set(0);
                }
                bits.sleep(cfgAll.redundancyKeep);
            }
        } catch (Exception e) {
            logger.exception(e);
        }
    }

    /**
     * generate show output
     *
     * @return output
     */
    public static userFormat doShow() {
        userFormat l = new userFormat("|", "iface|state|prio|reach|magic|uptime|heard");
        l.add("self|" + state + "|" + cfgInit.redunPrio + "|-|" + magic + "|" + bits.timeDump(uptime) + "|-");
        for (int i = 0; i < ifaces.size(); i++) {
            prtRedunIfc ifc = ifaces.get(i);
            l.add(ifc.name + "|" + ifc.last.state + "|" + ifc.last.priority + "|" + ifc.reach + "|" + ifc.last.magic + "|" + bits.timeDump(ifc.last.uptime) + "|" + bits.timePast(ifc.heard));
        }
        return l;
    }

    /**
     * add one physical interface
     *
     * @param name name of interface
     * @param thrd interface thread handler
     */
    public static void ifcAdd(String name, ifcThread thrd) {
        prtRedunIfc ifc = new prtRedunIfc();
        ifc.doInit(name, thrd);
        ifaces.add(ifc);
    }

    /**
     * fill in generic parts
     *
     * @return get self packet
     */
    public static packRedundancy getSelf() {
        packRedundancy pckP = new packRedundancy();
        pckP.state = prtRedun.state;
        pckP.magic = prtRedun.magic;
        pckP.uptime = prtRedun.uptime;
        pckP.priority = cfgInit.redunPrio;
        return pckP;
    }

    /**
     * find best peer
     *
     * @return best peer index, -1 if none
     */
    public static int findActive() {
        packRedundancy bst = getSelf();
        int idx = -1;
        for (int i = 0; i < ifaces.size(); i++) {
            prtRedunIfc ifc = ifaces.get(i);
            if (ifc.reach.get() != 3) {
                continue;
            }
            if (ifc.last.state == packRedundancy.statActive) {
                return i;
            }
            String a = bst.otherBetter(ifc.last);
            if (a == null) {
                continue;
            }
            idx = i;
            bst = ifc.last;
        }
        return idx;
    }

    /**
     * terminate the redundancy
     */
    public static void doShut() {
    }

    /**
     * initialize the redundancy
     *
     * @param con console
     */
    public static void doInit(pipeSide con) {
        if (ifaces.size() < 1) {
            state = packRedundancy.statActive;
            return;
        }
        logger.info("initializing redundancy");
        state = packRedundancy.statSpeak;
        new Thread(new prtRedun()).start();
        bits.sleep(cfgAll.redundancyInit);
        int act = findActive();
        if (act < 0) {
            state = packRedundancy.statActive;
            logger.info("became active");
            return;
        }
        state = packRedundancy.statStandby;
        logger.info("became standby, active on " + ifaces.get(act));
        for (;;) {
            bits.sleep(cfgAll.redundancyKeep);
            int i = findActive();
            if (i < 0) {
                break;
            }
            if (i != act) {
                act = i;
                logger.info("active changed to " + ifaces.get(act));
            }
            if (con == null) {
                continue;
            }
            if (con.ready2rx() < 1) {
                continue;
            }
            byte[] buf = new byte[256];
            con.nonBlockGet(buf, 0, buf.length);
            con.linePut("this node is standby");
        }
        state = packRedundancy.statActive;
        logger.info("lost active");
    }

    /**
     * sync config to peers
     */
    public static void doConfig() {
        for (int i = 0; i < ifaces.size(); i++) {
            ifaces.get(i).doFile(cfgInit.cfgFileSw, packRedundancy.fnStart);
        }
    }

    /**
     * reload peers
     */
    public static void doReload() {
        for (int i = 0; i < ifaces.size(); i++) {
            ifaces.get(i).doRetry(packRedundancy.typReload, new packHolder(true, true));
        }
    }

}

class prtRedunIfc implements ifcUp {

    private ifcThread lower;

    private counter cntr = new counter();

    private addrMac hwaddr;

    private RandomAccessFile filRx;

    private String filNm;

    public String name;

    public final syncInt reach = new syncInt(0);

    public packRedundancy last = new packRedundancy();

    public long heard;

    public int dualAct;

    public notifier notif = new notifier();

    public int ackRx;

    public String toString() {
        return "" + name;
    }

    public void doInit(String nam, ifcThread thrd) {
        reach.set(0);
        name = nam;
        lower = thrd;
        last.state = packRedundancy.statInit;
        heard = 0;
        dualAct = 0;
        lower.setFilter(false);
        lower.setUpper(this);
        lower.startLoop(1);
        hwaddr = (addrMac) lower.getHwAddr();
        filNm = version.myWorkDir() + "red" + bits.randomD() + ".tmp";
    }

    public void setParent(ifcDn parent) {
    }

    public void setState(state.states stat) {
    }

    public void closeUp() {
    }

    public counter getCounter() {
        return cntr;
    }

    public void recvPack(packHolder pck) {
        packRedundancy pckP = new packRedundancy();
        if (pckP.parseHeader(pck)) {
            return;
        }
        if (debugger.prtRedun) {
            logger.debug("rx " + pckP);
        }
        last = pckP;
        if (pckP.peer == prtRedun.magic) {
            reach.set(3);
        } else {
            reach.set(1);
        }
        heard = bits.getTime();
        switch (pckP.type) {
            case packRedundancy.typHello:
                if ((last.magic == prtRedun.magic) && (last.state >= prtRedun.state)) {
                    cfgInit.stopRouter(true, 6, "magic collision");
                }
                if (prtRedun.state != packRedundancy.statActive) {
                    dualAct = 0;
                    break;
                }
                if (last.state != packRedundancy.statActive) {
                    dualAct = 0;
                    break;
                }
                String a = prtRedun.getSelf().otherBetter(pckP);
                if (a != null) {
                    cfgInit.stopRouter(true, 9, "dual active, reloading myself because peer " + a);
                }
                logger.warn("dual active, reloading unresponsive peer");
                dualAct++;
                if (dualAct < 5) {
                    break;
                }
                doPack(packRedundancy.typReload, new packHolder(true, true));
                break;
            case packRedundancy.typReload:
                if (prtRedun.state == packRedundancy.statActive) {
                    break;
                }
                doAck(-4);
                cfgInit.stopRouter(true, 10, "peer request");
                break;
            case packRedundancy.typAck:
                ackRx = pck.msbGetD(0);
                notif.wakeup();
                break;
            case packRedundancy.typFilBeg:
                try {
                filRx.close();
            } catch (Exception e) {
            }
            try {
                filRx = new RandomAccessFile(filNm, "rw");
                filRx.seek(0);
                filRx.setLength(0);
            } catch (Exception e) {
                logger.error("unable to open file");
                break;
            }
            doAck(-2);
            break;
            case packRedundancy.typFilDat:
                int i = pck.msbGetD(0);
                int o = pck.msbGetW(4);
                pck.getSkip(6);
                byte[] buf = new byte[o];
                pck.getCopy(buf, 0, 0, o);
                try {
                    filRx.seek(i);
                    filRx.write(buf);
                } catch (Exception e) {
                    logger.error("unable to write file");
                    break;
                }
                doAck(i);
                break;
            case packRedundancy.typFilEnd:
                try {
                filRx.close();
            } catch (Exception e) {
                logger.error("unable to close file");
                break;
            }
            filRx = null;
            a = pck.getAsciiZ(0, packRedundancy.dataMax, 0);
            String b = null;
            logger.info("received file " + a);
            if (a.equals(packRedundancy.fnCore)) {
                b = version.getFileName();
            }
            if (a.equals(packRedundancy.fnStart)) {
                b = cfgInit.cfgFileSw;
            }
            if (b == null) {
                logger.error("got invalid filename");
                break;
            }
            userFlash.rename(filNm, b, true, true);
            doAck(-3);
            break;
        }
    }

    public void doPack(int typ, packHolder pckB) {
        pckB.merge2beg();
        packRedundancy pckP = prtRedun.getSelf();
        pckP.type = typ;
        pckP.peer = last.magic;
        pckP.createHeader(pckB);
        if (debugger.prtRedun) {
            logger.debug("tx " + pckP);
        }
        pckB.ETHsrc.setAddr(hwaddr);
        pckB.ETHtrg.setAddr(addrMac.getBroadcast());
        lower.sendPack(pckB);
    }

    public void doAck(int ofs) {
        packHolder pck = new packHolder(true, true);
        pck.msbPutD(0, ofs);
        pck.putSkip(4);
        doPack(packRedundancy.typAck, pck);
    }

    public boolean doRetry(int typ, packHolder pck) {
        ackRx = -1;
        for (int i = 0; i < 8; i++) {
            doPack(typ, pck.copyBytes(true, true));
            notif.sleep(cfgAll.redundancyKeep);
            if (ackRx != -1) {
                return false;
            }
        }
        logger.error("peer does not respond");
        return true;
    }

    public boolean doFile(String fn, String rfn) {
        logger.info("syncing " + fn + " as " + rfn);
        RandomAccessFile fr;
        long siz;
        try {
            fr = new RandomAccessFile(fn, "r");
            siz = fr.length();
        } catch (Exception e) {
            logger.error("unable to open file");
            return true;
        }
        if (doRetry(packRedundancy.typFilBeg, new packHolder(true, true))) {
            try {
                fr.close();
            } catch (Exception e) {
            }
            return true;
        }
        long pos = 0;
        for (; pos < siz;) {
            long rndl = siz - pos;
            if (rndl > packRedundancy.dataMax) {
                rndl = packRedundancy.dataMax;
            }
            int rndi = (int) rndl;
            byte[] buf = new byte[rndi];
            try {
                fr.read(buf, 0, rndi);
            } catch (Exception e) {
                logger.error("unable to read file");
                return true;
            }
            packHolder pck = new packHolder(true, true);
            pck.msbPutD(0, (int) pos);
            pck.msbPutW(4, buf.length);
            pck.putSkip(6);
            pos += rndl;
            pck.putCopy(buf, 0, 0, buf.length);
            pck.putSkip(buf.length);
            if (doRetry(packRedundancy.typFilDat, pck)) {
                try {
                    fr.close();
                } catch (Exception e) {
                }
                return true;
            }
        }
        try {
            fr.close();
        } catch (Exception e) {
            logger.error("unable to close file");
            return true;
        }
        packHolder pck = new packHolder(true, true);
        pck.putAsciiZ(0, packRedundancy.dataMax, rfn, 0);
        pck.putSkip(packRedundancy.dataMax);
        if (doRetry(packRedundancy.typFilEnd, pck)) {
            return true;
        }
        return false;
    }

}
