package net.sf.saxon.functions;
import net.sf.saxon.Err;
import net.sf.saxon.charcode.UnicodeCharacterSet;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.sort.GlobalOrderComparer;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
* This class implements functions that are supplied as standard with SAXON,
* but which are not defined in the XSLT or XPath specifications. <p>
*
* To invoke these functions, use a function call of the form prefix:name() where
* name is the method name, and prefix maps to a URI such as
* http://saxon.sf.net/net.sf.saxon.functions.Extensions (only the part
* of the URI after the last slash is important).
*/



public class Extensions  {

    // The class is never instantiated
    private Extensions() {}

    /**
    * Switch tracing off. Only works if tracing is enabled.
    */

    public static void pauseTracing(XPathContext c) {
        c.getController().pauseTracing(true);
    }

    /**
    * Resume tracing. Only works if tracing was originally enabled
    * but is currently paused.
    */


    public static void resumeTracing(XPathContext c) {
        c.getController().pauseTracing(false);
    }

    /**
    * Return the system identifier of the context node
    */

    public static String systemId(XPathContext c) throws XPathException {
        Item item = c.getContextItem();
        if (item==null) {
            DynamicError e = new DynamicError("The context item for saxon:systemId() is not set");
            e.setXPathContext(c);
            throw e;
        }
        if (item instanceof NodeInfo) {
            return ((NodeInfo)item).getSystemId();
        } else {
            return "";
        }
    }

    /**
    * Return the line number of the context node.
    */
    public static int lineNumber(XPathContext c) {
        Item item = c.getCurrentIterator().current();
        if (item instanceof NodeInfo) {
            return ((NodeInfo)item).getLineNumber();
        } else {
            return -1;
        }
    }

    /**
    * Return the line number of the specified node.
    */
    public static int lineNumber(NodeInfo node) {
        return node.getLineNumber();
    }

    /**
     * Remove a document from the document pool. The effect is that the document becomes eligible for
     * garbage collection, allowing memory to be released when processing of the document has finished.
     * The downside is that a subsequent call on document() with the same URI causes the document to be
     * reloaded and reparsed, and the new nodes will have different node identity from the old.
     * @param context the evaluation context (supplied implicitly by the call mechanism)
     * @param doc the document to be released from the document pool
     * @return the document that was released. This allows a call such as
     * select="saxon:discard-document(document('a.xml'))"
     */

    public static DocumentInfo discardDocument(XPathContext context, DocumentInfo doc) {
        return context.getController().getDocumentPool().discard(doc);
    }

    /**
    * Determine whether two node-sets contain the same nodes
    * @param p1 The first node-set. The iterator must be correctly ordered.
    * @param p2 The second node-set. The iterator must be correctly ordered.
    * @return true if p1 and p2 contain the same set of nodes
    */

    public static boolean hasSameNodes(SequenceIterator p1, SequenceIterator p2) throws XPathException {
        SequenceIterator e1 = p1;
        SequenceIterator e2 = p2;

        while (true) {
            NodeInfo n1 = (NodeInfo)e1.next();
            NodeInfo n2 = (NodeInfo)e2.next();
            if (n1==null || n2==null) {
                return n1==n2;
            }
            if (!n1.isSameNodeInfo(n2)) {
                return false;
            }
        }
    }


    /**
    * Evaluate the stored expression supplied in the first argument
    */

    //public static Value eval (XPathContext c, Expression expr) throws XPathException {
    //    return expr.lazyEvaluate(c, false);
    //}
    /**
    * Total a stored expression over a set of nodes
    */

    public static double sum (XPathContext context,
                              SequenceIterator nsv,
                              Evaluate.PreparedExpression pexpression) throws XPathException {

        double total = 0.0;
        XPathContext c = context.newMinorContext();
        c.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
        c.setCurrentIterator(nsv);
        while (true) {
            Item next = nsv.next();
            if (next == null) break;
            Item val = pexpression.expression.evaluateItem(c);
            if (val instanceof NumericValue) {
                DoubleValue v = (DoubleValue)((NumericValue)val).convert(Type.DOUBLE);
                total += v.getDoubleValue();
            } else {
                DynamicError e = new DynamicError("expression in saxon:sum() must return numeric values");
                e.setXPathContext(c);
                throw e;
            }
        }
        return total;
    }

