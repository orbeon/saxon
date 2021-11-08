package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.LastPositionFinder;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;

import java.util.ArrayList;
import java.util.List;


/**
 * A sequence value implemented extensionally. That is, this class represents a sequence
 * by allocating memory to each item in the sequence.
 */

public final class SequenceExtent extends Value implements GroundedValue {
    private Item[] value;
    private int start = 0;  // zero-based offset of the start
    private int end;        // the 0-based index of the first item that is NOT included
                            // If start=0 this is the length of the sequence
    private ItemType itemType = null;   // memoized

//    private static int instances = 0;

    /**
     * Construct an sequence from an array of items. Note, the array of items is used as is,
     * which means the caller must not subsequently change its contents.
     *
     * @param items the array of items to be included in the sequence
     */

    public SequenceExtent(Item[] items) {
        value = items;
        end = items.length;
    }

    /**
     * Construct a SequenceExtent from part of an array of items
     * @param value The array
     * @param start zero-based offset of the first item in the array
     * that is to be included in the new SequenceExtent
     * @param length The number of items in the new SequenceExtent
     */

    public SequenceExtent(Item[] value, int start, int length) {
        this.value = value;
        this.start = start;
        end = this.start + length;
    }


    /**
     * Construct a SequenceExtent as a view of another SequenceExtent
     * @param ext The existing SequenceExtent
     * @param start zero-based offset of the first item in the existing SequenceExtent
     * that is to be included in the new SequenceExtent
     * @param length The number of items in the new SequenceExtent
     */

    public SequenceExtent(SequenceExtent ext, int start, int length) {
        value = ext.value;
        this.start = ext.start + start;
        end = this.start + length;
    }

    /**
     * Construct a SequenceExtent from a List. The members of the list must all
     * be Items
     *
     * @param list the list of items to be included in the sequence
     */

    public SequenceExtent(List list) {
        Item[] array = new Item[list.size()];
        value = (Item[])list.toArray(array);
        end = value.length;
    }

    /**
     * Construct a sequence containing all the items in a SequenceIterator.
     *
     * @exception org.orbeon.saxon.trans.XPathException if reading the items using the
     *     SequenceIterator raises an error
     * @param iter The supplied sequence of items. This must be positioned at
     *     the start, so that hasNext() returns true if there are any nodes in
     *      the node-set, and next() returns the first node.
     */

    public SequenceExtent(SequenceIterator iter) throws XPathException {
        if ((iter.getProperties() & SequenceIterator.LAST_POSITION_FINDER) == 0) {
            List list = new ArrayList(20);
            while (true) {
                Item it = iter.next();
                if (it == null) {
                    break;
                }
                list.add(it);
            }
            Item[] array = new Item[list.size()];
            value = (Item[])list.toArray(array);
            end = value.length;
        } else {
            end = ((LastPositionFinder)iter).getLastPosition();
            value = new Item[end];
            int i = 0;
            while (true) {
                Item it = iter.next();
                if (it == null) {
                    break;
                }
                value[i++] = it;
            }
        }
    }

    /**
     * Factory method to make a Value holding the contents of any SequenceIterator
     * @param iter a Sequence iterator that will be consumed to deliver the items in the sequence
     * @return a ValueRepresentation holding the items delivered by the SequenceIterator. If the
     * sequence is empty the result will be an instance of {@link EmptySequence}. If it is of length
     * one, the result will be an {@link Item}. In all other cases, it will be an instance of
     * {@link SequenceExtent}.
     */

    public static ValueRepresentation makeSequenceExtent(SequenceIterator iter) throws XPathException {
        if ((iter.getProperties() & SequenceIterator.GROUNDED) != 0) {
            return ((GroundedIterator)iter).materialize();
        }
        Value extent = new SequenceExtent(iter);
        int len = extent.getLength();
        if (len==0) {
            return EmptySequence.getInstance();
        } else if (len==1) {
            return extent.itemAt(0);
        } else {
            return extent;
        }
    }

    /**
     * Simplify this SequenceExtent
     * @return a Value holding the items delivered by the SequenceIterator. If the
     * sequence is empty the result will be an instance of {@link EmptySequence}. If it is of length
     * one, the result will be an {@link AtomicValue} or a {@link SingletonNode}.
     * In all other cases, the {@link SequenceExtent} will be returned unchanged.
     */

    public Value simplify() {
        int n = getLength();
        if (n == 0) {
            return EmptySequence.getInstance();
        } else if (n == 1) {
            return Value.asValue(itemAt(0));
        } else {
            return this;
        }
    }

