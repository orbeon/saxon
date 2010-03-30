package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.tree.ElementImpl;
import org.orbeon.saxon.tree.NodeFactory;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import java.math.BigDecimal;

/**
  * Class StyleNodeFactory. <br>
  * A Factory for nodes in the stylesheet tree. <br>
  * Currently only allows Element nodes to be user-constructed.
  * @author Michael H. Kay
  */

public class StyleNodeFactory implements NodeFactory {

    IntHashMap userStyles = new IntHashMap(4);
    Configuration config;
    NamePool namePool;
    boolean allowExtensions;
    ErrorListener errorListener;

    /**
     * Create the node factory for representing an XSLT stylesheet as a tree structure
     * @param config the Saxon configuration
     * @param errorListener used for reporting errors
     */

    public StyleNodeFactory(Configuration config, ErrorListener errorListener) {

		this.config = config;
        namePool = config.getNamePool();
		allowExtensions = config.isAllowExternalFunctions();
        this.errorListener = errorListener;
    }

    /**
    * Create an Element node. Note, if there is an error detected while constructing
    * the Element, we add the element anyway, and return success, but flag the element
    * with a validation error. This allows us to report more than
    * one error from a single compilation.
    * @param nameCode The element name
     * @param typeCode
     * @param attlist the attribute list
     */

