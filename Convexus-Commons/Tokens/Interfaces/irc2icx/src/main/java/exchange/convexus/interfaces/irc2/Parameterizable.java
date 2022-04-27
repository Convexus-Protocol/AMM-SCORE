package exchange.convexus.interfaces.irc2;

import com.eclipsesource.json.JsonObject;

public interface Parameterizable {
  public JsonObject toJson ();
  public Object[] toRaw ();
}
