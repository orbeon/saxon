package net.sf.saxon.event;

import net.sf.saxon.Configuration;
import net.sf.saxon.dom.DocumentWrapper;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StrippedDocument;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.tinytree.TinyDocumentImpl;
import net.sf.saxon.tree.TreeBuilder;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
    public final static int STANDARD_TREE = 0;
    public final static int TINY_TREE = 1;

    protected Configuration config;
    protected NamePool namePool;
    protected String systemId;
    protected LocationProvider locator;
    protected DocumentInfo currentDocument;
    protected boolean lineNumbering;

    protected boolean started = false;
    protected boolean timing = false;

    private long startTime;

    /**
     * create a Builder and initialise variables
     */

    public Builder() {
    }

    public void setConfiguration(Configuration config) {
        this.config = config;
        this.namePool = config.getNamePool();
        this.lineNumbering = config.isLineNumbering();
    }

    public Configuration getConfiguration() {
        return config;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setLineNumbering(boolean is) {
        lineNumbering = is;
    }

    public void setDocumentLocator(LocationProvider loc) {
        locator = loc;
    }

    /**
     * Get the Document Locator
     */

    public LocationProvider getDocumentLocator() {
        return locator;
    }

    /////////////////////////////////////////////////////////////////////////
    // Methods setting and getting options for building the tree
    /////////////////////////////////////////////////////////////////////////

    /**
     * Set the root (document) node to use. This method is used to support
     * the JAXP facility to attach transformation output to a supplied Document
     * node. It must be called before startDocument(), and the type of document
     * node must be compatible with the type of Builder used.
     */

    public void setRootNode(DocumentInfo doc) {
        currentDocument = doc;
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
            if (currentDocument instanceof TinyDocumentImpl) {
                ((TinyDocumentImpl)currentDocument).showSize();
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
     * Get the current document
     * @return the document that has been most recently built using this builder
     */

    public DocumentInfo getCurrentDocument() {
        return currentDocument;
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
        if (source == null) {
            throw new NullPointerException("Source supplied to builder cannot be null");
        }

        NodeInfo start;
        if (source instanceof DOMSource || source instanceof NodeInfo) {
            if (source instanceof DOMSource) {
                Node dsnode = ((DOMSource)source).getNode();
                if (dsnode instanceof NodeInfo) {
                    start = (NodeInfo)dsnode;
                } else {
                    Document dom;
                    if (dsnode instanceof Document) {
                        dom = (Document)dsnode;
                    } else {
                        dom = dsnode.getOwnerDocument();
                    }
                    DocumentWrapper docWrapper = new DocumentWrapper(dom, source.getSystemId(), config);
                    start = docWrapper.wrap(dsnode);
                }
            } else {
                start = (NodeInfo)source;
            }
            if (stripper != null) {
                DocumentInfo docInfo = start.getDocumentRoot();
                StrippedDocument strippedDoc = new StrippedDocument(docInfo, stripper);
                start = strippedDoc.wrap(start);
            }
            return start;

        } else {
            // we have a SAXSource or StreamSource
            Builder b;
            if (config.getTreeModel() == Builder.TINY_TREE) {
                b = new TinyBuilder();
            } else {
                b = new TreeBuilder();
            }
            b.setConfiguration(config);
            b.setLineNumbering(config.isLineNumbering());
            Receiver receiver = b;
            if (stripper != null) {
                stripper.setUnderlyingReceiver(b);
                receiver = stripper;
            }
            try {
                new Sender(config).send(source, receiver);
            } catch (XPathException err) {
                throw new DynamicError(err);
            }
            start = b.getCurrentDocument();
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
