#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <pthread.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <linux/if.h>
#include <poll.h>
#include <linux/if_link.h>
#include <xdp/xsk.h>
#include <sys/mman.h>


#include "p4emu_hdr.h"
#include "utils.h"

#define framesNum 1024

pthread_mutex_t ifaceLock[maxPorts];
struct xsk_umem *ifaceUmem[maxPorts];
struct xsk_socket *ifaceXsk[maxPorts];
struct xsk_ring_prod ifaceFq[maxPorts];
struct xsk_ring_cons ifaceCq[maxPorts];
struct xsk_ring_cons ifaceRx[maxPorts];
struct xsk_ring_prod ifaceTx[maxPorts];
char *ifaceBuf[maxPorts];
struct pollfd ifacePfd[maxPorts];

void sendPack(unsigned char *bufD, int bufS, int port) {
    unsigned int idx;
    pthread_mutex_lock(&ifaceLock[port]);
    idx = xsk_ring_cons__peek(&ifaceCq[port], 16, &idx);
    xsk_ring_cons__release(&ifaceCq[port], idx);
    if (xsk_ring_prod__reserve(&ifaceTx[port], 1, &idx) < 1) {
        pthread_mutex_unlock(&ifaceLock[port]);
        return;
    }
    struct xdp_desc *dsc = xsk_ring_prod__tx_desc(&ifaceTx[port], idx);
    dsc->addr = (framesNum + (idx % framesNum)) * XSK_UMEM__DEFAULT_FRAME_SIZE;
    dsc->options = 0;
    dsc->len = bufS;
    memcpy(ifaceBuf[port] + dsc->addr, bufD, bufS);
    xsk_ring_prod__submit(&ifaceTx[port], 1);
    pthread_mutex_unlock(&ifaceLock[port]);
    if (!xsk_ring_prod__needs_wakeup(&ifaceTx[port])) return;
    sendto(xsk_socket__fd(ifaceXsk[port]), NULL, 0, MSG_DONTWAIT, NULL, 0);
}

void setMtu(int port, int mtu) {
}

void setState(int port, int sta) {
}

int getState(int port) {
    return 1;
}


void getStats(int port, unsigned char*buf, unsigned char*pre, int*len) {
}



void err(char*buf) {
    printf("%s\n", buf);
    _exit(1);
}




pthread_t threadRaw[maxPorts];
int commandSock;
int ifaceId[maxPorts];



#define packGet                                                                                     \
    poll(&ifacePfd[port], 1, 1);                                                                    \
    unsigned int idx;                                                                               \
    if (xsk_ring_cons__peek(&ifaceRx[port], 1, &idx) < 1) continue;                                 \
    const struct xdp_desc *dsc = xsk_ring_cons__rx_desc(&ifaceRx[port], idx);                       \
    char *dat = xsk_umem__get_data(ifaceBuf[port], dsc->addr);                                      \
    bufS = dsc->len;                                                                                \
    memcpy(&bufD[preBuff], dat, bufS);                                                              \
    xsk_ring_prod__reserve(&ifaceFq[port], 1, &idx);                                                \
    *xsk_ring_prod__fill_addr(&ifaceFq[port], idx) = dsc->addr;                                     \
    xsk_ring_prod__submit(&ifaceFq[port], 1);                                                       \
    xsk_ring_cons__release(&ifaceRx[port], 1);                                                      \




void doIfaceLoop(int * param) {
    int port = *param;
    unsigned char bufA[16384];
    unsigned char bufB[16384];
    unsigned char bufC[16384];
    unsigned char bufD[16384];
    int bufS;
    EVP_CIPHER_CTX *encrCtx;
    EVP_MD_CTX *hashCtx;
    if (initContext(&encrCtx, &hashCtx) != 0) err("error initializing context");
    if (port == cpuPort) {
        for (;;) {
            packGet;
            processCpuPack(&bufA[0], &bufB[0], &bufC[0], &bufD[0], bufS, encrCtx, hashCtx);
        }
    } else {
        for (;;) {
            packGet;
            processDataPacket(&bufA[0], &bufB[0], &bufC[0], &bufD[0], bufS, port, port, encrCtx, hashCtx);
        }
    }
    err("port thread exited");
}



void doSockLoop() {
    FILE *commands = fdopen(commandSock, "r");
    if (commands == NULL) err("failed to open file");
    EVP_CIPHER_CTX *encrCtx;
    EVP_MD_CTX *hashCtx;
    if (initContext(&encrCtx, &hashCtx) != 0) err("error initializing context");
    unsigned char buf[16384];
    for (;;) {
        memset(&buf, 0, sizeof(buf));
        if (fgets((char*)&buf[0], sizeof(buf), commands) == NULL) break;
        if (doOneCommand(&buf[0], encrCtx, hashCtx) != 0) break;
    }
    err("command thread exited");
}



