package org.orbeon.saxon.event;

import org.orbeon.saxon.om.AttributeCollectionImpl;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.Navigator;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaException;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.transform.Result;
import java.util.Properties;
import java.util.Stack;

/**
 * A ContentHandlerProxy is an Emitter that filters data before passing it to an
 * underlying SAX2 ContentHandler. Relevant events (notably comments) can also be
 * fed to a LexicalHandler.
 * <p/>
 * Note that in general the output passed to an Emitter
 * corresponds to an External General Parsed Entity. A SAX2 ContentHandler only expects
 * to deal with well-formed XML documents, so we only pass it the contents of the first
 * element encountered.
 * </p><p>
 * This ContentHandlerProxy provides no access to type information. For a ContentHandler that
 * makes type information available, see {@link org.orbeon.saxon.dom.TypedContentHandler}
 */

public class ContentHandlerProxy extends Emitter implements Locator {
    protected ContentHandler handler;
    protected LexicalHandler lexicalHandler;
    private LocationProvider locationProvider;
    private int depth = 0;
    private boolean requireWellFormed = false;
    private boolean undeclareNamespaces = false;
    private Stack elementStack = new Stack();
    private Stack namespaceStack = new Stack();
    protected AttributeCollectionImpl pendingAttributes;
    private int pendingElement = -1;
    private int currentLocationId;

    private static final String marker = "##";

    /**
     * Set the underlying content handler. This call is mandatory before using the Emitter.
     */

    public void setUnderlyingContentHandler(ContentHandler handler) {
        this.handler = handler;
        if (handler instanceof LexicalHandler) {
            this.lexicalHandler = (LexicalHandler)handler;
        }
    }

    /**
     * Get the underlying content handler
     */

    public ContentHandler getUnderlyingContentHandler() {
        return handler;
    }

    /**
     * Set the Lexical Handler to be used. If called, this must be called AFTER
     * setUnderlyingContentHandler()
     */

    public void setLexicalHandler(LexicalHandler handler) {
        this.lexicalHandler = handler;
    }

	/**
	* Set the pipeline configuration
	*/

	public void setPipelineConfiguration(PipelineConfiguration config) {
        super.setPipelineConfiguration(config);
	    this.locationProvider = config.getLocationProvider();
	}

    /**
     * Set the output details.
     */

    public void setOutputProperties(Properties details) throws XPathException {
        String prop = details.getProperty(SaxonOutputKeys.REQUIRE_WELL_FORMED);
        if (prop != null) {
            requireWellFormed = prop.equals("yes");
        }
        prop = details.getProperty(SaxonOutputKeys.UNDECLARE_PREFIXES);
        if (prop != null) {
            undeclareNamespaces = prop.equals("yes");
        }
        super.setOutputProperties(details);
    }

    /**
     * Determine whether the content handler can handle a stream of events that is merely
     * well-balanced, or whether it can only handle a well-formed sequence.
     */

    public boolean isRequireWellFormed() {
        return requireWellFormed;
    }

    /**
     * Indicate whether the content handler can handle a stream of events that is merely
     * well-balanced, or whether it can only handle a well-formed sequence.
     */

    public void setRequireWellFormed(boolean wellFormed) {
        requireWellFormed = wellFormed;
    }

    /**
     * Determine whether namespace undeclaration events (for a non-null prefix) should be notified.
     * The default is no, because some ContentHandlers (e.g. JDOM) can't cope with them.
     *
     * @return true if namespace undeclarations (xmlns:p="") are output
     */

    public boolean isUndeclareNamespaces() {
        return undeclareNamespaces;
    }

    /**
     * Determine whether namespace undeclaration events (for a non-null prefix) should be notified.
     * The default is no, because some ContentHandlers (e.g. JDOM) can't cope with them.
     *
     * @param undeclareNamespaces true if namespace undeclarations (xmlns:p="") are to be output
     */

    public void setUndeclareNamespaces(boolean undeclareNamespaces) {
        this.undeclareNamespaces = undeclareNamespaces;
    }

    /**
     * Start of document
     */

    public void open() throws XPathException {
        pendingAttributes = new AttributeCollectionImpl(getPipelineConfiguration().getConfiguration().getNamePool());
        if (handler == null) {
            throw new DynamicError("ContentHandlerProxy.startDocument(): no underlying handler provided");
        }
        try {
            locationProvider = getPipelineConfiguration().getLocationProvider();
            pendingAttributes.setLocationProvider(locationProvider);
            handler.setDocumentLocator(this);
            handler.startDocument();
        } catch (SAXException err) {
            throw new DynamicError(err);
        }
        depth = 0;
    }

    /**
     * End of document
     */

    public void close() throws XPathException {
        try {
            handler.endDocument();
        } catch (SAXException err) {
            throw new DynamicError(err);
        }
    }

    /**
     * Start of a document node.
     */

    public void startDocument(int properties) throws XPathException {
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
    }

    /**
     * Notify the start of an element
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        depth++;
        if (depth <= 0 && requireWellFormed) {
            notifyNotWellFormed();
        }
        pendingElement = nameCode;
        currentLocationId = locationId;
        namespaceStack.push(marker);
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element.
     */

