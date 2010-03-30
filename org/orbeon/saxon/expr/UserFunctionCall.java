package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.evpull.EmptyEventIterator;
import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.instruct.UserFunction;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;


/**
 * This class represents a call to a user-defined function in the stylesheet or query.
*/

public class UserFunctionCall extends FunctionCall {

    private SequenceType staticType;
    private UserFunction function;
    private boolean tailCall = false;
        // indicates only that this is a tail call, not necessarily a recursive tail call
    private boolean confirmed = false;
        // A functionCall is confirmed if the function being called has been located. Generally in this
        // case the value of 'function' will be non-null; but in XSLT it is possible to look-ahead to confirm
        // a function binding before the relevant function is actually compiled.
    private int[] argumentEvaluationModes = null;

    /**
     * Create a function call to a user-written function in a query or stylesheet
     */

    public UserFunctionCall() {}

    /**
     * Set the static type
     * @param type the static type
     */

    public void setStaticType(SequenceType type) {
        staticType = type;
    }

    /**
     * Create the reference to the function to be called
     * @param compiledFunction the function being called
     */

    public void setFunction(UserFunction compiledFunction) {
        function = compiledFunction;
        confirmed = true;
    }

    /**
     * Check the function call against the declared function signature
     * @param compiledFunction the function being called
     * @param visitor an expression visitor
     */

    public void checkFunctionCall(UserFunction compiledFunction,
                            ExpressionVisitor visitor) throws XPathException {
        Executable executable = visitor.getExecutable();
        boolean isXSLT = executable != null && executable.getHostLanguage() == Configuration.XSLT;
        int n = compiledFunction.getNumberOfArguments();
        for (int i=0; i<n; i++) {
            RoleLocator role = new RoleLocator(
                    RoleLocator.FUNCTION, compiledFunction.getFunctionName(), i);
            if (isXSLT) {
                role.setErrorCode("XTTE0790");
            }
            //role.setSourceLocator(this);
            argument[i] = TypeChecker.staticTypeCheck(
                                argument[i],
                                compiledFunction.getArgumentType(i),
                                false,
                                role, visitor);
        }
    }


    /**
     * Get the function that is being called by this function call
     * @return the function being called
     */

    public UserFunction getFunction() {
        return function;
    }

    /**
     * Set this function as confirmed (the function being called is known to exist) or not
     * @param conf true if the function being called is known to exist
     */

    public void setConfirmed(boolean conf) {
        confirmed = conf;
    }

    /**
     * Determine whether this function call is confirmed
     * @return true if the function being called is known to exist
     */

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Determine whether this is a tail call (not necessarily a recursive tail call)
     * @return true if this function call is a tail call
     */

    public boolean isTailCall() {
        return tailCall;
    }

    /**
     * Determine whether this is a recursive tail call
     * @return true if this function call is a recursive tail call
     */

    public boolean isRecursiveTailCall() {
        return tailCall && getContainer() == function;
    }


    /**
     * Get the arguments (the expressions supplied in the function call)
     * @return the actual expressions used as arguments in the function call
     */

    public Expression[] getArguments() {
        return argument;
    }

