package org.orbeon.saxon.sql;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.RoleLocator;
import org.orbeon.saxon.expr.TypeChecker;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.GeneralVariable;
import org.orbeon.saxon.instruct.InstructionDetails;
import org.orbeon.saxon.instruct.TailCall;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.Name;
import org.orbeon.saxon.om.Navigator;
import org.orbeon.saxon.style.XSLGeneralVariable;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;


/**
* An sql:column element in the stylesheet.
*/

public class SQLColumn extends XSLGeneralVariable {

    /**
    * Determine whether this node is an instruction.
    * @return false - it is not an instruction
    */

    public boolean isInstruction() {
        return false;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return false: no, it may not contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return false;
    }

    public void prepareAttributes() throws TransformerConfigurationException {

        getVariableFingerprint();

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;
        String nameAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			String localName = atts.getLocalName(a);
			if (localName.equals("name")) {
        		nameAtt = atts.getValue(a).trim();
        	} else if (localName.equals("select")) {
        		selectAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(atts.getNameCode(a));
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
        } else if (!Name.isQName(nameAtt)) {
            compileError("Column name must be a valid QName");
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }

    }


    public void validate() throws TransformerConfigurationException {
        if (!(getParentNode() instanceof SQLInsert)) {
            compileError("parent node must be sql:insert");
        }
        select = typeCheck("select", select);
        try {
            RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, "sql:column/select", 0, null);
            select = TypeChecker.staticTypeCheck(select,
                        SequenceType.SINGLE_ATOMIC,
                        false, role, getStaticContext());

        } catch (XPathException err) {
            compileError(err);
        }
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        ColumnInstruction inst = new ColumnInstruction();
        initializeInstruction(exec, inst);
        return inst;
    }

    public String getColumnName() {
        return Navigator.getAttributeValue(this, "", "name");
    }

    protected static class ColumnInstruction extends GeneralVariable {

        public ColumnInstruction() {}

        public InstructionInfo getInstructionInfo() {
            InstructionDetails details = (InstructionDetails)super.getInstructionInfo();
            details.setConstructType(Location.EXTENSION_INSTRUCTION);
            return details;
        }

        public TailCall processLeavingTail(XPathContext context) {
            return null;
        }

        /**
         * Evaluate the variable (method exists only to satisfy the interface)
         */

        public Value evaluateVariable(XPathContext context) throws XPathException {
            throw new UnsupportedOperationException();
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
