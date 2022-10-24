#!/bin/bash

set -e


source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh

if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
deployName="Convexus Core Pool Factory"
info "Deploying ${deployName}..."

# Package information
pkg="core/factory"
javaPkg=":Convexus-Core:Contracts:Factory"
build="optimized"

# Setup packages
setupJavaDir ${pkg} ${javaPkg} ${build}
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})

# Start
info "Cleaning..."
./gradlew "${javaPkg}:clean" > /dev/null

# Deploy on ICON network
jq -n '{}' > ${deployDir}/params.json
python run.py -e ${network} deploy ${pkg}

success "${deployName} contract has been successfully deployed!"
