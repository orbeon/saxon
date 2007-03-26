package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;

/**
 * Implements the XPath number() function. This can also be used as a mapping function
 * in a MappingIterator to map a sequence of values to numbers.
 */

public class NumberFn extends SystemFunction implements ItemMappingFunction {

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
    */

     public Expression simplify(StaticContext env) throws XPathException {
        useContextItemAsDefault();
        return simplifyArguments(env);
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item arg0 = argument[0].evaluateItem(context);
        if (arg0==null) {
            return DoubleValue.NaN;
        }
        if (arg0 instanceof BooleanValue || arg0 instanceof NumericValue) {
            return ((AtomicValue)arg0).convert(Type.DOUBLE, context);
        }
        if (arg0 instanceof StringValue && !(arg0 instanceof AnyURIValue)) {
            CharSequence s = arg0.getStringValueCS();
            try {
                return new DoubleValue(Value.stringToNumber(s));
            } catch (NumberFormatException e) {
                return DoubleValue.NaN;
            }
        }
        return DoubleValue.NaN;
    }

    /**
     * Static method to perform the same conversion as the number() function. This is different from the
     * convert(Type.DOUBLE) in that it produces NaN rather than an error for non-numeric operands.
     */

    public static DoubleValue convert(AtomicValue value) {
        try {
            if (value==null) {
                return DoubleValue.NaN;
            }
            if (value instanceof BooleanValue || value instanceof NumericValue) {
                return (DoubleValue)value.convert(Type.DOUBLE, null);
            }
            CharSequence s = value.getStringValueCS();
            return new DoubleValue(Value.stringToNumber(s));
        } catch (NumberFormatException e) {
            return DoubleValue.NaN;
        } catch (XPathException e) {
            return DoubleValue.NaN;
        }
    }

    /**
     * Mapping function for use when converting a sequence of atomic values to doubles
     * using the rules of the number() function
     */

    public Item map(Item item) throws XPathException {
        return convert((AtomicValue)item);
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
