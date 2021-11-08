package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StringLiteral;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.ValueOf;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;



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


    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAttribute = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAttribute==null) {
            reportAbsence("name");
        }
    }

    public void validate() throws XPathException {
        //checkWithinTemplate();
        checkEmpty();
    }

    public Expression compile(Executable exec) throws XPathException {
        ValueOf text = new ValueOf(new StringLiteral('&' + nameAttribute + ';'), true, false);
//        try {
//            text.setSelect(new StringValue('&' + nameAttribute + ';'));
//        } catch (StaticError err) {
//            compileError(err);
//        }
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
