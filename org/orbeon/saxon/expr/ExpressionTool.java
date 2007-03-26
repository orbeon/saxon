package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.functions.ExtensionFunctionCall;
import org.orbeon.saxon.event.SequenceOutputter;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.SortExpression;
import org.orbeon.saxon.trace.InstructionInfoProvider;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

import javax.xml.transform.SourceLocator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This class, ExpressionTool, contains a number of useful static methods
 * for manipulating expressions. Most importantly, it provides the factory
 * method make() for constructing a new expression
 */

public class ExpressionTool {

    public static final int UNDECIDED = -1;
    public static final int NO_EVALUATION_NEEDED = 0;
    public static final int EVALUATE_VARIABLE = 1;
    public static final int MAKE_CLOSURE = 3;
    public static final int MAKE_MEMO_CLOSURE = 4;
    public static final int RETURN_EMPTY_SEQUENCE = 5;
    public static final int EVALUATE_AND_MATERIALIZE_VARIABLE = 6;
    public static final int CALL_EVALUATE_ITEM = 7;
    public static final int ITERATE_AND_MATERIALIZE = 8;
    public static final int PROCESS = 9;
    public static final int LAZY_TAIL_EXPRESSION = 10;
    public static final int SHARED_APPEND_EXPRESSION = 11;

    private ExpressionTool() {}

    /**
     * Parse an expression. This performs the basic analysis of the expression against the
     * grammar, it binds variable references and function calls to variable definitions and
     * function definitions, and it performs context-independent expression rewriting for
     * optimization purposes.
     *
     * @exception org.orbeon.saxon.trans.XPathException if the expression contains a static error
     * @param expression The expression (as a character string)
     * @param env An object giving information about the compile-time
     *     context of the expression
     * @param terminator The token that marks the end of this expression; typically
     * Tokenizer.EOF, but may for example be a right curly brace
     * @param lineNumber the line number of the start of the expression
     * @return an object of type Expression
     */

    public static Expression make(String expression, StaticContext env,
                                  int start, int terminator, int lineNumber) throws XPathException {
        ExpressionParser parser = new ExpressionParser();
        if (terminator == -1) {
            terminator = Token.EOF;
        }
        Expression exp = parser.parse(expression, start, terminator, lineNumber, env);
        exp = exp.simplify(env);
        makeParentReferences(exp);
        return exp;
    }

    /**
     * Copy location information (the line number) from one expression
     * to another
     */

    public static void copyLocationInfo(Expression from, Expression to) {
        if (from instanceof ComputedExpression && to instanceof ComputedExpression) {
            ((ComputedExpression)to).setLocationId(((ComputedExpression)from).getLocationId());
        }
    }

    /**
     * Establish the links from subexpressions to their parent expressions,
     * by means of a recursive tree walk.
     */

    public static void makeParentReferences(Expression top) {
        // This costly method is now unnecessary; we are setting the parent expression
        // as the tree is built. But it can be put back in if needed.
//        if (top==null) {
//            return;
//        }
//        for (Iterator children = top.iterateSubExpressions(); children.hasNext();) {
//            Expression child = (Expression)children.next();
//            if (child instanceof ComputedExpression) {
//                ((ComputedExpression)child).setParentExpression((ComputedExpression)top);
//                makeParentReferences(child);
//            }
//        }
    }

    /**
     * Get location information for an expression in the form of a SourceLocator
     */

    public static SourceLocator getLocator(Expression exp) {
        if (exp instanceof ComputedExpression) {
            return (ComputedExpression)exp;
        } else {
            return null;
        }
    }

    /**
     * Determine whether an expression is a repeatedly-evaluated subexpression
     * of a parent expression. For example, the predicate in a filter expression is
     * a repeatedly-evaluated subexpression of the filter expression.
     */

