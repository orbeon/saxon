package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.*;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.sort.IntHashSet;

import java.io.PrintStream;


/**
 * An AxisExpression is always obtained by simplifying a PathExpression.
 * It represents a PathExpression that starts at the context node, and uses
 * a simple node-test with no filters. For example "*", "title", "./item",
 * "@*", or "ancestor::chapter*".
 *
 * <p>An AxisExpression delivers nodes in axis order (not in document order).
 * To get nodes in document order, in the case of a reverse axis, the expression
 * should be wrapped in a Reverser.</p>
*/

public final class AxisExpression extends ComputedExpression {

    private byte axis;
    private NodeTest test;
    private ItemType itemType = null;
    private ItemType contextItemType = null;
    int computedCardinality = -1;

    /**
    * Constructor
    * @param axis       The axis to be used in this AxisExpression: relevant constants are defined
     *                  in class org.orbeon.saxon.om.Axis.
    * @param nodeTest   The conditions to be satisfied by selected nodes. May be null,
     *                  indicating that any node on the axis is acceptable
     * @see org.orbeon.saxon.om.Axis
    */

    public AxisExpression(byte axis, NodeTest nodeTest) {
        this.axis = axis;
        this.test = nodeTest;
    }

    /**
    * Simplify an expression
    * @return the simplified expression
    */

