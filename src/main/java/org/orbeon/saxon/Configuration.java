package org.orbeon.saxon;

// ORBEON
///*DOTNETONLY*/  import org.orbeon.saxon.dotnet.DotNetPlatform;

import org.orbeon.saxon.event.*;
import org.orbeon.saxon.evpull.PullEventSource;
import org.orbeon.saxon.expr.Optimizer;
import org.orbeon.saxon.expr.PathMap;
import org.orbeon.saxon.expr.PendingUpdateList;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.ExtensionFunctionFactory;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.functions.StandardCollectionURIResolver;
import org.orbeon.saxon.functions.VendorFunctionLibrary;
import org.orbeon.saxon.instruct.Debugger;
import org.orbeon.saxon.instruct.SlotManager;
/*JAVAONLY*/  import org.orbeon.saxon.java.JavaPlatform;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pull.PullProvider;
import org.orbeon.saxon.pull.PullSource;
import org.orbeon.saxon.query.ModuleURIResolver;
import org.orbeon.saxon.query.QueryParser;
import org.orbeon.saxon.query.StandardModuleURIResolver;
import org.orbeon.saxon.sort.CollationURIResolver;
import org.orbeon.saxon.sort.StandardCollationURIResolver;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.sxpath.IndependentContext;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trans.DynamicLoader;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.tree.TreeBuilder;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.*;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.Serializable;
import java.util.*;


/**
 * This class holds details of user-selected configuration options for a set of transformations
 * and/or queries. When running XSLT, the preferred way of setting configuration options is via
 * the JAXP TransformerFactory interface, but the Configuration object provides a finer
 * level of control. As yet there is no standard API for XQuery, so the only way of setting
 * Configuration information is to use the methods on this class directly.
 * <p/>
 * <p>As well as holding configuration settings, this class acts as a factory for classes
 * providing service in particular areas: error handling, URI resolution, and the like. Some
 * of these services are chosen on the basis of the current platform (Java or .NET), some vary
 * depending whether the environment is schema-aware or not.</p>
 * <p/>
 * <p>The <code>Configuration</code> provides access to a {@link NamePool} which is used to manage
 * all the names used in stylesheets, queries, schemas, and source and documents: the NamePool
 * allocates integer codes to these names allowing efficient storage and comparison. Normally
 * there will be a one-to-one relationship between a <code>NamePool</code> and a <code>Configuration</code>.
 * It is possible, however, for several <code>Configuration</code> objects to share the same
 * <code>NamePool</code>. Until Saxon 8.9, by default all <code>Configuration</code> objects
 * shared a single <code>NamePool</code> unless configured otherwise; this changed in 8.9 so that
 * the default is to allocate a new <code>NamePool</code> for each <code>Configuration</code>.</p>
 * <p/>
 * <p>The <code>Configuration</code> establishes the scope within which node identity is managed.
 * Every document belongs to a <code>Configuration</code>, and every node has a distinct identity
 * within that <code>Configuration</code>. In consequence, it is not possible for any query or
 * transformation to manipulate multiple documents unless they all belong to the same
 * <code>Configuration</code>.</p>
 * <p/>
 * <p>Saxon-SA has a subclass of the <code>Configuration</code> class which provides the additional
 * services needed for schema-aware processing. The {@link com.saxonica.validate.SchemaAwareConfiguration}
 * also holds a cache of loaded schema components used for compiling schema-aware transformations
 * and queries, and for validating instance documents.</p>
 * <p/>
 * <p>Since Saxon 8.4, the JavaDoc documentation for Saxon attempts to identify interfaces
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

    private static Platform platform;
    private transient URIResolver uriResolver;
    private StandardURIResolver systemURIResolver = new StandardURIResolver(this);
    protected transient ErrorListener listener;
    private int xmlVersion = XML10;
    protected int xsdlVersion = XSD10;
    private int treeModel = Builder.TINY_TREE;
    private boolean lineNumbering = false;
    private boolean tracing = false;
    private boolean traceOptimizations = false;
    private transient TraceListener traceListener = null;
    private String traceListenerClass = null;

    private FunctionLibrary javaExtensionBinder;
    private FunctionLibrary dotNetExtensionBinder;
    private transient ExtensionFunctionFactory javaExtensionFunctionFactory;
    private transient ExtensionFunctionFactory dotNetExtensionFunctionFactory;

    private CollationURIResolver collationResolver = StandardCollationURIResolver.getInstance();
    private CollectionURIResolver collectionResolver = new StandardCollectionURIResolver();
    private ModuleURIResolver moduleURIResolver = null;
    private ModuleURIResolver standardModuleURIResolver = StandardModuleURIResolver.getInstance();
    private SchemaURIResolver schemaURIResolver = null;
    private transient SourceResolver sourceResolver = this;
    protected VendorFunctionLibrary vendorFunctionLibrary;
    protected int recoveryPolicy = RECOVER_WITH_WARNINGS;
    private String messageEmitterClass = "org.orbeon.saxon.event.MessageEmitter";
    private String sourceParserClass;
    private String styleParserClass;
    private boolean preferJaxpParser;
    private transient OutputURIResolver outputURIResolver;
    private boolean timing = false;
    private boolean versionWarning = false;
    private boolean allowExternalFunctions = true;
    private boolean traceExternalFunctions = false;
    private boolean validation = false;
    private boolean allNodesUntyped = false;
    private boolean lazyConstructionMode = false;
    private boolean allowMultiThreading = false;
    private boolean preEvaluateDocFunction = false;
    private boolean useXsiSchemaLocation = true;
    private int stripsWhiteSpace = Whitespace.IGNORABLE;
    private boolean xIncludeAware = false;
    private boolean useDisableOutputEscaping = false;
    private NamePool namePool = null;
    private DocumentNumberAllocator documentNumberAllocator = new DocumentNumberAllocator();
    private DocumentPool globalDocumentPool = new DocumentPool();
    private transient XPathContext conversionContext = null;
    private transient TypeHierarchy typeHierarchy;

    private int hostLanguage = XSLT;
    private int schemaValidationMode = Validation.PRESERVE;
    private boolean validationWarnings = false;
    private boolean expandDefaultAttributes = true;
    private boolean retainDTDattributeTypes = false;
    private transient Debugger debugger = null;
    protected Optimizer optimizer = null;
    private transient DynamicLoader dynamicLoader = new DynamicLoader();

    private SerializerFactory serializerFactory = new SerializerFactory();


    //private int implicitTimezone;
    private transient List sourceParserPool = new ArrayList(5);
    private transient List styleParserPool = new ArrayList(5);

    /**
     * The external object models are held in static so they are only loaded once in an application
     * that creates many Configurations repeatedly. This saves expensive searches of the classpath
     */

    private static List sharedExternalObjectModels = null;
    private List externalObjectModels = null;
    private int domLevel = 3;


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
     * Language versions for XML Schema
     */
    public static int XSD10 = 10;
    public static int XSD11 = 11;

    /**
     * Static initialization
     */

    static {

        // This code is designed to be modified by the build process so only the appropriate code for the
        // platform is included. However, it's also designed so that it can run correctly without any
        // special build action.

        /*JAVAONLY*/ platform = JavaPlatform.getInstance();

        /*DOTNETONLY*/ /*JAVAONLY*/ if (System.getProperty("java.vendor").equals("Jeroen Frijters")) {
            //System.err.println("Call to create .NET platform currently disabled in Configuration.java (needed for JDK1.4)");
// ORBEON
//            /*DOTNETONLY*/ platform = DotNetPlatform.getInstance();
            /*DOTNETONLY*/ /*JAVAONLY*/ }
//        System.err.println(System.getProperty("java.vendor"));
//        System.err.println(platform.getClass().getName());

    }


    /**
     * Create a non-schema-aware configuration object with default settings for all options.
     *
     * @since 8.4
     */

    public Configuration() {
        init();
    }

    /**
     * Factory method to create a Configuration. The resulting configuration will be schema-aware if
     * Saxon-SA is installed and if a license is available; otherwise it will be non-schema-aware.
     * Note that the license might not permit all processing options.
     *
     * @param classLoader - the class loader to be used. If null, the context class loader for the current
     *                    thread is used.
     * @param className   - the name of the schema aware configuration class. Defaults to
     *                    "com.saxonica.validate.SchemaAwareConfiguration" if null is supplied. This allows an assembly
     *                    qualified name to be supplied under .NET. The class, once instantiated, must be an instance
     *                    of Configuration, but despite the name of this method there is nothing that requires it to
     *                    be schema-aware.
     * @return a schema-aware configuration object if Saxon-SA can be loaded and a valid license is
     *         installed; otherwise, a non-schema-aware configuration object
     * @since 9.0
     */

    public static Configuration makeConfiguration(ClassLoader classLoader, String className) {
        try {
            Configuration c = makeSchemaAwareConfiguration(classLoader, className);
            if (c.isSchemaAware(XML_SCHEMA)) {
                return c;
            } else {
                return new Configuration();
            }
        } catch (RuntimeException err) {
            return new Configuration();
        }
    }


    protected void init() {
        //BuiltInListType.init();
        platform.initialize(this);
        if (platform.isDotNet()) {
            externalObjectModels = new ArrayList(3);
        } else {
            synchronized (Configuration.class) {
                if (sharedExternalObjectModels == null) {
                    registerStandardObjectModels();
                }
                externalObjectModels = new ArrayList(sharedExternalObjectModels);
            }
        }
        //namePool = NamePool.getDefaultNamePool();
        namePool = new NamePool();
        platform.makeExtensionLibrary(this);

        // Get the implicit timezone from the current system clock
        //GregorianCalendar calendar = new GregorianCalendar();
        //int tzmsecs = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET));
        //implicitTimezone = tzmsecs / 60000;
    }

    /**
     * Static method to instantiate a schema-aware configuration.
     * <p>On the .NET platform, this method should not be called unless it is known that the assembly
     * saxon9sa.dll has already been loaded. This can be achieved by an appropriate call on Assembly.Load():
     * for an example, see the C# Configuration.cs class in the Saxon.Api namespace.</p>
     * <p>This method fails if Saxon-SA cannot be loaded, but it does not fail if there is no license
     * available. In that case it returns a schema-aware configuration object, but any attempt to use
     * schema-aware processing will fail.
     *
     * @param classLoader - the class loader to be used. If null, the context class loader for the current
     *                    thread is used.
     * @param className   - the name of the schema aware configuration class. Defaults to
     *                    "com.saxonica.validate.SchemaAwareConfiguration" if null is supplied. This allows an assembly
     *                    qualified name to be supplied under .NET. The class, once instantiated, must be an instance
     *                    of Configuration, but despite the name of this method there is nothing that requires it to
     *                    be schema-aware.
     * @return the new SchemaAwareConfiguration
     * @throws RuntimeException if the Saxon-SA product cannot be loaded
     */

    public static Configuration makeSchemaAwareConfiguration(ClassLoader classLoader, String className)
            throws RuntimeException {
        if (className == null) {
            className = "com.saxonica.validate.SchemaAwareConfiguration";
        }
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
                    theClass = loader.loadClass(className);
                } catch (Exception ex) {
                    theClass = Class.forName(className);
                }
            } else {
                theClass = Class.forName(className);
            }
            return (Configuration)theClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copy an existing Configuration to create a new Configuration.
     * This is a shallow copy. The new Configuration will share all the option settings of the old;
     * it will also share the same NamePool, and the same DocumentNumberAllocator. If this configuration
     * is schema-aware then the new one will also be schema-aware, and will share the same Schema manager
     * and so on. (So any schema component loaded into one configuration will affect both).
     * <p/>
     * <p>Note that creating a new SchemaAwareConfiguration using this method can be significantly cheaper
     * than creating one from scratch, because it avoids the need to verify the Saxon-SA license key if this
     * has already been done.</p>
     *
     * @return a shallow copy of this Configuration
     */

    public Configuration copy() {
        Configuration c = new Configuration();
        copyTo(c);
        return c;
    }


    protected void copyTo(Configuration c) {
        c.uriResolver = uriResolver;
        c.systemURIResolver = systemURIResolver;
        c.listener = listener;
        c.xmlVersion = xmlVersion;
        c.treeModel = treeModel;
        c.lineNumbering = lineNumbering;
        c.tracing = tracing;
        c.traceOptimizations = traceOptimizations;
        c.traceListener = traceListener;
        c.javaExtensionBinder = javaExtensionBinder;
        c.dotNetExtensionBinder = dotNetExtensionBinder;
        c.javaExtensionFunctionFactory = javaExtensionFunctionFactory;
        c.dotNetExtensionFunctionFactory = dotNetExtensionFunctionFactory;
        c.collationResolver = collationResolver;
        c.collectionResolver = collectionResolver;
        c.moduleURIResolver = moduleURIResolver;
        c.standardModuleURIResolver = standardModuleURIResolver;
        c.schemaURIResolver = schemaURIResolver;
        c.sourceResolver = sourceResolver;
        c.vendorFunctionLibrary = vendorFunctionLibrary;
        c.recoveryPolicy = recoveryPolicy;
        c.messageEmitterClass = messageEmitterClass;
        c.sourceParserClass = sourceParserClass;
        c.styleParserClass = styleParserClass;
        c.outputURIResolver = outputURIResolver;
        c.timing = timing;
        c.versionWarning = versionWarning;
        c.allowExternalFunctions = allowExternalFunctions;
        c.validation = validation;
        c.allNodesUntyped = allNodesUntyped;
        c.lazyConstructionMode = lazyConstructionMode;
        c.allowMultiThreading = allowMultiThreading;
        c.preEvaluateDocFunction = preEvaluateDocFunction;
        c.stripsWhiteSpace = stripsWhiteSpace;
        c.xIncludeAware = xIncludeAware;
        c.namePool = namePool;
        c.documentNumberAllocator = documentNumberAllocator;
        c.conversionContext = conversionContext;
        c.typeHierarchy = typeHierarchy;
        c.hostLanguage = hostLanguage;
        c.schemaValidationMode = schemaValidationMode;
        c.validationWarnings = validationWarnings;
        c.expandDefaultAttributes = expandDefaultAttributes;
        c.retainDTDattributeTypes = retainDTDattributeTypes;
        c.debugger = debugger;
        c.optimizer = optimizer;
        c.serializerFactory = serializerFactory;
        c.dynamicLoader = dynamicLoader;
        c.sourceParserPool = sourceParserPool;
        c.externalObjectModels = externalObjectModels;
        c.domLevel = domLevel;
    }

    /**
     * Get a message used to identify this product when a transformation is run using the -t option
     *
     * @return A string containing both the product name and the product
     *         version
     * @since 8.4
     */

    public String getProductTitle() {
        return "Saxon " + Version.getProductVersion() + platform.getPlatformSuffix() + " from Saxonica";
    }

    /**
     * Determine if the configuration is schema-aware, for the given host language
     *
     * @param language the required host language: XSLT, XQUERY, or XML_SCHEMA
     * @return true if the configuration is schema-aware
     * @since 8.4
     */

    public boolean isSchemaAware(int language) {
        return false;
        // changing this to true will do no good!
    }

    /**
     * Display a message about the license status
     */

    public void displayLicenseMessage() {
    }

    /**
     * Get the host language used in this configuration. The typical values
     * are XSLT and XQUERY. The values XML_SCHEMA and JAVA_APPLICATION may also
     * be encountered.
     * <p/>
     * This method is problematic because it is possible to run multiple transformations
     * or queries within the same configuration. The method is therefore best avoided.
     * Instead, use {@link org.orbeon.saxon.expr.Container#getHostLanguage}.
     * Internally its only use is in deciding (in Saxon-SA only) which error listener to
     * use by default at compile time, and since the standard XSLT and XQuery listeners have
     * no differences when used for static errors, the choice is immaterial.
     *
     * @return Configuration.XSLT or Configuration.XQUERY
     */

    public int getHostLanguage() {
        return hostLanguage;
    }

    /**
     * Set the host language used in this configuration. The possible values
     * are XSLT and XQUERY.
     *
     * @param hostLanguage Configuration.XSLT or Configuration.XQUERY
     */

    public void setHostLanguage(int hostLanguage) {
        this.hostLanguage = hostLanguage;
    }

