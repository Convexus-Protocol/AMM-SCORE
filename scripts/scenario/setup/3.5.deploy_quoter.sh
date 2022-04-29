#!/bin/bash

set -e

source ./venv/bin/activate

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
deployName="Convexus Quoter"
info "Setting up ${deployName}..."

# Package information
pkg=$(getQuoter)
javaPkg=":Convexus-Periphery:Contracts:Quoter"
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
readOnlyPoolPkg=$(getReadOnlyPool)
factory=$(getAddress ${factoryPkg} ${network})
readOnlyPool=$(getAddress ${readOnlyPoolPkg} ${network})
filter=$(cat <<EOF
{
  factory: \$factory,
  readOnlyPool: \$readOnlyPool
}
EOF
)

jq -n \
  --arg factory $factory \
  --arg readOnlyPool $readOnlyPool \
  "${filter}" > ${deployDir}/params.json

./run.py -e ${network} deploy ${pkg}

success "${deployName} contract has been successfully deployed!"
