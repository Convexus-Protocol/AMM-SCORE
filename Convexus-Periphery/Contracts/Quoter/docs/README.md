# Convexus Quoter Contract Documentation

The `Quoter` is a contract that works similarly to the `SwapRouter` contract. It exposes readonly methods which allows getting the expected amount out or amount in for a given swap without executing the swap.

# **Tokens Swap**

The `Quoter` is able to give a quote for swapping two tokens thanks to the [SwapRouter `exactInput*/exactOuput* methods`](/Convexus-Periphery/Contracts/SwapRouter/docs/README.md#tokens-swap). It supports multiple ways of quotations, depending of the user needs:

- [`quoteExactInputSingle`](#quoterquoteexactinputsingle): Returns the amount out received for a given exact input but for a swap of a single pool
- [`quoteExactInput`](#quoterquoteexactinput): Returns the amount out received for a given exact input swap without executing the swap
- [`quoteExactOutputSingle`](#quoterquoteexactoutputsingle): Returns the amount in required to receive the given exact output amount but for a swap of a single pool
- [`quoteExactOutput`](#quoterquoteexactoutput): Returns the amount in required for a given exact output swap without executing the swap

-------------------------------------------------------------------------------------

⚠️ The Quoter implementation cannot be used as readonly for now, as the goloop engine doesn't support calling write methods as readonly. See [this GitHub issue](https://github.com/icon-project/goloop/issues/71) for more information.