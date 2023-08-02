#!/bin/sh
mvn clean package -U -Dmaven.test.skip=true

#cp dremio-backend/target/dremio-backend-1.18.9-jar-with-dependencies.jar ./dremio-backend-1.18.9.jar
cp dremio-backend/target/dremio-backend-1.18.9.jar .

zip -r dremio.zip  ./dremio-backend-1.18.9.jar ./dremioDriver   ./plugin.json
