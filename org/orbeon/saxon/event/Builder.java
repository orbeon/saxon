package org.orbeon.saxon.event;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.StrippedDocument;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.tinytree.TinyDocumentImpl;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.tree.TreeBuilder;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import java.util.Date;

/**
 * The abstract Builder class is responsible for taking a stream of SAX events
 * and constructing a Document tree. There is one concrete subclass for each
 * tree implementation.
 * @author Michael H. Kay
 */

public abstract class Builder implements Receiver {
    /**
     * Constant denoting the "linked tree" in which each node is represented as an object
     */
    public static final int LINKED_TREE = 0;
    /**
     * Alternative constant denoting the "linked tree" in which each node is represented as an object
     * Retained for backwards compatibility
     */
    public static final int STANDARD_TREE = 0;
    /**
     * Constant denoting the "tiny tree" in which the tree is represented internally using arrays of integers
     */
    public static final int TINY_TREE = 1;

    protected PipelineConfiguration pipe;
    protected Configuration config;
    protected NamePool namePool;
    protected String systemId;
    protected String baseURI;
    protected NodeInfo currentRoot;
    protected boolean lineNumbering = false;

    protected boolean started = false;
    protected boolean timing = false;

    private long startTime;

    /**
     * create a Builder and initialise variables
     */

    public Builder() {
    }

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        //System.err.println("Builder#setPipelineConfiguration pipe = " + pipe);
        if (pipe == null) {
            new NullPointerException("pipe not initialized").printStackTrace();
        }
        this.pipe = pipe;
        this.config = pipe.getConfiguration();
        this.namePool = config.getNamePool();
        this.lineNumbering = (lineNumbering || config.isLineNumbering());
    }

    public PipelineConfiguration getPipelineConfiguration () {
        return pipe;
    }

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * The SystemId is equivalent to the document-uri property defined in the XDM data model.
     * It should be set only in the case of a document that is potentially retrievable via this URI.
     * This means it should not be set in the case of a temporary tree constructed in the course of
     * executing a query or transformation.
     * @param systemId the SystemId, that is, the document-uri.
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * The SystemId is equivalent to the document-uri property defined in the XDM data model.
     * It should be set only in the case of a document that is potentially retrievable via this URI.
     * This means the value will be null in the case of a temporary tree constructed in the course of
     * executing a query or transformation.
     * @return the SystemId, that is, the document-uri.
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Set the base URI of the document node of the tree being constructed by this builder
     * @param baseURI the base URI
     */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
     * Get the base URI of the document node of the tree being constructed by this builder
     * @return the base URI
     */

    public String getBaseURI() {
        return baseURI;
    }



    /////////////////////////////////////////////////////////////////////////
    // Methods setting and getting options for building the tree
    /////////////////////////////////////////////////////////////////////////

    /**
     * Set line numbering on or off
     * @param lineNumbering set to true if line numbers are to be maintained for nodes in the tree being
     * constructed.
     */

    public void setLineNumbering(boolean lineNumbering) {
        this.lineNumbering = lineNumbering;
    }

    /**
     * Set timing option on or off
     */

    public void setTiming(boolean on) {
        timing = on;
    }

    /**
     * Get timing option
     */

    public boolean isTiming() {
        return timing;
    }

    public void open() throws XPathException {
        if (timing) {
            System.err.println("Building tree for " + getSystemId() + " using " + getClass());
            startTime = (new Date()).getTime();
        }
    }

    public void close() throws XPathException {
        if (timing) {
            long endTime = (new Date()).getTime();
            System.err.println("Tree built in " + (endTime - startTime) + " milliseconds");
            if (currentRoot instanceof TinyDocumentImpl) {
                ((TinyDocumentImpl)currentRoot).showSize();
            }
            startTime = endTime;
        }
    }

    /**
     * Start of a document node.
     * This event is ignored: we simply add the contained elements to the current document
    */

    public void startDocument(int properties) throws XPathException { }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException { }

    /**
     * Get the current root node. This will normally be a document node, but if the root of the tree
     * is an element node, it can be an element.
     * @return the root of the tree that is currently being built, or that has been most recently built
     * using this builder
     */

    public NodeInfo getCurrentRoot() {
        return currentRoot;
    }

    /**
     * Static method to build a document from any kind of Source object. If the source
     * is already in the form of a tree, it is wrapped as required.
     * @param source Any javax.xml.transform.Source object
     * @param stripper A stripper object, if whitespace text nodes are to be stripped;
     * otherwise null.
     * @param config The Configuration object
     * @return the NodeInfo of the start node in the resulting document object.
     */

    public static NodeInfo build(Source source, Stripper stripper, Configuration config) throws XPathException {
        return build(source, stripper, config.makePipelineConfiguration());
    }

    /**
     * Static method to build a document from any kind of Source object. If the source
     * is already in the form of a tree, it is wrapped as required.
     * @param source Any javax.xml.transform.Source object
     * @param stripper A stripper object, if whitespace text nodes are to be stripped;
     * otherwise null.
     * @param pipe The PipelineConfiguration object
     * @return the NodeInfo of the start node in the resulting document object.
     */

    public static NodeInfo build(Source source, Stripper stripper, PipelineConfiguration pipe)
    throws XPathException {
        Configuration config = pipe.getConfiguration();
        if (source == null) {
            throw new NullPointerException("Source supplied to builder cannot be null");
        }

        NodeInfo start;
        if (source instanceof DOMSource || source instanceof NodeInfo) {
            start = Controller.unravel(source, config);
            if (stripper != null) {
                DocumentInfo docInfo = start.getDocumentRoot();
                StrippedDocument strippedDoc = new StrippedDocument(docInfo, stripper);
                start = strippedDoc.wrap(start);
            }

        } else {
            // we have a SAXSource or StreamSource
            Builder b;
            if (config.getTreeModel() == Builder.TINY_TREE) {
                b = new TinyBuilder();
            } else {
                b = new TreeBuilder();
            }
            b.setPipelineConfiguration(pipe);
            if (config.isLineNumbering() ||
                    (source instanceof AugmentedSource && ((AugmentedSource)source).isLineNumbering())) {
                b.setLineNumbering(true);
            }
            Receiver receiver = b;
            if (stripper != null) {
                stripper.setUnderlyingReceiver(b);
                receiver = stripper;
            }
            try {
                new Sender(pipe).send(source, receiver);
            } catch (XPathException err) {
                throw DynamicError.makeDynamicError(err);
            }
            start = b.getCurrentRoot();
        }
        return start;
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
