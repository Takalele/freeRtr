package org.freertr.rtr;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import org.freertr.addr.addrIP;
import org.freertr.addr.addrPrefix;
import org.freertr.cfg.cfgAll;
import org.freertr.cfg.cfgInit;
import org.freertr.clnt.clntDns;
import org.freertr.clnt.clntWhois;
import org.freertr.ip.ipFwdIface;
import org.freertr.ip.ipFwdTab;
import org.freertr.pack.packDnsRec;
import org.freertr.pipe.pipeLine;
import org.freertr.pipe.pipeSide;
import org.freertr.prt.prtAccept;
import org.freertr.prt.prtGenConn;
import org.freertr.serv.servGeneric;
import org.freertr.tab.tabGen;
import org.freertr.tab.tabIntMatcher;
import org.freertr.tab.tabListing;
import org.freertr.tab.tabPrfxlstN;
import org.freertr.tab.tabRpkiRoa;
import org.freertr.tab.tabRpkiUtil;
import org.freertr.tab.tabRoute;
import org.freertr.tab.tabRouteAttr;
import org.freertr.tab.tabRouteEntry;
import org.freertr.tab.tabRouteUtil;
import org.freertr.tab.tabRtrmapN;
import org.freertr.tab.tabRtrplc;
import org.freertr.tab.tabRtrplcN;
import org.freertr.user.userFormat;
import org.freertr.util.bits;
import org.freertr.util.cmds;
import org.freertr.util.counter;
import org.freertr.util.debugger;
import org.freertr.util.logger;
import org.freertr.util.notifier;

/**
 * bgp4 neighbor
 *
 * @author matecsaba
 */
public class rtrBgpNeigh extends rtrBgpParam implements Comparable<rtrBgpNeigh>, Runnable {

    /**
     * address of peer
     */
    public final addrIP peerAddr;

    /**
     * local interface
     */
    public ipFwdIface localIfc = null;

    /**
     * sending interface
     */
    public ipFwdIface sendingIfc = null;

    /**
     * local address
     */
    public addrIP localAddr = new addrIP();

    /**
     * local other address
     */
    public addrIP localOddr = new addrIP();

    /**
     * accepted unicast prefixes
     */
    public tabRoute<addrIP> accUni = new tabRoute<addrIP>("rx");

    /**
     * accepted multicast prefixes
     */
    public tabRoute<addrIP> accMlt = new tabRoute<addrIP>("rx");

    /**
     * accepted other unicast prefixes
     */
    public tabRoute<addrIP> accOuni = new tabRoute<addrIP>("rx");

    /**
     * accepted other multicast prefixes
     */
    public tabRoute<addrIP> accOmlt = new tabRoute<addrIP>("rx");

    /**
     * accepted other flowspec prefixes
     */
    public tabRoute<addrIP> accOflw = new tabRoute<addrIP>("rx");

    /**
     * accepted other srte prefixes
     */
    public tabRoute<addrIP> accOsrt = new tabRoute<addrIP>("rx");

    /**
     * accepted flowspec prefixes
     */
    public tabRoute<addrIP> accFlw = new tabRoute<addrIP>("rx");

    /**
     * accepted vpnuni prefixes
     */
    public tabRoute<addrIP> accVpnU = new tabRoute<addrIP>("rx");

    /**
     * accepted vpnmulti prefixes
     */
    public tabRoute<addrIP> accVpnM = new tabRoute<addrIP>("rx");

    /**
     * accepted vpnflow prefixes
     */
    public tabRoute<addrIP> accVpnF = new tabRoute<addrIP>("rx");

    /**
     * accepted other vpnuni prefixes
     */
    public tabRoute<addrIP> accVpoU = new tabRoute<addrIP>("rx");

    /**
     * accepted other vpnmulti prefixes
     */
    public tabRoute<addrIP> accVpoM = new tabRoute<addrIP>("rx");

    /**
     * accepted other vpnflow prefixes
     */
    public tabRoute<addrIP> accVpoF = new tabRoute<addrIP>("rx");

    /**
     * accepted vpls prefixes
     */
    public tabRoute<addrIP> accVpls = new tabRoute<addrIP>("rx");

    /**
     * accepted mspw prefixes
     */
    public tabRoute<addrIP> accMspw = new tabRoute<addrIP>("rx");

    /**
     * accepted evpn prefixes
     */
    public tabRoute<addrIP> accEvpn = new tabRoute<addrIP>("rx");

    /**
     * accepted mdt prefixes
     */
    public tabRoute<addrIP> accMdt = new tabRoute<addrIP>("rx");

    /**
     * accepted nsh prefixes
     */
    public tabRoute<addrIP> accNsh = new tabRoute<addrIP>("rx");

    /**
     * accepted rpd prefixes
     */
    public tabRoute<addrIP> accRpd = new tabRoute<addrIP>("rx");

    /**
     * accepted spf prefixes
     */
    public tabRoute<addrIP> accSpf = new tabRoute<addrIP>("rx");

    /**
     * accepted rtfilter prefixes
     */
    public tabRoute<addrIP> accRtf = new tabRoute<addrIP>("rx");

    /**
     * accepted srte prefixes
     */
    public tabRoute<addrIP> accSrte = new tabRoute<addrIP>("rx");

    /**
     * accepted linkstate prefixes
     */
    public tabRoute<addrIP> accLnks = new tabRoute<addrIP>("rx");

    /**
     * accepted mvpn prefixes
     */
    public tabRoute<addrIP> accMvpn = new tabRoute<addrIP>("rx");

    /**
     * accepted other mvpn prefixes
     */
    public tabRoute<addrIP> accMvpo = new tabRoute<addrIP>("rx");

    /**
     * accepted mtree prefixes
     */
    public tabRoute<addrIP> accMtre = new tabRoute<addrIP>("rx");

    /**
     * accepted other mtree prefixes
     */
    public tabRoute<addrIP> accMtro = new tabRoute<addrIP>("rx");

    /**
     * willing unicast prefixes
     */
    public tabRoute<addrIP> wilUni = new tabRoute<addrIP>("tx");

    /**
     * willing multicast prefixes
     */
    public tabRoute<addrIP> wilMlt = new tabRoute<addrIP>("tx");

    /**
     * willing other unicast prefixes
     */
    public tabRoute<addrIP> wilOuni = new tabRoute<addrIP>("tx");

    /**
     * willing other multicast prefixes
     */
    public tabRoute<addrIP> wilOmlt = new tabRoute<addrIP>("tx");

    /**
     * willing other flowspec prefixes
     */
    public tabRoute<addrIP> wilOflw = new tabRoute<addrIP>("tx");

    /**
     * willing other srte prefixes
     */
    public tabRoute<addrIP> wilOsrt = new tabRoute<addrIP>("tx");

    /**
     * willing flowspec prefixes
     */
    public tabRoute<addrIP> wilFlw = new tabRoute<addrIP>("tx");

    /**
     * willing vpnuni prefixes
     */
    public tabRoute<addrIP> wilVpnU = new tabRoute<addrIP>("tx");

    /**
     * willing vpnmulti prefixes
     */
    public tabRoute<addrIP> wilVpnM = new tabRoute<addrIP>("tx");

    /**
     * willing vpnflow prefixes
     */
    public tabRoute<addrIP> wilVpnF = new tabRoute<addrIP>("tx");

    /**
     * willing other vpnuni prefixes
     */
    public tabRoute<addrIP> wilVpoU = new tabRoute<addrIP>("tx");

    /**
     * willing other vpnmulti prefixes
     */
    public tabRoute<addrIP> wilVpoM = new tabRoute<addrIP>("tx");

    /**
     * willing other vpnflow prefixes
     */
    public tabRoute<addrIP> wilVpoF = new tabRoute<addrIP>("tx");

    /**
     * willing vpls prefixes
     */
    public tabRoute<addrIP> wilVpls = new tabRoute<addrIP>("tx");

    /**
     * willing mspw prefixes
     */
    public tabRoute<addrIP> wilMspw = new tabRoute<addrIP>("tx");

    /**
     * willing evpn prefixes
     */
    public tabRoute<addrIP> wilEvpn = new tabRoute<addrIP>("tx");

    /**
     * willing mdt prefixes
     */
    public tabRoute<addrIP> wilMdt = new tabRoute<addrIP>("tx");

    /**
     * willing nsh prefixes
     */
    public tabRoute<addrIP> wilNsh = new tabRoute<addrIP>("tx");

    /**
     * willing rpd prefixes
     */
    public tabRoute<addrIP> wilRpd = new tabRoute<addrIP>("tx");

    /**
     * willing spf prefixes
     */
    public tabRoute<addrIP> wilSpf = new tabRoute<addrIP>("tx");

    /**
     * willing rtfilter prefixes
     */
    public tabRoute<addrIP> wilRtf = new tabRoute<addrIP>("tx");

    /**
     * willing srte prefixes
     */
    public tabRoute<addrIP> wilSrte = new tabRoute<addrIP>("tx");

    /**
     * willing linkstate prefixes
     */
    public tabRoute<addrIP> wilLnks = new tabRoute<addrIP>("tx");

    /**
     * willing mvpn prefixes
     */
    public tabRoute<addrIP> wilMvpn = new tabRoute<addrIP>("tx");

    /**
     * willing other mvpn prefixes
     */
    public tabRoute<addrIP> wilMvpo = new tabRoute<addrIP>("tx");

    /**
     * willing mtree prefixes
     */
    public tabRoute<addrIP> wilMtre = new tabRoute<addrIP>("tx");

    /**
     * willing other mtree prefixes
     */
    public tabRoute<addrIP> wilMtro = new tabRoute<addrIP>("tx");

    /**
     * changed unicast prefixes
     */
    public tabRoute<addrIP> chgUni = new tabRoute<addrIP>("chg");

    /**
     * changed multicast prefixes
     */
    public tabRoute<addrIP> chgMlt = new tabRoute<addrIP>("chg");

    /**
     * changed other unicast prefixes
     */
    public tabRoute<addrIP> chgOuni = new tabRoute<addrIP>("chg");

    /**
     * changed other multicast prefixes
     */
    public tabRoute<addrIP> chgOmlt = new tabRoute<addrIP>("chg");

    /**
     * changed other flowspec prefixes
     */
    public tabRoute<addrIP> chgOflw = new tabRoute<addrIP>("chg");

    /**
     * changed other srte prefixes
     */
    public tabRoute<addrIP> chgOsrt = new tabRoute<addrIP>("chg");

    /**
     * changed flowspec prefixes
     */
    public tabRoute<addrIP> chgFlw = new tabRoute<addrIP>("chg");

    /**
     * changed vpnuni prefixes
     */
    public tabRoute<addrIP> chgVpnU = new tabRoute<addrIP>("chg");

    /**
     * changed vpnmulti prefixes
     */
    public tabRoute<addrIP> chgVpnM = new tabRoute<addrIP>("chg");

    /**
     * changed vpnflow prefixes
     */
    public tabRoute<addrIP> chgVpnF = new tabRoute<addrIP>("chg");

    /**
     * changed other vpnuni prefixes
     */
    public tabRoute<addrIP> chgVpoU = new tabRoute<addrIP>("chg");

    /**
     * changed other vpnmulti prefixes
     */
    public tabRoute<addrIP> chgVpoM = new tabRoute<addrIP>("chg");

    /**
     * changed other vpnflow prefixes
     */
    public tabRoute<addrIP> chgVpoF = new tabRoute<addrIP>("chg");

