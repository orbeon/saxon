package org.orbeon.saxon;

import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.Optimizer;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.*;
import org.orbeon.saxon.instruct.Debugger;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pull.PullProvider;
import org.orbeon.saxon.query.ModuleURIResolver;
import org.orbeon.saxon.query.StandardModuleURIResolver;
import org.orbeon.saxon.sort.CollationURIResolver;
import org.orbeon.saxon.sort.StandardCollationURIResolver;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.IndependentContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
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


public class Configuration implements Serializable, SourceResolver {

    private Platform platform = JavaPlatform.getInstance();
    private transient URIResolver uriResolver;
    private StandardURIResolver systemURIResolver = new StandardURIResolver(this);
    protected transient ErrorListener listener;
    private int xmlVersion = XML10;
    private int treeModel = Builder.TINY_TREE;
    private boolean lineNumbering = false;
    private boolean tracing = false;
    private TraceListener traceListener = null;
    private FunctionLibrary extensionBinder;
    private CollationURIResolver collationResolver = StandardCollationURIResolver.getInstance();
    private CollectionURIResolver collectionResolver = new StandardCollectionURIResolver();
    private ModuleURIResolver moduleURIResolver = null;
    private ModuleURIResolver standardModuleURIResolver = new StandardModuleURIResolver(this);
    private SchemaURIResolver schemaURIResolver = null;
    private SourceResolver sourceResolver = this;
    protected VendorFunctionLibrary vendorFunctionLibrary;
    protected int recoveryPolicy = RECOVER_WITH_WARNINGS;
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
    private boolean allowMultiThreading = false;
    private int stripsWhiteSpace = Whitespace.IGNORABLE;
    private NamePool targetNamePool = null;
    private DocumentNumberAllocator documentNumberAllocator = new DocumentNumberAllocator();
    private XPathContext conversionContext = null;
    private transient TypeHierarchy typeHierarchy;

    private int hostLanguage = XSLT;
    private int schemaValidationMode = Validation.PRESERVE;
    private boolean validationWarnings = false;
    private boolean retainDTDattributeTypes = false;
    private Debugger debugger = null;
    protected Optimizer optimizer = null;
    private ExtensionFunctionFactory extensionFunctionFactory = new ExtensionFunctionFactory(this);
    private SerializerFactory serializerFactory = new SerializerFactory();

    private transient ClassLoader classLoader;
    private int implicitTimezone;
    private transient List sourceParserPool = new ArrayList(5);
    private transient List styleParserPool = new ArrayList(5);

    /**
     * The external object models are held in static so they are only loaded once in an application
     * that creates many Configurations repeatedly. This saves expensive searches of the classpath
     */

    private static List sharedExternalObjectModels = null;
    private List externalObjectModels = null;

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
     * Constant indicating the XML Version 1.0
     */

    public static final int XML10 = 10;

    /**
     * Constant indicating the XML Version 1.1
     */

    public static final int XML11 = 11;

    /**
     * Constant indicating that the host language is XSLT
     */
    public static final int XSLT = 50;

    /**
     * Constant indicating that the host language is XQuery
     */
    public static final int XQUERY = 51;

    /**
     * Constant indicating that the "host language" is XML Schema
     */
    public static final int XML_SCHEMA = 52;

    /**
     * Constant indicating that the host language is Java: that is, this is a free-standing
     * Java application with no XSLT or XQuery content
     */
    public static final int JAVA_APPLICATION = 53;

    /**
     * Constant indicating that the host language is XPATH itself - that is, a free-standing XPath environment
     */
    public static final int XPATH = 54;


    /**
     * Create a configuration object with default settings for all options. This is equivalent to
     * calling <code>new Configuration(true)</code>.
     * @since 8.4
     */

    public Configuration() {
        init();
    }


