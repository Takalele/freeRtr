#!/bin/sh
./backup.sh
cd src
./c.sh
./cb.sh
cd ../misc/native/
./c.sh
./p.sh
cd ../image/
./cj.sh
./ci.sh
