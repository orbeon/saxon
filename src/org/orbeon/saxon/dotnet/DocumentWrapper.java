package org.orbeon.saxon.dotnet;

import cli.System.Xml.XmlDocument;
import cli.System.Xml.XmlNode;
import cli.System.Xml.XmlNodeType;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.type.Type;

import java.util.Iterator;
import java.util.Collections;

/**
 * The document node of a tree implemented as a wrapper around a DOM Document.
 */

public class DocumentWrapper extends NodeWrapper implements DocumentInfo {

    protected Configuration config;
    protected String baseURI;
    protected int documentNumber;
    protected boolean level3 = false;

    /**
     * Wrap a DOM Document or DocumentFragment node
     * @param doc a DOM Document or DocumentFragment node
     * @param baseURI the base URI of the document
     * @param config the Saxon configuration
     */

    public DocumentWrapper(XmlNode doc, String baseURI, Configuration config) {
        //System.err.println("Creating DocumentWrapper for " +node);
        this.node = doc;
        this.parent = null;
        this.index = 0;
        //super(doc, null, 0);
        if (doc.get_NodeType().Value != XmlNodeType.Document) {
            throw new IllegalArgumentException("Node must be a DOM Document");
        }
        node = doc;
        nodeKind = Type.DOCUMENT;
        this.baseURI = baseURI;
        docWrapper = this;

//        // Find out if this is a level-3 DOM implementation
//        Method[] methods = doc.getClass().getMethods();
//        for (int i=0; i<methods.length; i++) {
//            if (methods[i].getName().equals("isSameNode")) {
//                level3 = true;
//                break;
//            }
//        }
        //System.err.println("Setting configuration");
        setConfiguration(config);
    }

    /**
     * Create a wrapper for a node in this document
     *
     * @param node the DOM node to be wrapped. This must be a node within the document wrapped by this
     *             DocumentWrapper
     * @throws IllegalArgumentException if the node is not a descendant of the Document node wrapped by
     *                                  this DocumentWrapper
     */

    public NodeWrapper wrap(XmlNode node) {
        if (node == this.node) {
            return this;
        }
        if (node.get_OwnerDocument() == this.node) {
            return makeWrapper(node, this);
        } else {
            throw new IllegalArgumentException(
                "DocumentWrapper#wrap: supplied node does not belong to the wrapped DOM document");
        }
    }

    /**
     * Set the Configuration that contains this document
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
        documentNumber = config.getDocumentNumberAllocator().allocateDocumentNumber();
    }

    /**
     * Get the configuration previously set using setConfiguration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the name pool used for the names in this document
     */

    public NamePool getNamePool() {
        return config.getNamePool();
    }

    /**
     * Get the unique document number
     */

    public int getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Get the element with a given ID, if any
     *
     * @param id the required ID value
     * @return a NodeInfo representing the element with the given ID, or null if there
     *         is no such element. This implementation does not necessarily conform to the
     *         rule that if an invalid document contains two elements with the same ID, the one
     *         that comes last should be returned.
     */

    public NodeInfo selectID(String id) {
        if (node instanceof XmlDocument) {
            XmlNode el = ((XmlDocument)node).GetElementById(id);
            if (el == null) {
                return null;
            }
            return wrap(el);
        } else {
            return null;
        }
    }

    /**
     * Determine whether this is the same node as another node. <br />
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @return true if this Node object and the supplied Node object represent the
     *         same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (!(other instanceof DocumentWrapper)) {
            return false;
        }
        return node == ((DocumentWrapper)other).node;
    }

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. In the DOM model, base URIs are held only an the document level.
    */

    public String getBaseURI() {
        return node.get_BaseURI();
    }

    /**
     * Get the list of unparsed entities defined in this document
     * @return an Iterator, whose items are of type String, containing the names of all
     *         unparsed entities defined in this document. If there are no unparsed entities or if the
     *         information is not available then an empty iterator is returned
     */

    public Iterator getUnparsedEntityNames() {
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Get the unparsed entity with a given name
     *
     * @param name the name of the entity
     * @return null: JDOM does not provide access to unparsed entities
     */

    public String[] getUnparsedEntity(String name) {
        return null;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
