package net.sf.saxon.sort;
import java.util.Comparator;

/**
 * A Comparer used for comparing descending keys
 *
 *
 */

public class DescendingComparer implements Comparator, java.io.Serializable {

    private Comparator baseComparer;

    public DescendingComparer(Comparator base) {
        baseComparer = base;
    }

    /**
    * Compare two objects.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are of the wrong type for this Comparer
    */

    public int compare(Object a, Object b) {
        return 0 - baseComparer.compare(a, b);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//