package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.xpath.XPathException;

import java.util.StringTokenizer;

/**
 * Implement the XPath normalize-space() function
 */

public class NormalizeSpace extends SystemFunction {

    /**
    * Simplify and validate.
    */

     public Expression simplify(StaticContext env) throws XPathException {
        useContextItemAsDefault();
        return simplifyArguments(env);
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
        if (sv==null) return StringValue.EMPTY_STRING;
        return new StringValue(normalize(sv.getStringValue()));
    }

    /**
    * The algorithm that does the work
    */

    public static String normalize(String s) {
        StringBuffer sb = new StringBuffer();
        StringTokenizer st = new StringTokenizer(s);
        while (st.hasMoreTokens()) {
            sb.append(st.nextToken());
            if (st.hasMoreTokens()) sb.append(" ");
        }
        return sb.toString();
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
