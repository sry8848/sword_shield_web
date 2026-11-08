#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" common/src/main/java/com/swordshield/next/common/{ScopeKind,ResourceRef,ScopeDecision,ScopeEvaluator,TenantActor}.java scripts/ScopeContractCheck.java
java -cp "$TMP" ScopeContractCheck
printf 'Pure Java scope model compiled OK. Full Spring build requires Maven.\n'
