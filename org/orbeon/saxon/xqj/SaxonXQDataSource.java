package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.FeatureKeys;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.om.Validation;

import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Saxon implementation of the XQJ XQDataSource interface. The first action of a client application
 * is to instantiate a SaxonXQDataSource. This is done directly: there is no factory class as with JAXP.
 * An application that does not want compile-time references to the Saxon XQJ implementation can instantiate
 * this class dynamically using the reflection API (class.newInstance()).
 * <p>
 * For full Javadoc descriptions of the public methods, see the XQJ specification.
 */
public class SaxonXQDataSource implements XQDataSource {

    private Configuration config;
    private PrintWriter logger;

    /**
     * Create a SaxonXQDataSource using a default configuration.
     * A schema-aware configuration will be created if Saxon-SA can be loaded from the
     * classpath; otherwise a basic (non-schema-aware) configuration is created
     */

    public SaxonXQDataSource() {
        try {
            config = Configuration.makeSchemaAwareConfiguration(null, null);
        } catch (RuntimeException err) {
            config = new Configuration();
        }
    }

    /**
     * Create a Saxon XQDataSource with a specific configuration
     * @param config The Saxon configuration to be used
     */

    public SaxonXQDataSource(Configuration config) {
        this.config = config;
    }

    /**
     * Get the Saxon Configuration in use. Changes made to this Configuration will affect this
     * data source and XQJ connections created from it (either before or afterwards). Equally,
     * changes made to this SaxonXQDataSource are reflected in the Configuration object (which means
     * they also impact any other SaxonXQDataSource objects that share the same Configuration).
     * @return the configuration in use.
     */

    public Configuration getConfiguration() {
        return config;
    }

    public XQConnection getConnection() throws XQException {
        return new SaxonXQConnection(this);
    }

    /**
     * Get a connection based on an underlying JDBC connection
     * @param con the JDBC connection
     * @return a connection based on an underlying JDBC connection
     * @throws XQException The Saxon implementation of this method always throws
     * an XQException, indicating that Saxon does not support connection to a JDBC data source.
     */

    public XQConnection getConnection(Connection con) throws XQException {
        throw new XQException("Saxon cannot connect to a SQL data source");
    }

    /**
     * Get a connection, by supplying a username and password. The Saxon implementation of this is equivalent
     * to the default constructor: the username and password are ignored.
     * @param username the user name
     * @param password the password
     * @return a connection
     * @throws XQException
     */

    public XQConnection getConnection(String username, String password) throws XQException {
        return getConnection();
    }

    public int getLoginTimeout() {
        return 0;
    }

    public PrintWriter getLogWriter() {
        return logger;
    }

    /**
     * Get a configuration property setting. The properties that are supported, and their meanings, are the
     * same as the properties that can be obtained using "get" methods; for example
     * <code>getProperty("dtdValidation")</code>  returns the same result as <code>getDtdValidation()</code>.
     *
     * <p>Further Saxon configuration properties are available via the Saxon <code>Configuration</code> object,
     * which can be obtained using the {@link #getConfiguration} method.</p>
     *
     * @param name the name of the configuration property
     * @return the value of the configuration property. Note that in the case of on/off properties this
     * will be returned as the string true/false
     * @throws XQException
     */

    public String getProperty(String name) throws XQException {
        if ("allowExternalFunctions".equals(name)) {
            return getAllowExternalFunctions();
        } else if ("dtdValidation".equals(name)) {
            return getDtdValidation();
        } else if ("expandAttributeDefaults".equals(name)) {
            return getExpandAttributeDefaults();
        } else if ("expandXInclude".equals(name)) {
            return getExpandXInclude();
        } else if ("retainLineNumbers".equals(name)) {
            return getRetainLineNumbers();
        } else if ("schemaValidationMode".equals(name)) {
            return getSchemaValidationMode();
        } else if ("stripWhitespace".equals(name)) {
            return getStripWhitespace();
        } else if ("useXsiSchemaLocation".equals(name)) {
            return getUseXsiSchemaLocation();
        } else if ("xmlVersion".equals(name)) {
            return getXmlVersion();
        } else if ("xsdVersion".equals(name)) {
            return getXsdVersion();
        }
        throw new XQException("Property " + name + " is not recognized");
    }

