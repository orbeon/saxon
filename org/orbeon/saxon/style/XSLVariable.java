package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.ComputedExpression;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.GeneralVariable;
import org.orbeon.saxon.instruct.GlobalVariable;
import org.orbeon.saxon.instruct.LocalVariable;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.SequenceType;

import javax.xml.transform.TransformerConfigurationException;

/**
* Handler for xsl:variable elements in stylesheet. <br>
* The xsl:variable element has mandatory attribute name and optional attribute select
*/

public class XSLVariable extends XSLVariableDeclaration {

    private int state = 0;
            // 0 = before prepareAttributes()
            // 1 = during prepareAttributes()
            // 2 = after prepareAttributes()

    public void prepareAttributes() throws TransformerConfigurationException {
        if (state==2) return;
        if (state==1) {
            compileError("Circular reference to variable");
        }
        state = 1;
        //System.err.println("Prepare attributes of $" + getVariableName());
        super.prepareAttributes();
        state = 2;
    }

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction (well, it can be, anyway)
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Get the static type of the variable. This is the declared type, unless the value
    * is statically known and constant, in which case it is the actual type of the value.
    */

    public SequenceType getRequiredType() {
        // System.err.println("Get required type of $" + getVariableName());
        SequenceType defaultType = (requiredType==null ? SequenceType.ANY_SEQUENCE : requiredType);
        if (assignable) {
            return defaultType;
        } else if (requiredType != null) {
            return requiredType;
        } else if (select!=null) {
            if (select instanceof EmptySequence) {
                // returning Type.EMPTY gives problems with static type checking
                return defaultType;
            } else {
                try {
                    // try to infer the type from the select expression
                    return new SequenceType(select.getItemType(), select.getCardinality());
                } catch (Exception err) {
                    // this may fail because the select expression references a variable or function
                    // whose type is not yet known, because of forwards (perhaps recursive) references.
                    return defaultType;
                }
            }
        } else if (hasChildNodes()) {
            return new SequenceType(NodeKindTest.DOCUMENT, StaticProperty.EXACTLY_ONE);
        } else {
            // no select attribute or content: value is an empty string
            return SequenceType.SINGLE_STRING;
        }
    }

    /**
    * Compile: this ensures space is available for local variables declared within
    * this global variable
    */

    public Expression compile(Executable exec) throws TransformerConfigurationException {

        if (references.size()==0 && !assignable) {
            redundant = true;
        }

        if (!redundant) {
            GeneralVariable inst;
            if (global) {
                inst = new GlobalVariable();
                ((GlobalVariable)inst).setExecutable(getExecutable());
                if (select instanceof ComputedExpression) {
                    ((ComputedExpression)select).setParentExpression(inst);
                }
            } else {
                inst = new LocalVariable();
            }
            initializeInstruction(exec, inst);
            inst.setVariableName(getVariableName());
            inst.setSlotNumber(getSlotNumber());
            inst.setRequiredType(getRequiredType());
            ExpressionTool.makeParentReferences(inst);
            fixupBinding(inst);
            return inst;
        }

        return null;
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
