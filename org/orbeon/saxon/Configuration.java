package org.orbeon.saxon;

import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.StandardOutputResolver;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.functions.JavaExtensionLibrary;
import org.orbeon.saxon.functions.ExtensionFunctionFactory;
import org.orbeon.saxon.functions.VendorFunctionLibrary;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.instruct.Debugger;
import org.orbeon.saxon.expr.Optimizer;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import java.io.Serializable;
import java.util.Locale;


/**
 * This class holds details of user-selected configuration options for a transformation
 * or query
 */


public class Configuration implements Serializable {

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
    private boolean allowExternalFunctions = true;
    private boolean traceExternalFunctions = false;
    private boolean validation = false;
    private NamePool targetNamePool = null;
    private boolean stripsAllWhiteSpace = false;
    private int hostLanguage = XSLT;
    private boolean schemaValidation = false;
    private boolean validationWarnings = false;
    private boolean retainDTDattributeTypes = false;
    private Debugger debugger = null;
    protected Optimizer optimizer = null;
    private ExtensionFunctionFactory extensionFunctionFactory = new ExtensionFunctionFactory();

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
     * Create a configuration object with default settings for all options
     */

    public Configuration() {
        targetNamePool = NamePool.getDefaultNamePool();
        extensionBinder = new JavaExtensionLibrary(this);
    }

   /**
     * Get a message used to identify this product when a transformation is run using the -t option
     * @return A string containing both the product name and the product
     *     version
     */

    public String getProductTitle() {
        return "Saxon " + Version.getProductVersion() + " from Saxonica";
    }

    /**
     * Determine if the configuration is schema-aware, for the given host language
     * @param language the required host language: XSLT, XQUERY, or XML_SCHEMA
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
     * Get the host language used in this configuration. The possible values
     * are XSLT and XQUERY.
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
     */

