package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Name;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.QNameValue;


/**
* This class supports the fn:QName() function (previously named fn:expanded-QName())
*/

public class QNameFn extends SystemFunction {

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);

        String uri;
        if (arg0 == null) {
            uri = null;
        } else {
            uri = arg0.getStringValue();
        }

        try {
            String lex = argument[1].evaluateItem(context).getStringValue();
            String[] parts = Name.getQNameParts(lex);
            return new QNameValue(parts[0], uri, parts[1]);
        } catch (QNameException e) {
            dynamicError(e.getMessage(), "FOCA0002", context);
            return null;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