    /**
    * Get the maximum numeric value of a stored expression over a set of nodes
    */

    public static double max (XPathContext context,
                              SequenceIterator nsv,
                              Evaluate.PreparedExpression pexpression) throws XPathException {
        double max = Double.NEGATIVE_INFINITY;
        XPathContext c = context.newMinorContext();
        c.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
        c.setCurrentIterator(nsv);
        while (true) {
            Item next = nsv.next();
            if (next==null) break;
            Item val = pexpression.expression.evaluateItem(c);
            if (val instanceof NumericValue) {
                DoubleValue v = (DoubleValue)((NumericValue)val).convert(Type.DOUBLE);
                if (v.getDoubleValue()>max) max = v.getDoubleValue();
            } else {
                DynamicError e = new DynamicError("expression in saxon:max() must return numeric values");
                e.setXPathContext(c);
                throw e;
            }
        }
        return max;
    }

     /**
    * Get the minimum numeric value of a stored expression over a set of nodes
    */

    public static double min (XPathContext context,
                              SequenceIterator nsv,
                              Evaluate.PreparedExpression pexpression) throws XPathException {
        double min = Double.POSITIVE_INFINITY;
        XPathContext c = context.newMinorContext();
        c.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
        c.setCurrentIterator(nsv);
        while (true) {
            Item next = nsv.next();
            if (next==null) break;
            Item val = pexpression.expression.evaluateItem(c);
            if (val instanceof NumericValue) {
                DoubleValue v = (DoubleValue)((NumericValue)val).convert(Type.DOUBLE);
                if (v.getDoubleValue()<min) min = v.getDoubleValue();
            } else {
                DynamicError e = new DynamicError("expression in saxon:min() must return numeric values");
                e.setXPathContext(context);
                throw e;
            }
        }
        return min;
    }

    /**
    * Get the node with maximum numeric value of the string-value of each of a set of nodes
    */

    public static Value highest (SequenceIterator nsv) throws XPathException {
        return net.sf.saxon.exslt.Math.highest(nsv);
    }


    /**
    * Get the maximum numeric value of a stored expression over a set of nodes
    */

    public static SequenceIterator highest (XPathContext context,
                                        SequenceIterator nsv,
                                        Evaluate.PreparedExpression pexpression) throws XPathException {
        double max = Double.NEGATIVE_INFINITY;
        XPathContext c = context.newMinorContext();
        c.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
        Item highest = null;
        c.setCurrentIterator(nsv);
        while (true) {
            Item next = nsv.next();
            if (next==null) break;
            Item val = pexpression.expression.evaluateItem(c);
            if (val instanceof NumericValue) {
                DoubleValue v = (DoubleValue)((NumericValue)val).convert(Type.DOUBLE);
                if (v.getDoubleValue()>max) {
                    max = v.getDoubleValue();
                    highest = nsv.current();
                }
            } else {
                DynamicError e = new DynamicError("expression in saxon:highest() must return numeric values");
                e.setXPathContext(context);
                throw e;
            }
        }
        return SingletonIterator.makeIterator(highest);
    }

    /**
    * Get the node with minimum numeric value of the string-value of each of a set of nodes
    */

    public static Value lowest (SequenceIterator nsv) throws XPathException {
        return net.sf.saxon.exslt.Math.lowest(nsv);
    }

    /**
    * Get the node with minimum numeric value of a stored expression over a set of nodes
    */

    public static SequenceIterator lowest (XPathContext context,
                                       SequenceIterator nsv,
                                       Evaluate.PreparedExpression pexpression) throws XPathException {
        double min = Double.POSITIVE_INFINITY;
        XPathContext c = context.newMinorContext();
        c.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
        Item lowest = null;
        c.setCurrentIterator(nsv);
        while (true) {
            Item next = nsv.next();
            if (next==null) break;
            Item val = pexpression.expression.evaluateItem(c);
            if (val instanceof NumericValue) {
                DoubleValue v = (DoubleValue)((NumericValue)val).convert(Type.DOUBLE);
                if (v.getDoubleValue()<min) {
                    min = v.getDoubleValue();
                    lowest = nsv.current();
                }
            } else {
                DynamicError e = new DynamicError("expression in saxon:lowest() must return numeric values");
                e.setXPathContext(context);
                throw e;
            }
        }
        return SingletonIterator.makeIterator(lowest);
    }

