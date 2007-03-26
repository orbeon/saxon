package org.orbeon.saxon.xpath;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.FeatureKeys;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.Validation;

import javax.xml.XMLConstants;
import javax.xml.xpath.*;

/**
 * Saxon implementation of the JAXP 1.3 XPathFactory
 */
public class XPathFactoryImpl extends XPathFactory {

    private Configuration config;
    private XPathVariableResolver variableResolver;
    private XPathFunctionResolver functionResolver;

    public XPathFactoryImpl() {
        config = makeConfiguration();
    }

    protected Configuration makeConfiguration() {
        return new Configuration();
    }

    /**
     * Get the Configuration object
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Test whether a given object model is supported. Returns true if the object model
     * is the Saxon object model, DOM, JDOM, or XOM
     * @param model The URI identifying the object model.
     * @return true if the object model is one of
     * {@link NamespaceConstant#OBJECT_MODEL_SAXON},
     * {@link XPathConstants#DOM_OBJECT_MODEL},
     * {@link NamespaceConstant#OBJECT_MODEL_JDOM}, or
     * {@link NamespaceConstant#OBJECT_MODEL_XOM}, or
     * {@link NamespaceConstant#OBJECT_MODEL_DOM4J}
     */
    public boolean isObjectModelSupported(String model) {
        if (model.equals(NamespaceConstant.OBJECT_MODEL_SAXON)) return true;
        if (model.equals(XPathConstants.DOM_OBJECT_MODEL)) return true;
        if (model.equals(NamespaceConstant.OBJECT_MODEL_JDOM)) return true;
        if (model.equals(NamespaceConstant.OBJECT_MODEL_XOM)) return true;
        if (model.equals(NamespaceConstant.OBJECT_MODEL_DOM4J)) return true;
        return false;
    }

    /**
     * Set a feature of this XPath implementation. The only features currently
     * recognized are:
     * <ul>
     * <li> {@link XMLConstants#FEATURE_SECURE_PROCESSING} </li>
     * <li> {@link org.orbeon.saxon.FeatureKeys#SCHEMA_VALIDATION}: requests schema validation of source documents.
     *   The property is rejected if the configuration is not schema-aware. </li>
     * </ul>
     * @param feature a URI identifying the feature
     * @param b true to set the feature on, false to set it off
     * @throws XPathFactoryConfigurationException if the feature name is not recognized
     */

    public void setFeature(String feature, boolean b) throws XPathFactoryConfigurationException {
        if (feature.equals(XMLConstants.FEATURE_SECURE_PROCESSING)) {
            config.setAllowExternalFunctions(!b);
        } else if (feature.equals(FeatureKeys.SCHEMA_VALIDATION)) {
            config.setSchemaValidationMode(b ? Validation.STRICT : Validation.STRIP);
        } else {
            throw new XPathFactoryConfigurationException("Unknown feature: " + feature);
        }
    }

    /**
     * Get a feature of this XPath implementation. The only features currently
     * recognized are:
     * <ul>
     * <li> {@link XMLConstants#FEATURE_SECURE_PROCESSING} </li>
     * <li> {@link org.orbeon.saxon.FeatureKeys#SCHEMA_VALIDATION}: requests schema validation of source documents. </li>
     * </ul>
     * @param feature a URI identifying the feature
     * @return true if the feature is on, false if it is off
     * @throws XPathFactoryConfigurationException if the feature name is not recognized
     */

    public boolean getFeature(String feature) throws XPathFactoryConfigurationException {
        if (feature.equals(XMLConstants.FEATURE_SECURE_PROCESSING)) {
            return !config.isAllowExternalFunctions();
        } else if (feature.equals(FeatureKeys.SCHEMA_VALIDATION)) {
            return config.getSchemaValidationMode() == Validation.STRICT;
        } else {
            throw new XPathFactoryConfigurationException("Unknown feature: " + feature);
        }
    }

    /**
     * Set a resolver for XPath variables. This will be used to obtain the value of
     * any variable referenced in an XPath expression. The variable resolver must be allocated
     * before the expression is compiled, but it will only be called when the expression
     * is evaluated.
     * @param xPathVariableResolver The object used to resolve references to variables.
     */
    public void setXPathVariableResolver(XPathVariableResolver xPathVariableResolver) {
        variableResolver = xPathVariableResolver;
    }

    /**
     * Set a resolver for XPath functions. This will be used to obtain an implementation
     * of any external function referenced in an XPath expression. This is not required for
     * system functions, Saxon extension functions, constructor functions named after types,
     * or extension functions bound using a namespace that maps to a Java class.
     * @param xPathFunctionResolver The object used to resolve references to external functions.
     */

    public void setXPathFunctionResolver(XPathFunctionResolver xPathFunctionResolver) {
        functionResolver = xPathFunctionResolver;
    }

    /**
     * Create an XPath evaluator
     * @return an XPath object, which can be used to compile and execute XPath expressions.
     */
    public XPath newXPath() {
        XPathEvaluator xpath = new XPathEvaluator(config);
        xpath.setXPathFunctionResolver(functionResolver);
        xpath.setXPathVariableResolver(variableResolver);
        //config.registerStandardObjectModels();
        return xpath;
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