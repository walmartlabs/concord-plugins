#!/usr/bin/env bash

# Run the task locally with the Concord CLI.
# NOTE: this only supports runtime-v2 workflows

if [ -f "${PARAM_FILE}" ]; then
    echo "Reading settings from ${PARAM_FILE}"
    source "${PARAM_FILE}" # get required parameters from a file
fi

# check for vault base URL parameter
if [ -z "${VAULT_BASE_URL}" ]; then
  echo "VAULT_BASE_URL is unset"
  exit 1
fi

# check for vault api token parameter
if [ -z "${VAULT_TOKEN}" ]; then
  echo "VAULT_TOKEN is unset"
  exit 1
fi

# check for vault namespace parameter
if [ -z "${VAULT_NS}" ]; then
  echo "No vault namespace set"
  VAULT_NS=""
fi

# check for vault namespace parameter
if [ -z "${VAULT_PATH}" ]; then
  echo "VAULT_PATH is unset"
  exit 1
fi

echo "Validating pom.xml and testCLI.yml versions match"

# detect version from pom.xml
pomVersion=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

# detect version
ymlVersion=$(grep -e "mvn://.*hashivault-task" testCLI.yml | cut -d':' -f 4 | cut -d'"' -f 1)

if [ "${pomVersion:-missingpom}" != "${ymlVersion-missingyml}" ]
then
  echo "Looks like testCLI.yml plugin version doesn't match pom.xml version."
  echo " pom.xml: ${pomVersion}"
  echo "testCLI.yml: ${ymlVersion}"
  exit 1
fi

# see if task jar needs build/rebuild
doBuild=1
if [ -f ./target/last_hash.md5 ]; then
  echo "Checking for ./src modifications"
  lastHash=$(cat ./target/last_hash.md5)
  # md5sum all source files, then md5sum the result
  currentHash=$(find ./src -type f -exec md5sum {} + | md5sum | awk '{print $1}')
  if [[ "${lastHash}" == "${currentHash}" ]]; then
    echo "No src files changed. Skipping build..."
    doBuild=
  else
    echo "${currentHash}" > ./target/last_hash.md5
  fi
fi

# build task jar if src files changed
if [ $doBuild ]; then
  echo "Building task jar..."
  # cd to parent and build for build
  pushd ../..
  # build the task
  mvn -N -DskipTests=true clean install
  mvn -pl tasks/hashivault -DskipTests=true install
  popd || exit
  # save md5 of src files to check next time
  find ./src -type f -exec md5sum {} + | md5sum | awk '{print $1}' > target/last_hash.md5
fi

# Execute Concord CLI
concord run \
 -e vaultBaseUrl="${VAULT_BASE_URL}" \
 -e vaultToken="${VAULT_TOKEN}" \
 -e vaultNs="${VAULT_NS}" \
 -e vaultPath="${VAULT_PATH}" \
 testCLI.yml
