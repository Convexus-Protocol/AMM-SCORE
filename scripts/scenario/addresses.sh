#!/bin/bash

. ./scripts/util/get_address.sh
. ./scripts/convexus/pkg.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 1
fi

endpoint=$1

echo "export const DEFAULT_BOOKMARK = {"
echo "  \"$(getAddress "$(getFactoryPkg)" ${endpoint})\": \"Factory\","
echo "  \"$(getAddress core/pools/lisbon/bnusd-icx/pool ${endpoint})\": \"BNUSD/ICX Pool\","
echo "  \"$(getAddress core/pools/lisbon/bnusd-usdc/pool ${endpoint})\": \"BNUSD/USDC Pool\","
echo "  \"$(getAddress core/pools/lisbon/usdc-icx/pool ${endpoint})\": \"USDC/ICX Pool\","
echo "  \"$(getAddress core/pools/lisbon/usdc-sicx/pool ${endpoint})\": \"USDC/SICX Pool\","
echo "  \"$(getAddress core/pools/lisbon/usdt-icx/pool ${endpoint})\": \"USDT/ICX Pool\","
echo "  \"$(getAddress core/pools/lisbon/weth-crv/pool ${endpoint})\": \"WETH/CRV Pool\","
echo "  \"$(getAddress $(getSwapRouterPkg) ${endpoint})\": \"Swap Router\","
echo "  \"$(getAddress $(getNonfungibleTokenPositionDescriptorPkg) ${endpoint})\": \"Position Descriptor\","
echo "  \"$(getAddress $(getNonFungiblePositionManagerPkg) ${endpoint})\": \"Position Manager\","
echo "  \"$(getAddress $(getReadOnlyPoolPkg) ${endpoint})\": \"Pool ReadOnly\","
echo "  \"$(getAddress $(getQuoterPkg) ${endpoint})\": \"Quoter\","
echo "  \"$(getAddress $(getPoolInitializerPkg) ${endpoint})\": \"Pool Initializer\","
echo "  \"$(getAddress tokens/bnUSD-token ${endpoint})\": \"bnUSD\","
echo "  \"$(getAddress tokens/sICX-token ${endpoint})\": \"sICX\","
echo "  \"$(getAddress tokens/USDC-token ${endpoint})\": \"USDC\","
echo "  \"$(getAddress tokens/USDT-token ${endpoint})\": \"USDT\","
echo "  \"$(getAddress tokens/CRV-token ${endpoint})\": \"CRV\","
echo "  \"$(getAddress tokens/WETH-token ${endpoint})\": \"WETH\","
echo "}"
