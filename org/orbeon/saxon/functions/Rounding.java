package net.sf.saxon.functions;
import net.sf.saxon.expr.Token;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.NumericValue;

/**
* This class supports the ceiling(), floor(), round(), and round-to-half-even() functions,
 * and also the abs() function
*/

public final class Rounding extends SystemFunction {

    public static final int FLOOR = 0;
    public static final int CEILING = 1;
    public static final int ROUND = 2;
    public static final int HALF_EVEN = 3;
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
                double sign = val.signum();
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
