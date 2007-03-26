package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.javax.xml.xquery.*;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.query.DynamicQueryContext;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.query.XQueryExpression;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Saxon implementation of the XQL interface XQConnection. This interface represents a
 * "connection" between an XQuery application and an XQuery server. In Saxon the client
 * and server run in the same process so the concept of a connection is rather notional,
 * and some of the properties have little meaning. However, the connection is the factory
 * object used to compile queries.
 * <p>
 * For Javadoc descriptions of the public methors, see the XQJ documentation.
 */
public class SaxonXQConnection extends SaxonXQDataFactory implements XQConnection {

    private Configuration config;
    private XQCommonHandler commonHandler;
    private boolean closed;
    private int holdability = XQConstants.HOLDTYPE_HOLD_CURSORS_OVER_COMMIT;
    private int scrollability = XQConstants.SCROLLTYPE_FORWARD_ONLY;

    /**
     * Create an SaxonXQConnection from a SaxonXQDataSource
     * @param dataSource the data source.
     */
    SaxonXQConnection(SaxonXQDataSource dataSource) {
        this.config = dataSource.getConfiguration();
        this.commonHandler = dataSource.getCommonHandler();
    }

    public Configuration getConfiguration() {
        return config;
    }

    public XQCommonHandler getCommonHandler() {
        return commonHandler;
    }

    public void clearWarnings() throws XQException {
        checkNotClosed();
        // TODO: clearWarnings
    }

    public void close() {
        closed = true;
    }

    public void commit() throws XQException {
        checkNotClosed();
        // no-op
    }

    public XQExpression createExpression() throws XQException {
        checkNotClosed();
        return new SaxonXQExpression(this);
    }

    public int getHoldability() throws XQException {
        checkNotClosed();
        return holdability;
    }

    public XQMetaData getMetaData() throws XQException {
        checkNotClosed();
        throw new UnsupportedOperationException("Metadata is not yet implemented");
        //TODO: metaData (not yet implemented: still an open issue in the spec)
    }

    public String getMetaDataProperty(String key)  throws XQException{
        checkNotClosed();
        throw new UnsupportedOperationException("Metadata is not yet implemented");
        //TODO: metaData (not yet implemented: still an open issue in the spec)
    }

    public int getQueryLanguageTypeAndVersion() throws XQException {
        checkNotClosed();
        return XQConstants.LANGTYPE_XQUERY;
    }

    public int getScrollability()  throws XQException {
        checkNotClosed();
        return scrollability;
    }

    public String[] getSupportedMetaDataPropertyNames()  throws XQException {
        checkNotClosed();
        throw new UnsupportedOperationException("Metadata is not yet implemented");
        //TODO: metaData (not yet implemented: still an open issue in the spec)
    }

    public int getUpdatability()  throws XQException{
         checkNotClosed();
        return XQConstants.RESULTTYPE_READ_ONLY;
    }

    public XQWarning getWarnings() throws XQException {
        checkNotClosed();
        return null;  //TODO: warnings
    }

    public boolean isClosed() {
        return closed;
    }

    public XQPreparedExpression prepareExpression(InputStream xquery) throws XQException {
        return prepareExpression(xquery, null);
    }

