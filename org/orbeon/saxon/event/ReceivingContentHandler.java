package org.orbeon.saxon.event;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.tinytree.CharSlice;
import org.orbeon.saxon.tinytree.CompressedWhitespace;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ValidationException;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.transform.TransformerException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
  * ReceivingContentHandler is a glue class that provides a standard SAX ContentHandler
  * interface to a Saxon Receiver. To achieve this it needs to map names supplied
  * as strings to numeric name codes, for which purpose it needs access to a name
  * pool. The class also performs the function of assembling adjacent text nodes.
  * <p>The class was previously named ContentEmitter.</p>
  * @author Michael H. Kay
  */

public class ReceivingContentHandler
        implements ContentHandler, LexicalHandler, DTDHandler, SaxonLocator, SourceLocationProvider
{
    private NamePool pool;
    private PipelineConfiguration pipe;
    private Receiver receiver;
    private boolean inDTD = false;	// true while processing the DTD
    private Locator locator;        // a SAX Locator

    // buffer for accumulating character data, until the next markup event is received

    private char[] buffer = new char[512];
    private int charsUsed = 0;
    private CharSlice slice = new CharSlice(buffer, 0, 0);

    // array for accumulating namespace information

    private int[] namespaces = new int[20];
    private int namespacesUsed = 0;

    // determine whether ignorable whitespace is ignored

    private boolean ignoreIgnorable = false;

    // determine whether DTD attribute types are retained

    private boolean retainDTDAttributeTypes = false;

    /**
     * A local cache is used to avoid allocating namecodes for the same name more than once.
     * This reduces contention on the NamePool. This is a two-level hashmap: the first level
     * has the namespace URI as its key, and returns a HashMap which maps lexical QNames to integer
     * namecodes.
     */

    private HashMap cache = new HashMap(10);
    private HashMap noNamespaceMap;


    /**
    * create a ReceivingContentHandler and initialise variables
    */

    public ReceivingContentHandler() {
    }

    /**
     * Set the ReceivingContentHandler to its initial state, except for the local name cache,
     * which is retained
     */

    public void reset() {
        pipe = null;
        pool = null;
        receiver = null;
        ignoreIgnorable = false;
        retainDTDAttributeTypes = false;
        charsUsed = 0;
        slice.setLength(0);
        namespacesUsed = 0;
        locator = null;
    }

	public void setReceiver(Receiver e) {
		receiver = e;
        //receiver = new TracingFilter(e);
	}

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
        pipe.setLocationProvider(this);
        Configuration config = pipe.getConfiguration();
        this.pool = config.getNamePool();
        ignoreIgnorable = config.getStripsWhiteSpace() != Whitespace.NONE;
        retainDTDAttributeTypes = config.isRetainDTDAttributeTypes();
    }

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    public Configuration getConfiguration() {
        return pipe.getConfiguration();
    }

    /**
     * Set whether "ignorable whitespace" should be ignored. This method is effective only
     * if called after setPipelineConfiguration, since the default value is taken from the
     * configuration.
     */

    public void setIgnoreIgnorableWhitespace(boolean ignore) {
        ignoreIgnorable = ignore;
    }

    /**
     * Determine whether "ignorable whitespace" is ignored. This returns the value that was set
     * using {@link #setIgnoreIgnorableWhitespace} if that has been called; otherwise the value
     * from the configuration.
     */

    public boolean isIgnoringIgnorableWhitespace() {
        return ignoreIgnorable;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void startDocument () throws SAXException {
        // System.err.println("ReceivingContentHandler#startDocument");
        try {
            charsUsed = 0;
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
        // System.err.println("RCH: end document");
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
        if (prefix.equals("xmlns")) {
            // the binding xmlns:xmlns="http://www.w3.org/2000/xmlns/"
            // should never be reported, but it's been known to happen
            return;
        }
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
//        System.err.println("ReceivingContentHandler#startElement " + uri + "," + localname + "," + rawname + " at line " + locator.getLineNumber());
        //for (int a=0; a<atts.getLength(); a++) {
        //     System.err.println("  Attribute " + atts.getURI(a) + "/" + atts.getLocalName(a) + "/" + atts.getQName(a));
        //}
        try {
            flush();

    		int nameCode = getNameCode(uri, localname, rawname);
    		receiver.startElement(nameCode, StandardNames.XDT_UNTYPED, 0, 0);

    		for (int n=0; n<namespacesUsed; n++) {
    		    receiver.namespace(namespaces[n], 0);
    		}

    		for (int a=0; a<atts.getLength(); a++) {
                int properties = 0;
                String qname = atts.getQName(a);
                if (qname.startsWith("xmlns") && (qname.equals("xmlns") || qname.startsWith("xmlns:"))) {
                    // We normally configure the parser so that it doesn't notify namespaces as attributes.
                    // But when running as a TransformerHandler, we have no control over the feature settings
                    // of the sender of the events. So we filter them out, just in case. There might be cases
                    // where we ought not just to ignore them, but to handle them as namespace events, but
                    // we'll cross that bridge when we come to it.
                    continue;
                }
    		    int attCode = getNameCode(atts.getURI(a), atts.getLocalName(a), atts.getQName(a));
    		    String type = atts.getType(a);
    		    int typeCode = StandardNames.XDT_UNTYPED_ATOMIC;
                if (retainDTDAttributeTypes) {
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
                        typeCode = StandardNames.XS_ID | NodeInfo.IS_DTD_TYPE;
                    } else if (type.equals("IDREF")) {
                        typeCode = StandardNames.XS_IDREF | NodeInfo.IS_DTD_TYPE;
                    } else if (type.equals("IDREFS")) {
                        typeCode = StandardNames.XS_IDREFS | NodeInfo.IS_DTD_TYPE;
                    }
                }

    		    receiver.attribute(attCode, typeCode, atts.getValue(a), 0, properties);
    		}

    		receiver.startContent();

            namespacesUsed = 0;
        } catch (ValidationException err) {
            if (err.getLineNumber() == -1) {
                err.setLocator(locator);
            }
            throw new SAXException(err);
        } catch (XPathException err) {
            throw new SAXException(err);
        }
    }

    private int getNameCode(String uri, String localname, String rawname) throws SAXException {
        // System.err.println("URI=" + uri + " local=" + " raw=" + rawname);
        // The XML parser isn't required to report the rawname (qname), though all known parsers do.
        // If none is provided, we give up
        if (rawname.equals("")) {
            throw new SAXException("Saxon requires an XML parser that reports the QName of each element");
        }
        // It's also possible (especially when using a TransformerHandler) that the parser
        // has been configured to report the QName rather than the localname+URI
        if (localname.equals("")) {
            throw new SAXException("Parser configuration problem: namespace reporting is not enabled");
        }

        // Following code maintains a local cache to remember all the namecodes that have been
        // allocated, which reduces contention on the NamePool. It also avoid parsing the lexical QName
        // when the same name is used repeatedly. We also get a tiny improvement by avoiding the first hash
        // table lookup for names in the null namespace.

        HashMap map2 = (uri.equals("") ? noNamespaceMap : (HashMap)cache.get(uri));
        if (map2 == null) {
            map2 = new HashMap(50);
            cache.put(uri, map2);
            if (uri.equals("")) {
                noNamespaceMap = map2;
            }
        }

        Integer n = (Integer)map2.get(rawname);
        // we use the rawname (qname) rather than the local name because we want a namecode rather than
        // a fingerprint - that is, the prefix matters.
        if (n == null) {
            String prefix = NameChecker.getPrefix(rawname);
            int nc = pool.allocate(prefix, uri, localname);
            n = new Integer(nc);
            map2.put(rawname, n);
            return nc;
        } else {
            return n.intValue();
        }

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
            if (!err.hasBeenReported()) {
                try {
                    pipe.getErrorListener().fatalError(err);
                } catch (TransformerException e) {
                    //
                }
            }
            err.setHasBeenReported();
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

        while (charsUsed + length > buffer.length) {
            char[] newbuffer = new char[buffer.length*2];
            System.arraycopy(buffer, 0, newbuffer, 0, charsUsed);
            buffer = newbuffer;
            slice = new CharSlice(buffer, 0, 0);
        }
        System.arraycopy(ch, start, buffer, charsUsed, length);
        charsUsed += length;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void ignorableWhitespace (char ch[], int start, int length) throws SAXException {
        if (!ignoreIgnorable) {
            characters(ch, start, length);
        }
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
                    if (!getConfiguration().getNameChecker().isValidNCName(name)) {
                        throw new SAXException("Invalid processing instruction name (" + name + ')');
                    }
                	receiver.processingInstruction(name, Whitespace.removeLeadingWhitespace(remainder), 0, 0);
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
    * Flush buffer for accumulated character data
    */

    private void flush() throws XPathException {
        if (charsUsed > 0) {
            slice.setLength(charsUsed);
            CharSequence cs = CompressedWhitespace.compress(slice);
            receiver.characters(cs, 0, ReceiverOptions.WHOLE_TEXT_NODE);
            charsUsed = 0;
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