    /**
     * changed vpls prefixes
     */
    public tabRoute<addrIP> chgVpls = new tabRoute<addrIP>("chg");

    /**
     * changed mspw prefixes
     */
    public tabRoute<addrIP> chgMspw = new tabRoute<addrIP>("chg");

    /**
     * changed evpn prefixes
     */
    public tabRoute<addrIP> chgEvpn = new tabRoute<addrIP>("chg");

    /**
     * changed mdt prefixes
     */
    public tabRoute<addrIP> chgMdt = new tabRoute<addrIP>("chg");

    /**
     * changed nsh prefixes
     */
    public tabRoute<addrIP> chgNsh = new tabRoute<addrIP>("chg");

    /**
     * changed rpd prefixes
     */
    public tabRoute<addrIP> chgRpd = new tabRoute<addrIP>("chg");

    /**
     * changed spf prefixes
     */
    public tabRoute<addrIP> chgSpf = new tabRoute<addrIP>("chg");

    /**
     * changed rtfilter prefixes
     */
    public tabRoute<addrIP> chgRtf = new tabRoute<addrIP>("chg");

    /**
     * changed srte prefixes
     */
    public tabRoute<addrIP> chgSrte = new tabRoute<addrIP>("chg");

    /**
     * changed linkstate prefixes
     */
    public tabRoute<addrIP> chgLnks = new tabRoute<addrIP>("chg");

    /**
     * changed mvpn prefixes
     */
    public tabRoute<addrIP> chgMvpn = new tabRoute<addrIP>("chg");

    /**
     * changed other mvpn prefixes
     */
    public tabRoute<addrIP> chgMvpo = new tabRoute<addrIP>("chg");

    /**
     * changed mtree prefixes
     */
    public tabRoute<addrIP> chgMtre = new tabRoute<addrIP>("chg");

    /**
     * changed other mtree prefixes
     */
    public tabRoute<addrIP> chgMtro = new tabRoute<addrIP>("chg");

    /**
     * update group number
     */
    public int groupMember = -1;

    /**
     * last time advertised
     */
    public long advertLast;

    /**
     * advertise count
     */
    public int advertCount;

    /**
     * neighbor reachable
     */
    public boolean reachable = false;

    /**
     * old reachable state
     */
    public boolean reachOld;

    /**
     * neighbor reachable change time
     */
    public long reachTim;

    /**
     * neighbor reachable change number
     */
    public long reachNum;

    /**
     * neighbor session change number
     */
    public long sessNum;

    /**
     * speaker
     */
    public rtrBgpSpeak conn;

    /**
     * type of peer
     */
    public int peerType;

    /**
     * full compute last
     */
    public long fullLast;

    /**
     * incremental compute last
     */
    public long incrLast;

    /**
     * full compute count
     */
    public int fullCount;

    /**
     * incremental compute count
     */
    public int incrCount;

    /**
     * full compute time
     */
    public int fullTime;

    /**
     * incremental compute time
     */
    public int incrTime;

    /**
     * transmit sleeper
     */
    public notifier transmit = new notifier();

    /**
     * reachability statistics
     */
    public final counter reachabStat = new counter();

    /**
     * unreachability statistics
     */
    public final counter unreachStat = new counter();

    /**
     * message types received
     */
    public final counter[] msgStats = new counter[256];

    /**
     * attribute types received
     */
    public final counter[] attrStats = new counter[256];

    private boolean need2run;

    /**
     * create neighbor
     *
     * @param parent bgp process
     * @param addr address of peer
     */
    public rtrBgpNeigh(rtrBgp parent, addrIP addr) {
        super(parent, false);
        peerAddr = addr;
        for (int i = 0; i < msgStats.length; i++) {
            msgStats[i] = new counter();
        }
        for (int i = 0; i < attrStats.length; i++) {
            attrStats[i] = new counter();
        }
        conn = new rtrBgpSpeak(lower, this, null, false);
    }

    public int compareTo(rtrBgpNeigh o) {
        return peerAddr.compareTo(o.peerAddr);
    }

    public String toString() {
        return "" + peerAddr;
    }

    /**
     * flap connection
     */
    public void flapBgpConn() {
        conn.closeNow();
    }

    /**
     * save table
     *
     * @param fil file to use
     * @param safi safi to refresh
     */
    public void saveTable(RandomAccessFile fil, int safi) {
        rtrBgpMrt.dumpTable(fil, safi, conn.getLearned(safi), false, lower.fwdCore.ipVersion, remoteAs, localAs, peerAddr, localAddr);
        rtrBgpMrt.dumpTable(fil, safi, conn.getAdverted(safi), true, lower.fwdCore.ipVersion, remoteAs, localAs, peerAddr, localAddr);
    }

    public void doTempCfg(String cmd, boolean negated) {
    }

    public void getConfig(List<String> l, String beg, int filter) {
        l.addAll(getParamCfg(beg, "neighbor " + peerAddr + " ", filter));
        l.add(beg + cmds.comment);
    }

    /**
     * get dampening of peer
     *
     * @param mtch matcher
     * @return dampening
     */
    public userFormat getDampening(tabIntMatcher mtch) {
        if (dampenPfxs == null) {
            return null;
        }
        userFormat l = new userFormat("|", "afi|prefix|penalty|dampened|ago|last");
        for (int i = 0; i < dampenPfxs.size(); i++) {
            rtrBgpDamp ntry = dampenPfxs.get(i);
            if (ntry == null) {
                continue;
            }
            if (!mtch.matches(ntry.penalty)) {
                continue;
            }
            l.add("" + ntry);
        }
        return l;
    }

