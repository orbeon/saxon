package net.sf.saxon.event;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.xpath.XPathException;

import javax.xml.transform.OutputKeys;
import java.util.Properties;

/**
* XMLIndenter: This ProxyEmitter indents elements, by adding character data where appropriate.
* The character data is always added as "ignorable white space", that is, it is never added
* adjacent to existing character data.
*
* @author Michael Kay
*/


public final class XMLIndenter extends ProxyReceiver {

    private int level = 0;
    private int indentSpaces = 3;
    private String indentChars = "                                                          ";
    private boolean sameline = false;
    private boolean afterTag = true;
    private boolean allWhite = true;
    private int line = 0;       // line and column measure the number of lines and columns
    private int column = 0;     // .. in whitespace text nodes between tags
    private int suppressedAtLevel = -1;
    private int xmlspace;

    /**
    * Set the properties for this indenter
    */

    public void setOutputProperties(Properties props) {
        String s = props.getProperty(SaxonOutputKeys.INDENT_SPACES);
        if (s==null) {
            indentSpaces = 3;
        } else {
            try {
                indentSpaces = Integer.parseInt(s.trim());
            } catch (NumberFormatException err) {
                indentSpaces = 3;
            }
        }
        String omit = props.getProperty(OutputKeys.OMIT_XML_DECLARATION);
        afterTag = omit==null || !omit.trim().equals("yes") ||
                    props.getProperty(OutputKeys.DOCTYPE_SYSTEM)!=null ;
    }

    /**
    * Start of document
    */

    public void open() throws XPathException {
        super.open();
        xmlspace = getNamePool().allocate("xml", NamespaceConstant.XML, "space") & 0xfffff;
    }

    /**
    * Output element start tag
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (afterTag) {
            indent();
        }
        super.startElement(nameCode, typeCode, locationId, properties);
        level++;
        sameline = true;
        afterTag = true;
        allWhite = true;
        line = 0;
    }

    /**
    * Output an attribute
    */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        if ((nameCode & 0xfffff) == xmlspace && value.equals("preserve") && suppressedAtLevel < 0) {
            suppressedAtLevel = level;
        }
        super.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
    * Output element end tag
    */

    public void endElement() throws XPathException {
        level--;
        if (afterTag && !sameline) indent();
        super.endElement();
        sameline = false;
        afterTag = true;
        allWhite = true;
        line = 0;
        if (level == (suppressedAtLevel - 1)) {
            suppressedAtLevel = -1;
            // remove the suppression of indentation
        }
    }

    /**
    * Output a processing instruction
    */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        super.processingInstruction(target, data, locationId, properties);
        afterTag = true;
    }

    /**
    * Output character data
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        for (int i=0; i<chars.length(); i++) {
            char c = chars.charAt(i);
            if (c=='\n') {
                sameline = false;
                line++;
                column = 0;
            }
            if (!Character.isWhitespace(c)) {
                allWhite = false;
            }
            column++;
        }
        super.characters(chars, locationId, properties);
        if (!allWhite) {
            afterTag = false;
        }
    }

    /**
    * Output a comment
    */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        super.comment(chars, locationId, properties);
        afterTag = true;
    }

    /**
    * Output white space to reflect the current indentation level
    */

    private void indent() throws XPathException {
        if (suppressedAtLevel >= 0) {
            // indentation has been suppressed (e.g. by xmlspace="preserve")
            return;
        }
        int spaces = level * indentSpaces;
        if (line>0 && column>spaces) {
            return;     // there's already enough white space, don't add more
        }
        while (spaces > indentChars.length()) {
            indentChars += indentChars;
        }

        if (line == 0) {
            super.characters("\n", 0, 0);
        }
        super.characters(indentChars.subSequence(0, spaces), 0, 0);
        sameline = false;
    }

};

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

