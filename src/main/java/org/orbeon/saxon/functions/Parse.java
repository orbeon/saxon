package org.orbeon.saxon.functions;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.Source;
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

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (baseURI == null) {
            super.checkArguments(visitor);
            baseURI = visitor.getStaticContext().getBaseURI();
        }
    }

    /**
     * Pre-evaluate a function at compile time. Static evaluation is suppressed for
     * saxon:parse because no controller is available.
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        return this;
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
        Source source = new SAXSource(is);
        source.setSystemId(baseURI);
        Builder b = controller.makeBuilder();
        Receiver s = b;
        source = AugmentedSource.makeAugmentedSource(source);
                ((AugmentedSource)source).setStripSpace(Whitespace.XSLT);
        if (controller.getExecutable().stripsInputTypeAnnotations()) {
            s = controller.getConfiguration().getAnnotationStripper(s);
        }
        try {
            new Sender(controller.makePipelineConfiguration()).send(source, s);
            NodeInfo node = b.getCurrentRoot();
            b.reset();
            return node;
        } catch (XPathException err) {
            throw new XPathException(err);
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
