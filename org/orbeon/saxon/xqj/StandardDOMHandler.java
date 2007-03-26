package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.dom.DOMObjectModel;
import org.orbeon.saxon.dom.NodeOverNodeInfo;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.javax.xml.xquery.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.pull.PullSource;
import org.orbeon.saxon.pull.StaxBridge;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInSchemaFactory;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;
import org.w3c.dom.Node;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Implementation of XQCommonHandler that performs the mappings between Java and XPath
 * as defined in the XQJ specification. This is the handler that is used by default.
 * <p>
 * This handler implements the mappings defined in the XQJ specification. In addition,
 * it defines the following mappings, which are applied after those defined in XQJ:</p>
 *
 * <p>For fromObject:</p>
 * <ul>
 * <li>If the supplied object is an instance of javax.xml.transform.Source, a document
 * node is constructed from the source and the resulting node is returned as the Item</li>
 * <li>If the supplied object is an instance of javax.xml.stream.XMLStreamReader, a document
 * node is constructed from the XMLStreamReader and the resulting node is returned as the Item</li>*
 * </ul>
 */
public class StandardDOMHandler implements XQCommonHandler {

    Configuration config;

    public StandardDOMHandler(XQDataSource source) {
        this.config = ((SaxonXQDataSource)source).getConfiguration();
    }

    public XQItem fromObject(Object obj) throws XQException {
        return new SaxonXQItem(convertToItem(obj), config);
    }

