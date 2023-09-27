#!/bin/sh
mvn clean package -U -Dmaven.test.skip=true

cp dm-backend/target/dm-backend-*.jar .

zip -r dm.zip  ./dm-backend-*.jar ./dmDriver   ./plugin.json

rm -f dm-backend-*.jar