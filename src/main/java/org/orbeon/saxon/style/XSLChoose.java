package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.Literal;
import org.orbeon.saxon.instruct.Choose;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.TraceWrapper;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.BooleanValue;

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

    public void prepareAttributes() throws XPathException {
		AttributeCollection atts = getAttributeList();
		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
        	checkUnknownAttribute(nc);
        }
    }

    public void validate() throws XPathException {
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof XSLWhen) {
                if (otherwise!=null) {
                    otherwise.compileError("xsl:otherwise must come last", "XTSE0010");
                }
                numberOfWhens++;
            } else if (curr instanceof XSLOtherwise) {
                if (otherwise!=null) {
                    ((XSLOtherwise)curr).compileError("Only one xsl:otherwise is allowed in an xsl:choose", "XTSE0010");
                } else {
                    otherwise = (StyleElement)curr;
                }
            } else if (curr instanceof StyleElement) {
                ((StyleElement)curr).compileError("Only xsl:when and xsl:otherwise are allowed here", "XTSE0010");
            } else {
                compileError("Only xsl:when and xsl:otherwise are allowed within xsl:choose", "XTSE0010");
            }
        }

        if (numberOfWhens==0) {
            compileError("xsl:choose must contain at least one xsl:when", "XTSE0010");
        }
    }

    /**
    * Mark tail-recursive calls on templates and functions.
    */

    public boolean markTailCalls() {
        boolean found = false;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                return found;
            }
            if (curr instanceof StyleElement) {
                found |= ((StyleElement)curr).markTailCalls();
            }
        }
    }


    public Expression compile(Executable exec) throws XPathException {

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
                Expression b = ((XSLWhen)curr).compileSequenceConstructor(
                        exec, curr.iterateAxis(Axis.CHILD), true);
                if (b == null) {
                    b = Literal.makeEmptySequence();
                }
                try {
                    b = makeExpressionVisitor().simplify(b);
                    actions[w] = b;
                } catch (XPathException e) {
                    compileError(e);
                }

                if (getPreparedStylesheet().isCompileWithTracing()) {
                    TraceWrapper trace = makeTraceInstruction((XSLWhen)curr, actions[w]);
                    actions[w] = trace;
                }

                // Optimize for constant conditions (true or false)
                if (conditions[w] instanceof Literal && ((Literal)conditions[w]).getValue() instanceof BooleanValue) {
                    if (((BooleanValue)((Literal)conditions[w]).getValue()).getBooleanValue()) {
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
                conditions[w] = Literal.makeLiteral(BooleanValue.TRUE);
                Expression b = ((XSLOtherwise)curr).compileSequenceConstructor(
                        exec, curr.iterateAxis(Axis.CHILD), true);
                if (b == null) {
                    b = Literal.makeEmptySequence();
                }
                try {
                    b = makeExpressionVisitor().simplify(b);
                    actions[w] = b;
                } catch (XPathException e) {
                    compileError(e);
                }
                if (getPreparedStylesheet().isCompileWithTracing()) {
                    TraceWrapper trace = makeTraceInstruction((XSLOtherwise)curr, actions[w]);
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
            if (entries==1 && (conditions[0] instanceof Literal) &&
                    ((Literal)conditions[0]).getValue() instanceof BooleanValue) {
                if (((BooleanValue)((Literal)conditions[0]).getValue()).getBooleanValue()) {
                    // only one condition left, and it's known to be true: return the corresponding action
                    return actions[0];
                } else {
                    // but if it's false, return a no-op
                    return null;
                }
            }
            Expression[] conditions2 = new Expression[entries];
            System.arraycopy(conditions, 0, conditions2, 0, entries);
            Expression[] actions2 = new Expression[entries];
            System.arraycopy(actions, 0, actions2, 0, entries);
            conditions = conditions2;
            actions = actions2;
        }

        return new Choose(conditions, actions);
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
//
