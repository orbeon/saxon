package net.sf.saxon.pattern;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.style.ExpressionContext;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.IntegerValue;

import java.util.Iterator;
import java.util.Arrays;
import java.util.Collections;

/**
* A LocationPathPattern represents a path, for example of the form A/B/C... The components are represented
* as a linked list, each component pointing to its predecessor
*/

public final class LocationPathPattern extends Pattern {

    // the following public variables are exposed to the ExpressionParser

    public Pattern parentPattern = null;
    public Pattern ancestorPattern = null;
    public NodeTest nodeTest = AnyNodeTest.getInstance();
    protected Expression[] filters = null;
    protected int numberOfFilters = 0;
    protected Expression equivalentExpr = null;
    protected boolean firstElementPattern = false;
    protected boolean lastElementPattern = false;
    protected boolean specialFilter = false;
    private boolean usesCurrent = false;

    /**
    * Add a filter to the pattern (while under construction)
    * @param filter The predicate (a boolean expression or numeric expression) to be added
    */

    public void addFilter(Expression filter) {
    	if (filters==null) {
    		filters = new Expression[3];
    	} else if (numberOfFilters == filters.length) {
            Expression[] f2 = new Expression[numberOfFilters * 2];
            System.arraycopy(filters, 0, f2, 0, numberOfFilters);
            filters = f2;
        }
        filters[numberOfFilters++] = filter;
        ExpressionTool.makeParentReferences(filter);
        if (filter instanceof ComputedExpression) {
            ((ComputedExpression)filter).setParentExpression(this);
        }
    }

    /**
    * Simplify the pattern: perform any context-independent optimisations
    */

    public Pattern simplify(StaticContext env) throws XPathException {

        // detect the simple cases: no parent or ancestor pattern, no predicates

        if (    parentPattern == null &&
                ancestorPattern == null &&
                filters == null) {
            NodeTestPattern ntp = new NodeTestPattern(nodeTest);
            ntp.setSystemId(getSystemId());
            ntp.setLineNumber(getLineNumber());
            return ntp;
        }

        // simplify each component of the pattern

        if (parentPattern != null) {
            parentPattern = parentPattern.simplify(env);
            if (parentPattern instanceof LocationPathPattern) {
                usesCurrent = ((LocationPathPattern)parentPattern).usesCurrent;
            }
        } else if (ancestorPattern != null) {
            ancestorPattern = ancestorPattern.simplify(env);
            if (ancestorPattern instanceof LocationPathPattern) {
                usesCurrent = ((LocationPathPattern)ancestorPattern).usesCurrent;
            }
        }

        if (filters != null) {
	        for (int i=numberOfFilters-1; i>=0; i--) {
	            Expression filter = filters[i].simplify(env);
	            filters[i] = filter;
                int dep = filter.getDependencies();
                if ((dep & StaticProperty.DEPENDS_ON_CURRENT_ITEM) != 0) {
	                usesCurrent = true;
	            }
                if ((dep & StaticProperty.DEPENDS_ON_CURRENT_GROUP) != 0) {
                    String errorCode = "XT1060";
                    String function = "current-group()";
                    if (toString().indexOf("current-grouping-key") >= 0) {
                        errorCode = "XT1070";
                        function = "current-grouping-key()";
                    }
                    StaticError err = new StaticError("The " + function + " function cannot be used in a pattern");
                    err.setErrorCode(errorCode);
                    throw err;
                }
	        }
	    }

        return this;
    }

    /**
    * Type-check the pattern, performing any type-dependent optimizations.
    * @return the optimised Pattern
    */

