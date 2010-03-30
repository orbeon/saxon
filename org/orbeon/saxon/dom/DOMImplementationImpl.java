package org.orbeon.saxon.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

/**
 * A simple implementation of the DOMImplementation interface, for use when accessing
 * Saxon tree structure using the DOM API.
*/

class DOMImplementationImpl implements DOMImplementation {

 /**
 * Test if the DOM implementation implements a specific feature.
 * @param feature  The name of the feature to test (case-insensitive).
 * @param version  This is the version number of the feature to test.
 * @return <code>true</code> if the feature is implemented in the
 *   specified version, <code>false</code> otherwise. This implementation
  * returns true if the feature is "XML" or "Core" and the version is null,
  * "", "3.0", "2.0", or "1.0".
 */

public boolean hasFeature(String feature, String version) {
    return (feature.equalsIgnoreCase("XML") || feature.equalsIgnoreCase("Core")) &&
            (version == null || version.length() == 0 ||
            version.equals("3.0") || version.equals("2.0") || version.equals("1.0"));
}

    /**
     * This method returns a specialized object which implements the
     * specialized APIs of the specified feature and version, as specified
     * in .
     * @param feature  The name of the feature requested.
     * @param version  This is the version number of the feature to test.
     * @return  Always returns null in this implementation
     * @since DOM Level 3
     */

public Object getFeature(String feature,
                         String version) {
    return null;
}


/**
 *  Creates an empty <code>DocumentType</code> node.
 * @param qualifiedName  The  qualified name of the document type to be
 *   created.
 * @param publicId  The external subset public identifier.
 * @param systemId  The external subset system identifier.
 * @return  A new <code>DocumentType</code> node with
 *   <code>Node.ownerDocument</code> set to <code>null</code> .
 * @exception org.w3c.dom.DOMException
 *    INVALID_CHARACTER_ERR: Raised if the specified qualified name
 *   contains an illegal character.
 *   <br> NAMESPACE_ERR: Raised if the <code>qualifiedName</code> is
 *   malformed.
 * @since DOM Level 2
 */

public DocumentType createDocumentType(String qualifiedName,
                                       String publicId,
                                       String systemId)
                                       throws DOMException
{
    NodeOverNodeInfo.disallowUpdate();
    return null;
}

/**
 *  Creates an XML <code>Document</code> object of the specified type with
 * its document element.
 * @param namespaceURI  The  namespace URI of the document element to
 *   create.
 * @param qualifiedName  The  qualified name of the document element to be
 *   created.
 * @param doctype  The type of document to be created or <code>null</code>.
 * @return  A new <code>Document</code> object.
 * @exception org.w3c.dom.DOMException
 * @since DOM Level 2
 */
public Document createDocument(String namespaceURI,
                               String qualifiedName,
                               DocumentType doctype)
                               throws DOMException
{
    NodeOverNodeInfo.disallowUpdate();
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