    /**
     * get status of peer
     *
     * @return status
     */
    public userFormat getStatus() {
        userFormat l = new userFormat("|", "category|value");
        l.add("peer|" + peerAddr);
        l.add("shutdown|" + checkShutdown());
        l.add("ready2adv|" + conn.ready2adv);
        l.add("reachable state|" + reachable);
        l.add("reachable changed|" + bits.timePast(reachTim) + " ago, at " + bits.time2str(cfgAll.timeZoneName, reachTim + cfgAll.timeServerOffset, 3));
        l.add("reachable changes|" + reachNum);
        l.add("session changes|" + sessNum);
        l.add("fallover|" + sendingIfc);
        l.add("update group|" + groupMember);
        l.add("socket mode|" + socketMode);
        rtrBgpDump.getMsgStats(l, rtrBgpUtil.msgOpen, msgStats, "|tx=", " rx=");
        rtrBgpDump.getMsgStats(l, rtrBgpUtil.msgUpdate, msgStats, "|tx=", " rx=");
        rtrBgpDump.getMsgStats(l, rtrBgpUtil.msgNotify, msgStats, "|tx=", " rx=");
        rtrBgpDump.getUnReachStats(l, reachabStat, unreachStat, "|rx=", " tx=");
        rtrBgpDump.getUnknwSum(l, false, msgStats, "|rx=", " tx=");
        rtrBgpDump.getUnknwSum(l, true, attrStats, "|rx=", " tx=");
        l.add("local asn|" + clntWhois.asn2mixed(localAs, true));
        l.add("remote asn|" + clntWhois.asn2mixed(remoteAs, true));
        l.add("type|" + rtrBgpUtil.peerType2string(peerType));
        l.add("leak role|rx=" + rtrBgpUtil.leakRole2string(conn.peerLeakRole, false) + ", tx=" + rtrBgpUtil.leakRole2string(leakRole, leakAttr));
        l.add("dynamic capability|" + conn.peerDynCap + ", rx=" + conn.dynCapaRx + ", tx=" + conn.dynCapaTx);
        l.add("rpki in|" + rtrBgpUtil.rpkiMode2string(rpkiIn) + " vpn=" + rtrBgpUtil.rpkiMode2string(vpkiIn));
        l.add("rpki out|" + rtrBgpUtil.rpkiMode2string(rpkiOut) + " vpn=" + rtrBgpUtil.rpkiMode2string(vpkiOut));
        l.add("safi open|" + rtrBgpParam.mask2string(conn.peerAfis));
        l.add("safi got|" + rtrBgpParam.mask2string(conn.originalSafiList));
        l.add("safi not remote|" + rtrBgpParam.mask2string(addrFams - conn.peerAfis));
        l.add("safi not local|" + rtrBgpParam.mask2string(conn.originalSafiList - conn.peerAfis));
        l.add("ipinfo|" + conn.ipInfoRes);
        l.add("local address|" + localAddr);
        l.add("other address|" + localOddr);
        l.add("router id|" + conn.peerRouterID);
        l.add("uptime|" + bits.timePast(conn.upTime) + " ago, at " + bits.time2str(cfgAll.timeZoneName, conn.upTime + cfgAll.timeServerOffset, 3));
        l.add("hold time|" + bits.timeDump(conn.peerHold / 1000));
        l.add("keepalive time|" + bits.timeDump(conn.peerKeep / 1000));
        l.add("32bit as|" + conn.peer32bitAS);
        l.add("refresh|" + conn.peerRefreshOld + " " + conn.peerRefreshNew + ", rx=" + conn.refreshRx + ", tx=" + conn.refreshTx);
        l.add("extended open|rx=" + conn.peerExtOpen + ", tx=" + extOpen);
        l.add("extended message|rx=" + conn.peerExtUpd + ", tx=" + extUpdate);
        l.add("description|" + description);
        l.add("hostname|" + conn.peerHostname);
        l.add("domainname|" + conn.peerDomainname);
        l.add("software|" + conn.peerSoftware);
        l.add("compression|rx=" + (conn.compressRx != null) + ", tx=" + (conn.compressTx != null));
        l.add("strict bfd|" + conn.strictBfd);
        l.add("graceful got|" + rtrBgpParam.mask2string(conn.peerGrace));
        l.add("graceful sent|" + rtrBgpParam.mask2string(graceRestart));
        l.add("longlive graceful got|" + rtrBgpParam.mask2string(conn.peerLlGrace));
        l.add("longlive graceful sent|" + rtrBgpParam.mask2string(llGraceRestart));
        l.add("multilabel got|" + rtrBgpParam.mask2string(conn.peerMltLab));
        l.add("multilabel sent|" + rtrBgpParam.mask2string(multiLabel));
        l.add("extnexthop cur|" + rtrBgpParam.mask2string(conn.peerExtNextCur));
        l.add("extnexthop otr|" + rtrBgpParam.mask2string(conn.peerExtNextOtr));
        l.add("addpath rx open|" + rtrBgpParam.mask2string(conn.addpathRx));
        l.add("addpath tx open|" + rtrBgpParam.mask2string(conn.addpathTx));
        l.add("addpath rx got|" + rtrBgpParam.mask2string(conn.originalAddRlist));
        l.add("addpath tx got|" + rtrBgpParam.mask2string(conn.originalAddTlist));
        l.add("addpath rx not remote|" + rtrBgpParam.mask2string(addpathRmode - conn.addpathRx));
        l.add("addpath tx not remote|" + rtrBgpParam.mask2string(addpathTmode - conn.addpathTx));
        l.add("addpath rx not local|" + rtrBgpParam.mask2string(conn.originalAddRlist - conn.addpathRx));
        l.add("addpath tx not local|" + rtrBgpParam.mask2string(conn.originalAddTlist - conn.addpathTx));
        l.add("unicast advertised|" + conn.advUni.size() + " of " + wilUni.size() + ", list = " + chgUni.size() + ", accepted = " + accUni.size() + " of " + conn.lrnUni.size());
        l.add("multicast advertised|" + conn.advMlt.size() + " of " + wilMlt.size() + ", list = " + chgMlt.size() + ", accepted = " + accMlt.size() + " of " + conn.lrnMlt.size());
        l.add("ouni advertised|" + conn.advOuni.size() + " of " + wilOuni.size() + ", list = " + chgOuni.size() + ", accepted = " + accOuni.size() + " of " + conn.lrnOuni.size());
        l.add("omlt advertised|" + conn.advOmlt.size() + " of " + wilOmlt.size() + ", list = " + chgOmlt.size() + ", accepted = " + accOmlt.size() + " of " + conn.lrnOmlt.size());
        l.add("oflw advertised|" + conn.advOflw.size() + " of " + wilOflw.size() + ", list = " + chgOflw.size() + ", accepted = " + accOflw.size() + " of " + conn.lrnOflw.size());
        l.add("osrt advertised|" + conn.advOsrt.size() + " of " + wilOsrt.size() + ", list = " + chgOsrt.size() + ", accepted = " + accOsrt.size() + " of " + conn.lrnOsrt.size());
        l.add("flowspec advertised|" + conn.advFlw.size() + " of " + wilFlw.size() + ", list = " + chgFlw.size() + ", accepted = " + accFlw.size() + " of " + conn.lrnFlw.size());
        l.add("vpnuni advertised|" + conn.advVpnU.size() + " of " + wilVpnU.size() + ", list = " + chgVpnU.size() + ", accepted = " + accVpnU.size() + " of " + conn.lrnVpnU.size());
        l.add("vpnmlt advertised|" + conn.advVpnM.size() + " of " + wilVpnM.size() + ", list = " + chgVpnM.size() + ", accepted = " + accVpnM.size() + " of " + conn.lrnVpnM.size());
        l.add("vpnflw advertised|" + conn.advVpnF.size() + " of " + wilVpnF.size() + ", list = " + chgVpnF.size() + ", accepted = " + accVpnF.size() + " of " + conn.lrnVpnF.size());
        l.add("ovpnuni advertised|" + conn.advVpoU.size() + " of " + wilVpoU.size() + ", list = " + chgVpoU.size() + ", accepted = " + accVpoU.size() + " of " + conn.lrnVpoU.size());
        l.add("ovpnmlt advertised|" + conn.advVpoM.size() + " of " + wilVpoM.size() + ", list = " + chgVpoM.size() + ", accepted = " + accVpoM.size() + " of " + conn.lrnVpoM.size());
        l.add("ovpnflw advertised|" + conn.advVpoF.size() + " of " + wilVpoF.size() + ", list = " + chgVpoF.size() + ", accepted = " + accVpoF.size() + " of " + conn.lrnVpoF.size());
        l.add("vpls advertised|" + conn.advVpls.size() + " of " + wilVpls.size() + ", list = " + chgVpls.size() + ", accepted = " + accVpls.size() + " of " + conn.lrnVpls.size());
        l.add("mspw advertised|" + conn.advMspw.size() + " of " + wilMspw.size() + ", list = " + chgMspw.size() + ", accepted = " + accMspw.size() + " of " + conn.lrnMspw.size());
        l.add("evpn advertised|" + conn.advEvpn.size() + " of " + wilEvpn.size() + ", list = " + chgEvpn.size() + ", accepted = " + accEvpn.size() + " of " + conn.lrnEvpn.size());
        l.add("mdt advertised|" + conn.advMdt.size() + " of " + wilMdt.size() + ", list = " + chgMdt.size() + ", accepted = " + accMdt.size() + " of " + conn.lrnMdt.size());
        l.add("nsh advertised|" + conn.advNsh.size() + " of " + wilNsh.size() + ", list = " + chgNsh.size() + ", accepted = " + accNsh.size() + " of " + conn.lrnNsh.size());
        l.add("rpd advertised|" + conn.advRpd.size() + " of " + wilRpd.size() + ", list = " + chgRpd.size() + ", accepted = " + accRpd.size() + " of " + conn.lrnRpd.size());
        l.add("spf advertised|" + conn.advSpf.size() + " of " + wilSpf.size() + ", list = " + chgSpf.size() + ", accepted = " + accSpf.size() + " of " + conn.lrnSpf.size());
        l.add("rtfilter advertised|" + conn.advRtf.size() + " of " + wilRtf.size() + ", list = " + chgRtf.size() + ", accepted = " + accRtf.size() + " of " + conn.lrnRtf.size());
        l.add("srte advertised|" + conn.advSrte.size() + " of " + wilSrte.size() + ", list = " + chgSrte.size() + ", accepted = " + accSrte.size() + " of " + conn.lrnSrte.size());
        l.add("linkstate advertised|" + conn.advLnks.size() + " of " + wilLnks.size() + ", list = " + chgLnks.size() + ", accepted = " + accLnks.size() + " of " + conn.lrnLnks.size());
        l.add("mvpn advertised|" + conn.advMvpn.size() + " of " + wilMvpn.size() + ", list = " + chgMvpn.size() + ", accepted = " + accMvpn.size() + " of " + conn.lrnMvpn.size());
        l.add("omvpn advertised|" + conn.advMvpo.size() + " of " + wilMvpo.size() + ", list = " + chgMvpo.size() + ", accepted = " + accMvpo.size() + " of " + conn.lrnMvpo.size());
        l.add("mtree advertised|" + conn.advMtre.size() + " of " + wilMtre.size() + ", list = " + chgMtre.size() + ", accepted = " + accMtre.size() + " of " + conn.lrnMtre.size());
        l.add("omtree advertised|" + conn.advMtro.size() + " of " + wilMtro.size() + ", list = " + chgMtro.size() + ", accepted = " + accMtro.size() + " of " + conn.lrnMtro.size());
        l.add("version|" + conn.adversion + " of " + lower.compRound + ", needfull=" + conn.needFull + ", buffull=" + conn.buffFull);
        l.add("full|" + fullCount + ", " + bits.time2str(cfgAll.timeZoneName, fullLast + cfgAll.timeServerOffset, 3) + ", " + bits.timePast(fullLast) + " ago, " + fullTime + " ms");
        l.add("incremental|" + incrCount + ", " + bits.time2str(cfgAll.timeZoneName, incrLast + cfgAll.timeServerOffset, 3) + ", " + bits.timePast(incrLast) + " ago, " + incrTime + " ms");
        l.add("advertise|" + advertCount + ", " + bits.time2str(cfgAll.timeZoneName, advertLast + cfgAll.timeServerOffset, 3) + ", " + bits.timePast(advertLast) + " ago");
        l.add("connection|" + conn.cntr.getShStat());
        l.add("lastio|" + conn.cntr.getShTraff());
        l.add("uncompressed|" + conn.compressCntr.getShStat());
        l.add("uncompress lastio|" + conn.compressCntr.getShTraff());
        l.add("buffer|" + pipeSide.getStatus(conn.pipe));
        l.add("policy reject|" + conn.repPolRej);
        l.add("aspath loop|" + conn.repAsPath);
        l.add("confed loop|" + conn.repAsConf);
        l.add("originator id|" + conn.repOrgnId);
        l.add("cluster list|" + conn.repClstrL);
        return l;
    }

    /**
     * update peer structures
     *
     * @param ifc interface to use
     */
    public void updateAddr(ipFwdIface ifc) {
        localIfc = ifc;
        localAddr = ifc.addr.copyBytes();
        ifc = lower.vrfCore.getOtherIface(lower.fwdCore, localIfc);
        if (ifc == null) {
            localOddr = localAddr.copyBytes();
        } else {
            localOddr = ifc.addr.copyBytes();
        }
        if (!fallOver) {
            return;
        }
        sendingIfc = ipFwdTab.findSendingIface(lower.fwdCore, peerAddr);
    }

    /**
     * update peer structures
     */
    public void updatePeer() {
        if (localAs != remoteAs) {
            peerType = rtrBgpUtil.peerExtrn;
            if (remoteConfed) {
                peerType = rtrBgpUtil.peerCnfed;
            }
            if (serverClnt) {
                peerType = rtrBgpUtil.peerServr;
            }
        } else {
            peerType = rtrBgpUtil.peerIntrn;
            if (reflectClnt) {
                peerType = rtrBgpUtil.peerRflct;
            }
        }
    }

    /**
     * start this neighbor process
     */
    public void startNow() {
        if (need2run) {
            return;
        }
        need2run = true;
        new Thread(this).start();
    }

    /**
     * stop this neighbor process
     */
    public void stopNow() {
        doStopNow();
        delListenPeer();
    }

    /**
     * stop peer
     */
    private void doStopNow() {
        need2run = false;
        shutdown = true;
    }

    /**
     * delete listening peer
     */
    public void delListenPeer() {
        if ((socketMode > 0) && (socketMode < 4)) {
            return;
        }
        logger.warn("removing dynamic " + peerAddr);
        doStopNow();
        rtrBgpNeigh old = lower.lstnNei.del(this);
        if (old == null) {
            return;
        }
        old.doStopNow();
    }

    public void run() {
        try {
            doWork();
        } catch (Exception e) {
            logger.traceback(e);
        }
        need2run = false;
        conn.closeNow();
    }

    private void doWork() {
        long lastKeep = 0;
        for (;;) {
            transmit.misleep(1000);
            long tim = bits.getTime();
            if ((lastKeep + conn.peerKeep) < tim) {
                if (conn.ready2adv) {
                    conn.sendKeepAlive();
                }
                lastKeep = tim - 1;
                prefixDampen();
            }
            if (!need2run) {
                return;
            }
            if (checkShutdown()) {
                if (conn.txFree() < 1) {
                    continue;
                }
                flapBgpConn();
                continue;
            }
            if (conn.txFree() >= 0) {
                doAdvert();
                continue;
            }
            lastKeep = 0;
            if (cfgInit.booting) {
                continue;
            }
            switch (socketMode) {
                case 1: // active
                    openConn(0);
                    break;
                case 2: // passive
                    openConn(60);
                    break;
                case 3: // both
                    if (!openConn(0)) {
                        break;
                    }
                    openConn(bits.random(randomStartF, randomStartL));
                    break;
                case 4: // dynamic
                case 5: // listen
                    doStopNow();
                    delListenPeer();
                    return;
            }
        }
    }

    private boolean openConn(int tim) {
        ipFwdIface ifc;
        if (srcIface == null) {
            ifc = ipFwdTab.findSendingIface(lower.fwdCore, peerAddr);
        } else {
            ifc = srcIface.getFwdIfc(peerAddr);
        }
        if (ifc == null) {
            return true;
        }
        if (ifc.addr == null) {
            return true;
        }
        pipeSide pipe;
        if (tim < 1) {
            if (proxy2use != null) {
                pipe = proxy2use.doConnect(servGeneric.protoTcp, proxy2adr, proxy2prt, "bgp");
            } else {
                pipe = lower.tcpCore.streamConnect(new pipeLine(bufferSize, false), ifc, 0, peerAddr, rtrBgp.port, "bgp", keyId, passwd, ttlSecurity, tosValue);
            }
        } else {
            if (proxy2use != null) {
                pipe = null;
                bits.sleep(tim);
            } else {
                prtAccept ac = new prtAccept(lower.tcpCore, new pipeLine(bufferSize, false), ifc, rtrBgp.port, peerAddr, 0, "bgp", keyId, passwd, ttlSecurity, tosValue);
                ac.wait4conn(tim * 1000);
                pipe = ac.getConn(true);
            }
        }
        if (pipe == null) {
            return true;
        }
        if (pipe.wait4ready(holdTimer)) {
            return true;
        }
        updateAddr(ifc);
        conn.closeNow();
        conn = new rtrBgpSpeak(lower, this, pipe, false);
        return false;
    }

