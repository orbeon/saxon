package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;

import java.util.Iterator;
import java.util.Stack;

/**
 *  The ExpressionVisitor supports the various phases of processing of an expression tree which require
 *  a recursive walk of the tree structure visiting each node in turn. In maintains a stack holding the
 *  ancestor nodes of the node currently being visited.
 */

public class ExpressionVisitor {

    private Stack stack;
    private Container container;
    private Executable executable;
    private StaticContext staticContext;
    private Configuration configuration;

    /**
     * Create an ExpressionVisitor
     */

    public ExpressionVisitor() {
        stack = new Stack();
    }

    /**
     * Get the Saxon configuration
     * @return the Saxon configuration
     */

    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Set the Saxon configuration
     * @param configuration the Saxon configuration
     */


    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Get the container of the expressions being visited
     * @return the container
     */

    public Container getContainer() {
        return container;
    }

    /**
     * Set the container of the expressions being visited
     * @param container the container
     */

    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * Get the Executable containing the expressions being visited
     * @return the Executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the Executable containing the expressions being visited
     * @param executable the Executable
     */

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Get the stack containing all the expressions currently being visited
     * @return the expression stack holding all the containing expressions of the current expression;
     * the objects on this Stack are instances of {@link Expression}
     */

    public Stack getStack() {
        return stack;
    }

    /**
     * Set the stack used to hold the expressions being visited
     * @param stack the expression stack
     */

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    /**
     * Get the static context for the expressions being visited. Note: this may not reflect all changes
     * in static context (e.g. namespace context, base URI) applying to nested expressions
     * @return the static context
     */

    public StaticContext getStaticContext() {
        return staticContext;
    }

    /**
     * Set the static context for the expressions being visited. Note: this may not reflect all changes
     * in static context (e.g. namespace context, base URI) applying to nested expressions
     * @param staticContext the static context
     */

    public void setStaticContext(StaticContext staticContext) {
        this.staticContext = staticContext;
    }

    /**
     * Get the current expression, the one being visited
     * @return the current expression
     */

    public Expression getCurrentExpression() {
        return (Expression)stack.peek();
    }

    /**
     * Factory method: make an expression visitor
     * @param env the static context
     * @return the new expression visitor
     */

    public static ExpressionVisitor make(StaticContext env) {
        ExpressionVisitor visitor = new ExpressionVisitor();
        visitor.setStaticContext(env);
        visitor.setConfiguration(env.getConfiguration());
        return visitor;
    }

    /**
     * Simplify an expression, via the ExpressionVisitor
     * @param exp the expression to be simplified
     * @return the simplified expression
     * @throws XPathException
     */

    public Expression simplify(Expression exp) throws XPathException {
        if (exp != null) {
            stack.push(exp);
            Expression exp2 = exp.simplify(this);
            stack.pop();
            return exp2;
        } else {
            return null;
        }
    }

    /**
     * Type check an expression, via the ExpressionVisitor
     * @param exp the expression to be typechecked
     * @param contextItemType the static type of the context item for this expression
     * @return the expression that results from type checking (this may be wrapped in expressions that
     * perform dynamic checking of the item type or cardinality, or that perform atomization or numeric
     * promotion)
     * @throws XPathException if static type checking fails, that is, if the expression cannot possibly
     * deliver a value of the required type
     */

    public Expression typeCheck(Expression exp, ItemType contextItemType) throws XPathException {
        if (exp != null) {
            stack.push(exp);
            Expression exp2 = exp.typeCheck(this, contextItemType);
            stack.pop();
            return exp2;
        } else {
            return null;
        }
    }

   /**
     * Optimize an expression, via the ExpressionVisitor
     * @param exp the expression to be typechecked
     * @param contextItemType the static type of the context item for this expression
     * @return the rewritten expression
     * @throws XPathException
     */

    public Expression optimize(Expression exp, ItemType contextItemType) throws XPathException {
        if (exp != null) {
            stack.push(exp);
            Expression exp2 = exp.optimize(this, contextItemType);
            stack.pop();
            return exp2;
        } else {
            return null;
        }
    }

    /**
     * Get the parent expression of the current expression in the expression tree
     * @return the parent of the current expression (or null if this is the root)
     */

    public Expression getParentExpression() {
        int pos = stack.size() - 2;
        if (pos > 0) {
            return (Expression)stack.get(pos);
        } else {
            return null;
        }
    }

    /**
     * Return true if the current expression at the top of the visitor's stack is evaluated repeatedly
     * when a given ancestor expression is evaluated once
     * @param ancestor the ancestor expression. May be null, in which case the search goes all the way
     * to the base of the stack.
     * @return true if the current expression is evaluated repeatedly
     */

    public boolean isLoopingSubexpression(Expression ancestor) {
        int top = stack.size()-1;
        while (true) {
            if (top <= 0) {
                return false;
            }
            Expression parent = (Expression)stack.get(top - 1);
            if (parent.hasLoopingSubexpression(((Expression)stack.get(top)))) {
                return true;
            }
            if (parent == ancestor) {
                return false;
            }
            top--;
        }
    }

   /**
     * Reset the static properties for the current expression and for all its containing expressions.
     * This should be done whenever the expression is changed in a way that might
     * affect the properties. It causes the properties to be recomputed next time they are needed.
     */

    public final void resetStaticProperties() {
       Iterator up = stack.iterator();
       while (up.hasNext()) {
           Expression exp = (Expression)up.next();
           exp.resetLocalStaticProperties();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

