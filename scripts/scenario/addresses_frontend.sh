#!/bin/bash

. ./scripts/util/get_address.sh
. ./scripts/convexus/pkg.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 1
fi

endpoint=$1

j=$(cat << EOF
{
  "ConvexusFactory": "$(getAddress "$(getFactoryPkg)" ${endpoint})",
  "ConvexusSwapRouter": "$(getAddress $(getSwapRouterPkg) ${endpoint}))",
  "ConvexusPositionDescriptor": "$(getAddress $(getNonfungibleTokenPositionDescriptorPkg) ${endpoint}))",
  "ConvexusPositionManager": "$(getAddress $(getNonFungiblePositionManagerPkg) ${endpoint}))",
  "ConvexusPoolReadOnly": "$(getAddress $(getReadOnlyPoolPkg) ${endpoint}))",
  "ConvexusQuoter": "$(getAddress $(getQuoterPkg) ${endpoint}))",
  "ConvexusPoolInitializer": "$(getAddress $(getPoolInitializerPkg) ${endpoint}))",
  "ConvexusNftPositionManager": "$(getAddress $(getNonFungiblePositionManagerPkg) ${endpoint}))",
  "pools": [
    {
      "name": "ICX/USDC",
      "address": "$(getAddress core/pools/berlin/usdc-icx/pool ${endpoint})",
      "token0": "cx1111111111111111111111111111111111111111",
      "token1": "$(getAddress tokens/USDC-token ${endpoint})"
    }
  ],
  "supportedTokens": [
    {
      "symbol": "ICX",
      "name": "ICON",
      "address": "cx1111111111111111111111111111111111111111",
      "decimals": 18,
      "img": "icx.png"
    },
    {
      "symbol": "WETH",
      "name": "WETH",
      "address": "$(getAddress tokens/WETH-token ${endpoint})",
      "decimals": 18,
      "img": "weth.png"
    },
    {
      "symbol": "CRV",
      "name": "CRV",
      "address": "$(getAddress tokens/CRV-token ${endpoint})",
      "decimals": 18,
      "img": "crv.png"
    },
    {
      "symbol": "USDC",
      "name": "ICON USD Coin",
      "address": "$(getAddress tokens/USDC-token ${endpoint})",
      "decimals": 6,
      "img": "iusdc.png"
    },
    {
      "symbol": "USDT",
      "name": "USDT",
      "address": "$(getAddress tokens/USDT-token ${endpoint})",
      "decimals": 6,
      "img": "iusdt.png"
    },
    {
      "symbol": "bnUSD",
      "name": "Balanced Dollars",
      "address": "$(getAddress tokens/bnUSD-token ${endpoint})",
      "decimals": 18,
      "img": "bnusd.png"
    },
    {
      "symbol": "sICX",
      "name": "Staked ICON",
      "address": "$(getAddress tokens/sICX-token ${endpoint})",
      "decimals": 18,
      "img": "sicx.png"
    }
  ]
}
EOF
)

echo $j | jq .