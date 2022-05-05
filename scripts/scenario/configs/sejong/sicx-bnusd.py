import json

BNUSD = "cx5838cb516d6156a060f90e9a3de92381331ff024"
SICX  = "cx70806fdfa274fe12ab61f1f98c5a7a1409a0c108"

config = {
  # Type: String
  # "network" value must be:
  #   - "lisbon" for Lisbon Network,
  #   - "berlin" for Berlin Network,
  #   - "custom" for the custom Convexus Network
  #   - "mainnet" for MainNet Network,
  "network": "sejong",

  "pool": {
    # Type: Address
    # token0 and token1 addresses
    "token0": BNUSD,
    "token1": SICX,

    # Type: Integer
    "fee": 3000
  }
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))