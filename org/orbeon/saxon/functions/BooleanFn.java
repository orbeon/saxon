package net.sf.saxon.functions;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;

/**
* This class supports the XPath functions boolean(), not(), true(), and false()
*/


public class BooleanFn extends SystemFunction {

    public static final int BOOLEAN = 0;
    public static final int NOT = 1;
    public static final int TRUE = 2;
    public static final int FALSE = 3;

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        if (operation==BOOLEAN || operation==NOT) {
            argument[0] = ExpressionTool.unsortedIfHomogeneous(argument[0], false);
        }
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the effective boolean value
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        switch (operation) {
            case BOOLEAN:
                return argument[0].effectiveBooleanValue(c);
            case NOT:
                return !argument[0].effectiveBooleanValue(c);
            case TRUE:
                return true;
            case FALSE:
                return false;
            default:
                throw new UnsupportedOperationException("Unknown boolean operation");
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
