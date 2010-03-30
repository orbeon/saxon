package org.orbeon.saxon;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.XPathContextMajor;
import org.orbeon.saxon.expr.PathMap;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.functions.EscapeURI;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.trace.*;
import org.orbeon.saxon.trans.*;
import org.orbeon.saxon.tree.TreeBuilder;
import org.orbeon.saxon.value.DateTimeValue;
import org.orbeon.saxon.type.SchemaURIResolver;
import org.xml.sax.SAXParseException;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

/**
 * The Controller is Saxon's implementation of the JAXP Transformer class, and represents
 * an executing instance of a transformation or query. Multiple concurrent executions of
 * the same transformation or query will use different Controller instances. This class is
 * therefore not thread-safe.
 * <p>
 * The Controller is serially reusable, as required by JAXP: when one transformation or query
 * is finished, it can be used to run another. However, there is no advantage in doing this
 * rather than allocating a new Controller each time.
 * <p>
 * The Controller can also be used when running Java applications that use neither XSLT nor
 * XQuery. A dummy Controller is created when running free-standing XPath expressions.
 * <p>
 * The Controller holds those parts of the dynamic context that do not vary during the course
 * of a transformation or query, or that do not change once their value has been computed.
 * This also includes those parts of the static context that are required at run-time.
 * <p>
 * Wherever possible XSLT applications should use the JAXP Transformer class directly,
 * rather than relying on Saxon-specific methods in the Controller. However, some
 * features are currently available only through this class. This applies especially
 * to new features specific to XSLT 2.0, since the JAXP interface still supports
 * only XSLT 1.0. Such methods may be superseded in the future by JAXP methods.
 * <p>
 * Many methods on the Controller are designed for internal use and should not be
 * considered stable. From release 8.4 onwards, those methods that are considered sufficiently
 * stable to constitute path of the Saxon public API are labelled with the JavaDoc tag "since":
 * the value indicates the release at which the method was added to the public API.
 *
 * @author Michael H. Kay
 * @since 8.4
 */

public class Controller extends Transformer {

    private Configuration config;
    private Item initialContextItem;
    private Item contextForGlobalVariables;
    private Bindery bindery;                // holds values of global and local variables
    private NamePool namePool;
    private Receiver messageEmitter;
    private RuleManager ruleManager;
    private Properties localOutputProperties;
    private GlobalParameterSet parameters;
    private PreparedStylesheet preparedStylesheet;
    private TraceListener traceListener;
    private boolean tracingPaused;
    private PrintStream traceFunctionDestination = System.err;
    private URIResolver standardURIResolver;
    private URIResolver userURIResolver;
    private Result principalResult;
    private String principalResultURI;
    private String cookedPrincipalResultURI;
    private boolean thereHasBeenAnExplicitResultDocument;
    private OutputURIResolver outputURIResolver;
    private UnparsedTextURIResolver unparsedTextResolver;
    private SchemaURIResolver schemaURIResolver;
    private ErrorListener errorListener;
    private int recoveryPolicy;
    private Executable executable;
    private int treeModel = Builder.TINY_TREE;
    private Template initialTemplate = null;
    private HashSet allOutputDestinations;
    private DocumentPool sourceDocumentPool;
    private SequenceOutputter reusableSequenceOutputter = null;
    private HashMap userDataTable;
    private DateTimeValue currentDateTime;
    private boolean dateTimePreset = false;
    private StructuredQName initialMode = null;
    private NodeInfo lastRememberedNode = null;
    private int lastRememberedNumber = -1;
    private ClassLoader classLoader;
    private PathMap pathMap = null;
//    private int nextLocalDocumentNumber = -1;

    /**
     * Create a Controller and initialise variables. Note: XSLT applications should
     * create the Controller by using the JAXP newTransformer() method, or in S9API
     * by using XsltExecutable.load()
     *
     * @param config The Configuration used by this Controller
     */

    public Controller(Configuration config) {
        this.config = config;
        // create a dummy executable
        executable = new Executable(config);
        executable.setHostLanguage(config.getHostLanguage());
        sourceDocumentPool = new DocumentPool();
        reset();
    }

    /**
     * Create a Controller and initialise variables.
     *
     * @param config The Configuration used by this Controller
     * @param executable The executable used by this Controller
     */

    public Controller(Configuration config, Executable executable) {
        this.config = config;
        this.executable = executable;
        sourceDocumentPool = new DocumentPool();
        reset();
    }

    /**
     * <p>Reset this <code>Transformer</code> to its original configuration.</p>
     * <p/>
     * <p><code>Transformer</code> is reset to the same state as when it was created with
     * {@link javax.xml.transform.TransformerFactory#newTransformer()},
     * {@link javax.xml.transform.TransformerFactory#newTransformer(javax.xml.transform.Source source)} or
     * {@link javax.xml.transform.Templates#newTransformer()}.
     * <code>reset()</code> is designed to allow the reuse of existing <code>Transformer</code>s
     * thus saving resources associated with the creation of new <code>Transformer</code>s.</p>
     * <p>
     * <i>The above is from the JAXP specification. With Saxon, it's unlikely that reusing a Transformer will
     * give any performance benefits over creating a new one. The one case where it might be beneficial is
     * to reuse the document pool (the set of documents that have been loaded using the doc() or document()
     * functions). Therefore, this method does not clear the document pool. If you want to clear the document
     * pool, call the method {@link #clearDocumentPool} as well.</i>
     * <p/>
     * <p>The reset <code>Transformer</code> is not guaranteed to have the same {@link javax.xml.transform.URIResolver}
     * or {@link javax.xml.transform.ErrorListener} <code>Object</code>s, e.g. {@link Object#equals(Object obj)}.
     * It is guaranteed to have a functionally equal <code>URIResolver</code>
     * and <code>ErrorListener</code>.</p>
     *
     * @since 1.5
     */

    public void reset() {
        bindery = new Bindery();
		namePool = config.getNamePool();
        standardURIResolver = config.getSystemURIResolver();
        userURIResolver = config.getURIResolver();
        outputURIResolver = config.getOutputURIResolver();
        schemaURIResolver = config.getSchemaURIResolver();
        unparsedTextResolver = new StandardUnparsedTextResolver();
        errorListener = config.getErrorListener();
        recoveryPolicy = config.getRecoveryPolicy();
        if (errorListener instanceof StandardErrorListener) {
            // if using a standard error listener, make a fresh one
            // for each transformation, because it is stateful - and also because the
            // host language is now known (a Configuration can serve multiple host languages)
            PrintStream ps = ((StandardErrorListener)errorListener).getErrorOutput();
            errorListener = ((StandardErrorListener)errorListener).makeAnother(executable.getHostLanguage());
            ((StandardErrorListener)errorListener).setErrorOutput(ps);
            ((StandardErrorListener)errorListener).setRecoveryPolicy(recoveryPolicy);
        }

        userDataTable = new HashMap(20);

        traceListener = null;
        tracingPaused = false;
        traceFunctionDestination = System.err;
        TraceListener tracer;
        try {
            tracer = config.makeTraceListener();
        } catch (XPathException err) {
            throw new IllegalStateException(err.getMessage());
        }
        if (tracer!=null) {
            addTraceListener(tracer);
        }

        setTreeModel(config.getTreeModel());
        initialContextItem = null;
        contextForGlobalVariables = null;
        messageEmitter = null;
        localOutputProperties = null;
        parameters = null;

        principalResult = null;
        principalResultURI = null;
        initialTemplate = null;
        allOutputDestinations = null;
        thereHasBeenAnExplicitResultDocument = false;
        currentDateTime = null;
        dateTimePreset = false;
        initialMode = null;
        lastRememberedNode = null;
        lastRememberedNumber = -1;
        classLoader = null;

    }

    /**
     * Get the Configuration associated with this Controller. The Configuration holds
     * settings that potentially apply globally to many different queries and transformations.
     * @return the Configuration object
     * @since 8.4
     */
    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the initial mode for the transformation.
     * <p>
     * XSLT 2.0 allows a transformation to be started in a mode other than the default mode.
     * The transformation then starts by looking for the template rule in this mode that best
     * matches the initial context node.
     * <p>
     * This method may eventually be superseded by a standard JAXP method.
     *
     * @param expandedModeName the name of the initial mode.  The mode is
     *     supplied as an expanded QName, that is "localname" if there is no
     *     namespace, or "{uri}localname" otherwise
     * @since 8.4
     */

    public void setInitialMode(String expandedModeName) {
        if (expandedModeName==null) return;
        if (expandedModeName.length() == 0) return;
        initialMode = StructuredQName.fromClarkName(expandedModeName);
    }

    /**
     * Get the initial mode for the transformation
     * @return the initial mode, as a name in Clark format
     */

