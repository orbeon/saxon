package net.sf.saxon.functions;
import net.sf.saxon.expr.Token;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.StringValue;

/**
 * This class implements the XPath substring() function
 */

public class Substring extends SystemFunction {

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue sv = (AtomicValue)argument[0].evaluateItem(context);
        if (sv==null) {
            sv = StringValue.EMPTY_STRING;
        }
        CharSequence s = sv.getStringValueCS();

        AtomicValue a1 = (AtomicValue)argument[1].evaluateItem(context);
        NumericValue a = (NumericValue)a1.getPrimitiveValue();

        if (argument.length==2) {
            return new StringValue(substring(s, a));
        } else {
            AtomicValue b2 = (AtomicValue)argument[2].evaluateItem(context);
            NumericValue b = (NumericValue)b2.getPrimitiveValue();
            return new StringValue(substring(s, a, b, context));
        }
    }

    /**
    * Implement substring function with two arguments.
    */

    private static CharSequence substring(CharSequence s, NumericValue start) {
        int slength = s.length();

        long lstart;
        if (start instanceof IntegerValue) {
            lstart = ((IntegerValue)start).longValue();
        } else {
            NumericValue rstart = start.round();
            // We need to be careful to handle cases such as plus/minus infinity
            if (rstart.compareTo(IntegerValue.ZERO) <= 0) {
                return s;
            } else if (rstart.compareTo(new IntegerValue(slength)) > 0) {
                // this works even where the string contains surrogate pairs,
                // because the Java length is always >= the XPath length
                return "";
            } else {
                try {
                    lstart = rstart.longValue();
                } catch (XPathException err) {
                    // this shouldn't happen unless the string length exceeds the bounds
                    // of a long
                    throw new AssertionError("string length out of permissible range");
                }
            }
        }

        int pos=1;
        int cpos=0;
        while (cpos<slength) {
            if (pos >= lstart) {
                return s.subSequence(cpos, s.length());
            }

            int ch = (int)s.charAt(cpos++);
            if (ch<55296 || ch>56319) pos++;    // don't count high surrogates, i.e. D800 to DBFF
        }
        return "";
    }

    /**
    * Implement substring function with three arguments.
    */

    private static CharSequence substring(CharSequence s, NumericValue start, NumericValue len, XPathContext context) {
        int slength = s.length();

        long lstart;
        if (start instanceof IntegerValue) {
            lstart = ((IntegerValue)start).longValue();
        } else {
            start = start.round();
            // We need to be careful to handle cases such as plus/minus infinity and NaN
            if (start.compareTo(IntegerValue.ZERO) <= 0) {
                lstart = 0;
            } else if (start.compareTo(new IntegerValue(slength)) > 0) {
                // this works even where the string contains surrogate pairs,
                // because the Java length is always >= the XPath length
                return "";
            } else if (start.isNaN()) {
                return "";
            } else {
                try {
                    lstart = start.longValue();
                } catch (XPathException err) {
                    // this shouldn't happen unless the string length exceeds the bounds
                    // of a long
                    throw new AssertionError("string length out of permissible range");
                }
            }
        }

        NumericValue end;
        try {
            end = start.arithmetic(Token.PLUS, len.round(), context);
        } catch (XPathException e) {
            throw new AssertionError("Unexpected arithmetic failure in substring");
        }
        long lend;
        if (end instanceof IntegerValue) {
            lend = ((IntegerValue)end).longValue();
        } else {
            // We need to be careful to handle cases such as plus/minus infinity and NaN
            if (end.compareTo(IntegerValue.ZERO) <= 0) {
                return "";
            } else if (end.isNaN()) {
                return "";
            } else if (end.compareTo(new IntegerValue(slength)) > 0) {
                // this works even where the string contains surrogate pairs,
                // because the Java length is always >= the XPath length
                lend = slength+1;
            } else {
                try {
                    lend = end.ceiling().longValue();
                } catch (XPathException err) {
                    // this shouldn't happen unless the string length exceeds the bounds
                    // of a long
                    throw new AssertionError("string length out of permissible range");
                }
            }
        }

        int jstart=-1;
        int jend=-1;
        int pos=1;
        int cpos=0;
        while (cpos<slength) {
            if (pos >= lstart) {
                if (pos < lend) {
                    if (jstart<0) {
                        jstart = cpos;
                    }
                } else {
                    jend = cpos;
                    break;
                }
            }

            int ch = (int)s.charAt(cpos++);
            if (ch<55296 || ch>56319) pos++;    // don't count high surrogates, i.e. D800 to DBFF
        }
        if (jstart<0 || jstart==jend) {
            return "";
        } else if (jend<0) {
            return s.subSequence(jstart, s.length());
        } else {
            return s.subSequence(jstart, jend);
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
