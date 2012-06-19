package org.orbeon.saxon.expr;
import org.orbeon.saxon.instruct.Choose;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.MemoClosure;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.type.ItemType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Assignation is an abstract superclass for the kinds of expression
* that declare range variables: for, some, and every.
*/

public abstract class Assignation extends Expression implements Binding {

    protected int slotNumber = -999;     // slot number for range variable
                                         // (initialized to ensure a crash if no real slot is allocated)
    protected Expression sequence;       // the expression over which the variable ranges
    protected Expression action;         // the action performed for each value of the variable
    protected StructuredQName variableName;
    protected SequenceType requiredType;

    //protected RangeVariable declaration;



    /**
     * Set the required type (declared type) of the variable
     * @param requiredType the required type
     */
    public void setRequiredType(SequenceType requiredType) {
        this.requiredType = requiredType;
    }

    /**
     * Set the name of the variable
     * @param variableName the name of the variable
     */

    public void setVariableQName(StructuredQName variableName) {
        this.variableName = variableName;
    }


    /**
     * Get the name of the variable
     * @return the variable name, as a QName
     */

    public StructuredQName getVariableQName() {
        return variableName;
    }

    public StructuredQName getObjectName() {
        return variableName;
    }


    /**
     * Get the declared type of the variable
     *
     * @return the declared type
     */

    public SequenceType getRequiredType() {
        return requiredType;
    }

    /**
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     */

    public int getLocalSlotNumber() {
        return slotNumber;
    }

