package exchange.convexus.periphery.poolreadonly.cache;

import exchange.convexus.utils.StringUtils;
import score.Address;
import scorex.util.HashMap;

public abstract class DictDBCache<K, V> {

  HashMap<String, V> map = new HashMap<>();
  public final Address target;

  public String serializeKey (K key) {
    if (key.getClass() == byte[].class) {
      return StringUtils.byteArrayToHex((byte[]) key);
    }

    return key.toString();
  }

  public DictDBCache (Address target) {
    this.target = target;
  }

  public V get (K _key) {
    String key = serializeKey(_key);
    if (map.get(key) == null) {
      this.map.put(key, this.getExternal(_key));
    }

    return this.map.get(key);
  }

  public V getOrDefault (K key, V defaultValue) {
    V result = this.get(key);
    return result != null ? result : defaultValue;
  }

  abstract public V getExternal(K key);

  public void set (K key, V value) {
    this.map.put(serializeKey(key), value);
  }
}