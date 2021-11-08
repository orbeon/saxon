package org.orbeon.saxon;

import javax.xml.transform.URIResolver;

/**
 * This is a marker interface: if a URIResolver implements this interface and returns null from
 * its resolve() method, then the standard URI resolver will not be invoked.
 * <p>
 * The main use case for this is to support protocols that the standard Java java.net.URL class
 * does not recognize. In the case of doc-available(), we want to return false, rather than throwing
 * an exception in such cases.
 */

public interface NonDelegatingURIResolver extends URIResolver {
    // marker interface only
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