package net.sf.saxon.event;

import net.sf.saxon.trans.XPathException;
import net.sf.saxon.om.FastStringBuffer;

/**
  * The CommentStripper class is a filter that removes all comments and processing instructions.
  * It also concatenates text nodes that are split by comments and PIs
  * @author Michael H. Kay
  */

  
public class CommentStripper extends ProxyReceiver {

    private FastStringBuffer buffer = new FastStringBuffer(200);

    /**
    * Default constructor for use in subclasses
    */
    
    public CommentStripper() {}
	

    public void startElement (int nameCode, int typeCode, int locationId, int properties)
    throws XPathException {
        flush();
        super.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endElement () throws XPathException {
        flush();
        super.endElement();
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException {
        buffer.append(chars);
    }

    /**
    * Remove comments
    */
    
    public void comment (CharSequence chars, int locationId, int properties) {}
    
    /**
    * Remove processing instructions
    */
    
    public void processingInstruction(String name, CharSequence data, int locationId, int properties) {}

    /**
    * Flush the character buffer
    */
    
    private void flush() throws XPathException {
        super.characters(buffer, 0, 0);
        buffer.setLength(0);
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