    private boolean advertFullTable(int safi, long mask, tabRoute<addrIP> will, tabRoute<addrIP> done) {
        if ((conn.peerAfis & mask) == 0) {
            return false;
        }
        boolean needEor = false;
        boolean needEof = false;
        if (conn.needFull.get() < 2) {
            will = new tabRoute<addrIP>(will);
            needEor = (conn.needEorAfis & mask) != 0;
            needEof = (conn.needEofAfis & mask) != 0;
        }
        if (conn.addPthTx(safi)) {
            for (int i = 0; i < will.size(); i++) {
                tabRouteEntry<addrIP> wil = will.get(i);
                if (wil == null) {
                    continue;
                }
                tabRouteEntry<addrIP> don = done.find(wil);
                if (wil.differs(tabRoute.addType.alters, don) == 0) {
                    continue;
                }
                conn.sendUpdateAP(safi, wil, don);
                done.add(tabRoute.addType.always, wil, false, false);
                if (conn.txFree() < 1024) {
                    return true;
                }
            }
            for (int i = done.size() - 1; i >= 0; i--) {
                tabRouteEntry<addrIP> don = done.get(i);
                if (don == null) {
                    continue;
                }
                if (will.find(don) != null) {
                    continue;
                }
                done.del(don);
                conn.sendUpdateAP(safi, null, don);
                if (conn.txFree() < 1024) {
                    return true;
                }
            }
            if (needEor) {
                conn.sendEndOfRib(safi);
                conn.needEorAfis &= ~mask;
            }
            if (needEof) {
                conn.sendFreshMark(safi, 2);
                conn.needEofAfis &= ~mask;
            }
            return false;
        }
        List<tabRouteEntry<addrIP>> lst = new ArrayList<tabRouteEntry<addrIP>>();
        tabRouteEntry<addrIP> sen = null;
        for (int i = 0; i < will.size(); i++) {
            tabRouteEntry<addrIP> ntry = will.get(i);
            if (ntry == null) {
                continue;
            }
            if (ntry.differs(tabRoute.addType.notyet, done.find(ntry)) == 0) {
                continue;
            }
            if (sen != null) {
                sen.prefix = ntry.prefix;
            }
            if (ntry.differs(tabRoute.addType.notyet, sen) != 0) {
                if (lst.size() > 0) {
                    conn.sendUpdateSP(safi, lst, true);
                }
                if (conn.txFree() < 2048) {
                    return true;
                }
                lst.clear();
                sen = ntry.copyBytes(tabRoute.addType.notyet);
            }
            done.add(tabRoute.addType.always, ntry, false, false);
            lst.add(ntry);
            if (lst.size() > 64) {
                sen = null;
            }
        }
        if (lst.size() > 0) {
            conn.sendUpdateSP(safi, lst, true);
            if (conn.txFree() < 2048) {
                return true;
            }
        }
        lst.clear();
        for (int i = done.size() - 1; i >= 0; i--) {
            tabRouteEntry<addrIP> ntry = done.get(i);
            if (ntry == null) {
                continue;
            }
            if (will.find(ntry) != null) {
                continue;
            }
            done.del(ntry);
            lst.add(ntry);
            if (lst.size() < 64) {
                continue;
            }
            conn.sendUpdateSP(safi, lst, false);
            if (conn.txFree() < 2048) {
                return true;
            }
            lst.clear();
        }
        if (lst.size() > 0) {
            conn.sendUpdateSP(safi, lst, false);
            if (conn.txFree() < 2048) {
                return true;
            }
        }
        if (needEor) {
            conn.sendEndOfRib(safi);
            conn.needEorAfis &= ~mask;
        }
        if (needEof) {
            conn.sendFreshMark(safi, 2);
            conn.needEofAfis &= ~mask;
        }
        return false;
    }

    private boolean advertFull() {
        long tim = bits.getTime();
        if (advertFullTable(lower.afiUni, rtrBgpParam.mskUni, wilUni, conn.advUni)) {
            return true;
        }
        if (advertFullTable(lower.afiLab, rtrBgpParam.mskLab, wilUni, conn.advUni)) {
            return true;
        }
        if (advertFullTable(lower.afiCtp, rtrBgpParam.mskCtp, wilUni, conn.advUni)) {
            return true;
        }
        if (advertFullTable(lower.afiCar, rtrBgpParam.mskCar, wilUni, conn.advUni)) {
            return true;
        }
        if (advertFullTable(lower.afiMlt, rtrBgpParam.mskMlt, wilMlt, conn.advMlt)) {
            return true;
        }
        if (advertFullTable(lower.afiOlab, rtrBgpParam.mskOlab, wilOuni, conn.advOuni)) {
            return true;
        }
        if (advertFullTable(lower.afiOctp, rtrBgpParam.mskOctp, wilOuni, conn.advOuni)) {
            return true;
        }
        if (advertFullTable(lower.afiOcar, rtrBgpParam.mskOcar, wilOuni, conn.advOuni)) {
            return true;
        }
        if (advertFullTable(lower.afiOuni, rtrBgpParam.mskOuni, wilOuni, conn.advOuni)) {
            return true;
        }
        if (advertFullTable(lower.afiOmlt, rtrBgpParam.mskOmlt, wilOmlt, conn.advOmlt)) {
            return true;
        }
        if (advertFullTable(lower.afiFlw, rtrBgpParam.mskFlw, wilFlw, conn.advFlw)) {
            return true;
        }
        if (advertFullTable(lower.afiOflw, rtrBgpParam.mskOflw, wilOflw, conn.advOflw)) {
            return true;
        }
        if (advertFullTable(lower.afiSrte, rtrBgpParam.mskSrte, wilSrte, conn.advSrte)) {
            return true;
        }
        if (advertFullTable(lower.afiOsrt, rtrBgpParam.mskOsrt, wilOsrt, conn.advOsrt)) {
            return true;
        }
        if (advertFullTable(lower.afiVpnU, rtrBgpParam.mskVpnU, wilVpnU, conn.advVpnU)) {
            return true;
        }
        if (advertFullTable(lower.afiVpnM, rtrBgpParam.mskVpnM, wilVpnM, conn.advVpnM)) {
            return true;
        }
        if (advertFullTable(lower.afiVpnF, rtrBgpParam.mskVpnF, wilVpnF, conn.advVpnF)) {
            return true;
        }
        if (advertFullTable(lower.afiVpoU, rtrBgpParam.mskVpoU, wilVpoU, conn.advVpoU)) {
            return true;
        }
        if (advertFullTable(lower.afiVpoM, rtrBgpParam.mskVpoM, wilVpoM, conn.advVpoM)) {
            return true;
        }
        if (advertFullTable(lower.afiVpoF, rtrBgpParam.mskVpoF, wilVpoF, conn.advVpoF)) {
            return true;
        }
        if (advertFullTable(lower.afiVpls, rtrBgpParam.mskVpls, wilVpls, conn.advVpls)) {
            return true;
        }
        if (advertFullTable(lower.afiMspw, rtrBgpParam.mskMspw, wilMspw, conn.advMspw)) {
            return true;
        }
        if (advertFullTable(lower.afiEvpn, rtrBgpParam.mskEvpn, wilEvpn, conn.advEvpn)) {
            return true;
        }
        if (advertFullTable(lower.afiMdt, rtrBgpParam.mskMdt, wilMdt, conn.advMdt)) {
            return true;
        }
        if (advertFullTable(lower.afiNsh, rtrBgpParam.mskNsh, wilNsh, conn.advNsh)) {
            return true;
        }
        if (advertFullTable(lower.afiRpd, rtrBgpParam.mskRpd, wilRpd, conn.advRpd)) {
            return true;
        }
        if (advertFullTable(lower.afiSpf, rtrBgpParam.mskSpf, wilSpf, conn.advSpf)) {
            return true;
        }
        if (advertFullTable(lower.afiRtf, rtrBgpParam.mskRtf, wilRtf, conn.advRtf)) {
            return true;
        }
        if (advertFullTable(lower.afiMvpn, rtrBgpParam.mskMvpn, wilMvpn, conn.advMvpn)) {
            return true;
        }
        if (advertFullTable(lower.afiMvpo, rtrBgpParam.mskMvpo, wilMvpo, conn.advMvpo)) {
            return true;
        }
        if (advertFullTable(lower.afiMtre, rtrBgpParam.mskMtre, wilMtre, conn.advMtre)) {
            return true;
        }
        if (advertFullTable(lower.afiMtro, rtrBgpParam.mskMtro, wilMtro, conn.advMtro)) {
            return true;
        }
        if (advertFullTable(lower.afiLnks, rtrBgpParam.mskLnks, wilLnks, conn.advLnks)) {
            return true;
        }
        int ver = conn.needFull.ver();
        if (conn.needFull.get() == 1) {
            conn.needFull.setIf(0, ver);
        } else {
            conn.needFull.setIf(1, ver);
        }
        fullLast = bits.getTime();
        fullTime = (int) (fullLast - tim);
        fullCount++;
        return false;
    }

    private boolean advertIncrTable(int safi, long mask, tabRoute<addrIP> will, tabRoute<addrIP> chg, tabRoute<addrIP> done) {
        if ((conn.peerAfis & mask) == 0) {
            return false;
        }
        chg = new tabRoute<addrIP>(chg);
        if (conn.addPthTx(safi)) {
            for (int i = 0; i < chg.size(); i++) {
                tabRouteEntry<addrIP> cur = chg.get(i);
                if (cur == null) {
                    continue;
                }
                tabRouteEntry<addrIP> wil = will.find(cur);
                tabRouteEntry<addrIP> don = done.find(cur);
                if (wil == null) {
                    if (don == null) {
                        continue;
                    }
                    done.del(don);
                    conn.sendUpdateAP(safi, wil, don);
                } else {
                    if (wil.differs(tabRoute.addType.alters, don) == 0) {
                        continue;
                    }
                    done.add(tabRoute.addType.always, wil, false, false);
                    conn.sendUpdateAP(safi, wil, don);
                }
                if (conn.txFree() < 1024) {
                    return true;
                }
            }
            return false;
        }
        List<tabRouteEntry<addrIP>> lstA = new ArrayList<tabRouteEntry<addrIP>>();
        List<tabRouteEntry<addrIP>> lstW = new ArrayList<tabRouteEntry<addrIP>>();
        tabRouteEntry<addrIP> sen = null;
        for (int i = 0; i < chg.size(); i++) {
            tabRouteEntry<addrIP> cur = chg.get(i);
            if (cur == null) {
                continue;
            }
            tabRouteEntry<addrIP> wil = will.find(cur);
            tabRouteEntry<addrIP> don = done.find(cur);
            if (wil == null) {
                if (don == null) {
                    continue;
                }
                done.del(don);
                lstW.add(don);
                if (lstW.size() < 64) {
                    continue;
                }
                conn.sendUpdateSP(safi, lstW, false);
                if (conn.txFree() < 2048) {
                    return true;
                }
                lstW.clear();
                continue;
            }
            if (wil.differs(tabRoute.addType.notyet, don) == 0) {
                continue;
            }
            if (sen != null) {
                sen.prefix = wil.prefix;
            }
            if (wil.differs(tabRoute.addType.notyet, sen) != 0) {
                if (lstA.size() > 0) {
                    conn.sendUpdateSP(safi, lstA, true);
                }
                if (conn.txFree() < 2048) {
                    return true;
                }
                lstA.clear();
                sen = wil.copyBytes(tabRoute.addType.notyet);
            }
            done.add(tabRoute.addType.always, wil, false, false);
            lstA.add(wil);
            if (lstA.size() > 64) {
                sen = null;
            }
        }
        if (lstW.size() > 0) {
            conn.sendUpdateSP(safi, lstW, false);
            if (conn.txFree() < 2048) {
                return true;
            }
        }
        if (lstA.size() > 0) {
            conn.sendUpdateSP(safi, lstA, true);
            if (conn.txFree() < 2048) {
                return true;
            }
        }
        return false;
    }

