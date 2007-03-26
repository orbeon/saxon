package org.orbeon.saxon.dotnet;
import org.orbeon.saxon.OutputURIResolver;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;

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

public class DotNetOutputResolver implements OutputURIResolver {

    private static DotNetOutputResolver theInstance = new DotNetOutputResolver();

    /**
    * Get a singular instance
    */

    public static DotNetOutputResolver getInstance() {
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

                //UPDATE: 2005/03/14
                //M. David Peterson
                //Reworked this to function exactly as Saxon functions
                //I've left the following lines in for now until such time as I better understand
                //the proper context of how to interpret the input of a directory e.g. dir=output
                //is interpreted to mean /root/ouput instead of /baseURI/output.
                //The following lines will rework the base and href variables into a concated string
                //that is then used to create a new URI which is then used to create a new file with the
                //baseURI as its root.

            	//String hrefFile = href.toString().replaceAll("file:///", "");
                //String pathAndpathFile = base.concat(hrefFile);
                //URI pathURI = new URI(pathAndpathFile);
                //File newFile = new File(pathURI);

                URI baseURI = new URI(base);
                absoluteURI = baseURI.resolve(href);
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
        	        // <M:D/> And this line creates a new StreamResult, passing in the concatted string as the file value.
	        		StreamResult result = new StreamResult(new java.io.FileOutputStream(newFile.toString()));

                    //StreamResult result = new StreamResult(new FileOutputStream(newFile));
                    //StreamResult result = new StreamResult(newFile.toURI().toASCIIString());
                            // The call new StreamResult(newFile) does file-to-URI conversion incorrectly
                    result.setSystemId(newFile.toString());
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

//    public static void main(String[] args) {
//        System.err.println("supplied base file: " + args[0]);
//        System.err.println("relative URI: " + args[1]);
//        System.err.println("base URI: " + new File(args[0]).toURI().toString());
//        System.err.println("resolved URI: " + new File(args[0]).toURI().resolve(args[1]).toString());
//    }
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
// Portions created by M. David Peterson are Copyright (C) x2x2x.org. All Rights Reserved.
//
// Contributor(s): none.
//
