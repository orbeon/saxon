package org.orbeon.saxon.type;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.Source;
import java.io.Serializable;


/**
 * A SchemaURIResolver is used when resolving references to
 * schema documents. It takes as input the target namespace of the schema to be loaded, and a set of
 * location hints as input, and returns one or more Source obects containing the schema documents
 * to be imported.
* @author Michael H. Kay
*/

public interface SchemaURIResolver extends Serializable {

    /**
     * Resolve a URI identifying a schema document, given the target namespace URI and
     * a set of associated location hints.
     * @param targetNamespace the target namespaces of the schema to be imported. The "null namesapce"
     * is identified by a zero-length string. In the case of an xsd:include directive, where no
     * target namespace is specified, the parameter is null.
     * @param baseURI The base URI of the module containing the "import schema" declaration;
     * null if no base URI is known
     * @param locations The set of URIs identified as schema location hints. In most cases (xsd:include, xsd:import,
     * xsi:schemaLocation, xsl:import-schema) there is only one URI in this list. With an XQuery "import module"
     * declaration, however, a list of URIs may be specified.
     * @return an array of Source objects each identifying a schema document to be loaded.
     * These need not necessarily correspond one-to-one with the location hints provided.
     * @throws org.orbeon.saxon.trans.XPathException if the module cannot be located, and if delegation to the default
     * module resolver is not required.
    */

    public Source[] resolve(String targetNamespace, String baseURI, String[] locations) throws XPathException;


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
