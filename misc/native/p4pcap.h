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
#include <pcap.h>


#include "p4cns.h"

pcap_t *ifacePcap[maxPorts];

void sendpack(unsigned char *bufD, int bufS, int port) {
    pcap_sendpacket(ifacePcap[port], bufD, bufS);
}

void setState(int port, int sta) {
}

int getState(int port) {
    return 1;
}




#include "p4tab.h"
#include "p4msg.h"
#include "p4fwd.h"


pthread_t threadRaw[maxPorts];
int commandSock;
int ifaceId[maxPorts];



void err(unsigned char*buf) {
    printf("%s\n", buf);
    exit(1);
}




void doIfaceLoop(int * param) {
    int port = *param;
    unsigned char bufC[16384];
    unsigned char bufD[16384];
    struct pcap_pkthdr head;
    const unsigned char *pack;
    int bufS;
    unsigned int addrLen;
    EVP_CIPHER_CTX *encrCtx = EVP_CIPHER_CTX_new();
    if (encrCtx == NULL) err("error getting encr context");
    EVP_MD_CTX *hashCtx = EVP_MD_CTX_new();
    if (hashCtx == NULL) err("error getting hash context");
    if (port == cpuport) {
        for (;;) {
            pack = pcap_next(ifacePcap[port], &head);
            bufS = head.caplen;
            if (bufS < 0) break;
            memmove(&bufD[preBuff], pack, bufS);
            processCpuPack(&bufD[0], bufS);
        }
    } else {
        for (;;) {
            pack = pcap_next(ifacePcap[port], &head);
            bufS = head.caplen;
            if (bufS < 0) break;
            memmove(&bufD[preBuff], pack, bufS);
            processDataPacket(&bufC[0], &bufD[0], bufS, port, encrCtx, hashCtx);
        }
    }
    err("port thread exited");
}



void doSockLoop() {
    FILE *commands = fdopen(commandSock, "r");
    if (commands == NULL) err("failed to open file");
    unsigned char buf[1024];
    for (;;) {
        memset(&buf, 0, sizeof(buf));
        if (fgets(buf, sizeof(buf), commands) == NULL) break;
        if (doOneCommand(&buf[0]) != 0) break;
    }
    err("command thread exited");
}



void doStatLoop() {
    FILE *commands = fdopen(commandSock, "w");
    if (commands == NULL) err("failed to open file");
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
        int i = scanf("%s", buf);
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
    unsigned char errbuf[PCAP_ERRBUF_SIZE + 1];

    ports = 0;
    for (int i = 4; i < argc; i++) {
        initIface(ports, argv[i]);
        ports++;
    }
    if (ports < 2) err("using: dp <addr> <port> <cpuport> <ifc0> <ifc1> [ifcN] ...");
    printf("pcap version: %s\n", pcap_lib_version());
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
    if(connect(commandSock, (struct sockaddr*)&addr, sizeof(addr)) < 0) err("failed to connect socket");
    cpuport = atoi(argv[3]);
    printf("cpu port is #%i of %i...\n", cpuport, ports);

    for (int i = 0; i < ports; i++) {
        printf("opening interface %s\n", ifaceName[i]);
        ifacePcap[i] = pcap_create(ifaceName[i], errbuf);
        if (ifacePcap[i] == NULL) err("unable to open interface");
        if (pcap_set_snaplen(ifacePcap[i], 65536) < 0) err("unable to set snaplen");
        if (pcap_set_promisc(ifacePcap[i], 1) < 0) err("unable to set promisc");
        if (pcap_set_immediate_mode(ifacePcap[i], 1) < 0) err("unable to set immediate");
        if (pcap_activate(ifacePcap[i]) < 0) err("activation failed");
        if (pcap_setdirection(ifacePcap[i], PCAP_D_IN) < 0) err("unable to set direction");
        ifaceId[i] = i;
    }

    pthread_t threadSock;
    if (pthread_create(&threadSock, NULL, (void*) & doSockLoop, NULL)) err("error creating socket thread");
    pthread_t threadStat;
    if (pthread_create(&threadStat, NULL, (void*) & doStatLoop, NULL)) err("error creating status thread");

    for (int i=0; i < ports; i++) {
        if (pthread_create(&threadRaw[i], NULL, (void*) & doIfaceLoop, &ifaceId[i])) err("error creating port thread");
    }

    doMainLoop();
}
