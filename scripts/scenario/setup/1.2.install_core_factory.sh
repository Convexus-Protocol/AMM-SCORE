#!/bin/bash

set -e


source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh

source ./scripts/convexus/pkg.sh

if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
deployName="Convexus Core Pool Factory"
info "Setting up ${deployName}..."

# Package information
pkg=$(getFactoryPkg)
javaPkg=":Convexus-Core:Contracts:Factory"
build="optimized"

# Setup packages
setupJavaDir ${pkg} ${javaPkg} ${build}
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})
callsDir=$(getCallsDir ${pkg} ${network})

# Start
info "Compiling the Pool contract..."
poolJavaPkg=":Convexus-Core:Contracts:Pool"
./gradlew "${poolJavaPkg}:clean" > /dev/null
./gradlew "${poolJavaPkg}:build" > /dev/null
./gradlew "${poolJavaPkg}:optimizedJar" > /dev/null

# Set the Pool contracts bytes
actionName="setPoolContract"
poolJarPath="./Convexus-Core/Contracts/Pool/build/libs/Pool-optimized.jar"

if [ ! -f "$poolJarPath" ]; then
  error "The Pool contract JAR cannot be found"
  exit 1
fi

tmpHexJarPath=$(mktemp /tmp/pool.XXXXXX.jar)
echo "\"0x$(xxd -p ${poolJarPath})\"" | tr -d '\n' > ${tmpHexJarPath}

filter=$(cat <<EOF
{
  method: "setPoolContract",
  params: {
    contractBytes: \$contractBytes
  }
}
EOF
)

echo '{}' | jq \
  --argfile contractBytes ${tmpHexJarPath} \
  "${filter}" > ${callsDir}/${actionName}.json

python run.py -e ${network} invoke ${pkg} ${actionName}

success "${deployName} contract has been successfully setup!"