    /**
    * Method called during the type checking phase
    */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        // these checks are now done in setFunction(), at the time when the function
        // call is bound to an actual function
    }

   /**
     * Get the qualified of the function being called
     * @return the qualified name
     */

    public final StructuredQName getFunctionName() {
        StructuredQName n = super.getFunctionName();
        if (n == null) {
            return function.getFunctionName();
        } else {
            return n;
        }
    }

    /**
     * Get the evaluation modes that have been determined for each of the arguments
     * @return an array of integers representing the evaluation modes, one for each argument
     */

    public int[] getArgumentEvaluationModes() {
        return argumentEvaluationModes;
    }

    /**
    * Pre-evaluate a function at compile time. This version of the method suppresses
    * early evaluation by doing nothing.
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
    * Determine the data type of the expression, if possible
    * @return Type.ITEM (meaning not known in advance)
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (staticType == null) {
            // the actual type is not known yet, so we return an approximation
            return AnyItemType.getInstance();
        } else {
            return staticType.getPrimaryType();
        }
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_USER_FUNCTIONS;
    }

    /**
     * Determine whether this is an updating expression as defined in the XQuery update specification
     *
     * @return true if this is an updating expression
     */

    public boolean isUpdatingExpression() {
        return function.isUpdating();
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        if (function == null) {
            // not bound yet, we have no way to register the new copy with the XSLFunction
            throw new UnsupportedOperationException("copy");
        }
        UserFunctionCall ufc = new UserFunctionCall();
        ufc.setFunction(function);
        ufc.setStaticType(staticType);
        Expression[] a2 = new Expression[argument.length];
        for (int i=0; i<argument.length; i++) {
            a2[i] = argument[i].copy();
        }
        ufc.argument = a2;
        if (argumentEvaluationModes != null) {
            int[] am2 = new int[argumentEvaluationModes.length];
            System.arraycopy(argumentEvaluationModes, 0, am2, 0, am2.length);
            ufc.argumentEvaluationModes = am2;
        }
        return ufc;
    }

    /**
    * Determine the cardinality of the result
    */

    public int computeCardinality() {
        if (staticType == null) {
            // the actual type is not known yet, so we return an approximation
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return staticType.getCardinality();
        }
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.typeCheck(visitor, contextItemType);
        if (function != null) {
            if (e == this) {
                computeArgumentEvaluationModes();
            }
            if (staticType == SequenceType.ANY_SEQUENCE) {
                // try to get a better type
                staticType = function.getResultType(visitor.getConfiguration().getTypeHierarchy());
            }
        }
        return e;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e == this && function != null) {
            computeArgumentEvaluationModes();
            // TODO: in XSLT, a function call that is a forwards reference is not being inlined, because function==null
            // at the time optimize() is called.
            Expression e2 = visitor.getConfiguration().getOptimizer().tryInlineFunctionCall(
                    this, visitor, contextItemType);
            if (e2 != this) {
                return visitor.optimize(e2, contextItemType);
            }
            return e2;
        }
        return e;
    }

    /**
     * Compute the evaluation mode of each argument
     */

    public void computeArgumentEvaluationModes() {
        argumentEvaluationModes = new int[argument.length];
        for (int i=0; i<argument.length; i++) {
            int refs = function.getParameterDefinitions()[i].getReferenceCount();
            if (refs == 0) {
                // the argument is never referenced, so don't evaluate it
                argumentEvaluationModes[i] = ExpressionTool.RETURN_EMPTY_SEQUENCE;
            } else if (function.getParameterDefinitions()[i].isIndexedVariable()) {
                argumentEvaluationModes[i] = ExpressionTool.MAKE_INDEXED_VARIABLE;
            } else if ((argument[i].getDependencies() & StaticProperty.DEPENDS_ON_USER_FUNCTIONS) != 0) {
                // if the argument contains a call to a user-defined function, then it might be a recursive call.
                // It's better to evaluate it now, rather than waiting until we are on a new stack frame, as
                // that can blow the stack if done repeatedly. (See test func42)
                argumentEvaluationModes[i] = ExpressionTool.eagerEvaluationMode(argument[i]);
            } else {
                int m = ExpressionTool.lazyEvaluationMode(argument[i]);
                if (m == ExpressionTool.MAKE_CLOSURE && refs > 1) {
                    m = ExpressionTool.MAKE_MEMO_CLOSURE;
                }
                argumentEvaluationModes[i] = m;
            }
        }
    }


    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     * <p/>
     * <p>The default implementation of this method assumes that an expression does no navigation other than
     * the navigation done by evaluating its subexpressions, and that the subexpressions are evaluated in the
     * same context as the containing expression. The method must be overridden for any expression
     * where these assumptions do not hold. For example, implementations exist for AxisExpression, ParentExpression,
     * and RootExpression (because they perform navigation), and for the doc(), document(), and collection()
     * functions because they create a new navigation root. Implementations also exist for PathExpression and
     * FilterExpression because they have subexpressions that are evaluated in a different context from the
     * calling expression.</p>
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        return addExternalFunctionCallToPathMap(pathMap, pathMapNodeSet);
    }

    /**
     * Mark tail-recursive calls on stylesheet functions. This marks the function call as tailRecursive if
     * if is a call to the containing function, and in this case it also returns "true" to the caller to indicate
     * that a tail call was found.
    */

    public int markTailFunctionCalls(StructuredQName qName, int arity) {
        tailCall = true;
        return (getFunctionName().equals(qName) &&
               arity == getNumberOfArguments() ? 2 : 1);
    }

    // TODO: attempt to establish whether the function is capable of creating new nodes. This
    // enables non-creative functions to be moved out of loops. The problem is how to achieve this
    // without looping in the case of recursive functions. A simple solution might be to go only
    // one level deep: if the body of a function is known (without analysing function calls) to be
    // non-creative, then all calls on that function can be marked as non-creative. Note also that
    // a function is creative if one of its arguments is creative and the result of the function
    // depends on the identity of that argument.

    public int getImplementationMethod() {
        if (Cardinality.allowsMany(getCardinality())) {
            return ITERATE_METHOD | PROCESS_METHOD;
        } else {
            return EVALUATE_METHOD;
        }
    }

    /**
    * Call the function, returning the value as an item. This method will be used
    * only when the cardinality is zero or one. If the function is tail recursive,
    * it returns an Object representing the arguments to the next (recursive) call
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        ValueRepresentation val = callFunction(c);
        return Value.asItem(val);
    }

    /**
    * Call the function, returning an iterator over the results. (But if the function is
    * tail recursive, it returns an iterator over the arguments of the recursive call)
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        ValueRepresentation result = callFunction(c);
        return Value.getIterator(result);
    }


    /**
     * Evaluate an updating expression, adding the results to a Pending Update List.
     * The default implementation of this method, which is used for non-updating expressions,
     * throws an UnsupportedOperationException
     *
     * @param context the XPath dynamic evaluation context
     * @param pul     the pending update list to which the results should be written
     */

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
        ValueRepresentation[] actualArgs = evaluateArguments(context);
        XPathContextMajor c2 = context.newCleanContext();
        c2.setOrigin(this);
        function.callUpdating(actualArgs, c2, pul);
    }

    /**
     * This is the method that actually does the function call
     * @param c the dynamic context
     * @return the result of the function
     * @throws XPathException if dynamic errors occur
     */
    private ValueRepresentation callFunction(XPathContext c) throws XPathException {
        ValueRepresentation[] actualArgs = evaluateArguments(c);

        if (tailCall) {
            ((XPathContextMajor)c).requestTailCall(function, actualArgs);
            return EmptySequence.getInstance();
        }

        XPathContextMajor c2 = c.newCleanContext();
        c2.setOrigin(this);
        c2.setTemporaryOutputState(true);
        try {
            return function.call(actualArgs, c2);
        } catch (StackOverflowError err) {
            throw new XPathException("Too many nested function calls. May be due to infinite recursion.", this);
        } catch (NullPointerException err) {
            if (function == null) {
                    throw new NullPointerException("Unbound function call " +
                            function.getFunctionName().getDisplayName());
            } else {
                throw err;
            }
        }
    }

    /**
     * Process the function call in push mode
     * @param context the XPath dynamic context
     * @throws XPathException
     */

    public void process(XPathContext context) throws XPathException {
        ValueRepresentation[] actualArgs = evaluateArguments(context);
        if (tailCall) {
            ((XPathContextMajor)context).requestTailCall(function, actualArgs);
        } else {
            SequenceReceiver out = context.getReceiver();
            XPathContextMajor c2 = context.newCleanContext();
            c2.setReceiver(out);
            c2.setOrigin(this);
            function.process(actualArgs, c2);
        }
    }

    /**
     * Process the function call in pull mode
     * @param context the XPath dynamic context
     * @throws XPathException
     */

    public EventIterator iterateEvents(XPathContext context) throws XPathException {
        ValueRepresentation[] actualArgs = evaluateArguments(context);
        if (tailCall) {
            ((XPathContextMajor)context).requestTailCall(function, actualArgs);
            return EmptyEventIterator.getInstance();
        } else {
            SequenceReceiver out = context.getReceiver();
            XPathContextMajor c2 = context.newCleanContext();
            c2.setReceiver(out);
            c2.setOrigin(this);
            return function.iterateEvents(actualArgs, c2);
        }
    }


    private ValueRepresentation[] evaluateArguments(XPathContext c) throws XPathException {
        int numArgs = argument.length;
        ValueRepresentation[] actualArgs = new ValueRepresentation[numArgs];
        if (argumentEvaluationModes == null) {
            // should have been done at compile time
            computeArgumentEvaluationModes();
        }
        for (int i=0; i<numArgs; i++) {

            int refs = function.getParameterDefinitions()[i].getReferenceCount();
            actualArgs[i] = ExpressionTool.evaluate(argument[i], argumentEvaluationModes[i], c, refs);

            if (actualArgs[i] == null) {
                actualArgs[i] = EmptySequence.getInstance();
            }
            // If the argument has come in as a (non-memo) closure but there are multiple references to it,
            // then we materialize it in memory now. This shouldn't really happen but it does (tour.xq)
            if (refs > 1 && actualArgs[i] instanceof Closure && !(actualArgs[i] instanceof MemoClosure)) {
                actualArgs[i] = ((Closure)actualArgs[i]).reduce();
            }
        }
        return actualArgs;
    }

    /**
     * Call the function dynamically. For this to be possible, the static arguments of the function call
     * must have been set up as SuppliedParameterReference objects. The actual arguments are placed on the
     * callee's stack, and the type conversion takes place "in situ".
     * @param suppliedArguments the values to be used for the arguments of the function
     * @param context the dynamic evaluation context
     * @return the result of evaluating the function
     */

    public ValueRepresentation dynamicCall(ValueRepresentation[] suppliedArguments, XPathContext context) throws XPathException {
        ValueRepresentation[] convertedArgs = new ValueRepresentation[suppliedArguments.length];
        XPathContextMajor c2 = context.newCleanContext();
        c2.setOrigin(this);
        c2.setCaller(context);
        c2.openStackFrame(suppliedArguments.length);
        for (int i=0; i<suppliedArguments.length; i++) {
            c2.setLocalVariable(i, suppliedArguments[i]);
            convertedArgs[i] = ExpressionTool.lazyEvaluate(argument[i], c2, 10);
        }
        XPathContextMajor c3 = c2.newCleanContext();
        c3.setOrigin(this);
        return function.call(convertedArgs, c3);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("functionCall");
        out.emitAttribute("name", getDisplayName());
        out.emitAttribute("tailCall", (tailCall ? "true" : "false"));
        for (int a=0; a<argument.length; a++) {
            argument[a].explain(out);
        }
        out.endElement();
    }


    /**
     * Get diagnostic information about this expression
     */

//    public InstructionInfo getInstructionInfo() {
//        InstructionDetails details = new InstructionDetails();
//        details.setConstructType(Location.FUNCTION_CALL);
//        details.setLineAndColumn(getLineNumber());
//        details.setSystemId(getSystemId());
//        details.setObjectName(getFunctionName());
//        details.setProperty("expression", this);
//        details.setProperty("target", function);
//        return details;
//    }

    public int getConstructType() {
        return Location.FUNCTION_CALL;
    }

    public Object getProperty(String name) {
        if (name.equals("target")) {
            return function;
        }
        return super.getProperty(name);
    }

    public StructuredQName getObjectName() {
        return getFunctionName();
    }


    /**
     * Get the line number within the document or module containing a particular location
     *
     * @param locationId identifier of the location in question (as passed down the Receiver pipeline)
     * @return the line number within the document or module.
     */

    public int getLineNumber(long locationId) {
        return getLineNumber();
    }

    /**
     * Get the URI of the document or module containing a particular location
     *
     * @param locationId identifier of the location in question (as passed down the Receiver pipeline)
     * @return the URI of the document or module.
     */

    public String getSystemId(long locationId) {
        return getSystemId();
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
