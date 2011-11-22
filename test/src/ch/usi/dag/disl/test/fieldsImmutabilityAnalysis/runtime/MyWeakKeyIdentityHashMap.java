package ch.usi.dag.disl.test.fieldsImmutabilityAnalysis.runtime;

/**
 * Identity-based hashmap that internally uses {@code MyWeakReference} to
 * store {@code keys}.
 * Key values can be made finalizable, finalized, and then reclaimed.
 * 
 * If the size of the map (the number of key-value mappings) sufficiently
 * exceeds the expected maximum size, the number of buckets is increased.
 * Upon resizing, cleared references to keys are removed from the map.
 * It is possible to register a dumper to be executed upon removal.
 * The same dumper can be used to dump all entries in the map. 
 * 
 * The implementation is NOT thread-safe.
 */
public final class MyWeakKeyIdentityHashMap<K,V> {
    private static final int DEFAULT_INITIAL_CAPACITY = 4096;
    private static final int INCREMENT_FACTOR = 2;
    private transient Object[] table;
    private int size = 0;
    private transient int threshold;

    private int currentSize;

    private EntryDumper<MyWeakReference<K>, V> entryDumper = null;

    /**
     * Constructs a new, empty identity hash map with the default initial
     * capacity and no {@code RemovalCallback}.
     */
    public MyWeakKeyIdentityHashMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity and
     * no dumper.
     *
     * @param initialCapacity the initial capacity of the map
     * @throws IllegalArgumentException if <tt>initCapacity</tt> is not positive
     */
    public MyWeakKeyIdentityHashMap(int initialCapacity) {
        this(initialCapacity, null);
    }

