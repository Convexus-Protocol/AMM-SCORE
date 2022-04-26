#!/bin/bash

set -e

CONFIG_DIR=./config
DEPLOY_DIR=${CONFIG_DIR}/deploy
CALLS_DIR=${CONFIG_DIR}/calls

hex () {
  integer=$1
  python -c "print(hex(${integer}))"
}

unhex () {
  hex=$1
  python -c "print(int('${hex}', 16))"
}
