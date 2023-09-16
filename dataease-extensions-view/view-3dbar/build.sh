#!/bin/sh
mvn clean package

cp view-3dbar-backend/target/view-3dbar-backend-1.18.9.jar .

zip -r 3dbar.zip  ./view-3dbar-backend-1.18.9.jar ./plugin.json

rm -f ./view-3dbar-backend-1.18.9.jar
