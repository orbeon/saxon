package org.orbeon.saxon.event;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.AttributeCollectionImpl;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaException;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.transform.Result;
import java.util.Properties;
import java.util.Stack;

/**
 * A ContentHandlerProxy is a Receiver that converts events into the form expected by an
 * underlying SAX2 ContentHandler. Relevant events (notably comments) can also be
 * fed to a LexicalHandler.
 * <p/>
 * Note that in general the output passed to a Receiver
 * corresponds to an External General Parsed Entity. A SAX2 ContentHandler only expects
 * to deal with well-formed XML documents, so we only pass it the contents of the first
 * element encountered, unless the saxon:require-well-formed output property is set to "no".
 * </p><p>
 * This ContentHandlerProxy provides no access to type information. For a ContentHandler that
 * makes type information available, see {@link com.saxonica.jaxp.TypedContentHandler}
 * <p></p>
 * The ContentHandlerProxy can also be nominated as a TraceListener, to receive notification
 * of trace events. This will be done automatically if the option setTraceListener(
 */

public class ContentHandlerProxy implements Receiver {
    private PipelineConfiguration pipe;
    private String systemId;
    protected ContentHandler handler;
    protected LexicalHandler lexicalHandler;
    private LocationProvider locationProvider;
    private int depth = 0;
    private boolean requireWellFormed = false;
    private boolean undeclareNamespaces = false;
    private Stack elementStack = new Stack();
    private Stack namespaceStack = new Stack();
    private ContentHandlerProxyTraceListener traceListener;
    protected AttributeCollectionImpl pendingAttributes;
    private int pendingElement = -1;
    private long currentLocationId;

    // MARKER is a value added to the namespace stack at the start of an element, so that we know how
    // far to unwind the stack on an end-element event.

    private static final String MARKER = "##";

    /**
     * Set the underlying content handler. This call is mandatory before using this Receiver.
     * If the content handler is an instance of {@link LexicalHandler}, then it will also receive
     * notification of lexical events such as comments.
     * @param handler the SAX content handler to which all events will be directed
     */

    public void setUnderlyingContentHandler(ContentHandler handler) {
        this.handler = handler;
        if (handler instanceof LexicalHandler) {
            lexicalHandler = (LexicalHandler)handler;
        }
    }

    /**
     * Get the underlying content handler
     * @return the SAX content handler to which all events are being directed
     */

    public ContentHandler getUnderlyingContentHandler() {
        return handler;
    }

    /**
     * Set the Lexical Handler to be used. If called, this must be called AFTER
     * setUnderlyingContentHandler()
     * @param handler the SAX lexical handler to which lexical events (such as comments) will
     * be notified.
     */

    public void setLexicalHandler(LexicalHandler handler) {
        lexicalHandler = handler;
    }

	/**
	 * Set the pipeline configuration
     * @param pipe the pipeline configuration
	*/

