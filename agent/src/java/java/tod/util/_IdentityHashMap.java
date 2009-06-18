/*
 * Copyright 2000-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package java.tod.util;

/**
 * Slightly modified version of IdentifyHashMap (doesn't need a superclass)
 */

/**
 * This class implements the <tt>Map</tt> interface with a hash table, using
 * reference-equality in place of object-equality when comparing keys (and
 * values).  In other words, in an <tt>IdentityHashMap</tt>, two keys
 * <tt>k1</tt> and <tt>k2</tt> are considered equal if and only if
 * <tt>(k1==k2)</tt>.  (In normal <tt>Map</tt> implementations (like
 * <tt>HashMap</tt>) two keys <tt>k1</tt> and <tt>k2</tt> are considered equal
 * if and only if <tt>(k1==null ? k2==null : k1.equals(k2))</tt>.)
 *
 * <p><b>This class is <i>not</i> a general-purpose <tt>Map</tt>
 * implementation!  While this class implements the <tt>Map</tt> interface, it
 * intentionally violates <tt>Map's</tt> general contract, which mandates the
 * use of the <tt>equals</tt> method when comparing objects.  This class is
 * designed for use only in the rare cases wherein reference-equality
 * semantics are required.</b>
 *
 * <p>A typical use of this class is <i>topology-preserving object graph
 * transformations</i>, such as serialization or deep-copying.  To perform such
 * a transformation, a program must maintain a "node table" that keeps track
 * of all the object references that have already been processed.  The node
 * table must not equate distinct objects even if they happen to be equal.
 * Another typical use of this class is to maintain <i>proxy objects</i>.  For
 * example, a debugging facility might wish to maintain a proxy object for
 * each object in the program being debugged.
 *
 * <p>This class provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key.  This class makes no
 * guarantees as to the order of the map; in particular, it does not guarantee
 * that the order will remain constant over time.
 *
 * <p>This class provides constant-time performance for the basic
 * operations (<tt>get</tt> and <tt>put</tt>), assuming the system
 * identity hash function ({@link System#identityHashCode(Object)})
 * disperses elements properly among the buckets.
 *
 * <p>This class has one tuning parameter (which affects performance but not
 * semantics): <i>expected maximum size</i>.  This parameter is the maximum
 * number of key-value mappings that the map is expected to hold.  Internally,
 * this parameter is used to determine the number of buckets initially
 * comprising the hash table.  The precise relationship between the expected
 * maximum size and the number of buckets is unspecified.
 *
 * <p>If the size of the map (the number of key-value mappings) sufficiently
 * exceeds the expected maximum size, the number of buckets is increased
 * Increasing the number of buckets ("rehashing") may be fairly expensive, so
 * it pays to create identity hash maps with a sufficiently large expected
 * maximum size.  On the other hand, iteration over collection views requires
 * time proportional to the number of buckets in the hash table, so it
 * pays not to set the expected maximum size too high if you are especially
 * concerned with iteration performance or memory usage.
 *
 * <p><b>Note that this implementation is not synchronized.</b> If multiple
 * threads access this map concurrently, and at least one of the threads
 * modifies the map structurally, it <i>must</i> be synchronized externally.
 * (A structural modification is any operation that adds or deletes one or
 * more mappings; merely changing the value associated with a key that an
 * instance already contains is not a structural modification.)  This is
 * typically accomplished by synchronizing on some object that naturally
 * encapsulates the map.  If no such object exists, the map should be
 * "wrapped" using the <tt>Collections.synchronizedMap</tt> method.  This is
 * best done at creation time, to prevent accidental unsynchronized access to
 * the map: <pre>
 *	 Map m = Collections.synchronizedMap(new HashMap(...));
 * </pre>
 *
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> or <tt>add</tt> methods, the iterator will throw a
 * <tt>ConcurrentModificationException</tt>.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>fail-fast iterators should be used only
 * to detect bugs.</i>
 *
 * <p>Implementation note: This is a simple <i>linear-probe</i> hash table,
 * as described for example in texts by Sedgewick and Knuth.  The array
 * alternates holding keys and values.  (This has better locality for large
 * tables than does using separate arrays.)  For many JRE implementations
 * and operation mixes, this class will yield better performance than
 * {@link HashMap} (which uses <i>chaining</i> rather than linear-probing).
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @see	 System#identityHashCode(Object)
 * @see	 Object#hashCode()
 * @see	 Collection
 * @see		Map
 * @see		HashMap
 * @see		TreeMap
 * @author  Doug Lea and Josh Bloch
 * @since   1.4
 */

public class _IdentityHashMap<K,V>
{
	/**
	 * The initial capacity used by the no-args constructor.
	 * MUST be a power of two.  The value 32 corresponds to the
	 * (specified) expected maximum size of 21, given a load factor
	 * of 2/3.
	 */
	private static final int DEFAULT_CAPACITY = 32;

