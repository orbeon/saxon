package net.sf.saxon.dom;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.Type;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
  * The document node of a tree implemented as a wrapper around a DOM Document.
  */

public class DocumentWrapper extends NodeWrapper implements DocumentInfo {

    protected Configuration config;
    protected String baseURI;
    protected int documentNumber;

    public DocumentWrapper(Document doc, String baseURI, Configuration config) {
        super(doc, null, 0);
        node = doc;
        nodeKind = Type.DOCUMENT;
        this.baseURI = baseURI;
        docWrapper = this;
        setConfiguration(config);
    }

    /**
     * Create a wrapper for a node in this document
     * @param node the DOM node to be wrapped. This must be a node within the document wrapped by this
     * DocumentWrapper
     */

    public NodeWrapper wrap(Node node) {
        if (node == this.node) {
            return this;
        }
        Node p = node;
        while (p != null) {
            if (p == docWrapper.node) {
                // TODO: use isSameNode() (DOM level 3) when we can rely on it being available
                return makeWrapper(node, this);
            }
            p = p.getParentNode();
        }
        throw new IllegalArgumentException(
                "DocumentWrapper#wrap: supplied node does not belong to the wrapped DOM document");
    }

    /**
    * Set the Configuration that contains this document
    */

    public void setConfiguration(Configuration config) {
        this.config = config;
        NamePool pool = config.getNamePool();
		documentNumber = pool.allocateDocumentNumber(this);
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
    * @param id the required ID value
    * @return a NodeInfo representing the element with the given ID, or null if there
     * is no such element. This implementation does not necessarily conform to the
     * rule that if an invalid document contains two elements with the same ID, the one
     * that comes last should be returned.
    */

    public NodeInfo selectID(String id) {
        Node el = ((Document)node).getElementById(id);
        if (el==null) {
            return null;
        }
        return wrap(el);
    }

    /**
    * Determine whether this is the same node as another node. <br />
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (!(other instanceof DocumentWrapper)) {
            return false;
        }
        return node == ((DocumentWrapper)other).node;
    }

    /**
    * Get the unparsed entity with a given name
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
