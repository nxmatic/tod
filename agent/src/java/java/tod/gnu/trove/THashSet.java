///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2001, Eric D. Friedman All Rights Reserved.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
///////////////////////////////////////////////////////////////////////////////

package java.tod.gnu.trove;

import java.tod.util._Arrays;


/**
 * An implementation of the <tt>Set</tt> interface that uses an
 * open-addressed hash table to store its contents.
 *
 * Created: Sat Nov  3 10:38:17 2001
 *
 * @author Eric D. Friedman
 * @version $Id: THashSet.java,v 1.20 2008/05/07 19:26:30 robeden Exp $
 */

public class THashSet<E> extends TObjectHash<E> {
    
    static final long serialVersionUID = 1L;

    /**
     * Creates a new <code>THashSet</code> instance with the default
     * capacity and load factor.
     */
    public THashSet() {
        super();
    }

    /**
     * Creates a new <code>THashSet</code> instance with the default
     * capacity and load factor.
     * 
     * @param strategy used to compute hash codes and to compare objects.
     */
    public THashSet(TObjectHashingStrategy<E> strategy) {
        super(strategy);
    }

    /**
     * Creates a new <code>THashSet</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the default load factor.
     *
     * @param initialCapacity an <code>int</code> value
     */
    public THashSet(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Creates a new <code>THashSet</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the default load factor.
     *
     * @param initialCapacity an <code>int</code> value
     * @param strategy used to compute hash codes and to compare objects.
     */
    public THashSet(int initialCapacity, TObjectHashingStrategy<E> strategy) {
        super(initialCapacity, strategy);
    }

    /**
     * Creates a new <code>THashSet</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the specified load factor.
     *
     * @param initialCapacity an <code>int</code> value
     * @param loadFactor a <code>float</code> value
     */
    public THashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Creates a new <code>THashSet</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the specified load factor.
     *
     * @param initialCapacity an <code>int</code> value
     * @param loadFactor a <code>float</code> value
     * @param strategy used to compute hash codes and to compare objects.
     */
    public THashSet(int initialCapacity, float loadFactor, TObjectHashingStrategy<E> strategy) {
        super(initialCapacity, loadFactor, strategy);
    }

    /**
     * Inserts a value into the set.
     *
     * @param obj an <code>Object</code> value
     * @return true if the set was modified by the add operation
     */
    public boolean add(E obj) {
        int index = insertionIndex(obj);

        if (index < 0) {
            return false;       // already present in set, nothing to add
        }

        Object old = _set[index];
        _set[index] = obj;

        postInsertHook(old == FREE);
        return true;            // yes, we added something
    }

    public int hashCode() {
        HashProcedure p = new HashProcedure();
        forEach(p);
        return p.getHashCode();
    }

    private final class HashProcedure implements TObjectProcedure<E> {
        private int h = 0;

        public int getHashCode() {
            return h;
        }

        public final boolean execute(E key) {
            h += _hashingStrategy.computeHashCode(key);
            return true;
        }
    }

    /**
     * Expands the set to accommodate new values.
     *
     * @param newCapacity an <code>int</code> value
     */
    protected void rehash(int newCapacity) {
        int oldCapacity = _set.length;
        Object oldSet[] = _set;

        _set = new Object[newCapacity];
        _Arrays.fill(_set, FREE);

        for (int i = oldCapacity; i-- > 0;) {
            if(oldSet[i] != FREE && oldSet[i] != REMOVED) {
                E o = (E) oldSet[i];
                int index = insertionIndex(o);
                if (index < 0) { // everyone pays for this because some people can't RTFM
                    throwObjectContractViolation(_set[(-index -1)], o);
                }
                _set[index] = o;
            }
        }
    }

    /**
     * Empties the set.
     */
    public void clear() {
        super.clear();

        _Arrays.fill(_set, 0, _set.length, FREE);
    }

    /**
     * Removes <tt>obj</tt> from the set.
     *
     * @param obj an <code>Object</code> value
     * @return true if the set was modified by the remove operation.
     */
    public boolean remove(Object obj) {
        int index = index((E) obj);
        if (index >= 0) {
            removeAt(index);
            return true;
        }
        return false;
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder("{");
        forEach(new TObjectProcedure() {
            private boolean first = true;

            public boolean execute(Object value) {
                if ( first ) first = false;
                else buf.append( "," );

                buf.append(value);
                return true;
            }
        });
        buf.append("}");
        return buf.toString();
    }

} // THashSet
