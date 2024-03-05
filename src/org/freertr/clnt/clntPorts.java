package org.freertr.clnt;

import org.freertr.addr.addrIP;
import org.freertr.cfg.cfgIfc;
import org.freertr.cfg.cfgVrf;
import org.freertr.ip.ipFwdIface;
import org.freertr.pipe.pipeLine;
import org.freertr.pipe.pipeSide;
import org.freertr.prt.prtTcp;

/**
 * port scanner
 *
 * @author matecsaba
 */
public class clntPorts {

    /**
     * create instance
     */
    public clntPorts() {
    }

    /**
     * vrf to use
     */
    public cfgVrf vrf;

    /**
     * iface to use
     */
    public cfgIfc ifc;

    /**
     * target address
     */
    public addrIP trg;

    /**
     * timeout
     */
    public int tim;

    /**
     * test one port
     *
     * @param prt port number
     * @return false on one, true on close
     */
    public boolean testOne(int prt) {
        if (trg == null) {
            return true;
        }
        if (vrf == null) {
            return true;
        }
        ipFwdIface ifc2 = null;
        if (ifc != null) {
            ifc2 = ifc.getFwdIfc(trg);
        }
        prtTcp tcp = vrf.getTcp(trg);
        pipeSide pip = tcp.streamConnect(new pipeLine(65536, false), ifc2, 0, trg, prt, "portscan", -1, null, -1, -1);
        if (pip == null) {
            return true;
        }
        boolean res = pip.wait4ready(tim);
        pip.setClose();
        return res;
    }

}