//    /**
//     * Set the Platform to be used for platform-dependent methods
//     * @param platform the platform to be used
//     */
//
//    public void setPlatform(Platform platform) {
//        this.platform = platform;
//    }

    /**
     * Get the Platform to be used for platform-dependent methods
     *
     * @return the platform to be used
     */

    public static Platform getPlatform() {
        return platform;
    }

    /**
     * Set the DynamicLoader to be used. By default an instance of {@link DynamicLoader} is used
     * for all dynamic loading of Java classes. This method allows the actions of the standard
     * DynamicLoader to be overridden
     *
     * @param dynamicLoader the DynamicLoader to be used by this Configuration
     */

    public void setDynamicLoader(DynamicLoader dynamicLoader) {
        this.dynamicLoader = dynamicLoader;
    }

    /**
     * Get the DynamicLoader used by this Configuration. By default the standard system-supplied
     * dynamic loader is returned.
     *
     * @return the DynamicLoader in use - either a user-supplied DynamicLoader, or the standard one
     *         supplied by the system.
     */

    public DynamicLoader getDynamicLoader() {
        return dynamicLoader;
    }

    /**
     * Load a class using the class name provided.
     * Note that the method does not check that the object is of the right class.
     * <p/>
     * This method is intended for internal use only. The call is delegated to the
     * <code>DynamicLoader</code>, which may be overridden by a user-defined <code>DynamicLoader</code>.
     *
     * @param className   A string containing the name of the
     *                    class, for example "com.microstar.sax.LarkDriver"
     * @param tracing     true if diagnostic tracing is required
     * @param classLoader The ClassLoader to be used to load the class, or null to
     *                    use the ClassLoader selected by the DynamicLoader.
     * @return an instance of the class named, or null if it is not
     *         loadable.
     * @throws XPathException if the class cannot be loaded.
     */

    public Class getClass(String className, boolean tracing, ClassLoader classLoader) throws XPathException {
        return dynamicLoader.getClass(className, tracing, classLoader);
    }

    /**
     * Instantiate a class using the class name provided.
     * Note that the method does not check that the object is of the right class.
     * <p/>
     * This method is intended for internal use only. The call is delegated to the
     * <code>DynamicLoader</code>, which may be overridden by a user-defined <code>DynamicLoader</code>.
     * <p/>
     * Diagnostic output is produced if the option "isTiming" is set (corresponding to the -t option on
     * the command line).
     *
     * @param className   A string containing the name of the
     *                    class, for example "com.microstar.sax.LarkDriver"
     * @param classLoader The ClassLoader to be used to load the class, or null to
     *                    use the ClassLoader selected by the DynamicLoader.
     * @return an instance of the class named, or null if it is not
     *         loadable.
     * @throws XPathException if the class cannot be loaded.
     */

    public Object getInstance(String className, ClassLoader classLoader) throws XPathException {
        return dynamicLoader.getInstance(className, isTiming(), classLoader);
    }

    /**
     * Get the URIResolver used in this configuration
     *
     * @return the URIResolver. If no URIResolver has been set explicitly, the
     *         default URIResolver is used.
     * @since 8.4
     */

    public URIResolver getURIResolver() {
        if (uriResolver == null) {
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
     *
     * @param resolver The URIResolver to be used.
     * @since 8.4
     */

    public void setURIResolver(URIResolver resolver) {
        uriResolver = resolver;
        if (resolver instanceof StandardURIResolver) {
            ((StandardURIResolver)resolver).setConfiguration(this);
        }
    }

    /**
     * Set the URIResolver to a URI resolver that allows query parameters after the URI,
     * and in the case of Saxon-SA, that inteprets the file extension .ptree
     */

    public void setParameterizedURIResolver() {
        getSystemURIResolver().setRecognizeQueryParameters(true);
    }

    /**
     * Get the system-defined URI Resolver. This is used when the user-defined URI resolver
     * returns null as the result of the resolve() method
     *
     * @return the system-defined URI resolver
     */

    public StandardURIResolver getSystemURIResolver() {
        return systemURIResolver;
    }

    /**
     * Create an instance of a URIResolver with a specified class name.
     * Note that this method does not register the URIResolver with this Configuration.
     *
     * @param className The fully-qualified name of the URIResolver class
     * @return The newly created URIResolver
     * @throws TransformerException if the requested class does not
     *                              implement the javax.xml.transform.URIResolver interface
     */
    public URIResolver makeURIResolver(String className) throws TransformerException {
        Object obj = dynamicLoader.getInstance(className, null);
        if (obj instanceof StandardURIResolver) {
            ((StandardURIResolver)obj).setConfiguration(this);
        }
        if (obj instanceof URIResolver) {
            return (URIResolver)obj;
        }
        throw new XPathException("Class " + className + " is not a URIResolver");
    }

    /**
     * Get the ErrorListener used in this configuration. If no ErrorListener
     * has been supplied explicitly, the default ErrorListener is used.
     *
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
     *
     * @param listener the ErrorListener to be used
     * @since 8.4
     */

    public void setErrorListener(ErrorListener listener) {
        this.listener = listener;
    }


    /**
     * Report a fatal error
     *
     * @param err the exception to be reported
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
     *
     * @param multithreading true if multithreading optimizations area allowed
     */

    public void setMultiThreading(boolean multithreading) {
        allowMultiThreading = multithreading;
    }

    /**
     * Determine whether multithreading optimizations are allowed
     *
     * @return true if multithreading optimizations are allowed
     */

    public boolean isMultiThreading() {
        return allowMultiThreading;
    }

    /**
     * Set the XML version to be used by default for validating characters and names.
     * Note that source documents specifying xml version="1.0" or "1.1" are accepted
     * regardless of this setting. The effect of this switch is to change the validation
     * rules for types such as Name and NCName, to change the meaning of \i and \c in
     * regular expressions, and to determine whether the serializer allows XML 1.1 documents
     * to be constructed.
     *
     * @param version one of the constants XML10 or XML11
     * @since 8.6
     */

    public void setXMLVersion(int version) {
        xmlVersion = version;
    }

    /**
     * Get the XML version to be used by default for validating characters and names
     *
     * @return one of the constants {@link #XML10} or {@link #XML11}
     * @since 8.6
     */

    public int getXMLVersion() {
        return xmlVersion;
    }

    /**
     * Get a class that can be used to check names against the selected XML version
     *
     * @return a class that can be used for name checking
     * @since 8.6
     */

    public NameChecker getNameChecker() {
        //noinspection RedundantCast
        return (xmlVersion == XML10 ?
                (NameChecker)Name10Checker.getInstance() :
                (NameChecker)Name11Checker.getInstance());
    }

    /**
     * Get an XPathContext object with sufficient capability to perform comparisons and conversions
     *
     * @return a dynamic context for performing conversions
     */

    public XPathContext getConversionContext() {
        if (conversionContext == null) {
            conversionContext = new IndependentContext(this).makeEarlyEvaluationContext();
        }
        return conversionContext;
    }

    /**
     * Get the Tree Model used by this Configuration. This is either
     * {@link Builder#LINKED_TREE} or {@link Builder#TINY_TREE}. The default (confusingly)
     * is <code>Builder.TINY_TREE</code>.
     *
     * @return the selected Tree Model
     * @since 8.4
     */

    public int getTreeModel() {
        return treeModel;
    }

    /**
     * Set the Tree Model used by this Configuration. This is either
     * {@link Builder#LINKED_TREE} or {@link Builder#TINY_TREE}. The default (confusingly)
     * is <code>Builder.TINY_TREE</code>.
     *
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
     *
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
     *
     * @param lineNumbering true if line numbers are maintained in source documents
     * @since 8.4
     */

    public void setLineNumbering(boolean lineNumbering) {
        this.lineNumbering = lineNumbering;
    }

    /**
     * Set whether or not source documents (including stylesheets and schemas) are have
     * XInclude processing applied to them, or not. Default is false.
     *
     * @param state true if XInclude elements are to be expanded, false if not
     * @since 8.9
     */

    public void setXIncludeAware(boolean state) {
        xIncludeAware = state;
    }

    /**
     * Test whether or not source documents (including stylesheets and schemas) are to have
     * XInclude processing applied to them, or not
     *
     * @return true if XInclude elements are to be expanded, false if not
     * @since 8.9
     */

    public boolean isXIncludeAware() {
        return xIncludeAware;
    }

    /**
     * Get the TraceListener used for run-time tracing of instruction execution.
     *
     * @return the TraceListener that was set using {@link #getTraceListener()} if set.
     * Otherwise, returns null.
     * @since 8.4. Modified in 9.1.
     */

    public TraceListener getTraceListener() {
        return traceListener;
    }


    /**
     * Get or create the TraceListener used for run-time tracing of instruction execution.
     *
     * @return If a TraceListener has been set using {@link #setTraceListener(org.orbeon.saxon.trace.TraceListener)},
     * returns that TraceListener. Otherwise, if a TraceListener class has been set using
     * {@link #setTraceListenerClass(String)}, returns a newly created instance of that class.
     * Otherwise, returns null.
     * @throws XPathException if the supplied TraceListenerClass cannot be instantiated as an instance
     * of TraceListener
     * @since 9.1.
     */

    public TraceListener makeTraceListener() throws XPathException {
        if (traceListener != null) {
            return traceListener;
        } else if (traceListenerClass != null) {
            try {
                return makeTraceListener(traceListenerClass);
            } catch (ClassCastException e) {
                throw new XPathException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * Set the TraceListener to be used for run-time tracing of instruction execution.
     * <p/>
     * <p>Note: this method should not be used if the Configuration is multithreading. In that situation,
     * use {@link #setCompileWithTracing(boolean)} to force stylesheets and queries to be compiled
     * with trace code enabled, and use {@link Controller#addTraceListener(org.orbeon.saxon.trace.TraceListener)} to
     * supply a TraceListener at run time.</p>
     *
     * @param traceListener The TraceListener to be used.
     * @since 8.4
     */

    public void setTraceListener(TraceListener traceListener) {
        this.traceListener = traceListener;
        setCompileWithTracing(true);
        setMultiThreading(false);
    }

    /**
     * Set the name of the trace listener class to be used for run-time tracing of instruction
     * execution. A new instance of this class will be created for each query or transformation
     * that requires tracing. The class must be an instance of {@link TraceListener}.
     * @param className the name of the trace listener class
     * @throws IllegalArgumentException if the class cannot be instantiated or does not implement
     * TraceListener
     * @since 9.1
     */

    public void setTraceListenerClass(String className) {
        try {
            makeTraceListener(className);
        } catch (XPathException err) {
            throw new IllegalArgumentException(className + ": " + err.getMessage());
        }
        this.traceListenerClass = className;
        setCompileWithTracing(true);
    }

    /**
     * Get the name of the trace listener class to be used for run-time tracing of instruction
     * execution. A new instance of this class will be created for each query or transformation
     * that requires tracing. The class must be an instance of {@link TraceListener}.
     * @return the name of the trace listener class, or null if no trace listener class
     * has been nominated.
     * @since 9.1
     */

    public String getTraceListenerClass() {
        return traceListenerClass;
    }

    /**
     * Determine whether compile-time generation of trace code was requested
     *
     * @return true if compile-time generation of code was requested
     * @since 8.8
     */

    public boolean isCompileWithTracing() {
        return tracing;
    }

    /**
     * Request compile-time generation of trace code (or not)
     *
     * @param trace true if compile-time generation of trace code is required
     * @since 8.8
     */

    public void setCompileWithTracing(boolean trace) {
        tracing = trace;
    }

    /**
     * Set optimizer tracing on or off
     *
     * @param trace set to true to switch optimizer tracing on, false to switch it off
     */

    public void setOptimizerTracing(boolean trace) {
        traceOptimizations = trace;
    }

    /**
     * Test whether optimizer tracing is on or off
     *
     * @return true if optimizer tracing is switched on
     */

    public boolean isOptimizerTracing() {
        return traceOptimizations;
    }


    /**
     * Create an instance of a TraceListener with a specified class name
     *
     * @param className The fully qualified class name of the TraceListener to
     *                  be constructed
     * @return the newly constructed TraceListener
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the requested class does not
     *          implement the org.orbeon.saxon.trace.TraceListener interface
     */

    public TraceListener makeTraceListener(String className)
            throws XPathException {
        Object obj = dynamicLoader.getInstance(className, null);
        if (obj instanceof TraceListener) {
            return (TraceListener)obj;
        }
        throw new XPathException("Class " + className + " is not a TraceListener");
    }

    /**
     * Set the FunctionLibrary used to bind calls on extension functions. This allows the
     * rules for identifying extension functions to be customized (in principle, it would
     * allow support for extension functions in other languages to be provided).
     * <p/>
     * When an application supplies its own FunctionLibrary for binding extension functions,
     * this replaces the default binding mechanism for Java extension functions, namely
     * {@link org.orbeon.saxon.functions.JavaExtensionLibrary}. It thus disables the function libraries
     * for built-in Saxon extensions and for EXSLT extensions. It is possible to create a
     * function library that adds to the existing mechanisms, rather than replacing them,
     * by supplying as the FunctionLibrary a {@link org.orbeon.saxon.functions.FunctionLibraryList}
     * that itself contains two FunctionLibrary objects: a JavaExtensionLibrary, and a user-written
     * FunctionLibrary.
     *
     * @param scheme The URI scheme served by the extension binder. Currently this must be one
     *               of "java" or "clitype". On the Java platform, the only scheme currently supported is "java";
     *               on the .NET platform, the "java" and "clitype" schemes coexist.
     * @param binder The FunctionLibrary object used to locate implementations of extension
     *               functions, based on their name and arity
     * @see #setExtensionFunctionFactory
     */

    public void setExtensionBinder(String scheme, FunctionLibrary binder) {
        if (scheme.equals("java")) {
            javaExtensionBinder = binder;
        } else if (scheme.equals("clitype")) {
            dotNetExtensionBinder = binder;
        } else {
            throw new IllegalArgumentException("Unknown scheme " + scheme + " - must be java or clitype");
        }
    }

    /**
     * Get the FunctionLibrary used to bind calls on extension functions on the specified
     * platform.
     * <p/>
     * This mechanism is for advanced users only, and the details are subject to change.
     *
     * @param scheme The URI scheme served by the extension binder. Currently this must be one
     *               of "java" or "clitype". On the Java platform, the only scheme currently supported is "java";
     *               on the .NET platform, the "java" and "clitype" schemes coexist.
     * @return the registered FunctionLibrary for extension functions if one has been
     *         registered; or the default FunctionLibrary for extension functions otherwise
     */

    public FunctionLibrary getExtensionBinder(String scheme) {
        if (scheme.equals("java")) {
            return javaExtensionBinder;
        } else if (scheme.equals("clitype")) {
            return dotNetExtensionBinder;
        } else {
            throw new IllegalArgumentException("Unknown scheme " + scheme + " - must be java or clitype");
        }
    }

    /**
     * Get the FunctionLibrary used to bind calls on Saxon-defined extension functions.
     * <p/>
     * This method is intended for internal use only.
     *
     * @return the FunctionLibrary used for extension functions in the Saxon library.
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
     * <p/>
     * Note that it is undefined whether collation URIs are resolved at compile time
     * or at run-time. It is therefore inadvisable to change the CollationURIResolver after
     * compiling a query or stylesheet and before running it.
     *
     * @param resolver the collation URI resolver to be used. This replaces any collation
     *                 URI resolver previously registered.
     * @since 8.5
     */

    public void setCollationURIResolver(CollationURIResolver resolver) {
        collationResolver = resolver;
    }

    /**
     * Get the collation URI resolver associated with this configuration. This will
     * return the CollationURIResolver previously set using the {@link #setCollationURIResolver}
     * method; if this has not been called, it returns the system-defined collation URI resolver
     *
     * @return the registered CollationURIResolver
     * @since 8.5
     */

    public CollationURIResolver getCollationURIResolver() {
        return collationResolver;
    }

    /**
     * Set a CollectionURIResolver to be used to resolve collection URIs (that is,
     * the URI supplied in a call to the collection() function).
     * <p/>
     * Collection URIs are always resolved at run-time, using the CollectionURIResolver
     * in force at the time the collection() function is called.
     *
     * @param resolver the collection URI resolver to be used. This replaces any collection
     *                 URI resolver previously registered.
     * @since 8.5
     */

    public void setCollectionURIResolver(CollectionURIResolver resolver) {
        collectionResolver = resolver;
    }

    /**
     * Get the collection URI resolver associated with this configuration. This will
     * return the CollectionURIResolver previously set using the {@link #setCollectionURIResolver}
     * method; if this has not been called, it returns the system-defined collection URI resolver
     *
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
     *
     * @param resolver the URI resolver for XQuery modules
     */

    public void setModuleURIResolver(ModuleURIResolver resolver) {
        moduleURIResolver = resolver;
    }

    /**
     * Create and register an instance of a ModuleURIResolver with a specified class name.
     * This will be used for resolving URIs in XQuery "import module" declarations, unless
     * a more specific ModuleURIResolver has been nominated as part of the StaticQueryContext.
     *
     * @param className The fully-qualified name of the LocationHintResolver class
     * @throws TransformerException if the requested class does not
     *                              implement the org.orbeon.saxon.LocationHintResolver interface
     */
    public void setModuleURIResolver(String className) throws TransformerException {
        Object obj = dynamicLoader.getInstance(className, null);
        if (obj instanceof ModuleURIResolver) {
            setModuleURIResolver((ModuleURIResolver)obj);
        } else {
            throw new XPathException("Class " + className + " is not a LocationHintResolver");
        }
    }

    /**
     * Get the user-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in the XQuery prolog; returns null if none has been explicitly set.
     *
     * @return the resolver for Module URIs
     */

    public ModuleURIResolver getModuleURIResolver() {
        return moduleURIResolver;
    }

    /**
     * Get the standard system-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in the XQuery prolog.
     *
     * @return the standard system-defined ModuleURIResolver for resolving URIs
     */

    public ModuleURIResolver getStandardModuleURIResolver() {
        return standardModuleURIResolver;
    }

    /**
     * Set a user-defined SchemaURIResolver for resolving URIs used in "import schema"
     * declarations.
     *
     * @param resolver the URI resolver used for import schema declarations
     */

    public void setSchemaURIResolver(SchemaURIResolver resolver) {
        schemaURIResolver = resolver;
    }

    /**
     * Get the user-defined SchemaURIResolver for resolving URIs used in "import schema"
     * declarations; if none has been explicitly set, returns null.
     *
     * @return the user-defined SchemaURIResolver for resolving URIs
     */

    public SchemaURIResolver getSchemaURIResolver() {
        return schemaURIResolver;
    }


    /**
     * Determine how recoverable run-time errors are to be handled. This applies
     * only if the standard ErrorListener is used.
     *
     * @return the current recovery policy. The options are {@link #RECOVER_SILENTLY},
     *         {@link #RECOVER_WITH_WARNINGS}, or {@link #DO_NOT_RECOVER}.
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
     *
     * @param recoveryPolicy the recovery policy to be used. The options are {@link #RECOVER_SILENTLY},
     *                       {@link #RECOVER_WITH_WARNINGS}, or {@link #DO_NOT_RECOVER}.
     * @since 8.4
     */

    public void setRecoveryPolicy(int recoveryPolicy) {
        this.recoveryPolicy = recoveryPolicy;
    }

    /**
     * Get the name of the class that will be instantiated to create a MessageEmitter,
     * to process the output of xsl:message instructions in XSLT.
     *
     * @return the full class name of the message emitter class.
     * @since 8.4
     */

    public String getMessageEmitterClass() {
        return messageEmitterClass;
    }

    /**
     * Set the name of the class that will be instantiated to create a MessageEmitter,
     * to process the output of xsl:message instructions in XSLT.
     *
     * @param messageEmitterClass the full class name of the message emitter class. This
     *                            must implement org.orbeon.saxon.event.Emitter.
     * @since 8.4
     */

    public void setMessageEmitterClass(String messageEmitterClass) {
        this.messageEmitterClass = messageEmitterClass;
    }

    /**
     * Get the name of the class that will be instantiated to create an XML parser
     * for parsing source documents (for example, documents loaded using the document()
     * or doc() functions).
     * <p/>
     * This method is retained in Saxon for backwards compatibility, but the preferred way
     * of choosing an XML parser is to use JAXP interfaces, for example by supplying a
     * JAXP Source object initialized with an appropriate implementation of org.xml.sax.XMLReader.
     *
     * @return the fully qualified name of the XML parser class
     */

    public String getSourceParserClass() {
        return sourceParserClass;
    }

    /**
     * Set the name of the class that will be instantiated to create an XML parser
     * for parsing source documents (for example, documents loaded using the document()
     * or doc() functions).
     * <p/>
     * This method is retained in Saxon for backwards compatibility, but the preferred way
     * of choosing an XML parser is to use JAXP interfaces, for example by supplying a
     * JAXP Source object initialized with an appropriate implementation of org.xml.sax.XMLReader.
     *
     * @param sourceParserClass the fully qualified name of the XML parser class. This must implement
     *                          the SAX2 XMLReader interface.
     */

    public void setSourceParserClass(String sourceParserClass) {
        this.sourceParserClass = sourceParserClass;
    }

    /**
     * Get the name of the class that will be instantiated to create an XML parser
     * for parsing stylesheet modules.
     * <p/>
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
     * <p/>
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
     *
     * @return the OutputURIResolver. If none has been supplied explicitly, the
     *         default OutputURIResolver is returned.
     * @since 8.4
     */

    public OutputURIResolver getOutputURIResolver() {
        if (outputURIResolver == null) {
            outputURIResolver = StandardOutputResolver.getInstance();
        }
        return outputURIResolver;
    }

    /**
     * Set the OutputURIResolver that will be used to resolve URIs used in the
     * href attribute of the xsl:result-document instruction.
     *
     * @param outputURIResolver the OutputURIResolver to be used.
     * @since 8.4
     */

    public void setOutputURIResolver(OutputURIResolver outputURIResolver) {
        this.outputURIResolver = outputURIResolver;
    }

    /**
     * Set a custom SerializerFactory. This will be used to create a serializer for a given
     * set of output properties and result destination.
     *
     * @param factory a custom SerializerFactory
     * @since 8.8
     */

    public void setSerializerFactory(SerializerFactory factory) {
        serializerFactory = factory;
    }

    /**
     * Get the SerializerFactory. This returns the standard built-in SerializerFactory, unless
     * a custom SerializerFactory has been registered.
     *
     * @return the SerializerFactory in use
     * @since 8.8
     */

    public SerializerFactory getSerializerFactory() {
        return serializerFactory;
    }

    /**
     * Determine whether brief progress messages and timing information will be output
     * to System.err.
     * <p/>
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
     * <p/>
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
     *
     * @return true if these messages are to be output.
     * @since 8.4
     */

    public boolean isVersionWarning() {
        return versionWarning;
    }

    /**
     * Determine whether a warning is to be output when running against a stylesheet labelled
     * as version="1.0". The XSLT specification requires such a warning unless the user disables it.
     *
     * @param warn true if these messages are to be output.
     * @since 8.4
     */

    public void setVersionWarning(boolean warn) {
        versionWarning = warn;
    }

    /**
     * Determine whether calls to external Java functions are permitted.
     *
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
     * where the query/stylesheet is executed.
     * <p/>
     * <p>Setting the value to false disallows all of the following:</p>
     * <p/>
     * <ul>
     * <li>Calls to Java extension functions</li>
     * <li>Use of the XSLT system-property() function to access Java system properties</li>
     * <li>Use of a relative URI in the <code>xsl:result-document</code> instruction</li>
     * <li>Calls to XSLT extension instructions</li>
     * </ul>
     * <p/>
     * <p>Note that this option does not disable use of the <code>doc()</code> function or similar
     * functions to access the filestore of the machine where the transformation or query is running.
     * That should be done using a user-supplied <code>URIResolver</code></p>
     *
     * @param allowExternalFunctions true if external function calls are to be
     *                               permitted.
     * @since 8.4
     */

    public void setAllowExternalFunctions(boolean allowExternalFunctions) {
        this.allowExternalFunctions = allowExternalFunctions;
    }

    /**
     * Determine whether calls on external functions are to be traced for diagnostic
     * purposes.
     *
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
     *
     * @param traceExternalFunctions true if tracing is to be enabled
     *                               for calls to external Java functions
     */

    public void setTraceExternalFunctions(boolean traceExternalFunctions) {
        this.traceExternalFunctions = traceExternalFunctions;
    }

    /**
     * Get an ExtensionFunctionFactory. This is used at compile time for generating
     * the code that calls Java or .NET extension functions. It is possible to supply a user-defined
     * ExtensionFunctionFactory to customize the way extension functions are bound.
     * <p/>
     * This mechanism is intended for advanced use only, and is subject to change.
     *
     * @param scheme - the extension function scheme. This must be one of "java" or "clitype",
     *               corresponding to the scheme name in the namespace URI of the extension function call
     * @return the factory object registered to generate calls on extension functions,
     *         if one has been registered; if not, the default factory used by Saxon. The result
     *         will always be a {@link org.orbeon.saxon.functions.JavaExtensionFunctionFactory} in the case of the Java platform,
     *         or a <code>org.orbeon.saxon.dotnet.DotNetExtensionFunctionFactory</code> on the .NET platform
     */

    public ExtensionFunctionFactory getExtensionFunctionFactory(String scheme) {
        if ("java".equals(scheme)) {
            return javaExtensionFunctionFactory;
        } else if ("clitype".equals(scheme)) {
            return dotNetExtensionFunctionFactory;
        } else {
            throw new IllegalArgumentException("Unknown extension function scheme");
        }
    }

    /**
     * Set an ExtensionFunctionFactory. This is used at compile time for generating
     * the code that calls Java extension functions. It is possible to supply a user-defined
     * ExtensionFunctionFactory to customize the way extension functions are called. The
     * ExtensionFunctionFactory determines how external methods are called, but is not
     * involved in binding the external method corresponding to a given function name or URI.
     * <p/>
     * This mechanism is intended for advanced use only, and is subject to change.
     *
     * @param scheme  - the extension function scheme. This must be one of "java" or "clitype",
     *                corresponding to the scheme name in the namespace URI of the extension function call
     * @param factory The extension function factory. This must
     *                always be a {@link org.orbeon.saxon.functions.JavaExtensionFunctionFactory} for the "java" scheme,
     *                or a <code>org.orbeon.saxon.dotnet.DotNetExtensionFunctionFactory</code> for the "clitype" scheme
     * @see #setExtensionBinder
     */

    public void setExtensionFunctionFactory(String scheme, ExtensionFunctionFactory factory) {
        if ("java".equals(scheme)) {
            javaExtensionFunctionFactory = factory;
        } else if ("clitype".equals(scheme)) {
            dotNetExtensionFunctionFactory = factory;
        } else {
            throw new IllegalArgumentException("Unknown extension function scheme");
        }
    }

    /**
     * Determine whether the XML parser for source documents will be asked to perform
     * \ validation of source documents
     *
     * @return true if DTD validation is requested.
     * @since 8.4
     */

    public boolean isValidation() {
        return validation;
    }

    /**
     * Determine whether the XML parser for source documents will be asked to perform
     * DTD validation of source documents
     *
     * @param validation true if DTD validation is to be requested.
     * @since 8.4
     */

    public void setValidation(boolean validation) {
        this.validation = validation;
    }

    /**
     * Specify that all nodes encountered within this query or transformation will be untyped
     *
     * @param allUntyped true if all nodes will be untyped
     */

    public void setAllNodesUntyped(boolean allUntyped) {
        allNodesUntyped = allUntyped;
    }

    /**
     * Determine whether all nodes encountered within this query or transformation are guaranteed to be
     * untyped
     *
     * @return true if it is known that all nodes will be untyped
     */

    public boolean areAllNodesUntyped() {
        return allNodesUntyped;
    }

    /**
     * Create a document projector for a given path map. Document projection is available only
     * in Saxon-SA, so the Saxon-B version of this method throws an exception
     *
     * @param map the path map used to control document projection
     * @return a push filter that implements document projection
     * @throws UnsupportedOperationException if this is not a schema-aware configuration, or
     *                                       if no Saxon-SA license is available
     */

    public ProxyReceiver makeDocumentProjector(PathMap.PathMapRoot map) {
        throw new UnsupportedOperationException("Document projection requires a schema-aware Configuration");
    }

    /**
     * Determine whether source documents (supplied as a StreamSource or SAXSource)
     * should be subjected to schema validation
     *
     * @return the schema validation mode previously set using setSchemaValidationMode(),
     *         or the default mode {@link Validation#STRIP} otherwise.
     */

    public int getSchemaValidationMode() {
        return schemaValidationMode;
    }

    /**
     * Indicate whether source documents (supplied as a StreamSource or SAXSource)
     * should be subjected to schema validation
     *
     * @param validationMode the validation (or construction) mode to be used for source documents.
     *                       One of {@link Validation#STRIP}, {@link Validation#PRESERVE}, {@link Validation#STRICT},
     *                       {@link Validation#LAX}
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
     *             are to be treated as fatal errors.
     * @since 8.4
     */

    public void setValidationWarnings(boolean warn) {
        validationWarnings = warn;
    }

    /**
     * Determine whether schema validation failures on result documents are to be treated
     * as fatal errors or as warnings.
     *
     * @return true if validation errors are to be treated as warnings (that is, the
     *         validation failure is reported but processing continues as normal); false
     *         if validation errors are fatal.
     * @since 8.4
     */

    public boolean isValidationWarnings() {
        return validationWarnings;
    }

    /**
     * Indicate whether attributes that have a fixed or default value are to be expanded when
     * generating a final result tree. By default (and for conformance with the W3C specifications)
     * it is required that fixed and default values should be expanded. However, there are use cases
     * for example when generating XHTML when this serves no useful purpose and merely bloats the output.
     * <p/>
     * <p>This option can be overridden at the level of a PipelineConfiguration</p>
     *
     * @param expand true if fixed and default values are to be expanded as required by the W3C
     *               specifications; false if this action is to be disabled. Note that this only affects the validation
     *               of final result trees; it is not possible to suppress expansion of fixed or default values on input
     *               documents, as this would make the type annotations on input nodes unsound.
     * @since 9.0
     */

    public void setExpandAttributeDefaults(boolean expand) {
        expandDefaultAttributes = expand;
    }

    /**
     * Determine whether elements and attributes that have a fixed or default value are to be expanded.
     * This option applies both to DTD-defined attribute defaults and to schema-defined defaults for
     * elements and attributes. If an XML parser is used that does not report whether defaults have
     * been used, this option is ignored.
     * <p/>
     * * <p>This option can be overridden at the level of a PipelineConfiguration</p>
     *
     * @return true if elements and attributes that have a fixed or default value are to be expanded,
     *         false if defaults are not to be expanded. The default value is true. Note that the setting "false"
     *         is potentially non-conformant with the W3C specifications.
     * @since 9.0
     */

    public boolean isExpandAttributeDefaults() {
        return expandDefaultAttributes;
    }


    /**
     * Get the target namepool to be used for stylesheets/queries and for source documents.
     *
     * @return the target name pool. If no NamePool has been specified explicitly, the
     *         default NamePool is returned.
     * @since 8.4
     */

    public NamePool getNamePool() {
        return namePool;
    }

    /**
     * Set the NamePool to be used for stylesheets/queries and for source documents.
     * <p/>
     * <p> Using this method allows several Configurations to share the same NamePool. This
     * was the normal default arrangement until Saxon 8.9, which changed the default so
     * that each Configuration uses its own NamePool.</p>
     * <p/>
     * <p>Sharing a NamePool creates a potential bottleneck, since changes to the namepool are
     * synchronized.</p>
     *
     * @param targetNamePool The NamePool to be used.
     * @since 8.4
     */

    public void setNamePool(NamePool targetNamePool) {
        namePool = targetNamePool;
    }

    /**
     * Get the TypeHierarchy: a cache holding type information
     *
     * @return the type hierarchy cache
     */

    public final TypeHierarchy getTypeHierarchy() {
        if (typeHierarchy == null) {
            typeHierarchy = new TypeHierarchy(this);
        }
        return typeHierarchy;
    }

    /**
     * Get the document number allocator.
     * <p/>
     * The document number allocator is used to allocate a unique number to each document built under this
     * configuration. The document number forms the basis of all tests for node identity; it is therefore essential
     * that when two documents are accessed in the same XPath expression, they have distinct document numbers.
     * Normally this is ensured by building them under the same Configuration. Using this method together with
     * {@link #setDocumentNumberAllocator}, however, it is possible to have two different Configurations that share
     * a single DocumentNumberAllocator
     *
     * @return the current DocumentNumberAllocator
     * @since 9.0
     */

    public DocumentNumberAllocator getDocumentNumberAllocator() {
        return documentNumberAllocator;
    }

    /**
     * Set the document number allocator.
     * <p/>
     * The document number allocator is used to allocate a unique number to each document built under this
     * configuration. The document number forms the basis of all tests for node identity; it is therefore essential
     * that when two documents are accessed in the same XPath expression, they have distinct document numbers.
     * Normally this is ensured by building them under the same Configuration. Using this method together with
     * {@link #getDocumentNumberAllocator}, however, it is possible to have two different Configurations that share
     * a single DocumentNumberAllocator</p>
     * <p>This method is for advanced applications only. Misuse of the method can cause problems with node identity.
     * The method should not be used except while initializing a Configuration, and it should be used only to
     * arrange for two different configurations to share the same DocumentNumberAllocators. In this case they
     * should also share the same NamePool.
     *
     * @param allocator the DocumentNumberAllocator to be used
     * @since 9.0
     */

    public void setDocumentNumberAllocator(DocumentNumberAllocator allocator) {
        documentNumberAllocator = allocator;
    }

    /**
     * Determine whether two Configurations are compatible. When queries, transformations, and path expressions
     * are run, all the Configurations used to build the documents and to compile the queries and stylesheets
     * must be compatible. Two Configurations are compatible if they share the same NamePool and the same
     * DocumentNumberAllocator.
     *
     * @param other the other Configuration to be compared with this one
     * @return true if the two configurations are compatible
     */

    public boolean isCompatible(Configuration other) {
        return namePool == other.namePool && documentNumberAllocator == other.documentNumberAllocator;
    }

    /**
     * Get the global document pool. This is used for documents preloaded during query or stylesheet
     * compilation. The user application can preload documents into the global pool, where they will be found
     * if any query or stylesheet requests the specified document using the doc() or document() function.
     *
     * @return the global document pool
     */

    public DocumentPool getGlobalDocumentPool() {
        return globalDocumentPool;
    }

    /**
     * Determine whether whitespace-only text nodes are to be stripped unconditionally
     * from source documents.
     *
     * @return true if all whitespace-only text nodes are stripped.
     * @since 8.4
     */

    public boolean isStripsAllWhiteSpace() {
        return stripsWhiteSpace == Whitespace.ALL;
    }

    /**
     * Determine whether whitespace-only text nodes are to be stripped unconditionally
     * from source documents.
     *
     * @param stripsAllWhiteSpace if all whitespace-only text nodes are to be stripped.
     * @since 8.4
     */

    public void setStripsAllWhiteSpace(boolean stripsAllWhiteSpace) {
        if (stripsAllWhiteSpace) {
            stripsWhiteSpace = Whitespace.ALL;
        }
    }

    /**
     * Set which kinds of whitespace-only text node should be stripped.
     *
     * @param kind the kind of whitespace-only text node that should be stripped when building
     *             a source tree. One of {@link Whitespace#NONE} (none), {@link Whitespace#ALL} (all),
     *             or {@link Whitespace#IGNORABLE} (element-content whitespace as defined in a DTD or schema)
     */

    public void setStripsWhiteSpace(int kind) {
        stripsWhiteSpace = kind;
    }

    /**
     * Set which kinds of whitespace-only text node should be stripped.
     *
     * @return kind the kind of whitespace-only text node that should be stripped when building
     *         a source tree. One of {@link Whitespace#NONE} (none), {@link Whitespace#ALL} (all),
     *         or {@link Whitespace#IGNORABLE} (element-content whitespace as defined in a DTD or schema)
     */

    public int getStripsWhiteSpace() {
        return stripsWhiteSpace;
    }


    /**
     * Get a parser for source documents. The parser is allocated from a pool if any are available
     * from the pool: the client should ideally return the parser to the pool after use, so that it
     * can be reused.
     * <p/>
     * This method is intended primarily for internal use.
     *
     * @return a parser, in which the namespace properties must be set as follows:
     *         namespaces=true; namespace-prefixes=false. The DTD validation feature of the parser will be set
     *         on or off depending on the {@link #setValidation(boolean)} setting.
     */

    public synchronized XMLReader getSourceParser() throws TransformerFactoryConfigurationError {
        if (sourceParserPool == null) {
            sourceParserPool = new ArrayList(10);
        }
        if (!sourceParserPool.isEmpty()) {
            int n = sourceParserPool.size() - 1;
            XMLReader parser = (XMLReader)sourceParserPool.get(n);
            sourceParserPool.remove(n);
            return parser;
        }
        XMLReader parser;
        if (getSourceParserClass() != null) {
            parser = makeParser(getSourceParserClass());
        } else {
            parser = loadParser();
        }
        try {
            Sender.configureParser(parser);
        } catch (XPathException err) {
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
     *
     * @param parser The parser: the caller must not supply a parser that was obtained by any
     *               mechanism other than calling the getSourceParser() method.
     */

    public synchronized void reuseSourceParser(XMLReader parser) {
        if (sourceParserPool == null) {
            sourceParserPool = new ArrayList(10);
        }
        try {
            // give things back to the garbage collecter
            parser.setContentHandler(null);
            parser.setEntityResolver(null);
            parser.setDTDHandler(null);
            parser.setErrorHandler(null);
            // Unfortunately setting the lexical handler to null doesn't work on Xerces, because
            // it tests "value instanceof LexicalHandler". So we set it to a lexical handler that
            // holds no references
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", dummyLexicalHandler);
        } catch (Exception err) {
            //
        }
        sourceParserPool.add(parser);
    }

    /**
     * Get a parser by instantiating the SAXParserFactory
     *
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
     * <p/>
     * This method is intended for internal use only.
     *
     * @return an XML parser (a SAX2 parser) that can be used for stylesheets and schema documents
     */

    public synchronized XMLReader getStyleParser() throws TransformerFactoryConfigurationError {
        if (styleParserPool == null) {
            styleParserPool = new ArrayList(10);
        }
        if (!styleParserPool.isEmpty()) {
            int n = styleParserPool.size() - 1;
            XMLReader parser = (XMLReader)styleParserPool.get(n);
            styleParserPool.remove(n);
            return parser;
        }
        XMLReader parser;
        if (getStyleParserClass() != null) {
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

    private static LexicalHandler dummyLexicalHandler = new DefaultHandler2();

    /**
     * Return a stylesheet (or schema) parser to the pool, for reuse
     *
     * @param parser The parser: the caller must not supply a parser that was obtained by any
     *               mechanism other than calling the getStyleParser() method.
     */

    public synchronized void reuseStyleParser(XMLReader parser) {
        if (styleParserPool == null) {
            styleParserPool = new ArrayList(10);
        }
        try {
            // give things back to the garbage collecter
            parser.setContentHandler(null);
            parser.setEntityResolver(null);
            parser.setDTDHandler(null);
            parser.setErrorHandler(null);
            // Unfortunately setting the lexical handler to null doesn't work on Xerces, because
            // it tests "value instanceof LexicalHandler". Instead we set the lexical handler to one
            // that holds no references
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", dummyLexicalHandler);
        } catch (Exception err) {
            //
        }        
        styleParserPool.add(parser);
    }

    /**
     * Simple interface to load a schema document
     *
     * @param absoluteURI the absolute URI of the location of the schema document
     */

    public void loadSchema(String absoluteURI) throws SchemaException {
        readSchema(makePipelineConfiguration(), "", absoluteURI, null);
    }

    /**
     * Read a schema from a given schema location
     * <p/>
     * This method is intended for internal use.
     *
     * @param pipe           the PipelineConfiguration
     * @param baseURI        the base URI of the instruction requesting the reading of the schema
     * @param schemaLocation the location of the schema to be read
     * @param expected       The expected targetNamespace of the schema being read.
     * @return the target namespace of the schema; null if there is no expectation
     * @throws UnsupportedOperationException when called in the non-schema-aware version of the product
     */

    public String readSchema(PipelineConfiguration pipe, String baseURI, String schemaLocation, String expected)
            throws SchemaException {
        needSchemaAwareVersion();
        return null;
    }

    /**
     * Read schemas from a list of schema locations.
     * <p/>
     * This method is intended for internal use.
     *
     * @param pipe            the pipeline configuration
     * @param baseURI         the base URI against which the schema locations are to be resolved
     * @param schemaLocations the relative URIs specified as schema locations
     * @param expected        the namespace URI which is expected as the target namespace of the loaded schema
     */

    public void readMultipleSchemas(PipelineConfiguration pipe, String baseURI, Collection schemaLocations, String expected)
            throws SchemaException {
        needSchemaAwareVersion();
    }


    /**
     * Read an inline schema from a stylesheet.
     * <p/>
     * This method is intended for internal use.
     *
     * @param root          the xs:schema element in the stylesheet
     * @param expected      the target namespace expected; null if there is no
     *                      expectation.
     * @param errorListener The destination for error messages. May be null, in which case
     *                      the errorListener registered with this Configuration is used.
     * @return the actual target namespace of the schema
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
     *
     * @param schemaSource the JAXP Source object identifying the schema document to be loaded
     * @throws SchemaException               if the schema cannot be read or parsed or if it is invalid
     * @throws UnsupportedOperationException if the configuration is not schema-aware
     * @since 8.4
     */

    public void addSchemaSource(Source schemaSource) throws SchemaException {
        addSchemaSource(schemaSource, getErrorListener());
    }

    /**
     * Load a schema, which will be available for use by all subsequent operations using
     * this SchemaAwareConfiguration.
     *
     * @param schemaSource  the JAXP Source object identifying the schema document to be loaded
     * @param errorListener the ErrorListener to be notified of any errors in the schema.
     * @throws SchemaException if the schema cannot be read or parsed or if it is invalid
     */

    public void addSchemaSource(Source schemaSource, ErrorListener errorListener) throws SchemaException {
        needSchemaAwareVersion();
    }

    /**
     * Determine whether the Configuration contains a cached schema for a given target namespace
     *
     * @param targetNamespace the target namespace of the schema being sought (supply "" for the
     *                        unnamed namespace)
     * @return true if the schema for this namespace is available, false if not.
     */

    public boolean isSchemaAvailable(String targetNamespace) {
        return false;
    }

    /**
     * Get the set of namespaces of imported schemas
     *
     * @return a Set whose members are the namespaces of all schemas in the schema cache, as
     *         String objects
     */

    public Set getImportedNamespaces() {
        return Collections.EMPTY_SET;
    }

    /**
     * Mark a schema namespace as being sealed. This is done when components from this namespace
     * are first used for validating a source document or compiling a source document or query. Once
     * a namespace has been sealed, it is not permitted to change the schema components in that namespace
     * by redefining them, deriving new types by extension, or adding to their substitution groups.
     *
     * @param namespace the namespace URI of the components to be sealed
     */

    public void sealNamespace(String namespace) {
        //
    }

    /**
     * Get the set of complex types that have been defined as extensions of a given type.
     * Note that we do not seal the schema namespace, so this list is not necessarily final; we must
     * assume that new extensions of built-in simple types can be added at any time
     * @param type the type whose extensions are required
     */

    public Iterator getExtensionsOfType(SchemaType type) {
        return Collections.EMPTY_SET.iterator();
    }

    /**
     * Import a precompiled Schema Component Model from a given Source. The schema components derived from this schema
     * document are added to the cache of schema components maintained by this SchemaManager
     *
     * @param source the XML file containing the schema component model, as generated by a previous call on
     *               {@link #exportComponents}
     */

    public void importComponents(Source source) throws XPathException {
        needSchemaAwareVersion();
    }

    /**
     * Export a precompiled Schema Component Model containing all the components (except built-in components)
     * that have been loaded into this Processor.
     *
     * @param out the destination to recieve the precompiled Schema Component Model in the form of an
     *            XML document
     */

    public void exportComponents(Receiver out) throws XPathException {
        needSchemaAwareVersion();
    }


    /**
     * Get a global element declaration.
     * <p/>
     * This method is intended for internal use.
     *
     * @param fingerprint the NamePool fingerprint of the name of the required
     *                    element declaration
     * @return the element declaration whose name matches the given
     *         fingerprint, or null if no element declaration with this name has
     *         been registered.
     */

    public SchemaDeclaration getElementDeclaration(int fingerprint) {
        return null;
    }

    /**
     * Get a global attribute declaration.
     * <p/>
     * This method is intended for internal use
     *
     * @param fingerprint the namepool fingerprint of the required attribute
     *                    declaration
     * @return the attribute declaration whose name matches the given
     *         fingerprint, or null if no element declaration with this name has
     *         been registered.
     */

    public SchemaDeclaration getAttributeDeclaration(int fingerprint) {
        return null;
    }

    /**
     * Get the top-level schema type definition with a given fingerprint.
     * <p/>
     * This method is intended for internal use and for use by advanced
     * applications. (The SchemaType object returned cannot yet be considered
     * a stable API, and may be superseded when a JAXP API for schema information
     * is defined.)
     *
     * @param fingerprint the fingerprint of the schema type
     * @return the schema type , or null if there is none
     *         with this name.
     */

    public SchemaType getSchemaType(int fingerprint) {
        if (fingerprint < 1023) {
            return BuiltInType.getSchemaType(fingerprint);
        }
        if (getNamePool().getURI(fingerprint).equals(NamespaceConstant.JAVA_TYPE)) {
            try {
                Class namedClass = dynamicLoader.getClass(getNamePool().getLocalName(fingerprint), false, null);
                if (namedClass == null) {
                    return null;
                }
                return new ExternalObjectType(namedClass, this);
            } catch (XPathException err) {
                return null;
            }
        }
        return null;
    }

    /**
     * Check that a type is validly derived from another type, following the rules for the Schema Component
     * Constraint "Is Type Derivation OK (Simple)" (3.14.6) or "Is Type Derivation OK (Complex)" (3.4.6) as
     * appropriate.
     *
     * @param derived the derived type
     * @param base    the base type; the algorithm tests whether derivation from this type is permitted
     * @param block   the derivations that are blocked by the relevant element declaration
     * @throws SchemaException if the derivation is not allowed
     */

    public void checkTypeDerivationIsOK(SchemaType derived, SchemaType base, int block)
            throws SchemaException, ValidationException {
        // no action. Although the method can be used to check built-in types, it is never
        // needed in the non-schema-aware product
    }

    /**
     * Get a document-level validator to add to a Receiver pipeline.
     * <p/>
     * This method is intended for internal use.
     *
     * @param receiver            The receiver to which events should be sent after validation
     * @param systemId            the base URI of the document being validated
     * @param validationMode      for example Validation.STRICT or Validation.STRIP. The integer may
     *                            also have the bit Validation.VALIDATE_OUTPUT set, indicating that the strean being validated
     *                            is to be treated as a final output stream (which means multiple errors can be reported)
     * @param stripSpace          options for space stripping
     * @param schemaType          The type against which the outermost element of the document must be validated
     *                            (null if there is no constraint)
     * @param topLevelElementName the namepool name code of the required top-level element in the instance
     *                            document, or -1 if there is no specific element required
     * @return A Receiver to which events can be sent for validation
     */

    public Receiver getDocumentValidator(Receiver receiver,
                                         String systemId,
                                         int validationMode,
                                         int stripSpace,
                                         SchemaType schemaType,
                                         int topLevelElementName) {
        // non-schema-aware version
        return receiver;
    }

    /**
     * Get a Receiver that can be used to validate an element, and that passes the validated
     * element on to a target receiver. If validation is not supported, the returned receiver
     * will be the target receiver.
     * <p/>
     * This method is intended for internal use.
     *
     * @param receiver   the target receiver tp receive the validated element
     * @param nameCode   the nameCode of the element to be validated. This must correspond to the
     *                   name of an element declaration in a loaded schema
     * @param locationId current location in the stylesheet or query
     * @param schemaType the schema type (typically a complex type) against which the element is to
     *                   be validated
     * @param validation The validation mode, for example Validation.STRICT or Validation.LAX
     * @return The target receiver, indicating that with this configuration, no validation
     *         is performed.
     */
    public SequenceReceiver getElementValidator(SequenceReceiver receiver,
                                                int nameCode,
                                                int locationId,
                                                SchemaType schemaType,
                                                int validation)
            throws XPathException {
        return receiver;
    }

    /**
     * Validate an attribute value.
     * <p/>
     * This method is intended for internal use.
     *
     * @param nameCode   the name of the attribute
     * @param value      the value of the attribute as a string
     * @param validation STRICT or LAX
     * @return the type annotation to apply to the attribute node
     * @throws ValidationException if the value is invalid
     */

    public int validateAttribute(int nameCode, CharSequence value, int validation)
            throws ValidationException {
        return StandardNames.XS_UNTYPED_ATOMIC;
    }

    /**
     * Add to a pipeline a receiver that strips all type annotations. This
     * has a null implementation in the Saxon-B product, because type annotations
     * can never arise.
     * <p/>
     * This method is intended for internal use.
     *
     * @param destination the Receiver that events will be written to after whitespace stripping
     * @return the Receiver to which events should be sent for stripping
     */

    public Receiver getAnnotationStripper(Receiver destination) {
        return destination;
    }

    /**
     * Create a new SAX XMLReader object using the class name provided. <br>
     * <p/>
     * The named class must exist and must implement the
     * org.xml.sax.XMLReader or Parser interface. <br>
     * <p/>
     * This method returns an instance of the parser named.
     * <p/>
     * This method is intended for internal use.
     *
     * @param className A string containing the name of the
     *                  SAX parser class, for example "com.microstar.sax.LarkDriver"
     * @return an instance of the Parser class named, or null if it is not
     *         loadable or is not a Parser.
     */
    public XMLReader makeParser(String className)
            throws TransformerFactoryConfigurationError {
        Object obj;
        try {
            obj = dynamicLoader.getInstance(className, null);
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
     * <p/>
     * This method is intended for internal use.
     *
     * @param lang the language code
     * @return the Java locale
     */

    public static Locale getLocale(String lang) {
        int hyphen = lang.indexOf("-");
        String language, country;
        if (hyphen < 1) {
            language = lang;
            country = "";
        } else {
            language = lang.substring(1, hyphen);
            country = lang.substring(hyphen + 1);
        }
        return new Locale(language, country);
    }

    /**
     * Set the debugger to be used.
     * <p/>
     * This method is provided for advanced users only, and is subject to change.
     *
     * @param debugger the debugger to be used.
     */

    public void setDebugger(Debugger debugger) {
        this.debugger = debugger;
    }

    /**
     * Get the debugger in use. This will be null if no debugger has been registered.
     * <p/>
     * This method is provided for advanced users only, and is subject to change.
     *
     * @return the debugger in use, or null if none is in use
     */

    public Debugger getDebugger() {
        return debugger;
    }

    /**
     * Factory method to create a SlotManager.
     * <p/>
     * This method is provided for advanced users only, and is subject to change.
     *
     * @return a SlotManager (which is a skeletal stack frame representing the mapping of variable
     *         names to slots on the stack frame)
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
     * <p/>
     * This method is intended for internal use only.
     *
     * @return the optimizer used in this configuration
     */

    public Optimizer getOptimizer() {
        if (optimizer == null) {
            optimizer = new Optimizer(this);
        }
        return optimizer;
    }


    /**
     * Load a named collator class and check it is OK.
     * <p/>
     * This method is intended for internal use only.
     *
     * @param className the name of the collator class
     * @return a StringCollator to implement a collation
     */

    public StringCollator makeCollator(String className) throws XPathException {
        Object handler = dynamicLoader.getInstance(className, null);

        if (handler instanceof StringCollator) {
            return (StringCollator)handler;
        } else {
            throw new XPathException("Failed to load collation class " + className +
                    ": it is not an instance of org.orbeon.saxon.sort.StringCollator");
        }

    }

    /**
     * Set lazy construction mode on or off. In lazy construction mode, element constructors
     * are not evaluated until the content of the tree is required. Lazy construction mode
     * is currently experimental and is therefore off by default.
     *
     * @param lazy true to switch lazy construction mode on, false to switch it off.
     */

    public void setLazyConstructionMode(boolean lazy) {
        lazyConstructionMode = lazy;
    }

    /**
     * Determine whether lazy construction mode is on or off. In lazy construction mode, element constructors
     * are not evaluated until the content of the tree is required. Lazy construction mode
     * is currently experimental and is therefore off by default.
     *
     * @return true if lazy construction mode is enabled
     */

    public boolean isLazyConstructionMode() {
        return lazyConstructionMode;
    }

    /**
     * Register the standard Saxon-supplied object models.
     * <p/>
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
        String[] nodes = {"org.orbeon.saxon.dom.NodeOverNodeInfo",
                "org.w3c.dom.Node",
                "org.jdom.Element",
                "nu.xom.Node",
                "org.dom4j.Element"};

        sharedExternalObjectModels = new ArrayList(4);
        for (int i = 0; i < models.length; i++) {
            try {
                dynamicLoader.getClass(nodes[i], false, null);
                // designed to trigger an exception if the class can't be loaded
                ExternalObjectModel model = (ExternalObjectModel)dynamicLoader.getInstance(models[i], null);
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
     *
     * @param model The external object model.
     *              This can either be one of the system-supplied external
     *              object models for JDOM, XOM, or DOM, or a user-supplied external object model.
     *              <p/>
     *              This method is intended for advanced users only, and is subject to change.
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
     * Get the external object model with a given URI, if registered
     * @param uri the identifying URI of the required external object model
     * @return the requested external object model if available, or null otherwise
     */

    public ExternalObjectModel getExternalObjectModel(String uri) {
        for (int m=0; m<externalObjectModels.size(); m++) {
            ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
            if (model.getIdentifyingURI().equals(uri)) {
                return model;
            }
        }
        return null;
    }

    /**
     * Get all the registered external object models.
     * <p/>
     * This method is intended for internal use only.
     *
     * @return a list of external object models supported. The members of the list are of
     *         type {@link ExternalObjectModel}
     */

    public List getExternalObjectModels() {
        return externalObjectModels;
    }

    /**
     * Get a NodeInfo corresponding to a DOM or other external Node,
     * either by wrapping or unwrapping the external Node.
     * <p/>
     * This method is intended for internal use.
     *
     * @param source A Source representing the wrapped or unwrapped external Node. This will typically
     *               be a DOMSource, but it may be a similar Source recognized by some other registered external
     *               object model.
     * @return If the Source is a DOMSource and the underlying node is a wrapper around a Saxon NodeInfo,
     *         returns the wrapped Saxon NodeInfo. If the Source is a DOMSource and the undelying node is not such a wrapper,
     *         returns a new Saxon NodeInfo that wraps the DOM Node. If the Source is any other kind of source, it
     *         is offered to each registered external object model for similar treatment. The result is the
     *         NodeInfo object obtained by wrapping or unwrapping the supplied external node.
     * @throws IllegalArgumentException if the source object is not of a recognized class. This method does
     *                                  <emph>not</emph> call the registered {@link SourceResolver to resolve the Source}.
     */

    public NodeInfo unravel(Source source) {
        List externalObjectModels = getExternalObjectModels();
        for (int m = 0; m < externalObjectModels.size(); m++) {
            ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
            NodeInfo node = model.unravel(source, this);
            if (node != null) {
                if (node.getConfiguration() != this) {
                    throw new IllegalArgumentException("Externally supplied Node belongs to the wrong Configuration");
                }
                return node;
            }
        }
        if (source instanceof NodeInfo) {
            if (((NodeInfo)source).getConfiguration() != this) {
                throw new IllegalArgumentException("Externally supplied NodeInfo belongs to the wrong Configuration");
            }
            return (NodeInfo)source;
        }
        if (source instanceof DOMSource) {
            throw new IllegalArgumentException("When a DOMSource is used, saxon9-dom.jar must be on the classpath");
        } else {
            throw new IllegalArgumentException("A source of class " +
                    source.getClass() + " is not recognized by any registered object model");
        }
    }

    /**
     * Set the level of DOM interface to be used
     *
     * @param level the DOM level. Must be 2 or 3. By default Saxon assumes that DOM level 3 is available;
     *              this parameter can be set to the value 2 to indicate that Saxon should not use methods unless they
     *              are available in DOM level 2.
     */

    public void setDOMLevel(int level) {
        if (!(level == 2 || level == 3)) {
            throw new IllegalArgumentException("DOM Level must be 2 or 3");
        }
        domLevel = level;
    }

    /**
     * Get the level of DOM interface to be used
     *
     * @return the DOM level. Always 2 or 3.
     */

    public int getDOMLevel() {
        return domLevel;
    }

    /**
     * Get a new QueryParser
     *
     * @param updating indicates whether or not XQuery update syntax may be used. Note that XQuery Update
     *                 is supported only in Saxon-SA
     * @return the QueryParser
     * @throws UnsupportedOperationException if a parser that supports update syntax is requested on Saxon-B
     */

    public QueryParser newQueryParser(boolean updating) {
        if (updating) {
            throw new UnsupportedOperationException("XQuery Update is supported only in Saxon-SA");
        } else {
            return new QueryParser();
        }
    }

    /**
     * Get a new Pending Update List
     *
     * @return the new Pending Update List
     * @throws UnsupportedOperationException if called when using Saxon-B
     */

    public PendingUpdateList newPendingUpdateList() {
        throw new UnsupportedOperationException("XQuery update is supported only in Saxon-SA");
    }

    /**
     * Make a PipelineConfiguration from the properties of this Configuration
     *
     * @return a new PipelineConfiguration
     * @since 8.4
     */

    public PipelineConfiguration makePipelineConfiguration() {
        PipelineConfiguration pipe = new PipelineConfiguration();
        pipe.setConfiguration(this);
        pipe.setErrorListener(getErrorListener());
        pipe.setURIResolver(getURIResolver());
        pipe.setSchemaURIResolver(getSchemaURIResolver());
        pipe.setHostLanguage(getHostLanguage());
        pipe.setExpandAttributeDefaults(isExpandAttributeDefaults());
        pipe.setUseXsiSchemaLocation(useXsiSchemaLocation);
        return pipe;
    }

    /**
     * Get the configuration, given the context. This is provided as a static method to make it accessible
     * as an extension function.
     *
     * @param context the XPath dynamic context
     * @return the Saxon Configuration for a given XPath dynamic context
     */

    public static Configuration getConfiguration(XPathContext context) {
        return context.getConfiguration();
    }

    /**
     * Supply a SourceResolver. This is used for handling unknown implementations of the
     * {@link javax.xml.transform.Source} interface: a user-supplied SourceResolver can handle
     * such Source objects and translate them to a kind of Source that Saxon understands.
     *
     * @param resolver the source resolver.
     */

    public void setSourceResolver(SourceResolver resolver) {
        sourceResolver = resolver;
    }

    /**
     * Get the current SourceResolver. If none has been supplied, a system-defined SourceResolver
     * is returned.
     *
     * @return the current SourceResolver
     */

    public SourceResolver getSourceResolver() {
        return sourceResolver;
    }

    /**
     * Implement the SourceResolver interface
     */

    public Source resolveSource(Source source, Configuration config) throws XPathException {
        if (source instanceof AugmentedSource) {
            return source;
        }
        if (source instanceof StreamSource) {
            return source;
        }
        if (source instanceof SAXSource) {
            return source;
        }
        if (source instanceof DOMSource) {
            return source;
        }
        if (source instanceof NodeInfo) {
            return source;
        }
        if (source instanceof PullProvider) {
            return source;
        }
        if (source instanceof PullSource) {
            return source;
        }
        if (source instanceof PullEventSource) {
            return source;
        }
        return null;
    }

    /**
     * Build a document tree, using options set on this Configuration and on the supplied source
     * object. Options set on the source object override options set in the Configuration. The Source
     * object must be one of the kinds of source recognized by Saxon, or a source that can be resolved
     * using the registered SourceResolver.
     *
     * @param source the Source to be used. This may be an {@link AugmentedSource}, allowing options
     *               to be specified for the way in which this document will be built.
     *               <p>A new tree will be built, using either the Tiny Tree or the Linked Tree implementation,
     *               except under the following circumstances:</p>
     *               <ul>
     *               <li>The supplied object is a DocumentInfo. In this case a new tree will
     *               be built only if validation or whitespace stripping has been requested in the Configuration;
     *               otherwise the DocumentInfo will be returned unchanged.</li>
     *               <li>The supplied object is an AugmentedSource wrapping a DocumentInfo. In this case a new tree will
     *               be built if wrap=no has been specified, if validation has been requested, or if whitespace stripping
     *               has been requested, either in the AugmentedSource or in the Configuration.</li>
     *               <li>The supplied object is a DOMSource. In this case a new tree will
     *               be built if validation or whitespace stripping has been requested in the Configuration,
     *               or if the DOM Node is not a Document Node; in other cases the Document node of the DOMSource will be wrapped
     *               in a Saxon {@link org.orbeon.saxon.dom.DocumentWrapper}, except in the case where the DOM Document node
     *               is a {@link org.orbeon.saxon.dom.DocumentOverNodeInfo}, in which case the DocumentInfo that it wraps is
     *               returned.</li>
     *               <li>The supplied object is an AugmentedSource wrapping a DOMSource. In this case a new tree will
     *               be built if wrap=no has been specified, if validation has been requested, or if whitespace stripping
     *               has been requested, either in the AugmentedSource or in the Configuration.</li>
     *               </ul>
     *               <p>The choice between a tiny tree and a linked tree is determined first be the properties of the
     *               AugmentedSource if that's what is supplied; otherwise by the properties of this Configuration.
     * @return the document node of the constructed or wrapped document
     * @throws XPathException if any errors occur during document parsing or validation. Detailed
     *                        errors occurring during schema validation will be written to the ErrorListener associated
     *                        with the AugmentedSource, if supplied, or with the Configuration otherwise.
     * @since 8.9. Modified in 9.0 to avoid copying a supplied document where this is not
     *        necessary.
     */

    public DocumentInfo buildDocument(Source source) throws XPathException {

        if (source == null) {
            throw new NullPointerException("source");
        }

        // Resolve user-defined implementations of Source
        source = resolveSource(source, this);
        if (source == null) {
            throw new XPathException("Unknown source class");
        }

        boolean mustCopy = schemaValidationMode != Validation.PRESERVE
                || stripsWhiteSpace == Whitespace.ALL;

        if (!mustCopy) {

            // If the source is a DocumentInfo, return it unchanged
            if (source instanceof DocumentInfo) {
                return (DocumentInfo)source;
            }

            // Handle an AugmentedSource wrapping a DOMSource or DocumentInfo
            if (source instanceof AugmentedSource) {
                AugmentedSource asource = (AugmentedSource)source;
                Source underSource = asource.getContainedSource();
                if (underSource instanceof DocumentInfo || underSource instanceof DOMSource) {
                    boolean wrap = asource.getWrapDocument() == null || asource.getWrapDocument().booleanValue();
                    if (asource.getSchemaValidation() != Validation.STRIP) {
                        wrap = false;
                    } else if (asource.getStripSpace() != Whitespace.NONE) {
                        wrap = false;
                    }
                    if (wrap) {
                        NodeInfo node = unravel(underSource);
                        if (node instanceof DocumentInfo) {
                            return (DocumentInfo)node;
                        } else {
                            source = node;
                        }
                    } else {
                        source = underSource;
                    }
                }
            }

            // Handle a DOMSource
            if (source instanceof DOMSource) {
                NodeInfo node = unravel(source);
                if (node instanceof DocumentInfo) {
                    return (DocumentInfo)node;
                } else {
                    source = node;
                }
            }
        }
      
        // Create a pipeline configuration
        PipelineConfiguration pipe = makePipelineConfiguration();

        // Create an appropriate Builder

        Builder b;
        int treeModel = Builder.UNSPECIFIED_TREE_MODEL;
        if (source instanceof AugmentedSource) {
            treeModel = ((AugmentedSource)source).getTreeModel();
        }

        if (treeModel == Builder.UNSPECIFIED_TREE_MODEL) {
            treeModel = this.treeModel;
        }
        if (treeModel == Builder.TINY_TREE) {
            b = new TinyBuilder();
        } else {
            b = new TreeBuilder();
        }

        // Set builder properties

        b.setPipelineConfiguration(pipe);
        b.setTiming(isTiming());

        // Decide whether line numbering is in use

        boolean lineNumbering = this.lineNumbering;
        if (source instanceof AugmentedSource && ((AugmentedSource)source).isLineNumberingSet()) {
            lineNumbering = ((AugmentedSource)source).isLineNumbering();
        }
        b.setLineNumbering(lineNumbering);

        new Sender(pipe).send(source, b);

        // Get the constructed document

        DocumentInfo newdoc = (DocumentInfo)b.getCurrentRoot();

        // Reset the builder, detaching it from the constructed document

        b.reset();

        // If requested, close the input stream

        if (source instanceof AugmentedSource && ((AugmentedSource)source).isPleaseCloseAfterUse()) {
            ((AugmentedSource)source).close();
        }

        // Return the constructed document

        return newdoc;
    }


    /**
     * Load a named output emitter or SAX2 ContentHandler and check it is OK.
     *
     * @param clarkName  the QName of the user-supplied ContentHandler (requested as a prefixed
     *                   value of the method attribute in xsl:output, or anywhere that serialization parameters
     *                   are allowed), encoded in Clark format as {uri}local
     * @param controller the Controller. Allows a local class loader to be used.
     * @return a Receiver (despite the name, it is not required to be an Emitter)
     */

    public Receiver makeEmitter(String clarkName, Controller controller) throws XPathException {
        int brace = clarkName.indexOf('}');
        String localName = clarkName.substring(brace + 1);
        int colon = localName.indexOf(':');
        String className = localName.substring(colon + 1);
        Object handler;
        try {
            handler = dynamicLoader.getInstance(className, controller.getClassLoader());
        } catch (XPathException e) {
            throw new XPathException("Cannot load user-supplied output method " + className,
                    SaxonErrorCode.SXCH0004);
        }

        if (handler instanceof Receiver) {
            return (Receiver)handler;
        } else if (handler instanceof ContentHandler) {
            ContentHandlerProxy emitter = new ContentHandlerProxy();
            emitter.setUnderlyingContentHandler((ContentHandler)handler);
            emitter.setOutputProperties(controller.getOutputProperties());
            return emitter;
        } else {
            throw new XPathException("Failed to load " + className +
                    ": it is neither a Receiver nor a SAX2 ContentHandler");
        }

    }

    /**
     * Set a property of the configuration. This method underpins the setAttribute() method of the
     * TransformerFactory implementation, and is provided
     * to enable setting of Configuration properties using URIs without instantiating a TransformerFactory:
     * specifically, this may be useful when running XQuery, and it is also used by the Validator API
     *
     * @param name  the URI identifying the property to be set. See the class {@link FeatureKeys} for
     *              constants representing the property names that can be set.
     * @param value the value of the property
     * @throws IllegalArgumentException if the property name is not recognized or if the value is not
     * a valid value for the named property
     */

    public void setConfigurationProperty(String name, Object value) {

        if (name.equals(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
            boolean b = requireBoolean("ALLOW_EXTERNAL_FUNCTIONS", value);
            setAllowExternalFunctions(b);

        } else if (name.equals(FeatureKeys.COLLATION_URI_RESOLVER)) {
            if (!(value instanceof CollationURIResolver)) {
                throw new IllegalArgumentException(
                        "COLLATION_URI_RESOLVER value must be an instance of org.orbeon.saxon.sort.CollationURIResolver");
            }
            setCollationURIResolver((CollationURIResolver)value);

        } else if (name.equals(FeatureKeys.COLLATION_URI_RESOLVER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException(
                        "COLLATION_URI_RESOLVER_CLASS must be a String");
            }
            try {
                Object obj = getInstance((String)value, null);
                setCollationURIResolver((CollationURIResolver)obj);
            } catch (XPathException err) {
                throw new IllegalArgumentException(
                        "Cannot instantiate COLLATION_URI_RESOLVER_CLASS. " + err.getMessage());
            } catch (ClassCastException err) {
                throw new IllegalArgumentException(
                        "COLLATION_URI_RESOLVER_CLASS does not implement CollationURIResolver");
            }

        } else if (name.equals(FeatureKeys.COLLECTION_URI_RESOLVER)) {
            if (!(value instanceof CollectionURIResolver)) {
                throw new IllegalArgumentException(
                        "COLLECTION_URI_RESOLVER value must be an instance of org.orbeon.saxon.CollectionURIResolver");
            }
            setCollectionURIResolver((CollectionURIResolver)value);

        } else if (name.equals(FeatureKeys.COLLECTION_URI_RESOLVER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException(
                        "COLLECTION_URI_RESOLVER_CLASS must be a String");
            }
            try {
                Object obj = getInstance((String)value, null);
                setCollectionURIResolver((CollectionURIResolver)obj);
            } catch (XPathException err) {
                throw new IllegalArgumentException(
                        "Cannot instantiate COLLECTION_URI_RESOLVER_CLASS. " + err.getMessage());
            } catch (ClassCastException err) {
                throw new IllegalArgumentException(
                        "COLLECTION_URI_RESOLVER_CLASS does not implement CollectionURIResolver");
            }

        } else if (name.equals(FeatureKeys.COMPILE_WITH_TRACING)) {
            boolean b = requireBoolean("COMPILE_WITH_TRACING", value);
            setCompileWithTracing(b);

        } else if (name.equals(FeatureKeys.DTD_VALIDATION)) {
            boolean b = requireBoolean("DTD_VALIDATION", value);
            setValidation(b);

        } else if (name.equals(FeatureKeys.EXPAND_ATTRIBUTE_DEFAULTS)) {
            boolean b = requireBoolean("EXPAND_ATTRIBUTE_DEFAULTS", value);
            setExpandAttributeDefaults(b);

        } else if (name.equals(FeatureKeys.LINE_NUMBERING)) {
            boolean b = requireBoolean("LINE_NUMBERING", value);
            setLineNumbering(b);

        } else if (name.equals(FeatureKeys.MESSAGE_EMITTER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("MESSAGE_EMITTER_CLASS class must be a String");
            }
            setMessageEmitterClass((String)value);

        } else if (name.equals(FeatureKeys.MODULE_URI_RESOLVER)) {
            if (!(value instanceof ModuleURIResolver)) {
                throw new IllegalArgumentException(
                        "MODULE_URI_RESOLVER value must be an instance of org.orbeon.saxon.query.ModuleURIResolver");
            }
            setModuleURIResolver((ModuleURIResolver)value);

        } else if (name.equals(FeatureKeys.MODULE_URI_RESOLVER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException(
                        "MODULE_URI_RESOLVER_CLASS value must be a string");
            }
            try {
                Object obj = getInstance((String)value, null);
                setModuleURIResolver((ModuleURIResolver)obj);
            } catch (XPathException err) {
                throw new IllegalArgumentException(
                        "Cannot instantiate MODULE_URI_RESOLVER_CLASS. " + err.getMessage());
            } catch (ClassCastException err) {
                throw new IllegalArgumentException(
                        "MODULE_URI_RESOLVER_RESOLVER_CLASS does not implement ModuleURIResolver");
            }

        } else if (name.equals(FeatureKeys.NAME_POOL)) {
            if (!(value instanceof NamePool)) {
                throw new IllegalArgumentException("NAME_POOL value must be an instance of org.orbeon.saxon.om.NamePool");
            }
            setNamePool((NamePool)value);

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER)) {
            if (!(value instanceof OutputURIResolver)) {
                throw new IllegalArgumentException(
                        "OUTPUT_URI_RESOLVER value must be an instance of org.orbeon.saxon.OutputURIResolver");
            }
            setOutputURIResolver((OutputURIResolver)value);

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException(
                        "OUTPUT_URI_RESOLVER_CLASS value must be a string");
            }
            try {
                Object obj = getInstance((String)value, null);
                setOutputURIResolver((OutputURIResolver)obj);
            } catch (XPathException err) {
                throw new IllegalArgumentException(
                        "Cannot instantiate OUTPUT_URI_RESOLVER_CLASS. " + err.getMessage());
            } catch (ClassCastException err) {
                throw new IllegalArgumentException(
                        "OUTPUT_URI_RESOLVER_RESOLVER_CLASS does not implement OutputURIResolver");
            }

        } else if (name.equals(FeatureKeys.PRE_EVALUATE_DOC_FUNCTION)) {
            preEvaluateDocFunction = requireBoolean("PRE_EVALUATE_DOC_FUNCTION", value);

        } else if (name.equals(FeatureKeys.PREFER_JAXP_PARSER)) {
            preferJaxpParser = requireBoolean("PREFER_JAXP_PARSER", value);            

        } else if (name.equals(FeatureKeys.RECOGNIZE_URI_QUERY_PARAMETERS)) {
            if (!(value instanceof Boolean)) {
                throw new IllegalArgumentException("RECOGNIZE_QUERY_URI_PARAMETERS must be a boolean");
            }
            getSystemURIResolver().setRecognizeQueryParameters(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.RECOVERY_POLICY)) {
            if (!(value instanceof Integer)) {
                throw new IllegalArgumentException("RECOVERY_POLICY value must be an Integer");
            }
            setRecoveryPolicy(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.RECOVERY_POLICY_NAME)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("RECOVERY_POLICY_NAME value must be a String");
            }
            int rval;
            if (value.equals("recoverSilently")) {
                rval = RECOVER_SILENTLY;
            } else if (value.equals("recoverWithWarnings")) {
                rval = RECOVER_WITH_WARNINGS;
            } else if (value.equals("doNotRecover")) {
                rval = DO_NOT_RECOVER;
            } else {
                throw new IllegalArgumentException(
                        "Unrecognized value of RECOVERY_POLICY_NAME = '" + value +
                                "': must be 'recoverSilently', 'recoverWithWarnings', or 'doNotRecover'");
            }
            setRecoveryPolicy(rval);

        } else if (name.equals(FeatureKeys.SCHEMA_URI_RESOLVER)) {
            if (!(value instanceof SchemaURIResolver)) {
                throw new IllegalArgumentException(
                        "SCHEMA_URI_RESOLVER value must be an instance of org.orbeon.saxon.type.SchemaURIResolver");
            }
            setSchemaURIResolver((SchemaURIResolver)value);

        } else if (name.equals(FeatureKeys.SCHEMA_URI_RESOLVER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException(
                        "SCHEMA_URI_RESOLVER_CLASS value must be a string");
            }
            try {
                Object obj = getInstance((String)value, null);
                setSchemaURIResolver((SchemaURIResolver)obj);
            } catch (XPathException err) {
                throw new IllegalArgumentException(
                        "Cannot instantiate SCHEMA_URI_RESOLVER_CLASS. " + err.getMessage());
            } catch (ClassCastException err) {
                throw new IllegalArgumentException(
                        "SCHEMA_URI_RESOLVER_RESOLVER_CLASS does not implement SchemaURIResolver");
            }

        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION)) {
            if (!(value instanceof Integer)) {
                throw new IllegalArgumentException("SCHEMA_VALIDATION must be an integer");
            }
            setSchemaValidationMode(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION_MODE)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("SCHEMA_VALIDATION_MODE must be a string");
            }
            setSchemaValidationMode(Validation.getCode((String)value));

        } else if (name.equals(FeatureKeys.SOURCE_PARSER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("SOURCE_PARSER_CLASS class must be a String");
            }
            setSourceParserClass((String)value);

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
            setStripsWhiteSpace(ival);


        } else if (name.equals(FeatureKeys.STYLE_PARSER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("STYLE_PARSER_CLASS class must be a String");
            }
            setStyleParserClass((String)value);

        } else if (name.equals(FeatureKeys.TIMING)) {
            setTiming(requireBoolean("TIMING", value));

        } else if (name.equals(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS)) {
            setTraceExternalFunctions(requireBoolean("TRACE_EXTERNAL_FUNCTIONS", value));

        } else if (name.equals(FeatureKeys.TRACE_OPTIMIZER_DECISIONS)) {
            setOptimizerTracing(requireBoolean("TRACE_OPTIMIZER_DECISIONS", value));

        } else if (name.equals(FeatureKeys.TRACE_LISTENER)) {
            if (!(value instanceof TraceListener)) {
                throw new IllegalArgumentException("TRACE_LISTENER is of wrong class");
            }
            setTraceListener((TraceListener)value);

        } else if (name.equals(FeatureKeys.TRACE_LISTENER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("TRACE_LISTENER_CLASS must be a string");
            }
            setTraceListenerClass((String)value);

        } else if (name.equals(FeatureKeys.TREE_MODEL)) {
            if (!(value instanceof Integer)) {
                throw new IllegalArgumentException("Tree model must be an Integer");
            }
            setTreeModel(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.TREE_MODEL_NAME)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("TREE_MODEL_NAME must be a string");
            }
            if (value.equals("tinyTree")) {
                setTreeModel(Builder.TINY_TREE);
            } else if (value.equals("linkedTree")) {
                setTreeModel(Builder.LINKED_TREE);
            } else {
                throw new IllegalArgumentException(
                        "Unrecognized value TREE_MODEL_NAME = '" + value +
                                "': must be 'linkedTree' or 'tinyTree'");
            }

        } else if (name.equals(FeatureKeys.USE_PI_DISABLE_OUTPUT_ESCAPING)) {
            useDisableOutputEscaping = requireBoolean("USE_PI_DISABLE_OUTPUT_ESCAPING", value);

        } else if (name.equals(FeatureKeys.USE_XSI_SCHEMA_LOCATION)) {
            useXsiSchemaLocation = requireBoolean("USE_XSI_SCHEMA_LOCATION", value);

        } else if (name.equals(FeatureKeys.VALIDATION_WARNINGS)) {
            setValidationWarnings(requireBoolean("VALIDATION_WARNINGS", value));

        } else if (name.equals(FeatureKeys.VERSION_WARNING)) {
            setVersionWarning(requireBoolean("VERSION_WARNING", value));

        } else if (name.equals(FeatureKeys.XINCLUDE)) {
            setXIncludeAware(requireBoolean("XINCLUDE", value));

        } else if (name.equals(FeatureKeys.XML_VERSION) || name.equals("http://saxon.sf.bet/feature/xml-version")) {
            // spelling mistake retained for backwards compatibility with 8.9 and earlier
            if (!(value instanceof String && (value.equals("1.0") || value.equals("1.1")))) {
                throw new IllegalArgumentException(
                        "XML_VERSION value must be \"1.0\" or \"1.1\" as a String");

            }
            setXMLVersion((value.equals("1.0") ? XML10 : XML11));

        } else if (name.equals(FeatureKeys.XSD_VERSION)) {
            if (!(value instanceof String && (value.equals("1.0") || value.equals("1.1")))) {
                throw new IllegalArgumentException(
                        "XSD_VERSION value must be \"1.0\" or \"1.1\" as a String");

            }
            xsdlVersion = ((value.equals("1.0") ? XSD10 : XSD11));

        } else {
            throw new IllegalArgumentException("Unknown attribute " + name);
        }
    }

    private boolean requireBoolean(String propertyName, Object value) {
        if (value instanceof Boolean) {
            return ((Boolean)value).booleanValue();
        } else if (value instanceof String) {
            if ("true".equals(value)) {
                return true;
            } else if ("false".equals(value)) {
                return false;
            } else {
                throw new IllegalArgumentException(propertyName + " must be 'true' or 'false'");
            }
        } else {
            throw new IllegalArgumentException(propertyName + " must be a boolean");
        }
    }

    /**
     * Get a property of the configuration
     *
     * @param name the name of the required property. See the class {@link FeatureKeys} for
     *             constants representing the property names that can be requested.
     * @return the value of the property
     * @throws IllegalArgumentException thrown if the property is not one that Saxon recognizes.
     */

    public Object getConfigurationProperty(String name) {
        if (name.equals(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
            return Boolean.valueOf(isAllowExternalFunctions());

        } else if (name.equals(FeatureKeys.COLLATION_URI_RESOLVER)) {
            return getCollationURIResolver();

        } else if (name.equals(FeatureKeys.COLLATION_URI_RESOLVER_CLASS)) {
            return getCollationURIResolver().getClass().getName();

        } else if (name.equals(FeatureKeys.COLLECTION_URI_RESOLVER)) {
            return getCollectionURIResolver();

        } else if (name.equals(FeatureKeys.COLLECTION_URI_RESOLVER_CLASS)) {
            return getCollectionURIResolver().getClass().getName();            

        } else if (name.equals(FeatureKeys.COMPILE_WITH_TRACING)) {
            return Boolean.valueOf(isCompileWithTracing());

        } else if (name.equals(FeatureKeys.DTD_VALIDATION)) {
            return Boolean.valueOf(isValidation());

        } else if (name.equals(FeatureKeys.EXPAND_ATTRIBUTE_DEFAULTS)) {
            return Boolean.valueOf(isExpandAttributeDefaults());

        } else if (name.equals(FeatureKeys.LINE_NUMBERING)) {
            return Boolean.valueOf(isLineNumbering());

        } else if (name.equals(FeatureKeys.MESSAGE_EMITTER_CLASS)) {
            return getMessageEmitterClass();

        } else if (name.equals(FeatureKeys.MODULE_URI_RESOLVER)) {
            return getModuleURIResolver();

        } else if (name.equals(FeatureKeys.MODULE_URI_RESOLVER_CLASS)) {
            return getModuleURIResolver().getClass().getName();            

        } else if (name.equals(FeatureKeys.NAME_POOL)) {
            return getNamePool();

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER)) {
            return getOutputURIResolver();

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER_CLASS)) {
            return getOutputURIResolver().getClass().getName();

        } else if (name.equals(FeatureKeys.PRE_EVALUATE_DOC_FUNCTION)) {
            return Boolean.valueOf(preEvaluateDocFunction);

        } else if (name.equals(FeatureKeys.PREFER_JAXP_PARSER)) {
            return Boolean.valueOf(preferJaxpParser);

        } else if (name.equals(FeatureKeys.RECOGNIZE_URI_QUERY_PARAMETERS)) {
            return Boolean.valueOf(getSystemURIResolver().queryParametersAreRecognized());

        } else if (name.equals(FeatureKeys.RECOVERY_POLICY)) {
            return new Integer(getRecoveryPolicy());

        } else if (name.equals(FeatureKeys.RECOVERY_POLICY_NAME)) {
            switch (getRecoveryPolicy()) {
                case RECOVER_SILENTLY: return "recoverSilently";
                case RECOVER_WITH_WARNINGS: return "recoverWithWarnings";
                case DO_NOT_RECOVER: return "doNotRecover";
                default: return null;
            }

        } else if (name.equals(FeatureKeys.SCHEMA_URI_RESOLVER)) {
            return getSchemaURIResolver();

        } else if (name.equals(FeatureKeys.SCHEMA_URI_RESOLVER_CLASS)) {
            return getSchemaURIResolver().getClass().getName();

        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION)) {
            return new Integer(getSchemaValidationMode());

        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION_MODE)) {
            return Validation.toString(getSchemaValidationMode());

        } else if (name.equals(FeatureKeys.SOURCE_PARSER_CLASS)) {
            return getSourceParserClass();

        } else if (name.equals(FeatureKeys.STRIP_WHITESPACE)) {
            int s = getStripsWhiteSpace();
            if (s == Whitespace.ALL) {
                return "all";
            } else if (s == Whitespace.IGNORABLE) {
                return "ignorable";
            } else {
                return "none";
            }

        } else if (name.equals(FeatureKeys.STYLE_PARSER_CLASS)) {
            return getStyleParserClass();

        } else if (name.equals(FeatureKeys.TIMING)) {
            return Boolean.valueOf(isTiming());

        } else if (name.equals(FeatureKeys.TRACE_LISTENER)) {
            return traceListener;

        } else if (name.equals(FeatureKeys.TRACE_LISTENER_CLASS)) {
            return traceListenerClass;

        } else if (name.equals(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS)) {
            return Boolean.valueOf(isTraceExternalFunctions());

        } else if (name.equals(FeatureKeys.TRACE_OPTIMIZER_DECISIONS)) {
            return Boolean.valueOf(isOptimizerTracing());

        } else if (name.equals(FeatureKeys.TREE_MODEL)) {
            return new Integer(getTreeModel());

        } else if (name.equals(FeatureKeys.TREE_MODEL_NAME)) {
            return getTreeModel() == Builder.TINY_TREE ? "tinyTree" : "linkedTree";


        } else if (name.equals(FeatureKeys.USE_PI_DISABLE_OUTPUT_ESCAPING)) {
            return Boolean.valueOf(useDisableOutputEscaping);

        } else if (name.equals(FeatureKeys.USE_XSI_SCHEMA_LOCATION)) {
            return Boolean.valueOf(useXsiSchemaLocation);

        } else if (name.equals(FeatureKeys.VALIDATION_WARNINGS)) {
            return Boolean.valueOf(isValidationWarnings());

        } else if (name.equals(FeatureKeys.VERSION_WARNING)) {
            return Boolean.valueOf(isVersionWarning());

        } else if (name.equals(FeatureKeys.XINCLUDE)) {
            return Boolean.valueOf(isXIncludeAware());

        } else if (name.equals(FeatureKeys.XML_VERSION)) {
            // Spelling mistake retained for backwards compatibility with 8.9 and earlier
            return (getXMLVersion() == XML10 ? "1.0" : "1.1");

        } else if (name.equals(FeatureKeys.XSD_VERSION)) {
            return (xsdlVersion == XSD10 ? "1.0" : "1.1");

        } else {
            throw new IllegalArgumentException("Unknown attribute " + name);
        }
    }

//    public static void main(String[] args) {
//        Configuration config = Configuration.makeConfiguration(null, null);
//        System.err.println(config.getClass() + ": " + config.isSchemaAware(XML_SCHEMA));
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
// Contributor(s): none.
//