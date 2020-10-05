package rtr;

import pack.packHolder;
import tab.tabGen;
import tab.tabListing;
import tab.tabPrfxlstN;
import tab.tabRoute;
import tab.tabRouteEntry;
import tab.tabRtrmapN;
import util.bits;
import util.debugger;
import util.logger;
import util.notifier;
import util.shrtPthFrst;
import util.typLenVal;
import addr.addrIP;
import addr.addrIsis;
import cfg.cfgAll;
import ip.ipCor4;
import ip.ipFwdIface;
import java.util.List;
import tab.tabLabelBier;
import tab.tabRouteAttr;
import tab.tabRtrplcN;
import util.shrtPthFrstRes;
import util.state;
import util.syncInt;

/**
 * isis level
 *
 * @author matecsaba
 */
public class rtrIsisLevel implements Runnable {

    /**
     * level
     */
    public final int level;

    /**
     * list of lsps
     */
    protected final tabGen<rtrIsisLsp> lsps;

    /**
     * computed routes
     */
    protected final tabRoute<addrIP> routes;

    /**
     * advertise default route
     */
    public boolean defOrigin;

    /**
     * overloaded
     */
    public boolean overloaded;

    /**
     * set attached
     */
    public boolean attachedSet;

    /**
     * clear attached
     */
    public boolean attachedClr;

    /**
     * allow attached
     */
    public boolean attachedAlw;

    /**
     * clear attached
     */
    public boolean interLevels;

    /**
     * hostname
     */
    public boolean hostname;

    /**
     * traffic engineering
     */
    public boolean traffEng;

    /**
     * segment routing enabled
     */
    public boolean segrouEna;

    /**
     * bier enabled
     */
    public boolean bierEna;

    /**
     * lsp password
     */
    public String lspPassword;

    /**
     * max lsp size
     */
    public int maxLspSize;

    /**
     * lsp refresh interval
     */
    public int lspRefresh;

    /**
     * lsp lifetime
     */
    public int lspLifetime;

    /**
     * learn prefix list
     */
    public tabListing<tabPrfxlstN, addrIP> prflstFrom;

    /**
     * learn prefix list
     */
    public tabListing<tabPrfxlstN, addrIP> prflstInto;

    /**
     * advertise route map
     */
    public tabListing<tabRtrmapN, addrIP> roumapFrom;

    /**
     * advertise route map
     */
    public tabListing<tabRtrmapN, addrIP> roumapInto;

    /**
     * advertise route map
     */
    public tabListing<tabRtrplcN, addrIP> roupolFrom;

    /**
     * advertise route map
     */
    public tabListing<tabRtrplcN, addrIP> roupolInto;

    /**
     * last spf
     */
    protected shrtPthFrst<rtrIsisLevelSpf> lastSpf;

    /**
     * segment routing usage
     */
    protected boolean[] segrouUsd;

    /**
     * bier results
     */
    protected tabLabelBier bierRes;

    private final rtrIsis lower;

    private syncInt todo = new syncInt(0); // 1=need2run, 2=running, 0xffff0=works

    private tabGen<rtrIsisLsp> need2adv;

    private notifier notif;

    /**
     * create one level
     *
     * @param parent the isis process
     * @param lev level number
     */
    public rtrIsisLevel(rtrIsis parent, int lev) {
        lastSpf = new shrtPthFrst<rtrIsisLevelSpf>(null);
        lower = parent;
        level = lev;
        lsps = new tabGen<rtrIsisLsp>();
        routes = new tabRoute<addrIP>("computed");
        need2adv = new tabGen<rtrIsisLsp>();
        notif = new notifier();
        attachedClr = level == 2;
        attachedAlw = level == 1;
        interLevels = true;
        maxLspSize = 1024;
        lspLifetime = 1200000;
        lspRefresh = lspLifetime / 3;
        hostname = true;
    }

    public String toString() {
        return "isis level" + level;
    }

