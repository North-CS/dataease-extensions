#!/bin/sh
mvn clean package

cp view-sankey-backend/target/view-sankey-backend-*.jar .

zip -r sankey.zip  ./view-sankey-backend-*.jar ./plugin.json

rm -f ./view-sankey-backend-*.jar