    /**
    * Get the items that satisfy the given expression, up to and excluding the first one
    * (in sequence order) that doesn't
    */

    public static SequenceIterator leading (XPathContext context,
                         SequenceIterator in, Evaluate.PreparedExpression pexp) throws XPathException {
        XPathContext c2 = context.newMinorContext();
        c2.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
        return new FilterIterator.Leading(in, pexp.expression, c2);
    }

    /**
    * Find all the nodes in ns1 that are after the first node in ns2.
    * Return ns1 if ns2 is empty,
    */

    // This function is no longer documented as a user-visible extension function.
    // But exslt:trailing depends on it.

    public static SequenceIterator after (
                     XPathContext context,
                     SequenceIterator ns1,
                     SequenceIterator ns2) throws XPathException {

        NodeInfo first = null;

        // Find the first node in ns2 (in document order)

        GlobalOrderComparer comparer = GlobalOrderComparer.getInstance();
        while (true) {
            Item item = ns2.next();
            if (item == null) {
                if (first == null) {
                    return ns1;
                } else {
                    break;
                }
            }
            if (item instanceof NodeInfo) {
                NodeInfo node = (NodeInfo)item;
                if (first==null) {
                    first = node;
                } else {
                    if (comparer.compare(node, first) < 0) {
                        first = node;
                    }
                }
            } else {
                DynamicError e = new DynamicError("Operand of after() contains an item that is not a node");
                e.setXPathContext(context);
                throw e;
            }
        }

        // Filter ns1 to select nodes that come after this one

        Expression filter = new IdentityComparison(
                                    new ContextItemExpression(),
                                    Token.FOLLOWS,
                                    new SingletonNode(first));

        return new FilterIterator(ns1, filter, context);

    }


    /**
    * Return a node-set by tokenizing a supplied string. Tokens are delimited by any sequence of
    * whitespace characters.
    */

    // This extension is superseded by fn:tokenize(); it is no longer documented in Saxon 8.1

    public static SequenceIterator tokenize(String s) {
        if (s == null) {
            // empty sequence supplied: treat as empty string
            return EmptyIterator.getInstance();
        }
        return new StringTokenIterator(s);
    }

    /**
    * Return a sequence by tokenizing a supplied string. The argument delim is a String, any character
    * in this string is considered to be a delimiter character, and any sequence of delimiter characters
    * acts as a separator between tokens.
    */

    // This extension is superseded by fn:tokenize(); it is no longer documented in Saxon 8.1

    public static SequenceIterator tokenize(String s, String delim) {
        if (s == null) {
            // empty sequence supplied: treat as empty string
            return EmptyIterator.getInstance();
        }
        return new StringTokenIterator(s, delim);
    }



    /**
    * Return an XPath expression that identifies the current node
    */

    public static String path(XPathContext c) throws XPathException {
        Item item = c.getContextItem();
        if (item==null) {
            DynamicError e = new DynamicError("The context item for saxon:path() is not set");
            e.setXPathContext(c);
            throw e;
        }
        if (item instanceof NodeInfo) {
            return Navigator.getPath((NodeInfo)item);
        } else {
            return "";
        }
    }

    /**
     * Display the value of the type annotation of a node
     */

    public static String typeAnnotation(XPathContext context, NodeInfo node) {
        int code = node.getTypeAnnotation();
        if (code==-1) {
            int nodeKind = node.getNodeKind();
            if (nodeKind == Type.ELEMENT || nodeKind == Type.DOCUMENT) {
                return "untyped";
            } else {
                return "untypedAtomic";
            }
        }
        SchemaType type = context.getController().getConfiguration().getSchemaType(code & 0xfffff);
        if (type==null) {
            // Anonymous types are not accessible by the namecode
            return context.getController().getNamePool().getDisplayName(code);
        }
        return "type " + type.getDescription();
    }

	/**
	* Return the XPathContext object
	*/