    /**
     * get flags value
     *
     * @return flags
     */
    protected int getFlagsVal() {
        int i;
        if (level == 1) {
            i = 1;
        } else {
            i = 3;
        }
        if (!attachedClr) {
            if (lower.amIattach()) {
                i |= rtrIsisLsp.flgAttach;
            }
            if (attachedSet) {
                i |= rtrIsisLsp.flgAttach;
            }
        }
        if (overloaded) {
            i |= rtrIsisLsp.flgOver;
        }
        return i;
    }

    /**
     * advertise one lsp
     *
     * @param lsp lsp to advertise
     * @param purge set true to purge it out
     */
    protected synchronized void generateLsp(rtrIsisLsp lsp, boolean purge) {
        if (purge) {
            if (lsp.getTimeRemain(true) < 1) {
                return;
            }
            lsp.setTimeRemain(0);
        } else {
            lsp.setTimeRemain(lspLifetime / 1000);
        }
        rtrIsisLsp old = lsps.find(lsp);
        if (old == null) {
            lsp.sequence = 1;
        } else {
            lsp.sequence = old.sequence + 1;
        }
        if (debugger.rtrIsisEvnt) {
            logger.debug("generate lsp " + lsp);
        }
        lsp.generateCheckSum();
        lsps.put(lsp);
    }

    /**
     * advertise needed lsps
     */
    protected void advertiseLsps() {
        if (debugger.rtrIsisEvnt) {
            logger.debug("advertise lsps in level" + level);
        }
        int done = 0;
        for (int i = 0; i < lsps.size(); i++) {
            rtrIsisLsp ntry = lsps.get(i);
            if (ntry == null) {
                continue;
            }
            if (lower.routerID.compare(ntry.srcID, lower.routerID) != 0) {
                continue;
            }
            if (need2adv.find(ntry) != null) {
                continue;
            }
            if (ntry.getTimeRemain(true) < 1) {
                continue;
            }
            generateLsp(ntry, true);
            done++;
        }
        for (int i = 0; i < need2adv.size(); i++) {
            rtrIsisLsp ntry = need2adv.get(i);
            if (ntry == null) {
                continue;
            }
            if (!ntry.contentDiffers(lsps.find(ntry))) {
                continue;
            }
            generateLsp(ntry, false);
            done++;
        }
        if (done > 0) {
            wakeNeighs();
        }
    }

    /**
     * wake up neighbors
     */
    protected void wakeNeighs() {
        for (int o = 0; o < lower.ifaces.size(); o++) {
            rtrIsisIface iface = lower.ifaces.get(o);
            if (iface == null) {
                continue;
            }
            for (int i = 0; i < iface.neighs.size(); i++) {
                rtrIsisNeigh neigh = iface.neighs.get(i);
                if (neigh == null) {
                    continue;
                }
                if (neigh.level.level != level) {
                    continue;
                }
                neigh.notif.wakeup();
            }
        }
    }

    /**
     * purge out aged lsps
     */
    protected void purgeLsps() {
        if (debugger.rtrIsisEvnt) {
            logger.debug("purge lsps in level" + level);
        }
        int done = 0;
        for (int i = lsps.size(); i >= 0; i--) {
            rtrIsisLsp ntry = lsps.get(i);
            if (ntry == null) {
                continue;
            }
            int o = ntry.getTimeRemain(true);
            if (o < (-lspRefresh / 1000)) {
                if (debugger.rtrIsisEvnt) {
                    logger.debug("purge " + ntry);
                }
                lsps.del(ntry);
                continue;
            }
            if (lower.routerID.compare(ntry.srcID, lower.routerID) != 0) {
                continue;
            }
            if (o > (lspRefresh / 1000)) {
                continue;
            }
            if (need2adv.find(ntry) == null) {
                continue;
            }
            generateLsp(ntry, false);
            done++;
        }
        if (done > 0) {
            wakeNeighs();
        }
    }

