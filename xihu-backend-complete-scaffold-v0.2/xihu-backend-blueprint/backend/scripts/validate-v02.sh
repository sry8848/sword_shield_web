#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
java -version
mkdir -p .verify-classes
trap 'rm -rf .verify-classes' EXIT
find common/src/main/java      content-service/src/main/java file-service/src/main/java      interaction-service/src/main/java incentive-service/src/main/java      limited-seat-service/src/main/java payment-service/src/main/java      customer-service/src/main/java analytics-service/src/main/java      workflow-service/src/main/java organization-service/src/main/java      -name '*.java'     ! -name 'ApiEnvelope.java' ! -name 'ApiFailure.java'      > .verify-sources
trap 'rm -rf .verify-classes .verify-sources' EXIT
javac --release 17 -encoding UTF-8 -d .verify-classes     @.verify-sources scripts/ScopeContractCheck.java scripts/ExtendedAuthorizationCheck.java
java -cp .verify-classes ScopeContractCheck
java -cp .verify-classes ExtendedAuthorizationCheck
printf '%s\n' 'PASS: all future contract module sources compile with Java 17'
