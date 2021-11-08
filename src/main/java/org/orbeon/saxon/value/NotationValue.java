package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;


/**
 * An xs:NOTATION value.
 */

public final class NotationValue extends QualifiedNameValue {

   /**
    * Constructor
    * @param prefix The prefix part of the QName (not used in comparisons). Use null or "" to represent the
    * default prefix.
    * @param uri The namespace part of the QName. Use null or "" to represent the null namespace.
    * @param localName The local part of the QName
    * @param checker Used for checking names against XML 1.0 or XML 1.1 syntax rules
    */

    public NotationValue(String prefix, String uri, String localName, NameChecker checker) throws XPathException {
        if (checker != null && !checker.isValidNCName(localName)) {
            XPathException err = new XPathException("Malformed local name in NOTATION: '" + localName + '\'');
            err.setErrorCode("FORG0001");
            throw err;
        }
        prefix = (prefix==null ? "" : prefix);
        uri = (uri==null ? "" : uri);
        if (checker != null && uri.length() == 0 && prefix.length() != 0) {
            XPathException err = new XPathException("NOTATION has null namespace but non-empty prefix");
            err.setErrorCode("FOCA0002");
            throw err;
        }
        qName = new StructuredQName(prefix, uri, localName);
        typeLabel = BuiltInAtomicType.NOTATION;
    }

   /**
     * Constructor for a value that is known to be valid
     * @param prefix The prefix part of the QName (not used in comparisons). Use null or "" to represent the
     * default prefix.
     * @param uri The namespace part of the QName. Use null or "" to represent the null namespace.
     * @param localName The local part of the QName
     */

    public NotationValue(String prefix, String uri, String localName) {
        qName = new StructuredQName(prefix, uri, localName);
        typeLabel = BuiltInAtomicType.NOTATION;
    }

    /**
     * Constructor for a value that is known to be valid
     * @param prefix The prefix part of the QName (not used in comparisons). Use null or "" to represent the
     * default prefix.
     * @param uri The namespace part of the QName. Use null or "" to represent the null namespace.
     * @param localName The local part of the QName
     * @param typeLabel A type derived from xs:NOTATION to be used for the new value
     */

     public NotationValue(String prefix, String uri, String localName, AtomicType typeLabel) {
         qName = new StructuredQName(prefix, uri, localName);
         this.typeLabel = typeLabel;
     }


    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        NotationValue v = new NotationValue(getPrefix(), getNamespaceURI(), getLocalName());
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
        return BuiltInAtomicType.NOTATION;
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context The XPath dynamic context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getPrimitiveType()) {
            case StandardNames.XS_ANY_ATOMIC_TYPE:
            case StandardNames.XS_NOTATION:
                return this;

            case StandardNames.XS_STRING:
                return new StringValue(getStringValue());
                
            case StandardNames.XS_UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValue());

            default:
                ValidationFailure err = new ValidationFailure("Cannot convert NOTATION to " +
                        requiredType.getDisplayName());
                err.setErrorCode("XPTY0004");
                return err;
        }
    }

    /**
     * Determine if two Notation values are equal. This comparison ignores the prefix part
     * of the value.
     * @throws ClassCastException if they are not comparable
     * @throws IllegalStateException if the two QNames are in different name pools
     */

    public boolean equals(Object other) {
        return qName.equals(((NotationValue)other).qName);
    }

    public Comparable getSchemaComparable() {
        return new NotationComparable();
    }

    private class NotationComparable implements Comparable {

        public NotationValue getNotationValue() {
            return NotationValue.this;
        }

        public int compareTo(Object o) {
            return equals(o) ? 0 : INDETERMINATE_ORDERING;
        }

        public boolean equals(Object o) {
            return (o instanceof NotationComparable && qName.equals(((NotationComparable)o).getNotationValue().qName));
        }

        public int hashCode() {
            return qName.hashCode();
        }
    }

     /**
     * The toString() method returns the name in the form QName("uri", "local")
     * @return the name in Clark notation: {uri}local
     */

    public String toString() {
        return "NOTATION(" + getClarkName() + ')';
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