    /**
     * advertise one lsp
     *
     * @param pck payload
     */
    protected void advertiseLsp(packHolder pck) {
        pck.merge2beg();
        if (pck.dataSize() < 1) {
            return;
        }
        rtrIsisLsp lsp = new rtrIsisLsp();
        lsp.srcID = lower.routerID.copyBytes();
        lsp.nodID = pck.RTPsrc;
        lsp.lspNum = pck.RTPtyp;
        lsp.bufDat = pck.getCopy();
        lsp.flags = getFlagsVal();
        need2adv.put(lsp);
    }

    /**
     * get authentication data
     *
     * @return binary data, null if disabled
     */
    protected byte[] getAuthen() {
        if (lspPassword == null) {
            return new byte[0];
        }
        byte[] buf = (" " + lspPassword).getBytes();
        buf[0] = 1;
        return buf;
    }

    private void advertiseTlv(packHolder pck, typLenVal tlv) {
        if ((pck.headSize() + tlv.valSiz) > maxLspSize) {
            advertiseLsp(pck);
            pck.setDataSize(0);
            pck.RTPtyp++;
            if (lspPassword != null) {
                advertiseTlv(pck, rtrIsisLsp.tlvAuthen, getAuthen());
            }
        }
        tlv.putThis(pck);
    }

    private void advertiseTlv(packHolder pck, int typ, byte[] buf) {
        typLenVal tlv = rtrIsis.getTlv();
        tlv.valDat = buf;
        tlv.valSiz = buf.length;
        tlv.valTyp = typ;
        advertiseTlv(pck, tlv);
    }

    private void createNeighs(packHolder pck, rtrIsisIface ifc, boolean subs) {
        for (int o = 0; o < ifc.neighs.size(); o++) {
            rtrIsisNeigh nei = ifc.neighs.get(o);
            if (nei == null) {
                continue;
            }
            if (nei.peerAdjState != rtrIsisNeigh.statUp) {
                continue;
            }
            if (nei.level.level != level) {
                continue;
            }
            if (pck.RTPsrc == 0) {
                byte[] buf = new byte[0];
                if (subs) {
                    buf = rtrIsisTe.putSubs(lower, ifc, nei);
                }
                if (nei.segrouLab != null) {
                    buf = bits.byteConcat(buf, rtrIsisSr.putAdj(lower.fwdCore.ipVersion == ipCor4.protocolVersion, nei.segrouLab.getValue()));
                }
                advertiseTlv(pck, lower.putISneigh(nei.rtrID, 0, ifc.metric, buf));
                if (subs) {
                    advertiseTlv(pck, rtrIsisTe.putSrlg(lower, nei.rtrID, 0, ifc.iface.addr, nei.ifcAddr, ifc.teSrlg));
                }
            } else {
                advertiseTlv(pck, lower.putISneigh(nei.rtrID, 0, 0, new byte[0]));
            }
        }
    }

    private void createIface(packHolder pck, rtrIsisIface ifc) {
        boolean subs = traffEng && !ifc.teSuppress;
        if (ifc.netPnt2pnt) {
            createNeighs(pck, ifc, subs);
            return;
        }
        byte[] buf = new byte[0];
        if (subs) {
            buf = rtrIsisTe.putSubs(lower, ifc, null);
        }
        advertiseTlv(pck, lower.putISneigh(ifc.getDisAddr(level), ifc.getDisCirc(level), ifc.metric, buf));
        if (subs) {
            advertiseTlv(pck, rtrIsisTe.putSrlg(lower, ifc.getDisAddr(level), ifc.getDisCirc(level), ifc.iface.addr, null, ifc.teSrlg));
        }
        if (!ifc.amIdis(level)) {
            return;
        }
        packHolder p = new packHolder(true, true);
        p.RTPsrc = ifc.circuitID;
        if (lspPassword != null) {
            advertiseTlv(p, rtrIsisLsp.tlvAuthen, getAuthen());
        }
        advertiseTlv(p, lower.putISneigh(lower.routerID, 0, 0, new byte[0]));
        createNeighs(p, ifc, false);
        advertiseLsp(p);
    }

