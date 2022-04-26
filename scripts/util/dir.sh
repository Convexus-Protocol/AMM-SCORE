#!/bin/bash

set -e

CONFIG_DIR=./config
DEPLOY_DIR=${CONFIG_DIR}/deploy
CALLS_DIR=${CONFIG_DIR}/calls

getDeployDir () {
  pkg=$1
  network=$2
  deployDir=${DEPLOY_DIR}/${pkg}/${network}
  echo ${deployDir}
}

setupDeployDir () {
  pkg=$1
  network=$2
  deployDir=$(getDeployDir ${pkg} ${network})
  mkdir -p ${deployDir}
}

getCallsDir () {
  pkg=$1
  network=$2
  callsDir=${CALLS_DIR}/${pkg}/${network}
  echo ${callsDir}
}

setupCallsDir () {
  pkg=$1
  network=$2
  callsDir=$(getCallsDir ${pkg} ${network})
  mkdir -p ${callsDir}
}

getJavaDir () {
  javaPkg=$1
  build=$2
  javaDir=${DEPLOY_DIR}/${pkg}
  echo ${javaDir}
}

setupJavaDir () {
  javaPkg=$1
  build=$2
  javaDir=$(getJavaDir ${javaPkg} ${build})
  mkdir -p ${javaDir}

  jq -n \
    --arg javaPkg $javaPkg \
    --arg build $build \
  '{javaPkg: $javaPkg, build: $build}' > ${javaDir}/build.json
}
