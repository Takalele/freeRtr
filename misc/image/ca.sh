#!/bin/sh
cd ../../src
for a in amd64 i686 arm64 arm32hf mips64 ppc32 ppc64el risc64 s390x loong sparc sh4 ; do
  java -Xmx256m -jar rtr.jar test image ../misc/image/plat.$a ../misc/image/image.jvm
  done
