package org.orbeon.saxon.functions;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NameTest;
import org.orbeon.saxon.sort.GenericAtomicComparer;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ComplexType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;

import javax.xml.transform.TransformerException;
import java.util.ArrayList;
import java.util.List;

/**
* XSLT 2.0 deep-equal() function.
* Supports deep comparison of two sequences (of nodes and/or atomic values)
* optionally using a collation
*/

public class DeepEqual extends CollatingFunction {

    /**
     * Flag indicating that two elements should only be considered equal if they have the same
     * in-scope namespaces
     */
    public static final int INCLUDE_NAMESPACES = 1<<0;

    /**
     * Flag indicating that two element or attribute nodes are considered equal only if their
     * names use the same namespace prefix
     */
    public static final int INCLUDE_PREFIXES = 1<<1;

    /**
     * Flag indicating that comment children are taken into account when comparing element or document nodes
     */
    public static final int INCLUDE_COMMENTS = 1<<2;

    /**
     * Flag indicating that processing instruction nodes are taken into account when comparing element or document nodes
     */
    public static final int INCLUDE_PROCESSING_INSTRUCTIONS = 1<<3;

    /**
     * Flag indicating that whitespace text nodes are ignored when comparing element nodes
     */
    public static final int EXCLUDE_WHITESPACE_TEXT_NODES = 1<<4;

    /**
     * Flag indicating that elements and attributes should always be compared according to their string
     * value, not their typed value
     */
    public static final int COMPARE_STRING_VALUES = 1<<5;

    /**
     * Flag indicating that elements and attributes must have the same type annotation to be considered
     * deep-equal
     */
    public static final int COMPARE_ANNOTATIONS = 1<<6;

    /**
     * Flag indicating that a warning message explaining the reason why the sequences were deemed non-equal
     * should be sent to the ErrorListener
     */
    public static final int WARNING_IF_FALSE = 1<<7;

    /**
     * Flag indicating that adjacent text nodes in the top-level sequence are to be merged
     */

    public static final int JOIN_ADJACENT_TEXT_NODES = 1<<8;

    private transient Configuration config = null;

    /**
    * preEvaluate: if all arguments are known statically, evaluate early
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        config = visitor.getConfiguration();
        return super.preEvaluate(visitor);
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        GenericAtomicComparer collator = getAtomicComparer(2, context);

        SequenceIterator op1 = argument[0].iterate(context);
        SequenceIterator op2 = argument[1].iterate(context);

        Configuration config =
                (this.config!=null ? this.config : context.getConfiguration());
        return BooleanValue.get(deepEquals(op1, op2, collator, config, 0));
    }

    /**
     * Determine when two sequences are deep-equal
     * @param op1 the first sequence
     * @param op2 the second sequence
     * @param collator the collator to be used
     * @param config the configuration (gives access to the NamePool)
     * @param flags bit-significant integer giving comparison options. Always zero for standard
     * F+O deep-equals comparison.
     * @return true if the sequences are deep-equal
     */

