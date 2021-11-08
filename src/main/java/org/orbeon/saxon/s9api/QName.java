package org.orbeon.saxon.s9api;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;

/**
 * The QName class represents an instance of xs:QName, as defined in the XPath 2.0 data model.
 * Internally, it has three components, a namespace URI, a local name, and a prefix. The prefix
 * is intended to be used only when converting the value back to a string.
 *
 * <p>This class also defines a number of QName-valued constants relating to built-in types in
 * XML Schema</p>
 *
 * <p>A QName is immutable.</p>
 *
 * <p>Note that a QName is not itself an {@link XdmItem} in this model; however it can be wrapped in an
 * XdmItem.</p>
 */
public class QName {

    private StructuredQName sqName;

    /**
     * Construct a QName using a namespace prefix, a namespace URI, and a local name (in that order).
     * <p>This constructor does not check that the components of the QName are lexically valid.</p>
     *
     * @param prefix    The prefix of the name. Use either the string "" or null for names that have
     *                  no prefix (that is, they are in the default namespace)
     * @param uri       The namespace URI. Use either the string "" or null for names that are not in any namespace.
     * @param localName The local part of the name
     */

    public QName(String prefix, String uri, String localName) {
        sqName = new StructuredQName(prefix, uri, localName);
    }

    /**
     * Construct a QName using a namespace URI and a lexical representation. The lexical representation
     * may be a local name on its own, or it may be in the form prefix:local-name.
     * <p>This constructor does not check that the components of the QName are lexically valid.</p>
     *
     * @param uri     The namespace URI. Use either the string "" or null for names that are not in any namespace.
     * @param lexical Either the local part of the name, or the prefix and local part in the format prefix:local
     */

    public QName(String uri, String lexical) {
        uri = (uri == null ? "" : uri);
        int colon = lexical.indexOf(':');
        if (colon < 0) {
            sqName = new StructuredQName("", uri, lexical);
        } else {
            String prefix = lexical.substring(0, colon);
            String local = lexical.substring(colon + 1);
            sqName = new StructuredQName(prefix, uri, local);
        }
    }

    /**
     * Construct a QName from a localName alone. The localName must not contain a colon.
     * The resulting QName represents a name in no namespace (which therefore has no prefix)</p>
     *
     * @param localName The local name. This must be a valid NCName, in particular it must contain no colon
     */

    public QName(String localName) {
        int colon = localName.indexOf(':');
        if (colon < 0) {
            sqName = new StructuredQName("", "", localName);
        } else {
            throw new IllegalArgumentException("Local name contains a colon");
        }
    }

    /**
     * Construct a QName from a lexical QName, supplying an element node whose
     * in-scope namespaces are to be used to resolve any prefix contained in the QName.
     *
     * <p>This constructor checks that the components of the QName are
     * lexically valid.</p>
     * <p>If the lexical QName has no prefix, the name is considered to be in the
     * default namespace, as defined by <code>xmlns="..."</code>.</p>
     * <p>If the prefix of the lexical QName is not in scope, returns null.</p>
     *
     * @param lexicalQName The lexical QName, in the form <code>prefix:local</code>
     *                     or simply <code>local</code>.
     * @param element      The element node whose in-scope namespaces are to be used
     *                     to resolve the prefix part of the lexical QName.
     * @throws IllegalArgumentException If the prefix of the lexical QName is not in scope
     * or if the lexical QName is invalid (for example, if it contains invalid characters)
     */

    public QName(String lexicalQName, XdmNode element) {
        try {
            NodeInfo node = (NodeInfo) element.getUnderlyingValue();
            sqName = StructuredQName.fromLexicalQName(lexicalQName, true,
                    node.getConfiguration().getNameChecker(), new InscopeNamespaceResolver(node));

        } catch (XPathException err) {
            throw new IllegalArgumentException(err);
        }
    }

    /**
     * Construct a QName from a JAXP QName object
     *
     * @param qName the JAXP QName object
     */

    public QName(javax.xml.namespace.QName qName) {
        sqName = new StructuredQName(qName.getPrefix(), qName.getNamespaceURI(), qName.getLocalPart());
    }

    /**
     * Protected constructor accepting a StructuredQName
     * @param sqName the StructuredQName
     */

    protected QName(StructuredQName sqName) {
        this.sqName = sqName;
    }

