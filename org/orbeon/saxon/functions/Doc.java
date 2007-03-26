package org.orbeon.saxon.functions;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.BooleanValue;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

/**
 * Implement the fn:doc() function - a simplified form of the Document function
 */

public class Doc extends SystemFunction {

    public static final int DOC = 0;
    public static final int DOC_AVAILABLE = 1;

    private String expressionBaseURI = null;

    public void checkArguments(StaticContext env) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(env);
            expressionBaseURI = env.getBaseURI();
        }
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        if (operation == DOC) {
            return doc(context);
        } else {
            // operation == DOC_AVAILABLE
            try {
                Controller controller = context.getController();
                // suppress all error messages while attempting to fetch the document
                ErrorListener old = controller.getErrorListener();
                controller.setErrorListener(new ErrorListener() {
                    public void warning(TransformerException exception) {}
                    public void error(TransformerException exception) {}
                    public void fatalError(TransformerException exception) {}
                });
                Item item = doc(context);
                controller.setErrorListener(old);
                return BooleanValue.get(item != null);
            } catch (XPathException err) {
                return BooleanValue.FALSE;
            }
        }
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return StaticProperty.ORDERED_NODESET |
                StaticProperty.PEER_NODESET |
                StaticProperty.NON_CREATIVE;
        // Declaring it as a peer node-set expression avoids sorting of expressions such as
        // doc(XXX)/a/b/c
        // The doc() function might appear to be creative: but it isn't, because multiple calls
        // with the same arguments will produce identical results.
    }

    private Item doc(XPathContext context) throws XPathException {
        AtomicValue hrefVal = (AtomicValue)argument[0].evaluateItem(context);
        if (hrefVal==null) {
            return null;
        }
        String href = hrefVal.getStringValue();
        Item item = Document.makeDoc(href, expressionBaseURI, context, this);
        if (item==null) {
            // we failed to read the document
            dynamicError("Failed to load document " + href, "FODC0005", context);
            return null;
        }
        return item;
    }

    /**
     * Copy the document identified by this expression to a given Receiver. This method is used only when it is
     * known that the document is being copied, because there is then no problem about node identity.
     */

    public void sendDocument(XPathContext context, Receiver out) throws XPathException {
        AtomicValue hrefVal = (AtomicValue)argument[0].evaluateItem(context);
        if (hrefVal==null) {
            return;
        }
        String href = hrefVal.getStringValue();
        Document.sendDoc(href, expressionBaseURI, context, this, out);
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
