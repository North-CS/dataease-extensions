#!/bin/sh
mvn clean package -U -Dmaven.test.skip=true

cp kingbase-backend/target/kingbase-backend-*.jar .

zip -r kingbase.zip  ./kingbase-backend-*.jar ./kingbaseDriver   ./plugin.json

rm -f kingbase-backend-*.jar
