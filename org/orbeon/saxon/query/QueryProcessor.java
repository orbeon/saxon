package net.sf.saxon.query;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Stripper;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.xpath.XPathException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import java.io.IOException;
import java.io.Reader;

/**
 * A QueryProcessor allows the compilation of XQuery queries for subsequent
 * execution. Compilation consists of parsing, static analysis, optimization,
 * and creation of an internal representation of the query for subsequent
 * evaluation.
 *
 * <p>The compilation of a query takes context information from a static context,
 * which is represented by a StaticQueryContext object. This provides the necessary
 * information about collations, base URI, etc.</p>
 *
 * <p>This class is retained for backwards compatibility, but there is no longer
 * any need for applications to use it directly, since all its methods are available
 * directly from the StaticQueryContext object.</p>
 *
 * @deprecated since Saxon 8.1: use methods on the StaticQueryContext directly
 */

public class QueryProcessor {

    private StaticQueryContext staticContext;
    private Configuration config;

    /**
     * Default constructor.
     * @param env the static context for queries. Note that a StaticQueryContext object may be initialized with
     * context information that the query can use, but it is also modified when a query is compiled against
     * it: for example, namespaces, variables, and functions declared in the query prolog are registered in
     * the static context. Therefore, it is not a good idea to use a StaticQueryContext object to compile more
     * than one query.
     * @deprecated since Saxon 8.1: use methods on the StaticQueryContext directly
    */

    public QueryProcessor(StaticQueryContext env) {
        config = env.getConfiguration();
        config.setHostLanguage(Configuration.XQUERY);
        config.setRecoveryPolicy(Configuration.DO_NOT_RECOVER);
        this.staticContext = env;
    }

    /**
     * Convenience method for building Saxon's internal representation of a source XML
     * document. The document will be built using the default NamePool, which means that
     * any process that uses it must also use the default NamePool.
     * @param source Any javax.xml.transform.Source object representing the document against
     * which queries will be executed. Note that a Saxon {@link net.sf.saxon.om.DocumentInfo DocumentInfo}
     * (indeed any {@link net.sf.saxon.om.NodeInfo NodeInfo})
     * can be used as a Source. To use a third-party DOM Document as a source, create an instance of
     * {@link javax.xml.transform.dom.DOMSource DOMSource} to wrap it.
     * <p>For additional control over the way in which the source document is processed,
     * supply an {@link net.sf.saxon.AugmentedSource AugmentedSource} object and set appropriate options on the object.</p>
     * @return the DocumentInfo representing the root node of the resulting document object.
     */

    public DocumentInfo buildDocument(Source source) throws XPathException {
        Stripper stripper = null;
        if (config.isStripsAllWhiteSpace()) {
            stripper = AllElementStripper.getInstance();
            stripper.setStripAll();
        }
        try {
            NodeInfo contextNode = Builder.build(source, stripper, config);
            return contextNode.getDocumentRoot();
        } catch (XPathException err) {
            Throwable cause = err.getException();
            if (cause != null && cause instanceof SAXParseException) {
                // This generally means the error was already reported.
                // But if a RuntimeException occurs in Saxon during a callback from
                // the Crimson parser, Crimson wraps this in a SAXParseException without
                // reporting it further.
                SAXParseException spe = (SAXParseException)cause;
                cause = spe.getException();
                if (cause instanceof RuntimeException) {
                    try {
                        config.getErrorListener().fatalError(err);
                    } catch (TransformerException e) {
                        //
                    }
                }
            } else {
                while (err.getException() instanceof XPathException) {
                    err = (XPathException)err.getException();
                }
                try {
                    config.getErrorListener().fatalError(err);
                } catch (TransformerException e) {
                    //
                }
            }
            throw err;
        }
    }

    /**
    * Set the static context for compiling XQuery expressions. This provides control over the
    * environment in which the query is compiled, for example it allows namespace prefixes to
    * be declared, variables to be bound and functions to be defined. For most purposes, the static
    * context can be defined by providing and tailoring an instance of the StandaloneContext class.
    * Until this method is called, a default query context is used, in which no namespaces are defined
    * other than the standard ones (xml, xslt, and saxon), and no variables or functions (other than the
    * core XPath functions) are available.
    */

    public void setStaticContext(StaticQueryContext context) {
        staticContext = context;
    }

    /**
    * Get the current static context
    */

    public StaticQueryContext getStaticContext() {
        return staticContext;
    }

    /**
     * Set the configuration. The configuration contains parameters that affect the
     * way the query is compiled and executed.
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the configuration used by this QueryProcessor
     */

    public Configuration getConfiguration() {
        return config;
    }
    /**
     * Prepare an XQuery query for subsequent evaluation. The source text of the query
     * is supplied as a String. The base URI of the query is taken from the static context,
     * and defaults to the current working directory.
     * @param query The XQuery query to be evaluated, supplied as a string.
     * @return an XQueryExpression object representing the prepared expression
     * @throws net.sf.saxon.xpath.XPathException if the syntax of the expression is wrong,
     * or if it references namespaces, variables, or functions that have not been declared,
     * or contains other static errors.
    */

    public XQueryExpression compileQuery(String query)
    throws XPathException {

        QueryParser qp = new QueryParser();
        XQueryExpression queryExp = qp.makeXQueryExpression(query, staticContext, config);
        return queryExp;
    }

    /**
    * Prepare an XQuery query for subsequent evaluation. The Query is supplied
     * in the form of a Reader. The base URI of the query is taken from the static context,
     * and defaults to the current working directory.
     * @param source A Reader giving access to the text of the XQuery query to be compiled.
     * @return an XPathExpression object representing the prepared expression.
     * @throws net.sf.saxon.xpath.XPathException if the syntax of the expression is wrong, or if it references namespaces,
     * variables, or functions that have not been declared, or any other static error is reported.
     * @throws java.io.IOException if a failure occurs reading the supplied input.
    */

    public XQueryExpression compileQuery(Reader source)
    throws XPathException, IOException {
        char[] buffer = new char[4096];
        StringBuffer sb = new StringBuffer(4096);
        while (true) {
            int n = source.read(buffer);
            if (n>0) {
                sb.append(buffer, 0, n);
            } else {
                break;
            }
        }
        return compileQuery(sb.toString());
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//