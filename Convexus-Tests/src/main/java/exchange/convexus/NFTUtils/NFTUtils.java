package exchange.convexus.NFTUtils;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.mockito.ArgumentCaptor;

import exchange.convexus.positionmgr.DecreaseLiquidityParams;
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

  public static BigInteger mintPosition (
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
    reset(nft.spy);
    NFTUtils.mint(
      nft, 
      from,
      token0,
      token1,
      fee,
      tickLower,
      tickUpper,
      amount0Desired,
      amount1Desired,
      amount0Min,
      amount1Min,
      recipient,
      deadline
    );

    // Get IncreaseLiquidity event
    ArgumentCaptor<BigInteger> _tokenId = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _liquidity = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(nft.spy).IncreaseLiquidity(_tokenId.capture(), _liquidity.capture(), _amount0.capture(), _amount1.capture());
    return _tokenId.getValue();
  }
  
  public static void decreaseLiquidity (
    ScoreSpy<NonFungiblePositionManager> nft,
    Account from,
    BigInteger tokenId,
    BigInteger liquidity,
    BigInteger amount0Min,
    BigInteger amount1Min,
    BigInteger deadline
  ) {
    nft.invoke(from, "decreaseLiquidity", new DecreaseLiquidityParams(
      tokenId,
      liquidity,
      amount0Min,
      amount1Min,
      deadline
    ));
  }

  public static void safeTransferFrom(ScoreSpy<NonFungiblePositionManager> nft, Account lp, Address staker, BigInteger tokenId) {
    nft.invoke(lp, "safeTransferFrom", lp.getAddress(), staker, tokenId, "".getBytes());
  }
}
