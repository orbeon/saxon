package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.type.ItemType;

import javax.xml.transform.TransformerConfigurationException;


/**
* Handler for xsl:when elements in stylesheet. <br>
* The xsl:while element has a mandatory attribute test, a boolean expression.
*/

public class XSLWhen extends StyleElement {

    private Expression test;

    public Expression getCondition() {
        return test;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return getCommonChildItemType();
    }

    public void prepareAttributes() throws TransformerConfigurationException {
        String testAtt=null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.TEST) {
        		testAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (testAtt==null) {
            reportAbsence("test");
        } else {
            test = makeExpression(testAtt);
        }
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void validate() throws TransformerConfigurationException {
        if (!(getParent() instanceof XSLChoose)) {
            compileError("xsl:when must be immediately within xsl:choose", "XT0010");
        }
        test = typeCheck("test", test);
    }

    /**
    * Mark tail-recursive calls on stylesheet functions. For most instructions, this does nothing.
    */

    public void markTailCalls() {
        StyleElement last = getLastChildInstruction();
        if (last != null) {
            last.markTailCalls();
        }
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        return null;
        // compilation is handled from the xsl:choose element
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
