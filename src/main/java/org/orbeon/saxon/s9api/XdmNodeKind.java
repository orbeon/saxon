package org.orbeon.saxon.s9api;

import org.orbeon.saxon.type.Type;

/**
 * Enumeration class defining the seven kinds of node defined in the XDM model
 */
public enum XdmNodeKind {
    DOCUMENT (Type.DOCUMENT),
    ELEMENT (Type.ELEMENT),
    ATTRIBUTE (Type.ATTRIBUTE),
    TEXT (Type.TEXT),
    COMMENT (Type.COMMENT),
    PROCESSING_INSTRUCTION (Type.PROCESSING_INSTRUCTION),
    NAMESPACE (Type.NAMESPACE);

    private int number;
    private XdmNodeKind(int number) {
        this.number = number;
    }
    protected int getNumber() {
        return number;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

