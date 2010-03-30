package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.dom.NodeOverNodeInfo;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.SerializerFactory;
import org.orbeon.saxon.event.TreeReceiver;
import org.orbeon.saxon.evpull.*;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.VirtualNode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.*;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQResultItem;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * This Saxon class is used to implement both the XQItem and XQResultItem interfaces in XQJ.
 * Where the item is not a real XQResultItem, getConnection() will return null.
 */
public class SaxonXQItem extends Closable implements XQResultItem, SaxonXQItemAccessor {

    private Item item;
    private Configuration config;
    SaxonXQDataFactory dataFactory;

    public SaxonXQItem(Item item, SaxonXQDataFactory factory) {
        if (item == null) {
            throw new NullPointerException("item");
        }
        this.item = item;
        dataFactory = factory;
        config = factory.getConfiguration();
        setClosableContainer(factory);
    }

    Configuration getConfiguration() {
        return config;
    }

    public Item getSaxonItem() {
        return item;
    }

    public XQConnection getConnection() throws XQException {
        checkNotClosed();
        if (dataFactory instanceof XQConnection) {
            return (XQConnection)dataFactory;
        } else {
            return null;
        }
    }

    public String getAtomicValue() throws XQException {
        checkNotClosed();
        if (item instanceof AtomicValue) {
            return item.getStringValue();
        }
        throw new XQException("Failed to getAtomicValue: item is a node, or is closed");
    }

    public boolean getBoolean() throws XQException {
        checkNotClosed();
        if (item instanceof BooleanValue) {
            return ((BooleanValue)item).getBooleanValue();
        }
        throw new XQException("Failed in getBoolean: item is not a boolean, or is closed");
    }

