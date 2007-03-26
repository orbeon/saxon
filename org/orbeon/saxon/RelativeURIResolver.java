package org.orbeon.saxon;

import javax.xml.transform.URIResolver;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

/**
 * The standard JAXP URIResolver is given a relative URI and a base URI and returns the resource
 * identified by this combination. However, to support a stable implementation of the doc() function,
 * Saxon needs to know what the absolute URI is before the resource is fetched, so it can determine whether
 * a document with that absolute URI actually exists.
 * <p>
 * This extended interface defines a URIResolver that separates the two functions of resolving a relative URI
 * against a base URI, and fetching a resource with that absolute URI. If the URI resolver supplied to Saxon
 * implements this interface, the absolute URI associated with a loaded document will be the URI returned by
 * this resolver.
 * <p>
 * The particular motivation for providing this interface is to allow a URIResolver to wrap a .NET XmlResolver,
 * which has additional capability not present in the JAXP interface.
 */

public interface RelativeURIResolver extends URIResolver {

     /**
     * Called by the processor when it encounters
     * an xsl:include, xsl:import, or document() function.
     *
     * Despite the name, the main purpose of this method is to dereference the URI, not merely
     * to resolve it.
     *
     * @param href An href attribute, which may be relative or absolute. 
     * @param base The base URI against which the first argument will be made
     * absolute if the absolute URI is required.
     *
     * @return A Source object, or null if the href cannot be resolved,
     * and the processor should try to resolve the URI itself.
     *
     * @throws javax.xml.transform.TransformerException if an error occurs when trying to
     * resolve the URI.
     */
    public Source resolve(String href, String base)
        throws TransformerException;

    /**
     * Create an absolute URI from a relative URI and a base URI. This method performs the
     * process which is correctly called "URI resolution": this is purely a syntactic operation
     * on the URI strings, and does not retrieve any resources.
     * @param href A relative or absolute URI, to be resolved against the specified base URI
     * @param base The base URI against which the first argument will be made
     * absolute if the absolute URI is required.
     * @return A string containing the absolute URI that results from URI resolution. If the resource
     * needs to be fetched, this absolute URI will be supplied as the href parameter in a subsequent
     * call to the <code>resolve</code> method.
     */

    public String makeAbsolute(String href, String base)
        throws TransformerException;

    /**
     * Specify the media type of the resource that is expected to be delivered. This information is
     * supplied by the processor primarily to indicate whether the URIResolver is allowed to return
     * an XML tree already parsed. If the value is "text/plain" then the Source returned by the
     * resolve() method should be a StreamSource.
     */

    public void setExpectedMediaType(String mediaType);

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