    public String getInitialMode() {
        return initialMode.getClarkName();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Methods for managing output destinations and formatting
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Set the output properties for the transformation.  These
     * properties will override properties set in the templates
     * with xsl:output.
     * <p>
     * As well as the properties defined in the JAXP OutputKeys class,
     * Saxon defines an additional set of properties in {@link SaxonOutputKeys}.
     * These fall into two categories: Constants representing serialization
     * properties defined in XSLT 2.0 (which are not yet supported by JAXP),
     * and constants supporting Saxon extensions to the set of serialization
     * properties.
     *
     * @param properties the output properties to be used for the
     *     transformation. If the value is null, the properties are reset to
     *     be the properties of the Templates object (that is, for XSLT 2.0,
     *     the properties set in the unnamed xsl:output object).
     * @throws IllegalArgumentException if any of the properties are invalid (other than
     *     properties in a user-defined namespace)
     * @see SaxonOutputKeys
     * @since 8.4
     */

    public void setOutputProperties(Properties properties) {
        if (properties == null) {
            localOutputProperties = null;
        } else {
            Enumeration keys = properties.propertyNames();
            while(keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                setOutputProperty(key, properties.getProperty(key));
            }
        }
    }

    /**
     * Get the output properties for the transformation.
     * <p>
     * As well as the properties defined in the JAXP OutputKeys class,
     * Saxon defines an additional set of properties in {@link SaxonOutputKeys}.
     * These fall into two categories: Constants representing serialization
     * properties defined in XSLT 2.0 (which are not yet supported by JAXP),
     * and constants supporting Saxon extensions to the set of serialization
     * properties.
     *
     * @return the output properties being used for the transformation,
     *     including properties defined in the stylesheet for the unnamed
     *     output format
     * @see SaxonOutputKeys
     * @since 8.4
     */

    public Properties getOutputProperties() {
        if (localOutputProperties == null) {
            if (executable==null) {
                return new Properties();
            } else {
                localOutputProperties = new Properties(executable.getDefaultOutputProperties());
            }
        }

        // Make a copy, so that modifications to the returned properties object have no effect (even on the
        // local output properties)

        Properties newProps = new Properties();
        Enumeration keys = localOutputProperties.propertyNames();
        while(keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            newProps.setProperty(key, localOutputProperties.getProperty(key));
        }
        return newProps;
    }

    /**
     * Set an output property for the transformation.
     * <p>
     * As well as the properties defined in the JAXP OutputKeys class,
     * Saxon defines an additional set of properties in {@link SaxonOutputKeys}.
     * These fall into two categories: Constants representing serialization
     * properties defined in XSLT 2.0 (which are not yet supported by JAXP),
     * and constants supporting Saxon extensions to the set of serialization
     * properties.
     *
     * @param name the name of the property
     * @param value the value of the property
     * @throws IllegalArgumentException if the property is invalid (except for
     *     properties in a user-defined namespace)
     * @see SaxonOutputKeys
     * @since 8.4
     */

    public void setOutputProperty(String name, String value) {
        if (localOutputProperties == null) {
            localOutputProperties = getOutputProperties();
        }
        try {
            SaxonOutputKeys.checkOutputProperty(name, value, getConfiguration().getNameChecker());
        } catch (XPathException err) {
            throw new IllegalArgumentException(err.getMessage());
        }
        localOutputProperties.setProperty(name, value);
    }

    /**
     * Get the value of an output property.
     * <p>
     * As well as the properties defined in the JAXP OutputKeys class,
     * Saxon defines an additional set of properties in {@link SaxonOutputKeys}.
     * These fall into two categories: Constants representing serialization
     * properties defined in XSLT 2.0 (which are not yet supported by JAXP),
     * and constants supporting Saxon extensions to the set of serialization
     * properties.
     *
     * @param name the name of the requested property
     * @return the value of the requested property
     * @see SaxonOutputKeys
     * @since 8.4
     */

    public String getOutputProperty(String name) {
         try {
            SaxonOutputKeys.checkOutputProperty(name, null, getConfiguration().getNameChecker());
        } catch (XPathException err) {
            throw new IllegalArgumentException(err.getMessage());
        }
        if (localOutputProperties == null) {
            if (executable==null) {
                return null;
            } else {
                localOutputProperties = executable.getDefaultOutputProperties();
            }
        }
        return localOutputProperties.getProperty(name);
    }

    /**
     * Set the base output URI.
     * This defaults to the system ID of the principal Result object, but
     * a different value can be set for use where there is no principal result.
     * The command line interface sets this to the current working directory.
     * <p>
     * The concept of the base output URI is new in XSLT 2.0: it defines the
     * base URI for resolving relative URIs in the <code>href</code> attribute
     * of the <code>xsl:result-document</code> instruction. This method may be
     * superseded by a standard JAXP method when JAXP is updated to support XSLT 2.0.
     *
     * @param uri the base output URI
     * @since 8.4
     */

    public void setBaseOutputURI(String uri) {
        principalResultURI = uri;
    }

    /**
     * Get the base output URI.
     * This defaults to the system ID of the principal Result object, but
     * a different value can be set for use where there is no principal result.
     * The command line interface sets this to the current working directory.
     * <p>
     * The concept of the base output URI is new in XSLT 2.0: it defines the
     * base URI for resolving relative URIs in the <code>href</code> attribute
     * of the <code>xsl:result-document</code> instruction. This method may be
     * superseded by a standard JAXP method when JAXP is updated to support XSLT 2.0.
     *
     * @return the base output URI
     * @since 8.4
     */

    public String getBaseOutputURI() {
        return principalResultURI;
    }

    /**
     * Get the base output URI after processing. The processing consists of (a) defaulting
     * to the current user directory if no base URI is available and if the stylesheet is trusted,
     * and (b) applying IRI-to-URI escaping
     * @return the base output URI after processing.
     */

    public String getCookedBaseOutputURI() {
        if (cookedPrincipalResultURI == null) {
            String base = getBaseOutputURI();
            if (base == null && config.isAllowExternalFunctions()) {
                // if calling external functions is allowed, then the stylesheet is trusted, so
                // we allow it to write to files relative to the current directory
                base = new File(System.getProperty("user.dir")).toURI().toString();
            }
            if (base != null) {
                base = EscapeURI.iriToUri(base).toString();
            }
            cookedPrincipalResultURI = base;
        }
        return cookedPrincipalResultURI;
    }

    /**
     * Get the principal result destination.
     * <p>This method is intended for internal use only. It is typically called by Saxon during the course
     * of a transformation, to discover the result that was supplied in the transform() call.</p>
     * @return the Result object supplied as the principal result destination.
     */

    public Result getPrincipalResult() {
        return principalResult;
    }

    /**
     * Check that an output destination has not been used before, optionally adding
     * this URI to the set of URIs that have been used.
     * @param uri the URI to be used as the output destination
     * @return true if the URI is available for use; false if it has already been used.
     * <p>
     * This method is intended for internal use only.
     */

    public boolean checkUniqueOutputDestination(String uri) {
        if (uri == null) {
            return true;    // happens when writing say to an anonymous StringWriter
        }
        if (allOutputDestinations == null) {
            allOutputDestinations = new HashSet(20);
        }
        if (uri.startsWith("file:///")) {
            uri = "file:/" + uri.substring(8);
        }
        return !allOutputDestinations.contains(uri);
    }

    /**
     * Add a URI to the set of output destinations that cannot be written to, either because
     * they have already been written to, or because they have been read
     * @param uri A URI that is not available as an output destination
     */

    public void addUnavailableOutputDestination(String uri) {
        if (allOutputDestinations == null) {
            allOutputDestinations = new HashSet(20);
        }
        allOutputDestinations.add(uri);
    }

    /**
     * Remove a URI from the set of output destinations that cannot be written to or read from.
     * Used to support saxon:discard-document()
     * @param uri A URI that is being made available as an output destination
     */

    public void removeUnavailableOutputDestination(String uri) {
        if (allOutputDestinations != null) {
            allOutputDestinations.remove(uri);
        }
    }


    /**
     * Determine whether an output URI is available for use. This method is intended
     * for use by applications, via an extension function.
     * @param uri A uri that the application is proposing to use in the href attribute of
     * xsl:result-document: if this function returns false, then the xsl:result-document
     * call will fail saying the URI has already been used.
     * @return true if the URI is available for use. Note that this function is not "stable":
     * it may return different results for the same URI at different points in the transformation.
     */

    public boolean isUnusedOutputDestination(String uri) {
        return allOutputDestinations == null || !allOutputDestinations.contains(uri);
    }

    /**
     * Check whether an XSLT implicit result tree can be written. This is allowed only if no xsl:result-document
     * has been written for the principal output URI
     */

    public void checkImplicitResultTree() throws XPathException {
        if (!checkUniqueOutputDestination(principalResultURI)) {
            XPathException err = new XPathException("Cannot write an implicit result document if an explicit result document has been written to the same URI: " +
                    principalResultURI);
            err.setErrorCode("XTDE1490");
            throw err;
        }
    }

    /**
     * Set that an explicit result tree has been written using xsl:result-document
     */

    public void setThereHasBeenAnExplicitResultDocument() {
        thereHasBeenAnExplicitResultDocument = true;
    }

    /**
     * Test whether an explicit result tree has been written using xsl:result-document
     * @return true if the transformation has evaluated an xsl:result-document instruction
     */

    public boolean hasThereBeenAnExplicitResultDocument() {
        return thereHasBeenAnExplicitResultDocument;
    }

    /**
     * Allocate a SequenceOutputter for a new output destination. Reuse the existing one
     * if it is available for reuse (this is designed to ensure that the TinyTree structure
     * is also reused, creating a forest of trees all sharing the same data structure)
     * @param size the estimated size of the output sequence
     * @return SequenceOutputter the allocated SequenceOutputter
     */

    public SequenceOutputter allocateSequenceOutputter(int size) {
        if (reusableSequenceOutputter != null) {
            SequenceOutputter out = reusableSequenceOutputter;
            reusableSequenceOutputter = null;
            return out;
        } else {
            return new SequenceOutputter(this, size);
        }
    }

    /**
     * Accept a SequenceOutputter that is now available for reuse
     * @param out the SequenceOutputter that is available for reuse
     */

    public void reuseSequenceOutputter(SequenceOutputter out) {
        reusableSequenceOutputter = out;
    }

    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Set the initial named template to be used as the entry point.
     * <p>
     * XSLT 2.0 allows a transformation to start by executing a named template, rather than
     * by matching an initial context node in a source document. This method may eventually
     * be superseded by a standard JAXP method once JAXP supports XSLT 2.0.
     * <p>
     * Note that any parameters supplied using {@link #setParameter} are used as the values
     * of global stylesheet parameters. There is no way to supply values for local parameters
     * of the initial template.
     *
     * @param expandedName The expanded name of the template in {uri}local format, or null
     * to indicate that there should be no initial template.
     * @throws XPathException if there is no named template with this name
     * @since 8.4
     */

    public void setInitialTemplate(String expandedName) throws XPathException {
        if (expandedName == null) {
            initialTemplate = null;
            return;
        }
        StructuredQName qName = StructuredQName.fromClarkName(expandedName);
        Template t = getExecutable().getNamedTemplate(qName);
        if (t == null) {
            XPathException err = new XPathException("There is no named template with expanded name "
                    + expandedName);
            err.setErrorCode("XTDE0040");
            reportFatalError(err);
            throw err;
        } else if (t.hasRequiredParams()) {
            XPathException err = new XPathException("The named template "
                    + expandedName
                    + " has required parameters, so cannot be used as the entry point");
            err.setErrorCode("XTDE0060");
            reportFatalError(err);
            throw err;
        } else {
            initialTemplate = t;
        }
    }

    /**
     * Get the initial template
     * @return the name of the initial template, as an expanded name in Clark format if set, or null otherwise
     * @since 8.7
     */

    public String getInitialTemplate() {
        if (initialTemplate == null) {
            return null;
        } else {
            return initialTemplate.getTemplateName().getClarkName();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Make a PipelineConfiguration based on the properties of this Controller.
     * <p>
     * This interface is intended primarily for internal use, although it may be necessary
     * for applications to call it directly if they construct pull or push pipelines
     * @return a newly constructed PipelineConfiguration holding a reference to this
     * Controller as well as other configuration information.
     */

    public PipelineConfiguration makePipelineConfiguration() {
        PipelineConfiguration pipe = new PipelineConfiguration();
        pipe.setConfiguration(getConfiguration());
        pipe.setErrorListener(getErrorListener());
        pipe.setURIResolver(userURIResolver==null ? standardURIResolver : userURIResolver);
        pipe.setSchemaURIResolver(schemaURIResolver);
        pipe.setExpandAttributeDefaults(getConfiguration().isExpandAttributeDefaults());
        pipe.setUseXsiSchemaLocation(((Boolean)getConfiguration().getConfigurationProperty(
                FeatureKeys.USE_XSI_SCHEMA_LOCATION)).booleanValue());
        pipe.setController(this);
        final Executable executable = getExecutable();
        if (executable != null) {
            // can be null for an IdentityTransformer
            pipe.setLocationProvider(executable.getLocationMap());
            pipe.setHostLanguage(executable.getHostLanguage());
        }
        return pipe;
    }

    /**
     * Make an Emitter to be used for xsl:message output.
     * <p>
     * This method is intended for internal use only.
     *
     * @exception XPathException if any dynamic error occurs; in
     *     particular, if the registered MessageEmitter class is not an
     *     Emitter
     * @return The newly constructed message Emitter
     */

    private Receiver makeMessageEmitter() throws XPathException {
        String emitterClass = config.getMessageEmitterClass();

        Object messageReceiver = config.getInstance(emitterClass, getClassLoader());
        if (!(messageReceiver instanceof Receiver)) {
            throw new XPathException(emitterClass + " is not a Receiver");
        }
        setMessageEmitter((Receiver)messageReceiver);
//        if (messageReceiver instanceof Emitter) {
//            Properties props = new Properties();
//            props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//            ((Emitter)messageReceiver).setOutputProperties(props);
//        }
        return (Receiver)messageReceiver;
    }

    /**
     * Set the Receiver to be used for xsl:message output.
     * <p>
     * Recent versions of the JAXP interface specify that by default the
     * output of xsl:message is sent to the registered ErrorListener. Saxon
     * does not yet implement this convention. Instead, the output is sent
     * to a default message emitter, which is a slightly customised implementation
     * of the standard Saxon Emitter interface.</p>
     * <p>
     * This interface can be used to change the way in which Saxon outputs
     * xsl:message output.</p>
     * <p>
     * It is not necessary to use this interface in order to change the destination
     * to which messages are written: that can be achieved by obtaining the standard
     * message emitter and calling its {@link Emitter#setWriter} method.</p>
     * <p>
     * Although any <code>Receiver</code> can be supplied as the destination for messages,
     * applications may find it convenient to implement a subclass of {@link org.orbeon.saxon.event.SequenceWriter},
     * in which only the abstract <code>write()</code> method is implemented. This will have the effect that the
     * <code>write()</code> method is called to output each message as it is generated, with the <code>Item</code>
     * that is passed to the <code>write()</code> method being the document node at the root of an XML document
     * containing the contents of the message. 
     * <p>
     * This method is intended for use by advanced applications. The Receiver interface
     * itself is subject to change in new Saxon releases.</p>
     * <p>
     * The supplied Receiver will have its open() method called once at the start of
     * the transformation, and its close() method will be called once at the end of the
     * transformation. Each individual call of an xsl:message instruction is wrapped by
     * calls of startDocument() and endDocument(). If terminate="yes" is specified on the
     * xsl:message call, the properties argument of the startDocument() call will be set
     * to the value {@link ReceiverOptions#TERMINATE}.</p>
     * @param receiver The receiver to receive xsl:message output.
     * @since 8.4; changed in 8.9 to supply a Receiver rather than an Emitter
     */

    public void setMessageEmitter(Receiver receiver) {
        messageEmitter = receiver;
        if (receiver.getPipelineConfiguration() == null) {
            messageEmitter.setPipelineConfiguration(makePipelineConfiguration());
        }
        if (messageEmitter instanceof Emitter && ((Emitter)messageEmitter).getOutputProperties() == null) {
            try {
                Properties props = new Properties();
                props.setProperty(OutputKeys.METHOD, "xml");
                props.setProperty(OutputKeys.INDENT, "yes");
                props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                ((Emitter)messageEmitter).setOutputProperties(props);
            } catch (XPathException e) {
                // no action
            }
        }
    }

    /**
     * Get the Emitter used for xsl:message output. This returns the emitter
     * previously supplied to the {@link #setMessageEmitter} method, or the
     * default message emitter otherwise.
     *
     * @return the Receiver being used for xsl:message output
     * @since 8.4; changed in 8.9 to return a Receiver rather than an Emitter
     */

    public Receiver getMessageEmitter() {
       return messageEmitter;
    }

    /**
     * Make a CharacterMapExpander to handle the character map definitions in the serialization
     * properties.
     * <p>
     * This method is intended for internal use only.
     *
     * @param useMaps the expanded use-character-maps property: a space-separated list of names
     * of character maps to be used, each one expressed as an expanded-QName in Clark notation
     * (that is, {uri}local-name).
     * @param sf the SerializerFactory - used to create a CharacterMapExpander
     * @return a CharacterMapExpander if one is required, or null if not (for example, if the
     * useMaps argument is an empty string).
     * @throws XPathException if a name in the useMaps property cannot be resolved to a declared
     * character map.
     */

    public CharacterMapExpander makeCharacterMapExpander(String useMaps, SerializerFactory sf) throws XPathException {
        CharacterMapExpander characterMapExpander = null;
        HashMap characterMapIndex = getExecutable().getCharacterMapIndex();
        if (useMaps != null && characterMapIndex != null) {
            List characterMaps = new ArrayList(5);
            StringTokenizer st = new StringTokenizer(useMaps, " \t\n\r", false);
            while (st.hasMoreTokens()) {
                String expandedName = st.nextToken();
                StructuredQName qName = StructuredQName.fromClarkName(expandedName);
                IntHashMap map = (IntHashMap)characterMapIndex.get(qName);
                if (map==null) {
                    throw new XPathException("Character map '" + expandedName + "' has not been defined");
                }
                characterMaps.add(map);
            }
            if (!characterMaps.isEmpty()) {
                characterMapExpander = sf.newCharacterMapExpander();
                characterMapExpander.setCharacterMaps(characterMaps);
            }
        }
        return characterMapExpander;
    }

    /**
     * Set the policy for handling recoverable errrors
     * @param policy the recovery policy to be used. The options are {@link Configuration#RECOVER_SILENTLY},
     * {@link Configuration#RECOVER_WITH_WARNINGS}, or {@link Configuration#DO_NOT_RECOVER}.
     * @since 8.7.1
     */

    public void setRecoveryPolicy(int policy) {
        recoveryPolicy = policy;
        if (errorListener instanceof StandardErrorListener) {
            ((StandardErrorListener)errorListener).setRecoveryPolicy(policy);
        }
    }

    /**
     * Get the policy for handling recoverable errors
     *
     * @return the current policy. If none has been set with this Controller, the value registered with the
     * Configuration is returned.
     * @since 8.7.1
     */

    public int getRecoveryPolicy() {
        return recoveryPolicy;
    }

	/**
	 * Set the error listener.
	 *
	 * @param listener the ErrorListener to be used
	 */

	public void setErrorListener(ErrorListener listener) {
		errorListener = listener;
	}

	/**
	 * Get the error listener.
	 *
	 * @return the ErrorListener in use
	 */

	public ErrorListener getErrorListener() {
		return errorListener;
	}

    /**
     * Report a recoverable error. This is an XSLT concept: by default, such an error results in a warning
     * message, and processing continues. In XQuery, however, there are no recoverable errors so a fatal
     * error is reported.
     * <p>
     * This method is intended for internal use only.
     *
     * @param err An exception holding information about the error
     * @throws XPathException if the error listener decides not to
     *     recover from the error
     */

    public void recoverableError(XPathException err) throws XPathException {
        try {
            if (executable.getHostLanguage() == Configuration.XQUERY) {
                reportFatalError(err);
                throw err;
            } else {
                errorListener.error(err);
            }
        } catch (TransformerException e) {
            XPathException de = XPathException.makeXPathException(e);
            de.setHasBeenReported();
            throw de;
        }
    }

    /**
     * Report a fatal error
     * @param err the error to be reported
     */

    public void reportFatalError(XPathException err) {
        if (!err.hasBeenReported()) {
            try {
                getErrorListener().fatalError(err);
            } catch (TransformerException e) {
                //
            }
            err.setHasBeenReported();
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    // Methods for managing the various runtime control objects
    /////////////////////////////////////////////////////////////////////////////////////////


    /**
     * Get the Executable object.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the Executable (which represents the compiled stylesheet)
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Get the document pool. This is used only for source documents, not for stylesheet modules.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the source document pool
     */

    public DocumentPool getDocumentPool() {
        return sourceDocumentPool;
    }

    /**
     * Clear the document pool.
     * This is sometimes useful when re-using the same Transformer
     * for a sequence of transformations, but it isn't done automatically, because when
     * the transformations use common look-up documents, the caching is beneficial.
     */

    public void clearDocumentPool() {
        sourceDocumentPool = new DocumentPool();
    }

    /**
     * Set the initial context node (used for evaluating global variables).
     * When a transformation is invoked using the {@link #transform} method, the
     * initial context node is set automatically. This method is useful in XQuery,
     * to define an initial context node for evaluating global variables, and also
     * in XSLT 2.0, when the transformation is started by invoking a named template.
     *
     * @param doc The principal source document
     * @since 8.4
     * @deprecated From Saxon 8.7, replaced by {@link #setInitialContextItem(Item)}
     */

    public void setPrincipalSourceDocument(DocumentInfo doc) {
        initialContextItem = doc;
    }

    /**
     * Set the initial context item.
     * <p/>
     * When a transformation is invoked using the {@link #transform} method, the
     * initial context node is set automatically. This method is useful in XQuery,
     * to define an initial context node for evaluating global variables, and also
     * in XSLT 2.0, when the transformation is started by invoking a named template.
     *
     * <p>When an initial context item is set, it also becomes the context item used for
     * evaluating global variables. The two context items can only be different when the
     * {@link #transform} method is used to transform a document starting at a node other
     * than the root.</p>
     *
     * <p>In XQuery, the two context items are always
     * the same; in XSLT, the context node for evaluating global variables is the root of the
     * tree containing the initial context item.</p>
     *
     * @param item The initial context item. The XSLT specification says that this
     * must be a node; however this restriction is not enforced, and any item can be supplied
     * as an initial context item if the transformation is started by calling a named initial template.
     * (There is no similar restriction in XQuery)
     * @since 8.7
     */

    public void setInitialContextItem(Item item) {
        initialContextItem = item;
        contextForGlobalVariables = item;
        // TODO: are we enforcing the rule that in XSLT the context for global variables is always the
        // root of the tree?
    }

    /**
     * Get the current bindery.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the Bindery (in which values of all variables are held)
     */

    public Bindery getBindery() {
        return bindery;
    }

    /**
     * Get the initial context item. This returns the item (often a document node)
     * previously supplied to the {@link #setInitialContextItem} method, or the
     * initial context node set implicitly using methods such as {@link #transform}.
     * @return the initial context item. Note that in XSLT this must be a node, but in
     * XQuery it may also be an atomic value.
     * @since 8.7
     */

    public Item getInitialContextItem() {
        return initialContextItem;
    }

    /**
     * Get the item used as the context for evaluating global variables. In XQuery this
     * is the same as the initial context item; in XSLT it is the root of the tree containing
     * the initial context node.
     * @return the context item for evaluating global variables, or null if there is none
     * @since 8.7
     */

    public Item getContextForGlobalVariables() {
        return contextForGlobalVariables;
        // See bug 5224, which points out that the rules for XQuery 1.0 weren't clearly defined
    }

    /**
     * Set an object that will be used to resolve URIs used in
     * document(), etc.
     *
     * @param resolver An object that implements the URIResolver interface, or
     *      null.
     */

    public void setURIResolver(URIResolver resolver) {
        userURIResolver = resolver;
        if (resolver instanceof StandardURIResolver) {
            ((StandardURIResolver)resolver).setConfiguration(getConfiguration());
        }                                                      
    }

    /**
     * Get the URI resolver.
     *
     * <p><i>This method changed in Saxon 8.5, to conform to the JAXP specification. If there
     * is no user-specified URIResolver, it now returns null; previously it returned the system
     * default URIResolver.</i></p>
     *
     * @return the user-supplied URI resolver if there is one, or null otherwise.
     */

    public URIResolver getURIResolver() {
        return userURIResolver;
    }

    /**
     * Get the fallback URI resolver. This is the URIResolver that Saxon uses when
     * the user-supplied URI resolver returns null.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the the system-defined URIResolver
     */

    public URIResolver getStandardURIResolver() {
        return standardURIResolver;
    }

     /**
     * Set the URI resolver for secondary output documents.
      * <p>
      * XSLT 2.0 introduces the <code>xsl:result-document</code instruction,
      * allowing a transformation to have multiple result documents. JAXP does
      * not yet support this capability. This method allows an OutputURIResolver
      * to be specified that takes responsibility for deciding the destination
      * (and, if it wishes, the serialization properties) of secondary output files.
      * <p>
      * This method may eventually be superseded by a standard JAXP method.
     *
     * @param resolver An object that implements the OutputURIResolver
     *     interface, or null.
      * @since 8.4
     */

    public void setOutputURIResolver(OutputURIResolver resolver) {
        if (resolver==null) {
            outputURIResolver = config.getOutputURIResolver();
        } else {
            outputURIResolver = resolver;
        }
    }

    /**
     * Get the output URI resolver.
     *
     * @return the user-supplied URI resolver if there is one, or the
     *     system-defined one otherwise.
     * @see #setOutputURIResolver
     * @since 8.4
     */

    public OutputURIResolver getOutputURIResolver() {
        return outputURIResolver;
    }

    /**
     * Set an UnparsedTextURIResolver to be used to resolve URIs passed to the XSLT
     * unparsed-text() function.
     * @param resolver the unparsed text URI resolver to be used. This replaces any unparsed text
     * URI resolver previously registered.
     * @since 8.9
     */

    public void setUnparsedTextURIResolver(UnparsedTextURIResolver resolver) {
        unparsedTextResolver = resolver;
    }

    /**
     * Get the URI resolver for the unparsed-text() function. This will
     * return the UnparsedTextURIResolver previously set using the {@link #setUnparsedTextURIResolver}
     * method.
     * @return the registered UnparsedTextURIResolver
     * @since 8.9
     */

    public UnparsedTextURIResolver getUnparsedTextURIResolver() {
        return unparsedTextResolver;
    }

    /**
     * Set the SchemaURIResolver used for resolving references to schema
     * documents. Defaults to the SchemaURIResolver registered with the
     * Configuration
     * @param resolver the resolver for references to schema documents
     */

    public void setSchemaURIResolver(SchemaURIResolver resolver) {
        schemaURIResolver = resolver;
    }

    /**
     * Get the SchemaURIResolver used for resolving references to schema
     * documents. If none has been set on the Controller, returns the
     * SchemaURIResolver registered with the Configuration
     * @return the resolver for references to schema documents
     */

    public SchemaURIResolver getSchemaURIResolver() {
        return schemaURIResolver;
    }

    /**
     * Get the KeyManager.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the KeyManager, which holds details of all key declarations
     */

    public KeyManager getKeyManager() {
        return executable.getKeyManager();
    }

	/**
	 * Get the name pool in use. The name pool is responsible for mapping QNames used in source
     * documents and compiled stylesheets and queries into numeric codes. All source documents
     * used by a given transformation or query must use the same name pool as the compiled stylesheet
     * or query.
	 *
	 * @return the name pool in use
     * @since 8.4
	 */

	public NamePool getNamePool() {
		return namePool;
	}

    /**
     * Set the tree data model to use. This affects all source documents subsequently constructed using a
     * Builder obtained from this Controller. This includes a document built from a StreamSource or
     * SAXSource supplied as a parameter to the {@link #transform} method.
     *
     * @param model the required tree model: {@link Builder#LINKED_TREE} or
     *     {@link Builder#TINY_TREE}
     * @see org.orbeon.saxon.event.Builder
     * @since 8.4
     */

    public void setTreeModel(int model) {
        treeModel = model;
    }

    /**
     * Get the tree data model to use. This affects all source documents subsequently constructed using a
     * Builder obtained from this Controller. This includes a document built from a StreamSource or
     * SAXSource supplied as a parameter to the {@link #transform} method.
     *
     * @return model the tree model: {@link Builder#LINKED_TREE} or
     *     {@link Builder#TINY_TREE}
     * @see org.orbeon.saxon.event.Builder
     * @since 9.1
     */

    public int getTreeModel() {
        return treeModel;
    }

    /**
     * Make a builder for the selected tree model.
     *
     * @return an instance of the Builder for the chosen tree model
     * @since 8.4
     */

    public Builder makeBuilder() {
        Builder b;
        if (treeModel==Builder.TINY_TREE)  {
            b = new TinyBuilder();
        } else {
            b = new TreeBuilder();
        }
        b.setTiming(config.isTiming());
        b.setLineNumbering(config.isLineNumbering());
        b.setPipelineConfiguration(makePipelineConfiguration());
        return b;
    }

    /**
     * Make a Stripper configured to implement the whitespace stripping rules.
     * In the case of XSLT the whitespace stripping rules are normally defined
     * by <code>xsl:strip-space</code> and <code>xsl:preserve-space</code elements
     * in the stylesheet. Alternatively, stripping of all whitespace text nodes
     * may be defined at the level of the Configuration, using the method
     * {@link Configuration#setStripsAllWhiteSpace(boolean)}.
     *
     * @param b the Receiver to which the events filtered by this stripper are
     *     to be sent (often a Builder). May be null if the stripper is not being used for filtering
     *     into a Builder or other Receiver.
     * @return the required Stripper. A Stripper may be used in two ways. It acts as
     * a filter applied to an event stream, that can be used to remove the events
     * representing whitespace text nodes before they reach a Builder. Alternatively,
     * it can be used to define a view of an existing tree in which the whitespace
     * text nodes are dynamically skipped while navigating the XPath axes.
     * @since 8.4 - Generalized in 8.5 to accept any Receiver as an argument
     */

    public Stripper makeStripper(Receiver b) {
        if (config.isStripsAllWhiteSpace()) {
            if (b==null) {
                return AllElementStripper.getInstance();
            } else {
                Stripper s = new AllElementStripper();
                s.setUnderlyingReceiver(b);
                s.setPipelineConfiguration(b.getPipelineConfiguration());
                return s;
            }
        }
        Stripper stripper;
        if (executable==null) {
            stripper = new Stripper(new Mode(Mode.STRIPPER_MODE, Mode.DEFAULT_MODE_NAME));
        } else {
            stripper = executable.newStripper();
        }
        stripper.setXPathContext(newXPathContext());
        if (b == null) {
            stripper.setPipelineConfiguration(makePipelineConfiguration());
        } else {
            stripper.setPipelineConfiguration(b.getPipelineConfiguration());
            stripper.setUnderlyingReceiver(b);
        }
        return stripper;
    }

    /**
     * Add a document to the document pool.
     * <p>
     * This method is intended for internal use only.
     *
     * @param doc the root node of the document to be added
     * @param systemId the document-URI property of this document
     */
    public void registerDocument(DocumentInfo doc, String systemId) {
        sourceDocumentPool.add(doc, systemId);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Methods for registering and retrieving handlers for template rules
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Set the RuleManager, used to manage template rules for each mode.
     * <p>
     * This method is intended for internal use only.
     *
     * @param r the Rule Manager
     */
    public void setRuleManager(RuleManager r) {
        ruleManager = r;
    }

    /**
     * Get the Rule Manager.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the Rule Manager, used to hold details of template rules for
     *     all modes
     */
    public RuleManager getRuleManager() {
        return ruleManager;
    }

    /////////////////////////////////////////////////////////////////////////
    // Methods for tracing
    /////////////////////////////////////////////////////////////////////////

    /**
     * Get the TraceListener. By default, there is no TraceListener, and this
     * method returns null. A TraceListener may be added using the method
     * {@link #addTraceListener}. If more than one TraceListener has been added,
     * this method will return a composite TraceListener. Because this form
     * this takes is implementation-dependent, this method is not part of the
     * stable Saxon public API.
     *
     * @return the TraceListener used for XSLT or XQuery instruction tracing
     */
    public TraceListener getTraceListener() { // e.g.
        return traceListener;
    }

    /**
     * Test whether instruction execution is being traced. This will be true
     * if (a) at least one TraceListener has been registered using the
     * {@link #addTraceListener} method, and (b) tracing has not been temporarily
     * paused using the {@link #pauseTracing} method.
     *
     * @return true if tracing is active, false otherwise
     * @since 8.4
     */

    public final boolean isTracing() { // e.g.
        return traceListener != null && !tracingPaused;
    }

    /**
     * Pause or resume tracing. While tracing is paused, trace events are not sent to any
     * of the registered TraceListeners.
     *
     * @param pause true if tracing is to pause; false if it is to resume
     * @since 8.4
     */
    public final void pauseTracing(boolean pause) {
        tracingPaused = pause;
    }

    /**
     * Adds the specified trace listener to receive trace events from
     * this instance. Note that although TraceListeners can be added
     * or removed dynamically, this has no effect unless the stylesheet
     * or query has been compiled with tracing enabled. This is achieved
     * by calling {@link Configuration#setTraceListener} or by setting
     * the attribute {@link FeatureKeys#TRACE_LISTENER} on the
     * TransformerFactory. Conversely, if this property has been set in the
     * Configuration or TransformerFactory, the TraceListener will automatically
     * be added to every Controller that uses that Configuration.
     *
     * @param trace the trace listener. If null is supplied, the call has no effect.
     * @since 8.4
     */

    public void addTraceListener(TraceListener trace) { // e.g.
        if (trace != null) {
            traceListener = TraceEventMulticaster.add(traceListener, trace);
        }
    }

    /**
     * Removes the specified trace listener so that the listener will no longer
     * receive trace events.
     *
     * @param trace the trace listener.
     * @since 8.4
     */

    public void removeTraceListener(TraceListener trace) { // e.g.
        traceListener = TraceEventMulticaster.remove(traceListener, trace);
    }

    /**
     * Set the destination for output from the fn:trace() function.
     * By default, the destination is System.err. If a TraceListener is in use,
     * this is ignored, and the trace() output is sent to the TraceListener.
     * @param stream the PrintStream to which trace output will be sent. If set to
     * null, trace output is suppressed entirely. It is the caller's responsibility
     * to close the stream after use.
     * @since 9.1
     */

    public void setTraceFunctionDestination(PrintStream stream) {
        traceFunctionDestination = stream;
    }

    /**
     * Get the destination for output from the fn:trace() function.
     * @return the PrintStream to which trace output will be sent. If no explicitly
     * destination has been set, returns System.err. If the destination has been set
     * to null to suppress trace output, returns null.
     * @since 9.1
     */

    public PrintStream getTraceFunctionDestination() {
        return traceFunctionDestination;
    }

    /**
     * Associate this Controller with a compiled stylesheet.
     * <p>
     * This method is intended for internal use only.
     *
     * @param sheet the compiled stylesheet
     */

    public void setPreparedStylesheet(PreparedStylesheet sheet) {
        preparedStylesheet = sheet;
        executable = sheet.getExecutable();
        //setOutputProperties(sheet.getOutputProperties());
        // above line deleted for bug 490964 - may have side-effects
    }

    /**
     * Associate this Controller with an Executable. This method is used by the XQuery
     * processor. The Executable object is overkill in this case - the only thing it
     * currently holds are copies of the collation table.
     * <p>
     * This method is intended for internal use only
     * @param exec the Executable
     */

    public void setExecutable(Executable exec) {
        executable = exec;
    }

    /**
     * Initialize the controller ready for a new transformation. This method should not normally be called by
     * users (it is done automatically when transform() is invoked). However, it is available as a low-level API
     * especially for use with XQuery.
     */

    public void initializeController() throws XPathException {
        setRuleManager(executable.getRuleManager());
        //setDecimalFormatManager(executable.getDecimalFormatManager());

        if (traceListener!=null) {
            traceListener.open();
        }

        // get a new bindery, to clear out any variables from previous runs

        bindery = new Bindery();
        executable.initializeBindery(bindery);

        // if parameters were supplied, set them up

        defineGlobalParameters();
    }

    /**
     * Register the global parameters of the transformation or query. This should be called after a sequence
     * of calls on {@link #setParameter}. It checks that all required parameters have been supplied, and places
     * the values of the parameters in the Bindery to make them available for use during the query or
     * transformation.
     * <p>
     * This method is intended for internal use only
     */

    public void defineGlobalParameters() throws XPathException {
        executable.checkAllRequiredParamsArePresent(parameters);
        bindery.defineGlobalParameters(parameters);
    }

    /**
     * Allocate space in the bindery for global variables.
     * <p>For internal use only.</p>
     * @param numberOfVariables the number of global variables for which space is required
     */

    public void allocateGlobalVariables(int numberOfVariables) {
        SlotManager map = executable.getGlobalVariableMap();
        map.setNumberOfVariables(numberOfVariables);
        bindery.allocateGlobals(map);
    }



    /////////////////////////////////////////////////////////////////////////
    // Allow user data to be associated with nodes on a tree
    /////////////////////////////////////////////////////////////////////////

    /**
     * Get user data associated with a key. To retrieve user data, two objects are required:
     * an arbitrary object that may be regarded as the container of the data (originally, and
     * typically still, a node in a tree), and a name. The name serves to distingush data objects
     * associated with the same node by different client applications.
     * <p>
     * This method is intended primarily for internal use, though it may also be
     * used by advanced applications.
     *
     * @param key an object acting as a key for this user data value. This must be equal
     * (in the sense of the equals() method) to the key supplied when the data value was
     * registered using {@link #setUserData}.
     * @param name the name of the required property
     * @return the value of the required property
     */

    public Object getUserData(Object key, String name) {
        String keyValue = key.hashCode() + " " + name;
        // System.err.println("getUserData " + name + " on object returning " + userDataTable.get(key));
        return userDataTable.get(keyValue);
    }

    /**
     * Set user data associated with a key. To store user data, two objects are required:
     * an arbitrary object that may be regarded as the container of the data (originally, and
     * typically still, a node in a tree), and a name. The name serves to distingush data objects
     * associated with the same node by different client applications.
     * <p>
     * This method is intended primarily for internal use, though it may also be
     * used by advanced applications.
     *
     * @param key an object acting as a key for this user data value. This must be equal
     * (in the sense of the equals() method) to the key supplied when the data value was
     * registered using {@link #setUserData}. If data for the given object and name already
     * exists, it is overwritten.
     * @param name the name of the required property
     * @param data the value of the required property
     */

    public void setUserData(Object key, String name, Object data)  {
        // System.err.println("setUserData " + name + " on object to " + data);
        String keyVal = key.hashCode() + " " + name;
        if (data==null) {
            userDataTable.remove(keyVal);
        } else {
            userDataTable.put(keyVal, data);
        }
    }


    /////////////////////////////////////////////////////////////////////////
    // implement the javax.xml.transform.Transformer methods
    /////////////////////////////////////////////////////////////////////////

    /**
     * Perform a transformation from a Source document to a Result document.
     *
     * @exception XPathException if the transformation fails. As a
     *     special case, the method throws a TerminationException (a subclass
     *     of XPathException) if the transformation was terminated using
     *      xsl:message terminate="yes".
     * @param source The input for the source tree. May be null if and only if an
     * initial template has been supplied.
     * @param result The destination for the result tree.
     */

    public void transform(Source source, Result result) throws TransformerException {
        if (preparedStylesheet==null) {
            throw new XPathException("Stylesheet has not been prepared");
        }

        if (!dateTimePreset) {
            currentDateTime = null;     // reset at start of each transformation
        }

        boolean close = false;
        try {
            NodeInfo startNode = null;
            boolean wrap = true;
            int validationMode = config.getSchemaValidationMode();
            Source underSource = source;
            if (source instanceof AugmentedSource) {
                Boolean localWrap = ((AugmentedSource)source).getWrapDocument();
                if (localWrap != null) {
                    wrap = localWrap.booleanValue();
                }
                close = ((AugmentedSource)source).isPleaseCloseAfterUse();
                int localValidate = ((AugmentedSource)source).getSchemaValidation();
                if (localValidate != Validation.DEFAULT) {
                    validationMode = localValidate;
                }
                if (validationMode == Validation.STRICT || validationMode == Validation.LAX) {
                    // If validation of a DOMSource or NodeInfo is requested, we must copy it, we can't wrap it
                    wrap = false;
                }
                underSource = ((AugmentedSource)source).getContainedSource();
            }
            Source s2 = config.getSourceResolver().resolveSource(underSource, config);
            if (s2 != null) {
                underSource = s2;
            }
            if (wrap && (underSource instanceof NodeInfo || underSource instanceof DOMSource)) {
                startNode = prepareInputTree(underSource);
                registerDocument(startNode.getDocumentRoot(), underSource.getSystemId());

            } else if (source == null) {
                if (initialTemplate == null) {
                    throw new XPathException("Either a source document or an initial template must be specified");
                }

            } else {
                // The input is a SAXSource or StreamSource or AugmentedSource, or
                // a DOMSource with wrap=no: build the document tree

                Builder sourceBuilder = makeBuilder();
                Sender sender = new Sender(sourceBuilder.getPipelineConfiguration());
                Receiver r = sourceBuilder;
                if (config.isStripsAllWhiteSpace() || executable.stripsWhitespace() ||
                        validationMode == Validation.STRICT || validationMode == Validation.LAX) {
                    r = makeStripper(sourceBuilder);
                }
                if (executable.stripsInputTypeAnnotations()) {
                    r = config.getAnnotationStripper(r);
                }
                sender.send(source, r);
                if (close) {
                    ((AugmentedSource)source).close();
                }
                DocumentInfo doc = (DocumentInfo)sourceBuilder.getCurrentRoot();
                sourceBuilder.reset();
                registerDocument(doc, source.getSystemId());
                startNode = doc;
            }

            transformDocument(startNode, result);

        } catch (TerminationException err) {
            //System.err.println("Processing terminated using xsl:message");
            throw err;
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
                    reportFatalError(err);
                }
            } else {
                reportFatalError(err);
            }
            throw err;
        } finally {
            if (close) {
                ((AugmentedSource)source).close();
            }
            principalResultURI = null;
        }
    }

    /**
     * Prepare an input tree for processing. This is used when either the initial
     * input, or a Source returned by the document() function, is a NodeInfo or a
     * DOMSource. The preparation consists of wrapping a DOM document inside a wrapper
     * that implements the NodeInfo interface, and/or adding a space-stripping wrapper
     * if the stylesheet strips whitespace nodes.
     * <p>
     * This method is intended for internal use.
     *
     * @param source the input tree. Must be either a DOMSource or a NodeInfo
     * @return the NodeInfo representing the input node, suitably wrapped.
     */

    public NodeInfo prepareInputTree(Source source) {
        NodeInfo start = getConfiguration().unravel(source);
        if (executable.stripsWhitespace()) {
            DocumentInfo docInfo = start.getDocumentRoot();
            StrippedDocument strippedDoc = new StrippedDocument(docInfo, makeStripper(null));
            start = strippedDoc.wrap(start);
        }
        return start;
    }

    /**
     * Get a NodeInfo corresponding to a DOM Node, either by wrapping or unwrapping the DOM Node.
     * <p>
     * This method is intended for internal use.
     * @param source the wrapped or unwrapped DOM Node
     * @param config the Saxon configuration
     * @return a Saxon NodeInfo object obtained by wrapping or unwrapping the supplied DOM node.
     * @deprecated since 9.0: use {@link Configuration#unravel}
     */

    public static NodeInfo unravel(Source source, Configuration config) {
        return config.unravel(source);
    }

    /**
     * Transform a source XML document supplied as a tree. <br>
     * <p>
     * This method is intended for internal use. External applications should use
     * the {@link #transform} method, which is part of the JAXP interface. Note that
     * <code>NodeInfo</code> implements the JAXP <code>Source</code> interface, so
     * it may be supplied directly to the transform() method.
     *
     * @exception XPathException if any dynamic error occurs
     * @param startNode A Node that identifies the source document to be
     *     transformed and the node where the transformation should start.
     *     May be null if the transformation is to start using an initial template.
     * @param result The output destination
     */

    public void transformDocument(NodeInfo startNode, Result result)
    throws TransformerException {
        // System.err.println("*** TransformDocument");
        if (executable==null) {
            throw new XPathException("Stylesheet has not been compiled");
        }

        if (getMessageEmitter() == null) {
            Receiver me = makeMessageEmitter();
            setMessageEmitter(me);
            if (me instanceof Emitter && ((Emitter)me).getWriter()==null) {
                try {
                    ((Emitter)me).setWriter(new OutputStreamWriter(System.err));
                } catch (Exception err) {
                    // This has been known to fail on .NET because the default encoding set for the
                    // .NET environment is not supported by the Java class library. So we'll try again
                    try {
                        ((Emitter)me).setWriter(new OutputStreamWriter(System.err, "utf8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new XPathException(e);
                    }
                }
            }
        }
        getMessageEmitter().open();

        // Determine whether we need to close the output stream at the end. We
        // do this if the Result object is a StreamResult and is supplied as a
        // system ID, not as a Writer or OutputStream

        boolean mustClose = (result instanceof StreamResult &&
                ((StreamResult)result).getOutputStream() == null);

        principalResult = result;
        if (principalResultURI == null) {
            principalResultURI = result.getSystemId();
        }

        XPathContextMajor initialContext = newXPathContext();
        initialContext.setOriginatingConstructType(Location.CONTROLLER);

        if (startNode != null) {

            initialContextItem = startNode;
            contextForGlobalVariables = startNode.getRoot();

            if (startNode.getConfiguration()==null) {
                // must be a non-standard document implementation
                throw new TransformerException("The supplied source document must be associated with a Configuration");
            }

            if (!startNode.getConfiguration().isCompatible(preparedStylesheet.getConfiguration())) {
                throw new XPathException(
                        "Source document and stylesheet must use the same or compatible Configurations",
                        SaxonErrorCode.SXXP0004);
            }
            SequenceIterator currentIter = SingletonIterator.makeIterator(startNode);
            if (initialTemplate != null) {
                currentIter.next();
            }
            initialContext.setCurrentIterator(currentIter);
        }

        initializeController();

        // In tracing/debugging mode, evaluate all the global variables first
        if (traceListener != null) {
            preEvaluateGlobals(initialContext);
        }

        Properties xslOutputProps;
        if (localOutputProperties == null) {
            xslOutputProps = executable.getDefaultOutputProperties();
        } else {
            xslOutputProps = localOutputProperties;
        }

        // deal with stylesheet chaining
        String nextInChain = xslOutputProps.getProperty(SaxonOutputKeys.NEXT_IN_CHAIN);
        if (nextInChain != null) {
            String baseURI = xslOutputProps.getProperty(SaxonOutputKeys.NEXT_IN_CHAIN_BASE_URI);
            result = prepareNextStylesheet(nextInChain, baseURI, result);
        }

        // add a property to indicate that this is the implicit result document, which
        // should only be created if either it is non-empty, or no xsl:result-document has been executed

        Properties props = new Properties(xslOutputProps);
        props.setProperty(SaxonOutputKeys.IMPLICIT_RESULT_DOCUMENT, "yes");

        initialContext.changeOutputDestination(props, result, true,
                Configuration.XSLT, Validation.PRESERVE, null);

        // Process the source document using the handlers that have been set up

        if (initialTemplate == null) {
//            SequenceIterator single = SingletonIterator.makeIterator(startNode);
//            initialContext.setCurrentIterator(single);    
            initialContextItem = startNode;
            final Mode mode = getRuleManager().getMode(initialMode, false);
            if (mode == null || (initialMode != null && mode.isEmpty())) {
                throw new XPathException("Requested initial mode " +
                        (initialMode == null ? "" : initialMode.getDisplayName()) +
                        " does not exist", "XTDE0045");
            }
            TailCall tc = ApplyTemplates.applyTemplates(
                                initialContext.getCurrentIterator(),
                                mode,
                                null, null, initialContext, false, 0);
            while (tc != null) {
                tc = tc.processLeavingTail();
            }
        } else {
            Template t = initialTemplate;
            XPathContextMajor c2 = initialContext.newContext();
            initialContext.setOriginatingConstructType(Location.CONTROLLER);
            c2.openStackFrame(t.getStackFrameMap());
            c2.setLocalParameters(new ParameterSet());
            c2.setTunnelParameters(new ParameterSet());

            TailCall tc = t.expand(c2);
            while (tc != null) {
                tc = tc.processLeavingTail();
            }
        }

        if (traceListener!=null) {
            traceListener.close();
        }

        Receiver out = initialContext.getReceiver();
        if (out instanceof ComplexContentOutputter && ((ComplexContentOutputter)out).contentHasBeenWritten()) {
            if (principalResultURI != null) {
                if (!checkUniqueOutputDestination(principalResultURI)) {
                    XPathException err = new XPathException(
                            "Cannot write more than one result document to the same URI, or write to a URI that has been read: " +
                            result.getSystemId());
                    err.setErrorCode("XTDE1490");
                    throw err;
                } else {
                    addUnavailableOutputDestination(principalResultURI);
                }
            }
        }

        out.endDocument();
        out.close();
        getMessageEmitter().close();

        if (mustClose && result instanceof StreamResult) {
            OutputStream os = ((StreamResult)result).getOutputStream();
            if (os != null) {
                try {
                    os.close();
                } catch (java.io.IOException err) {
                    throw new XPathException(err);
                }
            }
        }

    }

    /**
     * Pre-evaluate global variables (when debugging/tracing).
     * <p>
     * This method is intended for internal use.
     * @param context the dynamic context for evaluating the global variables
     * @throws XPathException if a dynamic error occurs while evaluating the global variables.
     */

    public void preEvaluateGlobals(XPathContext context) throws XPathException {
        HashMap vars = getExecutable().getCompiledGlobalVariables();
        if (vars != null) {
            Iterator iter = vars.values().iterator();
            while (iter.hasNext()) {
                GlobalVariable var = (GlobalVariable)iter.next();
                var.evaluateVariable(context);
            }
        }
    }

    /**
     * Prepare another stylesheet to handle the output of this one.
     * <p>
     * This method is intended for internal use, to support the
     * <code>saxon:next-in-chain</code> extension.
     *
     * @exception XPathException if any dynamic error occurs
     * @param href URI of the next stylesheet to be applied
     * @param baseURI base URI for resolving href if it's a relative
     *     URI
     * @param result the output destination of the current stylesheet
     * @return a replacement destination for the current stylesheet
     */

    public Result prepareNextStylesheet(String href, String baseURI, Result result)
    throws TransformerException {

        PreparedStylesheet next = preparedStylesheet.getCachedStylesheet(href, baseURI);

        if (next == null) {
            Source source = null;
            if (userURIResolver != null) {
                source = userURIResolver.resolve(href, baseURI);
            }
            if (source == null) {
                source = standardURIResolver.resolve(href, baseURI);
            }
            TransformerFactoryImpl factory = new TransformerFactoryImpl();
            factory.setConfiguration(config);
            next = (PreparedStylesheet)factory.newTemplates(source);
            preparedStylesheet.putCachedStylesheet(href, baseURI, next);
        }

        TransformerReceiver nextTransformer =
                new TransformerReceiver((Controller) next.newTransformer());

        nextTransformer.setSystemId(principalResultURI);
        nextTransformer.setPipelineConfiguration(makePipelineConfiguration());
        nextTransformer.setResult(result);
        nextTransformer.open();

        return nextTransformer;
    }

    //////////////////////////////////////////////////////////////////////////
    // Handle parameters to the transformation
    //////////////////////////////////////////////////////////////////////////

    /**
     * Set a parameter for the transformation.
     * <p>
     * The following table shows some of the classes that are supported
     * by this method. (Others may also be supported, but continued support is
     * not guaranteed.) Each entry in the table shows first the Java class of the
     * supplied object, and then the type of the resulting XPath value.
     * <p>
     * <table>
     * <thead>
     *   <tr><th>Java Class</th><th>XPath 2.0 type</th></tr>
     * </thead>
     * <tbody>
     *   <tr><td>String</td><td>xs:string</td></tr>
     *   <tr><td>Boolean</td><td>xs:boolean</td></tr>
     *   <tr><td>Integer</td><td>xs:integer</td></tr>
     *   <tr><td>Long</td><td>xs:integer</td></tr>
     *   <tr><td>Double</td><td>xs:double</td></tr>
     *   <tr><td>Float</td><td>xs:float</td></tr>
     *   <tr><td>BigDecimal</td><td>xs:decimal</td></tr>
     *   <tr><td>BigInteger</td><td>xs:integer</td></tr>
     *   <tr><td>Date</td><td>xs:dateTime</td></tr>
     *   <tr><td>Array or List of any of the above</td><td>sequence of the above</td></tr>
     *   <tr><td>null</td><td>empty sequence</td></tr>
     * </tbody></table>
     * <p>
     * A node may be supplied as a <code>NodeInfo</code> object, a sequence of nodes
     * as an array or List of <code>NodeInfo</code> objects.
     * <p>
     * In addition, any object that implements the Saxon {@link org.orbeon.saxon.value.Value} interface
     * may be supplied, and will be used without conversion.
     * <p>
     * A node belong to an external object model (such as DOM, JDOM, or XOM) may be supplied provided (a)
     * that the external object model is registered with the Configuration, and (b) that the node is part
     * of a document tree that has been registered in the document pool.
     *
     * @param expandedName The name of the parameter in {uri}local format
     * @param value The value object.  This must follow the rules above.
     * Other formats in addition to those listed above may be accepted.
     * @since 8.4
     */

    public void setParameter(String expandedName, Object value) {

        if (parameters == null) {
            parameters = new GlobalParameterSet();
        }

        parameters.put(StructuredQName.fromClarkName(expandedName), value);

    }

    /**
     * Supply a parameter using Saxon-specific representations of the name and value
     * @param qName The structured representation of the parameter name
     * @param value The value of the parameter, or null to remove a previously set value
     */

    public void setParameter(StructuredQName qName, ValueRepresentation value) {
        if (parameters == null) {
            parameters = new GlobalParameterSet();
        }
        parameters.put(qName, value);
    }

    /**
     * Reset the parameters to a null list.
     */

    public void clearParameters() {
        parameters = null;
    }

    /**
     * Get a parameter to the transformation. This returns the value of a parameter
     * that has been previously set using the {@link #setParameter} method. The value
     * is returned exactly as supplied, that is, before any conversion to an XPath value.
     *
     * @param expandedName the name of the required parameter, in
     *     "{uri}local-name" format
     * @return the value of the parameter, if it exists, or null otherwise
     */

    public Object getParameter(String expandedName) {
        if (parameters==null) {
            return null;
        }
        return parameters.get(StructuredQName.fromClarkName(expandedName));
    }

    /**
     * Get an iterator over the names of global parameters that have been defined
     * @return an Iterator whose items are strings in the form of Clark names, that is {uri}local
     */

    public Iterator iterateParameters() {
        if (parameters == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        int k = parameters.getNumberOfKeys();
        List list = new ArrayList(k);
        Collection keys = parameters.getKeys();
        for (Iterator it = keys.iterator(); it.hasNext();) {
            StructuredQName qName = (StructuredQName)it.next();
            String clarkName = qName.getClarkName();
            list.add(clarkName);
        }
        return list.iterator();
    }

    /**
     * Set the current date and time for this query or transformation.
     * This method is provided primarily for testing purposes, to allow tests to be run with
     * a fixed date and time. The supplied date/time must include a timezone, which is used
     * as the implicit timezone.
     *
     * <p>Note that comparisons of date/time values currently use the implicit timezone
     * taken from the system clock, not from the value supplied here.</p>
     *
     * @param dateTime the date/time value to be used as the current date and time
     * @throws IllegalStateException if a current date/time has already been
     * established by calling getCurrentDateTime(), or by a previous call on setCurrentDateTime()
     */

    public void setCurrentDateTime(DateTimeValue dateTime) throws XPathException {
        if (currentDateTime==null) {
            if (dateTime.getComponent(Component.TIMEZONE) == null) {
                throw new XPathException("No timezone is present in supplied value of current date/time");
            }
            currentDateTime = dateTime;
            dateTimePreset = true;
        } else {
            throw new IllegalStateException(
                    "Current date and time can only be set once, and cannot subsequently be changed");
        }
    }

    /**
     * Get the current date and time for this query or transformation.
     * All calls during one transformation return the same answer.
     *
     * @return Get the current date and time. This will deliver the same value
     *      for repeated calls within the same transformation
     */

    public DateTimeValue getCurrentDateTime() {
        if (currentDateTime==null) {
            currentDateTime = new DateTimeValue(new GregorianCalendar(), true);
        }
        return currentDateTime;
    }

    /**
     * Get the implicit timezone for this query or transformation
     * @return the implicit timezone as an offset in minutes
     */

    public int getImplicitTimezone() {
        return getCurrentDateTime().getTimezoneInMinutes();
    }

    /////////////////////////////////////////
    // Methods for handling dynamic context
    /////////////////////////////////////////

    /**
     * Make an XPathContext object for expression evaluation.
     * <p>
     * This method is intended for internal use.
     *
     * @return the new XPathContext
     */

    public XPathContextMajor newXPathContext() {
        return new XPathContextMajor(this);
    }

    /**
     * Set the last remembered node, for node numbering purposes.
     * <p>
     * This method is strictly for internal use only.
     *
     * @param node the node in question
     * @param number the number of this node
     */

    public void setRememberedNumber(NodeInfo node, int number) {
        lastRememberedNode = node;
        lastRememberedNumber = number;
    }

    /**
     * Get the number of a node if it is the last remembered one.
     * <p>
     * This method is strictly for internal use only.
     *
     * @param node the node for which remembered information is required
     * @return the number of this node if known, else -1.
     */

    public int getRememberedNumber(NodeInfo node) {
        if (lastRememberedNode == node) {
            return lastRememberedNumber;
        }
        return -1;
    }

    /**
     * Indicate whether document projection should be used, and supply the PathMap used to control it.
     * Note: this is available only under Saxon-SA.
     * @param pathMap a path map to be used for projecting source documents
     */

    public void setUseDocumentProjection(PathMap pathMap) {
        this.pathMap = pathMap;
    }

    /**
     * Get the path map used for document projection, if any.
     * @return the path map to be used for document projection, if one has been supplied; otherwise null
     */

    public PathMap getPathMapForDocumentProjection() {
        return pathMap;
    }

    /**
     * Set a ClassLoader to be used when loading external classes. Examples of classes that are
     * loaded include SAX parsers, localization modules for formatting numbers and dates,
     * extension functions, external object models. In an environment such as Eclipse that uses
     * its own ClassLoader, this ClassLoader should be nominated to ensure that any class loaded
     * by Saxon is identical to a class of the same name loaded by the external environment.
     * <p>
     * This method is for application use, but is experimental and subject to change.
     *
     * @param loader the ClassLoader to be used.
     */

    public void setClassLoader(ClassLoader loader) {
        classLoader = loader;
    }

    /**
     * Get the ClassLoader supplied using the method {@link #setClassLoader}.
     * If none has been supplied, return null.
     * <p>
     * This method is for application use, but is experimental and subject to change.
     *
     * @return the ClassLoader in use.
     */

    public ClassLoader getClassLoader() {
        return classLoader;
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
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
