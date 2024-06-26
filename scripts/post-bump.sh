#!/usr/bin/env bash

# If previous version and version arent set then exit
if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Previous version or version not set"
  exit 1
fi

git push origin || exit 1
git push origin v"${2}" || exit 1 # Push the new version tag for the release

cog changelog v"${1}"..v"${2}" | gh release create "v$2" -F - -t "Terix release $2" "build/libs/Terix-$2.jar" "build/Terix-API/libs/Terix-API-$2.jar"

gh workflow run "docs.yml" # Generate the documentation

git fetch --tags origin # Fetch the tags from the origin
sed -i "s/version=.*/version=$3/" ./gradle.properties # We now in snapshot

