package org.orbeon.saxon.instruct;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.functions.BooleanFn;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.evpull.EmptyEventIterator;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Compiled representation of an xsl:choose or xsl:if element in the stylesheet.
 * Also used for typeswitch in XQuery.
*/

public class Choose extends Instruction {

    // The class implements both xsl:choose and xsl:if. There is a list of boolean
    // expressions (conditions) and a list of corresponding actions: the conditions
    // are evaluated in turn, and when one is found that is true, the corresponding
    // action is evaluated. For xsl:if, there is always one condition and one action.
    // An xsl:otherwise is compiled as if it were xsl:when test="true()". If no
    // condition is satisfied, the instruction returns an empty sequence.

    private Expression[] conditions;
    private Expression[] actions;


    /**
    * Construct an xsl:choose instruction
    * @param conditions the conditions to be tested, in order
    * @param actions the actions to be taken when the corresponding condition is true
    */

    public Choose(Expression[] conditions, Expression[] actions) {
        this.conditions = conditions;
        this.actions = actions;
        if (conditions.length != actions.length) {
            throw new IllegalArgumentException("Choose: unequal length arguments");
        }
        for (int i=0; i<conditions.length; i++) {
            adoptChildExpression(conditions[i]);
            adoptChildExpression(actions[i]);
        }
    }

    /**
     * Make a simple conditional expression (if (condition) then (thenExp) else (elseExp)
     * @param condition the condition to be tested
     * @param thenExp the expression to be evaluated if the condition is true
     * @param elseExp the expression to be evaluated if the condition is false
     * @return the expression
     */

    public static Expression makeConditional(Expression condition, Expression thenExp, Expression elseExp) {
        if (Literal.isEmptySequence(elseExp)) {
            Expression[] conditions = new Expression[] {condition};
            Expression[] actions = new Expression[] {thenExp};
            return new Choose(conditions, actions);
        } else {
            Expression[] conditions = new Expression[] {condition, new Literal(BooleanValue.TRUE)};
            Expression[] actions = new Expression[] {thenExp, elseExp};
            return new Choose(conditions, actions);
        }
    }

    /**
     * Make a simple conditional expression (if (condition) then (thenExp) else ()
     * @param condition the condition to be tested
     * @param thenExp the expression to be evaluated if the condition is true
     * @return the expression
     */

    public static Expression makeConditional(Expression condition, Expression thenExp) {
        Expression[] conditions = new Expression[] {condition};
        Expression[] actions = new Expression[] {thenExp};
        return new Choose(conditions, actions);
    }

    /**
     * Test whether an expression is a single-branch choose, that is, an expression of the form
     * if (condition) then exp else ()
     * @param exp the expression to be tested
     * @return true if the expression is a choose expression and there is only one condition,
     * so that the expression returns () if this condition is false
     */

    public static boolean isSingleBranchChoice(Expression exp) {
        return (exp instanceof Choose && ((Choose)exp).conditions.length == 1);
    }

    /**
     * Get the array of conditions to be tested
     * @return the array of condition expressions
     */

    public Expression[] getConditions() {
        return conditions;
    }

    /**
     * Get the array of actions to be performed
     * @return the array of expressions to be evaluated when the corresponding condition is true
     */

