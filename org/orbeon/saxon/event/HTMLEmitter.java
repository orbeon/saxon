package org.orbeon.saxon.event;
import org.orbeon.saxon.tinytree.CompressedWhitespace;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;

import javax.xml.transform.OutputKeys;

/**
  * This class generates HTML output
  * @author Michael H. Kay
  */

public class HTMLEmitter extends XMLEmitter {

	/**
	* Preferred character representations
	*/

    protected static final int REP_NATIVE = 0;
	protected static final int REP_ENTITY = 1;
	protected static final int REP_DECIMAL = 2;
	protected static final int REP_HEX = 3;

	protected int nonASCIIRepresentation = REP_NATIVE;
	protected int excludedRepresentation = REP_ENTITY;

	private int inScript;
	private boolean started = false;
	private String elementName;
    private short uriCode;

	/**
	 * Decode preferred representation
     * @param rep string containing preferred representation (native, entity, decimal, or hex)
     * @return integer code for the preferred representation
	*/

	private static int representationCode(String rep) {
		if (rep.equalsIgnoreCase("native")) return REP_NATIVE;
		if (rep.equalsIgnoreCase("entity")) return REP_ENTITY;
		if (rep.equalsIgnoreCase("decimal")) return REP_DECIMAL;
		if (rep.equalsIgnoreCase("hex")) return REP_HEX;
		return REP_ENTITY;
	}

    /**
    * Table of HTML tags that have no closing tag
    */

    static HTMLTagHashSet emptyTags = new HTMLTagHashSet(31);

    static {
        setEmptyTag("area");
        setEmptyTag("base");
        setEmptyTag("basefont");
        setEmptyTag("br");
        setEmptyTag("col");
        setEmptyTag("frame");
        setEmptyTag("hr");
        setEmptyTag("img");
        setEmptyTag("input");
        setEmptyTag("isindex");
        setEmptyTag("link");
        setEmptyTag("meta");
        setEmptyTag("param");
    }

    private static void setEmptyTag(String tag) {
        emptyTags.add(tag);
    }

    protected static boolean isEmptyTag(String tag) {
        return emptyTags.contains(tag);
    }

    /**
    * Table of boolean attributes
    */

    // we use two HashMaps to avoid unnecessary string concatenations

    private static HTMLTagHashSet booleanAttributes = new HTMLTagHashSet(31);
    private static HTMLTagHashSet booleanCombinations = new HTMLTagHashSet(53);

    static {
        setBooleanAttribute("area", "nohref");
        setBooleanAttribute("button", "disabled");
        setBooleanAttribute("dir", "compact");
        setBooleanAttribute("dl", "compact");
        setBooleanAttribute("frame", "noresize");
        setBooleanAttribute("hr", "noshade");
        setBooleanAttribute("img", "ismap");
        setBooleanAttribute("input", "checked");
        setBooleanAttribute("input", "disabled");
        setBooleanAttribute("input", "readonly");
        setBooleanAttribute("menu", "compact");
        setBooleanAttribute("object", "declare");
        setBooleanAttribute("ol", "compact");
        setBooleanAttribute("optgroup", "disabled");
        setBooleanAttribute("option", "selected");
        setBooleanAttribute("option", "disabled");
        setBooleanAttribute("script", "defer");
        setBooleanAttribute("select", "multiple");
        setBooleanAttribute("select", "disabled");
        setBooleanAttribute("td", "nowrap");
        setBooleanAttribute("textarea", "disabled");
        setBooleanAttribute("textarea", "readonly");
        setBooleanAttribute("th", "nowrap");
        setBooleanAttribute("ul", "compact");
    }

    private static void setBooleanAttribute(String element, String attribute) {
        booleanAttributes.add(attribute);
        booleanCombinations.add(element + '+' + attribute);
    }

    private static boolean isBooleanAttribute(String element, String attribute, String value) {
        return attribute.equalsIgnoreCase(value) &&
                booleanAttributes.contains(attribute) &&
                booleanCombinations.contains(element + '+' + attribute);
    }

    /**
    * Constructor
    */

    public HTMLEmitter() {

    }

    /**
    * Output start of document
    */

    public void open() throws XPathException {}

