package net.sf.saxon.query;

import net.sf.saxon.expr.*;
import net.sf.saxon.functions.ExecutableFunctionLibrary;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XQueryFunction implements InstructionInfo, Container {
    private int nameCode;
    List arguments;              // A list of RangeVariableDeclaration objects
    SequenceType resultType;
    Expression body = null;
    List references = new ArrayList(10);
    int lineNumber;
    int columnNumber;
    String systemId;
    private Executable executable;
    private UserFunction compiledFunction = null;
    NamespaceResolver namespaceResolver;
    private StaticContext staticContext;

    public XQueryFunction() {}

    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    public int getNameCode() {
        return nameCode;
    }

    public String getFunctionDisplayName(NamePool pool) {
        return pool.getDisplayName(nameCode);
    }

    public int getFunctionFingerprint() {
        return nameCode & 0xfffff;
    }

    public SequenceType getResultType() {
        return resultType;
    }

    public void setExecutable(Executable exec) {
        executable = exec;
    }

    public Executable getExecutable() {
        return executable;
    }

    public StaticContext getStaticContext() {
        return staticContext;
    }

    public SequenceType[] getArgumentTypes() {
        SequenceType[] types = new SequenceType[arguments.size()];
        for (int i=0; i<arguments.size(); i++) {
            types[i] = ((RangeVariableDeclaration)arguments.get(i)).getRequiredType();
        }
        return types;
    }

    public UserFunctionParameter[] getParameterDefinitions() {
        UserFunctionParameter[] params = new UserFunctionParameter[arguments.size()];
        for (int i=0; i<arguments.size(); i++) {
            SequenceType type = ((RangeVariableDeclaration)arguments.get(i)).getRequiredType();
            UserFunctionParameter param = new UserFunctionParameter();
            param.setRequiredType(type);
            params[i] = param;
        }
        return params;
    }

    public int getNumberOfArguments() {
        return arguments.size();
    }

    public void registerReference(UserFunctionCall ufc) {
        references.add(ufc);
    }

    public UserFunction compile(StaticQueryContext env) throws StaticError {
        staticContext = env;
        try {
            // If a query function is imported into several modules, then the compile()
            // method will be called once for each importing module. If the compiled
            // function already exists, then this is a repeat call, and the only thing
            // needed is to fix up references to the function from within the importing
            // module.

            if (compiledFunction == null) {
                // first get the UserFunctionParameter object for each declared
                // argument of the function, and bind the references to that argument
                SlotManager map = env.getConfiguration().makeSlotManager();
                UserFunctionParameter[] params = getParameterDefinitions();
                Iterator iter = arguments.iterator();
                int slot = 0;
                while (iter.hasNext()) {
                    RangeVariableDeclaration decl = (RangeVariableDeclaration)iter.next();
                    UserFunctionParameter param = params[slot];
                    param.setSlotNumber(slot++);
                    param.setRequiredType(decl.getRequiredType());
                    map.allocateSlotNumber(decl.getNameCode() & 0xfffff);
                    decl.fixupReferences(param);
                }

                // type-check the body of the function

                body = body.simplify(env).analyze(env, null);
                if (body instanceof ComputedExpression) {
                    ((ComputedExpression)body).setParentExpression(this);
                }
                RoleLocator role =
                        new RoleLocator(RoleLocator.FUNCTION_RESULT, new Integer(nameCode), 0, env.getNamePool());
                body = TypeChecker.staticTypeCheck(body, resultType, false, role, env);
                if (body instanceof ComputedExpression) {
                    ((ComputedExpression)body).setParentExpression(this);
                }
                ExpressionTool.allocateSlots(body, slot, map);

                if (env.getConfiguration().getTraceListener() != null) {
                    namespaceResolver = env.getNamespaceResolver();
                    TraceExpression trace = new TraceExpression(body);
                    trace.setLineNumber(lineNumber);
                    trace.setColumnNumber(columnNumber);
                    trace.setSystemId(env.getBaseURI());
                    trace.setConstructType(StandardNames.XSL_FUNCTION);
                    trace.setObjectNameCode(nameCode);
                    trace.setLocationId(env.getLocationMap().allocateLocationId(systemId, lineNumber));
                    body = trace;
                }

                compiledFunction = new UserFunction(body);
                compiledFunction.setFunctionNameCode(nameCode);
                compiledFunction.setParameterDefinitions(params);
                //compiledFunction.setArgumentTypes(getArgumentTypes());
                compiledFunction.setResultType(getResultType());
                compiledFunction.setLineNumber(lineNumber);
                compiledFunction.setSystemId(systemId);
                compiledFunction.setExecutable(executable);
                compiledFunction.setStackFrameMap(map);

                // mark tail calls within the function body

                ExpressionTool.markTailFunctionCalls(body);

                for (int i=0; i<params.length; i++) {
                    RangeVariableDeclaration decl = (RangeVariableDeclaration)arguments.get(i);
                    UserFunctionParameter param = params[i];
                    int refs = decl.getReferenceCount(param);
                    param.setReferenceCount(refs);
                }

            }

            // bind all references to this function to the UserFunction object

            fixupReferences(env);

            // register this function with the function library available at run-time (e.g. for saxon:evaluate())

            if (executable.getFunctionLibrary() instanceof ExecutableFunctionLibrary) {
                ExecutableFunctionLibrary lib  = (ExecutableFunctionLibrary)executable.getFunctionLibrary();
                lib.addFunction(compiledFunction);
            } else {
                throw new AssertionError("executable.getFunctionLibrary() is an instance of " +
                        executable.getFunctionLibrary().getClass().getName());
            }

            return compiledFunction;
        } catch (XPathException e) {
            if (e.getLocator()==null) {
                e.setLocator(this);
            }
            if (e instanceof StaticError) {
                throw (StaticError)e;
            } else {
                throw new StaticError(e);
            }
        }
    }

    /**
     * Fix up references to this function
     */

    public void fixupReferences(StaticContext env) throws XPathException {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            UserFunctionCall ufc = (UserFunctionCall)iter.next();
            ufc.setFunction(compiledFunction, env);
        }
    }

    /**
     * Type-check references to this function
     */

    public void checkReferences(StaticContext env) throws XPathException {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            UserFunctionCall ufc = (UserFunctionCall)iter.next();
            ufc.checkFunctionCall(compiledFunction, env);
        }

        // clear the list of references, so that more can be added in another module
        references = new ArrayList(0);

    }

    /**
     * Produce diagnostic output showing the compiled and optimized expression tree for a function
     * @param pool the namepool to be used
     */
    public void explain(NamePool pool) {
        System.err.println("declare function " + pool.getDisplayName(nameCode) + " {");
        body.display(4, pool, System.err);
        System.err.println("}");
    }

    /**
     * Get the callable compiled function contained within this XQueryFunction definition.
     */

    public UserFunction getUserFunction() {
        return compiledFunction;
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link Location}.
     */

    public int getConstructType() {
        return StandardNames.XSL_FUNCTION;
    }

    /**
     * Get the name of the instruction. This is applicable only when the construct type
     * is Location.INSTRUCTION.
     */

    public int getInstructionFingerprint() {
        return -1;
    }

    /**
     * Get a description of the instruction for use in error messages. For an XSLT instruction this
     * will be the display name
     */

