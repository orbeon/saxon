package org.orbeon.saxon.functions;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.event.Stripper;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.style.ExpressionContext;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.xpath.XPathException;
import org.xml.sax.InputSource;

import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.xpath.DynamicError;

import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;


/**
* This class implements the saxon:parse() extension function,
* which is specially-recognized by the system because it needs access
* to parts of the static context
*/

public class Parse extends SystemFunction {

    String baseURI;

    /**
    * Method supplied by each class of function to check arguments during parsing, when all
    * the argument expressions have been read
    */

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        baseURI = env.getBaseURI();
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        Controller controller = c.getController();
        AtomicValue content = (AtomicValue)argument[0].evaluateItem(c);
        StringReader sr = new StringReader(content.getStringValue());
        InputSource is = new InputSource(sr);
        is.setSystemId(baseURI);
        SAXSource source = new SAXSource(is);
        source.setSystemId(baseURI);
        Builder b = controller.makeBuilder();
        Stripper s = controller.makeStripper(b);
        try {
            new Sender(controller.getConfiguration()).send(source, s);
            return b.getCurrentDocument();
        } catch (XPathException err) {
            throw new DynamicError(err);
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
