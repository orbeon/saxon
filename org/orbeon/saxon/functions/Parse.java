package net.sf.saxon.functions;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.Sender;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import org.xml.sax.InputSource;

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
        if (baseURI == null) {
            super.checkArguments(env);
            baseURI = env.getBaseURI();
        }
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
        Receiver s = controller.makeStripper(b);
        if (controller.getExecutable().stripsInputTypeAnnotations()) {
            s = controller.getConfiguration().getAnnotationStripper(s);
        }
        try {
            new Sender(controller.makePipelineConfiguration()).send(source, s);
            return (DocumentInfo)b.getCurrentRoot();
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
