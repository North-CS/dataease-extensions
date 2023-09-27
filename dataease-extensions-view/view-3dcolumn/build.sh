#!/bin/sh
mvn clean package

cp view-3dcolumn-backend/target/view-3dcolumn-backend-*.jar .

zip -r 3dcolumn.zip  ./view-3dcolumn-backend-*.jar ./plugin.json

rm -f ./view-3dcolumn-backend-*.jar
