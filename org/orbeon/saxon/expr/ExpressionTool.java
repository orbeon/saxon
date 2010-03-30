package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.SequenceOutputter;
import org.orbeon.saxon.functions.Current;
import org.orbeon.saxon.functions.ExtensionFunctionCall;
import org.orbeon.saxon.functions.Put;
import org.orbeon.saxon.instruct.Block;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.instruct.UserFunction;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.value.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
    public static final int MAKE_INDEXED_VARIABLE = 12;
    public static final int MAKE_SINGLETON_CLOSURE = 13;

    private ExpressionTool() {}

    /**
     * Parse an expression. This performs the basic analysis of the expression against the
     * grammar, it binds variable references and function calls to variable definitions and
     * function definitions, and it performs context-independent expression rewriting for
     * optimization purposes.
     *
     * @param expression The expression (as a character string)
     * @param env An object giving information about the compile-time
     *     context of the expression
     * @param start position of the first significant character in the expression
     * @param terminator The token that marks the end of this expression; typically
     * Tokenizer.EOF, but may for example be a right curly brace
     * @param lineNumber the line number of the start of the expression
     * @param compileWithTracing true if diagnostic tracing during expression parsing is required
     * @return an object of type Expression
     * @throws XPathException if the expression contains a static error
     */

    public static Expression make(String expression, StaticContext env,
                                  int start, int terminator, int lineNumber, boolean compileWithTracing) throws XPathException {
        ExpressionParser parser = new ExpressionParser();
        parser.setCompileWithTracing(compileWithTracing);
        if (terminator == -1) {
            terminator = Token.EOF;
        }
        Expression exp = parser.parse(expression, start, terminator, lineNumber, env);
        exp = ExpressionVisitor.make(env).simplify(exp);
        return exp;
    }

    /**
     * Copy location information (the line number and reference to the container) from one expression
     * to another
     * @param from the expression containing the location information
     * @param to the expression to which the information is to be copied
     */

    public static void copyLocationInfo(Expression from, Expression to) {
        if (from != null && to != null) {
            to.setLocationId(from.getLocationId());
            to.setContainer(from.getContainer());
        }
    }

    /**
     * Remove unwanted sorting from an expression, at compile time
     * @param opt the expression optimizer
     * @param exp the expression to be optimized
     * @param retainAllNodes true if there is a need to retain exactly those nodes returned by exp
     * even if there are duplicates; false if the caller doesn't mind whether duplicate nodes
     * are retained or eliminated
     * @return the expression after rewriting
     */

    public static Expression unsorted(Optimizer opt, Expression exp, boolean retainAllNodes)
    throws XPathException {
        if (exp instanceof Literal) {
            return exp;   // fast exit
        }
        PromotionOffer offer = new PromotionOffer(opt);
        offer.action = PromotionOffer.UNORDERED;
        offer.retainAllNodes = retainAllNodes;
        return exp.promote(offer);
    }

    /**
     * Remove unwanted sorting from an expression, at compile time, if and only if it is known
     * that the result of the expression will be homogeneous (all nodes, or all atomic values).
     * This is done when we need the effective boolean value of a sequence: the EBV of a
     * homogenous sequence does not depend on its order, but this is not true when atomic
     * values and nodes are mixed: (N, AV) is true, but (AV, N) is an error.
     * @param opt the expression optimizer
     * @param exp the expression to be optimized
     * @return the expression after rewriting
     */

    public static Expression unsortedIfHomogeneous(Optimizer opt, Expression exp)
    throws XPathException {
        if (exp instanceof Literal) {
            return exp;   // fast exit
        }
        if (exp.getItemType(opt.getConfiguration().getTypeHierarchy()) instanceof AnyItemType) {
            return exp;
        } else {
            PromotionOffer offer = new PromotionOffer(opt);
            offer.action = PromotionOffer.UNORDERED;
            offer.retainAllNodes = false;
                // TODO: redundant code. All callers set this to false
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
        if (exp instanceof Literal) {
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
            if (Cardinality.allowsMany(exp.getCardinality())) {
                return MAKE_MEMO_CLOSURE;
            } else {
                return MAKE_SINGLETON_CLOSURE;
            }

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
                        ((Block)exp).getChildren()[0] instanceof Literal)) {
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
        if (exp instanceof Literal && !(((Literal)exp).getValue() instanceof Closure)) {
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
     * @param exp the expression to be evaluated
     * @param evaluationMode the evaluation mode for this expression
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
                return ((Literal)exp).getValue();

            case EVALUATE_VARIABLE:
                return ((VariableReference)exp).evaluateVariable(context);

            case MAKE_CLOSURE:
                return Closure.make(exp, context, ref);
                //return new SequenceExtent(exp.iterate(context));

            case MAKE_MEMO_CLOSURE:
                return Closure.make(exp, context, (ref==1 ? 10 : ref));

            case MAKE_SINGLETON_CLOSURE:
                return new SingletonClosure(exp, context);

            case RETURN_EMPTY_SEQUENCE:
                return EmptySequence.getInstance();

            case EVALUATE_AND_MATERIALIZE_VARIABLE:
                ValueRepresentation v = ((VariableReference)exp).evaluateVariable(context);
                if (v instanceof Closure) {
                    return SequenceExtent.makeSequenceExtent(((Closure)v).iterate());
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
                if (ref == FilterExpression.FILTERED) {
                    return context.getConfiguration().getOptimizer().makeSequenceExtent(exp, ref, context);
                } else {
                    return SequenceExtent.makeSequenceExtent(exp.iterate(context));
                }

            case PROCESS:
                Controller controller = context.getController();
                XPathContext c2 = context.newMinorContext();
                c2.setOrigin(exp);
                SequenceOutputter seq = controller.allocateSequenceOutputter(20);
                PipelineConfiguration pipe = controller.makePipelineConfiguration();
                pipe.setHostLanguage(exp.getHostLanguage());
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
                    SequenceIterator it = ((MemoClosure)base).iterate();
                    base = ((GroundedIterator)it).materialize();
                }
                if (base instanceof IntegerRange) {
                    long start = ((IntegerRange)base).getStart() + 1;
                    long end = ((IntegerRange)base).getEnd();
                    if (start == end) {
                        return Int64Value.makeIntegerValue(end);
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
                if (exp instanceof Block) {
                    Block block = (Block)exp;
                    Expression base = block.getChildren()[0];
                    Value baseVal;
                    if (base instanceof Literal) {
                        baseVal = ((Literal)base).getValue();
                    } else if (base instanceof VariableReference) {
                        baseVal = Value.asValue(evaluate(base, EVALUATE_VARIABLE, context, ref));
                        if (baseVal instanceof MemoClosure && ((MemoClosure)baseVal).isFullyRead()) {
                            baseVal = ((MemoClosure)baseVal).materialize();
                        }
                    } else {
                        throw new AssertionError("base of shared append expression is of class " + base.getClass());
                    }
                    if (baseVal instanceof ShareableSequence && ((ShareableSequence)baseVal).isShareable()) {
                        List list = ((ShareableSequence)baseVal).getList();
                        SequenceIterator iter = block.getChildren()[1].iterate(context);
                        while (true) {
                            Item i = iter.next();
                            if (i == null) {
                                break;
                            }
                            list.add(i);
                        }
                        return new ShareableSequence(list);
                    } else {
                        List list = new ArrayList(20);
                        SequenceIterator iter = baseVal.iterate();
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
                } else {
                    // it's not a Block: it must have been rewritten after deciding to use this evaluation mode
                    return SequenceExtent.makeSequenceExtent(exp.iterate(context));
                }
            }

            case MAKE_INDEXED_VARIABLE:
                return context.getConfiguration().getOptimizer().makeIndexedValue(exp.iterate(context));

            default:
                throw new IllegalArgumentException("Unknown evaluation mode " + evaluationMode);

        }
    }

    /**
     * Do lazy evaluation of an expression. This will return a value, which may optionally
     * be a SequenceIntent, which is a wrapper around an iterator over the value of the expression.
     * @param exp the expression to be evaluated
     * @param context the run-time evaluation context for the expression. If
     *     the expression is not evaluated immediately, then parts of the
     *     context on which the expression depends need to be saved as part of
     *      the Closure
     * @param ref an indication of how the value will be used. The value 1 indicates that the value
     *     is only expected to be used once, so that there is no need to keep it in memory. A small value >1
     *     indicates multiple references, so the value will be saved when first evaluated. The special value
     *     FILTERED indicates a reference within a loop of the form $x[predicate], indicating that the value
     *     should be saved in a way that permits indexing.
     * @return a value: either the actual value obtained by evaluating the
     *     expression, or a Closure containing all the information needed to
     *     evaluate it later
     * @throws XPathException if any error occurs in evaluating the
     *     expression
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
     * @param qName the name of the containing function
     * @param arity the arity of the containing function
     * @return 0 if no tail call was found; 1 if a tail call to a different function was found;
     * 2 if a tail call to the specified function was found. In this case the
     * UserFunctionCall object representing the tail function call will also have been marked as
     * a tail call.
     */

    public static int markTailFunctionCalls(Expression exp, StructuredQName qName, int arity) {
        return exp.markTailFunctionCalls(qName, arity);
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
                frame.allocateSlotNumber(((Assignation)exp).getVariableQName());
                if (count == 2) {
                    frame.allocateSlotNumber(((ForExpression)exp).getPositionVariableName());
                }
            }
        }
        if (exp instanceof VariableReference) {
            VariableReference var = (VariableReference)exp;
            Binding binding = var.getBinding();
            if (exp instanceof LocalVariableReference) {
                ((LocalVariableReference)var).setSlotNumber(binding.getLocalSlotNumber());
            }
            if (binding instanceof Assignation && binding.getLocalSlotNumber() < 0) {
                // This indicates something badly wrong: we've found a variable reference on the tree, that's
                // bound to a variable declaration that is no longer on the tree. All we can do is print diagnostics.
                Assignation decl = (Assignation)binding;
                String msg = "*** Internal Saxon error: local variable encountered whose binding has been deleted";
                System.err.println(msg);
                System.err.println("Variable name: " + decl.getVariableName());
                System.err.println("Line number of reference: " + var.getLocationId());
                System.err.println("Line number of declaration: " + decl.getLocationId());
                System.err.println("DECLARATION:");
                decl.explain(System.err);
                throw new IllegalStateException(msg);
            }

        }
        for (Iterator children = exp.iterateSubExpressions(); children.hasNext();) {
            Expression child = (Expression)children.next();
            nextFree = allocateSlots(child, nextFree, frame, false);
        }

        return nextFree;

        // Note, we allocate a distinct slot to each range variable, even if the
        // scopes don't overlap. This isn't strictly necessary, but might help
        // debugging.
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
            iterator.close();
            return true;
        } else {
            //first = ((AtomicValue)first).getPrimitiveValue();
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
                    ebvError("sequence of two or more items starting with an external object value");
                }
                // return true if external object reference is not null
                return (((ObjectValue)first).getObject() != null);
            } else {
                ebvError("sequence starting with an atomic value other than a boolean, number, string, or URI");
                return false;
            }
        }
    }

    /**
     * Report an error in computing the effective boolean value of an expression
     * @param reason the nature of the error
     * @throws XPathException
     */

    public static void ebvError(String reason) throws XPathException {
        XPathException err = new XPathException("Effective boolean value is not defined for a " + reason);
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
        if (bindingList == null || bindingList.length == 0) {
            return false;
        }
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
     * @param qName The name of the function
     * @return true if the expression contains a call on the function
     */

    public static boolean callsFunction(Expression exp, StructuredQName qName) {
        if (exp instanceof FunctionCall && (((FunctionCall)exp).getFunctionName().equals(qName))) {
            return true;
        }
        Iterator iter = exp.iterateSubExpressions();
        while (iter.hasNext()) {
            Expression e = (Expression)iter.next();
            if (callsFunction(e, qName)) {
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
     * Gather a list of the names of the user-defined functions which a given expression calls directly
     * @param e the expression being tested
     * @param list a list of the functions that are called. The items in this list are strings in the format
     * "{uri}local/arity"
     */

    public static void gatherCalledFunctionNames(Expression e, List list) {
        if (e instanceof UserFunctionCall) {
            StructuredQName name = ((UserFunctionCall)e).getFunctionName();
            int arity = ((UserFunctionCall)e).getNumberOfArguments();
            String key = name.getClarkName() + "/" + arity;
            if (!list.contains(key)) {
                list.add(key);
            }
        } else {
            for (Iterator children = e.iterateSubExpressions(); children.hasNext();) {
                Expression child = (Expression)children.next();
                gatherCalledFunctionNames(child, list);
            }
        }
    }


    /**
     * Reset cached static properties within a subtree, meaning that they have to be
     * recalulated next time they are required
     * @param exp the root of the subtree within which static properties should be reset
     */

    public static void resetPropertiesWithinSubtree(Expression exp) {
        exp.resetLocalStaticProperties();
        for (Iterator children = exp.iterateSubExpressions(); children.hasNext();) {
            Expression child = (Expression)children.next();
            resetPropertiesWithinSubtree(child);
        }
    }

    /**
     * Resolve calls to the XSLT current() function within an expression
     * @param exp the expression within which calls to current() should be resolved
     * @param config the Saxon configuration
     * @return the expression after resolving calls to current()
     */

    public static Expression resolveCallsToCurrentFunction(Expression exp, Configuration config)
            throws XPathException {
        if (callsFunction(exp, Current.FN_CURRENT)) {
            LetExpression let = new LetExpression();
            let.setVariableQName(
                    new StructuredQName("saxon", NamespaceConstant.SAXON, "current" + exp.hashCode()));
            let.setRequiredType(SequenceType.SINGLE_ITEM);
            let.setSequence(new CurrentItemExpression());
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
     * Determine whether an expression can be evaluated without reference to the part of the context
     * document outside the subtree rooted at the context node.
     * @param exp the expression in question
     * @return true if the expression has no dependencies on the context node, or if the only dependencies
     * on the context node are downward selections using the self, child, descendant, attribute, and namespace
     * axes.
     */

    public static boolean isSubtreeExpression(Expression exp) {
        if (exp instanceof Literal) {
            return true;
        }
        if ((exp.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) != 0) {
            if (exp instanceof ContextItemExpression) {
                return true;
            } else if (exp instanceof AxisExpression) {
                return Axis.isSubtreeAxis[((AxisExpression)exp).getAxis()];
            } else if ((exp.getIntrinsicDependencies() & StaticProperty.DEPENDS_ON_FOCUS) != 0) {
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
            return true;
        }
    }

    /**
     * Get a list of all references to a particular variable within a subtree
     * @param exp the expression at the root of the subtree
     * @param binding the variable binding whose references are sought
     * @param list a list to be populated with the references to this variable
     */

    public static void gatherVariableReferences(Expression exp, Binding binding, List list) {
        if (exp instanceof VariableReference &&
                ((VariableReference)exp).getBinding() == binding) {
            list.add(exp);
        } else {
            for (Iterator iter = exp.iterateSubExpressions(); iter.hasNext(); ) {
                gatherVariableReferences((Expression)iter.next(), binding, list);
            }
        }
    }

    /**
     * Determine how often a variable is referenced. This is the number of times
     * it is referenced at run-time: so a reference in a loop counts as "many". This code
     * currently handles local variables (Let expressions) and function parameters. It is
     * not currently used for XSLT template parameters. It's not the end of the world if
     * the answer is wrong (unless it's wrongly given as zero), but if wrongly returned as
     * 1 then the variable will be repeatedly evaluated.
     * @param exp the expression within which variable references are to be counted
     * @param binding identifies the variable of interest
     * @param inLoop true if the expression is within a loop, in which case a reference counts as many.
     * This should be set to false on the initial call, it may be set to true on an internal recursive
     * call 
     * @return the number of references. The interesting values are 0, 1,  "many" (represented
     * by any value >1), and the special value FILTERED, which indicates that there are
     * multiple references and one or more of them is of the form $x[....] indicating that an
     * index might be useful.
     */

    public static int getReferenceCount(Expression exp, Binding binding, boolean inLoop) {
        int rcount = 0;
        if (exp instanceof VariableReference && ((VariableReference)exp).getBinding() == binding) {
            if (((VariableReference)exp).isFiltered()) {
                return FilterExpression.FILTERED;
            } else {
                rcount += (inLoop ? 10 : 1);
            }
        } else {
            for (Iterator iter = exp.iterateSubExpressions(); iter.hasNext(); ) {
                Expression child = (Expression)iter.next();
                boolean childLoop = inLoop || (exp.hasLoopingSubexpression(child));
                rcount += getReferenceCount(child, binding, childLoop);
                if (rcount >= FilterExpression.FILTERED) {
                    break;
                }
            }
        }
        return rcount;
    }

    /**
     * Gather the set of all the subexpressions of an expression (the transitive closure)
     * @param exp the parent expression
     * @param set the set to be populated; on return it will contain all the subexpressions.
     * Beware that testing for membership of a set of expressions relies on the equals() comparison,
     * which does not test identity.
     */

    public static void gatherAllSubExpressions(Expression exp, HashSet set) {
        set.add(exp);
        for (Iterator iter = exp.iterateSubExpressions(); iter.hasNext(); ) {
            gatherAllSubExpressions((Expression)iter.next(), set);
        }
    }

    /**
     * Get the size of an expression tree (the number of subexpressions it contains)
     * @param exp the expression whose size is required
     * @return the size of the expression tree, as the number of nodes
     */

    public static int expressionSize(Expression exp) {
        int total = 1;
        for (Iterator iter = exp.iterateSubExpressions(); iter.hasNext(); ) {
            total += expressionSize((Expression)iter.next());
        }
        return total;
    }


    /**
     * Rebind all variable references to a binding
     * @param exp the expression whose contained variable references are to be rebound
     * @param oldBinding the old binding for the variable references
     * @param newBinding the new binding to which the variables should be rebound
     */

    public static void rebindVariableReferences(
            Expression exp, Binding oldBinding, Binding newBinding) {
        if (exp instanceof VariableReference) {
            if (((VariableReference)exp).getBinding() == oldBinding) {
                ((VariableReference)exp).fixup(newBinding);
            }
        } else {
            Iterator iter = exp.iterateSubExpressions();
            while (iter.hasNext()) {
                Expression e = (Expression)iter.next();
                rebindVariableReferences(e, oldBinding, newBinding);
            }
        }
    }

    /**
     * Determine whether the expression is either an updating expression, or an expression that is permitted
     * in a context where updating expressions are allowed
     * @param exp the expression under test
     * @return true if the expression is an updating expression, or an empty sequence, or a call on error()
     */

    public static boolean isAllowedInUpdatingContext(Expression exp) {
        return Literal.isEmptySequence(exp) ||
                exp instanceof org.orbeon.saxon.functions.Error ||
                exp.isUpdatingExpression() ||
                exp instanceof Put ||
                (exp instanceof LetExpression && isAllowedInUpdatingContext(((LetExpression)exp).getAction()));
        // The last condition is stretching the rules in the XQuery update specification. We need the rule because
        // the branches of a typeswitch get turned into let expressions before we can check them.
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
