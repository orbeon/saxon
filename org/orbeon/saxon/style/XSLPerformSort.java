package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.SortExpression;
import org.orbeon.saxon.sort.SortKeyDefinition;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;


/**
* Handler for xsl:perform-sort elements in stylesheet (XSLT 2.0). <br>
*/

public class XSLPerformSort extends StyleElement {

    Expression select = null;

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
        if (select==null) {
            return getCommonChildItemType();
        } else {
            return select.getItemType();
        }
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

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }

    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        checkSortComesFirst(true);

        if (select != null) {
            // if there is a select attribute, check that there are no children other than xsl:sort and xsl:fallback
            AxisIterator kids = iterateAxis(Axis.CHILD);
            while (true) {
                NodeInfo child = (NodeInfo)kids.next();
                if (child == null) {
                    break;
                }
                if (child instanceof XSLSort || child instanceof XSLFallback) {
                    // no action
                } else if (child.getNodeKind() == Type.TEXT && !Navigator.isWhite(child.getStringValue())) {
                        // with xml:space=preserve, white space nodes may still be there
                        // ERR XT1040
                    compileError("Within xsl:perform-sort, significant text must not appear if there is a select attribute");
                } else {
                    // ERR XT1040
                    ((StyleElement)child).compileError("Within xsl:perform-sort, child instructions are not allowed if there is a select attribute");
                }
            }
        }
        select = typeCheck("select", select);
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        SortKeyDefinition[] sortKeys = makeSortKeys();
        if (select != null) {
            SortExpression sortedSequence = new SortExpression(select, sortKeys);
            ExpressionTool.makeParentReferences(sortedSequence);
            return sortedSequence;
        } else {
            Expression body = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
            if (body == null) {
                body = EmptySequence.getInstance();
            }
            try {
                SortExpression sortedSequence =  new SortExpression(body.simplify(getStaticContext()), sortKeys);
                ExpressionTool.makeParentReferences(sortedSequence);
                return sortedSequence;
            } catch (XPathException e) {
                compileError(e);
                return null;
            }
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
