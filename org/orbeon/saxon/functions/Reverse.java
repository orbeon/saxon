package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.sort.Reverser;
import net.sf.saxon.trans.XPathException;

/**
* Implement XPath function fn:reverse()
*/

public class Reverse extends CompileTimeFunction {

    /**
    * Simplify and validate.
    */

     public Expression simplify(StaticContext env) throws XPathException {
        Reverser a = new Reverser(argument[0]);
        return a.simplify(env);
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
