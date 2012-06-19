package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.query.DynamicQueryContext;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.query.XQueryExpression;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.xquery.*;
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
    private SaxonXQStaticContext staticContext;

    /**
     * Create an SaxonXQConnection from a SaxonXQDataSource
     * @param dataSource the data source.
     */
    SaxonXQConnection(SaxonXQDataSource dataSource) {
        config = dataSource.getConfiguration();
        staticContext = new SaxonXQStaticContext(config);
        init();
    }

    public Configuration getConfiguration() {
        return config;
    }

    public void commit() throws XQException {
        checkNotClosed();
    }

    public XQExpression createExpression() throws XQException {
        checkNotClosed();
        return new SaxonXQExpression(this);
    }

    public XQExpression createExpression(XQStaticContext properties) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(properties, "properties");
        return new SaxonXQExpression(this, (SaxonXQStaticContext)properties);
    }


    public boolean getAutoCommit() throws XQException {
        return false;
    }

    public XQMetaData getMetaData() throws XQException {
        checkNotClosed();
        return new SaxonXQMetaData(this);
    }


    public XQStaticContext getStaticContext() throws XQException {
        checkNotClosed();
        return staticContext;
    }

    public XQPreparedExpression prepareExpression(InputStream xquery) throws XQException {
        return prepareExpression(xquery, staticContext);
    }

    public XQPreparedExpression prepareExpression(InputStream xquery, XQStaticContext properties) throws XQException {
        checkNotClosed();
        try {
            SaxonXQStaticContext xqStaticContext = ((SaxonXQStaticContext)properties);
            StaticQueryContext sqc = xqStaticContext.getSaxonStaticQueryContext();
            XQueryExpression exp = sqc.compileQuery(xquery, null);
            DynamicQueryContext dqc = new DynamicQueryContext(config);
            return new SaxonXQPreparedExpression(this, exp, xqStaticContext, dqc);
        } catch (XPathException e) {
            throw newXQException(e);
        } catch (IOException e) {
            throw newXQException(e);
        } catch (NullPointerException e) {
            throw newXQException(e);
        }
    }

    public XQPreparedExpression prepareExpression(Reader xquery) throws XQException {
        return prepareExpression(xquery, staticContext);
    }

    public XQPreparedExpression prepareExpression(Reader xquery, XQStaticContext properties) throws XQException {
        checkNotClosed();
        try {
            SaxonXQStaticContext xqStaticContext = ((SaxonXQStaticContext)properties);
            StaticQueryContext sqc = xqStaticContext.getSaxonStaticQueryContext();
            XQueryExpression exp = sqc.compileQuery(xquery);
            DynamicQueryContext dqc = new DynamicQueryContext(config);
            return new SaxonXQPreparedExpression(this, exp, xqStaticContext, dqc);
        } catch (XPathException e) {
            throw newXQException(e);
        } catch (IOException e) {
            throw newXQException(e);
        } catch (NullPointerException e) {
            throw newXQException(e);
        }
    }

    public XQPreparedExpression prepareExpression(String xquery) throws XQException {
        return prepareExpression(xquery, staticContext);
    }

    public XQPreparedExpression prepareExpression(String xquery, XQStaticContext properties) throws XQException {
        checkNotClosed();
        try {
            SaxonXQStaticContext xqStaticContext = ((SaxonXQStaticContext)properties);
            StaticQueryContext sqc = xqStaticContext.getSaxonStaticQueryContext();
            XQueryExpression exp = sqc.compileQuery(xquery);
            DynamicQueryContext dqc = new DynamicQueryContext(config);
            return new SaxonXQPreparedExpression(this, exp, xqStaticContext, dqc);
        } catch (XPathException e) {
            throw newXQException(e);
        } catch (NullPointerException e) {
            throw newXQException(e);
        }
    }

    /**
     * Copy a prepared expression to create a new prepared expression. The prepared expression to be copied
     * may belong to a different connection. This method (which is a Saxon extension to the XQJ interface) allows
     * a query to be compiled once, and reused concurrently under multiple connections in multiple threads. The
     * compiled code of the existing query and its static context are shared with the original query, but a new
     * dynamic context is established, so that the two expressions can safely be used in parallel.
     * @param expression the XQPreparedExpression to be copied. This must have been created using Saxon, and it
     * must have been created with an XQConnection derived from the same XQDataSource as this connection.
     * @return a copy of the supplied expression, that can be used in a different connection or thread with its
     * own dynamic context. The new copy of the expression belongs to this connection, and can be used in the same
     * way as an expression created using any of the prepareExpression() methods on this class.
     * @throws XQException, for example if either of the connections has been closed
     */

    public XQPreparedExpression copyPreparedExpression(XQPreparedExpression expression) throws XQException {
        checkNotClosed();
        if (!(expression instanceof SaxonXQPreparedExpression)) {
            throw new IllegalArgumentException("Supplied expression must be compiled using Saxon");
        }
        XQueryExpression xqe = ((SaxonXQPreparedExpression)expression).getXQueryExpression();
        if (xqe.getExecutable().getConfiguration() != config) {
            throw new IllegalArgumentException("Supplied expression must derive from the same XQDataSource");
        }
        SaxonXQStaticContext sqc = ((SaxonXQPreparedExpression)expression).getSaxonXQStaticContext();
        DynamicQueryContext dqc = new DynamicQueryContext(config);
        return new SaxonXQPreparedExpression(this, xqe, sqc, dqc);
    }

    public void rollback() throws XQException {
        checkNotClosed();
        // no-op
    }


    public void setAutoCommit(boolean autoCommit) throws XQException {
        checkNotClosed();
        // no-op
    }

    public void setStaticContext(XQStaticContext properties) throws XQException {
        checkNotClosed();
        SaxonXQDataSource.checkNotNull(properties, "properties");
        staticContext = (SaxonXQStaticContext)properties;
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
// Contributor(s): Gunther Schadow (changes to allow access to public fields; also wrapping
// of extensions and mapping of null to empty sequence).
//