    public Pattern analyze(StaticContext env, ItemType contextItemType) throws XPathException {

        // analyze each component of the pattern

        if (parentPattern != null) {
            parentPattern = parentPattern.analyze(env, contextItemType);
        } else if (ancestorPattern != null) {
            ancestorPattern = ancestorPattern.analyze(env, contextItemType);
        }

        if (filters != null) {
	        for (int i=numberOfFilters-1; i>=0; i--) {
	            Expression filter = filters[i].analyze(env, contextItemType);
	            // System.err.println("Filter after analyze:");filter.display(10);
	            filters[i] = filter;
	            // if the last filter is constant true, remove it
	            if ((filter instanceof BooleanValue) && ((BooleanValue)filter).getBooleanValue()) {
	                if (i==numberOfFilters-1) {
	                    numberOfFilters--;
	                } // otherwise don't bother doing anything with it.
	            }
                ((ExpressionContext)env).getStyleElement().allocateSlots(filter);
	        }
	    }

        // see if it's an element pattern with a single positional predicate of [1]

        if (nodeTest.getPrimitiveType() == Type.ELEMENT && numberOfFilters==1) {
            if (((filters[0] instanceof IntegerValue) &&
                         ((IntegerValue)filters[0]).longValue()==1) ||
                ((filters[0] instanceof PositionRange) &&
                         ((PositionRange)filters[0]).getMinPosition()==1 &&
                         ((PositionRange)filters[0]).getMaxPosition()==1) ) {
                firstElementPattern = true;
                specialFilter = true;
                numberOfFilters = 0;
                filters = null;
            }
        }

        // see if it's an element pattern with a single positional predicate
        // of [position()=last()]

        if (nodeTest.getPrimitiveType() == Type.ELEMENT &&
                numberOfFilters==1 &&
                filters[0] instanceof IsLastExpression &&
                ((IsLastExpression)filters[0]).getCondition()) {
            lastElementPattern = true;
            specialFilter = true;
            numberOfFilters = 0;
            filters = null;
        }

        if (isPositional()) {
            equivalentExpr = makeEquivalentExpression(env);
            equivalentExpr = equivalentExpr.analyze(env, contextItemType);
            // the rewritten expression may use additional variables; must ensure there are enough slots for these
            // (see test match55)
            ((ExpressionContext)env).getStyleElement().allocateSlots(equivalentExpr);
            specialFilter = true;
        }

        return this;

        // TODO:PERF: identify subexpressions within a pattern predicate that could be promoted
        // In the case of match patterns in template rules, these would have to become global variables.
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     */

    public int getDependencies() {
        int dependencies = 0;
        if (parentPattern != null) {
            dependencies |= parentPattern.getDependencies();
        }
        if (ancestorPattern != null) {
            dependencies |= ancestorPattern.getDependencies();
        }
        for (int i=0; i<numberOfFilters; i++) {
            dependencies |= filters[i].getDependencies();
        }
        // the only dependency that's interesting is a dependency on local variables
        dependencies &= StaticProperty.DEPENDS_ON_LOCAL_VARIABLES;
        return dependencies;
    }

    /**
     * Iterate over the subexpressions within this pattern
     */

    public Iterator iterateSubExpressions() {
        Iterator iter;
        if (numberOfFilters == 0) {
            iter = Collections.EMPTY_LIST.iterator();
        } else {
            iter = Arrays.asList(filters).subList(0, numberOfFilters).iterator();
        }
        if (parentPattern != null) {
            Iterator[] pair = {iter, parentPattern.iterateSubExpressions()};
            iter = new MultiIterator(pair);
        }
        if (ancestorPattern != null) {
            Iterator[] pair = {iter, ancestorPattern.iterateSubExpressions()};
            iter = new MultiIterator(pair);
        }
        return iter;
    }

    /**
     * Offer promotion for subexpressions within this pattern. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     * <p/>
     * <p>Unlike the corresponding method on {@link net.sf.saxon.expr.Expression}, this method does not return anything:
     * it can make internal changes to the pattern, but cannot return a different pattern. Only certain
     * kinds of promotion are applicable within a pattern: specifically, promotions affecting local
     * variable references within the pattern.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer) throws XPathException {

        if (parentPattern != null) {
            parentPattern.promote(offer);
        }
        if (ancestorPattern != null) {
            ancestorPattern.promote(offer);
        }
        for (int i=0; i<numberOfFilters; i++) {
            filters[i] = filters[i].promote(offer);
        }
    }

    /**
    * For a positional pattern, make an equivalent nodeset expression to evaluate the filters
    */

    private ComputedExpression makeEquivalentExpression(StaticContext env) throws XPathException {
        byte axis = (nodeTest.getPrimitiveType()==Type.ATTRIBUTE ?
                        Axis.ATTRIBUTE :
                        Axis.CHILD );
        ComputedExpression step = new AxisExpression(axis, nodeTest);
        for (int n=0; n<numberOfFilters; n++) {
            step = new FilterExpression(step, filters[n], env);
        }
        return new PathExpression(new ParentNodeExpression(), step);
        // Note, the resulting expression is not required to deliver results in document order
    }

    /**
    * Determine whether the pattern matches a given node.
    * @param node the node to be tested
    * @return true if the pattern matches, else false
    */

    public boolean matches(NodeInfo node, XPathContext context) throws XPathException {
        return internalMatches(node, context);
        // At one time matches() and internalMatches() differed in the way they handled the current() function
        // appearing within a predicate. The pattern is now rewritten so current() is extracted to the top, so this
        // distinction no longer applies.
    }

    /**
    * Test whether the pattern matches, but without changing the current() node
    */

    protected boolean internalMatches(NodeInfo node, XPathContext context) throws XPathException {
        // System.err.println("Matching node type and fingerprint");
        if (!nodeTest.matches(node)) {
            return false;
        }
        if (parentPattern!=null) {
            //System.err.println("Testing parent pattern " + parentPattern + "(" + parentPattern.getClass() + ")");
            NodeInfo par = node.getParent();
            if (par==null) return false;
            if (!parentPattern.internalMatches(par, context)) {
                return false;
            }
        }

        if (ancestorPattern!=null) {
            NodeInfo anc = node.getParent();
            while (true) {
                if (anc==null) return false;
                if (ancestorPattern.internalMatches(anc, context)) break;
                anc = anc.getParent();
            }
        }

        if (specialFilter) {
            if (firstElementPattern) {
                SequenceIterator iter = node.iterateAxis(Axis.PRECEDING_SIBLING, nodeTest);
                return iter.next() == null;
            }

            if (lastElementPattern) {
                SequenceIterator iter = node.iterateAxis(Axis.FOLLOWING_SIBLING, nodeTest);
                return iter.next() == null;
            }

            if (equivalentExpr!=null) {

                // for a positional pattern, we do it the hard way: test whether the
                // node is a member of the nodeset obtained by evaluating the
                // equivalent expression

                // System.err.println("Testing positional pattern against node " + node.generateId());
                XPathContext c2 = context.newMinorContext();
                c2.setOriginatingConstructType(Location.PATTERN);
                c2.setCurrentIterator(SingletonIterator.makeIterator(node));
                try {
                    SequenceIterator nsv = equivalentExpr.iterate(c2);
                    while (true) {
                        NodeInfo n = (NodeInfo)nsv.next();
                        if (n == null) {
                            return false;
                        }
                        if (n.isSameNodeInfo(node)) {
                            return true;
                        }
                    }
                } catch (DynamicError e) {
                    DynamicError err = new DynamicError("An error occurred matching pattern {" + toString() + "}: ", e);
                    err.setXPathContext(c2);
                    err.setErrorCode(e.getErrorCodeLocalPart());
                    err.setLocator(this);
                    c2.getController().recoverableError(err);
                    return false;
                }
            }
        }

        if (filters!=null) {
            XPathContext c2 = context.newMinorContext();
            c2.setOriginatingConstructType(Location.PATTERN);
            c2.setCurrentIterator(SingletonIterator.makeIterator(node));
                // it's a non-positional filter, so we can handle each node separately

            for (int i=0; i<numberOfFilters; i++) {
                try {
                    if (!filters[i].effectiveBooleanValue(c2)) {
                        return false;
                    }
                } catch (DynamicError e) {
                    DynamicError err = new DynamicError("An error occurred matching pattern {" + toString() + "}: ", e);
                    err.setXPathContext(c2);
                    err.setErrorCode(e.getErrorCodeLocalPart());
                    err.setLocator(this);
                    c2.getController().recoverableError(err);
                    return false;
                }
            }
        }

        return true;
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * For patterns that match nodes of several types, return Node.NODE
    * @return the type of node matched by this pattern. e.g. Node.ELEMENT or Node.TEXT
    */

    public int getNodeKind() {
        return nodeTest.getPrimitiveType();
    }

    /**
    * Determine the fingerprint of nodes to which this pattern applies.
    * Used for optimisation.
    * @return the fingerprint of nodes matched by this pattern.
    */

    public int getFingerprint() {
        return nodeTest.getFingerprint();
    }

    /**
    * Get a NodeTest that all the nodes matching this pattern must satisfy
    */

    public NodeTest getNodeTest() {
        return nodeTest;
    }

    /**
    * Determine if the pattern uses positional filters
    * @return true if there is a numeric filter in the pattern, or one that uses the position()
    * or last() functions
    */

    private boolean isPositional() {
    	if (filters==null) return false;
        for (int i=0; i<numberOfFilters; i++) {
            int type = filters[i].getItemType().getPrimitiveType();
            if (type==Type.DOUBLE || type==Type.DECIMAL ||
                type==Type.INTEGER || type==Type.FLOAT || type==Type.ATOMIC) return true;
            if ((filters[i].getDependencies() &
            		 (StaticProperty.DEPENDS_ON_POSITION | StaticProperty.DEPENDS_ON_LAST)) != 0) return true;
        }
        return false;
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
