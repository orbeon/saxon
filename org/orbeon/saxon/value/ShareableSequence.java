package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.ListIterator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;

import java.util.List;


/**
 * A sequence value implemented extensionally using an extensible List whose leading part can be shared
 * with other sequence values. The list can be appended to by other users (at most one other user!),
 * but the items within the range used by this sequence value cannot be modified.
 */

public final class ShareableSequence extends Value {
    private List list;
    private int end;        // the 0-based index of the first item that is NOT included
    private ItemType itemType = null;   // memoized


    /**
     * Construct an sequence from an array of items. Note, the list of items is used as is,
     * which means the caller must not subsequently change its contents; however it is permitted
     * to subsequently append items to the list (indeed, that is the raison d'etre of this class)
     *
     * @param list the list of items to be included in the sequence
     */

    public ShareableSequence(List list) {
        //System.err.println("** Using shareable sequence **");
        this.list = list;
        end = list.size();
    }

    /**
     * Determine whether another value can share this list. This is true provided the list has
     * not already been extended by another value.
     * @return true if another value can share this list
     */

    public boolean isShareable() {
        return list.size() == end;
    }

    /**
     * Get the underlying list
     * @return the underlying list of values
     */

    public List getList() {
        return list;
    }

    /**
     * Simplify this value
     * @return the simplified value
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
        return end;
    }

    /**
     * Determine the cardinality
     *
     * @return the cardinality of the sequence, using the constants defined in
     *      org.orbeon.saxon.value.Cardinality
     * @see Cardinality
     */

    public int getCardinality() {
        switch (end) {
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
        if (end==0) {
            itemType = AnyItemType.getInstance();
        } else {
            itemType = Type.getItemType(itemAt(0), th);
            for (int i=1; i<end; i++) {
                if (itemType == AnyItemType.getInstance()) {
                    // make a quick exit
                    return itemType;
                }
                itemType = Type.getCommonSuperType(itemType, Type.getItemType(itemAt(i), th), th);
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
            return (Item)list.get(n);
        }
    }

    /**
     * Return an iterator over this sequence.
     *
     * @return the required SequenceIterator, positioned at the start of the
     *     sequence
     */

    public SequenceIterator iterate() {
        return new ListIterator(list, end);
    }

    /**
     * Get the effective boolean value
     */

    public boolean effectiveBooleanValue() throws XPathException {
        int len = getLength();
        if (len == 0) {
            return false;
        } else if (itemAt(0) instanceof NodeInfo) {
            return true;
        } else if (len > 1) {
            // this is a type error - reuse the error messages
            return ExpressionTool.effectiveBooleanValue(iterate());
        } else {
            return ((AtomicValue)itemAt(0)).effectiveBooleanValue();
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

