#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <pthread.h>
#include <arpa/inet.h>
#include <openssl/conf.h>
//#include <openssl/provider.h>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/err.h>
#include <dpdk/rte_config.h>
#include <dpdk/rte_common.h>
#include <dpdk/rte_version.h>
#include <dpdk/rte_eal.h>
#include <dpdk/rte_ethdev.h>
#include <dpdk/rte_mbuf.h>
#include <dpdk/rte_ring.h>


#undef basicLoop


#include "p4cns.h"


struct rte_mempool *mbuf_pool;

struct rte_ring *tx_ring[RTE_MAX_ETHPORTS];

void sendpack(unsigned char *bufD, int bufS, int port) {
    struct rte_mbuf *mbuf = rte_pktmbuf_alloc(mbuf_pool);
    if (mbuf == NULL) rte_exit(EXIT_FAILURE, "error allocating bmuf\n");
    unsigned char * pack = rte_pktmbuf_append(mbuf, bufS);
    memmove(pack, bufD, bufS);
    rte_ring_mp_enqueue(tx_ring[port], mbuf);
}


void setState(int port, int sta) {
    if (sta == 1) rte_eth_dev_set_link_up(port);
    else rte_eth_dev_set_link_down(port);
}


int getState(int port) {
    struct rte_eth_link link;
    if (rte_eth_link_get_nowait(port, &link) != 0) return 1;
    if (link.link_status == ETH_LINK_UP) return 1;
    return 0;
}




#include "p4tab.h"
#include "p4msg.h"
#include "p4fwd.h"


int commandSock;


#define RX_RING_SIZE 1024
#define TX_RING_SIZE 1024

#define NUM_MBUFS 8191
#define MBUF_CACHE_SIZE 250
#define BURST_SIZE 32
#define BURST_PAUSE 100
#define RING_SIZE 512


static const struct rte_eth_conf port_conf_default = {
    .rxmode = {
        .max_rx_pkt_len = RTE_ETHER_MAX_LEN,
    },
    .txmode = {
    },
};



struct lcore_conf {
    int rx_num;
    int rx_list[RTE_MAX_ETHPORTS];
    int tx_num;
    int tx_list[RTE_MAX_ETHPORTS];
} __rte_cache_aligned;
struct lcore_conf lcore_conf[RTE_MAX_LCORE];







void doSockLoop() {
    FILE *commands = fdopen(commandSock, "r");
    if (commands == NULL) rte_exit(EXIT_FAILURE, "failed to open file\n");
    unsigned char buf[1024];
    for (;;) {
        memset(&buf, 0, sizeof(buf));
        if (fgets(buf, sizeof(buf), commands) == NULL) break;
        if (doOneCommand(&buf[0]) != 0) break;
    }
    rte_exit(EXIT_FAILURE, "command thread exited\n");
}



void doStatLoop() {
    FILE *commands = fdopen(commandSock, "w");
    if (commands == NULL) rte_exit(EXIT_FAILURE, "failed to open file\n");
    int rnd = 0;
    for (;;) {
        doStatRound(commands);
        rnd++;
        if ((rnd % 15) == 0) doReportRound(commands);
        sleep(1);
    }
    rte_exit(EXIT_FAILURE, "stat thread exited\n");
}




void doMainLoop() {
    unsigned char buf[1024];

    for (;;) {
        printf("> ");
        buf[0] = 0;
        int i = scanf("%s", buf);
        if (i < 1) {
            sleep(1);
            continue;
        }
        if (doConsoleCommand(&buf[0]) != 0) break;
        printf("\n");
    }
    rte_exit(EXIT_FAILURE, "main thread exited\n");
}




