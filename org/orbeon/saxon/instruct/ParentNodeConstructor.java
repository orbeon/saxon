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

    /**
     * Create a document or element node constructor instruction
     */

    public ParentNodeConstructor() {}

    /**
     * Set the static base URI of the instruction
     * @param uri the static base URI
     */

    public void setBaseURI(String uri) {
        baseURI = uri;
    }

    /**
     * Get the static base URI of the instruction
     * @return  the static base URI
     */

    public String getBaseURI() {
        return baseURI;
    }

    /**
     * Indicate that lazy construction should (or should not) be used. Note that
     * this request will be ignored if validation is required
     * @param lazy set to true if lazy construction should be used
     */

    public void setLazyConstruction(boolean lazy) {
        lazyConstruction = lazy;
    }

    /**
     * Establish whether lazy construction is to be used
     * @return true if lazy construction is to be used
     */

    public final boolean isLazyConstruction() {
        return lazyConstruction;
    }

    /**
     * Set the schema type to be used for validation
     * @param type the type to be used for validation. (For a document constructor, this is the required
     * type of the document element)
     */

    public void setSchemaType(SchemaType type) {
        schemaType = type;
        namespaceSensitiveType = (type instanceof SimpleType) && ((SimpleType)type).isNamespaceSensitive();
    }

    /**
     * Get the schema type chosen for validation; null if not defined
     * @return the type to be used for validation. (For a document constructor, this is the required
     * type of the document element)
     */

    public SchemaType getSchemaType() {
        return schemaType;
    }

    /**
     * Determine whether the schema type is namespace sensitive. The result is undefined if schemaType is null.
     * @return true if the schema type is namespace sensitive
     */

    public boolean isNamespaceSensitive() {
        return namespaceSensitiveType;
    }

    /**
     * Set the validation mode for the new document or element node
     * @param mode the validation mode, for example {@link Validation#STRICT}
     */

    public void setValidationMode(int mode) {
        validation = mode;
    }


    /**
     * Get the validation mode for this instruction
     * @return the validation mode, for example {@link Validation#STRICT} or {@link Validation#PRESERVE}
     */
    public int getValidationMode() {
        return validation;
    }

    /**
     * Get the implied validation mode for this
     */

    /**
     * Set the expression that constructs the content of the element
     * @param content the content expression
     */

    public void setContentExpression(Expression content) {
        this.content = content;
        adoptChildExpression(content);
    }

    /**
     * Get the expression that constructs the content of the element
     * @return the content expression
     */

    public Expression getContentExpression() {
        return content;
    }

    /**
     * Get the cardinality of the sequence returned by evaluating this instruction
     * @return the static cardinality
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }    


    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @return the simplified expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        content = visitor.simplify(content);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        content = visitor.typeCheck(content, contextItemType);
//        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
//        ItemType contentItemType = content.getItemType(th);
//        contentMayContainAttributesOrNamespaces =
//                th.relationship(contentItemType, NodeKindTest.ATTRIBUTE) != TypeHierarchy.DISJOINT &&
//                th.relationship(contentItemType, NodeKindTest.NAMESPACE) != TypeHierarchy.DISJOINT;
        adoptChildExpression(content);
        verifyLazyConstruction();
        checkContentSequence(visitor.getStaticContext());
        return this;
    }

    /**
     * Check that the child instructions don't violate any obvious constraints for this kind of node
     * @param env the static context
     * @throws XPathException
     */

    protected abstract void checkContentSequence(StaticContext env) throws XPathException;

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        content = visitor.optimize(content, contextItemType);
        if (content instanceof Block) {
            content = ((Block)content).mergeAdjacentTextInstructions();
        }
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
     * @param pathMap        the PathMap to which the expression should be added
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
     * @return the pathMapNodeSet representing the points in the source document that are both reachable by this
     *         expression, and that represent possible results of this expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        PathMap.PathMapNodeSet result = super.addToPathMap(pathMap, pathMapNodeSet); 
        result.addDescendants();
        return new PathMap.PathMapNodeSet(pathMap.makeNewRoot(this));
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

