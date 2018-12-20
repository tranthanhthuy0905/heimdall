#!/bin/sh
set -e

ARTIFACT_BASE="builddata"

./bin/sbt clean coverage test coverageReport

# clean up old reports/scoverage
rm -rf ${ARTIFACT_BASE}/

# fix teamcity build error
rm -rf scoverage-reports

# Aggregate reports - has to be done after the multi-project run
#./bin/sbt coverageAggregate

# Grab unit test reports and coverage reports
# and put it in a reports/ folder to survive the clean
mkdir -p ${ARTIFACT_BASE}
find . -name test-reports ! -path "*/${ARTIFACT_BASE}/*" -exec rsync -arR {} ${ARTIFACT_BASE}/ \;
find target -name scoverage-report -exec cp -r {}/ ${ARTIFACT_BASE}/coverage-report \;

# fix build error in teamcity
cp -r ${ARTIFACT_BASE}/coverage-report scoverage-reports

# Do a clean - we don't want to accidentally reuse instrumented binaries!!!!
bin/sbt clean
