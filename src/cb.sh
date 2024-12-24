#!/bin/sh
echo compiling
cp rtr.jar rtr2.jar
zip -d rtr2.jar "org/freertr/pipe/pipeWindow*"
zip -d rtr2.jar "org/freertr/user/userGame*"
native-image -O3 --no-fallback -jar rtr2.jar -H:+UnlockExperimentalVMOptions -H:-ReduceImplicitExceptionStackTraceInformation -o rtr.bin
upx -o rtr2.bin rtr.bin
