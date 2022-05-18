#!/bin/bash

set -e

# --- Core ---

# Factory
getFactoryPkg() {
  echo "core/factory"
}

# Pool
getPoolPkg() {
  poolId=$1
  echo "core/pools/${poolId}/pool"
}

# SwapRouter
getSwapRouterPkg() {
  echo "periphery/swaprouter"
}

# NonFungiblePositionManager
getNonFungiblePositionManagerPkg() {
  echo "periphery/positionmgr"
}

# ReadOnlyPool
getReadOnlyPool() {
  echo "periphery/readonlypool"
}

# Quoter
getQuoter() {
  echo "periphery/quoter"
}

# Pool Initializer
getPoolInitializerPkg() {
  echo "periphery/poolinitializer"
}

# NonfungibleTokenPositionDescriptor
getNonfungibleTokenPositionDescriptorPkg() {
  echo "periphery/positiondescriptor"
}

# Tokens
getToken0Pkg () {
  poolId=$1
  echo core/pools/${poolId}/token0
}

getToken1Pkg () {
  poolId=$1
  echo core/pools/${poolId}/token1
}