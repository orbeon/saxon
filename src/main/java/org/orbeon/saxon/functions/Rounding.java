package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.ArithmeticExpression;
import org.orbeon.saxon.expr.Calculator;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Int64Value;
import org.orbeon.saxon.value.NumericValue;

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
        NumericValue val = (NumericValue)val0;

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
                    NumericValue scaleVal = (NumericValue)scaleVal0;
                    scale = (int)scaleVal.longValue();
                }
                return val.roundHalfToEven(scale);
            case ABS:
                double sign = val.signum();
                if (sign < 0) {
                    return val.negate();
                } else if (sign == 0) {
                    // ensure that the result is positive zero
                    //return val.arithmetic(Token.PLUS, Int64Value.ZERO, context);
                    return ArithmeticExpression.compute(val, Calculator.PLUS, Int64Value.ZERO, context);
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
