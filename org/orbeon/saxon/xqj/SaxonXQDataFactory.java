package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.dom.DOMObjectModel;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.Token;
import org.orbeon.saxon.javax.xml.xquery.*;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.pattern.*;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.sort.IntToIntHashMap;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
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

public abstract class SaxonXQDataFactory implements XQDataFactory {

    abstract Configuration getConfiguration();

    abstract XQCommonHandler getCommonHandler();

    // Two-way mapping between XQJ integer codes for built-in types and the Saxon equivalents

    private static IntToIntHashMap XQJtoSaxonTypeTranslation = new IntToIntHashMap(80);
    private static IntToIntHashMap SaxonToXQJTypeTranslation = new IntToIntHashMap(80);

    private static void map(int x, int y) {
        XQJtoSaxonTypeTranslation.put(x, y);
        SaxonToXQJTypeTranslation.put(y, x);
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
        map(XQItemType.XQBASETYPE_XDT_ANYATOMICTYPE, StandardNames.XDT_ANY_ATOMIC_TYPE);
        map(XQItemType.XQBASETYPE_XDT_DAYTIMEDURATION, StandardNames.XDT_DAY_TIME_DURATION);
        map(XQItemType.XQBASETYPE_XDT_UNTYPED, StandardNames.XDT_UNTYPED);
        map(XQItemType.XQBASETYPE_XDT_UNTYPEDATOMIC, StandardNames.XDT_UNTYPED_ATOMIC);
        map(XQItemType.XQBASETYPE_XDT_YEARMONTHDURATION, StandardNames.XDT_YEAR_MONTH_DURATION);
    }

    /**
     * Get the XQJ type code corresponding to a given Saxon type code
     * @param type the Saxon type code
     * @return the corresponding XQJ type code
     */

    static int mapSaxonTypeToXQJ(int type) {
        return SaxonToXQJTypeTranslation.get(type);
    }

    /**
     * Create an atomic item type object representing a particular built-in atomic type
     *
     * @param baseType the built-in atomic type, typically a constant such as
     *                 XQItemType.XQBASETYPE_BOOLEAN
     * @return the corresponding XQItemType
     * @throws XQException if the supplied baseType parameter is not an atomic type
     */

    public XQItemType createAtomicItemType(int baseType) throws XQException {
        int saxonType = XQJtoSaxonTypeTranslation.get(baseType);
        if (saxonType == XQJtoSaxonTypeTranslation.getDefaultValue()) {
            throw new XQException("Unknown base type " + baseType);
        }
        SchemaType st = BuiltInSchemaFactory.getSchemaType(saxonType);
        if (st instanceof AtomicType) {
            return new SaxonXQItemType((AtomicType)st, getConfiguration());
        } else {
            throw new XQException("baseType " + baseType + " is not atomic");
        }
    }

    public XQItem createItem(XQItem item) {
        return new SaxonXQItem(((SaxonXQItem) item).getItem(), ((SaxonXQItem) item).getConfiguration());
    }

    public XQItem createItemFromAtomicValue(String value, XQItemType type) throws XQException {
        if (value == null || type == null) {
            throw new XQException("The " + (value == null ? "value" : "type") + " argument must not be null");
        }
        AtomicType at = testAtomic(type);
        StringValue sv = new StringValue(value);
        return new SaxonXQItem(sv.convert(at, null, true), getConfiguration());
    }

    public XQItem createItemFromBoolean(boolean value, XQItemType type) throws XQException {
        if (type == null) {
            return new SaxonXQItem(BooleanValue.get(value), getConfiguration());
        } else {
            AtomicType at = testAtomic(type);
            if (at.getPrimitiveType() == Type.BOOLEAN) {
                try {
                    AtomicValue av = BooleanValue.get(value).convert(at, null, true);
                    return new SaxonXQItem(av, getConfiguration());
                } catch (Exception e) {
                    throw new XQException("Failed to convert boolean value to required type: " + e.getMessage());
                }
            } else {
                throw new XQException("Target type for a boolean must be xs:boolean or a subtype");
            }
        }
    }

