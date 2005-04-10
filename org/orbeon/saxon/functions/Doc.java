package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.Controller;

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
