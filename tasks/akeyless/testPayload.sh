#!/usr/bin/env bash

# Run the task locally with the Concord CLI.
# NOTE: this only supports runtime-v2 workflows

if [ -f "${PARAM_FILE}" ]; then
    echo "Reading settings from ${PARAM_FILE}"
    source "${PARAM_FILE}" # get required parameters from a file
fi


echo "Validating pom.xml and test.yml versions match"

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
  mvn -pl tasks/akeyless -DskipTests=true clean install
  popd || exit
  # save md5 of src files to check next time
  find ./src -type f -exec md5sum {} + | md5sum | awk '{print $1}' > target/last_hash.md5
fi

# remove old payload files
rm -rf target/test && mkdir -p target/test
# copy the test flow
if [ "${RUNTIME:-}" == "v2" ]; then
  echo "Using runtime-v2"
  cp testPayloadV2.yml target/test/concord.yml
else
  echo "Using runtime-v1"
  cp testPayloadV1.yml target/test/concord.yml
fi

# copy the task's JAR...
mkdir -p target/test/lib/
cp target/akeyless-task-*SNAPSHOT.jar target/test/lib/

# ...and dependencies
mvn dependency:copy-dependencies -DoutputDirectory="${PWD}/target/lib_tmp/" -Dmdep.useSubDirectoryPerScope > /dev/null
cp target/lib_tmp/compile/*.jar target/test/lib/

# enable JVM debugging if parameter given and target server is localhost
if [ "${DEBUG:-}" == "true" ]; then
  if [[ "${SERVER_URL}" =~ .*"localhost".*  ]]; then
    echo "Debug enabled"
    cat > target/test/_agent.json << EOF
{
    "jvmArgs": ["-Xdebug", "-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y"]
}
EOF
  else
    echo "Debug not allowed on non-localhost servers"
  fi
fi


pushd target/test > /dev/null && zip -r payload.zip ./* > /dev/null && popd > /dev/null || exit

HTTP_CODE=$(curl -sn \
  -o target/test/out.json \
  -w '%{http_code}' \
  ${PROXY:+ -x "${PROXY}"} \
  ${ORG:+ -F "org=${ORG}"} \
  ${PROJECT:+ -F "project=${PROJECT}"} \
  -F archive=@target/test/payload.zip \
  "${SERVER_URL}/api/v1/process"
)

ok=$(grep ok target/test/out.json | sed 's/.* : //;s/",\{0,1\}//')
instanceId=$(grep instanceId target/test/out.json | sed 's/.* "//;s/",\{0,1\}//')

if [[ "${ok}" == "true" ]]
then
  echo "Process started successfully."
  echo "       Logs: ${SERVER_URL}/#/process/${instanceId}/log"
  echo "Form Wizard: ${SERVER_URL}/#/process/${instanceId}/wizard?fullScreen=true"
else
  echo "uh oh. HTTP code : ${HTTP_CODE}"
  echo "${SERVER_URL}/#/process/${instanceId}/log"
fi
