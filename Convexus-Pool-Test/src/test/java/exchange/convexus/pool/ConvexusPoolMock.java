package exchange.convexus.pool;

import java.math.BigInteger;

import score.Address;
import score.annotation.External;

public class ConvexusPoolMock extends ConvexusPool {

  public ConvexusPoolMock(Address _token0, Address _token1, Address _factory, int fee, int tickSpacing) {
    super(_token0, _token1, _factory, fee, tickSpacing);
  }
  
  @External
  public void setFeeGrowthGlobal0X128 (BigInteger _feeGrowthGlobal0X128) {
    this.feeGrowthGlobal0X128.set(_feeGrowthGlobal0X128);
  }
  
  @External
  public void setFeeGrowthGlobal1X128 (BigInteger _feeGrowthGlobal1X128) {
    this.feeGrowthGlobal1X128.set(_feeGrowthGlobal1X128);
  }
}
