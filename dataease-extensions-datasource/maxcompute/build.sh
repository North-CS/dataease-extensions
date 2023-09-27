#!/bin/sh
mvn clean package

cp maxcompute-backend/target/maxcompute-backend-*.jar .

zip -r maxcompute.zip  ./maxcompute-backend-*.jar ./maxcomputeDriver   ./plugin.json

rm -f maxcompute-backend-*.jar
