package org.orbeon.saxon.sort;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.type.Type;

import java.io.Serializable;

/**
 * A Comparer used for comparing nodes in document order. This
 * comparer is used when there is no guarantee that the nodes being compared
 * come from the same document
 *
 * @author Michael H. Kay
 *
 */

public final class GlobalOrderComparer implements NodeOrderComparer, Serializable {

    private static GlobalOrderComparer instance = new GlobalOrderComparer();

    /**
    * Get an instance of a GlobalOrderComparer. The class maintains no state
    * so this returns the same instance every time.
    */

    public static GlobalOrderComparer getInstance() {
        return instance;
    }

    public int compare(NodeInfo a, NodeInfo b) {
        NodeInfo r1 = a.getRoot();
        NodeInfo r2 = b.getRoot();
        if (r1.isSameNodeInfo(r2)) {
            return a.compareOrder(b);
        }
        int d1 = r1.getDocumentNumber();
        int d2 = r2.getDocumentNumber();
        return d1 - d2;
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