    public static boolean deepEquals(SequenceIterator op1, SequenceIterator op2,
                                     GenericAtomicComparer collator, Configuration config, int flags) {
        boolean result = true;
        String reason = null;


        try {

            if ((flags & JOIN_ADJACENT_TEXT_NODES) != 0) {
                op1 = mergeAdjacentTextNodes(op1);
                op2 = mergeAdjacentTextNodes(op2);
            }
            while (true) {
                Item item1 = op1.next();
                Item item2 = op2.next();

                if (item1 == null && item2 == null) {
                    break;
                }

                if (item1 == null || item2 == null) {
                    result = false;
                    reason = "sequences have different lengths";
                    break;
                }

                if (item1 instanceof NodeInfo) {
                    if (item2 instanceof NodeInfo) {
                        if (!deepEquals((NodeInfo)item1, (NodeInfo)item2, collator, config, flags)) {
                            result = false;
                            reason = "nodes at position " + op1.position() + " differ";
                            break;
                        }
                    } else {
                        result = false;
                        reason = "comparing a node to an atomic value at position " + op1.position();
                        break;
                    }
                } else {
                    if (item2 instanceof NodeInfo) {
                        result = false;
                        reason = "comparing an atomic value to a node at position " + op1.position();
                        break;
                    } else {
                        AtomicValue av1 = ((AtomicValue)item1);
                        AtomicValue av2 = ((AtomicValue)item2);
                        if (av1.isNaN() && av2.isNaN()) {
                            // treat as equal, no action
                        } else if (!collator.comparesEqual(av1, av2)) {
                            result = false;
                            reason = "atomic values at position " + op1.position() + " differ";
                            break;
                        }
                    }
                }
            } // end while

        } catch (ClassCastException err) {
            // this will happen if the sequences contain non-comparable values
            // comparison errors are masked
            result = false;
            reason = "sequences contain non-comparable values";
        } catch (XPathException err) {
            // comparison errors are masked
            result = false;
            reason = "error occurred while comparing two values (" + err.getMessage() + ')';
        }

        if (!result) {
            explain(config, reason, flags);
//                config.getErrorListener().warning(
//                        new XPathException("deep-equal(): " + reason)
//                );
        }

        return result;
    }

    /**
    * Determine whether two nodes are deep-equal
    */

