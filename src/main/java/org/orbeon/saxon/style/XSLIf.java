package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.Literal;
import org.orbeon.saxon.instruct.Choose;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.Value;


/**
* Handler for xsl:if elements in stylesheet. <br>
* The xsl:if element has a mandatory attribute test, a boolean expression.
* The content is output if the test condition is true.
*/

public class XSLIf extends StyleElement {

    private Expression test;

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

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

        String testAtt=null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.TEST) {
        		testAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (testAtt==null) {
            reportAbsence("test");
        } else {
            test = makeExpression(testAtt);
        }
    }

    public void validate() throws XPathException {
        test = typeCheck("test", test);
    }

    /**
    * Mark tail-recursive calls on stylesheet functions. For most instructions, this does nothing.
    */

    public boolean markTailCalls() {
        StyleElement last = getLastChildInstruction();
        return last != null && last.markTailCalls();
    }

    public Expression compile(Executable exec) throws XPathException {
        if (test instanceof Literal) {
            Value testVal = ((Literal)test).getValue();
            // condition known statically, so we only need compile the code if true.
            // This can happen with expressions such as test="function-available('abc')".
            try {
                if (testVal.effectiveBooleanValue()) {
                    return compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
//                    Block block = new Block();
//                    block.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
//                    compileChildren(exec, block, true);
//                    return block.simplify(getStaticContext());
                } else {
                    return null;
                }
            } catch (XPathException err) {
                // fall through to non-optimizing case
            }
        }

        Expression action = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
        if (action == null) {
            return null;
        }
        Expression[] conditions = {test};
        Expression[] actions = {action};

        Choose inst = new Choose(conditions, actions);
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
