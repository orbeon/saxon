package net.sf.saxon.functions;
import net.sf.saxon.Err;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.AnyURIValue;

import java.net.URI;
import java.net.URISyntaxException;

/**
* This class supports the resolve-uri() functions in XPath 2.0
*/

public class ResolveURI extends SystemFunction {

    String expressionBaseURI = null;

    public void checkArguments(StaticContext env) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(env);
            expressionBaseURI = env.getBaseURI();
        }
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        URI relativeURI;
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        if (arg0 == null) {
            return null;
        }
        String relative = arg0.getStringValue();
        try {
            relativeURI = new URI(relative);
        } catch (URISyntaxException err) {
            dynamicError("Relative URI " + Err.wrap(relative) + " is invalid: " + err.getMessage(),
                    "FORG0002", context);
            return null;
        }
        if (relativeURI.isAbsolute()) {
            return new StringValue(relative);
        }

        String base;
        URI baseURI;
        if (argument.length == 2) {
            base = argument[1].evaluateAsString(context);
        } else {
            base = expressionBaseURI;
            if (expressionBaseURI == null) {
                dynamicError("Base URI in static context is unknown",
                        "FONS0005", context);
                return null;
            }
        }
        try {
            baseURI = new URI(base);
        } catch (URISyntaxException err) {
            dynamicError("Base URI " + Err.wrap(base) + " is invalid: " + err.getMessage(),
                    "FORG0002", context);
            return null;
        }
        if (!baseURI.isAbsolute()) {
            // TODO: this rule has been removed from the spec
            dynamicError("Base URI " + Err.wrap(base) + " is not an absolute URI",
                    "FORG0009", context);
            return null;
        }
        URI resolvedURI = baseURI.resolve(relativeURI);
        return new AnyURIValue(resolvedURI.toString());
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
