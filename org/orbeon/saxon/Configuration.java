package org.orbeon.saxon;

import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.StandardOutputResolver;
import org.orbeon.saxon.expr.Optimizer;
import org.orbeon.saxon.functions.ExtensionFunctionFactory;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.functions.JavaExtensionLibrary;
import org.orbeon.saxon.functions.VendorFunctionLibrary;
import org.orbeon.saxon.instruct.Debugger;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.ExternalObjectModel;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.type.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import java.io.Serializable;
import java.util.*;


/**
 * This class holds details of user-selected configuration options for a transformation
 * or query. When running XSLT, the preferred way of setting configuration options is via
 * the JAXP TransformerFactory interface, but the Configuration object provides a finer
 * level of control. As yet there is no standard API for XQuery, so the only way of setting
 * Configuration information is to use the methods on this class directly.
 * <p>
 * Since Saxon 8.4, the JavaDoc documentation for Saxon attempts to identify interfaces
 * that are considered stable, and will only be changed in a backwards-incompatible way
 * if there is an overriding reason to do so. These interfaces and methods are labelled
 * with the JavaDoc "since" tag. The value 8.n indicates a method in this category that
 * was introduced in Saxon version 8.n: or in the case of 8.4, that was present in Saxon 8.4
 * and possibly in earlier releases. (In some cases, these methods have been unchanged for
 * a long time.) Methods without a "since" tag, although public, are provided for internal
 * use or for use by advanced users, and are subject to change from one release to the next.
 * The presence of a "since" tag on a class or interface indicates that there are one or more
 * methods in the class that are considered stable; it does not mean that all methods are
 * stable.
 *
 * @since 8.4
 */


public class Configuration implements ConversionContext, Serializable {

    private transient URIResolver resolver;
    protected transient ErrorListener listener;
    private int treeModel = Builder.TINY_TREE;
    private boolean lineNumbering = false;
    private TraceListener traceListener = null;
    private FunctionLibrary extensionBinder;
    protected VendorFunctionLibrary vendorFunctionLibrary;
    private int recoveryPolicy = RECOVER_WITH_WARNINGS;
    private String messageEmitterClass = "org.orbeon.saxon.event.MessageEmitter";
    private String sourceParserClass;
    private String styleParserClass;
    private transient OutputURIResolver outputURIResolver;
    private boolean timing = false;
    private boolean versionWarning = true;
    private boolean allowExternalFunctions = true;
    private boolean traceExternalFunctions = false;
    private boolean validation = false;
    private boolean allNodesUntyped = false;
    private boolean lazyConstructionMode = false;
    private NamePool targetNamePool = null;
    private boolean stripsAllWhiteSpace = false;
    private int hostLanguage = XSLT;
    private int schemaValidationMode = Validation.STRIP;
    private boolean validationWarnings = false;
    private boolean retainDTDattributeTypes = false;
    private Debugger debugger = null;
    protected Optimizer optimizer = null;
    private ExtensionFunctionFactory extensionFunctionFactory = new ExtensionFunctionFactory(this);
    private List externalObjectModels = new ArrayList(4);
    private ClassLoader classLoader;
    private int implicitTimezone;

    /**
     * Constant indicating that the processor should take the recovery action
     * when a recoverable error occurs, with no warning message.
     */
    public static final int RECOVER_SILENTLY = 0;
    /**
     * Constant indicating that the processor should produce a warning
     * when a recoverable error occurs, and should then take the recovery
     * action and continue.
     */
    public static final int RECOVER_WITH_WARNINGS = 1;
    /**
     * Constant indicating that when a recoverable error occurs, the
     * processor should not attempt to take the defined recovery action,
     * but should terminate with an error.
     */
    public static final int DO_NOT_RECOVER = 2;

    /**
     * Constant indicating that the host language is XSLT
     */
    public static final int XSLT = 10;

    /**
     * Constant indicating that the host language is XQuery
     */
    public static final int XQUERY = 11;

    /**
     * Constant indicating that the "host language" is XML Schema
     */
    public static final int XML_SCHEMA = 12;

    /**
     * Constant indicating that the host language is Java: that is, this is a free-standing
     * Java application with no XSLT or XQuery content
     */
    public static final int JAVA_APPLICATION = 13;

    /**
     * Constant indicating that the host language is XPATH itself - that is, a free-standing XPath environment
     */
    public static final int XPATH = 14;

    /**
     * Create a configuration object with default settings for all options
     * @since 8.4
     */

