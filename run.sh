#!/bin/bash
first_device=`adb devices | awk  'NR==2' | awk  '{print $1}'`
packageName="com.malin.v8"
echo "apk will install to "$first_device
adb -s $first_device uninstall $packageName
gradle -q app:installDebug -x lint --parallel --offline --continue &&
adb -s $first_device shell am start $packageName/.MainActivity
