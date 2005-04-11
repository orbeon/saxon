package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.om.Validation;

import java.util.Iterator;

/**
 * An abstract class to act as a common parent for instructions that create element nodes
 * and document nodes.
 */

public abstract class ParentNodeConstructor extends Instruction {

    protected Expression content;
    private boolean lazyConstruction = false;
    int validation = Validation.PRESERVE;
    SchemaType schemaType;


    public ParentNodeConstructor() {}

    /**
     * Indicate that lazy construction should (or should not) be used
     * @param lazy set to true if lazy construction should be used
     */

    public void setLazyConstruction(boolean lazy) {
        lazyConstruction = lazy;
    }

    /**
     * Establish whether lazy construction is to be used
     */

    public final boolean isLazyConstruction() {
        return lazyConstruction;
    }

    /**
     * Get the validation mode for this instruction
     * @return the validation mode, for example {@link Validation#STRICT} or {@link Validation#PRESERVE}
     */
    public int getValidationAction() {
        return validation;
    }

    /**
     * Set the expression that constructs the content of the element
     */

    public void setContentExpression(Expression content) {
        this.content = content;
        adoptChildExpression(content);
    }

    /**
     * Get the expression that constructs the content of the element
     */

    public Expression getContentExpression() {
        return content;
    }


    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @return the simplified expression
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(StaticContext env) throws XPathException {
        content = content.simplify(env);
        return this;
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @return the original expression, rewritten to perform necessary
     *         run-time type checks, and to perform other type-related
     *         optimizations
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        content = content.analyze(env, contextItemType);
        return this;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws net.sf.saxon.trans.XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        content = content.promote(offer);
    }

    /**
      * Get the immediate sub-expressions of this expression.
      * @return an iterator containing the sub-expressions of this expression
      */

    public Iterator iterateSubExpressions() {
        return new MonoIterator(content);
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true.
     */

    public final boolean createsNewNodes() {
        return true;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Check that lazy construction is possible for this element
     */

    void verifyLazyConstruction() {
        if (!isLazyConstruction()) {
            return;
        }
        // Lazy construction is not possible if the expression depends on the values of position() or last(),
        // as we can't save these.
        if ((getDependencies() & (StaticProperty.DEPENDS_ON_POSITION | StaticProperty.DEPENDS_ON_LAST)) != 0) {
            setLazyConstruction(false);
        }
        // Lazy construction is not possible if validation is required
        if (validation == Validation.STRICT || validation == Validation.LAX
                || schemaType != null) {
            setLazyConstruction(false);
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

