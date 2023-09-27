#!/bin/sh
mvn clean package

cp presto-backend/target/presto-backend-*.jar .

zip -r presto.zip  ./presto-backend-*.jar ./prestoDriver   ./plugin.json

rm -f presto-backend-*.jar