static int doPacketLoop(__rte_unused void *arg) {
    unsigned char bufD[16384];
    int bufS;
    struct rte_mbuf *bufs[BURST_SIZE];
    unsigned char * bufP;
    int port;
    int seq;
    int num;
    int pkts;
    int i;
    EVP_CIPHER_CTX *encrCtx = EVP_CIPHER_CTX_new();
    if (encrCtx == NULL) rte_exit(EXIT_FAILURE, "error getting encr context");
    EVP_MD_CTX *hashCtx = EVP_MD_CTX_new();
    if (hashCtx == NULL) rte_exit(EXIT_FAILURE, "error getting hash context");

    int lcore = rte_lcore_id();
    struct lcore_conf *myconf = &lcore_conf[lcore];

    printf("lcore %i started with %i rx and %i tx ports!\n", lcore, myconf->rx_num, myconf->tx_num);
    if ((myconf->rx_num + myconf->tx_num) < 1) return 0;

    for (;;) {
        pkts = 0;
        for (seq = 0; seq < myconf->tx_num; seq++) {
            port = myconf->tx_list[seq];
            num = rte_ring_count(tx_ring[port]);
            if (num > BURST_SIZE) num = BURST_SIZE;
            num = rte_ring_sc_dequeue_bulk(tx_ring[port], (void**)bufs, num, NULL);
            rte_eth_tx_burst(port, 0, bufs, num);
            pkts += num;
        }
        for (seq = 0; seq < myconf->rx_num; seq++) {
            port = myconf->rx_list[seq];
            num = rte_eth_rx_burst(port, 0, bufs, BURST_SIZE);
            for (i = 0; i < num; i++) {
                bufS = rte_pktmbuf_pkt_len(bufs[i]);
                bufP = rte_pktmbuf_mtod(bufs[i], void *);
                if ((bufs[i]->ol_flags & PKT_RX_VLAN_STRIPPED) != 0) {
                    memmove(&bufD[preBuff], bufP, 12);
                    put16msb(bufD, preBuff + 12, ETHERTYPE_VLAN);
                    put16msb(bufD, preBuff + 14, bufs[i]->vlan_tci);
                    memmove(&bufD[preBuff + 16], bufP + 12, bufS - 12);
                    bufS += 4;
                } else {
                    memmove(&bufD[preBuff], bufP, bufS);
                }
                rte_pktmbuf_free(bufs[i]);
                if (port == cpuport) processCpuPack(&bufD[0], bufS);
                else processDataPacket(&bufD[0], bufS, port, encrCtx, hashCtx);
            }
            pkts += num;
        }
        if (pkts < 1) usleep(BURST_PAUSE);
    }
    rte_exit(EXIT_FAILURE, "packet thread exited\n");
    return 0;
}







