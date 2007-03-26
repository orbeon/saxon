package org.orbeon.saxon.dotnet;
import cli.System.IO.Stream;
import cli.System.IO.TextReader;
import cli.System.Type;
import cli.System.Uri;
import cli.System.Xml.XmlResolver;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.query.ModuleURIResolver;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;


/**
 * This class is the standard ModuleURIResolver used to implement the "import module" declaration
 * in a Query Prolog. It is used when no user-defined ModuleURIResolver has been specified, or when
 * the user-defined ModuleURIResolver decides to delegate to the standard ModuleURIResolver.
 * @author Michael H. Kay
*/

public class DotNetStandardModuleURIResolver implements ModuleURIResolver {

    private Configuration config;
    private XmlResolver resolver;

    public DotNetStandardModuleURIResolver() {}

    public DotNetStandardModuleURIResolver(Configuration config, XmlResolver resolver) {
        this.config = config;
        this.resolver = resolver;
    }

    /**
     * Resolve a module URI and associated location hints.
     * @param moduleURI The module namespace URI of the module to be imported; or null when
     * loading a non-library module.
     * @param baseURI The base URI of the module containing the "import module" declaration;
     * null if no base URI is known
     * @param locations The set of URIs specified in the "at" clause of "import module",
     * which serve as location hints for the module
     * @return an array of StreamSource objects each identifying the contents of a module to be
     * imported. Each StreamSource must contain a
     * non-null absolute System ID which will be used as the base URI of the imported module,
     * and either an InputSource or a Reader representing the text of the module. The method
     * may also return null, in which case the system attempts to resolve the URI using the
     * standard module URI resolver.
     * @throws org.orbeon.saxon.trans.XPathException if the module cannot be located
    */

    public StreamSource[] resolve(String moduleURI, String baseURI, String[] locations) throws XPathException {
        if (locations.length == 0) {
            StaticError err = new StaticError("Cannot locate module for namespace " + moduleURI);
            err.setErrorCode("XQST0059");
            throw err;
        } else {
            // One or more locations given: import modules from all these locations
            Uri base = new Uri(baseURI);
            StreamSource[] sources = new StreamSource[locations.length];
            for (int m=0; m<locations.length; m++) {
                String href = locations[m];
                Uri absoluteURI;
                try {
                    absoluteURI = resolver.ResolveUri(base, href);
                } catch (Throwable err) {
                    StaticError se = new StaticError("Cannot resolve relative URI " + href, err);
                    se.setErrorCode("XQST0059");
                    throw se;
                }
                sources[m] = getQuerySource(absoluteURI);
            }
            return sources;
        }
    }

    /**
      * Get a StreamSource object representing the source of a query, given its URI.
      * If the encoding can be determined, it returns a StreamSource containing a Reader that
      * performs the required decoding. Otherwise, it returns a StreamSource containing an
      * InputSource, leaving the caller to sort out encoding problems.
      * @param abs the absolute URI of the source query
      * @return a StreamSource containing a Reader or InputSource, as well as a systemID representing
      * the base URI of the query.
      * @throws org.orbeon.saxon.trans.StaticError if the URIs are invalid or cannot be resolved or dereferenced, or
      * if any I/O error occurs
      */

     private StreamSource getQuerySource(Uri abs)
             throws StaticError {

        try {

            Object obj = resolver.GetEntity(abs, "application/xquery", Type.GetType("System.IO.Stream"));
            // expect cli.System.IO.FileNotFoundException if this fails
            if (obj instanceof Stream) {
                StreamSource source = new StreamSource(new DotNetInputStream((Stream)obj));
                source.setSystemId(abs.toString());
                return source;
            } else if (obj instanceof TextReader) {
                StreamSource source = new StreamSource(new DotNetReader((TextReader)obj));
                source.setSystemId(abs.toString());
                return source;
            } else if (obj instanceof StreamSource) {
                return ((StreamSource)obj);
            } else {
                throw new TransformerException(
                        "Unrecognized object returned by XmlResolver (type " + obj.getClass().getName());
            }
        } catch (Throwable e) {
            throw new StaticError(e.getMessage(), e);
        }

        // TODO: look for the encoding in the HTTP header if any

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