	public static XPathContext getContext(XPathContext c) {
		return c;
	}

	/**
	* Get a pseudo-attribute of a processing instruction. Return an empty string
	* if the pseudo-attribute is not present.
	* Character references and built-in entity references are expanded
	*/

	public static String getPseudoAttribute(XPathContext c, String name)
	throws XPathException {
	    Item pi = c.getContextItem();
        if (pi==null) {
            DynamicError e = new DynamicError("The context item for saxon:getPseudoAttribute() is not set");
            e.setXPathContext(c);
            throw e;
        }
	    // we'll assume it's a PI, it doesn't matter if it isn't...
	    String val = ProcInstParser.getPseudoAttribute(pi.getStringValue(), name);
	    if (val==null) return "";
	    return val;
	}

    /**
    * Get a dayTimeDuration value corresponding to a given number of seconds
    */
    // no longer documented in Saxon 8.1
    public static SecondsDurationValue dayTimeDurationFromSeconds(double arg) throws XPathException {
        return SecondsDurationValue.fromSeconds(arg);
    }

    /**
    * Get a yearMonthDuration value corresponding to a given number of months
    */
    // no longer documented in Saxon 8.1
    public static MonthDurationValue yearMonthDurationFromMonths(double arg) {
        return MonthDurationValue.fromMonths((int)arg);
    }

    /**
     * Perform decimal division to a user-specified precision
     */

    public static BigDecimal decimalDivide(BigDecimal arg1, BigDecimal arg2, int scale) {
        return arg1.divide(arg2, scale, BigDecimal.ROUND_DOWN);
    }


    /**
     * Get the UTF-8 encoding of a string
     * @param in the supplied string
     * @return a sequence of integers, each in the range 0-255, representing the octets of the UTF-8
     * encoding of the given string
     */

    public static List stringToUtf8(String in) {
        ArrayList list = new ArrayList(in.length()*2);
        byte[] octets = new byte[4];
        for (int i=0; i<in.length(); i++) {
            int used = UnicodeCharacterSet.getUTF8Encoding(
                    in.charAt(i), (i+1<in.length()? in.charAt(i+1): (char)0), octets);
            for (int j=0; j<used; j++) {
                list.add(new Integer(255&(int)octets[j]));
            }
        }
        return list;
    }

    /**
     * Convert a sequence of integers in the range 0-255, representing a sequence of octets,
     * to a base64Binary value
     */

    public static Base64BinaryValue octetsToBase64Binary(byte[] in) {
        return new Base64BinaryValue(in);
    }

    /**
     * Convert a sequence of integers in the range 0-255, representing a sequence of octets,
     * to a hexBinary value
     */

    public static HexBinaryValue octetsToHexBinary(byte[] in) {
        return new HexBinaryValue(in);
    }

    /**
     * Convert a base64Binary value to a sequence of integers representing the octets contained in the value
     */

    public static byte[] base64BinaryToOctets(Base64BinaryValue in) {
        return in.getBinaryValue();
    }

    /**
     * Convert a hexBinary value to a sequence of integers representing the octets contained in the value
     */

    public static byte[] hexBinaryToOctets(HexBinaryValue in) {
        return in.getBinaryValue();
    }

    /**
     * Create a parentless namespace node. This function is useful in XQuery when namespaces need to be created
     * dynamically. The effect is the same as that of the xsl:namespace instruction in XSLT.
     */

    public static NodeInfo namespaceNode(XPathContext context, String prefix, String uri) throws XPathException {
        if (prefix == null) {
            prefix = "";
        }
        if (!("".equals(prefix) || XMLChar.isValidNCName(prefix))) {
            DynamicError err = new DynamicError("Namespace prefix " + Err.wrap(prefix) + " is not a valid NCName");
            throw err;
        }
        if (uri==null || "".equals(uri)) {
            DynamicError err = new DynamicError("URI of namespace node must not be empty");
            throw err;
        }
        final NamePool namePool = context.getController().getNamePool();
        Orphan node = new Orphan(context.getController().getConfiguration());
        node.setNodeKind(Type.NAMESPACE);
        node.setNameCode(namePool.allocate("", "", prefix));
        node.setStringValue(uri);
        return node;
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
