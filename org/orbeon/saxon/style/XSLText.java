package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.ValueOf;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;

/**
* Handler for xsl:text elements in stylesheet. <BR>
*/

public class XSLText extends XSLStringConstructor {

    private boolean disable = false;
    private StringValue value;

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return NodeKindTest.TEXT;
    }

    public void prepareAttributes() throws TransformerConfigurationException {

        String disableAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.DISABLE_OUTPUT_ESCAPING) {
        		disableAtt = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (disableAtt != null) {
            if (disableAtt.equals("yes")) {
                disable = true;
            } else if (disableAtt.equals("no")) {
                disable = false;
            } else {
                compileError("disable-output-escaping attribute must be either 'yes' or 'no'", "XT0020");
            }
        }
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();

        // 2.0 spec has reverted to the 1.0 rule that xsl:text may not have child elements
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            Item child = kids.next();
            if (child == null) {
                value = StringValue.EMPTY_STRING;
                break;
            } else if (child instanceof StyleElement) {
                compileError("xsl:text must not contain child elements", "XT0010");
                return;
            } else {
                try {
                    value = new StringValue(child.getStringValue());
                } catch (XPathException e) {
                    value = StringValue.EMPTY_STRING;
                }
                break;
            }
        }
        super.validate();
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        ValueOf inst = new ValueOf(value, disable);
        //compileContent(exec, inst);
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
