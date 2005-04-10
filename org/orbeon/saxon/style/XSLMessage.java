package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.instruct.Block;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.Message;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.TransformerConfigurationException;


/**
* An xsl:message element in the stylesheet. <br>
*/

public final class XSLMessage extends StyleElement {

    private Expression terminate = null;
    private Expression select = null;

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

        String terminateAtt = null;
        String selectAtt = null;
		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f == StandardNames.TERMINATE) {
        		terminateAtt = atts.getValue(a).trim();
            } else if (f == StandardNames.SELECT) {
                selectAtt = atts.getValue(a);

            } else {
        		checkUnknownAttribute(nc);
        	}
        }
        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }


        if (terminateAtt==null) {
            terminateAtt = "no";
        }

        terminate = makeAttributeValueTemplate(terminateAtt);
        if (terminate instanceof StringValue) {
            String t = ((StringValue)terminate).getStringValue();
            if (!(t.equals("yes") || t.equals("no"))) {
                compileError("terminate must be 'yes' or 'no'", "XTSE0020");
            }
        }
    }

    public void validate() throws XPathException {
        if (!(getParent() instanceof XSLFunction)) {
            checkWithinTemplate();
        }
        select = typeCheck("select", select);
        terminate = typeCheck("terminate", terminate);
    }

    public Expression compile(Executable exec) throws XPathException {
        Expression b = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
        if (b != null) {
            if (select == null) {
                select = b;
            } else {
                //select = new AppendExpression(select, Token.COMMA, b);
                select = Block.makeBlock(select, b);
            }
        }
        if (select == null) {
            select = new StringValue("xsl:message (no content)");
        }
        Message inst = new Message(select, terminate);
        ExpressionTool.makeParentReferences(inst);
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
