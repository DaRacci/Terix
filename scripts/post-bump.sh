#!/usr/bin/env bash

# If previous version and version arent set then exit
if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Previous version or version not set"
  exit 1
fi

if [ -f temp ]; then
  echo "Removing temp file"
  rm temp
fi

git push origin v"${2}" || exit 1 # Push the new version tag for the release

# Test the project
./gradlew clean build test

URL="https://github.com/DaRacci/Terix/compare/v$1..v$2"
grep -Poz "(?s)(?<=## \\[v$2\\]\\(${URL}\\) - ....-..-..\n).*?(?=- - -)" CHANGELOG.md >> ./.templog.md
head -n -1 .templog.md > .temp ; mv .temp .templog.md # Remove that weird null line

SEMIPATH=build/libs/Terix
gh release create "v$2" -F ./.templog.md -t "Terix release $2" $SEMIPATH-$2.jar Terix-API/$SEMIPATH-API-$2-sources.jar Terix-Core/$SEMIPATH-Core-$2.jar
rm ./.templog.md

git push origin master || exit 1 # Push the new version tag for the release

gh workflow run "docs.yml" # Generate the documentation

git fetch --tags origin # Fetch the tags from the origin
sed -i "s/version=.*/version=$3/" ./gradle.properties # We now in snapshot