    public static boolean isRepeatedSubexpression(Expression parent, Expression child, StaticContext env) {
        if (parent instanceof PathExpression) {
            return child == ((PathExpression)parent).getStepExpression();
        }
        if (parent instanceof FilterExpression) {
            final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
            return child == ((FilterExpression)parent).getFilter() &&
                    !th.isSubType(child.getItemType(th), Type.NUMBER_TYPE);
        }
        if (parent instanceof ForExpression) {
            return child == ((ForExpression)parent).getAction();
        }
        if (parent instanceof QuantifiedExpression) {
            return child == ((QuantifiedExpression)parent).getAction();
        }
        if (parent instanceof SimpleMappingExpression) {
            return child == ((SimpleMappingExpression)parent).getStepExpression();
        }
        if (parent instanceof SortExpression) {
            return ((SortExpression)parent).isSortKey(child);
        }
        if (parent instanceof AnalyzeString) {
            return child == ((AnalyzeString)parent).getMatchingExpression() ||
                    child == ((AnalyzeString)parent).getNonMatchingExpression();
        }
        if (parent instanceof ForEach) {
            return child == ((ForEach)parent).getActionExpression();
        }
        if (parent instanceof ForEachGroup) {
            return child == ((ForEachGroup)parent).getActionExpression();
        }
        if (parent instanceof While) {
            return child == ((While)parent).getActionExpression();
        }
        if (parent instanceof GeneralComparison) {
            return child == ((GeneralComparison)parent).getOperands()[1];
        }
        if (parent instanceof GeneralComparison10) {
            Expression[] ops = ((GeneralComparison10)parent).getOperands();
            return child == ops[0] || child == ops[1];
        }
        if (parent instanceof ApplyTemplates) {
            // treat the expressions within xsl:with-param as repeated
            return child instanceof WithParam;
        }
        return false;
    }
    /**
     * Remove unwanted sorting from an expression, at compile time
     */

    public static Expression unsorted(Optimizer opt, Expression exp, boolean eliminateDuplicates)
    throws XPathException {
        if (exp instanceof Value) return exp;   // fast exit
        PromotionOffer offer = new PromotionOffer(opt);
        offer.action = PromotionOffer.UNORDERED;
        offer.mustEliminateDuplicates = eliminateDuplicates;
        Expression exp2 = exp.promote(offer);
        if (exp2 != exp) {
            ComputedExpression.setParentExpression(exp2, exp.getParentExpression());
            return exp2;
        }
        return exp;
    }

    /**
     * Remove unwanted sorting from an expression, at compile time, if and only if it is known
     * that the result of the expression will be homogeneous (all nodes, or all atomic values).
     * This is done when we need the effective boolean value of a sequence: the EBV of a
     * homogenous sequence does not depend on its order, but this is not true when atomic
     * values and nodes are mixed: (N, AV) is true, but (AV, N) is an error.
     */

    public static Expression unsortedIfHomogeneous(Optimizer opt, Expression exp, boolean eliminateDuplicates)
    throws XPathException {
        if (exp instanceof Value) {
            return exp;   // fast exit
        }
        if (exp.getItemType(opt.getConfiguration().getTypeHierarchy()) instanceof AnyItemType) {
            return exp;
        } else {
            PromotionOffer offer = new PromotionOffer(opt);
            offer.action = PromotionOffer.UNORDERED;
            offer.mustEliminateDuplicates = eliminateDuplicates;
            return exp.promote(offer);
        }
    }

    /**
     * Determine the method of evaluation to be used when lazy evaluation of an expression is
     * preferred. This method is called at compile time, after all optimizations have been done,
     * to determine the preferred strategy for lazy evaluation, depending on the type of expression.
     *
     * @param exp the expression to be evaluated
     * @return an integer constant identifying the evaluation mode
     */

