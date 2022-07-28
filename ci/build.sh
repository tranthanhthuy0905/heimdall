#!/bin/bash

set -exu

#------------------------ Setting docker image version --------------------
SERVICE="heimdall"
dateStr=$(date "+%Y%m%d")
version=$dateStr.$BUILD_NUMBER
release_branch_regex='^refs/heads/releases/(release-2[0-9]{3}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2})$'

DOCKER_IMAGE_NAME="ecom/${SERVICE}"
DOCKER_IMAGE_VERSION="$dateStr.$BUILD_NUMBER"
DOCKER_IMAGE_TAG="$DOCKER_IMAGE_NAME:$DOCKER_IMAGE_VERSION"

if [[ $BRANCH_NAME =~ $release_branch_regex ]]; then
  version="$version-${BASH_REMATCH[1]}"
fi

# Output docker images and tag to script
echo ${DOCKER_IMAGE_TAG} >>docker-images.txt

# Let Argocd update job
echo "##teamcity[setParameter name='docker.image_tag' value='${version}']"

./tests.sh

bin/sbt compile outputVersion package universal:packageBin -Dpackaging.buildQualifier="" -Dpackaging.buildNumber="$version" \
  clean \
  docker:publishLocal