    public ElementImpl makeElementNode(
            NodeInfo parent,
            int nameCode,
            int typeCode, AttributeCollectionImpl attlist,
            int[] namespaces,
            int namespacesUsed,
            LocationProvider locator,
            int locationId,
            int sequence)
    {
        boolean toplevel = (parent instanceof XSLStylesheet);
        String baseURI = null;
        int lineNumber = -1;
        int columnNumber = -1;
        if (locator!=null) {
            baseURI = locator.getSystemId(locationId);
            lineNumber = locator.getLineNumber(locationId);
            columnNumber = locator.getColumnNumber(locationId);
        }

        if (parent instanceof DataElement) {
            DataElement d = new DataElement();
            d.setNamespaceDeclarations(namespaces, namespacesUsed);
            d.initialise(nameCode, typeCode, attlist, parent, sequence);
            d.setLocation(baseURI, lineNumber, columnNumber);
            return d;
        }

    	int f = nameCode&0xfffff;

    	// Try first to make an XSLT element

    	StyleElement e = makeXSLElement(f);

		if (e != null) {  // recognized as an XSLT element

            e.setNamespaceDeclarations(namespaces, namespacesUsed);
            e.initialise(nameCode, typeCode, attlist, parent, sequence);
            e.setLocation(baseURI, lineNumber, columnNumber);
            // We're not catching multiple errors in the following attributes, but catching each of the
            // exceptions helps to ensure we don't report spurious errors through not processing some
            // of the attributes when others are faulty.
            try {
	            e.processExtensionElementAttribute(StandardNames.EXTENSION_ELEMENT_PREFIXES);
            } catch (TransformerException err) {
	            e.setValidationError(err, StyleElement.REPORT_ALWAYS);
	        }
            try {
	            e.processExcludedNamespaces(StandardNames.EXCLUDE_RESULT_PREFIXES);
            } catch (TransformerException err) {
	            e.setValidationError(err, StyleElement.REPORT_ALWAYS);
	        }
            try {
	            e.processVersionAttribute(StandardNames.VERSION);
            } catch (TransformerException err) {
	            e.setValidationError(err, StyleElement.REPORT_ALWAYS);
	        }
	        e.processDefaultXPathNamespaceAttribute(StandardNames.XPATH_DEFAULT_NAMESPACE);
            return e;

        } else {   // not recognized as an XSLT element

	        short uriCode = namePool.getURICode(nameCode);
	        String localname = namePool.getLocalName(nameCode);
            StyleElement temp = null;

            // Detect a misspelt XSLT declaration

            if (uriCode == NamespaceConstant.XSLT_CODE &&
                    (parent instanceof XSLStylesheet) &&
                    ((XSLStylesheet)parent).getVersion().compareTo(BigDecimal.valueOf('2')) <= 0 ) {
                temp = new AbsentExtensionElement();
                temp.setValidationError(new XPathException("Unknown top-level XSLT declaration"),
                       StyleElement.REPORT_UNLESS_FORWARDS_COMPATIBLE );
            }

	        Class assumedClass = LiteralResultElement.class;

	        // We can't work out the final class of the node until we've examined its attributes
	        // such as version and extension-element-prefixes; but we can have a good guess, and
	        // change it later if need be.


			boolean assumedSaxonElement = false;

			// recognize Saxon extension elements

            if (temp==null) {
                if (uriCode == NamespaceConstant.SAXON_CODE) {
                    temp = makeSaxonElement(f);
                    if (temp!=null) {
                        assumedClass = temp.getClass();
                        assumedSaxonElement = true;
                    }
                } else if (toplevel && uriCode != 0) {
                    DataElement d = new DataElement();
                    d.setNamespaceDeclarations(namespaces, namespacesUsed);
                    d.initialise(nameCode, typeCode, attlist, parent, sequence);
                    d.setLocation(baseURI, lineNumber, columnNumber);
                    return d;
                }
            }

			if (temp==null) {
		        temp = new LiteralResultElement();
		    }

	        temp.setNamespaceDeclarations(namespaces, namespacesUsed);

  	        try {
	            temp.initialise(nameCode, typeCode, attlist, parent, sequence);
                temp.setLocation(baseURI, lineNumber, columnNumber);
                temp.processDefaultCollationAttribute(StandardNames.XSL_DEFAULT_COLLATION_CLARK);
	            temp.processExtensionElementAttribute(StandardNames.XSL_EXTENSION_ELEMENT_PREFIXES_CLARK);
	            temp.processExcludedNamespaces(StandardNames.XSL_EXCLUDE_RESULT_PREFIXES_CLARK);
	            temp.processVersionAttribute(StandardNames.XSL_VERSION_CLARK);
	            temp.processDefaultXPathNamespaceAttribute(StandardNames.XSL_XPATH_DEFAULT_NAMESPACE_CLARK);
	        } catch (XPathException err) {
	            temp.setValidationError(err, StyleElement.REPORT_ALWAYS);
	        }

	        // Now we work out what class of element we really wanted, and change it if necessary

	        TransformerException reason;
	        Class actualClass;

	        if (uriCode == NamespaceConstant.XSLT_CODE) {
                reason = new XPathException("Unknown XSLT element: " + localname);
                ((XPathException)reason).setErrorCode("XTSE0010");
                ((XPathException)reason).setIsStaticError(true);
                actualClass = AbsentExtensionElement.class;
                temp.setValidationError(reason, StyleElement.REPORT_UNLESS_FALLBACK_AVAILABLE);
	        } else if (uriCode == NamespaceConstant.SAXON_CODE) {
	        	if (toplevel || temp.isExtensionNamespace(uriCode)) {
	        		if (assumedSaxonElement) {
	        			// all is well
	        			actualClass = assumedClass;
	        		} else {
	        			actualClass = AbsentExtensionElement.class;
	        			reason = new XPathException(
	        			                "Unknown Saxon extension instruction: " + localname);
                        temp.setValidationError(reason, StyleElement.REPORT_UNLESS_FALLBACK_AVAILABLE);
	        		}
	        	} else {
                    if (assumedSaxonElement) {
                        // We've got an element such as saxon:call-template but the
                        // user didn't declare the Saxon namespace as an extension element
                        // namespace. Chances are he just forgot.
                        try {
                            XPathException te = new XPathException("saxon:" + localname +
                                            " is not being treated as an extension instruction because the namespace " +
                                            " http://saxon.sf.net/ has not been declared in extension-element-prefixes");
                            te.setLocator(temp);
                            te.setErrorCode(SaxonErrorCode.SXWN9008);
                            errorListener.warning(te);
                        } catch (TransformerException e1) {
                            // no action
                        }
                    }
	        		actualClass = LiteralResultElement.class;
	        	}
	        } else if (temp.isExtensionNamespace(uriCode) && !toplevel) {
                actualClass = (Class)userStyles.get(nameCode&0xfffff);
                if (actualClass==null) {
                    if (allowExtensions) {
                        ExtensionElementFactory factory = getFactory(uriCode);
                        if (factory != null) {
                            actualClass = factory.getExtensionClass(localname);
                            if (actualClass != null) {
                                userStyles.put(nameCode&0xfffff, actualClass);             // for quicker access next time
                            }
                        }
                    } else {
                        actualClass = AbsentExtensionElement.class;
	        			reason = new XPathException("Extension instructions are disabled");
                        temp.setValidationError(reason, StyleElement.REPORT_IF_INSTANTIATED);
                    }

                    if (actualClass == null) {

                        // if we can't instantiate an extension element, we don't give up
                        // immediately, because there might be an xsl:fallback defined. We
                        // create a surrogate element called AbsentExtensionElement, and
                        // save the reason for failure just in case there is no xsl:fallback

                        actualClass = AbsentExtensionElement.class;
                        XPathException se = new XPathException("Unknown extension instruction", temp);
                        se.setErrorCode("XTDE1450");
                        reason = se;
                        temp.setValidationError(reason, StyleElement.REPORT_IF_INSTANTIATED);
                    }
                }
	        } else {
                if (!toplevel && namePool.getURI(nameCode).indexOf("ElementFactory") >= 0) {
                    // looks like the user forgot to declare an extension element namespace
                    ExtensionElementFactory factory = getFactory(uriCode);
                    if (factory != null) {
                        try {
                            XPathException te = new XPathException(namePool.getDisplayName(nameCode) +
                                            " is not being treated as an extension instruction because its namespace " +
                                            " has not been declared in extension-element-prefixes");
                            te.setLocator(temp);
                            te.setErrorCode(SaxonErrorCode.SXWN9008);
                            errorListener.warning(te);
                        } catch (TransformerException e1) {
                            // no action
                        }
                    }
                }
                actualClass = LiteralResultElement.class;
	        }

	        StyleElement node;
            if (actualClass.equals(assumedClass)) {
	            node = temp;    // the original element will do the job
	        } else {
	            try {
	                node = (StyleElement)actualClass.newInstance();
	            } catch (InstantiationException err1) {
	                throw new TransformerFactoryConfigurationError(err1, "Failed to create instance of " + actualClass.getName());
	            } catch (IllegalAccessException err2) {
	                throw new TransformerFactoryConfigurationError(err2, "Failed to access class " + actualClass.getName());
	            }
	            node.substituteFor(temp);   // replace temporary node with the new one
	        }
	        return node;
	    }
    }

