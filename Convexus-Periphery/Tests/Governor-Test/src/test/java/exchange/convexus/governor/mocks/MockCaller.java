package exchange.convexus.governor.mocks;

import java.math.BigInteger;

import exchange.convexus.structs.MethodCall;
import exchange.convexus.structs.MethodParam;
import score.Address;
import score.annotation.External;

public class MockCaller {

  public MockCaller () {}
  
  @External(readonly = true)
  public Object call (Address target, String method, MethodParam[] params) {
    MethodCall methodcall = new MethodCall(target, BigInteger.ZERO, method, params);
    return methodcall.call();
  }
  
  @External
  public void invoke (Address target, BigInteger value, String method, MethodParam[] params) {
    MethodCall methodcall = new MethodCall(target, value, method, params);
    methodcall.call();
  }
}
