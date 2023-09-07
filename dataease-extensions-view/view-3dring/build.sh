#!/bin/sh
mvn clean package

cp view-3dring-backend/target/view-3dring-backend-1.18.9.jar .

zip -r 3dring.zip  ./view-3dring-backend-1.18.9.jar ./plugin.json

rm -f ./view-3dring-backend-1.18.9.jar
