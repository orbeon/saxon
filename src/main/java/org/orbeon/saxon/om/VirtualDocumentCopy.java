package org.orbeon.saxon.om;

import org.orbeon.saxon.Configuration;

import java.util.Iterator;

/**
 * A virtual copy of a document node
 *
 */

public class VirtualDocumentCopy extends VirtualCopy implements DocumentInfo {

    public VirtualDocumentCopy(DocumentInfo base) {
        super(base);
    }

    /**
     * Set the configuration, which defines the name pool used for all names in this document.
     * This is always called after a new document has been created.
     *
     * @param config The configuration to be used
     */

    public void setConfiguration(Configuration config) {
        //
    }

    /**
     * Get the element with a given ID, if any
     *
     * @param id the required ID value
     * @return the element with the given ID, or null if there is no such ID
     *         present (or if the parser has not notified attributes as being of
     *         type ID)
     */

    public NodeInfo selectID(String id) {
        NodeInfo n = ((DocumentInfo)original).selectID(id);
        if (n == null) {
            return null;
        }
        VirtualCopy vc = VirtualCopy.makeVirtualCopy(n, original);
        vc.documentNumber = documentNumber;
        return vc;
    }

    /**
     * Get the list of unparsed entities defined in this document
     * @return an Iterator, whose items are of type String, containing the names of all
     *         unparsed entities defined in this document. If there are no unparsed entities or if the
     *         information is not available then an empty iterator is returned
     */

    public Iterator getUnparsedEntityNames() {
        return ((DocumentInfo)original).getUnparsedEntityNames();
    }

    /**
     * Get the unparsed entity with a given name
     *
     * @param name the name of the entity
     * @return if the entity exists, return an array of two Strings, the first
     *         holding the system ID of the entity, the second holding the public
     *         ID if there is one, or null if not. If the entity does not exist,
     *         return null.
     */

    public String[] getUnparsedEntity(String name) {
        return ((DocumentInfo)original).getUnparsedEntity(name);
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

