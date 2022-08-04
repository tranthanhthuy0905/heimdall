#!/bin/bash

set -ex

#------------------------ Setting docker image version --------------------
dateStr=$(date "+%Y%m%d")
version="${dateStr}.${BUILD_NUMBER}"
release_branch_regex='^refs/heads/releases/(release-2[0-9]{3}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2})$'

if [[ "$BRANCH_NAME" =~ $release_branch_regex ]]; then
  version="$version-${BASH_REMATCH[1]}"
fi

# service build already test it
#./tests.sh

bin/sbt compile outputVersion package universal:packageBin -Dpackaging.buildQualifier="" -Dpackaging.buildNumber="$version" \
  clean \
  docker:publishLocal \
  writeDockerImageList
