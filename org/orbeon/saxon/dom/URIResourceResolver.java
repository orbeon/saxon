package net.sf.saxon.dom;

import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSInput;

import javax.xml.transform.URIResolver;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.Reader;
import java.io.InputStream;
import java.io.StringReader;

/**
 * This class implements the JAXP URIResourceResolver as a wrapper around
 * a DOM Level 3 LSResourceResolver. This serves two purposes: it allows the
 * same underlying object to be used in both roles, and it allows an LSResourceResolver
 * to be passed around the system in places where a URIResolved is expected, for
 * example in the PipelineConfiguration
 */

public class URIResourceResolver implements URIResolver {

    private LSResourceResolver resolver;

    public URIResourceResolver(LSResourceResolver resolver) {
        this.resolver = resolver;
    }

    public LSResourceResolver getLSResourceResolver() {
        return resolver;
    }

    /**
     * Called by an XSLT processor when it encounters
     * an xsl:include, xsl:import, or document() function.
     *
     * @param href An href attribute, which may be relative or absolute.
     * @param base The base URI against which the first argument will be made
     *             absolute if the absolute URI is required.
     * @return A Source object, or null if the href cannot be resolved,
     *         and the processor should try to resolve the URI itself.
     * @throws javax.xml.transform.TransformerException
     *          if an error occurs when trying to
     *          resolve the URI.
     */
    public Source resolve(String href, String base) throws TransformerException {
        LSInput lsin = resolver.resolveResource(
                "http://www.w3.org/TR/REC-xml", null, null, href, base);
        if (lsin == null) {
            return null;
        }

        Reader reader = lsin.getCharacterStream();
        InputStream stream = lsin.getByteStream();
        String content = lsin.getStringData();
        String systemId = lsin.getSystemId();
        String publicId = lsin.getPublicId();

        if (content != null) {
            reader = new StringReader(content);
        }

        StreamSource source = new StreamSource();
        source.setSystemId(systemId);
        source.setPublicId(publicId);
        source.setReader(reader);
        source.setInputStream(stream);

        return source;

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