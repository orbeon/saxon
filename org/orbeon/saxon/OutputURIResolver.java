package net.sf.saxon;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;


/**
* This interface defines an OutputURIResolver. This is a counterpart to the JAXP
* URIResolver, but is used to map the URI of a secondary result document to a Result object
* which acts as the destination for the new document.
* @author Michael H. Kay
*/

public interface OutputURIResolver {

    /**
    * Resolve an output URI.
    * @param href The relative URI of the output document. This corresponds to the
    * href attribute of the xsl:result-document instruction.
    * @param base The base URI that should be used. This is the base URI of the
    * element that contained the href attribute. It may be null if no systemID was supplied
    * for the stylesheet.
    * @return a Result object representing the destination for the XML document. The
    * method can also return null, in which case the standard output URI resolver
    * will be used to create a Result object.
    */

    public Result resolve(String href, String base) throws TransformerException;

    /**
    * Signal completion of the result document. This method is called by the system
    * when the result document has been successfully written. It allows the resolver
    * to perform tidy-up actions such as closing output streams, or firing off
    * processes that take this result tree as input. Note that the OutputURIResolver
    * is stateless, so the the original Result object is supplied to identify the document
    * that has been completed.
     * @param result The result object returned by the previous call of resolve()
    */

    public void close(Result result) throws TransformerException;

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
