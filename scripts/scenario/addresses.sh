#!/bin/bash

. ./scripts/util/get_address.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 1
fi

endpoint=$1

echo " -- [Network: ${endpoint}] -- "

echo "Custom Bond 1 bnUSD -> sICX : "$(getAddress "bond-custombond" ${endpoint})
echo "Custom Treasury 1 bnUSD -> sICX : "$(getAddress "bond-customtreasury" ${endpoint})
echo "Custom Bond 2 LP(BALN/bnUSD) -> BALN : "$(getAddress "bond-2-balanced-custombond" ${endpoint})
echo "Custom Treasury 2 LP(BALN/bnUSD) -> BALN : "$(getAddress "bond-2-balanced-customtreasury" ${endpoint})
echo "Factory : "$(getAddress "bond-factory" ${endpoint})
echo "Factory Storage : "$(getAddress "bond-factorystorage" ${endpoint})
echo "Subsidy Router : "$(getAddress "bond-subsidyrouter" ${endpoint})
echo "Bond 1 PayoutToken : "$(getAddress "bond-payouttoken" ${endpoint})
echo "Bond 1 PrincipalToken : "$(getAddress "bond-principaltoken" ${endpoint})
echo "Bond 2 PayoutToken : "$(getAddress "bond-2-balanced-payouttoken" ${endpoint})
echo "Bond 2 PrincipalToken : "$(getAddress "bond-2-balanced-principaltoken" ${endpoint})

echo "=========================================================="

echo "export const DEFAULT_BOOKMARK = {"
echo "  \"$(getAddress "bond-custombond" ${endpoint})\": \"Custom Bond 1 bnUSD -> sICX\","
echo "  \"$(getAddress "bond-customtreasury" ${endpoint})\": \"Custom Treasury 1 bnUSD -> sICX\","
echo "  \"$(getAddress "bond-2-balanced-custombond" ${endpoint})\": \"Custom Bond 2 LP(BALN/bnUSD) -> BALN\","
echo "  \"$(getAddress "bond-2-balanced-customtreasury" ${endpoint})\": \"Custom Treasury 2 LP(BALN/bnUSD) -> BALN\","
echo "  \"$(getAddress "bond-factory" ${endpoint})\": \"Factory\","
echo "  \"$(getAddress "bond-factorystorage" ${endpoint})\": \"Factory Storage\","
echo "  \"$(getAddress "bond-subsidyrouter" ${endpoint})\": \"Subsidy Router\","
echo "  \"$(getAddress "bond-payouttoken" ${endpoint})\": \"Bond 1 PayoutToken\","
echo "  \"$(getAddress "bond-principaltoken" ${endpoint})\": \"Bond 1 PrincipalToken\","
echo "  \"$(getAddress "bond-2-balanced-payouttoken" ${endpoint})\": \"Bond 2 PayoutToken\","
echo "  \"$(getAddress "bond-2-balanced-principaltoken" ${endpoint})\": \"Bond 2 PrincipalToken\","
echo "}"