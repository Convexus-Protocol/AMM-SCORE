#!/bin/bash

set -e


source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

source ./scripts/convexus/pkg.sh

if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <tokenId>"
  exit 1
fi

tokenId=$1

# Start

actionName="positions"

filter=$(cat <<EOF
{
  method: "symbol"
}
EOF
)
jq -n \
  "${filter}" > ${tokenCallsDir}/${actionName}.json

echo $(python run.py -e ${network} call ${tokenPkg} ${actionName})