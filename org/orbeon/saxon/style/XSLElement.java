package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.Name;
import net.sf.saxon.om.NamespaceException;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.om.Validation;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.tree.AttributeCollection;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.Configuration;

import javax.xml.transform.TransformerConfigurationException;


/**
* An xsl:element element in the stylesheet. <br>
*/

public class XSLElement extends StyleElement {

    private Expression elementName;
    private Expression namespace = null;
    private String use;
    private AttributeSet[] attributeSets = null;
    private int validation;
    private SchemaType schemaType = null;
    private boolean inheritNamespaces = true;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();

		String nameAtt = null;
		String namespaceAtt = null;
		String validationAtt = null;
		String typeAtt = null;
        String inheritAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.NAMESPACE) {
        		namespaceAtt = atts.getValue(a);
        	} else if (f==StandardNames.VALIDATION) {
        		validationAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.TYPE) {
        		typeAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.INHERIT_NAMESPACES) {
                inheritAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.USE_ATTRIBUTE_SETS) {
        		use = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
        } else {
            elementName = makeAttributeValueTemplate(nameAtt);
            if (elementName instanceof StringValue) {
                if (!Name.isQName(nameAtt)) {
                    compileError("Element name is not a valid QName");
                    // to prevent duplicate error messages:
                    elementName = new StringValue("saxon-error-element");
                }
            }
        }

        if (namespaceAtt!=null) {
            namespace = makeAttributeValueTemplate(namespaceAtt);
        }

        if (validationAtt!=null) {
            validation = Validation.getCode(validationAtt);
            if (validation != Validation.STRIP && !getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("To perform validation, a schema-aware XSLT processor is needed");
            }
            if (validation == Validation.INVALID) {
                compileError("Invalid value for validation attribute. " +
                             "Permitted values are (strict, lax, preserve, strip)");
            }
        } else {
            validation = getContainingStylesheet().getDefaultValidation();
        }

        if (typeAtt!=null) {
            if (!getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("The type attribute is available only with a schema-aware XSLT processor");
            }
            schemaType = getSchemaType(typeAtt);
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("validation and type attributes are mutually exclusive");
        }

        if (inheritAtt != null) {
            if (inheritAtt.equals("yes")) {
                inheritNamespaces = true;
            } else if (inheritAtt.equals("no")) {
                inheritNamespaces = false;
            } else {
                compileError("The inherit-namespaces attribute has permitted values (yes, no)");
            }
        }
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        if (use!=null) {
            attributeSets = getAttributeSets(use, null);        // find any referenced attribute sets
        }
        elementName = typeCheck("name", elementName);
        namespace = typeCheck("namespace", namespace);
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {

        NamespaceResolver nsContext = null;

        // deal specially with the case where the element name is known statically

        if (elementName instanceof StringValue) {
            String qName = ((StringValue)elementName).getStringValue();

            String[] parts;
            try {
                parts = Name.getQNameParts(qName);
            } catch (QNameException e) {
                compileError("Invalid element name: " + qName);
                return null;
            }

            String nsuri = null;
            if (namespace instanceof StringValue) {
                nsuri = ((StringValue)namespace).getStringValue();
                if (nsuri.equals("")) {
                    parts[0] = "";
                }
            } else if (namespace==null) {
                try {
          		    nsuri = getURIForPrefix(parts[0], true);
          		} catch (NamespaceException err) {
          		    compileError(err.getMessage());
          		    return null;
          		}
            }
            if (nsuri != null) {
                int nameCode = getTargetNamePool().allocate(parts[0], nsuri, parts[1]);
                FixedElement inst = new FixedElement(nameCode,
                                                     null,
                                                     attributeSets,
                                                     inheritNamespaces,
                                                     schemaType,
                                                     validation);
                compileChildren(exec, inst, true);
                ExpressionTool.makeParentReferences(inst);
                return inst;
            }
        } else {
            // if the namespace URI must be deduced at run-time from the element name
            // prefix, we need to save the namespace context of the instruction

            if (namespace==null) {
                nsContext = makeNamespaceContext();
            }
        }

        Element inst = new Element( elementName,
                                        namespace,
                                        nsContext,
                                        attributeSets,
                                        schemaType,
                                        validation);
        compileChildren(exec, inst, true);
        ExpressionTool.makeParentReferences(inst);
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
