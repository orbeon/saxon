package net.sf.saxon.event;
import net.sf.saxon.Configuration;
import net.sf.saxon.StandardURIResolver;
import net.sf.saxon.om.ProcInstParser;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.util.ArrayList;

/**
  * The PIGrabber class is a Receiver that looks for xml-stylesheet processing
  * instructions and tests whether they match specified criteria; for those that do, it creates
  * an InputSource object referring to the relevant stylesheet
  * @author Michael H. Kay
  */

public class PIGrabber extends ProxyReceiver {

    private Configuration config = null;
    private String reqMedia = null;
    private String reqTitle = null;
    private String baseURI = null;
    private URIResolver uriResolver = null;
    private ArrayList stylesheets = new ArrayList();
    private boolean terminated = false;

    public void setFactory(Configuration config) {
        this.config = config;
    }

    public void setCriteria(String media, String title, String charset) {
        this.reqMedia = media;
        this.reqTitle = title;
    }

    /**
    * Set the base URI
    */

    public void setBaseURI(String uri) {
        baseURI = uri;
    }

    /**
    * Set the URI resolver to be used for the href attribute
    */

    public void setURIResolver(URIResolver resolver) {
        uriResolver = resolver;
    }

    public void open() {
        // bypass the check that there must be an underlying receiver
    }

    /**
    * Abort the parse when the first start element tag is found
    */

    public void startElement (int namecode, int typecode, int locationId, int properties)
    throws XPathException {
        terminated = true;
	    // abort the parse when the first start element tag is found
        throw new DynamicError("#start#");
    }

    /**
    * Determine whether the parse terminated because the first start element tag was found
    */

    public boolean isTerminated() {
        return terminated;
    }

    /**
    * Handle xml-stylesheet PI
    */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties)
    throws XPathException {
        if (target.equals("xml-stylesheet")) {

            String value = data.toString();
            String piMedia = ProcInstParser.getPseudoAttribute(value, "media");
            String piTitle = ProcInstParser.getPseudoAttribute(value, "title");
            String piType = ProcInstParser.getPseudoAttribute(value, "type");
            String piAlternate = ProcInstParser.getPseudoAttribute(value, "alternate");

			if (piType==null) return;

			// System.err.println("Found xml-stylesheet media=" + piMedia + " title=" + piTitle);

            if ( (piType.equals("text/xml") || piType.equals("application/xml") ||
                    piType.equals("text/xsl") || piType.equals("applicaton/xsl")) &&

                    (reqMedia==null || piMedia==null || reqMedia.equals(piMedia)) &&

                    ( ( piTitle==null && (piAlternate==null || piAlternate.equals("no"))) ||
                      ( reqTitle==null ) ||
                      ( piTitle!=null && piTitle.equals(reqTitle) ) ) )
            {
                String href = ProcInstParser.getPseudoAttribute(value, "href");
                if (href==null) {
                    throw new DynamicError("xml-stylesheet PI has no href attribute");
                }

				// System.err.println("Adding " + href);
                if (piTitle==null && (piAlternate==null || piAlternate.equals("no"))) {
                    stylesheets.add(0, href);
                } else {
                    stylesheets.add(href);
                }
            } else {
				//System.err.println("No match on required media=" + reqMedia + " title=" + reqTitle );
			}
        }
    }

    /**
    * Return list of stylesheets that matched, as an array of Source objects
    * @return null if there were no matching stylesheets.
    * @throws net.sf.saxon.trans.XPathException if a URI cannot be resolved
    */

    public Source[] getAssociatedStylesheets() throws TransformerException {
        if (stylesheets.size()==0) {
            return null;
        }
        if (uriResolver==null) {
            uriResolver = new StandardURIResolver(config);
        }
        Source[] result = new Source[stylesheets.size()];
        for (int i=0; i<stylesheets.size(); i++) {
            String href = (String)stylesheets.get(i);
            Source s = uriResolver.resolve(href, baseURI);
            if (s == null) {
                s = new StandardURIResolver(config).resolve(href, baseURI);
            }
            result[i] = s;
        }
        return result;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
