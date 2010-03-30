package org.orbeon.saxon.xqj;

import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.dom.DOMObjectModel;
import org.orbeon.saxon.evpull.PullEventSource;
import org.orbeon.saxon.evpull.StaxToEventBridge;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.Token;
import org.orbeon.saxon.expr.JPConverter;
import org.orbeon.saxon.expr.EarlyEvaluationContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.*;
import org.orbeon.saxon.sort.IntToIntHashMap;
import org.orbeon.saxon.sort.IntToIntMap;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xquery.*;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Saxon implementation of the XQJ interface XQDataFactory. This is an abstract superclass for SaxonXQDataSource
 * and SaxonXQConnection, both of which provide the factory methods in this interface.
 * <p>
 * For Javadoc specifications of the public methods in this class, see the XQJ documentation.
 */

public abstract class SaxonXQDataFactory extends Closable implements XQDataFactory {

    private ObjectConverter objectConverter;

    abstract Configuration getConfiguration();

    // Two-way mapping between XQJ integer codes for built-in types and the Saxon equivalents

    private static IntToIntMap XQJtoSaxonTypeTranslation = new IntToIntHashMap(80);
    private static IntToIntMap saxonToXQJTypeTranslation = new IntToIntHashMap(80);

    private static void map(int x, int y) {
        XQJtoSaxonTypeTranslation.put(x, y);
        saxonToXQJTypeTranslation.put(y, x);
    }

    static {
        map(XQItemType.XQBASETYPE_ANYSIMPLETYPE, StandardNames.XS_ANY_SIMPLE_TYPE);
        map(XQItemType.XQBASETYPE_ANYTYPE, StandardNames.XS_ANY_TYPE);
        map(XQItemType.XQBASETYPE_ANYURI, StandardNames.XS_ANY_URI);
        map(XQItemType.XQBASETYPE_BASE64BINARY, StandardNames.XS_BASE64_BINARY);
        map(XQItemType.XQBASETYPE_BOOLEAN, StandardNames.XS_BOOLEAN);
        map(XQItemType.XQBASETYPE_BYTE, StandardNames.XS_BYTE);
        map(XQItemType.XQBASETYPE_DATE, StandardNames.XS_DATE);
        map(XQItemType.XQBASETYPE_DATETIME, StandardNames.XS_DATE_TIME);
        map(XQItemType.XQBASETYPE_DECIMAL, StandardNames.XS_DECIMAL);
        map(XQItemType.XQBASETYPE_DOUBLE, StandardNames.XS_DOUBLE);
        map(XQItemType.XQBASETYPE_DURATION, StandardNames.XS_DURATION);
        map(XQItemType.XQBASETYPE_ENTITIES, StandardNames.XS_ENTITIES);
        map(XQItemType.XQBASETYPE_ENTITY, StandardNames.XS_ENTITY);
        map(XQItemType.XQBASETYPE_FLOAT, StandardNames.XS_FLOAT);
        map(XQItemType.XQBASETYPE_GDAY, StandardNames.XS_G_DAY);
        map(XQItemType.XQBASETYPE_GMONTH, StandardNames.XS_G_MONTH);
        map(XQItemType.XQBASETYPE_GMONTHDAY, StandardNames.XS_G_MONTH_DAY);
        map(XQItemType.XQBASETYPE_GYEAR, StandardNames.XS_G_YEAR);
        map(XQItemType.XQBASETYPE_GYEARMONTH, StandardNames.XS_G_YEAR_MONTH);
        map(XQItemType.XQBASETYPE_HEXBINARY, StandardNames.XS_HEX_BINARY);
        map(XQItemType.XQBASETYPE_ID, StandardNames.XS_ID);
        map(XQItemType.XQBASETYPE_IDREF, StandardNames.XS_IDREF);
        map(XQItemType.XQBASETYPE_IDREFS, StandardNames.XS_IDREFS);
        map(XQItemType.XQBASETYPE_INT, StandardNames.XS_INT);
        map(XQItemType.XQBASETYPE_INTEGER, StandardNames.XS_INTEGER);
        map(XQItemType.XQBASETYPE_LANGUAGE, StandardNames.XS_LANGUAGE);
        map(XQItemType.XQBASETYPE_LONG, StandardNames.XS_LONG);
        map(XQItemType.XQBASETYPE_NAME, StandardNames.XS_NAME);
        map(XQItemType.XQBASETYPE_NCNAME, StandardNames.XS_NCNAME);
        map(XQItemType.XQBASETYPE_NEGATIVE_INTEGER, StandardNames.XS_NEGATIVE_INTEGER);
        map(XQItemType.XQBASETYPE_NMTOKEN, StandardNames.XS_NMTOKEN);
        map(XQItemType.XQBASETYPE_NMTOKENS, StandardNames.XS_NMTOKENS);
        map(XQItemType.XQBASETYPE_NONNEGATIVE_INTEGER, StandardNames.XS_NON_NEGATIVE_INTEGER);
        map(XQItemType.XQBASETYPE_NONPOSITIVE_INTEGER, StandardNames.XS_NON_POSITIVE_INTEGER);
        map(XQItemType.XQBASETYPE_NORMALIZED_STRING, StandardNames.XS_NORMALIZED_STRING);
        map(XQItemType.XQBASETYPE_NOTATION, StandardNames.XS_NOTATION);
        map(XQItemType.XQBASETYPE_POSITIVE_INTEGER, StandardNames.XS_POSITIVE_INTEGER);
        map(XQItemType.XQBASETYPE_QNAME, StandardNames.XS_QNAME);
        map(XQItemType.XQBASETYPE_SHORT, StandardNames.XS_SHORT);
        map(XQItemType.XQBASETYPE_STRING, StandardNames.XS_STRING);
        map(XQItemType.XQBASETYPE_TIME, StandardNames.XS_TIME);
        map(XQItemType.XQBASETYPE_TOKEN, StandardNames.XS_TOKEN);
        map(XQItemType.XQBASETYPE_UNSIGNED_BYTE, StandardNames.XS_UNSIGNED_BYTE);
        map(XQItemType.XQBASETYPE_UNSIGNED_INT, StandardNames.XS_UNSIGNED_INT);
        map(XQItemType.XQBASETYPE_UNSIGNED_LONG, StandardNames.XS_UNSIGNED_LONG);
        map(XQItemType.XQBASETYPE_UNSIGNED_SHORT, StandardNames.XS_UNSIGNED_SHORT);
        map(XQItemType.XQBASETYPE_ANYATOMICTYPE, StandardNames.XS_ANY_ATOMIC_TYPE);
        map(XQItemType.XQBASETYPE_DAYTIMEDURATION, StandardNames.XS_DAY_TIME_DURATION);
        map(XQItemType.XQBASETYPE_UNTYPED, StandardNames.XS_UNTYPED);
        map(XQItemType.XQBASETYPE_UNTYPEDATOMIC, StandardNames.XS_UNTYPED_ATOMIC);
        map(XQItemType.XQBASETYPE_YEARMONTHDURATION, StandardNames.XS_YEAR_MONTH_DURATION);
    }

