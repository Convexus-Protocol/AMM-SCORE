#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

# Network must be given as a parameter of this script
if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
info "Cleaning..."
./gradlew clean > /dev/null

# --- Deploy Factory Storage ---
${setupScriptsDir}/1.1.karmabond_factorystorage.sh ${network}

# --- Deploy Subsidy Router ---
${setupScriptsDir}/1.2.karmabond_subsidyrouter.sh ${network}

# --- Deploy Factory ---
${setupScriptsDir}/1.3.karmabond_factory.sh ${network}

# --- Deploy Treasury ---
${setupScriptsDir}/1.4.karmabond_treasury.sh ${network}

# --- Deploy DAO ---
${setupScriptsDir}/1.5.karmabond_dao.sh ${network}

success "All Karma Bond core contracts have been successfully deployed!"