    private void createAddrs(packHolder pck) {
        tabRoute<addrIP> rs = new tabRoute<addrIP>("rs");
        tabRoute<addrIP> oa = new tabRoute<addrIP>("rs");
        if (defOrigin) {
            tabRouteEntry<addrIP> ntry = new tabRouteEntry<addrIP>();
            ntry.prefix = lower.getDefaultRoute();
            ntry.best.origin = 1;
            rs.add(tabRoute.addType.better, ntry, false, false);
        }
        for (int i = 0; i < lower.ifaces.size(); i++) {
            rtrIsisIface ifc = lower.ifaces.get(i);
            if (ifc == null) {
                continue;
            }
            if (ifc.iface.lower.getState() != state.states.up) {
                continue;
            }
            if (!ifc.suppressInt) {
                advertiseTlv(pck, lower.putAddrIface(ifc.iface.addr));
            }
            if (ifc.suppressAddr) {
                continue;
            }
            tabRouteEntry<addrIP> ntry = new tabRouteEntry<addrIP>();
            ntry.prefix = ifc.iface.network.copyBytes();
            ntry.best.distance = tabRouteAttr.distanIfc;
            ntry.best.metric = ifc.metric;
            ntry.best.segrouIdx = ifc.srIndex;
            if (ifc.srNode) {
                ntry.best.rouSrc |= 8;
            }
            if (ifc.srPop) {
                ntry.best.rouSrc |= 16;
            }
            ntry.best.bierIdx = ifc.brIndex;
            oa.add(tabRoute.addType.better, ntry, false, false);
            if ((ifc.circuitLevel & level) == 0) {
                continue;
            }
            rs.add(tabRoute.addType.better, ntry, false, false);
        }
        for (int i = 0; i < lower.routerRedistedU.size(); i++) {
            tabRouteEntry<addrIP> ntry = lower.routerRedistedU.get(i);
            if (ntry == null) {
                continue;
            }
            ntry = ntry.copyBytes(tabRoute.addType.notyet);
            ntry.best.distance = tabRouteAttr.distanIfc + 1;
            ntry.best.rouSrc = 1;
            ntry.best.segrouIdx = 0;
            ntry.best.bierIdx = 0;
            rs.add(tabRoute.addType.better, ntry, false, false);
        }
        if (interLevels) {
            tabRoute<addrIP> other = lower.getLevel(3 - level).routes;
            for (int i = 0; i < other.size(); i++) {
                tabRouteEntry<addrIP> ntry = other.get(i);
                if (ntry == null) {
                    continue;
                }
                ntry = ntry.copyBytes(tabRoute.addType.notyet);
                if ((ntry.best.rouSrc & 4) != 0) {
                    continue;
                }
                if (level == 2) {
                    if ((ntry.best.rouSrc & 2) != 0) {
                        continue;
                    }
                } else {
                    ntry.best.rouSrc |= 2;
                }
                if (oa.find(ntry.prefix) != null) {
                    continue;
                }
                ntry.best.rouSrc &= -1 - 8;
                rs.add(tabRoute.addType.better, ntry, false, false);
            }
        }
        tabRoute<addrIP> fl = new tabRoute<addrIP>("fl");
        tabRoute.addUpdatedTable(tabRoute.addType.better, rtrBgpUtil.safiUnicast, 0, fl, rs, true, roumapInto, roupolInto, prflstInto);
        for (int i = 0; i < fl.size(); i++) {
            tabRouteEntry<addrIP> ntry = fl.get(i);
            if (ntry == null) {
                continue;
            }
            byte[] subs = new byte[0];
            if (ntry.best.tag != 0) {
                subs = bits.byteConcat(subs, lower.putAddrTag(ntry.best.tag));
            }
            if (segrouEna && (ntry.best.segrouIdx > 0)) {
                subs = bits.byteConcat(subs, rtrIsisSr.putPref(ntry.best.segrouIdx, ((ntry.best.rouSrc & 16) != 0), (ntry.best.rouSrc & 3) != 0, (ntry.best.rouSrc & 8) != 0));
            }
            if (bierEna && (ntry.best.bierIdx > 0)) {
                subs = bits.byteConcat(subs, rtrIsisBr.putPref(lower, ntry.best.bierIdx));
            }
            advertiseTlv(pck, lower.putAddrReach(ntry.prefix, ntry.best.rouSrc, ntry.best.metric, subs));
        }
    }

