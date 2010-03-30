package org.orbeon.saxon.value;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;


/**
* An XPath value that encapsulates a Java object. Such a value can only be obtained by
* calling an extension function that returns it.
*/

public class ObjectValue extends AtomicValue {

    private Object value;

    /**
     * Default constructor for use in subclasses
     */

    public ObjectValue() {
        typeLabel = BuiltInAtomicType.ANY_ATOMIC;
    }

    /**
    * Constructor
    * @param object the object to be encapsulated
    */

    public ObjectValue(Object object) {
        value = object;
        typeLabel = BuiltInAtomicType.ANY_ATOMIC;
    }

    /**
    * Constructor
    * @param object the object to be encapsulated
    * @param type the type of the external object
    */

    public ObjectValue(Object object, ExternalObjectType type) {
        value = object;
        typeLabel = type;
    }


    /**
     * Set the value in this object value
     * @param value the external value to be wrapped
     */

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        ObjectValue v = new ObjectValue(value);
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
        return BuiltInAtomicType.ANY_ATOMIC;
    }

    /**
     * Determine the data type of the items in the expression, if possible
     *
     * @param th The TypeHierarchy.
     * @return for the default implementation: AnyItemType (not known)
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (typeLabel.equals(BuiltInAtomicType.ANY_ATOMIC)) {
            if (th == null) {
                throw new NullPointerException("No TypeHierarchy supplied");
            } else {
                Configuration config = th.getConfiguration();
                typeLabel = new ExternalObjectType(value.getClass(), config);
            }
        }
        return typeLabel;
    }

    /**
     * Display the type name for use in error messages
     * @return the type name
     */

    public String displayTypeName() {
        return "java-type:" + value.getClass().getName();
    }

    /**
    * Convert to target data type
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getPrimitiveType()) {
        case StandardNames.XS_ANY_ATOMIC_TYPE:
        case StandardNames.SAXON_JAVA_LANG_OBJECT:
            return this;
        case StandardNames.XS_BOOLEAN:
            return BooleanValue.get(
                    (value != null && value.toString().length() > 0));
        case StandardNames.XS_STRING:
            return new StringValue(getStringValue());
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValue());
        default:
            return new StringValue(getStringValue()).convertPrimitive(requiredType, validate, context);
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
     * Get the effective boolean value of the value
     *
     * @return true, unless the value is boolean false, numeric zero, or
     *         zero-length string
     */
    public boolean effectiveBooleanValue() throws XPathException {
        return value != null;
    }

    /**
     * Get the encapsulated object
     * @return the Java object that this external object wraps
    */

    public Object getObject() {
        return value;
    }


    public Comparable getSchemaComparable() {
        throw new UnsupportedOperationException("External objects cannot be compared according to XML Schema rules");
    }


    /**
     * Get an object value that implements the XPath equality and ordering comparison semantics for this value.
     * If the ordered parameter is set to true, the result will be a Comparable and will support a compareTo()
     * method with the semantics of the XPath lt/gt operator, provided that the other operand is also obtained
     * using the getXPathComparable() method. In all cases the result will support equals() and hashCode() methods
     * that support the semantics of the XPath eq operator, again provided that the other operand is also obtained
     * using the getXPathComparable() method. A context argument is supplied for use in cases where the comparison
     * semantics are context-sensitive, for example where they depend on the implicit timezone or the default
     * collation.
     *
     * @param ordered true if an ordered comparison is required. In this case the result is null if the
     *                type is unordered; in other cases the returned value will be a Comparable.
     * @param collator
     *@param context the XPath dynamic evaluation context, used in cases where the comparison is context
     *                sensitive @return an Object whose equals() and hashCode() methods implement the XPath comparison semantics
     *         with respect to this atomic value. If ordered is specified, the result will either be null if
     *         no ordering is defined, or will be a Comparable
     */

    public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
        return (ordered ? null : this);
    }

    /**
    * Determine if two ObjectValues are equal
    * @throws ClassCastException if they are not comparable
    */

    public boolean equals(Object other) {
        return value.equals(((ObjectValue)other).value);
    }

    public int hashCode() {
        return value.hashCode();
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//
//        if (value==null) return null;
//
//        if (target.isAssignableFrom(value.getClass())) {
//            return value;
//        } else if (target==Value.class || target==ObjectValue.class) {
//            return this;
//        } else if (target==String.class || target==CharSequence.class) {
//            return getStringValue();
//
//        } else {
//            Object o = convertSequenceToJava(target, context);
//            if (o == null) {
//                throw new XPathException("Conversion of external object to " + target.getName() +
//                            " is not supported");
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