    private static String[] supportedPropertyNames = {
        "allowExternalFunctions",
        "dtdValidation",
        "expandAttributeDefaults",
        "expandXInclude",
        "retainLineNumbers",
        "schemaValidationMode",
        "stripWhitespace",
        "useXsiSchemaLocation",
        "xmlVersion",
        "xsdVersion"
    };

    public String[] getSupportedPropertyNames() {
        return supportedPropertyNames;
    }

    public void setLoginTimeout(int seconds) throws XQException {
        // no-op
    }

    public void setLogWriter(PrintWriter out) throws XQException {
        logger = out;
    }

    public void setProperties(Properties props) throws XQException {
        checkNotNull(props, "props");
        Enumeration iter = props.keys();
        while (iter.hasMoreElements()) {
            String name = (String)iter.nextElement();
            String value = props.getProperty(name);
            setProperty(name, value);
        }
    }

    /**
     * Set a configuration property. The properties that are supported, and their meanings, are the
     * same as the properties that can be obtained using "set" methods; for example
     * <code>setProperty("dtdValidation", "true")</code>  has the same effect as
     * <code>setDtdValidation("true")</code>.
     *
     * <p>Further Saxon configuration properties can be set via the Saxon <code>Configuration</code> object,
     * which can be obtained using the {@link #getConfiguration} method.</p>
     *
     * @param name the name of the configuration property
     * @param value the value of the configuration property
     * @throws XQException
     */

    public void setProperty(String name, String value) throws XQException {
        try {
            if ("allowExternalFunctions".equals(name)) {
                setAllowExternalFunctions(value);
            } else if ("dtdValidation".equals(name)) {
                setDtdValidation(value);
            } else if ("expandAttributeDefaults".equals(name)) {
                setExpandAttributeDefaults(value);
            } else if ("expandXInclude".equals(name)) {
                setExpandXInclude(value);
            } else if ("retainLineNumbers".equals(name)) {
                setRetainLineNumbers(value);
            } else if ("schemaValidationMode".equals(name)) {
                setSchemaValidationMode(value);
            } else if ("stripWhitespace".equals(name)) {
                setStripWhitespace(value);
            } else if ("useXsiSchemaLocation".equals(name)) {
                setUseXsiSchemaLocation(value);
            } else if ("xmlVersion".equals(name)) {
                setXmlVersion(value);
            } else if ("xsdVersion".equals(name)) {
                setXsdVersion(value);
            } else {
                throw new XQException("Property " + name + " is not recognized");
            }
        } catch (IllegalArgumentException err) {
            throw new XQException("Invalid value for " + name + ": " + err.getMessage());
        }
    }

    static void checkNotNull(Object arg, String name) throws XQException {
        if (arg == null) {
            throw new XQException("Argument " + name + " is null");
        }
    }

    /**
     * Say whether queries are allowed to call external functions.
     * @param value set to "true" if external function calls are allowed (default) or "false" otherwise
     */

    public void setAllowExternalFunctions(String value) {
        if ("true".equals(value)) {
            config.setAllowExternalFunctions(true);
        } else if ("false".equals(value)) {
            config.setAllowExternalFunctions(false);
        } else {
            throw new IllegalArgumentException("allowExternalFunctions");
        }
    }

    /**
     * Ask whether queries are allowed to call external functions.
     * @return "true" if external function calls are allowed, "false" otherwise
     */

    public String getAllowExternalFunctions() {
        return (config.isAllowExternalFunctions() ? "true" : "false");
    }

    /**
     * Say whether source documents are to be parsed with DTD validation enabled
     * @param value "true" if DTD validation is to be enabled, otherwise "false". Default is "false".
     */

    public void setDtdValidation(String value) {
        if ("true".equals(value)) {
            config.setValidation(true);
        } else if ("false".equals(value)) {
            config.setValidation(false);
        } else {
            throw new IllegalArgumentException("dtdValidation");
        }
    }

    /**
     * Ask whether source documents are to be parsed with DTD validation enabled
     * @return "true" if DTD validation is to be enabled, otherwise "false". Default is "false".
     */

