package net.sf.saxon.dom;

import net.sf.saxon.Transform;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Variant of command line net.sf.saxon.Transform do build the source document
 * in DOM and then proceed with the transformation. This class is provided largely for
 * testing purposes.
 */

public class DOMTransform extends Transform {

    public List preprocess(List sources) throws XPathException {
        try {
            ArrayList domSources = new ArrayList(sources.size());
            for (int i=0; i<sources.size(); i++) {
                StreamSource src = (StreamSource)sources.get(i);
                InputSource ins = new InputSource(src.getSystemId());

                // The following statement, if uncommented, forces use of the Xerces DOM.
                // This system property can also be set from the command line using the -D option

                System.setProperty("javax.xml.parser.DocumentBuilderFactory",
                                   "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document doc = builder.parse(ins);
                DocumentWrapper dom = new DocumentWrapper(doc, src.getSystemId(), getConfiguration());
                domSources.add(dom);
            }
            return domSources;
        } catch (ParserConfigurationException e) {
            throw new DynamicError(e);
        } catch (SAXException e) {
            throw new DynamicError(e);
        } catch (IOException e) {
            throw new DynamicError(e);
    }
    }

    public static void main(String[] args) {
        new DOMTransform().doMain(args, "DOMTransform");
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