int main(int argc, char **argv) {

    int ret = rte_eal_init(argc, argv);
    if (ret < 0) rte_exit(EXIT_FAILURE, "error with eal initialization\n");

    argc -= ret;
    argv += ret;

    ports = rte_eth_dev_count_avail();
    if (ports < 2) rte_exit(EXIT_FAILURE, "at least 2 ports needed\n");

    if (argc < 4) rte_exit(EXIT_FAILURE, "using: dp <host> <rport> <cpuport> [port rxlcore txlcore] ...\n");
    printf("dpdk version: %s\n", rte_version());
    if (initTables() != 0) rte_exit(EXIT_FAILURE, "error initializing tables");
    int port = atoi(argv[2]);
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof (addr));
    if (inet_aton(argv[1], &addr.sin_addr) == 0) rte_exit(EXIT_FAILURE, "bad addr address\n");
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    printf("connecting %s %i.\n", inet_ntoa(addr.sin_addr), port);
    commandSock = socket(AF_INET, SOCK_STREAM, 0);
    if (commandSock < 0) rte_exit(EXIT_FAILURE, "unable to open socket\n");
    if(connect(commandSock, (struct sockaddr*)&addr, sizeof(addr)) < 0) rte_exit(EXIT_FAILURE, "failed to connect socket\n");
    cpuport = atoi(argv[3]);
    printf("cpu port is #%i of %i...\n", cpuport, ports);

    int port2rx[RTE_MAX_ETHPORTS];
    int port2tx[RTE_MAX_ETHPORTS];
    memset(&port2rx, 0, sizeof(port2rx));
    memset(&port2tx, 0, sizeof(port2tx));
    for (int i = 4; i< argc; i += 3) {
        int p = atoi(argv[i + 0]);
        int r = atoi(argv[i + 1]);
        int t = atoi(argv[i + 2]);
        port2rx[p] = r;
        port2tx[p] = t;
    }
    memset(&lcore_conf, 0, sizeof(lcore_conf));
    for (int i = 0; i < ports; i++) {
        int r = port2rx[i];
        int t = port2tx[i];
        lcore_conf[r].rx_list[lcore_conf[r].rx_num] = i;
        lcore_conf[r].rx_num++;
        lcore_conf[t].tx_list[lcore_conf[t].tx_num] = i;
        lcore_conf[t].tx_num++;
    }

    mbuf_pool = rte_pktmbuf_pool_create("mbufs", NUM_MBUFS * ports, MBUF_CACHE_SIZE, 0, RTE_MBUF_DEFAULT_BUF_SIZE, rte_socket_id());
    if (mbuf_pool == NULL) rte_exit(EXIT_FAILURE, "cannot create mbuf pool\n");

    RTE_ETH_FOREACH_DEV(port) {
        unsigned char buf[128];
        sprintf(buf, "dpdk-port%i", port);
        int sock = rte_eth_dev_socket_id(port);
        printf("opening port %i on lcore (rx %i tx %i) on socket %i...\n", port, port2rx[port], port2tx[port], sock);
        initIface(port, buf);

        struct rte_eth_conf port_conf = port_conf_default;
        uint16_t nb_rxd = RX_RING_SIZE;
        uint16_t nb_txd = TX_RING_SIZE;
        struct rte_eth_dev_info dev_info;
        struct rte_eth_txconf txconf;

        if (!rte_eth_dev_is_valid_port(port)) rte_exit(EXIT_FAILURE, "not valid port\n");

        ret = rte_eth_dev_info_get(port, &dev_info);
        if (ret != 0) rte_exit(EXIT_FAILURE, "error getting device info\n");

        if (dev_info.tx_offload_capa & DEV_TX_OFFLOAD_MBUF_FAST_FREE) {
            port_conf.txmode.offloads |= DEV_TX_OFFLOAD_MBUF_FAST_FREE;
        }

        if (dev_info.rx_offload_capa & DEV_RX_OFFLOAD_JUMBO_FRAME) {
            port_conf.rxmode.offloads |= DEV_RX_OFFLOAD_JUMBO_FRAME;
            port_conf.rxmode.max_rx_pkt_len = RTE_MBUF_DEFAULT_DATAROOM;
        }

        ret = rte_eth_dev_configure(port, 1, 1, &port_conf);
        if (ret != 0) rte_exit(EXIT_FAILURE, "error configuring port\n");

        ret = rte_eth_dev_adjust_nb_rx_tx_desc(port, &nb_rxd, &nb_txd);
        if (ret != 0) rte_exit(EXIT_FAILURE, "error adjusting descriptors\n");

        ret = rte_eth_rx_queue_setup(port, 0, nb_rxd, sock, NULL, mbuf_pool);
        if (ret != 0) rte_exit(EXIT_FAILURE, "error setting up rx queue\n");

        txconf = dev_info.default_txconf;
        txconf.offloads = port_conf.txmode.offloads;
        ret = rte_eth_tx_queue_setup(port, 0, nb_txd, sock, &txconf);
        if (ret != 0) rte_exit(EXIT_FAILURE, "error setting up tx queue\n");

        tx_ring[port] = rte_ring_create(buf, RING_SIZE, sock, RING_F_SP_ENQ | RING_F_SC_DEQ);
        if (tx_ring[port] == NULL) rte_exit(EXIT_FAILURE, "error allocating tx ring\n");

        ret = rte_eth_dev_start(port);
        if (ret != 0) rte_exit(EXIT_FAILURE, "error starting port\n");

        struct rte_ether_addr addr;
        ret = rte_eth_macaddr_get(port, &addr);
        if (ret != 0) rte_exit(EXIT_FAILURE, "error getting mac\n");

        ret = rte_eth_promiscuous_enable(port);
        if (ret != 0) rte_exit(EXIT_FAILURE, "error setting promiscuous mode\n");
    }

    pthread_t threadSock;
    if (pthread_create(&threadSock, NULL, (void*) & doSockLoop, NULL)) rte_exit(EXIT_FAILURE, "error creating socket thread\n");
    pthread_t threadStat;
    if (pthread_create(&threadStat, NULL, (void*) & doStatLoop, NULL)) rte_exit(EXIT_FAILURE, "error creating status thread\n");

    rte_eal_mp_remote_launch(&doPacketLoop, NULL, CALL_MASTER);

    doMainLoop();

}
