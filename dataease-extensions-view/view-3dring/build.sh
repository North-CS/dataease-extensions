#!/bin/sh
mvn clean package

cp view-3dring-backend/target/view-3dring-backend-*.jar .

zip -r 3dring.zip  ./view-3dring-backend-*.jar ./plugin.json

rm -f ./view-3dring-backend-*.jar
