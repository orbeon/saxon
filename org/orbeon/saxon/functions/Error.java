package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.StaticError;

/**
* Implement XPath function fn:error()
*/

public class Error extends SystemFunction {

    /**
    * Simplify and validate.
    */

     public Expression simplify(StaticContext env) throws StaticError {
        return this;
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
    * Evaluation of the expression always throws an error
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item arg = null;
        if (argument.length > 0) {
            arg = argument[0].evaluateItem(context);
        }
        DynamicError e;
        if (arg==null) {
            e = new DynamicError("Error signalled by application call on error()");
        } else {
            e = new DynamicError(arg.getStringValue());
        }
        e.setErrorCode("FOER0000");
        e.setXPathContext(context);
        throw e;
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
