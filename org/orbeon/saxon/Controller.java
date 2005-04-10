package net.sf.saxon;
import net.sf.saxon.event.*;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.functions.Component;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trace.*;
import net.sf.saxon.trans.*;
import net.sf.saxon.tree.TreeBuilder;
import net.sf.saxon.value.DateTimeValue;
import org.xml.sax.SAXParseException;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.PrintStream;
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

public class Controller extends Transformer implements InstructionInfoProvider {

    private Configuration config;
    private DocumentInfo principalSourceDocument;
    private Bindery bindery;                // holds values of global and local variables
    private NamePool namePool;
    private DecimalFormatManager decimalFormatManager;
    private Emitter messageEmitter;
    private RuleManager ruleManager;
    private Properties outputProperties;
    private GlobalParameterSet parameters;
    private PreparedStylesheet preparedStylesheet;
    private TraceListener traceListener; // e.g.
    private boolean tracingPaused;
    private URIResolver standardURIResolver;
    private URIResolver userURIResolver;
    private Result principalResult;
    private String principalResultURI;
    private OutputURIResolver outputURIResolver;
    private ErrorListener errorListener;
    private Executable executable;
    private int treeModel = Builder.TINY_TREE;
    private boolean disableStripping = false;
    private Template initialTemplate = null;
    private HashSet allOutputDestinations;
    private DocumentPool sourceDocumentPool;
    private HashMap userDataTable;
    private DateTimeValue currentDateTime;
    private boolean dateTimePreset = false;
    private int initialMode = -1;
    private NodeInfo lastRememberedNode = null;
    private int lastRememberedNumber = -1;
    private ClassLoader classLoader;
//    private int nextLocalDocumentNumber = -1;

    /**
     * Create a Controller and initialise variables. Constructor is protected,
     * the Controller should be created using newTransformer() in the PreparedStylesheet
     * class.
     *
     * @param config The Configuration used by this Controller
     */

    public Controller(Configuration config) {
        this.config = config;
        // create a dummy executable
        executable = new Executable();
        executable.setConfiguration(config);
        executable.setHostLanguage(config.getHostLanguage());
        init();
    }

    /**
     * Create a Controller and initialise variables. Constructor is protected,
     * the Controller should be created using newTransformer() in the PreparedStylesheet
     * class.
     *
     * @param config The Configuration used by this Controller
     * @param executable The executable used by this Controller
     */

    public Controller(Configuration config, Executable executable) {
        this.config = config;
        this.executable = executable;
        init();
    }