    /**
     * Constructs a new, empty map with the default initial capacity and
     * the specified dumper.
     *
     * @param entryDumper the dumper to be executed upon removal of cleared references
     * @throws IllegalArgumentException if <tt>initCapacity</tt> is not positive
     */
    public MyWeakKeyIdentityHashMap(EntryDumper<MyWeakReference<K>, V> entryDumper) {
        this(DEFAULT_INITIAL_CAPACITY, entryDumper);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity and
     * dumper.
     *
     * @param initialCapacity the initial capacity of the map
     * @param entryDumper the dumper to be executed upon removal of cleared references
     * @throws IllegalArgumentException if <tt>initCapacity</tt> is not positive
     */
    public MyWeakKeyIdentityHashMap(int initialCapacity, EntryDumper<MyWeakReference<K>, V> entryDumper) {
        if(initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity is not positive: " + initialCapacity);
        }
        currentSize = initialCapacity;
        threshold = (currentSize * 2) / 3;
        table = new Object[currentSize * 2];

        this.entryDumper = entryDumper;
    }

    private int hash(Object obj, int size) {
        return (System.identityHashCode(obj) % size) * 2;
    }

    private static int nextKeyIndex(int prevIndex, int size) {
        return (prevIndex + 2 < (size * 2) ? prevIndex + 2 : 0);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key == k)},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.) 
     * 
     * This map stores a {@code MyWeakReference} to {@code key}.
     * 
     * The implementation is NOT thread safe.
     * 
     * @return A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        Object[] tab = table;
        int index = hash(key, currentSize);
        while (true) {
            MyWeakReference<Object> item = (MyWeakReference<Object>)tab[index];
            if (item == null)
                return null;
            if (item.get() == key)
                return (V)tab[index + 1];
            index = nextKeyIndex(index, currentSize);
        }
    }

    /**
     * Associates the specified value with the specified key in this identity
     * hash map.  If the map previously contained a mapping for the key, the
     * old value is replaced and returned.
     * 
     * This map automatically stores a {@code MyWeakReference} to {@code key}.
     * 
     * The implementation is NOT thread safe.
     * 
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    @SuppressWarnings("unchecked")
    public V put(K key, V value, String objectID) {
        Object[] tab = table;
        int index = hash(key, currentSize);

        MyWeakReference<K> item;
        while ((item = (MyWeakReference<K>)tab[index]) != null) {
            if (item.get() == key) {
                V oldValue = (V)tab[index + 1];
                tab[index + 1] = value;
                return oldValue;
            }
            index = nextKeyIndex(index, currentSize);
        }

        tab[index] = new MyWeakReference<K>(key, objectID);
        tab[index + 1] = value;
        if (++size >= threshold)
            cleanAndResize();
        return null;
    }

//    /**
//     * Removes the mapping for this key from this map if present.
//     * 
//     * The implementation is NOT thread safe.
//     * 
//     * @param key key whose mapping is to be removed from the map
//     * @return the previous value associated with <tt>key</tt>, or
//     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
//     *         (A <tt>null</tt> return can also indicate that the map
//     *         previously associated <tt>null</tt> with <tt>key</tt>.)
//     */
//    public V remove(Object key) {
//        Object[] tab = table;
//        int index = hash(key, currentSize);
//
//        MyWeakReference<K> item;
//        while (true) {
//            item = (MyWeakReference<K>)tab[index];
//            if (item.get() == key) {
//                size--;
//                V oldValue = (V)tab[index + 1];
//                tab[index + 1] = null;
//                tab[index] = null;
//                //TODO: to be implemented
//                closeDeletion(index);
//                return oldValue;
//            }
//            if (item == null)
//                return null;
//            index = nextKeyIndex(index, currentSize);
//        }
//    }

    /**
     * Dumps all entries using the registered dumper.
     * 
     * The implementation is NOT thread safe.
     */
    @SuppressWarnings("unchecked")
    public void dump() {
        if(entryDumper != null) {
            Object[] tab = table;
            int length = tab.length;

            for (int j = 0; j < length; j += 2) {
                MyWeakReference<K> mwr;
                if ((mwr = (MyWeakReference<K>)tab[j]) != null) {
                    entryDumper.dumpEntry(mwr, (V)tab[j+1]);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void cleanAndResize() {
        int newSize = currentSize * INCREMENT_FACTOR;

        Object[] oldTable = table;
        int oldLength = oldTable.length;

        Object[] newTable = new Object[newSize * 2];

        for (int j = 0; j < oldLength; j += 2) {
            MyWeakReference<Object> key = (MyWeakReference<Object>)oldTable[j];
            if (key != null) {
                Object obj;
                if((obj = key.get()) != null) {
                    Object value = oldTable[j+1];
                    int i = hash(obj, newSize);
                    while (newTable[i] != null)
                        i = nextKeyIndex(i, newSize);
                    newTable[i] = key;
                    newTable[i + 1] = value;
                }
                else {
                    if(entryDumper != null) {
                        entryDumper.dumpEntry((MyWeakReference<K>)oldTable[j], (V)oldTable[j+1]);
                    }
                    size--;
                }
            }
        }
        table = newTable;
        currentSize = newSize;
        threshold = newSize * 2 / 3;
    }

    /**
     * Registers a dumper to be executed upon removal of cleared references.
     *  
     * @param entryDumper the dumper to be registered
     * @return the previous dumper (if any)
     */
    public EntryDumper<MyWeakReference<K>, V> registerEntryDumper(EntryDumper<MyWeakReference<K>, V> entryDumper) {
        EntryDumper<MyWeakReference<K>, V> prevCallback = this.entryDumper;
        this.entryDumper = entryDumper;
        return prevCallback;
    }

    /**
     * Gets the registered dumper to be executed upon removal of cleared references.
     * 
     * @return the registered dumper (if any)
     */
    public EntryDumper<MyWeakReference<K>, V> getEntryDumper() {
        return entryDumper;
    }

    /**
     * Removes the registered dumper.
     *  
     * @return the previous dumper (if any)
     */
    public EntryDumper<MyWeakReference<K>, V> removeEntryDumper() {
        return registerEntryDumper(null);
    }

//    /**
//     * This method has not been implemented.
//     * Throws a RuntimeException.
//     */
//    public Set<java.util.Map.Entry<K, V>> entrySet() {
//        throw new RuntimeException("Method " + this.getClass().getName() + ".entrySet() has not been implemented");
//    }

    public interface EntryDumper<MyWeakReference, V> {
        void dumpEntry(MyWeakReference key, V value);
        void close();
    }
}