void doStatLoop() {
    FILE *commands = fdopen(commandSock, "w");
    if (commands == NULL) err("failed to open file");
    fprintf(commands, "platform %sxsk\r\n", platformBase);
    fprintf(commands, "capabilities %s\r\n", getCapas());
    for (int i = 0; i < dataPorts; i++) fprintf(commands, "portname %i %s\r\n", i, ifaceName[i]);
    fprintf(commands, "cpuport %i\r\n", cpuPort);
    fprintf(commands, "dynrange %i 65535\r\n", maxPorts);
    fprintf(commands, "vrfrange 1 65535\r\n");
    fprintf(commands, "nomore\r\n");
    fflush(commands);
    int rnd = 0;
    for (;;) {
        doStatRound(commands, rnd);
        rnd++;
        usleep(100000);
    }
    err("stat thread exited");
}




void doMainLoop() {
    unsigned char buf[1024];

    for (;;) {
        printf("> ");
        buf[0] = 0;
        int i = scanf("%1023s", buf);
        if (i < 1) {
            sleep(1);
            continue;
        }
        if (doConsoleCommand(&buf[0]) != 0) break;
        printf("\n");
    }
    err("main thread exited");
}




int main(int argc, char **argv) {

    dataPorts = 0;
    for (int i = 5; i < argc; i++) {
        initIface(dataPorts, argv[i]);
        dataPorts++;
    }
    if (dataPorts < 2) err("using: dp <addr> <port> <cpuport> <skb/drv/hw> <ifc0> <ifc1> [ifcN]");
    if (dataPorts > maxPorts) dataPorts = maxPorts;
    if (initTables() != 0) err("error initializing tables");
    int port = atoi(argv[2]);
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof (addr));
    if (inet_aton(argv[1], &addr.sin_addr) == 0) err("bad addr address");
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    printf("connecting %s %i.\n", inet_ntoa(addr.sin_addr), port);
    commandSock = socket(AF_INET, SOCK_STREAM, 0);
    if (commandSock < 0) err("unable to open socket");
    if (connect(commandSock, (struct sockaddr*)&addr, sizeof(addr)) < 0) err("failed to connect socket");
    cpuPort = atoi(argv[3]);
    int bpf_flag = 0;
    if (strcmp(argv[4],"skb") == 0) {
        bpf_flag = XDP_FLAGS_SKB_MODE;
    }
    if (strcmp(argv[4],"drv") == 0) {
        bpf_flag = XDP_FLAGS_DRV_MODE;
    }
    if (strcmp(argv[4],"hw") == 0) {
        bpf_flag = XDP_FLAGS_HW_MODE;
    }
    printf("cpu port is #%i of %i...\n", cpuPort, dataPorts);

    for (int o = 0; o < dataPorts; o++) {
        printf("opening interface %s\n", ifaceName[o]);
        posix_memalign((void**)&ifaceBuf[o], getpagesize(), XSK_UMEM__DEFAULT_FRAME_SIZE * 2 * framesNum);
        if (ifaceBuf[o] == NULL) err("error allocating buffer");
        if (xsk_umem__create(&ifaceUmem[o], ifaceBuf[o], XSK_UMEM__DEFAULT_FRAME_SIZE * 2 * framesNum, &ifaceFq[o], &ifaceCq[o], NULL) != 0) err("error creating umem");
        struct xsk_socket_config cfg;
        memset(&cfg, 0, sizeof(cfg));
        cfg.rx_size = XSK_RING_CONS__DEFAULT_NUM_DESCS;
        cfg.tx_size = XSK_RING_PROD__DEFAULT_NUM_DESCS;
        cfg.xdp_flags = bpf_flag;
        if (xsk_socket__create(&ifaceXsk[o], ifaceName[o], 0, ifaceUmem[o], &ifaceRx[o], &ifaceTx[o], &cfg) != 0) {
            if (o < (dataPorts-1)) err("error creating xsk");
            dataPorts--;
            break;
        }
        unsigned int i = 0;
        xsk_ring_prod__reserve(&ifaceFq[o], framesNum, &i);
        for (i=0; i < framesNum; i++) *xsk_ring_prod__fill_addr(&ifaceFq[o], i) = i * XSK_UMEM__DEFAULT_FRAME_SIZE;
        xsk_ring_prod__submit(&ifaceFq[o], framesNum);
        memset(&ifacePfd[o], 0, sizeof (ifacePfd[o]));
        ifacePfd[o].fd = xsk_socket__fd(ifaceXsk[o]);
        ifacePfd[o].events = POLLIN | POLLERR;
        pthread_mutex_init(&ifaceLock[o], NULL);
        ifaceId[o] = o;
    }

    pthread_t threadSock;
    if (pthread_create(&threadSock, NULL, (void*) & doSockLoop, NULL)) err("error creating socket thread");
    pthread_t threadStat;
    if (pthread_create(&threadStat, NULL, (void*) & doStatLoop, NULL)) err("error creating status thread");

    for (int i=0; i < dataPorts; i++) {
        if (pthread_create(&threadRaw[i], NULL, (void*) & doIfaceLoop, &ifaceId[i])) err("error creating port thread");
    }

    doMainLoop();
}
