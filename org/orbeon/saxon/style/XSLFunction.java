package net.sf.saxon.style;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;

import javax.xml.transform.TransformerConfigurationException;
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

    public void prepareAttributes() throws TransformerConfigurationException {

		AttributeCollection atts = getAttributeList();

    	for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
            if (f==StandardNames.NAME) {
				nameAtt = atts.getValue(a).trim();
				if (nameAtt.indexOf(':')<0) {
					compileError("Function name must have a namespace prefix", "XT0740");
				}
				try {
				    setObjectNameCode(makeNameCode(nameAtt.trim()));
        		} catch (NamespaceException err) {
        		    compileError(err.getMessage(), "XT0280");
        		} catch (XPathException err) {
                    compileError(err);
                }
        	} else if (f==StandardNames.AS) {
        		asAtt = atts.getValue(a);
            } else if (f==StandardNames.OVERRIDE) {
                overrideAtt = atts.getValue(a).trim();
                if (overrideAtt.equals("yes")) {
                    override = true;
                } else if (overrideAtt.equals("no")) {
                    override = false;
                } else {
                    compileError("override must be 'yes' or 'no'", "XT0020");
                }
            } else if (f==StandardNames.SAXON_MEMO_FUNCTION) {
                String memoAtt = atts.getValue(a).trim();
                if (memoAtt.equals("yes")) {
                    memoFunction = true;
                } else if (memoAtt.equals("no")) {
                    memoFunction = false;
                } else {
                    compileError("saxon:memo-function must be 'yes' or 'no'", "XT0020");
                }
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt == null) {
            reportAbsence("name");
        }

        if (asAtt == null) {
            resultType = SequenceType.ANY_SEQUENCE;
        } else {
            resultType = makeSequenceType(asAtt);
        }

        functionName = nameAtt;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body.
    * @return true: yes, it may contain a general template-body
    */

    public boolean mayContainSequenceConstructor() {
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
        return override;
    }

    /**
    * Notify all references to this function of the data type.
     * @throws TransformerConfigurationException
    */

    public void fixupReferences() throws TransformerConfigurationException {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            ((UserFunctionCall)iter.next()).setStaticType(resultType);
        }
        super.fixupReferences();
    }

    public void validate() throws TransformerConfigurationException {

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
                    ((XSLFunction)child).getFunctionFingerprint() == getFunctionFingerprint() &&
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
            compileError("Duplicate function declaration", "XT0770");
        }
    }


    /**
     * Compile the function definition to create an executable representation
     * @return an Instruction, or null. The instruction returned is actually
     * rather irrelevant; the compile() method has the side-effect of binding
     * all references to the function to the executable representation
     * (a UserFunction object)
     * @throws TransformerConfigurationException
     */

    public Expression compile(Executable exec) throws TransformerConfigurationException {
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
     * @throws TransformerConfigurationException
     */

    private void compileAsExpression(Executable exec) throws TransformerConfigurationException {
        //Block body = new Block();
        //compileChildren(exec, body, false);
        Expression exp = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), false);
        if (exp == null) {
            exp = EmptySequence.getInstance();
        }

        //Expression exp = body;

        UserFunction fn = new UserFunction();
        fn.setBody(exp);
        fn.setFunctionNameCode(getObjectNameCode());
        setParameterDefinitions(fn);
        //fn.setParameterDefinitions(getParameterDefinitions());
        //fn.setArgumentTypes(getArgumentTypes());
        fn.setResultType(getResultType());
        fn.setLineNumber(getLineNumber());
        fn.setSystemId(getSystemId());
        fn.setStackFrameMap(stackFrameMap);
        fn.setMemoFunction(memoFunction);
        fn.setExecutable(exec);

        Expression exp2 = exp;
        try {
            exp2 = exp.simplify(staticContext).analyze(staticContext, null);
            if (resultType != null) {
                RoleLocator role =
                        new RoleLocator(RoleLocator.FUNCTION_RESULT, functionName, 0, null);
                exp2 = TypeChecker.staticTypeCheck(exp2, resultType, false, role, getStaticContext());
            }

        } catch (XPathException err) {
            compileError(err);
        }

        if (getConfiguration().getTraceListener() != null) {
            TraceWrapper trace = new TraceInstruction(exp2, this);
            trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
            exp2 = trace;
        }

        allocateSlots(exp2);
        if (exp2 != exp) {
            fn.setBody(exp2);
        }

        fixupInstruction(fn, getStaticContext());
        compiledFunction = fn;
    }

    /**
    * Fixup all function references.
     * @param compiledFunction the Instruction representing this function in the compiled code
     * @throws TransformerConfigurationException if an error occurs.
    */

    private void fixupInstruction(UserFunction compiledFunction, StaticContext env)
    throws TransformerConfigurationException {
        try {
            Iterator iter = references.iterator();
            while (iter.hasNext()) {
                UserFunctionCall call = ((UserFunctionCall)iter.next());
                call.setFunction(compiledFunction, env);
                call.checkFunctionCall(compiledFunction, env);
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
     * Get the fingerprint of the name of this function.
     * @return the fingerprint of the name
     */

    public int getFunctionFingerprint() {
        if (getObjectFingerprint()==-1) {
            // this is a forwards reference to the function
            try {
        	    prepareAttributes();
        	} catch (TransformerConfigurationException err) {
        	    return -1;              // we'll report the error later
        	}
        }
        return getObjectFingerprint();
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
                param.setSlotNumber(((XSLParam)node).getSlotNumber());
                ((XSLParam)node).fixupBinding(param);
                List references = ((XSLParam)node).getReferences();
                int refs = RangeVariableDeclaration.getReferenceCount(references, param);
                param.setReferenceCount(refs);
            }
        }
    }

    /**
     * Get the compiled function
     */

    public UserFunction getCompiledFunction() {
        return compiledFunction;
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link net.sf.saxon.trace.Location}. This method is part of the
     * {@link net.sf.saxon.trace.InstructionInfo} interface
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
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
