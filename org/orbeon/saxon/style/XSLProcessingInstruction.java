package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.ProcessingInstruction;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.value.StringValue;

import javax.xml.transform.TransformerConfigurationException;

/**
* An xsl:processing-instruction element in the stylesheet.
*/

public class XSLProcessingInstruction extends XSLStringConstructor {

    Expression name;

    public void prepareAttributes() throws TransformerConfigurationException {

        String nameAtt = null;
        String selectAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAtt = atts.getValue(a).trim();
       	    } else if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
        } else {
            name = makeAttributeValueTemplate(nameAtt);
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        name = typeCheck("name", name);
        select = typeCheck("select", select);
        super.validate();
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        ProcessingInstruction inst = new ProcessingInstruction(name);
        compileContent(exec, inst, StringValue.SINGLE_SPACE);
        //inst.setSeparator(new StringValue(select==null ? "" : " "));
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
