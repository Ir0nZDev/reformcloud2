@echo off
java -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:CompileThreshold=100 -XX:+UseCompressedOops -Xmx512m -Xms256m -jar runner.jar