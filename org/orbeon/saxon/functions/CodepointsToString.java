package org.orbeon.saxon.functions;
import org.orbeon.saxon.charcode.UTF16;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.StringLiteral;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.NumericValue;
import org.orbeon.saxon.value.StringValue;

/**
 * This class supports the function codepoints-to-string
 */

public class CodepointsToString extends SystemFunction {

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        final XPathContext context = visitor.getStaticContext().makeEarlyEvaluationContext();
        return new StringLiteral(
            unicodeToString(argument[0].iterate(context), context));
    }

    /**
    * Evaluate
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        return StringValue.makeStringValue(unicodeToString(argument[0].iterate(c), c));
    }

    /**
    * Return the Unicode string corresponding to a given sequence of Unicode code values
     * @param chars iterator delivering the characters as integer values
     * @param context the evaluation context
     * @throws org.orbeon.saxon.trans.XPathException if any of the integers is not the codepoint of a valid XML character
    */

    public static CharSequence unicodeToString(SequenceIterator chars, XPathContext context) throws XPathException {
        FastStringBuffer sb = new FastStringBuffer(256);
        NameChecker checker = context.getConfiguration().getNameChecker();
        while (true) {
            NumericValue nextInt = (NumericValue)chars.next();
            if (nextInt == null) {
                return sb.condense();
            }
            long next = nextInt.longValue();
            if (next < 0 || next > Integer.MAX_VALUE || !checker.isValidChar((int)next)) {
                XPathException e = new XPathException("Invalid XML character [x " + Integer.toHexString((int)next) + ']');
                e.setErrorCode("FOCH0001");
                if (context instanceof XPathContext) {
                    e.setXPathContext((XPathContext)context);
                }
                throw e;
            }
            if (next<65536) {
                sb.append((char)next);
            } else {  // output a surrogate pair
                sb.append(UTF16.highSurrogate((int)next));
                sb.append(UTF16.lowSurrogate((int)next));
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