	/**
	 * The minimum capacity, used if a lower value is implicitly specified
	 * by either of the constructors with arguments.  The value 4 corresponds
	 * to an expected maximum size of 2, given a load factor of 2/3.
	 * MUST be a power of two.
	 */
	private static final int MINIMUM_CAPACITY = 4;

	/**
	 * The maximum capacity, used if a higher value is implicitly specified
	 * by either of the constructors with arguments.
	 * MUST be a power of two <= 1<<29.
	 */
	private static final int MAXIMUM_CAPACITY = 1 << 29;

	/**
	 * The table, resized as necessary. Length MUST always be a power of two.
	 */
	private transient Object[] table;

	/**
	 * The number of key-value mappings contained in this identity hash map.
	 *
	 * @serial
	 */
	private int size;

	/**
	 * The number of modifications, to support fast-fail iterators
	 */
	private transient volatile int modCount;

	/**
	 * The next size value at which to resize (capacity * load factor).
	 */
	private transient int threshold;

	/**
	 * Value representing null keys inside tables.
	 */
	private static final Object NULL_KEY = new Object();

	/**
	 * Use NULL_KEY for key if it is null.
	 */

	private static Object maskNull(Object key) {
		return (key == null ? NULL_KEY : key);
	}

	/**
	 * Return internal representation of null key back to caller as null
	 */
	private static Object unmaskNull(Object key) {
		return (key == NULL_KEY ? null : key);
	}

	/**
	 * Constructs a new, empty identity hash map with a default expected
	 * maximum size (21).
	 */
	public _IdentityHashMap() {
		init(DEFAULT_CAPACITY);
	}

	/**
	 * Constructs a new, empty map with the specified expected maximum size.
	 * Putting more than the expected number of key-value mappings into
	 * the map may cause the internal data structure to grow, which may be
	 * somewhat time-consuming.
	 *
	 * @param expectedMaxSize the expected maximum size of the map.
	 * @throws IllegalArgumentException if <tt>expectedMaxSize</tt> is negative
	 */
	public _IdentityHashMap(int expectedMaxSize) {
		if (expectedMaxSize < 0)
			throw new IllegalArgumentException("expectedMaxSize is negative: "
											   + expectedMaxSize);
		init(capacity(expectedMaxSize));
	}

	/**
	 * Returns the appropriate capacity for the specified expected maximum
	 * size.  Returns the smallest power of two between MINIMUM_CAPACITY
	 * and MAXIMUM_CAPACITY, inclusive, that is greater than
	 * (3 * expectedMaxSize)/2, if such a number exists.  Otherwise
	 * returns MAXIMUM_CAPACITY.  If (3 * expectedMaxSize)/2 is negative, it
	 * is assumed that overflow has occurred, and MAXIMUM_CAPACITY is returned.
	 */
	private int capacity(int expectedMaxSize) {
		// Compute min capacity for expectedMaxSize given a load factor of 2/3
		int minCapacity = (3 * expectedMaxSize)/2;

		// Compute the appropriate capacity
		int result;
		if (minCapacity > MAXIMUM_CAPACITY || minCapacity < 0) {
			result = MAXIMUM_CAPACITY;
		} else {
			result = MINIMUM_CAPACITY;
			while (result < minCapacity)
				result <<= 1;
		}
		return result;
	}

	/**
	 * Initialize object to be an empty map with the specified initial
	 * capacity, which is assumed to be a power of two between
	 * MINIMUM_CAPACITY and MAXIMUM_CAPACITY inclusive.
	 */
	private void init(int initCapacity) {
		// assert (initCapacity & -initCapacity) == initCapacity; // power of 2
		// assert initCapacity >= MINIMUM_CAPACITY;
		// assert initCapacity <= MAXIMUM_CAPACITY;

		threshold = (initCapacity * 2)/3;
		table = new Object[2 * initCapacity];
	}

	/**
	 * Returns the number of key-value mappings in this identity hash map.
	 *
	 * @return the number of key-value mappings in this map.
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns <tt>true</tt> if this identity hash map contains no key-value
	 * mappings.
	 *
	 * @return <tt>true</tt> if this identity hash map contains no key-value
	 *		 mappings.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Return index for Object x.
	 */
	private static int hash(Object x, int length) {
		int h = System.identityHashCode(x);
		// Multiply by -127, and left-shift to use least bit as part of hash
		return ((h << 1) - (h << 8)) & (length - 1);
	}

	/**
	 * Circularly traverse table of size len.
	 **/
	private static int nextKeyIndex(int i, int len) {
		return (i + 2 < len ? i + 2 : 0);
	}

