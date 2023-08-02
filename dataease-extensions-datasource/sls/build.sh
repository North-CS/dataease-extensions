#!/bin/sh
mvn clean package -U -Dmaven.test.skip=true

cp sls-backend/target/sls-backend-1.18.3-jar-with-dependencies.jar ./sls-backend-1.18.3.jar

zip -r sls.zip  ./sls-backend-1.18.3.jar ./slsDriver ./plugin.json
