package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.xpath.XPathException;



public class Translate extends SystemFunction {

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue sv = (AtomicValue)argument[0].evaluateItem(context);
        if (sv==null) {
            sv = StringValue.EMPTY_STRING;
        };
        String s1 = sv.getStringValue();

        sv = (AtomicValue)argument[1].evaluateItem(context);
        String s2 = sv.getStringValue();

        sv = (AtomicValue)argument[2].evaluateItem(context);
        String s3 = sv.getStringValue();

        return new StringValue(translate(s1, s2, s3));
    }

    /**
    * Perform the translate function
    */

    private static CharSequence translate(String s0, String s1, String s2) {

        // check for surrogate pairs
        int len0 = StringValue.getLength(s0);
        int len1 = StringValue.getLength(s1);
        int len2 = StringValue.getLength(s2);
        if (s0.length()!=len0 ||
                s1.length()!=len1 ||
                s2.length()!=len2 ) {
            return slowTranslate(s0, s1, s2);
        }

        StringBuffer sb = new StringBuffer();
        int s2len = s2.length();
        for (int i=0; i<s0.length(); i++) {
            char c = s0.charAt(i);
            int j = s1.indexOf(c);
            if (j<s2len) {
                sb.append(( j<0 ? c : s2.charAt(j) ));
            }
        }
        return sb;
    }

    /**
    * Perform the translate function when surrogate pairs are in use
    */

    private static CharSequence slowTranslate(String s0, String s1, String s2) {
        int[] a0 = StringValue.expand(s0);
        int[] a1 = StringValue.expand(s1);
        int[] a2 = StringValue.expand(s2);
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<a0.length; i++) {
            int c = a0[i];
            int j = -1;
            for (int test=0; test<a1.length; test++) {
                if (a1[test]==c) {
                    j = test;
                    break;
                }
            }
            int newchar = -1;
            if (j<0) {
                newchar = a0[i];
            } else if (j<a2.length) {
                newchar = a2[j];
            } else {
                // no new character
            }

            if (newchar>=0) {
                if (newchar<65536) {
                    sb.append((char)newchar);
                }
                else {  // output a surrogate pair
                    //To compute the numeric value of the character corresponding to a surrogate
                    //pair, use this formula (all numbers are hex):
            	    //(FirstChar - D800) * 400 + (SecondChar - DC00) + 10000
                    newchar -= 65536;
                    sb.append((char)((newchar / 1024) + 55296));
                    sb.append((char)((newchar % 1024) + 56320));
                }
            }
        }
        return sb;
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