    public Object toObject(XQItemAccessor item) throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue p = ((AtomicValue)item).getPrimitiveValue();
            int t = p.getItemType(config.getTypeHierarchy()).getPrimitiveType();
            switch (t) {
                case Type.ANY_URI:
                    return p.getStringValue();
                case Type.BASE64_BINARY:
                    return ((Base64BinaryValue)p).getBinaryValue();
                case Type.BOOLEAN:
                    return Boolean.valueOf(((BooleanValue)p).getBooleanValue());
                case Type.DATE:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case Type.DATE_TIME:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case Type.DECIMAL:
                    return ((DecimalValue)p).getValue();
                case Type.DOUBLE:
                    return new Double(((DoubleValue)p).getDoubleValue());
                case Type.DURATION:
                    return new SaxonDuration((DurationValue)p);
                case Type.FLOAT:
                    return new Float(((FloatValue)p).getFloatValue());
                case Type.G_DAY:
                case Type.G_MONTH:
                case Type.G_MONTH_DAY:
                case Type.G_YEAR:
                case Type.G_YEAR_MONTH:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case Type.HEX_BINARY:
                    return ((HexBinaryValue)p).getBinaryValue();
                case Type.INTEGER:
                    if (p instanceof BigIntegerValue) {
                        return ((BigIntegerValue)p).getBigInteger();
                    } else {
                        int sub = ((AtomicType)p.getItemType(null)).getFingerprint();
                        switch (sub) {
                            case Type.INTEGER:
                            case Type.NEGATIVE_INTEGER:
                            case Type.NON_NEGATIVE_INTEGER:
                            case Type.NON_POSITIVE_INTEGER:
                            case Type.POSITIVE_INTEGER:
                            case Type.UNSIGNED_LONG:
                                return BigInteger.valueOf(((IntegerValue)p).longValue());
                            case Type.BYTE:
                                return new Byte((byte)((IntegerValue)p).longValue());
                            case Type.INT:
                            case Type.UNSIGNED_SHORT:
                                return new Integer((int)((IntegerValue)p).longValue());
                            case Type.LONG:
                            case Type.UNSIGNED_INT:
                                return new Long((long)((IntegerValue)p).longValue());
                            case Type.SHORT:
                            case Type.UNSIGNED_BYTE:
                                return new Short((short)((IntegerValue)p).longValue());
                            default:
                                throw new XQException("Unrecognized integer subtype " + sub);
                        }
                    }
                case Type.QNAME:
                    return ((QNameValue)p).makeQName(config);
                case Type.STRING:
                    return p.getStringValue();
                case Type.TIME:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case Type.DAY_TIME_DURATION:
                    return new SaxonDuration((DurationValue)p);
                case Type.YEAR_MONTH_DURATION:
                    return new SaxonDuration((DurationValue)p);
                default:
                    throw new XQException("unsupported type");
            }
        } else {
            return NodeOverNodeInfo.wrap((NodeInfo)item);
        }
    }

    public Item convertToItem(Object value) throws XQException {
        try {
            if (value instanceof Boolean) {
                return BooleanValue.get(((Boolean)value).booleanValue());
            } else if (value instanceof byte[]) {
                return new HexBinaryValue((byte[])value);
            } else if (value instanceof Byte) {
                return new IntegerValue(((Byte)value).byteValue(),
                        (AtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_BYTE));
            } else if (value instanceof Float) {
                return new FloatValue(((Float)value).floatValue());
            } else if (value instanceof Double) {
                return new DoubleValue(((Double)value).doubleValue());
            } else if (value instanceof Integer) {
                return new IntegerValue(((Integer)value).intValue(),
                        (AtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_INT));
            } else if (value instanceof Long) {
                return new IntegerValue(((Long)value).longValue(),
                        (AtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_LONG));
            } else if (value instanceof Short) {
                return new IntegerValue(((Short)value).shortValue(),
                        (AtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_SHORT));
            } else if (value instanceof String) {
                return new UntypedAtomicValue((String)value);
            } else if (value instanceof BigDecimal) {
                return new DecimalValue((BigDecimal)value);
            } else if (value instanceof BigInteger) {
                return new BigIntegerValue((BigInteger)value);
            } else if (value instanceof Duration) {
                // this is simpler and safer (but perhaps slower) than extracting all the components
                return new DurationValue(((Duration)value).toString());
            } else if (value instanceof XMLGregorianCalendar) {
                XMLGregorianCalendar g = (XMLGregorianCalendar)value;
                QName gtype = g.getXMLSchemaType();
                if (gtype.equals(DatatypeConstants.DATETIME)) {
                    return new DateTimeValue(gtype.toString());
                } else if (gtype.equals(DatatypeConstants.DATE)) {
                    return new DateValue(gtype.toString());
                } else if (gtype.equals(DatatypeConstants.TIME)) {
                    return new TimeValue(gtype.toString());
                } else if (gtype.equals(DatatypeConstants.GYEAR)) {
                    return new GYearValue(gtype.toString());
                } else if (gtype.equals(DatatypeConstants.GYEARMONTH)) {
                    return new GYearMonthValue(gtype.toString());
                } else if (gtype.equals(DatatypeConstants.GMONTH)) {
                    return new GMonthValue(gtype.toString());
                } else if (gtype.equals(DatatypeConstants.GMONTHDAY)) {
                    return new GMonthDayValue(gtype.toString());
                } else if (gtype.equals(DatatypeConstants.GDAY)) {
                    return new GDayValue(gtype.toString());
                } else {
                    throw new AssertionError("Unknown Gregorian date type");
                }
            } else if (value instanceof QName) {
                QName q = (QName)value;
                return new QNameValue(q.getPrefix(), q.getNamespaceURI(), q.getLocalPart(), null);
            } else if (value instanceof Node) {
                return (Item)(new DOMObjectModel().convertObjectToXPathValue(value, config));
            } else if (value instanceof Source) {
                // Saxon extension to the XQJ specification
                Builder b = new TinyBuilder();
                PipelineConfiguration pipe = config.makePipelineConfiguration();
                b.setPipelineConfiguration(pipe);
                new Sender(pipe).send((Source)value, b);
                return b.getCurrentRoot();
            } else if (value instanceof XMLStreamReader) {
                // Saxon extension to the XQJ specification
                StaxBridge bridge = new StaxBridge();
                bridge.setXMLStreamReader((XMLStreamReader)value);
                Builder b = new TinyBuilder();
                PipelineConfiguration pipe = config.makePipelineConfiguration();
                b.setPipelineConfiguration(pipe);
                new Sender(pipe).send(new PullSource(bridge), b);
                return b.getCurrentRoot();
            } else {
                throw new DynamicError("Java object cannot be converted to an XQuery value");
            }
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
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