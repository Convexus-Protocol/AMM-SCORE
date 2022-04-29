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
echo "Factory             : "$(getAddress "core/factory" ${endpoint})
echo "SICX/USDC Pool      : "$(getAddress "core/pools/custom-sicx-usdc/pool" ${endpoint})
echo "SICX Token          : "$(getAddress "core/pools/custom-sicx-usdc/token0" ${endpoint})
echo "USDC Token          : "$(getAddress "core/pools/custom-sicx-usdc/token1" ${endpoint})
echo "Swap Router         : "$(getAddress "periphery/swaprouter" ${endpoint})
echo "Position Descriptor : "$(getAddress "periphery/positiondescriptor" ${endpoint})
echo "Position Manager    : "$(getAddress "periphery/positionmgr" ${endpoint})
echo "Pool ReadOnly       : "$(getAddress $(getReadOnlyPool) ${endpoint})
echo "Quoter              : "$(getAddress $(getQuoter) ${endpoint})

echo "=========================================================="

echo "export const DEFAULT_BOOKMARK = {"
echo "  \"$(getAddress "core/factory" ${endpoint})\": \"Factory\","
echo "  \"$(getAddress "core/pools/custom-sicx-usdc/pool" ${endpoint})\": \"SICX/USDC Pool\","
echo "  \"$(getAddress "core/pools/custom-sicx-usdc/token0" ${endpoint})\": \"SICX Token\","
echo "  \"$(getAddress "core/pools/custom-sicx-usdc/token1" ${endpoint})\": \"USDC Token\","
echo "  \"$(getAddress "periphery/swaprouter" ${endpoint})\": \"Swap Router\","
echo "  \"$(getAddress "periphery/positiondescriptor" ${endpoint})\": \"Position Descriptor\","
echo "  \"$(getAddress "periphery/positionmgr" ${endpoint})\": \"Position Manager\","
echo "}"