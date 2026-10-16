@echo off
cd /d "%~dp0"
REM Use a 60-second deadline only to demonstrate escalation in class.
set MAVEN_OPTS=-Dsblas.deadline.seconds=60 -Dsblas.escalation.poll.seconds=5
mvn clean compile exec:java
pause
