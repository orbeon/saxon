package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.EmptySequence;

import javax.xml.transform.TransformerConfigurationException;


/**
 * An xsl:sequence element in the stylesheet. <br>
 * The xsl:sequence element takes attributes:<ul>
 * <li>a mandatory attribute select="expression".</li>
 * </ul>
 */

public final class XSLSequence extends StyleElement {

    private Expression select;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return select.getItemType();
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return false;
    }

    /**
    * Determine whether this type of element is allowed to contain an xsl:fallback
    * instruction
    */

    public boolean mayContainFallback() {
        return true;
    }

    public void prepareAttributes() throws TransformerConfigurationException {

		String selectAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        } else {
            reportAbsence(StandardNames.SELECT);
            select = EmptySequence.getInstance();
        }
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) break;
            if (!(child instanceof XSLFallback)) {
                compileError("The only child node allowed for xsl:sequence is an xsl:fallback instruction", "XT0010");
                break;
            }
        }
        select = typeCheck("select", select);
    }

    /**
    * Mark tail-recursive calls on templates and functions.
    */

    public void markTailCalls() {
        StyleElement last = getLastChildInstruction();
        if (last != null) {
            last.markTailCalls();
        } else if (select != null) {
            ExpressionTool.markTailFunctionCalls(select);
        }
    }


    public Expression compile(Executable exec) {
        return select;
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