    public static int lazyEvaluationMode(Expression exp) {
        // this sequence of tests rearranged 7 Apr 2005 because opt017 was failing.
        if (exp instanceof Value) {
            return NO_EVALUATION_NEEDED;

        } else if (exp instanceof VariableReference) {
            return EVALUATE_VARIABLE;

        } else if ((exp.getDependencies() &
                (   StaticProperty.DEPENDS_ON_POSITION |
                    StaticProperty.DEPENDS_ON_LAST |
                    StaticProperty.DEPENDS_ON_CURRENT_ITEM |
                    StaticProperty.DEPENDS_ON_CURRENT_GROUP |
                    StaticProperty.DEPENDS_ON_REGEX_GROUP )) != 0) {
            // we can't save these values in the closure, so we evaluate
            // the expression now if they are needed
            return eagerEvaluationMode(exp);

        } else if (exp instanceof ErrorExpression) {
            return CALL_EVALUATE_ITEM;
                // evaluateItem() on an error expression throws the latent exception

        } else if (exp instanceof LazyExpression) {
            // A LazyExpression is always evaluated lazily (if at all possible) to
            // prevent spurious errors (see opt017)
            return MAKE_MEMO_CLOSURE;

        } else if (!Cardinality.allowsMany(exp.getCardinality())) {
            // singleton expressions are always evaluated eagerly
            return eagerEvaluationMode(exp);

        } else if (exp instanceof TailExpression) {
                    // Treat tail recursion as a special case, to avoid creating a deeply-nested
                    // tree of Closures. If this expression is a TailExpression, and its first
                    // argument is also a TailExpression, we combine the two TailExpressions into
                    // one and return a closure over that.
            TailExpression tail = (TailExpression)exp;
            Expression base = tail.getBaseExpression();
            if (base instanceof VariableReference) {
                return LAZY_TAIL_EXPRESSION;
            } else {
                return MAKE_CLOSURE;
            }

        } else if (exp instanceof Block &&
                    ((Block)exp).getChildren().length == 2 &&
                    (((Block)exp).getChildren()[0] instanceof VariableReference ||
                        ((Block)exp).getChildren()[0] instanceof Value)) {
                    // If the expression is a Block, that is, it is appending a value to a sequence,
                    // then we have the opportunity to use a shared list underpinning the old value and
                    // the new. This takes precedence over lazy evaluation (it would be possible to do this
                    // lazily, but more difficult). We currently only do this for the common case of a two-argument
                    // append expression, in the case where the first argument is either a value, or a variable
                    // reference identifying a value. The most common case is that the first argument is a reference
                    // to an argument of recursive function, where the recursive function returns the result of
                    // appending to the sequence.
            return SHARED_APPEND_EXPRESSION;

        } else {
            // create a Closure, a wrapper for the expression and its context
            return MAKE_CLOSURE;
        }
    }

    /**
     * Determine the method of evaluation to be used when lazy evaluation of an expression is
     * preferred. This method is called at compile time, after all optimizations have been done,
     * to determine the preferred strategy for lazy evaluation, depending on the type of expression.
     *
     * @param exp the expression to be evaluated
     * @return an integer constant identifying the evaluation mode
     */

    public static int eagerEvaluationMode(Expression exp) {
        if (exp instanceof Value && !(exp instanceof Closure)) {
            return NO_EVALUATION_NEEDED;
        }
        if (exp instanceof VariableReference) {
            return EVALUATE_AND_MATERIALIZE_VARIABLE;
        }
        int m = exp.getImplementationMethod();
        if ((m & Expression.EVALUATE_METHOD) != 0) {
            return CALL_EVALUATE_ITEM;
        } else if ((m & Expression.ITERATE_METHOD) != 0) {
            return ITERATE_AND_MATERIALIZE;
        } else {
            return PROCESS;
        }
    }


    /**
     * Do lazy evaluation of an expression. This will return a value, which may optionally
     * be a SequenceIntent, which is a wrapper around an iterator over the value of the expression.
     *
     * @param context the run-time evaluation context for the expression. If
     *     the expression is not evaluated immediately, then parts of the
     *     context on which the expression depends need to be saved as part of
     *      the Closure
     * @param ref an indication of how the value will be used. The value 1 indicates that the value
     *     is only expected to be used once, so that there is no need to keep it in memory. A small value >1
     *     indicates multiple references, so the value will be saved when first evaluated. The special value
     *     FILTERED indicates a reference within a loop of the form $x[predicate], indicating that the value
     *     should be saved in a way that permits indexing.
     * @exception XPathException if any error occurs in evaluating the
     *     expression
     * @return a value: either the actual value obtained by evaluating the
     *     expression, or a Closure containing all the information needed to
     *     evaluate it later
     */

