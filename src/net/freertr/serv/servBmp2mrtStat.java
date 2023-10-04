package net.freertr.serv;

import java.util.Comparator;
import net.freertr.addr.addrIP;
import net.freertr.cfg.cfgAll;
import net.freertr.cfg.cfgRtr;
import net.freertr.clnt.clntWhois;
import net.freertr.rtr.rtrBgp;
import net.freertr.rtr.rtrBgpNeigh;
import net.freertr.tab.tabRouteAttr;
import net.freertr.user.userFormat;
import net.freertr.util.bits;
import net.freertr.util.cmds;

/**
 * bmp server connection
 *
 * @author matecsaba
 */
public class servBmp2mrtStat implements Comparator<servBmp2mrtStat> {

    /**
     * connecting peer
     */
    protected addrIP from;

    /**
     * remote address
     */
    protected addrIP peer;

    /**
     * as number
     */
    protected int asn;

    /**
     * packets in
     */
    protected int packIn;

    /**
     * packets out
     */
    protected int packOut;

    /**
     * packets rate
     */
    protected int packRate;

    /**
     * packets last
     */
    protected long packLast;

    /**
     * bytes in
     */
    protected int byteIn;

    /**
     * bytes out
     */
    protected int byteOut;

    /**
     * last state
     */
    protected boolean state;

    /**
     * connection time
     */
    protected long since;

    /**
     * chanegs seen
     */
    protected int change;

    /**
     * router to use
     */
    protected tabRouteAttr.routeType rouT;

    /**
     * router to use
     */
    protected int rouI;

    /**
     * router to use
     */
    protected boolean rouD;

    /**
     * process to use
     */
    protected rtrBgp prc;

    /**
     * neighbor to use
     */
    protected rtrBgpNeigh nei;

    /**
     * dynamic peer
     */
    protected boolean dyn;

    /**
     * report last
     */
    protected long repLast;

    /**
     * report packets
     */
    protected int repPack;

    /**
     * report bytes
     */
    protected int repByte;

    /**
     * policy rejected
     */
    protected int repPolRej;

    /**
     * duplicate advertisements
     */
    protected int repDupAdv;

    /**
     * duplicate withdrawals
     */
    protected int repDupWit;

    /**
     * cluster list
     */
    protected int repClstrL;

    /**
     * as path
     */
    protected int repAsPath;

    /**
     * originator id
     */
    protected int repOrgnId;

    /**
     * as confederation
     */
    protected int repAsConf;

    /**
     * bad updates
     */
    protected int repWitUpd;

    /**
     * bad withdraws
     */
    protected int repWitPrf;

    /**
     * duplicate updates
     */
    protected int repDupUpd;

    /**
     * create instance
     */
    public servBmp2mrtStat() {
    }

    public int compare(servBmp2mrtStat o1, servBmp2mrtStat o2) {
        int i = o1.from.compare(o1.from, o2.from);
        if (i != 0) {
            return i;
        }
        return o1.peer.compare(o1.peer, o2.peer);
    }

    public String toString() {
        return from + "|" + peer + "|" + asn + "|" + clntWhois.asn2name(asn, true) + "|" + state + "|" + change + "|" + bits.timePast(since) + "|" + bits.time2str(cfgAll.timeZoneName, since + cfgAll.timeServerOffset, 3);
    }

    /**
     * convert from string
     *
     * @param cmd command to read
     * @param stat
     * @return
     */
    public boolean fromString(cmds cmd, boolean stat) {
        rouD = cmd.word().equals("tx");
        tabRouteAttr.routeType rt = cfgRtr.name2num(cmd.word());
        if (rt == null) {
            cmd.error("invalid routing protocol");
            return true;
        }
        int ri = bits.str2num(cmd.word());
        cfgRtr rp = cfgAll.rtrFind(rt, ri, false);
        if (rp == null) {
            cmd.error("bad process number");
            return true;
        }
        if (rp.bgp == null) {
            cmd.error("not a bgp process");
            return true;
        }
        prc = rp.bgp;
        if (!stat) {
            rouT = rt;
            rouI = ri;
            return false;
        }
        addrIP adr = new addrIP();
        adr.fromString(cmd.word());
        nei = rp.bgp.findPeer(adr);
        if (nei == null) {
            cmd.error("no such peer");
            return true;
        }
        rouT = rt;
        rouI = ri;
        return false;
    }

    /**
     * get current config
     *
     * @param stat status
     * @return string
     */
    public String getCfg(boolean stat) {
        String a = (rouD ? "tx" : "rx") + " " + cfgRtr.num2name(rouT) + " " + rouI;
        if (stat) {
            return a + " " + nei;
        } else {
            return a;
        }
    }

    /**
     * get show output
     *
     * @return output
     */
    public userFormat getShow() {
        userFormat res = new userFormat("|", "category|value");
        res.add("from|" + from);
        res.add("peer|" + peer);
        res.add("dynamic|" + dyn);
        res.add("asnum|" + asn);
        res.add("asname|" + clntWhois.asn2name(asn, true));
        res.add("asinfo|" + clntWhois.asn2info(asn));
        res.add("state|" + state);
        res.add("since|" + bits.time2str(cfgAll.timeZoneName, since + cfgAll.timeServerOffset, 3) + " (" + bits.timePast(since) + " ago)");
        res.add("change|" + change);
        res.add("pack in|" + packIn);
        res.add("pack out|" + packOut);
        res.add("byte in|" + byteIn);
        res.add("byte out|" + byteOut);
        res.add("pack last|" + bits.time2str(cfgAll.timeZoneName, packLast + cfgAll.timeServerOffset, 3) + " (" + bits.timePast(packLast) + " ago)");
        res.add("report pack|" + repPack);
        res.add("report byte|" + repByte);
        res.add("report last|" + bits.time2str(cfgAll.timeZoneName, repLast + cfgAll.timeServerOffset, 3) + " (" + bits.timePast(repLast) + " ago)");
        res.add("rep policy drp|" + repPolRej);
        res.add("rep dup advert|" + repDupAdv);
        res.add("rep dup withdrw|" + repDupWit);
        res.add("rep dup update|" + repDupUpd);
        res.add("rep cluster id|" + repClstrL);
        res.add("rep as path|" + repAsPath);
        res.add("rep originator|" + repOrgnId);
        res.add("rep confed|" + repAsConf);
        res.add("rep bad updt|" + repWitUpd);
        res.add("rep bad prfx|" + repWitPrf);
        res.add("process|" + rouT + " " + rouI);
        res.add("neighbor|" + nei);
        res.add("direction|" + rouD);
        return res;
    }

    /**
     * add counters
     *
     * @param oth where from
     */
    public void addCnts(servBmp2mrtStat oth) {
        if (since < oth.since) {
            since = oth.since;
        }
        if (packLast < oth.packLast) {
            packLast = oth.packLast;
        }
        if (repLast < oth.repLast) {
            repLast = oth.repLast;
        }
        change += oth.change;
        packIn += oth.packIn;
        packOut += oth.packOut;
        byteIn += oth.byteIn;
        byteOut += oth.byteOut;
        repPack += oth.repPack;
        repByte += oth.repByte;
        repPolRej += oth.repPolRej;
        repDupAdv += oth.repDupAdv;
        repDupWit += oth.repDupWit;
        repDupUpd += oth.repDupUpd;
        repClstrL += oth.repClstrL;
        repAsPath += oth.repAsPath;
        repOrgnId += oth.repOrgnId;
        repAsConf += oth.repAsConf;
        repWitUpd += oth.repWitUpd;
        repWitPrf += oth.repWitPrf;
    }

}
