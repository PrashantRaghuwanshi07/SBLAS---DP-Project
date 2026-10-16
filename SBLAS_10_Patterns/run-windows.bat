@echo off
cd /d "%~dp0"
set MAVEN_OPTS=-Dsblas.deadline.seconds=604800 -Dsblas.escalation.poll.seconds=15
mvn clean compile exec:java
pause
