#!/bin/sh
cd sqlite-frontend
npm run buildPlugin
cd ..

mvn clean package -U -Dmaven.test.skip=true

cp sqlite-backend/target/sqlite-backend-*.jar .

zip -r sqlite.zip  ./sqlite-backend-*.jar ./sqliteDriver   ./plugin.json

rm -f sqlite-backend-*.jar