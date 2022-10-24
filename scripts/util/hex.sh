#!/bin/bash

set -e

hex () {
  integer=$1
  python -c "print(hex(${integer}))"
}

unhex () {
  hex=$1
  python -c "print(int('${hex}', 16))"
}
