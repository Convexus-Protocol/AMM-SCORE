#!/bin/bash

. ./scripts/util/get_address.sh

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 1
fi

endpoint=$1

echo " -- [Network: ${endpoint}] -- "


echo "=========================================================="

echo "export const DEFAULT_BOOKMARK = {"
echo "}"