package exchange.convexus.periphery.initializer;

import exchange.convexus.positionmgr.MintResult;
import score.Address;

public class CreatePoolAndMintResult {

  public Address pool;
  public MintResult mintResult;

  public CreatePoolAndMintResult() {}

  public CreatePoolAndMintResult(Address pool, MintResult mintResult) {
    this.pool = pool;
    this.mintResult = mintResult;
  }
}
