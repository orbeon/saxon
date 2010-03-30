package org.orbeon.saxon.om;

import org.orbeon.saxon.expr.LastPositionFinder;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.SingletonNode;


/**
* SingletonIterator: an iterator over a sequence of zero or one values
*/

public class SingletonIterator implements UnfailingIterator,
        org.orbeon.saxon.expr.ReversibleIterator, LastPositionFinder, GroundedIterator, LookaheadIterator {

    private Item item;
    private int position = 0;

    /**
     * Private constructor: external classes should use the factory method
     * @param value the item to iterate over
     */

    private SingletonIterator(Item value) {
        this.item = value;
    }

   /**
    * Factory method.
    * @param item the item to iterate over
    * @return a SingletonIterator over the supplied item, or an EmptyIterator
    * if the supplied item is null.
    */

    public static UnfailingIterator makeIterator(Item item) {
       if (item==null) {
           return EmptyIterator.getInstance();
       } else {
           return new SingletonIterator(item);
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

    public SequenceIterator getAnother() {
        return new SingletonIterator(item);
    }

    public SequenceIterator getReverseIterator() {
        return new SingletonIterator(item);
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
        if (item instanceof AtomicValue) {
            return (AtomicValue)item;
        } else {
            return new SingletonNode((NodeInfo)item);
        }
    }

    /**
     * Indicate that any nodes returned in the sequence will be atomized. This
     * means that if it wishes to do so, the implementation can return the typed
     * values of the nodes rather than the nodes themselves. The implementation
     * is free to ignore this hint.
     * <p>
     * This implementation attempts atomization of a singleton node if it is untyped.
     * This avoids adding an iterator to iterate over the value in the common case where
     * the typed value of the node is a single atomic value.
     *
     * @param atomizing true if the caller of this iterator will atomize any
     *                  nodes that are returned, and is therefore willing to accept the typed
     *                  value of the nodes instead of the nodes themselves.
     */

//    public void setIsAtomizing(boolean atomizing) {
//        if (atomizing && (item instanceof NodeInfo)) {
//            NodeInfo node = (NodeInfo)item;
//            switch (node.getNodeKind()) {
//                case Type.DOCUMENT:
//                case Type.TEXT:
//                    item = new UntypedAtomicValue(node.getStringValueCS());
//                    return;
//
//                case Type.ELEMENT:
//                case Type.ATTRIBUTE:
//                    int t = ((NodeInfo)item).getTypeAnnotation();
//                    if (t == -1 || t == StandardNames.XDT_UNTYPED || t == StandardNames.XDT_UNTYPED_ATOMIC) {
//                        item = new UntypedAtomicValue(node.getStringValueCS());
//                        return;
//                    } else {
//                        // do nothing: don't attempt to atomize the node here
//                       return;
//                    }
//
//                case Type.COMMENT:
//                case Type.PROCESSING_INSTRUCTION:
//                case Type.NAMESPACE:
//                    item = new StringValue(node.getStringValueCS());
//                    return;
//                default:
//                    return;
//            }
//        }
//    }

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
