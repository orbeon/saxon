package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;

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
        typeLabel = BuiltInAtomicType.UNTYPED_ATOMIC;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        UntypedAtomicValue v = new UntypedAtomicValue(value);
        v.length = length;
        v.doubleValue = doubleValue;
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.UNTYPED_ATOMIC;
    }


    /**
    * Convert to target data type
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        int req = requiredType.getFingerprint();
        if (req== StandardNames.XS_STRING) {
            if (value.length() == 0) {
                // this case is common!
                return StringValue.EMPTY_STRING;
            } else {
                return new StringValue(value);
            }
        } else if (req== StandardNames.XS_UNTYPED_ATOMIC) {
            return this;
        } else if (req== StandardNames.XS_DOUBLE || req== StandardNames.XS_NUMERIC) {
            // for conversion to double (common in 1.0 mode), cache the result
            try {
                return toDouble();
            } catch (ValidationException e) {
                return new ValidationFailure(e);
            }
        } else {
            return super.convertPrimitive(requiredType, validate, context);
        }
    }

    /**
     * Convert the value to a double, returning a DoubleValue
     */

    private AtomicValue toDouble() throws ValidationException {
        if (doubleValue == null) {
            doubleValue = new DoubleValue(value);
        }
        return doubleValue;
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

    public int compareTo(AtomicValue other, StringCollator collator, XPathContext context) {
        if (other instanceof NumericValue) {
            if (doubleValue == null) {
                try {
                    doubleValue = (DoubleValue)convertPrimitive(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
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
                                                              other.getStringValueCS());
            } else {
                return collator.compareStrings(getStringValue(), other.getStringValue());
            }
        } else {
            final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
            ConversionResult result =
                    convert((AtomicType)other.getItemType(th), true, context);
            if (result instanceof ValidationFailure) {
                throw new ClassCastException("Cannot convert untyped atomic value '" + getStringValue()
                        + "' to type " + other.getItemType(th));
            }
            return ((Comparable)((AtomicValue)result)).compareTo(other);

        } 
    }

    /**
     * Convert to Java object (for passing to external functions)
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//        if (target == Object.class) {
//            return getStringValue();
//        } else if (target.isAssignableFrom(StringValue.class)) {
//            return this;
//        } else if (target == String.class || target == CharSequence.class) {
//            return getStringValue();
//        } else if (target == boolean.class) {
//            BooleanValue bval = (BooleanValue)convertPrimitive(BuiltInAtomicType.BOOLEAN, true, context).asAtomic();
//            return Boolean.valueOf(bval.getBooleanValue());
//        } else if (target == Boolean.class) {
//            BooleanValue bval = (BooleanValue)convertPrimitive(BuiltInAtomicType.BOOLEAN, true, context).asAtomic();
//            return Boolean.valueOf(bval.getBooleanValue());
//        } else if (target == double.class) {
//            DoubleValue dval = (DoubleValue)convertPrimitive(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
//            return new Double(dval.getDoubleValue());
//        } else if (target == Double.class) {
//            DoubleValue dval = (DoubleValue)convertPrimitive(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
//            return new Double(dval.getDoubleValue());
//        } else if (target == float.class) {
//            DoubleValue dval = (DoubleValue)convertPrimitive(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
//            return new Float(dval.getDoubleValue());
//        } else if (target == Float.class) {
//            DoubleValue dval = (DoubleValue)convertPrimitive(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
//            return new Float(dval.getDoubleValue());
//        } else if (target == long.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Long(dval.longValue());
//        } else if (target == Long.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Long(dval.longValue());
//        } else if (target == int.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Integer((int) dval.longValue());
//        } else if (target == Integer.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Integer((int) dval.longValue());
//        } else if (target == short.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Short((short) dval.longValue());
//        } else if (target == Short.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Short((short) dval.longValue());
//        } else if (target == byte.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Byte((byte) dval.longValue());
//        } else if (target == Byte.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Byte((byte) dval.longValue());
//        } else if (target == char.class || target == Character.class) {
//            if (value.length() == 1) {
//                return new Character(value.charAt(0));
//            } else {
//                XPathException de = new XPathException("Cannot convert xs:string to Java char unless length is 1");
//                de.setXPathContext(context);
//                de.setErrorCode(SaxonErrorCode.SXJE0005);
//                throw de;
//            }
//        } else {
//            Object o = super.convertSequenceToJava(target, context);
//            if (o == null) {
//                XPathException err = new XPathException("Conversion of xs:untypedAtomic to " + target.getName() + " is not supported");
//                err.setXPathContext(context);
//                err.setErrorCode(SaxonErrorCode.SXJE0006);
//                throw err;
//            }
//            return o;
//        }
//    }

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

