#!/bin/sh
java -Xmx512m -jar rtr.jar test tester intop1- persist intop1.ini discard .*ike.* summary slot 101 retry 8 url http://sources.freertr.org/cfg/ randord $@
./te.sh
