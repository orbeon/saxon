package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.*;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.EmptySequence;

import java.util.Iterator;


/**
 * An AxisExpression is always obtained by simplifying a PathExpression.
 * It represents a PathExpression that starts at the context node, and uses
 * a simple node-test with no filters. For example "*", "title", "./item",
 * "@*", or "ancestor::chapter*".
 *
 * <p>An AxisExpression delivers nodes in axis order (not in document order).
 * To get nodes in document order, in the case of a reverse axis, the expression
 * should be wrapped in a call on reverse().</p>
*/

public final class AxisExpression extends Expression {

    private byte axis;
    private NodeTest test;
    private ItemType itemType = null;
    private ItemType contextItemType = null;
    int computedCardinality = -1;
    private boolean doneWarnings = false;

    /**
     * Constructor
     * @param axis       The axis to be used in this AxisExpression: relevant constants are defined
     *                   in class org.orbeon.saxon.om.Axis.
     * @param nodeTest   The conditions to be satisfied by selected nodes. May be null,
     *                   indicating that any node on the axis is acceptable
     * @see org.orbeon.saxon.om.Axis
     */

    public AxisExpression(byte axis, NodeTest nodeTest) {
        this.axis = axis;
        test = nodeTest;
    }

    /**
     * Simplify an expression
     * @return the simplified expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) {

        if (axis == Axis.PARENT && (test==null || test instanceof AnyNodeTest)) {
            ParentNodeExpression p = new ParentNodeExpression();
            ExpressionTool.copyLocationInfo(this, p);
            return p;
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        Configuration config = visitor.getConfiguration();
        NamePool namePool = config.getNamePool();
        StaticContext env = visitor.getStaticContext();
        if (contextItemType == null) {
            XPathException err = new XPathException("Axis step " + toString(namePool) +
                    " cannot be used here: the context item is undefined");
            err.setIsTypeError(true);
            err.setErrorCode("XPDY0002");
            err.setLocator(this);
            throw err;
        }
        if (contextItemType.isAtomicType()) {
            XPathException err = new XPathException("Axis step " + toString(namePool) +
                    " cannot be used here: the context item is an atomic value");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0020");
            err.setLocator(this);
            throw err;
        }

        if (this.contextItemType == contextItemType && doneWarnings) {
            return this;
        }

        this.contextItemType = contextItemType;
        doneWarnings = true;

        if (contextItemType instanceof NodeTest) {
            int origin = contextItemType.getPrimitiveType();
            if (origin != Type.NODE) {
                if (Axis.isAlwaysEmpty(axis, origin)) {
                    env.issueWarning("The " + Axis.axisName[axis] + " axis starting at " +
                            (origin==Type.ELEMENT || origin == Type.ATTRIBUTE ? "an " : "a ") +
                            NodeKindTest.nodeKindName(origin) + " node will never select anything",
                            this);
                    return Literal.makeEmptySequence();
                }
            }

            if (test != null) {
                int kind = test.getPrimitiveType();
                if (kind != Type.NODE) {
                    if (!Axis.containsNodeKind(axis, kind)) {
                        env.issueWarning("The " + Axis.axisName[axis] + " axis will never select any " +
                            NodeKindTest.nodeKindName(kind) + " nodes",
                            this);
                        return Literal.makeEmptySequence();
                    }
                }
                if (axis==Axis.SELF && kind!=Type.NODE && origin!=Type.NODE && kind!=origin) {
                    env.issueWarning("The self axis will never select any " +
                            NodeKindTest.nodeKindName(kind) +
                            " nodes when starting at " +
                            (origin==Type.ELEMENT || origin == Type.ATTRIBUTE ? "an " : "a ")  +
                            NodeKindTest.nodeKindName(origin) + " node", this);
                    return Literal.makeEmptySequence();
                }
                if (axis==Axis.SELF) {
                    itemType = new CombinedNodeTest((NodeTest)contextItemType, Token.INTERSECT, test);
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

                            return Literal.makeEmptySequence();
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
                    if ((axis == Axis.CHILD || axis==Axis.DESCENDANT || axis==Axis.DESCENDANT_OR_SELF) &&
                        (kind==Type.ELEMENT || kind==Type.ATTRIBUTE || kind==Type.DOCUMENT)) {
                        env.issueWarning("The " + Axis.axisName[axis] + " axis will never select any " +
                                NodeKindTest.nodeKindName(kind) +
                                " nodes when starting at a node with simple type " +
                                contentType.getDescription(), this);
                    } else if (axis == Axis.CHILD && kind == Type.TEXT &&
                            (visitor.getParentExpression() instanceof Atomizer)) {
                        env.issueWarning("Selecting the text nodes of an element with simple content may give the " +
                                "wrong answer in the presence of comments or processing instructions. It is usually " +
                                "better to omit the '/text()' step", this);
                    } else if (axis == Axis.ATTRIBUTE) {
                        Iterator extensions = config.getExtensionsOfType(contentType);
                        boolean found = false;
                        if (targetfp == -1) {
                            while (extensions.hasNext()) {
                                ComplexType extension = (ComplexType)extensions.next();
                                if (extension.allowsAttributes()) {
                                    found = true;
                                    break;
                                }
                            }
                        } else {
                            while (extensions.hasNext()) {
                                ComplexType extension = (ComplexType)extensions.next();
                                try {
                                    if (extension.getAttributeUseType(targetfp) != null) {
                                        found = true;
                                        break;
                                    }
                                } catch (SchemaException e) {
                                    // ignore the error
                                }
                            }
                        }
                        if (!found) {
                            env.issueWarning("The " + Axis.axisName[axis] + " axis will never select " +
                                    (targetfp == -1 ?
                                            "any attribute nodes" :
                                            "an attribute node named " + env.getNamePool().getDisplayName(targetfp)) +
                                    " when starting at a node with simple type " +
                                    contentType.getDescription(), this);
                            // Despite the warning, leave the expression unchanged. This is because
                            // we don't necessarily know about all extended types at compile time:
                            // in particular, we don't seal the XML Schema namespace to block extensions
                            // of built-in types
                        }
                    }
                } else if (((ComplexType)contentType).isSimpleContent() &&
                        (axis==Axis.CHILD || axis==Axis.DESCENDANT || axis==Axis.DESCENDANT_OR_SELF) &&
                        (kind==Type.ELEMENT || kind==Type.DOCUMENT)) {
                    // We don't need to consider extended types here, because a type with complex content
                    // can never be defined as an extension of a type with simple content
                    env.issueWarning("The " + Axis.axisName[axis] + " axis will never select any " +
                            NodeKindTest.nodeKindName(kind) +
                            " nodes when starting at a node with type " +
                            contentType.getDescription() +
                            ", as this type requires simple content", this);
                    return new Literal(EmptySequence.getInstance());
                } else if (((ComplexType)contentType).isEmptyContent() &&
                        (axis==Axis.CHILD || axis==Axis.DESCENDANT || axis==Axis.DESCENDANT_OR_SELF)) {
                    for (Iterator iter=config.getExtensionsOfType(contentType); iter.hasNext();) {
                        ComplexType extension = (ComplexType)iter.next();
                        if (!extension.isEmptyContent()) {
                            return this;
                        }
                    }
                    env.issueWarning("The " + Axis.axisName[axis] + " axis will never select any" +
                            " nodes when starting at a node with type " +
                            contentType.getDescription() +
                            ", as this type requires empty content", this);
                    return new Literal(EmptySequence.getInstance());
                } else if (axis==Axis.ATTRIBUTE && targetfp != -1) {
                    try {
                        SchemaType schemaType = ((ComplexType)contentType).getAttributeUseType(targetfp);
                        if (schemaType == null) {
                            String n = env.getNamePool().getDisplayName(targetfp);

                            env.issueWarning("The complex type " + contentType.getDescription() +
                                " does not allow an attribute named " + n, this);
                            return new Literal(EmptySequence.getInstance());
                        } else {
                            itemType = new CombinedNodeTest(
                                    test,
                                    Token.INTERSECT,
                                    new ContentTypeTest(Type.ATTRIBUTE, schemaType, env.getConfiguration()));
                        }
                    } catch (SchemaException e) {
                        // ignore the exception
                    }
                } else if (axis==Axis.CHILD && kind==Type.ELEMENT) {
                    try {
                        int childElement = targetfp;
                        if (targetfp == -1) {
                            // select="child::*"
                            IntHashSet children = new IntHashSet();
                            ((ComplexType)contentType).gatherAllPermittedChildren(children);
                            if (children.isEmpty()) {
                                env.issueWarning("The complex type " + contentType.getDescription() +
                                " does not allow children", this);
                                return new Literal(EmptySequence.getInstance());
                            }
                            if (children.contains(-1)) {
                                return this;
                            }
                            if (children.size() == 1) {
                                IntIterator iter = children.iterator();
                                if (iter.hasNext()) {
                                    childElement = iter.next();
                                }
                            } else {
                                return this;
                            }
                        }
                        SchemaType schemaType = ((ComplexType)contentType).getElementParticleType(childElement, true);
                        if (schemaType == null) {
                            String n = env.getNamePool().getDisplayName(childElement);
                            env.issueWarning("The complex type " + contentType.getDescription() +
                                " does not allow a child element named " + n, this);
                            return new Literal(EmptySequence.getInstance());
                        } else {
                            itemType = new CombinedNodeTest(
                                    test,
                                    Token.INTERSECT,
                                    new ContentTypeTest(Type.ELEMENT, schemaType, env.getConfiguration()));
                            computedCardinality = ((ComplexType)contentType).getElementParticleCardinality(childElement, true);
                            visitor.resetStaticProperties();
                            if (computedCardinality == StaticProperty.ALLOWS_ZERO) {
                                // this shouldn't happen, because we've already checked for this a different way.
                                // but it's worth being safe (there was a bug involving an incorrect inference here)
                                String n = env.getNamePool().getDisplayName(childElement);
                                env.issueWarning("The complex type " + contentType.getDescription() +
                                    " appears not to allow a child element named " + n, this);
                                return new Literal(EmptySequence.getInstance());
                            }
                            if (!Cardinality.allowsMany(computedCardinality)) {
                                // if there can be at most one child of this name, create a FirstItemExpression
                                // to stop the search after the first one is found
                                return new FirstItemExpression(this);
                            }
                        }
                    } catch (SchemaException e) {
                        // ignore the exception
                    }
                } else if (axis==Axis.DESCENDANT && kind==Type.ELEMENT && targetfp != -1) {
                    // when searching for a specific element on the descendant axis, try to produce a more
                    // specific path that avoids searching branches of the tree where the element cannot occur
                    try {
                        IntHashSet descendants = new IntHashSet();
                        ((ComplexType)contentType).gatherAllPermittedDescendants(descendants);
                        if (descendants.contains(-1)) {
                            return this;
                        }
                        if (descendants.contains(targetfp)) {
                            IntHashSet children = new IntHashSet();
                            ((ComplexType)contentType).gatherAllPermittedChildren(children);
                            IntHashSet usefulChildren = new IntHashSet();
                            boolean considerSelf = false;
                            boolean considerDescendants = false;
                            for (IntIterator child = children.iterator(); child.hasNext();) {
                                int c = child.next();
                                if (c == targetfp) {
                                    usefulChildren.add(c);
                                    considerSelf = true;
                                }
                                SchemaType st = ((ComplexType)contentType).getElementParticleType(c, true);
                                if (st == null) {
                                    throw new AssertionError("Can't find type for element " + c);
                                }
                                if (st instanceof ComplexType) {
                                    IntHashSet subDescendants = new IntHashSet();
                                    ((ComplexType)st).gatherAllPermittedDescendants(subDescendants);
                                    if (subDescendants.contains(targetfp)) {
                                        usefulChildren.add(c);
                                        considerDescendants = true;
                                    }
                                }
                            }
                            if (usefulChildren.size() < children.size()) {
                                NodeTest childTest = makeUnionNodeTest(usefulChildren, visitor.getConfiguration().getNamePool());
                                AxisExpression first = new AxisExpression(Axis.CHILD, childTest);
                                ExpressionTool.copyLocationInfo(this, first);
                                byte nextAxis;
                                if (considerSelf) {
                                    nextAxis = (considerDescendants ? Axis.DESCENDANT_OR_SELF : Axis.SELF);
                                } else {
                                    nextAxis = Axis.DESCENDANT;
                                }
                                AxisExpression next = new AxisExpression(nextAxis, test);
                                ExpressionTool.copyLocationInfo(this, next);
                                PathExpression path = new PathExpression(first, next);
                                ExpressionTool.copyLocationInfo(this, path);
                                return path.typeCheck(visitor, contextItemType);
                            }
                        } else {
                            String n = env.getNamePool().getDisplayName(targetfp);
                            env.issueWarning("The complex type " + contentType.getDescription() +
                                " does not allow a descendant element named " + n, this);
                        }
                    } catch (SchemaException e) {
                        throw new AssertionError(e);
                    }


                }
            }
        }

        return this;
    }

    /**
     * Make a union node test for a set of supplied element fingerprints
     * @param elements the set of integer element fingerprints to be tested for
     * @param pool the name pool
     * @return a NodeTest that returns true if the node is an element whose name is one of the names
     * in this set
     */

