package org.orbeon.saxon.trans;
import javax.xml.transform.SourceLocator;

/**
 * Exception used for static errors in XPath, XSLT, or XQuery
 * @deprecated since 9.0: use the superclass, {@link XPathException}
*/

public class StaticError extends XPathException {

    public StaticError(String message) {
        super(message);
    }

    public StaticError(Exception err) {
        super(err);
    }

    public StaticError(String message, Throwable err) {
        super(message, err);
    }

    public StaticError(String message, SourceLocator loc) {
        super(message, loc);
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

