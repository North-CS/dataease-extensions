#!/bin/sh
mvn clean package -U -Dmaven.test.skip=true

cp dremio-backend/target/dremio-backend-1.18.8.jar .

zip -r dremio.zip  ./dremio-backend-1.18.8.jar ./dremioDriver   ./plugin.json
