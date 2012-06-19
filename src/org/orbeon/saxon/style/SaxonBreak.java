package org.orbeon.saxon.style;

import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.BreakInstr;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Whitespace;

/**
* A saxon:break element in the stylesheet, or (by subclassing) a saxon:continue element
*/

public class SaxonBreak extends StyleElement {

    SaxonIterate saxonIterate = null;

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
        return AnyItemType.getInstance();
    }

    public void prepareAttributes() throws XPathException {
		AttributeCollection atts = getAttributeList();
		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
            checkUnknownAttribute(nc);
        }
    }

    public void validate() throws XPathException {
        validatePosition();
        if (saxonIterate == null) {
            compileError(getDisplayName() + " must be a descendant of a saxon:iterate instruction", "XTSE0010");
            return;
        }
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLFallback && mayContainFallback()) {
                // xsl:fallback is not allowed on xsl:call-template, but is allowed on saxon:call-template (cheat!)
            } else if (child.getNodeKind() == Type.TEXT) {
                    // with xml:space=preserve, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValueCS())) {
                    compileError("No character data is allowed within " + getDisplayName(), "XTSE0010");
                }
            } else {
                compileError("Child element " + Err.wrap(child.getDisplayName(), Err.ELEMENT) +
                        " is not allowed within " + getDisplayName(), "XTSE0010");
            }
        }

    }

    /**
     * Test that this saxon:continue or saxon:break instruction appears in a valid position
     * @throws XPathException
     */

    protected void validatePosition() throws XPathException {
        NodeInfo inst = this;
        boolean isLast = true;
        while (true) {
            if (!(inst instanceof XSLWhen)) {
                AxisIterator sibs = inst.iterateAxis(Axis.FOLLOWING_SIBLING);
                while (true) {
                    NodeInfo sib = (NodeInfo)sibs.next();
                    if (sib == null) {
                        break;
                    }
                    if (sib instanceof XSLFallback || sib instanceof SaxonFinally) {
                        continue;
                    }
                    isLast = false;
                }
            }
            inst = inst.getParent();
            if (inst instanceof SaxonIterate) {
                saxonIterate = (SaxonIterate)inst;
                break;
            } else if (inst instanceof XSLWhen || inst instanceof XSLOtherwise
                    || inst instanceof XSLIf || inst instanceof XSLChoose) {
                // continue;
            } else if (inst == null) {
                compileError(getDisplayName() + " is not allowed at outermost level", "XTSE0010");
                return;
            } else {
                compileError(getDisplayName() + " is not allowed within " + inst.getDisplayName(), "XTSE0010");
                return;
            }
        }
        if (!isLast) {
            compileError(getDisplayName() + " must be the last instruction in the saxon:iterate loop", "XTSE0010");
        }
    }

    public Expression compile(Executable exec) throws XPathException {
        return new BreakInstr ();
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

