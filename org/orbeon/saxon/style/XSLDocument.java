package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.instruct.Instruction;
import net.sf.saxon.instruct.ResultDocument;
import net.sf.saxon.instruct.DocumentInstr;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.NamespaceException;
import net.sf.saxon.om.Validation;
import net.sf.saxon.tree.AttributeCollection;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.Configuration;

import javax.xml.transform.TransformerConfigurationException;
import java.util.Properties;

/**
* An xsl:document instruction in the stylesheet. <BR>
* This instruction creates a document node in the result tree, optionally
 * validating it.
*/

public class XSLDocument extends StyleElement {

    private int validationAction = Validation.STRIP;
    private SchemaType schemaType = null;

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

        String validationAtt = null;
        String typeAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
            if (f==StandardNames.VALIDATION) {
                validationAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.TYPE) {
                typeAtt = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (validationAtt==null) {
            validationAction = getContainingStylesheet().getDefaultValidation();
        } else {
            validationAction = Validation.getCode(validationAtt);
            if (validationAction != Validation.STRIP && !getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("To perform validation, a schema-aware XSLT processor is needed");
            }
            if (validationAction == Validation.INVALID) {
                compileError("Invalid value of validation attribute");
            }
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
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {

        DocumentInstr inst = new DocumentInstr(false, null, getBaseURI());
        inst.setValidationAction(validationAction);
        inst.setSchemaType(schemaType);

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
// Additional Contributor(s): Brett Knights [brett@knightsofthenet.com]
//
