package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;

/**
 * A QName value. This implements the so-called "triples proposal", in which the prefix is retained as
 * part of the value. The prefix is not used in any operation on a QName other than conversion of the
 * QName to a string.
 */

public class QNameValue extends QualifiedNameValue {


    /**
     * Constructor for a QName that is known to be valid. No validation takes place.
     * @param prefix The prefix part of the QName (not used in comparisons). Use "" to represent the
     * default prefix.
     * @param uri The namespace part of the QName. Use "" to represent the non-namespace.
     * @param localName The local part of the QName
     */

    public QNameValue(String prefix, String uri, String localName) {
        this(prefix, uri, localName, BuiltInAtomicType.QNAME);
    }

    /**
     * Constructor for a QName that is known to be valid, allowing a user-defined subtype of QName
     * to be specified. No validation takes place.
     * @param prefix The prefix part of the QName (not used in comparisons). Use "" to represent the
     * default prefix (but null is also accepted)
     * @param uri The namespace part of the QName. Use null to represent the non-namespace (but "" is also
     * accepted).
     * @param localName The local part of the QName
     * @param type The type label, xs:QName or a subtype of xs:QName
     */

    public QNameValue(String prefix, String uri, String localName, AtomicType type) {
        qName = new StructuredQName(prefix, uri, localName);
        if (type == null) {
            type = BuiltInAtomicType.QNAME;
        }
        typeLabel = type;
    }

    /**
     * Constructor starting from a NamePool namecode
     * @param namePool The name pool containing the specified name code
     * @param nameCode The name code identifying this name in the name pool
     */

    public QNameValue(NamePool namePool, int nameCode) {
        String prefix = namePool.getPrefix(nameCode);
        String uri = namePool.getURI(nameCode);
        String localPart = namePool.getLocalName(nameCode);
        qName = new StructuredQName(prefix, uri, localPart);
        typeLabel = BuiltInAtomicType.QNAME;
    }


    /**
     * Constructor. This constructor validates that the local part is a valid NCName.
     * @param prefix The prefix part of the QName (not used in comparisons). Use "" to represent the
     * default prefix (but null is also accepted).
     * Note that the prefix is not checked for lexical correctness, because in most cases
     * it will already have been matched against in-scope namespaces. Where necessary the caller must
     * check the prefix.
     * @param uri The namespace part of the QName. Use null to represent the non-namespace (but "" is also
     * accepted).
     * @param localName The local part of the QName
     * @param type The atomic type, which must be either xs:QName, or a
     * user-defined type derived from xs:QName by restriction
     * @param checker NameChecker used to check the name against XML 1.0 or XML 1.1 rules. Supply null
     * if the name does not need to be checked (the caller asserts that it is known to be valid)
     * @throws XPathException if the local part of the name is malformed or if the name has a null
     * namespace with a non-empty prefix
     */

    public QNameValue(String prefix, String uri, String localName, AtomicType type, NameChecker checker) throws XPathException {
        if (checker != null && !checker.isValidNCName(localName)) {
            XPathException err = new XPathException("Malformed local name in QName: '" + localName + '\'');
            err.setErrorCode("FORG0001");
            throw err;
        }
        prefix = (prefix==null ? "" : prefix);
        uri = ("".equals(uri) ? null : uri);
        if (checker != null && uri == null && prefix.length() != 0) {
            XPathException err = new XPathException("QName has null namespace but non-empty prefix");
            err.setErrorCode("FOCA0002");
            throw err;
        }
        qName = new StructuredQName(prefix, uri, localName);
        typeLabel = type;
    }

    /**
     * Constructor
     * @param qName the name as a StructuredQName
     * @param typeLabel idenfies a subtype of xs:QName
     */

    public QNameValue(StructuredQName qName, AtomicType typeLabel) {
        this.qName = qName;
        this.typeLabel = typeLabel;
    }

    /**
     * Convert to a StructuredQName
     * @return the name as a StructuredQName
     */

    public StructuredQName toStructuredQName() {
        return qName;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        return new QNameValue(qName, typeLabel);
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.QNAME;
    }

    /**
     * Convert a QName to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context XPath dynamic context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getPrimitiveType()) {
            case StandardNames.XS_ANY_ATOMIC_TYPE:
            case StandardNames.XS_QNAME:
                return this;
            case StandardNames.XS_STRING:
                return new StringValue(getStringValue());
            case StandardNames.XS_UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValue());
            default:
                ValidationFailure err = new ValidationFailure("Cannot convert QName to " +
                        requiredType.getDisplayName());
                err.setErrorCode("XPTY0004");
                return err;
        }
    }

    /**
     * Get a component. Returns a zero-length string if the namespace-uri component is
     * requested and is not present.
     * @param part either Component.LOCALNAME or Component.NAMESPACE indicating which
     * component of the value is required
     * @return either the local name or the namespace URI, in each case as a StringValue
     */

    public AtomicValue getComponent(int part) {
        if (part == Component.LOCALNAME) {
            return (AtomicValue)StringValue.makeRestrictedString(
                    getLocalName(), BuiltInAtomicType.NCNAME, null);
        } else if (part == Component.NAMESPACE) {
            return new AnyURIValue(getNamespaceURI());
        } else if (part == Component.PREFIX) {
            String prefix = getPrefix();
            if (prefix.length() == 0) {
                return null;
            } else {
                return (AtomicValue)StringValue.makeRestrictedString(
                        prefix, BuiltInAtomicType.NCNAME, null);
            }
        } else {
            throw new UnsupportedOperationException("Component of QName must be URI, Local Name, or Prefix");
        }
    }

    /**
     * Determine if two QName values are equal. This comparison ignores the prefix part
     * of the value.
     * @throws ClassCastException if they are not comparable
     */

    public boolean equals(Object other) {
        return qName.equals(((QNameValue)other).qName);
    }

    public Comparable getSchemaComparable() {
        return new QNameComparable();
    }

    private class QNameComparable implements Comparable {

        public QNameValue getQNameValue() {
            return QNameValue.this;
        }

        public int compareTo(Object o) {
            return equals(o) ? 0 : INDETERMINATE_ORDERING;
        }

        public boolean equals(Object o) {
            return (o instanceof QNameComparable && qName.equals(((QNameComparable)o).getQNameValue().qName));
        }

        public int hashCode() {
            return qName.hashCode();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

