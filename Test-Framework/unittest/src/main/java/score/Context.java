/*
 * Copyright 2020 ICONLOOP Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package score;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import loader.ImmortalDappModule;
import score.impl.AnyDBImpl;
import score.impl.Crypto;
import score.impl.RLPObjectReader;
import score.impl.RLPObjectWriter;
import score.struct.Property;

import java.io.IOException;
import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ByteClassLoader extends ClassLoader {
    private final byte[] jarBytes;
    public final ImmortalDappModule dapp;

    public ByteClassLoader(ClassLoader parent, byte[] jarBytes) throws IOException {
        super(parent);
        this.jarBytes = jarBytes;
        this.dapp = ImmortalDappModule.readFromJar(this.jarBytes);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (var item : this.dapp.classes.entrySet()) {
            String className = item.getKey();
            byte[] classBytes = item.getValue();
            if (name.equals(className)) {
                return defineClass(className, classBytes, 0, classBytes.length, null);
            }
        }

        throw new ClassNotFoundException(String.format("Cannot find %s class", name));
    }
}

public final class Context extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final StackWalker stackWalker =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private Context() {
    }

    public static byte[] getTransactionHash() {
        return null;
    }

    public static int getTransactionIndex() {
        return 0;
    }

    public static long getTransactionTimestamp() {
        return 0L;
    }

    public static BigInteger getTransactionNonce() {
        return BigInteger.ZERO;
    }

    public static Address getAddress() {
        return sm.getAddress();
    }

    public static Address getCaller() {
        return sm.getCaller();
    }

    public static Address getOrigin() {
        return sm.getOrigin();
    }

    public static Address getOwner() {
        return sm.getOwner();
    }

    public static BigInteger getValue() {
        return sm.getCurrentFrame().getValue();
    }

    public static long getBlockTimestamp() {
        return sm.getBlock().getTimestamp();
    }

    public static long getBlockHeight() {
        return sm.getBlock().getHeight();
    }

    public static BigInteger getBalance(Address address) throws IllegalArgumentException {
        return Account.getAccount(address).getBalance();
    }

    public static<T> T call(Class<T> cls, BigInteger value,
                            Address targetAddress, String method, Object... params) {
        return null;
    }
    
    public static Class<?> walkScore(Stream<StackFrame> stackFrameStream) {
        // skip(1) because it's always "Context.call"
        for (var stackFrame : stackFrameStream.skip(1).collect(Collectors.toList())) {
            try {
                sm.getScoreFromClass(stackFrame.getDeclaringClass());
                return stackFrame.getDeclaringClass();
            } catch (IllegalStateException e) {
                // pass
            }
        }

        return null;
    }

    public static Object call(BigInteger value, Address targetAddress, String method, Object... params) {
        var caller = stackWalker.walk(Context::walkScore);
        return sm.call(caller, value, targetAddress, method, params);
    }

    private static Object _shadow(Object obj, Class<?> c) {
        if (obj == null) {
            return null;
        } else if (c == boolean.class) {
            return obj;
        } else if (c == char.class) {
            return (char) ((BigInteger) obj).intValue();
        } else if (c == byte.class) {
            return ((BigInteger) obj).byteValueExact();
        } else if (c == short.class) {
            return ((BigInteger) obj).shortValueExact();
        } else if (c == int.class) {
            return ((BigInteger) obj).intValueExact();
        } else if (c == long.class) {
            return ((BigInteger) obj).longValueExact();
        } else if (c == java.lang.Boolean.class) {
            return java.lang.Boolean.valueOf((Boolean) obj);
        } else if (c == java.lang.Character.class) {
            return java.lang.Character.valueOf((char)((BigInteger)obj).intValue());
        } else if (c == java.lang.Byte.class) {
            return java.lang.Byte.valueOf(((BigInteger)obj).byteValueExact());
        } else if (c == java.lang.Short.class) {
            return java.lang.Short.valueOf(((BigInteger)obj).shortValueExact());
        } else if (c == java.lang.Integer.class) {
            return java.lang.Integer.valueOf(((BigInteger)obj).intValueExact());
        } else if (c == java.lang.Long.class) {
            return java.lang.Long.valueOf(((BigInteger)obj).longValueExact());
        } else if (c == java.math.BigInteger.class) {
            return new java.math.BigInteger(((BigInteger)obj).toString());
        } else if (c == java.lang.String.class) {
            return new java.lang.String((String)obj);
        } else if (c == score.Address.class) {
            return new score.Address(((Address)obj).toByteArray());
        }
        // Is it an Array ?
        else if (c.isArray()) {
            int arrayLength = Array.getLength(c);
            if (arrayLength == 0) {
                return new Object[] {};
            }

            var firstItem = _shadow(Array.get(c, 0), Array.get(c, 0).getClass());
            Object[] returnArray = (Object[]) Array.newInstance(firstItem.getClass(), arrayLength);

            for (int i = 0; i < Array.getLength(c); i++) {
                // Decide what to do with each array item, recursive call it
                returnArray[i] = _shadow(Array.get(c, i), Array.get(c, i).getClass());
            }
            return returnArray;
        } else if (c == java.util.List.class) {
            @SuppressWarnings("unchecked")
            java.util.List<Object> list = (java.util.List<Object>) obj;

            java.util.List<Object> result = new java.util.ArrayList<>();
            
            int len = list.size();
            if (len == 0) {
                return list;
            }
            for (int i = 0; i < len; i++) {
                // Decide what to do with each array item, recursive call it
                result.add(_shadow(Array.get(c, i), Array.get(c, i).getClass()));
            }
            return result;

        } else if (c == java.util.Map.class) {
            @SuppressWarnings("unchecked")
            var o = (Map<String, Object>) obj;
            for (var e : o.entrySet()) {
                o.put(e.getKey(), _shadow(e.getValue(), e.getValue().getClass()));
            }
            return o;
        } else {
            // this must be a writable struct
            try {
                @SuppressWarnings("unchecked")
                var o = (Map<String, Object>) obj;
                var ctor = c.getConstructor();
                var res = ctor.newInstance();
                for (var e : o.entrySet()) {
                    var wp = Property.getWritableProperty(c, e.getKey());
                    if (wp == null) {
                        throw new IllegalArgumentException();
                    }
                    wp.set(res, _shadow(e.getValue(), wp.getType()));
                }
                return res;
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | InstantiationException
                    | InvocationTargetException e) {
                throw new IllegalArgumentException();
            }
        }
    }

    public static<T> T call(Class<T> cls, Address targetAddress, String method, Object... params) {
        var result = call(targetAddress, method, params);
        return cls.cast(_shadow(result, cls));
    }

    public static Object call(Address targetAddress, String method, Object... params) {
        return call(BigInteger.ZERO, targetAddress, method, params);
    }

    public static void transfer(Address targetAddress, BigInteger value) {
        call(value, targetAddress, "fallback");
    }

    public static Address deploy(byte[] content, Object... params) {
        var caller = stackWalker.walk(Context::walkScore);
        try {
            ByteClassLoader classLoader = new ByteClassLoader(ByteClassLoader.class.getClassLoader(), content);
            Class<?> dappClass = classLoader.findClass(classLoader.dapp.mainClass);
            return sm.deploy(sm.getScoreFromClass(caller).getAccount(), dappClass, params).getAddress();
        } catch (Exception e) {
            Context.revert("Failure during SCORE deployment");
            return null;
        }
    }

    public static Address deploy(Address targetAddress, byte[] content, Object... params) {
        return null;
    }

    public static void revert(int code, String message) {
        throw new AssertionError(String.format("Reverted(%d): %s", code, message));
    }

    public static void revert(int code) {
        throw new AssertionError(String.format("Reverted(%d)", code));
    }

    public static void revert(String message) {
        revert(0, message);
    }

    public static void revert() {
        revert(0);
    }

    public static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void println(String message) {
        System.out.println(message);
    }

    public static byte[] hash(String alg, byte[] msg) {
        require(null != alg, "Algorithm can't be NULL");
        require(null != msg, "Message can't be NULL");
        return Crypto.hash(alg, msg);
    }

    public static boolean verifySignature(String alg, byte[] msg, byte[] sig, byte[] pubKey) {
        require(null != alg, "Algorithm can't be NULL");
        require(null != msg, "Message can't be NULL");
        require(null != sig, "Signature can't be NULL");
        require(null != pubKey, "Public key can't be NULL");
        return Crypto.verifySignature(alg, msg, sig, pubKey);
    }

    public static byte[] recoverKey(String alg, byte[] msg, byte[] sig, boolean compressed) {
        require(null != msg && null != sig);
        require(msg.length == 32, "the length of msg must be 32");
        require(sig.length == 65, "the length of sig must be 65");
        return Crypto.recoverKey(alg, msg, sig, compressed);
    }

    public static Address getAddressFromKey(byte[] pubKey) {
        require(null != pubKey, "pubKey can't be NULL");
        return new Address(Crypto.getAddressBytesFromKey(pubKey));
    }

    public static int getFeeSharingProportion() {
        return 0;
    }

    public static void setFeeSharingProportion(int proportion) {
    }

    @SuppressWarnings("unchecked")
    public static<K, V> BranchDB<K, V> newBranchDB(String id, Class<?> leafValueClass) {
        return new AnyDBImpl(id, leafValueClass);
    }

    @SuppressWarnings("unchecked")
    public static<K, V> DictDB<K, V> newDictDB(String id, Class<V> valueClass) {
        return new AnyDBImpl(id, valueClass);
    }

    @SuppressWarnings("unchecked")
    public static<E> ArrayDB<E> newArrayDB(String id, Class<E> valueClass) {
        return new AnyDBImpl(id, valueClass);
    }

    @SuppressWarnings("unchecked")
    public static<E> VarDB<E> newVarDB(String id, Class<E> valueClass) {
        return new AnyDBImpl(id, valueClass);
    }

    public static void logEvent(Object[] indexed, Object[] data) {
    }

    public static ObjectReader newByteArrayObjectReader(String codec, byte[] byteArray) {
        if ("RLPn".equals(codec)) {
            return new RLPObjectReader(byteArray);
        }
        throw new IllegalArgumentException("Unknown codec");
    }

    public static ByteArrayObjectWriter newByteArrayObjectWriter(String codec) {
        if ("RLPn".equals(codec)) {
            return new RLPObjectWriter();
        }
        throw new IllegalArgumentException("Unknown codec");
    }
}
