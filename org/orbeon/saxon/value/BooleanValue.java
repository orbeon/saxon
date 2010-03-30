package org.orbeon.saxon.value;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ConversionResult;
import org.orbeon.saxon.type.ValidationFailure;

/**
 * A boolean XPath value
 */

public final class BooleanValue extends AtomicValue implements Comparable {
    private boolean value;

    /**
     * The boolean value TRUE
     */
    public static final BooleanValue TRUE = new BooleanValue(true);
    /**
     * The boolean value FALSE
     */
    public static final BooleanValue FALSE = new BooleanValue(false);

    /**
     * Private Constructor: create a boolean value. Only two instances of this class are
     * ever created, one to represent true and one to represent false.
     * @param value the initial value, true or false
     */

    private BooleanValue(boolean value) {
        this.value = value;
        typeLabel = BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Factory method: get a BooleanValue
     *
     * @param value true or false, to determine which boolean value is
     *     required
     * @return the BooleanValue requested
     */

    public static BooleanValue get(boolean value) {
        return (value ? TRUE : FALSE);
    }

    /**
     * Create a new Boolean value with a user-supplied type label.
     * It is the caller's responsibility to ensure that the value is valid for the subtype
     * @param value the boolean value
     * @param typeLabel the type label, xs:boolean or a subtype
     */

    public BooleanValue(boolean value, AtomicType typeLabel) {
        this.value = value;
        this.typeLabel = typeLabel;
    }

    /**
     * Create a copy of this atomic value (usually so that the type label can be changed).
     * The type label of the copy will be reset to the primitive type.
     * @param typeLabel the atomic type label to be added to the copied value
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        BooleanValue v = new BooleanValue(value);
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Convert a string to a boolean value, using the XML Schema rules (including
     * whitespace trimming)
     * @param s the input string
     * @return the relevant BooleanValue if validation succeeds; or a ValidationFailure if not.
     */

    public static ConversionResult fromString(CharSequence s) {
        // implementation designed to avoid creating new objects
        s = Whitespace.trimWhitespace(s);
        int len = s.length();
        if (len == 1) {
            char c = s.charAt(0);
            if (c == '1') {
                return TRUE;
            } else if (c == '0') {
                return FALSE;
            }
        } else if (len == 4) {
            if (s.charAt(0) == 't' && s.charAt(1) == 'r' && s.charAt(2) == 'u' && s.charAt(3) == 'e') {
                return TRUE;
            }
        } else if (len == 5) {
            if (s.charAt(0) == 'f' && s.charAt(1) == 'a' && s.charAt(2) == 'l' && s.charAt(3) == 's' && s.charAt(4) == 'e') {
                return FALSE;
            }
        }
        ValidationFailure err = new ValidationFailure(
                            "The string " + Err.wrap(s, Err.VALUE) + " cannot be cast to a boolean");
        err.setErrorCode("FORG0001");
        return err;
    }

    /**
     * Get the value
     * @return true or false, the actual boolean value of this BooleanValue
     */

    public boolean getBooleanValue() {
        return value;
    }

    /**
     * Get the effective boolean value of this expression
     *
     * @return the boolean value
     */
    public boolean effectiveBooleanValue() {
        return value;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context XPath dynamic context
     * @return an AtomicValue, a value of the required type
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getPrimitiveType()) {
        case StandardNames.XS_BOOLEAN:
        case StandardNames.XS_ANY_ATOMIC_TYPE:
            return this;
        case StandardNames.XS_NUMERIC:
        case StandardNames.XS_INTEGER:
            return (value ? Int64Value.PLUS_ONE : Int64Value.ZERO);
        case StandardNames.XS_DECIMAL:
            return (value ? DecimalValue.ONE : DecimalValue.ZERO);
        case StandardNames.XS_FLOAT:
            return (value ? FloatValue.ONE : FloatValue.ZERO);
        case StandardNames.XS_DOUBLE:
            return (value ? DoubleValue.ONE : DoubleValue.ZERO);
        case StandardNames.XS_STRING:
            return (value ? StringValue.TRUE : StringValue.FALSE);
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationFailure err = new ValidationFailure("Cannot convert boolean to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    /**
     * Convert to string
     * @return "true" or "false"
     */

    public String getStringValue() {
        return (value ? "true" : "false");
    }

    /**
     * Convert to Java object (for passing to external functions)
     *
     * @param target the Java class to which conversion is required
     * @exception XPathException if conversion is not possible or fails
     * @return An object of the specified Java class
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//        if (target==Object.class) {
//            return Boolean.valueOf(value);
//        } else if (target.isAssignableFrom(BooleanValue.class)) {
//            return this;
//        } else if (target==boolean.class) {
//            return Boolean.valueOf(value);
//        } else if (target==Boolean.class) {
//            return Boolean.valueOf(value);
//        } else {
//            Object o = super.convertSequenceToJava(target, context);
//            if (o == null) {
//                XPathException err = new XPathException("Conversion of xs:boolean to " + target.getName() +
//                        " is not supported");
//                err.setXPathContext(context);
//                err.setErrorCode(SaxonErrorCode.SXJE0001);
//                throw err;
//            }
//            return o;
//        }
//    }


    /**
     * Get a Comparable value that implements the XML Schema ordering comparison semantics for this value.
     * The default implementation returns "this". This is overridden for particular atomic types.
     * <p/>
     * <p>In the case of data types that are partially ordered, the returned Comparable extends the standard
     * semantics of the compareTo() method by returning the value {@link #INDETERMINATE_ORDERING} when there
     * is no defined order relationship between two given values.</p>
     *
     * @return a Comparable that follows XML Schema comparison rules
     */

    public Comparable getSchemaComparable() {
        return new BooleanComparable();
    }

    private class BooleanComparable implements Comparable {

        public boolean asBoolean() {
            return BooleanValue.this.getBooleanValue();
        }

        public int compareTo(Object o) {
            return equals(o) ? 0 : INDETERMINATE_ORDERING;
        }

        public boolean equals(Object o) {
            return o instanceof BooleanComparable && asBoolean() == ((BooleanComparable)o).asBoolean();
        }

        public int hashCode() {
            return asBoolean() ? 9999999 : 8888888;
        }

    }

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns null. This is overridden for types that allow ordered comparisons in XPath: numeric, boolean,
     * string, date, time, dateTime, yearMonthDuration, dayTimeDuration, and anyURI.
     * @param ordered
     * @param collator
     * @param context
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
        return this;
    }

    /**
     * Compare the value to another boolean value
     *
     * @throws ClassCastException if the other value is not a BooleanValue
     *     (the parameter is declared as Object to satisfy the Comparable
     *     interface)
     * @param other The other boolean value
     * @return -1 if this one is the lower, 0 if they are equal, +1 if this
     *     one is the higher. False is considered to be less than true.
     */

    public int compareTo(Object other) {
        if (!(other instanceof BooleanValue)) {
            throw new ClassCastException("Boolean values are not comparable to " + other.getClass());
        }
        if (value == ((BooleanValue)other).value) return 0;
        if (value) return +1;
        return -1;
    }

    /**
     * Determine whether two boolean values are equal
     *
     * @param other the value to be compared to this value
     * @return true if the other value is a boolean value and is equal to this
     *      value
     * @throws ClassCastException if other value is not xs:boolean or derived therefrom
     */
    public boolean equals(Object other) {
        BooleanValue val = (BooleanValue)other;
        return (value == val.value);
    }

    /**
     * Get a hash code for comparing two BooleanValues
     *
     * @return the hash code
     */
    public int hashCode() {
        return (value ? 0 : 1);
    }

    /**
     * Diagnostic display of this value as a string
     * @return a string representation of this value: "true()" or "false()"
     */
    public String toString() {
        return getStringValue() + "()";
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

