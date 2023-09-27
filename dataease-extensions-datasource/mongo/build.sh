#!/bin/sh
mvn clean package -U -Dmaven.test.skip=true

cp mongo-backend/target/mongo-backend-*.jar .

zip -r mongo.zip  ./mongo-backend-*.jar ./mongobiDriver   ./plugin.json

rm -f mongo-backend-*.jar
