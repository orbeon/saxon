package org.orbeon.saxon.event;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.sort.IntSet;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.tinytree.CharSlice;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ComplexType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.value.Whitespace;

import javax.xml.transform.OutputKeys;
import java.util.Arrays;
import java.util.Properties;
import java.util.StringTokenizer;

/**
* XMLIndenter: This ProxyReceiver indents elements, by adding character data where appropriate.
* The character data is always added as "ignorable white space", that is, it is never added
* adjacent to existing character data.
*
* @author Michael Kay
*/


public class XMLIndenter extends ProxyReceiver {

    private int level = 0;
    private int indentSpaces = 3;
    //private String indentChars = "\n                                                          ";
    private char[] indentChars = {'\n', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};
    private boolean sameline = false;
    private boolean afterStartTag = false;
    private boolean afterEndTag = true;
    private boolean allWhite = true;
    private int line = 0;       // line and column measure the number of lines and columns
    private int column = 0;     // .. in whitespace text nodes between tags
    private int suppressedAtLevel = -1;
    private int xmlspace;
    private IntSet suppressedElements = null;
    private IntSet doubleSpacedElements = null;

    /**
     * Create an XML Indenter
     */

    public XMLIndenter() {
    }

    /**
     * Set the properties for this indenter
     * @param props the serialization properties
    */

    public void setOutputProperties(Properties props) {
        String s = props.getProperty(SaxonOutputKeys.INDENT_SPACES);
        if (s==null) {
            indentSpaces = 3;
        } else {
            try {
                indentSpaces = Integer.parseInt(Whitespace.trim(s));
            } catch (NumberFormatException err) {
                indentSpaces = 3;
            }
        }
        String omit = props.getProperty(OutputKeys.OMIT_XML_DECLARATION);
        afterEndTag = omit==null || !Whitespace.trim(omit).equals("yes") ||
                    props.getProperty(OutputKeys.DOCTYPE_SYSTEM)!=null ;
        s = props.getProperty(SaxonOutputKeys.SUPPRESS_INDENTATION);
        if (s != null) {
            suppressedElements = new IntHashSet(8);
            NamePool pool = getNamePool();
            StringTokenizer st = new StringTokenizer(s, " \t\r\n");
            while (st.hasMoreTokens()) {
                String clarkName = st.nextToken();
                int fp = pool.allocateClarkName(clarkName);
                suppressedElements.add(fp);
            }
        }
        s = props.getProperty(SaxonOutputKeys.DOUBLE_SPACE);
        if (s != null) {
            doubleSpacedElements = new IntHashSet(8);
            NamePool pool = getNamePool();
            StringTokenizer st = new StringTokenizer(s, " \t\r\n");
            while (st.hasMoreTokens()) {
                String clarkName = st.nextToken();
                int fp = pool.allocateClarkName(clarkName);
                doubleSpacedElements.add(fp);
            }
        }
    }

    /**
    * Start of document
    */

    public void open() throws XPathException {
        nextReceiver.open();
        //xmlspace = getNamePool().allocate("xml", NamespaceConstant.XML, "space") & 0xfffff;
        xmlspace = StandardNames.XML_SPACE;
    }

    /**
    * Output element start tag
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (afterStartTag || afterEndTag) {
            if (doubleSpacedElements != null && doubleSpacedElements.contains(nameCode&NamePool.FP_MASK)) {
                nextReceiver.characters("\n", 0, 0);
                line = 0;
                column = 0;
            }
            indent();
        }
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
        level++;
        sameline = true;
        afterStartTag = true;
        afterEndTag = false;
        allWhite = true;
        line = 0;
        SchemaType type;
        if (suppressedElements != null && suppressedElements.contains(nameCode&NamePool.FP_MASK)) {
            suppressedAtLevel = level;
        }
        if (typeCode >= 1024 && suppressedAtLevel < 0 &&
                ((type = getConfiguration().getSchemaType(typeCode)) != null && type.isComplexType() &&
                        ((ComplexType)type).isMixedContent())) {
            // suppress indentation for elements with mixed content. (Note this also suppresses
            // indentation for all descendants of such elements. We could be smarter than this.)
            suppressedAtLevel = level;
        }
    }

    /**
    * Output an attribute
    */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        if ((nameCode & NamePool.FP_MASK) == xmlspace && value.equals("preserve") && suppressedAtLevel < 0) {
            // Note, we are suppressing indentation within an xml:space="preserve" region even if a descendant
            // specifies xml:space="default
            suppressedAtLevel = level;
        }
        nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
    * Output element end tag
    */

    public void endElement() throws XPathException {
        level--;
        if (afterEndTag && !sameline) {
            indent();
        }
        nextReceiver.endElement();
        sameline = false;
        afterEndTag = true;
        afterStartTag = false;
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
        if (afterEndTag) {
            indent();
        }
        nextReceiver.processingInstruction(target, data, locationId, properties);
        afterStartTag = false;
        afterEndTag = false;
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
        nextReceiver.characters(chars, locationId, properties);
        if (!allWhite) {
            afterStartTag = false;
            afterEndTag = false;
        }
    }

    /**
    * Output a comment
    */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        if (afterEndTag) {
            indent();
        }
        nextReceiver.comment(chars, locationId, properties);
        afterStartTag = false;
        afterEndTag = false;
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
        if (line>0) {
            spaces -= column;
            if (spaces <= 0) {
                return;     // there's already enough white space, don't add more
            }
        }
        if (spaces+2 >= indentChars.length) {
            int increment = 5 * indentSpaces;
            if (spaces + 2 > indentChars.length + increment) {
                increment += spaces + 2;
            }
            char[] c2 = new char[indentChars.length + increment];
            System.arraycopy(indentChars, 0, c2, 0, indentChars.length);
            Arrays.fill(c2, indentChars.length, c2.length, ' ');
            indentChars = c2;
        }
        // output the initial newline character only if line==0
        int start = (line == 0 ? 0 : 1);
        //super.characters(indentChars.subSequence(start, start+spaces+1), 0, ReceiverOptions.NO_SPECIAL_CHARS);
        nextReceiver.characters(new CharSlice(indentChars, start, spaces+1), 0, ReceiverOptions.NO_SPECIAL_CHARS);
        sameline = false;
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

