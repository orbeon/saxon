package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.CopyOf;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.type.SchemaType;

import javax.xml.transform.TransformerConfigurationException;


/**
* An xsl:copy-of element in the stylesheet. <br>
*/

public final class XSLCopyOf extends StyleElement {

    private Expression select;
    private boolean copyNamespaces;
    private int validation = Validation.PRESERVE;
    private SchemaType schemaType;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();
		String selectAtt = null;
		String copyNamespacesAtt = null;
		String validationAtt = null;
		String typeAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
            } else if (f==StandardNames.COPY_NAMESPACES) {
                copyNamespacesAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.VALIDATION) {
                validationAtt = atts.getValue(a).trim();
            } else if (f==StandardNames.TYPE) {
                typeAtt = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        } else {
            reportAbsence("select");
        }

        if (copyNamespacesAtt == null) {
            copyNamespaces = true;
        } else {
            if (copyNamespacesAtt.equals("yes")) {
                copyNamespaces = true;
            } else if (copyNamespacesAtt.equals("no")) {
                copyNamespaces = false;
            } else {
                compileError("Value of copy-namespaces must be 'yes' or 'no'", "XT0020");
            }
        }

        if (validationAtt!=null) {
            validation = Validation.getCode(validationAtt);
            if (validation != Validation.STRIP && !getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("To perform validation, a schema-aware XSLT processor is needed", "XT1660");
            }
            if (validation == Validation.INVALID) {
                compileError("invalid value of validation attribute", "XT0020");
            }
        }

        if (typeAtt!=null) {
            schemaType = getSchemaType(typeAtt);
            if (!getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("The type attribute is available only with a schema-aware XSLT processor", "XT1660");
            }
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("The validation and type attributes are mutually exclusive", "XT1505");
        }
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        checkEmpty();
        select = typeCheck("select", select);
    }

    public Expression compile(Executable exec) {
        CopyOf inst = new CopyOf(select, copyNamespaces, validation, schemaType);
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
