package net.sf.saxon.sort;
import net.sf.saxon.om.NodeInfo;

import java.io.Serializable;

/**
 * A Comparer used for comparing nodes in document order. This
 * comparer assumes that the nodes being compared come from the same document
 *
 * @author Michael H. Kay
 *
 */

public final class LocalOrderComparer implements NodeOrderComparer, Serializable {

    private static LocalOrderComparer instance = new LocalOrderComparer();

    /**
    * Get an instance of a LocalOrderComparer. The class maintains no state
    * so this returns the same instance every time.
    */

    public static LocalOrderComparer getInstance() {
        return instance;
    }

    public int compare(NodeInfo a, NodeInfo b) {
        NodeInfo n1 = (NodeInfo)a;
        NodeInfo n2 = (NodeInfo)b;
        return n1.compareOrder(n2);
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