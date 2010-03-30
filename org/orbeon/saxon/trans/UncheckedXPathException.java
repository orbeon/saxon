package org.orbeon.saxon.trans;

/**
 * When tree construction is deferred, innocuous methods such as NodeInfo#getLocalName() may
 * trigger a dynamic error. Rather than make all such methods on NodeInfo throw a checked XPathException,
 * we instead throw an UncheckedXPathException, which is a simple wrapper for an XPathException.
 * Appropriate places in the client code must check for this condition and translate it back into an
 * XPathException.
 */

public class UncheckedXPathException extends RuntimeException {

    private XPathException cause;

    public UncheckedXPathException(XPathException cause) {
        this.cause = cause;
    }

    public XPathException getXPathException() {
        return cause;
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

