package net.sf.saxon.event;
import net.sf.saxon.xpath.XPathException;

import java.util.Properties;

/**
* HTMLIndenter: This ProxyEmitter indents HTML elements, by adding whitespace
* character data where appropriate.
* The character data is never added when within an inline element.
* The string used for indentation defaults to four spaces, but may be set using the
* indent-chars property
*
* @author Michael Kay
*/


public class HTMLIndenter extends ProxyReceiver {

    private int level = 0;
    private int indentSpaces = 3;
    private String indentChars = "                                                          ";
    private boolean sameLine = false;
    private boolean isInlineTag = false;
    private boolean inFormattedTag = false;
    private boolean afterInline = false;
    private boolean afterFormatted = true;    // to prevent a newline at the start


    // the list of inline tags is from the HTML 4.0 (loose) spec. The significance is that we
    // mustn't add spaces immediately before or after one of these elements.

    private static String[] inlineTags = {
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

    private static boolean isInline(String tag) {
        return inlineTable.contains(tag);
    }

    // Table of preformatted elements

    private static HTMLTagHashSet formattedTable = new HTMLTagHashSet(23);

    static {
        formattedTable.add("pre");
        formattedTable.add("script");
        formattedTable.add("style");
        formattedTable.add("textarea");
        formattedTable.add("xmp");          // obsolete but still encountered!
    }

    private static boolean isFormatted(String tag) {
        return formattedTable.contains(tag);
    }



    public HTMLIndenter() {}

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
            } catch (Exception err) {
                indentSpaces = 3;
            }
        }
    }

    /**
    * Start of document
    */

    //public void startDocument() throws XPathException {
    //    super.startDocument();
    //}

    /**
    * Output element start tag
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        String tag = getNamePool().getDisplayName(nameCode);
        isInlineTag = isInline(tag);
        inFormattedTag = inFormattedTag || isFormatted(tag);
        if (!isInlineTag && !inFormattedTag &&
             !afterInline && !afterFormatted) {
            indent();
        }


        super.startElement(nameCode, typeCode, locationId, properties);
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
        String tag = (String)((XMLEmitter)baseReceiver).elementStack.peek();
        boolean thisInline = isInline(tag);
        boolean thisFormatted = isFormatted(tag);
        if (!thisInline && !thisFormatted && !afterInline &&
                 !sameLine && !afterFormatted && !inFormattedTag) {
            indent();
            afterInline = false;
            afterFormatted = false;
        } else {
            afterInline = thisInline;
            afterFormatted = thisFormatted;
        }
        super.endElement();
        inFormattedTag = inFormattedTag && !thisFormatted;
        sameLine = false;
    }

    /**
    * Output character data
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (inFormattedTag) {
            super.characters(chars, locationId, properties);
        } else {
            int lastNL = 0;

            for (int i=0; i<chars.length(); i++) {
                if (chars.charAt(i)=='\n' || (i-lastNL > 120 && chars.charAt(i)==' ')) {
                    sameLine = false;
                    super.characters(chars.subSequence(lastNL, i), locationId, properties);
                    indent();
                    lastNL = i+1;
                    while (lastNL<chars.length() && chars.charAt(lastNL)==' ') {
                        lastNL++;
                    }
                }
            }
            if (lastNL < chars.length()) {
                super.characters(chars.subSequence(lastNL, chars.length()), locationId, properties);
            }
        }
        afterInline = false;
    }

    /**
    * Output a comment
    */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        indent();
        super.comment(chars, locationId, properties);
    }

    /**
    * End of document
    */

    //public void endDocument() throws XPathException {
    //   super.endDocument();
    //}

    /**
    * Output white space to reflect the current indentation level
    */

    private void indent() throws XPathException {
        int spaces = level * indentSpaces;
        while (spaces > indentChars.length()) {
            indentChars += indentChars;
        }
        super.characters("\n", 0, 0);
        super.characters(indentChars.subSequence(0, spaces), 0, 0);
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

