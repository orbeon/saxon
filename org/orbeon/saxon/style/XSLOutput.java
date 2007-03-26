package org.orbeon.saxon.style;
import org.orbeon.saxon.event.SaxonOutputKeys;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.OutputKeys;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

/**
* An xsl:output element in the stylesheet.
*/

public class XSLOutput extends StyleElement {

    private int fingerprint = -1;
    private String method = null;
    private String version = null;
    private String indent = null;
    private String encoding = null;
    private String mediaType = null;
    private String doctypeSystem = null;
    private String doctypePublic = null;
    private String omitDeclaration = null;
    private String standalone = null;
    private String cdataElements = null;
    private String includeContentType = null;
    private String nextInChain = null;
    private String representation = null;
    private String indentSpaces = null;
    private String byteOrderMark = null;
    private String escapeURIAttributes = null;
    private String normalizationForm = null;
    private String requireWellFormed = null;
    private String undeclareNamespaces = null;
    private String useCharacterMaps = null;
    private HashMap userAttributes = null;

    //Emitter handler = null;

    public void prepareAttributes() throws XPathException {
		AttributeCollection atts = getAttributeList();
		String nameAtt = null;

        for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAtt = atts.getValue(a).trim();
			} else if (f==StandardNames.METHOD) {
        		method = atts.getValue(a).trim();
        	} else if (f==StandardNames.VERSION) {
        		version = atts.getValue(a).trim();
        	} else if (f==StandardNames.BYTE_ORDER_MARK) {
                byteOrderMark = atts.getValue(a).trim();
            } else if (f==StandardNames.ENCODING) {
        		encoding = atts.getValue(a).trim();
        	} else if (f==StandardNames.OMIT_XML_DECLARATION) {
        		omitDeclaration = atts.getValue(a).trim();
        	} else if (f==StandardNames.STANDALONE) {
        		standalone = atts.getValue(a).trim();
        	} else if (f==StandardNames.DOCTYPE_PUBLIC) {
        		doctypePublic = atts.getValue(a).trim();
        	} else if (f==StandardNames.DOCTYPE_SYSTEM) {
        		doctypeSystem = atts.getValue(a).trim();
        	} else if (f==StandardNames.CDATA_SECTION_ELEMENTS) {
        		cdataElements = atts.getValue(a);
        	} else if (f==StandardNames.INDENT) {
        		indent = atts.getValue(a).trim();
        	} else if (f==StandardNames.MEDIA_TYPE) {
        		mediaType = atts.getValue(a).trim();
        	} else if (f==StandardNames.INCLUDE_CONTENT_TYPE) {
        		includeContentType = atts.getValue(a).trim();
        	} else if (f==StandardNames.NORMALIZATION_FORM) {
        		normalizationForm = atts.getValue(a).trim();
        	} else if (f==StandardNames.ESCAPE_URI_ATTRIBUTES) {
        		escapeURIAttributes = atts.getValue(a).trim();
            } else if (f==StandardNames.USE_CHARACTER_MAPS) {
        		useCharacterMaps = atts.getValue(a);
            } else if (f==StandardNames.UNDECLARE_PREFIXES) {
        		undeclareNamespaces = atts.getValue(a);
            } else if (f==StandardNames.SAXON_CHARACTER_REPRESENTATION) {
        		representation = atts.getValue(a).trim();
        	} else if (f==StandardNames.SAXON_INDENT_SPACES) {
        		indentSpaces = atts.getValue(a).trim();
        	} else if (f==StandardNames.SAXON_NEXT_IN_CHAIN) {
        		nextInChain = atts.getValue(a).trim();
            } else if (f==StandardNames.SAXON_REQUIRE_WELL_FORMED) {
                requireWellFormed = atts.getValue(a).trim();
        	} else {
        	    String attributeURI = getNamePool().getURI(nc);
        	    if ("".equals(attributeURI) ||
        	            NamespaceConstant.XSLT.equals(attributeURI) ||
        	            NamespaceConstant.SAXON.equals(attributeURI)) {
        		    checkUnknownAttribute(nc);
        		} else {
        		    String name = '{' + attributeURI + '}' + atts.getLocalName(a);
        		    if (userAttributes==null) {
        		        userAttributes = new HashMap(5);
        		    }
        		    userAttributes.put(name, atts.getValue(a));
        		}
        	}
        }
        if (nameAtt!=null) {
            try {
                fingerprint = makeNameCode(nameAtt.trim()) & 0xfffff;
            } catch (NamespaceException err) {
                compileError(err.getMessage(), "XTSE1570");
            } catch (XPathException err) {
                compileError(err.getMessage(), "XTSE1570");
            }
        }
    }

    /**
     * Get the name of the xsl:output declaration
     * @return the name, as a namepool fingerprint; or -1 for an unnamed output declaration
     */

    public int getOutputFingerprint() {
        return fingerprint;
    }

    public void validate() throws XPathException {
        checkTopLevel(null);
        checkEmpty();
    }

    public Expression compile(Executable exec) {
        return null;
    }


    /**
    * Validate the properties,
    * and return the values as additions to a supplied Properties object.
    */

    protected void gatherOutputProperties(Properties details, HashMap precedences)
    throws XPathException {

        if (method != null) {
            if (method.equals("xml") || method.equals("html") ||
                    method.equals("text") || method.equals("xhtml"))  {
                checkAndPut(OutputKeys.METHOD, method, details, precedences);
                //details.put(OutputKeys.METHOD, method);
            } else {
                String[] parts;
                try {
                    parts = getConfiguration().getNameChecker().getQNameParts(method);
                    String prefix = parts[0];
                    if (prefix.equals("")) {
                        compileError("method must be xml, html, xhtml, or text, or a prefixed name", "XTSE1570");
                    } else {
                        String uri = getURIForPrefix(prefix, false);
                        if (uri == null) {
                            undeclaredNamespaceError(prefix, "XTSE0280");
                        }
                        checkAndPut(OutputKeys.METHOD, '{' + uri + '}' + parts[1], details, precedences);
                        //details.put(OutputKeys.METHOD, '{' + uri + '}' + parts[1] );
                    }
                } catch (QNameException e) {
                    compileError("Invalid method name. " + e.getMessage(), "XTSE1570");
                }
            }
        }

        if (byteOrderMark != null) {
            if (byteOrderMark.equals("yes") || byteOrderMark.equals("no")) {
                checkAndPut(SaxonOutputKeys.BYTE_ORDER_MARK, byteOrderMark, details, precedences);
                //details.put(SaxonOutputKeys.BYTE_ORDER_MARK, byteOrderMark);
            } else {
                compileError("byte-order-mark value must be 'yes' or 'no'", "XTSE0020");
            }
        }

        if (version != null) {
            checkAndPut(OutputKeys.VERSION, version, details, precedences);
            //details.put(OutputKeys.VERSION, version);
        }

        if (indent != null) {
            if (indent.equals("yes") || indent.equals("no")) {
                //details.put(OutputKeys.INDENT, indent);
                checkAndPut(OutputKeys.INDENT, indent, details, precedences);
            } else {
                compileError("indent value must be 'yes' or 'no'", "XTSE0020");
            }
        }

        if (indentSpaces != null) {
            try {
                Integer.parseInt(indentSpaces);
                details.put(OutputKeys.INDENT, "yes");
                checkAndPut(SaxonOutputKeys.INDENT_SPACES, indentSpaces, details, precedences);
                //details.put(SaxonOutputKeys.INDENT_SPACES, indentSpaces);
            } catch (NumberFormatException err) {
                compileWarning("saxon:indent-spaces must be an integer. Using default value (3).",
                        SaxonErrorCode.SXWN9002);
            }
        }

        if (encoding != null) {
            checkAndPut(OutputKeys.ENCODING, encoding, details, precedences);
            //details.put(OutputKeys.ENCODING, encoding);
        }

        if (mediaType != null) {
            checkAndPut(OutputKeys.MEDIA_TYPE, mediaType, details, precedences);
            //details.put(OutputKeys.MEDIA_TYPE, mediaType);
        }

        if (doctypeSystem != null) {
            checkAndPut(OutputKeys.DOCTYPE_SYSTEM, doctypeSystem, details, precedences);
            //details.put(OutputKeys.DOCTYPE_SYSTEM, doctypeSystem);
        }

        if (doctypePublic != null) {
            checkAndPut(OutputKeys.DOCTYPE_PUBLIC, doctypePublic, details, precedences);
            //details.put(OutputKeys.DOCTYPE_PUBLIC, doctypePublic);
        }

        if (omitDeclaration != null) {
            if (omitDeclaration.equals("yes") || omitDeclaration.equals("no")) {
                checkAndPut(OutputKeys.OMIT_XML_DECLARATION, omitDeclaration, details, precedences);
                //details.put(OutputKeys.OMIT_XML_DECLARATION, omitDeclaration);
            } else {
                compileError("omit-xml-declaration attribute must be 'yes' or 'no'", "XTSE0020");
            }
        }

        if (standalone != null) {
            if (standalone.equals("yes") || standalone.equals("no") || standalone.equals("omit")) {
                checkAndPut(OutputKeys.STANDALONE, standalone, details, precedences);
                //details.put(OutputKeys.STANDALONE, standalone);
            } else {
                compileError("standalone attribute must be 'yes' or 'no' or 'omit'", "XTSE0020");
            }
        }

        if (cdataElements != null) {
            String existing = details.getProperty(OutputKeys.CDATA_SECTION_ELEMENTS);
            if (existing==null) {
                existing = "";
            }
            String s = "";
            StringTokenizer st = new StringTokenizer(cdataElements);
            while (st.hasMoreTokens()) {
                String displayname = st.nextToken();
                try {
                    String[] parts = getConfiguration().getNameChecker().getQNameParts(displayname);
                    String uri = getURIForPrefix(parts[0], true);
                    if (uri == null) {
                        undeclaredNamespaceError(parts[0], "XTSE0280");
                    }
                    s += " {" + uri + '}' + parts[1];
                } catch (QNameException err) {
                    compileError("Invalid CDATA element name. " + err.getMessage(), "XTSE0280");
                }

                details.put(OutputKeys.CDATA_SECTION_ELEMENTS, existing+s);
            }
        }

        if (normalizationForm != null && !normalizationForm.equals("none")) {
            // At this stage we check only that the normalization-form is syntactically valid,
            // not that it is one of the supported values. The latter check is done only if we
            // are actually serializing.
            if (XML11Char.isXML11ValidNmtoken(normalizationForm)) {
//            if (normalizationForm.equals("NFC") || normalizationForm.equals("NFD") ||
//                   normalizationForm.equals("NFKC") || normalizationForm.equals("NFKD") ) {
                checkAndPut(SaxonOutputKeys.NORMALIZATION_FORM, normalizationForm, details, precedences);
            } else {
                compileError("normalization-form must a valid NMTOKEN", "XTSE0020");
            }
        }

        if (undeclareNamespaces != null) {
            if (undeclareNamespaces.equals("yes") || undeclareNamespaces.equals("no")) {
                checkAndPut(SaxonOutputKeys.UNDECLARE_PREFIXES, undeclareNamespaces, details, precedences);
                //details.put(SaxonOutputKeys.UNDECLARE_PREFIXES, undeclareNamespaces);
            } else {
                compileError("undeclare-namespaces value must be 'yes' or 'no'", "XTSE0020");
            }
        }

        if (useCharacterMaps != null) {
            String s = prepareCharacterMaps(this, useCharacterMaps, details);
            details.put(SaxonOutputKeys.USE_CHARACTER_MAPS, s);
        }

        if (representation != null) {
            checkAndPut(SaxonOutputKeys.CHARACTER_REPRESENTATION, representation, details, precedences);
            //details.put(SaxonOutputKeys.CHARACTER_REPRESENTATION, representation);
        }

        if (includeContentType != null) {
            if (includeContentType.equals("yes") || includeContentType.equals("no")) {
                checkAndPut(SaxonOutputKeys.INCLUDE_CONTENT_TYPE, includeContentType, details, precedences);
                //details.put(SaxonOutputKeys.INCLUDE_CONTENT_TYPE, includeContentType);
            } else {
                compileError("include-content-type attribute must be 'yes' or 'no'", "XTSE0020");
            }
        }

        if (escapeURIAttributes != null) {
            if (escapeURIAttributes.equals("yes") || escapeURIAttributes.equals("no")) {
                checkAndPut(SaxonOutputKeys.ESCAPE_URI_ATTRIBUTES, escapeURIAttributes, details, precedences);
                //details.put(SaxonOutputKeys.ESCAPE_URI_ATTRIBUTES, escapeURIAttributes);
            } else {
                compileError("escape-uri-attributes value must be 'yes' or 'no'", "XTSE0020");
            }
        }

        if (nextInChain != null) {
            checkAndPut(SaxonOutputKeys.NEXT_IN_CHAIN, nextInChain, details, precedences);
            checkAndPut(SaxonOutputKeys.NEXT_IN_CHAIN_BASE_URI, getSystemId(), details, precedences);
            //details.put(SaxonOutputKeys.NEXT_IN_CHAIN, nextInChain);
            //details.put(SaxonOutputKeys.NEXT_IN_CHAIN_BASE_URI, getSystemId());
        }

        if (requireWellFormed != null) {
            if (requireWellFormed.equals("yes") || requireWellFormed.equals("no")) {
                checkAndPut(SaxonOutputKeys.REQUIRE_WELL_FORMED, requireWellFormed, details, precedences);
                //details.put(SaxonOutputKeys.REQUIRE_WELL_FORMED, requireWellFormed);
            } else {
                compileWarning("saxon:require-well-formed value must be 'yes' or 'no' (treated as no)",
                        SaxonErrorCode.SXWN9003);
            }
        }

        // deal with user-defined attributes

        if (userAttributes!=null) {
            Iterator iter = userAttributes.keySet().iterator();
            while (iter.hasNext()) {
                String attName = (String)iter.next();
                String data = (String)userAttributes.get(attName);
                details.put(attName, data);
            }
        }

    }

    /**
     * Add an output property to the list of properties after checking that it is consistent
     * with other properties
     */

    public void checkAndPut(String property, String value, Properties props, HashMap precedences)
            throws XPathException {
        String old = props.getProperty(property);
        if (old == null) {
            props.setProperty(property, value);
            precedences.put(property, new Integer(getPrecedence()));
        } else if (old.equals(value)) {
            // do nothing
        } else {
            Integer oldPrec = (Integer)precedences.get(property);
            if (oldPrec == null) return;    // shouldn't happen but ignore it
            int op = oldPrec.intValue();
            if (op > getPrecedence()) {
                // ignore this value, the other has higher precedence
            } else if (op == getPrecedence()) {
                compileError("Conflicting values for output property " + property, "XTSE1560");
            } else {
                // this has higher precedence: can't happen
                throw new IllegalStateException("Output properties must be processed in decreasing precedence order");
            }
        }
    }

    /**
     * Process the use-character-maps attribute
     * @param details The existing output properties
     * @return the augmented value of the use-character-maps attribute in Clark notation
     * @throws XPathException if the value is invalid
     */
    public static String prepareCharacterMaps(StyleElement element,
                                            String useCharacterMaps,
                                            Properties details)
            throws XPathException {
        XSLStylesheet principal = element.getPrincipalStylesheet();
        String existing = details.getProperty(SaxonOutputKeys.USE_CHARACTER_MAPS);
        if (existing==null) {
            existing = "";
        }
        String s = "";
        StringTokenizer st = new StringTokenizer(useCharacterMaps);
        while (st.hasMoreTokens()) {
            String displayname = st.nextToken();
            try {
                String[] parts = element.getConfiguration().getNameChecker().getQNameParts(displayname);
                String uri = element.getURIForPrefix(parts[0], false);
                if (uri == null) {
                    element.undeclaredNamespaceError(parts[0], "XTSE0280");
                }
                int nameCode = element.getTargetNamePool().allocate(parts[0], uri, parts[1]);
                XSLCharacterMap ref =
                        principal.getCharacterMap(nameCode & 0xfffff);
                if (ref == null) {
                    element.compileError("No character-map named '" + displayname + "' has been defined", "XTSE1590");
                }
                s += " {" + uri + '}' + parts[1];
            } catch (QNameException err) {
                element.compileError("Invalid character-map name. " + err.getMessage(), "XTSE1590");
            }

        }
        existing = s + existing;
        return existing;
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
