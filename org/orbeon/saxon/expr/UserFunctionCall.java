package org.orbeon.saxon.expr;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.instruct.InstructionDetails;
import org.orbeon.saxon.instruct.UserFunction;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.InstructionInfoProvider;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.SequenceValue;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.xpath.XPathException;

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
        Value val = callFunction(c);
        if (val instanceof Item) {
            return (Item)val;
        } else {
            return val.iterate(c).next();
        }
    }

    /**
    * Call the function, returning an iterator over the results. (But if the function is
    * tail recursive, it returns an iterator over the arguments of the recursive call)
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        return callFunction(c).iterate(c);
    }

    /**
     * This is the method that actually does the function call
     * @param c the dynamic context
     * @return the result of the function
     * @throws XPathException if dynamic errors occur
     */
    private Value callFunction(XPathContext c) throws XPathException {
        int numArgs = argument.length;

        Value[] actualArgs = new Value[numArgs];
        for (int i=0; i<numArgs; i++) {
            if (argument[i] instanceof Value) {
                actualArgs[i] = (Value)argument[i];
            } else {
                // Decide what form of lazy evaluation to use based on the number of references to the argument
                int refs = function.getParameterDefinitions()[i].getReferenceCount();
                if (refs == 0) {
                    // the argument is never referenced, so don't evaluate it
                    actualArgs[i] = EmptySequence.getInstance();
                } else {
                    boolean keep = (refs > 1);
                    actualArgs[i] = ExpressionTool.lazyEvaluate(argument[i], c, keep);
                }
            }
        }

        if (tailRecursive) {
            return new FunctionCallPackage(function, actualArgs, c);
        }

        XPathContextMajor c2 = c.newCleanContext();
        c2.setOrigin(this);
        return function.call(actualArgs, c2, true);
    }

    /**
     * Call the function dynamically. For this to be possible, the static arguments of the function call
     * must have been set up as SuppliedParameterReference objects. The actual arguments are placed on the
     * callee's stack, and the type conversion takes place "in situ".
     */

    public Value dynamicCall(Value[] suppliedArguments, XPathContext context) throws XPathException {
        Value[] convertedArgs = new Value[suppliedArguments.length];
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

    public class FunctionCallPackage extends SequenceValue {

        private UserFunction function;
        private Value[] actualArgs;
        private XPathContext evaluationContext;

        public FunctionCallPackage(UserFunction function, Value[] actualArgs, XPathContext c) {
            this.function = function;
            this.actualArgs = actualArgs;
            this.evaluationContext = c;
        }

        public Value call() throws XPathException {
            XPathContextMajor c2 = evaluationContext.newCleanContext();
            c2.setOrigin(UserFunctionCall.this);
            return function.call(actualArgs, c2, false);
        }

        public Value appendTo(SequenceReceiver out) throws XPathException {
            Value v = call();
            SequenceIterator fv = v.iterate(evaluationContext);
            while (true) {
                Item fvit = fv.next();
                if (fvit == null) return null;
                if (fvit instanceof UserFunctionCall.FunctionCallPackage) {
                    return (Value)fvit;
                } else {
                    out.append(fvit, locationId);
                }
            }
        }

        /**
         * Determine the data type of the items in the expression, if possible
         *
         * @return AnyItemType (not known)
         */

        public ItemType getItemType() {
            return function.getResultType().getPrimaryType();
        }

        /**
         * Determine the cardinality
         */

        public int getCardinality() {
            return function.getResultType().getCardinality();
        }

        /**
         * Return an Iterator to iterate over the values of a sequence.
         */

        public SequenceIterator iterate(XPathContext context) throws XPathException {
            return call().iterate(context);
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
