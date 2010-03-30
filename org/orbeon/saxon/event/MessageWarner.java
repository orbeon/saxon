package org.orbeon.saxon.event;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import java.io.StringWriter;


/**
 * MessageWarner is a user-selectable receiver for XSLT xsl:message output. It causes xsl:message output
 * to be notified to the warning() method of the JAXP ErrorListener, or to the error() method if
 * terminate="yes" is specified. This behaviour is specified in recent versions of the JAXP interface
 * specifications, but it is not the default behaviour, for backwards compatibility reasons.
 *
 * <p>The text of the message that is sent to the ErrorListener is an XML serialization of the actual
 * message content.</p>
  */
  
public class MessageWarner extends XMLEmitter {

    boolean abort = false;

    public void startDocument(int properties) throws XPathException {
        setWriter(new StringWriter());
        abort = (properties & ReceiverOptions.TERMINATE) != 0;
        super.startDocument(properties);
    }

    public void endDocument() throws XPathException {
        ErrorListener listener = getPipelineConfiguration().getErrorListener();
        XPathException de = new XPathException(getWriter().toString());
        de.setErrorCode("XTMM9000");
        try {
            if (abort) {
                listener.error(de);
            } else {
                listener.warning(de);
            }
        } catch (TransformerException te) {
            throw XPathException.makeXPathException(te);
        }
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
