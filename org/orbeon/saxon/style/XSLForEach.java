package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.ForEach;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.sort.SortExpression;
import org.orbeon.saxon.sort.SortKeyDefinition;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.EmptySequence;

import javax.xml.transform.TransformerConfigurationException;


/**
* Handler for xsl:for-each elements in stylesheet. <br>
*/

public class XSLForEach extends StyleElement {

    Expression select = null;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Specify that xsl:sort is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return (child instanceof XSLSort);
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return getCommonChildItemType();
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

		String selectAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt==null) {
            reportAbsence("select");
        } else {
            select = makeExpression(selectAtt);
        }

    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        checkSortComesFirst(false);
        select = typeCheck("select", select);
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        SortKeyDefinition[] sortKeys = makeSortKeys();
        Expression sortedSequence = select;
        if (sortKeys != null) {
            sortedSequence = new SortExpression(select, sortKeys);
            ExpressionTool.makeParentReferences(sortedSequence);
        }
//        Block block = new Block();
//        compileChildren(exec, block, true);
        Expression block = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
        if (block == null) {
            // body of for-each is empty: it's a no-op.
            return EmptySequence.getInstance();
        }
        try {
            ForEach inst = new ForEach(sortedSequence, block.simplify(getStaticContext()));
            ExpressionTool.makeParentReferences(inst);
            return inst;
        } catch (XPathException err) {
            compileError(err);
            return null;
        }
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
