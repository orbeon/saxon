package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ConversionResult;
import org.orbeon.saxon.type.ValidationFailure;
import org.orbeon.saxon.value.*;

/**
 * Implements the XPath number() function. This can also be used as a mapping function
 * in a MappingIterator to map a sequence of values to numbers.
 */

public class NumberFn extends SystemFunction implements ItemMappingFunction {

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault();
        argument[0].setFlattened(true);
        return simplifyArguments(visitor);
    }


    /**
     * Add a representation of a doc() call or similar function to a PathMap.
     * This is a convenience method called by the addToPathMap() methods for doc(), document(), collection()
     * and similar functions. These all create a new root expression in the path map.
     *
     * @param pathMap      the PathMap to which the expression should be added
     * @param pathMapNodes the node in the PathMap representing the focus at the point where this expression
     *                     is called. Set to null if this expression appears at the top level.
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addDocToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodes) {
        PathMap.PathMapNodeSet result = argument[0].addToPathMap(pathMap, pathMapNodes);
        if (result != null) {
            result.setAtomized();
        }
        return null;
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
            ConversionResult result = ((AtomicValue)arg0).convert(BuiltInAtomicType.DOUBLE, true, context);
            if (result instanceof ValidationFailure) {
                return DoubleValue.NaN;
            } else {
                return (AtomicValue)result;
            }
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
     * @param value the value to be converted
     * @return the result of the conversion
     */

    public static DoubleValue convert(AtomicValue value) {
        try {
            if (value==null) {
                return DoubleValue.NaN;
            }
            if (value instanceof BooleanValue || value instanceof NumericValue) {
                ConversionResult result = value.convert(BuiltInAtomicType.DOUBLE, true, null);
                if (result instanceof ValidationFailure) {
                    return DoubleValue.NaN;
                } else {
                    return (DoubleValue)result;
                }
            }
            if (value instanceof StringValue && !(value instanceof AnyURIValue)) {
                CharSequence s = value.getStringValueCS();
                return new DoubleValue(Value.stringToNumber(s));
            }
            return DoubleValue.NaN;
        } catch (NumberFormatException e) {
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
