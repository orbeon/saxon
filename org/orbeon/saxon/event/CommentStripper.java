package org.orbeon.saxon.event;

import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.tinytree.CompressedWhitespace;

/**
  * The CommentStripper class is a filter that removes all comments and processing instructions.
  * It also concatenates text nodes that are split by comments and PIs. This follows the rules for
  * processing stylesheets.
  * @author Michael H. Kay
  */


public class CommentStripper extends ProxyReceiver {

    private CompressedWhitespace savedWhitespace = null;
    private FastStringBuffer buffer = new FastStringBuffer(200);

    /**
    * Default constructor for use in subclasses
    */

    public CommentStripper() {}
    static int seq = 0;

    public void startElement (int nameCode, int typeCode, int locationId, int properties)
    throws XPathException {
        flush();
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endElement () throws XPathException {
        flush();
        nextReceiver.endElement();
    }

    /**
     * Handle a text node. Because we're often handling stylesheets on this path, whitespace text
     * nodes will often be stripped but we can't strip them immediately because of the case
     * [element]   [!-- comment --]text[/element], where the space before the comment is considered
     * significant. But it's worth going to some effort to avoid uncompressing the whitespace in the
     * more common case, so that it can easily be detected and stripped downstream.
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException {
        if (chars instanceof CompressedWhitespace) {
            if (buffer.length() == 0 && savedWhitespace == null) {
                savedWhitespace = (CompressedWhitespace)chars;
            } else {
                ((CompressedWhitespace)chars).uncompress(buffer);
            }
        } else {
            if (savedWhitespace != null) {
                savedWhitespace.uncompress(buffer);
                savedWhitespace = null;
            }
            buffer.append(chars);
        }

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
        if (buffer.length() > 0) {
            nextReceiver.characters(buffer, 0, 0);
        } else if (savedWhitespace != null) {
            nextReceiver.characters(savedWhitespace, 0, 0);
        }
        savedWhitespace = null;
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
