package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Whitespace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Handler for xsl:function elements in stylesheet (XSLT 2.0). <BR>
* Attributes: <br>
* name gives the name of the function
* saxon:memo-function=yes|no indicates whether it acts as a memo function.
*/

public class XSLFunction extends StyleElement implements StylesheetProcedure {

    private String nameAtt = null;
    private String asAtt = null;
    private String overrideAtt = null;
    private SequenceType resultType;
    private String functionName;
    private SlotManager stackFrameMap;
    private boolean memoFunction = false;
    private boolean override = true;
    private int numberOfArguments = -1;  // -1 means not yet known
    private UserFunction compiledFunction;

    // List of UserFunctionCall objects that reference this XSLFunction
    List references = new ArrayList(10);

    /**
     * Method called by UserFunctionCall to register the function call for
     * subsequent fixup.
     * @param ref the UserFunctionCall to be registered
    */

    public void registerReference(UserFunctionCall ref) {
        references.add(ref);
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();
        overrideAtt = "yes";
    	for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
            if (f.equals(StandardNames.NAME)) {
				nameAtt = Whitespace.trim(atts.getValue(a));
				if (nameAtt.indexOf(':')<0) {
					compileError("Function name must have a namespace prefix", "XTSE0740");
				}
				try {
				    setObjectName(makeQName(nameAtt));
        		} catch (NamespaceException err) {
        		    compileError(err.getMessage(), "XTSE0280");
        		} catch (XPathException err) {
                    compileError(err);
                }
        	} else if (f.equals(StandardNames.AS)) {
        		asAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.OVERRIDE)) {
                overrideAtt = Whitespace.trim(atts.getValue(a));
                if (overrideAtt.equals("yes")) {
                    override = true;
                } else if (overrideAtt.equals("no")) {
                    override = false;
                } else {
                    override = true;
                    compileError("override must be 'yes' or 'no'", "XTSE0020");
                }
            } else if (f.equals(StandardNames.SAXON_MEMO_FUNCTION)) {
                String memoAtt = Whitespace.trim(atts.getValue(a));
                if (memoAtt.equals("yes")) {
                    memoFunction = true;
                } else if (memoAtt.equals("no")) {
                    memoFunction = false;
                } else {
                    compileError("saxon:memo-function must be 'yes' or 'no'", "XTSE0020");
                }
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt == null) {
            reportAbsence("name");
            nameAtt="xsl:unnamed-function";
        }

        if (asAtt == null) {
            resultType = SequenceType.ANY_SEQUENCE;
        } else {
            resultType = makeSequenceType(asAtt);
        }

        functionName = nameAtt;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be -1.
     */

