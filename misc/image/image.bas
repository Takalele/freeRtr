download 3
reget-time 8
arch amd64

del-ifdn %dwn%
exec mkdir %dwn%
del-alw %tmp%
exec mkdir -m 0755 %tmp%
exec mkdir -m 0755 %tmp%/rtr
exec mkdir -m 0755 %tmp%/sys
exec mkdir -m 0755 %tmp%/proc
exec mkdir -m 0755 %tmp%/tmp
exec mkdir -m 0755 %tmp%/mnt
exec mkdir -m 0755 %tmp%/var
exec mkdir -m 0755 %tmp%/var/lock
exec mkdir -m 0755 %tmp%/var/run
exec mkdir -m 0755 %tmp%/dev
exec mkdir -m 0755 %tmp%/run
exec mkdir -m 0755 %tmp%/etc
exec mkdir -m 0755 %tmp%/lib
exec mkdir -m 0755 %tmp%/lib32
exec mkdir -m 0755 %tmp%/lib64
exec mkdir -m 0755 %tmp%/bin
exec mkdir -m 0755 %tmp%/sbin
exec mkdir -m 0755 %tmp%/usr
exec mkdir -m 0755 %tmp%/usr/bin
exec mkdir -m 0755 %tmp%/usr/sbin

exec cp rtr.ver %tmp%/rtr/
exec cp ../binTmp/*.bin %tmp%/rtr/

#catalog-read dev xz http://at.archive.ubuntu.com/ubuntu/ devel main universe
#catalog-read prop xz http://at.archive.ubuntu.com/ubuntu/ devel-proposed main universe
#catalog-read liq gz https://liquorix.net/debian/ sid main
#catalog-read xan gz http://deb.xanmod.org/ releases main
catalog-read sid xz http://ftp.at.debian.org/debian/ sid main
catalog-read exp xz http://ftp.at.debian.org/debian/ experimental main

select-dis debconf.*
select-dis adduser.*
select-dis passwd.*
select-dis util-linux.*
select-dis dpkg.*
select-dis perl.*
select-dis gcc.*
select-dis init-system-helper.*
select-dis initramfs-tool.*
select-dis linux-initramfs-tool.*
select-dis linux-headers.*
select-dis multiarch-support.*
select-dis ucf.*
select-dis fontconfig-config.*
select-dis dmsetup.*
select-dis ca-certificate.*
select-dis mount.*
select-dis x11.*
select-dis lsb-base.*
select-dis linux-base.*
select-dis openssl.*
select-dis systemd.*
select-dis libasound.*
select-dis libavahi.*
select-dis libcups.*
select-dis libharfbuzz.*
select-dis liblcms.*
select-dis libfontconfig.*
select-dis libjpeg.*
select-dis libnss.*
select-dis libfreetype.*
select-dis libpcsc.*
select-dis libx11.*
select-dis libxext.*
select-dis libxi.*
select-dis libxrender.*
select-dis libxtst.*