    protected void openDocument() throws XPathException {
        if (writer==null) {
            makeWriter();
        }
        if (started) return;
        started = true;
            // This method is sometimes called twice, especially during an identity transform
            // This check stops two DOCTYPE declarations being output.

        String version = outputProperties.getProperty(OutputKeys.VERSION);
        if (version != null && !(version.equals("4.0") || version.equals("4.01"))) {
            XPathException err = new XPathException("Unsupported HTML version: " + version);
            err.setErrorCode("SESU0013");
            throw err;
        }

        String byteOrderMark = outputProperties.getProperty(SaxonOutputKeys.BYTE_ORDER_MARK);

        if ("yes".equals(byteOrderMark) &&
                "UTF-8".equalsIgnoreCase(outputProperties.getProperty(OutputKeys.ENCODING))) {
            try {
                writer.write('\uFEFF');
            } catch (java.io.IOException err) {
                // Might be an encoding exception; just ignore it
            }
        }

        String systemId = outputProperties.getProperty(OutputKeys.DOCTYPE_SYSTEM);
        String publicId = outputProperties.getProperty(OutputKeys.DOCTYPE_PUBLIC);

        if (systemId!=null || publicId!=null) {
            writeDocType("html", systemId, publicId);
        }

        empty = false;
        inScript = -1000000;

        // Handle saxon:character-representation

        String representation = outputProperties.getProperty(
                                    SaxonOutputKeys.CHARACTER_REPRESENTATION);
        if (representation != null) {
	        String nonASCIIrep;
	        String excludedRep;
	        int semi = representation.indexOf(';');
	        if (semi < 0) {
	        	nonASCIIrep = Whitespace.trim(representation);
	        	excludedRep = nonASCIIrep;
	        } else {
	        	nonASCIIrep = Whitespace.trim(representation.substring(0, semi));
	        	excludedRep = Whitespace.trim(representation.substring(semi+1));
	        }
	        nonASCIIRepresentation = representationCode(nonASCIIrep);
	        excludedRepresentation = representationCode(excludedRep);
	        if (excludedRepresentation == REP_NATIVE) {
	        	excludedRepresentation = REP_ENTITY;
	        }
	    }

    }

    /**
    * Output element start tag
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {

        super.startElement(nameCode, typeCode, locationId, properties);
		uriCode = namePool.getURICode(nameCode);
        elementName = (String)elementStack.peek();

        if (uriCode==0 &&
                (   elementName.equalsIgnoreCase("script") ||
                    elementName.equalsIgnoreCase("style"))) {
            inScript = 0;
        }
        inScript++;
    }

    public void startContent() throws XPathException {
        closeStartTag();                   // prevent <xxx/> syntax
    }

    /**
    * Write attribute name=value pair. Overrides the XML behaviour if the name and value
    * are the same (we assume this is a boolean attribute to be minimised), or if the value is
    * a URL.
    */

    protected void writeAttribute(int elCode, String attname, CharSequence value, int properties) throws XPathException {
        try {
            if (uriCode==0) {
                if (isBooleanAttribute(elementName, attname, value.toString())) {
                    writer.write(attname);
                    return;
                }
            }
            super.writeAttribute(elCode, attname, value, properties);
        } catch (java.io.IOException err) {
            throw new XPathException(err);
        }
    }


    /**
    * Escape characters. Overrides the XML behaviour
    */

