package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.*;

/**
* A FilterIterator filters an input sequence using a filter expression. Note that a FilterIterator
* is not used where the filter is a constant number (PositionFilter is used for this purpose instead),
* so this class does no optimizations for numeric predicates.
*/

public class FilterIterator implements SequenceIterator {

    protected SequenceIterator base;
    protected Expression filter;
    private int position = 0;
    private Item current = null;
    protected XPathContext filterContext;

    /**
    * Constructor
    * @param base An iteration of the items to be filtered
    * @param filter The expression defining the filter predicate
    * @param context The context in which the expression is being evaluated
    */

    public FilterIterator(SequenceIterator base, Expression filter,
                            XPathContext context) {
        this.base = base;
        this.filter = filter;
        filterContext = context.newMinorContext();
        filterContext.setCurrentIterator(base);
        filterContext.setOriginatingConstructType(Location.FILTER_EXPRESSION);
    }

    /**
    * Get the next item if there is one
    */

    public Item next() throws XPathException {
        current = getNextMatchingItem();
        if (current != null) {
            position++;
        }
        return current;
    }

    /**
    * Get the next node that matches the filter predicate if there is one,
     * or null if not.
    */

    protected Item getNextMatchingItem() throws XPathException {
        while (true) {
            Item next = base.next();
            if (next == null) {
                return null;
            }
            if (matches()) {
                return next;
            }
        }
    }

    /**
    * Determine whether the context item matches the filter predicate
    */

    protected boolean matches() throws XPathException {

        // This code is carefully designed to avoid reading more items from the
        // iteration of the filter expression than are absolutely essential.

        // The code is almost identical to the code in ExpressionTool#effectiveBooleanValue
        // except for the handling of a numeric result

        SequenceIterator iterator = filter.iterate(filterContext);
        Item first = iterator.next();
        if (first == null) {
            return false;
        }
        if (first instanceof NodeInfo) {
            return true;
        } else {
            if (first instanceof BooleanValue) {
                if (iterator.next() != null) {
                    ExpressionTool.ebvError("sequence of two or more items starting with an atomic value");
                }
                return ((BooleanValue)first).getBooleanValue();
            } else if (first instanceof StringValue) {
                if (iterator.next() != null) {
                    ExpressionTool.ebvError("sequence of two or more items starting with an atomic value");
                }
                return (first.getStringValueCS().length()!=0);
            } else if (first instanceof NumericValue) {
                if (iterator.next() != null) {
                    ExpressionTool.ebvError("sequence of two or more items starting with an atomic value");
                }
                IntegerValue basePos = new IntegerValue(base.position());
                return first.equals(basePos);
            } else {
                ExpressionTool.ebvError("sequence starting with an atomic value other than a boolean, number, or string");
                return false;
            }
        }
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    /**
    * Get another iterator to return the same nodes
    */

    public SequenceIterator getAnother() throws XPathException {
        return new FilterIterator(base.getAnother(), filter,
                                    filterContext);
    }

    /**
    * Subclass to handle the common special case where it is statically known
    * that the filter cannot return a numeric value
    */

    public static final class NonNumeric extends FilterIterator {

        public NonNumeric(SequenceIterator base, Expression filter,
                            XPathContext context) {
            super(base, filter, context);
        }

        /**
        * Determine whether the context item matches the filter predicate
        */

        protected boolean matches() throws XPathException {
            return filter.effectiveBooleanValue(filterContext);
        }

        /**
        * Get another iterator to return the same nodes
        */

        public SequenceIterator getAnother() throws XPathException {
            return new FilterIterator.NonNumeric(base.getAnother(), filter,
                                        filterContext);
        }
    }

    /**
     * Subclass to support the extension function saxon:leading, which terminates
     * the iteration at the first item whose predicate is false
    */

    public static final class Leading extends FilterIterator {

        public Leading(SequenceIterator base, Expression filter,
                            XPathContext context) {
            super(base, filter, context);
        }

        /**
        * Determine whether the context item matches the filter predicate
        */

        protected boolean matches() throws XPathException {
            return filter.effectiveBooleanValue(filterContext);
        }

        /**
        * Get the next node that matches the filter predicate if there is one
        */

        protected Item getNextMatchingItem() throws XPathException {
            while (true) {
                Item next = base.next();
                if (next == null) {
                    return null;
                }
                if (matches()) {
                    return next;
                } else {
                    // terminate the iteration on the first non-match
                    return null;
                }
            }
        }

        /**
        * Get another iterator to return the same nodes
        */

        public SequenceIterator getAnother() throws XPathException {
            return new FilterIterator.Leading(base.getAnother(), filter,
                                        filterContext);
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
