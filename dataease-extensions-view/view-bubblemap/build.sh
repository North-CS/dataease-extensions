#!/bin/sh
mvn clean package

cp view-bubblemap-backend/target/view-bubblemap-backend-*.jar .

zip -r bubblemap.zip  ./view-bubblemap-backend-*.jar ./plugin.json

rm -f ./view-bubblemap-backend-*.jar
