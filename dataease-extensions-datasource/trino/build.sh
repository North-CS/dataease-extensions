#!/bin/sh
mvn clean package

cp trino-backend/target/trino-backend-*.jar .

zip -r trino.zip  ./trino-backend-*.jar ./trinoDriver   ./plugin.json

rm -f trino-backend-*.jar
