package org.orbeon.saxon.jdom;

import org.orbeon.saxon.Transform;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Variant of command line org.orbeon.saxon.Transform do build the source document
 * in JDOM and then proceed with the transformation. This class is provided largely for
 * testing purposes.
 */

public class JDOMTransform extends Transform {

    public List preprocess(List sources) throws XPathException {
        try {
            ArrayList jdomSources = new ArrayList(sources.size());
            for (int i=0; i<sources.size(); i++) {
                Source src = (Source)sources.get(i);
                InputSource is;
                if (src instanceof SAXSource) {
                    SAXSource ss = (SAXSource)sources.get(i);
                    is = ss.getInputSource();
                } else if (src instanceof StreamSource) {
                    StreamSource ss = (StreamSource)src;
                    if (ss.getInputStream() != null) {
                        is = new InputSource(ss.getInputStream());
                    } else if (ss.getReader() != null) {
                        is = new InputSource(ss.getReader());
                    } else {
                        is = new InputSource(ss.getSystemId());
                    }
                } else {
                    throw new IllegalArgumentException("Unknown kind of source");
                }
                is.setSystemId(src.getSystemId());
                SAXBuilder builder = new SAXBuilder();
                org.jdom.Document doc = builder.build(is);
                DocumentWrapper jdom = new DocumentWrapper(doc, is.getSystemId(), config);
                jdomSources.add(jdom);
            }
            return jdomSources;
        } catch (JDOMException e) {
            throw new DynamicError(e);
        } catch (IOException e) {
            throw new DynamicError(e);
        }
    }

    public static void main(String[] args) {
        new JDOMTransform().doMain(args, "JDOMTransform");
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
// Contributor(s): none
//