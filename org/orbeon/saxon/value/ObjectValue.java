package org.orbeon.saxon.value;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.type.ExternalObjectType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;


/**
* An XPath value that encapsulates a Java object. Such a value can only be obtained by
* calling an extension function that returns it.
*/

public class ObjectValue extends AtomicValue {
    private Object value;

    /**
     * Default constructor for use in subclasses
     */

    public ObjectValue() {}

    /**
    * Constructor
    * @param object the object to be encapsulated
    */

    public ObjectValue(Object object) {
        this.value = object;
    }

    /**
     * Set the value in this object value
     */

    public void setValue(Object value) {
        this.value = value;
    }

    /**
    * Convert to target data type
    */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        switch(requiredType) {
        case Type.ATOMIC:
        case Type.OBJECT:
        case Type.ITEM:
            return this;
        case Type.BOOLEAN:
            return BooleanValue.get(
                    (value==null ? false : value.toString().length() > 0));
        case Type.STRING:
            return new StringValue(getStringValue());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValue());
        default:
            return new StringValue(getStringValue()).convert(requiredType, context);
        }
    }

    /**
    * Get the value as a String
    * @return a String representation of the value
    */

    public String getStringValue() {
        return (value==null ? "" : value.toString());
    }

    /**
    * Determine the data type of the expression
    * @return Type.OBJECT
    */

    public ItemType getItemType() {
        return new ExternalObjectType(value.getClass());
    }

    /**
    * Get the encapsulated object
    */

    public Object getObject() {
        return value;
    }

    /**
    * Determine if two ObjectValues are equal
    * @throws ClassCastException if they are not comparable
    */

    public boolean equals(Object other) {
        return this.value.equals(((ObjectValue)other).value);
    }

    public int hashCode() {
        return value.hashCode();
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {

        if (value==null) return null;

        if (target.isAssignableFrom(value.getClass())) {
            return value;
        } else if (target==Value.class || target==ObjectValue.class) {
            return this;
        } else if (target==boolean.class || target==Boolean.class) {
            BooleanValue bval = (BooleanValue)convert(Type.BOOLEAN, null);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==double.class || target==Double.class) {
            DoubleValue bval = (DoubleValue)convert(Type.DOUBLE, null);
            return new Double(bval.getDoubleValue());
        } else if (target==float.class || target==Float.class) {
            DoubleValue bval = (DoubleValue)convert(Type.FLOAT, null);
            return new Float(bval.getDoubleValue());
        } else if (target==long.class || target==Long.class) {
            IntegerValue bval = (IntegerValue)convert(Type.INTEGER, null);
            return new Long(bval.longValue());
        } else if (target==int.class || target==Integer.class) {
            IntegerValue bval = (IntegerValue)convert(Type.INTEGER, null);
            return new Integer((int)bval.longValue());
        } else if (target==short.class || target==Short.class) {
            IntegerValue bval = (IntegerValue)convert(Type.INTEGER, null);
            return new Short((short)bval.longValue());
        } else if (target==byte.class || target==Byte.class) {
            IntegerValue bval = (IntegerValue)convert(Type.INTEGER, null);
            return new Byte((byte)bval.longValue());
        } else if (target==char.class || target==Character.class) {
            String s = getStringValue();
            if (s.length()==1) {
                return new Character(s.charAt(0));
            } else {
                throw new DynamicError("Cannot convert string to Java char unless length is 1");
            }
        } else {
            throw new DynamicError("Conversion of external object to " + target.getName() +
                        " is not supported");
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

