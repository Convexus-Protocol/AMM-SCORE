package exchange.convexus.staker;

import java.math.BigInteger;

class RewardAmount {
  public BigInteger reward;
  public BigInteger secondsInsideX128;

  public RewardAmount (
    BigInteger reward, 
    BigInteger secondsInsideX128
  ) {
    this.reward = reward;
    this.secondsInsideX128 = secondsInsideX128;
  }
}