    /**
     * Get the XQJ type code corresponding to a given Saxon type code
     * @param type the Saxon type code
     * @return the corresponding XQJ type code
     */

    static int mapSaxonTypeToXQJ(int type) {
        return saxonToXQJTypeTranslation.get(type);
    }


    protected void init() {
        objectConverter = new StandardObjectConverter(this);
    }

    /**
     * Set the ObjectConverter to be used. This allows user-defined object conversions to override
     * or supplement the standard conversions
     * @param converter the user-supplied ObjectConverter
     */

    public void setObjectConverter(ObjectConverter converter) {
        objectConverter = converter;
    }

    /**
     * Get the ObjectConverter in use. This will either be the default object converter supplied by Saxon,
     * or a user-supplied ObjectConverter if one has been set.
     * @return the ObjectConverter in use.
     */

    public ObjectConverter getObjectConverter() {
        return objectConverter;
    }

    /**
     * Create an atomic item type object representing a particular built-in atomic type
     *
     * @param baseType the built-in atomic type, typically a constant such as
     *                 XQItemType.XQBASETYPE_BOOLEAN
     * @return the corresponding XQItemType
     * @throws XQException if the supplied baseType parameter is not an atomic type
     */

    public XQItemType createAtomicType(int baseType) throws XQException {
        checkNotClosed();
        int saxonType = XQJtoSaxonTypeTranslation.get(baseType);
        if (saxonType == XQJtoSaxonTypeTranslation.getDefaultValue()) {
            throw new XQException("Unknown base type " + baseType);
        }
        SchemaType st = BuiltInType.getSchemaType(saxonType);
        if (st instanceof AtomicType) {
            return new SaxonXQItemType((AtomicType)st, getConfiguration());
        } else {
            throw new XQException("baseType " + baseType + " is not atomic");
        }
    }

    /**
     * See interface definition, and description of Saxon extensions below.
     *
     * <p>In addition to the actions described in the XQJ interface definitions, Saxon allows the
     * typename to be a name representing a Java external type. In this case the URI part of the QName
     * must be {@link NamespaceConstant#JAVA_TYPE}, and the local part of the name must be the Java class
     * name (qualified with its package name)
     * @param baseType the "baseType" (in XQJ terminology)
     * @param typename the qualified name of the type
     * @param schemaURI the location of a schema document in which the type is defined (may be null)
     * @return the item type definition
     * @throws XQException
     */


    public XQItemType createAtomicType(int baseType, QName typename, URI schemaURI) throws XQException {
        checkNotClosed();
        if (typename == null) {
            return createAtomicType(baseType);
        }
        SchemaType st = getConfiguration().getSchemaType(getFingerprint(typename));
        if (st == null) {
            loadSchema(schemaURI);
            st = getConfiguration().getSchemaType(getFingerprint(typename));
        }
        if (st == null) {
            throw new XQException("Type " + typename + " not found in schema");
        } else if (st instanceof AtomicType) {
            return new SaxonXQItemType((AtomicType)st, getConfiguration());
        } else {
            throw new XQException("Type " + typename + " is not atomic");
        }
    }


