package net.sf.saxon.value;
import net.sf.saxon.Err;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.Facet;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;

import java.util.Iterator;
import java.io.PrintStream;


/**
 * A DerivedAtomicValue is an atomic value of a user-defined type
 */

public class DerivedAtomicValue extends AtomicValue {

    // the value in the value-space of a primitive type
    AtomicValue primitiveValue;

    // the user-defined type to which this value belongs
    AtomicType typeLabel;

    private DerivedAtomicValue() {}

    /**
     * Create a value of a user-defined atomic type
     * @param primValue the value in the value space of the primitive type
     * @param lexicalValue the value in the lexical space. If null, the string value of primValue
     * is used. This value is checked against the pattern facet (if any)
     * @param type the required atomic type
     * @param throwError true if an exception is to be thrown when the value is
     * invalid (if false, the method returns null instead)
     * @throws XPathException if the value is invalid
     */

    public static DerivedAtomicValue makeValue(
            AtomicValue primValue, String lexicalValue, AtomicType type, boolean throwError)
            throws XPathException {
        DerivedAtomicValue val = new DerivedAtomicValue();
        val.primitiveValue = primValue;
        val.typeLabel = type;
        // TODO: no need to revalidate when atomizing an already-validated node
        XPathException err = val.checkAgainstFacets(lexicalValue);
        if (err != null) {
            if (throwError) {
                throw err;
            } else {
                return null;
            }
        }
        return val;
    }

    /**
     * Get the primitive value (the value in the value space)
     */

    public AtomicValue getPrimitiveValue() {
        return primitiveValue;
    }
    /**
     * Validate that the value conforms to all the facets for the given type.
     * @param lexicalValue The value to be checked against the pattern facet.
     * May be null; if so the actual value is converted to a string for checking
     * against the pattern.
     * @return null if the value is OK, otherwise an XPathException. Note that
     * the exception is returned rather than being thrown. This is designed to
     * make the "castable" operator more efficient.
     */

    private XPathException checkAgainstFacets(String lexicalValue) {
        Iterator iter = typeLabel.getFacets();
        StringValue normalizedValue = null;
        while (iter.hasNext()) {
            Facet f = (Facet)iter.next();
            if (f.getName().equals(Facet.PATTERN)) {
                if (normalizedValue == null) {
                    if (lexicalValue == null) {
                        normalizedValue = new StringValue(primitiveValue.getStringValue());
                    } else {
                        normalizedValue = new StringValue(typeLabel.applyWhitespaceNormalization(lexicalValue));
                    }
                }
                if (!f.testAtomicValue(normalizedValue)) {
                    return new DynamicError(
                            "Value " + Err.wrap(normalizedValue.toString(), Err.VALUE) + " violates the pattern facet " +
                            (f.getValue()==null ? "" : Err.wrap(f.getValue(), Err.VALUE) + " ") +
                            "of the type "
                            + typeLabel.getDescription());
                }
            } else {
                if (!f.testAtomicValue(primitiveValue)) {
                    return new DynamicError(
                            "Value " + Err.wrap(displayValue(), Err.VALUE) + " violates the " +
                            f.getName() + " facet " +
                            (f.getValue()==null ? "" : Err.wrap(f.getValue(), Err.VALUE) + " ") +
                            "of the type "
                            + typeLabel.getDescription());
                    }
            }
        }

        return null;
    }

    private String displayValue() {
        String v = primitiveValue.getStringValue();
        if (v.length() > 30) {
            v = v.substring(0, 30) + "...";
        }
        return v;
    }

    /**
     * Convert the value to a given type. The result of the conversion will be an
     * atomic value of the required type.
     *
     * @exception XPathException if conversion is not allowed for this
     *     required type, or if the particular value cannot be converted
     * @param requiredType type code of the required atomic type
     * @return the result of the conversion, if conversion was possible. This
     *      will always be an instance of the class corresponding to the type
     *      of value requested
     */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
         return primitiveValue.convert(requiredType, context);
    }

    /**
     * Evaluate the value (this simply returns the value unchanged)
     *
     * @exception XPathException
     * @param context the evaluation context (not used in this implementation)
     * @return the value, unchanged
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return this;
    }

    /**
     * Evaluate as a string
     */

    public String getStringValue() {
        return primitiveValue.getStringValue();
    }

    /**
     * Get the effective boolean value of the value
     *
     * @param context the evaluation context (not used in this implementation)
     * @return true, unless the value is boolean false, numeric zero, or
     *     zero-length string
     */
    public boolean effectiveBooleanValue(XPathContext context) {
        return primitiveValue.effectiveBooleanValue(context);
    }



    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        return primitiveValue.convertToJava(target, config, context);
    }

    /**
     * Get the item type of this atomic value
     * @return the item type
     */

    public ItemType getItemType() {
        return typeLabel;
    }

    /**
     * Generate a hash code to support equality testing
     * @return the hash code of the primitive base value
     */

    public int hashCode() {
        return primitiveValue.hashCode();
    }

    /**
     * Determine if this value is equal to another value
     * @param obj the other value to be compared
     * @return true if the values are equal according to the XPath rules
     */

    public boolean equals(Object obj) {
        return primitiveValue.equals(obj);
    }

    /**
     * Generate a string representation suitable for error messages
     * @return the string representation for the primitive base type
     */

    public String toString() {
        return primitiveValue.toString();
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

