package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.JPConverter;
import org.orbeon.saxon.expr.EarlyEvaluationContext;
import org.orbeon.saxon.dom.DOMObjectModel;
import org.orbeon.saxon.dom.NodeOverNodeInfo;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.evpull.PullEventSource;
import org.orbeon.saxon.evpull.StaxToEventBridge;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ExternalObjectType;
import org.orbeon.saxon.value.*;
import org.w3c.dom.Node;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItemAccessor;
import javax.xml.xquery.XQItemType;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This class provides all the conversion methods used to convert data between XDM values
 * and Java values in the XQJ API. At one time the XQJ specification defined such a class,
 * and it has been retained in the Saxon implementation.
 * <p>
 * This handler implements the mappings defined in the XQJ specification. In addition,
 * it defines the following mappings, which are applied after those defined in XQJ:</p>
 *
 * <p>For fromObject:</p>
 * <ul>
 * <li>If the supplied object is an instance of javax.xml.transform.Source, a document
 * node is constructed from the source and the resulting node is returned as the Item</li>
 * <li>If the supplied object is an instance of javax.xml.stream.XMLStreamReader, a document
 * node is constructed from the XMLStreamReader and the resulting node is returned as the Item</li>
 * <li>If the supplied object is
 * </ul>
 */
public class StandardObjectConverter implements ObjectConverter {

    SaxonXQDataFactory dataFactory;
    Configuration config;

    /**
     * Create an instance of the class
     * @param factory the factory object
     */

    public StandardObjectConverter(SaxonXQDataFactory factory) {
        dataFactory = factory;
        config = factory.getConfiguration();
    }

    //@SuppressWarnings({"ConstantConditions"})
    public Object toObject(XQItemAccessor xqItemAccessor) throws XQException {
        Item item = ((SaxonXQItemAccessor)xqItemAccessor).getSaxonItem();
        if (item instanceof AtomicValue) {
            AtomicValue p = ((AtomicValue)item);
            int t = p.getItemType(config.getTypeHierarchy()).getPrimitiveType();
            switch (t) {
                case StandardNames.XS_ANY_URI:
                    return p.getStringValue();
                case StandardNames.XS_BASE64_BINARY:
                    return ((Base64BinaryValue)p).getBinaryValue();
                case StandardNames.XS_BOOLEAN:
                    return Boolean.valueOf(((BooleanValue)p).getBooleanValue());
                case StandardNames.XS_DATE:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case StandardNames.XS_DATE_TIME:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case StandardNames.XS_DECIMAL:
                    return ((DecimalValue)p).getDecimalValue();
                case StandardNames.XS_DOUBLE:
                    return new Double(((DoubleValue)p).getDoubleValue());
                case StandardNames.XS_DURATION:
                    return new SaxonDuration((DurationValue)p);
                case StandardNames.XS_FLOAT:
                    return new Float(((FloatValue)p).getFloatValue());
                case StandardNames.XS_G_DAY:
                case StandardNames.XS_G_MONTH:
                case StandardNames.XS_G_MONTH_DAY:
                case StandardNames.XS_G_YEAR:
                case StandardNames.XS_G_YEAR_MONTH:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case StandardNames.XS_HEX_BINARY:
                    return ((HexBinaryValue)p).getBinaryValue();
                case StandardNames.XS_INTEGER:
                    if (p instanceof BigIntegerValue) {
                        return ((BigIntegerValue)p).asBigInteger();
                    } else {
                        int sub = ((AtomicType)p.getItemType(null)).getFingerprint();
                        switch (sub) {
                            case StandardNames.XS_INTEGER:
                            case StandardNames.XS_NEGATIVE_INTEGER:
                            case StandardNames.XS_NON_NEGATIVE_INTEGER:
                            case StandardNames.XS_NON_POSITIVE_INTEGER:
                            case StandardNames.XS_POSITIVE_INTEGER:
                            case StandardNames.XS_UNSIGNED_LONG:
                                return BigInteger.valueOf(((Int64Value)p).longValue());
                            case StandardNames.XS_BYTE:
                                return new Byte((byte)((Int64Value)p).longValue());
                            case StandardNames.XS_INT:
                            case StandardNames.XS_UNSIGNED_SHORT:
                                return new Integer((int)((Int64Value)p).longValue());
                            case StandardNames.XS_LONG:
                            case StandardNames.XS_UNSIGNED_INT:
                                return new Long(((Int64Value)p).longValue());
                            case StandardNames.XS_SHORT:
                            case StandardNames.XS_UNSIGNED_BYTE:
                                return new Short((short)((Int64Value)p).longValue());
                            default:
                                throw new XQException("Unrecognized integer subtype " + sub);
                        }
                    }
                case StandardNames.XS_QNAME:
                    return ((QualifiedNameValue)p).makeQName(config);
                case StandardNames.XS_STRING:
                case StandardNames.XS_UNTYPED_ATOMIC:
                    return p.getStringValue();
                case StandardNames.XS_TIME:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case StandardNames.XS_DAY_TIME_DURATION:
                    return new SaxonDuration((DurationValue)p);
                case StandardNames.XS_YEAR_MONTH_DURATION:
                    return new SaxonDuration((DurationValue)p);
                default:
                    throw new XQException("unsupported type");
            }
        } else {
            return NodeOverNodeInfo.wrap((NodeInfo)item);
        }
    }