    public Expression simplify(StaticContext env) {

        if (axis == Axis.PARENT && (test==null || test instanceof AnyNodeTest)) {
            ParentNodeExpression p = new ParentNodeExpression();
            p.setParentExpression(getParentExpression());
            ExpressionTool.copyLocationInfo(this, p);
            return p;
        }

        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        this.contextItemType = contextItemType;
        if (contextItemType == null) {
            StaticError err = new StaticError("Axis step " + toString(env.getNamePool()) +
                    " cannot be used here: the context item is undefined");
            err.setIsTypeError(true);
            err.setErrorCode("XPDY0002");
            err.setLocator(this);
            throw err;
        }
        if (contextItemType.isAtomicType()) {
            StaticError err = new StaticError("Axis step " + toString(env.getNamePool()) +
                    " cannot be used here: the context item is an atomic value");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0020");
            err.setLocator(this);
            throw err;
        }

        if (contextItemType instanceof NodeTest) {
            int origin = contextItemType.getPrimitiveType();
            if (origin != Type.NODE) {
                if (Axis.isAlwaysEmpty(axis, origin)) {
                    env.issueWarning("The " + Axis.axisName[axis] + " axis starting at " +
                            (origin==Type.ELEMENT || origin == Type.ATTRIBUTE ? "an " : "a ") +
                            NodeKindTest.toString(origin) + " node will never select anything",
                            this);
                    return EmptySequence.getInstance();
                }
            }

            if (test != null) {
                int kind = test.getPrimitiveType();
                if (kind != Type.NODE) {
                    if (!Axis.containsNodeKind(axis, kind)) {
                        env.issueWarning("The " + Axis.axisName[axis] + " axis will never select any " +
                            NodeKindTest.toString(kind) + " nodes",
                            this);
                        return EmptySequence.getInstance();
                    }
                }
                if (axis==Axis.SELF && kind!=Type.NODE && origin!=Type.NODE && kind!=origin) {
                    env.issueWarning("The self axis will never select any " +
                            NodeKindTest.toString(kind) +
                            " nodes when starting at " +
                            (origin==Type.ELEMENT || origin == Type.ATTRIBUTE ? "an " : "a ")  +
                            NodeKindTest.toString(origin) + " node", this);
                    return EmptySequence.getInstance();
                }
                if (axis==Axis.SELF) {
                    itemType = contextItemType;
                }

                // If the content type of the context item is known, see whether the node test can select anything

                if (contextItemType instanceof DocumentNodeTest && axis == Axis.CHILD && kind == Type.ELEMENT) {
                    NodeTest elementTest = ((DocumentNodeTest)contextItemType).getElementTest();
                    IntHashSet requiredNames = elementTest.getRequiredNodeNames();
                    if (requiredNames != null) {
                        // check that the name appearing in the step is one of the names allowed by the nodetest
                        IntHashSet selected = test.getRequiredNodeNames();
                        if (selected != null && selected.intersect(requiredNames).isEmpty()) {
                            env.issueWarning("Starting at a document node, the step is selecting an element whose name " +
                                    "is not among the names of child elements permitted for this document node type", this);

                            return EmptySequence.getInstance();
                        }
                    }
                    itemType = elementTest;
                    return this;
                }

                SchemaType contentType = ((NodeTest)contextItemType).getContentType();
                if (contentType == AnyType.getInstance()) {
                    // fast exit in non-schema-aware case
                    return this;
                }

                int targetfp = test.getFingerprint();

                if (contentType.isSimpleType()) {
                    if ((axis == Axis.CHILD || axis==Axis.ATTRIBUTE || axis==Axis.DESCENDANT || axis==Axis.DESCENDANT_OR_SELF) &&
                        (kind==Type.ELEMENT || kind==Type.ATTRIBUTE || kind==Type.DOCUMENT)) {
                        env.issueWarning("The " + Axis.axisName[axis] + " axis will never select any " +
                                NodeKindTest.toString(kind) +
                                " nodes when starting at a node with simple type " +
                                contentType.getDescription(), this);
                    }
                    else if (axis == Axis.CHILD && kind == Type.TEXT && (getParentExpression() instanceof Atomizer)) {
                        env.issueWarning("Selecting the text nodes of an element with simple content may give the " +
                                "wrong answer in the presence of comments or processing instructions. It is usually " +
                                "better to omit the '/text()' step", this);
                    }
                } else if (((ComplexType)contentType).isSimpleContent() &&
                        (axis==Axis.CHILD || axis==Axis.DESCENDANT || axis==Axis.DESCENDANT_OR_SELF) &&
                        (kind==Type.ELEMENT || kind==Type.DOCUMENT)) {
                    env.issueWarning("The " + Axis.axisName[axis] + " axis will never select any " +
                            NodeKindTest.toString(kind) +
                            " nodes when starting at a node with type " +
                            contentType.getDescription() +
                            ", as this type requires simple content", this);
                } else if (((ComplexType)contentType).isEmptyContent() &&
                        (axis==Axis.CHILD || axis==Axis.DESCENDANT || axis==Axis.DESCENDANT_OR_SELF)) {
                    env.issueWarning("The " + Axis.axisName[axis] + " axis will never select any " +
                            " nodes when starting at a node with type " +
                            contentType.getDescription() +
                            ", as this type requires empty content", this);
                } else if (axis==Axis.ATTRIBUTE && targetfp != -1) {
                    try {
                        SchemaType schemaType = ((ComplexType)contentType).getAttributeUseType(targetfp);
                        if (schemaType == null) {
                            String n = env.getNamePool().getDisplayName(targetfp);

                            env.issueWarning("The complex type " + contentType.getDescription() +
                                " does not allow an attribute named " + n, this);
                        } else {
                            itemType = new CombinedNodeTest(
                                    test,
                                    Token.INTERSECT,
                                    new ContentTypeTest(Type.ATTRIBUTE, schemaType, env.getConfiguration()));
                        }
                    } catch (SchemaException e) {
                        // ignore the exception
                    }
                } else if (axis==Axis.CHILD && kind==Type.ELEMENT && targetfp != -1) {
                    try {
                        SchemaType schemaType = ((ComplexType)contentType).getElementParticleType(targetfp);
                        if (schemaType == null) {
                            String n = env.getNamePool().getDisplayName(targetfp);
                            env.issueWarning("The complex type " + contentType.getDescription() +
                                " does not allow a child element named " + n, this);
                        } else {
                            itemType = new CombinedNodeTest(
                                    test,
                                    Token.INTERSECT,
                                    new ContentTypeTest(Type.ELEMENT, schemaType, env.getConfiguration()));
                            computedCardinality = ((ComplexType)contentType).getElementParticleCardinality(targetfp);
                            resetStaticProperties();
                            if (!Cardinality.allowsMany(computedCardinality)) {
                                // if there can be at most one child of this name, create a FirstItemExpression
                                // to stop the search after the first one is found
                                return new FirstItemExpression(this);
                            }
                        }
                    } catch (SchemaException e) {
                        // ignore the exception
                    }
                }
            }
        }

        return this;
    }

    /**
     * Get the static type of the context item for this AxisExpression. May be null if not known.
     */

