package org.orbeon.saxon.expr;
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
    */

    public ItemType getItemType() {
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

    /**
    * Mark tail-recursive calls on stylesheet functions. For most expressions, this does nothing.
    */

    public boolean markTailFunctionCalls() {
        tailRecursive = true;
        return true;
    }

    // TODO: attempt to establish whether the function is capable of creating new nodes. This
    // enables non-creative functions to be moved out of loops. The problem is how to achieve this
    // without looping in the case of recursive functions. A simple solution might be to go only
    // one level deep: if the body of a function is known (without analysing function calls) to be
    // non-creative, then all calls on that function can be marked as non-creative. Note also that
    // a function is creative if one of its arguments is creative and the result of the function
    // depends on the identity of that argument.

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
        if (result instanceof FunctionCallPackage) {
            return SingletonIterator.makeIterator(((FunctionCallPackage)result));
        }
        return Value.getIterator(result);
    }

    /**
     * This is the method that actually does the function call
     * @param c the dynamic context
     * @return the result of the function
     * @throws XPathException if dynamic errors occur
     */
    private ValueRepresentation callFunction(XPathContext c) throws XPathException {
        int numArgs = argument.length;

        ValueRepresentation[] actualArgs = new ValueRepresentation[numArgs];
        for (int i=0; i<numArgs; i++) {
            // Decide what form of lazy evaluation to use based on the number of references to the argument
            int refs = function.getParameterDefinitions()[i].getReferenceCount();
            if (argument[i] instanceof Value) {
                actualArgs[i] = (Value)argument[i];
            } else {
                if (refs == 0) {
                    // the argument is never referenced, so don't evaluate it
                    actualArgs[i] = EmptySequence.getInstance();
                } else if ((argument[i].getDependencies() & StaticProperty.DEPENDS_ON_USER_FUNCTIONS) != 0) {
                    // if the argument contains a call to a user-defined function, then it might be a recursive call.
                    // It's better to evaluate it now, rather than waiting until we are on a new stack frame, as
                    // that can blow the stack if done repeatedly.
                    actualArgs[i] = ExpressionTool.eagerEvaluate(argument[i], c);
                } else {
                    boolean keep = (refs > 1);
                    actualArgs[i] = ExpressionTool.lazyEvaluate(argument[i], c, keep);
                }
            }
            // If the argument has come in as a (non-memo) closure but there are multiple references to it,
            // then we materialize it in memory now. This shouldn't really happen but it does (tour.xq)
            if (refs > 1 && actualArgs[i] instanceof Closure && !(actualArgs[i] instanceof MemoClosure)) {
                actualArgs[i] = ((Closure)actualArgs[i]).reduce();
            }
        }

        if (tailRecursive) {
            return new FunctionCallPackage(function, actualArgs, c);
        }

        XPathContextMajor c2 = c.newCleanContext();
        c2.setOrigin(this);
        try {
            return function.call(actualArgs, c2, true);
        } catch (StackOverflowError err) {
            throw new DynamicError("Too many nested function calls. May be due to infinite recursion.", this);
        }
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
        c2.openStackFrame(suppliedArguments.length);
        for (int i=0; i<suppliedArguments.length; i++) {
            c2.setLocalVariable(i, suppliedArguments[i]);
            convertedArgs[i] = ExpressionTool.lazyEvaluate(argument[i], c2, true);
        }
        XPathContextMajor c3 = c2.newCleanContext();
        c3.setOrigin(this);
        return function.call(convertedArgs, c3, true);
    }

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "function " + getDisplayName(pool) +
                (tailRecursive ? " (tail call)" : ""));
        for (int a=0; a<argument.length; a++) {
            argument[a].display(level+1, pool, out);
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

    /**
    * Inner class used to wrap up the set of actual arguments to a tail-recursive call of
    * the containing function. This argument package is passed back to the calling code
    * in place of a function result; the caller then loops to re-invoke the function
    * with these arguments, avoiding the creation of an additional stack frame.
    */

    public class FunctionCallPackage extends ObjectValue {

        private UserFunction function;
        private ValueRepresentation[] actualArgs;
        private XPathContext evaluationContext;

        public FunctionCallPackage(UserFunction function, ValueRepresentation[] actualArgs, XPathContext c) {
            super(function);
            this.function = function;
            this.actualArgs = actualArgs;
            this.evaluationContext = c;
        }

        /**
         * Determine the item type of the expression
         */

        public ItemType getItemType() {
            return UserFunctionCall.this.getItemType();
        }

        public ValueRepresentation call() throws XPathException {
            XPathContextMajor c2 = evaluationContext.newCleanContext();
            c2.setOrigin(UserFunctionCall.this);
            return function.call(actualArgs, c2, false);
        }

        public SequenceIterator iterateResults(XPathContext context) throws XPathException {
            ValueRepresentation result = call();
            return new MappingIterator(Value.getIterator(result), new Flattener(), context);
        }


        public ValueRepresentation appendTo(SequenceReceiver out) throws XPathException {
            // TODO: this method could be combined with the only method that calls it
            ValueRepresentation v = call();
            SequenceIterator fv = Value.getIterator(v);
            while (true) {
                Item fvit = fv.next();
                if (fvit == null) return null;
                if (fvit instanceof UserFunctionCall.FunctionCallPackage) {
                    return (Value)fvit;
                } else {
                    out.append(fvit, locationId, NodeInfo.ALL_NAMESPACES);
                }
            }
        }

         /**
         * Reduce a value to its simplest form. If the value is a closure or some other form of deferred value
         * such as a FunctionCallPackage, then it is reduced to a SequenceExtent. If it is a SequenceExtent containing
         * a single item, then it is reduced to that item. One consequence that is exploited by class FilterExpression
         * is that if the value is a singleton numeric value, then the result will be an instance of NumericValue
         */

        public Value reduce() throws XPathException {
            return new SequenceExtent(iterateResults(null)).reduce();
        }

        /**
         * Get the primitive value (the value in the value space). This returns an
         * AtomicValue of a class that would be used to represent the primitive value.
         * In effect this means that for built-in types, it returns the value itself,
         * but for user-defined type, it returns the primitive value minus the type
         * annotation. Note that getItemType() when applied to the result of this
         * function does not not necessarily return a primitive type: for example, this
         * function may return a value of type xdt:dayTimeDuration, which is not a
         * primitive type as defined by {@link net.sf.saxon.type.Type#isPrimitiveType(int)}
         */

        public AtomicValue getPrimitiveValue() {
            try {
                return ((AtomicValue)reduce()).getPrimitiveValue();
            } catch (XPathException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Mapping function that converts a sequence possibly containing embedded FunctionCallPackage items into
     * one in which any such items are fully expanded
     */

    public static class Flattener implements MappingFunction {

        //TODO: make this a singleton class
        /**
         * Map one item to a sequence.
         *
         * @param item    The item to be mapped.
         *                If context is supplied, this must be the same as context.currentItem().
         * @param context The processing context. Some mapping functions use this because they require
         *                context information. Some mapping functions modify the context by maintaining the context item
         *                and position. In other cases, the context may be null.
         * @return either (a) a SequenceIterator over the sequence of items that the supplied input
         *         item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
         *         sequence.
         */

        public Object map(Item item, XPathContext context) throws XPathException {
            if (item instanceof FunctionCallPackage) {
                return (((FunctionCallPackage)item).iterateResults(context));
            } else {
                return item;
            }
        }


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