    /**
     * Factory method to construct a QName from a string containing the expanded
     * QName in Clark notation, that is, <code>{uri}local</code>
     * <p/>
     * The prefix part of the <code>QName</code> will be set to an empty string.
     * </p>
     *
     * @param expandedName      The URI in Clark notation: <code>{uri}local</code> if the
     *                          name is in a namespace, or simply <code>local</code> if not.
     * @return the QName corresponding to the supplied name in Clark notation. This will always
     * have an empty prefix.
     */

    public static QName fromClarkName(String expandedName) {
        String namespaceURI;
        String localName;
        if (expandedName.charAt(0) == '{') {
            int closeBrace = expandedName.indexOf('}');
            if (closeBrace < 0) {
                throw new IllegalArgumentException("No closing '}' in Clark name");
            }
            namespaceURI = expandedName.substring(1, closeBrace);
            if (closeBrace == expandedName.length()) {
                throw new IllegalArgumentException("Missing local part in Clark name");
            }
            localName = expandedName.substring(closeBrace + 1);
        } else {
            namespaceURI = "";
            localName = expandedName;
        }

        return new QName("", namespaceURI, localName);
    }

    /**
     * Validate the QName against the XML 1.0 or XML 1.1 rules for valid names.
     *
     * @param processor  The Processor in which the name is to be validated.
     *                   This determines whether the XML 1.0 or XML 1.1 rules for forming names are used.
     * @return true if the name is valid, false if not
     */

    public boolean isValid(Processor processor) {
        NameChecker nc = processor.getUnderlyingConfiguration().getNameChecker();
        String prefix = getPrefix();
        if (prefix.length() > 0) {
            if (!nc.isValidNCName(prefix)) {
                return false;
            }
        }
        return nc.isValidNCName(getLocalName());
    }

    /**
     * Get the prefix of the QName. This plays no role in operations such as comparison
     * of QNames for equality, but is retained (as specified in XPath) so that a string representation
     * can be reconstructed.
     * <p/>
     * Returns the zero-length string in the case of a QName that has no prefix.
     * </p>
     * @return the prefix part of the QName, or "" if the name has no known prefix
     */

    public String getPrefix() {
        return sqName.getPrefix();
    }

    /**
     * The namespace URI of the QName. Returns "" (the zero-length string) if the
     * QName is not in a namespace.
     * @return the namespace part of the QName, or "" for a name in no namespace
     */

    public String getNamespaceURI() {
        return sqName.getNamespaceURI();
    }

    /**
     * The local part of the QName
     * @return the local part of the QName
     */

    public String getLocalName() {
        return sqName.getLocalName();
    }

    /**
     * The expanded name, as a string using the notation devised by James Clark.
     * If the name is in a namespace, the resulting string takes the form <code>{uri}local</code>.
     * Otherwise, the value is the local part of the name.
     * @return the name in Clark notation. If the name is not in a namespace, returns the
     * local part of the name. Otherwise returns the concatenation of "{", the namespace part
     * of the QName, "}", and the local part of the QName.
     */

    public String getClarkName() {
        String uri = getNamespaceURI();
        if (uri.length() == 0) {
            return getLocalName();
        } else {
            return "{" + uri + "}" + getLocalName();
        }
    }

    /**
     * Convert the value to a string. The resulting string is the lexical form of the QName,
     * using the original prefix if there was one.
     */

    public String toString() {
        return sqName.getDisplayName();
    }

    /**
     * Get a hash code for the QName, to support equality matching. This supports the
     * semantics of equality, which considers only the namespace URI and local name, and
     * not the prefix.
     * @return a hashCode for the QName
     */

    public int hashCode() {
        return sqName.hashCode();
    }

    /**
     * Test whether two QNames are equal. This supports the
     * semantics of equality, which considers only the namespace URI and local name, and
     * not the prefix.
     * @return true if the namespace URIs are equal and the local parts are equal, when
     * compared character-by-character.
     */

    public boolean equals(Object other) {
        return other instanceof QName && sqName.equals(((QName)other).sqName);
    }

    /**
     * Get the underlying StructuredQName
     * @return the underlying StructuredQName
     */

    protected StructuredQName getStructuredQName() {
        return sqName;
    }

