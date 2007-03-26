package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.instruct.InstructionDetails;
import org.orbeon.saxon.instruct.UserFunction;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.InstructionInfoProvider;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

import java.io.PrintStream;


/**
 * This class represents a call to a function defined in the stylesheet or query.
 * It is used for all user-defined functions in XQuery, and for a limited class of
 * user-defined functions in XSLT: those that can be reduced to the evaluation
 * of a single expression.
*/

public class UserFunctionCall extends FunctionCall implements InstructionInfoProvider {

    private SequenceType staticType;
    private UserFunction function;
    private boolean tailRecursive = false;
    private boolean confirmed = false;
        // A functionCall is confirmed if the function being called has been located. Generally in this
        // case the value of 'function' will be non-null; but in XSLT it is possible to look-ahead to confirm
        // a function binding before the relevant function is actually compiled.
    private int[] argumentEvaluationModes = null;

    public UserFunctionCall() {}

    /**
    * Set the static type
    */

    public void setStaticType(SequenceType type) {
        staticType = type;
    }

    /**
    * Create the reference to the function to be called, and validate for consistency
    */

    public void setFunction(UserFunction compiledFunction,
                            StaticContext env) throws XPathException {
        function = compiledFunction;
        confirmed = true;
    }

    /**
    * Check the function call against the declared function signature
    */

    public void checkFunctionCall(UserFunction compiledFunction,
                            StaticContext env) throws XPathException {
        int n = compiledFunction.getNumberOfArguments();
        for (int i=0; i<n; i++) {
            RoleLocator role = new RoleLocator(
                    RoleLocator.FUNCTION, new Integer(compiledFunction.getFunctionNameCode()), i, env.getNamePool());
            role.setSourceLocator(this);
            argument[i] = TypeChecker.staticTypeCheck(
                                argument[i],
                                compiledFunction.getArgumentType(i),
                                false,
                                role, env);
        }
    }


    /**
     * Get the function that is being called by this function call
     */

    public UserFunction getFunction() {
        return function;
    }

    /**
     * Set this function as confirmed (the function being called is known to exist) or not
     */

    public void setConfirmed(boolean conf) {
        confirmed = conf;
    }

    /**
     * Determine whether this function call is confirmed
     */

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Get the arguments (the expressions supplied in the function call)
     */

    public Expression[] getArguments() {
        return argument;
    }

    /**
    * Method called during the type checking phase
    */

    public void checkArguments(StaticContext env) throws XPathException {
        // these checks are now done in setFunction(), at the time when the function
        // call is bound to an actual function
    }

    /**
    * Pre-evaluate a function at compile time. This version of the method suppresses
    * early evaluation by doing nothing.
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
    * Determine the data type of the expression, if possible
    * @return Type.ITEM (meaning not known in advance)
     * @param th
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

    /**
    * Simplify the function call
    */

     public Expression simplify(StaticContext env) throws XPathException {
        for (int i=0; i<argument.length; i++) {
            argument[i] = argument[i].simplify(env);
        }
        return this;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        Expression e = super.typeCheck(env, contextItemType);
        if (e == this && function != null) {
            computeArgumentEvaluationModes();
        }
        return e;        
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(opt, env, contextItemType);
        if (e == this && function != null) {
            computeArgumentEvaluationModes();
        }
        return e;
    }

    public void computeArgumentEvaluationModes() {
        argumentEvaluationModes = new int[argument.length];
        for (int i=0; i<argument.length; i++) {
            int refs = function.getParameterDefinitions()[i].getReferenceCount();
            if (refs == 0) {
                // the argument is never referenced, so don't evaluate it
                argumentEvaluationModes[i] = ExpressionTool.RETURN_EMPTY_SEQUENCE;
            } else if ((argument[i].getDependencies() & StaticProperty.DEPENDS_ON_USER_FUNCTIONS) != 0) {
                // if the argument contains a call to a user-defined function, then it might be a recursive call.
                // It's better to evaluate it now, rather than waiting until we are on a new stack frame, as
                // that can blow the stack if done repeatedly. (See test func42)
                argumentEvaluationModes[i] = ExpressionTool.eagerEvaluationMode(argument[i]);
            } else {
                argumentEvaluationModes[i] = ExpressionTool.lazyEvaluationMode(argument[i]);
            }
        }
    }

    /**
    * Mark tail-recursive calls on stylesheet functions. This marks the function call as tailRecursive if
     * if is a call to the containing function, and in this case it also returns "true" to the caller to indicate
     * that a tail call was found.
    */

    public boolean markTailFunctionCalls(int nameCode, int arity) {
//        tailRecursive = ((nameCode & NamePool.FP_MASK) == (getFunctionNameCode() & NamePool.FP_MASK) &&
//                arity == getNumberOfArguments());
        tailRecursive = true;
        return tailRecursive;
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
        if (val instanceof Item) {
            return (Item)val;
        } else {
            return Value.getIterator(val).next();
        }
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
     * This is the method that actually does the function call
     * @param c the dynamic context
     * @return the result of the function
     * @throws XPathException if dynamic errors occur
     */
    private ValueRepresentation callFunction(XPathContext c) throws XPathException {

        ValueRepresentation[] actualArgs = evaluateArguments(c);

        if (tailRecursive) {
            ((XPathContextMajor)c).requestTailCall(function, actualArgs);
            return EmptySequence.getInstance();
            //return new FunctionCallPackage(function, actualArgs, c);
        }

        XPathContextMajor c2 = c.newCleanContext();
        c2.setOrigin(this);
        try {
            return function.call(actualArgs, c2);
        } catch (StackOverflowError err) {
            throw new DynamicError("Too many nested function calls. May be due to infinite recursion.", this);
        } catch (NullPointerException err) {
            if (function == null) {
                    throw new NullPointerException("Unbound function call " +
                            c2.getConfiguration().getNamePool().getDisplayName(getFunctionNameCode()));
            } else {
                throw err;
            }
        }
    }

    /**
     * Process the function call in push mode
     * @param context
     * @throws XPathException
     */

    public void process(XPathContext context) throws XPathException {
        ValueRepresentation[] actualArgs = evaluateArguments(context);
        if (tailRecursive) {
            ((XPathContextMajor)context).requestTailCall(function, actualArgs);
        } else {
            SequenceReceiver out = context.getReceiver();
            XPathContextMajor c2 = context.newCleanContext();
            c2.setReceiver(out);
            c2.setOrigin(this);
            function.process(actualArgs, c2);
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

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "call " + getDisplayName(config.getNamePool()) +
                (tailRecursive ? " (:tail call:)" : ""));
        for (int a=0; a<argument.length; a++) {
            argument[a].display(level+1, out, config);
        }
    }

    /**
     * Get diagnostic information about this expression
     */

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = new InstructionDetails();
        details.setConstructType(Location.FUNCTION_CALL);
        details.setLineNumber(getLineNumber());
        details.setSystemId(getSystemId());
        details.setObjectNameCode(getFunctionNameCode());
        details.setProperty("expression", this);
        details.setProperty("target", function);
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
