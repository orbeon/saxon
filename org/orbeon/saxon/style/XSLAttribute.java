package net.sf.saxon.style;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.instruct.Attribute;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.FixedAttribute;
import net.sf.saxon.om.*;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.value.StringValue;

import javax.xml.transform.TransformerConfigurationException;

/**
* xsl:attribute element in stylesheet. <br>
*/

public final class XSLAttribute extends XSLStringConstructor {

    private Expression attributeName;
    private Expression separator;
    private Expression namespace = null;
    private int validationAction = Validation.PRESERVE;
    private SimpleType schemaType;

    public void prepareAttributes() throws TransformerConfigurationException {

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
        		selectAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.SEPARATOR) {
        		separatorAtt = atts.getValue(a).trim();
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
            if (!Name.isQName(((StringValue)attributeName).getStringValue())) {
                compileError("Attribute name is not a valid QName", "XT0850");
                // prevent a duplicate error message...
                attributeName = new StringValue("saxon-error-attribute");
            }
        }


        if (namespaceAtt!=null) {
            namespace = makeAttributeValueTemplate(namespaceAtt);
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }

        if (separatorAtt == null) {
            if (selectAtt == null) {
                separator = StringValue.EMPTY_STRING;
            } else {
                separator = new StringValue(" ");
            }
        } else {
            separator = makeAttributeValueTemplate(separatorAtt);
        }

        if (validationAtt!=null) {
            if (validationAction != Validation.STRIP && !getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("To perform validation, a schema-aware XSLT processor is needed", "XT1660");
            }
            validationAction = Validation.getCode(validationAtt);
            if (validationAction == Validation.INVALID) {
                compileError("Invalid value of validation attribute", "XT0020");
            }
        }

        if (typeAtt!=null) {
            if (!getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("The type attribute is available only with a schema-aware XSLT processor", "XT1660");
            }
            SchemaType type = getSchemaType(typeAtt);
            if (type == null) {
                compileError("Unknown attribute type " + typeAtt, "XT1520");
            } else {
                if (!type.isSimpleType()) {
                    compileError("Type annotation for attributes must be a simple type", "XT1530");
                }
                schemaType = (SimpleType)type;
            }
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("The validation and type attributes are mutually exclusive", "XT1505");
        }
    }

    public void validate() throws TransformerConfigurationException {
        if (!(getParent() instanceof XSLAttributeSet)) {
            checkWithinTemplate();
        }
        attributeName = typeCheck("name", attributeName);
        namespace = typeCheck("namespace", namespace);
        select = typeCheck("select", select);
        separator = typeCheck("separator", separator);
        super.validate();
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        NamespaceResolver nsContext = null;

        int annotation = getTypeAnnotation(schemaType);

        // deal specially with the case where the attribute name is known statically

        if (attributeName instanceof StringValue) {
            String qName = ((StringValue)attributeName).getStringValue().trim();
            String[] parts;
            try {
                parts = Name.getQNameParts(qName);
            } catch (QNameException e) {
                // This can't happen, because of previous checks, but we'll behave as if it can
                compileError("Invalid attribute name: " + qName, "XT0850");
                return null;
            }

            if (qName.equals("xmlns")) {
                if (namespace==null) {
                    compileError("Invalid attribute name: " + qName, "XT0855");
                    return null;
                }
            }
            if (parts[0].equals("xmlns")) {
                if (namespace==null) {
                    compileError("Invalid attribute name: " + qName);
                    return null;
                } else {
                    // ignore the prefix "xmlns"
                    parts[0] = "";
                }
            }
            if (namespace==null) {
                String nsuri = "";
                if (!parts[0].equals("")) {
                    nsuri = getURIForPrefix(parts[0], false);
                    if (nsuri == null) {
                        undeclaredNamespaceError(parts[0], "XT0280");
                        return null;
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
                                        annotation);
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
