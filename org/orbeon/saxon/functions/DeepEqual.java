package net.sf.saxon.functions;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.sort.AtomicComparer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.BooleanValue;

/**
* XSLT 2.0 deep-equal() function.
* Supports deep comparison of two sequences (of nodes and/or atomic values)
* optionally using a collation
*/

public class DeepEqual extends CollatingFunction {

    private transient Configuration config = null;

    /**
    * preEvaluate: if all arguments are known statically, evaluate early
    */

    public Expression preEvaluate(StaticContext env) throws XPathException {
        config = env.getConfiguration();
        return super.preEvaluate(env);
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicComparer collator = getAtomicComparer(2, context);

        SequenceIterator op1 = argument[0].iterate(context);
        SequenceIterator op2 = argument[1].iterate(context);

        Configuration config =
                (this.config!=null ? this.config : context.getController().getConfiguration());
        return BooleanValue.get(deepEquals(op1, op2, collator, config));
    }

    /**
     * Determine when two sequences are deep-equal
     * @param op1 the first sequence
     * @param op2 the second sequence
     * @param collator the collator to be used
     * @param config
     * @return true if the sequences are deep-equal
     */

    public static boolean deepEquals(SequenceIterator op1, SequenceIterator op2,
                                     AtomicComparer collator, Configuration config) {
        boolean result = true;
        try {
            while (true) {
                Item item1 = op1.next();
                Item item2 = op2.next();

                if (item1 == null && item2 == null) {
                    break;
                }

                if (item1 == null || item2 == null) {
                    result = false;
                    break;
                }

                if (item1 instanceof NodeInfo) {
                    if (item2 instanceof NodeInfo) {
                        if (!deepEquals((NodeInfo)item1, (NodeInfo)item2, collator, config)) {
                            result = false;
                            break;
                        }
                    } else {
                        result = false;
                        break;
                    }
                } else {
                    if (item2 instanceof NodeInfo) {
                        result = false;
                        break;
                    } else {
                        if (!collator.comparesEqual(item1, item2)) {
                            result = false;
                            break;
                        }
                    }
                }
            } // end while

        } catch (ClassCastException err) {
            // this will happen if the sequences contain non-comparable values
            // comparison errors are masked
            result = false;
        } catch (XPathException err) {
            // comparison errors are masked
            result = false;
        }

        return result;
    }

    /**
    * Determine whether two nodes are deep-equal
    */

    private static boolean deepEquals(NodeInfo n1, NodeInfo n2,
                                      AtomicComparer collator, Configuration config)
    throws XPathException {
        // shortcut: a node is always deep-equal to itself
        if (n1.isSameNodeInfo(n2)) return true;

        if (n1.getNodeKind() != n2.getNodeKind()) return false;
        switch (n1.getNodeKind()) {
            case Type.ELEMENT:
                if (n1.getFingerprint() != n2.getFingerprint()) {
                    return false;
                }
                AxisIterator a1 = n1.iterateAxis(Axis.ATTRIBUTE);
                AxisIterator a2 = n2.iterateAxis(Axis.ATTRIBUTE);
                if (Aggregate.count(a1.getAnother()) != Aggregate.count(a2)) {
                    return false;
                }
                while (true) {
                    NodeInfo att1 = (NodeInfo)a1.next();
                    if (att1 == null) break;

                    AxisIterator a2iter = n2.iterateAxis(Axis.ATTRIBUTE,
                                            new NameTest(Type.ATTRIBUTE, att1.getFingerprint(), config.getNamePool()));
                    NodeInfo att2 = (NodeInfo)a2iter.next();

                    if (att2==null) {
                        return false;
                    }
                    if (!deepEquals(att1, att2, collator, config)) {
                        return false;
                    }
                }
                // fall through
            case Type.DOCUMENT:
                AxisIterator c1 = n1.iterateAxis(Axis.CHILD);
                AxisIterator c2 = n2.iterateAxis(Axis.CHILD);
                while (true) {
                    NodeInfo d1 = (NodeInfo)c1.next();
                    while (d1 != null && (
                            d1.getNodeKind() == Type.COMMENT ||
                            d1.getNodeKind() == Type.PROCESSING_INSTRUCTION)) {
                        d1 = (NodeInfo)c1.next();
                    }
                    NodeInfo d2 = (NodeInfo)c2.next();
                    while (d2 != null && (
                            d2.getNodeKind() == Type.COMMENT ||
                            d2.getNodeKind() == Type.PROCESSING_INSTRUCTION)) {
                        d2 = (NodeInfo)c2.next();
                    }
                    if (d1 == null || d2 == null) {
                        return (d1 == d2);
                    }
                    if (!deepEquals(d1, d2, collator, config)) {
                        return false;
                    }
                }


            case Type.ATTRIBUTE:
                if (n1.getFingerprint() != n2.getFingerprint()) {
                    return false;
                }
                return deepEquals(n1.getTypedValue(), n2.getTypedValue(), collator, config);

            case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
                if (n1.getFingerprint() != n2.getFingerprint()) {
                    return false;
                }
                // drop through
            case Type.TEXT:
            case Type.COMMENT:
                return (collator.comparesEqual(n1.getStringValue(), n2.getStringValue()));

            default:
                throw new IllegalArgumentException("Unknown node type");
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
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
