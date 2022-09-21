#!/bin/bash

. ./scripts/util/get_address.sh
. ./scripts/convexus/pkg.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 1
fi

endpoint=$1

echo "export const DEFAULT_BOOKMARK = {"
echo "  \"$(getAddress "core/factory" ${endpoint})\": \"Factory\","

if [ ${endpoint} == "berlin" ] ; then
  echo "  \"$(getAddress core/pools/berlin/bnusd-icx/pool ${endpoint})\": \"BNUSD/ICX Pool\","
  echo "  \"$(getAddress core/pools/berlin/bnusd-usdc/pool ${endpoint})\": \"BNUSD/USDC Pool\","
  echo "  \"$(getAddress core/pools/berlin/usdc-icx/pool ${endpoint})\": \"USDC/ICX Pool\","
  echo "  \"$(getAddress core/pools/berlin/usdc-sicx/pool ${endpoint})\": \"USDC/SICX Pool\","
  echo "  \"$(getAddress core/pools/berlin/usdt-icx/pool ${endpoint})\": \"USDT/ICX Pool\","
  echo "  \"$(getAddress core/pools/berlin/weth-crv/pool ${endpoint})\": \"WETH/CRV Pool\","
fi

echo "  \"$(getAddress $(getSwapRouterPkg) ${endpoint})\": \"Swap Router\","
echo "  \"$(getAddress $(getNonfungibleTokenPositionDescriptorPkg) ${endpoint})\": \"Position Descriptor\","
echo "  \"$(getAddress $(getNonFungiblePositionManagerPkg) ${endpoint})\": \"Position Manager\","
echo "  \"$(getAddress $(getReadOnlyPool) ${endpoint})\": \"Pool ReadOnly\","
echo "  \"$(getAddress $(getQuoter) ${endpoint})\": \"Quoter\","
echo "  \"$(getAddress $(getPoolInitializerPkg) ${endpoint})\": \"Pool Initializer\","

if [ ${endpoint} == "berlin" ] ; then
  echo "  \"$(getAddress tokens/bnUSD-token ${endpoint})\": \"bnUSD\","
  echo "  \"$(getAddress tokens/CRV-token ${endpoint})\": \"CRV\","
  echo "  \"$(getAddress tokens/CTT-token ${endpoint})\": \"CTT\","
  echo "  \"$(getAddress tokens/sICX-token ${endpoint})\": \"sICX\","
  echo "  \"$(getAddress tokens/USDC-token ${endpoint})\": \"USDC\","
  echo "  \"$(getAddress tokens/USDT-token ${endpoint})\": \"USDT\","
  echo "  \"$(getAddress tokens/WETH-token ${endpoint})\": \"WETH\","
fi

echo "}"

