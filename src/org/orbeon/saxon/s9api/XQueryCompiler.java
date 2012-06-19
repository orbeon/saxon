package org.orbeon.saxon.s9api;

import org.orbeon.saxon.query.ModuleURIResolver;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.ErrorListener;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *  An XQueryCompiler object allows XQuery 1.0 queries to be compiled. The compiler holds information that
 *  represents the static context for the compilation.
 *
  * <p>To construct an XQueryCompiler, use the factory method {@link Processor#newXQueryCompiler}.</p>
 *
 * <p>An XQueryCompiler may be used repeatedly to compile multiple queries. Any changes made to the
 * XQueryCompiler (that is, to the static context) do not affect queries that have already been compiled.
 * An XQueryCompiler may be used concurrently in multiple threads, but it should not then be modified once
 * initialized.</p>
 *
 * @since 9.0
 */

public class XQueryCompiler {

    private Processor processor;
    private StaticQueryContext env;
    private ItemType requiredContextItemType;
    private String encoding;

    /**
     * Protected constructor
     * @param processor the Saxon Processor
     */

    protected XQueryCompiler(Processor processor) {
        this.processor = processor;
        this.env = new StaticQueryContext(processor.getUnderlyingConfiguration());
    }

    /**
     * Set the static base URI for the query
     * @param baseURI the static base URI
     */

    public void setBaseURI(URI baseURI) {
        if (!baseURI.isAbsolute()) {
            throw new IllegalArgumentException("Base URI must be an absolute URI");
        }
        env.setBaseURI(baseURI.toString());
    }

    /**
     * Get the static base URI for the query
     * @return the static base URI
     */

    public URI getBaseURI() {
        try {
            return new URI(env.getBaseURI());
        } catch (URISyntaxException err) {
            throw new IllegalStateException(err);
        }
    }

    /**
     * Set the ErrorListener to be used during this query compilation episode
     * @param listener The error listener to be used. This is notified of all errors detected during the
     * compilation.
     */

    public void setErrorListener(ErrorListener listener) {
        env.setErrorListener(listener);
    }

    /**
     * Get the ErrorListener being used during this compilation episode
     * @return listener The error listener in use. This is notified of all errors detected during the
     * compilation. If no user-supplied ErrorListener has been set, returns the system-supplied
     * ErrorListener.
     */

    public ErrorListener getErrorListener() {
        return env.getErrorListener();
    }

    /**
     * Set whether trace hooks are to be included in the compiled code. To use tracing, it is necessary
     * both to compile the code with trace hooks included, and to supply a TraceListener at run-time
     * @param option true if trace code is to be compiled in, false otherwise
     */

    public void setCompileWithTracing(boolean option) {
        env.setCompileWithTracing(option);
    }

    /**
     * Ask whether trace hooks are included in the compiled code.
     * @return true if trace hooks are included, false if not.
     */

    public boolean isCompileWithTracing() {
        return env.isCompileWithTracing();
    }

    /**
     * Set a user-defined ModuleURIResolver for resolving URIs used in <code>import module</code>
     * declarations in the XQuery prolog.
     * This will override any ModuleURIResolver that was specified as part of the configuration.
     * @param resolver the ModuleURIResolver to be used
     */

    public void setModuleURIResolver(ModuleURIResolver resolver) {
        env.setModuleURIResolver(resolver);
    }

    /**
     * Get the user-defined ModuleURIResolver for resolving URIs used in <code>import module</code>
     * declarations in the XQuery prolog; returns null if none has been explicitly set either
     * here or in the Saxon Configuration.
     * @return the registered ModuleURIResolver
     */

    public ModuleURIResolver getModuleURIResolver() {
        return env.getModuleURIResolver();
    }

    /**
     * Set the encoding of the supplied query. This is ignored if the query is supplied
     * in character form, that is, as a <code>String</code> or as a <code>Reader</code>. If no value
     * is set, the query processor will attempt to infer the encoding, defaulting to UTF-8 if no
     * information is available.
     * @param encoding the encoding of the supplied query, for example "iso-8859-1"
     * @since 9.1
     */

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Get the encoding of the supplied query.
     * @param encoding the encoding previously set using {@link #setEncoding(String)}, or null
     * if no value has been set
     * @since 9.1
     */

    /**
     * Say whether the query is allowed to be updating. XQuery update syntax will be rejected
     * during query compilation unless this flag is set. XQuery Update is supported only under Saxon-SA.
     * @param updating true if the query is allowed to use the XQuery Update facility
     * (requires Saxon-SA). If set to false, the query must not be an updating query. If set
     * to true, it may be either an updating or a non-updating query.
     * @since 9.1
     */

    public void setUpdatingEnabled(boolean updating) {
        env.setUpdatingEnabled(updating);
    }

    /**
     * Ask whether the query is allowed to use XQuery Update syntax
     * @return true if the query is allowed to use the XQuery Update facility. Note that this
     * does not necessarily mean that the query is an updating query; but if the value is false,
     * the it must definitely be non-updating.
     * @since 9.1
     */

    public boolean isUpdatingEnabled() {
        return env.isUpdatingEnabled();
    }


    /**
     * Declare a namespace binding as part of the static context for queries compiled using this
     * XQueryCompiler. This binding may be overridden by a binding that appears in the query prolog.
     * The namespace binding will form part of the static context of the query, but it will not be copied
     * into result trees unless the prefix is actually used in an element or attribute name.
     *
     * @param prefix The namespace prefix. If the value is a zero-length string, this method sets the default
     *               namespace for elements and types.
     * @param uri    The namespace URI. It is possible to specify a zero-length string to "undeclare" a namespace;
     *               in this case the prefix will not be available for use, except in the case where the prefix
     *               is also a zero length string, in which case the absence of a prefix implies that the name
     *               is in no namespace.
     * @throws NullPointerException if either the prefix or uri is null.
     * @throws IllegalArgumentException in the event of an invalid declaration of the XML namespace
     */

    public void declareNamespace(String prefix, String uri) {
        env.declareNamespace(prefix, uri);
    }

    /**
     * Declare the static type of the context item. If this type is declared, and if a context item
     * is supplied when the query is invoked, then the context item must conform to this type (no
     * type conversion will take place to force it into this type).
     * @param type the required type of the context item
     */

    public void setRequiredContextItemType(ItemType type) {
        requiredContextItemType = type;
        env.setRequiredContextItemType(type.getUnderlyingItemType());
    }

    /**
     * Get the required type of the context item. If no type has been explicitly declared for the context
     * item, an instance of AnyItemType (representing the type item()) is returned.
     * @return the required type of the context item
     */

    public ItemType getRequiredContextItemType() {
        return requiredContextItemType;
    }

    /**
     * Compile a query supplied as a string.
     * <p>The base URI of the query should be supplied by calling {@link #setBaseURI(java.net.URI)} </p>
     * @param query the text of the query
     * @return an XQueryExecutable representing the compiled query
     * @throws SaxonApiException if the query compilation fails with a static error
     */

    public XQueryExecutable compile(String query) throws SaxonApiException {
        try {
            return new XQueryExecutable(processor, env.compileQuery(query));
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Compile a query supplied as a file
     * @param query the file containing the query. The URI corresponding to this file will be used as the
     * base URI of the query, overriding any URI supplied using {@link #setBaseURI(java.net.URI)} (but not
     * overriding any base URI specified within the query prolog)
     * @return an XQueryExecutable representing the compiled query
     * @throws SaxonApiException if the query compilation fails with a static error
     * @throws IOException if the file does not exist or cannot be read
     * @since 9.1
     */

    public XQueryExecutable compile(File query) throws SaxonApiException, IOException {
        try {
            String savedBaseUri = env.getBaseURI();
            env.setBaseURI(query.toURI().toString());
            XQueryExecutable exec =
                    new XQueryExecutable(processor, env.compileQuery(new FileInputStream(query), encoding));
            env.setBaseURI(savedBaseUri);
            return exec;
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Compile a query supplied as an InputStream
     * <p>The base URI of the query should be supplied by calling {@link #setBaseURI(java.net.URI)} </p>
     * @param query the input stream on which the query is supplied. This will be consumed by this method
     * @return an XQueryExecutable representing the compiled query
     * @throws SaxonApiException if the query compilation fails with a static error
     * @throws IOException if the file does not exist or cannot be read
     * @since 9.1
     */

    public XQueryExecutable compile(InputStream query) throws SaxonApiException, IOException {
        try {
            return new XQueryExecutable(processor, env.compileQuery(query, encoding));
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Compile a query supplied as a Reader
     * <p>The base URI of the query should be supplied by calling {@link #setBaseURI(java.net.URI)} </p>
     * @param query the input stream on which the query is supplied. This will be consumed by this method
     * @return an XQueryExecutable representing the compiled query
     * @throws SaxonApiException if the query compilation fails with a static error
     * @throws IOException if the file does not exist or cannot be read
     * @since 9.1
     */

    public XQueryExecutable compile(Reader query) throws SaxonApiException, IOException {
        try {
            return new XQueryExecutable(processor, env.compileQuery(query));
        } catch (XPathException e) {
            throw new SaxonApiException(e);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