    /** QName denoting the schema type xs:string **/
    public static final QName XS_STRING =
            new QName("xs", NamespaceConstant.SCHEMA, "string");
    /** QName denoting the schema type xs:boolean **/
    public static final QName XS_BOOLEAN =
            new QName("xs", NamespaceConstant.SCHEMA, "boolean");
    /** QName denoting the schema type xs:decimal **/
    public static final QName XS_DECIMAL =
            new QName("xs", NamespaceConstant.SCHEMA, "decimal");
    /** QName denoting the schema type xs:float **/
    public static final QName XS_FLOAT =
            new QName("xs", NamespaceConstant.SCHEMA, "float");
    /** QName denoting the schema type xs:double **/
    public static final QName XS_DOUBLE =
            new QName("xs", NamespaceConstant.SCHEMA, "double");
    /** QName denoting the schema type xs:duration **/
    public static final QName XS_DURATION =
            new QName("xs", NamespaceConstant.SCHEMA, "duration");
    /** QName denoting the schema type xs:dateTime **/
    public static final QName XS_DATE_TIME =
            new QName("xs", NamespaceConstant.SCHEMA, "dateTime");
    /** QName denoting the schema type xs:time **/
    public static final QName XS_TIME =
            new QName("xs", NamespaceConstant.SCHEMA, "time");
    /** QName denoting the schema type xs:date **/
    public static final QName XS_DATE =
            new QName("xs", NamespaceConstant.SCHEMA, "date");
    /** QName denoting the schema type xs:gYearMonth **/
    public static final QName XS_G_YEAR_MONTH =
            new QName("xs", NamespaceConstant.SCHEMA, "gYearMonth");
    /** QName denoting the schema type xs:gYear **/
    public static final QName XS_G_YEAR =
            new QName("xs", NamespaceConstant.SCHEMA, "gYear");
    /** QName denoting the schema type xs:gMonthDay **/
    public static final QName XS_G_MONTH_DAY =
            new QName("xs", NamespaceConstant.SCHEMA, "gMonthDay");
    /** QName denoting the schema type xs:gDay **/
    public static final QName XS_G_DAY =
            new QName("xs", NamespaceConstant.SCHEMA, "gDay");
    /** QName denoting the schema type xs:gMonth **/
    public static final QName XS_G_MONTH =
            new QName("xs", NamespaceConstant.SCHEMA, "gMonth");
    /** QName denoting the schema type xs:hexBinary **/
    public static final QName XS_HEX_BINARY =
            new QName("xs", NamespaceConstant.SCHEMA, "hexBinary");
    /** QName denoting the schema type xs:base64Binary **/
    public static final QName XS_BASE64_BINARY   =
            new QName("xs", NamespaceConstant.SCHEMA, "base64Binary");
    /** QName denoting the schema type xs:anyURI **/
    public static final QName XS_ANY_URI =
            new QName("xs", NamespaceConstant.SCHEMA, "anyURI");
    /** QName denoting the schema type xs:QName **/
    public static final QName XS_QNAME =
            new QName("xs", NamespaceConstant.SCHEMA, "QName");
    /** QName denoting the schema type xs:NOTATION **/
    public static final QName XS_NOTATION =
            new QName("xs", NamespaceConstant.SCHEMA, "NOTATION");
    /** QName denoting the schema type xs:integer **/
    public static final QName XS_INTEGER =
            new QName("xs", NamespaceConstant.SCHEMA, "integer");
    /** QName denoting the schema type xs:nonPositiveInteger **/
    public static final QName XS_NON_POSITIVE_INTEGER =
            new QName("xs", NamespaceConstant.SCHEMA, "nonPositiveInteger");
    /** QName denoting the schema type xs:negativeInteger **/
    public static final QName XS_NEGATIVE_INTEGER =
            new QName("xs", NamespaceConstant.SCHEMA, "negativeInteger");
    /** QName denoting the schema type xs:long **/
    public static final QName XS_LONG =
            new QName("xs", NamespaceConstant.SCHEMA, "long");
    /** QName denoting the schema type xs:int **/
    public static final QName XS_INT =
            new QName("xs", NamespaceConstant.SCHEMA, "int");
    /** QName denoting the schema type xs:short **/
    public static final QName XS_SHORT =
            new QName("xs", NamespaceConstant.SCHEMA, "short");
    /** QName denoting the schema type xs:byte **/
    public static final QName XS_BYTE =
            new QName("xs", NamespaceConstant.SCHEMA, "byte");
    /** QName denoting the schema type xs:nonNegativeInteger **/
    public static final QName XS_NON_NEGATIVE_INTEGER =
            new QName("xs", NamespaceConstant.SCHEMA, "nonNegativeInteger");
    /** QName denoting the schema type xs:positiveInteger **/
    public static final QName XS_POSITIVE_INTEGER =
            new QName("xs", NamespaceConstant.SCHEMA, "positiveInteger");
    /** QName denoting the schema type xs:unsignedLong **/
    public static final QName XS_UNSIGNED_LONG =
            new QName("xs", NamespaceConstant.SCHEMA, "unsignedLong");
    /** QName denoting the schema type xs:unsignedInt **/
    public static final QName XS_UNSIGNED_INT =
            new QName("xs", NamespaceConstant.SCHEMA, "unsignedInt");
    /** QName denoting the schema type xs:unsignedShort **/
    public static final QName XS_UNSIGNED_SHORT =
            new QName("xs", NamespaceConstant.SCHEMA, "unsignedShort");
    /** QName denoting the schema type xs:unsignedByte **/
    public static final QName XS_UNSIGNED_BYTE =
            new QName("xs", NamespaceConstant.SCHEMA, "unsignedByte");
    /** QName denoting the schema type xs:normalizedString **/
    public static final QName XS_NORMALIZED_STRING =
            new QName("xs", NamespaceConstant.SCHEMA, "normalizedString");
    /** QName denoting the schema type xs:token **/
    public static final QName XS_TOKEN =
            new QName("xs", NamespaceConstant.SCHEMA, "token");
    /** QName denoting the schema type xs:language **/
    public static final QName XS_LANGUAGE =
            new QName("xs", NamespaceConstant.SCHEMA, "language");
    /** QName denoting the schema type xs:NMTOKEN **/
    public static final QName XS_NMTOKEN =
            new QName("xs", NamespaceConstant.SCHEMA, "NMTOKEN");
    /** QName denoting the schema type xs:NMTOKENS **/
    public static final QName XS_NMTOKENS =
            new QName("xs", NamespaceConstant.SCHEMA, "NMTOKENS");
    /** QName denoting the schema type xs:Name **/
    public static final QName XS_NAME =
            new QName("xs", NamespaceConstant.SCHEMA, "Name");
    /** QName denoting the schema type xs:NCName **/
    public static final QName XS_NCNAME =
            new QName("xs", NamespaceConstant.SCHEMA, "NCName");
    /** QName denoting the schema type xs:ID **/
    public static final QName XS_ID =
            new QName("xs", NamespaceConstant.SCHEMA, "ID");
    /** QName denoting the schema type xs:IDREF **/
    public static final QName XS_IDREF =
            new QName("xs", NamespaceConstant.SCHEMA, "IDREF");
    /** QName denoting the schema type xs:IDREFS **/
    public static final QName XS_IDREFS =
            new QName("xs", NamespaceConstant.SCHEMA, "IDREFS");
    /** QName denoting the schema type xs:ENTITY **/
    public static final QName XS_ENTITY =
            new QName("xs", NamespaceConstant.SCHEMA, "ENTITY");
    /** QName denoting the schema type xs:ENTITIES **/
    public static final QName XS_ENTITIES =
            new QName("xs", NamespaceConstant.SCHEMA, "ENTITIES");
    /** QName denoting the schema type xs:untyped **/
    public static final QName XS_UNTYPED =
            new QName("xs", NamespaceConstant.SCHEMA, "untyped");
    /** QName denoting the schema type xs:untypedAtomic **/
    public static final QName XS_UNTYPED_ATOMIC =
            new QName("xs", NamespaceConstant.SCHEMA, "untypedAtomic");
    /** QName denoting the schema type xs:anyAtomicType **/
    public static final QName XS_ANY_ATOMIC_TYPE =
            new QName("xs", NamespaceConstant.SCHEMA, "anyAtomicType");
    /** QName denoting the schema type xs:yearMonthDuration **/
    public static final QName XS_YEAR_MONTH_DURATION =
            new QName("xs", NamespaceConstant.SCHEMA, "yearMonthDuration");
    /** QName denoting the schema type xs:dayTimeDuration **/
    public static final QName XS_DAY_TIME_DURATION =
            new QName("xs", NamespaceConstant.SCHEMA, "dayTimeDuration");

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

