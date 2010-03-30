package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.CopyOf;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.value.Whitespace;


/**
* An xsl:copy-of element in the stylesheet. <br>
*/

public final class XSLCopyOf extends StyleElement {

    private Expression select;
    private boolean copyNamespaces;
    private int validation = Validation.PRESERVE;
    private SchemaType schemaType;
    private boolean readOnce = false;       // extension attribute to enable serial processing

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();
		String selectAtt = null;
		String copyNamespacesAtt = null;
		String validationAtt = null;
		String typeAtt = null;
        String readOnceAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
            } else if (f==StandardNames.COPY_NAMESPACES) {
                copyNamespacesAtt = Whitespace.trim(atts.getValue(a));
            } else if (f==StandardNames.VALIDATION) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f==StandardNames.TYPE) {
                typeAtt = Whitespace.trim(atts.getValue(a));
            } else if (f==StandardNames.SAXON_READ_ONCE) {
                readOnceAtt = Whitespace.trim(atts.getValue(a));
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
                compileError("Value of copy-namespaces must be 'yes' or 'no'", "XTSE0020");
            }
        }

        if (validationAtt!=null) {
            validation = Validation.getCode(validationAtt);
            if (validation != Validation.STRIP && !getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
            }
            if (validation == Validation.INVALID) {
                compileError("invalid value of validation attribute", "XTSE0020");
            }
        } else {
            validation = getContainingStylesheet().getDefaultValidation();
        }

        if (typeAtt!=null) {
            schemaType = getSchemaType(typeAtt);
            if (!getConfiguration().isSchemaAware(Configuration.XSLT)) {
                compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
            }
            validation = Validation.BY_TYPE;
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("The @validation and @type attributes are mutually exclusive", "XTSE1505");
        }

//        if (validation == Validation.PRESERVE && !copyNamespaces) {
//            // this is an error only when copying namespace-sensitive content
//            compileError("copy-namespaces must be set to 'yes' when validation is set to 'preserve'", "XTTE0950");
//        }

        if (readOnceAtt != null) {
            if (readOnceAtt.equals("yes")) {
                readOnce = true;
            } else if (readOnceAtt.equals("no")) {
                readOnce = false;
            } else {
                compileError("saxon:read-once attribute must be 'yes' or 'no'");
            }
        }
    }

    public void validate() throws XPathException {
        checkEmpty();
        select = typeCheck("select", select);
    }

    public Expression compile(Executable exec) {
        CopyOf inst = new CopyOf(select, copyNamespaces, validation, schemaType, false);
        if (readOnce) {
            inst.setReadOnce(readOnce);
        }
        inst.setCopyLineNumbers(exec.getConfiguration().isLineNumbering());
        inst.setStaticBaseUri(getBaseURI());
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
