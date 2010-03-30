package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.om.UnfailingIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.query.XQueryFunction;
import org.orbeon.saxon.query.XQueryFunctionLibrary;
import org.orbeon.saxon.trans.XPathException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
* A compiled global variable in a stylesheet or query. <br>
*/

public class GlobalVariable extends GeneralVariable implements Container {

    private Executable executable;
    private SlotManager stackFrameMap = null;
    private int hostLanguage;

    /**
     * Create a global variable
     */

    public GlobalVariable(){}

    /**
     * Get the executable containing this global variable
     * @return the containing executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the containing executable
     * @param executable the executable that contains this global variable
     */

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Set the host language for this declaration
     * @param language the host language (for example XSLT, XQuery)
     */

    public void setHostLanguage(int language) {
        hostLanguage = language;
    }

    /**
     * Get the host language for this declaration
     * @return the host language (for example XSLT, XQuery)
     */

    public int getHostLanguage() {
        return hostLanguage;
    }

    /**
     * The expression that initializes a global variable may itself use local variables.
     * In this case a stack frame needs to be allocated while evaluating the global variable
     * @param map The stack frame map for local variables used while evaluating this global
     * variable.
     */

    public void setContainsLocals(SlotManager map) {
        this.stackFrameMap = map;
    }

    /**
     * Is this a global variable?
     * @return true (yes, it is a global variable)
     */

    public boolean isGlobal() {
        return true;
    }

    /**
     * Check for cycles in this variable definition
     * @param referees the calls leading up to this one; it's an error if this variable is on the
     * stack, because that means it calls itself directly or indirectly. The stack may contain
     * variable definitions (GlobalVariable objects) and user-defined functions (UserFunction objects).
     * It will never contain the same object more than once.
     * @param globalFunctionLibrary the library containing all global functions
     */

    public void lookForCycles(Stack referees, XQueryFunctionLibrary globalFunctionLibrary) throws XPathException {
        if (referees.contains(this)) {
            int s = referees.indexOf(this);
            referees.push(this);
            String message = "Circular definition of global variable. $" +
                    getVariableQName().getDisplayName();
            for (int i = s; i < referees.size() - 1; i++) {
                if (i != s) {
                    message += ", which";
                }
                if (referees.get(i + 1) instanceof GlobalVariable) {
                    GlobalVariable next = (GlobalVariable)referees.get(i + 1);
                    message += " uses $" + next.getVariableQName().getDisplayName();
                } else if (referees.get(i + 1) instanceof XQueryFunction) {
                    XQueryFunction next = (XQueryFunction)referees.get(i + 1);
                    message += " calls " + next.getFunctionName().getDisplayName() +
                            "#" + next.getNumberOfArguments() + "()";
                }
            }
            message += '.';
            XPathException err = new XPathException(message);
            err.setErrorCode("XQST0054");
            err.setIsStaticError(true);
            err.setLocator(this);
            throw err;
        }
        if (select != null) {
            referees.push(this);
            List list = new ArrayList(10);
            ExpressionTool.gatherReferencedVariables(select, list);
            for (int i=0; i<list.size(); i++) {
                Binding b = (Binding)list.get(i);
                if (b instanceof GlobalVariable) {
                    ((GlobalVariable)b).lookForCycles(referees, globalFunctionLibrary);
                }
            }
            list.clear();
            ExpressionTool.gatherCalledFunctionNames(select, list);
            for (int i=0; i<list.size(); i++) {
                XQueryFunction f = globalFunctionLibrary.getDeclarationByKey((String)list.get(i));
                if (!referees.contains(f)) {
                    // recursive function calls are allowed
                    lookForFunctionCycles(f, referees, globalFunctionLibrary);
                }
            }
            referees.pop();
        }
    }

    /**
     * Look for cyclic variable references that go via one or more function calls
     * @param f a used-defined function
     * @param referees a list of variables and functions that refer directly or indirectly to this variable
     * @param globalFunctionLibrary the library containing all global functions
     */

    private static void lookForFunctionCycles(
            XQueryFunction f, Stack referees, XQueryFunctionLibrary globalFunctionLibrary) throws XPathException {
        Expression body = f.getBody();
        referees.push(f);
        List list = new ArrayList(10);
        ExpressionTool.gatherReferencedVariables(body, list);
        for (int i=0; i<list.size(); i++) {
            Binding b = (Binding)list.get(i);
            if (b instanceof GlobalVariable) {
                ((GlobalVariable)b).lookForCycles(referees, globalFunctionLibrary);
            }
        }
        list.clear();
        ExpressionTool.gatherCalledFunctionNames(body, list);
        for (int i=0; i<list.size(); i++) {
            XQueryFunction qf = globalFunctionLibrary.getDeclarationByKey((String)list.get(i));
            if (!referees.contains(qf)) {
                // recursive function calls are allowed
                lookForFunctionCycles(qf, referees, globalFunctionLibrary);
            }
        }
        referees.pop();
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
            throw new AssertionError("*** No select expression for global variable $" +
                    getVariableQName().getDisplayName() + "!!");
        } else {
            XPathContextMajor c2 = context.newCleanContext();
            c2.setOrigin(this);
            UnfailingIterator initialNode =
                    SingletonIterator.makeIterator(c2.getController().getContextForGlobalVariables());
            initialNode.next();
            c2.setCurrentIterator(initialNode);
            if (stackFrameMap != null) {
                c2.openStackFrame(stackFrameMap);
            }
            return ExpressionTool.evaluate(select, evaluationMode, c2, referenceCount);
        }
    }

    /**
    * Evaluate the variable
    */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        final Controller controller = context.getController();
        final Bindery b = controller.getBindery();

        final ValueRepresentation v = b.getGlobalVariable(getSlotNumber());

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
                    XPathException e = new XPathException("Circular definition of variable " +
                            getVariableQName().getDisplayName());
                    int lang = getHostLanguage();
                    e.setErrorCode(lang == Configuration.XQUERY ? "XQST0054" : "XTDE0640");
                    e.setXPathContext(context);
                    // Detect it more quickly the next time (in a pattern, the error is recoverable)
                    select = new ErrorExpression(e);
                    e.setLocator(this);
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

//    public InstructionInfo getInstructionInfo() {
//        InstructionDetails details = new InstructionDetails();
//        details.setConstructType(getInstructionNameCode());
//        details.setObjectName(getVariableQName());
//        details.setProperty("expression", this);
//        details.setSystemId(getSystemId());
//        details.setLineAndColumn(getLineNumber());
//        details.setColumnNumber(getColumnNumber());
//        return details;
//    }


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
