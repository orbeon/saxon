package net.sf.saxon.expr;
import net.sf.saxon.sort.DocumentSorter;
import net.sf.saxon.sort.Reverser;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

/**
* PromotionOffer is an object used transiently during compilation of an expression. It contains
* information passed by a containing expression to its subexpressions, when looking for subexpressions
* that can be promoted to a higher level because they are not dependent on the context established
* by the containing expression. The object is also used to pass back return information when the
* promotion actually takes place.
*/

public class PromotionOffer  {

    /**
    * FOCUS_INDEPENDENT requests promotion of all non-trivial subexpressions that don't depend on the
    * focus. This is typically used to extract subexpressions from a filter predicate. The offer is
    * optional - each subexpression can decide whether it's worth the trouble of promoting itself.
    * The offer is normally passed on to subexpressions, except subexpressions that are evaluated
    * with a different focus
    */

    public static final int FOCUS_INDEPENDENT = 10;

    /**
    * RANGE_INDEPENDENT requests promotion of all non-trivial subexpressions that don't depend on a
    * specified range variable. This is typically used to extract subexpressions from the action of
    * a for expression or the condition of a some/every quantified expression. The offer is
    * optional - each subexpression can decide whether it's worth the trouble of promoting itself.
    * The offer is normally passed on to subexpressions, except subexpressions that are evaluated
    * with a different focus or a different range variable, because these may have other dependencies
    * that prevent their promotion.
    */

    public static final int RANGE_INDEPENDENT = 11;

    /**
    * Inline variable references causes all references to a variable V to be replaced by the
    * expression E. The variable is supplied in the "binding" property; the replacement expression
    * in the containingExpression property
    */

    public static final int INLINE_VARIABLE_REFERENCES = 12;

    /**
     * UNORDERED indicates that the containing expression does not require the results
     * to be delivered in any particular order. The boolean mustEliminateDuplicates
     * is set if duplicate items in the result are not allowed.
     */

    public static final int UNORDERED = 13;

    /**
    * action is one of the possible promotion actions, FOCUS_INDEPENDENT, RANGE_INDEPENDENT,
    * INLINE_VARIABLE_REFERENCES, ANY_ORDER, ANY_ORDER_UNIQUE
    */

    public int action;

    /**
    * In the case of FOCUS_INDEPENDENT, "promoteDocumentDependent" is a boolean that, when set to
    * true, indicates that it is safe to promote a subexpression that depends on the context document
    * but not on other aspects of the focus. This is the case, for example, in a filter expression when
    * it is known that all the nodes selected by the expression will be in the same document - as happens
    * when the filter is applied to a path expression. This allows subexpressions such as key() to be
    * promoted
    */

    public boolean promoteDocumentDependent = false;

    /**
     * In the case of FOCUS_INDEPENDENT, "promoteXSLTFunctions" is a boolean that, when set to true, indicates
     * that it is safe to promote XSLT functions such as current(). This flag is set when rewriting XPath expressions
     * and is unset when rewriting XSLT templates.
     */

    public boolean promoteXSLTFunctions = true;

    /**
     * In the case of UNORDERED, "mustEliminateDuplicates" is a boolean that is set to
     * true if the nodes can be delivered in any order so long as there are no duplicates
     * (for example, as required by the count() function). If this boolean is false, the
     * nodes can be delivered in any order and duplicates are allowed (for example, as
     * required by the boolean() function).
     */

    public boolean mustEliminateDuplicates = true;

    /**
    * In the case of RANGE_INDEPENDENT and WHERE_CLAUSE, "binding" identifies the range variables whose dependencies
    * we are looking for. For INLINE_VARIABLE_REFERENCES it is a single Binding that we are aiming to inline
    */

    public Binding[] bindingList;

    /**
    * When a promotion offer is made, containingExpression identifies the level to which the promotion
    * should occur. When a subexpression is promoted, an expression of the form let $VAR := SUB return ORIG
    * is created, and this replaces the original containingExpression within the PromotionOffer.
    */

    public Expression containingExpression;

    /**
    * Method to test whether a subexpression qualifies for promotion, and if so, to
    * accept the promotion.
    * @return if promotion was done, returns the expression that should be used in place
    * of the child expression. If no promotion was done, returns null.
    */

    public Expression accept(Expression child) throws XPathException {
        switch (action) {
            case RANGE_INDEPENDENT: {
                int properties = child.getSpecialProperties();
                if (((properties & StaticProperty.NON_CREATIVE) != 0) && !ExpressionTool.dependsOnVariable(child, bindingList)) {
                    return promote(child);
                }
                break;
            }

            case FOCUS_INDEPENDENT: {
                int dependencies = child.getDependencies();
                int properties = child.getSpecialProperties();
                if (!promoteXSLTFunctions && ((dependencies & StaticProperty.DEPENDS_ON_XSLT_CONTEXT) != 0)) {
                    break;
                }
                if ((dependencies & StaticProperty.DEPENDS_ON_FOCUS) == 0 &&
                        (properties & StaticProperty.NON_CREATIVE) != 0) {
                    return promote(child);
                } else if (promoteDocumentDependent &&
                        (dependencies & StaticProperty.DEPENDS_ON_NON_DOCUMENT_FOCUS) == 0 &&
                        (properties & StaticProperty.NON_CREATIVE) != 0) {
                    return promote(child);
                }
                break;
            }
            case INLINE_VARIABLE_REFERENCES: {
                if (child instanceof VariableReference &&
                    ((VariableReference)child).getBinding() == bindingList[0]) {
                    return containingExpression;
                }
                break;
            }
            case UNORDERED: {
                if (child instanceof Reverser) {
                    return ((Reverser)child).getBaseExpression();
                } else if (child instanceof DocumentSorter && !mustEliminateDuplicates) {
                    return ((DocumentSorter)child).getBaseExpression();
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown promotion action " + action);
        }
        return null;
    }

    /**
    * Method to do the promotion.
    */

    private Expression promote(Expression child) {
        RangeVariableDeclaration decl = new RangeVariableDeclaration();
        decl.setVariableName("zz:" + decl.hashCode());
        SequenceType type = SequenceType.makeSequenceType(child.getItemType(), child.getCardinality());
        decl.setRequiredType(type);

        VariableReference var = new VariableReference(decl);
        ExpressionTool.copyLocationInfo(containingExpression, var);
        var.setParentExpression(child.getParentExpression());

        Container container = containingExpression.getParentExpression();
        LetExpression let = new LetExpression();
        let.setVariableDeclaration(decl);
        let.setSequence(child);
        let.setAction(containingExpression);
        let.setParentExpression(container);
        let.adoptChildExpression(containingExpression);
        containingExpression = let;

        return var;
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
// Contributor(s): none.
//
