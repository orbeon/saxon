package org.orbeon.saxon.event;
import org.orbeon.saxon.charcode.UnicodeCharacterSet;
import org.orbeon.saxon.om.XMLChar;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.tinytree.CharSlice;
import org.orbeon.saxon.tinytree.CompressedWhitespace;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.sort.IntHashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.util.Stack;
import java.io.CharArrayWriter;
import java.io.File;

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

    // Getting a display name for a namecode can be expensive because it involves string
    // concatenation, and more importantly, checking of the name against the encoding. So
    // we keep a local cache of names we have seen before.

    private IntHashMap nameLookup = new IntHashMap(100);

    // For other names we use a hashtable. It

    private boolean indenting = false;
    private int indentSpaces = 3;
    private String indentChars = "\n                                                          ";
    private int totalAttributeLength = 0;
    private boolean requireWellFormed = false;


    static boolean[] specialInText;         // lookup table for special characters in text
    static boolean[] specialInAtt;          // lookup table for special characters in attributes
        // create look-up table for ASCII characters that need special treatment

    static {
        specialInText = new boolean[128];
        for (int i=0; i<=31; i++) specialInText[i] = true;  // allowed in XML 1.1 as character references
        for (int i=32; i<=127; i++) specialInText[i] = false;
        specialInText[(char)0] = true;
            // used to switch escaping on and off for mapped characters
        specialInText['\n'] = false;
        specialInText['\t'] = false;
        specialInText['\r'] = true;
        specialInText['<'] = true;
        specialInText['>'] = true;
        specialInText['&'] = true;

        specialInAtt = new boolean[128];
        for (int i=0; i<=31; i++) specialInAtt[i] = true; // allowed in XML 1.1 as character references
        for (int i=32; i<=127; i++) specialInAtt[i] = false;
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
        String rep = outputProperties.getProperty(SaxonOutputKeys.CHARACTER_REPRESENTATION);
        if (rep!=null) {
        	preferHex = (rep.trim().equalsIgnoreCase("hex"));
        }
        rep = outputProperties.getProperty(SaxonOutputKeys.UNDECLARE_PREFIXES);
        if (rep!=null) {
        	undeclareNamespaces = (rep.trim().equalsIgnoreCase("yes"));
        }
        writeDeclaration();
    }

    /**
    * Output the XML declaration
    */

    public void writeDeclaration() throws XPathException {
        if (declarationIsWritten) return;
        declarationIsWritten = true;
        try {
            indenting = "yes".equals(outputProperties.getProperty(OutputKeys.INDENT));
            String s = outputProperties.getProperty(SaxonOutputKeys.INDENT_SPACES);
            if (s!=null) {
                try {
                    indentSpaces = Integer.parseInt(s.trim());
                } catch (NumberFormatException err) {}
            }

            String byteOrderMark = outputProperties.getProperty(SaxonOutputKeys.BYTE_ORDER_MARK);

            if ("yes".equals(byteOrderMark) &&
                    "UTF-8".equalsIgnoreCase(outputProperties.getProperty(OutputKeys.ENCODING))) {
                // For UTF-16, Java outputs a BOM whether we like it or not
                writer.write('\uFEFF');
            }

            String omitXMLDeclaration = outputProperties.getProperty(OutputKeys.OMIT_XML_DECLARATION);
            if (omitXMLDeclaration==null) {
                omitXMLDeclaration = "no";
            }

            String version = outputProperties.getProperty(OutputKeys.VERSION);
            if (version==null) {
                version = getConfiguration().getNameChecker().getXMLVersion();
            } else {
                if (!version.equals("1.0") && !version.equals("1.1")) {
                    DynamicError err = new DynamicError("XML version must be 1.0 or 1.1");
                    err.setErrorCode("SESU0006");
                    throw err;
                }
                if (!version.equals("1.0") && omitXMLDeclaration.equals("yes") &&
                        outputProperties.getProperty(OutputKeys.DOCTYPE_SYSTEM) != null) {
                    DynamicError err = new DynamicError(
                            "Values of 'version', 'omit-xml-declaration', and 'doctype-system' conflict");
                    err.setErrorCode("SEPM0009");
                    throw err;
                }
            }

            if (version.equals("1.0") && undeclareNamespaces) {
                DynamicError err = new DynamicError(
                            "Cannot undeclare namespaces with XML version 1.0");
                err.setErrorCode("SEPM0010");
                throw err;
            }

            String encoding = outputProperties.getProperty(OutputKeys.ENCODING);
            if (encoding==null || encoding.equalsIgnoreCase("utf8")) {
                encoding = "UTF-8";
            }

            String standalone = outputProperties.getProperty(OutputKeys.STANDALONE);
            if ("omit".equals(standalone)) {
                standalone = null;
            }

            if (standalone != null) {
                requireWellFormed = true;
                if (omitXMLDeclaration.equals("yes")) {
                    DynamicError err = new DynamicError("Values of 'standalone' and 'omit-xml-declaration' conflict");
                    err.setErrorCode("SEPM0009");
                    throw err;
                }
            }

            if (omitXMLDeclaration.equals("no")) {
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
            if (declarationIsWritten && !indenting) {
                // don't add a newline if indenting, because the indenter will already have done so
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
    * End of the document.
    */

    public void close() throws XPathException {
        // if nothing has been written, we should still create the file and write an XML declaration
        if (empty) {
            openDocument();
        }
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
        } else if (requireWellFormed && elementStack.isEmpty()) {
            DynamicError err = new DynamicError(
                    "When 'standalone' or 'doctype-system' is specified, the document must be well-formed; " +
                    "but this document contains more than one top-level element");
            err.setErrorCode("SEPM0004");
            throw err;
        }
        String displayName = null;

        // See if we've seen this name before
        displayName = (String)nameLookup.get(nameCode);

        // Otherwise, look it up in the namepool and check that it's encodable
        if (displayName == null) {
    	    displayName = namePool.getDisplayName(nameCode);
            if (!allCharactersEncodable) {
                int badchar = testCharacters(displayName);
                if (badchar!=0) {
                    DynamicError err = new DynamicError("Element name contains a character (decimal + " +
                                                    badchar + ") not available in the selected encoding");
                    err.setErrorCode("SERE0008");
                    throw err;
                }
            }
            nameLookup.put(nameCode, displayName);
        }

        elementStack.push(displayName);
        elementCode = nameCode;

        try {
            if (empty) {
                String systemId = outputProperties.getProperty(OutputKeys.DOCTYPE_SYSTEM);
                String publicId = outputProperties.getProperty(OutputKeys.DOCTYPE_PUBLIC);
                if (systemId!=null) {
                    requireWellFormed = true;
                    writeDocType(displayName, systemId, publicId);
                }
                empty = false;
            }
            if (openStartTag) {
                closeStartTag();
            }
            writer.write('<');
            writer.write(displayName);
            openStartTag = true;
            totalAttributeLength = 0;

        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    public void namespace(int namespaceCode, int properties) throws XPathException {
        try {
            String nsprefix = namePool.getPrefixFromNamespaceCode(namespaceCode);
            String nsuri = namePool.getURIFromNamespaceCode(namespaceCode);

            int len = nsuri.length() + nsprefix.length() + 8;
            String sep = " ";
            if (indenting && (totalAttributeLength + len) > 80 && totalAttributeLength != 0) {
                sep = getAttributeIndentString();
            }
            totalAttributeLength += len;

            if (nsprefix.equals("")) {
                writer.write(sep);
                writeAttribute(elementCode, "xmlns", nsuri, 0);
            } else if (nsprefix.equals("xml")) {
                return;
            } else {
                int badchar = testCharacters(nsprefix);
                if (badchar!=0) {
                    DynamicError err = new DynamicError("Namespace prefix contains a character (decimal + " +
                                                    badchar + ") not available in the selected encoding");
                    err.setErrorCode("SERE0008");
                    throw err;
                }
                if (undeclareNamespaces || !nsuri.equals("")) {
                    writer.write(sep);
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
        displayName = (String)nameLookup.get(nameCode);

        // Otherwise, look it up in the namepool and check that it's encodable
        if (displayName == null) {
    	    displayName = namePool.getDisplayName(nameCode);
            if (!allCharactersEncodable) {
                int badchar = testCharacters(displayName);
                if (badchar!=0) {
                    DynamicError err = new DynamicError("Attribute name contains a character (decimal + " +
                                                    badchar + ") not available in the selected encoding");
                    err.setErrorCode("SERE0008");
                    throw err;
                }
            }
            nameLookup.put(nameCode, displayName);
        }

        final int len = displayName.length() + value.length() + 4;
        String sep = " ";
        if (indenting && (totalAttributeLength + len) > 80 && totalAttributeLength != 0) {
            sep = getAttributeIndentString();
        }
        totalAttributeLength += len;

        try {
            writer.write(sep);
            writeAttribute(
                elementCode,
                displayName,
                value,
                properties );

        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    private String getAttributeIndentString() {
        int indent = (elementStack.size()-1) * indentSpaces + ((String)elementStack.peek()).length() + 3;
        while (indent >= indentChars.length()) {
            indentChars += "                     ";
        }
        return indentChars.substring(0, indent);
    }

    public void startContent() throws XPathException {
        // don't add ">" to the start tag until we know whether the element has content
    }

    public void closeStartTag() throws XPathException {
        try {
            if (openStartTag) {
                writer.write('>');
                openStartTag = false;
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    /**
    * Close an empty element tag. (This is overridden in XHTMLEmitter).
    */

    protected String emptyElementTagCloser(String displayName, int nameCode) {
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
                writer.write(emptyElementTagCloser(displayName, elementCode));
                openStartTag = false;
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
            if (!Whitespace.isWhite(chars)) {
                if (requireWellFormed || outputProperties.getProperty(OutputKeys.DOCTYPE_SYSTEM)!=null) {
                    DynamicError err = new DynamicError(
                        "When 'standalone' or 'doctype-system' is specified, the document must be well-formed; " +
                        "but this document contains a top-level text node");
                    err.setErrorCode("SEPM0004");
                    throw err;
                }
            }
        }

        if (requireWellFormed && elementStack.isEmpty() && !Whitespace.isWhite(chars)) {
            DynamicError err = new DynamicError(
                    "When 'standalone' or 'doctype-system' is specified, the document must be well-formed; " +
                    "but this document contains a top-level text node");
            err.setErrorCode("SEPM0004");
            throw err;
        }

        try {
            if (openStartTag) {
                closeStartTag();
            }

            if ((properties & ReceiverOptions.NO_SPECIAL_CHARS) != 0) {
                writeCharSequence(chars);
            } else if ((properties & ReceiverOptions.DISABLE_ESCAPING) == 0) {
                writeEscape(chars, false);
            } else {
                // disable-output-escaping="yes"
                if (testCharacters(chars) == 0) {
                    if ((properties & ReceiverOptions.USE_NULL_MARKERS) == 0) {
                        writeCharSequence(chars);
                    } else {
                        // Need to strip out any null markers. See test output-html109
                        final int len = chars.length();
                        for (int i=0; i<len; i++) {
                            char c = chars.charAt(i);
                            if (c != 0) {
                                writer.write(c);
                            }
                        }
                    }
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
        } else if (s instanceof CompressedWhitespace) {
            ((CompressedWhitespace)s).write(writer);
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
        int x = testCharacters(target);
        if (x != 0) {
            DynamicError err = new DynamicError("Character in processing instruction name cannot be represented " +
                    "in the selected encoding (code " + x + ')');
            err.setErrorCode("SERE0008");
            throw err;
        }
        x = testCharacters(data);
        if (x != 0) {
            DynamicError err = new DynamicError("Character in processing instruction data cannot be represented " +
                    "in the selected encoding (code " + x + ')');
            err.setErrorCode("SERE0008");
            throw err;
        }
        try {
            if (openStartTag) {
                closeStartTag();
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
    throws java.io.IOException, XPathException {
        int segstart = 0;
        boolean disabled = false;
        final boolean[] specialChars = (inAttribute ? specialInAtt : specialInText);

        if (chars instanceof CompressedWhitespace) {
            ((CompressedWhitespace)chars).writeEscape(specialChars, writer);
            return;
        }

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
                } else if (c == 0x2028) {
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
                if (c==0x2028) {
                    outputCharacterReference(c);
                } else if (XMLChar.isHighSurrogate(c)) {
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
                } else {
                    // C0 control characters
                     outputCharacterReference(c);
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
        int x = testCharacters(chars);
        if (x != 0) {
            DynamicError err = new DynamicError("Character in comment cannot be represented " +
                    "in the selected encoding (code " + x + ')');
            err.setErrorCode("SERE0008");
            throw err;
        }
        try {
            if (openStartTag) {
                closeStartTag();
            }
            writer.write("<!--");
            writer.write(chars.toString());
            writer.write("-->");
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
    }

    public static void main(String[] params) throws Exception {
        StreamResult iStreamResult = new StreamResult(new CharArrayWriter());
        XMLEmitter iResult = new XMLEmitter();
        iResult.setStreamResult(iStreamResult);

        StreamSource iSource = new StreamSource(new File("c:\\temp\\test.xml"));

        System.setProperty("javax.xml.transform.TransformerFactory",
        "org.orbeon.saxon.TransformerFactoryImpl");
        TransformerFactory iTfactory = TransformerFactory.newInstance();
        Templates iTemplates = iTfactory.newTemplates(
                new StreamSource(new File("c:\\temp\\test.xsl")));
        iTemplates.newTransformer().transform(iSource, iResult);

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
