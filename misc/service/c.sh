#!/bin/sh

TRG=$1

if [ -z "$TRG" ] ; then
  echo "usage: c.sh <target directory>"
  exit
  fi

echo installing to $TRG directory

echo net.ipv6.conf.all.disable_ipv6=1 > /etc/sysctl.d/disableipv6.conf
echo net.ipv6.conf.default.disable_ipv6=1 > /etc/sysctl.d/disableipv6.conf

cat > /etc/network/interfaces << EOF
iface lo inet loopback
#allow-hotplug eth0
#iface eth0 inet dhcp
#iface eth0 inet6 auto
EOF

cat > /etc/resolv.conf << EOF
nameserver 8.8.8.8
EOF

cat > /etc/init.d/rtr << EOF
#!/bin/sh

### BEGIN INIT INFO
# Provides:          rtr
# Required-Start:    $local_fs
# Required-Stop:
# Default-Start:     2 3 4 5
# Default-Stop:
# Short-Description: router processes
### END INIT INFO

case "$1" in
  start)
        cd $TRG
        $TRG/hwdet-all.sh
        ;;
  stop)
        kill -9 `pidof java` `pidof pcapInt.bin` `pidof rawInt.bin` `pidof mapInt.bin` `pidof tapInt.bin` `pidof vlan.bin` `pidof bundle.bin` `pidof socat`
        ;;
  status)
        echo java: `pidof java`
        echo pcapInt: `pidof pcapInt.bin`
        echo rawInt: `pidof rawInt.bin`
        echo mapInt: `pidof mapInt.bin`
        echo tapInt: `pidof tapInt.bin`
        echo vlan: `pidof vlan.bin`
        echo bundle: `pidof bundle.bin`
        echo socat: `pidof socat`
        ;;
  restart)
        $0 stop
        $0 start
  ;;
  *)
        echo "Usage: $N {start|stop|status|restart}" >&2
        exit 1
        ;;
esac

exit 0
EOF

chmod 777 /etc/init.d/rtr
update-rc.d rtr defaults

cat > /lib/systemd/system/rtr.service << EOF
[Unit]
Description=router processes
Wants=network.target
After=network-pre.target
Before=network.target

[Service]
Type=forking
ExecStart=$TRG/hwdet-all.sh

[Install]
WantedBy=multi-user.target
EOF

systemctl set-default multi-user.target
rm /usr/lib/systemd/network/*
SVC="network-manager NetworkManager ModemManager systemd-networkd systemd-networkd-wait-online wpa_supplicant"
systemctl disable $SVC
systemctl mask $SVC
systemctl unmask rtr
systemctl enable rtr

mkdir -p $TRG
cp ../default.cfg $TRG/rtr-sw.txt
cp ../../src/rtr.jar $TRG/
cp ../../binTmp/*.bin $TRG/
cp /proc/net/dev $TRG/hwdet.eth
cp /proc/tty/driver/serial $TRG/hwdet.ser
ip link show > $TRG/hwdet.mac
java -jar $TRG/rtr.jar test hwdet tuntap 10.255.255.1/24 10.255.255.254 path $TRG/ iface raw line raw
chmod 777 $TRG/*.sh
ls -l $TRG/