    public XQItem createItemFromByte(byte value, XQItemType type) throws XQException {
        if (type == null) {
            try {
                return new SaxonXQItem(new IntegerValue(value, (AtomicType) BuiltInSchemaFactory.getSchemaType(StandardNames.XS_BYTE)),
                        getConfiguration());
            } catch (DynamicError de) {
                throw new XQException(de.getMessage(), de, null, null);
            }
        } else {
            return createItemFromLong(value, type);
        }
    }

    public XQItem createItemFromDocument(InputSource source) throws XQException, IOException {
        try {
            SAXSource ss = new SAXSource(source);
            DocumentInfo doc = new StaticQueryContext(getConfiguration()).buildDocument(ss);
            return new SaxonXQItem(doc, getConfiguration());
        } catch (XPathException de) {
            throw new XQException(de.getMessage(), de, null, null);
        }
    }

    public XQItem createItemFromDouble(double value, XQItemType type) throws XQException {
        if (type == null) {
            return new SaxonXQItem(new DoubleValue(value), getConfiguration());
        } else {
            AtomicType at = testAtomic(type);
            if (at.getPrimitiveType() == Type.DOUBLE) {
                try {
                    AtomicValue av = new DoubleValue(value).convert(at, null, true);
                    return new SaxonXQItem(av, getConfiguration());
                } catch (Exception e) {
                    throw new XQException("Failed to convert double value to required type: " + e.getMessage());
                }
            } else {
                throw new XQException("Target type for a double must be xs:double or a subtype");
            }
        }
    }

    public XQItem createItemFromFloat(float value, XQItemType type) throws XQException {
        if (type == null) {
            return new SaxonXQItem(new FloatValue(value), getConfiguration());
        } else {
            AtomicType at = testAtomic(type);
            if (at.getPrimitiveType() == Type.DOUBLE) {
                try {
                    AtomicValue av = new FloatValue(value).convert(at, null, true);
                    return new SaxonXQItem(av, getConfiguration());
                } catch (Exception e) {
                    throw new XQException("Failed to convert float value to required type: " + e.getMessage());
                }
            } else {
                throw new XQException("Target type for a float must be xs:float or a subtype");
            }
        }
    }

    public XQItem createItemFromInt(int value, XQItemType type) throws XQException {
        if (type == null) {
            try {
                final AtomicType intType = (AtomicType) BuiltInSchemaFactory.getSchemaType(StandardNames.XS_INT);
                return new SaxonXQItem(new IntegerValue(value, intType), getConfiguration());
            } catch (DynamicError de) {
                throw new XQException(de.getMessage(), de, null, null);
            }
        } else {
            return createItemFromLong(value, type);
        }
    }

