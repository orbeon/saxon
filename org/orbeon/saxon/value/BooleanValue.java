package org.orbeon.saxon.value;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.type.*;

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
     * Convert a string to a boolean value, using the XML Schema rules (including
     * whitespace trimming)
     * @param s the input string
     * @return the relevant BooleanValue if validation succeeds; or an ErrorValue if not.
     */

    public static AtomicValue fromString(CharSequence s) {
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
        ValidationException err = new ValidationException(
                            "The string " + Err.wrap(s, Err.VALUE) + " cannot be cast to a boolean");
        err.setErrorCode("FORG0001");
        return new ValidationErrorValue(err);
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
     * @param context dynamic evaluation context, not used in this
     *     implementation
     * @return the boolean value
     */
    public boolean effectiveBooleanValue(XPathContext context) {
        return value;
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context
     * @return an AtomicValue, a value of the required type
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getPrimitiveType()) {
        case Type.BOOLEAN:
        case Type.ANY_ATOMIC:
        case Type.ITEM:
            return this;
        case Type.NUMBER:
        case Type.INTEGER:
            return (value ? IntegerValue.PLUS_ONE : IntegerValue.ZERO);
        case Type.DECIMAL:
            return (value ? DecimalValue.ONE : DecimalValue.ZERO);
        case Type.FLOAT:
            return (value ? FloatValue.ONE : FloatValue.ZERO);
        case Type.DOUBLE:
            return (value ? DoubleValue.ONE : DoubleValue.ZERO);
        case Type.STRING:
            return (value ? StringValue.TRUE : StringValue.FALSE);
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationException err = new ValidationException("Cannot convert boolean to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            return new ValidationErrorValue(err);
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
     * Determine the data type of the expression
     * @return Type.BOOLEAN,
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.BOOLEAN_TYPE;
    }

    /**
     * Convert to Java object (for passing to external functions)
     *
     * @param target the Java class to which conversion is required
     * @exception XPathException if conversion is not possible or fails
     * @return An object of the specified Java class
     */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target==Object.class) {
            return Boolean.valueOf(value);
        } else if (target.isAssignableFrom(BooleanValue.class)) {
            return this;
        } else if (target==boolean.class) {
            return Boolean.valueOf(value);
        } else if (target==Boolean.class) {
            return Boolean.valueOf(value);
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==double.class) {
            return new Double((double)(value ? 1 : 0));
        } else if (target==Double.class) {
            return new Double((double)(value ? 1 : 0));
        } else if (target==float.class) {
            return new Float((float)(value ? 1 : 0));
        } else if (target==Float.class) {
            return new Float((float)(value ? 1 : 0));
        } else if (target==long.class) {
            return new Long((long)(value ? 1 : 0));
        } else if (target==Long.class) {
            return new Long((long)(value ? 1 : 0));
        } else if (target==int.class) {
            return new Integer(value ? 1 : 0);
        } else if (target==Integer.class) {
            return new Integer(value ? 1 : 0);
        } else if (target==short.class) {
            return new Short((short)(value ? 1 : 0));
        } else if (target==Short.class) {
            return new Short((short)(value ? 1 : 0));
        } else if (target==byte.class) {
            return new Byte((byte)(value ? 1 : 0));
        } else if (target==Byte.class) {
            return new Byte((byte)(value ? 1 : 0));
        } else if (target==char.class) {
            return new Character(value ? '1' : '0');
        } else if (target==Character.class) {
            return new Character(value ? '1' : '0');
        } else {
            Object o = super.convertToJava(target, context);
            if (o == null) {
                DynamicError err = new DynamicError("Conversion of xs:boolean to " + target.getName() +
                        " is not supported");
                err.setXPathContext(context);
                err.setErrorCode(SaxonErrorCode.SXJE0001);
                throw err;
            }
            return o;
        }
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
        if (this.value == ((BooleanValue)other).value) return 0;
        if (this.value) return +1;
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
        BooleanValue val = (BooleanValue)((AtomicValue)other).getPrimitiveValue();
        return (this.value == val.value);
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

