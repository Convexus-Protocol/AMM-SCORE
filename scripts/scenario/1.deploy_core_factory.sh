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

# --- Deploy Pool Factory ---
${setupDir}/1.1.deploy_core_factory.sh ${network}
${setupDir}/1.2.install_core_factory.sh ${network}