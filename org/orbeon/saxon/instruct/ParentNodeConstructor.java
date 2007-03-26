package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.SimpleType;

import java.util.Iterator;

/**
 * An abstract class to act as a common parent for instructions that create element nodes
 * and document nodes.
 */

public abstract class ParentNodeConstructor extends Instruction {

    protected Expression content;
    private boolean lazyConstruction = false;
    private boolean namespaceSensitiveType;
    int validation = Validation.PRESERVE;
    private SchemaType schemaType;
    private String baseURI;


    public ParentNodeConstructor() {}

    public void setBaseURI(String uri) {
        baseURI = uri;
    }

    protected String getBaseURI() {
        return baseURI;
    }

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
     * Set the schema type to be used for validation
     */

    public void setSchemaType(SchemaType type) {
        schemaType = type;
        namespaceSensitiveType = (type instanceof SimpleType) && ((SimpleType)type).isNamespaceSensitive();
    }

    /**
     * Get the schema type chosen for validation; null if not defined
     */

    public SchemaType getSchemaType() {
        return schemaType;
    }

    /**
     * Determine whether the schema type is namespace sensitive. The result is undefined if schemaType is null.
     */

    public boolean isNamespaceSensitive() {
        return namespaceSensitiveType;
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
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(StaticContext env) throws XPathException {
        content = content.simplify(env);
        return this;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        content = content.typeCheck(env, contextItemType);
        adoptChildExpression(content);
        verifyLazyConstruction();
        checkContentForAttributes(env);
        return this;
    }

    protected abstract void checkContentForAttributes(StaticContext env) throws XPathException;

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        content = content.optimize(opt, env, contextItemType);
        adoptChildExpression(content);
        return this;
    }


    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws org.orbeon.saxon.trans.XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (offer.action != PromotionOffer.UNORDERED) {
            content = doPromotion(content, offer);
        }
    }

    /**
      * Get the immediate sub-expressions of this expression.
      * @return an iterator containing the sub-expressions of this expression
      */

    public Iterator iterateSubExpressions() {
        return new MonoIterator(content);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (content == original) {
            content = replacement;
            found = true;
        }
                return found;
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

