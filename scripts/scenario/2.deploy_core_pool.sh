#!/bin/bash

set -e


source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

source ./scripts/convexus/pkg.sh

if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <poolId>"
  exit 1
fi

poolId=$1

# Start
info "Setting up tokens..."
${setupDir}/2.1.token0.sh ${poolId}
${setupDir}/2.2.token1.sh ${poolId}

info "Deploying the Pool from the Factory..."
${setupDir}/2.3.deploy_pool_from_factory.sh ${poolId}

info "Initializing the Pool ..."
${setupDir}/2.4.setup_pool.sh ${poolId}