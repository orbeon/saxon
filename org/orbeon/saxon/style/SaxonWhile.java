package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.While;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.value.EmptySequence;

import javax.xml.transform.TransformerConfigurationException;


/**
* Handler for saxon:while elements in stylesheet.
* The saxon:while element has a mandatory attribute test, a boolean expression.
* The content is output repeatedly so long as the test condition is true.
*/

public class SaxonWhile extends StyleElement {

    private Expression test;

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

    public void prepareAttributes() throws TransformerConfigurationException {

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
            return;
        }
        test = makeExpression(testAtt);
    }

    public void validate() throws TransformerConfigurationException {
        checkWithinTemplate();
        test = typeCheck("test", test);
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        Expression action = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
        if (action == null) {
            action = EmptySequence.getInstance();
        }
        //try {
            While w = new While(test, action);
            ExpressionTool.makeParentReferences(w);
            return w;
//        } catch (XPathException e) {
//            compileError(e);
//            return null;
//        }
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
