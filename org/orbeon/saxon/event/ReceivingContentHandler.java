package net.sf.saxon.event;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.Name;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.XMLChar;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.tinytree.CharSlice;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ValidationException;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;

import java.net.URI;
import java.net.URISyntaxException;

/**
  * ReceivingContentHandler is a glue class that provides a standard SAX ContentHandler
  * interface to a Saxon Receiver. To achieve this it needs to map names supplied
  * as strings to numeric name codes, for which purpose it needs access to a name
  * pool. The class also performs the function of assembling adjacent text nodes.
  * <p>The class was previously named ContentEmitter.</p>
  * @author Michael H. Kay
  */

public class ReceivingContentHandler
        implements ContentHandler, LexicalHandler, DTDHandler, SaxonLocator
{
    private NamePool pool;
    private PipelineConfiguration pipe;
    private Receiver receiver;
    private boolean inDTD = false;	// true while processing the DTD
    private Locator locator;        // a SAX Locator

    // buffer for accumulating character data, until the next markup event is received

    private char[] buffer = new char[4096];
    private int used = 0;

    // array for accumulating namespace information

    private int[] namespaces = new int[50];
    private int namespacesUsed = 0;

    //private boolean isStyleSheet = false;

    /**
    * create a ReceivingContentHandler and initialise variables
    */

    public ReceivingContentHandler() {
    }

	public void setReceiver(Receiver e) {
		receiver = e;
	}

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
        pipe.setLocationProvider(this);
        this.pool = pipe.getConfiguration().getNamePool();

    }

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    public Configuration getConfiguration() {
        return pipe.getConfiguration();
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void startDocument () throws SAXException {
        // System.err.println("ReceivingContentHandler#startDocument");
        try {
            used = 0;
            namespacesUsed = 0;
            pipe.setLocationProvider(this);
            receiver.setPipelineConfiguration(pipe);
            receiver.open();
            receiver.startDocument(0);
        } catch (XPathException err) {
            throw new SAXException(err);
        }
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endDocument () throws SAXException {
        try {
            flush();
            receiver.endDocument();
            receiver.close();
        } catch (ValidationException err) {
            err.setLocator(locator);
            throw new SAXException(err);
        } catch (XPathException err) {
            throw new SAXException(err);
        }
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void setDocumentLocator (Locator locator) {
    	this.locator = locator;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        //System.err.println("StartPrefixMapping " + prefix + "=" + uri);
    	if (namespacesUsed >= namespaces.length) {
    		int[] n2 = new int[namespacesUsed * 2];
    		System.arraycopy(namespaces, 0, n2, 0, namespacesUsed);
    		namespaces = n2;
    	}
    	namespaces[namespacesUsed++] = pool.allocateNamespaceCode(prefix, uri);
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endPrefixMapping(String prefix) throws SAXException {}

    /**
    * Callback interface for SAX: not for application use
    */
    public void startElement (String uri, String localname, String rawname, Attributes atts)
    throws SAXException
    {
        //System.err.println("ReceivingContentHandler#startElement " + uri + "," + localname + "," + rawname + " at line " + locator.getLineNumber());
        //for (int a=0; a<atts.getLength(); a++) {
        //     System.err.println("  Attribute " + atts.getURI(a) + "/" + atts.getLocalName(a) + "/" + atts.getQName(a));
        //}
        try {
            flush();

    		int nameCode = getNameCode(uri, localname, rawname);
    		receiver.startElement(nameCode, -1, 0, 0);

    		for (int n=0; n<namespacesUsed; n++) {
    		    receiver.namespace(namespaces[n], 0);
    		}


    		for (int a=0; a<atts.getLength(); a++) {
                int properties = 0;
    		    int attCode = getNameCode(atts.getURI(a), atts.getLocalName(a), atts.getQName(a));
    		    String type = atts.getType(a);
    		    int typeCode = -1;
                if (getConfiguration().isRetainDTDAttributeTypes()) {
                    if (type.equals("CDATA")) {
                        // no action
                    } else if (type.equals("ID")) {
                        typeCode = StandardNames.XS_ID;
                    } else if (type.equals("IDREF")) {
                        typeCode = StandardNames.XS_IDREF;
                    } else if (type.equals("IDREFS")) {
                        typeCode = StandardNames.XS_IDREFS;
                    } else if (type.equals("NMTOKEN")) {
                        typeCode = StandardNames.XS_NMTOKEN;
                    } else if (type.equals("NMTOKENS")) {
                        typeCode = StandardNames.XS_NMTOKENS;
                    } else if (type.equals("ENTITY")) {
                        typeCode = StandardNames.XS_ENTITY;
                    } else if (type.equals("ENTITIES")) {
                        typeCode = StandardNames.XS_ENTITIES;
                    }
                } else {
                    if (type.equals("ID")) {
                        properties |= ReceiverOptions.DTD_ID_ATTRIBUTE;
                    } else if (type.equals("IDREF") || type.equals("IDREFS")) {
                        properties |= ReceiverOptions.DTD_IDREFS_ATTRIBUTE;
                    }
                }

    		    receiver.attribute(attCode, typeCode, atts.getValue(a), 0, properties);
    		}

    		receiver.startContent();

            namespacesUsed = 0;
//        } catch (ValidationException err) {
//            err.setLocator(locator);
//            throw new SAXException(err);
        } catch (XPathException err) {
            throw new SAXException(err);
        }
    }

    private int getNameCode(String uri, String localname, String rawname) throws SAXException {
        // System.err.println("URI=" + uri + " local=" + " raw=" + rawname);
        // The XML parser isn't required to report the rawname (qname), though most of them do.
        // If none is provided, we give up
        if (rawname.equals("")) {
            throw new SAXException("Saxon requires an XML parser that reports the QName of each element");
        }
        // It's also possible (especially when using a TransformerHandler) that the parser
        // has been configured to report the QName rather than the localname+URI
        if (localname.equals("")) {
            throw new SAXException("Parser configuration problem: namespace reporting is not enabled");
        }
        String prefix = Name.getPrefix(rawname);
        return pool.allocate(prefix, uri, localname);
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endElement (String uri, String localname, String rawname) throws SAXException {
        //System.err.println("ReceivingContentHandler#End element " + rawname);
        try {
            flush();
            receiver.endElement();
        } catch (ValidationException err) {
            err.setLocator(locator);
            throw new SAXException(err);
        } catch (XPathException err) {
            throw new SAXException(err);
        }
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void characters (char ch[], int start, int length) throws SAXException {
        // System.err.println("characters (" + length + ")");
        // need to concatenate chunks of text before we can decide whether a node is all-white

        while (used + length > buffer.length) {
            char[] newbuffer = new char[buffer.length*2];
            System.arraycopy(buffer, 0, newbuffer, 0, used);
            buffer = newbuffer;
        }
        System.arraycopy(ch, start, buffer, used, length);
        used += length;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void ignorableWhitespace (char ch[], int start, int length) throws SAXException {
        characters(ch, start, length);
    }

    /**
    * Callback interface for SAX: not for application use<BR>
    */

    public void processingInstruction (String name, String remainder) throws SAXException {
        try {
            flush();
            if (!inDTD) {
                if (name==null) {
                	// trick used by some SAX1 parsers to notify a comment
                	comment(remainder.toCharArray(), 0, remainder.length());
                } else {
                    // some parsers allow through PI names containing colons
                    if (!XMLChar.isValidNCName(name)) {
                        throw new SAXException("Invalid processing instruction name (" + name + ')');
                    }
                	receiver.processingInstruction(name, remainder, 0, 0);
                }
            }
        } catch (XPathException err) {
            throw new SAXException(err);
        }
    }

    /**
    * Callback interface for SAX (part of LexicalHandler interface): not for application use
    */

    public void comment (char ch[], int start, int length) throws SAXException {
        try {
            flush();
            if (!inDTD) {
            	receiver.comment(new CharSlice(ch, start, length), 0, 0);
            }
        } catch (XPathException err) {
            throw new SAXException(err);
        }
    }

    /**
    * Flush buffer for accumulated character data, suppressing white space if appropriate
    */

    private void flush() throws XPathException {
        if (used > 0) {
            receiver.characters(new CharSlice(buffer, 0, used), 0, 0);
            used = 0;
        }
    }

    public void skippedEntity(String name) throws SAXException {}

    // No-op methods to satisfy lexical handler interface

	/**
	* Register the start of the DTD. Comments in the DTD are skipped because they
	* are not part of the XPath data model
	*/

    public void startDTD (String name, String publicId, String systemId) throws SAXException {
		inDTD = true;
    }

	/**
	* Register the end of the DTD. Comments in the DTD are skipped because they
	* are not part of the XPath data model
	*/

    public void endDTD () throws SAXException {
		inDTD = false;
    }

    public void startEntity (String name) throws SAXException {};

    public void endEntity (String name)	throws SAXException {};

    public void startCDATA () throws SAXException {};

    public void endCDATA ()	throws SAXException {};

    //////////////////////////////////////////////////////////////////////////////
    // Implement DTDHandler interface
    //////////////////////////////////////////////////////////////////////////////


    public void notationDecl(       String name,
                                    String publicId,
                                    String systemId) throws SAXException
    {}


    public void unparsedEntityDecl( String name,
                                    String publicId,
                                    String systemId,
                                    String notationName) throws SAXException
    {
        //System.err.println("Unparsed entity " + name + "=" + systemId);

        // Some SAX parsers report the systemId as written. We need to turn it into
        // an absolute URL.

        String uri = systemId;
        if (locator!=null) {
            try {
                String baseURI = locator.getSystemId();
                URI absoluteURI = new URI(baseURI).resolve(systemId);
                uri = absoluteURI.toString();
            } catch (URISyntaxException err) {}
        }
        try {
            receiver.setUnparsedEntity(name, uri, publicId);
        } catch (XPathException err) {
            throw new SAXException(err);
        }
    }

    // implement the SaxonLocator interface. This is needed to bridge a SAX Locator to a JAXP SourceLocator

    /**
     * Return the public identifier for the current document event.
     * @return A string containing the system identifier, or
     *         null if none is available.
     */

    public String getSystemId() {
        if (locator == null) {
            return null;
        } else {
            return locator.getSystemId();
        }
    }

    /**
     * Return the public identifier for the current document event.
     * @return A string containing the public identifier, or
     *         null if none is available.
     */

    public String getPublicId() {
        if (locator==null) {
            return null;
        } else {
            return locator.getPublicId();
        }
    }

    /**
     * Return the line number where the current document event ends.
     * @return The line number, or -1 if none is available.
     */

    public int getLineNumber() {
        if (locator==null) {
            return -1;
        } else {
            return locator.getLineNumber();
        }
    }

    /**
     * Return the character position where the current document event ends.
     * @return The column number, or -1 if none is available.
     */

    public int getColumnNumber() {
        if (locator==null) {
            return -1;
        } else {
            return locator.getColumnNumber();
        }
    }

    public String getSystemId(int locationId) {
        return getSystemId();
    }

    public int getLineNumber(int locationId) {
        return getLineNumber();
    }

}   // end of class ReceivingContentHandler

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
