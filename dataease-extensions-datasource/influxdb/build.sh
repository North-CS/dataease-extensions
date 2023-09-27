#!/bin/sh
mvn clean package -U -Dmaven.test.skip=true

version=`mvn -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -q`
cp influxdb-backend/target/influxdb-backend-*-jar-with-dependencies.jar ./influxdb-backend-$version.jar

zip -r influxdb.zip  ./influxdb-backend-*.jar ./influxdbDriver ./plugin.json

rm -f influxdb-backend-*.jar
