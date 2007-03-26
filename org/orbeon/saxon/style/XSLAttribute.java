package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.Attribute;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.FixedAttribute;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.SimpleType;
import org.orbeon.saxon.value.AnyURIValue;
import org.orbeon.saxon.value.StringValue;

/**
* xsl:attribute element in stylesheet. <br>
*/

public final class XSLAttribute extends XSLStringConstructor {

    private Expression attributeName;
    private Expression separator;
    private Expression namespace = null;
    private int validationAction = Validation.PRESERVE;
    private SimpleType schemaType;

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		String nameAtt = null;
		String namespaceAtt = null;
        String selectAtt = null;
        String separatorAtt = null;
		String validationAtt = null;
		String typeAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.NAMESPACE) {
        		namespaceAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
        	} else if (f==StandardNames.SEPARATOR) {
        		separatorAtt = atts.getValue(a);
        	} else if (f==StandardNames.VALIDATION) {
        		validationAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.TYPE) {
        		typeAtt = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
            return;
        }
        attributeName = makeAttributeValueTemplate(nameAtt);
        if (attributeName instanceof StringValue) {
            if (!getConfiguration().getNameChecker().isQName(((StringValue)attributeName).getStringValue())) {
                invalidAttributeName("Attribute name " + Err.wrap(nameAtt) + " is not a valid QName");
            }
            if (nameAtt.equals("xmlns")) {
                if (namespace==null) {
                    invalidAttributeName("Invalid attribute name: xmlns");
                }
            }
            if (nameAtt.startsWith("xmlns:")) {
                if (namespaceAtt == null) {
                    invalidAttributeName("Invalid attribute name: " + Err.wrap(nameAtt));
                } else {
                    // ignore the prefix "xmlns"
                    nameAtt = nameAtt.substring(6);
                    attributeName = new StringValue(nameAtt);
                }
            }
        }


        if (namespaceAtt!=null) {
            namespace = makeAttributeValueTemplate(namespaceAtt);
            if (namespace instanceof StringValue) {
                if (!AnyURIValue.isValidURI(((StringValue)namespace).getStringValue())) {
                    compileError("The value of the namespace attribute must be a valid URI", "XTDE0865");
                }
            }
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }

        if (separatorAtt == null) {
            if (selectAtt == null) {
                separator = StringValue.EMPTY_STRING;
            } else {
                separator = StringValue.SINGLE_SPACE;
            }
        } else {
            separator = makeAttributeValueTemplate(separatorAtt);
        }

        if (validationAtt!=null) {
            validationAction = Validation.getCode(validationAtt);
            if (validationAction != Validation.STRIP && !getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
                validationAction = getContainingStylesheet().getDefaultValidation();
            }
            if (validationAction == Validation.INVALID) {
                compileError("Invalid value of validation attribute", "XTSE0020");
                validationAction = getContainingStylesheet().getDefaultValidation();
            }
        } else {
            validationAction = getContainingStylesheet().getDefaultValidation();
        }

        if (typeAtt!=null) {
            if (!getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError(
                        "The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
            } else {
                SchemaType type = getSchemaType(typeAtt);
                if (type == null) {
                    compileError("Unknown attribute type " + typeAtt, "XTSE1520");
                } else {
                    if (type.isSimpleType()) {
                        schemaType = (SimpleType)type;
                    } else {
                        compileError("Type annotation for attributes must be a simple type", "XTSE1530");
                        type = null;
                    }
                }
            }
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("The validation and type attributes are mutually exclusive", "XTSE1505");
            validationAction = getContainingStylesheet().getDefaultValidation();
            schemaType = null;
        }
    }

    private void invalidAttributeName(String message) throws XPathException {
//        if (forwardsCompatibleModeIsEnabled()) {
//            DynamicError err = new DynamicError(message);
//            err.setErrorCode("XTDE0850");
//            err.setLocator(this);
//            attributeName = new ErrorExpression(err);
//        } else {
            compileError(message, "XTDE0850");
            // prevent a duplicate error message...
            attributeName = new StringValue("saxon-error-attribute");
//        }
    }

    public void validate() throws XPathException {
        if (!(getParent() instanceof XSLAttributeSet)) {
            checkWithinTemplate();
        }
        if (schemaType != null) {
            if (schemaType.isNamespaceSensitive()) {
                compileError("Validation at attribute level must not specify a " +
                        "namespace-sensitive type (xs:QName or xs:NOTATION)", "XTTE1545");
            }
        }
        attributeName = typeCheck("name", attributeName);
        namespace = typeCheck("namespace", namespace);
        select = typeCheck("select", select);
        separator = typeCheck("separator", separator);
        super.validate();
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     *
     * @return the error code defined for this condition, for this particular instruction
     */

    protected String getErrorCodeForSelectPlusContent() {
        return "XTSE0840";
    }

    public Expression compile(Executable exec) throws XPathException {
        NamespaceResolver nsContext = null;

        int annotation = getTypeAnnotation(schemaType);

        // deal specially with the case where the attribute name is known statically

        if (attributeName instanceof StringValue) {
            String qName = ((StringValue)attributeName).getStringValue().trim();
            String[] parts;
            try {
                parts = getConfiguration().getNameChecker().getQNameParts(qName);
            } catch (QNameException e) {
                // This can't happen, because of previous checks
                return null;
            }

            if (namespace==null) {
                String nsuri = "";
                if (!parts[0].equals("")) {
                    nsuri = getURIForPrefix(parts[0], false);
                    if (nsuri == null) {
                        undeclaredNamespaceError(parts[0], "XTSE0280");
                        return null;
                    }
                }
                int nameCode = getTargetNamePool().allocate(parts[0], nsuri, parts[1]);
                FixedAttribute inst = new FixedAttribute(nameCode,
                                                         validationAction,
                                                         schemaType,
                                                         annotation);
                inst.setParentExpression(this);     // temporarily
                compileContent(exec, inst, separator);
                //inst.setSeparator(separator);
                ExpressionTool.makeParentReferences(inst);
                return inst;
            } else if (namespace instanceof StringValue) {
                String nsuri = ((StringValue)namespace).getStringValue();
                if (nsuri.equals("")) {
                    parts[0] = "";
                } else if (parts[0].equals("")) {
                    // Need to choose an arbitrary prefix
                    // First see if the requested namespace is declared in the stylesheet
                    AxisIterator iter = iterateAxis(Axis.NAMESPACE);
                    while (true) {
                        NodeInfo ns = (NodeInfo)iter.next();
                        if (ns == null) {
                            break;
                        }
                        if (ns.getStringValue().equals(nsuri)) {
                            parts[0] = ns.getLocalPart();
                            break;
                        }
                    }
                    // Otherwise see the URI is known to the namepool
                    if (parts[0].equals("")) {
                        String p = getTargetNamePool().suggestPrefixForURI(
                                ((StringValue)namespace).getStringValue());
                        if (p != null) {
                            parts[0] = p;
                        }
                    }
                    // Otherwise choose something arbitrary. This will get changed
                    // if it clashes with another attribute
                    if (parts[0].equals("")) {
                        parts[0] = "ns0";
                    }
                }
                int nameCode = getTargetNamePool().allocate(parts[0], nsuri, parts[1]);
                FixedAttribute inst = new FixedAttribute(nameCode,
                                                         validationAction,
                                                         schemaType,
                                                         annotation);
                compileContent(exec, inst, separator);
                //inst.setSeparator(separator);
                ExpressionTool.makeParentReferences(inst);
                return inst;
            }
        } else {
            // if the namespace URI must be deduced at run-time from the attribute name
            // prefix, we need to save the namespace context of the instruction

            if (namespace==null) {
                nsContext = makeNamespaceContext();
            }
        }

        Attribute inst = new Attribute( attributeName,
                                        namespace,
                                        nsContext,
                                        validationAction,
                                        schemaType,
                                        annotation,
                                        false);
        compileContent(exec, inst, separator);
        return inst;
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
