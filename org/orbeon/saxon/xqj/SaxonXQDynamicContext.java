package org.orbeon.saxon.xqj;

import org.orbeon.saxon.javax.xml.xquery.*;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.query.DynamicQueryContext;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.DoubleValue;
import org.orbeon.saxon.value.FloatValue;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import javax.xml.transform.sax.SAXSource;
import java.util.TimeZone;

/**
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: 15-May-2006
 * Time: 13:54:02
 * To change this template use File | Settings | File Templates.
 */
public abstract class SaxonXQDynamicContext implements XQDynamicContext {

    protected abstract DynamicQueryContext getDynamicContext();

    protected abstract void checkNotClosed() throws XQException;

    protected abstract SaxonXQDataFactory getDataFactory() throws XQException ;


    public void bindAtomicValue(QName varname, String value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromAtomicValue(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindBoolean(QName varname, boolean value, XQItemType type) throws XQException {
        checkNotClosed();
        bindExternalVariable(varname, BooleanValue.get(value));
    }

    public void bindByte(QName varname, byte value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromByte(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindContextItem(XQItem contextitem) throws XQException {
        checkNotClosed();
        getDynamicContext().setContextItem(((SaxonXQItem) contextitem).getItem());
    }

    public void bindDocument(QName varname, InputSource source) throws XQException {
        checkNotClosed();
        try {
            SAXSource ss = new SAXSource(source);
            DocumentInfo doc = new StaticQueryContext(getDynamicContext().getConfiguration()).buildDocument(ss);
            getDynamicContext().setParameterValue(getClarkName(varname), doc);
        } catch (XPathException de) {
            throw new XQException(de.getMessage(), de, null, null);
        }
    }

    public void bindDouble(QName varname, double value, XQItemType type) throws XQException {
        checkNotClosed();
        getDynamicContext().setParameterValue(getClarkName(varname), new DoubleValue(value));
    }

    public void bindFloat(QName varname, float value, XQItemType type) throws XQException {
        checkNotClosed();
        getDynamicContext().setParameterValue(getClarkName(varname), new FloatValue(value));
    }

    public void bindInt(QName varname, int value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromInt(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindItem(QName varname, XQItem value) throws XQException {
        checkNotClosed();
        bindExternalVariable(varname, ((SaxonXQItem) value).getItem());
    }

    public void bindLong(QName varname, long value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromLong(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindNode(QName varname, Node value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromNode(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindObject(QName varname, Object value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromObject(value, type);
        bindExternalVariable(varname, item.getItem());

    }

    public void bindSequence(QName varname, XQSequence value) throws XQException {
        checkNotClosed();
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
            throw new XQException(de.getMessage(), de, null, null);
        }
    }

    public void bindShort(QName varname, short value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromShort(value, type);
        bindExternalVariable(varname, item.getItem());

    }

    public TimeZone getImplicitTimeZone() throws XQException {
        checkNotClosed();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setImplicitTimeZone(TimeZone implicitTimeZone) throws XQException {
        checkNotClosed();
        //To change body of implemented methods use File | Settings | File Templates.
    }


    private void bindExternalVariable(QName varName, ValueRepresentation value) {
        getDynamicContext().setParameterValue(getClarkName(varName), value);

    }


    private String getClarkName(QName qname) {
        String uri = qname.getNamespaceURI();
        return "{" + (uri == null ? "" : uri) + "}" + qname.getLocalPart();
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