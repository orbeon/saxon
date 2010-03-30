package org.orbeon.saxon.xqj;

import org.orbeon.saxon.query.DynamicQueryContext;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.query.XQueryExpression;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQResultSequence;
import javax.xml.xquery.XQStaticContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Saxon implementation of the XQJ XQExpression interface
 */
public class SaxonXQExpression extends SaxonXQDynamicContext implements XQExpression {

    SaxonXQStaticContext sqc;
    DynamicQueryContext context;
    boolean closed;

    SaxonXQExpression(SaxonXQConnection connection) throws XQException {
        this.connection = connection;
        context = new DynamicQueryContext(connection.getConfiguration());
        sqc = (SaxonXQStaticContext)connection.getStaticContext();
        setClosableContainer(connection);
    }

    SaxonXQExpression(SaxonXQConnection connection, SaxonXQStaticContext staticContext) {
        this.connection = connection;
        context = new DynamicQueryContext(connection.getConfiguration());
        sqc = staticContext;
        setClosableContainer(connection);
    }

    protected DynamicQueryContext getDynamicContext() {
        return context;
    }

    protected SaxonXQDataFactory getDataFactory() throws XQException {
        return connection;
    }

    public void cancel() throws XQException {
        checkNotClosed();
        //
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
            StaticQueryContext env = sqc.getSaxonStaticQueryContext();
            XQueryExpression exp = env.compileQuery(query, null);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, sqc, context);
            return pe.executeQuery();
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        } catch (IOException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        }
    }

    public XQResultSequence executeQuery(Reader query) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext env = sqc.getSaxonStaticQueryContext();
            XQueryExpression exp = env.compileQuery(query);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, sqc, context);
            return pe.executeQuery();
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        } catch (IOException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        }
    }

    public XQResultSequence executeQuery(String query) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext env = sqc.getSaxonStaticQueryContext();
            XQueryExpression exp = env.compileQuery(query);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, sqc, context);
            XQResultSequence result = pe.executeQuery();
            ((Closable)result).setClosableContainer(this);
            return result;
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        }
    }

    public XQStaticContext getStaticContext() throws XQException {
        checkNotClosed();
        return connection.getStaticContext();
    }

    protected boolean externalVariableExists(QName name) {
        return true;
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