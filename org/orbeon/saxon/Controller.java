package net.sf.saxon;
import net.sf.saxon.dom.DocumentWrapper;
import net.sf.saxon.event.*;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trace.*;
import net.sf.saxon.trans.DecimalFormatManager;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.RuleManager;
import net.sf.saxon.tree.TreeBuilder;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * <B>Controller</B> processes an XML file, calling registered node handlers
 * when appropriate to process its elements, character content, and attributes.
 * This is Saxon's implementation of the JAXP Transformer class<P>
 *
 * @author Michael H. Kay
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
    private int recoveryPolicy = Configuration.RECOVER_WITH_WARNINGS;
    private int treeModel = Builder.TINY_TREE;
    private boolean disableStripping = false;
    private Template initialTemplate = null;

    private DocumentPool sourceDocumentPool;
    private HashMap userDataTable;
    private GregorianCalendar currentDateTime;
    private int initialMode = -1;
    private NodeInfo lastRememberedNode = null;
    private int lastRememberedNumber = -1;

    /**
     * Create a Controller and initialise variables. Constructor is protected,
     * the Controller should be created using newTransformer() in the PreparedStylesheet
     * class.
     *
     * @param config The Configuration used by this Controller
     */

    public Controller(Configuration config) {
        this.config = config;
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
            // for each transformation, because it is stateful
            PrintStream ps = ((StandardErrorListener)errorListener).getErrorOutput();
            errorListener = ((StandardErrorListener)errorListener).makeAnother();
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

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the initial mode for the transformation.
     *
     * @param expandedModeName the name of the initial mode.  The mode is
     *     supplied as an expanded QName, that is "localname" if there is no
     *     namespace, or "{uri}localname" otherwise
     */

    public void setInitialMode(String expandedModeName) {
        if (expandedModeName==null) return;
        if (expandedModeName.equals("")) return;
        initialMode = namePool.allocateClarkName(expandedModeName);
    }


    //////////////////////////////////////////////////////////////////////////
    // Methods to process the tree
    //////////////////////////////////////////////////////////////////////////


    /**
     * Process a Document.<p>
     * This method is intended for use when performing a pure Java transformation,
     * without a stylesheet. Where there is an XSLT stylesheet, use transformDocument()
     * or transform() instead: those methods set up information from the stylesheet before calling
     * run(). <p>
     * The process starts by calling the registered node
     * handler to process the supplied node. Note that the same document can be processed
     * any number of times, typically with different node handlers for each pass. The NodeInfo
     * will typically be the root of a tree built using net.sf.saxon.event.Builder.<p>
     *
     * @param context The initial context; the context node is the one at which processing should start
     * @exception XPathException if the transformation fails for any
     *     reason
     */

//    public void run(XPathContext context) throws XPathException
//    {
//        NodeInfo node = (NodeInfo)context.getContextItem();
//        principalSourceDocument = node.getDocumentRoot();
//        if (principalSourceDocument == null) {
//            throw new DynamicError("Source tree must be rooted at a document node");
//        }
//        XPathContextMajor c2 = context.newContext();
//        c2.setOrigin(this);
//        TailCall tc = ApplyTemplates.applyTemplates(
//                            context.getCurrentIterator(),
//                            getRuleManager().getMode(initialMode),
//                            null, null, c2);
//        while (tc != null) {
//            tc = tc.processLeavingTail(context);
//        }
//    }





    ////////////////////////////////////////////////////////////////////////////////
    // Methods for managing output destinations and formatting
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Set the output properties for the transformation.  These
     * properties will override properties set in the templates
     * with xsl:output.
     *
     * @param properties the output properties to be used for the
     *     transformation
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
     *
     * @return the output properties being used for the transformation,
     *     including properties defined in the stylesheet for the unnamed
     *     output format
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
     *
     * @param name the name of the property
     * @param value the value of the property
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
     *
     * @param name the name of the requested property
     * @return the value of the requested property
     * @see net.sf.saxon.event.SaxonOutputKeys
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
     * Set the base output URI (as it is known in the XSLT spec).
     * This defaults to the system ID of the principal Result object, but
     * a different value can be set for use where there is no principal result.
     * The command line interface sets this to the current working directory.
     */

    public void setBaseOutputURI(String uri) {
        principalResultURI = uri;
    }

    /**
     * Get the URI of the principal result destination.
     *
     * @return the URI, as a String
     */

    public String getPrincipalResultURI() {
        return principalResultURI;
    }

    /**
     * Get the principal result destination
     */

    public Result getPrincipalResult() {
        return principalResult;
    }


    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Set the initial named template to be used as the entry point
     * @param expandedName The expanded name of the template in {uri}local format
     * @throws XPathException if there is no named template with this name
     */

    public void setInitialTemplate(String expandedName) throws XPathException {
        int fingerprint = namePool.allocateClarkName(expandedName);
        Template t = getExecutable().getNamedTemplate(fingerprint);
        if (t == null) {
            DynamicError err = new DynamicError("There is no named template with expanded name "
                                           + expandedName);
            err.setErrorCode("XT0040");
            throw err;
        } else {
            initialTemplate = t;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Make a PipelineConfiguration based on the properties of this Controller
     */

    public PipelineConfiguration makePipelineConfiguration() {
        PipelineConfiguration pipe = new PipelineConfiguration();
        pipe.setConfiguration(getConfiguration());
        pipe.setErrorListener(getErrorListener());
        pipe.setURIResolver(getURIResolver());
        if (getExecutable() != null) {
            // can be null for an IdentityTransformer
            pipe.setLocationProvider(getExecutable().getLocationMap());
        }
        return pipe;
    }

    /**
     * Make an Emitter to be used for xsl:message output.
     *
     * @exception XPathException if any dynamic error occurs; in
     *     particular, if the registered MessageEmitter class is not an
     *     Emitter
     * @return The newly constructed message Emitter
     */

    public Emitter makeMessageEmitter() throws XPathException {
        String emitterClass = config.getMessageEmitterClass();

        Object emitter = Loader.getInstance(emitterClass);
        if (!(emitter instanceof Emitter)) {
            throw new DynamicError(emitterClass + " is not an Emitter");
        }
        setMessageEmitter((Emitter)emitter);
        return messageEmitter;
    }

    /**
     * Set the Emitter to be used for xsl:message output
     */

    public void setMessageEmitter(Emitter emitter) {
        messageEmitter = emitter;
        messageEmitter.setPipelineConfiguration(makePipelineConfiguration());
    }

    /**
     * Get the Emitter used for xsl:message output.
     *
     * @return the Emitter being used for xsl:message output
     */

    public Emitter getMessageEmitter() {
       return messageEmitter;
    }

    /**
     * Get the policy for handling recoverable errors.
     *
     * @return the current policy, as set by setRecoveryPolicy
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
     *
     * @param err An exception holding information about the error
     * @exception DynamicError if the error listener decides not to
     *     recover from the error
     */

    public void recoverableError(XPathException err) throws DynamicError {
        try {
            if (config.getHostLanguage() == Configuration.XQUERY) {
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
     *
     * @return the Executable (which represents the compiled stylesheet)
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Get the document pool. This is used only for source documents, not for stylesheet modules
     *
     * @return the source document pool
     */

    public DocumentPool getDocumentPool() {
        return sourceDocumentPool;
    }

    /**
     * Clear the document pool.
     * This is sometimes useful when using the same Transformer
     * for a sequence of transformations, but it isn't done automatically, because when
     * the transformations use common look-up documents, the caching is beneficial.
     */

    public void clearDocumentPool() {
        sourceDocumentPool = new DocumentPool();
    }

    /**
     * Set line numbering (of the source document) on or off.
     *
     * @param onOrOff true to switch line numbering on; false to switch it off
     */

    public void setLineNumbering(boolean onOrOff) {
    }

    /**
     * Set the principal source document (used for evaluating global variables)
     */

    public void setPrincipalSourceDocument(DocumentInfo doc) {
        principalSourceDocument = doc;
    }

    /**
     * Get the current bindery.
     *
     * @return the Bindery (in which values of all variables are held)
     */

    public Bindery getBindery() {
        return bindery;
    }

    /**
     * Get the principal source document
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
     * Get the primary URI resolver.
     *
     * @return the user-supplied URI resolver if there is one, or the
     *     system-defined one otherwise (Note, this isn't quite as JAXP
     *     specifies it).
     */

    public URIResolver getURIResolver() {
        return (userURIResolver==null ? standardURIResolver : userURIResolver);
    }

    /**
     * Get the fallback URI resolver.
     *
     * @return the the system-defined URIResolver
     */

    public URIResolver getStandardURIResolver() {
        return standardURIResolver;
    }

     /**
     * Set the URI resolver for secondary output documents.
     *
     * @param resolver An object that implements the OutputURIResolver
     *     interface, or null.
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
     */

    public OutputURIResolver getOutputURIResolver() {
        return outputURIResolver;
    }

    /**
     * Get the KeyManager.
     *
     * @return the KeyManager, which holds details of all key declarations
     */

    public KeyManager getKeyManager() {
        return executable.getKeyManager();
    }

// --Recycle Bin START (30/06/03 19:50):
//	/**
//	 * Set the name pool to be used.
//	 *
//	 * @param pool the name pool to be used
//	 */
//
//	public void setConfiguration(NamePool pool) {
//		namePool = pool;
//	}
// --Recycle Bin STOP (30/06/03 19:50)

	/**
	 * Get the name pool in use.
	 *
	 * @return the name pool in use
	 */

	public NamePool getNamePool() {
		return namePool;
	}

    /**
     * Set the tree data model to use.
     *
     * @param model the required tree model: Builder.STANDARD_TREE or
     *     Builder.TINY_TREE
     * @see net.sf.saxon.event.Builder
     */

    public void setTreeModel(int model) {
        treeModel = model;
    }

// --Recycle Bin START (30/06/03 19:50):
//    /**
//     * Get the tree model in use.
//     *
//     * @return the tree model in use
//     * @see net.sf.saxon.event.Builder
//     */
//
//    public int getTreeModel() {
//        return treeModel;
//    }
// --Recycle Bin STOP (30/06/03 19:50)

// --Recycle Bin START (30/06/03 19:48):
//    /**
//     * Disable whitespace stripping.
//     *
//     * @param disable true if whitespace stripping is to be disabled, false if
//     *      it is to be enabled
//     */
//
//    public void disableWhitespaceStripping(boolean disable) {
//        disableStripping = disable;
//    }
// --Recycle Bin STOP (30/06/03 19:48)

// --Recycle Bin START (30/06/03 19:48):
//    /**
//     * Determine if whitespace stripping is disabled.
//     *
//     * @return true if whitespace stripping is disabled
//     */
//
//    public boolean isWhitespaceStrippingDisabled() {
//        return disableStripping;
//    }
// --Recycle Bin STOP (30/06/03 19:48)

    /**
     * Make a builder for the selected tree model.
     *
     * @return an instance of the Builder for the chosen tree model
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
     *
     * @param b the Builder to which the events filtered by this stripper are
     *     to be sent. May be null if the stripper is not being used for filtering
     *     into a Builder.
     * @return the required stripper
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
            stripper = new Stripper(new Mode(true));
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
     *
     * @param manager the Decimal Format Manager. This object is responsible
     *     for maintaining all named and unnamed decimal format declarations
     */
    public void setDecimalFormatManager(DecimalFormatManager manager) {
        decimalFormatManager = manager;
    }

    /**
     * Get the Decimal Format Manager.
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
     *
     * @param r the Rule Manager
     */
    public void setRuleManager(RuleManager r) {
        ruleManager = r;
    }

    /**
     * Get the Rule Manager.
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
     * Get the TraceListener.
     *
     * @return the TraceListener used for XSLT instruction tracing
     */
    public TraceListener getTraceListener() { // e.g.
        return traceListener;
    }

    /**
     * Test whether instruction execution is being traced.
     *
     * @return true if tracing is active, false otherwise
     */
    public final boolean isTracing() { // e.g.
        return traceListener != null && !tracingPaused;
    }

    /**
     * Pause or resume tracing.
     *
     * @param pause true if tracing is to pause; false if it is to resume
     */
    public final void pauseTracing(boolean pause) {
        tracingPaused = pause;
    }

    /**
     * Adds the specified trace listener to receive trace events from
     * this instance.
     * Must be called before the invocation of the render method.
     *
     * @param trace the trace listener.
     */

    public void addTraceListener(TraceListener trace) { // e.g.
        traceListener = TraceEventMulticaster.add(traceListener, trace);
    }

    /**
     * Removes the specified trace listener so that the next invocation
     * of the render method will not send trace events to the listener.
     *
     * @param trace the trace listener.
     */

    public void removeTraceListener(TraceListener trace) { // e.g.
        traceListener = TraceEventMulticaster.remove(traceListener, trace);
    }

    /**
     * Associate this Controller with a compiled stylesheet.
     *
     * @param sheet the compiled stylesheet
     */

    public void setPreparedStylesheet(PreparedStylesheet sheet) {
        preparedStylesheet = sheet;
        //styleSheetElement = (XSLStylesheet)sheet.getStyleSheetDocument().getDocumentElement();
        executable = sheet.getExecutable();
        //setOutputProperties(sheet.getOutputProperties());
        // above line deleted for bug 490964 - may have side-effects
    }

    /**
     * Associate this Controller with an Executable. This method is used by the XQuery
     * processor. The Executable object is overkill in this case - the only thing it
     * currently holds are copies of the collation table.
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

    public void defineGlobalParameters(Bindery bindery) {
        bindery.defineGlobalParameters(parameters);
    }



    /////////////////////////////////////////////////////////////////////////
    // Allow user data to be associated with nodes on a tree
    /////////////////////////////////////////////////////////////////////////

    /**
     * Get user data associated with a node.
     * @param node the node to which the data is attached
     * @param name the name of the required property
     * @return the value of the required property
     */
    public Object getUserData(Object node, String name) {
        String key = node.hashCode() + " " + name;
        // System.err.println("getUserData " + name + " on object returning " + userDataTable.get(key));
        return userDataTable.get(key);
    }

    /**
     * Set user data associated with a node (or any other object).
     * @param node
     * @param name
     * @param data
     */
    public void setUserData(Object node, String name, Object data)  {
        // System.err.println("setUserData " + name + " on object to " + data);
        String key = node.hashCode() + " " + name;
        if (data==null) {
            userDataTable.remove(key);
        } else {
            userDataTable.put(key, data);
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

        currentDateTime = null;     // reset at start of each transformation
        //principalResultURI = result.getSystemId();

        try {
            NodeInfo startNode = null;
            boolean wrap = true;
            boolean validate = config.isSchemaValidation();
            Source underSource = source;
            if (source instanceof AugmentedSource) {
                Boolean localWrap = ((AugmentedSource)source).getWrapDocument();
                if (localWrap != null) {
                    wrap = localWrap.booleanValue();
                }
                Boolean localValidate = ((AugmentedSource)source).getSchemaValidation();
                if (localValidate != null) {
                    validate = localValidate.booleanValue();
                }
                if (validate) {
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
     * @param source the input tree. Must be either a DOMSource or a NodeInfo
     * @return the NodeInfo representing the input node, suitably wrapped.
     */

    public NodeInfo prepareInputTree(Source source) {
        NodeInfo start;
        if (source instanceof DOMSource) {
            Node dsnode = ((DOMSource)source).getNode();
            if (dsnode instanceof NodeInfo) {
                start = (NodeInfo)dsnode;
            } else {
                Document dom;
                if (dsnode instanceof Document) {
                    dom = (Document)dsnode;
                } else {
                    dom = dsnode.getOwnerDocument();
                }
                DocumentWrapper docWrapper = new DocumentWrapper(dom, source.getSystemId(), getConfiguration());
                start = docWrapper.wrap(dsnode);
            }
        } else {
            start = (NodeInfo)source;
        }
        if (executable.stripsWhitespace() && !disableStripping) {
            DocumentInfo docInfo = start.getDocumentRoot();
            StrippedDocument strippedDoc = new StrippedDocument(docInfo, makeStripper(null));
            start = strippedDoc.wrap(start);
        }
        return start;
    }

    /**
     * Render a source XML document supplied as a tree. <br>
     * A new output destination should be created for each source document,
     * by using setOutputDetails(). <br>
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
                sourceDoc.setConfiguration(getConfiguration());
            }

            if (sourceDoc.getNamePool() != preparedStylesheet.getTargetNamePool()) {
                throw new DynamicError("Source document and stylesheet must use the same name pool");
            }

            initialContext.setCurrentIterator(SingletonIterator.makeIterator(sourceDoc));
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

        // Process the source document using the handlers that have been set up

        if (initialTemplate == null) {
            initialContext.setCurrentIterator(SingletonIterator.makeIterator(startNode));
            principalSourceDocument = (startNode==null ? null : startNode.getDocumentRoot());
            if (principalSourceDocument == null) {
                throw new DynamicError("Source tree must be rooted at a document node");
            }
            TailCall tc = ApplyTemplates.applyTemplates(
                                initialContext.getCurrentIterator(),
                                getRuleManager().getMode(initialMode),
                                null, null, initialContext, false);
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

            TailCall tc;
            //if (isTracing()) {
            //    tc = t.traceExpand(c2);
            //} else {
                tc = t.expand(c2);
            //}
            while (tc != null) {
                tc = tc.processLeavingTail(c2);
            }
        }

        if (traceListener!=null) {
            traceListener.close();
        }

        //initialContext.resetOutputDestination(null);
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
     * Pre-evaluate global variables (when debugging/tracing)
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
     *
     * @param expandedName The name of the parameter in {uri}local format
     * @param value The value object.  This can be any valid Java
     *     object  it follows the same conversion rules as a value returned
     *     from a Saxon extension function.
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
     * Get a parameter to the transformation.
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
     * Get the current date and time for this transformation.
     * All calls during one transformation return the same answer.
     *
     * @return Get the current date and time. This will deliver the same value
     *      for repeated calls within the same transformation
     */

    public GregorianCalendar getCurrentDateTime() {
        if (currentDateTime==null) {
            currentDateTime = new GregorianCalendar();
        }
        return currentDateTime;
    }

    /////////////////////////////////////////
    // Methods for handling dynamic context
    /////////////////////////////////////////

    /**
     * Make an XPathContext object for expression evaluation.
     * @return the new XPathContext
     */

    public XPathContextMajor newXPathContext() {
        return new XPathContextMajor(this);
    }

    /**
     * Set the last remembered node, for node numbering purposes.
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
     *
     * @param node the node for which remembered information is required
     * @return the number of this node if known, else -1.
     */

    public int getRememberedNumber(NodeInfo node) {
        if (lastRememberedNode == node) return lastRememberedNumber;
        return -1;
    }

    /**
     * Get diagnostic information about this context
     */

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = new InstructionDetails();
        details.setConstructType(Location.CONTROLLER);
        return details;
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
