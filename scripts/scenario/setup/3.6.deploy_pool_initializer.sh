#!/bin/bash

set -e


source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh

source ./scripts/convexus/pkg.sh

if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
deployName="Convexus Pool Initializer"
info "Setting up ${deployName}..."

# Package information
pkg=$(getPoolInitializerPkg)
javaPkg=":Convexus-Periphery:Contracts:PoolInitializer"
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
factoryPkg=$(getFactoryPkg)
factory=$(getAddress ${factoryPkg} ${network})
filter=$(cat <<EOF
{
  factory: \$factory
}
EOF
)

jq -n \
  --arg factory $factory \
  "${filter}" > ${deployDir}/params.json

python run.py -e ${network} deploy ${pkg}

success "${deployName} contract has been successfully deployed!"