    public Expression[] getActions() {
        return actions;
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    * We assume that if there was
     * only one condition then it was an xsl:if; this is not necessarily so, but
     * it's adequate for tracing purposes.
    */


    public int getInstructionNameCode() {
        return (conditions.length==1 ? StandardNames.XSL_IF : StandardNames.XSL_CHOOSE);
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor expression visitor object
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            conditions[i] = visitor.simplify(conditions[i]);
            try {
                actions[i] = visitor.simplify(actions[i]);
            } catch (XPathException err) {
                // mustn't throw the error unless the branch is actually selected, unless its a type error
                if (err.isTypeError()) {
                    throw err;
                } else {
                    actions[i] = new ErrorExpression(err);
                }
            }
        }

        // Eliminate a redundant if (false)

        for (int i=0; i<conditions.length; i++) {
            if (Literal.isConstantBoolean(conditions[i], false)) {
                if (conditions.length == 1) {
                    return new Literal(EmptySequence.getInstance());
                }
                Expression[] c = new Expression[conditions.length-1];
                Expression[] a = new Expression[conditions.length-1];
                if (i != 0) {
                    System.arraycopy(conditions, 0, c, 0, i);
                    System.arraycopy(actions, 0, a, 0, i);
                }
                if (i != conditions.length) {
                    System.arraycopy(conditions, i+1, c, i, conditions.length-i-1);
                    System.arraycopy(actions, i+1, a, i, actions.length-i-1);
                }
                conditions = c;
                actions = a;
                i--;

            }
        }

        // Eliminate everything that follows if (true)

        for (int i=0; i<conditions.length-1; i++) {
            if (Literal.isConstantBoolean(conditions[i], true)) {
                if (i == 0) {
                    return actions[0];
                }
                Expression[] c = new Expression[i+1];
                Expression[] a = new Expression[i+1];
                System.arraycopy(conditions, 0, c, 0, i+1);
                System.arraycopy(actions, 0, a, 0, i+1);
                break;
            }
        }

        // See if only condition left is if (true) then x else ()

        if (conditions.length == 1 && Literal.isConstantBoolean(conditions[0], true)) {
            return actions[0];
        }

        // Eliminate a redundant <xsl:otherwise/> or "when (test) then ()"

        if (/*Literal.isConstantBoolean(conditions[conditions.length-1], true) && */
                Literal.isEmptySequence(actions[actions.length-1])) {
            if (conditions.length == 1) {
                return new Literal(EmptySequence.getInstance());
            } else {
                Expression[] c = new Expression[conditions.length-1];
                System.arraycopy(conditions, 0, c, 0, conditions.length-1);
                Expression[] a = new Expression[actions.length-1];
                System.arraycopy(actions, 0, a, 0, actions.length-1);
            }
        }

        // Flatten an "else if"

        if (Literal.isConstantBoolean(conditions[conditions.length-1], true) &&
                actions[actions.length-1] instanceof Choose) {
            Choose choose2 = (Choose)actions[actions.length-1];
            int newLen = conditions.length + choose2.conditions.length - 1;
            Expression[] c2 = new Expression[newLen];
            Expression[] a2 = new Expression[newLen];
            System.arraycopy(conditions, 0, c2, 0, conditions.length - 1);
            System.arraycopy(actions, 0, a2, 0, actions.length - 1);
            System.arraycopy(choose2.conditions, 0, c2, conditions.length - 1, choose2.conditions.length);
            System.arraycopy(choose2.actions, 0, a2, actions.length - 1, choose2.actions.length);
            conditions = c2;
            actions = a2;
        }
        
        // Rewrite "if (EXP) then true() else false()" as boolean(EXP)

        if (conditions.length == 2 &&
                Literal.isConstantBoolean(actions[0], true) &&
                Literal.isConstantBoolean(actions[1], false) &&
                Literal.isConstantBoolean(conditions[1], true)) {
            TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
            if (th.isSubType(conditions[0].getItemType(th), BuiltInAtomicType.BOOLEAN) &&
                        conditions[0].getCardinality() == StaticProperty.EXACTLY_ONE) {
                return conditions[0];
            } else {
                return SystemFunction.makeSystemFunction("boolean", new Expression[]{conditions[0]});
            }
        }
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            conditions[i] = visitor.typeCheck(conditions[i], contextItemType);
            XPathException err = TypeChecker.ebvError(conditions[i], visitor.getConfiguration().getTypeHierarchy());
            if (err != null) {
                err.setLocator(conditions[i]);
                throw err;
            }
        }
        for (int i=0; i<actions.length; i++) {
            try {
                actions[i] = visitor.typeCheck(actions[i], contextItemType);
            } catch (XPathException err) {
                // mustn't throw the error unless the branch is actually selected, unless its a type error
                if (err.isTypeError()) {
                    throw err;
                } else {
                    actions[i] = new ErrorExpression(err);
                }
            }
        }
        return simplify(visitor);
    }

    /**
     * Determine whether this expression implements its own method for static type checking
     *
     * @return true - this expression has a non-trivial implementation of the staticTypeCheck()
     *         method
     */

    public boolean implementsStaticTypeCheck() {
        return true;
    }

    /**
     * Static type checking for conditional expressions is delegated to the expression itself,
     * and is performed separately on each branch of the conditional, so that dynamic checks are
     * added only on those branches where the check is actually required. This also results in a static
     * type error if any branch is incapable of delivering a value of the required type. One reason
     * for this approach is to avoid doing dynamic type checking on a recursive function call as this
     * prevents tail-call optimization being used.
     * @param req the required type
     * @param backwardsCompatible true if backwards compatibility mode applies
     * @param role the role of the expression in relation to the required type
     * @param visitor an expression visitor
     * @return the expression after type checking (perhaps augmented with dynamic type checking code)
     * @throws XPathException if failures occur, for example if the static type of one branch of the conditional
     * is incompatible with the required type
     */

    public Expression staticTypeCheck(SequenceType req,
                                             boolean backwardsCompatible,
                                             RoleLocator role, ExpressionVisitor visitor)
    throws XPathException {
        for (int i=0; i<actions.length; i++) {
            actions[i] = TypeChecker.staticTypeCheck(actions[i], req, backwardsCompatible, role, visitor);
        }
        // If the last condition isn't true(), then we need to consider the fall-through case, which returns
        // an empty sequence
        if (!Literal.isConstantBoolean(conditions[conditions.length-1], true) &&
                !Cardinality.allowsZero(req.getCardinality())) {
            String cond = (conditions.length == 1 ? "the condition is not" : "none of the conditions is");
            XPathException err = new XPathException(
                    "Conditional expession: If " + cond + " satisfied, an empty sequence will be returned, " +
                            "but this is not allowed as the " + role.getMessage());
            err.setErrorCode(role.getErrorCode());
            err.setIsTypeError(true);
            throw err;
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            conditions[i] = visitor.optimize(conditions[i], contextItemType);
            Expression ebv = BooleanFn.rewriteEffectiveBooleanValue(conditions[i], visitor, contextItemType);
            if (ebv != null && ebv != conditions[i]) {
                conditions[i] = ebv;
                adoptChildExpression(ebv);
            }
            if (conditions[i] instanceof Literal &&
                    !(((Literal)conditions[i]).getValue() instanceof BooleanValue)) {
                final boolean b;
                try {
                    b = ((Literal)conditions[i]).getValue().effectiveBooleanValue();
                } catch (XPathException err) {
                    err.setLocator(this);
                    throw err;
                }
                conditions[i] = new Literal(BooleanValue.get(b));
            }
        }
        for (int i=0; i<actions.length; i++) {
            try {
                actions[i] = visitor.optimize(actions[i], contextItemType);
            } catch (XPathException err) {
                // mustn't throw the error unless the branch is actually selected, unless its a type error
                if (err.isTypeError()) {
                    throw err;
                } else {
                    actions[i] = new ErrorExpression(err);
                }
            }
        }
        if (actions.length == 0) {
            return Literal.makeEmptySequence();
        }
        Expression e = visitor.getConfiguration().getOptimizer().trySwitch(this, visitor.getStaticContext());
        return e.simplify(visitor);
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        Expression[] c2 = new Expression[conditions.length];
        Expression[] a2 = new Expression[conditions.length];
        for (int c=0; c<conditions.length; c++) {
            c2[c] = conditions[c].copy();
            a2[c] = actions[c].copy();
        }
        return new Choose(c2, a2);
    }


    /**
     * Check to ensure that this expression does not contain any updating subexpressions.
     * This check is overridden for those expressions that permit updating subexpressions.
     *
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the expression has a non-permitted updateing subexpression
     */

    public void checkForUpdatingSubexpressions() throws XPathException {
        for (int c=0; c<conditions.length; c++) {
            conditions[c].checkForUpdatingSubexpressions();
            if (conditions[c].isUpdatingExpression()) {
                XPathException err = new XPathException(
                        "Updating expression appears in a context where it is not permitted", "XUST0001");
                err.setLocator(conditions[c]);
                throw err;
            }
        }
        boolean updating = false;
        boolean nonUpdating = false;
        for (int i=0; i<actions.length; i++) {
            Expression act = actions[i];
            act.checkForUpdatingSubexpressions();
            if (!ExpressionTool.isAllowedInUpdatingContext(act)) {
                if (updating) {
                    XPathException err = new XPathException(
                            "If any branch is an updating expression, then all must be updating expressions (or () or error())",
                            "XUST0001");
                    err.setLocator(actions[i]);
                    throw err;
                }
                nonUpdating = true;
            }
            if (act.isUpdatingExpression()) {
                if (nonUpdating) {
                    XPathException err = new XPathException(
                            "If any branch is an updating expression, then all must be updating expressions (or () or error())",
                            "XUST0001");
                    err.setLocator(actions[i]);
                    throw err;
                }
                updating = true;
            }
        }
    }

    /**
     * Determine whether this is an updating expression as defined in the XQuery update specification
     *
     * @return true if this is an updating expression
     */

    public boolean isUpdatingExpression() {
        for (int c=0; c<actions.length; c++) {
            if (actions[c].isUpdatingExpression()) {
                return true;
            }
        }
        return false;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        int m = Expression.PROCESS_METHOD | Expression.ITERATE_METHOD;
        if (!Cardinality.allowsMany(getCardinality())) {
            m |= Expression.EVALUATE_METHOD;
        }
        return m;
    }

    /**
     * Mark tail-recursive calls on functions. For most expressions, this does nothing.
     *
     * @return 0 if no tail call was found; 1 if a tail call on a different function was found;
     * 2 if a tail recursive call was found and if this call accounts for the whole of the value.
     */

    public int markTailFunctionCalls(StructuredQName qName, int arity) {
        int result = 0;
        for (int i=0; i<actions.length; i++) {
            result = Math.max(result, actions[i].markTailFunctionCalls(qName, arity));
        }
        return result;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @return the static item type of the instruction
     * @param th Type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        ItemType type = actions[0].getItemType(th);
        for (int i=1; i<actions.length; i++) {
            type = Type.getCommonSuperType(type, actions[i].getItemType(th), th);
        }
        return type;
    }

    /**
     * Compute the cardinality of the sequence returned by evaluating this instruction
     * @return the static cardinality
     */

    public int computeCardinality() {
        int card = 0;
        boolean includesTrue = false;
        for (int i=0; i<actions.length; i++) {
            card = Cardinality.union(card, actions[i].getCardinality());
            if (Literal.isConstantBoolean(conditions[i], true)) {
                includesTrue = true;
            }
        }
        if (!includesTrue) {
            // we may drop off the end and return an empty sequence (typical for xsl:if)
            card = Cardinality.union(card, StaticProperty.ALLOWS_ZERO);
        }
        return card;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public int computeSpecialProperties() {
        int props = actions[0].getSpecialProperties();
        for (int i=1; i<actions.length; i++) {
            props &= actions[i].getSpecialProperties();
        }
        return props;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if any of the "actions" creates new nodes.
     * (Nodes created by the conditions can't contribute to the result).
     */

    public final boolean createsNewNodes() {
        for (int i=0; i<actions.length; i++) {
            int props = actions[i].getSpecialProperties();
            if ((props & StaticProperty.NON_CREATIVE) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(conditions.length + actions.length);
        for (int i=0; i<conditions.length; i++) {
            list.add(conditions[i]);
        }
        for (int i=0; i<actions.length; i++) {
            list.add(actions[i]);
        }
        return list.iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        for (int i=0; i<conditions.length; i++) {
            if (conditions[i] == original) {
                conditions[i] = replacement;
                found = true;
            }
        }
        for (int i=0; i<actions.length; i++) {
            if (actions[i] == original) {
                actions[i] = replacement;
                found = true;
            }
        }
        return found;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        // xsl:when acts as a guard: expressions inside the when mustn't be evaluated if the when is false,
        // and conditions after the first mustn't be evaluated if a previous condition is true. So we
        // don't pass all promotion offers on
        if (offer.action == PromotionOffer.UNORDERED ||
                offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                offer.action == PromotionOffer.REPLACE_CURRENT) {
            for (int i=0; i<conditions.length; i++) {
                conditions[i] = doPromotion(conditions[i], offer);
            }
            for (int i=0; i<actions.length; i++) {
                actions[i] = doPromotion(actions[i], offer);
            }
        } else {
            // in other cases, only the first xsl:when condition is promoted
            conditions[0]  = doPromotion(conditions[0], offer);
        }
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        for (int i=0; i<actions.length; i++) {
            actions[i].checkPermittedContents(parentType, env, whole);
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
     * @param pathMapNodeSet the set of PathMap nodes to which the paths from this expression should be appended
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        // expressions used in a condition contribute paths, but these do not contribute to the result
        for (int i=0; i<conditions.length; i++) {
            conditions[i].addToPathMap(pathMap, pathMapNodeSet);
        }
        PathMap.PathMapNodeSet result = new PathMap.PathMapNodeSet();
        for (int i=0; i<actions.length; i++) {
            PathMap.PathMapNodeSet temp = actions[i].addToPathMap(pathMap, pathMapNodeSet);
            result.addNodeSet(temp);
        }
        return result;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("choose");
        for (int i=0; i<conditions.length; i++) {
            out.startSubsidiaryElement("when");
            conditions[i].explain(out);
            out.endSubsidiaryElement();
            out.startSubsidiaryElement("then");
            actions[i].explain(out);
            out.endSubsidiaryElement();
        }
        out.endElement();
    }

    /**
    * Process this instruction, that is, choose an xsl:when or xsl:otherwise child
    * and process it.
    * @param context the dynamic context of this transformation
    * @throws XPathException if any non-recoverable dynamic error occurs
    * @return a TailCall, if the chosen branch ends with a call of call-template or
    * apply-templates. It is the caller's responsibility to execute such a TailCall.
    * If there is no TailCall, returns null.
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            final boolean b;
            try {
                b = conditions[i].effectiveBooleanValue(context);
            } catch (XPathException e) {
                e.maybeSetLocation(conditions[i]);
                throw e;
            }
            if (b) {
                if (actions[i] instanceof TailCallReturner) {
                    return ((TailCallReturner)actions[i]).processLeavingTail(context);
                } else {
                    actions[i].process(context);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            final boolean b;
            try {
                b = conditions[i].effectiveBooleanValue(context);
            } catch (XPathException e) {
                e.maybeSetLocation(conditions[i]);
                throw e;
            }
            if (b) {
                return actions[i].evaluateItem(context);
            }
        }
        return null;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation relies on the process() method: it
     * "pushes" the results of the instruction to a sequence in memory, and then
     * iterates over this in-memory sequence.
     *
     * In principle instructions should implement a pipelined iterate() method that
     * avoids the overhead of intermediate storage.
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            final boolean b;
            try {
                b = conditions[i].effectiveBooleanValue(context);
            } catch (XPathException e) {
                e.maybeSetLocation(conditions[i]);
                throw e;
            }
            if (b) {
                return (actions[i]).iterate(context);
            }
        }
        return EmptyIterator.getInstance();
    }


    /**
     * Deliver the result of the expression as a sequence of events.
     * <p/>
     * <p>The events (of class {@link org.orbeon.saxon.evpull.PullEvent}) are either complete
     * items, or one of startElement, endElement, startDocument, or endDocument, known
     * as semi-nodes. The stream of events may also include a nested EventIterator.
     * If a start-end pair exists in the sequence, then the events between
     * this pair represent the content of the document or element. The content sequence will
     * have been processed to the extent that any attribute and namespace nodes in the
     * content sequence will have been merged into the startElement event. Namespace fixup
     * will have been performed: that is, unique prefixes will have been allocated to element
     * and attribute nodes, and all namespaces will be declared by means of a namespace node
     * in the startElement event or in an outer startElement forming part of the sequence.
     * However, duplicate namespaces may appear in the sequence.</p>
     * <p>The content of an element or document may include adjacent or zero-length text nodes,
     * atomic values, and nodes represented as nodes rather than broken down into events.</p>
     *
     * @param context The dynamic evaluation context
     * @return the result of the expression as an iterator over a sequence of PullEvent objects
     * @throws org.orbeon.saxon.trans.XPathException
     *          if a dynamic error occurs during expression evaluation
     */

    public EventIterator iterateEvents(XPathContext context) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            final boolean b;
            try {
                b = conditions[i].effectiveBooleanValue(context);
            } catch (XPathException e) {
                e.maybeSetLocation(conditions[i]);
                throw e;
            }
            if (b) {
                return (actions[i]).iterateEvents(context);
            }
        }
        return EmptyEventIterator.getInstance();
    }


    /**
     * Evaluate an updating expression, adding the results to a Pending Update List.
     * The default implementation of this method, which is used for non-updating expressions,
     * throws an UnsupportedOperationException
     *
     * @param context the XPath dynamic evaluation context
     * @param pul     the pending update list to which the results should be written
     */

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            final boolean b;
            try {
                b = conditions[i].effectiveBooleanValue(context);
            } catch (XPathException e) {
                e.maybeSetLocation(conditions[i]);
                throw e;
            }
            if (b) {
                actions[i].evaluatePendingUpdates(context, pul);
                return;
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
// Contributor(s):
//
