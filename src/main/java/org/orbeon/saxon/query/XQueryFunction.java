package org.orbeon.saxon.query;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.ExecutableFunctionLibrary;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XQueryFunction implements InstructionInfo, Container, Declaration {
    private StructuredQName functionName;
    private List arguments;              // A list of UserFunctionParameter objects
    private SequenceType resultType;
    private Expression body = null;
    private List references = new ArrayList(10);
    private int lineNumber;
    private int columnNumber;
    private String systemId;
    private Executable executable;
    private UserFunction compiledFunction = null;
    private boolean memoFunction;
    private NamespaceResolver namespaceResolver;
    private QueryModule staticContext;
    private boolean isUpdating = false;

    /**
     * Create an XQuery function
     */

    public XQueryFunction() {
        arguments = new ArrayList(8);
    }

    /**
     * Set the name of the function
     * @param name the name of the function as a StructuredQName object
     */

    protected void setFunctionName(StructuredQName name) {
        functionName = name;
    }

    /**
     * Set the arguments of the function
     * @param arguments a list of the arguments of the function, as a list whose
     * members are instances of {@link UserFunctionParameter}
     */

//    protected void setArgumentList(List arguments) {
//        this.arguments = arguments;
//    }

    /**
     * Add an argument to the list of arguments
     * @param argument the formal declaration of the argument to be added
     */

    protected void addArgument(UserFunctionParameter argument) {
        arguments.add(argument);
    }

    /**
     * Set the required result type of the function
     * @param resultType the declared result type of the function
     */

    protected void setResultType(SequenceType resultType) {
        this.resultType = resultType;
    }

    /**
     * Set the body of the function
     * @param body the expression forming the body of the function
     */

    protected void setBody(Expression body) {
        this.body = body;
        if (body != null) {
            body.setContainer(this);
        }
    }

    /**
     * Get the body of the function
     * @return the expression making up the body of the function
     */

    public Expression getBody() {
        return body;
    }

    /**
     * Set the system ID of the module containing the function
     * @param systemId the system ID (= base URI) of the module containing the function
     */

    protected void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Set the line number of the function declaration within its module
     * @param line the line number of the function declaration
     */

    protected void setLineNumber(int line) {
        lineNumber = line;
    }

    /**
     * Set the column number of the function declaration
     * @param column the column number of the function declaration
     */

    protected void setColumnNumber(int column) {
        columnNumber = column;
    }

    /**
     * Get the name of the function as a structured QName
     * @return the name of the function as a structured QName
     */

    public StructuredQName getFunctionName() {
        return functionName;
    }

    /**
     * Get the name of the function for display in error messages
     * @return the name of the function as a lexical QName
     */

    public String getDisplayName() {
        return functionName.getDisplayName();
    }

    /**
     * Get an identifying key for this function, which incorporates the URI and local part of the
     * function name plus the arity
     * @return an identifying key
     */

    public String getIdentificationKey() {
        return functionName.getClarkName() + '/' + arguments.size();
    }

    /**
     * Construct what the identification key would be for a function with given URI, local name, and arity
     * @param uri the URI part of the function name
     * @param localName the local part of the function name
     * @param arity the number of arguments in the function
     * @return an identifying key
     */

    public static String getIdentificationKey(String uri, String localName, int arity) {
        FastStringBuffer sb = new FastStringBuffer(uri.length() + localName.length() + 8);
        sb.append('{');
        sb.append(uri);
        sb.append('}');
        sb.append(localName);
        sb.append('/');
        sb.append(arity+"");
        return sb.toString();
    }

    /**
     * Construct what the identification key would be for a function with given URI, local name, and arity
     * @param qName the name of the function
     * @param arity the number of arguments
     * @return an identifying key
     */

    public static String getIdentificationKey(StructuredQName qName, int arity) {
        String uri = qName.getNamespaceURI();
        String localName = qName.getLocalName();
        FastStringBuffer sb = new FastStringBuffer(uri.length() + localName.length() + 8);
        sb.append('{');
        sb.append(uri);
        sb.append('}');
        sb.append(localName);
        sb.append('/');
        sb.append(arity+"");
        return sb.toString();
    }

    /**
     * Get the result type of the function
     * @return the declared result type
     */

    public SequenceType getResultType() {
        return resultType;
    }

    /**
     * Set the executable in which this function is contained
     * @param exec the executable
     */

    public void setExecutable(Executable exec) {
        executable = exec;
    }

    /**
     * Get the executable in which this function is contained
     * @return the executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     * @return the location provider
     */

    public LocationProvider getLocationProvider() {
        return executable.getLocationMap();
    }

    /**
     * Set the static context for this function
     * @param env the static context for the module in which the function is declared
     */

    public void setStaticContext(QueryModule env) {
        staticContext = env;
    }

    /**
     * Get the static context for this function
     * @return the static context for the module in which the function is declared
     */

    public StaticContext getStaticContext() {
        return staticContext;
    }

    /**
     * Get the declared types of the arguments of this function
     * @return an array, holding the types of the arguments in order
     */

    public SequenceType[] getArgumentTypes() {
        SequenceType[] types = new SequenceType[arguments.size()];
        for (int i=0; i<arguments.size(); i++) {
            types[i] = ((UserFunctionParameter)arguments.get(i)).getRequiredType();
        }
        return types;
    }

    /**
     * Get the definitions of the arguments to this function
     * @return an array of UserFunctionParameter objects, one for each argument
     */

    public UserFunctionParameter[] getParameterDefinitions() {
        UserFunctionParameter[] params = new UserFunctionParameter[arguments.size()];
        return (UserFunctionParameter[])arguments.toArray(params);
//        for (int i=0; i<arguments.size(); i++) {
//            RangeVariable decl = ((RangeVariable)arguments.get(i));
//            SequenceType type = decl.getRequiredType();
//            UserFunctionParameter param = new UserFunctionParameter();
//            param.setRequiredType(type);
//            param.setVariableQName(decl.getVariableQName());
//            params[i] = param;
//        }
//        return params;
    }

    /**
     * Get the arity of the function
     * @return the arity (the number of arguments)
     */

    public int getNumberOfArguments() {
        return arguments.size();
    }

    /**
     * Register a call on this function
     * @param ufc a user function call that references this function.
     */

    public void registerReference(UserFunctionCall ufc) {
        references.add(ufc);
    }

    /**
     * Set that this is, or is not, a memo function. A memo function remembers the results of calls
     * on the function so that the a subsequent call with the same arguments simply look up the result
     * @param isMemoFunction true if this is a memo function.
     */

    public void setMemoFunction(boolean isMemoFunction) {
        memoFunction = isMemoFunction;
    }

    /**
     * Find out whether this is a memo function
     * @return true if this is a memo function
     */

    public boolean isMemoFunction() {
        return memoFunction;
    }

    /**
     * Set whether this is an updating function (as defined in XQuery Update)
     * @param isUpdating true if this is an updating function
     */

    public void setUpdating(boolean isUpdating) {
        this.isUpdating = isUpdating;
    }

    /**
     * Ask whether this is an updating function (as defined in XQuery Update)
     * @return true if this is an updating function
     */

    public boolean isUpdating() {
        return isUpdating;
    }


    /**
     * Compile this function to create a run-time definition that can be interpreted (note, this
     * has nothing to do with Java code generation)
     * @throws XPathException if errors are found
     */

    public void compile() throws XPathException {
        Configuration config = staticContext.getConfiguration();
        try {
            // If a query function is imported into several modules, then the compile()
            // method will be called once for each importing module. If the compiled
            // function already exists, then this is a repeat call, and the only thing
            // needed is to fix up references to the function from within the importing
            // module.

            if (compiledFunction == null) {
                SlotManager map = config.makeSlotManager();
                UserFunctionParameter[] params = getParameterDefinitions();
                for (int i=0; i<params.length; i++) {
                    params[i].setSlotNumber(i);
                    map.allocateSlotNumber(params[i].getVariableQName());
                }

                // type-check the body of the function

                ExpressionVisitor visitor = ExpressionVisitor.make(staticContext);
                visitor.setExecutable(getExecutable());
                body = visitor.simplify(body);
                body = visitor.typeCheck(body, null);
                //body = visitor.optimize(body, null);

                // Try to extract new global variables from the body of the function
                //body = config.getOptimizer().promoteExpressionsToGlobal(body, visitor);
                
                body.setContainer(this);
                RoleLocator role =
                        new RoleLocator(RoleLocator.FUNCTION_RESULT, functionName, 0);
                //role.setSourceLocator(this);
                body = TypeChecker.staticTypeCheck(body, resultType, false, role, visitor);

                if (config.isCompileWithTracing()) {
                    namespaceResolver = staticContext.getNamespaceResolver();
                    TraceExpression trace = new TraceExpression(body);
                    trace.setLineNumber(lineNumber);
                    trace.setColumnNumber(columnNumber);
                    trace.setSystemId(staticContext.getBaseURI());
                    trace.setConstructType(StandardNames.XSL_FUNCTION);
                    trace.setObjectName(functionName);
                    trace.setLocationId(staticContext.getLocationMap().allocateLocationId(systemId, lineNumber));
                    body = trace;
                }

                compiledFunction = new UserFunction(body);
                compiledFunction.setHostLanguage(Configuration.XQUERY);
                compiledFunction.setFunctionName(functionName);
                compiledFunction.setParameterDefinitions(params);
                compiledFunction.setResultType(getResultType());
                compiledFunction.setLineNumber(lineNumber);
                compiledFunction.setSystemId(systemId);
                compiledFunction.setExecutable(executable);
                compiledFunction.setStackFrameMap(map);
                compiledFunction.setMemoFunction(memoFunction);
                compiledFunction.setUpdating(isUpdating);

                for (int i=0; i<params.length; i++) {
                    UserFunctionParameter param = params[i];
                    int refs = ExpressionTool.getReferenceCount(body, param, false);
                    param.setReferenceCount(refs);
                }

            }

            // bind all references to this function to the UserFunction object

            fixupReferences(staticContext);

            // register this function with the function library available at run-time (e.g. for saxon:evaluate())

            if (executable.getFunctionLibrary() instanceof ExecutableFunctionLibrary) {
                ExecutableFunctionLibrary lib  = (ExecutableFunctionLibrary)executable.getFunctionLibrary();
                lib.addFunction(compiledFunction);
            } else {
                throw new AssertionError("executable.getFunctionLibrary() is an instance of " +
                        executable.getFunctionLibrary().getClass().getName());
            }

            //compiledFunction.computeEvaluationMode();
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            throw e;
        }
    }

    /**
     * Optimize the body of this function
     */

    public void optimize() throws XPathException {
        body.checkForUpdatingSubexpressions();
        if (isUpdating) {
            if (!ExpressionTool.isAllowedInUpdatingContext(body)) {
                XPathException err = new XPathException(
                         "The body of an updating function must be an updating expression", "XUST0002");
                err.setLocator(body);
                throw err;
            }
        } else {
            //body.checkForUpdatingSubexpressions();
            if (body.isUpdatingExpression()) {
                XPathException err = new XPathException(
                         "The body of a non-updating function must be a non-updating expression", "XUST0001");
                err.setLocator(body);
                throw err;
            }
        }
        ExpressionVisitor visitor = ExpressionVisitor.make(staticContext);
        visitor.setExecutable(getExecutable());
        body = visitor.optimize(body, null);

        // Try to extract new global variables from the body of the function
        body = staticContext.getConfiguration().getOptimizer().promoteExpressionsToGlobal(body, visitor);

        // mark tail calls within the function body

        int arity = arguments.size();
        if (!isUpdating) {
            int tailCalls = ExpressionTool.markTailFunctionCalls(body, functionName, arity);
            if (tailCalls != 0) {
                compiledFunction.setBody(body);
                compiledFunction.setTailRecursive(tailCalls > 0, tailCalls > 1);
                body = new TailCallLoop(compiledFunction);
            }
        }

        body.setContainer(this);
        compiledFunction.setBody(body);
        compiledFunction.computeEvaluationMode();
        ExpressionTool.allocateSlots(body, arity, compiledFunction.getStackFrameMap());
    }

    /**
     * Fix up references to this function
     * @param env the static context
     */

    public void fixupReferences(StaticContext env) throws XPathException {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            UserFunctionCall ufc = (UserFunctionCall)iter.next();
            ufc.setFunction(compiledFunction);
            ufc.computeArgumentEvaluationModes();
        }
    }

    /**
     * Type-check references to this function
     * @param visitor the expression visitor
     */

    public void checkReferences(ExpressionVisitor visitor) throws XPathException {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            UserFunctionCall ufc = (UserFunctionCall)iter.next();
            ufc.checkFunctionCall(compiledFunction, visitor);
            ufc.computeArgumentEvaluationModes();
        }

        // clear the list of references, so that more can be added in another module
        references = new ArrayList(0);

    }

    /**
     * Produce diagnostic output showing the compiled and optimized expression tree for a function
     * @param out the destination to be used
     */
    public void explain(ExpressionPresenter out) {
        out.startElement("declareFunction");
        out.emitAttribute("name", functionName.getDisplayName());
        out.emitAttribute("arity", ""+getNumberOfArguments());
        if (compiledFunction == null) {
            out.emitAttribute("unreferenced", "true");
        } else {
            if (compiledFunction.isMemoFunction()) {
                out.emitAttribute("memo", "true");
            }
            out.emitAttribute("tailRecursive", (compiledFunction.isTailRecursive() ? "true" : "false"));
            body.explain(out);
        }
        out.endElement();
    }

    /**
     * Get the callable compiled function contained within this XQueryFunction definition.
     * @return the compiled function object
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
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     */

    public StructuredQName getObjectName() {
        return functionName;
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

    public String getSystemId(long locationId) {
        return getSystemId();
    }

    public int getLineNumber(long locationId) {
        return getLineNumber();
    }

    public int getColumnNumber(long locationId) {
        return getColumnNumber();
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
            return functionName.getDisplayName();
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

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     * @return typically {@link org.orbeon.saxon.Configuration#XSLT} or {@link org.orbeon.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage() {
        return Configuration.XQUERY;
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
         boolean found = false;
         if (body == original) {
             body = replacement;
             found = true;
         }
         return found;
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