    protected void writeEscape(final CharSequence chars, final boolean inAttribute)
    throws java.io.IOException, XPathException {

        int segstart = 0;
        final boolean[] specialChars = (inAttribute ? specialInAtt : specialInText);

        if (chars instanceof CompressedWhitespace) {
            ((CompressedWhitespace)chars).writeEscape(specialChars, writer);
            return;
        }
        boolean disabled = false;

        while (segstart < chars.length()) {
            int i = segstart;

            // find a maximal sequence of "ordinary" characters

            if (nonASCIIRepresentation == REP_NATIVE) {
                char c;
                while (i < chars.length() &&
                        ((c = chars.charAt(i)) < 127 ? !specialChars[c] : (characterSet.inCharset(c) && c > 160)
     				 )
     			  ) {
                    i++;
                }
            } else {
                char c;
                while (i < chars.length() && (c = chars.charAt(i)) < 127 && !specialChars[c]) {
                    i++;
                }
            }



            // if this was the whole string, output the string and quit

            if (i == chars.length()) {
                if (segstart == 0) {
                    writeCharSequence(chars);
                } else {
                    writeCharSequence(chars.subSequence(segstart, i));
                }
                return;
            }

            // otherwise, output this sequence and continue
            if (i > segstart) {
                writeCharSequence(chars.subSequence(segstart, i));
            }

            final char c = chars.charAt(i);

            if (c==0) {
                // used to switch escaping on and off
                disabled = !disabled;
            } else if (disabled) {
                writer.write(c);
            } else if (c<=127) {

                // handle a special ASCII character

                if (inAttribute) {
                    if (c=='<') {
                        writer.write('<');                       // not escaped
                    } else if (c=='>') {
                        writer.write("&gt;");           // recommended for older browsers
                    } else if (c=='&') {
                        if (i+1<chars.length() && chars.charAt(i+1)=='{') {
                            writer.write('&');                   // not escaped if followed by '{'
                        } else {
                            writer.write("&amp;");
                        }
                    } else if (c=='\"') {
                        writer.write("&#34;");
                    } else if (c=='\n') {
                        writer.write("&#xA;");
                    } else if (c=='\t') {
                        writer.write("&#x9;");
                    } else if (c=='\r') {
                        writer.write("&#xD;");
                    }
                } else {
                    if (c=='<') {
                        writer.write("&lt;");
                    } else if (c=='>') {
                        writer.write("&gt;");  // changed to allow for "]]>"
                    } else if (c=='&') {
                        writer.write("&amp;");
                    } else if (c=='\r') {
                        writer.write("&#xD;");
                    }
                }

        	} else if (c==160) {
        		// always output NBSP as an entity reference
            	writer.write("&nbsp;");

            } else if (c>=127 && c<160) {
                // these control characters are illegal in HTML
                XPathException err = new XPathException("Illegal HTML character: decimal " + (int)c);
                err.setErrorCode("SERE0014");
                throw err;

            } else if (c>=55296 && c<=56319) {  //handle surrogate pair

                //A surrogate pair is two consecutive Unicode characters.  The first
                //is in the range D800 to DBFF, the second is in the range DC00 to DFFF.
                //To compute the numeric value of the character corresponding to a surrogate
                //pair, use this formula (all numbers are hex):
        	    //(FirstChar - D800) * 400 + (SecondChar - DC00) + 10000

                    // we'll trust the data to be sound
                int charval = (((int)c - 55296) * 1024) + ((int)chars.charAt(i+1) - 56320) + 65536;
                outputCharacterReference(charval);
                i++;


            } else if (characterSet.inCharset(c)) {
            	switch(nonASCIIRepresentation) {
            		case REP_NATIVE:
            			writer.write(c);
            			break;
            		case REP_ENTITY:
            			if (c>160 && c<=255) {

			                // if chararacter in iso-8859-1, use an entity reference

			                writer.write('&');
			                writer.write(latin1Entities[(int)c-160]);
			                writer.write(';');
			                break;
			            }
			            // else fall through
			        case REP_DECIMAL:
			        	preferHex = false;
			        	outputCharacterReference(c);
			        	break;
			        case REP_HEX:
			        	preferHex = true;
			        	// fall through
			        default:
			        	outputCharacterReference(c);
			        	break;
			    }

            } else {
                // Character not present in encoding
                switch(excludedRepresentation) {
            		case REP_ENTITY:
            			if (c>160 && c<=255) {

			                // if chararacter in iso-8859-1, use an entity reference

			                writer.write('&');
			                writer.write(latin1Entities[(int)c-160]);
			                writer.write(';');
			                break;
			            }
			            // else fall through
                    case REP_NATIVE:
                    case REP_DECIMAL:
			        	preferHex = false;
			        	outputCharacterReference(c);
			        	break;
			        case REP_HEX:
			        	preferHex = true;
			        	// fall through
			        default:
			        	outputCharacterReference(c);
			        	break;
			    }
            }

            segstart = ++i;
        }

    }

    /**
    * Output an element end tag.
    */

    public void endElement() throws XPathException {
        String name = (String)elementStack.peek();
        inScript--;
        if (inScript==0) {
            inScript = -1000000;
        }

        if (isEmptyTag(name) && uriCode==0) {
            // no end tag required
            elementStack.pop();
        } else {
            super.endElement();
        }

    }

