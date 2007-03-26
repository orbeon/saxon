package org.orbeon.saxon.xqj;

import org.orbeon.saxon.event.ContentHandlerProxy;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.TreeReceiver;
import org.orbeon.saxon.javax.xml.xquery.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pull.PullFromIterator;
import org.orbeon.saxon.pull.PullToStax;
import org.orbeon.saxon.query.QueryResult;
import org.orbeon.saxon.trans.XPathException;
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
 * The class is a Saxon implementation of the XQJ interface XQResultSequence. This
 * implementation is used to represent a sequence that can only be read in a forwards direction.
 */
public class SaxonXQForwardSequence implements XQResultSequence {

    private SequenceIterator iterator;
    private SaxonXQConnection connection;
    int position = 0;   // set to -count when positioned after the end
    boolean closed = false;

    protected SaxonXQForwardSequence(SequenceIterator iterator, SaxonXQConnection connection) {
        this.iterator = iterator;
        this.connection = connection;
    }

    SequenceIterator getCleanIterator() throws XPathException {
        return iterator.getAnother();
    }

    public void clearWarnings() {

    }

    public XQConnection getConnection() {
        return connection;
    }

    public XQWarning getWarnings() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getAtomicValue() throws XQException {
        return getCurrentXQItem().getAtomicValue();
    }

    public boolean getBoolean() throws XQException {
        return getCurrentXQItem().getBoolean();
    }

    public byte getByte() throws XQException {
        return getCurrentXQItem().getByte();
    }

    public double getDouble() throws XQException {
        return getCurrentXQItem().getDouble();
    }

    public float getFloat() throws XQException {
        return getCurrentXQItem().getFloat();
    }

    public int getInt() throws XQException {
        return getCurrentXQItem().getInt();
    }

    public XMLStreamReader getItemAsStream() throws XQException {
        return getCurrentXQItem().getItemAsStream();
    }

    public String getItemAsString() throws XQException {
        return getCurrentXQItem().getItemAsString();
    }

    public XQItemType getItemType() throws XQException {
        return getCurrentXQItem().getItemType();
    }

    public long getLong() throws XQException {
       return getCurrentXQItem().getLong();
    }

    public Node getNode() throws XQException {
        return getCurrentXQItem().getNode();
    }

    public URI getNodeUri() throws XQException {
        return getCurrentXQItem().getNodeUri();
    }

    public Object getObject() throws XQException {
        return getCurrentXQItem().getObject();
    }

    public Object getObject(XQCommonHandler handler) throws XQException {
        return getCurrentXQItem().getObject(handler);
    }

    public short getShort() throws XQException {
        return getCurrentXQItem().getShort();
    }

    public boolean instanceOf(XQItemType type) throws XQException {
        return getCurrentXQItem().instanceOf(type);
    }

    public void writeItem(OutputStream os, Properties props) throws XQException {
        getCurrentXQItem().writeItem(os, props);
    }

    public void writeItem(Writer ow, Properties props) throws XQException {
        getCurrentXQItem().writeItem(ow, props);
    }

    public void writeItemToSAX(ContentHandler saxHandler) throws XQException {
        getCurrentXQItem().writeItemToSAX(saxHandler);
    }

    public boolean absolute(int itempos) throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public void afterLast() throws XQException {
        checkNotClosed();
        position = -1;
    }

    public void beforeFirst() throws XQException {
        checkNotClosed();
        // This should probably be an error for a forwards-only sequence. But the spec doesn't actually
        // say so, and as it happens, we can implement the functionality by getting a new iterator.
        try {
            iterator = iterator.getAnother();
            position = 0;
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void close() throws XQException {
        closed = true;
        iterator = null;
        connection = null;
    }

    public int count() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean first() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public XQItem getItem() throws XQException {
        return new SaxonXQItem(iterator.current(), connection.getConfiguration());
    }

    public int getPosition() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public XMLStreamReader getSequenceAsStream() throws XQException {
        PullFromIterator provider = new PullFromIterator(iterator);
        provider.setPipelineConfiguration(connection.getConfiguration().makePipelineConfiguration());
        return new PullToStax(provider);
    }

    public String getSequenceAsString(Properties props) throws XQException {
        StringWriter sw = new StringWriter();
        writeSequence(sw, props);
        return sw.toString();
    }

    public boolean isAfterLast() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean isBeforeFirst() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isFirst() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean isLast() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean isOnItem() throws XQException {
        checkNotClosed();
        return position > 0;
    }

    public boolean isScrollable() throws XQException {
        checkNotClosed();
        return false;
    }

    public boolean last() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean next() throws XQException {
        checkNotClosed();
        if (position < 0) {
            return false;
        }
        try {
            Item next = iterator.next();
            if (next == null) {
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
        throw new XQException("Sequence is forwards-only");
    }

    public boolean relative(int itempos) throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public void writeSequence(OutputStream os, Properties props) throws XQException {
        checkNotClosed();
        try {
            QueryResult.serializeSequence(iterator, connection.getConfiguration(), os, props);
            // TODO: if we're positioned on an item, this will serialize starting at the next item
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void writeSequence(Writer ow, Properties props) throws XQException {
        checkNotClosed();
        PrintWriter pw;
        if (ow instanceof PrintWriter) {
            pw = (PrintWriter)ow;
        } else {
            pw = new PrintWriter(ow);
        }
        try {
            QueryResult.serializeSequence(iterator, connection.getConfiguration(), pw, props);
            // TODO: if we're positioned on an item, this will serialize starting at the next item
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void writeSequenceToSAX(ContentHandler saxHandler) throws XQException {
        checkNotClosed();
        if (position < 0) {
            return; // nothing to do
        }
        try {
            PipelineConfiguration pipe = connection.getConfiguration().makePipelineConfiguration();
            ContentHandlerProxy chp = new ContentHandlerProxy();
            chp.setUnderlyingContentHandler(saxHandler);
            chp.setPipelineConfiguration(pipe);
            TreeReceiver tr = new TreeReceiver(chp);
            tr.open();
            Item item = iterator.current();
            while (true) {
                tr.append(item, 0, NodeInfo.ALL_NAMESPACES);
                item = iterator.next();
                if (item == null) {
                    break;
                }
            }
            tr.close();
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    private XQItemAccessor getCurrentXQItem() throws XQException {
        checkNotClosed();
        if (position == 0) {
            throw new XQException("The XQSequence is positioned before the first item");
        } else if (position < 0) {
            throw new XQException("The XQSequence is positioned after the last item");
        }
        return new SaxonXQItem(iterator.current(), connection.getConfiguration());
    }

    private void checkNotClosed() throws XQException {
        if (closed) {
            throw new XQException("The XQSequence has been closed");
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