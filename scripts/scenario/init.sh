#!/bin/bash

endpoint="gochain"

# Deploy pool tokens
./run.py -e ${endpoint} deploy irc2-token 0.9.1 debug ./config/deploy/iusdc
./run.py -e ${endpoint} deploy irc2-token 0.9.1 debug ./config/deploy/sicx

# Deploy factory
./run.py -e ${endpoint} deploy Switchy-Factory 0.9.1 debug ./config/deploy/factory

# Deploy pool
# Write the tokens addresses to the pool params
iusdc_address=$(cat ./config/deploy/iusdc/${endpoint}/deploy.json | jq .scoreAddress -r)
sicx_address=$(cat ./config/deploy/sicx/${endpoint}/deploy.json | jq .scoreAddress -r)
echo '{}' | jq --arg token0 $iusdc_address --arg token1 $sicx_address '{_token0: $token0, _token1: $token1}' > ./config/deploy/iusdc-sicx-pool/${endpoint}/params.json
./run.py -e ${endpoint} deploy Switchy-Pool 0.9.1 debug ./config/deploy/iusdc-sicx-pool

# Deploy LiquidityManagement
factory_address=$(cat ./config/deploy/factory/${endpoint}/deploy.json | jq .scoreAddress -r)
echo '{}' | jq --arg _factory $factory_address --arg _sICX $sicx_address '{_factory: $_factory, _sICX: $_sICX}' > ./config/deploy/liquidity-management/${endpoint}/params.json
./run.py -e ${endpoint} deploy Switchy-LiquidityManagement 0.9.1 debug ./config/deploy/liquidity-management
