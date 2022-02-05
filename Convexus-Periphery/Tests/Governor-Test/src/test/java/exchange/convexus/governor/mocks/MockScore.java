package exchange.convexus.governor.mocks;

import java.math.BigInteger;

import score.Context;
import score.annotation.External;
import score.annotation.Payable;

public class MockScore {

  public MockScore () {}
  
  @External(readonly = true)
  public BigInteger methodBigInteger (BigInteger value) {
    return BigInteger.ONE;
  }
  
  @External(readonly = true)
  public BigInteger methodBigIntegerArray (BigInteger[] value) {
    return BigInteger.ONE;
  }
  
  @External(readonly = true)
  public BigInteger methodStringArray (String[] value) {
    return BigInteger.ONE;
  }

  @External
  @Payable
  public void methodPayable (BigInteger v) {
    Context.require(v.equals(Context.getValue()));
  }

  @External
  public void methodStruct (MockStruct s) {
    
  }
}
