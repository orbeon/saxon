package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Tokenizer;
import org.orbeon.saxon.expr.Token;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.value.IntegerValue;
import org.orbeon.saxon.value.NumericValue;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.xpath.XPathException;

/**
* This class supports the ceiling(), floor(), round(), and round-to-half-even() functions,
 * and also the abs() function
*/

public final class Rounding extends SystemFunction {

    public final static int FLOOR = 0;
    public final static int CEILING = 1;
    public final static int ROUND = 2;
    public final static int HALF_EVEN = 3;
    public static final int ABS = 4;

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue val0 = (AtomicValue)argument[0].evaluateItem(context);
        if (val0==null) return null;
        NumericValue val = (NumericValue)val0.getPrimitiveValue();

        switch (operation) {
            case FLOOR:
                return val.floor();
            case CEILING:
                return val.ceiling();
            case ROUND:
                return val.round();
            case HALF_EVEN:
                int scale = 0;
                if (argument.length==2) {
                    AtomicValue scaleVal0 = (AtomicValue)argument[1].evaluateItem(context);
                    NumericValue scaleVal = (NumericValue)scaleVal0.getPrimitiveValue();
                    scale = (int)scaleVal.longValue();
                }
                return val.roundToHalfEven(scale);
            case ABS:
                int sign = val.compareTo(IntegerValue.ZERO);
                if (sign < 0) {
                    return val.negate();
                } else if (sign == 0) {
                    // ensure that the result is positive zero
                    return val.arithmetic(Token.PLUS, IntegerValue.ZERO, context);
                } else {
                    return val;
                }
            default:
                throw new UnsupportedOperationException("Unknown rounding function");
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
