#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

source ./scripts/convexus/pkg.sh

if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start

# Deploy Swap Router
info "Deploying the SwapRouter..."
${setupDir}/3.1.deploy_swap_router.sh ${network}

# Deploy NFTPosition Descriptor
info "Deploying the NFT Position Descriptor..."
${setupDir}/3.2.deploy_nft_position_descriptor.sh ${network}

# Deploy NFTPosition Manager
info "Deploying the NFT Position Manager..."
${setupDir}/3.3.deploy_nft_position_manager.sh ${network}