	/**
	 * Make an XSL element node
     * @param f the fingerprint of the node name
     * @return the constructed element node
	*/

	private StyleElement makeXSLElement(int f) {
        switch (f) {
		case StandardNames.XSL_ANALYZE_STRING:
            return new XSLAnalyzeString();
		case StandardNames.XSL_APPLY_IMPORTS:
            return new XSLApplyImports();
		case StandardNames.XSL_APPLY_TEMPLATES:
            return new XSLApplyTemplates();
		case StandardNames.XSL_ATTRIBUTE:
            return new XSLAttribute();
		case StandardNames.XSL_ATTRIBUTE_SET:
            return new XSLAttributeSet();
		case StandardNames.XSL_CALL_TEMPLATE:
            return new XSLCallTemplate();
		case StandardNames.XSL_CHARACTER_MAP:
            return new XSLCharacterMap();
		case StandardNames.XSL_CHOOSE:
            return new XSLChoose();
		case StandardNames.XSL_COMMENT:
            return new XSLComment();
		case StandardNames.XSL_COPY:
            return new XSLCopy();
		case StandardNames.XSL_COPY_OF:
            return new XSLCopyOf();
		case StandardNames.XSL_DECIMAL_FORMAT:
            return new XSLDecimalFormat();
		case StandardNames.XSL_DOCUMENT:
            return new XSLDocument();
		case StandardNames.XSL_ELEMENT:
            return new XSLElement();
		case StandardNames.XSL_FALLBACK:
            return new XSLFallback();
		case StandardNames.XSL_FOR_EACH:
            return new XSLForEach();
		case StandardNames.XSL_FOR_EACH_GROUP:
            return new XSLForEachGroup();
		case StandardNames.XSL_FUNCTION:
            return new XSLFunction();
		case StandardNames.XSL_IF:
            return new XSLIf();
		case StandardNames.XSL_IMPORT:
            return new XSLImport();
		case StandardNames.XSL_IMPORT_SCHEMA:
            return new XSLImportSchema();
		case StandardNames.XSL_INCLUDE:
            return new XSLInclude();
		case StandardNames.XSL_KEY:
            return new XSLKey();
		case StandardNames.XSL_MATCHING_SUBSTRING:
            return new XSLMatchingSubstring();
		case StandardNames.XSL_MESSAGE:
            return new XSLMessage();
		case StandardNames.XSL_NEXT_MATCH:
            return new XSLNextMatch();
		case StandardNames.XSL_NON_MATCHING_SUBSTRING:
            return new XSLMatchingSubstring();	//sic
		case StandardNames.XSL_NUMBER:
            return new XSLNumber();
		case StandardNames.XSL_NAMESPACE:
            return new XSLNamespace();
		case StandardNames.XSL_NAMESPACE_ALIAS:
            return new XSLNamespaceAlias();
		case StandardNames.XSL_OTHERWISE:
            return new XSLOtherwise();
		case StandardNames.XSL_OUTPUT:
            return new XSLOutput();
		case StandardNames.XSL_OUTPUT_CHARACTER:
            return new XSLOutputCharacter();
		case StandardNames.XSL_PARAM:
            return new XSLParam();
		case StandardNames.XSL_PERFORM_SORT:
            return new XSLPerformSort();
		case StandardNames.XSL_PRESERVE_SPACE:
            return new XSLPreserveSpace();
		case StandardNames.XSL_PROCESSING_INSTRUCTION:
            return new XSLProcessingInstruction();
		case StandardNames.XSL_RESULT_DOCUMENT:
            return new XSLResultDocument();
		case StandardNames.XSL_SEQUENCE:
            return new XSLSequence();
		case StandardNames.XSL_SORT:
            return new XSLSort();
		case StandardNames.XSL_STRIP_SPACE:
            return new XSLPreserveSpace();
		case StandardNames.XSL_STYLESHEET:
            return new XSLStylesheet();
		case StandardNames.XSL_TEMPLATE:
            return new XSLTemplate();
		case StandardNames.XSL_TEXT:
            return new XSLText();
		case StandardNames.XSL_TRANSFORM:
            return new XSLStylesheet();
		case StandardNames.XSL_VALUE_OF:
            return new XSLValueOf();
		case StandardNames.XSL_VARIABLE:
            return new XSLVariable();
		case StandardNames.XSL_WITH_PARAM:
            return new XSLWithParam();
		case StandardNames.XSL_WHEN:
            return new XSLWhen();
        default: return null;
        }
	}