    public String getDtdValidation()  {
        return (config.isValidation() ? "true" : "false");
    }

    /**
     * Say whether whether fixed and default values defined
     * in a schema or DTD will be expanded. By default, or if the value is "true" (and for conformance with the
     * specification) validation against a DTD or schema will cause default values defined in the schema
     * or DTD to be inserted into the document. Setting this feature to "false" suppresses this behaviour. In
     * the case of DTD-defined defaults this only works if the XML parser reports whether each attribute was
     * specified in the source or generated by expanding a default value. Not all XML parsers report this
     * information.
     * @param value "true" if default values are to be expanded, otherwise "false". Default is "true".
     */

    public void setExpandAttributeDefaults(String value) {
        if ("true".equals(value)) {
            config.setExpandAttributeDefaults(true);
        } else if ("false".equals(value)) {
            config.setExpandAttributeDefaults(false);
        } else {
            throw new IllegalArgumentException("expandAttributeDefaults");
        }
    }

    /**
     * Ask whether fixed or default values defined in a schema or DTD will be expanded
     * @return "true" if such values will be expanded, otherwise "false"
     */

    public String getExpandAttributeDefaults() {
        return (config.isExpandAttributeDefaults() ? "true" : "false");
    }

    /**
     * Say whether XInclude processing is to be applied to source documents
     * @param value "true" if XInclude directives are to expanded, otherwise "false". Default is "false".
     */

    public void setExpandXInclude(String value) {
        if ("true".equals(value)) {
            config.setXIncludeAware(true);
        } else if ("false".equals(value)) {
            config.setXIncludeAware(false);
        } else {
            throw new IllegalArgumentException("expandXInclude");
        }
    }

    /**
     * Ask whether XInclude processing is to be applied to source documents
     * @return "true" if  XInclude directives are to expanded, otherwise "false". Default is "false".
     */

    public String getExpandXInclude() {
        return (config.isXIncludeAware() ? "true" : "false");
    }


    /**
     * Say whether source documents should have line and column information retained. This
     * information is available via extension functions <code>saxon:line-number()</code> and
     * <code>saxon:column-number()</code>
     * @param value "true" if line and column information is to be retained, otherwise "false". Default is "false".
     */

    public void setRetainLineNumbers(String value) {
        if ("true".equals(value)) {
            config.setLineNumbering(true);
        } else if ("false".equals(value)) {
            config.setLineNumbering(false);
        } else {
            throw new IllegalArgumentException("retainLineNumbers");
        }
    }

    /**
     * Ask whether line and column information will be retained for source documents
     * @return "true" if line and column information is retained, otherwise "false"
     */

    public String getRetainLineNumbers() {
        return (config.isLineNumbering() ? "true" : "false");
    }

    /**
     * Say whether source documents should be validated against a schema
     * @param value set to "strict" if source documents are to be subjected to strict validation,
     * "lax" if source documents are to be subjected to lax validation, "skip" if source documents
     * are not to be subjected to schema validation
     */

    public void setSchemaValidationMode(String value) {
        if ("strict".equals(value)) {
            config.setSchemaValidationMode(Validation.STRICT);
        } else if ("lax".equals(value)) {
            config.setSchemaValidationMode(Validation.LAX);
        } else if ("skip".equals(value)) {
            config.setSchemaValidationMode(Validation.SKIP);
        } else {
            throw new IllegalArgumentException("schemaValidationMode");
        }
    }

    /**
     * Ask whether source documents will be validated against a schema
     * @return "strict" if source documents are to be subjected to strict validation,
     * "lax" if source documents are to be subjected to lax validation, "skip" if source documents
     * are not to be subjected to schema validation
     */

    public String getSchemaValidationMode() {
        return Validation.toString(config.getSchemaValidationMode());
    }

    /**
     * Say whether whitespace should be stripped when loading source documents
     * @param value "all" if all whitespace text nodes are to be stripped, "ignorable" if
     * only whitespace text nodes in elements defined in a schema or DTD as having element-only
     * content are to be stripped, "none" if no whitespace text nodes are to be stripped
     */

