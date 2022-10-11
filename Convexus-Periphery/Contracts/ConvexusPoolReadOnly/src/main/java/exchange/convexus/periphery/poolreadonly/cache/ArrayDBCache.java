package exchange.convexus.periphery.poolreadonly.cache;

import score.Address;
import scorex.util.ArrayList;

public abstract class ArrayDBCache<V> {

  ArrayList<V> array = new ArrayList<>();
  public final Address target;

  public ArrayDBCache (Address target) {
    this.target = target;
  }

  public V get (int key) {
    if (array.get(key) == null) {
      this.array.set(key, this.getExternal(key));
    }

    return this.array.get(key);
  }

  abstract public V getExternal(int key);

  public void set (int key, V value) {
    this.array.set(key, value);
  }
}