package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

/**
* Handler for global variables in a stylesheet or query. <br>
*/

public class GlobalVariable extends GeneralVariable implements Container {

    private Executable executable;
    private SlotManager stackFrameMap = null;

    public Executable getExecutable() {
        return executable;
    }

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    public void setContainsLocals(SlotManager map) {
        this.stackFrameMap = map;
    }

    public boolean isGlobal() {
        return true;
    }

    /**
    * Process the variable declaration
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        // This code is not used. A global variable is not really an instruction, although
        // it is modelled as such, and it will be evaluated using the evaluateVariable() call
        return null;
    }

    /**
     * Evaluate the variable. That is,
     * get the value of the select expression if present or the content
     * of the element otherwise, either as a tree or as a sequence
    */

    public ValueRepresentation getSelectValue(XPathContext context) throws XPathException {
        if (select==null) {
            throw new AssertionError("*** No select expression!!");
        } else {
            XPathContextMajor c2 = context.newCleanContext();
            c2.setOrigin(this);
            AxisIterator initialNode = SingletonIterator.makeIterator(c2.getController().getPrincipalSourceDocument());
            initialNode.next();
            c2.setCurrentIterator(initialNode);
            if (stackFrameMap != null) {
                c2.openStackFrame(stackFrameMap);
            }
            return ExpressionTool.lazyEvaluate(select, c2, true);
        }
    }

    /**
    * Evaluate the variable
    */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        Bindery b = controller.getBindery();

        ValueRepresentation v = b.getGlobalVariableValue(this);

        if (v != null) {
            return v;
        } else {

            // This is the first reference to a global variable; try to evaluate it now.
            // But first set a flag to stop looping. This flag is set in the Bindery because
            // the VariableReference itself can be used by multiple threads simultaneously

            try {
                b.setExecuting(this, true);
                ValueRepresentation value = getSelectValue(context);
                b.defineGlobalVariable(this, value);
                b.setExecuting(this, false);
                return value;

            } catch (XPathException err) {
                b.setExecuting(this, false);
                if (err instanceof XPathException.Circularity) {
                    DynamicError e = new DynamicError("Circular definition of variable " + getVariableName());
                    e.setXPathContext(context);
                    e.setErrorCode("XTDE0640");
                    // Detect it more quickly the next time (in a pattern, the error is recoverable)
                    select = new ErrorExpression(e);
                    throw e;
                } else {
                    throw err;
                }
            }
        }
    }

    /**
     * Get InstructionInfo for this expression
     */

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = new InstructionDetails();
        details.setConstructType(StandardNames.XSL_VARIABLE);
        details.setObjectNameCode(getVariableFingerprint());
        details.setProperty("expression", this);
        details.setSystemId(getSystemId());
        details.setLineNumber(getLineNumber());
        details.setColumnNumber(getColumnNumber());
        return details;
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
