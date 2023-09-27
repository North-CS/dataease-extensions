#!/bin/sh
 mvn clean package -U -Dmaven.test.skip=true

cp kylin-backend/target/kylin-backend-*.jar .

zip -r kylin.zip  ./kylin-backend-*.jar ./kylinDriver   ./plugin.json

rm -f kylin-backend-*.jar
