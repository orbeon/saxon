package net.sf.saxon.style;
import net.sf.saxon.instruct.Instruction;
import net.sf.saxon.instruct.Text;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.tree.AttributeCollection;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.xpath.StaticError;

import javax.xml.transform.TransformerConfigurationException;



/**
* A saxon:entity-ref element in the stylesheet. This causes an entity reference
* to be output to the XML or HTML output stream. <br>
*/

public class SaxonEntityRef extends StyleElement {

    String nameAttribute;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }


    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAttribute = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAttribute==null) {
            reportAbsence("name");
        }
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        checkEmpty();
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        Text text = new Text(true);
        try {
            text.setSelect(new StringValue('&' + nameAttribute + ';'));
        } catch (StaticError err) {
            compileError(err);
        }
        ExpressionTool.makeParentReferences(text);
        return text;
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
