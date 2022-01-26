#!/bin/bash

. ./scripts/util/get_address.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 0
fi

endpoint=$1

echo " -- [Network: ${endpoint}] -- "

echo "USDC : "$(getAddress "iusdc" ${endpoint})
echo "SICX : "$(getAddress "sicx" ${endpoint})
echo "Factory : "$(getAddress "factory" ${endpoint})
echo "Pool (USDC/SICX) : "$(getAddress "iusdc-sicx-pool" ${endpoint})
echo "SwapRouter : "$(getAddress "swaprouter" ${endpoint})
echo "NFTPositionDescriptor : "$(getAddress "nftpositiondescriptor" ${endpoint})
echo "NFTPositionManager : "$(getAddress "nftpositionmanager" ${endpoint})

echo "=========================================================="

echo "export const DEFAULT_BOOKMARK = {"
echo "  \"$(getAddress "iusdc" ${endpoint})\": \"USDC\","
echo "  \"$(getAddress "sicx" ${endpoint})\": \"SICX\","
echo "  \"$(getAddress "factory" ${endpoint})\": \"Factory\","
echo "  \"$(getAddress "iusdc-sicx-pool" ${endpoint})\": \"Pool (USDC/SICX)\","
echo "  \"$(getAddress "swaprouter" ${endpoint})\": \"SwapRouter\","
echo "  \"$(getAddress "nftpositiondescriptor" ${endpoint})\": \"NFTPositionDescriptor\","
echo "  \"$(getAddress "nftpositionmanager" ${endpoint})\": \"NFTPositionManager\","
echo "}"