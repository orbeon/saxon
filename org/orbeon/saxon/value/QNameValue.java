package net.sf.saxon.value;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.Component;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.XMLChar;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;

import javax.xml.namespace.QName;


/**
 * A QName value. This implements the so-called "triples proposal", in which the prefix is retained as
 * part of the value. The prefix is not used in any operation on a QName other than conversion of the
 * QName to a string.
 */

public class QNameValue extends AtomicValue {

    private String prefix;  // "" for the default prefix
    private String uri;     // "" for the null namespace
    private String localPart;

    // Note: an alternative design was considered in which the QName was represented by a NamePool and a
    // nameCode. This caused difficulties because there is not always enough context information available
    // when creating a QName to locate the NamePool.

    /**
     * Constructor
     * @param namePool The name pool containing the specified name code
     * @param nameCode The name code identifying this name in the name pool
     */

    public QNameValue(NamePool namePool, int nameCode) {
        prefix = namePool.getPrefix(nameCode);
        uri = namePool.getURI(nameCode);
        localPart = namePool.getLocalName(nameCode);
    }

    /**
     * Constructor. This constructor validates that the local part is a valid NCName.
     * @param prefix The prefix part of the QName (not used in comparisons). Use null or "" to represent the
     * default prefix.
     * @param uri The namespace part of the QName. Use null or "" to represent the null namespace.
     * @param localName The local part of the QName
     */

    public QNameValue(String prefix, String uri, String localName) throws XPathException {
        if (!XMLChar.isValidNCName(localName)) {
            DynamicError err = new DynamicError("Malformed local name in QName: '" + localName + '\'');
            err.setErrorCode("FORG0001");
            throw err;
        }
        this.prefix = (prefix==null ? "" : prefix);
        this.uri = (uri==null ? "" : uri);
        this.localPart = localName;
    }

    /**
     * Construct a QNameValue from a JAXP QName. This does no validation of the components of
     * the supplied QName.
     */

    public QNameValue(QName qname) {
        this.prefix = qname.getPrefix();
        this.uri = qname.getNamespaceURI();
        this.localPart = qname.getLocalPart();
    }

    /**
     * Get the string value as a String. Returns the QName as a lexical QName, retaining the original
     * prefix if available.
     */

    public String getStringValue() {
        if ("".equals(prefix)) {
            return localPart;
        } else {
            return prefix + ':' + localPart;
        }
    }

    /**
     * Get the value as a JAXP QName
     */

    public QName getQName() {
        return new QName(uri, localPart, prefix);
    }

    /**
     * Get the name in Clark notation, that is {uri}local
     */

    public String getClarkName() {
        if ("".equals(uri)) {
            return localPart;
        } else {
            return '{' + uri + '}' + localPart;
        }
    }

    /**
     * Get the local part
     */

    public String getLocalName() {
        return localPart;
    }

    /**
     * Get the namespace part (null means no namespace)
     */

    public String getNamespaceURI() {
        return ("".equals(uri) ? null : uri);
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
            try {
                return new RestrictedStringValue(localPart, StandardNames.XS_NCNAME);
            } catch (XPathException e) {
                throw new IllegalStateException("Local part of QName is not a valid NCName");
                // TODO: revalidation should be unnecessary
            }
        } else if (part == Component.NAMESPACE) {
            return new StringValue(uri);
        } else {
            throw new UnsupportedOperationException("Component of QName must be URI or Local Name");
        }
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type
     * @throws XPathException if the conversion is not possible
     */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        switch (requiredType) {
            case Type.ATOMIC:
            case Type.ITEM:
            case Type.QNAME:
                return this;
            case Type.STRING:
                return new StringValue(getStringValue());
            case Type.UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValue());
            default:
                DynamicError err = new DynamicError("Cannot convert QName to " +
                        StandardNames.getDisplayName(requiredType));
                err.setXPathContext(context);
                err.setErrorCode("FORG0001");
                throw err;
        }
    }

    /**
     * Return the type of the expression
     * @return Type.QNAME (always)
     */

    public ItemType getItemType() {
        return Type.QNAME_TYPE;
    }


    /**
     * Determine if two QName values are equal. This comparison ignores the prefix part
     * of the value.
     * @throws ClassCastException if they are not comparable
     * @throws IllegalStateException if the two QNames are in different name pools
     */

    public boolean equals(Object other) {
        QNameValue val = (QNameValue) other;
        return localPart.equals(val.localPart) && uri.equals(val.uri);
    }

    public int hashCode() {
        return localPart.hashCode() ^ uri.hashCode();
    }


    /**
     * Convert to Java object (for passing to external functions)
     */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(QNameValue.class)) {
            return this;
        } else if (target.isAssignableFrom(QName.class)) {
            return getQName();
        } else {
            Object o = super.convertToJava(target, config, context);
            if (o == null) {
                throw new DynamicError("Conversion of QName to " + target.getName() +
                        " is not supported");
            }
            return o;
        }
    }

    /**
     * The toString() method returns the name in the form QName("uri", "local")
     * @return the name in in the form QName("uri", "local")
     */

    public String toString() {
        return "QName(\"" + uri + "\", \"" + localPart + ')';
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

