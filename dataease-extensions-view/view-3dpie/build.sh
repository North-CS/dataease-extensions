#!/bin/sh
mvn clean package

cp view-3dpie-backend/target/view-3dpie-backend-*.jar .

zip -r 3dpie.zip  ./view-3dpie-backend-*.jar ./plugin.json

rm -f ./view-3dpie-backend-*.jar