    public XQItemType createAttributeType(QName nodename, int basetype) throws XQException {
        checkNotClosed();
        Configuration config = getConfiguration();

        int saxonType = XQJtoSaxonTypeTranslation.get(basetype);
        if (saxonType == XQJtoSaxonTypeTranslation.getDefaultValue()) {
            throw new XQException("Unknown base type " + basetype);
        }
        SchemaType st = BuiltInType.getSchemaType(saxonType);
        if (!(st.isSimpleType())) {
            throw new XQException("baseType " + basetype + " is not a simple type");
        }
        ContentTypeTest contentTest = new ContentTypeTest(Type.ATTRIBUTE, st, config);
        if (nodename == null) {
            return new SaxonXQItemType(contentTest, config);
        } else {
            NameTest nameTest = new NameTest(
                Type.ATTRIBUTE, nodename.getNamespaceURI(), nodename.getLocalPart(), config.getNamePool());
            CombinedNodeTest combined = new CombinedNodeTest(nameTest, Token.INTERSECT, contentTest);
            return new SaxonXQItemType(combined, config);
        }

    }

    public XQItemType createAttributeType(QName nodename, int basetype, QName typename, URI schemaURI) throws XQException {
        checkNotClosed();
        if (typename == null) {
            return createAttributeType(nodename, basetype);
        }
        Configuration config = getConfiguration();

        SchemaType st = BuiltInType.getSchemaType(getFingerprint(typename));
        if (st == null) {
            loadSchema(schemaURI);
            st = getConfiguration().getSchemaType(getFingerprint(typename));
        }
        if (st == null) {
            throw new XQException("Type " + typename + " not found in schema");
        } else if (!(st.isSimpleType())) {
            throw new XQException("Type " + typename + " is not a simple type");
        }
        ContentTypeTest contentTest = new ContentTypeTest(Type.ATTRIBUTE, st, config);
        if (nodename == null) {
            return new SaxonXQItemType(contentTest, config);
        } else {
            NameTest nameTest = new NameTest(
                Type.ATTRIBUTE, nodename.getNamespaceURI(), nodename.getLocalPart(), config.getNamePool());
            CombinedNodeTest combined = new CombinedNodeTest(nameTest, Token.INTERSECT, contentTest);
            return new SaxonXQItemType(combined, config);
        }
    }

    public XQItemType createCommentType() throws XQException {
        checkNotClosed();
        return new SaxonXQItemType(NodeKindTest.COMMENT, getConfiguration());
    }

