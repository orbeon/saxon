package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.sort.CodepointCollator;

import java.util.Comparator;

/**
* An Untyped Atomic value. This inherits from StringValue for implementation convenience, even
* though an untypedAtomic value is not a String in the data model type hierarchy.
*/

public class UntypedAtomicValue extends StringValue {

    public static final UntypedAtomicValue ZERO_LENGTH_UNTYPED =
            new UntypedAtomicValue("");

    // If the value is used once as a number, it's likely that it will be used
    // repeatedly as a number, so we cache the result of conversion

    DoubleValue doubleValue = null;

    /**
    * Constructor
    * @param value the String value. Null is taken as equivalent to "".
    */

    public UntypedAtomicValue(CharSequence value) {
        this.value = (value==null ? "" : value);
    }

    /**
    * Return the type of the expression
    * @return Type.UNTYPED_ATOMIC (always)
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.UNTYPED_ATOMIC_TYPE;
    }

    /**
    * Convert to target data type
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        int req = requiredType.getFingerprint();
        if (req==Type.STRING) {
            if (value.length() == 0) {
                // this case is common!
                return StringValue.EMPTY_STRING;
            } else {
                return new StringValue(value);
            }
        } else if (req==Type.UNTYPED_ATOMIC) {
            return this;
        } else if (req==Type.DOUBLE || req==Type.NUMBER) {
            // for conversion to double (common in 1.0 mode), cache the result
            if (doubleValue==null) {
                AtomicValue v = super.convertPrimitive(requiredType, validate, context);
                if (v instanceof DoubleValue) {
                    // the alternative is that it's an ErrorValue
                    doubleValue = (DoubleValue)v;
                }
                return v;
            }
            return doubleValue;
        } else {
            return super.convertPrimitive(requiredType, validate, context);
        }
    }

    /**
    * Compare an untypedAtomic value with another value, using a given collator to perform
    * any string comparisons. This works by converting the untypedAtomic value to the type
     * of the other operand, which is the correct behavior for operators like "=" and "!=",
     * but not for "eq" and "ne": in the latter case, the untypedAtomic value is converted
     * to a string and this method is therefore not used.
     * @return -1 if the this value is less than the other, 0 if they are equal, +1 if this
     * value is greater.
     * @throws ClassCastException if the value cannot be cast to the type of the other operand
    */

    public int compareTo(Object other, Comparator collator, XPathContext context) {
        if (other instanceof NumericValue) {
            if (doubleValue == null) {
                try {
                    doubleValue = (DoubleValue)convert(Type.DOUBLE, context);
                } catch (XPathException err) {
                    throw new ClassCastException("Cannot convert untyped value " +
                        '\"' + getStringValueCS() + "\" to a double");
                }
            }
            return doubleValue.compareTo(other);
        } else if (other instanceof StringValue) {
            if (collator instanceof CodepointCollator) {
                // This optimization avoids creating String objects for the purpose of the comparison
                return ((CodepointCollator)collator).compareCS(getStringValueCS(),
                                                              ((StringValue)other).getStringValueCS());
            } else {
                return collator.compare(getStringValue(), ((StringValue)other).getStringValue());
            }
        } else if (other instanceof AtomicValue) {
            final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
            AtomicValue conv =
                    convert((AtomicType)((Value)other).getItemType(th), context, true);
            if (conv instanceof ValidationErrorValue) {
                throw new ClassCastException("Cannot convert untyped atomic value '" + getStringValue()
                        + "' to type " + ((Value)other).getItemType(th));
            }
            if (!(conv instanceof Comparable)) {
                throw new ClassCastException("Type " + ((Value)other).getItemType(th) + " is not ordered");
            }
            return ((Comparable)conv).compareTo(other);

        } else {
            // I'm not sure if we need this, but it does no harm
            return collator.compare(getStringValue(), other.toString());
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

