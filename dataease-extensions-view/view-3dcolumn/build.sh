#!/bin/sh
mvn clean package

cp view-3dcolumn-backend/target/view-3dcolumn-backend-1.18.9.jar .

zip -r 3dcolumn.zip  ./view-3dcolumn-backend-1.18.9.jar ./plugin.json

rm -f ./view-3dcolumn-backend-1.18.9.jar
