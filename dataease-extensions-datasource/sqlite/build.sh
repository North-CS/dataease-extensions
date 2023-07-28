#!/bin/sh
cd sqlite-frontend
npm run buildPlugin
cd ..

mvn clean package -U -Dmaven.test.skip=true

cp sqlite-backend/target/sqlite-backend-1.18.8.jar .

zip -r sqlite.zip  ./sqlite-backend-1.18.8.jar ./sqliteDriver   ./plugin.json

rm -f sqlite-backend-1.18.8.jar