    protected void init() {
        if (System.getProperty("java.vendor").equals("Jeroen Frijters")) {
            try {
                setPlatform((Platform)getInstance("org.orbeon.saxon.dotnet.DotNetPlatform", null));
                platform.initialize(this);
            } catch (XPathException e) {
                throw new RuntimeException("Failed to load .NET platform code: " + e.getMessage());
            }
            externalObjectModels = Collections.EMPTY_LIST;
        } else {
            synchronized (Configuration.class) {
                if (sharedExternalObjectModels == null) {
                    registerStandardObjectModels();
                }
                externalObjectModels = new ArrayList(sharedExternalObjectModels);
            }
        }
        targetNamePool = NamePool.getDefaultNamePool();
        extensionBinder = new JavaExtensionLibrary(this);

        // Get the implicit timezone from the current system clock
        GregorianCalendar calendar = new GregorianCalendar();
        int tzmsecs = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET));
        implicitTimezone = tzmsecs / 60000;
    }

    /**
     * Static method to instantiate a schema-aware configuration.
     * <p>On the .NET platform, this method should not be called unless it is known that the assembly
     * saxon8sa.dll has already been loaded. This can be achieved by an appropriate call on Assembly.Load():
     * for an example, see the C# Configuration.cs class in the Saxon.Api namespace.</p>
     * @param classLoader - the class loader to be used. If null, the context class loader for the current
     * thread is used.
     * @throws RuntimeException if the Saxon-SA
     * product cannot be loaded or if no license key is available.
     */

    public static Configuration makeSchemaAwareConfiguration(ClassLoader classLoader) throws RuntimeException {
        try {
            Class theClass;
            ClassLoader loader = classLoader;
            if (loader == null) {
                try {
                    loader = Thread.currentThread().getContextClassLoader();
                } catch (Exception err) {
                    System.err.println("Failed to getContextClassLoader() - continuing");
                }
            }
            if (loader != null) {
                try {
                    theClass = loader.loadClass("com.saxonica.validate.SchemaAwareConfiguration");
                } catch (Exception ex) {
                    theClass = Class.forName("com.saxonica.validate.SchemaAwareConfiguration");
                }
            } else {
                theClass = Class.forName("com.saxonica.validate.SchemaAwareConfiguration");
            }
            Configuration config = (Configuration)theClass.newInstance();
            config.init();
            return config;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

   /**
    * Get a message used to identify this product when a transformation is run using the -t option
    * @return A string containing both the product name and the product
    *     version
    * @since 8.4
    */

    public String getProductTitle() {
        return "Saxon " + Version.getProductVersion() + platform.getPlatformSuffix() + " from Saxonica";
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
     * Instead, use {@link org.orbeon.saxon.instruct.Executable#getHostLanguage}.
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
     * Set the Platform to be used for platform-dependent methods
     * @param platform the platform to be used
     */

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

   /**
     * Get the Platform to be used for platform-dependent methods
     * @return the platform to be used
     */

    public Platform getPlatform() {
        return platform;
    }

    /**
     * Get the URIResolver used in this configuration
     * @return the URIResolver. If no URIResolver has been set explicitly, the
     * default URIResolver is used.
     * @since 8.4
     */

    public URIResolver getURIResolver() {
        if (uriResolver==null) {
            return systemURIResolver;
        }
        return uriResolver;
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
        this.uriResolver = resolver;
    }

    /**
     * Get the system-defined URI Resolver. This is used when the user-defined URI resolver
     * returns null as the result of the resolve() method
     */

    public StandardURIResolver getSystemURIResolver() {
        return systemURIResolver;
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
     * Report a fatal error
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

    /**
     * Set whether multithreading optimizations are allowed
     */

    public void setMultiThreading(boolean multithreading) {
        allowMultiThreading = multithreading;
    }

    /**
     * Determine whether multithreading optimizations are allowed
     */

    public boolean isMultiThreading() {
        return allowMultiThreading;
    }

    /**
     * Set the XML version to be used by default for validating characters and names
     * @param version one of the constants XML10 or XML11
     * @since 8.6
     */

    public void setXMLVersion(int version) {
        this.xmlVersion = version;
        // TODO: we should reject XML 1.1 input unless this is set to XML11. It's not clear this is happening.
    }

    /**
     * Get the XML version to be used by default for validating characters and names
     * @return one of the constants XML10 or XML11
     * @since 8.6
     */

    public int getXMLVersion() {
        return this.xmlVersion;
    }

    /**
     * Get a class that can be used to check names against the selected XML version
     * @return a class that can be used for name checking
     * @since 8.6
     */

    public NameChecker getNameChecker() {
        return (xmlVersion == XML10 ?
                (NameChecker)Name10Checker.getInstance() :
                (NameChecker)Name11Checker.getInstance());
    }

    /**
     * Get an XPathContext object with sufficient capability to perform comparisons and conversions
     */

    public XPathContext getConversionContext() {
        if (conversionContext == null) {
            conversionContext = new IndependentContext(this).makeEarlyEvaluationContext();
        }
        return conversionContext;
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
     *
     * <p>Note: this method should not be used if the Configuration is multithreading. In that situation,
     * use {@link #setCompileWithTracing(boolean)} to force stylesheets and queries to be compiled
     * with trace code enabled, and use {@link Controller#addTraceListener(org.orbeon.saxon.trace.TraceListener)} to
     * supply a TraceListener at run time.</p>
     * @param traceListener The TraceListener to be used.
     * @since 8.4
     */

    public void setTraceListener(TraceListener traceListener) {
        this.traceListener = traceListener;
        setMultiThreading(false);
    }

    /**
     * Determine whether compile-time generation of trace code was requested
     * @since 8.8
     */

    public boolean isCompileWithTracing() {
        return tracing;
    }

    /**
     * Request compile-time generation of trace code (or not)
     * @since 8.8
     */

    public void setCompileWithTracing(boolean trace) {
        this.tracing = trace;
    }


    /** Create an instance of a TraceListener with a specified class name
     *
     * @exception org.orbeon.saxon.trans.XPathException if the requested class does not
     *     implement the org.orbeon.saxon.trace.TraceListener interface
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
     * by supplying as the FunctionLibrary a {@link org.orbeon.saxon.functions.FunctionLibraryList}
     * that itself contains two FunctionLibrary objects: a JavaExtensionLibrary, and a user-written
     * FunctionLibrary.
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
     * Set a CollationURIResolver to be used to resolve collation URIs (that is,
     * to take a URI identifying a collation, and return the corresponding collation).
     * Note that Saxon attempts first to resolve a collation URI using the resolver
     * registered with the Controller; if that returns null, it tries again using the
     * resolver registered with the Configuration.
     * <p>
     * Note that it is undefined whether collation URIs are resolved at compile time
     * or at run-time. It is therefore inadvisable to change the CollationURIResolver after
     * compiling a query or stylesheet and before running it.
     * @param resolver the collation URI resolver to be used. This replaces any collation
     * URI resolver previously registered.
     * @since 8.5
     */

    public void setCollationURIResolver(CollationURIResolver resolver) {
        collationResolver = resolver;
    }

    /**
     * Get the collation URI resolver associated with this configuration. This will
     * return the CollationURIResolver previously set using the {@link #setCollationURIResolver}
     * method; if this has not been called, it returns the system-defined collation URI resolver
     * @return the registered CollationURIResolver
     * @since 8.5
     */

    public CollationURIResolver getCollationURIResolver() {
        return collationResolver;
    }

    /**
     * Set a CollectionURIResolver to be used to resolve collection URIs (that is,
     * the URI supplied in a call to the collection() function).
     * <p>
     * Collection URIs are always resolved at run-time, using the CollectionURIResolver
     * in force at the time the collection() function is called.
     * @param resolver the collection URI resolver to be used. This replaces any collection
     * URI resolver previously registered.
     * @since 8.5
     */

    public void setCollectionURIResolver(CollectionURIResolver resolver) {
        collectionResolver = resolver;
    }

    /**
     * Get the collection URI resolver associated with this configuration. This will
     * return the CollectionURIResolver previously set using the {@link #setCollectionURIResolver}
     * method; if this has not been called, it returns the system-defined collection URI resolver
     * @return the registered CollationURIResolver
     * @since 8.5
     */

    public CollectionURIResolver getCollectionURIResolver() {
        return collectionResolver;
    }

    /**
     * Set a user-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in an XQuery prolog.
     * This acts as the default value for the ModuleURIResolver in the StaticQueryContext, and may be
     * overridden by a more specific ModuleURIResolver nominated as part of the StaticQueryContext.
     */

    public void setModuleURIResolver(ModuleURIResolver resolver) {
        moduleURIResolver = resolver;
    }

    /**
     * Create and register an instance of a ModuleURIResolver with a specified class name.
     * This will be used for resolving URIs in XQuery "import module" declarations, unless
     * a more specific ModuleURIResolver has been nominated as part of the StaticQueryContext.
     *
     * @exception TransformerException if the requested class does not
     *     implement the org.orbeon.saxon.LocationHintResolver interface
     * @param className The fully-qualified name of the LocationHintResolver class
     */
    public void setModuleURIResolver(String className) throws TransformerException {
        Object obj = getInstance(className, null);
        if (obj instanceof ModuleURIResolver) {
            setModuleURIResolver((ModuleURIResolver)obj);
        } else {
            throw new DynamicError("Class " + className + " is not a LocationHintResolver");
        }
    }

    /**
     * Get the user-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in the XQuery prolog; returns null if none has been explicitly set.
     */

    public ModuleURIResolver getModuleURIResolver() {
        return moduleURIResolver;
    }

    /**
     * Get the standard system-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in the XQuery prolog.
     */

    public ModuleURIResolver getStandardModuleURIResolver() {
        return standardModuleURIResolver;
    }

    /**
     * Set a user-defined SchemaURIResolver for resolving URIs used in "import schema"
     * declarations.
     */

    public void setSchemaURIResolver(SchemaURIResolver resolver) {
        schemaURIResolver = resolver;
    }

    /**
     * Get the user-defined SchemaURIResolver for resolving URIs used in "import schema"
     * declarations; if none has been explicitly set, returns null.
     */

    public SchemaURIResolver getSchemaURIResolver() {
        return schemaURIResolver;
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
     * Set a custom SerializerFactory. This will be used to create a serializer for a given
     * set of output properties and result destination.
     * @since 8.8
     */

    public void setSerializerFactory(SerializerFactory factory) {
        this.serializerFactory = factory;
    }

    /**
     * Get the SerializerFactory. This returns the standard built-in SerializerFactory, unless
     * a custom SerializerFactory has been registered.
     * @since 8.8
     */

    public SerializerFactory getSerializerFactory() {
        return serializerFactory;
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
     * Get the TypeHierarchy: a cache holding type information
     */

    public final TypeHierarchy getTypeHierarchy() {
        if (typeHierarchy == null) {
            typeHierarchy = new TypeHierarchy(this);
        }
        return typeHierarchy;
    }
    /**
     * Get the document number allocator.
     * <p>
     * This is intended primarily for internal use
     */

    public DocumentNumberAllocator getDocumentNumberAllocator() {
        return documentNumberAllocator;
    }

    /**
     * Determine whether whitespace-only text nodes are to be stripped unconditionally
     * from source documents.
     * @return true if all whitespace-only text nodes are stripped.
     * @since 8.4
     */

    public boolean isStripsAllWhiteSpace() {
        return stripsWhiteSpace == Whitespace.ALL;
    }

    /**
     * Determine whether whitespace-only text nodes are to be stripped unconditionally
     * from source documents.
     * @param stripsAllWhiteSpace if all whitespace-only text nodes are to be stripped.
     * @since 8.4
     */

    public void setStripsAllWhiteSpace(boolean stripsAllWhiteSpace) {
        if (stripsAllWhiteSpace) {
            this.stripsWhiteSpace = Whitespace.ALL;
        }
    }

    /**
     * Set which kinds of whitespace-only text node should be stripped.
     * @param kind the kind of whitespace-only text node that should be stripped when building
     * a source tree. One of {@link Whitespace#NONE} (none), {@link Whitespace#ALL} (all),
     * or {@link Whitespace#IGNORABLE} (element-content whitespace as defined in a DTD or schema)
     */

    public void setStripsWhiteSpace(int kind) {
        stripsWhiteSpace = kind;
    }

    /**
     * Set which kinds of whitespace-only text node should be stripped.
     * @return kind the kind of whitespace-only text node that should be stripped when building
     * a source tree. One of {@link Whitespace#NONE} (none), {@link Whitespace#ALL} (all),
     * or {@link Whitespace#IGNORABLE} (element-content whitespace as defined in a DTD or schema)
     */

    public int getStripsWhiteSpace() {
        return stripsWhiteSpace;
    }


    /**
     * Get a parser for source documents. The parser is allocated from a pool if any are available
     * from the pool: the client should ideally return the parser to the pool after use, so that it
     * can be reused.
     * <p>
     * This method is intended primarily for internal use.
     * @return a parser, in which the namespace properties must be set as follows:
     * namespaces=true; namespace-prefixes=false. The DTD validation feature of the parser will be set
     * on or off depending on the {@link #setValidation(boolean)} setting.
    */

    public synchronized XMLReader getSourceParser() throws TransformerFactoryConfigurationError {
        if (sourceParserPool == null) {
            sourceParserPool = new ArrayList(10);
        }
        if (sourceParserPool.size() > 0) {
            int n = sourceParserPool.size()-1;
            XMLReader parser = (XMLReader)sourceParserPool.get(n);
            sourceParserPool.remove(n);
            return parser;
        }
        XMLReader parser;
        if (getSourceParserClass()!=null) {
            parser = makeParser(getSourceParserClass());
        } else {
            parser = loadParser();
        }
        try {
            Sender.configureParser(parser);
        } catch (DynamicError err) {
            throw new TransformerFactoryConfigurationError(err);
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
     * Return a source parser to the pool, for reuse
     * @param parser The parser: the caller must not supply a parser that was obtained by any
     * mechanism other than calling the getSourceParser() method.
     */

    public synchronized void reuseSourceParser(XMLReader parser) {
        if (sourceParserPool == null) {
            sourceParserPool = new ArrayList(10);
        }
        sourceParserPool.add(parser);
    }

    /**
     * Get a parser by instantiating the SAXParserFactory
     * @return the parser (XMLReader)
     */

    private XMLReader loadParser() {
        XMLReader parser;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        } catch (ParserConfigurationException err) {
            throw new TransformerFactoryConfigurationError(err);
        } catch (SAXException err) {
            throw new TransformerFactoryConfigurationError(err);
        }
        return parser;
    }

    /**
    * Get the parser for stylesheet documents. This parser is also used for schema documents.
     * <p>
     * This method is intended for internal use only.
     *
    */

    public synchronized XMLReader getStyleParser() throws TransformerFactoryConfigurationError {
        if (styleParserPool == null) {
            styleParserPool = new ArrayList(10);
        }
        if (styleParserPool.size() > 0) {
            int n = styleParserPool.size()-1;
            XMLReader parser = (XMLReader)styleParserPool.get(n);
            styleParserPool.remove(n);
            return parser;
        }
        XMLReader parser;
        if (getStyleParserClass()!=null) {
            parser = makeParser(getStyleParserClass());
        } else {
            parser = loadParser();
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
     * Return a stylesheet (or schema) parser to the pool, for reuse
     * @param parser The parser: the caller must not supply a parser that was obtained by any
     * mechanism other than calling the getStyleParser() method.
     */

    public synchronized void reuseStyleParser(XMLReader parser) {
        if (styleParserPool == null) {
            styleParserPool = new ArrayList(10);
        }
        styleParserPool.add(parser);
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
     * @param root the xs:schema element in the stylesheet
     * @param expected the target namespace expected; null if there is no
     * expectation.
     * @param errorListener The destination for error messages. May be null, in which case
     * the errorListener registered with this Configuration is used.
     * @return the actual target namespace of the schema
     *
     */

    public String readInlineSchema(NodeInfo root, String expected, ErrorListener errorListener)
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
     * this Configuration. Any errors will be notified to the ErrorListener associated with
     * this Configuration.
     * @param schemaSource the JAXP Source object identifying the schema document to be loaded
     * @throws SchemaException if the schema cannot be read or parsed or if it is invalid
     * @throws UnsupportedOperationException if the configuration is not schema-aware
     * @since 8.4
     */

    public void addSchemaSource(Source schemaSource) throws SchemaException {
        addSchemaSource(schemaSource, getErrorListener());
    }

    /**
     * Load a schema, which will be available for use by all subsequent operations using
     * this SchemaAwareConfiguration.
     * @param schemaSource the JAXP Source object identifying the schema document to be loaded
     * @param errorListener the ErrorListener to be notified of any errors in the schema.
     * @throws SchemaException if the schema cannot be read or parsed or if it is invalid
     */

    public void addSchemaSource(Source schemaSource, ErrorListener errorListener) throws SchemaException {
        needSchemaAwareVersion();
    }

    /**
     * Determine whether the Configuration contains a cached schema for a given target namespace
     * @param targetNamespace the target namespace of the schema being sought (supply "" for the
     * unnamed namespace)
     * @return true if the schema for this namespace is available, false if not.
     */

    public boolean isSchemaAvailable(String targetNamespace) {
        return false;
    }

    /**
     * Get the set of namespaces of imported schemas
     */

    public Set getImportedNamespaces() {
        return Collections.EMPTY_SET;
    }

    /**
     * Mark a schema namespace as being sealed. This is done when components from this namespace
     * are first used for validating a source document or compiling a source document or query. Once
     * a namespace has been sealed, it is not permitted to change the schema components in that namespace
     * by redefining them, deriving new types by extension, or adding to their substitution groups.
     * @param namespace the namespace URI of the components to be sealed
     */

    public void sealNamespace(String namespace) {
        //
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
     * Check that a type is validly derived from another type, following the rules for the Schema Component
     * Constraint "Is Type Derivation OK (Simple)" (3.14.6) or "Is Type Derivation OK (Complex)" (3.4.6) as
     * appropriate.
     * @param derived the derived type
     * @param base the base type; the algorithm tests whether derivation from this type is permitted
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException if the derivation is not allowed
     */

    public void checkTypeDerivationIsOK(SchemaType derived, SchemaType base, int block)
            throws SchemaException, ValidationException {
        // no action. Although the method can be used to check built-in types, it is never
        // needed in the non-schema-aware product
    }

    /**
     * Get a document-level validator to add to a Receiver pipeline.
     * <p>
     * This method is intended for internal use.
     * @param receiver The receiver to which events should be sent after validation
     * @param systemId the base URI of the document being validated
     * @param validationMode for example Validation.STRICT or Validation.STRIP. The integer may
     * also have the bit Validation.VALIDATE_OUTPUT set, indicating that the strean being validated
     * is to be treated as a final output stream (which means multiple errors can be reported)
     * @param stripSpace
     * @param schemaType The type against which the outermost element of the document must be validated
     * (null if there is no constraint)
     * @return A Receiver to which events can be sent for validation
     */

    public Receiver getDocumentValidator(Receiver receiver,
                                         String systemId,
                                         int validationMode,
                                         int stripSpace,
                                         SchemaType schemaType) {
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
     * @return The target receiver, indicating that with this configuration, no validation
     * is performed.
     */
    public Receiver getElementValidator(Receiver receiver,
                                        int nameCode,
                                        int locationId,
                                        SchemaType schemaType,
                                        int validation)
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

    public int validateAttribute(int nameCode, CharSequence value, int validation)
    throws ValidationException {
        return Type.UNTYPED_ATOMIC;
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
            optimizer = new Optimizer(this);
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

    private void registerStandardObjectModels() {
        // Try to load the support classes for various object models, registering
        // them in the Configuration. We require both the Saxon object model definition
        // and an implementation of the object model itself to be loadable before
        // the object model is registered.
        String[] models = {"org.orbeon.saxon.dom.DOMEnvelope",
                           "org.orbeon.saxon.dom.DOMObjectModel",
                           "org.orbeon.saxon.jdom.JDOMObjectModel",
                           "org.orbeon.saxon.xom.XOMObjectModel",
                           "org.orbeon.saxon.dom4j.DOM4JObjectModel"};
        String[] nodes =  {"org.orbeon.saxon.dom.NodeOverNodeInfo",
                           "org.w3c.dom.Node",
                           "org.jdom.Element",
                           "nu.xom.Node",
                           "org.dom4j.Element"};

        sharedExternalObjectModels = new ArrayList(4);
        for (int i=0; i<models.length; i++) {
            try {
                getClass(nodes[i], false, null);
                        // designed to trigger an exception if the class can't be loaded
                ExternalObjectModel model = (ExternalObjectModel)getInstance(models[i], null);
                sharedExternalObjectModels.add(model);
                //registerExternalObjectModel(model);
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

            } catch (Throwable err) {
                // On the .NET platform all kinds of things can go wrong, but we don't really care
                System.err.println("Warning: failed to load external object model: " + err.getMessage());
            }
        }
    }

    /**
     * Register an external object model with this Configuration.
     * @param model The external object model.
     * This can either be one of the system-supplied external
     * object models for JDOM, XOM, or DOM, or a user-supplied external object model.
     * <p>
     * This method is intended for advanced users only, and is subject to change.
     */

    public void registerExternalObjectModel(ExternalObjectModel model) {
        if (externalObjectModels == null) {
            externalObjectModels = new ArrayList(5);
        }
        if (!externalObjectModels.contains(model)) {
            externalObjectModels.add(model);
        }
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
        if (externalObjectModels == null) {
            return null;
        }
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
        pipe.setSchemaURIResolver(getSchemaURIResolver());
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

    /**
     * Get the configuration, given the context. This is provided as a static method to make it accessible
     * as an extension function.
     */

    public static Configuration getConfiguration(XPathContext context) {
        return context.getConfiguration();
    }

    /**
     * Supply a SourceResolver
     */

    public void setSourceResolver(SourceResolver resolver) {
        sourceResolver = resolver;
    }

    /**
     * Get the current SourceResolver. If none has been supplied, a system-defined SourceResolver
     * is returned.
     * @return the current SourceResolver
     */

    public SourceResolver getSourceResolver() {
        return sourceResolver;
    }

    /**
     * Implement the SourceResolver interface
     */

    public Source resolveSource(Source source, Configuration config) throws XPathException {
        if (source instanceof AugmentedSource) return source;
        if (source instanceof StreamSource) return source;
        if (source instanceof SAXSource) return source;
        if (source instanceof DOMSource) return source;
        if (source instanceof NodeInfo) return source;
        if (source instanceof PullProvider) return source;
        return null;
    }

    /**
    * Load a named output emitter or SAX2 ContentHandler and check it is OK.
    */

    public Receiver makeEmitter (String clarkName, Controller controller) throws XPathException {
        int brace = clarkName.indexOf('}');
        String localName = clarkName.substring(brace+1);
        int colon = localName.indexOf(':');
        String className = localName.substring(colon+1);
        Object handler;
        try {
            handler = getInstance(className, controller.getClassLoader());
        } catch (XPathException e) {
            throw new DynamicError("Cannot load user-supplied output method " + className);
        }

        if (handler instanceof Receiver) {
            return (Receiver)handler;
        } else if (handler instanceof ContentHandler) {
            ContentHandlerProxy emitter = new ContentHandlerProxy();
            emitter.setUnderlyingContentHandler((ContentHandler)handler);
            emitter.setOutputProperties(controller.getOutputProperties());
            return emitter;
        } else {
            throw new DynamicError("Failed to load " + className +
                        ": it is neither a Receiver nor a SAX2 ContentHandler");
        }

    }

    /**
     * Set a property of the configuration. This method underpins the setAttribute() method of the
     * TransformerFactory implementation, and is provided
     * to enable setting of Configuration properties using URIs without instantiating a TransformerFactory:
     * specifically, this may be useful when running XQuery, and it is also used by the Validator API
     * @param name the URI identifying the property to be set
     * @param value the value of the property
     * @throws IllegalArgumentException if the property name is not recognized
     */

    public void setConfigurationProperty(String name, Object value) {
        final Configuration config = this;
        if (name.equals(FeatureKeys.TREE_MODEL)) {
        	if (!(value instanceof Integer)) {
        		throw new IllegalArgumentException("Tree model must be an Integer");
        	}
        	config.setTreeModel(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("ALLOW_EXTERNAL_FUNCTIONS must be a boolean");
        	}
        	config.setAllowExternalFunctions(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.RECOGNIZE_URI_QUERY_PARAMETERS)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("RECOGNIZE_QUERY_URI_PARAMETERS must be a boolean");
        	}
        	config.getSystemURIResolver().setRecognizeQueryParameters(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("TRACE_EXTERNAL_FUNCTIONS must be a boolean");
        	}
        	config.setTraceExternalFunctions(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.TIMING)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("TIMING must be a boolean");
        	}
        	config.setTiming(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.DTD_VALIDATION)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("DTD_VALIDATION must be a boolean");
        	}
        	config.setValidation(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.STRIP_WHITESPACE)) {
        	if (!(value instanceof String)) {
        		throw new IllegalArgumentException("STRIP_WHITESPACE must be a string");
        	}
            int ival;
            if (value.equals("all")) {
                ival = Whitespace.ALL;
            } else if (value.equals("none")) {
                ival = Whitespace.NONE;
            } else if (value.equals("ignorable")) {
                ival = Whitespace.IGNORABLE;
            } else {
                throw new IllegalArgumentException(
                        "Unrecognized value STRIP_WHITESPACE = '" + value +
                        "': must be 'all', 'none', or 'ignorable'");
            }
        	config.setStripsWhiteSpace(ival);

        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION)) {
        	if (!(value instanceof Integer)) {
        		throw new IllegalArgumentException("SCHEMA_VALIDATION must be an integer");
        	}
        	config.setSchemaValidationMode(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.VALIDATION_WARNINGS)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("VALIDATION_WARNINGS must be a boolean");
        	}
        	config.setValidationWarnings(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.VERSION_WARNING)) {
             if (!(value instanceof Boolean)) {
                 throw new IllegalArgumentException("VERSION_WARNING must be a boolean");
             }
             config.setVersionWarning(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.TRACE_LISTENER)) {
        	if (!(value instanceof TraceListener)) {
        		throw new IllegalArgumentException("TRACE_LISTENER is of wrong class");
        	}
        	config.setTraceListener((TraceListener)value);

        } else if (name.equals(FeatureKeys.COMPILE_WITH_TRACING)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("COMPILE_WITH_TRACING must be a Boolean");
        	}
        	config.setCompileWithTracing(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.LINE_NUMBERING)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("LINE_NUMBERING value must be Boolean");
        	}
        	config.setLineNumbering(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.RECOVERY_POLICY)) {
        	if (!(value instanceof Integer)) {
        		throw new IllegalArgumentException("RECOVERY_POLICY value must be Integer");
        	}
        	config.setRecoveryPolicy(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.MESSAGE_EMITTER_CLASS)) {
        	if (!(value instanceof String)) {
        		throw new IllegalArgumentException("MESSAGE_EMITTER class must be a String");
        	}
        	config.setMessageEmitterClass((String)value);

        } else if (name.equals(FeatureKeys.SOURCE_PARSER_CLASS)) {
        	if (!(value instanceof String)) {
        		throw new IllegalArgumentException("SOURCE_PARSER class must be a String");
        	}
        	config.setSourceParserClass((String)value);

        } else if (name.equals(FeatureKeys.STYLE_PARSER_CLASS)) {
        	if (!(value instanceof String)) {
        		throw new IllegalArgumentException("STYLE_PARSER class must be a String");
        	}
        	config.setStyleParserClass((String)value);

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER)) {
        	if (!(value instanceof OutputURIResolver)) {
        		throw new IllegalArgumentException(
                        "OUTPUT_URI_RESOLVER value must be an instance of org.orbeon.saxon.OutputURIResolver");
        	}
        	config.setOutputURIResolver((OutputURIResolver)value);

        } else if (name.equals(FeatureKeys.NAME_POOL)) {
        	if (!(value instanceof NamePool)) {
        		throw new IllegalArgumentException("NAME_POOL value must be an instance of org.orbeon.saxon.om.NamePool");
        	}
        	config.setNamePool((NamePool)value);

        } else if (name.equals(FeatureKeys.COLLATION_URI_RESOLVER)) {
        	if (!(value instanceof CollationURIResolver)) {
        		throw new IllegalArgumentException(
                        "COLLATION_URI_RESOLVER value must be an instance of org.orbeon.saxon.sort.CollationURIResolver");
        	}
        	config.setCollationURIResolver((CollationURIResolver)value);

        } else if (name.equals(FeatureKeys.COLLECTION_URI_RESOLVER)) {
        	if (!(value instanceof CollectionURIResolver)) {
        		throw new IllegalArgumentException(
                        "COLLECTION_URI_RESOLVER value must be an instance of org.orbeon.saxon.CollectionURIResolver");
        	}
        	config.setCollectionURIResolver((CollectionURIResolver)value);

        } else if (name.equals(FeatureKeys.XML_VERSION)) {
            if (!(value instanceof String && (value.equals("1.0") || value.equals("1.1")))) {
                throw new IllegalArgumentException(
                        "XML_VERSION value must be \"1.0\" or \"1.1\" as a String");

            }
            config.setXMLVersion((value.equals("1.0") ? XML10 : XML11));

        } else {
	        throw new IllegalArgumentException("Unknown attribute " + name);
	    }
    }

    /**
     * Get a property of the configuration
     * @param name the name of the required property
     * @return the value of the property
     * @throws IllegalArgumentException thrown if the property is not one that Saxon recognizes.
     */

    public Object getConfigurationProperty(String name) {
        final Configuration config = this;
        if (name.equals(FeatureKeys.TREE_MODEL)) {
        	return new Integer(config.getTreeModel());

        } else if (name.equals(FeatureKeys.TIMING)) {
        	return Boolean.valueOf(config.isTiming());

        } else if (name.equals(FeatureKeys.DTD_VALIDATION)) {
        	return Boolean.valueOf(config.isValidation());

        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION)) {
        	return new Integer(config.getSchemaValidationMode());

        } else if (name.equals(FeatureKeys.VALIDATION_WARNINGS)) {
        	return Boolean.valueOf(config.isValidationWarnings());

        } else if (name.equals(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
        	return Boolean.valueOf(config.isAllowExternalFunctions());

        } else if (name.equals(FeatureKeys.RECOGNIZE_URI_QUERY_PARAMETERS)) {
        	return Boolean.valueOf(config.getSystemURIResolver().queryParametersAreRecognized());

        } else if (name.equals(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS)) {
        	return Boolean.valueOf(config.isTraceExternalFunctions());

        } else if (name.equals(FeatureKeys.VERSION_WARNING)) {
        	return Boolean.valueOf(config.isVersionWarning());

        } else if (name.equals(FeatureKeys.TRACE_LISTENER)) {
        	return config.getTraceListener();

        } else if (name.equals(FeatureKeys.COMPILE_WITH_TRACING)) {
        	return Boolean.valueOf(config.isCompileWithTracing());

    	} else if (name.equals(FeatureKeys.LINE_NUMBERING)) {
    		return Boolean.valueOf(config.isLineNumbering());

    	} else if (name.equals(FeatureKeys.RECOVERY_POLICY)) {
    		return new Integer(config.getRecoveryPolicy());

        } else if (name.equals(FeatureKeys.MESSAGE_EMITTER_CLASS)) {
        	return config.getMessageEmitterClass();

        } else if (name.equals(FeatureKeys.SOURCE_PARSER_CLASS)) {
        	return config.getSourceParserClass();

        } else if (name.equals(FeatureKeys.STYLE_PARSER_CLASS)) {
        	return config.getStyleParserClass();

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER)) {
        	return config.getOutputURIResolver();

        } else if (name.equals(FeatureKeys.NAME_POOL)) {
        	return config.getNamePool();

        } else if (name.equals(FeatureKeys.COLLATION_URI_RESOLVER)) {
        	return config.getCollationURIResolver();

        } else if (name.equals(FeatureKeys.COLLECTION_URI_RESOLVER)) {
        	return config.getCollectionURIResolver();

        } else {
	        throw new IllegalArgumentException("Unknown attribute " + name);
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
// Contributor(s): none.
//