    /**
     * Convert a Java object to a Saxon Item
     * @param value the Java object
     * @return the corresponding Item
     * @throws XQException
     */

    public Item convertToItem(Object value) throws XQException {
        try {
            if (value instanceof Boolean) {
                return BooleanValue.get(((Boolean)value).booleanValue());
            } else if (value instanceof byte[]) {
                return new HexBinaryValue((byte[])value);
            } else if (value instanceof Byte) {
                return new Int64Value(((Byte)value).byteValue(), BuiltInAtomicType.BYTE, false);
            } else if (value instanceof Float) {
                return new FloatValue(((Float)value).floatValue());
            } else if (value instanceof Double) {
                return new DoubleValue(((Double)value).doubleValue());
            } else if (value instanceof Integer) {
                return new Int64Value(((Integer)value).intValue(), BuiltInAtomicType.INT, false);
            } else if (value instanceof Long) {
                return new Int64Value(((Long)value).longValue(), BuiltInAtomicType.LONG, false);
            } else if (value instanceof Short) {
                return new Int64Value(((Short)value).shortValue(), BuiltInAtomicType.SHORT, false);
            } else if (value instanceof String) {
                return new StringValue((String)value);
            } else if (value instanceof BigDecimal) {
                return new DecimalValue((BigDecimal)value);
            } else if (value instanceof BigInteger) {
                return new BigIntegerValue((BigInteger)value);
            } else if (value instanceof SaxonDuration) {
                return ((SaxonDuration)value).getDurationValue();
            } else if (value instanceof Duration) {
                // this is simpler and safer (but perhaps slower) than extracting all the components
                return DurationValue.makeDuration(value.toString()).asAtomic();
            } else if (value instanceof SaxonXMLGregorianCalendar) {
                return ((SaxonXMLGregorianCalendar)value).toCalendarValue();
            } else if (value instanceof XMLGregorianCalendar) {
                XMLGregorianCalendar g = (XMLGregorianCalendar)value;
                QName gtype = g.getXMLSchemaType();
                if (gtype.equals(DatatypeConstants.DATETIME)) {
                    return DateTimeValue.makeDateTimeValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.DATE)) {
                    return DateValue.makeDateValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.TIME)) {
                    return TimeValue.makeTimeValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.GYEAR)) {
                    return GYearValue.makeGYearValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.GYEARMONTH)) {
                    return GYearMonthValue.makeGYearMonthValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.GMONTH)) {
                    return GMonthValue.makeGMonthValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.GMONTHDAY)) {
                    return GMonthDayValue.makeGMonthDayValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.GDAY)) {
                    return GDayValue.makeGDayValue(gtype.toString()).asAtomic();
                } else {
                    throw new AssertionError("Unknown Gregorian date type");
                }
            } else if (value instanceof QName) {
                QName q = (QName)value;
                return new QNameValue(q.getPrefix(), q.getNamespaceURI(), q.getLocalPart(),
                        BuiltInAtomicType.QNAME, null);
            } else if (value instanceof Node) {
                JPConverter jp = DOMObjectModel.getInstance().getJPConverter(Node.class);
                return Value.asItem(jp.convert(value, new EarlyEvaluationContext(config, null)));
                //return Value.asItem(DOMObjectModel.getInstance().convertObjectToXPathValue(value, config));
            } else if (value instanceof Source) {
                // Saxon extension to the XQJ specification
                Builder b = new TinyBuilder();
                PipelineConfiguration pipe = config.makePipelineConfiguration();
                b.setPipelineConfiguration(pipe);
                new Sender(pipe).send((Source)value, b);
                NodeInfo node = b.getCurrentRoot();
                b.reset();
                return node;
            } else if (value instanceof XMLStreamReader) {
                // Saxon extension to the XQJ specification
                StaxToEventBridge bridge = new StaxToEventBridge();
                bridge.setXMLStreamReader((XMLStreamReader)value);
                PipelineConfiguration pipe = config.makePipelineConfiguration();
                bridge.setPipelineConfiguration(pipe);
                Builder b = new TinyBuilder();
                b.setPipelineConfiguration(pipe);
                new Sender(pipe).send(new PullEventSource(bridge), b);
                NodeInfo node = b.getCurrentRoot();
                b.reset();
                return node;
            } else {
                throw new XPathException("Java object cannot be converted to an XQuery value");
            }
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        }
    }

   /**
     * Convert a Java object to an Item, when a required type has been specified. Note that Saxon only calls
     * this method when none of the standard conversions defined in the XQJ specification is able to handle
     * the object.
     * @param value the supplied Java object
     * @param type the required XPath data type
     * @return the Item that results from the conversion
     * @throws XQException if the Java object cannot be converted to an XQItem
     */

    public Item convertToItem(Object value, XQItemType type) throws XQException {
        if (((SaxonXQItemType)type).getSaxonItemType() instanceof ExternalObjectType) {
            return new ObjectValue(value, ((ExternalObjectType)((SaxonXQItemType)type).getSaxonItemType()));
        } else {
            throw new XQException("Supplied Java object cannot be converted to an XQItem");
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