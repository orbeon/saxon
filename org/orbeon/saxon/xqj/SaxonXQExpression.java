package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.javax.xml.xquery.*;
import org.orbeon.saxon.query.DynamicQueryContext;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.query.XQueryExpression;
import org.orbeon.saxon.trans.XPathException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**

 */
public class SaxonXQExpression extends SaxonXQDynamicContext implements XQExpression {

    SaxonXQConnection connection;
    DynamicQueryContext context;
    Configuration config;
    boolean closed;

    SaxonXQExpression(SaxonXQConnection connection) {
        this.connection = connection;
        this.config = connection.getConfiguration();
        context = new DynamicQueryContext(config);
    }

    protected DynamicQueryContext getDynamicContext() {
        return context;
    }

    protected SaxonXQDataFactory getDataFactory() throws XQException {
        return connection;
    }

    protected void checkNotClosed() throws XQException {
        if (connection.isClosed()) {
            close();
        }
        if (isClosed()) {
            throw new XQException("Expression has been closed");
        }
    }

    public void cancel() throws XQException {
        checkNotClosed();
        //
    }

    public void clearWarnings() throws XQException {
        checkNotClosed();
        //
    }

    public void close() {
        closed = true;
    }

    public void executeCommand(Reader command) throws XQException {
        checkNotClosed();
        throw new XQException("Saxon does not recognize any non-XQuery commands");
    }

    public void executeCommand(String command) throws XQException {
        checkNotClosed();
        throw new XQException("Saxon does not recognize any non-XQuery commands");
    }

    public XQResultSequence executeQuery(InputStream query) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext sqc = new StaticQueryContext(config);
            XQueryExpression exp = sqc.compileQuery(query, null);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, context);
            return pe.executeQuery();
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        } catch (IOException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public XQResultSequence executeQuery(Reader query) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext sqc = new StaticQueryContext(config);
            XQueryExpression exp = sqc.compileQuery(query);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, context);
            return pe.executeQuery();
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        } catch (IOException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public XQResultSequence executeQuery(String query) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext sqc = new StaticQueryContext(config);
            XQueryExpression exp = sqc.compileQuery(query);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, context);
            return pe.executeQuery();
        } catch (XPathException e) {
            throw new XQException(e.getMessage(), e, null, null);
        }
    }

    public int getQueryLanguageTypeAndVersion() throws XQException {
        return XQConstants.LANGTYPE_XQUERY;
    }

    public int getQueryTimeout() throws XQException {
        checkNotClosed();
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public XQWarning getWarnings() throws XQException {
        checkNotClosed();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isClosed() {
        if (connection.isClosed()) {
            closed = true;
        }
        return closed;
    }

    public void setQueryTimeout(int seconds) throws XQException {
        checkNotClosed();
        //To change body of implemented methods use File | Settings | File Templates.
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