    private void generateLsps() {
        if (debugger.rtrIsisEvnt) {
            logger.debug("generate lsps in level" + level);
        }
        need2adv.clear();
        packHolder pck = new packHolder(true, true);
        if (lspPassword != null) {
            advertiseTlv(pck, rtrIsisLsp.tlvAuthen, getAuthen());
        }
        advertiseTlv(pck, rtrIsisLsp.tlvProtSupp, lower.getNLPIDlst());
        if (lower.multiTopo) {
            int i = getFlagsVal();
            byte[] buf = lower.getMTopoLst();
            if ((i & rtrIsisLsp.flgOver) != 0) {
                buf[0] |= 0x80;
            }
            if ((i & rtrIsisLsp.flgAttach) != 0) {
                buf[0] |= 0x40;
            }
            advertiseTlv(pck, rtrIsisLsp.tlvMultiTopo, buf);
        }
        advertiseTlv(pck, rtrIsisLsp.tlvAreaAddr, lower.areaID.getAddrDat(true));
        if (traffEng) {
            advertiseTlv(pck, rtrIsisTe.putAddr(lower));
        }
        if (segrouEna && (lower.segrouLab != null)) {
            advertiseTlv(pck, rtrIsisSr.putBase(lower));
        }
        if (hostname) {
            byte[] buf = cfgAll.hostName.getBytes();
            advertiseTlv(pck, rtrIsisLsp.tlvHostName, buf);
        }
        for (int i = 0; i < lower.ifaces.size(); i++) {
            rtrIsisIface ifc = lower.ifaces.get(i);
            if (ifc == null) {
                continue;
            }
            if (ifc.iface.lower.getState() != state.states.up) {
                continue;
            }
            createIface(pck, ifc);
        }
        createAddrs(pck);
        advertiseLsp(pck);
        advertiseLsps();
    }

