package org.orbeon.saxon.event;
import org.orbeon.saxon.trans.XPathException;


/**
  * MessageEmitter is the default Receiver for xsl:message output.
  * It is the same as XMLEmitter except for an extra newline at the end of the message
  */
  
public class MessageEmitter extends XMLEmitter
{
    public void endDocument() throws XPathException {
        try {
            writer.write('\n');
        } catch (java.io.IOException err) {
            throw new XPathException(err);
        }
        super.close();
    }

    public void close() throws XPathException {
        try {
            if (writer != null) {
                writer.flush();
            }
        } catch (java.io.IOException err) {
            throw new XPathException(err);
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