    public XQPreparedExpression prepareExpression(InputStream xquery, XQItemType contextItemType) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext sqc = new StaticQueryContext(config);
            if (contextItemType != null) {
                sqc.setRequiredContextItemType(((SaxonXQItemType)contextItemType).getSaxonItemType());
            }
            XQueryExpression exp = sqc.compileQuery(xquery, null);
            DynamicQueryContext dqc = new DynamicQueryContext(config);
            return new SaxonXQPreparedExpression(this, exp, dqc);
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        } catch (IOException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public XQPreparedExpression prepareExpression(Reader xquery) throws XQException {
        return prepareExpression(xquery, null);
    }

    public XQPreparedExpression prepareExpression(Reader xquery, XQItemType contextItemType) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext sqc = new StaticQueryContext(config);
            if (contextItemType != null) {
                sqc.setRequiredContextItemType(((SaxonXQItemType)contextItemType).getSaxonItemType());
            }
            XQueryExpression exp = sqc.compileQuery(xquery);
            DynamicQueryContext dqc = new DynamicQueryContext(config);
            return new SaxonXQPreparedExpression(this, exp, dqc);
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        } catch (IOException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public XQPreparedExpression prepareExpression(String xquery) throws XQException {
        return prepareExpression(xquery, null);
    }

    public XQPreparedExpression prepareExpression(String xquery, XQItemType contextItemType) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext sqc = new StaticQueryContext(config);
            if (contextItemType != null) {
                sqc.setRequiredContextItemType(((SaxonXQItemType)contextItemType).getSaxonItemType());
            }
            XQueryExpression exp = sqc.compileQuery(xquery);
            DynamicQueryContext dqc = new DynamicQueryContext(config);
            return new SaxonXQPreparedExpression(this, exp, dqc);
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public void rollback() throws XQException {
        checkNotClosed();
        // no-op
    }

    public void setCommonHandler(XQCommonHandler handler) {
        commonHandler = handler;
    }

    public void setHoldability(int holdability) throws XQException {
        checkNotClosed();
        switch (holdability) {
            case XQConstants.HOLDTYPE_HOLD_CURSORS_OVER_COMMIT:
            case XQConstants.HOLDTYPE_CLOSE_CURSORS_AT_COMMIT:
                this.holdability = holdability;
            default:
                throw new XQException("Invalid holdability value - " + holdability);
        }
    }

    public void setQueryLanguageTypeAndVersion(int langtype) throws XQException {
        checkNotClosed();
        if (langtype != XQConstants.LANGTYPE_XQUERY) {
            throw new XQException("XQueryX is not supported");
        }
    }

    public void setScrollability(int scrollability) throws XQException {
        checkNotClosed();
        switch (scrollability) {
            case XQConstants.SCROLLTYPE_FORWARD_ONLY:
            case XQConstants.SCROLLTYPE_SCROLLABLE:
                this.scrollability = scrollability;
            default:
                throw new XQException("Invalid scrollability value - " + scrollability);
        }
    }

    public void setUpdatability(int updatability) throws XQException {
        checkNotClosed();
        if (updatability != XQConstants.RESULTTYPE_READ_ONLY) {
            throw new XQException("Query must be read-only");
        }
    }

    public String getBaseURI() throws XQException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getBoundarySpacePolicy() throws XQException {
        return XQConstants.BOUNDARY_SPACE_STRIP;
    }

    public int getConstructionMode() throws XQException {
        return XQConstants.CONSTRUCTION_MODE_STRIP;
    }

    public int getCopyNamespacesModeInherit() throws XQException {
        return XQConstants.COPY_NAMESPACES_MODE_INHERIT;
    }

    public int getCopyNamespacesModePreserve() throws XQException {
        return XQConstants.COPY_NAMESPACES_MODE_PRESERVE;
    }

    public String getDefaultCollation() throws XQException {
        return NamespaceConstant.CODEPOINT_COLLATION_URI;
    }

    public String getDefaultElementTypeNamespace() throws XQException {
        return "";
    }

    public String getDefaultFunctionNamespace() throws XQException {
        return NamespaceConstant.FN;
    }

    public int getDefaultOrderForEmptySequences() throws XQException {
        return XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_LEAST;
    }

    public String[] getInScopeNamespacePrefixes() throws XQException {
        String[] prefixes = {"xs", "xdt", "local", "xsi", "xml", "fn"};
        return prefixes;
    }

    public String getNamespaceURI(String prefix) throws XQException {
        if (prefix.equals("xs")) {
            return NamespaceConstant.SCHEMA;
        } else if (prefix.equals("xdt")) {
            return NamespaceConstant.XDT;
        } else if (prefix.equals("xsi")) {
            return NamespaceConstant.SCHEMA_INSTANCE;
        } else if (prefix.equals("xml")) {
            return NamespaceConstant.XML;
        } else if (prefix.equals("fn")) {
            return NamespaceConstant.FN;
        } else {
            throw new XQException("No namespace is bound to prefix " + prefix);
        }
    }

    public int getOrderingMode() throws XQException {
        return XQConstants.ORDERING_MODE_ORDERED;
    }

    public QName[] getStaticInScopeVariableNames() throws XQException {
        return new QName[0];
    }

    public XQSequenceType getStaticInScopeVariableType(QName varname) throws XQException {
        return null;
    }

    private void checkNotClosed() throws XQException {
        if (closed) {
            throw new XQException("Connection has been closed");
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
// Contributor(s): Gunther Schadow (changes to allow access to public fields; also wrapping
// of extensions and mapping of null to empty sequence).
//