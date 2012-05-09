package ch.usi.dag.disl.example.racer.runtime;

import java.lang.ref.WeakReference;

public final class MyWeakKeyIdentityHashMap {
    private static final int DEFAULT_INITIAL_CAPACITY = 4096;
    private static final int INCREMENT_FACTOR = 2;
    private transient Object[] table;
    private int size = 0;
    private transient int threshold;

    private volatile int currentSize;

    public MyWeakKeyIdentityHashMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public MyWeakKeyIdentityHashMap(int initCapacity) {
        currentSize = initCapacity;
        threshold = (currentSize * 2) / 3;
        table = new Object[currentSize * 2];
    }

    private int hash(Object obj, int size) {
        return (System.identityHashCode(obj) % size) * 2;
    }

    private static int nextKeyIndex(int prevIndex, int size) {
        return (prevIndex + 2 < (size * 2) ? prevIndex + 2 : 0);
    }

    //Called by the aspect upon fieldGet/fieldSet
    public Object get(Object key) {
        Object[] tab = table;
        int index = hash(key, currentSize);
        while (true) {
            WeakReference<Object> item = (WeakReference<Object>)tab[index];
            if (item == null)
                return null;
            if (item.get() == key)
                return tab[index + 1];
            index = nextKeyIndex(index, currentSize);
        }
    }

    //Called by the aspect upon fieldGet/fieldSet
    public void put(Object key, Object value) {
        Object[] tab = table;
        int index = hash(key, currentSize);

        WeakReference<Object> item;
        while ((item = (WeakReference<Object>)tab[index]) != null) {
            if (item.get() == key) {
                tab[index + 1] = value;
                return;
            }
            index = nextKeyIndex(index, currentSize);
        }

        tab[index] = new WeakReference<Object>(key);
        tab[index + 1] = value;
        if (++size >= threshold)
            cleanAndResize();
    }

    private void cleanAndResize() {
        int newSize = currentSize * INCREMENT_FACTOR;

        Object[] oldTable = table;
        int oldLength = oldTable.length;

        Object[] newTable = new Object[newSize * 2];

        for (int j = 0; j < oldLength; j += 2) {
            WeakReference<Object> key = (WeakReference<Object>)oldTable[j];
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
                    size--;
                }
            }
        }
        table = newTable;
        currentSize = newSize;
        threshold = newSize * 2 / 3;
    }
}