package org.orbeon.saxon.value;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
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
        s = trimWhitespace(s).toString();
        if (s.equals("true") || s.equals("1")) {
            return TRUE;
        } else if (s.equals("false") || s.equals("0")) {
            return FALSE;
        } else {
            ValidationException err = new ValidationException(
                                "The string " + Err.wrap(s, Err.VALUE) + " cannot be cast to a boolean");
            err.setErrorCode("FORG0001");
            return new ErrorValue(err);
        }
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
     * @return an AtomicValue, a value of the required type
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate) {
        switch(requiredType.getPrimitiveType()) {
        case Type.BOOLEAN:
        case Type.ATOMIC:
        case Type.ITEM:
            return this;
        case Type.NUMBER:
        case Type.INTEGER:
            return new IntegerValue(value ? 1 : 0);
        case Type.DECIMAL:
        case Type.FLOAT:
        case Type.DOUBLE:
            return new IntegerValue(value ? 1 : 0).convertPrimitive(requiredType, validate);
        case Type.STRING:
            return new StringValue(getStringValueCS());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationException err = new ValidationException("Cannot convert boolean to " +
                                     requiredType.getDisplayName());
            //err.setXPathContext(context);
            err.setErrorCode("FORG0001");
            return new ErrorValue(err);
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
     */

    public ItemType getItemType() {
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
                DynamicError err = new DynamicError("Conversion of boolean to " + target.getName() +
                        " is not supported");
                err.setXPathContext(context);
                err.setErrorCode("SAXON:0000");
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
     */
    public boolean equals(Object other) {
        if (!(other instanceof BooleanValue)) {
            throw new ClassCastException("Boolean values are not comparable to " + other.getClass());
        }
        return (this.value == ((BooleanValue)other).value);
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

