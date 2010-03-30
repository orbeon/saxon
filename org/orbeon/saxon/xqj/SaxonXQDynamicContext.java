package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.query.DynamicQueryContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.*;
import org.w3c.dom.Node;
import org.xml.sax.XMLReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.xquery.*;
import java.io.InputStream;
import java.io.Reader;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Saxon implementation of the XQJ DynamicContext interface
 */

public abstract class SaxonXQDynamicContext extends Closable implements XQDynamicContext {

    protected SaxonXQConnection connection;

    protected abstract DynamicQueryContext getDynamicContext();

    protected final Configuration getConfiguration() {
        return connection.getConfiguration();
    }

    protected abstract SaxonXQDataFactory getDataFactory() throws XQException;

    protected abstract boolean externalVariableExists(QName name);

    private TimeZone implicitTimeZone = null;

    public void bindAtomicValue(QName varname, String value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromAtomicValue(value, type);
        bindExternalVariable(varname, item.getSaxonItem());
    }

    public void bindBoolean(QName varname, boolean value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        BooleanValue target = BooleanValue.get(value);
        checkAtomic(type, target);
        bindExternalVariable(varname, target);
    }

    public void bindByte(QName varname, byte value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromByte(value, type);
        AtomicValue target = (AtomicValue)item.getSaxonItem();
        checkAtomic(type, target);
        bindExternalVariable(varname, target);
    }

    public void bindDocument(QName varname, InputStream value, String baseURI, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, baseURI, type);
        bindExternalVariable(varname, item.getSaxonItem());
    }

    public void bindDocument(QName varname, Reader value, String baseURI, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, baseURI, type);
        bindExternalVariable(varname, item.getSaxonItem());
    }

    public void bindDocument(QName varname, Source value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, type);
        bindExternalVariable(varname, item.getSaxonItem());
    }

    public void bindDocument(QName varname, String value, String baseURI, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, baseURI, type);
        bindExternalVariable(varname, item.getSaxonItem());
    }

    public void bindDocument(QName varname, XMLReader value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, type);
        bindExternalVariable(varname, item.getSaxonItem());
    }

    public void bindDocument(QName varname, XMLStreamReader value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, type);
        bindExternalVariable(varname, item.getSaxonItem());
    }

    public void bindDouble(QName varname, double value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        AtomicValue target = new DoubleValue(value);
        checkAtomic(type, target);
        bindExternalVariable(varname, target);
    }

    public void bindFloat(QName varname, float value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        AtomicValue target = new FloatValue(value);
        checkAtomic(type, target);
        bindExternalVariable(varname, target);
    }

    public void bindInt(QName varname, int value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromInt(value, type);
        AtomicValue target = (AtomicValue)item.getSaxonItem();
        checkAtomic(type, target);
        bindExternalVariable(varname, target);
    }

    public void bindItem(QName varname, XQItem value) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        bindExternalVariable(varname, ((SaxonXQItem) value).getSaxonItem());
    }

    public void bindLong(QName varname, long value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromLong(value, type);
        AtomicValue target = (AtomicValue)item.getSaxonItem();
        checkAtomic(type, target);
        bindExternalVariable(varname, target);
    }

    public void bindNode(QName varname, Node value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromNode(value, type);
        bindExternalVariable(varname, item.getSaxonItem());
    }

    public void bindObject(QName varname, Object value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromObject(value, type);
        bindExternalVariable(varname, item.getSaxonItem());

    }

    public void bindSequence(QName varname, XQSequence value) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        try {
            if (value instanceof SaxonXQForwardSequence) {
                getDynamicContext().setParameter(getClarkName(varname),
                        ((SaxonXQForwardSequence) value).getCleanIterator());
            } else if (value instanceof SaxonXQSequence) {
                bindExternalVariable(varname, ((SaxonXQSequence) value).getValue());
            } else {
                throw new XQException("XQSequence value is not a Saxon sequence");
            }
        } catch (XPathException de) {
            XQException err = new XQException(de.getMessage());
            err.initCause(de);
            throw err;
        }
    }

    public void bindShort(QName varname, short value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromShort(value, type);
        AtomicValue target = (AtomicValue)item.getSaxonItem();
        checkAtomic(type, target);
        bindExternalVariable(varname, target);
    }

    public void bindString(QName varname, String value, XQItemType type) throws XQException {
        checkNotClosed();
        checkNotNull(varname);
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromString(value, type);
        AtomicValue target = (AtomicValue)item.getSaxonItem();
        checkAtomic(type, target);
        bindExternalVariable(varname, target);
    }    

    public TimeZone getImplicitTimeZone() throws XQException {
        checkNotClosed();
        if (implicitTimeZone != null) {
            return implicitTimeZone;
        } else {
            return new GregorianCalendar().getTimeZone(); 
        }
    }

    public void setImplicitTimeZone(TimeZone implicitTimeZone) throws XQException {
        checkNotClosed();
        GregorianCalendar now = new GregorianCalendar(implicitTimeZone);
        try {
            getDynamicContext().setCurrentDateTime(new DateTimeValue(now, true));
        } catch (XPathException e) {
            throw new XQException(e.getMessage());
        }
        this.implicitTimeZone = implicitTimeZone;
    }


    private void bindExternalVariable(QName varName, ValueRepresentation value) throws XQException {
        checkNotNull(varName);
        checkNotNull(value);
        try {
            if (varName.equals(XQConstants.CONTEXT_ITEM)) {
                getDynamicContext().setContextItem(Value.asItem(value));
            } else {
                if (!externalVariableExists(varName)) {
                    throw new XQException("No external variable named " + varName + " exists in the query");
                }
                getDynamicContext().setParameterValue(getClarkName(varName), value);
            }
        } catch (XPathException e) {
            XQException err = new XQException(e.getMessage());
            err.initCause(e);
            throw err;
        }
    }

    private void checkAtomic(XQItemType type, AtomicValue value) throws XQException {
        if (type == null) {
            return;
        }
        ItemType itemType = ((SaxonXQItemType)type).getSaxonItemType();
        if (!itemType.isAtomicType()) {
            throw new XQException("Target type is not atomic");
        }
        AtomicType at = (AtomicType)itemType;
        if (!at.matchesItem(value, true, getConfiguration())) {
            throw new XQException("value is invalid for specified type");
        }
    }


    private static String getClarkName(QName qname) {
        String uri = qname.getNamespaceURI();
        return "{" + (uri == null ? "" : uri) + "}" + qname.getLocalPart();
    }

    private static void checkNotNull(Object arg) throws XQException {
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