    public XQItem createItemFromLong(long value, XQItemType type) throws XQException {
        if (type == null) {
            try {
                final AtomicType longType = (AtomicType) BuiltInSchemaFactory.getSchemaType(StandardNames.XS_LONG);
                return new SaxonXQItem(new IntegerValue(value, longType), getConfiguration());
            } catch (DynamicError de) {
                throw new XQException(de.getMessage(), de, null, null);
            }
        } else {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == Type.INTEGER || prim == Type.DECIMAL) {
                try {
                    AtomicValue av = new IntegerValue(value).convert(at, null, true);
                    return new SaxonXQItem(av, getConfiguration());
                } catch (Exception e) {
                    throw new XQException("Failed to convert long|int|short|byte value to required type: " + e.getMessage());
                }
            } else {
                throw new XQException("Target type for a long|int|short|byte must be xs:decimal or a subtype");
            }
        }
    }

    public XQItem createItemFromNode(Node value, XQItemType type) throws XQException {
        try {
            NodeInfo n = new DOMObjectModel().wrapOrUnwrapNode(value, getConfiguration());
            return new SaxonXQItem(n, getConfiguration());
        } catch (XPathException e) {
            throw new XQException("Failed to convert node: " + e.getMessage(), e, null, null);
        }
    }

    public XQItem createItemFromObject(Object value, XQItemType type) throws XQException {
        if (type == null) {
            return getCommonHandler().fromObject(value);
        } else {
            return convertToXQItem(value, type);
        }
    }


    private XQItem convertToXQItem(Object value, XQItemType type) throws XQException {
        if (value instanceof Boolean) {
            return createItemFromBoolean(((Boolean) value).booleanValue(), type);
        } else if (value instanceof byte[]) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            AtomicValue result;
            if (prim == Type.HEX_BINARY) {
                result = new HexBinaryValue((byte[]) value).convert(at, null, true);
            } else if (prim == Type.BASE64_BINARY) {
                result = new Base64BinaryValue((byte[]) value).convert(at, null, true);
                ;
            } else {
                throw new XQException("Target type must be xs:hexBinary, xs:base64Binary, or a subtype");
            }
            return new SaxonXQItem(result, getConfiguration());
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
            AtomicValue result;
            if (prim == Type.UNTYPED_ATOMIC) {
                result = new UntypedAtomicValue((String) value);
            } else if (prim == Type.STRING) {
                result = new StringValue((String) value).convert(at, null, true);
            } else if (prim == Type.ANY_URI) {
                result = new AnyURIValue((String) value).convert(at, null, true);
            } else {
                // TODO: the spec also allow NOTATION, but string->notation conversion doesn't work
                throw new XQException("Target type must be string, untypedAtomic, or anyURI");
            }
            return new SaxonXQItem(result, getConfiguration());
        } else if (value instanceof BigDecimal) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == Type.DECIMAL || prim == Type.INTEGER) {
                AtomicValue result = new DecimalValue((BigDecimal) value).convert(at, null, true);
                return new SaxonXQItem(result, getConfiguration());
            } else {
                throw new XQException("Target type must be xs:decimal or a subtype");
            }
        } else if (value instanceof BigInteger) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == Type.DECIMAL || prim == Type.INTEGER) {
                AtomicValue result = new DecimalValue(new BigDecimal((BigInteger) value)).convert(at, null, true);
                return new SaxonXQItem(result, getConfiguration());
            } else {
                throw new XQException("Target type must be xs:decimal or a subtype");
            }
        } else if (value instanceof Duration) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == Type.DURATION || prim == Type.DAY_TIME_DURATION || prim == Type.YEAR_MONTH_DURATION) {
                DurationValue dv = (DurationValue) getCommonHandler().fromObject(value);
                return new SaxonXQItem(dv.convert(at, null, true), getConfiguration());
            } else {
                throw new XQException("Target type must be xs:duration or a subtype");
            }

        } else if (value instanceof XMLGregorianCalendar) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            switch (prim) {
                case Type.DATE_TIME:
                case Type.DATE:
                case Type.TIME:
                case Type.G_YEAR:
                case Type.G_YEAR_MONTH:
                case Type.G_MONTH:
                case Type.G_MONTH_DAY:
                case Type.G_DAY:
                    AtomicValue dv = (AtomicValue) getCommonHandler().fromObject(value);
                    return new SaxonXQItem(dv.convert(at, null, true), getConfiguration());
                default:
                    throw new XQException("Target type must be a date/time type");
            }
        } else if (value instanceof QName) {
            AtomicType at = testAtomic(type);
            int prim = at.getPrimitiveType();
            if (prim == Type.QNAME) {
                QNameValue dv = (QNameValue) getCommonHandler().fromObject(value);
                return new SaxonXQItem(dv.convert(at, null, true), getConfiguration());
            } else {
                throw new XQException("Target type must be xs:QName or a subtype");
            }
        } else if (value instanceof Node) {
            // TODO: check that the right type of node was requested in the type parameter
            NodeInfo result = (NodeInfo) getCommonHandler().fromObject(value);
            return new SaxonXQItem(result, getConfiguration());
        } else {
            throw new XQException("Java object cannot be converted to an XQuery value");
        }
    }


    public XQItem createItemFromShort(short value, XQItemType type) throws XQException {
        if (type == null) {
            try {
                final AtomicType shortType = (AtomicType) BuiltInSchemaFactory.getSchemaType(StandardNames.XS_SHORT);
                return new SaxonXQItem(new IntegerValue(value, shortType), getConfiguration());
            } catch (DynamicError de) {
                throw new XQException(de.getMessage(), de, null, null);
            }
        } else {
            return createItemFromLong(value, type);
        }
    }

    public XQItemType createItemType(int itemkind, int basetype, QName nodename) throws XQException {
        return createItemType(itemkind, basetype, nodename, null, null, false);
    }

    public XQItemType createItemType(int itemkind, int basetype, QName nodename,
                                     QName typename, URI schemaURI, boolean nillable) throws XQException {
        Configuration config = getConfiguration();
        switch (itemkind) {
            case XQItemType.XQITEMKIND_ITEM:
                return new SaxonXQItemType(AnyItemType.getInstance(), config);
            case XQItemType.XQITEMKIND_ATOMIC:
                if (typename == null) {
                    int saxonType = XQJtoSaxonTypeTranslation.get(basetype);
                    SchemaType st = BuiltInSchemaFactory.getSchemaType(saxonType);
                    if (st instanceof AtomicType) {
                        return new SaxonXQItemType((AtomicType) st, config);
                    } else {
                        throw new XQException("itemkind is atomic, but basetype is not");
                    }
                } else {
                    NamePool pool = config.getNamePool();
                    int typeCode = pool.allocate(typename.getPrefix(),
                            typename.getNamespaceURI(),
                            typename.getLocalPart());
                    SchemaType schemaType = config.getSchemaType(typeCode & NamePool.FP_MASK);
                    if (schemaType instanceof AtomicType) {
                        return new SaxonXQItemType((AtomicType) schemaType, config);
                    } else {
                        throw new XQException("Type " + typename + " is not a known atomic type");
                    }
                }
            case XQItemType.XQITEMKIND_NODE:
                return new SaxonXQItemType(AnyNodeTest.getInstance(), config);
            case XQItemType.XQITEMKIND_DOCUMENT:
                return new SaxonXQItemType(NodeKindTest.DOCUMENT, config);
            case XQItemType.XQITEMKIND_COMMENT:
                return new SaxonXQItemType(NodeKindTest.COMMENT, config);
            case XQItemType.XQITEMKIND_PI:
                return new SaxonXQItemType(NodeKindTest.PROCESSING_INSTRUCTION, config);
            case XQItemType.XQITEMKIND_TEXT:
                return new SaxonXQItemType(NodeKindTest.TEXT, config);
            case XQItemType.XQITEMKIND_DOCUMENT_ELEMENT:
            case XQItemType.XQITEMKIND_ELEMENT:
                {
                    NodeTest elementTest;
                    if (nodename == null) {
                        elementTest = NodeKindTest.ELEMENT;
                    } else if (typename == null) {
                        NamePool pool = config.getNamePool();
                        int nameCode = pool.allocate(nodename.getPrefix(),
                                nodename.getNamespaceURI(),
                                nodename.getLocalPart());
                        elementTest = new NameTest(Type.ELEMENT, nameCode, pool);
                    } else {
                        NamePool pool = config.getNamePool();
                        int nameCode = -1;
                        if (nodename != null) {
                            nameCode = pool.allocate(nodename.getPrefix(),
                                    nodename.getNamespaceURI(),
                                    nodename.getLocalPart());
                        }
                        int typeCode = pool.allocate(typename.getPrefix(),
                                typename.getNamespaceURI(),
                                typename.getLocalPart());
                        SchemaType schemaType = config.getSchemaType(typeCode & NamePool.FP_MASK);
                        if (schemaType == null) {
                            throw new XQException("Unknown schema type " + typename);
                        }
                        ContentTypeTest typeTest = new ContentTypeTest(Type.ELEMENT, schemaType, config);
                        typeTest.setNillable(nillable);
                        NodeTest nameTest = new NameTest(Type.ELEMENT, nameCode, pool);
                        elementTest = new CombinedNodeTest(nameTest, Token.INTERSECT, typeTest);
                    }
                    if (itemkind == XQItemType.XQITEMKIND_ELEMENT) {
                        return new SaxonXQItemType(elementTest, config);
                    } else {
                        return new SaxonXQItemType(new DocumentNodeTest(elementTest), config);
                    }
                }
            case XQItemType.XQITEMKIND_ATTRIBUTE:
                {
                    if (nodename == null) {
                        return new SaxonXQItemType(NodeKindTest.ATTRIBUTE, config);
                    } else if (typename == null) {
                        NamePool pool = config.getNamePool();
                        int nameCode = pool.allocate(nodename.getPrefix(),
                                nodename.getNamespaceURI(),
                                nodename.getLocalPart());
                        return new SaxonXQItemType(new NameTest(Type.ATTRIBUTE, nameCode, pool), config);
                    } else {
                        NamePool pool = config.getNamePool();
                        int nameCode = -1;
                        if (nodename != null) {
                            nameCode = pool.allocate(nodename.getPrefix(),
                                    nodename.getNamespaceURI(),
                                    nodename.getLocalPart());
                        }
                        int typeCode = pool.allocate(typename.getPrefix(),
                                typename.getNamespaceURI(),
                                typename.getLocalPart());
                        SchemaType schemaType = config.getSchemaType(typeCode & NamePool.FP_MASK);
                        if (schemaType == null) {
                            throw new XQException("Unknown schema type " + typename);
                        }
                        ContentTypeTest typeTest = new ContentTypeTest(Type.ATTRIBUTE, schemaType, config);
                        if (nillable) {
                            throw new XQException("An attribute test cannot be nillable");
                        }
                        typeTest.setNillable(false);
                        NodeTest nameTest = new NameTest(Type.ATTRIBUTE, nameCode, pool);
                        ItemType result = new CombinedNodeTest(nameTest, Token.INTERSECT, typeTest);
                        return new SaxonXQItemType(result, config);
                    }
                }
            default:
                throw new XQException("Unknown itemkind " + itemkind);
        }

    }

    public XQSequence createSequence(Iterator i) throws XQException {
        List list = new ArrayList(50);
        while (i.hasNext()) {
            Object object = i.next();
            XQItem item = createItemFromObject(object, null);
            list.add(((SaxonXQItem)item).getItem());
        }
        Value extent = new SequenceExtent(list);
        return new SaxonXQSequence(extent, getConfiguration());
    }

    public XQSequence createSequence(XQSequence s) throws XQException {
        if (s instanceof SaxonXQSequence) {
            return new SaxonXQSequence(((SaxonXQSequence) s).getValue(), getConfiguration());
        } else if (s instanceof SaxonXQForwardSequence) {
            try {
                Value extent = Value.asValue(
                        SequenceExtent.makeSequenceExtent(((SaxonXQForwardSequence) s).getCleanIterator()));
                return new SaxonXQSequence(extent, getConfiguration());
            } catch (XPathException e) {
                throw new XQException(e.getMessage(), e, null, null);
            }
        } else {
            throw new XQException("Supplied sequence is not a Saxon implementation");
        }
    }

    public XQSequenceType createSequenceType(XQItemType item, int occurrence) throws XQException {
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