	public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
        locationProvider = pipe.getLocationProvider();
	}

    /**
     * Get the pipeline configuration
     */

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    /**
     * Get the Saxon configuration
     * @return the Saxon configuration
     */

    public Configuration getConfiguration() {
        return pipe.getConfiguration();
    }

    /**
     * Set the System ID of the destination tree
     * @param systemId the system ID (effectively the base URI)
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the System ID of the destination tree
     * @return the system ID (effectively the base URI)
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Get the associated TraceListener that receives notification of trace events
     * @return the trace listener. If there is no existing trace listener, then a new one
     * will be created.
     */

    public ContentHandlerProxyTraceListener getTraceListener() {
        if (traceListener == null) {
            traceListener = new ContentHandlerProxyTraceListener();
        }
        return traceListener;
    }

    /**
     * Get the location provider
     * @return the location provider, used to map location ids to actual URIs and line numbers
     */

    public LocationProvider getLocationProvider() {
        return locationProvider;
    }

    /**
     * Get the current location identifier
     * @return the location identifier of the most recent event. This can be translated to real
     * location information by passing it to the location provider.
     */

    public long getCurrentLocationId() {
        return currentLocationId;
    }

    /**
     * Notify an unparsed entity URI. This implementation does nothing: the event is ignored.
     *
     * @param name     The name of the unparsed entity
     * @param systemID The system identifier of the unparsed entity
     * @param publicID The public identifier of the unparsed entity
     */

    public void setUnparsedEntity(String name, String systemID, String publicID) throws XPathException {
        // no-op
    }

    /**
     * Set the output details.
     * @param details the serialization properties. The only values used by this implementation are
     * {@link SaxonOutputKeys#REQUIRE_WELL_FORMED} and {@link SaxonOutputKeys#UNDECLARE_PREFIXES}.
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
    }

    /**
     * Ask whether the content handler can handle a stream of events that is merely
     * well-balanced, or whether it can only handle a well-formed sequence.
     * @return true if the content handler requires the event stream to represent a well-formed
     * XML document (containing exactly one top-level element node and no top-level text nodes)
     */

    public boolean isRequireWellFormed() {
        return requireWellFormed;
    }

    /**
     * Set whether the content handler can handle a stream of events that is merely
     * well-balanced, or whether it can only handle a well-formed sequence. The default is false.
     * @param wellFormed set to true if the content handler requires the event stream to represent a well-formed
     * XML document (containing exactly one top-level element node and no top-level text nodes). Otherwise,
     * multiple top-level elements and text nodes are allowed, as in the XDM model.
     */

    public void setRequireWellFormed(boolean wellFormed) {
        requireWellFormed = wellFormed;
    }

    /**
     * Ask whether namespace undeclaration events (for a non-null prefix) should be notified.
     * The default is no, because some ContentHandlers (e.g. JDOM) can't cope with them.
     *
     * @return true if namespace undeclarations (xmlns:p="") are to be output
     */

    public boolean isUndeclareNamespaces() {
        return undeclareNamespaces;
    }

    /**
     * Set whether namespace undeclaration events (for a non-null prefix) should be notified.
     * The default is no, because some ContentHandlers (e.g. JDOM) can't cope with them.
     *
     * @param undeclareNamespaces true if namespace undeclarations (xmlns:p="") are to be output
     */

    public void setUndeclareNamespaces(boolean undeclareNamespaces) {
        this.undeclareNamespaces = undeclareNamespaces;
    }

    /**
     * Notify the start of the event stream
     */

    public void open() throws XPathException {
        pendingAttributes = new AttributeCollectionImpl(getPipelineConfiguration().getConfiguration());
        if (handler == null) {
            throw new IllegalStateException("ContentHandlerProxy.open(): no underlying handler provided");
        }
        try {
            locationProvider = getPipelineConfiguration().getLocationProvider();
            pendingAttributes.setLocationProvider(locationProvider);
            Locator locator = new ContentHandlerProxyLocator(this);
            handler.setDocumentLocator(locator);
            handler.startDocument();
        } catch (SAXException err) {
            handleSAXException(err);
        }
        depth = 0;
    }

    /**
     * Notify the end of the event stream
     */

    public void close() throws XPathException {
        try {
            handler.endDocument();
        } catch (SAXException err) {
            handleSAXException(err);
        }
    }

    /**
     * Notify the start of the document.
     */

    public void startDocument(int properties) throws XPathException {
    }

    /**
     * Notify the end of the document
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
        namespaceStack.push(MARKER);
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element.
     */

    public void namespace(int namespaceCode, int properties) throws XPathException {
        if (namespaceCode == NamespaceConstant.XML_NAMESPACE_CODE) {
            return;
        }
        NamePool pool = pipe.getConfiguration().getNamePool();
        String prefix = pool.getPrefixFromNamespaceCode(namespaceCode);
        String uri = pool.getURIFromNamespaceCode(namespaceCode);
        if ((!undeclareNamespaces) && uri.length()==0 && !(prefix.length()==0)) {
            // This is a namespace undeclaration, but the ContentHandler doesn't want to know about undeclarations
            return;
        }
        try {
            handler.startPrefixMapping(prefix, uri);
            namespaceStack.push(prefix);
        } catch (SAXException err) {
            handleSAXException(err);
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
            NamePool namePool = pipe.getConfiguration().getNamePool();
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
            handleSAXException(err);

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
                handleSAXException(err);
            }
        }

        while (true) {
            String prefix = (String)namespaceStack.pop();
            if (prefix.equals(MARKER)) {
                break;
            }
            try {
                handler.endPrefixMapping(prefix);
            } catch (SAXException err) {
                handleSAXException(err);
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
                if (Whitespace.isWhite(chars)) {
                    // ignore top-level white space
                } else {
                    notifyNotWellFormed();
                }
            } else {
                handler.characters(chars.toString().toCharArray(), 0, chars.length());
            }
        } catch (SAXException err) {
            handleSAXException(err);
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
        XPathException err = new XPathException("The result tree cannot be supplied to the ContentHandler because it is not well-formed XML");
        err.setErrorCode(SaxonErrorCode.SXCH0002);
        throw err;
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
            handleSAXException(err);
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
            handleSAXException(err);
        }
    }


    /**
     * Switch escaping on or off. This is called when the XSLT disable-output-escaping attribute
     * is used to switch escaping on or off. It is not called for other sections of output (e.g.
     * element names) where escaping is inappropriate. The action, as defined in JAXP 1.1, is
     * to notify the request to the Content Handler using a processing instruction.
     * @param escaping true if escaping is to be switched on, false to switch it off
     */

    private void setEscaping(boolean escaping) {
        try {
            handler.processingInstruction(
                    (escaping ? Result.PI_ENABLE_OUTPUT_ESCAPING : PI_DISABLE_OUTPUT_ESCAPING),
                    "");
        } catch (SAXException err) {
            throw new AssertionError(err);
        }
    }


    /**
     * Handle a SAXException thrown by the ContentHandler
     * @param err the exception to be handler
     * @throws XPathException always
     */

    private void handleSAXException(SAXException err) throws XPathException {
        Exception nested = err.getException();
        if (nested instanceof XPathException) {
            throw (XPathException)nested;
        } else if (nested instanceof SchemaException) {
            throw new XPathException(nested);
        } else {
            XPathException de = new XPathException(err);
            de.setErrorCode(SaxonErrorCode.SXCH0003);
            throw de;
        }
    }

    /**
     * Create a TraceListener that will collect information about the current
     * location in the source document. This is used to provide information
     * to the receiving application for diagnostic purposes.
     */

    public class ContentHandlerProxyTraceListener implements TraceListener {

        private Stack contextItemStack;

        /**
         * Get the context item stack
         * @return the context item stack
         */

        public Stack getContextItemStack() {
            return contextItemStack;
        }

        /**
         * Method called at the start of execution, that is, when the run-time transformation starts
         */

        public void open() {
            contextItemStack = new Stack();
        }

        /**
         * Method called at the end of execution, that is, when the run-time execution ends
         */

        public void close() {
            contextItemStack = null;
        }

        /**
         * Method that is called when an instruction in the stylesheet gets processed.
         *
         * @param instruction gives information about the instruction being
         *                    executed, and about the context in which it is executed. This object is mutable,
         *                    so if information from the InstructionInfo is to be retained, it must be copied.
         */

        public void enter(InstructionInfo instruction, XPathContext context) {
            // do nothing
        }

        /**
         * Method that is called after processing an instruction of the stylesheet,
         * that is, after any child instructions have been processed.
         *
         * @param instruction gives the same information that was supplied to the
         *                    enter method, though it is not necessarily the same object. Note that the
         *                    line number of the instruction is that of the start tag in the source stylesheet,
         *                    not the line number of the end tag.
         */

        public void leave(InstructionInfo instruction) {
            // do nothing
        }

        /**
         * Method that is called by an instruction that changes the current item
         * in the source document: that is, xsl:for-each, xsl:apply-templates, xsl:for-each-group.
         * The method is called after the enter method for the relevant instruction, and is called
         * once for each item processed.
         *
         * @param currentItem the new current item. Item objects are not mutable; it is safe to retain
         *                    a reference to the Item for later use.
         */

        public void startCurrentItem(Item currentItem) {
            if (contextItemStack == null) {
                open();
            }
            contextItemStack.push(currentItem);
        }

        /**
         * Method that is called when an instruction has finished processing a new current item
         * and is ready to select a new current item or revert to the previous current item.
         * The method will be called before the leave() method for the instruction that made this
         * item current.
         *
         * @param currentItem the item that was current, whose processing is now complete. This will represent
         *                    the same underlying item as the corresponding startCurrentItem() call, though it will
         *                    not necessarily be the same actual object.
         */

        public void endCurrentItem(Item currentItem) {
            contextItemStack.pop();
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
