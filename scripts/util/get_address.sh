#!/bin/bash

set -e

getAddress () {
  package=$1
  endpoint=$2
  cat ./scripts/config/deploy/${package}/${endpoint}/deploy.json | jq .scoreAddress -r
}
