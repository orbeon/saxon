package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.evpull.EmptyEventIterator;
import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.evpull.EventIteratorOverSequence;
import org.orbeon.saxon.evpull.SingletonEventIterator;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.LocationMap;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.StringValue;

import javax.xml.transform.SourceLocator;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 * Interface supported by an XPath expression. This includes both compile-time
 * and run-time methods.
 */

public abstract class Expression
        implements SequenceIterable, EvaluableItem, Serializable, SourceLocator, InstructionInfo {

    public static final int EVALUATE_METHOD = 1;
    public static final int ITERATE_METHOD = 2;
    public static final int PROCESS_METHOD = 4;

    protected int staticProperties = -1;
    protected int locationId = -1;
    private Container container;
    private int[] slotsUsed;

//    private void writeObject(ObjectOutputStream oos) throws IOException {
//        System.err.println("Expression " + this.getClass());
//        oos.defaultWriteObject();
//    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     * {@link #PROCESS_METHOD}
     */

    public int getImplementationMethod() {
        if (Cardinality.allowsMany(getCardinality())) {
            return ITERATE_METHOD;
        } else {
            return EVALUATE_METHOD;
        }
    }

    /**
     * Determine whether this expression implements its own method for static type checking
     * @return true if this expression has a non-trivial implementation of the staticTypeCheck()
     * method
     */

    public boolean implementsStaticTypeCheck() {
        return false;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception org.orbeon.saxon.trans.XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    /**
     * Perform type checking of an expression and its subexpressions. This is the second phase of
     * static optimization.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables may not be accurately known if they have not been explicitly declared.</p>
     *
     * <p>If the implementation returns a value other than "this", then it is required to ensure that
     * the parent pointer and location information in the returned expression have been set up correctly.
     * It should not rely on the caller to do this, although for historical reasons many callers do so.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     * The parameter is set to null if it is known statically that the context item will be undefined.
     * If the type of the context item is not known statically, the argument is set to
     * {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @throws XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary run-time type checks,
     * and to perform other type-related optimizations
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
     * Static type checking of some expressions is delegated to the expression itself, by calling
     * this method. The default implementation of the method throws UnsupportedOperationException.
     * If there is a non-default implementation, then implementsStaticTypeCheck() will return true
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
        throw new UnsupportedOperationException("staticTypeCheck");
    }


    /**
     * Perform optimisation of an expression and its subexpressions. This is the third and final
     * phase of static optimization.
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     * The parameter is set to null if it is known statically that the context item will be undefined.
     * If the type of the context item is not known statically, the argument is set to
     * {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @throws XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten if appropriate to optimize execution
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }


    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *     expressions that don't depend on the context to an outer level in
     *     the containing expression
     * @exception org.orbeon.saxon.trans.XPathException if any error is detected
     * @return if the offer is not accepted, return this expression unchanged.
     *      Otherwise return the result of rewriting the expression to promote
     *      this subexpression
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        // The following temporary code checks that this method is implemented for all expressions
        // that have subexpressions

//        if (iterateSubExpressions().hasNext()) {
//            throw new UnsupportedOperationException("promote is not implemented for " + getClass());
//        }
        return this;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public final int getSpecialProperties() {
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.SPECIAL_PROPERTY_MASK;
    }

    /**
     * Determine the static cardinality of the expression. This establishes how many items
     * there will be in the result of the expression, at compile time (i.e., without
     * actually evaluating the result.
     *
     * @return one of the values Cardinality.ONE_OR_MORE,
     *     Cardinality.ZERO_OR_MORE, Cardinality.EXACTLY_ONE,
     *     Cardinality.ZERO_OR_ONE, Cardinality.EMPTY. This default
     *     implementation returns ZERO_OR_MORE (which effectively gives no
     *     information).
     */

    public int getCardinality() {
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.CARDINALITY_MASK;
    }

    /**
	 * Determine the data type of the expression, if possible. All expression return
	 * sequences, in general; this method determines the type of the items within the
	 * sequence, assuming that (a) this is known in advance, and (b) it is the same for
	 * all items in the sequence.
     *
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
	 *
	 * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
	 *     Type.NODE, or Type.ITEM (meaning not known at compile time)
     * @param th the type hierarchy cache
     */

    public abstract ItemType getItemType(TypeHierarchy th);

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     *
     * @return a set of bit-significant flags identifying the dependencies of
     *     the expression
     */

    public int getDependencies() {
        // Implemented as a memo function: we only compute the dependencies
        // for each expression once
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.DEPENDENCY_MASK;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Given an expression that is an immediate child of this expression, test whether
     * the evaluation of the parent expression causes the child expression to be
     * evaluated repeatedly
     * @param child the immediate subexpression
     * @return true if the child expression is evaluated repeatedly
     */

    public boolean hasLoopingSubexpression(Expression child) {
        return false;
    }

    /**
     * Within the subtree rooted at this node, find the expression that is the parent of a given leaf node.
     * @param leaf the expression whose parent is required
     * @return the parent of the expression. If leaf is not found in the tree, return null
     */

    public Expression findParentOf(Expression leaf) {
        for (Iterator children = iterateSubExpressions(); children.hasNext();) {
            Expression child = (Expression)children.next();
            if (child == leaf) {
                return this;
            } else {
                Expression target = child.findParentOf(leaf);
                if (target != null) {
                    return target;
                }
            }
        }
        return null;
    }

    /**
     * Mark an expression as being "flattened". This is a collective term that includes extracting the
     * string value or typed value, or operations such as simple value construction that concatenate text
     * nodes before atomizing. The implication of all of these is that although the expression might
     * return nodes, the identity of the nodes has no significance. This is called during type checking
     * of the parent expression.
     * @param flattened set to true if the result of the expression is atomized or otherwise turned into
     * an atomic value
     */

    public void setFlattened(boolean flattened) {
        // no action in general
    }

    /**
     * Mark an expression as filtered: that is, it appears as the base expression in a filter expression.
     * This notification currently has no effect except when the expression is a variable reference.
     * @param filtered if true, marks this expression as the base of a filter expression
     */

    public void setFiltered(boolean filtered) {
        // default: do nothing
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception org.orbeon.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return iterate(context).next();
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @exception org.orbeon.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Item value = evaluateItem(context);
        return SingletonIterator.makeIterator(value);
    }

    /**
     * Deliver the result of the expression as a sequence of events.
     *
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
     * @param context The dynamic evaluation context
     * @return the result of the expression as an iterator over a sequence of PullEvent objects
     * @throws XPathException if a dynamic error occurs during expression evaluation
     */

    public EventIterator iterateEvents(XPathContext context) throws XPathException {
        int m = getImplementationMethod();
        if ((m & EVALUATE_METHOD) != 0) {
            Item item = evaluateItem(context);
            if (item == null) {
                return EmptyEventIterator.getInstance();
            } else {
                return new SingletonEventIterator(item);
            }
        } else {
            return new EventIteratorOverSequence(iterate(context));
        }
    }

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception org.orbeon.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the effective boolean value
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return ExpressionTool.effectiveBooleanValue(iterate(context));
    }

    /**
     * Evaluate an expression as a String. This function must only be called in contexts
     * where it is known that the expression will return a single string (or where an empty sequence
     * is to be treated as a zero-length string). Implementations should not attempt to convert
     * the result to a string, other than converting () to "". This method is used mainly to
     * evaluate expressions produced by compiling an attribute value template.
     *
     * @exception org.orbeon.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @exception ClassCastException if the result type of the
     *     expression is not xs:string?
     * @param context The context in which the expression is to be evaluated
     * @return the value of the expression, evaluated in the current context.
     *     The expression must return a string or (); if the value of the
     *     expression is (), this method returns "".
     */

    public CharSequence evaluateAsString(XPathContext context) throws XPathException {
        Item o = evaluateItem(context);
//        if (o instanceof AtomicValue && !((AtomicValue)o).hasBuiltInType()) {
//            o = ((AtomicValue) o).getPrimitiveValue();
//        }
        StringValue value = (StringValue) o;  // the ClassCastException is deliberate
        if (value == null) return "";
        return value.getStringValue();
    }

    /**
     * Process the instruction, without returning any tail calls
     * @param context The dynamic context, giving access to the current node,
     * the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        int m = getImplementationMethod();

        if ((m & EVALUATE_METHOD) != 0) {
            Item item = evaluateItem(context);
            if (item != null) {
                context.getReceiver().append(item, locationId, NodeInfo.ALL_NAMESPACES);
            }

        } else if ((m & ITERATE_METHOD) != 0) {

            SequenceIterator iter = iterate(context);
            SequenceReceiver out = context.getReceiver();
            try {
                while (true) {
                    Item it = iter.next();
                    if (it == null) {
                        break;
                    }
                    out.append(it, locationId, NodeInfo.ALL_NAMESPACES);
                }
            } catch (XPathException e) {
                e.maybeSetLocation(this);
                e.maybeSetContext(context);
                throw e;
            }

        } else {
            throw new AssertionError("process() is not implemented in the subclass " + getClass());
        }
    }

    /**
     * Evaluate an updating expression, adding the results to a Pending Update List.
     * The default implementation of this method, which is used for non-updating expressions,
     * throws an UnsupportedOperationException
     * @param context the XPath dynamic evaluation context
     * @param pul the pending update list to which the results should be written
     */

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
        throw new UnsupportedOperationException("Expression " + getClass() + " is not an updating expression");
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        // fallback implementation
        FastStringBuffer buff = new FastStringBuffer(120);
        String className = getClass().getName();
        while (true) {
            int dot = className.indexOf('.');
            if (dot >= 0) {
                className = className.substring(dot+1);
            } else {
                break;
            }
        }
        buff.append(className);
        Iterator iter = iterateSubExpressions();
        boolean first = true;
        while (iter.hasNext()) {
            buff.append(first ? "(" : ", ");
            buff.append(iter.next().toString());
            first = false;
        }
        buff.append(")");
        return buff.toString();
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     * @param level this argument is ignored
     * @param out the expression presenter used to display the structure
     * @param config the Saxon configuration
     * @deprecated since 9.0 - use the explain method
     */

    public void display(int level, PrintStream out, Configuration config) {
        try {
            ExpressionPresenter ep = new ExpressionPresenter(config,
                    ExpressionPresenter.defaultDestination(config, out));
            explain(ep);
        } catch (XPathException err) {
            // ignore the exception
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     * @param out the expression presenter used to display the structure
     */

    public abstract void explain(ExpressionPresenter out);

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied outputstream.
     * @param out the expression presenter used to display the structure
     */

    public final void explain(OutputStream out) {
        ExpressionPresenter ep = new ExpressionPresenter(getExecutable().getConfiguration(), out);
        explain(ep);
        ep.close();
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     * @param parentType the "given complex type": the method is checking that the nodes returned by this
     * expression are acceptable members of the content model of this type
     * @param env the static context
     * @param whole if true, we want to check that the value of this expression satisfies the content model
     * as a whole; if false we want to check that the value of the expression is acceptable as one part
     * of the content
     * @throws XPathException if the value delivered by this expression cannot be part of the content model
     * of the given type
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        //
    }

    /**
     * Mark an expression as being in a given Container. This link is used primarily for diagnostics:
     * the container links to the location map held in the executable.
     *
     * <p>This affects the expression and all its subexpressions. Any subexpressions that are not in the
     * same container are marked with the new container, and this proceeds recursively. However, any
     * subexpression that is already in the correct container is not modified.</p>
     *
     * @param container The container of this expression.
     */

    public void setContainer(Container container) {
        this.container = container;
        if (container != null) {
            Iterator children = iterateSubExpressions();
            while (children.hasNext()) {
                Expression child = (Expression)children.next();
                // child can be null while expressions are under construction
                if (child != null && child.getContainer() != container) {
                    child.setContainer(container);
                }
            }
        }
    }

    /**
     * Get the container in which this expression is located. This will usually be a top-level construct
     * such as a function or global variable, and XSLT template, or an XQueryExpression. In the case of
     * free-standing XPath expressions it will be the StaticContext object
     * @return the expression's container
     */

    public Container getContainer() {
        return container;
    }

    /**
     * Set up a parent-child relationship between this expression and a given child expression.
     * <p>
     * Note: many calls on this method are now redundant, but are kept in place for "belt-and-braces"
     * reasons. The rule is that an implementation of simplify(), typeCheck(), or optimize() that returns
     * a value other than "this" is required to set the location information and parent pointer in the new
     * child expression. However, in the past this was often left to the caller, which did it by calling
     * this method, either unconditionally on return from one of these methods, or after testing that the
     * returned object was not the same as the original.
     * @param child the child expression
     */

    public void adoptChildExpression(Expression child) {
        if (child == null) {
            return;
        }

        if (container == null) {
            container = child.container;
        } else {
            child.setContainer(container);
        }

        if (locationId == -1) {
            ExpressionTool.copyLocationInfo(child, this);
        } else if (child.locationId == -1) {
            ExpressionTool.copyLocationInfo(this, child);
        }
        resetLocalStaticProperties();
    }

    /**
     * Set the location ID on an expression.
     * @param id the location id
     */

    public void setLocationId(int id) {
        locationId = id;
    }

    /**
     * Get the location ID of the expression
     * @return a location identifier, which can be turned into real
     * location information by reference to a location provider
     */

    public final int getLocationId() {
        return locationId;
    }

    /**
     * Get the line number of the expression
     */

    public int getLineNumber() {
        if (locationId == -1) {
            return -1;
        }
        return locationId & 0xfffff;
    }

    /**
     * Get the column number of the expression
     */

    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get the systemId of the module containing the expression
     */

    public String getSystemId() {
        if (locationId == -1) {
            return null;
        }
        Executable exec = getExecutable();
        if (exec == null) {
            return null;
        }
        LocationMap map = exec.getLocationMap();
        if (map == null) {
            return null;
        }
        return map.getSystemId(locationId);
    }

    /**
     * Get the publicId of the module containing the expression (to satisfy the SourceLocator interface)
     */

    public final String getPublicId() {
        return null;
    }

    /**
     * Get the executable containing this expression
     * @return the containing Executable
     */

    public Executable getExecutable() {
        return getContainer().getExecutable();
    }

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     * @return the LocationProvider used to turn the location id into real location information
     */

    public LocationProvider getLocationProvider() {
        Executable exec = getExecutable();
        if (exec != null) {
            return exec.getLocationMap();
        } else {
            return null;
        }
    }

    /**
     * Promote a subexpression if possible, and if the expression was changed, carry out housekeeping
     * to reset the static properties and correct the parent pointers in the tree
     * @param subexpression the subexpression that is a candidate for promotion
     * @param offer details of the promotion being considered
     * @return the result of the promotion. This will be the current expression if no promotion
     * actions have taken place
     */

    public final Expression doPromotion(Expression subexpression, PromotionOffer offer) throws XPathException {
        Expression e = subexpression.promote(offer);
        if (e != subexpression) {
            adoptChildExpression(e);
        } else if (offer.accepted) {
            resetLocalStaticProperties();
        }
        return e;
    }

    /**
     * Compute the static properties. This should only be done once for each
     * expression.
     */

    public final void computeStaticProperties() {
        staticProperties =
                computeDependencies() |
                computeCardinality() |
                computeSpecialProperties();
    }

    /**
     * Reset the static properties of the expression to -1, so that they have to be recomputed
     * next time they are used.
     */

    protected void resetLocalStaticProperties() {
        staticProperties = -1;
    }

    /**
     * Compute the static cardinality of this expression
     * @return the computed cardinality, as one of the values {@link StaticProperty#ALLOWS_ZERO_OR_ONE},
     * {@link StaticProperty#EXACTLY_ONE}, {@link StaticProperty#ALLOWS_ONE_OR_MORE},
     * {@link StaticProperty#ALLOWS_ZERO_OR_MORE}
     */

    protected abstract int computeCardinality();

    /**
     * Compute the special properties of this expression. These properties are denoted by a bit-significant
     * integer, possible values are in class {@link StaticProperty}. The "special" properties are properties
     * other than cardinality and dependencies, and most of them relate to properties of node sequences, for
     * example whether the nodes are in document order.
     * @return the special properties, as a bit-significant integer
     */

    protected int computeSpecialProperties() {
        return 0;
    }

    /**
     * Compute the dependencies of an expression, as the union of the
     * dependencies of its subexpressions. (This is overridden for path expressions
     * and filter expressions, where the dependencies of a subexpression are not all
     * propogated). This method should be called only once, to compute the dependencies;
     * after that, getDependencies should be used.
     * @return the depencies, as a bit-mask
     */

    public int computeDependencies() {
        int dependencies = getIntrinsicDependencies();
        for (Iterator children = iterateSubExpressions(); children.hasNext();) {
            dependencies |= ((Expression)children.next()).getDependencies();
        }
        return dependencies;
    }

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     *     dependencies. The flags are documented in class org.orbeon.saxon.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        return 0;
    }

    /**
     * Check to ensure that this expression does not contain any inappropriate updating subexpressions.
     * This check is overridden for those expressions that permit updating subexpressions.
     * @throws XPathException if the expression has a non-permitted updating subexpression
     */

    public void checkForUpdatingSubexpressions() throws XPathException {
        for (Iterator iter = iterateSubExpressions(); iter.hasNext();) {
            Expression sub = (Expression)iter.next();
            sub.checkForUpdatingSubexpressions();
            if (sub.isUpdatingExpression()) {
                XPathException err = new XPathException(
                        "Updating expression appears in a context where it is not permitted", "XUST0001");
                err.setLocator(sub);
                throw err;
            }
        }
    }

    /**
     * Determine whether this is an updating expression as defined in the XQuery update specification
     * @return true if this is an updating expression
     */

    public boolean isUpdatingExpression() {
        for (Iterator iter = iterateSubExpressions(); iter.hasNext();) {
            if (((Expression)iter.next()).isUpdatingExpression()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public abstract Expression copy();

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        // overridden in subclasses
        throw new IllegalArgumentException("Invalid replacement");
    }

    /**
     * Suppress validation on contained element constructors, on the grounds that the parent element
     * is already performing validation. The default implementation does nothing.
     * @param validationMode the kind of validation being performed on the parent expression
     */

    public void suppressValidation(int validationMode) {
        // do nothing
    }

    /**
     * Mark tail-recursive calls on stylesheet functions. For most expressions, this does nothing.
     * @param qName the name of the function
     * @param arity the arity (number of parameters) of the function
     *
     * @return 0 if no tail call was found; 1 if a tail call on a different function was found;
     * 2 if a tail recursive call was found and if this call accounts for the whole of the value.
     */

    public int markTailFunctionCalls(StructuredQName qName, int arity) {
        return 0;
    }

    /**
     * Get the local variables (identified by their slot numbers) on which this expression depends.
     * Should only be called if the caller has established that there is a dependency on local variables.
     * @return an array of integers giving the slot numbers of the local variables referenced in this
     * expression.
     */

    public synchronized int[] getSlotsUsed() {
        // synchronized because it's calculated lazily at run-time the first time it's needed
        if (slotsUsed != null) {
            return slotsUsed;
        }
        IntHashSet slots = new IntHashSet(10);
        gatherSlotsUsed(this, slots);
        slotsUsed = new int[slots.size()];
        int i=0;
        IntIterator iter = slots.iterator();
        while (iter.hasNext()) {
            slotsUsed[i++] = iter.next();
        }
        Arrays.sort(slotsUsed);
        return slotsUsed;
    }

    private static void gatherSlotsUsed(Expression exp, IntHashSet slots) {
        if (exp instanceof VariableReference) {
            Binding binding = ((VariableReference)exp).getBinding();
            if (binding == null) {
                throw new NullPointerException("Unbound variable at line " + exp.getLineNumber());
            }
            if (!binding.isGlobal()) {
                int slot = binding.getLocalSlotNumber();
                if (slot != -1) {
                    if (!slots.contains(slot)) {
                        slots.add(slot);
                    }
                }
            }
        } else {
            Iterator iter = exp.iterateSubExpressions();
            while (iter.hasNext()) {
                Expression sub = (Expression)iter.next();
                gatherSlotsUsed(sub, slots);
            }
        }
    }

    /**
     * Method used in subclasses to signal a dynamic error
     * @param message the error message
     * @param code the error code
     * @param context the XPath dynamic context
     */

    protected void dynamicError(String message, String code, XPathContext context) throws XPathException {
        XPathException err = new XPathException(message, this);
        err.setXPathContext(context);
        err.setErrorCode(code);
        throw err;
    }

    /**
     * Method used in subclasses to signal a runtime type error
     * @param message the error message
     * @param errorCode the error code
     * @param context the XPath dynamic context
     */

    protected void typeError(String message, String errorCode, XPathContext context) throws XPathException {
        XPathException e = new XPathException(message, this);
        e.setIsTypeError(true);
        e.setErrorCode(errorCode);
        e.setXPathContext(context);
        throw e;
    }

    /**
     * Get InstructionInfo for this expression
     */

//    public InstructionInfo getInstructionInfo() {
//        InstructionDetails details = new InstructionDetails();
//        details.setConstructType(getConstructType());
//        details.setProperty("expression", this);
//        details.setSystemId(getSystemId());
//        details.setLineAndColumn(getLineNumber());
//        details.setColumnNumber(getColumnNumber());
//        if (this instanceof Assignation) {
//            details.setObjectName(((Assignation)this).getVariableQName());
//        }
//        return details;
//    }

    /**
     * Get the type of this expression for use in tracing and diagnostics
     * @return the type of expression, as enumerated in class {@link org.orbeon.saxon.trace.Location}
     */

    public int getConstructType() {
        return Location.XPATH_EXPRESSION;
    }

    public StructuredQName getObjectName() {
        return null;
    }

    public Object getProperty(String name) {
        if (name.equals("expression")) {
            return this;
        } else {
            return null;
        }
    }


    /**
     * Get the line number within the document or module containing a particular location
     *
     * @param locationId identifier of the location in question (as passed down the Receiver pipeline)
     * @return the line number within the document or module.
     */

    public int getLineNumber(long locationId) {
        return getLineNumber();
    }

    public int getColumnNumber(long locationId) {
        return getColumnNumber();
    }

    /**
     * Get the URI of the document or module containing a particular location
     *
     * @param locationId identifier of the location in question (as passed down the Receiver pipeline)
     * @return the URI of the document or module.
     */

    public String getSystemId(long locationId) {
        return getSystemId();
    }

    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property. The iterator may return properties whose
     * value is null.
     */

    public Iterator getProperties() {
        return new MonoIterator("expression");
    }

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     * @return typically {@link org.orbeon.saxon.Configuration#XSLT} or {@link org.orbeon.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage() {
        return getContainer().getHostLanguage();
    }

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * <p>The default implementation of this method assumes that an expression does no navigation other than
     * the navigation done by evaluating its subexpressions, and that the subexpressions are evaluated in the
     * same context as the containing expression. The method must be overridden for any expression
     * where these assumptions do not hold. For example, implementations exist for AxisExpression, ParentExpression,
     * and RootExpression (because they perform navigation), and for the doc(), document(), and collection()
     * functions because they create a new navigation root. Implementations also exist for PathExpression and
     * FilterExpression because they have subexpressions that are evaluated in a different context from the
     * calling expression.</p>
     *
     * @param pathMap the PathMap to which the expression should be added
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
     * @return the pathMapNodeSet representing the points in the source document that are both reachable by this
     * expression, and that represent possible results of this expression. For an expression that does
     * navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     * expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        boolean dependsOnFocus = (getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) != 0;
        TypeHierarchy th = getExecutable().getConfiguration().getTypeHierarchy();
        PathMap.PathMapNodeSet attachmentPoint;
        if (pathMapNodeSet == null) {
            if (dependsOnFocus) {
                ContextItemExpression cie = new ContextItemExpression();
                cie.setContainer(getContainer());
                pathMapNodeSet = new PathMap.PathMapNodeSet(pathMap.makeNewRoot(cie));
            }
            attachmentPoint = pathMapNodeSet;
        } else {
            attachmentPoint = (dependsOnFocus ? pathMapNodeSet : null);
        }
        PathMap.PathMapNodeSet result = new PathMap.PathMapNodeSet();
        for (Iterator iter = iterateSubExpressions(); iter.hasNext(); ) {
            Expression child = (Expression)iter.next();
            result.addNodeSet(child.addToPathMap(pathMap, attachmentPoint));
        }
        if (getItemType(th) instanceof AtomicType) {
            // if expression returns an atomic value then any nodes accessed don't contribute to the result
            return null;
        } else {
            return result;
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