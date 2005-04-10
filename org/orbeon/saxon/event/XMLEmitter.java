package net.sf.saxon.event;
import net.sf.saxon.charcode.UnicodeCharacterSet;
import net.sf.saxon.om.XMLChar;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tinytree.CharSlice;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import java.util.Stack;

/**
  * XMLEmitter is an Emitter that generates XML output
  * to a specified destination.
  */

public class XMLEmitter extends Emitter
{
    protected boolean empty = true;
    protected boolean openStartTag = false;
    protected boolean declarationIsWritten = false;
    protected int elementCode;

    protected boolean preferHex = false;
    protected boolean undeclareNamespaces = false;
    private boolean warningIssued = false;

    // The element stack holds the display names (lexical QNames) of elements that
    // have been started but not finished. It is used to obtain the element name
    // for the end tag.

    protected Stack elementStack = new Stack();

    // Namecodes in the range 0..2048 are common. So for these codes,
    // we maintain a direct lookup from the namecode to the display name
    // that bypasses reference to the namepool

    private String[] nameLookup = new String[2048];


    static boolean[] specialInText;         // lookup table for special characters in text
    static boolean[] specialInAtt;          // lookup table for special characters in attributes
        // create look-up table for ASCII characters that need special treatment

    static {
        specialInText = new boolean[128];
        for (int i=0; i<=15; i++) specialInText[i] = true;  // allowed in XML 1.1 as character references
        for (int i=16; i<=127; i++) specialInText[i] = false;
        specialInText[(char)0] = true;
            // used to switch escaping on and off for mapped characters
        specialInText['\n'] = false;
        specialInText['\t'] = false;
        specialInText['\r'] = true;
        specialInText['<'] = true;
        specialInText['>'] = true;
        specialInText['&'] = true;

        specialInAtt = new boolean[128];
        for (int i=0; i<=15; i++) specialInAtt[i] = true; // allowed in XML 1.1 as character references
        for (int i=16; i<=127; i++) specialInAtt[i] = false;
        specialInAtt[(char)0] = true;
            // used to switch escaping on and off for mapped characters
        specialInAtt['\r'] = true;
        specialInAtt['\n'] = true;
        specialInAtt['\t'] = true;
        specialInAtt['<'] = true;
        specialInAtt['>'] = true;
        specialInAtt['&'] = true;
        specialInAtt['\"'] = true;
    }

    /**
     * Start of the event stream. Nothing is done at this stage: the opening of the output
     * file is deferred until some content is written to it.
    */

    public void open() throws XPathException {}

    /**
     * Start of a document node. Nothing is done at this stage: the opening of the output
     * file is deferred until some content is written to it.
    */

