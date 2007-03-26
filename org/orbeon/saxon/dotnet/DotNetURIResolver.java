package org.orbeon.saxon.dotnet;

import cli.System.IO.Stream;
import cli.System.IO.TextReader;
import cli.System.Type;
import cli.System.Uri;
import cli.System.Xml.XmlResolver;
import org.orbeon.saxon.RelativeURIResolver;
import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.trans.DynamicError;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

/**
 * This class implements the JAXP URIResolver as a wrapper around
 * a .NET XmlResolver.
 */

public class DotNetURIResolver implements RelativeURIResolver {

    private XmlResolver resolver;
    private String mediaType;

    public DotNetURIResolver(XmlResolver resolver) {
        this.resolver = resolver;
    }

    public XmlResolver getXmlResolver() {
        return resolver;
    }

    /**
     * Specify the media type of the resource that is expected to be delivered. This information is
     * supplied by the processor primarily to indicate whether the URIResolver is allowed to return
     * an XML tree already parsed. If the value is "text/plain" then the Source returned by the
     * resolve() method should be a StreamSource.
     */

    public void setExpectedMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * Create an absolute URI from a relative URI and a base URI. This method performs the
     * process which is correctly called "URI resolution": this is purely a syntactic operation
     * on the URI strings, and does not retrieve any resources.
     *
     * @param href A relative or absolute URI, to be resolved against the specified base URI
     * @param base The base URI against which the first argument will be made
     *             absolute if the absolute URI is required.
     * @return A string containing the absolute URI that results from URI resolution. If the resource
     *         needs to be fetched, this absolute URI will be supplied as the href parameter in a subsequent
     *         call to the <code>resolve</code> method.
     */

    public String makeAbsolute(String href, String base) throws TransformerException {

            if (base == null) {
                try {
                    return new Uri(href).ToString();
                } catch (Exception e) {
                    DynamicError de = new DynamicError("Invalid URI: " + e.getMessage());
                    de.setErrorCode("FODC0005");
                    throw de;
                }
            } else {
                try {
                    return resolver.ResolveUri(new Uri(base), href).ToString();
                } catch (Exception e) {
                    DynamicError de = new DynamicError("Failure making absolute URI: " + e.getMessage());
                    de.setErrorCode("FODC0005");
                    throw de;
                }
            }

    }

    /**
     * Called by an XSLT processor when it encounters
     * an xsl:include, xsl:import, or document() function.
     *
     * @param href An href attribute, holding a relative or absolute URI.
     * @param base The base URI, ignored if href is absolute.
     * @return A Source object, or null if the href cannot be resolved,
     *         and the processor should try to resolve the URI itself.
     * @throws javax.xml.transform.TransformerException
     *          if an error occurs when trying to
     *          resolve the URI.
     */
    public Source resolve(String href, String base) throws TransformerException {
        //System.err.println("Resolving " + href + " against " + base);
        // TODO: handle fragment identifiers
        try {
            Uri abs;
            if (new java.net.URI(href).isAbsolute()) {
                abs = new Uri(href);
            } else {
                abs = resolver.ResolveUri(new Uri(base), href);
            }
            Object obj = resolver.GetEntity(abs, mediaType, Type.GetType("System.IO.Stream"));
            // expect cli.System.IO.FileNotFoundException if this fails
            if (obj instanceof Stream) {
                StreamSource source = new StreamSource(new DotNetInputStream((Stream)obj));
                source.setSystemId(abs.toString());
                AugmentedSource as = AugmentedSource.makeAugmentedSource(source);
                as.setPleaseCloseAfterUse(true);
                return as;
            } else if (obj instanceof TextReader) {
                StreamSource source = new StreamSource(new DotNetReader((TextReader)obj));
                source.setSystemId(abs.toString());
                AugmentedSource as = AugmentedSource.makeAugmentedSource(source);
                as.setPleaseCloseAfterUse(true);
                return as;
            } else if (obj instanceof Source) {
                return ((Source)obj);
            } else {
                throw new TransformerException(
                        "Unrecognized object returned by XmlResolver (type " + obj.getClass().getName());
            }
        } catch (Throwable e) {
            throw new TransformerException(e.getMessage(), e);
        }
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