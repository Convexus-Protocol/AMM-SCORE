package exchange.convexus.periphery.poolreadonly.cache;

import score.Address;

public abstract class VarDBCache<T> {

  T value = null;
  public final Address target;

  public VarDBCache (Address target) {
    this.target = target;
  }

  public T get () {
    if (value == null) {
      this.value = this.getExternal();
    }

    return this.value;
  }

  abstract public T getExternal();

  public void set (T value) {
    this.value = value;
  }
}