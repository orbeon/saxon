package net.sf.saxon.functions;
import net.sf.saxon.expr.PositionIterator;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.TailExpression;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.type.ItemType;

/**
* Implements the XPath 2.0 subsequence()  function
*/


public class Subsequence extends SystemFunction {

    // Ideally, we would simply convert this at compile time to a filter expression.
    // Unfortunately, this is not always possible, because a filter expression changes
    // the focus for evaluating the filter expression, while this function does not.

    //public final static int ITEM_AT = 0;
    public final static int SUBSEQUENCE = 1;

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
        int start = (int)startVal.round().longValue();

        if (argument.length==3) {
            AtomicValue lengthVal0 = (AtomicValue)argument[2].evaluateItem(context);
            NumericValue lengthVal = (NumericValue)lengthVal0.getPrimitiveValue();
            int end = start + (int)lengthVal.round().longValue() - 1;
            if (start < 1) {
                start = 1;
            }
            return PositionIterator.make(seq, start, end);
        } else {
            if (start <= 1) {
                return seq;
            } else {
                return new TailExpression.TailIterator(seq, start);
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
