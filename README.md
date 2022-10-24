<p align="center">
  <img 
    src="https://i.imgur.com/qqIPMGE.png" 
    alt="Convexus logo">
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# 📖 **Introduction**

Convexus introduces concentrated liquidity, giving individual liquidity providers (LPs) granular control over what price ranges their capital is allocated to. Individual positions are aggregated together into a single pool, forming one combined curve for users to trade against. Multiple fee tiers will be baked into the protocol allowing LPs to be appropriately compensated for taking on varying degrees of risk.

Convexus empowers developers, LPs and traders to participate in a financial marketplace that is open and accessible to all.

These features make Convexus a flexible and efficient automated market maker:

- LPs can provide liquidity with more capital efficiency, earning higher returns on their capital through more frequent trades and associated fees.
- This capital efficiency will pave the way for low-slippage trade execution that could surpass the liquidity of both centralized exchanges and stablecoin-focused automated market makers such as Curve.
- LPs can significantly increase their exposure to preferred assets and reduce their downside risk.
- LPs can also sell one asset for another by adding liquidity to a price range entirely above or below the market price, similar to a limit order on an exchange, while also earning the trade fees for that trade.

See our introduction [Medium](https://convexus.medium.com/convexus-cbf2db4ce9e7) article for more information, and the [technical documentation](https://dev.convexus.net) if you need to build on Convexus.

# 🏗️ Build

## Prerequisite

You need to install JDK 11 or later version. Visit [OpenJDK.net](http://openjdk.java.net/) for prebuilt binaries.
Or you can install a proper OpenJDK package from your OS vendors.
You will also need `jq`.

Please find below the recommanded commands to run to install these packages:

- In macOS:

```bash
$ # Install brew
$ /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
$ # Install JDK
$ brew tap AdoptOpenJDK/openjdk
$ brew cask install adoptopenjdk11
$ # Install JQ
$ brew install jq
```

- In Windows (Windows 10 or later)

You can install the JDK11 natively, but you will need `bash` in order to run the Convexus deploy scripts.
Please [install WSL](https://docs.microsoft.com/en-us/windows/wsl/install-manual) first, so you can follow the same instructions than Linux.

- In Linux (Ubuntu 18.04):

```bash
$ # Install JDK
$ sudo apt install openjdk-11-jdk
$ # Install JQ
$ sudo apt install jq
```

## Testing the setup

```java
$ ./gradlew build
```

This should run all the unittest, and run successfully with a similar output than below:

```java
Starting a Gradle Daemon (subsequent builds will be faster)

$ ./gradlew build

[...]

BUILD SUCCESSFUL in 2m 4s
162 actionable tasks: 155 executed, 7 up-to-date
```

## Setup the Convexus build system

You'll need `python3` and `pip` installed on your machine beforehand.

Now we can install the `virtualenv` and the dependencies:

```bash
$ # Run this in the root folder of the project
$ python -m venv ./venv
$ source ./venv/bin/activate
$ pip install -r ./requirements.txt
```

Everytime you want to use the Convexus build & deploy system, please do `source ./venv/bin/activate` beforehand.

## Deploy the Convexus Core Factory contracts

```bash
$ # Run this in the root folder of the project
$ # We specify "berlin" as an argument here, which means the contracts
$ # will be deployed on the Berlin Network. You can set it to "custom" too for the
$ # custom Convexus network
$ ./scripts/scenario/1.deploy_core_factory.sh berlin
[...]
# This should end with the following message
[🎉] Convexus Core Pool Factory contract has been successfully setup!
```

## Deploy a Convexus Pool contract

For deploying pools, we will need to create a new configuration file, that will contain basic information about the newly deployed pool.

These configuration files are located in [`./scripts/scenario/configs/`](./scripts/scenario/configs/).

They will be named like that : `${bondId}.py`. A `bondId` is an arbitrary name that will represents the bond being deployed. This name must be unique for all bonds accross all networks. You can find an example of a config file [here](scripts/scenario/configs/berlin/bnusd-usdc.py). 

That file contain a `config` dict variable that you can modify depending of your needs.

There are two important variables in that dict: 
  - "network", that will be the network where the bond is deployed. It can be "custom" too.
  - "pool", that will contain the settings of the newly deployed pool

Once you've changed these values, you can deploy the pool by doing so:

```bash
$ # Run this in the root folder of the project
$ # We specify "custom-sicx-usdc" as an argument here, because we've named our config
$ # file "custom-sicx-usdc.py", as we want a Bond ID = custom-sicx-usdc
$ ./scripts/scenario/2.deploy_core_pool.sh berlin/bnusd-usdc
[...]
# This should end with the following message
[🎉] Pool successfully initialized!
```

## Deploy the Convexus Periphery contracts

The process for deploying a custom bond contract is straightforward. 

```bash
$ # We only need the network name
$ ./scripts/scenario/3.deploy_periphery.sh berlin
[...]
# This should end with the following message
[🎉] All Periphery Contracts have been deployed successfully!
```
