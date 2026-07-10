#!/bin/bash
rm -rf bin framework.jar
mkdir bin
javac -source 17 -target 17 -cp "lib/jakarta.servlet-api-6.0.0.jar" -d bin $(find src -name "*.java")
jar cvf framework.jar -C bin .

mv framework.jar /home/main/Documents/L2/s4/Spring/Application_test/lib/framework.jar