    public XQItemType createDocumentElementType(XQItemType elementType) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(elementType, "elementType");
        ItemType itemType = ((SaxonXQItemType)elementType).getSaxonItemType();
        if (itemType instanceof NodeTest && (((NodeTest)itemType).getNodeKindMask() & (1<<Type.ELEMENT)) != 0) {
            return new SaxonXQItemType(new DocumentNodeTest((NodeTest)itemType), getConfiguration());
        } else {
            throw new XQException("elementType is of wrong kind");
        }
    }

    public XQItemType createDocumentSchemaElementType(XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(type, "type");
        ItemType itemType = ((SaxonXQItemType)type).getSaxonItemType();
        if (itemType instanceof NodeTest && (((NodeTest)itemType).getNodeKindMask() & (1<<Type.ELEMENT)) != 0) {
            return new SaxonXQItemType(new DocumentNodeTest((NodeTest)itemType), getConfiguration());
        } else {
            throw new XQException("elementType is of wrong kind");
        }
    }

    public XQItemType createDocumentType() throws XQException {
        checkNotClosed();
        return new SaxonXQItemType(NodeKindTest.DOCUMENT, getConfiguration());
    }

    public XQItemType createElementType(QName nodename, int basetype) throws XQException {
        checkNotClosed();
        Configuration config = getConfiguration();

        if (basetype == XQItemType.XQBASETYPE_ANYTYPE) {
            if (nodename == null) {
                return new SaxonXQItemType(NodeKindTest.ELEMENT, config);
            } else {
                return new SaxonXQItemType(
                        new NameTest(Type.ELEMENT, getFingerprint(nodename), config.getNamePool()),
                        config);
            }
        }

        int saxonType = XQJtoSaxonTypeTranslation.get(basetype);
        if (saxonType == XQJtoSaxonTypeTranslation.getDefaultValue()) {
            throw new XQException("Unknown base type " + basetype);
        }
        SchemaType st = BuiltInType.getSchemaType(saxonType);
        ContentTypeTest contentTest = new ContentTypeTest(Type.ELEMENT, st, config);
        if (nodename == null) {
            return new SaxonXQItemType(contentTest, config);
        } else {
            NameTest nameTest = new NameTest(
                Type.ELEMENT, nodename.getNamespaceURI(), nodename.getLocalPart(), config.getNamePool());
            CombinedNodeTest combined = new CombinedNodeTest(nameTest, Token.INTERSECT, contentTest);
            return new SaxonXQItemType(combined, config);
        }

    }

    public XQItemType createElementType(QName nodename, int basetype, QName typename, URI schemaURI, boolean allowNill)
    throws XQException {
        checkNotClosed();
        if (typename == null) {
            return createElementType(nodename, basetype);
        }
        Configuration config = getConfiguration();

        SchemaType st = BuiltInType.getSchemaType(getFingerprint(typename));
        if (st == null) {
            loadSchema(schemaURI);
            st = getConfiguration().getSchemaType(getFingerprint(typename));
        }
        if (st == null) {
            throw new XQException("Type " + typename + " not found in schema");
        }

        ContentTypeTest contentTest = new ContentTypeTest(Type.ELEMENT, st, config);
        contentTest.setNillable(allowNill);
        if (nodename == null) {
            return new SaxonXQItemType(contentTest, config);
        } else {
            NameTest nameTest = new NameTest(
                Type.ELEMENT, nodename.getNamespaceURI(), nodename.getLocalPart(), config.getNamePool());
            CombinedNodeTest combined = new CombinedNodeTest(nameTest, Token.INTERSECT, contentTest);
            return new SaxonXQItemType(combined, config);
        }
    }

    public XQItem createItem(XQItem item) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(item, "item");
        return new SaxonXQItem(((SaxonXQItem)item).getSaxonItem(), this);
    }

    public XQItem createItemFromAtomicValue(String value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        SaxonXQDataSource.checkNotNull(type, "type");
        AtomicType at = testAtomic(type);
        StringValue sv = new StringValue(value);
        ConversionResult result = sv.convert(at, true, getConfiguration().getConversionContext());
        if (result instanceof ValidationFailure) {
            throw new XQException(((ValidationFailure)result).getMessage());
        }
        return new SaxonXQItem((AtomicValue)result, this);
    }

    public XQItem createItemFromBoolean(boolean value, XQItemType type) throws XQException {
        checkNotClosed();
        if (type == null) {
            return new SaxonXQItem(BooleanValue.get(value), this);
        } else {
            AtomicType at = testAtomic(type);
            if (at.getPrimitiveType() == StandardNames.XS_BOOLEAN) {
                try {
                    ConversionResult result =
                            BooleanValue.get(value).convert(at, true, getConfiguration().getConversionContext());
                    if (result instanceof ValidationFailure) {
                        throw new XQException(((ValidationFailure)result).getMessage());
                    }
                    return new SaxonXQItem((AtomicValue)result, this);
                } catch (Exception e) {
                    throw new XQException("Failed to convert boolean value to required type: " + e.getMessage());
                }
            } else {
                throw new XQException("Target type for a boolean must be xs:boolean or a subtype");
            }
        }
    }

    public XQItem createItemFromByte(byte value, XQItemType type) throws XQException {
        checkNotClosed();
        if (type == null) {
            try {
                return new SaxonXQItem(new Int64Value(value, BuiltInAtomicType.BYTE, false), this);
            } catch (XPathException de) {
                throw newXQException(de);
            }
        } else {
            return createItemFromLong(value, type);
        }
    }

    public XQItem createItemFromDocument(InputStream value, String baseURI, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        try {
            Source ss = new SAXSource(new InputSource(value));
            ss.setSystemId(baseURI);
            ss = augmentSource(ss, type);
            DocumentInfo doc = getConfiguration().buildDocument(ss);
            checkDocumentType(doc, (SaxonXQItemType)type);
            return new SaxonXQItem(doc, this);
        } catch (XPathException de) {
            throw newXQException(de);
        }
    }

    public XQItem createItemFromDocument(Reader value, String baseURI, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        try {
            Source ss = new SAXSource(new InputSource(value));
            ss.setSystemId(baseURI);
            ss = augmentSource(ss, type);
            DocumentInfo doc = getConfiguration().buildDocument(ss);
            checkDocumentType(doc, (SaxonXQItemType)type);
            return new SaxonXQItem(doc, this);
        } catch (XPathException de) {
            throw newXQException(de);
        }
    }

    public XQItem createItemFromDocument(Source value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        try {
            Source ss = augmentSource(value, type);
            DocumentInfo doc = getConfiguration().buildDocument(ss);
            checkDocumentType(doc, (SaxonXQItemType)type);
            return new SaxonXQItem(doc, this);
        } catch (XPathException de) {
            throw newXQException(de);
        }
    }

    public XQItem createItemFromDocument(String value, String baseURI, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        try {
            Source ss = new SAXSource(new InputSource(new StringReader(value)));
            ss.setSystemId(baseURI);
            ss = augmentSource(ss, type);
            DocumentInfo doc = getConfiguration().buildDocument(ss);
            checkDocumentType(doc, (SaxonXQItemType)type);
            return new SaxonXQItem(doc, this);
        } catch (XPathException de) {
            throw newXQException(de);
        }
    }

    public XQItem createItemFromDocument(XMLReader value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        // This method is weird. It seems the user is expected to supply an XMLReader that already
        // knows what document it is supposed to read!
        try {
            Source ss = new SAXSource(value, new InputSource("dummy"));
            ss = augmentSource(ss, type);
            AugmentedSource as = AugmentedSource.makeAugmentedSource(ss);
            as.setSourceIsXQJ(true);
            DocumentInfo doc = getConfiguration().buildDocument(as);
            checkDocumentType(doc, (SaxonXQItemType)type);
            return new SaxonXQItem(doc, this);
        } catch (XPathException de) {
            throw newXQException(de);
        }
    }

    public XQItem createItemFromDocument(XMLStreamReader value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        try {
            StaxToEventBridge bridge = new StaxToEventBridge();
            bridge.setXMLStreamReader(value);
            bridge.setPipelineConfiguration(getConfiguration().makePipelineConfiguration());
            Source ss = new PullEventSource(bridge);
            ss = augmentSource(ss, type);
            DocumentInfo doc = getConfiguration().buildDocument(ss);
            checkDocumentType(doc, (SaxonXQItemType)type);
            return new SaxonXQItem(doc, this);
        } catch (XPathException de) {
            throw newXQException(de);
        }
    }

    private Source augmentSource(Source in, XQItemType required) throws XQException {
        if (required == null) {
            return in;
        } else {
            int kind = required.getItemKind();
            switch (kind) {
                case XQItemType.XQITEMKIND_DOCUMENT:
                case XQItemType.XQITEMKIND_NODE:
                case XQItemType.XQITEMKIND_ITEM:
                    // these are not allowed according to the 0.9 spec, but that seems unreasonable
                    // no validation required
                    return in;
                case XQItemType.XQITEMKIND_DOCUMENT_ELEMENT:
                    // no validation required unless a type is specified
                    ItemType it = ((SaxonXQItemType)required).getSaxonItemType();
                    it = ((DocumentNodeTest)it).getElementTest();
                    SchemaType contentType = ((NodeTest)it).getContentType();
                    int fp = contentType.getFingerprint();
                    if (fp == StandardNames.XS_ANY_TYPE || fp == StandardNames.XS_UNTYPED) {
                        return in;
                    }
                    break;
                case XQItemType.XQITEMKIND_DOCUMENT_SCHEMA_ELEMENT:
                    break;
                default:
                    throw new XQException("Required item type for document node is incorrect");
            }
        } 
        AugmentedSource out = AugmentedSource.makeAugmentedSource(in);
        out.setSchemaValidationMode(Validation.STRICT);
        return out;
    }

    private void checkDocumentType(DocumentInfo doc, SaxonXQItemType required) throws XQException {
        checkNotClosed();
        if (required != null &&
                !required.getSaxonItemType().matchesItem(doc, false, getConfiguration())) {
            throw new XQException("Document was successfully built but has the wrong type");
        }
    }

    public XQItem createItemFromDouble(double value, XQItemType type) throws XQException {
        checkNotClosed();
        if (type == null) {
            return new SaxonXQItem(new DoubleValue(value), this);
        } else {
            AtomicType at = testAtomic(type);
            if (at.getPrimitiveType() == StandardNames.XS_DOUBLE) {
                try {
                    ConversionResult result = new DoubleValue(value).convert(at, true, getConfiguration().getConversionContext());
                    if (result instanceof ValidationFailure) {
                        throw new XQException(((ValidationFailure)result).getMessage());
                    }
                    return new SaxonXQItem((AtomicValue)result, this);
                } catch (Exception e) {
                    throw new XQException("Failed to convert double value to required type: " + e.getMessage());
                }
            } else {
                throw new XQException("Target type for a double must be xs:double or a subtype");
            }
        }
    }

    public XQItem createItemFromFloat(float value, XQItemType type) throws XQException {
        checkNotClosed();
        if (type == null) {
            return new SaxonXQItem(new FloatValue(value), this);
        } else {
            AtomicType at = testAtomic(type);
            if (at.getPrimitiveType() == StandardNames.XS_FLOAT) {
                try {
                    ConversionResult result = new FloatValue(value).convert(at, true, getConfiguration().getConversionContext());
                    if (result instanceof ValidationFailure) {
                        throw new XQException(((ValidationFailure)result).getMessage());
                    }
                    return new SaxonXQItem((AtomicValue)result, this);
                } catch (Exception e) {
                    throw new XQException("Failed to convert float value to required type: " + e.getMessage());
                }
            } else {
                throw new XQException("Target type for a float must be xs:float or a subtype");
            }
        }
    }

    public XQItem createItemFromInt(int value, XQItemType type) throws XQException {
        checkNotClosed();
        if (type == null) {
            try {
                return new SaxonXQItem(new Int64Value(value, BuiltInAtomicType.INT, false), this);
            } catch (XPathException de) {
                throw newXQException(de);
            }
        } else {
            return createItemFromLong(value, type);
        }
    }

    public XQItem createItemFromLong(long value, XQItemType type) throws XQException {
        checkNotClosed();
        if (type == null) {
            try {
                return new SaxonXQItem(new Int64Value(value, BuiltInAtomicType.LONG, false), this);
            } catch (XPathException de) {
                throw newXQException(de);
            }
        } else {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == StandardNames.XS_INTEGER || prim == StandardNames.XS_DECIMAL) {
                try {
                    ConversionResult result = Int64Value.makeIntegerValue(value).convert(at, true, getConfiguration().getConversionContext());
                    if (result instanceof ValidationFailure) {
                        throw new XQException(((ValidationFailure)result).getMessage());
                    }
                    return new SaxonXQItem((AtomicValue)result, this);
                } catch (Exception e) {
                    throw new XQException("Failed to convert long|int|short|byte value to required type: " + e.getMessage());
                }
            } else {
                throw new XQException("Target type for a long|int|short|byte must be xs:decimal or a subtype");
            }
        }
    }

    public XQItem createItemFromNode(Node value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        try {
            JPConverter jp = DOMObjectModel.getInstance().getJPConverter(Node.class);
            NodeInfo n = (NodeInfo)jp.convert(value, new EarlyEvaluationContext(getConfiguration(), null));
            //NodeInfo n = DOMObjectModel.getInstance().wrapOrUnwrapNode(value, getConfiguration());
            XQItem result = new SaxonXQItem(n, this);
            if (type != null && !result.instanceOf(type)) {
                throw new XQException("The node is not a valid instance of the required type");
            }
            return result;
        } catch (XPathException de) {
            throw newXQException(de);
        }
    }

    public XQItem createItemFromObject(Object value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        if (type == null) {
            return new SaxonXQItem(getObjectConverter().convertToItem(value), this);
        } else {
            return convertToXQItem(value, type);
        }
    }


    public XQItem createItemFromString(String value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        if (type == null) {
            return new SaxonXQItem(new StringValue(value), this);
        } else {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == StandardNames.XS_STRING) {
                try {
                    ConversionResult result = new StringValue(value).convert(at, true, getConfiguration().getConversionContext());
                    if (result instanceof ValidationFailure) {
                        throw new XQException(((ValidationFailure)result).getMessage());
                    }
                    return new SaxonXQItem((AtomicValue)result, this);
                } catch (Exception e) {
                    throw new XQException("Failed to convert string value to required type: " + e.getMessage());
                }
            } else {
                throw new XQException("Target type for a string must be xs:string or a subtype");
            }
        }
    }

    public XQItemType createItemType() throws XQException {
        checkNotClosed();
        return new SaxonXQItemType(AnyItemType.getInstance(), getConfiguration());
    }

    /**
     * Convert a Java object to an XQItem of a given type
     * @param value the supplied Java object
     * @param type the required type
     * @return an XQItem of the required type
     * @throws XQException if the value cannot be converted
     */

    private XQItem convertToXQItem(Object value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(value, "value");
        if (value instanceof Boolean) {
            return createItemFromBoolean(((Boolean) value).booleanValue(), type);
        } else if (value instanceof byte[]) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            ConversionResult result;
            if (prim == StandardNames.XS_HEX_BINARY) {
                result = new HexBinaryValue((byte[]) value).convert(at, true, getConfiguration().getConversionContext());
            } else if (prim == StandardNames.XS_BASE64_BINARY) {
                result = new Base64BinaryValue((byte[]) value).convert(at, true, getConfiguration().getConversionContext());
            } else {
                throw new XQException("Target type must be xs:hexBinary, xs:base64Binary, or a subtype");
            }
            if (result instanceof ValidationFailure) {
                throw new XQException(((ValidationFailure)result).getMessage());
            }
            return new SaxonXQItem((AtomicValue)result, this);
        } else if (value instanceof Byte) {
            return createItemFromByte(((Byte) value).byteValue(), type);
        } else if (value instanceof Float) {
            return createItemFromFloat(((Float) value).floatValue(), type);
        } else if (value instanceof Double) {
            return createItemFromDouble(((Double) value).doubleValue(), type);
        } else if (value instanceof Integer) {
            return createItemFromInt(((Integer) value).intValue(), type);
        } else if (value instanceof Long) {
            return createItemFromLong(((Long) value).longValue(), type);
        } else if (value instanceof Short) {
            return createItemFromShort(((Short) value).shortValue(), type);
        } else if (value instanceof String) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            ConversionResult result;
            if (prim == StandardNames.XS_UNTYPED_ATOMIC) {
                result = new UntypedAtomicValue((String) value);
            } else if (prim == StandardNames.XS_STRING) {
                result = new StringValue((String) value).convert(at, true, getConfiguration().getConversionContext());
            } else if (prim == StandardNames.XS_ANY_URI) {
                result = new AnyURIValue((String) value).convert(at, true, getConfiguration().getConversionContext());
            } else {
                // Note: the spec also allow NOTATION, but string->notation conversion doesn't work
                throw new XQException("Target type must be string, untypedAtomic, or anyURI");
            }
            if (result instanceof ValidationFailure) {
                throw new XQException(((ValidationFailure)result).getMessage());
            }
            return new SaxonXQItem((AtomicValue)result, this);
        } else if (value instanceof BigDecimal) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == StandardNames.XS_DECIMAL || prim == StandardNames.XS_INTEGER) {
                ConversionResult result = new DecimalValue((BigDecimal) value).convert(at, true, getConfiguration().getConversionContext());
                if (result instanceof ValidationFailure) {
                    throw new XQException(((ValidationFailure)result).getMessage());
                }
                return new SaxonXQItem((AtomicValue)result, this);
            } else {
                throw new XQException("Target type must be xs:decimal or a subtype");
            }
        } else if (value instanceof BigInteger) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == StandardNames.XS_DECIMAL || prim == StandardNames.XS_INTEGER) {
                ConversionResult result = new DecimalValue(new BigDecimal((BigInteger) value)).convert(at, true, getConfiguration().getConversionContext());
                if (result instanceof ValidationFailure) {
                    throw new XQException(((ValidationFailure)result).getMessage());
                }
                return new SaxonXQItem((AtomicValue)result, this);
            } else {
                throw new XQException("Target type must be xs:decimal or a subtype");
            }
        } else if (value instanceof Duration) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == StandardNames.XS_DURATION || prim == StandardNames.XS_DAY_TIME_DURATION || prim == StandardNames.XS_YEAR_MONTH_DURATION) {
                DurationValue dv = (DurationValue) getObjectConverter().convertToItem(value);
                ConversionResult result = dv.convert(at, true, getConfiguration().getConversionContext());
                if (result instanceof ValidationFailure) {
                    throw new XQException(((ValidationFailure)result).getMessage());
                }
                return new SaxonXQItem((AtomicValue)result, this);
            } else {
                throw new XQException("Target type must be xs:duration or a subtype");
            }

        } else if (value instanceof XMLGregorianCalendar) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            switch (prim) {
                case StandardNames.XS_DATE_TIME:
                case StandardNames.XS_DATE:
                case StandardNames.XS_TIME:
                case StandardNames.XS_G_YEAR:
                case StandardNames.XS_G_YEAR_MONTH:
                case StandardNames.XS_G_MONTH:
                case StandardNames.XS_G_MONTH_DAY:
                case StandardNames.XS_G_DAY:
                    AtomicValue dv = (AtomicValue) getObjectConverter().convertToItem(value);
                    ConversionResult result = dv.convert(at, true, getConfiguration().getConversionContext());
                    if (result instanceof ValidationFailure) {
                        throw new XQException(((ValidationFailure)result).getMessage());
                    }
                    return new SaxonXQItem((AtomicValue)result, this);
                default:
                    throw new XQException("Target type must be a date/time type");
            }
        } else if (value instanceof QName) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == StandardNames.XS_QNAME) {
                QualifiedNameValue dv = (QualifiedNameValue) getObjectConverter().convertToItem(value);
                ConversionResult result = dv.convert(at, true, getConfiguration().getConversionContext());
                if (result instanceof ValidationFailure) {
                    throw new XQException(((ValidationFailure)result).getMessage());
                }
                return new SaxonXQItem((AtomicValue)result, this);
            } else {
                throw new XQException("Target type must be xs:QName or a subtype");
            }
        } else if (value instanceof Node) {
            NodeInfo result = (NodeInfo) getObjectConverter().convertToItem(value);
            XQItem item = new SaxonXQItem(result, this);
            if (!item.instanceOf(type)) {
                throw new XQException("Supplied node does not match the requested XQItemType");
            }
            return item;
        } else {
            return new SaxonXQItem(getObjectConverter().convertToItem(value, type), this);
        }
    }


    public XQItem createItemFromShort(short value, XQItemType type) throws XQException {
        checkNotClosed();
        if (type == null) {
            try {
                return new SaxonXQItem(new Int64Value(value, BuiltInAtomicType.SHORT, false), this);
            } catch (XPathException de) {
                throw newXQException(de);
            }
        } else {
            return createItemFromLong(value, type);
        }
    }



    public XQItemType createNodeType() throws XQException {
        checkNotClosed();
        return new SaxonXQItemType(AnyNodeTest.getInstance(), getConfiguration());
    }

    public XQItemType createProcessingInstructionType(String piTarget) throws XQException {
        checkNotClosed();
        if (piTarget == null) {
            return new SaxonXQItemType(NodeKindTest.PROCESSING_INSTRUCTION, getConfiguration());
        } else {
            return new SaxonXQItemType(
                    new NameTest(Type.PROCESSING_INSTRUCTION, "", piTarget, getConfiguration().getNamePool()),
                    getConfiguration());
        }
    }

    public XQItemType createSchemaAttributeType(QName nodename, int basetype, URI schemaURI) throws XQException {
        checkNotClosed();
        Configuration config = getConfiguration();
        int fp = getFingerprint(nodename);
        SchemaDeclaration attributeDecl = config.getAttributeDeclaration(fp);
        if (attributeDecl == null && schemaURI != null) {
            loadSchema(schemaURI);
            attributeDecl = config.getAttributeDeclaration(fp);
        }
        if (attributeDecl == null) {
            throw new XQException("Attribute declaration " + nodename + " not found in schema");
        }
        NameTest nameTest = new NameTest(Type.ATTRIBUTE, fp, config.getNamePool());
        ContentTypeTest typeTest = new ContentTypeTest(Type.ATTRIBUTE, attributeDecl.getType(), config);
        CombinedNodeTest combo = new CombinedNodeTest(nameTest, Token.INTERSECT, typeTest);
        return new SaxonXQItemType(combo, config);
    }

    public XQItemType createSchemaElementType(QName nodename, int basetype, URI schemaURI) throws XQException {
        checkNotClosed();
        Configuration config = getConfiguration();
        int fp = getFingerprint(nodename);
        SchemaDeclaration elementDecl = config.getElementDeclaration(fp);
        if (elementDecl == null && schemaURI != null) {
            loadSchema(schemaURI);
            elementDecl = config.getElementDeclaration(fp);
        }
        if (elementDecl == null) {
            throw new XQException("Element declaration " + nodename + " not found in schema");
        }
        NodeTest nameTest = elementDecl.makeSchemaNodeTest();
        ContentTypeTest typeTest = new ContentTypeTest(Type.ELEMENT, elementDecl.getType(), config);
        CombinedNodeTest combo = new CombinedNodeTest(nameTest, Token.INTERSECT, typeTest);
        return new SaxonXQItemType(combo, config);

    }

    public XQSequence createSequence(Iterator i) throws XQException {
        checkNotClosed();
        if (i == null) {
            throw new XQException("createSequence(): argument is null");
        }
        List list = new ArrayList(50);
        while (i.hasNext()) {
            Object object = i.next();
            XQItem item;
            if (object instanceof XQItem) {
                item = (XQItem)object;
            } else {
                item = createItemFromObject(object, null);
            }
            list.add(((SaxonXQItem)item).getSaxonItem());
        }
        Value extent = new SequenceExtent(list);
        return new SaxonXQSequence(extent, this);
    }

    public XQSequence createSequence(XQSequence s) throws XQException {
        checkNotClosed();
        if (s instanceof SaxonXQSequence) {
            return new SaxonXQSequence(((SaxonXQSequence) s).getValue(), this);
        } else if (s instanceof SaxonXQForwardSequence) {
            try {
                Value extent = Value.asValue(
                        SequenceExtent.makeSequenceExtent(((SaxonXQForwardSequence) s).getCleanIterator()));
                return new SaxonXQSequence(extent, this);
            } catch (XPathException de) {
                throw newXQException(de);
            }
        } else {
            throw new XQException("Supplied sequence is not a Saxon implementation");
        }
    }

    public XQSequenceType createSequenceType(XQItemType item, int occurrence) throws XQException {
        checkNotClosed();
        if (item instanceof SaxonXQItemType) {
            ItemType itemType = ((SaxonXQItemType) item).getSaxonItemType();
            int cardinality;
            switch (occurrence) {
                case XQSequenceType.OCC_EXACTLY_ONE:
                    cardinality = StaticProperty.EXACTLY_ONE;
                    break;
                case XQSequenceType.OCC_ONE_OR_MORE:
                    cardinality = StaticProperty.ALLOWS_ONE_OR_MORE;
                    break;
                case XQSequenceType.OCC_ZERO_OR_ONE:
                    cardinality = StaticProperty.ALLOWS_ZERO_OR_ONE;
                    break;
                case XQSequenceType.OCC_ZERO_OR_MORE:
                    cardinality = StaticProperty.ALLOWS_ZERO_OR_MORE;
                    break;
                default:
                    throw new XQException("Invalid occurrence value");
            }
            SequenceType st = SequenceType.makeSequenceType(itemType, cardinality);
            return new SaxonXQSequenceType(st, getConfiguration());
        } else {
            throw new XQException("Supplied XQItemType is not a Saxon-created object");
        }
    }

    public XQItemType createTextType() throws XQException {
        checkNotClosed();
        return new SaxonXQItemType(NodeKindTest.TEXT, getConfiguration());
    }

    private AtomicType testAtomic(XQItemType type) throws XQException {
        if (type instanceof SaxonXQItemType) {
            AtomicType at = ((SaxonXQItemType) type).getAtomicType();
            if (at == null) {
                throw new XQException("Requested type is not atomic");
            }
            return at;
        } else {
            throw new XQException("Supplied XQItemType is not a Saxon-created object");
        }
    }

    private static XQException newXQException(Exception err) {
        XQException e = new XQException(err.getMessage());
        e.initCause(err);
        return e;
    }

    private int getFingerprint(QName name) {
        return getConfiguration().getNamePool().allocate(
                name.getPrefix(), name.getNamespaceURI(), name.getLocalPart());
    }

    /**
     * Attempt to load the schema document at a given location into the Configuration
     * @param schemaURI the absolute URI of the location of the schema document. If null is supplied,
     * the method is a no-op.
     * @throws XQException if the URI is not absolute, or if no schema is found at the location,
     * or if the schema is invalid, or if it is inconsistent with existing schema components present
     * in the Configuration.
     */

    private void loadSchema(URI schemaURI) throws XQException {
        if (schemaURI == null) {
            return;
        }
        if (!schemaURI.isAbsolute()) {
            throw new XQException("Schema URI must be an absolute URI");
        }
        try {
            getConfiguration().loadSchema(schemaURI.toString());
        } catch (SchemaException err) {
            throw newXQException(err);
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
// Contributor(s):
//