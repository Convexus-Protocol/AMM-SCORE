#!/bin/bash

set -e

# Core
getFactoryStoragePkg () {
  echo karmabond/factorystorage
}

getSubsidyRouterPkg () {
  echo karmabond/subsidyrouter
}

getFactoryPkg () {
  echo karmabond/factory
}

getKarmaTreasuryPkg () {
  echo karmabond/treasury
}

getKarmaDAOPkg () {
  echo karmabond/dao
}

# Bond
getPayoutPkg () {
  bondId=$1
  echo bonds/${bondId}/payout
}

getPrincipalPkg () {
  bondId=$1
  echo bonds/${bondId}/principal
}

getCustomTreasuryPkg () {
  bondId=$1
  echo bonds/${bondId}/customTreasury
}

getCustomBondPkg () {
  bondId=$1
  echo bonds/${bondId}/bond
}