    public ItemType getContextItemType() {
        return contextItemType;
    }


    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) {
        return this;
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        if (!(other instanceof AxisExpression)) {
            return false;
        }
        if (axis != ((AxisExpression)other).axis) {
            return false;
        }
        if (test==null) {
            return ((AxisExpression)other).test==null;
        }
        return test.toString().equals(((AxisExpression)other).test.toString());
    }

    /**
    * get HashCode for comparing two expressions
    */

    public int hashCode() {
        // generate an arbitrary hash code that depends on the axis and the node test
        int h = 9375162 + axis<<20;
        if (test != null) {
            h ^= test.getPrimitiveType()<<16;
            h ^= test.getFingerprint();
        }
        return h;
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
    * XPathContext.CURRENT_NODE
    */

    public int getIntrinsicDependencies() {
	    return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return StaticProperty.CONTEXT_DOCUMENT_NODESET |
               StaticProperty.SINGLE_DOCUMENT_NODESET |
               StaticProperty.NON_CREATIVE |
               (Axis.isForwards[axis] ? StaticProperty.ORDERED_NODESET  : StaticProperty.REVERSE_DOCUMENT_ORDER) |
               (Axis.isPeerAxis[axis] ? StaticProperty.PEER_NODESET : 0) |
               (Axis.isSubtreeAxis[axis] ? StaticProperty.SUBTREE_NODESET : 0) |
               ((axis==Axis.ATTRIBUTE || axis==Axis.NAMESPACE) ? StaticProperty.ATTRIBUTE_NS_NODESET : 0);
    }

    /**
    * Determine the data type of the items returned by this expression
    * @return Type.NODE or a subtype, based on the NodeTest in the axis step, plus
     * information about the content type if this is known from schema analysis
     * @param th
     */

    public final ItemType getItemType(TypeHierarchy th) {
        if (itemType != null) {
            return itemType;
        }
        int p = Axis.principalNodeType[axis];
        switch (p) {
        case Type.ATTRIBUTE:
        case Type.NAMESPACE:
            return NodeKindTest.makeNodeKindTest(p);
        default:
            if (test==null) {
                return AnyNodeTest.getInstance();
            } else {
                return test;
                //return NodeKindTest.makeNodeKindTest(test.getPrimitiveType());
            }
        }
    }

    /**
    * Specify that the expression returns a singleton
    */

    public final int computeCardinality() {
        if (computedCardinality != -1) {
            return computedCardinality;
        }
        if (axis == Axis.ATTRIBUTE && test instanceof NameTest) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else if (axis == Axis.SELF) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
        // the parent axis isn't handled by this class
    }

    /**
    * Get the axis
    */

    public byte getAxis() {
        return axis;
    }

    /**
    * Get the NodeTest. Returns null if the AxisExpression can return any node.
    */

    public NodeTest getNodeTest() {
        return test;
    }

    /**
    * Evaluate the path-expression in a given context to return a NodeSet
    * @param context the evaluation context
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        try {
            if (test==null) {
                return ((NodeInfo)item).iterateAxis(axis);
            } else {
                return ((NodeInfo)item).iterateAxis(axis, test);
            }
        } catch (NullPointerException npe) {
            NamePool pool = null;
            try {
                pool = context.getConfiguration().getNamePool();
            } catch (Exception err) {}
            DynamicError err = new DynamicError("The context item for axis step " +
                    (pool==null ? toString() : toString(pool)) + " is undefined");
            err.setErrorCode("XPDY0002");
            err.setXPathContext(context);
            err.setLocator(this);
            err.setIsTypeError(true);
            throw err;
        } catch (ClassCastException cce) {
            NamePool pool = null;
            try {
                pool = context.getConfiguration().getNamePool();
            } catch (Exception err) {}
            DynamicError err = new DynamicError("The context item for axis step " +
                    (pool==null ? toString() : toString(pool)) + " is not a node");
            err.setErrorCode("XPTY0020");
            err.setXPathContext(context);
            err.setLocator(this);
            err.setIsTypeError(true);
            throw err;
        } catch (UnsupportedOperationException err) {
            // the namespace axis is not supported for all tree implementations
            dynamicError(err.getMessage(), "XPST0010", context);
            return null;
        }
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + toString(config.getNamePool()));
    }

    /**
     * Represent the expression as a string for diagnostics
     */

    public String toString() {
        return Axis.axisName[axis] +
                "::" +
                (test==null ? "node()" : test.toString());
    }

    /**
     * Represent the expression as a string for diagnostics
     */

    public String toString(NamePool pool) {
        return Axis.axisName[axis] +
                "::" +
                (test==null ? "node()" : test.toString(pool));
    }

    public PathMap.PathMapNode addToPathMap(PathMap pathMap, PathMap.PathMapNode pathMapNode) {
        if (pathMapNode == null) {
            ContextItemExpression cie = new ContextItemExpression();
            cie.setParentExpression(getParentExpression());
            pathMapNode = pathMap.makeNewRoot(cie);
        }
        PathMap.PathMapNode target = pathMapNode.createArc(this);
        if (isStringValueUsed() &&
                ((test.getNodeKindMask() & (1<<Type.ELEMENT | 1<<Type.DOCUMENT)) != 0)) {
            AxisExpression textAxis = new AxisExpression(Axis.DESCENDANT, NodeKindTest.TEXT);
            textAxis.setParentExpression(getParentExpression());
            target.createArc(textAxis);
        }
        return target;
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
