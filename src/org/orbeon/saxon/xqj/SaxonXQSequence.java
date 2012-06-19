package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.evpull.*;
import org.orbeon.saxon.expr.TailIterator;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.query.QueryResult;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Value;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
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
 * Saxon implementation of the XQSequence interface in XQJ, which represents an XDM sequence together
 * with a current position. This class is used for a sequence that can be read forwards, backwards,
 * or by absolute position.
 */
public class SaxonXQSequence extends Closable implements XQResultSequence, SaxonXQItemAccessor {

    private Value value;
    private int position;
    private SaxonXQPreparedExpression expression;
    private SaxonXQDataFactory factory;

    SaxonXQSequence(Value value, SaxonXQDataFactory factory) {
        this.value = value;
        this.factory = factory;
        setClosableContainer(factory);
    }

    SaxonXQSequence(Value value, SaxonXQPreparedExpression expression) {
        this.value = value;
        this.expression = expression;
        this.factory = expression.getConnection();
        setClosableContainer(expression);
    }

    Value getValue() {
        return value;
    }

    Configuration getConfiguration() {
        return factory.getConfiguration();
    }

    public boolean absolute(int itempos) throws XQException {
        checkNotClosed();
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
            throw newXQException(e);
        }
    }

    public void afterLast() throws XQException {
        checkNotClosed();
        position = -1;
    }

    public void beforeFirst() throws XQException {
        checkNotClosed();
        position = 0;
    }

    public int count() throws XQException {
        checkNotClosed();
        try {
            return value.getLength();
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public boolean first() throws XQException {
        checkNotClosed();
        try {
            if (value.getLength() == 0) {
                position = 0;
                return false;
            } else {
                position = 1;
                return true;
            }
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public XQItem getItem() throws XQException {
        checkNotClosed();
        try {
            SaxonXQItem item =  new SaxonXQItem(value.itemAt(position - 1), factory);
            item.setClosableContainer(this);
            return item;
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public Item getSaxonItem() throws XQException {
        try {
            return value.itemAt(position - 1);
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public int getPosition() throws XQException {
        checkNotClosed();
        try {
            if (position >= 0) {
                return position;
            } else {
                return value.getLength() + 1;
            }
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public XMLStreamReader getSequenceAsStream() throws XQException {
        checkNotClosed();
        EventIterator ei = new EventIteratorOverSequence(iterateRemainder());
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
        checkNotClosed();
        return position < 0;
    }

    public boolean isBeforeFirst() throws XQException {
        checkNotClosed();
        try {
            return position == 0 && value.getLength() != 0;
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public boolean isFirst() throws XQException {
        checkNotClosed();
        return position == 1;
    }

    public boolean isLast() throws XQException {
        checkNotClosed();
        try {
            return position == value.getLength();
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public boolean isOnItem() throws XQException {
        checkNotClosed();
        return position >= 1;
    }

    public boolean isScrollable() throws XQException {
        checkNotClosed();
        return true;
    }

    public boolean last() throws XQException {
        checkNotClosed();
        try {
            int n = value.getLength();
            if (n == 0) {
                position = -1;
                return false;
            } else {
                position = n;
                return true;
            }
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public boolean next() throws XQException {
        checkNotClosed();
        try {
            if (position == value.getLength()) {
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
        checkNotClosed();
        if (position == -1) {
            return last();
        }
        position--;
        return (position != 0);
    }

    public boolean relative(int itempos) throws XQException {
        checkNotClosed();
        try {
            if (position == -1) {
                position = value.getLength() + 1;
            }
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
            throw newXQException(e);
        }
    }

    public void writeSequence(OutputStream os, Properties props) throws XQException {
        checkNotClosed();
        checkNotNull(os);
        if (props == null) {
            props = new Properties();
        }
        props = SaxonXQSequence.setDefaultProperties(props);
        try {
            QueryResult.serializeSequence(iterateRemainder(), getConfiguration(), os, props);
            position = -1;
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void writeSequence(Writer ow, Properties props) throws XQException {
        checkNotClosed();
        checkNotNull(ow);
        if (props == null) {
            props = new Properties();
        }
        props = setDefaultProperties(props);
        try {
            PrintWriter pw;
            if (ow instanceof PrintWriter) {
                pw = (PrintWriter)ow;
            } else {
                pw = new PrintWriter(ow);
            }
            QueryResult.serializeSequence(iterateRemainder(), getConfiguration(), pw, props);
            position = -1;
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void writeSequenceToResult(Result result) throws XQException {
        checkNotClosed();
        Properties props = SaxonXQSequence.setDefaultProperties(null);
        try {
            QueryResult.serializeSequence(iterateRemainder(), getConfiguration(), result, props);
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void writeSequenceToSAX(ContentHandler saxHandler) throws XQException {
        checkNotClosed();
        writeSequenceToResult(new SAXResult(saxHandler));
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

    public String getItemAsString(Properties props) throws XQException {
        return getCurrentItem().getItemAsString(props);
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

    public void writeItemToResult(Result result) throws XQException {
        getCurrentItem().writeItemToResult(result);
    }

    public void writeItemToSAX(ContentHandler saxHandler) throws XQException {
        getCurrentItem().writeItemToSAX(saxHandler);
    }

    private SaxonXQItem getCurrentItem() throws XQException {
        checkNotClosed();
        if (position == 0) {
            throw new XQException("Sequence is positioned before first item");
        }
        if (position < 0) {
            throw new XQException("Sequence is positioned after last item");
        }
        try {
            SaxonXQItem item =  new SaxonXQItem(value.itemAt(position-1), factory);
            item.setClosableContainer(this);
            return item;
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public XQConnection getConnection() throws XQException {
        checkNotClosed();
        if (expression == null) {
            throw new IllegalStateException("Connection not available");
        }
        return expression.getConnection();
    }

    private SequenceIterator iterateRemainder() throws XQException {
        try {
            if (position == 0) {
                return value.iterate();
            } else if (position < 0) {
                return EmptyIterator.getInstance();
            } else {
                return TailIterator.make(value.iterate(), position);
            }
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    private void checkNotNull(Object arg) throws XQException {
        if (arg == null) {
            throw new XQException("Argument is null");
        }
    }

    private XQException newXQException(Exception err) {
        XQException xqe = new XQException(err.getMessage());
        xqe.initCause(err);
        return xqe;
    }

    static Properties setDefaultProperties(Properties props) {
        Properties newProps = (props == null ? new Properties() : new Properties(props));
        boolean changed = false;
        if (newProps.getProperty(OutputKeys.METHOD) == null) {
            newProps.setProperty(OutputKeys.METHOD, "xml");
            changed = true;
        }
        if (newProps.getProperty(OutputKeys.INDENT) == null) {
            newProps.setProperty(OutputKeys.INDENT, "yes");
            changed = true;
        }
        if (newProps.getProperty(OutputKeys.OMIT_XML_DECLARATION) == null) {
            newProps.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            changed = true;
        }
        return (changed || props == null ? newProps : props);
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