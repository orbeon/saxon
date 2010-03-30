package org.orbeon.saxon.s9api;

import javax.xml.transform.SourceLocator;

/**
 * A user-written implementation of the MessageListener interface may be registered with the XsltTransformer
 * to receive notification of xsl:message output. Each xsl:message instruction that is evaluated results in
 * a single call to the MessageListener
 */
public interface MessageListener {

    /**
     * Notify a message written using the <code>xsl:message</code> instruction
     * @param content a document node representing the message content. Note that the output of
     * <code>xsl:message</code> is always an XML document node. It can be flattened to obtain the 
     * string value if required by calling <code>getStringValue()</code>.
     * @param terminate Set to true if <code>terminate="yes"</code> was specified or to false otherwise.
     * The message listener does not need to take any special action based on this parameter, but the information
     * is available if required. If <code>terminate="yes"</code> was specified, then the transformation will abort
     * with an exception immediately on return from this callback.
     * @param locator an object that contains the location of the <code>xsl:message</code> instruction in the
     * stylesheet that caused this message to be output. This provides access to the URI of the stylesheet module
     * and the line number of the <code>xsl:message</code> instruction.
     */

    public void message(XdmNode content, boolean terminate, SourceLocator locator);
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

