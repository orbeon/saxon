package org.orbeon.saxon.om;

import org.orbeon.saxon.expr.LastPositionFinder;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SingletonNode;
import org.orbeon.saxon.value.Value;


/**
* SingletonIterator: an iterator over a sequence of zero or one values
*/

public class SingleNodeIterator implements AxisIterator, UnfailingIterator,
        org.orbeon.saxon.expr.ReversibleIterator, LastPositionFinder, GroundedIterator, LookaheadIterator {

    private NodeInfo item;
    private int position = 0;

    /**
     * Private constructor: external classes should use the factory method
     * @param value the item to iterate over
     */

    private SingleNodeIterator(NodeInfo value) {
        this.item = value;
    }

   /**
    * Factory method.
    * @param item the item to iterate over
    * @return a SingletonIterator over the supplied item, or an EmptyIterator
    * if the supplied item is null.
    */

    public static AxisIterator makeIterator(NodeInfo item) {
       if (item==null) {
           return EmptyIterator.getInstance();
       } else {
           return new SingleNodeIterator(item);
       }
   }

    /**
     * Determine whether there are more items to come. Note that this operation
     * is stateless and it is not necessary (or usual) to call it before calling
     * next(). It is used only when there is an explicit need to tell if we
     * are at the last element.
     *
     * @return true if there are more items
     */

    public boolean hasNext() {
        return position == 0;
    }

    /**
     * Move to the next node, without returning it. Returns true if there is
     * a next node, false if the end of the sequence has been reached. After
     * calling this method, the current node may be retrieved using the
     * current() function.
     */

    public boolean moveNext() {
        return next() != null;
    }

    public Item next() {
        if (position == 0) {
            position = 1;
            return item;
        } else if (position == 1) {
            position = -1;
            return null;
        } else {
            return null;
        }
    }

    public Item current() {
        if (position == 1) {
            return item;
        } else {
            return null;
        }
    }

    /**
     * Return the current position in the sequence.
     * @return 0 before the first call on next(); 1 before the second call on next(); -1 after the second
     * call on next().
     */
    public int position() {
       return position;
    }

    public int getLastPosition() {
        return 1;
    }

    public void close() {
    }

    /**
     * Return an iterator over an axis, starting at the current node.
     *
     * @param axis the axis to iterate over, using a constant such as
     *             {@link Axis#CHILD}
     * @param test a predicate to apply to the nodes before returning them.
     */

    public AxisIterator iterateAxis(byte axis, NodeTest test) {
        if (position == 1) {
            return item.iterateAxis(axis, test);
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Return the atomized value of the current node.
     *
     * @return the atomized value.
     * @throws NullPointerException if there is no current node
     */

    public Value atomize() throws XPathException {
        if (position == 1) {
            return item.atomize();
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Return the string value of the current node.
     *
     * @return the string value, as an instance of CharSequence.
     * @throws NullPointerException if there is no current node
     */

    public CharSequence getStringValue() {
        if (position == 1) {
            return item.getStringValueCS();
        } else {
            throw new NullPointerException();
        }
    }


    public SequenceIterator getAnother() {
        return new SingleNodeIterator(item);
    }

    public SequenceIterator getReverseIterator() {
        return new SingleNodeIterator(item);
    }

    public Item getValue() {
        return item;
    }

    /**
     * Return a Value containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding Value. If the value is a closure or a function call package, it will be
     * evaluated and expanded.
     */

    public GroundedValue materialize() {
        return new SingletonNode(item);
    }


    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return GROUNDED | LAST_POSITION_FINDER | LOOKAHEAD;
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
