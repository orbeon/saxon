package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.sort.IntToIntHashMap;



public class Translate extends SystemFunction {

    private IntToIntHashMap staticMap = null;
            // if the second and third arguments are known statically, we build a hash table for fast
            // lookup at run-time.

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        Expression e = super.typeCheck(env, contextItemType);
        if (e == this && argument[1] instanceof StringValue && argument[2] instanceof StringValue) {
            // second and third arguments known statically: build an index
            staticMap = buildMap((StringValue)argument[1], (StringValue)argument[2]);
        }
        return e;
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        StringValue sv1 = (StringValue)argument[0].evaluateItem(context);
        if (sv1==null) {
            return StringValue.EMPTY_STRING;
        };

        if (staticMap != null) {
            CharSequence in = sv1.getStringValueCS();
            CharSequence sb = translateUsingMap(in, staticMap);
            return new StringValue(sb);
        }

        StringValue sv2 = (StringValue)argument[1].evaluateItem(context);

        StringValue sv3 = (StringValue)argument[2].evaluateItem(context);

        return StringValue.makeStringValue(translate(sv1, sv2, sv3));
    }

    /**
    * Perform the translate function
    */

    private static CharSequence translate(StringValue sv0, StringValue sv1, StringValue sv2) {

        // if any string contains surrogate pairs, expand everything to 32-bit characters
        if (sv0.containsSurrogatePairs() || sv1.containsSurrogatePairs() || sv2.containsSurrogatePairs()) {
            return translateUsingMap(sv0.getStringValueCS(), buildMap(sv1, sv2));
        }

        // if the size of the strings is above some threshold, use a hash map to avoid O(n*m) performance
        if (sv0.getLength() * sv1.getLength() > 60) {
            // TODO: make measurements to get the optimum cut-off point
            return translateUsingMap(sv0.getStringValueCS(), buildMap(sv1, sv2));
        }

        CharSequence cs0 = sv0.getStringValueCS();
        CharSequence cs1 = sv1.getStringValueCS();
        CharSequence cs2 = sv2.getStringValueCS();

        String st1 = cs1.toString();
        FastStringBuffer sb = new FastStringBuffer(cs0.length());
        int s2len = cs2.length();
        int s0len = cs0.length();
        for (int i=0; i<s0len; i++) {
            char c = cs0.charAt(i);
            int j = st1.indexOf(c);
            if (j<s2len) {
                sb.append(( j<0 ? c : cs2.charAt(j) ));
            }
        }
        return sb;
    }

    /**
    * Perform the translate function when surrogate pairs are in use
    */

//    private static CharSequence slowTranslate(StringValue sv0, StringValue sv1, StringValue sv2) {
//        int[] a0 = sv0.expand();
//        int[] a1 = sv1.expand();
//        int[] a2 = sv2.expand();
//        FastStringBuffer sb = new FastStringBuffer(a0.length);
//        for (int i=0; i<a0.length; i++) {
//            int c = a0[i];
//            int j = -1;
//            for (int test=0; test<a1.length; test++) {
//                if (a1[test]==c) {
//                    j = test;
//                    break;
//                }
//            }
//            int newchar = -1;
//            if (j<0) {
//                newchar = a0[i];
//            } else if (j<a2.length) {
//                newchar = a2[j];
//            } else {
//                // no new character
//            }
//
//            if (newchar>=0) {
//                if (newchar<65536) {
//                    sb.append((char)newchar);
//                }
//                else {  // output a surrogate pair
//                    //To compute the numeric value of the character corresponding to a surrogate
//                    //pair, use this formula (all numbers are hex):
//            	    //(FirstChar - D800) * 400 + (SecondChar - DC00) + 10000
//                    newchar -= 65536;
//                    sb.append((char)((newchar / 1024) + 55296));
//                    sb.append((char)((newchar % 1024) + 56320));
//                }
//            }
//        }
//        return sb;
//    }

    private static IntToIntHashMap buildMap(StringValue arg1, StringValue arg2) {
        int[] a1 = arg1.expand();
        int[] a2 = arg2.expand();
        IntToIntHashMap map = new IntToIntHashMap(a1.length, 0.5);
            // allow plenty of free space, it's better for lookups (though worse for iteration)
        for (int i=0; i<a1.length; i++) {
            if (map.find(a1[i])) {
                // no action: duplicate
            } else {
                map.put(a1[i], (i>a2.length-1 ? -1 : a2[i]));
            }
        }
        return map;
    }

    private static CharSequence translateUsingMap(CharSequence in, IntToIntHashMap map) {
        int len = in.length();
        FastStringBuffer sb = new FastStringBuffer(len);
        for (int i=0; i<len; i++) {
            int charval;
            int c = in.charAt(i);
            if (c >= 55296 && c <= 56319) {
                // we'll trust the data to be sound
                charval = ((c - 55296) * 1024) + ((int) in.charAt(i + 1) - 56320) + 65536;
                i++;
            } else {
                charval = c;
            }
            int newchar = map.get(charval);
            if (newchar == Integer.MAX_VALUE) {
                // character not in map, so is not to be translated
                newchar = charval;
            }
            if (newchar == -1) {
                // no action, delete the character
            } else if (newchar < 65536) {
                sb.append((char)newchar);
            } else {  // output a surrogate pair
                //To compute the numeric value of the character corresponding to a surrogate
                //pair, use this formula (all numbers are hex):
                //(FirstChar - D800) * 400 + (SecondChar - DC00) + 10000
                newchar -= 65536;
                sb.append((char)((newchar / 1024) + 55296));
                sb.append((char)((newchar % 1024) + 56320));
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
