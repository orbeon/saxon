package org.orbeon.saxon.pull;

import org.orbeon.saxon.instruct.ElementCreator;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.type.Type;

/**
 * An element node whose construction is deferred.
 */

public class UnconstructedElement extends UnconstructedParent {

    private int nameCode;

    public UnconstructedElement(ElementCreator instruction, XPathContext context) {
        super(instruction, context);
    }

    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    /**
     * Get name code. The name code is a coded form of the node name: two nodes
     * with the same name code have the same namespace URI, the same local name,
     * and the same prefix. By masking the name code with &0xfffff, you get a
     * fingerprint: two nodes with the same fingerprint have the same local name
     * and namespace URI.
     *
     * @return an integer name code, which may be used to obtain the actual node
     *         name from the name pool
     * @see net.sf.saxon.om.NamePool#allocate allocate
     * @see net.sf.saxon.om.NamePool#getFingerprint getFingerprint
     */

    public int getNameCode() {
        return nameCode;
    }

    public int getNodeKind() {
        return Type.ELEMENT;
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