    public static ValueRepresentation evaluate(Expression exp, int evaluationMode, XPathContext context, int ref)
    throws XPathException {
        switch (evaluationMode) {

            case NO_EVALUATION_NEEDED:
                return (Value)exp;

            case EVALUATE_VARIABLE:
                return ((VariableReference)exp).evaluateVariable(context);

            case MAKE_CLOSURE:
                return Closure.make(exp, context, ref);

            case MAKE_MEMO_CLOSURE:
                return Closure.make(exp, context, (ref==1 ? 10 : ref));

            case RETURN_EMPTY_SEQUENCE:
                return EmptySequence.getInstance();

            case EVALUATE_AND_MATERIALIZE_VARIABLE:
                ValueRepresentation v = ((VariableReference)exp).evaluateVariable(context);
                if (v instanceof Closure) {
                    return SequenceExtent.makeSequenceExtent(((Closure)v).iterate(context));
                } else {
                    return v;
                }

            case CALL_EVALUATE_ITEM:
                Item item = exp.evaluateItem(context);
                if (item == null) {
                    return EmptySequence.getInstance();
                } else {
                    return item;
                }

            case UNDECIDED:
            case ITERATE_AND_MATERIALIZE:
                return SequenceExtent.makeSequenceExtent(exp.iterate(context));

            case PROCESS:
                Controller controller = context.getController();
                XPathContext c2 = context.newMinorContext();
                c2.setOrigin((InstructionInfoProvider)exp);
                SequenceOutputter seq = controller.allocateSequenceOutputter(20);
                final PipelineConfiguration pipe = controller.makePipelineConfiguration();
                pipe.setHostLanguage((exp instanceof ComputedExpression ?
                        ((ComputedExpression)exp).getHostLanguage() :
                        controller.getExecutable().getHostLanguage()));
                seq.setPipelineConfiguration(pipe);
                c2.setTemporaryReceiver(seq);
                seq.open();
                exp.process(c2);
                seq.close();
                ValueRepresentation val = seq.getSequence();
                seq.reset();
                return val;

            case LAZY_TAIL_EXPRESSION: {
                TailExpression tail = (TailExpression)exp;
                VariableReference vr = (VariableReference)tail.getBaseExpression();
                ValueRepresentation base = evaluate(vr, EVALUATE_VARIABLE, context, ref);
                if (base instanceof MemoClosure) {
                    SequenceIterator it = ((MemoClosure)base).iterate(null);
                    base = ((GroundedIterator)it).materialize();
                }
                if (base instanceof IntegerRange) {
                    long start = ((IntegerRange)base).getStart() + 1;
                    long end = ((IntegerRange)base).getEnd();
                    if (start == end) {
                        return new IntegerValue(end);
                    } else {
                        return new IntegerRange(start, end);
                    }
                }
                if (base instanceof SequenceExtent) {
                    return new SequenceExtent(
                            (SequenceExtent)base,
                            tail.getStart() - 1,
                            ((SequenceExtent)base).getLength() - tail.getStart() + 1);
                }

                return Closure.make(tail, context, ref);
            }

            case SHARED_APPEND_EXPRESSION: {
                Block block = (Block)exp;
                Expression base = block.getChildren()[0];
                if (base instanceof VariableReference) {
                    base = Value.asValue(evaluate(base, EVALUATE_VARIABLE, context, ref));
                    if (base instanceof MemoClosure && ((MemoClosure)base).isFullyRead()) {
                        base = ((MemoClosure)base).materialize();
                    }
                }
                if (base instanceof ShareableSequence && ((ShareableSequence)base).isShareable()) {
                    List list = ((ShareableSequence)base).getList();
                    SequenceIterator iter = block.getChildren()[1].iterate(context);
                    while (true) {
                        Item i = iter.next();
                        if (i == null) {
                            break;
                        }
                        list.add(i);
                    }
                    return new ShareableSequence(list);
                } else if (base instanceof Value) {
                    List list = new ArrayList(20);
                    SequenceIterator iter = base.iterate(context);
                    while (true) {
                        Item i = iter.next();
                        if (i == null) {
                            break;
                        }
                        list.add(i);
                    }
                    iter = block.getChildren()[1].iterate(context);
                    while (true) {
                        Item i = iter.next();
                        if (i == null) {
                            break;
                        }
                        list.add(i);
                    }
                    return new ShareableSequence(list);
                }
            }

            default:
                throw new IllegalArgumentException("Unknown evaluation mode " + evaluationMode);

        }
    }

