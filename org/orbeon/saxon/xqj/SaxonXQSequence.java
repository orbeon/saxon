package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.ContentHandlerProxy;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.TreeReceiver;
import org.orbeon.saxon.expr.TailExpression;
import org.orbeon.saxon.javax.xml.xquery.*;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pull.PullFromIterator;
import org.orbeon.saxon.pull.PullToStax;
import org.orbeon.saxon.query.QueryResult;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Value;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.stream.XMLStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Properties;

/**
 * Saxon implementation of the XQSequence interface in XQJ, which represents an XDM sequence together
 * with a current position. This class is used for a sequence that can be read forwards, backwards,
 * or by absolute position.
 */
public class SaxonXQSequence implements XQResultSequence {

    private Value value;
    private Configuration config;
    private int position;
    private boolean closed;
    private SaxonXQConnection connection;

    SaxonXQSequence(Value value, Configuration config) {
        this.value = value;
        this.config = config;
    }

    SaxonXQSequence(Value value, Configuration config, SaxonXQConnection connection) {
        this.value = value;
        this.config = config;
        this.connection = connection;
    }

    Value getValue() {
        return value;
    }

    Configuration getConfiguration() {
        return getConfiguration();
    }

    public boolean absolute(int itempos) throws XQException {
        try {
            if (itempos > 0) {
                if (itempos <= value.getLength()) {
                    position = itempos;
                    return true;
                } else {
                    position = -1;
                    return false;
                }
            } else if (itempos < 0) {
                if (-itempos <= value.getLength()) {
                    position = value.getLength() + itempos + 1;
                    return true;
                } else {
                    position = 0;
                    return false;
                }
            } else {
                position = 0;
                return false;
            }
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void afterLast() throws XQException {
        position = -1;
    }

    public void beforeFirst() throws XQException {
        position = 0;
    }

    public void close() throws XQException {
        closed = true;
        value = null;
    }

    public int count() throws XQException {
        try {
            return value.getLength();
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public boolean first() throws XQException {
        try {
            if (value.getLength() == 0) {
                position = 0;
                return false;
            } else {
                position = 1;
                return true;
            }
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public XQItem getItem() throws XQException {
        try {
            SaxonXQItem item = new SaxonXQItem(value.itemAt(position - 1), config);
            item.setConnection(connection);
            return item;
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public int getPosition() throws XQException {
        try {
            if (position >= 0) {
                return position;
            } else {
                return value.getLength();
            }
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public XMLStreamReader getSequenceAsStream() throws XQException {
        return new PullToStax(new PullFromIterator(iterateRemainder()));
    }

    public String getSequenceAsString(Properties props) throws XQException {
        StringWriter sw = new StringWriter();
        writeSequence(sw, props);
        return sw.toString();
    }

    public boolean isAfterLast() throws XQException {
        return position < 0;
    }

    public boolean isBeforeFirst() throws XQException {
        return position == 0;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isFirst() throws XQException {
        return position == 1;
    }

    public boolean isLast() throws XQException {
        try {
            return position == value.getLength();
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public boolean isOnItem() throws XQException {
        return position >= 1;
    }

    public boolean isScrollable() throws XQException {
        return true;
    }

    public boolean last() throws XQException {
        try {
            int n = value.getLength();
            if (n == 0) {
                position = -1;
                return false;
            } else {
                position = 0;
                return true;
            }
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public boolean next() throws XQException {
        try {
            if (position == value.getLength()) {
                position = -1;
                return false;
            } else {
                position++;
                return true;
            }
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public boolean previous() throws XQException {
        position--;
        return (position != 0);
    }

    public boolean relative(int itempos) throws XQException {
        try {
            position += itempos;
            if (position <= 0) {
                position = 0;
                return false;
            }
            if (position > value.getLength()) {
                position = -1;
                return false;
            }
            return true;
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void writeSequence(OutputStream os, Properties props) throws XQException {
        try {
            QueryResult.serializeSequence(iterateRemainder(), config, os, props);
            position = -1;
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void writeSequence(Writer ow, Properties props) throws XQException {
        try {
            PrintWriter pw;
            if (ow instanceof PrintWriter) {
                pw = (PrintWriter)ow;
            } else {
                pw = new PrintWriter(ow);
            }
            QueryResult.serializeSequence(iterateRemainder(), config, pw, props);
            position = -1;
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void writeSequenceToSAX(ContentHandler saxHandler) throws XQException {
        try {
            PipelineConfiguration pipe = config.makePipelineConfiguration();
            SequenceIterator iter = iterateRemainder();
            ContentHandlerProxy chp = new ContentHandlerProxy();
            chp.setUnderlyingContentHandler(saxHandler);
            chp.setPipelineConfiguration(pipe);
            TreeReceiver tr = new TreeReceiver(chp);
            tr.open();
            while (true) {
                Item item = iter.next();
                if (item == null) {
                    break;
                }
                tr.append(item, 0, NodeInfo.ALL_NAMESPACES);
            }
            tr.close();
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public String getAtomicValue() throws XQException {
        return getCurrentItem().getAtomicValue();
    }

    public boolean getBoolean() throws XQException {
        return getCurrentItem().getBoolean();
    }

    public byte getByte() throws XQException {
        return getCurrentItem().getByte();
    }

    public double getDouble() throws XQException {
        return getCurrentItem().getDouble();
    }

    public float getFloat() throws XQException {
        return getCurrentItem().getFloat();
    }

    public int getInt() throws XQException {
        return getCurrentItem().getInt();
    }

    public XMLStreamReader getItemAsStream() throws XQException {
        return getCurrentItem().getItemAsStream();
    }

    public String getItemAsString() throws XQException {
        return getCurrentItem().getItemAsString();
    }

    public XQItemType getItemType() throws XQException {
        return getCurrentItem().getItemType();
    }

    public long getLong() throws XQException {
        return getCurrentItem().getLong();
    }

    public Node getNode() throws XQException {
        return getCurrentItem().getNode();
    }

    public URI getNodeUri() throws XQException {
        return getCurrentItem().getNodeUri();
    }

    public Object getObject() throws XQException {
        return getCurrentItem().getObject();
    }

    public Object getObject(XQCommonHandler handler) throws XQException {
        return getCurrentItem().getObject(handler);
    }

    public short getShort() throws XQException {
        return getCurrentItem().getShort();
    }

    public boolean instanceOf(XQItemType type) throws XQException {
        return getCurrentItem().instanceOf(type);
    }

    public void writeItem(OutputStream os, Properties props) throws XQException {
        getCurrentItem().writeItem(os, props);
    }

    public void writeItem(Writer ow, Properties props) throws XQException {
        getCurrentItem().writeItem(ow, props);
    }

    public void writeItemToSAX(ContentHandler saxHandler) throws XQException {
        getCurrentItem().writeItemToSAX(saxHandler);
    }

    private SaxonXQItem getCurrentItem() throws XQException {
        if (closed) {
            throw new XQException("Sequence is closed");
        }
        if (position == 0) {
            throw new XQException("Sequence is positioned before first item");
        }
        if (position < 0) {
            throw new XQException("Sequence is positioned after last item");
        }
        try {
            return new SaxonXQItem(value.itemAt(position-1), config);
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void clearWarnings() {
        //
    }

    public XQConnection getConnection() {
        return connection;
    }

    public XQWarning getWarnings() {
        return null;  
    }

    private SequenceIterator iterateRemainder() throws XQException {
        try {
            if (position == 0) {
                return value.iterate(null);
            } else if (position < 0) {
                return EmptyIterator.getInstance();
            } else {
                return new TailExpression.TailIterator(value.iterate(null), position);
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