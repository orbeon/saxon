package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;

import java.util.List;


/**
 * A sequence value implemented extensionally. That is, this class represents a sequence
 * by allocating memory to each item in the sequence.
 */

public final class SequenceExtent extends Value {
    private Item[] value;
    private int start = 0;  // zero-based offset of the start
    private int end;        // the 0-based index of the first item that is NOT included
    private ItemType itemType = null;   // memoized

    /**
     * Construct an sequence from an array of items. Note, the array of items is used as is,
     * which means the caller must not subsequently change its contents.
     *
     * @param items the array of items to be included in the sequence
     */

    public SequenceExtent(Item[] items) {
        this.value = items;
        end = items.length;
    }

    /**
     * Construct a SequenceExtent as a view of another SequenceExtent
     * @param ext The existing SequenceExtent
     * @param start zero-based offset of the first item in the existing SequenceExtent
     * that is to be included in the new SequenceExtent
     * @param length The number of items in the new SequenceExtent
     */

    public SequenceExtent(SequenceExtent ext, int start, int length) {
        this.value = ext.value;
        this.start = ext.start + start;
        this.end = this.start + length;
    }

    /**
     * Construct a SequenceExtent from a List. The members of the list must all
     * be Items
     *
     * @param list the list of items to be included in the sequence
     */

    public SequenceExtent(List list) {
        value = new Item[list.size()];
        for (int i=0; i<list.size(); i++) {
            value[i] = (Item)list.get(i);
        }
        end = list.size();
    }

    /**
     * Construct a sequence containing all the items in a SequenceIterator.
     *
     * @exception net.sf.saxon.trans.XPathException if reading the items using the
     *     SequenceIterator raises an error
     * @param iter The supplied sequence of items. This must be positioned at
     *     the start, so that hasNext() returns true if there are any nodes in
     *      the node-set, and next() returns the first node.
     */

    public SequenceExtent(SequenceIterator iter) throws XPathException {
        int size = 20;
                // Although the enumeration may be able to
                // say how many nodes it contains, it may be an expensive operation,
                // so we behave as if we don't know.
        value = new Item[size];
        int i = 0;
        while (true) {
            Item it = iter.next();
            if (it == null) {
                break;
            }
            if (i>=size) {
                size *= 2;
                Item newarray[] = new Item[size];
                System.arraycopy(value, 0, newarray, 0, i);
                value = newarray;
            }
            value[i++] = it;
        }
        end = i;
    }

    /**
     * Factory method to make a Value holding the contents of any SequenceIterator
     */

    public static Value makeSequenceExtent(SequenceIterator iter) throws XPathException {
        if (iter instanceof GroundedIterator) {
            return ((GroundedIterator)iter).materialize();
        }
        return new SequenceExtent(iter);
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD;
    }

    /**
     * Simplify this SequenceExtent
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
     */

    public ItemType getItemType() {
        if (itemType != null) {
            // only calculate it the first time
            return itemType;
        }
        if (end==start) {
            itemType = AnyItemType.getInstance();
        } else {
            itemType = computeItemType(value[start]);
            for (int i=start+1; i<end; i++) {
                if (itemType == AnyItemType.getInstance()) {
                    // make a quick exit
                    return itemType;
                }
                itemType = Type.getCommonSuperType(itemType, computeItemType(value[i]));
            }
        }
        return itemType;
    }

    private static ItemType computeItemType(Item item) {
        if (item instanceof AtomicValue) {
            return ((AtomicValue)item).getItemType();
        } else {
            return NodeKindTest.makeNodeKindTest(((NodeInfo)item).getNodeKind());
            // ignore the type annotation for now
        }
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
     * @param context dynamic evaluation context; not used in this
     *     implementation of the method
     * @return the required SequenceIterator, positioned at the start of the
     *     sequence
     */

    public SequenceIterator iterate(XPathContext context) {
        return new ArrayIterator(value, start, end);
    }

    /**
     * Return an enumeration of this sequence in reverse order (used for reverse axes)
     *
     * @return an AxisIterator that processes the items in reverse order
     */

    public AxisIterator reverseIterate() {
        return new ReverseArrayIterator(value, start, end);
    }

    /**
     * Get the effective boolean value
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        int len = getLength();
        if (len == 0) {
            return false;
        } else if (value[0] instanceof NodeInfo) {
            return true;
        } else if (len > 1) {
            // this is a type error - reuse the error messages
            return ExpressionTool.effectiveBooleanValue(iterate(context));
        } else {
            return ((AtomicValue)value[0]).effectiveBooleanValue(context);
        }
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

