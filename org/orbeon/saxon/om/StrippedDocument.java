package org.orbeon.saxon.om;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Stripper;
import org.orbeon.saxon.pattern.NodeKindTest;

import java.util.Iterator;

/**
  * A StrippedDocument represents a view of a real Document in which selected
 * whitespace text nodes are treated as having been stripped.
  */

public class StrippedDocument extends StrippedNode implements DocumentInfo {

    private Stripper stripper;
    private boolean preservesSpace;

    public StrippedDocument(DocumentInfo doc, Stripper stripper) {
        node = doc;
        parent = null;
        docWrapper = this;
        this.stripper = stripper;
        preservesSpace = findPreserveSpace(doc);
    }

    /**
     * Create a wrapped node within this document
     */

    public StrippedNode wrap(NodeInfo node) {
        return makeWrapper(node, this, null);
    }

    /**
     * Get the document's stripper
     */

    public Stripper getStripper() {
        return stripper;
    }

    /**
     * Get the configuration previously set using setConfiguration
     */

    public Configuration getConfiguration() {
        return node.getConfiguration();
    }

	/**
	* Get the name pool used for the names in this document
	*/

	public NamePool getNamePool() {
	    return node.getNamePool();
	}

	/**
	* Get the unique document number
	*/

	public int getDocumentNumber() {
	    return node.getDocumentNumber();
	}

    /**
    * Get the element with a given ID, if any
    * @param id the required ID value
    * @return the element with the given ID value, or null if there is none.
    */

    public NodeInfo selectID(String id) {
        NodeInfo n = ((DocumentInfo)node).selectID(id);
        if (n==null) {
            return null;
        } else {
            return makeWrapper(n, this, null);
        }
    }

    /**
     * Get the list of unparsed entities defined in this document
     * @return an Iterator, whose items are of type String, containing the names of all
     *         unparsed entities defined in this document. If there are no unparsed entities or if the
     *         information is not available then an empty iterator is returned
     */

    public Iterator getUnparsedEntityNames() {
        return ((DocumentInfo)node).getUnparsedEntityNames();
    }

    /**
    * Get the unparsed entity with a given name
    * @param name the name of the entity
    */

    public String[] getUnparsedEntity(String name) {
        return ((DocumentInfo)node).getUnparsedEntity(name);
    }

    /**
     * Determine whether the wrapped document contains any xml:space="preserve" attributes. If it
     * does, we will look for them when stripping individual nodes. It's more efficient to scan
     * the document in advance checking for xml:space attributes than to look for them every time
     * we hit a whitespace text node.
     */

    private static boolean findPreserveSpace(DocumentInfo doc) {
        AxisIterator iter = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT);
        while (true) {
            NodeInfo node = (NodeInfo)iter.next();
            if (node == null) {
                return false;
            }
            String val = node.getAttributeValue(StandardNames.XML_SPACE);
            if ("preserve".equals(val)) {
                return true;
            }
        }
    }

    /**
     * Does the stripped document contain any xml:space="preserve" attributes?
     */

    public boolean containsPreserveSpace() {
        return preservesSpace;
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
// The Initial Developer of the Original Code is
// Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
