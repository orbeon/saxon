package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.xpath.XPathException;

/**
* This class supports the get_X_from_Y functions defined in XPath 2.0
*/

public class Component extends SystemFunction {

    public final static int YEAR        = 1;
    public final static int MONTH       = 2;
    public final static int DAY         = 3;
    public final static int HOURS       = 4;
    public final static int MINUTES     = 5;
    public final static int SECONDS     = 6;
    public final static int TIMEZONE    = 7;
    public final static int LOCALNAME   = 8;
    public final static int NAMESPACE   = 9;


    //int targetType;
    int component;

     public Expression simplify(StaticContext env) throws XPathException {
        component = (operation>>16) & 0xffff;
        //targetType = operation & 0xffff;
        return super.simplify(env);
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg = (AtomicValue)argument[0].evaluateItem(context);

        if (arg == null) {
            return null;
        }

        return arg.getComponent(component);

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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