    public StructuredQName getObjectName() {
        StructuredQName qn = super.getObjectName();
        if (qn == null) {
            nameAtt = Whitespace.trim(getAttributeValue("name"));
            if (nameAtt == null) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-function");
            }
            try {
                qn = makeQName(nameAtt);
                setObjectName(qn);
            } catch (NamespaceException err) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-function");
            } catch (XPathException err) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-function");
            }
        }
        return qn;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body.
    * @return true: yes, it may contain a general template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    protected boolean mayContainParam() {
        return true;
    }

    /**
     * Specify that xsl:param is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return (child instanceof XSLParam);
    }
    /**
    * Is override="yes"?.
    * @return true if override="yes" was specified, otherwise false
    */

    public boolean isOverriding() {
        if (overrideAtt == null) {
            // this is a forwards reference
            try {
                prepareAttributes();
            } catch (XPathException e) {
                // no action: error will be caught later
            }
        }
        return override;
    }

    /**
    * Notify all references to this function of the data type.
     * @throws XPathException
    */

    public void fixupReferences() throws XPathException {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            ((UserFunctionCall)iter.next()).setStaticType(resultType);
        }
        super.fixupReferences();
    }

    public void validate() throws XPathException {

        stackFrameMap = getConfiguration().makeSlotManager();

        // check the element is at the top level of the stylesheet

        checkTopLevel(null);
        getNumberOfArguments();

        // check that this function is not a duplicate of another

        XSLStylesheet root = getPrincipalStylesheet();
        List toplevel = root.getTopLevel();
        boolean isDuplicate = false;
        for (int i=toplevel.size()-1; i>=0; i--) {
            Object child = toplevel.get(i);
            if (child instanceof XSLFunction &&
                    !(child == this) &&
                    ((XSLFunction)child).getObjectName().equals(getObjectName()) &&
                    ((XSLFunction)child).getNumberOfArguments() == numberOfArguments) {
                if (((XSLFunction)child).getPrecedence() == getPrecedence()) {
                    isDuplicate = true;
                }
                if (((XSLFunction)child).getPrecedence() > getPrecedence()) {
                    // it's not an error to have duplicates if there is another with higher precedence
                    isDuplicate = false;
                    break;
                }
            }
        }
        if (isDuplicate) {
            compileError("Duplicate function declaration", "XTSE0770");
        }
    }


    /**
     * Compile the function definition to create an executable representation
     * @return an Instruction, or null. The instruction returned is actually
     * rather irrelevant; the compile() method has the side-effect of binding
     * all references to the function to the executable representation
     * (a UserFunction object)
     * @throws XPathException
     */

    public Expression compile(Executable exec) throws XPathException {
        compileAsExpression(exec);
        return null;
    }

    /**
     * Compile the function into a UserFunction object, which treats the function
     * body as a single XPath expression. This involves recursively translating
     * xsl:variable declarations into let expressions, withe the action part of the
     * let expression containing the rest of the function body.
     * The UserFunction that is created will be linked from all calls to
     * this function, so nothing else needs to be done with the result. If there are
     * no calls to it, the compiled function will be garbage-collected away.
     * @param exec the Executable
     * @throws XPathException
     */

    private void compileAsExpression(Executable exec) throws XPathException {
        Expression exp = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), false);
        if (exp == null) {
            exp = Literal.makeEmptySequence();
        }

        UserFunction fn = new UserFunction();
        fn.setHostLanguage(Configuration.XSLT);
        fn.setBody(exp);
        fn.setFunctionName(getObjectName());
        setParameterDefinitions(fn);
        fn.setResultType(getResultType());
        fn.setLineNumber(getLineNumber());
        fn.setSystemId(getSystemId());
        fn.setStackFrameMap(stackFrameMap);
        fn.setMemoFunction(memoFunction);
        fn.setExecutable(exec);

        Expression exp2 = exp;
        ExpressionVisitor visitor = makeExpressionVisitor();
        try {
            // We've already done the typecheck of each XPath expression, but it's worth doing again at this
            // level because we have more information now.

            exp2 = visitor.typeCheck(exp, null);
            if (resultType != null) {
                RoleLocator role =
                        new RoleLocator(RoleLocator.FUNCTION_RESULT, functionName, 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                role.setErrorCode("XTTE0780");
                exp2 = TypeChecker.staticTypeCheck(exp2, resultType, false, role, visitor);
            }
            exp2 = exp2.optimize(visitor, null);

        } catch (XPathException err) {
            err.maybeSetLocation(this);
            compileError(err);
        }

        // Try to extract new global variables from the body of the function
        exp2 = getConfiguration().getOptimizer().promoteExpressionsToGlobal(exp2, visitor);

        // Add trace wrapper code if required
        if (getPreparedStylesheet().isCompileWithTracing()) {
            TraceWrapper trace = new TraceInstruction(exp2, this);
            trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
            exp2 = trace;
        }

        allocateSlots(exp2);
        if (exp2 != exp) {
            fn.setBody(exp2);
        }

        int tailCalls = ExpressionTool.markTailFunctionCalls(exp2, getObjectName(), getNumberOfArguments());
        if (tailCalls != 0) {
            fn.setTailRecursive(tailCalls > 0, tailCalls > 1);
            fn.setBody(new TailCallLoop(fn));
        }
        fixupInstruction(fn);
        compiledFunction = fn;

        fn.computeEvaluationMode();

        if (isExplaining()) {
            exp2.explain(System.err);
        }
    }

    /**
    * Fixup all function references.
     * @param compiledFunction the Instruction representing this function in the compiled code
     * @throws XPathException if an error occurs.
    */

    private void fixupInstruction(UserFunction compiledFunction)
    throws XPathException {
        ExpressionVisitor visitor = makeExpressionVisitor();
        try {
            Iterator iter = references.iterator();
            while (iter.hasNext()) {
                UserFunctionCall call = ((UserFunctionCall)iter.next());
                call.setFunction(compiledFunction);
                call.checkFunctionCall(compiledFunction, visitor);
                call.computeArgumentEvaluationModes();
            }
        } catch (XPathException err) {
            compileError(err);
        }
    }

    /**
     * Get associated Procedure (for details of stack frame).
     * @return the associated Procedure object
     */

    public SlotManager getSlotManager() {
        return stackFrameMap;
    }

    /**
     * Get the type of value returned by this function
     * @return the declared result type, or the inferred result type
     * if this is more precise
     */
    public SequenceType getResultType() {
        return resultType;
    }

    /**
     * Get the number of arguments declared by this function (that is, its arity).
     * @return the arity of the function
     */

    public int getNumberOfArguments() {
        if (numberOfArguments == -1) {
            numberOfArguments = 0;
            AxisIterator kids = iterateAxis(Axis.CHILD);
            while (true) {
                Item child = kids.next();
                if (child instanceof XSLParam) {
                    numberOfArguments++;
                } else {
                    return numberOfArguments;
                }
            }
        }
        return numberOfArguments;
    }

    /**
     * Set the definitions of the parameters in the compiled function, as an array.
     * @param fn the compiled object representing the user-written function
     */

    public void setParameterDefinitions(UserFunction fn) {
        UserFunctionParameter[] params = new UserFunctionParameter[getNumberOfArguments()];
        fn.setParameterDefinitions(params);
        int count = 0;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo node = (NodeInfo)kids.next();
            if (node == null) {
                return;
            }
            if (node instanceof XSLParam) {
                UserFunctionParameter param = new UserFunctionParameter();
                params[count++] = param;
                param.setRequiredType(((XSLParam)node).getRequiredType());
                param.setVariableQName(((XSLParam)node).getVariableQName());
                param.setSlotNumber(((XSLParam)node).getSlotNumber());
                ((XSLParam)node).fixupBinding(param);
                int refs = ExpressionTool.getReferenceCount(fn.getBody(), param, false);
                param.setReferenceCount(refs);
            }
        }
    }

    /**
     * Get the compiled function
     * @return the object representing the compiled user-written function
     */

    public UserFunction getCompiledFunction() {
        return compiledFunction;
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link org.orbeon.saxon.trace.Location}. This method is part of the
     * {@link org.orbeon.saxon.trace.InstructionInfo} interface
     */

    public int getConstructType() {
        return StandardNames.XSL_FUNCTION;
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