    public Configuration() {
        targetNamePool = NamePool.getDefaultNamePool();
        extensionBinder = new JavaExtensionLibrary(this);
        registerStandardObjectModels();

        // Get the implicit timezone from the current system clock
        GregorianCalendar calendar = new GregorianCalendar();
        int tzmsecs = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET));
        implicitTimezone = tzmsecs / 60000;
    }

   /**
    * Get a message used to identify this product when a transformation is run using the -t option
    * @return A string containing both the product name and the product
    *     version
    * @since 8.4
    */

    public String getProductTitle() {
        return "Saxon " + Version.getProductVersion() + " from Saxonica";
    }

    /**
     * Determine if the configuration is schema-aware, for the given host language
     * @param language the required host language: XSLT, XQUERY, or XML_SCHEMA
     * @since 8.4
     */

    public boolean isSchemaAware(int language) {
        return false;
        // changing this to true will do no good!
    }

    /**
     * Display a message about the license status
     */

    public void displayLicenseMessage() {}

    /**
     * Get the host language used in this configuration. The typical values
     * are XSLT and XQUERY. The values XML_SCHEMA and JAVA_APPLICATION may also
     * be encountered.
     * <p>
     * This method is problematic because it is possible to run multiple transformations
     * or queries within the same configuration. The method is therefore best avoided.
     * Instead, use {@link net.sf.saxon.instruct.Executable#getHostLanguage}.
     * Internally its only use is in deciding (in Saxon-SA only) which error listener to
     * use by default at compile time, and since the standard XSLT and XQuery listeners have
     * no differences when used for static errors, the choice is immaterial.
     * @return Configuration.XSLT or Configuration.XQUERY
     */

    public int getHostLanguage() {
        return hostLanguage;
    }

    /**
     * Set the host language used in this configuration. The possible values
     * are XSLT and XQUERY.
     * @param hostLanguage Configuration.XSLT or Configuration.XQUERY
     */

    public void setHostLanguage(int hostLanguage) {
        this.hostLanguage = hostLanguage;
    }

    /**
     * Get the URIResolver used in this configuration
     * @return the URIResolver. If no URIResolver has been set explicitly, the
     * default URIResolver is used.
     * @since 8.4
     */

    public URIResolver getURIResolver() {
        if (resolver==null) {
            resolver = new StandardURIResolver(this);
        }
        return resolver;
    }

    /**
     * Set the URIResolver to be used in this configuration. This will be used to
     * resolve the URIs used statically (e.g. by xsl:include) and also the URIs used
     * dynamically by functions such as document() and doc(). Note that the URIResolver
     * does not resolve the URI in the sense of RFC 2396 (which is also the sense in which
     * the resolve-uri() function uses the term): rather it dereferences an absolute URI
     * to obtain an actual resource, which is returned as a Source object.
     * @param resolver The URIResolver to be used.
     * @since 8.4
     */

    public void setURIResolver(URIResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Create an instance of a URIResolver with a specified class name
     *
     * @exception TransformerException if the requested class does not
     *     implement the javax.xml.transform.URIResolver interface
     * @param className The fully-qualified name of the URIResolver class
     * @return The newly created URIResolver
     */
    public URIResolver makeURIResolver(String className) throws TransformerException {
        Object obj = getInstance(className, null);
        if (obj instanceof URIResolver) {
            return (URIResolver)obj;
        }
        throw new DynamicError("Class " + className + " is not a URIResolver");
    }

    /**
     * Get the ErrorListener used in this configuration. If no ErrorListener
     * has been supplied explicitly, the default ErrorListener is used.
     * @return the ErrorListener.
     * @since 8.4
     */

    public ErrorListener getErrorListener() {
        if (listener == null) {
            listener = new StandardErrorListener();
            ((StandardErrorListener)listener).setRecoveryPolicy(recoveryPolicy);
        }
        return listener;
    }

    /**
     * Set the ErrorListener to be used in this configuration. The ErrorListener
     * is informed of all static and dynamic errors detected, and can decide whether
     * run-time warnings are to be treated as fatal.
     * @param listener the ErrorListener to be used
     * @since 8.4
     */

    public void setErrorListener(ErrorListener listener) {
        this.listener = listener;
    }

    /**
     * Get the Tree Model used by this Configuration. This is either
     * Builder.STANDARD_TREE or Builder.TINY_TREE. The default (confusingly)
     * is Builder.TINY_TREE.
     * @return the selected Tree Model
     * @since 8.4
     */

    public int getTreeModel() {
        return treeModel;
    }

    /**
     * Set the Tree Model used by this Configuration. This is either
     * Builder.STANDARD_TREE or Builder.TINY_TREE. The default (confusingly)
     * is Builder.TINY_TREE.
     * @param treeModel the selected Tree Model
     * @since 8.4
     */

    public void setTreeModel(int treeModel) {
        this.treeModel = treeModel;
    }

    /**
     * Determine whether source documents will maintain line numbers, for the
     * benefit of the saxon:line-number() extension function as well as run-time
     * tracing.
     * @return true if line numbers are maintained in source documents
     * @since 8.4
     */

    public boolean isLineNumbering() {
        return lineNumbering;
    }

    /**
     * Determine whether source documents will maintain line numbers, for the
     * benefit of the saxon:line-number() extension function as well as run-time
     * tracing.
     * @param lineNumbering true if line numbers are maintained in source documents
     * @since 8.4
     */

    public void setLineNumbering(boolean lineNumbering) {
        this.lineNumbering = lineNumbering;
    }

    /**
     * Get the TraceListener used for run-time tracing of instruction execution.
     * @return the TraceListener, or null if none is in use.
     * @since 8.4
     */

    public TraceListener getTraceListener() {
        return traceListener;
    }

    /**
     * Set the TraceListener to be used for run-time tracing of instruction execution.
     * @param traceListener The TraceListener to be used.
     * @since 8.4
     */

    public void setTraceListener(TraceListener traceListener) {
        this.traceListener = traceListener;
    }

    /** Create an instance of a TraceListener with a specified class name
     *
     * @exception net.sf.saxon.trans.XPathException if the requested class does not
     *     implement the net.sf.saxon.trace.TraceListener interface
     * @param className The fully qualified class name of the TraceListener to
     *      be constructed
     * @return the newly constructed TraceListener
     */

    public TraceListener makeTraceListener (String className)
    throws XPathException
    {
        Object obj = getInstance(className, null);
        if (obj instanceof TraceListener) {
            return (TraceListener)obj;
        }
        throw new DynamicError("Class " + className + " is not a TraceListener");
    }

    /**
     * Set the FunctionLibrary used to bind calls on extension functions. This allows the
     * rules for identifying extension functions to be customized (in principle, it would
     * allow support for extension functions in other languages to be provided).
     * <p>
     * When an application supplies its own FunctionLibrary for binding extension functions,
     * this replaces the default binding mechanism for Java extension functions, namely
     * {@link JavaExtensionLibrary}. It thus disables the function libraries
     * for built-in Saxon extensions and for EXSLT extensions. It is possible to create a
     * function library that adds to the existing mechanisms, rather than replacing them,
     * by supplying as the FunctionLibrary a {@link net.sf.saxon.functions.FunctionLibraryList}
     * that itself contains two FunctionLibrary objects: a JavaExtensionLibrary, and a user-written
     * FunctionLibrary.
     * <p>
     * This mechanism is recently introduced and is still experimental.
     * It is intended for advanced users only, and the details are subject to change.
     * @param binder The FunctionLibrary object used to locate implementations of extension
     * functions, based on their name and arity
     * @see #setExtensionFunctionFactory
     */

    public void setExtensionBinder(FunctionLibrary binder) {
        extensionBinder = binder;
    }

    /**
     * Get the FunctionLibrary used to bind calls on extension functions.
     * <p>
     * This mechanism is for advanced users only, and the details are subject to change.
     * @return the registered FunctionLibrary for extension functions if one has been
     * registered; or the default FunctionLibrary for extension functions otherwise
     */

    public FunctionLibrary getExtensionBinder() {
        return extensionBinder;
    }

    /**
     * Get the FunctionLibrary used to bind calls on Saxon-defined extension functions.
     * <p>
     * This method is intended for internal use only.
     */

    public VendorFunctionLibrary getVendorFunctionLibrary() {
        if (vendorFunctionLibrary == null) {
            vendorFunctionLibrary = new VendorFunctionLibrary();
        }
        return vendorFunctionLibrary;
    }
    /**
     * Determine how recoverable run-time errors are to be handled. This applies
     * only if the standard ErrorListener is used.
     * @return the current recovery policy. The options are {@link #RECOVER_SILENTLY},
     * {@link #RECOVER_WITH_WARNINGS}, or {@link #DO_NOT_RECOVER}.
     * @since 8.4
     */

    public int getRecoveryPolicy() {
        return recoveryPolicy;
    }

    /**
     * Determine how recoverable run-time errors are to be handled. This applies
     * only if the standard ErrorListener is used. The recovery policy applies to
     * errors classified in the XSLT 2.0 specification as recoverable dynamic errors,
     * but only in those cases where Saxon provides a choice over how the error is handled:
     * in some cases, Saxon makes the decision itself.
     * @param recoveryPolicy the recovery policy to be used. The options are {@link #RECOVER_SILENTLY},
     * {@link #RECOVER_WITH_WARNINGS}, or {@link #DO_NOT_RECOVER}.
     * @since 8.4
     */

    public void setRecoveryPolicy(int recoveryPolicy) {
        this.recoveryPolicy = recoveryPolicy;
    }

    /**
     * Get the name of the class that will be instantiated to create a MessageEmitter,
     * to process the output of xsl:message instructions in XSLT.
     * @return the full class name of the message emitter class.
     * @since 8.4
     */

    public String getMessageEmitterClass() {
        return messageEmitterClass;
    }

    /**
     * Set the name of the class that will be instantiated to create a MessageEmitter,
     * to process the output of xsl:message instructions in XSLT.
     * @param messageEmitterClass the full class name of the message emitter class. This
     * must implement org.orbeon.saxon.event.Emitter.
     * @since 8.4
     */

    public void setMessageEmitterClass(String messageEmitterClass) {
        this.messageEmitterClass = messageEmitterClass;
    }

    /**
     * Get the name of the class that will be instantiated to create an XML parser
     * for parsing source documents (for example, documents loaded using the document()
     * or doc() functions).
     * <p>
     * This method is retained in Saxon for backwards compatibility, but the preferred way
     * of choosing an XML parser is to use JAXP interfaces, for example by supplying a
     * JAXP Source object initialized with an appropriate implementation of org.xml.sax.XMLReader.
     * @return the fully qualified name of the XML parser class
     */

    public String getSourceParserClass() {
        return sourceParserClass;
    }

    /**
     * Set the name of the class that will be instantiated to create an XML parser
     * for parsing source documents (for example, documents loaded using the document()
     * or doc() functions).
     * <p>
     * This method is retained in Saxon for backwards compatibility, but the preferred way
     * of choosing an XML parser is to use JAXP interfaces, for example by supplying a
     * JAXP Source object initialized with an appropriate implementation of org.xml.sax.XMLReader.
     *
     * @param sourceParserClass the fully qualified name of the XML parser class. This must implement
     * the SAX2 XMLReader interface.
     */

    public void setSourceParserClass(String sourceParserClass) {
        this.sourceParserClass = sourceParserClass;
    }

    /**
     * Get the name of the class that will be instantiated to create an XML parser
     * for parsing stylesheet modules.
     * <p>
     * This method is retained in Saxon for backwards compatibility, but the preferred way
     * of choosing an XML parser is to use JAXP interfaces, for example by supplying a
     * JAXP Source object initialized with an appropriate implementation of org.xml.sax.XMLReader.
     *
     * @return the fully qualified name of the XML parser class
     */

    public String getStyleParserClass() {
        return styleParserClass;
    }

   /**
    * Set the name of the class that will be instantiated to create an XML parser
    * for parsing stylesheet modules.
    * <p>
    * This method is retained in Saxon for backwards compatibility, but the preferred way
    * of choosing an XML parser is to use JAXP interfaces, for example by supplying a
    * JAXP Source object initialized with an appropriate implementation of org.xml.sax.XMLReader.
    *
    * @param styleParserClass the fully qualified name of the XML parser class
    */

    public void setStyleParserClass(String styleParserClass) {
        this.styleParserClass = styleParserClass;
    }

    /**
     * Get the OutputURIResolver that will be used to resolve URIs used in the
     * href attribute of the xsl:result-document instruction.
     * @return the OutputURIResolver. If none has been supplied explicitly, the
     * default OutputURIResolver is returned.
     * @since 8.4
     */

    public OutputURIResolver getOutputURIResolver() {
        if (outputURIResolver==null) {
            outputURIResolver = StandardOutputResolver.getInstance();
        }
        return outputURIResolver;
    }

    /**
     * Set the OutputURIResolver that will be used to resolve URIs used in the
     * href attribute of the xsl:result-document instruction.
     * @param outputURIResolver the OutputURIResolver to be used.
     * @since 8.4
     */

    public void setOutputURIResolver(OutputURIResolver outputURIResolver) {
        this.outputURIResolver = outputURIResolver;
    }

    /**
     * Determine whether brief progress messages and timing information will be output
     * to System.err.
     * <p>
     * This method is provided largely for internal use. Progress messages are normally
     * controlled directly from the command line interfaces, and are not normally used when
     * driving Saxon from the Java API.
     *
     * @return true if these messages are to be output.
     */

    public boolean isTiming() {
        return timing;
    }

    /**
     * Determine whether brief progress messages and timing information will be output
     * to System.err.
     * <p>
     * This method is provided largely for internal use. Progress messages are normally
     * controlled directly from the command line interfaces, and are not normally used when
     *
     * @param timing true if these messages are to be output.
     */

    public void setTiming(boolean timing) {
        this.timing = timing;
    }

    /**
     * Determine whether a warning is to be output when running against a stylesheet labelled
     * as version="1.0". The XSLT specification requires such a warning unless the user disables it.
     * @return true if these messages are to be output.
     * @since 8.4
     */

    public boolean isVersionWarning() {
        return versionWarning;
    }

    /**
     * Determine whether a warning is to be output when running against a stylesheet labelled
     * as version="1.0". The XSLT specification requires such a warning unless the user disables it.
     * @param warn true if these messages are to be output.
     * @since 8.4
     */

    public void setVersionWarning(boolean warn) {
        this.versionWarning = warn;
    }

    /**
     * Determine whether calls to external Java functions are permitted.
     * @return true if such calls are permitted.
     * @since 8.4
     */

    public boolean isAllowExternalFunctions() {
        return allowExternalFunctions;
    }

    /**
     * Determine whether calls to external Java functions are permitted. Allowing
     * external function calls is potentially a security risk if the stylesheet or
     * Query is untrusted, as it allows arbitrary Java methods to be invoked, which can
     * examine or modify the contents of filestore and other resources on the machine
     * where the query/stylesheet is executed
     *
     * @param allowExternalFunctions true if external function calls are to be
     * permitted.
     * @since 8.4
     */

    public void setAllowExternalFunctions(boolean allowExternalFunctions) {
        this.allowExternalFunctions = allowExternalFunctions;
    }

    /**
     * Determine whether calls on external functions are to be traced for diagnostic
     * purposes.
     * @return true if tracing is enabled for calls to external Java functions
     */

    public boolean isTraceExternalFunctions() {
        return traceExternalFunctions;
    }

    /**
     * Determine whether attribute types obtained from a DTD are to be used to set type annotations
     * on the resulting nodes.
     *
     * @param useTypes set to true if DTD types are to be taken into account
     * @since 8.4
     */

    public void setRetainDTDAttributeTypes(boolean useTypes) throws TransformerFactoryConfigurationError {
        if (useTypes && !isSchemaAware(Configuration.XML_SCHEMA)) {
            throw new TransformerFactoryConfigurationError(
                    "Retaining DTD attribute types requires the schema-aware product");
        }
        retainDTDattributeTypes = useTypes;
    }

    /**
     * Determine whether attribute types obtained from a DTD are to be used to set type annotations
     * on the resulting nodes
     *
     * @return true if DTD types are to be taken into account
     * @since 8.4
     */

    public boolean isRetainDTDAttributeTypes() {
        return retainDTDattributeTypes;
    }
    /**
     * Determine whether calls on external functions are to be traced for diagnostic
     * purposes.
     * @param traceExternalFunctions true if tracing is to be enabled
     * for calls to external Java functions
     */

    public void setTraceExternalFunctions(boolean traceExternalFunctions) {
        this.traceExternalFunctions = traceExternalFunctions;
    }

    /**
     * Get an ExtensionFunctionFactory. This is used at compile time for generating
     * the code that calls Java extension functions. It is possible to supply a user-defined
     * ExtensionFunctionFactory to customize the way extension functions are bound.
     * <p>
     * This mechanism is intended for advanced use only, and is subject to change.
     *
     * @return the factory object registered to generate calls on extension functions,
     * if one has been registered; if not, the default factory used by Saxon.
     */

    public ExtensionFunctionFactory getExtensionFunctionFactory() {
        return extensionFunctionFactory;
    }

    /**
     * Set an ExtensionFunctionFactory. This is used at compile time for generating
     * the code that calls Java extension functions. It is possible to supply a user-defined
     * ExtensionFunctionFactory to customize the way extension functions are called. The
     * ExtensionFunctionFactory determines how external methods are called, but is not
     * involved in binding the external method corresponding to a given function name or URI.
     * <p>
     * This mechanism is intended for advanced use only, and is subject to change.
     * @see #setExtensionBinder
     */

    public void setExtensionFunctionFactory(ExtensionFunctionFactory factory) {
        extensionFunctionFactory = factory;
    }
    /**
     * Determine whether the XML parser for source documents will be asked to perform
     * DTD validation of source documents
     * @return true if DTD validation is requested.
     * @since 8.4
     */

    public boolean isValidation() {
        return validation;
    }

    /**
     * Determine whether the XML parser for source documents will be asked to perform
     * DTD validation of source documents
     * @param validation true if DTD validation is to be requested.
     * @since 8.4
     */

    public void setValidation(boolean validation) {
        this.validation = validation;
    }

    /**
     * Specify that all nodes encountered within this query or transformation will be untyped
     */

    public void setAllNodesUntyped(boolean allUntyped) {
        allNodesUntyped = allUntyped;
    }

    /**
     * Determine whether all nodes encountered within this query or transformation are guaranteed to be
     * untyped
     */

    public boolean areAllNodesUntyped() {
        return allNodesUntyped;
    }

    /**
     * Determine whether source documents (supplied as a StreamSource or SAXSource)
     * should be subjected to schema validation
     * @return the schema validation mode previously set using setSchemaValidationMode(),
     * or the default mode {@link Validation#STRIP} otherwise.
     */

    public int getSchemaValidationMode() {
        return schemaValidationMode;
    }

    /**
     * Indicate whether source documents (supplied as a StreamSource or SAXSource)
     * should be subjected to schema validation
     * @param validationMode the validation (or construction) mode to be used for source documents.
     * One of {@link Validation#STRIP}, {@link Validation#PRESERVE}, {@link Validation#STRICT},
     * {@link Validation#LAX}
     * @since 8.4
     */

    public void setSchemaValidationMode(int validationMode) {
        switch (validationMode) {
            case Validation.STRIP:
            case Validation.PRESERVE:
                break;
            case Validation.LAX:
            case Validation.STRICT:
                if (!isSchemaAware(XML_SCHEMA)) {
                    needSchemaAwareVersion();
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported validation mode " + validationMode);
        }
        schemaValidationMode = validationMode;
    }

    /**
     * Indicate whether schema validation failures on result documents are to be treated
     * as fatal errors or as warnings.
     *
     * @param warn true if schema validation failures are to be treated as warnings; false if they
     * are to be treated as fatal errors.
     * @since 8.4
     */

    public void setValidationWarnings(boolean warn) {
        validationWarnings = warn;
    }

    /**
     * Determine whether schema validation failures on result documents are to be treated
     * as fatal errors or as warnings.
     * @return true if validation errors are to be treated as warnings (that is, the
     * validation failure is reported but processing continues as normal); false
     * if validation errors are fatal.
     * @since 8.4
     */

    public boolean isValidationWarnings() {
        return validationWarnings;
    }

    /**
     * Get the target namepool to be used for stylesheets/queries and for source documents.
     * @return the target name pool. If no NamePool has been specified explicitly, the
     * default NamePool is returned.
     * @since 8.4
     */

    public NamePool getNamePool() {
        return targetNamePool;
    }

    /**
     * Set the NamePool to be used for stylesheets/queries and for source documents.
     * <p>
     * Normally all transformations and queries run under a single Java VM share the same
     * NamePool. This creates a potential bottleneck, since changes to the namepool are
     * synchronized. It is possible therefore to allocate a distinct NamePool to each
     * Configuration. This requires considerable care and should only be done when the
     * default arrangement is found to cause problems. There is a basic rule to follow:
     * any compiled stylesheet or query must use the same NamePool as its source and
     * result documents.
     *
     * @param targetNamePool The NamePool to be used.
     * @since 8.4
     */

    public void setNamePool(NamePool targetNamePool) {
        this.targetNamePool = targetNamePool;
    }

    /**
     * Determine whether whitespace-only text nodes are to be stripped unconditionally
     * from source documents.
     * @return true if all whitespace-only text nodes are stripped.
     * @since 8.4
     */

    public boolean isStripsAllWhiteSpace() {
        return stripsAllWhiteSpace;
    }

    /**
     * Determine whether whitespace-only text nodes are to be stripped unconditionally
     * from source documents.
     * @param stripsAllWhiteSpace if all whitespace-only text nodes are to be stripped.
     * @since 8.4
     */

    public void setStripsAllWhiteSpace(boolean stripsAllWhiteSpace) {
        this.stripsAllWhiteSpace = stripsAllWhiteSpace;
    }

    /**
    * Get a parser for source documents.
     * <p>
     * This method is intended primarily for internal use.
    */

    public XMLReader getSourceParser() throws TransformerFactoryConfigurationError {
        XMLReader parser;
        if (getSourceParserClass()!=null) {
            parser = makeParser(getSourceParserClass());
        } else {
            try {
                parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            } catch (ParserConfigurationException err) {
                throw new TransformerFactoryConfigurationError(err);
            } catch (SAXException err) {
                throw new TransformerFactoryConfigurationError(err);
            }
        }
        if (isValidation()) {
            try {
                parser.setFeature("http://xml.org/sax/features/validation", true);
            } catch (SAXException err) {
                throw new TransformerFactoryConfigurationError("The XML parser does not support validation");
            }
        }

        return parser;
    }

    /**
    * Get the parser for stylesheet documents. This parser is also used for schema documents.
     * <p>
     * This method is intended for internal use only.
     *
    */

    public XMLReader getStyleParser() throws TransformerFactoryConfigurationError {
        XMLReader parser;
        if (getStyleParserClass()!=null) {
            parser = makeParser(getStyleParserClass());
        } else {
            try {
                parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            } catch (ParserConfigurationException err) {
                throw new TransformerFactoryConfigurationError(err);
            } catch (SAXException err) {
                throw new TransformerFactoryConfigurationError(err);
            }
        }
        try {
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
            parser.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        } catch (SAXNotRecognizedException e) {
            throw new TransformerFactoryConfigurationError(e);
        } catch (SAXNotSupportedException e) {
            throw new TransformerFactoryConfigurationError(e);
        }
        return parser;
    }

    /**
     * Read a schema from a given schema location
     * @return the target namespace of the schema
     * <p>
     * This method is intended for internal use.
     */

    public String readSchema(PipelineConfiguration pipe, String baseURI, String schemaLocation, String expected)
    throws TransformerConfigurationException {
        needSchemaAwareVersion();
        return null;
    }

    /**
     * Read schemas from a list of schema locations.
     * <p>
     * This method is intended for internal use.
     */

    public void readMultipleSchemas(PipelineConfiguration pipe, String baseURI, List schemaLocations, String expected)
            throws SchemaException {
        needSchemaAwareVersion();
    }


    /**
     * Read an inline schema from a stylesheet.
     * <p>
     * This method is intended for internal use.
     * @param pipe
     * @param root the xs:schema element in the stylesheet
     * @param expected the target namespace expected; null if there is no
     * expectation.
     * @return the actual target namespace of the schema
     *
     */

    public String readInlineSchema(PipelineConfiguration pipe, NodeInfo root, String expected)
            throws SchemaException {
        needSchemaAwareVersion();
        return null;
    }

    private void needSchemaAwareVersion() {
        throw new UnsupportedOperationException(
                "You need the schema-aware version of Saxon for this operation");
    }

    /**
     * Load a schema, which will be available for use by all subsequent operations using
     * this Configuration.
     * @param schemaSource the JAXP Source object identifying the schema document to be loaded
     * @throws SchemaException if the schema cannot be read or parsed or if it is invalid
     * @since 8.4
     */

    public void addSchemaSource(Source schemaSource) throws SchemaException {
        needSchemaAwareVersion();
    }

    /**
     * Add a schema to the cache.
     * <p>
     * This method is intended for internal use
     * @param schema an object of class javax.xml.validation.schema, which is not declared as such
     * to avoid creating a dependency on this JDK 1.5 class
     */

    public void addSchema(Object schema)
    throws TransformerConfigurationException {
        needSchemaAwareVersion();
    }

    /**
     * Get a schema from the cache. Return null if not found.
     * <p>
     * This method is intended for internal use.
     * @return  an object of class javax.xml.validation.schema, which is not declared as such
     * to avoid creating a dependency on this JDK 1.5 class
     */

    public Object getSchema(String namespace) {
        return null;
    }

    /**
     * Get a global element declaration.
     * <p>
     * This method is intended for internal use.
     * @return the element declaration whose name matches the given
     * fingerprint, or null if no element declaration with this name has
     * been registered.
     */

    public SchemaDeclaration getElementDeclaration(int fingerprint) {
        return null;
    }

    /**
     * Get a global attribute declaration.
     * <p>
     * This method is intended for internal use
     * @return the attribute declaration whose name matches the given
     * fingerprint, or null if no element declaration with this name has
     * been registered.
     */

    public SchemaDeclaration getAttributeDeclaration(int fingerprint) {
        return null;
    }

    /**
      * Get the top-level schema type definition with a given fingerprint.
     * <p>
     * This method is intended for internal use and for use by advanced
     * applications. (The SchemaType object returned cannot yet be considered
     * a stable API, and may be superseded when a JAXP API for schema information
     * is defined.)
      * @param fingerprint the fingerprint of the schema type
      * @return the schema type , or null if there is none
      * with this name.
      */

     public SchemaType getSchemaType(int fingerprint) {
        if (fingerprint < 1023) {
            return BuiltInSchemaFactory.getSchemaType(fingerprint);
        }
        return null;
     }

    /**
     * Get a document-level validator to add to a Receiver pipeline.
     * <p>
     * This method is intended for internal use.
     * @param receiver The receiver to which events should be sent after validation
     * @param systemId the base URI of the document being validated
     * @param namePool the namePool to be used by the validator
     * @param validationMode for example Validation.STRICT or Validation.STRIP. The integer may
     * also have the bit Validation.VALIDATE_OUTPUT set, indicating that the strean being validated
     * is to be treated as a final output stream (which means multiple errors can be reported)
     * @return A Receiver to which events can be sent for validation
     */

    public Receiver getDocumentValidator(Receiver receiver,
                                         String systemId,
                                         NamePool namePool,
                                         int validationMode) {
        // non-schema-aware version
        return receiver;
    }

    /**
     * Get a Receiver that can be used to validate an element, and that passes the validated
     * element on to a target receiver. If validation is not supported, the returned receiver
     * will be the target receiver.
     * <p>
     * This method is intended for internal use.
     * @param receiver the target receiver tp receive the validated element
     * @param nameCode the nameCode of the element to be validated. This must correspond to the
     * name of an element declaration in a loaded schema
     * @param schemaType the schema type (typically a complex type) against which the element is to
     * be validated
     * @param validation The validation mode, for example Validation.STRICT or Validation.LAX
     * @param pool The name pool
     * @return The target receiver, indicating that with this configuration, no validation
     * is performed.
     */
    public Receiver getElementValidator(Receiver receiver,
                                        int nameCode,
                                        int locationId,
                                        SchemaType schemaType,
                                        int validation,
                                        NamePool pool)
            throws XPathException {
        return receiver;
    }

    /**
     * Validate an attribute value.
     * <p>
     * This method is intended for internal use.
     * @param nameCode the name of the attribute
     * @param value the value of the attribute as a string
     * @param validation STRICT or LAX
     * @return the type annotation to apply to the attribute node
     * @throws ValidationException if the value is invalid
     */

    public long validateAttribute(int nameCode, CharSequence value, int validation)
    throws ValidationException {
        return -1;
    }

    /**
     * Add to a pipeline a receiver that strips all type annotations. This
     * has a null implementation in the Saxon-B product, because type annotations
     * can never arise.
     * <p>
     * This method is intended for internal use.
     */

    public Receiver getAnnotationStripper(Receiver destination) {
        return destination;
    }

    /**
     * Make a test for elements corresponding to a give element declaration.
     * <p>
     * This method is intended for internal use.
     */

    public NodeTest makeSubstitutionGroupTest(SchemaDeclaration elementDecl) {
        needSchemaAwareVersion();
        return null;
    }


    /**
    * Create a new SAX XMLReader object using the class name provided. <br>
    *  <p>
    * The named class must exist and must implement the
    * org.xml.sax.XMLReader or Parser interface. <br>
    *  <p>
    * This method returns an instance of the parser named.
     * <p>
     * This method is intended for internal use.
    *
    * @param className A string containing the name of the
    *   SAX parser class, for example "com.microstar.sax.LarkDriver"
    * @return an instance of the Parser class named, or null if it is not
    * loadable or is not a Parser.
    *
    */
    public XMLReader makeParser (String className)
    throws TransformerFactoryConfigurationError
    {
        Object obj;
        try {
            obj = getInstance(className, null);
        } catch (XPathException err) {
            throw new TransformerFactoryConfigurationError(err);
        }
        if (obj instanceof XMLReader) {
            return (XMLReader)obj;
        }
        throw new TransformerFactoryConfigurationError("Class " + className +
                                 " is not a SAX2 XMLReader");
    }

    /**
    * Get a locale given a language code in XML format.
     * <p>
     * This method is intended for internal use.
    */

    public static Locale getLocale(String lang) {
        int hyphen = lang.indexOf("-");
        String language, country;
        if (hyphen < 1) {
            language = lang;
            country = "";
        } else {
            language = lang.substring(1, hyphen);
            country = lang.substring(hyphen+1);
        }
        return new Locale(language, country);
    }

    /**
     * Set the debugger to be used.
     * <p>
     * This method is provided for advanced users only, and is subject to change.
     */

    public void setDebugger(Debugger debugger) {
        this.debugger = debugger;
    }

    /**
     * Get the debugger in use. This will be null if no debugger has been registered.
     * <p>
     * This method is provided for advanced users only, and is subject to change.
     */

    public Debugger getDebugger() {
        return debugger;
    }

    /**
     * Factory method to create a SlotManager.
     * <p>
     * This method is provided for advanced users only, and is subject to change.
     */

    public SlotManager makeSlotManager() {
        if (debugger == null) {
            return new SlotManager();
        } else {
            return debugger.makeSlotManager();
        }
    }

    /**
     * Factory method to get an Optimizer.
     * <p>
     * This method is intended for internal use only.
     */

    public Optimizer getOptimizer() {
        if (optimizer == null) {
            optimizer = new Optimizer();
        }
        return optimizer;
    }

    /**
     * Set a ClassLoader to be used when loading external classes. Examples of classes that are
     * loaded include SAX parsers, localization modules for formatting numbers and dates,
     * extension functions, external object models. In an environment such as Eclipse that uses
     * its own ClassLoader, this ClassLoader should be nominated to ensure that any class loaded
     * by Saxon is identical to a class of the same name loaded by the external environment.
     * <p>
     * This method is intended for external use by advanced users, but should be regarded as
     * experimental.
     */

    public void setClassLoader(ClassLoader loader) {
        this.classLoader = loader;
    }

    /**
     * Get the ClassLoader supplied using the method {@link #setClassLoader}.
     * If none has been supplied, return null.
     * <p>
     * This method is intended for external use by advanced users, but should be regarded as
     * experimental.
     */

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
    * Load a class using the class name provided.
    * Note that the method does not check that the object is of the right class.
     * <p>
     * This method is intended for internal use only.
     *
    * @param className A string containing the name of the
    *   class, for example "com.microstar.sax.LarkDriver"
    * @param classLoader The ClassLoader to be used to load the class. If this is null, then
     * the classLoader used will be the first one available of: the classLoader registered
     * with the Configuration using {@link #setClassLoader}; the context class loader for
     * the current thread; or failing that, the class loader invoked implicitly by a call
     * of Class.forName() (which is the ClassLoader that was used to load the Configuration
     * object itself).
     * @return an instance of the class named, or null if it is not
    * loadable.
    * @throws XPathException if the class cannot be loaded.
    *
    */

    public Class getClass(String className, boolean tracing, ClassLoader classLoader) throws XPathException {
        if (tracing) {
            System.err.println("Loading " + className);
        }

        try {
            ClassLoader loader = classLoader;
            if (loader == null) {
                loader = this.classLoader;
            }
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader != null) {
                try {
                    return loader.loadClass(className);
                } catch (Exception ex) {
                    return Class.forName(className);
                }
            } else {
                return Class.forName(className);
            }
        }
        catch (Exception e) {
            if (tracing) {
                // The exception is often masked, especially when calling extension
                // functions
                System.err.println("No Java class " + className + " could be loaded");
            }
            throw new DynamicError("Failed to load " + className, e );
        }

    }

  /**
    * Instantiate a class using the class name provided.
    * Note that the method does not check that the object is of the right class.
   * <p>
   * This method is intended for internal use only.
   *
    * @param className A string containing the name of the
    *   class, for example "com.microstar.sax.LarkDriver"
    * @param classLoader The ClassLoader to be used to load the class. If this is null, then
     * the classLoader used will be the first one available of: the classLoader registered
     * with the Configuration using {@link #setClassLoader}; the context class loader for
     * the current thread; or failing that, the class loader invoked implicitly by a call
     * of Class.forName() (which is the ClassLoader that was used to load the Configuration
     * object itself).
    * @return an instance of the class named, or null if it is not
    * loadable.
    * @throws XPathException if the class cannot be loaded.
    *
    */

    public Object getInstance(String className, ClassLoader classLoader) throws XPathException {
        Class theclass = getClass(className, false, classLoader);
        try {
            return theclass.newInstance();
        } catch (Exception err) {
            throw new DynamicError("Failed to instantiate class " + className, err);
        }
    }

    /**
    * Load a named collator class and check it is OK.
     * <p>
     * This method is intended for internal use only.
    */

    public Comparator makeCollator (String className) throws XPathException
    {
        Object handler = getInstance(className, null);

        if (handler instanceof Comparator ) {
            return (Comparator )handler;
        } else {
            throw new DynamicError("Failed to load collation class " + className +
                        ": it is not an instance of java.util.Comparator");
        }

    }

    /**
     * Set lazy construction mode on or off. In lazy construction mode, element constructors
     * are not evaluated until the content of the tree is required. Lazy construction mode
     * is currently experimental and is therefore off by default.
     * @param lazy true to switch lazy construction mode on, false to switch it off.
     */

    public void setLazyConstructionMode(boolean lazy) {
        lazyConstructionMode = lazy;
    }

    /**
     * Determine whether lazy construction mode is on or off. In lazy construction mode, element constructors
     * are not evaluated until the content of the tree is required. Lazy construction mode
     * is currently experimental and is therefore off by default.
     * @return true if lazy construction mode is enabled
     */

    public boolean isLazyConstructionMode() {
        return lazyConstructionMode;
    }


    /**
     * Register the standard Saxon-supplied object models.
     * <p>
     * This method is intended for internal use only.
     */

    public void registerStandardObjectModels() {
        // Try to load the support classes for various object models, registering
        // them in the Configuration. We require both the Saxon object model definition
        // and an implementation of the object model itself to be loadable before
        // the object model is registered.
        String[] models = {"org.orbeon.saxon.dom.DOMObjectModel",
                           "org.orbeon.saxon.jdom.JDOMObjectModel",
                           "org.orbeon.saxon.xom.XOMObjectModel"};
        String[] nodes =  {"org.w3c.dom.Node",
                           "org.jdom.Element",
                           "nu.xom.Node"};

        for (int i=0; i<models.length; i++) {
            try {
                getClass(nodes[i], false, null);
                ExternalObjectModel model = (ExternalObjectModel)getInstance(models[i], null);
                registerExternalObjectModel(model);
            } catch (XPathException err) {
                // ignore the failure. We can't report an exception here, and in any case a failure
                // is legitimate if the object model isn't on the class path. We'll fail later when
                // we try to process a node in the chosen object model: the node simply won't be
                // recognized as one that Saxon can handle
            } catch (ClassCastException err) {
                // we've loaded the class, but it isn't an ExternalObjectModel. This can apparently
                // happen if there's more than one ClassLoader involved. We'll output a simple warning,
                // and then continue as if the external object model wasn't on the class path
                System.err.println("Warning: external object model " + models[i] +
                        " has been loaded, but is not an instance of org.orbeon.saxon.om.ExternalObjectModel");
            }
        }
    }


    /**
     * Register an external object model.
     * <p>
     * This method is intended for advanced users only, and is subject to change.
     */

    public void registerExternalObjectModel(ExternalObjectModel model) {
        externalObjectModels.add(model);
    }

    /**
     * Find the external object model corresponding to a given node.
     * <p>
     * This method is intended for internal use only.
     *
     * @param node a Node as implemented in some external object model
     * @return the first registered external object model that recognizes
     * this node; or null if no-one will own up to it.
     */

    public ExternalObjectModel findExternalObjectModel(Object node) {
        Iterator it = externalObjectModels.iterator();
        while (it.hasNext()) {
            final ExternalObjectModel model = (ExternalObjectModel)it.next();
            if (model.isRecognizedNode(node)) {
                return model;
            }
        }
        return null;
    }

    /**
     * Get all the registered external object models.
     * <p>
     * This method is intended for internal use only.
     */

    public List getExternalObjectModels() {
        return externalObjectModels;
    }

    /**
     * Make a PipelineConfiguration from the properties of this Configuration
     * @since 8.4
     */

    public PipelineConfiguration makePipelineConfiguration() {
        PipelineConfiguration pipe = new PipelineConfiguration();
        pipe.setConfiguration(this);
        pipe.setErrorListener(getErrorListener());
        pipe.setURIResolver(getURIResolver());
        return pipe;
    }

    /**
     * Set the implicit timezone, as a positive or negative offset from UTC in minutes.
     * The range is -14hours to +14hours
     */
    public void setImplicitTimezone(int minutes) {
        if (minutes < -14*60 || minutes > +14*60) {
            throw new IllegalArgumentException("Implicit timezone is out of range: range is " + (-14*60)
                            + " to +" + (+14*60) + " minutes");
        }
        implicitTimezone = minutes;
    }

    /**
     * Get the implicit timezone, as a positive or negative offset from UTC in minutes.
     * The range is -14hours to +14hours
     * @return the value set using {@link #setImplicitTimezone}, or failing that
     * the timezone from the system clock at the time the Configuration was created.
     */
    public int getImplicitTimezone() {
        return implicitTimezone;
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
// Contributor(s): none.
//