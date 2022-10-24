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

# Mint a position
info "Mint a position..."
${setupDir}/4.1.deposit_to_position_manager.sh ${poolId}

info "Send tokens to testers..."
${setupDir}/4.2.send_tokens_to_testers.sh ${poolId}
