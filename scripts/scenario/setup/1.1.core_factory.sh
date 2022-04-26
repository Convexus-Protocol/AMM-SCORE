#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh

source ./scripts/karma/pkg.sh

# Network must be given as a parameter of this script
if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
deployName="Core Pool Factory"
info "Deploying ${deployName}..."

# Package information
pkg=$(getFactoryStoragePkg)
javaPkg=":Karma-Bond:Contracts:Karma-FactoryStorage"
build="optimized"

# Setup packages
setupJavaDir ${javaPkg} ${build}
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})

# Deploy on ICON network
jq -n '{}' > ${deployDir}/params.json
./run.py -e ${network} deploy ${pkg}

success "${deployName} contract has been successfully deployed!"