    public void namespace(int namespaceCode, int properties) throws XPathException {
        if (namespaceCode == NamespaceConstant.XML_NAMESPACE_CODE) {
            return;
        }
        String prefix = namePool.getPrefixFromNamespaceCode(namespaceCode);
        String uri = namePool.getURIFromNamespaceCode(namespaceCode);
        if ((!undeclareNamespaces) && "".equals(uri) && !("".equals(prefix))) {
            return;
        }
        try {
            handler.startPrefixMapping(prefix, uri);
            namespaceStack.push(prefix);
        } catch (SAXException err) {
            throw new DynamicError(err);
        }
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children.
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
            throws XPathException {
        int index = pendingAttributes.getIndexByFingerprint(nameCode & 0xfffff);
        if (index < 0) {
            pendingAttributes.addAttribute(nameCode, typeCode, value.toString(), locationId, properties);
        } else {
            pendingAttributes.setAttribute(index, nameCode, typeCode, value.toString(), locationId, properties);
        }
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */

    public void startContent() throws XPathException {
        try {
            if (depth > 0 || !requireWellFormed) {
                String uri = namePool.getURI(pendingElement);
                String localName = namePool.getLocalName(pendingElement);
                String qname = namePool.getDisplayName(pendingElement);

                handler.startElement(uri,
                        localName,
                        qname,
                        pendingAttributes);

                elementStack.push(uri);
                elementStack.push(localName);
                elementStack.push(qname);

                pendingAttributes.clear();
                pendingElement = -1;
            }
        } catch (SAXException err) {
            Exception nested = err.getException();
            if (nested instanceof XPathException) {
                throw (XPathException)nested;
            } else if (nested instanceof SchemaException) {
                throw new DynamicError(nested);
            } else {
                throw new DynamicError(err);
            }
        }
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        if (depth > 0) {
            try {
                String qname = (String)elementStack.pop();
                String localName = (String)elementStack.pop();
                String uri = (String)elementStack.pop();
                handler.endElement(uri, localName, qname);
            } catch (SAXException err) {
                throw new DynamicError(err);
            }
        }

        while (true) {
            String prefix = (String)namespaceStack.pop();
            if (prefix.equals(marker)) {
                break;
            }
            try {
                handler.endPrefixMapping(prefix);
            } catch (SAXException err) {
                throw new DynamicError(err);
            }
        }
        depth--;
        // if this was the outermost element, and well formed output is required
        // then no further elements will be processed
        if (requireWellFormed && depth <= 0) {
            depth = Integer.MIN_VALUE;     // crude but effective
        }

    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        currentLocationId = locationId;
        boolean disable = ((properties & ReceiverOptions.DISABLE_ESCAPING) != 0);
        if (disable) {
            setEscaping(false);
        }
        try {
            if (depth <= 0 && requireWellFormed) {
                if (Navigator.isWhite(chars)) {
                    // ignore top-level white space
                } else {
                    notifyNotWellFormed();
                }
            } else {
                handler.characters(chars.toString().toCharArray(), 0, chars.length());
            }
        } catch (SAXException err) {
            throw new DynamicError(err);
        }
        if (disable) {
            setEscaping(true);
        }
    }

    /**
     * The following function is called when it is found that the output is not a well-formed document.
     * Unless the ContentHandler accepts "balanced content", this is a fatal error.
     */

    protected void notifyNotWellFormed() throws XPathException {
        throw new DynamicError("The result tree cannot be supplied to the ContentHandler because it is not well-formed XML");
    }


    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties)
            throws XPathException {
        currentLocationId = locationId;
        try {
            handler.processingInstruction(target, data.toString());
        } catch (SAXException err) {
            throw new DynamicError(err);
        }
    }

    /**
     * Output a comment. Passes it on to the ContentHandler provided that the ContentHandler
     * is also a SAX2 LexicalHandler.
     */

    public void comment(CharSequence chars, int locationId, int properties)
            throws XPathException {
        currentLocationId = locationId;
        try {
            if (lexicalHandler != null) {
                lexicalHandler.comment(chars.toString().toCharArray(), 0, chars.length());
            }
        } catch (SAXException err) {
            throw new DynamicError(err);
        }
    }


    /**
     * Switch escaping on or off. This is called when the XSLT disable-output-escaping attribute
     * is used to switch escaping on or off. It is not called for other sections of output (e.g.
     * element names) where escaping is inappropriate. The action, as defined in JAXP 1.1, is
     * to notify the request to the Content Handler using a processing instruction.
     */

    private void setEscaping(boolean escaping) {
        try {
            handler.processingInstruction((escaping ? Result.PI_ENABLE_OUTPUT_ESCAPING : PI_DISABLE_OUTPUT_ESCAPING),
                    "");
        } catch (SAXException err) {
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Implementation of Locator interface
    ////////////////////////////////////////////////////////////////////

    /**
     * Get the Public ID
     * @return null (always)
     */

    public String getPublicId() {
        return null;
    }

    /**
     * Get the System ID
     * @return the system ID giving the location of the most recent event notified
     */

    public String getSystemId() {
        return locationProvider.getSystemId(currentLocationId);
    }

    /**
     * Get the line number
     * @return the line number giving the location of the most recent event notified
     */

    public int getLineNumber() {
        return locationProvider.getLineNumber(currentLocationId);
    }

    /**
     * Get the column number
     * @return -1 (always)
     */

    public int getColumnNumber() {
        return -1;
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
