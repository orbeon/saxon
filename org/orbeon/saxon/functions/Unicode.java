package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.StringValue;


public class Unicode extends SystemFunction {

    public static final int TO_CODEPOINTS = 0;
    public static final int FROM_CODEPOINTS = 1;

    /**
    * Evaluate
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        switch (operation) {
        case TO_CODEPOINTS:
            throw new UnsupportedOperationException("Cannot call evaluateItem on a sequence");
        case FROM_CODEPOINTS:
            return new StringValue(unicodeToString(argument[0].iterate(c), c));
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
        return new StringValue(s).iterateCharacters();
    }

    /**
    * Return the Unicode string corresponding to a given sequence of Unicode code values
    */

    private static CharSequence unicodeToString(SequenceIterator chars, XPathContext context) throws XPathException {
        FastStringBuffer sb = new FastStringBuffer(256);
        while (true) {
            NumericValue nextInt = (NumericValue)chars.next();
            if (nextInt == null) {
                return sb.condense();
            }
            long next = nextInt.longValue();
            if (next > Integer.MAX_VALUE || !XMLChar.isValid((int)next)) {
                DynamicError e = new DynamicError("Invalid XML character [decimal " + next + ']');
                e.setErrorCode("FOCH0001");
                e.setXPathContext(context);
                throw e;
            }
            if (next<65536) {
                sb.append((char)next);
            }
            else {  // output a surrogate pair
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
