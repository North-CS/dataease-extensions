#!/bin/sh
mvn clean package -U -Dmaven.test.skip=true

cp influxdb-backend/target/influxdb-backend-1.18.4-jar-with-dependencies.jar ./influxdb-backend-1.18.4.jar

zip -r influxdb.zip  ./influxdb-backend-1.18.4.jar ./influxdbDriver ./plugin.json