	/**
	 * Returns the value to which the specified key is mapped in this identity
	 * hash map, or <tt>null</tt> if the map contains no mapping for
	 * this key.  A return value of <tt>null</tt> does not <i>necessarily</i>
	 * indicate that the map contains no mapping for the key; it is also
	 * possible that the map explicitly maps the key to <tt>null</tt>. The
	 * <tt>containsKey</tt> method may be used to distinguish these two
	 * cases.
	 *
	 * @param   key the key whose associated value is to be returned.
	 * @return  the value to which this map maps the specified key, or
	 *		  <tt>null</tt> if the map contains no mapping for this key.
	 * @see #put(Object, Object)
	 */
	public V get(Object key) {
		Object k = maskNull(key);
	Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);
		while (true) {
		Object item = tab[i];
			if (item == k)
				return (V) tab[i + 1];
			if (item == null)
				return null;
			i = nextKeyIndex(i, len);
		}
	}

	/**
	 * Tests whether the specified object reference is a key in this identity
	 * hash map.
	 *
	 * @param   key   possible key.
	 * @return  <code>true</code> if the specified object reference is a key
	 *		  in this map.
	 * @see	 #containsValue(Object)
	 */
	public boolean containsKey(Object key) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);
		while (true) {
			Object item = tab[i];
			if (item == k)
				return true;
			if (item == null)
				return false;
			i = nextKeyIndex(i, len);
		}
	}

	/**
	 * Tests whether the specified object reference is a value in this identity
	 * hash map.
	 *
	 * @param value value whose presence in this map is to be tested.
	 * @return <tt>true</tt> if this map maps one or more keys to the
	 *		 specified object reference.
	 * @see	 #containsKey(Object)
	 */
	public boolean containsValue(Object value) {
		Object[] tab = table;
		for (int i = 1; i < tab.length; i+= 2)
			if (tab[i] == value)
				return true;

		return false;
	}

	/**
	 * Tests if the specified key-value mapping is in the map.
	 *
	 * @param   key   possible key.
	 * @param   value possible value.
	 * @return  <code>true</code> if and only if the specified key-value
	 *		  mapping is in map.
	 */
	private boolean containsMapping(Object key, Object value) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);
		while (true) {
			Object item = tab[i];
			if (item == k)
				return tab[i + 1] == value;
			if (item == null)
				return false;
			i = nextKeyIndex(i, len);
		}
	}

	/**
	 * Associates the specified value with the specified key in this identity
	 * hash map.  If the map previously contained a mapping for this key, the
	 * old value is replaced.
	 *
	 * @param key the key with which the specified value is to be associated.
	 * @param value the value to be associated with the specified key.
	 * @return the previous value associated with <tt>key</tt>, or
	 *		   <tt>null</tt> if there was no mapping for <tt>key</tt>.  (A
	 *		 <tt>null</tt> return can also indicate that the map previously
	 *		 associated <tt>null</tt> with the specified key.)
	 * @see	 Object#equals(Object)
	 * @see	 #get(Object)
	 * @see	 #containsKey(Object)
	 */
	public V put(K key, V value) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);

		Object item;
		while ( (item = tab[i]) != null) {
			if (item == k) {
		V oldValue = (V) tab[i + 1];
				tab[i + 1] = value;
				return oldValue;
			}
			i = nextKeyIndex(i, len);
		}

		modCount++;
		tab[i] = k;
		tab[i + 1] = value;
		if (++size >= threshold)
			resize(len); // len == 2 * current capacity.
		return null;
	}

	/**
	 * Resize the table to hold given capacity.
	 *
	 * @param newCapacity the new capacity, must be a power of two.
	 */
	private void resize(int newCapacity) {
		// assert (newCapacity & -newCapacity) == newCapacity; // power of 2
		int newLength = newCapacity * 2;

	Object[] oldTable = table;
		int oldLength = oldTable.length;
		if (oldLength == 2*MAXIMUM_CAPACITY) { // can't expand any further
			if (threshold == MAXIMUM_CAPACITY-1)
				throw new IllegalStateException("Capacity exhausted.");
			threshold = MAXIMUM_CAPACITY-1;  // Gigantic map!
			return;
		}
		if (oldLength >= newLength)
			return;

	Object[] newTable = new Object[newLength];
		threshold = newLength / 3;

		for (int j = 0; j < oldLength; j += 2) {
			Object key = oldTable[j];
			if (key != null) {
				Object value = oldTable[j+1];
				oldTable[j] = null;
				oldTable[j+1] = null;
				int i = hash(key, newLength);
				while (newTable[i] != null)
					i = nextKeyIndex(i, newLength);
				newTable[i] = key;
				newTable[i + 1] = value;
			}
		}
		table = newTable;
	}


	/**
	 * Removes the mapping for this key from this map if present.
	 *
	 * @param key key whose mapping is to be removed from the map.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *		   if there was no entry for key.  (A <tt>null</tt> return can
	 *		   also indicate that the map previously associated <tt>null</tt>
	 *		   with the specified key.)
	 */
	public V remove(Object key) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);

		while (true) {
			Object item = tab[i];
			if (item == k) {
				modCount++;
				size--;
				V oldValue = (V) tab[i + 1];
				tab[i + 1] = null;
				tab[i] = null;
				closeDeletion(i);
				return oldValue;
			}
			if (item == null)
				return null;
			i = nextKeyIndex(i, len);
		}

	}

	/**
	 * Removes the specified key-value mapping from the map if it is present.
	 *
	 * @param   key   possible key.
	 * @param   value possible value.
	 * @return  <code>true</code> if and only if the specified key-value
	 *		  mapping was in map.
	 */
	private boolean removeMapping(Object key, Object value) {
		Object k = maskNull(key);
		Object[] tab = table;
		int len = tab.length;
		int i = hash(k, len);

		while (true) {
			Object item = tab[i];
			if (item == k) {
				if (tab[i + 1] != value)
					return false;
				modCount++;
				size--;
				tab[i] = null;
				tab[i + 1] = null;
				closeDeletion(i);
				return true;
			}
			if (item == null)
				return false;
			i = nextKeyIndex(i, len);
		}
	}

	/**
	 * Rehash all possibly-colliding entries following a
	 * deletion. This preserves the linear-probe
	 * collision properties required by get, put, etc.
	 *
	 * @param d the index of a newly empty deleted slot
	 */
	private void closeDeletion(int d) {
		// Adapted from Knuth Section 6.4 Algorithm R
		Object[] tab = table;
		int len = tab.length;

		// Look for items to swap into newly vacated slot
		// starting at index immediately following deletion,
		// and continuing until a null slot is seen, indicating
		// the end of a run of possibly-colliding keys.
		Object item;
		for (int i = nextKeyIndex(d, len); (item = tab[i]) != null;
			 i = nextKeyIndex(i, len) ) {
			// The following test triggers if the item at slot i (which
			// hashes to be at slot r) should take the spot vacated by d.
			// If so, we swap it in, and then continue with d now at the
			// newly vacated i.  This process will terminate when we hit
			// the null slot at the end of this run.
			// The test is messy because we are using a circular table.
			int r = hash(item, len);
			if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {
				tab[d] = item;
				tab[d + 1] = tab[i + 1];
				tab[i] = null;
				tab[i + 1] = null;
				d = i;
			}
		}
	}

	/**
	 * Removes all mappings from this map.
	 */
	public void clear() {
		modCount++;
		Object[] tab = table;
		for (int i = 0; i < tab.length; i++)
			tab[i] = null;
		size = 0;
	}

	/**
	 * Compares the specified object with this map for equality.  Returns
	 * <tt>true</tt> if the given object is also a map and the two maps
	 * represent identical object-reference mappings.  More formally, this
	 * map is equal to another map <tt>m</tt> if and only if
	 * map <tt>this.entrySet().equals(m.entrySet())</tt>.
	 *
	 * <p><b>Owing to the reference-equality-based semantics of this map it is
	 * possible that the symmetry and transitivity requirements of the
	 * <tt>Object.equals</tt> contract may be violated if this map is compared
	 * to a normal map.  However, the <tt>Object.equals</tt> contract is
	 * guaranteed to hold among <tt>IdentityHashMap</tt> instances.</b>
	 *
	 * @param  o object to be compared for equality with this map.
	 * @return <tt>true</tt> if the specified object is equal to this map.
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object o) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the hash code value for this map.  The hash code of a map
	 * is defined to be the sum of the hashcode of each entry in the map's
	 * entrySet view.  This ensures that <tt>t1.equals(t2)</tt> implies
	 * that <tt>t1.hashCode()==t2.hashCode()</tt> for any two
	 * <tt>IdentityHashMap</tt> instances <tt>t1</tt> and <tt>t2</tt>, as
	 * required by the general contract of {@link Object#hashCode()}.
	 *
	 * <p><b>Owing to the reference-equality-based semantics of the
	 * <tt>Map.Entry</tt> instances in the set returned by this map's
	 * <tt>entrySet</tt> method, it is possible that the contractual
	 * requirement of <tt>Object.hashCode</tt> mentioned in the previous
	 * paragraph will be violated if one of the two objects being compared is
	 * an <tt>IdentityHashMap</tt> instance and the other is a normal map.</b>
	 *
	 * @return the hash code value for this map.
	 * @see Object#hashCode()
	 * @see Object#equals(Object)
	 * @see #equals(Object)
	 */
	public int hashCode() {
		throw new UnsupportedOperationException();
	}
}