    private boolean advertIncr() {
        long tim = bits.getTime();
        if (advertIncrTable(lower.afiUni, rtrBgpParam.mskUni, wilUni, chgUni, conn.advUni)) {
            return true;
        }
        if (advertIncrTable(lower.afiLab, rtrBgpParam.mskLab, wilUni, chgUni, conn.advUni)) {
            return true;
        }
        if (advertIncrTable(lower.afiCtp, rtrBgpParam.mskCtp, wilUni, chgUni, conn.advUni)) {
            return true;
        }
        if (advertIncrTable(lower.afiCar, rtrBgpParam.mskCar, wilUni, chgUni, conn.advUni)) {
            return true;
        }
        if (advertIncrTable(lower.afiMlt, rtrBgpParam.mskMlt, wilMlt, chgMlt, conn.advMlt)) {
            return true;
        }
        if (advertIncrTable(lower.afiOlab, rtrBgpParam.mskOlab, wilOuni, chgOuni, conn.advOuni)) {
            return true;
        }
        if (advertIncrTable(lower.afiOctp, rtrBgpParam.mskOctp, wilOuni, chgOuni, conn.advOuni)) {
            return true;
        }
        if (advertIncrTable(lower.afiOcar, rtrBgpParam.mskOcar, wilOuni, chgOuni, conn.advOuni)) {
            return true;
        }
        if (advertIncrTable(lower.afiOuni, rtrBgpParam.mskOuni, wilOuni, chgOuni, conn.advOuni)) {
            return true;
        }
        if (advertIncrTable(lower.afiOmlt, rtrBgpParam.mskOmlt, wilOmlt, chgOmlt, conn.advOmlt)) {
            return true;
        }
        if (advertIncrTable(lower.afiFlw, rtrBgpParam.mskFlw, wilFlw, chgFlw, conn.advFlw)) {
            return true;
        }
        if (advertIncrTable(lower.afiOflw, rtrBgpParam.mskOflw, wilOflw, chgOflw, conn.advOflw)) {
            return true;
        }
        if (advertIncrTable(lower.afiSrte, rtrBgpParam.mskSrte, wilSrte, chgSrte, conn.advSrte)) {
            return true;
        }
        if (advertIncrTable(lower.afiOsrt, rtrBgpParam.mskOsrt, wilOsrt, chgOsrt, conn.advOsrt)) {
            return true;
        }
        if (advertIncrTable(lower.afiVpnU, rtrBgpParam.mskVpnU, wilVpnU, chgVpnU, conn.advVpnU)) {
            return true;
        }
        if (advertIncrTable(lower.afiVpnM, rtrBgpParam.mskVpnM, wilVpnM, chgVpnM, conn.advVpnM)) {
            return true;
        }
        if (advertIncrTable(lower.afiVpnF, rtrBgpParam.mskVpnF, wilVpnF, chgVpnF, conn.advVpnF)) {
            return true;
        }
        if (advertIncrTable(lower.afiVpoU, rtrBgpParam.mskVpoU, wilVpoU, chgVpoU, conn.advVpoU)) {
            return true;
        }
        if (advertIncrTable(lower.afiVpoM, rtrBgpParam.mskVpoM, wilVpoM, chgVpoM, conn.advVpoM)) {
            return true;
        }
        if (advertIncrTable(lower.afiVpoF, rtrBgpParam.mskVpoF, wilVpoF, chgVpoF, conn.advVpoF)) {
            return true;
        }
        if (advertIncrTable(lower.afiVpls, rtrBgpParam.mskVpls, wilVpls, chgVpls, conn.advVpls)) {
            return true;
        }
        if (advertIncrTable(lower.afiMspw, rtrBgpParam.mskMspw, wilMspw, chgMspw, conn.advMspw)) {
            return true;
        }
        if (advertIncrTable(lower.afiEvpn, rtrBgpParam.mskEvpn, wilEvpn, chgEvpn, conn.advEvpn)) {
            return true;
        }
        if (advertIncrTable(lower.afiMdt, rtrBgpParam.mskMdt, wilMdt, chgMdt, conn.advMdt)) {
            return true;
        }
        if (advertIncrTable(lower.afiNsh, rtrBgpParam.mskNsh, wilNsh, chgNsh, conn.advNsh)) {
            return true;
        }
        if (advertIncrTable(lower.afiRpd, rtrBgpParam.mskRpd, wilRpd, chgRpd, conn.advRpd)) {
            return true;
        }
        if (advertIncrTable(lower.afiSpf, rtrBgpParam.mskSpf, wilSpf, chgSpf, conn.advSpf)) {
            return true;
        }
        if (advertIncrTable(lower.afiRtf, rtrBgpParam.mskRtf, wilRtf, chgRtf, conn.advRtf)) {
            return true;
        }
        if (advertIncrTable(lower.afiMvpn, rtrBgpParam.mskMvpn, wilMvpn, chgMvpn, conn.advMvpn)) {
            return true;
        }
        if (advertIncrTable(lower.afiMvpo, rtrBgpParam.mskMvpo, wilMvpo, chgMvpo, conn.advMvpo)) {
            return true;
        }
        if (advertIncrTable(lower.afiMtre, rtrBgpParam.mskMtre, wilMtre, chgMtre, conn.advMtre)) {
            return true;
        }
        if (advertIncrTable(lower.afiMtro, rtrBgpParam.mskMtro, wilMtro, chgMtro, conn.advMtro)) {
            return true;
        }
        if (advertIncrTable(lower.afiLnks, rtrBgpParam.mskLnks, wilLnks, chgLnks, conn.advLnks)) {
            return true;
        }
        incrLast = bits.getTime();
        incrTime = (int) (incrLast - tim);
        incrCount++;
        return false;
    }

    private void doAdvert() {
        if ((bits.getTime() - conn.lastRx) > holdTimer) {
            conn.sendNotify(4, 0);
            return;
        }
        int doing = lower.compRound.get();
        if (doing == conn.adversion.get()) {
            return;
        }
        if (conn.txFree() < (bufferSize / 2)) {
            conn.needFull.add(1);
            conn.buffFull++;
            return;
        }
        if (unidirection && (conn.rxReady() > (bufferSize / 8))) {
            conn.needFull.add(1);
            return;
        }
        if (advertIntTx > 0) {
            if ((bits.getTime() - advertLast) < advertIntTx) {
                return;
            }
        }
        boolean b;
        long advs = conn.cntr.packTx;
        if (conn.needFull.get() != 0) {
            b = advertFull();
        } else {
            b = advertIncr();
        }
        if (maxPrxOutCnt > 0) {
            int i = conn.getPrefixSent();
            if (i > ((maxPrxOutCnt * maxPrxOutPrc) / 100)) {
                logger.info("neighbor " + peerAddr + " got " + i + " prefixes");
            }
            if (i > maxPrxOutCnt) {
                conn.sendNotify(6, 1);
            }
        }
        advs = conn.cntr.packTx - advs;
        if (advs > 0) {
            advertLast = bits.getTime();
            advertCount++;
        }
        if (b) {
            conn.needFull.add(1);
            return;
        }
        if (conn.needFull.get() != 0) {
            doing--;
        }
        conn.adversion.set(doing);
    }

    /**
     * check if labeled peer
     *
     * @return true if yes, false if no
     */
    public boolean getLabeledPeer() {
        if ((conn.peerAfis & rtrBgpParam.mskLab) != 0) {
            return true;
        }
        if ((conn.peerAfis & rtrBgpParam.mskCtp) != 0) {
            return true;
        }
        if ((conn.peerAfis & rtrBgpParam.mskCar) != 0) {
            return true;
        }
        return false;
    }

