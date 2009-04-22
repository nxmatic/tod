/*
 *  @(#)WeakHashMap.java	1.39 06/05/24
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package java.tod;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;


/**
 * An adaptation of {@link WeakHashMap} with {@link Object} keys,
 * primitive long values and identity matching.
 * TODO: check license, take OpenJDK 
 */
public class WeakLongHashMap {

	public static final long NULL_VALUE = 0;
	
    /**
     * The default initial capacity -- MUST be a power of two.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 1 << 18;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load fast used when none specified in constructor.
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    private Entry[] table;

    /**
     * The number of key-value mappings contained in this weak hash map.
     */
    private int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int threshold;

    /**
     * The load factor for the hash table.
     */
    private final float loadFactor;

    /**
     * Reference queue for cleared WeakEntries
     */
    private final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();

    /**
     * Constructs a new, empty <tt>WeakHashMap</tt> with the default initial
     * capacity (16) and load factor (0.75).
     */
    public WeakLongHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = DEFAULT_INITIAL_CAPACITY;
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
    }

    // internal utilities

    /**
     * Checks for equality of non-null reference x and possibly-null y.  By
     * default uses Object.equals.
     */
    static boolean eq(Object x, Object y) {
        return x == y;
    }

    /**
     * Returns index for hash code h.
     */
    static int indexFor(int h, int length) {
        return h & (length-1);
    }

    /**
     * Expunges stale entries from the table.
     */
    private void expungeStaleEntries() {
	Entry e;
        while ( (e = (Entry) queue.poll()) != null) {
            int h = e.hash;
            int i = indexFor(h, table.length);

            Entry prev = table[i];
            Entry p = prev;
            while (p != null) {
                Entry next = p.next;
                if (p == e) {
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                    e.next = null;  // Help GC
                    size--;
                    break;
                }
                prev = p;
                p = next;
            }
        }
    }

    /**
     * Returns the table after first expunging stale entries.
     */
    private Entry[] getTable() {
        expungeStaleEntries();
        return table;
    }

    /**
     * Returns the number of key-value mappings in this map.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public int size() {
        if (size == 0)
            return 0;
        expungeStaleEntries();
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Note: taken from HashMap
     * Applies a supplemental hash function to a given hashCode, which
     * defends against poor quality hash functions.  This is critical
     * because HashMap uses power-of-two length hash tables, that
     * otherwise encounter collisions for hashCodes that do not differ
     * in lower bits. Note: Null keys always map to hash 0, thus index 0.
     */
    static int hash(Object key) {
    	int h = System.identityHashCode(key);
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    
    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    public long get(Object k) {
        int h = hash(k);
        Entry[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry e = tab[index];
        while (e != null) {
            if (e.hash == h && eq(k, e.get()))
                return e.value;
            e = e.next;
        }
        return NULL_VALUE;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param  key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if there is a mapping for <tt>key</tt>;
     *         <tt>false</tt> otherwise
     */
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * Returns the entry associated with the specified key in this map.
     * Returns null if the map contains no mapping for this key.
     */
    Entry getEntry(Object k) {
        int h = hash(k);
        Entry[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry e = tab[index];
        while (e != null && !(e.hash == h && eq(k, e.get())))
            e = e.next;
        return e;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public long put(Object k, long value) {
        int h = hash(k);
        Entry[] tab = getTable();
        int i = indexFor(h, tab.length);

        for (Entry e = tab[i]; e != null; e = e.next) {
            if (h == e.hash && eq(k, e.get())) {
                long oldValue = e.value;
                if (value != oldValue)
                    e.value = value;
                return oldValue;
            }
        }

	Entry e = tab[i];
        tab[i] = new Entry(k, value, queue, h, e);
        if (++size >= threshold)
            resize(tab.length * 2);
        return NULL_VALUE;
    }

    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     *
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *        must be greater than current capacity unless current
     *        capacity is MAXIMUM_CAPACITY (in which case value
     *        is irrelevant).
     */
    void resize(int newCapacity) {
        Entry[] oldTable = getTable();
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        Entry[] newTable = new Entry[newCapacity];
        transfer(oldTable, newTable);
        table = newTable;

        /*
         * If ignoring null elements and processing ref queue caused massive
         * shrinkage, then restore old table.  This should be rare, but avoids
         * unbounded expansion of garbage-filled tables.
         */
        if (size >= threshold / 2) {
            threshold = (int)(newCapacity * loadFactor);
        } else {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    /** Transfers all entries from src to dest tables */
    private void transfer(Entry[] src, Entry[] dest) {
        for (int j = 0; j < src.length; ++j) {
            Entry e = src[j];
            src[j] = null;
            while (e != null) {
                Entry next = e.next;
                Object key = e.get();
                if (key == null) {
                    e.next = null;  // Help GC
                    size--;
                } else {
                    int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }

    /**
     * Removes the mapping for a key from this weak hash map if it is present.
     * More formally, if this map contains a mapping from key <tt>k</tt> to
     * value <tt>v</tt> such that <code>(key==null ?  k==null :
     * key.equals(k))</code>, that mapping is removed.  (The map can contain
     * at most one such mapping.)
     *
     * <p>Returns the value to which this map previously associated the key,
     * or <tt>null</tt> if the map contained no mapping for the key.  A
     * return value of <tt>null</tt> does not <i>necessarily</i> indicate
     * that the map contained no mapping for the key; it's also possible
     * that the map explicitly mapped the key to <tt>null</tt>.
     *
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>
     */
    public long remove(Object k) {
        int h = hash(k);
        Entry[] tab = getTable();
        int i = indexFor(h, tab.length);
        Entry prev = tab[i];
        Entry e = prev;

        while (e != null) {
            Entry next = e.next;
            if (h == e.hash && eq(k, e.get())) {
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return e.value;
            }
            prev = e;
            e = next;
        }

        return NULL_VALUE;
    }



    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        // clear out ref queue. We don't need to expunge entries
        // since table is getting cleared.
        while (queue.poll() != null)
            ;

        Entry[] tab = table;
        for (int i = 0; i < tab.length; ++i)
            tab[i] = null;
        size = 0;

        // Allocation of array may have caused GC, which may have caused
        // additional entries to go stale.  Removing these entries from the
        // reference queue will make them eligible for reclamation.
        while (queue.poll() != null)
            ;
    }

    /**
     * The entries in this hash table extend WeakReference, using its main ref
     * field as the key.
     */
    private static class Entry extends WeakReference<Object> {
        private long value;
        private final int hash;
        private Entry next;

        /**
         * Creates new entry.
         */
        Entry(Object key, long value,
	      ReferenceQueue<Object> queue,
              int hash, Entry next) {
            super(key, queue);
            this.value = value;
            this.hash  = hash;
            this.next  = next;
        }

        public Object getKey() {
            return get();
        }

        public long getValue() {
            return value;
        }

        public long setValue(long newValue) {
	    long oldValue = value;
            value = newValue;
            return oldValue;
        }

        public boolean equals(Object o) {
        	throw new RuntimeException();
//            if (!(o instanceof Entry))
//                return false;
//            Entry e = (Entry)o;
//            Object k1 = getKey();
//            Object k2 = e.getKey();
//            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
//                long v1 = getValue();
//                long v2 = e.getValue();
//                if (v1 == v2)
//                    return true;
//            }
//            return false;
        }

        public int hashCode() {
        	throw new RuntimeException();
//            Object k = getKey();
//            long v = getValue();
//            return  ((k==null ? 0 : k.hashCode()) ^
//                     ((int) v));
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

}
