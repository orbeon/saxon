package net.sf.saxon.event;
import net.sf.saxon.OutputURIResolver;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;


/**
* This class defines the default OutputURIResolver. This is a counterpart to the JAXP
* URIResolver, but is used to map the URI of a secondary result document to a Result object
* which acts as the destination for the new document.
* @author Michael H. Kay
*/

public class StandardOutputResolver implements OutputURIResolver {

    private static StandardOutputResolver theInstance = new StandardOutputResolver();

    /**
    * Get a singular instance
    */

    public static StandardOutputResolver getInstance() {
        return theInstance;
    }

    /**
    * Resolve an output URI
    * @param href The relative URI of the output document. This corresponds to the
    * href attribute of the xsl:result-document instruction.
    * @param base The base URI that should be used. This is the base output URI,
    * normally the URI of the principal output file.
    * @return a Result object representing the destination for the XML document
    */

    public Result resolve(String href, String base) throws XPathException {

        // System.err.println("Output URI Resolver (href='" + href + "', base='" + base + "')");

        try {
            URI absoluteURI;
            if (href.equals("")) {
                if (base==null) {
                    throw new DynamicError("The system identifier of the principal output file is unknown");
                }
                absoluteURI= new URI(base);
            } else {
                absoluteURI= new URI(href);
            }
            if (!absoluteURI.isAbsolute()) {
                if (base==null) {
                    throw new DynamicError("The system identifier of the principal output file is unknown");
                }
                URI baseURI = new URI(base);
                absoluteURI = baseURI.resolve(href);
            }

            if (absoluteURI.getScheme().equals("file")) {
                File newFile = new File(absoluteURI);
                try {
        	        if (!newFile.exists()) {
        	            String parent = newFile.getParent();
        	            if (parent!=null) {
            				File parentPath = new File(parent);
            				if (parentPath != null && !parentPath.exists()) {
         						parentPath.mkdirs();
            				}
        				    newFile.createNewFile();
        	            }
        		    }

                    //StreamResult result = new StreamResult(new FileOutputStream(newFile));
                    StreamResult result = new StreamResult(newFile.toURI().toASCIIString());
                            // The call new StreamResult(newFile) does file-to-URI conversion incorrectly
                    //result.setSystemId(newFile);
                    return result;

                } catch (java.io.IOException err) {
                    throw new DynamicError("Failed to create output file " + absoluteURI, err);
                }

            } else {

                // See if the Java VM can conjure up a writable URL connection for us.
                // This is optimistic: I have yet to discover a URL scheme that it can handle.

                URLConnection connection = absoluteURI.toURL().openConnection();
                connection.setDoInput(false);
                connection.setDoOutput(true);
                connection.connect();
                OutputStream stream = connection.getOutputStream();
                StreamResult result = new StreamResult(stream);
                result.setSystemId(absoluteURI.toASCIIString());
                return result;
            }
        } catch (URISyntaxException err) {
            throw new DynamicError("Invalid syntax for base URI", err);
        } catch (IllegalArgumentException err2) {
            throw new DynamicError("Invalid URI syntax", err2);
        } catch (MalformedURLException err3) {
            throw new DynamicError("Resolved URL is malformed", err3);
        } catch (UnknownServiceException err5) {
            throw new DynamicError("Specified protocol does not allow output", err5);
        } catch (IOException err4) {
            throw new DynamicError("Cannot open connection to specified URL", err4);
        }
    }

    /**
    * Signal completion of the result document. This method is called by the system
    * when the result document has been successfully written. It allows the resolver
    * to perform tidy-up actions such as closing output streams, or firing off
    * processes that take this result tree as input. Note that the OutputURIResolver
    * is stateless, so the original href is supplied to identify the document
    * that has been completed.
    */

    public void close(Result result) throws XPathException {
        if (result instanceof StreamResult) {
            OutputStream stream = ((StreamResult)result).getOutputStream();
            if (stream != null) {
                try {
                    stream.close();
                } catch (java.io.IOException err) {
                    throw new DynamicError("Failed while closing output file", err);
                }
            }
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
