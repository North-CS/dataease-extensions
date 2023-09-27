#!/bin/sh
mvn clean package -U -Dmaven.test.skip=true

cp dremio-backend/target/dremio-backend-*.jar .

zip -r dremio.zip  ./dremio-backend-*.jar ./dremioDriver   ./plugin.json

rm -f dremio-backend-*.jar