    /**
     * Do lazy evaluation of an expression. This will return a value, which may optionally
     * be a SequenceIntent, which is a wrapper around an iterator over the value of the expression.
     *
     * @param context the run-time evaluation context for the expression. If
     *     the expression is not evaluated immediately, then parts of the
     *     context on which the expression depends need to be saved as part of
     *      the Closure
     * @param ref an indication of how the value will be used. The value 1 indicates that the value
     *     is only expected to be used once, so that there is no need to keep it in memory. A small value >1
     *     indicates multiple references, so the value will be saved when first evaluated. The special value
     *     FILTERED indicates a reference within a loop of the form $x[predicate], indicating that the value
     *     should be saved in a way that permits indexing.
     * @exception XPathException if any error occurs in evaluating the
     *     expression
     * @return a value: either the actual value obtained by evaluating the
     *     expression, or a Closure containing all the information needed to
     *     evaluate it later
     */

    public static ValueRepresentation lazyEvaluate(Expression exp, XPathContext context, int ref) throws XPathException {
        final int evaluationMode = lazyEvaluationMode(exp);
        return evaluate(exp, evaluationMode, context, ref);
    }

    /**
     * Evaluate an expression now; lazy evaluation is not permitted in this case
     * @param exp the expression to be evaluated
     * @param context the run-time evaluation context
     * @exception org.orbeon.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the result of evaluating the expression
     */

    public static ValueRepresentation eagerEvaluate(Expression exp, XPathContext context) throws XPathException {
        final int evaluationMode = eagerEvaluationMode(exp);
        return evaluate(exp, evaluationMode, context, 10);
    }

    /**
     * Scan an expression to find and mark any recursive tail function calls
     * @param exp the expression to be analyzed
     * @param nameCode the name of the containing function
     * @param arity the arity of the containing function
     * @return true if a tail function call to the specified function was found. In this case the
     * UserFunctionCall object representing the tail function call will also have been marked as
     * tail-recursive.
     */

    public static boolean markTailFunctionCalls(Expression exp, int nameCode, int arity) {
        if (exp instanceof ComputedExpression) {
            return ((ComputedExpression)exp).markTailFunctionCalls(nameCode, arity);
        } else {
            return false;
        }
    }

    /**
     * Construct indent string, for diagnostic output
     *
     * @param level the indentation level (the number of spaces to return)
     * @return a string of "level*2" spaces
     */

    public static String indent(int level) {
        String s = "";
        for (int i=0; i<level; i++) {
            s += "  ";
        }
        return s;
    }

    /**
     * Allocate slot numbers to range variables
     * @param exp the expression whose range variables need to have slot numbers assigned
     * @param nextFree the next slot number that is available for allocation
     * @param frame a SlotManager object that is used to track the mapping of slot numbers
     * to variable names for debugging purposes. May be null.
     * @return the next unallocated slot number.
    */

    public static int allocateSlots(Expression exp, int nextFree, SlotManager frame) {
        return allocateSlots(exp, nextFree, frame, true);
    }

    private static int allocateSlots(Expression exp, int nextFree, SlotManager frame, boolean topLevel) {
        if (exp instanceof Assignation) {
            ((Assignation)exp).setSlotNumber(nextFree);
            int count = ((Assignation)exp).getRequiredSlots();
            nextFree += count;
            if (frame != null) {
                frame.allocateSlotNumber(((Assignation)exp).getVariableFingerprint());
                if (count == 2) {
                    frame.allocateSlotNumber(((ForExpression)exp).getPositionVariableNameCode() & NamePool.FP_MASK);
                }
            }
        }
        for (Iterator children = exp.iterateSubExpressions(); children.hasNext();) {
            Expression child = (Expression)children.next();
            nextFree = allocateSlots(child, nextFree, frame, false);
        }

        if (topLevel) {
            for (Iterator children = exp.iterateSubExpressions(); children.hasNext();) {
                Expression child = (Expression)children.next();
                refineVariableReference(child);
            }
        }
        return nextFree;

        // Note, we allocate a distinct slot to each range variable, even if the
        // scopes don't overlap. This isn't strictly necessary, but might help
        // debugging.
    }