    /**
    * Character data.
    */

    public void characters (CharSequence chars, int locationId, int properties)
    throws XPathException {
        int options = properties;
        if (inScript>0) {
            options |= ReceiverOptions.DISABLE_ESCAPING;
        }
        super.characters(chars, locationId, options);
    }

    /**
    * Handle a processing instruction.
    */

    public void processingInstruction (String target, CharSequence data, int locationId, int properties)
        throws XPathException
    {
        if (empty) {
            openDocument();
        }
        for (int i=0; i<data.length(); i++) {
            if (data.charAt(i) == '>') {
                XPathException err = new XPathException("A processing instruction in HTML must not contain a > character");
                err.setErrorCode("SERE0015");
                throw err;
            }
        }
        try {
            writer.write("<?");
            writer.write(target);
            writer.write(' ');
            writeCharSequence(data);
            writer.write('>');
        } catch (java.io.IOException err) {
            throw new XPathException(err);
        }
    }


    protected static final String[] latin1Entities = {

        "nbsp",   // "&#160;" -- no-break space = non-breaking space,
                  //                        U+00A0 ISOnum -->
        "iexcl",  // "&#161;" -- inverted exclamation mark, U+00A1 ISOnum -->
        "cent",   // "&#162;" -- cent sign, U+00A2 ISOnum -->
        "pound",  // "&#163;" -- pound sign, U+00A3 ISOnum -->
        "curren", // "&#164;" -- currency sign, U+00A4 ISOnum -->
        "yen",    // "&#165;" -- yen sign = yuan sign, U+00A5 ISOnum -->
        "brvbar", // "&#166;" -- broken bar = broken vertical bar,
                  //                        U+00A6 ISOnum -->
        "sect",   // "&#167;" -- section sign, U+00A7 ISOnum -->
        "uml",    // "&#168;" -- diaeresis = spacing diaeresis,
                  //                        U+00A8 ISOdia -->
        "copy",   // "&#169;" -- copyright sign, U+00A9 ISOnum -->
        "ordf",   // "&#170;" -- feminine ordinal indicator, U+00AA ISOnum -->
        "laquo",  // "&#171;" -- left-pointing double angle quotation mark
                  //                        = left pointing guillemet, U+00AB ISOnum -->
        "not",    // "&#172;" -- not sign, U+00AC ISOnum -->
        "shy",    // "&#173;" -- soft hyphen = discretionary hyphen,
                  //                        U+00AD ISOnum -->
        "reg",    // "&#174;" -- registered sign = registered trade mark sign,
                  //                        U+00AE ISOnum -->
        "macr",   // "&#175;" -- macron = spacing macron = overline
                  //                        = APL overbar, U+00AF ISOdia -->
        "deg",    // "&#176;" -- degree sign, U+00B0 ISOnum -->
        "plusmn", // "&#177;" -- plus-minus sign = plus-or-minus sign,
                  //                        U+00B1 ISOnum -->
        "sup2",   // "&#178;" -- superscript two = superscript digit two
                  //                        = squared, U+00B2 ISOnum -->
        "sup3",   // "&#179;" -- superscript three = superscript digit three
                  //                        = cubed, U+00B3 ISOnum -->
        "acute",  // "&#180;" -- acute accent = spacing acute,
                  //                       U+00B4 ISOdia -->
        "micro",  // "&#181;" -- micro sign, U+00B5 ISOnum -->
        "para",   // "&#182;" -- pilcrow sign = paragraph sign,
                  //                        U+00B6 ISOnum -->
        "middot", // "&#183;" -- middle dot = Georgian comma
                  //                        = Greek middle dot, U+00B7 ISOnum -->
        "cedil",  // "&#184;" -- cedilla = spacing cedilla, U+00B8 ISOdia -->
        "sup1",   // "&#185;" -- superscript one = superscript digit one,
                  //                        U+00B9 ISOnum -->
        "ordm",   // "&#186;" -- masculine ordinal indicator,
                  //                        U+00BA ISOnum -->
        "raquo",  // "&#187;" -- right-pointing double angle quotation mark
                  //                        = right pointing guillemet, U+00BB ISOnum -->
        "frac14", // "&#188;" -- vulgar fraction one quarter
                  //                        = fraction one quarter, U+00BC ISOnum -->
        "frac12", // "&#189;" -- vulgar fraction one half
                  //                        = fraction one half, U+00BD ISOnum -->
        "frac34", // "&#190;" -- vulgar fraction three quarters
                  //                        = fraction three quarters, U+00BE ISOnum -->
        "iquest", // "&#191;" -- inverted question mark
                  //                        = turned question mark, U+00BF ISOnum -->
        "Agrave", // "&#192;" -- latin capital letter A with grave
                  //                        = latin capital letter A grave,
                  //                        U+00C0 ISOlat1 -->
        "Aacute", // "&#193;" -- latin capital letter A with acute,
                  //                        U+00C1 ISOlat1 -->
        "Acirc",  // "&#194;" -- latin capital letter A with circumflex,
                  //                        U+00C2 ISOlat1 -->
        "Atilde", // "&#195;" -- latin capital letter A with tilde,
                  //                        U+00C3 ISOlat1 -->
        "Auml",   // "&#196;" -- latin capital letter A with diaeresis,
                  //                        U+00C4 ISOlat1 -->
        "Aring",  // "&#197;" -- latin capital letter A with ring above
                  //                        = latin capital letter A ring,
                  //                        U+00C5 ISOlat1 -->
        "AElig",  // "&#198;" -- latin capital letter AE
                  //                        = latin capital ligature AE,
                  //                        U+00C6 ISOlat1 -->
        "Ccedil", // "&#199;" -- latin capital letter C with cedilla,
                  //                        U+00C7 ISOlat1 -->
        "Egrave", // "&#200;" -- latin capital letter E with grave,
                  //                        U+00C8 ISOlat1 -->
        "Eacute", // "&#201;" -- latin capital letter E with acute,
                  //                        U+00C9 ISOlat1 -->
        "Ecirc",  // "&#202;" -- latin capital letter E with circumflex,
                  //                        U+00CA ISOlat1 -->
        "Euml",   // "&#203;" -- latin capital letter E with diaeresis,
                  //                        U+00CB ISOlat1 -->
        "Igrave", // "&#204;" -- latin capital letter I with grave,
                  //                        U+00CC ISOlat1 -->
        "Iacute", // "&#205;" -- latin capital letter I with acute,
                  //                        U+00CD ISOlat1 -->
        "Icirc",  // "&#206;" -- latin capital letter I with circumflex,
                  //                        U+00CE ISOlat1 -->
        "Iuml",   // "&#207;" -- latin capital letter I with diaeresis,
                  //                        U+00CF ISOlat1 -->
        "ETH",    // "&#208;" -- latin capital letter ETH, U+00D0 ISOlat1 -->
        "Ntilde", // "&#209;" -- latin capital letter N with tilde,
                  //                        U+00D1 ISOlat1 -->
        "Ograve", // "&#210;" -- latin capital letter O with grave,
                  //                        U+00D2 ISOlat1 -->
        "Oacute", // "&#211;" -- latin capital letter O with acute,
                  //                        U+00D3 ISOlat1 -->
        "Ocirc",  // "&#212;" -- latin capital letter O with circumflex,
                  //                        U+00D4 ISOlat1 -->
        "Otilde", // "&#213;" -- latin capital letter O with tilde,
                  //                        U+00D5 ISOlat1 -->
        "Ouml",   // "&#214;" -- latin capital letter O with diaeresis,
                  //                        U+00D6 ISOlat1 -->
        "times",  // "&#215;" -- multiplication sign, U+00D7 ISOnum -->
        "Oslash", // "&#216;" -- latin capital letter O with stroke
                  //                        = latin capital letter O slash,
                  //                        U+00D8 ISOlat1 -->
        "Ugrave", // "&#217;" -- latin capital letter U with grave,
                  //                        U+00D9 ISOlat1 -->
        "Uacute", // "&#218;" -- latin capital letter U with acute,
                  //                        U+00DA ISOlat1 -->
        "Ucirc",  // "&#219;" -- latin capital letter U with circumflex,
                  //                        U+00DB ISOlat1 -->
        "Uuml",   // "&#220;" -- latin capital letter U with diaeresis,
                  //                        U+00DC ISOlat1 -->
        "Yacute", // "&#221;" -- latin capital letter Y with acute,
                  //                        U+00DD ISOlat1 -->
        "THORN",  // "&#222;" -- latin capital letter THORN,
                  //                        U+00DE ISOlat1 -->
        "szlig",  // "&#223;" -- latin small letter sharp s = ess-zed,
                  //                        U+00DF ISOlat1 -->
        "agrave", // "&#224;" -- latin small letter a with grave
                  //                        = latin small letter a grave,
                  //                        U+00E0 ISOlat1 -->
        "aacute", // "&#225;" -- latin small letter a with acute,
                  //                        U+00E1 ISOlat1 -->
        "acirc",  // "&#226;" -- latin small letter a with circumflex,
                  //                        U+00E2 ISOlat1 -->
        "atilde", // "&#227;" -- latin small letter a with tilde,
                  //                        U+00E3 ISOlat1 -->
        "auml",   // "&#228;" -- latin small letter a with diaeresis,
                  //                        U+00E4 ISOlat1 -->
        "aring",  // "&#229;" -- latin small letter a with ring above
                  //                        = latin small letter a ring,
                  //                        U+00E5 ISOlat1 -->
        "aelig",  // "&#230;" -- latin small letter ae
                  //                        = latin small ligature ae, U+00E6 ISOlat1 -->
        "ccedil", // "&#231;" -- latin small letter c with cedilla,
                  //                        U+00E7 ISOlat1 -->
        "egrave", // "&#232;" -- latin small letter e with grave,
                  //                        U+00E8 ISOlat1 -->
        "eacute", // "&#233;" -- latin small letter e with acute,
                  //                        U+00E9 ISOlat1 -->
        "ecirc",  // "&#234;" -- latin small letter e with circumflex,
                  //                        U+00EA ISOlat1 -->
        "euml",   // "&#235;" -- latin small letter e with diaeresis,
                  //                        U+00EB ISOlat1 -->
        "igrave", // "&#236;" -- latin small letter i with grave,
                  //                        U+00EC ISOlat1 -->
        "iacute", // "&#237;" -- latin small letter i with acute,
                  //                        U+00ED ISOlat1 -->
        "icirc",  // "&#238;" -- latin small letter i with circumflex,
                  //                        U+00EE ISOlat1 -->
        "iuml",   // "&#239;" -- latin small letter i with diaeresis,
                  //                        U+00EF ISOlat1 -->
        "eth",    // "&#240;" -- latin small letter eth, U+00F0 ISOlat1 -->
        "ntilde", // "&#241;" -- latin small letter n with tilde,
                  //                        U+00F1 ISOlat1 -->
        "ograve", // "&#242;" -- latin small letter o with grave,
                  //                        U+00F2 ISOlat1 -->
        "oacute", // "&#243;" -- latin small letter o with acute,
                  //                        U+00F3 ISOlat1 -->
        "ocirc",  // "&#244;" -- latin small letter o with circumflex,
                  //                        U+00F4 ISOlat1 -->
        "otilde", // "&#245;" -- latin small letter o with tilde,
                  //                        U+00F5 ISOlat1 -->
        "ouml",   // "&#246;" -- latin small letter o with diaeresis,
                  //                        U+00F6 ISOlat1 -->
        "divide", // "&#247;" -- division sign, U+00F7 ISOnum -->
        "oslash", // "&#248;" -- latin small letter o with stroke,
                  //                        = latin small letter o slash,
                  //                        U+00F8 ISOlat1 -->
        "ugrave", // "&#249;" -- latin small letter u with grave,
                  //                        U+00F9 ISOlat1 -->
        "uacute", // "&#250;" -- latin small letter u with acute,
                  //                        U+00FA ISOlat1 -->
        "ucirc",  // "&#251;" -- latin small letter u with circumflex,
                  //                        U+00FB ISOlat1 -->
        "uuml",   // "&#252;" -- latin small letter u with diaeresis,
                  //                        U+00FC ISOlat1 -->
        "yacute", // "&#253;" -- latin small letter y with acute,
                  //                        U+00FD ISOlat1 -->
        "thorn",  // "&#254;" -- latin small letter thorn,
                  //                        U+00FE ISOlat1 -->
        "yuml"    // "&#255;" -- latin small letter y with diaeresis,
                  //                        U+00FF ISOlat1 -->
	};


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