    public void startDocument(int properties) throws XPathException {}

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        if (!elementStack.isEmpty()) {
            throw new IllegalStateException("Attempt to end document in serializer when elements are unclosed");
        }
    }

    /**
     * Do the real work of starting the document. This happens when the first
     * content is written.
     * @throws XPathException
     */

    protected void openDocument () throws XPathException
    {
        if (writer==null) {
            makeWriter();
        }
        if (characterSet==null) {
            characterSet = UnicodeCharacterSet.getInstance();
        }
        writeDeclaration();
        String rep = outputProperties.getProperty(SaxonOutputKeys.CHARACTER_REPRESENTATION);
        if (rep!=null) {
        	preferHex = (rep.trim().equalsIgnoreCase("hex"));
        }
        rep = outputProperties.getProperty(SaxonOutputKeys.UNDECLARE_PREFIXES);
        if (rep!=null) {
        	undeclareNamespaces = (rep.trim().equalsIgnoreCase("yes"));
        }
    }

    /**
    * Output the XML declaration
    */

    public void writeDeclaration() throws XPathException {
        if (declarationIsWritten) return;
        declarationIsWritten = true;
        try {
            String byteOrderMark = outputProperties.getProperty(SaxonOutputKeys.BYTE_ORDER_MARK);

            if ("yes".equals(byteOrderMark) &&
                    !"UTF-16".equalsIgnoreCase(outputProperties.getProperty(OutputKeys.ENCODING))) {
                // For UTF-16, Java outputs a BOM whether we like it or not
                writer.write('\uFEFF');
            }

            String omit = outputProperties.getProperty(OutputKeys.OMIT_XML_DECLARATION);
            if (omit==null) {
                omit = "no";
            }

            String version = outputProperties.getProperty(OutputKeys.VERSION);
            if (version==null) {
                version = "1.0";
            }

            String encoding = outputProperties.getProperty(OutputKeys.ENCODING);
            if (encoding==null || encoding.equalsIgnoreCase("utf8")) {
                encoding = "UTF-8";
            }

            // If user requests an encoding other than UTF-8, UTF-16, or ASCII,
            // then force an XML declaration to be output, otherwise the
            // result will not be well-formed XML

//            if (omit.equals("yes") &&
//                  !(encoding.equalsIgnoreCase("UTF-8")) ||
//                    encoding.equalsIgnoreCase("utf-16") ||
//                    encoding.equalsIgnoreCase("us-ascii") ||
//                    encoding.equalsIgnoreCase("ascii")) {
//                omit = "no";
//            }

            String standalone = outputProperties.getProperty(OutputKeys.STANDALONE);
            if ("omit".equals(standalone)) {
                standalone = null;
            }

            if (omit.equals("no")) {
                writer.write("<?xml version=\"" + version + "\" " + "encoding=\"" + encoding + '\"' +
                        (standalone != null ? " standalone=\"" + standalone + '\"' : "") + "?>");
                    // no longer write a newline character: it's wrong if the output is an
                    // external general parsed entity
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    /**
    * Output the document type declaration
    */

    protected void writeDocType(String type, String systemId, String publicId) throws XPathException {
        try {
            if (declarationIsWritten) {
                writer.write("\n");
            }
            writer.write("<!DOCTYPE " + type + '\n');
            if (systemId!=null && publicId==null) {
                writer.write("  SYSTEM \"" + systemId + "\">\n");
            } else if (systemId==null && publicId!=null) {     // handles the HTML case
                writer.write("  PUBLIC \"" + publicId + "\">\n");
            } else {
                writer.write("  PUBLIC \"" + publicId + "\" \"" + systemId + "\">\n");
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    /**
    * End of the document. Close the output stream.
    */

    public void close () throws XPathException
    {
        try {
            if (writer != null) {
                writer.flush();
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    /**
    * Start of an element. Output the start tag, escaping special characters.
    */

    public void startElement (int nameCode, int typeCode, int locationId, int properties) throws XPathException
    {
        if (empty) {
            openDocument();
        }
        String displayName = null;

        // See if we've seen this name before
        if (nameCode < 2048) {
            displayName = nameLookup[nameCode];
        }

        // Otherwise, look it up in the namepool and check that it's encodable
        if (displayName == null) {
    	    displayName = namePool.getDisplayName(nameCode);
            if (nameCode < 2048) {
                nameLookup[nameCode] = displayName;
            }
            int badchar = testCharacters(displayName);
            if (badchar!=0) {
                throw new DynamicError("Element name contains a character (decimal + " +
                                                badchar + ") not available in the selected encoding");
            }
        }

        elementStack.push(displayName);
        elementCode = nameCode;

        try {
            if (empty) {
                String systemId = outputProperties.getProperty(OutputKeys.DOCTYPE_SYSTEM);
                String publicId = outputProperties.getProperty(OutputKeys.DOCTYPE_PUBLIC);
                if (systemId!=null) {
                    writeDocType(displayName, systemId, publicId);
                }
                empty = false;
            }
            if (openStartTag) {
                closeStartTag(displayName, false);
            }
            writer.write('<');
            writer.write(displayName);
            openStartTag = true;

        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    public void namespace(int namespaceCode, int properties) throws XPathException {
        try {
            String nsprefix = namePool.getPrefixFromNamespaceCode(namespaceCode);
            String nsuri = namePool.getURIFromNamespaceCode(namespaceCode);

            if (nsprefix.equals("")) {
                writer.write(' ');
                writeAttribute(elementCode, "xmlns", nsuri, 0);
            } else if (nsprefix.equals("xml")) {
                return;
            } else {
                int badchar = testCharacters(nsprefix);
                if (badchar!=0) {
                    throw new DynamicError("Namespace prefix contains a character (decimal + " +
                                                    badchar + ") not available in the selected encoding");
                }
                if (undeclareNamespaces || !nsuri.equals("")) {
                    writer.write(' ');
                    writeAttribute(elementCode, "xmlns:" + nsprefix, nsuri, 0);
                }
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        String displayName = null;

        // See if we've seen this name before
        if (nameCode < 2048) {
            displayName = nameLookup[nameCode];
        }

        // Otherwise, look it up in the namepool and check that it's encodable
        if (displayName == null) {
    	    displayName = namePool.getDisplayName(nameCode);
            if (nameCode < 2048) {
                nameLookup[nameCode] = displayName;
            }
            int badchar = testCharacters(displayName);
            if (badchar!=0) {
                throw new DynamicError("Attribute name contains a character (decimal + " +
                                                badchar + ") not available in the selected encoding");
            }
        }
        try {
            writer.write(' ');
            writeAttribute(
                elementCode,
                displayName,
                value,
                properties );

        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    public void startContent() throws XPathException {
        // don't add ">" to the start tag until we know whether the element has content
    }

    public void closeStartTag(String displayName, boolean emptyTag) throws XPathException {
        try {
            if (openStartTag) {
                if (emptyTag) {
                    writer.write(emptyElementTagCloser(displayName));
                } else {
                    writer.write('>');
                }
                openStartTag = false;
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    /**
    * Close an empty element tag. (This is overridden in XHTMLEmitter).
    */

    protected String emptyElementTagCloser(String displayName) {
        return "/>";
    }

    /**
    * Write attribute name=value pair.
     * @param elCode The element name is not used in this version of the
    * method, but is used in the HTML subclass.
     * @param attname The attribute name, which has already been validated to ensure
     * it can be written in this encoding
     * @param value The value of the attribute
     * @param properties Any special properties of the attribute
    */

    protected void writeAttribute(int elCode, String attname, CharSequence value, int properties) throws XPathException {
        try {
            String val = value.toString();
            writer.write(attname);
            if ((properties & ReceiverOptions.NO_SPECIAL_CHARS) != 0) {
                writer.write('=');
                writer.write('"');
                writer.write(val);
                writer.write('"');
            } else if ((properties & ReceiverOptions.USE_NULL_MARKERS) != 0) {
                // null (0) characters will be used before and after any section of
                // the value where escaping is to be disabled
                writer.write('=');
                char delimiter = (val.indexOf('"') >= 0 ? '\'' : '"');
                writer.write(delimiter);
                writeEscape(value, true);
                writer.write(delimiter);
            } else {
                writer.write("=\"");
                writeEscape(value, true);
                writer.write('\"');
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }


    /**
    * Test that all characters in a name are supported in the target encoding.
     * @return zero if all the characters are available, or the value of the
     * first offending character if not
    */

    protected int testCharacters(CharSequence chars) throws XPathException {
        for (int i=0; i<chars.length(); i++) {
            char c = chars.charAt(i);
            if (c > 127) {
                if (XMLChar.isHighSurrogate(c)) {
                    int cc = XMLChar.supplemental(c, chars.charAt(++i));
                    if (!characterSet.inCharset(cc)) {
                        return cc;
                    }
                } else if (!characterSet.inCharset(c)) {
                    return c;
                }
            }
        }
        return 0;
    }

    /**
    * End of an element.
    */

    public void endElement () throws XPathException
    {
        String displayName = (String)elementStack.pop();
        try {
            if (openStartTag) {
                closeStartTag(displayName, true);
            } else {
                writer.write("</");
                writer.write(displayName);
                writer.write('>');
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    /**
    * Character data.
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException
    {
        if (empty) {
            openDocument();
        }
        try {
            if (openStartTag) {
                closeStartTag(null, false);
            }
            // System.err.println("Output characters [" + new String(ch, start, length) + "]");
            if ((properties & ReceiverOptions.DISABLE_ESCAPING) == 0) {
                writeEscape(chars, false);
            } else {
                if (testCharacters(chars) == 0) {
                    writeCharSequence(chars);
                } else {
                    // Recoverable error: using disable output escaping with characters
                    // that are not available in the target encoding
                    if (!warningIssued) {
                        try {
                            getPipelineConfiguration().getErrorListener().warning(
                                new TransformerException("disable-output-escaping is ignored for characters " +
                                                         "not available in the chosen encoding"));
                        } catch (TransformerException e) {
                            throw DynamicError.makeDynamicError(e);
                        }
                        warningIssued = true;
                    }
                    writeEscape(chars, false);
                }
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    /**
     * Write a CharSequence: various implementations
     */

    public void writeCharSequence(CharSequence s) throws java.io.IOException {
        if (s instanceof String) {
            writer.write((String)s);
        } else if (s instanceof CharSlice) {
            ((CharSlice)s).write(writer);
        } else if (s instanceof FastStringBuffer) {
            ((FastStringBuffer)s).write(writer);
        } else {
            writer.write(s.toString());
        }
    }


    /**
    * Handle a processing instruction.
    */

    public void processingInstruction (String target, CharSequence data, int locationId, int properties)
        throws XPathException  {
        if (empty) {
            openDocument();
        }
        try {
            if (openStartTag) {
                closeStartTag(null, false);
            }
            writer.write("<?" + target + (data.length()>0 ? ' ' + data.toString() : "") + "?>");
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    /**
    * Write contents of array to current writer, after escaping special characters.
    * This method converts the XML special characters (such as < and &) into their
    * predefined entities.
    * @param chars The character sequence containing the string
    * @param inAttribute  Set to true if the text is in an attribute value
    */

    protected void writeEscape(final CharSequence chars, final boolean inAttribute)
    throws java.io.IOException {
        int segstart = 0;
        boolean disabled = false;
        final boolean[] specialChars = (inAttribute ? specialInAtt : specialInText);

        while (segstart < chars.length()) {
            int i = segstart;

            // find a maximal sequence of "ordinary" characters
            while (i < chars.length()) {
                final char c = chars.charAt(i);
                if (c < 127) {
                    if (specialChars[c]) {
                        break;
                    } else {
                        i++;
                    }
                } else if (c < 160) {
                    break;
                } else if (XMLChar.isHighSurrogate(c)) {
                    break;
                } else if (!characterSet.inCharset(c)) {
                    break;
                } else {
                    i++;
                }
            }

            // if this was the whole string write it out and exit
            if (i >= chars.length()) {
                if (segstart == 0) {
                    writeCharSequence(chars);
                } else {
                    writeCharSequence(chars.subSequence(segstart, i));
                }
                return;
            }

            // otherwise write out this sequence
            if (i > segstart) {
                writeCharSequence(chars.subSequence(segstart, i));
            }

            // examine the special character that interrupted the scan
            final char c = chars.charAt(i);
            if (c==0) {
                // used to switch escaping on and off
                disabled = !disabled;
            } else if (disabled) {
                writer.write(c);
            } else if (c>=127 && c<160) {
                // XML 1.1 requires these characters to be written as character references
                outputCharacterReference(c);
            } else if (c>=160) {
                if (XMLChar.isHighSurrogate(c)) {
                    char d = chars.charAt(++i);
                    int charval = XMLChar.supplemental(c, d);
                    if (characterSet.inCharset(charval)) {
                        writer.write(c);
                        writer.write(d);
                    } else {
                        outputCharacterReference(charval);
                    }
                } else {
                    // process characters not available in the current encoding
                    outputCharacterReference(c);
                }

            } else {

                // process special ASCII characters

                if (c=='<') {
                    writer.write("&lt;");
                } else if (c=='>') {
                    writer.write("&gt;");
                } else if (c=='&') {
                    writer.write("&amp;");
                } else if (c=='\"') {
                    writer.write("&#34;");
                } else if (c=='\n') {
                    writer.write("&#xA;");
                } else if (c=='\r') {
                    writer.write("&#xD;");
                } else if (c=='\t') {
                    writer.write("&#x9;");
                }
            }
            segstart = ++i;
        }
    }

	/**
	* Output a decimal or hexadecimal character reference
	*/

    private char[] charref = new char[10];
    protected void outputCharacterReference(int charval) throws java.io.IOException {
		if (preferHex) {
	        int o = 0;
	        charref[o++]='&';
	        charref[o++]='#';
			charref[o++]='x';
	        String code = Integer.toHexString(charval);
	        int len = code.length();
	        for (int k=0; k<len; k++) {
	            charref[o++]=code.charAt(k);
	        }
	        charref[o++]=';';
	        writer.write(charref, 0, o);
		} else {
	        int o = 0;
	        charref[o++]='&';
	        charref[o++]='#';
	        String code = Integer.toString(charval);
	        int len = code.length();
	        for (int k=0; k<len; k++) {
	            charref[o++]=code.charAt(k);
	        }
	        charref[o++]=';';
	        writer.write(charref, 0, o);
	    }
    }

    /**
    * Handle a comment.
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException
    {
        if (empty) {
            openDocument();
        }
        try {
            if (openStartTag) {
                closeStartTag(null, false);
            }
            writer.write("<!--");
            writer.write(chars.toString());
            writer.write("-->");
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
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