    private static void refineVariableReference(Expression exp) {
        if (exp instanceof VariableReference) {
            ((VariableReference)exp).refineVariableReference();
        } else {
            for (Iterator children = exp.iterateSubExpressions(); children.hasNext();) {
                Expression child = (Expression)children.next();
                refineVariableReference(child);
            }
        }
    }

    /**
     * Determine the effective boolean value of a sequence, given an iterator over the sequence
     * @param iterator An iterator over the sequence whose effective boolean value is required
     * @return the effective boolean value
     * @throws XPathException if a dynamic error occurs
     */
    public static boolean effectiveBooleanValue(SequenceIterator iterator) throws XPathException {
        Item first = iterator.next();
        if (first == null) {
            return false;
        }
        if (first instanceof NodeInfo) {
            return true;
        } else {
            first = ((AtomicValue)first).getPrimitiveValue();
            if (first instanceof BooleanValue) {
                if (iterator.next() != null) {
                    ebvError("sequence of two or more items starting with a boolean");
                }
                return ((BooleanValue)first).getBooleanValue();
            } else if (first instanceof StringValue) {   // includes anyURI value
                if (iterator.next() != null) {
                    ebvError("sequence of two or more items starting with a string");
                }
                return (first.getStringValueCS().length()!=0);
            } else if (first instanceof NumericValue) {
                if (iterator.next() != null) {
                    ebvError("sequence of two or more items starting with a numeric value");
                }
                final NumericValue n = (NumericValue)first;
                return (n.compareTo(0) != 0) && !n.isNaN();
            } else if (first instanceof ObjectValue) {
                if (iterator.next() != null) {
                    ebvError("sequence of two or more items starting with a numeric value");
                }
                // return true if external object reference is not null
                return (((ObjectValue)first).getObject() != null);
            } else {
                ebvError("sequence starting with an atomic value other than a boolean, number, string, or URI");
                return false;
            }
        }
    }

    public static void ebvError(String reason) throws XPathException {
        DynamicError err = new DynamicError("Effective boolean value is not defined for a " + reason);
        err.setErrorCode("FORG0006");
        err.setIsTypeError(true);
        throw err;
    }


    /**
     * Determine whether an expression depends on any one of a set of variables
     * @param e the expression being tested
     * @param bindingList the set of variables being tested
     * @return true if the expression depends on one of the given variables
     */