    private void init() {
        bindery = new Bindery();
		namePool = NamePool.getDefaultNamePool();
        standardURIResolver = new StandardURIResolver(config);
        userURIResolver = config.getURIResolver();

        outputURIResolver = config.getOutputURIResolver();
        errorListener = config.getErrorListener();
        if (errorListener instanceof StandardErrorListener) {
            // if using a standard error listener, make a fresh one
            // for each transformation, because it is stateful - and also because the
            // host language is now known (a Configuration can serve multiple host languages)
            PrintStream ps = ((StandardErrorListener)errorListener).getErrorOutput();
            errorListener = ((StandardErrorListener)errorListener).makeAnother(executable.getHostLanguage());
            ((StandardErrorListener)errorListener).setErrorOutput(ps);
            ((StandardErrorListener)errorListener).setRecoveryPolicy(
                    config.getRecoveryPolicy());
        }
        sourceDocumentPool = new DocumentPool();
        userDataTable = new HashMap(20);

        TraceListener tracer = config.getTraceListener();
        if (tracer!=null) {
            addTraceListener(tracer);
        }

        setTreeModel(config.getTreeModel());

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
        if (expandedModeName.equals("")) return;
        initialMode = namePool.allocateClarkName(expandedModeName);
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
     *     transformation
     * @see SaxonOutputKeys
     * @since 8.4
     */

    public void setOutputProperties(Properties properties) {
        Enumeration keys = properties.propertyNames();
        while(keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            setOutputProperty(key, properties.getProperty(key));
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
        if (outputProperties == null) {
            if (executable==null) {
                return new Properties();
            } else {
                outputProperties = executable.getDefaultOutputProperties();
            }
        }

        // Make a copy, so that modifications to the returned properties have no effect

        Properties newProps = new Properties();
        Enumeration keys = outputProperties.propertyNames();
        while(keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            newProps.put(key, outputProperties.getProperty(key));
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
     * @see SaxonOutputKeys
     * @since 8.4
     */

    public void setOutputProperty(String name, String value) {
        if (outputProperties == null) {
            outputProperties = getOutputProperties();
        }
        try {
            SaxonOutputKeys.checkOutputProperty(name, value);
        } catch (DynamicError err) {
            throw new IllegalArgumentException(err.getMessage());
        }
        outputProperties.put(name, value);
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
            SaxonOutputKeys.checkOutputProperty(name, null);
        } catch (DynamicError err) {
            throw new IllegalArgumentException(err.getMessage());
        }
        if (outputProperties == null) {
            if (executable==null) {
                return null;
            } else {
                outputProperties = executable.getDefaultOutputProperties();
            }
        }
        return outputProperties.getProperty(name);
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
     * Get the principal result destination.
     * <p>
     * This method is intended for internal use only.
     */

    public Result getPrincipalResult() {
        return principalResult;
    }

    /**
     * Check that an output destination has not been used before.
     * <p>
     * This method is intended for internal use only.
     */

    public boolean checkUniqueOutputDestination(String uri) {
        if (allOutputDestinations == null) {
            allOutputDestinations = new HashSet(20);
        }
        if (allOutputDestinations.contains(uri)) {
            return false;
        }
        allOutputDestinations.add(uri);
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Set the initial named template to be used as the entry point.
     * <p>
     * XSLT 2.0 allows a transformation to start by executing a named template, rather than
     * by matching an initial context node in a source document. This method may eventually
     * be superseded by a standard JAXP method once JAXP supports XSLT 2.0.
     * <p>
     * Although the Saxon command line interface does not allow both a source document and
     * an initial template to be specified, this API has no such restriction.
     * <p>
     * Note that any parameters supplied using {@link #setParameter} are used as the values
     * of global stylesheet parameters. There is no way to supply values for local parameters
     * of the initial template.
     *
     * @param expandedName The expanded name of the template in {uri}local format
     * @throws XPathException if there is no named template with this name
     * @since 8.4
     */

    public void setInitialTemplate(String expandedName) throws XPathException {
        int fingerprint = namePool.allocateClarkName(expandedName);
        Template t = getExecutable().getNamedTemplate(fingerprint);
        if (t == null) {
            DynamicError err = new DynamicError("There is no named template with expanded name "
                                           + expandedName);
            err.setErrorCode("XTDE0040");
            throw err;
        } else {
            initialTemplate = t;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Make a PipelineConfiguration based on the properties of this Controller.
     * <p>
     * This interface is intended primarily for internal use, although it may be necessary
     * for applications to call it directly for use in conjunction with the experimental pull
     * API.
     */

    public PipelineConfiguration makePipelineConfiguration() {
        PipelineConfiguration pipe = new PipelineConfiguration();
        pipe.setConfiguration(getConfiguration());
        pipe.setErrorListener(getErrorListener());
        pipe.setURIResolver(getURIResolver());
        pipe.setController(this);
        if (getExecutable() != null) {
            // can be null for an IdentityTransformer
            pipe.setLocationProvider(getExecutable().getLocationMap());
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

    public Emitter makeMessageEmitter() throws XPathException {
        String emitterClass = config.getMessageEmitterClass();

        Object emitter = config.getInstance(emitterClass, getClassLoader());
        if (!(emitter instanceof Emitter)) {
            throw new DynamicError(emitterClass + " is not an Emitter");
        }
        setMessageEmitter((Emitter)emitter);
        return messageEmitter;
    }

    /**
     * Set the Emitter to be used for xsl:message output.
     * <p>
     * Recent versions of the JAXP interface specify that by default the
     * output of xsl:message is sent to the registered ErrorListener. Saxon
     * does not yet implement this convention. Instead, the output is sent
     * to a default message emitter, which is a slightly customised implementation
     * of the standard Saxon Emitter interface.
     * <p>
     * This interface can be used to change the way in which Saxon outputs
     * xsl:message output.
     * <p>
     * It is not necessary to use this interface in order to change the destination
     * to which messages are written: that can be achieved by obtaining the standard
     * message emitter and calling its {@link Emitter#setWriter} method.
     * <p>
     * This method is intended for use by advanced applications. The Emitter interface
     * itself is not part of the stable Saxon public API.
     *
     * @param emitter The emitter to receive xsl:message output.
     */

    public void setMessageEmitter(Emitter emitter) {
        messageEmitter = emitter;
        messageEmitter.setPipelineConfiguration(makePipelineConfiguration());
    }

    /**
     * Get the Emitter used for xsl:message output. This returns the emitter
     * previously supplied to the {@link #setMessageEmitter} method, or the
     * default message emitter otherwise.
     *
     * @return the Emitter being used for xsl:message output
     */

    public Emitter getMessageEmitter() {
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
     * @return a CharacterMapExpander if one is required, or null if not (for example, if the
     * useMaps argument is an empty string).
     * @throws XPathException if a name in the useMaps property cannot be resolved to a declared
     * character map.
     */

    public CharacterMapExpander makeCharacterMapExpander(String useMaps) throws XPathException {
        CharacterMapExpander characterMapExpander = null;
        HashMap characterMapIndex = getExecutable().getCharacterMapIndex();
        if (useMaps != null && characterMapIndex != null) {
            List characterMaps = new ArrayList(5);
            StringTokenizer st = new StringTokenizer(useMaps);
            while (st.hasMoreTokens()) {
                String expandedName = st.nextToken();
                int f = namePool.getFingerprintForExpandedName(expandedName);
                HashMap map = (HashMap)characterMapIndex.get(new Integer(f));
                if (map==null) {
                    throw new DynamicError("Character map '" + expandedName + "' has not been defined");
                }
                characterMaps.add(map);
            }
            if (characterMaps.size() > 0) {
                characterMapExpander = new CharacterMapExpander();
                characterMapExpander.setCharacterMaps(characterMaps);
            }
        }
        return characterMapExpander;
    }

    /**
     * Get the policy for handling recoverable errors.
     * <p>
     * This method is intended for internal use
     *
     * @return the current policy. This is obtained from the error listener; if the error listener is
     * not a StandardErrorListener, the value RECOVER_WITH_WARNINGS is returned.
     */

    public int getRecoveryPolicy() {
        if (errorListener instanceof StandardErrorListener) {
            return ((StandardErrorListener)errorListener).getRecoveryPolicy();
        } else {
            return Configuration.RECOVER_WITH_WARNINGS;
        }
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
     * @exception DynamicError if the error listener decides not to
     *     recover from the error
     */

    public void recoverableError(XPathException err) throws DynamicError {
        try {
            if (executable.getHostLanguage() == Configuration.XQUERY) {
                errorListener.fatalError(err);
                throw err;
            } else {
                errorListener.error(err);
            }
        } catch (TransformerException e) {
            throw DynamicError.makeDynamicError(e);
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
     * Set the principal source document (used for evaluating global variables).
     * When a transformation is invoked using the {@link #transform} method, the
     * principal source document is set automatically. This method is useful in XQuery,
     * to define an initial context node for evaluating global variables, and also
     * in XSLT 2.0, when the transformation is started by invoking a named template.
     *
     * @param doc The principal source document
     * @since 8.4
     */

    public void setPrincipalSourceDocument(DocumentInfo doc) {
        principalSourceDocument = doc;
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
     * Get the principal source document. This returns the document
     * previously supplied to the {@link #setPrincipalSourceDocument} method, or the
     * principal source document set implicitly using methods such as {@link #transform}.
     * @return the principal source document
     * @since 8.4
     */

    public DocumentInfo getPrincipalSourceDocument() {
        return principalSourceDocument;
    }

    /**
     * Set an object that will be used to resolve URIs used in
     * document(), etc.
     *
     * @param resolver An object that implements the URIResolver interface, or
     *      null.
     */

    public void setURIResolver(URIResolver resolver) {
        // System.err.println("Setting uriresolver to " + resolver + " on " + this);
        userURIResolver = resolver;
    }

    /**
     * Get the URI resolver.
     *
     * @return the user-supplied URI resolver if there is one, or the
     *     system-defined one otherwise (Note, this isn't quite as JAXP
     *     specifies it; it is likely to change to conform to JAXP in a subsequent
     *     release).
     */

    public URIResolver getURIResolver() {
        // TODO: make this conform to JAXP
        return (userURIResolver==null ? standardURIResolver : userURIResolver);
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
            outputURIResolver = StandardOutputResolver.getInstance();
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
     * @param model the required tree model: {@link Builder#STANDARD_TREE} or
     *     {@link Builder#TINY_TREE}
     * @see net.sf.saxon.event.Builder
     * @since 8.4
     */

    public void setTreeModel(int model) {
        treeModel = model;
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
     * @param b the Builder to which the events filtered by this stripper are
     *     to be sent. May be null if the stripper is not being used for filtering
     *     into a Builder.
     * @return the required Stripper. A Stripper may be used in two ways. It acts as
     * a filter applied to an event stream, that can be used to remove the events
     * representing whitespace text nodes before they reach a Builder. Alternatively,
     * it can be used to define a view of an existing tree in which the whitespace
     * text nodes are dynamically skipped while navigating the XPath axes.
     * @since 8.4
     */

    public Stripper makeStripper(Builder b) {
        if (config.isStripsAllWhiteSpace()) {
            if (b==null) {
                return AllElementStripper.getInstance();
            } else {
                Stripper s = new AllElementStripper();
                s.setUnderlyingReceiver(b);
                return s;
            }
        }
        Stripper stripper;
        if (executable==null) {
            stripper = new Stripper(new Mode(Mode.STRIPPER_MODE));
        } else {
            stripper = executable.newStripper();
        }
		stripper.setController(this);
        if (b != null) {
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
     * @param systemId thesystem ID of this document
     */
    public void registerDocument(DocumentInfo doc, String systemId) {
        sourceDocumentPool.add(doc, systemId);
        namePool.allocateDocumentNumber(doc);
    }

    //////////////////////////////////////////////////////////////////////
    // Methods for handling decimal-formats
    //////////////////////////////////////////////////////////////////////


    /**
     * Set the Decimal Format Manager.
     * <p>
     * This method is intended for internal use only.
     *
     * @param manager the Decimal Format Manager. This object is responsible
     *     for maintaining all named and unnamed decimal format declarations
     */
    public void setDecimalFormatManager(DecimalFormatManager manager) {
        decimalFormatManager = manager;
    }

    /**
     * Get the Decimal Format Manager.
     * <p>
     * This method is intended for internal use only.
     *
     * @return the Decimal Format Manager
     */
    public DecimalFormatManager getDecimalFormatManager() {
        if (decimalFormatManager==null) {
            decimalFormatManager = new DecimalFormatManager();
        }
        return decimalFormatManager;
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
     * @param trace the trace listener.
     * @since 8.4
     */

    public void addTraceListener(TraceListener trace) { // e.g.
        traceListener = TraceEventMulticaster.add(traceListener, trace);
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
     * Internal method to create and initialize a controller.
     */

    private void initializeController() {
        setRuleManager(executable.getRuleManager());
        setDecimalFormatManager(executable.getDecimalFormatManager());

        if (traceListener!=null) {
            traceListener.open();
        }

        // get a new bindery, to clear out any variables from previous runs

        bindery = new Bindery();
        executable.initialiseBindery(bindery);

        // create an initial stack frame, used for evaluating standalone expressions,
        // e.g. expressions within the filter of a match pattern. This stack frame
        // never gets closed, but no one will notice.

        //bindery.openStackFrame();

        // if parameters were supplied, set them up

        defineGlobalParameters(bindery);
    }

    /**
     * Define the global parameters of the transformation or query.
     * <p>
     * This method is intended for internal use only
     * @param bindery The Bindery, which holds values of global variables and parameters
     */

    public void defineGlobalParameters(Bindery bindery) {
        bindery.defineGlobalParameters(parameters);
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
            throw new DynamicError("Stylesheet has not been prepared");
        }

        if (!dateTimePreset) {
            currentDateTime = null;     // reset at start of each transformation
        }

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
            if (wrap && (underSource instanceof NodeInfo || underSource instanceof DOMSource)) {
                startNode = prepareInputTree(source);
                registerDocument(startNode.getDocumentRoot(), source.getSystemId());

            } else if (source == null) {
                if (initialTemplate == null) {
                    throw new DynamicError("Either a source document or an initial template must be specified");
                }

            } else {
                // The input is a SAXSource or StreamSource, or
                // a DOMSource with wrap=no: build the document tree

                Builder sourceBuilder = makeBuilder();
                Sender sender = new Sender(makePipelineConfiguration());
                Receiver r = sourceBuilder;
                if (executable.stripsWhitespace()) {
                    r = makeStripper(sourceBuilder);
                }
                if (executable.stripsInputTypeAnnotations()) {
                    r = config.getAnnotationStripper(r);
                }
                sender.send(source, r);
                DocumentInfo doc = (DocumentInfo)sourceBuilder.getCurrentRoot();
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
                    errorListener.fatalError(err);
                }
            } else {
                errorListener.fatalError(err);
            }
            throw err;
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
        NodeInfo start = unravel(source, getConfiguration());
        if (executable.stripsWhitespace() && !disableStripping) {
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
     */

    public static NodeInfo unravel(Source source, Configuration config) {
        List externalObjectModels = config.getExternalObjectModels();
        for (int m=0; m<externalObjectModels.size(); m++) {
            ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
            NodeInfo node = model.unravel(source, config);
            if (node != null) {
                return node;
            }
        }
        if (source instanceof NodeInfo) {
            return (NodeInfo)source;
        }
        return null;
    }

    /**
     * Transform a source XML document supplied as a tree. <br>
     * A new output destination should be created for each source document,
     * by using setOutputDetails().
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

        if (executable==null) {
            throw new DynamicError("Stylesheet has not been compiled");
        }

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
        initialContext.setOrigin(this);

        if (startNode != null) {
            DocumentInfo sourceDoc;
            if (startNode instanceof DocumentInfo) {
                sourceDoc = (DocumentInfo)startNode;
            } else {
                sourceDoc = startNode.getDocumentRoot();
                if (sourceDoc == null) {
                    throw new DynamicError("Source tree must have a document node as its root");
                }
            }

            principalSourceDocument = sourceDoc;

            if (sourceDoc.getConfiguration()==null) {
                // must be a non-standard document implementation
                throw new TransformerException("The supplied source document must be associated with a Configuration");
                //sourceDoc.setConfiguration(getConfiguration());
            }

            if (sourceDoc.getNamePool() != preparedStylesheet.getTargetNamePool()) {
                throw new DynamicError("Source document and stylesheet must use the same name pool");
            }
            SequenceIterator currentIter = SingletonIterator.makeIterator(sourceDoc);
            currentIter.next();
            initialContext.setCurrentIterator(currentIter);
        }

        initializeController();

        // In tracing/debugging mode, evaluate all the global variables first
        if (getConfiguration().getTraceListener() != null) {
            preEvaluateGlobals(initialContext);
        }

        Properties xslOutputProps = executable.getDefaultOutputProperties();

        // overlay the output properties defined via the API
        if (outputProperties!=null) {
            Enumeration props = outputProperties.propertyNames();
            while (props.hasMoreElements()) {
                String p = (String)props.nextElement();
                String v = outputProperties.getProperty(p);
                xslOutputProps.put(p, v);
            }
        }

        // deal with stylesheet chaining
        String nextInChain = xslOutputProps.getProperty(SaxonOutputKeys.NEXT_IN_CHAIN);
        if (nextInChain != null) {
            String baseURI = xslOutputProps.getProperty(SaxonOutputKeys.NEXT_IN_CHAIN_BASE_URI);
            result = prepareNextStylesheet(nextInChain, baseURI, result);
        }

        initialContext.changeOutputDestination(xslOutputProps, result, true, Validation.PRESERVE, null);
        initialContext.getReceiver().startDocument(0);

        // Process the source document using the handlers that have been set up

        if (initialTemplate == null) {
            AxisIterator single = SingletonIterator.makeIterator(startNode);
            initialContext.setCurrentIterator(single);
            principalSourceDocument = (startNode==null ? null : startNode.getDocumentRoot());
            if (principalSourceDocument == null) {
                throw new DynamicError("Source tree must be rooted at a document node");
            }
            TailCall tc = ApplyTemplates.applyTemplates(
                                initialContext.getCurrentIterator(),
                                getRuleManager().getMode(initialMode),
                                null, null, initialContext, false, 0);
            while (tc != null) {
                tc = tc.processLeavingTail(initialContext);
            }
        } else {
            Template t = initialTemplate;
            XPathContextMajor c2 = initialContext.newContext();
            c2.setOrigin(this);
            c2.openStackFrame(t.getStackFrameMap());
            c2.setLocalParameters(new ParameterSet());
            c2.setTunnelParameters(new ParameterSet());

            TailCall tc = t.expand(c2);
            while (tc != null) {
                tc = tc.processLeavingTail(c2);
            }
        }

        if (traceListener!=null) {
            traceListener.close();
        }

        initialContext.getReceiver().endDocument();
        initialContext.getReceiver().close();

        if (mustClose && result instanceof StreamResult) {
            OutputStream os = ((StreamResult)result).getOutputStream();
            if (os != null) {
                try {
                    os.close();
                } catch (java.io.IOException err) {
                    throw new DynamicError(err);
                }
            }
        }

    }

    /**
     * Pre-evaluate global variables (when debugging/tracing).
     * <p>
     * This method is intended for internal use.
     */

    public void preEvaluateGlobals(XPathContext context) throws XPathException {
        HashMap vars = getExecutable().getGlobalVariableIndex();
        Iterator iter = vars.values().iterator();
        while (iter.hasNext()) {
            GlobalVariable var = (GlobalVariable)iter.next();
            var.evaluateVariable(context);
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

        // TODO: should cache the results, we are recompiling the referenced
        // stylesheet each time it's used

        Source source = getURIResolver().resolve(href, baseURI);
        TransformerFactoryImpl factory = new TransformerFactoryImpl();
        factory.setConfiguration(config);
        Templates next = factory.newTemplates(source);
        TransformerReceiver nextTransformer =
                new TransformerReceiver((Controller) next.newTransformer());

        nextTransformer.setSystemId(principalResultURI);
        nextTransformer.setPipelineConfiguration(makePipelineConfiguration());
        nextTransformer.setResult(result);

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
     * In addition, any object that implements the Saxon {@link net.sf.saxon.value.Value} interface
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

        int fingerprint = namePool.allocateClarkName(expandedName);
        parameters.put(fingerprint, value);

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
        if (parameters==null) return null;
        int f = namePool.allocateClarkName(expandedName);
        return parameters.get(f);
    }

    /**
     * Set the current date and time for this query or transformation.
     * This method is provided primarily for testing purposes, to allow tests to be run with
     * a fixed date and time. The supplied date/time must include a timezone, which is used
     * as the implicit timezone. Calls are ignored if a current date/time has already been
     * established by calling getCurrentDateTime().
     *
     * <p>Note that comparisons of date/time values currently use the implicit timezone
     * taken from the system clock, not from the value supplied here.</p>
     */

    public void setCurrentDateTime(DateTimeValue dateTime) throws XPathException {
        if (currentDateTime==null) {
            if (dateTime.getComponent(Component.TIMEZONE) == null) {
                throw new DynamicError("No timezone is present in supplied value of current date/time");
            }
            currentDateTime = dateTime;
            dateTimePreset = true;
        }
    }

    /**
     * Get the current date and time for this query or transformation.
     * All calls during one transformation return the same answer.
     * <p>
     * This method is intended for internal use.
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
        if (lastRememberedNode == node) return lastRememberedNumber;
        return -1;
    }

    /**
     * Get diagnostic information about this context.
     * <p>
     * This method is intended for internal use.
     */

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = new InstructionDetails();
        details.setConstructType(Location.CONTROLLER);
        return details;
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
        this.classLoader = loader;
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

    /**
     * Allocate a local document number: that is, a document number for a document
     * that is used only locally within this query or transformation (a temporary tree).
     * Local document numbers are allocated by the controller to avoid the synchronization
     * overhead of allocating a global document number from the NamePool. Local document
     * numbers are always negative, global document numbers are positive.
     */

//    public int allocateLocalDocumentNumber() {
//        return nextLocalDocumentNumber--;
//    }
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
