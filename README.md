<p align="center">
  <img 
    src="https://i.imgur.com/4RsDm76.png" 
    width="550px"
    alt="Convexus logo">
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Convexus

## **Introduction**

Convexus introduces concentrated liquidity, giving individual liquidity providers (LPs) granular control over what price ranges their capital is allocated to. Individual positions are aggregated together into a single pool, forming one combined curve for users to trade against. Multiple fee tiers will be baked into the protocol allowing LPs to be appropriately compensated for taking on varying degrees of risk.

Convexus empowers developers, LPs and traders to participate in a financial marketplace that is open and accessible to all.

These features make Convexus a flexible and efficient automated market maker:

- LPs can provide liquidity with more capital efficiency, earning higher returns on their capital through more frequent trades and associated fees.
- This capital efficiency will pave the way for low-slippage trade execution that could surpass the liquidity of both centralized exchanges and stablecoin-focused automated market makers such as Curve.
- LPs can significantly increase their exposure to preferred assets and reduce their downside risk.
- LPs can also sell one asset for another by adding liquidity to a price range entirely above or below the market price, similar to a limit order on an exchange, while also earning the trade fees for that trade.

See our introduction [Medium](https://convexus.medium.com/convexus-cbf2db4ce9e7) article for more information.

## **Core Layer Documentation**

- [Convexus Pool](/Convexus-Core/Contracts/Pool/docs/README.md)

## **Periphery Layer Documentation**

- [Convexus SwapRouter](/Convexus-Periphery/Contracts/SwapRouter/docs/README.md)

## **Commons Documentation**

- [Convexus Librairies](/Convexus-Commons/Librairies/docs/README.md)

## **Frequently Asked Questions**

<details>
<summary>❓ I want to swap a token against another</summary>

Please refer to the [`SwapRouter`](/Convexus-Periphery/Contracts/SwapRouter/docs/README.md) documentation. In most cases, you will need to call the [`exactInputSingle`](/Convexus-Periphery/Contracts/SwapRouter/docs/README.md#swaprouterexactinputsingle) method.
</details>


<details>
<summary>❓ How to get the price of a pool ?</summary>

Please refer to the [`Slot0`](/Convexus-Core/Contracts/Pool/docs/README.md#convexuspoolslot0) method and the structure definition. The `sqrtPriceX96` field contains the [Q64.96 price](/Convexus-Commons/Librairies/docs/README.md#how-to-encode-a-q6496-price) of the pool. You can decode it to a human readable price like [this](/Convexus-Commons/Librairies/docs/README.md#how-to-decode-a-q6496-to-a-floating-point-price).
</details>



<details>
<summary>❓ I want to provide liquidity to a pool</summary>

Please refer to the [`NonFungiblePositionManager`](/Convexus-Periphery/Contracts/NonFungiblePositionManager/docs/README.md) documentation. You will need first to deposit some funds using [`deposit`](/Convexus-Periphery/Contracts/NonFungiblePositionManager/docs/README.md#nonfungiblepositionmanagerdeposit), then create a new position wrapped in a NFT using [`mint`](/Convexus-Periphery/Contracts/NonFungiblePositionManager/docs/README.md#nonfungiblepositionmanagermint). The NFT represents the liquidity you provided to the pool.
</details>