    /**
    * Get the value of the range variable
    */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        ValueRepresentation actual = context.evaluateLocalVariable(slotNumber);
        if (actual instanceof MemoClosure && ((MemoClosure)actual).isFullyRead()) {
            actual = ((MemoClosure)actual).materialize();
            context.setLocalVariable(slotNumber, actual);
        }
        return actual;
    }

    /**
     * Add the "return" or "satisfies" expression, and fix up all references to the
     * range variable that occur within that expression
     * @param action the expression that occurs after the "return" keyword of a "for"
     * expression, the "satisfies" keyword of "some/every", or the ":=" operator of
     * a "let" expression.
     *
     * 
     */

    public void setAction(Expression action) {
        this.action = action;
        adoptChildExpression(action);
    }

    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     */

    public final boolean isGlobal() {
        return false;
    }

    /**
    * Test whether it is permitted to assign to the variable using the saxon:assign
    * extension element. This will only be for an XSLT global variable where the extra
    * attribute saxon:assignable="yes" is present.
    */

    public final boolean isAssignable() {
        return false;
    }

    /**
     * Check to ensure that this expression does not contain any inappropriate updating subexpressions.
     * This check is overridden for those expressions that permit updating subexpressions.
     *
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the expression has a non-permitted updateing subexpression
     */

    public void checkForUpdatingSubexpressions() throws XPathException {
        sequence.checkForUpdatingSubexpressions();
        if (sequence.isUpdatingExpression()) {
            XPathException err = new XPathException(
                        "Updating expression appears in a context where it is not permitted", "XUST0001");
            err.setLocator(sequence);
            throw err;
        }
        action.checkForUpdatingSubexpressions();
    }

    /**
     * Determine whether this is an updating expression as defined in the XQuery update specification
     * @return true if this is an updating expression
     */

    public boolean isUpdatingExpression() {
        return action.isUpdatingExpression();
    }

    /**
     * Get the action expression
     * @return the action expression (introduced by "return" or "satisfies")
     */

    public Expression getAction() {
        return action;
    }

    /**
     * Set the "sequence" expression - the one to which the variable is bound
     * @param sequence the expression to which the variable is bound
     */

    public void setSequence(Expression sequence) {
        this.sequence = sequence;
        adoptChildExpression(sequence);
    }

    /**
     * Get the "sequence" expression - the one to which the variable is bound
     * @return the expression to which the variable is bound
     */

    public Expression getSequence() {
        return sequence;
    }

    /**
    * Set the slot number for the range variable
     * @param nr the slot number to be used
    */

    public void setSlotNumber(int nr) {
        slotNumber = nr;
    }

    /**
     * Get the number of slots required. Normally 1, except for a FOR expression with an AT clause, where it is 2.
     * @return the number of slots required
     */

    public int getRequiredSlots() {
        return 1;
    }

    /**
    * Simplify the expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        sequence = visitor.simplify(sequence);
        action = visitor.simplify(action);
        return this;
    }


    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            sequence = doPromotion(sequence, offer);
            if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                    offer.action == PromotionOffer.UNORDERED ||
                    offer.action == PromotionOffer.REPLACE_CURRENT) {
                action = doPromotion(action, offer);
            } else if (offer.action == PromotionOffer.RANGE_INDEPENDENT ||
                    offer.action == PromotionOffer.FOCUS_INDEPENDENT) {
                // Pass the offer to the action expression only if the action isn't dependent on the
                // variable bound by this assignation
                Binding[] savedBindingList = offer.bindingList;
                offer.bindingList = extendBindingList(offer.bindingList);
                action = doPromotion(action, offer);
                offer.bindingList = savedBindingList;
            }
            return this;
        }
    }

    /**
     * Suppress validation on contained element constructors, on the grounds that the parent element
     * is already performing validation. The default implementation does nothing.
     */

    public void suppressValidation(int validationMode) {
        action.suppressValidation(validationMode);
    }

    /**
     * Extend an array of variable bindings to include the binding(s) defined in this expression
     * @param in a set of variable bindings
     * @return a set of variable bindings including all those supplied plus this one
     */

    protected Binding[] extendBindingList(Binding[] in) {
        Binding[] newBindingList;
        if (in == null) {
            newBindingList = new Binding[1];
        } else {
            newBindingList = new Binding[in.length + 1];
            System.arraycopy(in, 0, newBindingList, 0, in.length);
        }
        newBindingList[newBindingList.length - 1] = this;
        return newBindingList;
    }


    /**
     * Promote a WHERE clause whose condition doesn't depend on the variable being bound.
     * This rewrites an expression of the form
     *
     * <p>let $i := SEQ return if (C) then R else ()</p>
     * <p>to the form:</p>
     * <p>if (C) then (let $i := SEQ return R) else ()</p>
     *
     * @param positionBinding the binding of the position variable if any
     * @return an expression in which terms from the WHERE clause that can be extracted have been extracted
     */

    protected Expression promoteWhereClause(Binding positionBinding) {
        if (Choose.isSingleBranchChoice(action)) {
            //IfExpression ifex = (IfExpression)action;
            Expression condition = ((Choose)action).getConditions()[0];
            Binding[] bindingList;
            if (positionBinding == null) {
                bindingList = new Binding[] {this};
            } else {
                bindingList = new Binding[] {this, positionBinding};
            }
            List list = new ArrayList(5);
            Expression promotedCondition = null;
            BooleanExpression.listAndComponents(condition, list);
            for (int i = list.size() - 1; i >= 0; i--) {
                Expression term = (Expression) list.get(i);
                if (!ExpressionTool.dependsOnVariable(term, bindingList)) {
                    if (promotedCondition == null) {
                        promotedCondition = term;
                    } else {
                        promotedCondition = new BooleanExpression(term, Token.AND, promotedCondition);
                    }
                    list.remove(i);
                }
            }
            if (promotedCondition != null) {
                if (list.isEmpty()) {
                    // the whole if() condition has been promoted
                    Expression oldThen = ((Choose)action).getActions()[0];
                    setAction(oldThen);
                    return Choose.makeConditional(condition, this);
                } else {
                    // one or more terms of the if() condition have been promoted
                    Expression retainedCondition = (Expression) list.get(0);
                    for (int i = 1; i < list.size(); i++) {
                        retainedCondition = new BooleanExpression(retainedCondition, Token.AND, (Expression) list.get(i));
                    }
                    ((Choose)action).getConditions()[0] = retainedCondition;
                    Expression newIf = Choose.makeConditional(
                            promotedCondition, this, Literal.makeEmptySequence());
                    ExpressionTool.copyLocationInfo(this, newIf);
                    return newIf;
                }
            }
        }
        return null;
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator iterateSubExpressions() {
        return new PairIterator(sequence, action);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (sequence == original) {
            sequence = replacement;
            found = true;
        }
        if (action == original) {
            action = replacement;
            found = true;
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
     * @param pathMap        the PathMap to which the expression should be added
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
     * @return the pathMapNodeSet representing the points in the source document that are both reachable by this
     *         expression, and that represent possible results of this expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        PathMap.PathMapNodeSet varPath = sequence.addToPathMap(pathMap, pathMapNodeSet);
        pathMap.registerPathForVariable(this, varPath);
        return action.addToPathMap(pathMap, pathMapNodeSet);
    }

    // Following methods implement the VariableDeclaration interface, in relation to the range
    // variable

//    public int getVariableNameCode() {
//        return nameCode;
//    }

//    public int getVariableFingerprint() {
//        return nameCode & 0xfffff;
//    }

    /**
     * Get the display name of the range variable, for diagnostics only
     * @return the lexical QName of the range variable
    */

    public String getVariableName() {
        if (variableName == null) {
            return "zz:var" + hashCode();
        } else {
            return variableName.getDisplayName();
        }
    }

    /**
     * Refine the type information associated with this variable declaration. This is useful when the
     * type of the variable has not been explicitly declared (which is common); the variable then takes
     * a static type based on the type of the expression to which it is bound. The effect of this call
     * is to update the static expression type for all references to this variable.
     * @param type the inferred item type of the expression to which the variable is bound
     * @param cardinality the inferred cardinality of the expression to which the variable is bound
     * @param constantValue the constant value to which the variable is bound (null if there is no constant value)
     * @param properties other static properties of the expression to which the variable is bound
     * @param visitor an expression visitor to provide context information
     * @param currentExpression the expression that binds the variable
     */

    public void refineTypeInformation(ItemType type, int cardinality,
                                      Value constantValue, int properties,
                                      ExpressionVisitor visitor,
                                      Assignation currentExpression) {
        List references = new ArrayList();
        ExpressionTool.gatherVariableReferences(currentExpression.getAction(), this, references);
        for (Iterator iter=references.iterator(); iter.hasNext();) {
            BindingReference ref = (BindingReference)iter.next();
            if (ref instanceof VariableReference) {
                ((VariableReference)ref).refineVariableType(type, cardinality, constantValue, properties, visitor);
                visitor.resetStaticProperties();
                ExpressionTool.resetPropertiesWithinSubtree(currentExpression);
            }
        }
    }

    

    /**
    * Get the value of the range variable
    */

//    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
////        if (slotNumber == -999) {
////            display(10, context.getController().getNamePool(), System.err);
////        }
//        ValueRepresentation actual = context.evaluateLocalVariable(slotNumber);
//        if (actual instanceof MemoClosure && ((MemoClosure)actual).isFullyRead()) {
//            actual = ((MemoClosure)actual).materialize();
//            context.setLocalVariable(slotNumber, actual);
//        }
//        return actual;
//    }

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
