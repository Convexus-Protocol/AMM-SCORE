package exchange.convexus.callee;

import java.math.BigInteger;

import score.Address;
import score.ByteArrayObjectWriter;
import score.ObjectReader;

public class FlashData {

  Address sender;
  BigInteger pay0;
  BigInteger pay1;

  public FlashData (
    Address sender, 
    BigInteger pay0, 
    BigInteger pay1
  ) {
    this.sender = sender;
    this.pay0 = pay0;
    this.pay1 = pay1;
  }

  public static void writeObject(ByteArrayObjectWriter writer, FlashData flashData) {
    writer.write(flashData.sender);
    writer.write(flashData.pay0);
    writer.write(flashData.pay1);
  }

  public static FlashData readObject(ObjectReader reader) {
    return new FlashData(reader.readAddress(), reader.readBigInteger(), reader.readBigInteger());
  }
}