    private static boolean deepEquals(NodeInfo n1, NodeInfo n2,
                                      GenericAtomicComparer collator, Configuration config, int flags)
    throws XPathException {
        // shortcut: a node is always deep-equal to itself
        if (n1.isSameNodeInfo(n2)) return true;

        if (n1.getNodeKind() != n2.getNodeKind()) {
            explain(config, "node kinds differ: comparing " + Type.displayTypeName(n1) + " to " + Type.displayTypeName(n2), flags);
            return false;
        }

        final NamePool pool = config.getNamePool();
        switch (n1.getNodeKind()) {
            case Type.ELEMENT:
                if (n1.getFingerprint() != n2.getFingerprint()) {
                    explain(config, "element names differ: " + config.getNamePool().getClarkName(n1.getFingerprint()) +
                            " != " + config.getNamePool().getClarkName(n2.getFingerprint()), flags);
                    return false;
                }
                if (((flags & INCLUDE_PREFIXES) != 0) && (n1.getNameCode() != n2.getNameCode())) {
                    explain(config, "element prefixes differ: " + n1.getPrefix() +
                            " != " + n2.getPrefix(), flags);
                    return false;
                }
                AxisIterator a1 = n1.iterateAxis(Axis.ATTRIBUTE);
                AxisIterator a2 = n2.iterateAxis(Axis.ATTRIBUTE);
                if (Aggregate.count(a1.getAnother()) != Aggregate.count(a2)) {
                    explain(config, "elements have different number of attributes", flags);
                    return false;
                }
                while (true) {
                    NodeInfo att1 = (NodeInfo)a1.next();
                    if (att1 == null) break;

                    AxisIterator a2iter = n2.iterateAxis(Axis.ATTRIBUTE,
                                            new NameTest(Type.ATTRIBUTE, att1.getFingerprint(), pool));
                    NodeInfo att2 = (NodeInfo)a2iter.next();

                    if (att2==null) {
                        explain(config, "one element has an attribute " +
                                config.getNamePool().getClarkName(att1.getFingerprint()) +
                                ", the other does not", flags);
                        return false;
                    }
                    if (!deepEquals(att1, att2, collator, config, flags)) {
                        explain(config, "elements have different values for the attribute " +
                                config.getNamePool().getClarkName(att1.getFingerprint()), flags);
                        return false;
                    }
                }
                if ((flags & INCLUDE_NAMESPACES) != 0) {
                    IntHashSet ns1 = new IntHashSet(10);
                    IntHashSet ns2 = new IntHashSet(10);
                    AxisIterator it1 = n1.iterateAxis(Axis.NAMESPACE);
                    while (true) {
                        NodeInfo nn1 = (NodeInfo)it1.next();
                        if (nn1 == null) {
                            break;
                        }
                        int nscode1 = pool.getNamespaceCode(nn1.getLocalPart(), nn1.getStringValue());
                        ns1.add(nscode1);
                    }
                    AxisIterator it2 = n2.iterateAxis(Axis.NAMESPACE);
                    while (true) {
                        NodeInfo nn2 = (NodeInfo)it2.next();
                        if (nn2 == null) {
                            break;
                        }
                        int nscode2 = pool.getNamespaceCode(nn2.getLocalPart(), nn2.getStringValue());
                        ns2.add(nscode2);
                    }
                    if (!ns1.equals(ns2)) {
                        explain(config, "elements have different in-scope namespaces", flags);
                        return false;
                    }
                }

                if ((flags & COMPARE_ANNOTATIONS) != 0) {
                    if (n1.getTypeAnnotation() != n2.getTypeAnnotation()) {
                        explain(config, "elements have different type annotation", flags);
                        return false;
                    }
                }

                if ((flags & COMPARE_STRING_VALUES) == 0) {
                    int ann1 = n1.getTypeAnnotation();
                    int ann2 = n2.getTypeAnnotation();
                    if (ann1 == -1) {
                        // defensive programming
                        ann1 = StandardNames.XS_UNTYPED;
                    }
                    if (ann2 == -1) {
                        // defensive programming
                        ann2 = StandardNames.XS_UNTYPED;
                    }
                    final SchemaType type1 = config.getSchemaType(ann1);
                    final SchemaType type2 = config.getSchemaType(ann2);
                    final boolean isSimple1 = type1.isSimpleType() || ((ComplexType)type1).isSimpleContent();
                    final boolean isSimple2 = type2.isSimpleType() || ((ComplexType)type2).isSimpleContent();
                    if (isSimple1 != isSimple2) {
                        explain(config, "one element has a simple type, the other does not", flags);
                        return false;
                    }
                    if (isSimple1 && isSimple2) {
                        final SequenceIterator v1 = n1.getTypedValue();
                        final SequenceIterator v2 = n2.getTypedValue();
                        return deepEquals(v1, v2, collator, config, flags);
                    }
                }
                // fall through
            case Type.DOCUMENT:
                AxisIterator c1 = n1.iterateAxis(Axis.CHILD);
                AxisIterator c2 = n2.iterateAxis(Axis.CHILD);
                while (true) {
                    NodeInfo d1 = (NodeInfo)c1.next();
                    while (d1 != null && isIgnorable(d1, flags))  {
                        d1 = (NodeInfo)c1.next();
                    }
                    NodeInfo d2 = (NodeInfo)c2.next();
                    while (d2 != null && isIgnorable(d2, flags))  {
                        d2 = (NodeInfo)c2.next();
                    }
                    if (d1 == null || d2 == null) {
                        boolean r = (d1 == d2);
                        if (!r) {
                            explain(config, "nodes have different numbers of children", flags);
                        }
                        return r;
                    }
                    if (!deepEquals(d1, d2, collator, config, flags)) {
                        return false;
                    }
                }

            case Type.ATTRIBUTE:
                if (n1.getFingerprint() != n2.getFingerprint()) {
                    explain(config, "attribute names differ: " +
                            config.getNamePool().getClarkName(n1.getFingerprint()) +
                            " != " + config.getNamePool().getClarkName(n2.getFingerprint()), flags);
                    return false;
                }
                if (((flags & INCLUDE_PREFIXES) != 0) && (n1.getNameCode() != n2.getNameCode())) {
                    explain(config, "attribute prefixes differ: " + n1.getPrefix() +
                            " != " + n2.getPrefix(), flags);
                    return false;
                }
                if ((flags & COMPARE_ANNOTATIONS) != 0) {
                    if (n1.getTypeAnnotation() != n2.getTypeAnnotation()) {
                        explain(config, "attributes have different type annotations", flags);
                        return false;
                    }
                }
                boolean ar;
                if ((flags & COMPARE_STRING_VALUES) == 0) {
                    ar = deepEquals(n1.getTypedValue(), n2.getTypedValue(), collator, config, 0);
                } else {
                    ar = collator.comparesEqual(
                            new StringValue(n1.getStringValueCS()), 
                            new StringValue(n2.getStringValueCS()));
                }
                if (!ar) {
                    explain(config, "attribute values differ", flags);
                }
                return ar;


            case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
                if (n1.getFingerprint() != n2.getFingerprint()) {
                    explain(config, Type.displayTypeName(n1) + " names differ", flags);
                    return false;
                }
                // drop through
            case Type.TEXT:
            case Type.COMMENT:
                boolean vr = (collator.comparesEqual((AtomicValue)n1.atomize(), (AtomicValue)n2.atomize()));
                if (!vr) {
                    AtomicValue av1 = (AtomicValue)n1.atomize();
                    AtomicValue av2 = (AtomicValue)n2.atomize();
                    explain(config, Type.displayTypeName(n1) + " values differ (\"" +
                            Navigator.getPath(n1) + ", " + Navigator.getPath(n2) + ": " +
                            StringValue.diagnosticDisplay(av1.getStringValue()) + "\", \"" +
                            StringValue.diagnosticDisplay(av2.getStringValue()) + "\")", flags);
                }
                return vr;

            default:
                throw new IllegalArgumentException("Unknown node type");
        }
    }

