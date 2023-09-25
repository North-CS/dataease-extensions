#!/bin/sh
mvn clean package

cp view-3dber-backend/target/view-3dber-backend-1.18.9.jar .

zip -r 3dber.zip  ./view-3dber-backend-1.18.9.jar ./plugin.json

rm -f ./view-3dber-backend-1.18.9.jar
