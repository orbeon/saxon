package org.orbeon.saxon.event;

import org.orbeon.saxon.xpath.XPathException;

import javax.xml.transform.OutputKeys;


/**
  * XHTMLEmitter is an Emitter that generates XHTML output.
  * It is the same as XMLEmitter except that it follows the legacy HTML browser
  * compatibility rules: for example, generating empty elements such as <BR />, and
  * using <p></p> for empty paragraphs rather than <p/>
  */

public class XHTMLEmitter extends XMLEmitter
{
    /**
    * Close an empty element tag.
    */

    protected String emptyElementTagCloser(String displayName) {
        if (displayName==null || HTMLEmitter.isEmptyTag(displayName)) {
            return " />";
        } else {
            return "></" + displayName + '>';
        }
    }

    /**
     * Start the content of an element
     */

    public void startContent() throws XPathException {
        super.startContent();
        // add a META tag after the HEAD tag if there is one.
        // TODO: if a META tag is added, any existing META tag with http-equiv="Content-Type" should be dropped
        if (elementStack.peek().equals("head")) {
            String includeMeta = outputProperties.getProperty(
                                    SaxonOutputKeys.INCLUDE_CONTENT_TYPE);
            if (!("no".equals(includeMeta))) {
                if (openStartTag) {
                    closeStartTag("head", false);
                }
                String encoding = outputProperties.getProperty(OutputKeys.ENCODING);
                if (encoding==null) encoding = "UTF-8";
                String mime = outputProperties.getProperty(OutputKeys.MEDIA_TYPE);
                if (mime==null) mime="text/html";
                try {
                    writer.write("\n      <meta http-equiv=\"Content-Type\" content=\"" +
                            mime + "; charset=" + encoding + "\"/>\n   ");
                } catch (java.io.IOException err) {}
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
