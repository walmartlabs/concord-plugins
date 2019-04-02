#!/bin/bash

SERVER_ADDR="$1"
ORG_NAME="$2"
PROJECT_NAME="$3"

rm -rf target && mkdir target
cp concord.yml example.tf playbook.yml target/

cd target && zip -r payload.zip ./* > /dev/null && cd ..

read -p "Username: " CURL_USER
curl -u ${CURL_USER} -F archive=@target/payload.zip -F org=${ORG_NAME} -F project=${PROJECT_NAME} http://${SERVER_ADDR}/api/v1/process
