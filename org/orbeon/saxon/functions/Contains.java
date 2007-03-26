package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.sort.RuleBasedSubstringMatcher;
import org.orbeon.saxon.sort.SubstringMatcher;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.StringValue;

import java.text.RuleBasedCollator;
import java.util.Comparator;


/**
* This class implements the contains(), starts-with(), ends-with(),
* substring-before(), and substring-after() functions
*/

public class Contains extends CollatingFunction {

    public static final int CONTAINS = 0;
    public static final int STARTSWITH = 1;
    public static final int ENDSWITH = 2;
    public static final int AFTER = 3;
    public static final int BEFORE = 4;

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        Comparator collator = getCollator(2, context);

        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        if (arg0==null) {
            arg0 = StringValue.EMPTY_STRING;
        }

        AtomicValue arg1 = (AtomicValue)argument[1].evaluateItem(context);
        if (arg1==null) {
            arg1 = StringValue.EMPTY_STRING;
        };

        String s0 = arg0.getStringValue();
        String s1 = arg1.getStringValue();

        if (collator instanceof RuleBasedCollator) {
            collator = new RuleBasedSubstringMatcher((RuleBasedCollator)collator);
        }

        if (collator instanceof SubstringMatcher) {
            switch(operation) {
                case CONTAINS:
                    return BooleanValue.get(((SubstringMatcher)collator).contains(s0, s1));
                case STARTSWITH:
                    return BooleanValue.get(((SubstringMatcher)collator).startsWith(s0, s1));
                case ENDSWITH:
                    return BooleanValue.get(((SubstringMatcher)collator).endsWith(s0, s1));
                case AFTER:
                    return StringValue.makeStringValue(((SubstringMatcher)collator).substringAfter(s0, s1));
                case BEFORE:
                    return StringValue.makeStringValue(((SubstringMatcher)collator).substringBefore(s0, s1));
                default:
                    throw new UnsupportedOperationException("Unknown operation " + operation);
            }
        } else {

            dynamicError("The collation requested for " + getDisplayName(context.getNamePool()) +
                    " does not support substring matching", "FOCH0004", context);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
