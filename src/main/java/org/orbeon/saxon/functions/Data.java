package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Atomizer;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.trans.XPathException;

/**
* Implement XPath function fn:data()
*/

public class Data extends CompileTimeFunction {

    /**
    * Simplify and validate.
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        Atomizer a = new Atomizer(argument[0], visitor.getConfiguration());
        ExpressionTool.copyLocationInfo(this, a);
        return visitor.simplify(a);
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
