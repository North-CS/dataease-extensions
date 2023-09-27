#!/bin/sh
mvn clean package

cp view-symbolmap-backend/target/view-symbolmap-backend-*.jar .

zip -r symbolmap.zip  ./view-symbolmap-backend-*.jar ./plugin.json

rm -f ./view-symbolmap-backend-*.jar
