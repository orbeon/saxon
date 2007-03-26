package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.dom.NodeOverNodeInfo;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.javax.xml.xquery.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.Orphan;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.pull.PullFromIterator;
import org.orbeon.saxon.pull.PullToStax;
import org.orbeon.saxon.query.QueryResult;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 *
 */
public class SaxonXQItem implements XQResultItem {

    private Item item;
    private Configuration config;
    private SaxonXQConnection connection;   // only present if the item is created as a ResultItem

    public SaxonXQItem(Item item, Configuration config) {
        this.item = item;
        this.config = config;
    }

    Configuration getConfiguration() {
        return config;
    }

    Item getItem() {
        return item;
    }

    void setConnection(SaxonXQConnection connection) {
        this.connection = connection;
    }



    public void clearWarnings() {
        //
    }

    public XQConnection getConnection() throws XQException {
        return connection;
    }

    public XQWarning getWarnings() throws XQException {
        return null;
    }

    public void close() throws XQException {
        item = null;
    }

    public boolean isClosed() {
        return item == null;
    }

    public String getAtomicValue() throws XQException {
        if (item instanceof AtomicValue) {
            return item.getStringValue();
        }
        throw new XQException("Failed to getAtomicValue: item is a node, or is closed");
    }

    public boolean getBoolean() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item).getPrimitiveValue();
            if (prim instanceof BooleanValue) {
                return ((BooleanValue)prim).getBooleanValue();
            }
        }
        throw new XQException("Failed in getBoolean: item is not a boolean, or is closed");
    }

    public byte getByte() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item).getPrimitiveValue();
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
                throw new XQException(err.getMessage(), err, null, null);
            }
        }
        throw new XQException("Value is not numeric");
    }

    public double getDouble() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item).getPrimitiveValue();
            if (prim instanceof DoubleValue) {
                return ((DoubleValue)prim).getDoubleValue();
            }
        }
        throw new XQException("Failed in getDouble: item is not a double, or is closed");
    }

    public float getFloat() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item).getPrimitiveValue();
            if (prim instanceof FloatValue) {
                return ((FloatValue)prim).getFloatValue();
            }
        }
        throw new XQException("Failed in getFloat: item is not a float, or is closed");
    }

    public int getInt() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item).getPrimitiveValue();
            return (byte)longValue(prim, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        throw new XQException("Failed in getInt: item is not an atomic value, or is closed");
    }

    public XMLStreamReader getItemAsStream() throws XQException {
        return new PullToStax(new PullFromIterator(SingletonIterator.makeIterator(item)));
    }

    public String getItemAsString() throws XQException {
        return getItemAsString(new Properties());
    }

    public String getItemAsString(Properties props) throws XQException {
        StringWriter writer = new StringWriter();
        writeItem(writer, props);
        return writer.toString();
    }

    public XQItemType getItemType() throws XQException {
        if (item instanceof AtomicValue) {
            return new SaxonXQItemType(
                    ((AtomicValue)item).getItemType(getConfiguration().getTypeHierarchy()),
                    getConfiguration());
        } else {
            return new SaxonXQItemType((NodeInfo)item);
        }
    }

    public long getLong() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item).getPrimitiveValue();
            return (byte)longValue(prim, Long.MIN_VALUE, Long.MAX_VALUE);
        }
        throw new XQException("Failed in getLong: item is not an atomic value, or is closed");
    }

    public Node getNode() throws XQException {
        if (!(item instanceof NodeInfo)) {
            throw new XQException("Failed in getNode: item is an atomic value, or is closed");
        }
        return NodeOverNodeInfo.wrap((NodeInfo)item);
    }

    public URI getNodeUri() throws XQException {
        if (item instanceof NodeInfo) {
            try {
                return new URI(((NodeInfo)item).getSystemId());     // TODO: this is an approximation to the spec
            } catch (URISyntaxException e) {
                throw new XQException("System ID of node is not a valid URI");
            }
        }
        throw new XQException("Item is not a node");
    }

    public Object getObject() throws XQException {
        return ((SaxonXQConnection)getConnection()).getCommonHandler().toObject(this);
    }

    public Object getObject(XQCommonHandler handler) throws XQException {
        return handler.toObject(this);
    }

    public short getShort() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item).getPrimitiveValue();
            return (byte)longValue(prim, Short.MIN_VALUE, Short.MAX_VALUE);
        }
        throw new XQException("Failed in getShort: item is not an atomic value, or is closed");
    }

    public boolean instanceOf(XQItemType type) throws XQException {
        return ((SaxonXQItemType)type).getSaxonItemType().matchesItem(item, false, config);
    }

    public void writeItem(OutputStream os, Properties props) throws XQException {
        NodeInfo node;
        if (item instanceof AtomicValue) {
            node = new Orphan(getConfiguration());
            ((Orphan)node).setNodeKind(Type.TEXT);
            ((Orphan)node).setStringValue(item.getStringValue());
        } else {
            node = (NodeInfo)item;
        }
        try {
            QueryResult.serialize(node, new StreamResult(os), props, getConfiguration());
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void writeItem(Writer ow, Properties props) throws XQException {
        NodeInfo node;
        if (item instanceof AtomicValue) {
            node = new Orphan(getConfiguration());
            ((Orphan)node).setNodeKind(Type.TEXT);
            ((Orphan)node).setStringValue(item.getStringValue());
        } else {
            node = (NodeInfo)item;
        }
        try {
            QueryResult.serialize(node, new StreamResult(ow), props, getConfiguration());
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void writeItemToSAX(ContentHandler saxHandler) throws XQException {
        ContentHandlerProxy chp = new ContentHandlerProxy();
        chp.setUnderlyingContentHandler(saxHandler);
        Receiver receiver = chp;

        NamespaceReducer reducer = new NamespaceReducer();
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        reducer.setPipelineConfiguration(pipe);
        reducer.setUnderlyingReceiver(receiver);
        ComplexContentOutputter outputter = new ComplexContentOutputter();
        outputter.setReceiver(reducer);
        outputter.setPipelineConfiguration(pipe);
        TreeReceiver tree = new TreeReceiver(outputter);
        tree.setPipelineConfiguration(pipe);
        try {
            tree.open();
            tree.append(item, 0, NodeInfo.ALL_NAMESPACES);
            tree.close();
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