    /**
     * Reduce a value to its simplest form. If the value is a closure or some other form of deferred value
     * such as a FunctionCallPackage, then it is reduced to a SequenceExtent. If it is a SequenceExtent containing
     * a single item, then it is reduced to that item. One consequence that is exploited by class FilterExpression
     * is that if the value is a singleton numeric value, then the result will be an instance of NumericValue
     */

    public Value reduce() {
        return simplify();
    }

    /**
     * Get the number of items in the sequence
     *
     * @return the number of items in the sequence
     */

    public int getLength() {
        return end - start;
    }

    /**
     * Determine the cardinality
     *
     * @return the cardinality of the sequence, using the constants defined in
     *      org.orbeon.saxon.value.Cardinality
     * @see org.orbeon.saxon.value.Cardinality
     */

    public int getCardinality() {
        switch (end - start) {
            case 0:
                return StaticProperty.EMPTY;
            case 1:
                return StaticProperty.EXACTLY_ONE;
            default:
                return StaticProperty.ALLOWS_ONE_OR_MORE;
        }
    }

    /**
     * Get the (lowest common) item type
     *
     * @return integer identifying an item type to which all the items in this
     *      sequence conform
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (itemType != null) {
            // only calculate it the first time
            return itemType;
        }
        if (end==start) {
            itemType = AnyItemType.getInstance();
        } else {
            itemType = Type.getItemType(value[start], th);
            for (int i=start+1; i<end; i++) {
                if (itemType == AnyItemType.getInstance()) {
                    // make a quick exit
                    return itemType;
                }
                itemType = Type.getCommonSuperType(itemType, Type.getItemType(value[i], th), th);
            }
        }
        return itemType;
    }

    /**
     * Get the n'th item in the sequence (starting with 0 as the first item)
     *
     * @param n the position of the required item
     * @return the n'th item in the sequence
     */

    public Item itemAt(int n) {
        if (n<0 || n>=getLength()) {
            return null;
        } else {
            return value[start+n];
        }
    }

    /**
     * Swap two items (needed to support sorting)
     *
     * @param a the position of the first item to be swapped
     * @param b the position of the second item to be swapped
     */

    public void swap(int a, int b) {
        Item temp = value[start+a];
        value[start+a] = value[start+b];
        value[start+b] = temp;
    }

    /**
     * Return an iterator over this sequence.
     *
     * @return the required SequenceIterator, positioned at the start of the
     *     sequence
     */

    public SequenceIterator iterate() {
        return new ArrayIterator(value, start, end);
    }

    /**
     * Return an enumeration of this sequence in reverse order (used for reverse axes)
     *
     * @return an AxisIterator that processes the items in reverse order
     */

    public UnfailingIterator reverseIterate() {
        return new ReverseArrayIterator(value, start, end);
    }

    /**
     * Get the effective boolean value
     */

    public boolean effectiveBooleanValue() throws XPathException {
        int len = getLength();
        if (len == 0) {
            return false;
        } else if (value[start] instanceof NodeInfo) {
            return true;
        } else if (len > 1) {
            // this is a type error - reuse the error messages
            return ExpressionTool.effectiveBooleanValue(iterate());
        } else {
            return ((AtomicValue)value[start]).effectiveBooleanValue();
        }
    }


    /**
     * Get a subsequence of the value
     *
     * @param start  the index of the first item to be included in the result, counting from zero.
     *               A negative value is taken as zero. If the value is beyond the end of the sequence, an empty
     *               sequence is returned
     * @param length the number of items to be included in the result. Specify Integer.MAX_VALUE to
     *               get the subsequence up to the end of the base sequence. If the value is negative, an empty sequence
     *               is returned. If the value goes off the end of the sequence, the result returns items up to the end
     *               of the sequence
     * @return the required subsequence. If min is
     */

    public GroundedValue subsequence(int start, int length) {
        int newStart;
        if (start < 0) {
            start = 0;
        } else if (start >= end) {
            return EmptySequence.getInstance();
        }
        newStart = this.start + start;
        int newEnd;
        if (length == Integer.MAX_VALUE) {
            newEnd = end;
        } else if (length < 0) {
            return EmptySequence.getInstance();
        } else {
            newEnd = newStart + length;
            if (newEnd > end) {
                newEnd = end;
            }
        }
        return new SequenceExtent(value, newStart, newEnd - newStart);
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

