package org.orbeon.saxon.sort;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.xpath.XPathException;
import java.util.Comparator;

/**
 * A Comparer used for comparing sort keys when data-type="text". The items to be
 * compared are converted to strings, and the strings are then compared using an
 * underlying collator
 *
 * @author Michael H. Kay
 *
 */

public class TextComparer implements Comparator, java.io.Serializable {

    private Comparator collator;

    public TextComparer(Comparator collator) {
        this.collator = collator;
    }

    /**
    * Compare two Items by converting them to strings and comparing the string values.
    * @param a the first Item to be compared.
    * @param b the second Item to be compared.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not Items, or are items that cannot be convered
    * to strings (e.g. QNames)
    */

    public int compare(Object a, Object b) throws ClassCastException {

        String s1, s2;

        try {
            s1 = (a instanceof String ? (String)a : ((Item)a).getStringValue());
        } catch (XPathException err) {
            throw new ClassCastException("Cannot convert sort key from " + a.getClass() + " to a string");
        }

        try {
            s2 = (b instanceof String ? (String)b : ((Item)b).getStringValue());
        } catch (XPathException err) {
            throw new ClassCastException("Cannot convert sort key from " + b.getClass() + " to a string");
        }

        int x = collator.compare(s1, s2);
        //System.err.println(((RuleBasedCollator)collator).getStrength() + " comparing " + s1 + " with " + s2 + " => " + x);
        return x;

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
// The Initial Developer of this module is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//