    public void setStripWhitespace(String value) {
        if ("all".equals(value)) {
            config.setStripsWhiteSpace(Whitespace.ALL);
        } else if ("ignorable".equals(value)) {
            config.setStripsWhiteSpace(Whitespace.IGNORABLE);
        } else if ("none".equals(value)) {
            config.setStripsWhiteSpace(Whitespace.NONE);
        } else {
            throw new IllegalArgumentException("stripWhitespace");
        }
    }

    /**
     * Ask whether whitespace will be stripped when loading source documents
     * @return "all" if all whitespace text nodes are to be stripped, "ignorable" if
     * only whitespace text nodes in elements defined in a schema or DTD as having element-only
     * content are to be stripped, "none" if no whitespace text nodes are to be stripped
     */

    public String getStripWhitespace() {
        switch (config.getStripsWhiteSpace()) {
            case Whitespace.ALL: return "all";
            case Whitespace.IGNORABLE: return "ignorable";
            default: return "none";
        }
    }

    /**
     * Say whether the schema processor is to take account of xsi:schemaLocation and
     * xsi:noNamespaceSchemaLocation attributes encountered in an instance document being validated
     * @param value set to "true" if these attributes are to be recognized (default) or "false" otherwise
     */

    public void setUseXsiSchemaLocation(String value) {
        if ("true".equals(value)) {
            config.setConfigurationProperty(FeatureKeys.USE_XSI_SCHEMA_LOCATION, BooleanValue.TRUE);
        } else if ("false".equals(value)) {
            config.setConfigurationProperty(FeatureKeys.USE_XSI_SCHEMA_LOCATION, BooleanValue.FALSE);
        } else {
            throw new IllegalArgumentException("useXsiSchemaLocation");
        }
    }

    /**
     * Ask whether the schema processor is to take account of xsi:schemaLocation and
     * xsi:noNamespaceSchemaLocation attributes encountered in an instance document being validated
     * @return "true" if these attributes are to be recognized (default) or "false" otherwise
     */

    public String getUseXsiSchemaLocation() {
        Boolean b = (Boolean)config.getConfigurationProperty(FeatureKeys.USE_XSI_SCHEMA_LOCATION);
        return (b.booleanValue() ? "true" : "false");
    }

    /**
     * Say whether XML 1.0 or XML 1.1 rules for XML names are to be followed
     * @param value "1.0" (default) if XML 1.0 rules are to be used, "1.1" if XML 1.1
     * rules apply
     */

    public void setXmlVersion(String value) {
        if ("1.0".equals(value)) {
            config.setXMLVersion(Configuration.XML10);
        } else if ("1.1".equals(value)) {
            config.setXMLVersion(Configuration.XML11);
        } else {
            throw new IllegalArgumentException("xmlVersion");
        }
    }

   /**
     * Ask whether XML 1.0 or XML 1.1 rules for XML names are to be followed
     * @return "1.0" if XML 1.0 rules are to be used, "1.1" if XML 1.1
     * rules apply
     */

    public String getXmlVersion() {
       return (config.getXMLVersion() == Configuration.XML10 ? "1.0" : "1.1");
   }

    /**
     * Say whether XML Schema 1.0 syntax must be used or whether XML Schema 1.1 features are allowed
     * @param value "1.0" (default) if XML Schema 1.0 rules are to be followed, "1.1" if XML Schema 1.1
     * features may be used
     */

    public void setXsdVersion(String value) {
        if ("1.0".equals(value)) {
            config.setConfigurationProperty(FeatureKeys.XSD_VERSION, "1.0");
        } else if ("1.1".equals(value)) {
            config.setConfigurationProperty(FeatureKeys.XSD_VERSION, "1.1");
        } else {
            throw new IllegalArgumentException("xsdVersion");
        }
    }

   /**
     * Ask whether XML Schema 1.0 syntax must be used or whether XML Schema 1.1 features are allowed
     * @return "1.0" (default) if XML Schema 1.0 rules are to be followed, "1.1" if XML Schema 1.1
     * features may be used
     */

    public String getXsdVersion() {
       return (String)config.getConfigurationProperty(FeatureKeys.XSD_VERSION);
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
//