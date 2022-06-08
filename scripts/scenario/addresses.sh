#!/bin/bash

. ./scripts/util/get_address.sh
. ./scripts/convexus/pkg.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 1
fi

endpoint=$1

echo " -- [Network: ${endpoint}] -- "

echo "--- Core Layer ---"
echo "Factory                : "$(getAddress "core/factory" ${endpoint})

if [ ${endpoint} == "custom" ]; then
echo "SICX/USDC Pool         : "$(getAddress "core/pools/custom/sicx-usdc/pool" ${endpoint})

echo "--- Tokens ---"
echo "USDC Token             : "$(getAddress "core/pools/custom/sicx-usdc/token0" ${endpoint})
echo "SICX Token             : "$(getAddress "core/pools/custom/sicx-usdc/token1" ${endpoint})
elif [ ${endpoint} == "sejong" ] ; then
echo "BNUSD/USDC Pool        : "$(getAddress "core/pools/sejong/bnusd-usdc/pool" ${endpoint})
echo "ICX/USDC Pool          : "$(getAddress "core/pools/sejong/usdc-icx/pool" ${endpoint})
echo "SICX/BNUSD Pool        : "$(getAddress "core/pools/sejong/usdc-sicx/pool" ${endpoint})

echo "--- Tokens ---"
echo "BNUSD Token            : "$(getAddress "core/pools/sejong/bnusd-usdc/token0" ${endpoint})
echo "USDC Token             : "$(getAddress "core/pools/sejong/bnusd-usdc/token1" ${endpoint})
echo "SICX Token             : "$(getAddress "core/pools/sejong/usdc-sicx/token1" ${endpoint})
elif [ ${endpoint} == "berlin" ] ; then
echo "BNUSD/USDC Pool        : "$(getAddress "core/pools/berlin/bnusd-usdc/pool" ${endpoint})
echo "ICX/USDC Pool          : "$(getAddress "core/pools/berlin/usdc-icx/pool" ${endpoint})
echo "SICX/BNUSD Pool        : "$(getAddress "core/pools/berlin/usdc-sicx/pool" ${endpoint})

echo "--- Tokens ---"
echo "BNUSD Token            : "$(getAddress "core/pools/berlin/bnusd-usdc/token1" ${endpoint})
echo "USDC Token             : "$(getAddress "core/pools/berlin/bnusd-usdc/token0" ${endpoint})
echo "SICX Token             : "$(getAddress "core/pools/berlin/usdc-sicx/token1" ${endpoint})
fi

echo "--- Periphery Layer ---"
echo "Swap Router            : "$(getAddress "periphery/swaprouter" ${endpoint})
echo "Position Descriptor    : "$(getAddress "periphery/positiondescriptor" ${endpoint})
echo "Position Manager       : "$(getAddress "periphery/positionmgr" ${endpoint})
echo "Pool ReadOnly          : "$(getAddress $(getReadOnlyPool) ${endpoint})
echo "Quoter                 : "$(getAddress $(getQuoter) ${endpoint})
echo "Staker                 : "$(getAddress $(getStaker) ${endpoint})
echo "Pool Initializer       : "$(getAddress $(getPoolInitializerPkg) ${endpoint})

echo "=========================================================="

echo "export const DEFAULT_BOOKMARK = {"
echo "  \"$(getAddress "core/factory" ${endpoint})\": \"[CORE] Factory\","

if [ ${endpoint} == "custom" ]; then
  echo "  \"$(getAddress "core/pools/custom/sicx-usdc/pool" ${endpoint})\": \"[CORE] SICX/USDC Pool\","
elif [ ${endpoint} == "sejong" ] ; then
  echo "  \"$(getAddress "core/pools/sejong/bnusd-usdc/pool" ${endpoint})\": \"[CORE] BNUSD/USDC Pool\","
  echo "  \"$(getAddress "core/pools/sejong/usdc-icx/pool" ${endpoint})\": \"[CORE] ICX/USDC Pool\","
  echo "  \"$(getAddress "core/pools/sejong/usdc-sicx/pool" ${endpoint})\": \"[CORE] SICX/USDC Pool\","
  echo "  \"$(getAddress "core/pools/sejong/bnusd-sicx/pool" ${endpoint})\": \"[CORE] BNUSD/SICX Pool\","
elif [ ${endpoint} == "berlin" ] ; then
  echo "  \"$(getAddress "core/pools/berlin/bnusd-usdc/pool" ${endpoint})\": \"[CORE] BNUSD/USDC Pool\","
  echo "  \"$(getAddress "core/pools/berlin/usdc-icx/pool" ${endpoint})\": \"[CORE] ICX/USDC Pool\","
  echo "  \"$(getAddress "core/pools/berlin/usdc-sicx/pool" ${endpoint})\": \"[CORE] SICX/USDC Pool\","
fi

echo "  \"$(getAddress "periphery/swaprouter" ${endpoint})\": \"[PERIPH] Swap Router\","
echo "  \"$(getAddress "periphery/positiondescriptor" ${endpoint})\": \"[PERIPH] Position Descriptor\","
echo "  \"$(getAddress "periphery/positionmgr" ${endpoint})\": \"[PERIPH] Position Manager\","
echo "  \"$(getAddress $(getReadOnlyPool) ${endpoint})\": \"[PERIPH] Pool ReadOnly\","
echo "  \"$(getAddress $(getQuoter) ${endpoint})\": \"[PERIPH] Quoter\","
echo "  \"$(getAddress $(getPoolInitializerPkg) ${endpoint})\": \"[PERIPH] Pool Initializer\","

if [ ${endpoint} == "custom" ]; then
  echo "  \"$(getAddress "core/pools/custom/sicx-usdc/token0" ${endpoint})\": \"[TOKENS] USDC\","
  echo "  \"$(getAddress "core/pools/custom/sicx-usdc/token1" ${endpoint})\": \"[TOKENS] SICX\","
elif [ ${endpoint} == "sejong" ] ; then
  echo "  \"$(getAddress "core/pools/sejong/bnusd-usdc/token0" ${endpoint})\": \"[TOKENS] BNUSD\","
  echo "  \"$(getAddress "core/pools/sejong/bnusd-usdc/token1" ${endpoint})\": \"[TOKENS] USDC\","
  echo "  \"$(getAddress "core/pools/sejong/usdc-sicx/token1"  ${endpoint})\": \"[TOKENS] SICX\","
elif [ ${endpoint} == "berlin" ] ; then
  echo "  \"$(getAddress "core/pools/berlin/bnusd-usdc/token1" ${endpoint})\": \"[TOKENS] BNUSD\","
  echo "  \"$(getAddress "core/pools/berlin/bnusd-usdc/token0" ${endpoint})\": \"[TOKENS] USDC\","
  echo "  \"$(getAddress "core/pools/berlin/usdc-sicx/token1"  ${endpoint})\": \"[TOKENS] SICX\","
fi


echo "}"