    private void addUpdateTableUni(int afi, long mask, tabRoute<addrIP> trg, tabRoute<addrIP> src, tabListing<tabRtrmapN, addrIP> rouMap, tabListing<tabRtrplcN, addrIP> rouPlc, tabListing<tabPrfxlstN, addrIP> prfLst) {
        if ((conn.peerAfis & mask) == 0) {
            return;
        }
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, afi, remoteAs, trg, src, true, rouMap, rouPlc, prfLst);
    }

    /**
     * set accepted list
     */
    public void setAccepted() {
        accUni = new tabRoute<addrIP>("bgp");
        accMlt = new tabRoute<addrIP>("bgp");
        accOuni = new tabRoute<addrIP>("bgp");
        accOmlt = new tabRoute<addrIP>("bgp");
        accOflw = new tabRoute<addrIP>("bgp");
        accOsrt = new tabRoute<addrIP>("bgp");
        accFlw = new tabRoute<addrIP>("bgp");
        accVpnU = new tabRoute<addrIP>("bgp");
        accVpnM = new tabRoute<addrIP>("bgp");
        accVpnF = new tabRoute<addrIP>("bgp");
        accVpoU = new tabRoute<addrIP>("bgp");
        accVpoM = new tabRoute<addrIP>("bgp");
        accVpoF = new tabRoute<addrIP>("bgp");
        accVpls = new tabRoute<addrIP>("bgp");
        accMspw = new tabRoute<addrIP>("bgp");
        accEvpn = new tabRoute<addrIP>("bgp");
        accMdt = new tabRoute<addrIP>("bgp");
        accNsh = new tabRoute<addrIP>("bgp");
        accRpd = new tabRoute<addrIP>("bgp");
        accSpf = new tabRoute<addrIP>("bgp");
        accRtf = new tabRoute<addrIP>("bgp");
        accSrte = new tabRoute<addrIP>("bgp");
        accLnks = new tabRoute<addrIP>("bgp");
        accMvpn = new tabRoute<addrIP>("bgp");
        accMvpo = new tabRoute<addrIP>("bgp");
        accMtre = new tabRoute<addrIP>("bgp");
        accMtro = new tabRoute<addrIP>("bgp");
        rtfilterUsed = null;
        reachable = false;
        if (sendingIfc != null) {
            ipFwdIface ifc = ipFwdTab.findSendingIface(lower.fwdCore, peerAddr);
            if (ifc == null) {
                return;
            }
            if (ifc.ifwNum != sendingIfc.ifwNum) {
                return;
            }
        }
        if (trackNxthop) {
            if (lower.nhtRoumap != null) {
                tabRouteEntry<addrIP> rou = lower.fwdCore.actualU.route(peerAddr);
                if (rou == null) {
                    return;
                }
                if (!lower.nhtRoumap.matches(lower.afiUni, remoteAs, rou)) {
                    return;
                }
            }
            if (lower.nhtRouplc != null) {
                tabRouteEntry<addrIP> rou = lower.fwdCore.actualU.route(peerAddr);
                if (rou == null) {
                    return;
                }
                rou = tabRtrplc.doRpl(lower.afiUni, remoteAs, rou, lower.nhtRouplc, true);
                if (rou == null) {
                    return;
                }
            }
            if (lower.nhtPfxlst != null) {
                tabRouteEntry<addrIP> rou = lower.fwdCore.actualU.route(peerAddr);
                if (rou == null) {
                    return;
                }
                if (!lower.nhtPfxlst.matches(lower.afiUni, remoteAs, rou)) {
                    return;
                }
            }
        }
        reachable = true;
        if (!softReconfig) {
            accUni = conn.lrnUni;
            accMlt = conn.lrnMlt;
            accOuni = conn.lrnOuni;
            accOmlt = conn.lrnOmlt;
            accOflw = conn.lrnOflw;
            accOsrt = conn.lrnOsrt;
            accFlw = conn.lrnFlw;
            accVpnU = conn.lrnVpnU;
            accVpnM = conn.lrnVpnM;
            accVpnF = conn.lrnVpnF;
            accVpoU = conn.lrnVpoU;
            accVpoM = conn.lrnVpoM;
            accVpoF = conn.lrnVpoF;
            accVpls = conn.lrnVpls;
            accMspw = conn.lrnMspw;
            accEvpn = conn.lrnEvpn;
            accMdt = conn.lrnMdt;
            accNsh = conn.lrnNsh;
            accRpd = conn.lrnRpd;
            accSpf = conn.lrnSpf;
            accRtf = conn.lrnRtf;
            accSrte = conn.lrnSrte;
            accLnks = conn.lrnLnks;
            accMvpn = conn.lrnMvpn;
            accMvpo = conn.lrnMvpo;
            accMtre = conn.lrnMtre;
            accMtro = conn.lrnMtro;
            if (rtfilterOut && ((conn.peerAfis & rtrBgpParam.mskRtf) != 0)) {
                rtfilterUsed = accRtf;
            }
            return;
        }
        setValidity(accUni, rpkiIn, lower.rpkiA);
        setValidity(accMlt, rpkiIn, lower.rpkiA);
        setValidity(accOuni, rpkiIn, lower.rpkiO);
        setValidity(accOmlt, rpkiIn, lower.rpkiO);
        setValidity(accVpnU, vpkiIn, lower.rpkiA);
        setValidity(accVpnM, vpkiIn, lower.rpkiA);
        setValidity(accVpoU, vpkiIn, lower.rpkiO);
        setValidity(accVpoM, vpkiIn, lower.rpkiO);
        addUpdateTableUni(lower.afiUni, rtrBgpParam.mskUni, accUni, conn.lrnUni, roumapIn, roupolIn, prflstIn);
        addUpdateTableUni(lower.afiLab, rtrBgpParam.mskLab, accUni, conn.lrnUni, roumapIn, roupolIn, prflstIn);
        addUpdateTableUni(lower.afiCtp, rtrBgpParam.mskCtp, accUni, conn.lrnUni, roumapIn, roupolIn, prflstIn);
        addUpdateTableUni(lower.afiCar, rtrBgpParam.mskCar, accUni, conn.lrnUni, roumapIn, roupolIn, prflstIn);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiMlt, remoteAs, accMlt, conn.lrnMlt, true, roumapIn, roupolIn, prflstIn);
        addUpdateTableUni(lower.afiOuni, rtrBgpParam.mskOuni, accOuni, conn.lrnOuni, oroumapIn, oroupolIn, oprflstIn);
        addUpdateTableUni(lower.afiOlab, rtrBgpParam.mskOlab, accOuni, conn.lrnOuni, oroumapIn, oroupolIn, oprflstIn);
        addUpdateTableUni(lower.afiOctp, rtrBgpParam.mskOctp, accOuni, conn.lrnOuni, oroumapIn, oroupolIn, oprflstIn);
        addUpdateTableUni(lower.afiOcar, rtrBgpParam.mskOcar, accOuni, conn.lrnOuni, oroumapIn, oroupolIn, oprflstIn);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiOmlt, remoteAs, accOmlt, conn.lrnOmlt, true, oroumapIn, oroupolIn, oprflstIn);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiOflw, remoteAs, accOflw, conn.lrnOflw, true, wroumapIn, wroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiOsrt, remoteAs, accOsrt, conn.lrnOsrt, true, wroumapIn, wroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiFlw, remoteAs, accFlw, conn.lrnFlw, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiVpnU, remoteAs, accVpnU, conn.lrnVpnU, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiVpnM, remoteAs, accVpnM, conn.lrnVpnM, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiVpnF, remoteAs, accVpnF, conn.lrnVpnF, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiVpoU, remoteAs, accVpoU, conn.lrnVpoU, true, wroumapIn, wroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiVpoM, remoteAs, accVpoM, conn.lrnVpoM, true, wroumapIn, wroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiVpoF, remoteAs, accVpoF, conn.lrnVpoF, true, wroumapIn, wroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiVpls, remoteAs, accVpls, conn.lrnVpls, true, eroumapIn, eroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiMspw, remoteAs, accMspw, conn.lrnMspw, true, eroumapIn, eroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiEvpn, remoteAs, accEvpn, conn.lrnEvpn, true, eroumapIn, eroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiMdt, remoteAs, accMdt, conn.lrnMdt, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiNsh, remoteAs, accNsh, conn.lrnNsh, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiRpd, remoteAs, accRpd, conn.lrnRpd, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiSpf, remoteAs, accSpf, conn.lrnSpf, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiRtf, remoteAs, accRtf, conn.lrnRtf, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiSrte, remoteAs, accSrte, conn.lrnSrte, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiLnks, remoteAs, accLnks, conn.lrnLnks, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiMvpn, remoteAs, accMvpn, conn.lrnMvpn, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiMvpo, remoteAs, accMvpo, conn.lrnMvpo, true, wroumapIn, wroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiMtre, remoteAs, accMtre, conn.lrnMtre, true, vroumapIn, vroupolIn, null);
        tabRoute.addUpdatedTable(tabRoute.addType.ecmp, lower.afiMtro, remoteAs, accMtro, conn.lrnMtro, true, wroumapIn, wroupolIn, null);
        if (rtfilterOut && ((conn.peerAfis & rtrBgpParam.mskRtf) != 0)) {
            rtfilterUsed = accRtf;
        }
        if (dampenPfxs == null) {
            return;
        }
        for (int i = 0; i < dampenPfxs.size(); i++) {
            rtrBgpDamp ntry = dampenPfxs.get(i);
            if (ntry == null) {
                continue;
            }
            if (!ntry.dampened) {
                continue;
            }
            tabRoute<addrIP> lst = getAccepted(ntry.afi);
            if (lst == null) {
                continue;
            }
            tabRouteEntry<addrIP> prf = new tabRouteEntry<addrIP>();
            prf.rouDst = ntry.rd;
            prf.prefix = ntry.prefix;
            lst.del(prf);
        }
    }

    /**
     * set merged list
     */
    public void setMerge() {
        tabRoute.addType mod;
        if (lower.routerEcmp) {
            mod = tabRoute.addType.lnkEcmp;
        } else {
            mod = tabRoute.addType.lnkBcmp;
        }
        lower.newlyUni.mergeFrom(mod, new tabRoute<addrIP>(accUni), tabRouteAttr.distanLim);
        lower.newlyMlt.mergeFrom(mod, new tabRoute<addrIP>(accMlt), tabRouteAttr.distanLim);
        lower.newlyOuni.mergeFrom(mod, new tabRoute<addrIP>(accOuni), tabRouteAttr.distanLim);
        lower.newlyOmlt.mergeFrom(mod, new tabRoute<addrIP>(accOmlt), tabRouteAttr.distanLim);
        lower.newlyOflw.mergeFrom(mod, new tabRoute<addrIP>(accOflw), tabRouteAttr.distanLim);
        lower.newlyOsrt.mergeFrom(mod, new tabRoute<addrIP>(accOsrt), tabRouteAttr.distanLim);
        lower.newlyFlw.mergeFrom(mod, new tabRoute<addrIP>(accFlw), tabRouteAttr.distanLim);
        lower.newlyVpnU.mergeFrom(mod, new tabRoute<addrIP>(accVpnU), tabRouteAttr.distanLim);
        lower.newlyVpnM.mergeFrom(mod, new tabRoute<addrIP>(accVpnM), tabRouteAttr.distanLim);
        lower.newlyVpnF.mergeFrom(mod, new tabRoute<addrIP>(accVpnF), tabRouteAttr.distanLim);
        lower.newlyVpoU.mergeFrom(mod, new tabRoute<addrIP>(accVpoU), tabRouteAttr.distanLim);
        lower.newlyVpoM.mergeFrom(mod, new tabRoute<addrIP>(accVpoM), tabRouteAttr.distanLim);
        lower.newlyVpoF.mergeFrom(mod, new tabRoute<addrIP>(accVpoF), tabRouteAttr.distanLim);
        lower.newlyVpls.mergeFrom(mod, new tabRoute<addrIP>(accVpls), tabRouteAttr.distanLim);
        lower.newlyMspw.mergeFrom(mod, new tabRoute<addrIP>(accMspw), tabRouteAttr.distanLim);
        lower.newlyEvpn.mergeFrom(mod, new tabRoute<addrIP>(accEvpn), tabRouteAttr.distanLim);
        lower.newlyMdt.mergeFrom(mod, new tabRoute<addrIP>(accMdt), tabRouteAttr.distanLim);
        lower.newlyNsh.mergeFrom(mod, new tabRoute<addrIP>(accNsh), tabRouteAttr.distanLim);
        lower.newlyRpd.mergeFrom(mod, new tabRoute<addrIP>(accRpd), tabRouteAttr.distanLim);
        lower.newlySpf.mergeFrom(mod, new tabRoute<addrIP>(accSpf), tabRouteAttr.distanLim);
        lower.newlyRtf.mergeFrom(mod, new tabRoute<addrIP>(accRtf), tabRouteAttr.distanLim);
        lower.newlySrte.mergeFrom(mod, new tabRoute<addrIP>(accSrte), tabRouteAttr.distanLim);
        lower.newlyLnks.mergeFrom(mod, new tabRoute<addrIP>(accLnks), tabRouteAttr.distanLim);
        lower.newlyMvpn.mergeFrom(mod, new tabRoute<addrIP>(accMvpn), tabRouteAttr.distanLim);
        lower.newlyMvpo.mergeFrom(mod, new tabRoute<addrIP>(accMvpo), tabRouteAttr.distanLim);
        lower.newlyMtre.mergeFrom(mod, new tabRoute<addrIP>(accMtre), tabRouteAttr.distanLim);
        lower.newlyMtro.mergeFrom(mod, new tabRoute<addrIP>(accMtro), tabRouteAttr.distanLim);
    }

    /**
     * set group membership
     */
    public void setGroup() {
        lower.have2reflect |= peerType == rtrBgpUtil.peerRflct;
        if (reachable != reachOld) {
            reachOld = reachable;
            reachTim = bits.getTime();
            reachNum++;
            if (debugger.rtrBgpEvnt) {
                logger.debug("reachable neighbor " + peerAddr + " " + reachable);
            }
        }
        groupMember = -1;
        if (checkShutdown()) {
            return;
        }
        if (conn == null) {
            return;
        }
        if (!conn.ready2adv) {
            return;
        }
        for (int i = 0; i < lower.groups.size(); i++) {
            rtrBgpGroup ntry = lower.groups.get(i);
            if (ntry.peerType != peerType) {
                continue;
            }
            if (nxtHopSelf || (!nxtHopUnchgd)) {
                if (localAddr.compareTo(ntry.localAddr) != 0) {
                    continue;
                }
                if (localOddr.compareTo(ntry.localOddr) != 0) {
                    continue;
                }
            }
            if (ntry.sameOutput(this)) {
                continue;
            }
            groupMember = i;
            return;
        }
        groupMember = lower.groups.size();
        rtrBgpGroup ntry = new rtrBgpGroup(lower, groupMember);
        ntry.copyFrom(this);
        ntry.peerType = peerType;
        ntry.localAddr = localAddr.copyBytes();
        ntry.localOddr = localOddr.copyBytes();
        lower.groups.add(ntry);
    }

    /**
     * set needed prefixes
     */
    public void setNeeded() {
        if (groupMember < 0) {
            wilUni = new tabRoute<addrIP>("tx");
            wilMlt = new tabRoute<addrIP>("tx");
            wilOuni = new tabRoute<addrIP>("tx");
            wilOmlt = new tabRoute<addrIP>("tx");
            wilOflw = new tabRoute<addrIP>("tx");
            wilOsrt = new tabRoute<addrIP>("tx");
            wilFlw = new tabRoute<addrIP>("tx");
            wilVpnU = new tabRoute<addrIP>("tx");
            wilVpnM = new tabRoute<addrIP>("tx");
            wilVpnF = new tabRoute<addrIP>("tx");
            wilVpoU = new tabRoute<addrIP>("tx");
            wilVpoM = new tabRoute<addrIP>("tx");
            wilVpoF = new tabRoute<addrIP>("tx");
            wilVpls = new tabRoute<addrIP>("tx");
            wilMspw = new tabRoute<addrIP>("tx");
            wilEvpn = new tabRoute<addrIP>("tx");
            wilMdt = new tabRoute<addrIP>("tx");
            wilNsh = new tabRoute<addrIP>("tx");
            wilRpd = new tabRoute<addrIP>("tx");
            wilSpf = new tabRoute<addrIP>("tx");
            wilRtf = new tabRoute<addrIP>("tx");
            wilSrte = new tabRoute<addrIP>("tx");
            wilLnks = new tabRoute<addrIP>("tx");
            wilMvpn = new tabRoute<addrIP>("tx");
            wilMvpo = new tabRoute<addrIP>("tx");
            wilMtre = new tabRoute<addrIP>("tx");
            wilMtro = new tabRoute<addrIP>("tx");
            chgUni = new tabRoute<addrIP>("chg");
            chgMlt = new tabRoute<addrIP>("chg");
            chgOuni = new tabRoute<addrIP>("chg");
            chgOmlt = new tabRoute<addrIP>("chg");
            chgOflw = new tabRoute<addrIP>("chg");
            chgOsrt = new tabRoute<addrIP>("chg");
            chgFlw = new tabRoute<addrIP>("chg");
            chgVpnU = new tabRoute<addrIP>("chg");
            chgVpnM = new tabRoute<addrIP>("chg");
            chgVpnF = new tabRoute<addrIP>("chg");
            chgVpoU = new tabRoute<addrIP>("chg");
            chgVpoM = new tabRoute<addrIP>("chg");
            chgVpoF = new tabRoute<addrIP>("chg");
            chgVpls = new tabRoute<addrIP>("chg");
            chgMspw = new tabRoute<addrIP>("chg");
            chgEvpn = new tabRoute<addrIP>("chg");
            chgMdt = new tabRoute<addrIP>("chg");
            chgNsh = new tabRoute<addrIP>("chg");
            chgRpd = new tabRoute<addrIP>("chg");
            chgSpf = new tabRoute<addrIP>("chg");
            chgRtf = new tabRoute<addrIP>("chg");
            chgSrte = new tabRoute<addrIP>("chg");
            chgLnks = new tabRoute<addrIP>("chg");
            chgMvpn = new tabRoute<addrIP>("chg");
            chgMvpo = new tabRoute<addrIP>("chg");
            chgMtre = new tabRoute<addrIP>("chg");
            chgMtro = new tabRoute<addrIP>("chg");
        } else {
            rtrBgpGroup grp = lower.groups.get(groupMember);
            wilUni = grp.wilUni;
            wilMlt = grp.wilMlt;
            wilOuni = grp.wilOuni;
            wilOmlt = grp.wilOmlt;
            wilOflw = grp.wilOflw;
            wilOsrt = grp.wilOsrt;
            wilFlw = grp.wilFlw;
            wilVpnU = grp.wilVpnU;
            wilVpnM = grp.wilVpnM;
            wilVpnF = grp.wilVpnF;
            wilVpoU = grp.wilVpoU;
            wilVpoM = grp.wilVpoM;
            wilVpoF = grp.wilVpoF;
            wilVpls = grp.wilVpls;
            wilMspw = grp.wilMspw;
            wilEvpn = grp.wilEvpn;
            wilMdt = grp.wilMdt;
            wilNsh = grp.wilNsh;
            wilRpd = grp.wilRpd;
            wilSpf = grp.wilSpf;
            wilRtf = grp.wilRtf;
            wilSrte = grp.wilSrte;
            wilLnks = grp.wilLnks;
            wilMvpn = grp.wilMvpn;
            wilMvpo = grp.wilMvpo;
            wilMtre = grp.wilMtre;
            wilMtro = grp.wilMtro;
            chgUni = grp.chgUni;
            chgMlt = grp.chgMlt;
            chgOuni = grp.chgOuni;
            chgOmlt = grp.chgOmlt;
            chgOflw = grp.chgOflw;
            chgOsrt = grp.chgOsrt;
            chgFlw = grp.chgFlw;
            chgVpnU = grp.chgVpnU;
            chgVpnM = grp.chgVpnM;
            chgVpnF = grp.chgVpnF;
            chgVpoU = grp.chgVpoU;
            chgVpoM = grp.chgVpoM;
            chgVpoF = grp.chgVpoF;
            chgVpls = grp.chgVpls;
            chgMspw = grp.chgMspw;
            chgEvpn = grp.chgEvpn;
            chgMdt = grp.chgMdt;
            chgNsh = grp.chgNsh;
            chgRpd = grp.chgRpd;
            chgSpf = grp.chgSpf;
            chgRtf = grp.chgRtf;
            chgSrte = grp.chgSrte;
            chgLnks = grp.chgLnks;
            chgMvpn = grp.chgMvpn;
            chgMvpo = grp.chgMvpo;
            chgMtre = grp.chgMtre;
            chgMtro = grp.chgMtro;
        }
        conn.needFull.add(1);
    }

    /**
     * validate a prefix
     *
     * @param afi address family
     * @param ntry route entry
     */
    public void setValidity(int afi, tabRouteEntry<addrIP> ntry) {
        if (lower.rpkiR == null) {
            return;
        }
        if ((afi == lower.afiUni) || (afi == lower.afiMlt)) {
            tabRpkiUtil.setValidityRoute(localAs, ntry, lower.rpkiA, lower.rpkiP, rpkiIn);
        }
        if ((afi == lower.afiOuni) || (afi == lower.afiOmlt)) {
            tabRpkiUtil.setValidityRoute(localAs, ntry, lower.rpkiO, lower.rpkiP, rpkiIn);
        }
        if ((afi == lower.afiVpnU) || (afi == lower.afiVpnM)) {
            tabRpkiUtil.setValidityRoute(localAs, ntry, lower.rpkiA, lower.rpkiP, vpkiIn);
        }
        if ((afi == lower.afiVpoU) || (afi == lower.afiVpoM)) {
            tabRpkiUtil.setValidityRoute(localAs, ntry, lower.rpkiO, lower.rpkiP, vpkiIn);
        }
    }

    /**
     * set validity
     *
     * @param tab table to use
     * @param mod mode to use
     * @param roa roa to use
     */
    public void setValidity(tabRoute<addrIP> tab, int mod, tabGen<tabRpkiRoa> roa) {
        if (lower.rpkiR == null) {
            return;
        }
        tabRpkiUtil.setValidityTable(localAs, tab, roa, lower.rpkiP, mod);
    }

    /**
     * group version
     */
    public void setGrpVer() {
        if (groupMember < 0) {
            return;
        }
        if (conn.needFull.get() > 1) {
            return;
        }
        rtrBgpGroup grp = lower.groups.get(groupMember);
        int i = conn.adversion.get();
        if (i < grp.minversion) {
            grp.minversion = i;
        }
    }

    /**
     * update dampening statistics
     *
     * @param afi afi
     * @param rd rd
     * @param prf prefix
     * @param pnlt penalty
     */
    protected void prefixDampen(int afi, long rd, addrPrefix<addrIP> prf, int pnlt) {
        rtrBgpDamp ntry = new rtrBgpDamp(afi, rd, prf);
        rtrBgpDamp old = dampenPfxs.add(ntry);
        if (old != null) {
            ntry = old;
        }
        ntry.penalty += pnlt;
        if (ntry.penalty > dampenMaxp) {
            ntry.penalty = dampenMaxp;
        }
        if (!ntry.dampened && (ntry.penalty > dampenSupp)) {
            ntry.dampened = true;
            if (debugger.rtrBgpDamp) {
                logger.debug("suppressing " + tabRouteUtil.rd2string(ntry.rd) + " " + ntry.prefix);
            }
        }
        ntry.last = bits.getTime();
    }

    /**
     * update dampening statistics
     */
    protected void prefixDampen() {
        if (dampenPfxs == null) {
            return;
        }
        long tim = bits.getTime();
        for (int i = dampenPfxs.size() - 1; i >= 0; i--) {
            rtrBgpDamp ntry = dampenPfxs.get(i);
            if (ntry == null) {
                continue;
            }
            if (ntry.last > (tim - dampenHalf)) {
                continue;
            }
            ntry.last = tim;
            ntry.penalty = ntry.penalty / 2;
            if (ntry.dampened && (ntry.penalty < dampenReus)) {
                ntry.dampened = false;
                if (debugger.rtrBgpDamp) {
                    logger.debug("unsuppressing " + tabRouteUtil.rd2string(ntry.rd) + " " + ntry.prefix);
                }
            }
            if (ntry.penalty < dampenMinp) {
                dampenPfxs.del(ntry);
                if (debugger.rtrBgpDamp) {
                    logger.debug("forgetting " + tabRouteUtil.rd2string(ntry.rd) + " " + ntry.prefix);
                }
            }
        }
    }

    /**
     * get accepted
     *
     * @param safi safi to query
     * @return table
     */
    public tabRoute<addrIP> getAccepted(int safi) {
        if (safi == lower.afiUni) {
            return accUni;
        }
        if (safi == lower.afiLab) {
            return accUni;
        }
        if (safi == lower.afiCtp) {
            return accUni;
        }
        if (safi == lower.afiCar) {
            return accUni;
        }
        if (safi == lower.afiMlt) {
            return accMlt;
        }
        if (safi == lower.afiOlab) {
            return accOuni;
        }
        if (safi == lower.afiOctp) {
            return accOuni;
        }
        if (safi == lower.afiOcar) {
            return accOuni;
        }
        if (safi == lower.afiOuni) {
            return accOuni;
        }
        if (safi == lower.afiOmlt) {
            return accOmlt;
        }
        if (safi == lower.afiOflw) {
            return accOflw;
        }
        if (safi == lower.afiOsrt) {
            return accOsrt;
        }
        if (safi == lower.afiFlw) {
            return accFlw;
        }
        if (safi == lower.afiVpnU) {
            return accVpnU;
        }
        if (safi == lower.afiVpnM) {
            return accVpnM;
        }
        if (safi == lower.afiVpnF) {
            return accVpnF;
        }
        if (safi == lower.afiVpoU) {
            return accVpoU;
        }
        if (safi == lower.afiVpoM) {
            return accVpoM;
        }
        if (safi == lower.afiVpoF) {
            return accVpoF;
        }
        if (safi == lower.afiVpls) {
            return accVpls;
        }
        if (safi == lower.afiMspw) {
            return accMspw;
        }
        if (safi == lower.afiEvpn) {
            return accEvpn;
        }
        if (safi == lower.afiMdt) {
            return accMdt;
        }
        if (safi == lower.afiNsh) {
            return accNsh;
        }
        if (safi == lower.afiRpd) {
            return accRpd;
        }
        if (safi == lower.afiSpf) {
            return accSpf;
        }
        if (safi == lower.afiRtf) {
            return accRtf;
        }
        if (safi == lower.afiSrte) {
            return accSrte;
        }
        if (safi == lower.afiLnks) {
            return accLnks;
        }
        if (safi == lower.afiMvpn) {
            return accMvpn;
        }
        if (safi == lower.afiMvpo) {
            return accMvpo;
        }
        if (safi == lower.afiMtre) {
            return accMtre;
        }
        if (safi == lower.afiMtro) {
            return accMtro;
        }
        logger.info("unknown safi (" + safi + ") requested");
        return null;
    }

    /**
     * get willing
     *
     * @param safi safi to query
     * @return table
     */
    public tabRoute<addrIP> getWilling(int safi) {
        if (safi == lower.afiUni) {
            return wilUni;
        }
        if (safi == lower.afiLab) {
            return wilUni;
        }
        if (safi == lower.afiCtp) {
            return wilUni;
        }
        if (safi == lower.afiCar) {
            return wilUni;
        }
        if (safi == lower.afiMlt) {
            return wilMlt;
        }
        if (safi == lower.afiOlab) {
            return wilOuni;
        }
        if (safi == lower.afiOctp) {
            return wilOuni;
        }
        if (safi == lower.afiOcar) {
            return wilOuni;
        }
        if (safi == lower.afiOuni) {
            return wilOuni;
        }
        if (safi == lower.afiOmlt) {
            return wilOmlt;
        }
        if (safi == lower.afiOflw) {
            return wilOflw;
        }
        if (safi == lower.afiOsrt) {
            return wilOsrt;
        }
        if (safi == lower.afiFlw) {
            return wilFlw;
        }
        if (safi == lower.afiVpnU) {
            return wilVpnU;
        }
        if (safi == lower.afiVpnM) {
            return wilVpnM;
        }
        if (safi == lower.afiVpnF) {
            return wilVpnF;
        }
        if (safi == lower.afiVpoU) {
            return wilVpoU;
        }
        if (safi == lower.afiVpoM) {
            return wilVpoM;
        }
        if (safi == lower.afiVpoF) {
            return wilVpoF;
        }
        if (safi == lower.afiVpls) {
            return wilVpls;
        }
        if (safi == lower.afiMspw) {
            return wilMspw;
        }
        if (safi == lower.afiEvpn) {
            return wilEvpn;
        }
        if (safi == lower.afiMdt) {
            return wilMdt;
        }
        if (safi == lower.afiNsh) {
            return wilNsh;
        }
        if (safi == lower.afiRpd) {
            return wilRpd;
        }
        if (safi == lower.afiSpf) {
            return wilSpf;
        }
        if (safi == lower.afiRtf) {
            return wilRtf;
        }
        if (safi == lower.afiSrte) {
            return wilSrte;
        }
        if (safi == lower.afiLnks) {
            return wilLnks;
        }
        if (safi == lower.afiMvpn) {
            return wilMvpn;
        }
        if (safi == lower.afiMvpo) {
            return wilMvpo;
        }
        if (safi == lower.afiMtre) {
            return wilMtre;
        }
        if (safi == lower.afiMtro) {
            return wilMtro;
        }
        logger.info("unknown safi (" + safi + ") requested");
        return null;
    }

    private String tabSiz(tabRoute<addrIP> tab) {
        if (tab == null) {
            return "-";
        }
        return "" + tab.size();
    }

    /**
     * neighbor list entry
     *
     * @param safi safi to query
     * @return line of string
     */
    public String showNeighs(int safi) {
        return showSummry1() + "|" + tabSiz(conn.getLearned(safi)) + "|" + tabSiz(getAccepted(safi)) + "|" + tabSiz(getWilling(safi)) + "|" + tabSiz(conn.getAdverted(safi)) + "|" + bits.timePast(conn.upTime);
    }

    /**
     * check readiness status
     *
     * @return peer readiness string
     */
    public String getReadiness() {
        if (shutdown) {
            return "admin";
        }
        return cmds.upDown(conn.ready2adv);
    }

    /**
     * neighbor list entry
     *
     * @return line of string
     */
    public String showSummry1() {
        return peerAddr + "|" + bits.num2str(remoteAs);
    }

    /**
     * neighbor list entry
     *
     * @return line of string
     */
    public String showSummry2() {
        return showSummry1() + "|" + getReadiness() + "|" + conn.getPrefixGot() + "|" + conn.getPrefixSent() + "|" + bits.timePast(conn.upTime);
    }

    /**
     * neighbor list entry
     *
     * @param mod mode
     * @return line of string
     */
    public String showSummary(int mod) {
        switch (mod) {
            case 1:
                return showSummry1() + "|" + rtrBgpParam.mask2string(conn.peerAfis) + "|" + rtrBgpParam.mask2string(addrFams - conn.peerAfis) + "|" + rtrBgpParam.mask2string(conn.originalSafiList - conn.peerAfis);
            case 2:
                return showSummry1() + "|" + groupMember + "|" + socketMode + "|" + bits.timePast(conn.upTime);
            case 3:
                return showSummry1() + "|" + reachable + "|" + bits.timePast(reachTim) + "|" + reachNum + "|" + sessNum + "|" + bits.timePast(conn.upTime);
            case 4:
                return showSummry1() + "|" + rtrBgpParam.mask2string(conn.peerGrace) + "|" + rtrBgpParam.mask2string(graceRestart & addrFams);
            case 5:
                return showSummry1() + "|" + rtrBgpParam.mask2string(conn.addpathRx) + "|" + rtrBgpParam.mask2string(conn.addpathTx) + "|" + rtrBgpParam.mask2string(addpathRmode - conn.addpathRx) + "|" + rtrBgpParam.mask2string(addpathTmode - conn.addpathTx) + "|" + rtrBgpParam.mask2string(conn.originalAddRlist - conn.addpathRx) + "|" + rtrBgpParam.mask2string(conn.originalAddTlist - conn.addpathTx);
            case 6:
                return showSummry1() + "|" + conn.peerRouterID + "|" + conn.peer32bitAS + "|" + conn.peerRefreshOld + " " + conn.peerRefreshNew + "|" + conn.peerDynCap + "|" + conn.peerExtOpen + "|" + conn.peerExtUpd + "|" + rtrBgpUtil.peerType2string(peerType) + "|" + rtrBgpUtil.leakRole2string(leakRole, leakAttr);
            case 7:
                return showSummry1() + "|" + pipeSide.getStatus(conn.pipe) + "|" + conn.buffFull + "|" + conn.adversion + "|" + incrCount + "|" + fullCount + "|" + conn.needFull;
            case 8:
                return showSummry1() + "|" + description;
            case 9:
                return showSummry1() + "|" + conn.peerHostname + "|" + conn.peerDomainname;
            case 10:
                return showSummry1() + "|" + (conn.compressRx != null) + "|" + (conn.compressTx != null) + "|" + bits.percent(conn.cntr.byteRx, conn.compressCntr.byteRx) + "|" + bits.percent(conn.cntr.byteTx, conn.compressCntr.byteTx);
            case 11:
                return showSummry1() + "|" + conn.cntr.packRx + "|" + conn.cntr.packTx + "|" + conn.cntr.byteRx + "|" + conn.cntr.byteTx + "|" + conn.refreshRx + "|" + conn.refreshTx + "|" + conn.dynCapaRx + "|" + conn.dynCapaTx;
            case 12:
                clntDns clnt = new clntDns();
                clnt.doResolvList(cfgAll.nameServerAddr, packDnsRec.generateReverse(peerAddr), false, packDnsRec.typePTR);
                return showSummry1() + "|" + clnt.getPTR();
            case 13:
                return showSummry2();
            case 14:
                return showSummry1() + "|" + rtrBgpParam.mask2string(conn.peerMltLab) + "|" + rtrBgpParam.mask2string(multiLabel & addrFams);
            case 15:
                return showSummry1() + "|" + rtrBgpParam.mask2string(conn.peerLlGrace) + "|" + rtrBgpParam.mask2string(llGraceRestart & addrFams);
            case 16:
                return showSummry1() + "|" + conn.peerSoftware;
            case 17:
                return showSummry2() + "|" + description;
            case 18:
                return showSummry1() + "|" + conn.unknownCntr.packRx + "|" + conn.unknownCntr.byteRx + "|" + bits.timePast(conn.unknownCntr.lastRx);
            case 19:
                return showSummry2() + "|" + clntWhois.asn2name(remoteAs, true) + "|" + clntWhois.asn2info(remoteAs);
            case 20:
                return showSummry1() + "|" + reachabStat.packRx + "|" + reachabStat.packTx + "|" + unreachStat.packRx + "|" + unreachStat.packTx + "|" + bits.timePast(reachabStat.lastRx) + "|" + bits.timePast(reachabStat.lastTx);
            default:
                return null;
        }
    }

    /**
     * get message statistics
     *
     * @return list of statistics
     */
    public userFormat getMsgStats() {
        return rtrBgpDump.getMsgStats(msgStats);
    }

    /**
     * get message statistics
     *
     * @return list of statistics
     */
    public userFormat getAttrStats() {
        return rtrBgpDump.getAttrStats(attrStats);
    }

    /**
     * get state information
     *
     * @return state
     */
    public String stateGet() {
        if (!haMode) {
            return null;
        }
        if (!conn.ready2adv) {
            return null;
        }
        if (!conn.peer32bitAS) {
            return null;
        }
        if (!conn.peerRefreshOld) {
            return null;
        }
        if (!conn.peerRefreshNew) {
            return null;
        }
        if ((conn.compressRx != null) && (conn.compressTx != null)) {
            return null;
        }
        prtGenConn sock = lower.tcpCore.findOneConn(conn.pipe);
        if (sock == null) {
            return null;
        }
        sock.restartable = true;
        return peerAddr + " " + template + " " + sock.portLoc + " " + sock.portRem + " " + sock.iface + " " + bits.num2str(remoteAs) + " " + conn.peerHold + " " + conn.upTime + " " + conn.peerAfis + " " + conn.addpathRx + " " + conn.addpathTx + " " + conn.peerMltLab + " " + conn.peerDynCap + " " + conn.peerRouterID;
    }

}
