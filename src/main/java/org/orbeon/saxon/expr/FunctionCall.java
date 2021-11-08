package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.NoDynamicContextException;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.value.Value;

import java.util.Arrays;
import java.util.Iterator;

/**
* Abstract superclass for calls to system-defined and user-defined functions
*/

public abstract class FunctionCall extends Expression {

    /**
     * The name of the function
     */

    private StructuredQName name;

    /**
    * The array of expressions representing the actual parameters
    * to the function call
    */

    protected Expression[] argument;

    /**
     * Set the name of the function being called
     * @param name the name of the function
     */

    public final void setFunctionName(StructuredQName name) {
        this.name = name;
    }

    /**
     * Get the qualified of the function being called
     * @return the qualified name 
     */

    public StructuredQName getFunctionName() {
        return name;
    }

    /**
     * Determine the number of actual arguments supplied in the function call
     * @return the arity (the number of arguments)
     */

    public final int getNumberOfArguments() {
        return argument.length;
    }

    /**
     * Method called by the expression parser when all arguments have been supplied
     * @param args the expressions contained in the argument list of the function call
     */

    public void setArguments(Expression[] args) {
        argument = args;
        for (int a=0; a<args.length; a++) {
            adoptChildExpression(args[a]);
        }
    }

    /**
     * Get the expressions supplied as actual arguments to the function
     * @return the array of expressions supplied in the argument list of the function call
     */

    public Expression[] getArguments() {
        return argument;
    }

    /**
    * Simplify the function call. Default method is to simplify each of the supplied arguments and
    * evaluate the function if all are now known.
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        return simplifyArguments(visitor); 
    }

    /**
    * Simplify the arguments of the function.
    * Called from the simplify() method of each function.
    * @return the result of simplifying the arguments of the expression
     * @param visitor an expression visitor
     */

    protected final Expression simplifyArguments(ExpressionVisitor visitor) throws XPathException {
        for (int i=0; i<argument.length; i++) {
            Expression exp = visitor.simplify(argument[i]);
            if (exp != argument[i]) {
                adoptChildExpression(exp);
                argument[i] = exp;
            }
        }
        return this;
    }

    /**
    * Type-check the expression. This also calls preEvaluate() to evaluate the function
    * if all the arguments are constant; functions that do not require this behavior
    * can override the preEvaluate method.
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        boolean fixed = true;
        for (int i=0; i<argument.length; i++) {
            Expression exp = visitor.typeCheck(argument[i], contextItemType);
            if (exp != argument[i]) {
                adoptChildExpression(exp);
                argument[i] = exp;
            }
            if (!(argument[i] instanceof Literal)) {
                fixed = false;
            }
        }
        checkArguments(visitor);
        if (fixed) {
            try {
                return preEvaluate(visitor);
            } catch (NoDynamicContextException err) {
                // Early evaluation failed, typically because the implicit timezone is not yet known.
                // Try again later at run-time.
                return this;
            }
        } else {
            return this;
        }
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        boolean fixed = true;
        for (int i=0; i<argument.length; i++) {
            Expression exp = visitor.optimize(argument[i], contextItemType);
            if (exp != argument[i]) {
                adoptChildExpression(exp);
                argument[i] = exp;
            }
            if (fixed && !(argument[i] instanceof Literal)) {
                fixed = false;
            }
        }
        checkArguments(visitor);
        if (fixed) {
            return preEvaluate(visitor);
        } else {
            return this;
        }
    }

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     * @param visitor an expression visitor
     * @return the result of the early evaluation, or the original expression, or potentially
     * a simplified expression
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        try {
            return Literal.makeLiteral(
                    Value.asValue(SequenceExtent.makeSequenceExtent(
                            iterate(visitor.getStaticContext().makeEarlyEvaluationContext()))));
        } catch (NoDynamicContextException e) {
            // early evaluation failed, usually because implicit timezone required
            return this;
        }
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                for (int i=0; i<argument.length; i++) {
                    argument[i] = doPromotion(argument[i], offer);
                }
            }
            return this;
        }
    }

    /**
     * Method supplied by each class of function to check arguments during parsing, when all
     * the argument expressions have been read
     * @param visitor the expression visitor
    */

    protected abstract void checkArguments(ExpressionVisitor visitor) throws XPathException;