//    public String getDescription(NamePool pool) {
//        return "declare function";
//    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     */

    public int getObjectNameCode() {
        return nameCode;
    }

    /**
     * Get the system identifier (URI) of the source module containing
     * the instruction. This will generally be an absolute URI. If the system
     * identifier is not known, the method may return null. In some cases, for example
     * where XML external entities are used, the correct system identifier is not
     * always retained.
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Get the line number of the instruction in the source stylesheet module.
     * If this is not known, or if the instruction is an artificial one that does
     * not relate to anything in the source code, the value returned may be -1.
     */

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Return the public identifier for the current document event.
     * @return A string containing the public identifier, or
     *         null if none is available.
     * @see #getSystemId
     */
    public String getPublicId() {
        return null;
    }

    /**
     * Return the column number
     * @return The column number, or -1 if none is available.
     * @see #getLineNumber
     */

    public int getColumnNumber() {
        return -1;
    }

    public String getSystemId(int locationId) {
        return getSystemId();
    }

    public int getLineNumber(int locationId) {
        return getLineNumber();
    }

    /**
     * Get the namespace context of the instruction. This will not always be available, in which
     * case the method returns null.
     */

    public NamespaceResolver getNamespaceResolver() {
        return namespaceResolver;
    }

    /**
     * Get the value of a particular property of the instruction. Properties
     * of XSLT instructions are generally known by the name of the stylesheet attribute
     * that defines them.
     * @param name The name of the required property
     * @return  The value of the requested property, or null if the property is not available
     */

    public Object getProperty(String name) {
        if ("name".equals(name)) {
            return staticContext.getNamePool().getDisplayName(nameCode);
        } else if ("as".equals(name)) {
            return resultType.toString();
        } else {
            return null;
        }
    }

    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property.
     */

    public Iterator getProperties() {
        return new PairIterator("name", "as");
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//