    private NodeTest makeUnionNodeTest(IntHashSet elements, NamePool pool) {
        NodeTest test = null;
        for (IntIterator iter = elements.iterator(); iter.hasNext();) {
            int fp = iter.next();
            NodeTest nextTest = new NameTest(Type.ELEMENT, fp, pool);
            if (test == null) {
                test = nextTest;
            } else {
                test = new CombinedNodeTest(test, Token.UNION, nextTest);
            }
        }
        if (test == null) {
            return EmptySequenceTest.getInstance();
        } else {
            return test;
        }
    }

    /**
     * Get the static type of the context item for this AxisExpression. May be null if not known.
     * @return the statically-inferred type, or null if not known
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
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) {
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
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        AxisExpression a2 = new AxisExpression(axis, test);
        a2.itemType = itemType;
        a2.contextItemType = contextItemType;
        a2.computedCardinality = computedCardinality;
        return a2;
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
     * @param th the type hierarchy cache
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
     * @return the axis number, for example {@link Axis#CHILD}
    */

    public byte getAxis() {
        return axis;
    }

    /**
     * Get the NodeTest. Returns null if the AxisExpression can return any node.
     * @return the node test, or null if all nodes are returned
    */

    public NodeTest getNodeTest() {
        return test;
    }


    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        if (pathMapNodeSet == null) {
            ContextItemExpression cie = new ContextItemExpression();
            cie.setContainer(getContainer());
            pathMapNodeSet = new PathMap.PathMapNodeSet(pathMap.makeNewRoot(cie));
        }
        PathMap.PathMapNodeSet target = pathMapNodeSet.createArc(this);
//        if (isStringValueUsed()) {
//            target.setAtomized();
//        }
        return target;
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
            NamePool pool;
            try {
                pool = context.getConfiguration().getNamePool();
            } catch (Exception err) {
                pool = null;
            }
            XPathException err = new XPathException("The context item for axis step " +
                    (pool == null ? toString() : toString(pool)) + " is undefined");
            err.setErrorCode("XPDY0002");
            err.setXPathContext(context);
            err.setLocator(this);
            err.setIsTypeError(true);
            throw err;
        } catch (ClassCastException cce) {
            NamePool pool;
            try {
                pool = context.getConfiguration().getNamePool();
            } catch (Exception err) {
                pool = null;
            }
            XPathException err = new XPathException("The context item for axis step " +
                    (pool == null ? toString() : toString(pool)) + " is not a node");
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
    * Iterate the axis from a given starting node, without regard to context
    * @param origin the starting node
    * @return the iterator over the axis
    */

    public SequenceIterator iterate(Item origin) throws XPathException {
        try {
            if (test==null) {
                return ((NodeInfo)origin).iterateAxis(axis);
            } else {
                return ((NodeInfo)origin).iterateAxis(axis, test);
            }
        } catch (ClassCastException cce) {
            XPathException err = new XPathException("The context item for axis step " +
                    toString() + " is not a node");
            err.setErrorCode("XPTY0020");
            err.setLocator(this);
            err.setIsTypeError(true);
            throw err;
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("axis");
        destination.emitAttribute("name", Axis.axisName[axis]);
        destination.emitAttribute("nodeTest", (test==null ? "node()" : test.toString()));
        destination.endElement();
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
     * @param pool the name pool, used for expanding names in the node test
     * @return a string representation of the expression
     */

    public String toString(NamePool pool) {
        return Axis.axisName[axis] +
                "::" +
                (test==null ? "node()" : test.toString(pool));
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
