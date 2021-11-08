package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.evpull.*;
import org.orbeon.saxon.functions.Insert;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.query.QueryResult;
import org.orbeon.saxon.trans.XPathException;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import javax.xml.xquery.*;
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
public class SaxonXQForwardSequence extends Closable implements XQResultSequence {

    private SequenceIterator iterator;
    SaxonXQPreparedExpression expression;
    int position = 0;   // set to -count when positioned after the end
    int lastReadPosition = Integer.MIN_VALUE;   // used to prevent reading the same item twice, which XQJ doesn't allow

    protected SaxonXQForwardSequence(SequenceIterator iterator, SaxonXQPreparedExpression expression) {
        this.iterator = iterator;
        this.expression = expression;
        setClosableContainer(expression);
    }

    SequenceIterator getCleanIterator() throws XPathException {
        return iterator.getAnother();
    }

    Configuration getConfiguration() {
        return expression.getConnection().getConfiguration();
    }

    public XQConnection getConnection()  throws XQException {
        checkNotClosed();
        return expression.getConnection();
    }

    public String getAtomicValue() throws XQException {
        return getCurrentXQItem(true).getAtomicValue();
    }

    public boolean getBoolean() throws XQException {
        return getCurrentXQItem(true).getBoolean();
    }

    public byte getByte() throws XQException {
        return getCurrentXQItem(true).getByte();
    }

    public double getDouble() throws XQException {
        return getCurrentXQItem(true).getDouble();
    }

    public float getFloat() throws XQException {
        return getCurrentXQItem(true).getFloat();
    }

    public int getInt() throws XQException {
        return getCurrentXQItem(true).getInt();
    }

    public XMLStreamReader getItemAsStream() throws XQException {
        return getCurrentXQItem(true).getItemAsStream();
    }

    public String getItemAsString(Properties props) throws XQException {
        return getCurrentXQItem(true).getItemAsString(props);
    }

    public XQItemType getItemType() throws XQException {
        return getCurrentXQItem(false).getItemType();
    }

    public long getLong() throws XQException {
       return getCurrentXQItem(true).getLong();
    }

    public Node getNode() throws XQException {
        return getCurrentXQItem(true).getNode();
    }

    public URI getNodeUri() throws XQException {
        return getCurrentXQItem(false).getNodeUri();
    }

    public Object getObject() throws XQException {
        return getCurrentXQItem(true).getObject();
    }

    public short getShort() throws XQException {
        return getCurrentXQItem(true).getShort();
    }

    public boolean instanceOf(XQItemType type) throws XQException {
        return getCurrentXQItem(false).instanceOf(type);
    }

    public void writeItem(OutputStream os, Properties props) throws XQException {
        getCurrentXQItem(true).writeItem(os, props);
    }

    public void writeItem(Writer ow, Properties props) throws XQException {
        getCurrentXQItem(true).writeItem(ow, props);
    }

    public void writeItemToResult(Result result) throws XQException {
        getCurrentXQItem(true).writeItemToResult(result);
    }

    public void writeItemToSAX(ContentHandler saxHandler) throws XQException {
        getCurrentXQItem(true).writeItemToSAX(saxHandler);
    }

    public boolean absolute(int itempos) throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public void afterLast() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public void beforeFirst() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public int count() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean first() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public XQItem getItem() throws XQException {
        return getCurrentXQItem(true);
    }

    public int getPosition() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public XMLStreamReader getSequenceAsStream() throws XQException {
        checkNotClosed();
        checkOnlyReadOnce();
        EventIterator ei = new EventIteratorOverSequence(iterator);
        ei = new BracketedDocumentIterator(ei);
        Configuration config = getConfiguration();
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        pipe.setHostLanguage(Configuration.XQUERY);
        ei = new Decomposer(ei, pipe);
        return new EventToStaxBridge(ei, config.getNamePool());
    }

    public String getSequenceAsString(Properties props) throws XQException {
        checkNotClosed();
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
            throw newXQException(e);
        }
    }

    public boolean previous() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean relative(int itempos) throws XQException {
        checkNotClosed();
//        if (itempos < 0) {
            throw new XQException("Sequence is forwards-only, cannot move backwards");
//        } else {
//            for (int i=0; i<itempos; i++) {
//                if (!next()) {
//                    return false;
//                }
//            }
//            return true;
//        }
    }

    public void writeSequence(OutputStream os, Properties props) throws XQException {
        checkNotClosed();
        checkOnlyReadOnce();
        if (props == null) {
            props = new Properties();
        }
        props = SaxonXQSequence.setDefaultProperties(props);
        SequenceIterator iter = iterator;
        if (isOnItem()) {
            iter = new Insert.InsertIterator(
                    SingletonIterator.makeIterator(iter.current()),
                    iter, 0);
        }
        try {
            QueryResult.serializeSequence(iter, getConfiguration(), os, props);
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void writeSequence(Writer ow, Properties props) throws XQException {
        checkNotClosed();
        checkNotNull(ow);
        checkOnlyReadOnce();
        if (props == null) {
            props = new Properties();
        }
        props = SaxonXQSequence.setDefaultProperties(props);
        PrintWriter pw;
        if (ow instanceof PrintWriter) {
            pw = (PrintWriter)ow;
        } else {
            pw = new PrintWriter(ow);
        }
        SequenceIterator iter = iterator;
        if (isOnItem()) {
            iter = new Insert.InsertIterator(
                    SingletonIterator.makeIterator(iter.current()),
                    iter, 0);
        }
        try {
            QueryResult.serializeSequence(iter, getConfiguration(), pw, props);
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void writeSequenceToResult(Result result) throws XQException {
        checkNotClosed();
        checkNotNull(result);
        checkOnlyReadOnce();
        Properties props = SaxonXQSequence.setDefaultProperties(null);
        try {
            QueryResult.serializeSequence(iterator, getConfiguration(), result, props);
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void writeSequenceToSAX(ContentHandler saxHandler) throws XQException {
        checkNotClosed();
        checkNotNull(saxHandler);
        writeSequenceToResult(new SAXResult(saxHandler));
    }

    private XQItem getCurrentXQItem(boolean onceOnly) throws XQException {
        checkNotClosed();
        if (position == 0) {
            throw new XQException("The XQSequence is positioned before the first item");
        } else if (position < 0) {
            throw new XQException("The XQSequence is positioned after the last item");
        }
        if (onceOnly) {
            checkOnlyReadOnce();
        }
        SaxonXQItem item = new SaxonXQItem(iterator.current(), expression.getConnection());
        item.setClosableContainer(this);
        return item;
    }

    Item getSaxonItem() {
        return iterator.current();
    }


    private void checkNotNull(Object arg) throws XQException {
        if (arg == null) {
            throw new XQException("Argument is null");
        }
    }

    private void checkOnlyReadOnce() throws XQException {
        if (position == lastReadPosition) {
            throw new XQException("XQJ does not allow the same item to be read more than once");
        }
        lastReadPosition = position;
    }

    private XQException newXQException(Exception err) {
        XQException xqe = new XQException(err.getMessage());
        xqe.initCause(err);
        return xqe;
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