package org.orbeon.saxon.om;

import org.orbeon.saxon.Configuration;

/**
 * The root node of an XPath tree. (Or equivalently, the tree itself).<P>
 * This class is used not only for the root of a document,
 * but also for the root of a result tree fragment, which is not constrained to contain a
 * single top-level element.
 *
 * @author Michael H. Kay
 */

public interface DocumentInfo extends NodeInfo {

	/**
	 * Set the configuration, which defines the name pool used for all names in this document.
     * This is always called after a new
	 * document has been created. The implementation must register the name pool with the document,
	 * so that it can be retrieved using getNamePool(). It must also call NamePool.allocateDocumentNumber(),
	 * and return the relevant document number when getDocumentNumber() is subsequently called.
	 * @param config The configuration to be used
	 */

	public void setConfiguration(Configuration config);

    /**
     * Get the configuration previously set using setConfiguration
     */

    public Configuration getConfiguration();

	/**
	 * Get the name pool used for the names in this document
	 * @return the name pool in which all the names used in this document are
	 *     registered
	 */

	public NamePool getNamePool();

	/**
	 * Get the unique document number for this document
	 * (the number is unique for all documents within a NamePool)
	 *
	 * @return the unique number identifying this document within the name pool
	 */

	public int getDocumentNumber();

    /**
     * Get the element with a given ID, if any
     *
     * @param id the required ID value
     * @return the element with the given ID, or null if there is no such ID
     *     present (or if the parser has not notified attributes as being of
     *     type ID)
     */

    public NodeInfo selectID(String id);

    /**
     * Get the unparsed entity with a given name
     *
     * @param name the name of the entity
     * @return if the entity exists, return an array of two Strings, the first
     *      holding the system ID of the entity, the second holding the public
     *      ID if there is one, or null if not. If the entity does not exist,
     *     return null.
     */

    public String[] getUnparsedEntity(String name);

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
