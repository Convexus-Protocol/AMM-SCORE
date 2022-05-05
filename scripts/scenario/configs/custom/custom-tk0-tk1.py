import json

token0 = "cxd4657c829519242113495d75636e051e5c06bd7c"
token1 = "cx0645da6aace1edb81ca17bdf56735ca9d2a12294"

config = {
  # Type: String
  # "network" value must be:
  #   - "lisbon" for Lisbon Network,
  #   - "berlin" for Berlin Network,
  #   - "custom" for the custom Convexus Network
  #   - "mainnet" for MainNet Network,
  "network": "custom",

  "pool": {
    # Type: Address
    # token0 and token1 addresses
    "token0": token0,
    "token1": token1,

    # Type: Integer
    "fee": 3000
  }
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))