package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.ValueOf;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.StringValue;

import javax.xml.transform.TransformerConfigurationException;


/**
* An xsl:value-of element in the stylesheet. <br>
* The xsl:value-of element takes attributes:<ul>
* <li>a mandatory attribute select="expression".
* This must be a valid String expression</li>
* <li>an optional disable-output-escaping attribute, value "yes" or "no"</li>
* <li>an optional separator attribute</li>
* </ul>
*/

public final class XSLValueOf extends XSLStringConstructor {

    private boolean disable = false;
    private Expression separator;

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return NodeKindTest.TEXT;
    }

    public void prepareAttributes() throws TransformerConfigurationException {

		String selectAtt = null;
		String disableAtt = null;
		String separatorAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.DISABLE_OUTPUT_ESCAPING) {
        		disableAtt = atts.getValue(a).trim();
			} else if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
			} else if (f==StandardNames.SEPARATOR) {
        		separatorAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }

        if (separatorAtt != null) {
            separator = makeAttributeValueTemplate(separatorAtt);
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
        super.validate();
        checkWithinTemplate();
        select = typeCheck("select", select);
        separator = typeCheck("separator", separator);
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {

        if (separator == null && select != null && backwardsCompatibleModeIsEnabled()) {
            if (!Type.isSubType(select.getItemType(), Type.ANY_ATOMIC_TYPE)) {
                select = new Atomizer(select, getStaticContext().getConfiguration());
            }
            if (Cardinality.allowsMany(select.getCardinality())) {
                select = new FirstItemExpression(select);
            }
            if (!Type.isSubType(select.getItemType(), Type.STRING_TYPE)) {
                select = new AtomicSequenceConverter(select, Type.STRING_TYPE);
            }
        } else {
            if (separator == null) {
                if (select == null) {
                    separator = StringValue.EMPTY_STRING;
                } else {
                    separator = new StringValue(" ");
                }
            }
        }
        ValueOf inst = new ValueOf(select, disable);
        compileContent(exec, inst, separator);
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
