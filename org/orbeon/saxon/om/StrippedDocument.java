package net.sf.saxon.om;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Stripper;

/**
  * A StrippedDocument represents a view of a real Document in which selected
 * whitespace text nodes are treated as having been stripped.
  */

public class StrippedDocument extends StrippedNode implements DocumentInfo {

    private Stripper stripper;

    public StrippedDocument(DocumentInfo doc, Stripper stripper) {
        this.node = doc;
        this.parent = null;
        this.docWrapper = this;
        this.stripper = stripper;
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
	* Set the name pool used for all names in this document
	*/

	public void setConfiguration(Configuration config) {
        ((DocumentInfo)node).setConfiguration(config);
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
    * Get the unparsed entity with a given name
    * @param name the name of the entity
    */

    public String[] getUnparsedEntity(String name) {
        return ((DocumentInfo)node).getUnparsedEntity(name);
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