    private static boolean isIgnorable(NodeInfo node, int flags) {
        final int kind = node.getNodeKind();
        if (kind == Type.COMMENT) {
            return (flags & INCLUDE_COMMENTS)==0;
        } else if (kind == Type.PROCESSING_INSTRUCTION) {
            return (flags & INCLUDE_PROCESSING_INSTRUCTIONS)==0;
        } else if (kind == Type.TEXT) {
            return ((flags & EXCLUDE_WHITESPACE_TEXT_NODES)!=0) &&
                    Whitespace.isWhite(node.getStringValueCS());
        }
        return false;
    }

    private static void explain(Configuration config, String message, int flags) {
        try {
            if ((flags & WARNING_IF_FALSE) != 0) {
                config.getErrorListener().warning(new XPathException("deep-equal(): " + message));
            }
        } catch (TransformerException e) {
            //
        }
    }

    private static SequenceIterator mergeAdjacentTextNodes(SequenceIterator in) throws XPathException {
        Configuration config = null;
        List items = new ArrayList(20);
        boolean prevIsText = false;
        FastStringBuffer textBuffer = new FastStringBuffer(100);
        while (true) {
            Item next = in.next();
            if (next == null) {
                break;
            }
            if (next instanceof NodeInfo && ((NodeInfo)next).getNodeKind() == Type.TEXT) {
                textBuffer.append(next.getStringValueCS());
                prevIsText = true;
                config = ((NodeInfo)next).getConfiguration();
            } else {
                if (prevIsText) {
                    Orphan textNode = new Orphan(config);
                    textNode.setNodeKind(Type.TEXT);
                    textNode.setStringValue(textBuffer.toString()); // must copy the buffer before reusing it
                    items.add(textNode);
                    textBuffer.setLength(0);
                }
                prevIsText = false;
                items.add(next);
            }
        }
        if (prevIsText) {
            Orphan textNode = new Orphan(config);
            textNode.setNodeKind(Type.TEXT);
            textNode.setStringValue(textBuffer.toString()); // must copy the buffer before reusing it
            items.add(textNode);
        }
        SequenceExtent extent = new SequenceExtent(items);
        return extent.iterate();
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
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
