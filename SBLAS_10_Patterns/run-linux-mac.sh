#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
MAVEN_OPTS="${MAVEN_OPTS:-} -Dsblas.deadline.seconds=604800 -Dsblas.escalation.poll.seconds=15" mvn clean compile exec:java
