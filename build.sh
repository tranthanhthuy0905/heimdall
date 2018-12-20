#!/bin/bash

set -e
set -x

if [[ -z $BUILD_NUMBER ]]; then
  echo 'BUILD_NUMBER not defined. Defaulting to 0!' >&2
  BUILD_NUMBER=0
fi

if [[ -z $BRANCH_NAME ]]; then
  echo 'BRANCH_NAME not defined. Defaulting to "master"!' >&2
  BRANCH_NAME="master"
fi

if [ $BRANCH_NAME == "master" ];
then
  export BUILD_QUALIFIER=""
else
  export BUILD_QUALIFIER="-$BRANCH_NAME-SNAPSHOT"
fi

./bin/sbt compile outputVersion package universal:packageBin -Dpackaging.buildQualifier="$BUILD_QUALIFIER" -Dpackaging.buildNumber="$BUILD_NUMBER"
./bin/sbt coverage test coverageReport