    /**
    * Check number of arguments. <BR>
    * A convenience routine for use in subclasses.
    * @param min the minimum number of arguments allowed
    * @param max the maximum number of arguments allowed
    * @param visitor an expression visitor
     * @return the actual number of arguments
    * @throws org.orbeon.saxon.trans.XPathException if the number of arguments is out of range
    */

    protected int checkArgumentCount(int min, int max, ExpressionVisitor visitor) throws XPathException {
        int numArgs = argument.length;
        if (min==max && numArgs != min) {
            throw new XPathException("Function " + getDisplayName() + " must have "
                    + min + pluralArguments(min),
                    this);
        }
        if (numArgs < min) {
            throw new XPathException("Function " + getDisplayName() + " must have at least "
                    + min + pluralArguments(min),
                    this);
        }
        if (numArgs > max) {
            throw new XPathException("Function " + getDisplayName() + " must have no more than "
                    + max + pluralArguments(max),
                    this);
        }
        return numArgs;
    }

    /**
     * Utility routine used in constructing error messages: get the word "argument" or "arguments"
     * @param num the number of arguments
     * @return the singular or plural word
    */

    private static String pluralArguments(int num) {
        if (num==1) return " argument";
        return " arguments";
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator iterateSubExpressions() {
        try {
            return Arrays.asList(argument).iterator();
        } catch (NullPointerException err) {
            // typically caused by doing CopyLocationInfo after creating the function
            // but before creating its arguments
            throw err;
        }
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        for (int i=0; i<argument.length; i++) {
             if (argument[i] == original) {
                 argument[i] = replacement;
                 found = true;
             }
        }
        return found;
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
     * @param pathMapNodes the node in the PathMap representing the focus at the point where this expression
     *                    is called. Set to null if this expression appears at the top level, in which case the expression, if it
     *                    is registered in the path map at all, must create a new path map root.
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addExternalFunctionCallToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodes) {
        // Except in the case of system functions, we have no idea where a function call might
        // navigate, so we assume the worst, and register that the path has unknown dependencies
        PathMap.PathMapNodeSet result = new PathMap.PathMapNodeSet();
        for (Iterator iter = iterateSubExpressions(); iter.hasNext(); ) {
            Expression child = (Expression)iter.next();
            result.addNodeSet(child.addToPathMap(pathMap, pathMapNodes));
        }
        result.setHasUnknownDependencies();
        return result;
//        AxisExpression axis = new AxisExpression(Axis.ANCESTOR_OR_SELF, AnyNodeTest.getInstance());
//        axis.setContainer(getContainer());
//        PathMap.PathMapNodeSet target = axis.addToPathMap(pathMap, pathMapNodes);
//        axis = new AxisExpression(Axis.ANCESTOR_OR_SELF, AnyNodeTest.getInstance());
//        axis.setContainer(getContainer());
//        return axis.addToPathMap(pathMap, target);
    }

    /**
     * Get the name of the function for display in messages
     * @return  the name of the function as a lexical QName
     */

    public final String getDisplayName() {
        return getFunctionName().getDisplayName();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        FastStringBuffer buff = new FastStringBuffer(120);
        buff.append(getDisplayName());
        Iterator iter = iterateSubExpressions();
        boolean first = true;
        while (iter.hasNext()) {
            buff.append(first ? "(" : ", ");
            buff.append(iter.next().toString());
            first = false;
        }
        buff.append(first ? "()" : ")");
        return buff.toString();
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("functionCall");
        out.emitAttribute("name", getDisplayName());
        for (int a=0; a<argument.length; a++) {
            argument[a].explain(out);
        }
        out.endElement();
    }

    /**
     * Determine whether two expressions are equivalent
     */

    public boolean equals(Object o) {
        if (!(o instanceof FunctionCall)) {
            return false;
        }
        FunctionCall f = (FunctionCall)o;
        if (!getFunctionName().equals(f.getFunctionName())) {
            return false;
        }
        if (getNumberOfArguments() != f.getNumberOfArguments()) {
            return false;
        }
        for (int i=0; i<getNumberOfArguments(); i++) {
            if (!argument[i].equals(f.argument[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get hashCode in support of equals() method
     */

    public int hashCode() {
        int h = getFunctionName().hashCode();
        for (int i=0; i<getNumberOfArguments(); i++) {
            h ^= argument[i].hashCode();
        }
        return h;
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
