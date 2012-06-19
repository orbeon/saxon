package org.orbeon.saxon.style;

import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trace.ExpressionPresenter;

/**
* saxon:finally element in stylesheet. <br>
*/

public class SaxonFinally extends StyleElement {

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {
		AttributeCollection atts = getAttributeList();
		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
        	checkUnknownAttribute(nc);
        }
    }

    public void validate() throws XPathException {
        StyleElement parent = (StyleElement)getParent();
        if (!(parent instanceof SaxonIterate)) {
            compileError("saxon:finally is not allowed as a child of " + parent.getDisplayName(), "XTSE0010");
        }
        AxisIterator sibs = iterateAxis(Axis.FOLLOWING_SIBLING, NodeKindTest.ELEMENT);
        while (true) {
            NodeInfo sib = (NodeInfo)sibs.next();
            if (sib == null) {
                break;
            }
            if (!(sib instanceof XSLFallback)) {
                compileError("saxon:finally must be the last child of saxon:iterate", "XTSE0010");
            }
        }
    }

    public Expression compile(Executable exec) throws XPathException {
        return compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
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

