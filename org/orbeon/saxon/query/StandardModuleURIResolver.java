package org.orbeon.saxon.query;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.JavaPlatform;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.stream.StreamSource;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;


/**
 * This class is the standard ModuleURIResolver used to implement the "import module" declaration
 * in a Query Prolog. It is used when no user-defined ModuleURIResolver has been specified, or when
 * the user-defined ModuleURIResolver decides to delegate to the standard ModuleURIResolver.
 * @author Michael H. Kay
*/

public class StandardModuleURIResolver implements ModuleURIResolver {

    private Configuration config;

    public StandardModuleURIResolver() {}

    public StandardModuleURIResolver(Configuration config) {
        this.config = config;
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
     * and either an InputSource or a Reader representing the text of the module. 
     * @throws org.orbeon.saxon.trans.XPathException if the module cannot be located
    */

    public StreamSource[] resolve(String moduleURI, String baseURI, String[] locations) throws XPathException {
        if (locations.length == 0) {
            StaticError err = new StaticError("Cannot locate module for namespace " + moduleURI);
            err.setErrorCode("XQST0059");
            throw err;
        } else {
            // One or more locations given: import modules from all these locations
            Platform platform = (config==null ? JavaPlatform.getInstance() : config.getPlatform());
            StreamSource[] sources = new StreamSource[locations.length];
            for (int m=0; m<locations.length; m++) {
                String href = locations[m];
                URI absoluteURI;
                try {
                    absoluteURI = platform.makeAbsolute(href, baseURI);
                } catch (URISyntaxException err) {
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
      * This method attempts to discover the encoding by reading any HTTP headers.
      * If the encoding can be determined, it returns a StreamSource containing a Reader that
      * performs the required decoding. Otherwise, it returns a StreamSource containing an
      * InputSource, leaving the caller to sort out encoding problems.
      * @param absoluteURI the absolute URI of the source query
      * @return a StreamSource containing a Reader or InputSource, as well as a systemID representing
      * the base URI of the query.
      * @throws org.orbeon.saxon.trans.StaticError if the URIs are invalid or cannot be resolved or dereferenced, or
      * if any I/O error occurs
      */

     private static StreamSource getQuerySource(URI absoluteURI)
             throws StaticError {

         try {
             InputStream is;
             URL absoluteURL = absoluteURI.toURL();
             URLConnection connection = absoluteURL.openConnection();
             connection.connect();
             is = connection.getInputStream();

             if (!is.markSupported()) {
                 is = new BufferedInputStream(is);
             }

             // Get any external (HTTP) encoding label.
             String contentType;
             String encoding = null;

             // The file:// URL scheme gives no useful information...
             if (!"file".equals(connection.getURL().getProtocol())) {

                 // Use the contentType from the HTTP header if available
                 contentType = connection.getContentType();

                 if (contentType != null) {
                     int pos = contentType.indexOf("charset");
                     if (pos>=0) {
                         pos = contentType.indexOf('=', pos + 7);
                         if (pos>=0) {
                             contentType = contentType.substring(pos + 1);
                         }
                         if ((pos = contentType.indexOf(';')) > 0) {
                             contentType = contentType.substring(0, pos);
                         }

                         // attributes can have comment fields (RFC 822)
                         if ((pos = contentType.indexOf('(')) > 0) {
                             contentType = contentType.substring(0, pos);
                         }
                         // ... and values may be quoted
                         if ((pos = contentType.indexOf('"')) > 0) {
                             contentType = contentType.substring(pos + 1,
                                     contentType.indexOf('"', pos + 2));
                         }
                         encoding = contentType.trim();
                     }
                 }
             }
             StreamSource ss = new StreamSource();
             if (encoding == null) {
                 ss.setInputStream(is);
             } else {
                 ss.setReader(new InputStreamReader(is, encoding));
             }
             ss.setSystemId(absoluteURL.toString());
             return ss;
         } catch (IOException err) {
             StaticError se = new StaticError(err);
             se.setErrorCode("XQST0059");
             throw se;
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
