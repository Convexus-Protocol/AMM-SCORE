#!/bin/bash

set -e

CONFIG_DIR=./scripts/config
DEPLOY_DIR=${CONFIG_DIR}/deploy
CALLS_DIR=${CONFIG_DIR}/calls

getDeployDir () {
  _pkg=$1
  _network=$2
  _deployDir=${DEPLOY_DIR}/${_pkg}/${_network}
  echo ${_deployDir}
}

setupDeployDir () {
  _pkg=$1
  _network=$2
  _deployDir=$(getDeployDir ${_pkg} ${_network})
  mkdir -p ${_deployDir}
}

getCallsDir () {
  _pkg=$1
  _network=$2
  _callsDir=${CALLS_DIR}/${_pkg}/${_network}
  echo ${_callsDir}
}

setupCallsDir () {
  _pkg=$1
  _network=$2
  _callsDir=$(getCallsDir ${_pkg} ${_network})
  mkdir -p ${_callsDir}
}

getJavaDir () {
  _pkg=$1
  _javaDir=${DEPLOY_DIR}/${_pkg}
  echo ${_javaDir}
}

setupJavaDir () {
  _pkg=$1
  _javaPkg=$2
  _build=$3
  _javaDir=$(getJavaDir ${_pkg} ${_build})
  mkdir -p ${_javaDir}

  jq -n \
    --arg javaPkg $_javaPkg \
    --arg build $_build \
  '{javaPkg: $javaPkg, build: $build}' > ${_javaDir}/build.json
}
