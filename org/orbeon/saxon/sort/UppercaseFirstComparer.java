package net.sf.saxon.sort;
import java.text.Collator;
import java.util.Comparator;

/**
 * A Comparer used for comparing keys
 *
 * @author Michael H. Kay
 *
 */

public class UppercaseFirstComparer implements Comparator, java.io.Serializable {

    private Collator baseCollator;

    public UppercaseFirstComparer(Collator base) {
        baseCollator = base;
        baseCollator.setStrength(Collator.SECONDARY);
    }

    /**
    * Compare two string objects: case is irrelevant, unless the strings are equal ignoring
    * case, in which case uppercase comes first.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects do not implement the CharSequence interface
    */

    public int compare(Object a, Object b) {
        int diff = baseCollator.compare(a, b);
        if (diff != 0) {
            return diff;
        }

        CharSequence a1 = (CharSequence)a;
        CharSequence b1 = (CharSequence)b;
        for (int i=0; i<a1.length(); i++) {
            if (a1.charAt(i) != b1.charAt(i)) {
                return (Character.isUpperCase(a1.charAt(i)) ? -1 : +1);
            }
        }
        return 0;


//        char[] a1 = ((String)a).toCharArray();
//        char[] b1 = ((String)b).toCharArray();
//        int alen = a1.length;
//        // the strings must be the same length, or we wouldn't have got this far
//        int i = 0;
//        int j = 0;
//
//        while (i < alen) {
//            if (i==alen) return 0;
//            diff = a1[i++] - b1[j++];
//            if (diff!=0) {
//                return (Character.isUpperCase(a1[i-1]) ? -1 : +1);
//            }
//        }

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
// The Initial Developer of this module is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//