package org.orbeon.saxon.jdom;

import org.orbeon.saxon.Query;
import org.orbeon.saxon.trans.XPathException;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Variant of command line org.orbeon.saxon.Transform do build the source document
 * in JDOM and then proceed with the transformation. This class is provided largely for
 * testing purposes.
 */

public class JDOMQuery extends Query {

    public List preprocess(List sources) throws XPathException {
        try {
            ArrayList jdomSources = new ArrayList(sources.size());
            for (int i=0; i<sources.size(); i++) {
                SAXSource ss = (SAXSource)sources.get(i);
                SAXBuilder builder = new SAXBuilder();
                org.jdom.Document doc = builder.build(ss.getInputSource());
                DocumentWrapper jdom = new DocumentWrapper(doc, ss.getSystemId(), getConfiguration());
                jdomSources.add(jdom);
            }
            return jdomSources;
        } catch (JDOMException e) {
            throw new XPathException(e);
        } catch (IOException e) {
            throw new XPathException(e);
        }
    }

    public static void main(String[] args) {
        new JDOMQuery().doQuery(args, "JDOMQuery");
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