    private void calculateSpf() {
        if (debugger.rtrIsisEvnt) {
            logger.debug("calculate spf on level" + level);
        }
        if (segrouEna && (lower.segrouLab != null)) {
            segrouUsd = new boolean[lower.segrouMax];
        } else {
            segrouUsd = null;
        }
        shrtPthFrst<rtrIsisLevelSpf> spf = new shrtPthFrst<rtrIsisLevelSpf>(lastSpf);
        for (int i = 0; i < lsps.size(); i++) {
            rtrIsisLsp lsp = lsps.get(i);
            if (lsp == null) {
                continue;
            }
            if (lsp.getTimeRemain(true) < 1) {
                continue;
            }
            boolean stub = (lsp.flags & rtrIsisLsp.flgOver) != 0;
            packHolder pck = lsp.getPayload();
            rtrIsisLevelSpf src = new rtrIsisLevelSpf(lsp.srcID, lsp.nodID);
            typLenVal tlv = rtrIsis.getTlv();
            for (;;) {
                if (tlv.getBytes(pck)) {
                    break;
                }
                int o = rtrIsisSr.getBase(tlv);
                if ((o > 0) && (segrouUsd != null)) {
                    spf.addSegRouB(src, o);
                    continue;
                }
                addrIsis adr = lower.getISalias(tlv);
                if (adr != null) {
                    rtrIsisLevelSpf trg = new rtrIsisLevelSpf(adr, 0);
                    spf.addConn(src, trg, 0, false, stub, null);
                    spf.addConn(trg, src, 0, false, stub, null);
                    continue;
                }
                tabGen<rtrIsisLsp> nel = lower.getISneigh(tlv);
                if (nel != null) {
                    for (o = 0; o < nel.size(); o++) {
                        rtrIsisLsp nei = nel.get(o);
                        if (nei == null) {
                            continue;
                        }
                        spf.addConn(src, new rtrIsisLevelSpf(nei.srcID, nei.nodID), nei.lspNum, nei.nodID == 0, stub, null);
                    }
                    continue;
                }
                if (!bierEna) {
                    continue;
                }
                tabGen<tabRouteEntry<addrIP>> rou = lower.getAddrReach(tlv);
                if (rou == null) {
                    continue;
                }
                for (o = 0; o < rou.size(); o++) {
                    tabRouteEntry<addrIP> pref = rou.get(o);
                    if (pref == null) {
                        continue;
                    }
                    if (pref.best.bierBeg == 0) {
                        continue;
                    }
                    spf.addBierB(src, pref.best.bierBeg);
                }
            }
        }
        spf.doCalc(new rtrIsisLevelSpf(lower.routerID, 0), null);
        for (int i = 0; i < lower.ifaces.size(); i++) {
            rtrIsisIface ifc = lower.ifaces.get(i);
            if (ifc == null) {
                continue;
            }
            if (ifc.iface.lower.getState() != state.states.up) {
                continue;
            }
            if ((segrouUsd != null) && (ifc.srIndex > 0)) {
                segrouUsd[ifc.srIndex] = true;
                lower.segrouLab[ifc.srIndex].setFwdCommon(7, lower.fwdCore);
            }
            for (int o = 0; o < ifc.neighs.size(); o++) {
                rtrIsisNeigh nei = ifc.neighs.get(o);
                if (nei == null) {
                    continue;
                }
                if (nei.peerAdjState != rtrIsisNeigh.statUp) {
                    continue;
                }
                if (nei.level.level != level) {
                    continue;
                }
                spf.addNextHop(ifc.metric, new rtrIsisLevelSpf(nei.rtrID, 0), nei.ifcAddr.copyBytes(), ifc.iface);
            }
        }
        boolean needAttach = (!lower.haveNeighbor(2)) && attachedAlw;
        tabRoute<addrIP> rs = new tabRoute<addrIP>("rs");
        for (int i = 0; i < lsps.size(); i++) {
            rtrIsisLsp lsp = lsps.get(i);
            if (lsp == null) {
                continue;
            }
            if (lsp.getTimeRemain(true) < 1) {
                continue;
            }
            rtrIsisLevelSpf src = new rtrIsisLevelSpf(lsp.srcID, lsp.nodID);
            List<shrtPthFrstRes<rtrIsisLevelSpf>> hop = spf.findNextHop(src);
            if (hop.size() < 1) {
                continue;
            }
            int met = spf.getMetric(src);
            int sro = spf.getSegRouB(src);
            int bro = spf.getBierB(src);
            packHolder pck = lsp.getPayload();
            typLenVal tlv = rtrIsis.getTlv();
            if ((lsp.flags & rtrIsisLsp.flgAttach) != 0) {
                tabRouteEntry<addrIP> pref = new tabRouteEntry<addrIP>();
                pref.prefix = lower.getDefaultRoute();
                pref.best.metric = met;
                pref.best.distance = lower.distantInt;
                pref.best.rouSrc = 6;
                pref.best.srcRtr = lsp.srcID.copyBytes();
                pref.best.segrouOld = sro;
                pref.best.bierOld = bro;
                shrtPthFrst.populateRoute(pref, hop);
                shrtPthFrst.populateSegrout(pref, pref.best, hop, (pref.best.rouSrc & 16) != 0);
                if (needAttach && ((lsp.flags & rtrIsisLsp.flgOver) == 0)) {
                    rs.add(tabRoute.addType.ecmp, pref, false, true);
                }
            }
            for (;;) {
                if (tlv.getBytes(pck)) {
                    break;
                }
                tabGen<tabRouteEntry<addrIP>> rou = lower.getAddrReach(tlv);
                if (rou == null) {
                    continue;
                }
                for (int o = 0; o < rou.size(); o++) {
                    tabRouteEntry<addrIP> pref = rou.get(o);
                    if (pref == null) {
                        continue;
                    }
                    pref.best.metric += met;
                    if ((pref.best.rouSrc & 1) == 0) {
                        pref.best.distance = lower.distantInt;
                    } else {
                        pref.best.distance = lower.distantExt;
                    }
                    pref.best.srcRtr = lsp.srcID.copyBytes();
                    spf.addSegRouI(src, pref.best.segrouIdx);
                    spf.addBierI(src, pref.best.bierIdx, (pref.best.rouSrc & 3) == 0);
                    pref.best.segrouOld = sro;
                    pref.best.bierOld = bro;
                    shrtPthFrst.populateRoute(pref, hop);
                    shrtPthFrst.populateSegrout(pref, pref.best, hop, (pref.best.rouSrc & 16) != 0);
                    if ((segrouUsd != null) && (pref.best.segrouIdx < lower.segrouMax) && (pref.best.labelRem != null)) {
                        lower.segrouLab[pref.best.segrouIdx].setFwdMpls(7, lower.fwdCore, (ipFwdIface) pref.best.iface, pref.best.nextHop, pref.best.labelRem);
                        segrouUsd[pref.best.segrouIdx] = true;
                    }
                    rs.add(tabRoute.addType.ecmp, pref, false, true);
                }
            }
        }
        routes.clear();
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, rtrBgpUtil.safiUnicast, 0, routes, rs, true, roumapFrom, roupolFrom, prflstFrom);
        lower.routerDoAggregates(rtrBgpUtil.safiUnicast, routes, null, lower.fwdCore.commonLabel, 0, null, 0);
        if (bierEna) {
            bierRes = spf.getBierI();
        } else {
            bierRes = null;
        }
        if (debugger.rtrIsisEvnt) {
            logger.debug("unreachable:" + spf.listUnreachables());
            logger.debug("reachable:" + spf.listReachables());
        }
        lastSpf = spf;
        lower.routerCreateComputed();
    }

    /**
     * schedule work
     *
     * @param i what to do: 1=genLsp, 2=calcSpf, 4=genAll, 7=all
     */
    public void schedWork(int i) {
        todo.or(i << 4);
        notif.wakeup();
    }

    public void run() {
        if (debugger.rtrIsisEvnt) {
            logger.debug("started level" + level);
        }
        todo.or(2);
        for (;;) {
            try {
                notif.misleep(30000);
                int ver = todo.ver();
                int val = todo.get();
                todo.andIf(0xf, ver);
                if ((val & 1) == 0) {
                    break;
                }
                if ((val & 0x10) != 0) {
                    generateLsps();
                }
                if ((val & 0x20) != 0) {
                    calculateSpf();
                }
                if ((val & 0x40) != 0) {
                    lower.genLsps(1);
                }
                purgeLsps();
            } catch (Exception e) {
                logger.traceback(e);
            }
        }
        todo.and(1);
        if (debugger.rtrIsisEvnt) {
            logger.debug("stopped level" + level);
        }
    }

    /**
     * start this level
     */
    public void startNow() {
        if ((todo.get() & 2) != 0) {
            return;
        }
        todo.or(1);
        new Thread(this).start();
    }

    /**
     * stop this level
     */
    public void stopNow() {
        todo.and(2);
    }

}
