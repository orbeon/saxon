package net.sf.saxon.style;
import net.sf.saxon.instruct.*;
import net.sf.saxon.tree.AttributeCollection;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.Axis;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;

/**
* An xsl:choose elements in the stylesheet. <br>
*/

public class XSLChoose extends StyleElement {

    private StyleElement otherwise;
    private int numberOfWhens = 0;

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
        return getCommonChildItemType();
    }

    public void prepareAttributes() throws TransformerConfigurationException {
		AttributeCollection atts = getAttributeList();
		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
        	checkUnknownAttribute(nc);
        }
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof XSLWhen) {
                if (otherwise!=null) {
                    compileError("xsl:otherwise must come last");
                }
                numberOfWhens++;
            } else if (curr instanceof XSLOtherwise) {
                if (otherwise!=null) {
                    compileError("Only one xsl:otherwise allowed in an xsl:choose");
                } else {
                    otherwise = (StyleElement)curr;
                }
            } else if (curr.getNodeKind() == Type.TEXT &&
                        Navigator.isWhite(curr.getStringValue())) {
                compileError("Text node inside xsl:choose");
                // tolerate a whitespace text node; but it should have been stripped
                // by now.
            } else {
                compileError("Only xsl:when and xsl:otherwise are allowed here");
            }
        }

        if (numberOfWhens==0) {
            compileError("xsl:choose must contain at least one xsl:when");
        }
    }

    /**
    * Mark tail-recursive calls on templates and functions.
    */

    public void markTailCalls() {
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                return;
            }
            if (curr instanceof StyleElement) {
                ((StyleElement)curr).markTailCalls();
            }
        }
    }


    public Expression compile(Executable exec) throws TransformerConfigurationException {

        int entries = numberOfWhens + (otherwise==null ? 0 : 1);
        Expression[] conditions = new Expression[entries];
        Expression[] actions = new Expression[entries];

        int w = 0;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof XSLWhen) {
                conditions[w] = ((XSLWhen)curr).getCondition();
                Block b = new Block();
                b.setLocationId(allocateLocationId(getSystemId(), curr.getLineNumber()));
                ((XSLWhen)curr).compileChildren(exec, b, true);
                try {
                    actions[w] = b.simplify(((XSLWhen)curr).getStaticContext());
                } catch (XPathException e) {
                    compileError(e);
                }

                if (getConfiguration().getTraceListener() != null) {
                    TraceWrapper trace = makeTraceInstruction((XSLWhen)curr, actions[w]);
                    trace.setParentExpression(b);
                    actions[w] = trace;
                }

                // Optimize for constant conditions (true or false)
                if (conditions[w] instanceof BooleanValue) {
                    if (((BooleanValue)conditions[w]).getBooleanValue()) {
                        // constant true: truncate the tests here
                        entries = w+1;
                        break;
                    } else {
                        // constant false: omit this test
                        w--;
                        entries--;
                    }
                }
                w++;
            } else if (curr instanceof XSLOtherwise) {
                conditions[w] = BooleanValue.TRUE;
                Block b = new Block();
                b.setLocationId(allocateLocationId(getSystemId(), curr.getLineNumber()));
                ((XSLOtherwise)curr).compileChildren(exec, b, true);
                try {
                    actions[w] = b.simplify(((XSLOtherwise)curr).getStaticContext());
                } catch (XPathException e) {
                    compileError(e);
                }
                if (getConfiguration().getTraceListener() != null) {
                    TraceWrapper trace = makeTraceInstruction((XSLOtherwise)curr, actions[w]);
                    trace.setParentExpression(b);
                    actions[w] = trace;
                }
                w++;
            } else {
                new AssertionError("Expected xsl:when or xsl:otherwise");
            }
        }

        if (conditions.length != entries) {
            // we've optimized some entries away
            if (entries==0) {
                return null; // return a no-op
            }
            if (entries==1 && (conditions[0] instanceof BooleanValue)) {
                if (((BooleanValue)conditions[0]).getBooleanValue()) {
                    // only one condition left, and it's known to be true: return the corresponding action
                    return actions[0];
                } else {
                    // but if it's false, return a no-op
                    return null;
                }
            }
            Expression[] conditions2 = new Expression[entries];
            System.arraycopy(conditions, 0, conditions2, 0, entries);
            Instruction[] actions2 = new Instruction[entries];
            System.arraycopy(actions, 0, actions2, 0, entries);
            conditions = conditions2;
            actions = actions2;
        }

        Choose ch = new Choose(conditions, actions);
        ExpressionTool.makeParentReferences(ch);
        return ch;
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
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
