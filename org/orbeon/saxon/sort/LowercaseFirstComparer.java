package net.sf.saxon.sort;
import java.text.Collator;
import java.util.Comparator;

/**
 * A Comparer used for comparing keys
 *
 * @author Michael H. Kay
 *
 */

public class LowercaseFirstComparer implements Comparator, java.io.Serializable {
    
    private Collator baseCollator;
    
    public LowercaseFirstComparer(Collator base) {
        baseCollator = base;
        baseCollator.setStrength(Collator.SECONDARY);
    }

    /**
    * Compare two string objects: case is irrelevant, unless the strings are equal ignoring
    * case, in which case lowercase comes first.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are of the wrong type for this Comparer
    */

    public int compare(Object a, Object b) {

        int diff = baseCollator.compare(a, b);
        if (diff != 0) {
            return diff;
        }
        char[] a1 = ((String)a).toCharArray();
        char[] b1 = ((String)b).toCharArray();
        int alen = a1.length;
        int blen = b1.length;
        int i = 0;
        int j = 0;

        while (true) {
            if (i==alen) return 0;
            diff = a1[i++] - b1[j++]; 
            if (diff!=0) {
                return (Character.isLowerCase(a1[i-1]) ? -1 : +1);
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
// The Initial Developer of this module is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved. 
//
// Contributor(s): none. 
//