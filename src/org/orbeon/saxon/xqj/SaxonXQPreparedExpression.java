package org.orbeon.saxon.xqj;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.GlobalParam;
import org.orbeon.saxon.instruct.GlobalVariable;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.query.DynamicQueryContext;
import org.orbeon.saxon.query.XQueryExpression;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;

import javax.xml.namespace.QName;
import javax.xml.xquery.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Saxon implementation of the XQJ interface XQPreparedExpression. This represents a compiled XQuery
 * expression, together with the dynamic context for its evaluation. Note that this means the object
 * should not be used in more than one thread concurrently.
 * <p>
 * Note that an expression is scrollable or not depending on the scrollability property of the XQConnection
 * that was used to compile this expression (at the time it was compiled). If the expression is scrollable then
 * its results are delivered in an XQSequence that supports scrolling backwards as well as forwards.
 * <p>
 * For full Javadoc details, see the XQJ interface specification.
 */
public class SaxonXQPreparedExpression extends SaxonXQDynamicContext implements XQPreparedExpression {

    private XQueryExpression expression;
    private SaxonXQStaticContext staticContext;
    private DynamicQueryContext context;
    private boolean scrollable;

    protected SaxonXQPreparedExpression(SaxonXQConnection connection,
                                        XQueryExpression expression,
                                        SaxonXQStaticContext sqc,
                                        DynamicQueryContext context)
    throws XQException {
        this.connection = connection;
        this.expression = expression;
        this.staticContext = sqc;
        this.context = context;
        scrollable = sqc.getScrollability() == XQConstants.SCROLLTYPE_SCROLLABLE;
        setClosableContainer(connection);
    }

    protected DynamicQueryContext getDynamicContext() {
        return context;
    }

    protected SaxonXQConnection getConnection() {
        return connection;
    }

    protected SaxonXQDataFactory getDataFactory() throws XQException {
        if (connection.isClosed()) {
            close();
        }
        checkNotClosed();
        return connection;
    }

    protected XQueryExpression getXQueryExpression() {
        return expression;
    }

    protected SaxonXQStaticContext getSaxonXQStaticContext() {
        return staticContext;
    }

    public void cancel()  throws XQException {
        checkNotClosed();
    }

    public XQResultSequence executeQuery() throws XQException {
        checkNotClosed();
        try {
            SequenceIterator iter = expression.iterator(context);
            if (scrollable) {
                Value value = Value.asValue(SequenceExtent.makeSequenceExtent(iter));
                return new SaxonXQSequence(value, this);
            } else {
                return new SaxonXQForwardSequence(iter, this);
            }
        } catch (XPathException de) {
            XQException xqe = new XQException(de.getMessage());
            xqe.initCause(de);
            throw xqe;
        }
    }

    public QName[] getAllExternalVariables() throws XQException {
        checkNotClosed();
        HashMap vars = expression.getExecutable().getCompiledGlobalVariables();
        if (vars == null || vars.isEmpty()) {
            return EMPTY_QNAME_ARRAY;
        } else {
            HashSet params = new HashSet(vars.size());
            Iterator iter = vars.values().iterator();
            while (iter.hasNext()) {
                GlobalVariable var = (GlobalVariable)iter.next();
                if (var instanceof GlobalParam) {
                    params.add(var.getVariableQName());
                }
            }
            QName[] qnames = new QName[params.size()];
            int q=0;
            for (Iterator it=params.iterator(); it.hasNext();) {
                StructuredQName name = (StructuredQName)it.next();
                qnames[q++] = new QName(name.getNamespaceURI(), name.getLocalName(), name.getPrefix());
            }
            return qnames;
        }
    }

    private static QName[] EMPTY_QNAME_ARRAY = new QName[0];

    public QName[] getAllUnboundExternalVariables() throws XQException {
        checkNotClosed();
        Set boundParameters = getDynamicContext().getParameters().keySet();
        IntHashSet unbound = new IntHashSet(boundParameters.size());
        QName[] all = getAllExternalVariables();
        for (int i=0; i<all.length; i++) {
            String clark = "{" + all[i].getNamespaceURI() + "}" + all[i].getLocalPart();
            if (!boundParameters.contains(clark)) {
                unbound.add(i);
            }
        }
        QName[] unboundq = new QName[unbound.size()];
        int c = 0;
        IntIterator iter = unbound.iterator();
        while (iter.hasNext()) {
            int x = iter.next();
            unboundq[c++] = all[x];
        }
        return unboundq;
    }

    public XQStaticContext getStaticContext() throws XQException {
        checkNotClosed();
        return new SaxonXQExpressionContext(expression);
    }

    public XQSequenceType getStaticResultType() throws XQException {
        checkNotClosed();
        Expression exp = expression.getExpression();    // unwrap two layers!
        ItemType itemType = exp.getItemType(connection.getConfiguration().getTypeHierarchy());
        int cardinality = exp.getCardinality();
        SequenceType staticType = SequenceType.makeSequenceType(itemType, cardinality);
        return new SaxonXQSequenceType(staticType, connection.getConfiguration());
    }

    public XQSequenceType getStaticVariableType(QName name) throws XQException {
        checkNotClosed();
        checkNotNull(name);
        StructuredQName qn = new StructuredQName(
                name.getPrefix(), name.getNamespaceURI(), name.getLocalPart());
        HashMap vars = expression.getExecutable().getCompiledGlobalVariables();
        GlobalVariable var = (vars == null ? null : (GlobalVariable)vars.get(qn));
        if (var == null) {
            throw new XQException("Variable " + name + " is not declared");
        }
        return new SaxonXQSequenceType(var.getRequiredType(), connection.getConfiguration());
    }


    protected boolean externalVariableExists(QName name) {
        StructuredQName qn = new StructuredQName(
                name.getPrefix(), name.getNamespaceURI(), name.getLocalPart());
        HashMap vars = expression.getExecutable().getCompiledGlobalVariables();
        GlobalVariable var = (vars == null ? null : (GlobalVariable)vars.get(qn));
        return var != null && var instanceof GlobalParam;
    }

    private void checkNotNull(Object arg) throws XQException {
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