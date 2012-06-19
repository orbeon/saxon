package org.orbeon.saxon.event;

import org.orbeon.saxon.om.NodeInfo;

/**
 * A CopyInformee is a Receiver that receives extra information while a tree is being copied. Specifically,
 * each time an element node is copied to the receiver, before calling the startElement() method, the copying
 * code will first call notifyElementNode(), giving the Receiver extra information about the element currently
 * being copied.
 */
public interface CopyInformee extends Receiver {

    /**
     * Provide information about the node being copied. This method is called immediately before
     * the startElement call for the element node in question.
     * @param element the node being copied, which must be an element node
     */

    public void notifyElementNode(NodeInfo element);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

