package net.sf.saxon.functions;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;

/** Implement the exists() and empty() functions **/

public class Existence extends SystemFunction {

    public static final int EXISTS = 0;
    public static final int EMPTY = 1;

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        argument[0] = ExpressionTool.unsorted(argument[0], false);
    }

    /**
    * Evaluate the function in a boolean context
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        switch (operation) {
            case EXISTS: return argument[0].iterate(c).next() != null;
            case EMPTY: return argument[0].iterate(c).next() == null;
        }
        return false;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(c));
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
