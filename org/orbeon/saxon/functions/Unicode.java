package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.NumericValue;
import org.orbeon.saxon.value.StringValue;

/**
 * This class supports the two functions string-to-codepoints() and codepoints-to-string()
 */

public class Unicode extends SystemFunction {

    public static final int TO_CODEPOINTS = 0;
    public static final int FROM_CODEPOINTS = 1;

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     */

    public Expression preEvaluate(StaticContext env) throws XPathException {
        switch (operation) {
        case TO_CODEPOINTS:
            return super.preEvaluate(env);
        case FROM_CODEPOINTS:
                final XPathContext context = env.makeEarlyEvaluationContext();
                return StringValue.makeStringValue(
                    unicodeToString(argument[0].iterate(context), context));
        default:
            throw new UnsupportedOperationException("Unknown Unicode operation");
        }
    }

    /**
    * Evaluate
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        switch (operation) {
        case TO_CODEPOINTS:
            throw new UnsupportedOperationException("Cannot call evaluateItem on a sequence");
        case FROM_CODEPOINTS:
            return StringValue.makeStringValue(unicodeToString(argument[0].iterate(c), c));
        default:
            throw new UnsupportedOperationException("Unknown Unicode operation");
        }
    }

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        switch (operation) {
        case TO_CODEPOINTS:
            Item item = argument[0].evaluateItem(c);
            if (item==null) {
                return EmptyIterator.getInstance();
            }
            return stringToUnicode(item.getStringValueCS());
        case FROM_CODEPOINTS:
            return SingletonIterator.makeIterator(evaluateItem(c));
        default:
            throw new UnsupportedOperationException("Unknown Unicode operation");
        }
    }

    /**
    * Return a sequence of integers representing the Unicode code values of the characters in a given
    * string.
    */

    private static SequenceIterator stringToUnicode(CharSequence s) {
        return StringValue.makeStringValue(s).iterateCharacters();
    }

    /**
    * Return the Unicode string corresponding to a given sequence of Unicode code values
     * @param chars iterator delivering the characters as integer values
     * @param context the evaluation context
     * @throws XPathException if any of the integers is not the codepoint of a valid XML character
    */

    private static CharSequence unicodeToString(SequenceIterator chars, XPathContext context) throws XPathException {
        FastStringBuffer sb = new FastStringBuffer(256);
        NameChecker checker = context.getConfiguration().getNameChecker();
        while (true) {
            NumericValue nextInt = (NumericValue)chars.next();
            if (nextInt == null) {
                return sb.condense();
            }
            long next = nextInt.longValue();
            if (next < 0 || next > Integer.MAX_VALUE || !checker.isValidChar((int)next)) {
                DynamicError e = new DynamicError("Invalid XML character [x " + Integer.toHexString((int)next) + ']');
                e.setErrorCode("FOCH0001");
                if (context instanceof XPathContext) {
                    e.setXPathContext((XPathContext)context);
                }
                throw e;
            }
            if (next<65536) {
                sb.append((char)next);
            } else {  // output a surrogate pair
                sb.append(XMLChar.highSurrogate((int)next));
                sb.append(XMLChar.lowSurrogate((int)next));
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
