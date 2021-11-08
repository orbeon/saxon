package org.orbeon.saxon.style;

import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.trans.XPathException;

/**
 * Extends the standard XPath static context with information that is available for
 * XPath expressions invoked from XSLT
 */

public interface XSLTStaticContext extends StaticContext {

   /**
    * Determine if an extension element is available
    * @throws org.orbeon.saxon.trans.XPathException if the name is invalid or the prefix is not declared
    */

    public boolean isElementAvailable(String qname) throws XPathException;

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
// Contributor(s): none.
//