    public void setURIResolver(URIResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Get the ErrorListener used in this configuration. If no ErrorListener
     * has been supplied explicitly, the default ErrorListener is used.
     * @return the ErrorListener.
     */

    public ErrorListener getErrorListener() {
        if (listener == null) {
            listener = new StandardErrorListener();
        }
        return listener;
    }

    /**
     * Set the ErrorListener to be used in this configuration. The ErrorListener
     * is informed of all static and dynamic errors detected, and can decide whether
     * run-time warnings are to be treated as fatal.
     * @param listener the ErrorListener to be used
     */

    public void setErrorListener(ErrorListener listener) {
        this.listener = listener;
    }

    /**
     * Get the Tree Model used by this Configuration. This is either
     * Builder.STANDARD_TREE or Builder.TINY_TREE. The default (confusingly)
     * is Builder.TINY_TREE.
     * @return the selected Tree Model
     */

    public int getTreeModel() {
        return treeModel;
    }

    /**
     * Set the Tree Model used by this Configuration. This is either
     * Builder.STANDARD_TREE or Builder.TINY_TREE. The default (confusingly)
     * is Builder.TINY_TREE.
     * @param treeModel the selected Tree Model
     */

    public void setTreeModel(int treeModel) {
        this.treeModel = treeModel;
    }

    /**
     * Determine whether source documents will maintain line numbers, for the
     * benefit of the saxon:line-number() extension function as well as run-time
     * tracing.
     * @return true if line numbers are maintained in source documents
     */

    public boolean isLineNumbering() {
        return lineNumbering;
    }

    /**
     * Determine whether source documents will maintain line numbers, for the
     * benefit of the saxon:line-number() extension function as well as run-time
     * tracing.
     * @param lineNumbering true if line numbers are maintained in source documents
     */

    public void setLineNumbering(boolean lineNumbering) {
        this.lineNumbering = lineNumbering;
    }

    /**
     * Get the TraceListener used for run-time tracing of instruction execution.
     * @return the TraceListener, or null if none is in use.
     */

    public TraceListener getTraceListener() {
        return traceListener;
    }

    /**
     * Set the TraceListener to be used for run-time tracing of instruction execution.
     * @param traceListener The TraceListener to be used.
     */

    public void setTraceListener(TraceListener traceListener) {
        this.traceListener = traceListener;
    }

    /**
     * Set the FunctionLibrary used to bind calls on extension functions. This allows the
     * rules for identifying extension functions to be customized (in principle, it would
     * allow support for extension functions in other languages to be provided).
     * @param binder The FunctionLibrary object used to locate implementations of extension
     * functions, based on their name and arity
     * @see #setExtensionFunctionFactory
     */

    public void setExtensionBinder(FunctionLibrary binder) {
        extensionBinder = binder;
    }

    /**
     * Get the FunctionLibrary used to bind calls on extension functions
     */

    public FunctionLibrary getExtensionBinder() {
        return extensionBinder;
    }

    /**
     * Get the FunctionLibrary used to bind calls on Saxon-defined extension functions
     */

    public VendorFunctionLibrary getVendorFunctionLibrary() {
        if (vendorFunctionLibrary == null) {
            vendorFunctionLibrary = new VendorFunctionLibrary();
        }
        return vendorFunctionLibrary;
    }
    /**
     * Determine how recoverable run-time errors are to be handled. This applies
     * only if the standard ErrorListener is used. The options are RECOVER_SILENTLY,
     * RECOVER_WITH_WARNINGS, or DO_NOT_RECOVER.
     * @return the current recovery policy
     */

    public int getRecoveryPolicy() {
        return recoveryPolicy;
    }

    /**
     * Determine how recoverable run-time errors are to be handled. This applies
     * only if the standard ErrorListener is used. The options are RECOVER_SILENTLY,
     * RECOVER_WITH_WARNINGS, or DO_NOT_RECOVER.
     * @param recoveryPolicy the recovery policy to be used.
     */

    public void setRecoveryPolicy(int recoveryPolicy) {
        this.recoveryPolicy = recoveryPolicy;
    }

    /**
     * Get the name of the class that will be instantiated to create a MessageEmitter,
     * to process the output of xsl:message instructions.
     * @return the full class name of the message emitter class.
     */

    public String getMessageEmitterClass() {
        return messageEmitterClass;
    }

    /**
     * Set the name of the class that will be instantiated to create a MessageEmitter,
     * to process the output of xsl:message instructions.
     * @param messageEmitterClass the full class name of the message emitter class. This
     * must implement org.orbeon.saxon.event.Emitter.
     */

    public void setMessageEmitterClass(String messageEmitterClass) {
        this.messageEmitterClass = messageEmitterClass;
    }

    /**
     * Get the name of the class that will be instantiated to create an XML parser
     * for parsing source documents (for example, documents loaded using the document()
     * or doc() functions).
     * @return the fully qualified name of the XML parser class
     */

    public String getSourceParserClass() {
        return sourceParserClass;
    }

    /**
     * Set the name of the class that will be instantiated to create an XML parser
     * for parsing source documents (for example, documents loaded using the document()
     * or doc() functions).
     * @param sourceParserClass the fully qualified name of the XML parser class. This must implement
     * the SAX2 XMLReader interface.
     */

    public void setSourceParserClass(String sourceParserClass) {
        this.sourceParserClass = sourceParserClass;
    }

    /**
     * Get the name of the class that will be instantiated to create an XML parser
     * for parsing stylesheet modules.
     * @return the fully qualified name of the XML parser class
     */

    public String getStyleParserClass() {
        return styleParserClass;
    }

   /**
     * Set the name of the class that will be instantiated to create an XML parser
     * for parsing stylesheet modules.
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
     */

    public void setOutputURIResolver(OutputURIResolver outputURIResolver) {
        this.outputURIResolver = outputURIResolver;
    }

    /**
     * Determine whether brief progress messages and timing information will be output
     * to System.err
     * @return true if these messages are to be output.
     */

    public boolean isTiming() {
        return timing;
    }

    /**
     * Determine whether brief progress messages and timing information will be output
     * to System.err
     * @param timing true if these messages are to be output.
     */

    public void setTiming(boolean timing) {
        this.timing = timing;
    }

    /**
     * Determine whether calls to external Java functions are permitted.
     * @return true if such calls are permitted.
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
     * @param allowExternalFunctions true if external function calls are to be
     * permitted.
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
     * on the resulting nodes
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
     * @see #setExtensionBinder
     */

    public void setExtensionFunctionFactory(ExtensionFunctionFactory factory) {
        extensionFunctionFactory = factory;
    }
    /**
     * Determine whether the XML parser for source documents will be asked to perform
     * DTD validation of source documents
     * @return true if DTD validation is requested.
     */

    public boolean isValidation() {
        return validation;
    }

    /**
     * Determine whether the XML parser for source documents will be asked to perform
     * DTD validation of source documents
     * @param validation true if DTD validation is to be requested.
     */

    public void setValidation(boolean validation) {
        this.validation = validation;
    }

    /**
     * Determine whether source documents (supplied as a StreamSource or SAXSource)
     * should be subjected to schema validation
     * @return true if source documents should be validated
     */

    public boolean isSchemaValidation() {
        return schemaValidation;
    }

    /**
     * Indicate whether source documents (supplied as a StreamSource or SAXSource)
     * should be subjected to schema validation
     * @param validate true if source documents should be validated
     */

    public void setSchemaValidation(boolean validate) {
        if (validate && !isSchemaAware(Configuration.XML_SCHEMA)) {
            needSchemaAwareVersion();
        }
        schemaValidation = validate;
    }

    /**
     * Indicate whether validation failures on result documents are to be treated
     * as fatal errors or as warnings
     */

    public void setValidationWarnings(boolean warn) {
        validationWarnings = warn;
    }

    /**
     * Determine whether validation failures on result documents are to be treated
     * as fatal errors or as warnings.
     * @return true if validation errors are to be treated as warnings (that is, the
     * validation failure is reported but processing continues as normal).
     */

    public boolean isValidationWarnings() {
        return validationWarnings;
    }

    /**
     * Report a validation error. This will throw an exception if validation errors are
     * being treated as fatal, but report a warning if they are being treated as warnings
     */

    public void reportValidationError(ValidationException err, boolean isOutput) throws ValidationException {
        if (validationWarnings && isOutput) {
            try {
                getErrorListener().warning(err);
            } catch (TransformerException e) {
                if (e instanceof ValidationException) {
                    throw (ValidationException)e;
                } else if (e.getException() instanceof ValidationException) {
                    throw (ValidationException)e.getException();
                } else {
                    throw new ValidationException(e);
                }
            }
        } else {
            throw err;
        }
    }
    /**
     * Get the target namepool to be used for stylesheets/queries and for source documents.
     * @return the target name pool. If no NamePool has been specified explicitly, the
     * default NamePool is returned.
     */

    public NamePool getNamePool() {
        return targetNamePool;
    }

    /**
     * Set the NamePool to be used for stylesheets/queries and for source documents.
     * @param targetNamePool The NamePool to be used.
     */

    public void setNamePool(NamePool targetNamePool) {
        this.targetNamePool = targetNamePool;
    }

    /**
     * Determine whether whitespace-only text nodes are to be stripped unconditionally
     * from source documents.
     * @return true if all whitespace-only text nodes are stripped.
     */

    public boolean isStripsAllWhiteSpace() {
        return stripsAllWhiteSpace;
    }

      /**
     * Determine whether whitespace-only text nodes are to be stripped unconditionally
     * from source documents.
     * @param stripsAllWhiteSpace if all whitespace-only text nodes are to be stripped.
     */

    public void setStripsAllWhiteSpace(boolean stripsAllWhiteSpace) {
        this.stripsAllWhiteSpace = stripsAllWhiteSpace;
    }

    /**
    * Get the parser for source documents
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
    * Get the parser for stylesheet documents
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
     */

    public String readSchema(String baseURI, String schemaLocation, String expected)
    throws TransformerConfigurationException {
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
     */

    public void addSchema(Source schemaSource) throws SchemaException {
        needSchemaAwareVersion();
    }

    /**
     * Add a schema to the cache
     */

    public void addSchema(SchemaMarker schema)
    throws TransformerConfigurationException {
        needSchemaAwareVersion();
    }

    /**
     * Get a schema from the cache. Return null if not found.
     */

    public SchemaMarker getSchema(String namespace) {
        return null;
    }

    /**
     * Get a global element declaration
     * @return the element declaration whose name matches the given
     * fingerprint, or null if no element declaration with this name has
     * been registered.
     */

    public SchemaDeclaration getElementDeclaration(int fingerprint) {
        return null;
    }

    /**
     * Get a global attribute declaration
     * @return the attribute declaration whose name matches the given
     * fingerprint, or null if no element declaration with this name has
     * been registered.
     */

    public SchemaDeclaration getAttributeDeclaration(int fingerprint) {
        return null;
    }

    /**
      * Get the top-level schema type with a given fingerprint.
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
     * Get a document-level validator to add to a Receiver pipeline
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
     * Validate an attribute value
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
     * Make a test for elements corresponding to a give element declaration
     */

    public NodeTest makeSubstitutionGroupTest(SchemaDeclaration elementDecl) {
        needSchemaAwareVersion();
        return null;
    }


    /**
    * Create a new SAX XMLReader object using the class name provided. <br>
    *
    * The named class must exist and must implement the
    * org.xml.sax.XMLReader or Parser interface. <br>
    *
    * This method returns an instance of the parser named.
    *
    * @param className A string containing the name of the
    *   SAX parser class, for example "com.microstar.sax.LarkDriver"
    * @return an instance of the Parser class named, or null if it is not
    * loadable or is not a Parser.
    *
    */
    public static XMLReader makeParser (String className)
    throws TransformerFactoryConfigurationError
    {
        Object obj;
        try {
            obj = Loader.getInstance(className);
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
    * Get a locale given a language code in XML format
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
     * Set the debugger to be used
     */

    public void setDebugger(Debugger debugger) {
        this.debugger = debugger;
    }

    /**
     * Get the debugger in use. This will be null if no debugger has been registered.
     */

    public Debugger getDebugger() {
        return debugger;
    }

    /**
     * Factory method to create a SlotManager
     */

    public SlotManager makeSlotManager() {
        if (debugger == null) {
            return new SlotManager();
        } else {
            return debugger.makeSlotManager();
        }
    }

    /**
     * Factory method to get an Optimizer
     */

    public Optimizer getOptimizer() {
        if (optimizer == null) {
            optimizer = new Optimizer();
        }
        return optimizer;
    }

//    public void builtInParse(Source source, Receiver receiver, int validation) throws XPathException {
//        needSchemaAwareVersion();
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