	/**
	* Make a SAXON extension element
     * @param f the fingerprint of the element name
     * @return the constructed element node
	*/

	private StyleElement makeSaxonElement(int f) {

        switch (f) {

		case StandardNames.SAXON_ASSIGN:
            return new SaxonAssign();
		case StandardNames.SAXON_BREAK:
            return new SaxonBreak();
		case StandardNames.SAXON_CALL_TEMPLATE:
            return new SaxonCallTemplate();
		case StandardNames.SAXON_COLLATION:
            return new SaxonCollation();
		case StandardNames.SAXON_CONTINUE:
            return new SaxonContinue();
        case StandardNames.SAXON_DOCTYPE:
            return new SaxonDoctype();
        case StandardNames.SAXON_ENTITY_REF:
            return new SaxonEntityRef();
        case StandardNames.SAXON_FINALLY:
            return new SaxonFinally();
        case StandardNames.SAXON_IMPORT_QUERY:
            return new SaxonImportQuery();
		case StandardNames.SAXON_ITERATE:
            return new SaxonIterate();
        case StandardNames.SAXON_SCRIPT:
            return new SaxonScript();
		case StandardNames.SAXON_WHILE:
            return new SaxonWhile();
		default: return null;
        }
	}

    /**
     * Get the factory class for user extension elements
     * If there is no appropriate class, return null
     * @param uriCode the code for the namespace URI of the extension element
     * @return the appropriate ExtensionElementFactory for this namespace
    */

    private ExtensionElementFactory getFactory(short uriCode) {
    	String uri = namePool.getURIFromNamespaceCode(uriCode);
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash<0 || lastSlash==uri.length()-1) {
            return null;
        }
        String factoryClass = uri.substring(lastSlash+1);
        ExtensionElementFactory factory;

        try {
            factory = (ExtensionElementFactory)config.getInstance(factoryClass, null);
        } catch (XPathException err) {
            return null;
        }
        return factory;
    }

    /**
     * Method to support the element-available() function
     * @param uri the namespace URI
     * @param localName the local Name
     * @return true if an extension element of this name is recognized
    */

    public boolean isElementAvailable(String uri, String localName) {
    	int fingerprint = namePool.getFingerprint(uri, localName);
    	if (uri.equals(NamespaceConstant.XSLT)) {
    		if (fingerprint==-1) return false; 	// all names are pre-registered
    		StyleElement e = makeXSLElement(fingerprint);
    		if (e!=null) return e.isInstruction();
    	}

		if (uri.equals(NamespaceConstant.SAXON)) {
			if (fingerprint==-1) return false;	// all names are pre-registered
	    	StyleElement e = makeSaxonElement(fingerprint);
	    	if (e!=null) return e.isInstruction();
	    }
        if (!allowExtensions) {
            // extension elements are disabled
            return false;
        }
    	short uriCode = namePool.getCodeForURI(uri);
        ExtensionElementFactory factory = getFactory(uriCode);
        if (factory==null) return false;
        Class actualClass = factory.getExtensionClass(localName);
        return (actualClass != null);

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
