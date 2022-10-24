import json

IUSDC = {
  "address": "cxf01b7f8f5250e037c5108bd2f3946b7bad934ac8",
  "amount": int(0.60 * 10**6)
}

ICX = {
  "address": "cx1111111111111111111111111111111111111111",
  "amount": 10**18
}

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
    "token0": IUSDC,
    "token1": ICX,

    # Type: Integer
    "fee": 3000
  }
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))