    public byte getByte() throws XQException {
        checkNotClosed();
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item);
            return (byte)longValue(prim, -128, 127);
        }
        throw new XQException("Failed in getByte: item is not an atomic value, or is closed");
    }

    private static long longValue(AtomicValue value, long min, long max) throws XQException {
        if (value instanceof NumericValue) {
            if (value instanceof DoubleValue || value instanceof FloatValue) {
                throw new XQException("Value is a double or float");
            }
            if (!((NumericValue)value).isWholeNumber()) {
                throw new XQException("Value is not a whole number");
            }
            try {
                long val = ((NumericValue)value).longValue();
                if (val >= min && val <= max) {
                    return val;
                } else {
                    throw new XQException("Value is out of range for requested type");
                }
            } catch (XPathException err) {
                XQException xqe = new XQException(err.getMessage());
                xqe.initCause(err);
                throw xqe;
            }
        }
        throw new XQException("Value is not numeric");
    }

    public double getDouble() throws XQException {
        checkNotClosed();
        if (item instanceof DoubleValue) {
            return ((DoubleValue)item).getDoubleValue();
        }
        throw new XQException("Failed in getDouble: item is not a double, or is closed");
    }

    public float getFloat() throws XQException {
        checkNotClosed();
        if (item instanceof FloatValue) {
            return ((FloatValue)item).getFloatValue();
        }
        throw new XQException("Failed in getFloat: item is not a float, or is closed");
    }

    public int getInt() throws XQException {
        checkNotClosed();
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item);
            return (byte)longValue(prim, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        throw new XQException("Failed in getInt: item is not an atomic value, or is closed");
    }

    public XMLStreamReader getItemAsStream() throws XQException {
        // The spec (section 12.1) requires that the item be converted into a document, and we
        // then read events corresponding to this document
        checkNotClosed();
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        pipe.setHostLanguage(Configuration.XQUERY);
        if (item instanceof DocumentInfo) {
            EventIterator eventIterator = new Decomposer((NodeInfo)item, pipe);
            return new EventToStaxBridge(eventIterator, config.getNamePool());
        } else {
            EventIterator contentIterator = new SingletonEventIterator(item);
            EventIterator eventIterator = new BracketedDocumentIterator(contentIterator);
            eventIterator = new Decomposer(eventIterator, pipe);
            return new EventToStaxBridge(eventIterator, config.getNamePool());
        }
    }

    public String getItemAsString(Properties props) throws XQException {
        checkNotClosed();
        if (props == null) {
            props = new Properties();
        }
        props = SaxonXQSequence.setDefaultProperties(props);
        StringWriter writer = new StringWriter();
        writeItem(writer, props);
        return writer.toString();
    }

    public XQItemType getItemType() throws XQException {
        checkNotClosed();
        if (item instanceof AtomicValue) {
            return new SaxonXQItemType(
                    ((AtomicValue)item).getItemType(getConfiguration().getTypeHierarchy()),
                    getConfiguration());
        } else {
            return new SaxonXQItemType((NodeInfo)item);
        }
    }

    public long getLong() throws XQException {
        checkNotClosed();
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item);
            return (byte)longValue(prim, Long.MIN_VALUE, Long.MAX_VALUE);
        }
        throw new XQException("Failed in getLong: item is not an atomic value, or is closed");
    }

    public Node getNode() throws XQException {
        checkNotClosed();
        if (!(item instanceof NodeInfo)) {
            throw new XQException("Failed in getNode: item is an atomic value, or is closed");
        }
        if (item instanceof VirtualNode) {
            Object n = ((VirtualNode)item).getUnderlyingNode();
            if (n instanceof Node) {
                return (Node)n;
            }
        }
        return NodeOverNodeInfo.wrap((NodeInfo)item);
    }

    public URI getNodeUri() throws XQException {
        checkNotClosed();
        if (item instanceof NodeInfo) {
            try {
                String systemId = ((NodeInfo)item).getSystemId();
                if (systemId == null) {
                    return new URI("");
                }
                return new URI(systemId);
            } catch (URISyntaxException e) {
                throw new XQException("System ID of node is not a valid URI");
            }
        }
        throw new XQException("Item is not a node");
    }

    public Object getObject() throws XQException {
        checkNotClosed();
        return dataFactory.getObjectConverter().toObject(this);
    }

    public short getShort() throws XQException {
        checkNotClosed();
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item);
            return (byte)longValue(prim, Short.MIN_VALUE, Short.MAX_VALUE);
        }
        throw new XQException("Failed in getShort: item is not an atomic value, or is closed");
    }

    public boolean instanceOf(XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(type);
        return ((SaxonXQItemType)type).getSaxonItemType().matchesItem(item, false, config);
    }

    public void writeItem(OutputStream os, Properties props) throws XQException {
        checkNotClosed();
        checkNotNull(os);
        writeItemToResult(new StreamResult(os), props);
    }

    public void writeItem(Writer ow, Properties props) throws XQException {
        checkNotClosed();
        checkNotNull(ow);
        writeItemToResult(new StreamResult(ow), props);
    }

    public void writeItemToResult(Result result) throws XQException {
        checkNotClosed();
        checkNotNull(result);
        writeItemToResult(result, new Properties());
    }

    private void writeItemToResult(Result result, Properties props) throws XQException {
        checkNotClosed();
        checkNotNull(result);
        if (props == null) {
            props = new Properties();
        }
        props = SaxonXQSequence.setDefaultProperties(props);
        try {
            SerializerFactory sf = config.getSerializerFactory();
            PipelineConfiguration pipe = config.makePipelineConfiguration();
            Receiver out = sf.getReceiver(result, pipe, props);
            TreeReceiver tr = new TreeReceiver(out);
            tr.open();
            tr.append(item, 0, NodeInfo.ALL_NAMESPACES);
            tr.close();
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        }
    }

    public void writeItemToSAX(ContentHandler saxHandler) throws XQException {
        checkNotClosed();
        checkNotNull(saxHandler);
        writeItemToResult(new SAXResult(saxHandler));
    }

    private void checkNotNull(Object arg) throws XQException {
        if (arg == null) {
            throw new XQException("Argument is null");
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