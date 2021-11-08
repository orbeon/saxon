package org.orbeon.saxon.event;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.tinytree.CharSlice;

import java.util.Properties;
import java.util.Arrays;

/**
* HTMLIndenter: This ProxyEmitter indents HTML elements, by adding whitespace
* character data where appropriate.
* The character data is never added when within an inline element.
* The string used for indentation defaults to three spaces, but may be set using the
* indent-chars property
*
* @author Michael Kay
*/


public class HTMLIndenter extends ProxyReceiver {

    private int level = 0;
    private int indentSpaces = 3;
    //private String indentChars = "                                                          ";
    private char[] indentChars = {'\n', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};
    private boolean sameLine = false;
    private boolean isInlineTag = false;
    private boolean inFormattedTag = false;
    private boolean afterInline = false;
    private boolean afterFormatted = true;    // to prevent a newline at the start
    private int[] propertyStack = new int[20];


    // the list of inline tags is from the HTML 4.0 (loose) spec. The significance is that we
    // mustn't add spaces immediately before or after one of these elements.

    protected static String[] inlineTags = {
        "tt", "i", "b", "u", "s", "strike", "big", "small", "em", "strong", "dfn", "code", "samp",
         "kbd", "var", "cite", "abbr", "acronym", "a", "img", "applet", "object", "font",
         "basefont", "br", "script", "map", "q", "sub", "sup", "span", "bdo", "iframe", "input",
         "select", "textarea", "label", "button", "ins", "del" };

        // INS and DEL are not actually inline elements, but the SGML DTD for HTML
        // (apparently) permits them to be used as if they were.

    private static HTMLTagHashSet inlineTable = new HTMLTagHashSet(101);

    static {
        for (int j=0; j<inlineTags.length; j++) {
            inlineTable.add(inlineTags[j]);
        }
    }

    protected static final int IS_INLINE = 1;
    protected static final int IS_FORMATTED = 2;

    /**
     * Classify an element name as inline, formatted, or both or neither.
     * This method is overridden in the XHTML indenter
     * @param nameCode the element name
     * @return a bit-significant integer containing flags IS_INLINE and/or IS_FORMATTED
     */
    protected int classifyTag(int nameCode) {
        int r = 0;
        String tag = getNamePool().getDisplayName(nameCode);
        if (inlineTable.contains(tag)) {
            r |= IS_INLINE;
        }
        if (formattedTable.contains(tag)) {
            r |= IS_FORMATTED;
        }
        return r;
    }

    // Table of preformatted elements

    private static HTMLTagHashSet formattedTable = new HTMLTagHashSet(23);
    protected static String[] formattedTags = {"pre", "script", "style", "textarea", "xmp"};
                                    // "xmp" is obsolete but still encountered!

    static {
        for (int i=0; i<formattedTags.length; i++) {
            formattedTable.add(formattedTags[i]);
        }
    }

    public HTMLIndenter() {
    }

    /**
    * Set the properties for this indenter
    */

    public void setOutputProperties(Properties props) {
        String s = props.getProperty(SaxonOutputKeys.INDENT_SPACES);
        if (s==null) {
            indentSpaces = 3;
        } else {
            try {
                indentSpaces = Integer.parseInt(s);
            } catch (NumberFormatException err) {
                indentSpaces = 3;
            }
        }
    }

    /**
    * Output element start tag
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        int tagProps = classifyTag(nameCode);
        if (level >= propertyStack.length) {
            int[] p2 = new int[level*2];
            System.arraycopy(propertyStack, 0, p2, 0, propertyStack.length);
            propertyStack = p2;
        }
        propertyStack[level] = tagProps;
        isInlineTag = (tagProps & IS_INLINE) != 0;
        inFormattedTag = inFormattedTag || ((tagProps & IS_FORMATTED) != 0);
        if (!isInlineTag && !inFormattedTag &&
             !afterInline && !afterFormatted) {
            indent();
        }

        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
        level++;
        sameLine = true;
        afterInline = false;
        afterFormatted = false;
    }

    /**
    * Output element end tag
    */

    public void endElement() throws XPathException {
        level--;
        boolean thisInline = (propertyStack[level] & IS_INLINE) != 0;
        boolean thisFormatted = (propertyStack[level] & IS_FORMATTED) != 0;
        if (!thisInline && !thisFormatted && !afterInline &&
                 !sameLine && !afterFormatted && !inFormattedTag) {
            indent();
            afterInline = false;
            afterFormatted = false;
        } else {
            afterInline = thisInline;
            afterFormatted = thisFormatted;
        }
        nextReceiver.endElement();
        inFormattedTag = inFormattedTag && !thisFormatted;
        sameLine = false;
    }

    /**
    * Output character data
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (inFormattedTag || (properties & ReceiverOptions.USE_NULL_MARKERS) != 0) {
            // don't split the text if in a tag such as <pre>, or if the text contains the result of
            // expanding a character map
            nextReceiver.characters(chars, locationId, properties);
        } else {
            // otherwise try to split long lines into multiple lines
            int lastNL = 0;
            for (int i=0; i<chars.length(); i++) {
                if (chars.charAt(i)=='\n' || (i-lastNL > 120 && chars.charAt(i)==' ')) {
                    sameLine = false;
                    nextReceiver.characters(chars.subSequence(lastNL, i), locationId, properties);
                    indent();
                    lastNL = i+1;
                    while (lastNL<chars.length() && chars.charAt(lastNL)==' ') {
                        lastNL++;
                    }
                }
            }
            if (lastNL < chars.length()) {
                nextReceiver.characters(chars.subSequence(lastNL, chars.length()), locationId, properties);
            }
        }
        afterInline = false;
    }

    /**
    * Output a comment
    */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        indent();
        nextReceiver.comment(chars, locationId, properties);
    }

    /**
    * Output white space to reflect the current indentation level
    */

    private void indent() throws XPathException {
        int spaces = level * indentSpaces;
        if (spaces+1 >= indentChars.length) {
            int increment = 5 * indentSpaces;
            if (spaces + 1 > indentChars.length + increment) {
                increment += spaces + 1;
            }
            char[] c2 = new char[indentChars.length + increment];
            System.arraycopy(indentChars, 0, c2, 0, indentChars.length);
            Arrays.fill(c2, indentChars.length, c2.length, ' ');
            indentChars = c2;
        }
        nextReceiver.characters(new CharSlice(indentChars, 0, spaces+1), 0, 0);
        sameLine = false;
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

