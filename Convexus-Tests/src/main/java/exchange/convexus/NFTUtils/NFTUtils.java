package exchange.convexus.NFTUtils;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import exchange.convexus.positionmgr.MintParams;
import exchange.convexus.positionmgr.NonFungiblePositionManager;
import exchange.convexus.utils.ScoreSpy;
import score.Address;

public class NFTUtils {
  
  public static void mint (
    ScoreSpy<NonFungiblePositionManager> nft,
    Account from,
    Address token0,
    Address token1,
    int fee,
    int tickLower,
    int tickUpper,
    BigInteger amount0Desired,
    BigInteger amount1Desired,
    BigInteger amount0Min,
    BigInteger amount1Min,
    Address recipient,
    BigInteger deadline
  ) {
    MintParams params = new MintParams();
    params.token0 = token0;
    params.token1 = token1;
    params.fee = fee;
    params.tickLower = tickLower;
    params.tickUpper = tickUpper;
    params.amount0Desired = amount0Desired;
    params.amount1Desired = amount1Desired;
    params.amount0Min = amount0Min;
    params.amount1Min = amount1Min;
    params.recipient = recipient;
    params.deadline = deadline;
    nft.invoke(from, "mint", params);
  }

}
