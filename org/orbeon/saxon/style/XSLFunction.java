package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NoNodeTest;
import org.orbeon.saxon.tree.AttributeCollection;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.trace.Location;

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
    List references = new ArrayList();

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
					compileError("Function name must have a namespace prefix");
				}
				try {
				    setObjectNameCode(makeNameCode(nameAtt.trim()));
        		    //functionFingerprint = functionNameCode & 0xfffff;
        		} catch (NamespaceException err) {
        		    compileError(err.getMessage());
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
                    compileError("override must be 'yes' or 'no'");
                }
            } else if (f==StandardNames.SAXON_MEMO_FUNCTION) {
                String memoAtt = atts.getValue(a).trim();
                if (memoAtt.equals("yes")) {
                    memoFunction = true;
                } else if (memoAtt.equals("no")) {
                    memoFunction = false;
                } else {
                    compileError("saxon:memo-function must be 'yes' or 'no'");
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
        for (int i=toplevel.size()-1; i>=0; i--) {
            Object child = toplevel.get(i);
            if (child instanceof XSLFunction &&
                    !(child == this) &&
                    ((XSLFunction)child).getFunctionFingerprint() == getFunctionFingerprint() &&
                    ((XSLFunction)child).getNumberOfArguments() == numberOfArguments &&
                    ((XSLFunction)child).getPrecedence() == getPrecedence()) {
                compileError("Duplicate function declaration");
            }
        }
        //markTailCalls();
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
        Block body = new Block();
        compileChildren(exec, body, false);
//        List executableChildren = new ArrayList();
//        for (int i=0; i<allChildren.length; i++) {
//            // Don't include the xsl:param elements
//            if (!(allChildren[i] instanceof LocalParam)) {
//                executableChildren.add(allChildren[i]);
//            }
//        }
//        Expression exp = convertToExpression(executableChildren, 0);
        //exp.display(20, getNamePool());
        Expression exp = body;

        UserFunction fn = new UserFunction();
        fn.setBody(exp);
        fn.setFunctionNameCode(getObjectNameCode());
        fn.setArgumentTypes(getArgumentTypes());
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
                        new RoleLocator(RoleLocator.FUNCTION_RESULT, functionName, 0);
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
     * This internal routine is called recursively to process one xsl:variable
     * instruction and create one let expression. The variable declared in the xsl:variable
     * becomes the range variable of the let expression, and the rest of the instruction
     * sequence becomes the action (return) part of the let expression.
     * @param instructions The list of instructions
     * @param offset The place in the list where processing is to start
     * @return the LetExpression that results from the translation
     */

//    private Expression convertToExpression(List instructions, int offset) {
//        if (instructions.size() <= offset) {
//            return EmptySequence.getInstance();
//        }
//        Expression start = (Expression)instructions.get(offset);
//        if (start instanceof TraceInstruction) {
//            start = ((TraceInstruction)start).getChild();
//        }
//
//        if (start instanceof LocalVariable) {
//            final LocalVariable ovar = (LocalVariable)start;
//            LetExpression let = new LetExpression();
//            RangeVariableDeclaration var = new RangeVariableDeclaration();
//            var.setRequiredType(ovar.getRequiredType());
//            var.setNameCode(ovar.getNameCode());
//            var.setVariableName(ovar.getVariableName());
//            let.setVariableDeclaration(var);
//            let.setSequence(ovar.getSelectExpression());
//            let.setSlotNumber(((LocalVariable)start).getSlotNumber());
//            let.setAction(convertToExpression(instructions, offset+1));
//            return let;
//        } else {
////            if (start instanceof SequenceInstruction) {
////                Expression select = ((SequenceInstruction)start).getSelectExpression();
////                if (select != null) {
////                    start = select;
////                }
////            }
//            if (offset == instructions.size()-1) {
//                ExpressionTool.markTailFunctionCalls(start);
//                return start;
//            } else {
//                return new AppendExpression(start, Tokenizer.COMMA, convertToExpression(instructions, offset+1));
//            }
//
////        } else if (start instanceof SequenceInstruction) {
////            Expression select = ((SequenceInstruction)start).getSelectExpression();
////            //  what if select is null?
////            ExpressionTool.markTailFunctionCalls(select);
////            return select;
////        } else {
////            return start;
//        }
//    }


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
                ((UserFunctionCall)iter.next()).setFunction(compiledFunction, env);
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
     * Get the required types of the arguments
     * @return an array of SequenceType objects, representing the required types of
     * each of the formal arguments
     */

    public SequenceType[] getArgumentTypes() {
        SequenceType[] types = new SequenceType[getNumberOfArguments()];
        int count = 0;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo node = (NodeInfo)kids.next();
            if (node == null) {
                return types;
            }
            if (node instanceof XSLParam) {
                types[count++] = ((XSLParam)node).getRequiredType();
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
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