    public static boolean dependsOnVariable(Expression e, Binding[] bindingList) {
        if (e instanceof VariableReference) {
            for (int i=0; i<bindingList.length; i++) {
                if (((VariableReference)e).getBinding() == bindingList[i]) {
                    return true;
                }
            }
            return false;
        } else {
            for (Iterator children = e.iterateSubExpressions(); children.hasNext();) {
                Expression child = (Expression)children.next();
                if (dependsOnVariable(child, bindingList)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Gather a list of all the variable bindings on which a given expression depends
     * @param e the expression being tested
     * @param list a list to which the bindings are to be added. The items in this list must
     * implement {@link Binding}
     */

    public static void gatherReferencedVariables(Expression e, List list) {
        if (e instanceof VariableReference) {
            Binding binding = ((VariableReference)e).getBinding();
            if (!list.contains(binding)) {
                list.add(binding);
            }
        } else {
            for (Iterator children = e.iterateSubExpressions(); children.hasNext();) {
                Expression child = (Expression)children.next();
                gatherReferencedVariables(child, list);
            }
        }
    }

    /**
     * Determine whether an expression contains a call on the function with a given fingerprint
     * @param exp The expression being tested
     * @param fp The fingerprint of the name of the function
     * @return true if the expression contains a call on the function
     */

    public static boolean callsFunction(Expression exp, int fp) {
        if (exp instanceof FunctionCall && (((FunctionCall)exp).getFunctionNameCode() & NamePool.FP_MASK) == fp) {
            return true;
        }
        Iterator iter = exp.iterateSubExpressions();
        while (iter.hasNext()) {
            Expression e = (Expression)iter.next();
            if (callsFunction(e, fp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gather a list of all the user-defined functions which a given expression calls directly
     * @param e the expression being tested
     * @param list a list of the functions that are called. The items in this list must
     * be objects of class {@link UserFunction}
     */

    public static void gatherCalledFunctions(Expression e, List list) {
        if (e instanceof UserFunctionCall) {
            UserFunction function = ((UserFunctionCall)e).getFunction();
            if (!list.contains(function)) {
                list.add(function);
            }
        } else {
            for (Iterator children = e.iterateSubExpressions(); children.hasNext();) {
                Expression child = (Expression)children.next();
                gatherCalledFunctions(child, list);
            }
        }
    }

    /**
     * Resolve calls to the current() function within an expression
     */

    public static Expression resolveCallsToCurrentFunction(Expression exp, Configuration config) throws XPathException {
        int current = config.getNamePool().getFingerprint(NamespaceConstant.FN, "current");
        if (current == -1) {
            return exp;
            // if the name fn:current is not in the name pool, then the expression can't contain any calls to it!
        }
        if (callsFunction(exp, current)) {
            RangeVariableDeclaration decl = new RangeVariableDeclaration();
            decl.setNameCode(config.getNamePool().allocate("saxon", NamespaceConstant.SAXON, "current" + exp.hashCode()));
            decl.setVariableName("saxon:current");
            decl.setRequiredType(SequenceType.SINGLE_ITEM);
            LetExpression let = new LetExpression();
            let.setSequence(new CurrentItemExpression());
            let.setVariableDeclaration(decl);
            PromotionOffer offer = new PromotionOffer(config.getOptimizer());
            offer.action = PromotionOffer.REPLACE_CURRENT;
            offer.containingExpression = let;
            exp = exp.promote(offer);
            let.setAction(exp);
            return let;
        } else {
            return exp;
        }
    }

    /**
     * Determine whether it is possible to rearrange an expression so that all references to a given
     * variable are replaced by a reference to ".". This is true of there are no references to the variable
     * within a filter predicate or on the rhs of a "/" operator.
     */

    public static boolean isVariableReplaceableByDot(Expression exp, Binding[] binding) {
        if (exp instanceof ComputedExpression) {
            if (exp instanceof FilterExpression) {
                Expression start = ((FilterExpression)exp).getBaseExpression();
                Expression filter = ((FilterExpression)exp).getFilter();
                return isVariableReplaceableByDot(start, binding) &&
                        !dependsOnVariable(filter, binding);
            } else if (exp instanceof PathExpression) {
                Expression start = ((PathExpression)exp).getFirstStep();
                Expression rest = ((PathExpression)exp).getRemainingSteps();
                return isVariableReplaceableByDot(start, binding) &&
                        !dependsOnVariable(rest, binding);
            } else {
                Iterator iter = exp.iterateSubExpressions();
                while (iter.hasNext()) {
                    Expression sub = (Expression)iter.next();
                    if (!isVariableReplaceableByDot(sub, binding)) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * Determine whether an expression can be evaluated without reference to the part of the context
     * document outside the subtree rooted at the context node.
     * @return true if the expression has no dependencies on the context node, or if the only dependencies
     * on the context node are downward selections using the self, child, descendant, attribute, and namespace
     * axes.
     */

    public static boolean isSubtreeExpression(Expression exp) {
        if (exp instanceof Value) {
            return true;
        }
        if ((exp.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) != 0) {
            if (exp instanceof ComputedExpression) {
                if (exp instanceof ContextItemExpression) {
                    return true;
                } else if (exp instanceof AxisExpression) {
                    return Axis.isSubtreeAxis[((AxisExpression)exp).getAxis()];
                } else if ((((ComputedExpression)exp).getIntrinsicDependencies() & StaticProperty.DEPENDS_ON_FOCUS) != 0) {
                    return false;
                } else if (exp instanceof ExtensionFunctionCall) {
                    return false;
                } else {
                    Iterator sub = exp.iterateSubExpressions();
                    while (sub.hasNext()) {
                        Expression s = (Expression)sub.next();
                        if (!isSubtreeExpression(s)) {
                            return false;
                        }
                    }
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return true;
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
