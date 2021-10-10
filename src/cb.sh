#!/bin/sh
echo compiling
cp rtr.jar rtr2.jar
zip -d rtr2.jar "net/freertr/pipe/pipeWindow*"
native-image --allow-incomplete-classpath --no-fallback --enable-all-security-services -jar rtr2.jar rtr.bin
