#!/bin/sh
mvn clean package -U -Dmaven.test.skip=true

version=`mvn -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -q`
cp sls-backend/target/sls-backend-*-jar-with-dependencies.jar ./sls-backend-${version}.jar

zip -r sls.zip  ./sls-backend-*.jar ./slsDriver ./plugin.json

rm -f sls-backend-*.jar