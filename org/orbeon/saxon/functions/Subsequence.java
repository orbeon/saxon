package net.sf.saxon.functions;
import net.sf.saxon.expr.PositionIterator;
import net.sf.saxon.expr.TailExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.Token;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.IntegerValue;

/**
* Implements the XPath 2.0 subsequence()  function
*/


public class Subsequence extends SystemFunction {

    // Ideally, we would simply convert this at compile time to a filter expression.
    // Unfortunately, this is not always possible, because a filter expression changes
    // the focus for evaluating the filter expression, while this function does not.

    /**
    * Determine the data type of the items in the sequence
    * @return the type of the argument
    */

    public ItemType getItemType() {
        return argument[0].getItemType();
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-significant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return argument[0].getSpecialProperties();
    }

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue startVal0 = (AtomicValue)argument[1].evaluateItem(context);
        NumericValue startVal = (NumericValue)startVal0.getPrimitiveValue();

        if (argument.length == 2) {
            long lstart;
            if (startVal instanceof IntegerValue) {
                lstart = ((IntegerValue)startVal).longValue();
                if (lstart <= 1) {
                    return seq;
                }
            } else {
                startVal = startVal.round();
                if (startVal.compareTo(IntegerValue.PLUS_ONE) <= 0) {
                    return seq;
                } else if (startVal.compareTo(IntegerValue.MAX_LONG) > 0) {
                    return EmptyIterator.getInstance();
                } else if (startVal.isNaN()) {
                    return EmptyIterator.getInstance();
                } else {
                    lstart = startVal.longValue();
                }
            }

            if (lstart > Integer.MAX_VALUE) {
                // we don't allow sequences longer than an this
                return EmptyIterator.getInstance();
            }

            return new TailExpression.TailIterator(seq, (int)lstart);

        } else {

            // There are three arguments

            AtomicValue lengthVal0 = (AtomicValue)argument[2].evaluateItem(context);
            NumericValue lengthVal = (NumericValue)lengthVal0.getPrimitiveValue();

            if (startVal instanceof IntegerValue && lengthVal instanceof IntegerValue) {
                long lstart = ((IntegerValue)startVal).longValue();
                if (lstart > Integer.MAX_VALUE) {
                    return EmptyIterator.getInstance();
                }
                long llength = ((IntegerValue)lengthVal).longValue();
                if (llength > Integer.MAX_VALUE) {
                    llength = Integer.MAX_VALUE;
                }
                if (llength < 1) {
                    return EmptyIterator.getInstance();
                }
                long lend = lstart + llength - 1;
                if (lend < 1) {
                    return EmptyIterator.getInstance();
                }
                int start = (lstart < 1 ? 1 : (int)lstart);
                return PositionIterator.make(seq, start, (int)lend);
            } else {
                if (startVal.isNaN()) {
                    return EmptyIterator.getInstance();
                }
                if (startVal.compareTo(IntegerValue.MAX_LONG) > 0) {
                    return EmptyIterator.getInstance();
                }
                startVal = startVal.round();

                if (lengthVal.isNaN()) {
                    return EmptyIterator.getInstance();
                }
                lengthVal = lengthVal.round();

                if (lengthVal.compareTo(IntegerValue.ZERO) <= 0) {
                    return EmptyIterator.getInstance();
                }
                NumericValue rend = startVal
                        .arithmetic(Token.PLUS, lengthVal, context)
                        .arithmetic(Token.MINUS, IntegerValue.PLUS_ONE, context);
                if (rend.compareTo(IntegerValue.ZERO) <= 0) {
                    return EmptyIterator.getInstance();
                }

                long lstart;
                if (startVal.compareTo(IntegerValue.PLUS_ONE) <= 0) {
                    lstart = 1;
                } else {
                    lstart = startVal.longValue();
                }
                if (lstart > Integer.MAX_VALUE) {
                    return EmptyIterator.getInstance();
                }

                long lend;
                if (rend.compareTo(IntegerValue.MAX_LONG) >= 0) {
                    lend = Integer.MAX_VALUE;
                } else {
                    lend = rend.longValue();
                }
                return PositionIterator.make(seq, (int)lstart, (int)lend);

            }
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
