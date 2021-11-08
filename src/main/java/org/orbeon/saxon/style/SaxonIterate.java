package org.orbeon.saxon.style;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.IterateInstr;
import org.orbeon.saxon.expr.Literal;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;

import java.util.ArrayList;
import java.util.List;

/**
* Handler for saxon:iterate elements in stylesheet. <br>
*/

public class SaxonIterate extends StyleElement {

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
        return (child instanceof XSLParam);
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

    protected boolean mayContainParam() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.SELECT)) {
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

    public void validate() throws XPathException {
        //checkParamComesFirst(false);
        select = typeCheck("select", select);
        if (!hasChildNodes()) {
            compileWarning("An empty saxon:iterate instruction has no effect", SaxonErrorCode.SXWN9009);
        }
    }

    public Expression compile(Executable exec) throws XPathException {
        SequenceIterator children = iterateAxis(Axis.CHILD);
        List nonFinallyChildren = new ArrayList();
        Expression finallyExp = null;
        while (true) {
            NodeInfo node = (NodeInfo)children.next();
            if (node == null) {
                break;
            } else if (node instanceof SaxonFinally) {
                finallyExp = ((SaxonFinally)node).compile(exec);
            } else {
                nonFinallyChildren.add(node);
            }
        }
        Expression block = compileSequenceConstructor(exec, new ListIterator(nonFinallyChildren), true);
        if (block == null) {
            // body of saxon:iterate is empty: it's a no-op.
            return Literal.makeEmptySequence();
        }
        try {
            return new IterateInstr(select, makeExpressionVisitor().simplify(block), finallyExp);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

