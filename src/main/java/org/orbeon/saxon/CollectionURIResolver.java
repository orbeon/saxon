package org.orbeon.saxon;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;

import java.io.Serializable;


/**
* This interface defines a CollectionURIResolver. This is a counterpart to the JAXP
* URIResolver, but is used to map the URI of collection into a sequence of documents
* @author Michael H. Kay
*/

public interface CollectionURIResolver extends Serializable {

    /**
    * Resolve a URI.
    * @param href The relative URI of the collection. This corresponds to the
    * argument supplied to the collection() function. If the collection() function
    * was called with no arguments (to get the "default collection") this argument
    * will be null.
    * @param base The base URI that should be used. This is the base URI of the
    * static context in which the call to collection() was made, typically the URI
    * of the stylesheet or query module
     * @param context The dynamic execution context
    * @return an Iterator over the documents in the collection. The items returned
    * by this iterator must be instances either of xs:anyURI, or of node() (specifically,
     * {@link org.orbeon.saxon.om.NodeInfo}.). If xs:anyURI values are returned, the corresponding
     * document will be retrieved as if by a call to the doc() function: this means that
     * the system first checks to see if the document is already loaded, and if not, calls
     * the registered URIResolver to dereference the URI. This is the recommended approach
     * to ensure that the resulting collection is stable: however, it has the consequence
     * that the documents will by default remain in memory for the duration of the query
     * or transformation.
    * <p>
    * If the URI is not recognized, the method may either return an empty iterator,
     * in which case no error is reported, or it may throw an exception, in which case
     * the query or transformation fails. Returning null has the same effect as returning
     * an empty iterator.
    */

    public SequenceIterator resolve(String href, String base, XPathContext context) throws XPathException;

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
