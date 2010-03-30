package org.orbeon.saxon.s9api;

import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.sxpath.IndependentContext;
import org.orbeon.saxon.sxpath.XPathEvaluator;
import org.orbeon.saxon.sxpath.XPathExpression;
import org.orbeon.saxon.sxpath.XPathVariable;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * An XPathCompiler object allows XPath queries to be compiled. The compiler holds information that
 * represents the static context for an XPath expression.
 *
 * <p>To construct an XPathCompiler, use the factory method
 * {@link Processor#newXPathCompiler}.</p>
 *
 * <p>An XPathCompiler may be used repeatedly to compile multiple
 * queries. Any changes made to the XPathCompiler (that is, to the
 * static context) do not affect queries that have already been compiled.
 * An XPathCompiler may be used concurrently in multiple threads, but
 * it should not then be modified once initialized.</p>
 *
 * <p>Changes to an XPathCompiler are cumulative. There is no simple way to reset
 * the XPathCompiler to its initial state; instead, simply create a new
 * XPathCompiler.</p>
 *
 * @since 9.0
 */

public class XPathCompiler {

    private Processor processor;
    private IndependentContext env;
    private ArrayList<XPathVariable> declaredVariables = new ArrayList<XPathVariable>();

    /**
     * Protected constructor
     * @param processor the s9api Processor
     */

    protected XPathCompiler(Processor processor) {
        this.processor = processor;
        env = new IndependentContext(processor.getUnderlyingConfiguration());
    }

    /**
     * Set whether XPath 1.0 backwards compatibility mode is to be used. In backwards compatibility
     * mode, more implicit type conversions are allowed in XPath expressions, for example it
     * is possible to compare a number with a string. The default is false (backwards compatibility
     * mode is off).
     *
     * @param option true if XPath 1.0 backwards compatibility is to be enabled, false if it is to
     *               be disabled.
     */

    public void setBackwardsCompatible(boolean option) {
        env.setBackwardsCompatibilityMode(option);
    }

    /**
     * Ask whether XPath 1.0 backwards compatibility mode is in force.
     *
     * @return true if XPath 1.0 backwards compatibility is enabled, false if it is disabled.
     */

    public boolean isBackwardsCompatible() {
        return env.isInBackwardsCompatibleMode();
    }

    /**
     * Set the static base URI for XPath expressions compiled using this XPathCompiler. The base URI
     * is part of the static context, and is used to resolve any relative URIs appearing within an XPath
     * expression, for example a relative URI passed as an argument to the doc() function. If no
     * static base URI is supplied, then the current working directory is used.
     *
     * @param uri the base URI to be set in the static context. This must be an absolute URI.
     */

    public void setBaseURI(URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("Supplied base URI must be absolute");
        }
        env.setBaseURI(uri.toString());
    }

    /**
     * Get the static base URI for XPath expressions compiled using this XPathCompiler. The base URI
     * is part of the static context, and is used to resolve any relative URIs appearing within an XPath
     * expression, for example a relative URI passed as an argument to the doc() function. If no
     * static base URI has been explicitly set, this method returns null.
     *
     * @return the base URI from the static context
     */

    public URI getBaseURI() {
        try {
            return new URI(env.getBaseURI());
        } catch (URISyntaxException err) {
            throw new IllegalStateException(err);
        }
    }

    /**
     * Declare a namespace binding as part of the static context for XPath expressions compiled using this
     * XPathCompiler
     *
     * @param prefix The namespace prefix. If the value is a zero-length string, this method sets the default
     *               namespace for elements and types.
     * @param uri    The namespace URI. It is possible to specify a zero-length string to "undeclare" a namespace;
     *               in this case the prefix will not be available for use, except in the case where the prefix
     *               is also a zero length string, in which case the absence of a prefix implies that the name
     *               is in no namespace.
     * @throws NullPointerException if either the prefix or uri is null.
     */

    public void declareNamespace(String prefix, String uri) {
        env.declareNamespace(prefix, uri);
    }

    /**
     * Import a schema namespace: that is, add the element and attribute declarations and type definitions
     * contained in a given namespace to the static context for the XPath expression.
     *
     * <p>This method will not cause the schema to be loaded. That must be done separately, using the
     * {@link SchemaManager}. This method will not fail if the schema has not been loaded (but in that case
     * the set of declarations and definitions made available to the XPath expression is empty). The schema
     * document for the specified namespace may be loaded before or after this method is called.</p>
     *
     * <p>This method does not bind a prefix to the namespace. That must be done separately, using the
     * {@link #declareNamespace(String, String)} method.</p>
     *
     * @param uri   The schema namespace to be imported. To import declarations in a no-namespace schema,
     * supply a zero-length string.
     * @since 9.1
     */

    public void importSchemaNamespace(String uri) {
        env.getImportedSchemaNamespaces().add(uri);
    }
    

    /**
     * Declare a variable as part of the static context for XPath expressions compiled using this
     * XPathCompiler. It is an error for the XPath expression to refer to a variable unless it has been
     * declared. This method declares the existence of the variable, but it does not
     * bind any value to the variable; that is done later, when the XPath expression is evaluated.
     * The variable is allowed to have any type (that is, the required type is <code>item()*</code>).
     *
     * @param qname The name of the variable, expressions as a QName
     */

    public void declareVariable(QName qname) {
        XPathVariable var = env.declareVariable(qname.getNamespaceURI(), qname.getLocalName());
        declaredVariables.add(var);
    }

    /**
     * Declare a variable as part of the static context for XPath expressions compiled using this
     * XPathCompiler. It is an error for the XPath expression to refer to a variable unless it has been
     * declared. This method declares the existence of the variable, and defines the required type
     * of the variable, but it does not bind any value to the variable; that is done later,
     * when the XPath expression is evaluated.
     *
     * @param qname The name of the variable, expressed as a QName
     * @param itemType The required item type of the value of the variable
     * @param occurrences The allowed number of items in the sequence forming the value of the variable
     * @throws SaxonApiException if the requiredType is syntactically invalid or if it refers to namespace
     * prefixes or schema components that are not present in the static context
     */

    public void declareVariable(QName qname, ItemType itemType, OccurrenceIndicator occurrences) throws SaxonApiException {
        XPathVariable var = env.declareVariable(qname.getNamespaceURI(), qname.getLocalName());
        var.setRequiredType(
                SequenceType.makeSequenceType(
                        itemType.getUnderlyingItemType(), occurrences.getCardinality()));
        declaredVariables.add(var);
    }


    /**
     * Compile an XPath expression, supplied as a character string.
     *
     * @param source A string containing the source text of the XPath expression
     * @return An XPathExecutable which represents the compiled xpath expression object.
     *         The XPathExecutable may be run as many times as required, in the same or a different thread.
     *         The XPathExecutable is not affected by any changes made to the XPathCompiler once it has been compiled.
     * @throws SaxonApiException if any static error is detected while analyzing the expression
     */

    public XPathExecutable compile(String source) throws SaxonApiException {
        try {
            XPathEvaluator eval = new XPathEvaluator(processor.getUnderlyingConfiguration());
            eval.setStaticContext(env);
            XPathExpression cexp = eval.createExpression(source);
            return new XPathExecutable(cexp, processor, env, declaredVariables);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Compile an XSLT 2.0 pattern, supplied as a character string. The compiled pattern behaves as a boolean
     * expression which, when evaluated in a particular context, returns true if the context node matches
     * the pattern, and false if it does not. An error is reported if there is no context item or it the context
     * item is not a node.
     * @param source A string conforming to the syntax of XSLT 2.0 patterns
     * @return An XPathExecutable representing an expression which evaluates to true when the context node matches
     * the pattern, and false when it does not.
     * @throws SaxonApiException if the pattern contains static errors: for example, if its syntax is incorrect,
     * or if it refers to undeclared variables or namespaces
     * @since 9.1
     */

    public XPathExecutable compilePattern(String source) throws SaxonApiException {
        try {
            XPathEvaluator eval = new XPathEvaluator(processor.getUnderlyingConfiguration());
            eval.setStaticContext(env);
            XPathExpression cexp = eval.createPattern(source);
            return new XPathExecutable(cexp, processor, env, declaredVariables);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Escape-hatch method to get the underlying static context object used by the implementation.
     * @return the underlying static context object. In the current implementation this will always
     * be an instance of {@link IndependentContext}.
     *
     * <p>This method provides an escape hatch to internal Saxon implementation objects that offer a finer and
     * lower-level degree of control than the s9api classes and methods. Some of these classes and methods may change
     * from release to release.</p>
     * @since 9.1
     */

    public StaticContext getUnderlyingStaticContext() {
        return env;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

