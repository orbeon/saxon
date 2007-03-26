package org.orbeon.saxon.sxpath;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.Stripper;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.AllElementStripper;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceResolver;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.IndependentContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.List;

/**
 * This is a cut-down version of the XPathEvaluator in the org.orbeon.saxon.xpath package. It provides
 * much of the same functionality, but without any dependencies on the JAXP 1.3 interfaces, which
 * are not available in JDK 1.4. The main restrictions are that it does not support mechanisms for
 * defining variables or functions.
  *
  * @author Michael H. Kay
  */

public class XPathEvaluator {

    private IndependentContext staticContext;
    private boolean stripSpace = false;

    /**
    * Default constructor. Creates an XPathEvaluator with a default configuration and name pool.
    */

    public XPathEvaluator() {
        this(new Configuration());
    }

    /**
     * Construct an XPathEvaluator with a specified configuration.
     * @param config the configuration to be used
     */
    public XPathEvaluator(Configuration config) {
        staticContext = new IndependentContext(config);
    }

    /**
     * Get the Configuration in use
     */

    public Configuration getConfiguration() {
        return staticContext.getConfiguration();
    }

    /**
    * Indicate whether all whitespace text nodes in the source document are to be
    * removed.
    * @param strip True if all whitespace text nodes are to be stripped from the source document,
    * false otherwise. The default if the method is not called is false.
    */

    public void setStripSpace(boolean strip) {
        stripSpace = strip;
    }

    /**
    * Build a source document.
     * @param source a JAXP Source object. This may be any implementation of Source that Saxon recognizes:
     * not only the standard kinds of source such as StreamSource, SAXSource, and DOMSource, but also for
     * example a JDOM or XOM DocumentWrapper.
     * @return the NodeInfo representing the root of the constructed tree.
     * @throws XPathException if, for example, XML parsing fails.
    */

    public NodeInfo build(Source source) throws XPathException {
        NamePool pool;
        if (source instanceof NodeInfo) {
            pool = ((NodeInfo)source).getNamePool();
        } else {
            pool = NamePool.getDefaultNamePool();
        }
        Stripper stripper = null;
        if (stripSpace) {
            stripper = AllElementStripper.getInstance();
        }
        Configuration config = new Configuration();
        config.setNamePool(pool);
        return Builder.build(source, stripper, config);
    }

    /**
    * Set the static context for compiling XPath expressions. This provides control over the
    * environment in which the expression is compiled, for example it allows namespace prefixes to
    * be declared, variables to be bound and functions to be defined. For most purposes, the static
    * context can be defined by providing and tailoring an instance of the IndependentContext class.
    * Until this method is called, a default static context is used, in which no namespaces are defined
    * other than the standard ones (xml, xslt, and saxon), and no variables or functions (other than the
    * core XPath functions) are available.
    */

    public void setStaticContext(IndependentContext context) {
        staticContext = context;
    }

    /**
    * Get the current static context. This will always return a value; if no static context has been
     * supplied by the user, the system creates its own.
    */

    public IndependentContext getStaticContext() {
        return staticContext;
    }

    /**
    * Prepare an XPath expression for subsequent evaluation.
    * @param expression The XPath expression to be evaluated, supplied as a string.
    * @return an XPathExpression object representing the prepared expression
    * @throws XPathException if the syntax of the expression is wrong, or if it references namespaces,
    * variables, or functions that have not been declared.
    */

    public XPathExpression createExpression(String expression) throws XPathException {
        Expression exp = ExpressionTool.make(expression, staticContext,0,-1,1);
        exp = exp.typeCheck(staticContext, Type.ITEM_TYPE);
        SlotManager map = staticContext.getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(exp, 0, map);
        XPathExpression xpe = new XPathExpression(this, exp);
        xpe.setStackFrameMap(map);
        return xpe;
    }

    /**
     * Set the external namespace resolver to be used. This overrides any namespaces declared directly
     * using declareNamespace on the staticContext object
     * @param namespaceContext The namespace context
     */

    public void setNamespaceResolver(NamespaceResolver namespaceContext) {
        staticContext.setNamespaceResolver(namespaceContext);
    }

    /**
     * Get the external namespace resolver, if one has been set using {@link #setNamespaceResolver}
     * @return the namespace context if set, or null otherwise
     */

    public NamespaceResolver getNamespaceResolver() {
        return staticContext.getNamespaceResolver();
    }

    /**
     * Set the default namespace for elements and types
     * @param uri The namespace to be used to qualify unprefixed element names and type names appearing
     * in the XPath expression.
     */

    public void setDefaultElementNamespace(String uri) {
        staticContext.setDefaultElementNamespace(uri);
    }

    /**
     * A simple command-line interface for the XPathEvaluator (not documented).
     * First parameter is the filename containing the source document, second
     * parameter is the XPath expression.
     */

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("format: java XPathEvaluator source.xml \"expression\"");
            return;
        }
        XPathEvaluator xpe = new XPathEvaluator();
        XPathExpression exp = xpe.createExpression(args[1]);
        List results = exp.evaluate(new StreamSource(new File(args[0])));
        for (int i = 0; i < results.size(); i++) {
            Object o = results.get(i);
            System.err.println(o);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Contributor(s):
//
