#!/usr/bin/env bash


# SERVER_URL='https://my-concord.com' DB_URL='https://puppet-db.com:8081' RBAC_URL='https://puppet-rbac.com:4433' PUPPET_TOKEN='api-token' MOM_CERT='/path/to/cert.pem' ./test.sh
# or
# PARAM_FILE=/path/to/params.properties ./test.sh

if [ -f "${PARAM_FILE}" ]; then
    echo "Reading settings from ${PARAM_FILE}"
    source "${PARAM_FILE}" # get required parameters from a file
fi

# Make sure we have a concord server url
if [ -z "${SERVER_URL}" ]; then
  echo 'SERVER_URL is unset' >&2
  exit 1
fi

# Make sure we have the databaseUrl url
if [ -z "${DB_URL}" ]; then
  echo 'DB_URL is unset' >&2
  exit 1
fi

# Make sure we have the RBAC url
if [ -z "${RBAC_URL}" ]; then
  echo 'RBAC_URL is unset' >&2
  exit 1
fi

# Warn if there's no api token
if [ -z "${PUPPET_TOKEN}" ]; then
  echo 'PUPPET_TOKEN is unset. Some calls will fail.' >&2
fi

# Warn if there's no api token
if [ -z "${MOM_CERT}" ]; then
  echo 'MOM_CERT is unset. Https calls may fail.' >&2
fi




# build the task
mvn clean package -DskipTests

rm -rf target/test && mkdir -p target/test
# copy the test flow
cp test.yml target/test/concord.yml

# copy the task's JAR...
mkdir -p target/test/lib/
cp target/puppet-task-*.jar target/test/lib/

# ...and dependencies
mvn dependency:copy-dependencies -DoutputDirectory=$(pwd)/target/lib_tmp/ -Dmdep.useSubDirectoryPerScope > /dev/null
cp target/lib_tmp/compile/*.jar target/test/lib/


# copy MoM cert or create an empty one
if [ -z "${MOM_CERT}" ]; then
    touch target/test/puppet-mom.pem
else
    cp "${MOM_CERT}" target/test/puppet-mom.pem
fi

pushd target/test > /dev/null && zip -r payload.zip ./* > /dev/null && popd > /dev/null

HTTP_CODE=$(curl -sn \
  -o target/test/out.json \
  -w '%{http_code}' \
  -F org=Default \
  -F arguments.puppetParams.databaseUrl=${DB_URL} \
  -F arguments.puppetParams.rbacUrl=${RBAC_URL} \
  -F arguments.puppetParams.apiToken=${PUPPET_TOKEN} \
  -F arguments.puppetParams.certificate.path='puppet-mom.pem' \
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
