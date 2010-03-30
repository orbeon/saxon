package org.orbeon.saxon.functions;

import org.orbeon.saxon.*;
import org.orbeon.saxon.number.NamedTimeZone;
import org.orbeon.saxon.charcode.UTF16;
import org.orbeon.saxon.charcode.UnicodeCharacterSet;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.query.DynamicQueryContext;
import org.orbeon.saxon.query.QueryResult;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.query.XQueryExpression;
import org.orbeon.saxon.sort.*;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.NoDynamicContextException;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.util.*;

/**
 * This class implements functions that are supplied as standard with SAXON,
 * but which are not defined in the XSLT or XPath specifications. <p>
 * <p/>
 * To invoke these functions, use a function call of the form prefix:name() where
 * name is the method name, and prefix maps to a URI such as
 * http://saxon.sf.net/org.orbeon.saxon.functions.Extensions (only the part
 * of the URI after the last slash is important).
 */


public class Extensions {

    // The class is never instantiated
    private Extensions() {
    }

    /**
     * Switch tracing off. Only works if tracing was enabled at compile time.
     *
     * @param c the XPath dynamic context
     */

    public static void pauseTracing(XPathContext c) {
        c.getController().pauseTracing(true);
    }

    /**
     * Resume tracing. Only works if tracing was originally enabled
     * but is currently paused.
     *
     * @param c the XPath dynamic context
     */

    public static void resumeTracing(XPathContext c) {
        c.getController().pauseTracing(false);
    }

    /**
     * Return the system identifier of the context node
     *
     * @param c the XPath dynamic context
     * @return the system ID
     */

    public static String systemId(XPathContext c) throws XPathException {
        Item item = c.getContextItem();
        if (item == null) {
            XPathException e = new XPathException("The context item for saxon:systemId() is not set");
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
     * The function saxon:generate-id() is equivalent to the standard XSLT function generate-id().
     * It is provided as an extension function to make it available in non-XSLT environments, for example
     * in XQuery.
     *
     * @param node the node whose identifier is required
     * @return as ASCII alphanumeric string that uniquely identifies this node
     */

    public static String generateId(NodeInfo node) {
        FastStringBuffer buffer = new FastStringBuffer(16);
        node.generateId(buffer);
        return buffer.toString();
    }

    /**
     * Return the line number of the context node.
     *
     * @param c the XPath dynamic context
     * @return the line number, or -1 if not available
     */
    public static int lineNumber(XPathContext c) {
        Item item = c.getContextItem();
        if (item instanceof NodeInfo) {
            return ((NodeInfo)item).getLineNumber();
        } else {
            return -1;
        }
    }

    /**
     * Return the line number of the specified node.
     *
     * @param node the node whose line number is required
     * @return the line number of the node. This is only available if line numbering was switched on.
     */
    public static int lineNumber(NodeInfo node) {
        if (node == null) {
            return -1;
        }
        return node.getLineNumber();
    }

    /**
     * Return the column number of the context node. This is only
     * available if line numbering has been enabled for the containing tree
     *
     * @param c the XPath dynamic context
     * @return the column number, or -1 if not available
     */
    public static int columnNumber(XPathContext c) {
        Item item = c.getCurrentIterator().current();
        if (item instanceof NodeInfo) {
            return ((NodeInfo)item).getColumnNumber();
        } else {
            return -1;
        }
    }

    /**
     * Return the column number of the specified node.
     *
     * @param node the node whose column number is required
     * @return the column number of the node. This is only available if line numbering was switched on.
     */
    public static int columnNumber(NodeInfo node) {
        if (node == null) {
            return -1;
        }
        return node.getColumnNumber();
    }


    /**
     * Remove a document from the document pool. The effect is that the document becomes eligible for
     * garbage collection, allowing memory to be released when processing of the document has finished.
     * The downside is that a subsequent call on document() with the same URI causes the document to be
     * reloaded and reparsed, and the new nodes will have different node identity from the old.
     *
     * @param context the evaluation context (supplied implicitly by the call mechanism)
     * @param doc     the document to be released from the document pool
     * @return the document that was released. This allows a call such as
     *         select="saxon:discard-document(document('a.xml'))"
     */

    public static DocumentInfo discardDocument(XPathContext context, DocumentInfo doc) {
        if (doc == null) {
            return null;
        }
        Controller c = context.getController();
        String uri = c.getDocumentPool().getDocumentURI(doc);
        if (uri != null) {
            c.removeUnavailableOutputDestination(uri);
        }
        return c.getDocumentPool().discard(doc);
    }

    /**
     * Determine whether two node-sets contain the same nodes
     *
     * @param p1 The first node-set. The iterator must be correctly ordered.
     * @param p2 The second node-set. The iterator must be correctly ordered.
     * @return true if p1 and p2 contain the same set of nodes
     */

    public static boolean hasSameNodes(SequenceIterator p1, SequenceIterator p2) throws XPathException {
        SequenceIterator e1 = p1;
        SequenceIterator e2 = p2;

        if (e1 == null) {
            e1 = EmptyIterator.getInstance();
        }

        if (e2 == null) {
            e2 = EmptyIterator.getInstance();
        }

        while (true) {
            NodeInfo n1 = (NodeInfo)e1.next();
            NodeInfo n2 = (NodeInfo)e2.next();
            if (n1 == null || n2 == null) {
                return n1 == n2;
            }
            if (!n1.isSameNodeInfo(n2)) {
                return false;
            }
        }
    }

    /**
     * Sort a sequence of nodes or atomic values, using the atomic value itself, or the atomized value of the node,
     * as the sort key. The values must all be comparable. Strings are compared using
     * codepoint collation. When nodes are atomized, the result must not be a sequence containing
     * more than one item.
     *
     * @param context the XPath dynamic context
     * @param input   the sequence to be sorted
     * @return an iterator over the sorted sequence
     */

    public static SequenceIterator sort(XPathContext context, SequenceIterator input) {
        SortKeyEvaluator sortkey = new SortKeyEvaluator() {
            public Item evaluateSortKey(int n, XPathContext context) throws XPathException {
                Item c = context.getContextItem();
                if (c instanceof NodeInfo) {
                    Value v = ((NodeInfo)c).atomize();
                    if (v.getLength() == 0) {
                        c = null;
                    } else if (v.getLength() == 1) {
                        c = v.itemAt(0);
                    } else {
                        throw new XPathException("error in saxon:sort() - a node has a typed value of length > 1");
                    }
                }
                return c;
            }
        };
        AtomicComparer[] comparers = {
                new GenericAtomicComparer(CodepointCollator.getInstance(), context)
        };
        return new SortedIterator(context, input, sortkey, comparers);
    }

    /**
     * Sort a sequence of nodes or atomic values, using a given expression to calculate the sort key.
     * as the sort key. The values must all be comparable. Strings are compared using
     * codepoint collation. When nodes are atomized, the result must not be a sequence containing
     * more than one item.
     *
     * @param context           the XPath dynamic context
     * @param input             the sequence to be sorted
     * @param sortKeyExpression the expression used to compute the sort keys
     * @return an iterator over the sorted sequence
     */

    public static SequenceIterator sort(XPathContext context, SequenceIterator input,
                                        final Evaluate.PreparedExpression sortKeyExpression) {
        SortKeyEvaluator sortkey = new SortKeyEvaluator() {
            public Item evaluateSortKey(int n, XPathContext context) throws XPathException {
                Item c = sortKeyExpression.expression.evaluateItem(context);
                if (c instanceof NodeInfo) {
                    Value v = ((NodeInfo)c).atomize();
                    if (v.getLength() == 0) {
                        c = null;
                    } else if (v.getLength() == 1) {
                        c = v.itemAt(0);
                    } else {
                        throw new XPathException("error in saxon:sort() - a node has a typed value of length > 1");
                    }
                }
                return c;
            }
        };
        AtomicComparer[] comparers = {
                new GenericAtomicComparer(CodepointCollator.getInstance(), context)
        };
        return new SortedIterator(context, input, sortkey, comparers);
    }

    /**
     * Get the node with maximum numeric value of the string-value of each of a set of nodes
     *
     * @param nsv the input sequence
     * @return the node with the maximum numeric value
     */

    public static Value highest(SequenceIterator nsv) throws XPathException {
        return org.orbeon.saxon.exslt.Math.highest(nsv);
    }


    /**
     * Get the maximum numeric value of a stored expression over a set of nodes
     *
     * @param context     the XPath dynamic evaluation context
     * @param nsv         the input sequence
     * @param pexpression the expression whose maximum is to be computed
     * @return an iterator over the items in the input sequence for which the expression takes its maximum value
     */

    public static SequenceIterator highest(XPathContext context,
                                           SequenceIterator nsv,
                                           Evaluate.PreparedExpression pexpression) throws XPathException {
        if (nsv == null) {
            return EmptyIterator.getInstance();
        }
        if (pexpression == null) {
            return EmptyIterator.getInstance();
        }

        double max = Double.NEGATIVE_INFINITY;
        XPathContext c = context.newMinorContext();
        c.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
        Item highest = null;
        c.setCurrentIterator(nsv);
        while (true) {
            Item next = nsv.next();
            if (next == null) {
                break;
            }
            Item val = pexpression.expression.evaluateItem(c);
            if (val instanceof NumericValue) {
                DoubleValue v = (DoubleValue)((NumericValue)val).convert(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
                if (v.getDoubleValue() > max) {
                    max = v.getDoubleValue();
                    highest = nsv.current();
                }
            } else {
                XPathException e = new XPathException("expression in saxon:highest() must return numeric values");
                e.setXPathContext(context);
                throw e;
            }
        }
        return SingletonIterator.makeIterator(highest);
    }

    /**
     * Get the node with minimum numeric value of the string-value of each of a set of nodes
     *
     * @param nsv the input sequence
     * @return the node with the minimum numeric value
     */

    public static Value lowest(SequenceIterator nsv) throws XPathException {
        return org.orbeon.saxon.exslt.Math.lowest(nsv);
    }

    /**
     * Get the node with minimum numeric value of the string-value of each of a set of nodes
     *
     * @param context     the XPath dynamic evaluation context
     * @param nsv         the input sequence
     * @param pexpression the expression whose minimum is to be computed
     * @return an iterator over the items in the input sequence for which the expression takes its minimum value
     */

    public static SequenceIterator lowest(XPathContext context,
                                          SequenceIterator nsv,
                                          Evaluate.PreparedExpression pexpression) throws XPathException {
        if (nsv == null) {
            return EmptyIterator.getInstance();
        }
        if (pexpression == null) {
            return EmptyIterator.getInstance();
        }

        double min = Double.POSITIVE_INFINITY;
        XPathContext c = context.newMinorContext();
        c.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
        Item lowest = null;
        c.setCurrentIterator(nsv);
        while (true) {
            Item next = nsv.next();
            if (next == null) {
                break;
            }
            Item val = pexpression.expression.evaluateItem(c);
            if (val instanceof NumericValue) {
                DoubleValue v = (DoubleValue)((NumericValue)val).convert(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
                if (v.getDoubleValue() < min) {
                    min = v.getDoubleValue();
                    lowest = nsv.current();
                }
            } else {
                XPathException e = new XPathException("expression in saxon:lowest() must return numeric values");
                e.setXPathContext(context);
                throw e;
            }
        }
        return SingletonIterator.makeIterator(lowest);
    }

    /**
     * Get the items that satisfy the given expression, up to and excluding the first one
     * (in sequence order) that doesn't satisfy the expression.
     *
     * @param context the XPath dynamic evaluation context
     * @param in      the input sequence
     * @param pexp    the expression against which items are to be tested
     * @return an iterator over the items in the input sequence up to and excluding the first
     *         one that doesn't satisfy the expression
     */

    public static SequenceIterator leading(XPathContext context,
                                           SequenceIterator in,
                                           Evaluate.PreparedExpression pexp) {
        if (in == null) {
            return EmptyIterator.getInstance();
        }
        if (pexp == null) {
            return EmptyIterator.getInstance();
        }

        XPathContext c2 = context.newMinorContext();
        c2.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
        return new FilterIterator.Leading(in, pexp.expression, c2);
    }

    /**
     * Find all the nodes in ns1 that are after the first node in ns2.
     * Return ns1 if ns2 is empty,
     *
     * @param context the dynamic evaluation context
     * @param ns1     the first operand
     * @param ns2     the second operand
     * @return an iterator over the nodes in ns1 that are after the first node in ns2
     */

    // This function is no longer documented as a user-visible extension function.
    // But exslt:trailing depends on it.
    public static SequenceIterator after(
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
                if (first == null) {
                    first = node;
                } else {
                    if (comparer.compare(node, first) < 0) {
                        first = node;
                    }
                }
            } else {
                XPathException e = new XPathException("Operand of after() contains an item that is not a node");
                e.setXPathContext(context);
                throw e;
            }
        }

        // Filter ns1 to select nodes that come after this one

        Expression filter = new IdentityComparison(
                new ContextItemExpression(),
                Token.FOLLOWS,
                new Literal(new SingletonNode(first)));

        return new FilterIterator(ns1, filter, context);

    }

    /**
     * Return an XPath expression that identifies a specified node
     *
     * @param node the node whose path is required
     * @return a path expression giving a path from the root of the tree to the specified node
     */

    public static String path(NodeInfo node) throws XPathException {
        return Navigator.getPath(node);
    }


    /**
     * Return an XPath expression that identifies the current node
     *
     * @param c the XPath dynamic context
     * @return a path expression giving a path from the root of the tree to the context node
     */

    public static String path(XPathContext c) throws XPathException {
        Item item = c.getContextItem();
        if (item == null) {
            XPathException e = new XPathException("The context item for saxon:path() is not set");
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
     * Display the value of the type annotation of a node or an atomic value
     *
     * @param context the XPath dynamic context
     * @param item    the node or atomic value whose type annotation is required
     * @return the type annotation or type label as a QName
     */

    public static QNameValue typeAnnotation(XPathContext context, Item item) {
        if (item == null) {
            return null;
        } else if (item instanceof NodeInfo) {
            NodeInfo node = (NodeInfo)item;

            int code = node.getTypeAnnotation();
            if ((code & NodeInfo.IS_DTD_TYPE) != 0) {
                code = StandardNames.XS_UNTYPED_ATOMIC;
            }
            if (code == -1) {
                int nodeKind = node.getNodeKind();
                if (nodeKind == Type.ELEMENT || nodeKind == Type.DOCUMENT) {
                    return new QNameValue("xs", NamespaceConstant.SCHEMA, "untyped");
                } else {
                    return new QNameValue("xs", NamespaceConstant.SCHEMA, "untypedAtomic");
                }
            }
            return new QNameValue(context.getNamePool(), code);
        } else {
            AtomicType label = ((AtomicValue)item).getTypeLabel();
            return new QNameValue(context.getNamePool(), label.getNameCode());
        }
    }

    /**
     * Return the XPathContext object
     *
     * @param c the context object
     * @return the context object (this looks crazy, but it works given that the function is called from an XPath
     *         environment where the context is supplied as an implicit argument)
     */

    public static XPathContext getContext(XPathContext c) {
        return c;
    }

    /**
     * Return the Controller object
     *
     * @param c the XPath dynamic context
     * @return the Controller
     */

    public static Controller getController(XPathContext c) {
        return c.getController();
    }

    /**
     * Return the Configuration object
     *
     * @param c the XPath dynamic context
     * @return the Saxon configuration
     */

    public static Configuration getConfiguration(XPathContext c) {
        return c.getConfiguration();
    }

    /**
     * Return a string containing a diagnostic print of the current execution stack
     * @param c the XPath dynamic context
     * @return a diagnostic stack print
     */

    public static String printStack(XPathContext c) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        StandardErrorListener.printStackTrace(new PrintStream(baos), c);
        return baos.toString();
        //return baos.toString().replace("\r", "");  - needs JDK 1.5
    }

    /**
     * Get a pseudo-attribute of a processing instruction. Return an empty string
     * if the pseudo-attribute is not present.
     * Character references and built-in entity references are expanded
     *
     * @param c    the XPath dynamic context. The context item should be a processing instruction,
     *             though it doesn't matter if it isn't: the function will look at the string-value of the context item
     *             whatever it is.
     * @param name the name of the required pseudo-attribute
     * @return the value of the pseudo-attribute if it is present
     */

    public static String getPseudoAttribute(XPathContext c, String name)
            throws XPathException {
        if (name == null) {
            return null;
        }
        Item pi = c.getContextItem();
        if (pi == null) {
            XPathException e = new XPathException("The context item for saxon:getPseudoAttribute() is not set");
            e.setXPathContext(c);
            throw e;
        }
        // we'll assume it's a PI, it doesn't matter if it isn't...
        String val = ProcInstParser.getPseudoAttribute(pi.getStringValue(), name);
        if (val == null) {
            return "";
        }
        return val;
    }

    /**
     * Get a dayTimeDuration value corresponding to a given number of seconds
     */
    // no longer documented in Saxon 8.1
//    public static DayTimeDurationValue dayTimeDurationFromSeconds(BigDecimal arg) throws XPathException {
//        return DayTimeDurationValue.fromSeconds(arg);
//    }

    /**
     * Get a yearMonthDuration value corresponding to a given number of months
     */
    // no longer documented in Saxon 8.1
//    public static YearMonthDurationValue yearMonthDurationFromMonths(int arg) {
//        return YearMonthDurationValue.fromMonths((int)arg);
//    }

    /**
     * Perform decimal division to a user-specified precision
     *
     * @param arg1  the numerator
     * @param arg2  the denominator
     * @param scale the required number of digits in the result of the division
     * @return the result of the division
     */

    public static BigDecimal decimalDivide(BigDecimal arg1, BigDecimal arg2, int scale) {
        if (arg1 == null || arg2 == null) {
            return null;
        }
        return arg1.divide(arg2, scale, BigDecimal.ROUND_DOWN);
    }


    /**
     * Get the UTF-8 encoding of a string
     *
     * @param in the supplied string
     * @return a sequence of integers, each in the range 0-255, representing the octets of the UTF-8
     *         encoding of the given string
     */

    public static List stringToUtf8(String in) {
        if (in == null) {
            return Collections.EMPTY_LIST;
        }
        ArrayList list = new ArrayList(in.length() * 2);
        byte[] octets = new byte[4];
        for (int i = 0; i < in.length(); i++) {
            int used = UnicodeCharacterSet.getUTF8Encoding(
                    in.charAt(i), (i + 1 < in.length() ? in.charAt(i + 1) : (char)0), octets);
            for (int j = 0; j < used; j++) {
                list.add(new Integer(255 & (int)octets[j]));
            }
        }
        return list;
    }

    /**
     * Convert a sequence of integers in the range 0-255, representing a sequence of octets,
     * to a base64Binary value
     *
     * @param in the input array of bytes (octets)
     * @return the corresponding base64Binary value
     */

    public static Base64BinaryValue octetsToBase64Binary(byte[] in) {
        if (in == null) {
            return null;
        }
        return new Base64BinaryValue(in);
    }

    /**
     * Convert a sequence of integers in the range 0-255, representing a sequence of octets,
     * to a hexBinary value
     *
     * @param in the input array of bytes (octets)
     * @return the corresponding HexBinary value
     */

    public static HexBinaryValue octetsToHexBinary(byte[] in) {
        if (in == null) {
            return null;
        }
        return new HexBinaryValue(in);
    }

    /**
     * Convert a base64Binary value to a sequence of integers representing the octets contained in the value
     *
     * @param in the supplied base64Binary value
     * @return the corresponding array of integers, representing the octet values
     */

    public static byte[] base64BinaryToOctets(Base64BinaryValue in) {
        if (in == null) {
            return null;
        }
        return in.getBinaryValue();
    }

    /**
     * Convert a hexBinary value to a sequence of integers representing the octets contained in the value
     *
     * @param in the input hexBinary value
     * @return the corresponding array of integers, representing the octet values
     */

    public static byte[] hexBinaryToOctets(HexBinaryValue in) {
        if (in == null) {
            return null;
        }
        return in.getBinaryValue();
    }

    /**
     * Convert a base64Binary value to a String, assuming a particular encoding
     *
     * @param context  the XPath dynamic context
     * @param in       the supplied base64Binary value
     * @param encoding the character encoding
     * @return the string that results from treating the base64binary value as a sequence of octets
     *         that encode a string in the given encoding
     */

    public static String base64BinaryToString(XPathContext context, Base64BinaryValue in, String encoding)
            throws Exception {
        if (in == null) {
            return null;
        }
        if (encoding == null) {
            encoding = "UTF-8";
        }
        byte[] bytes = in.getBinaryValue();
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        InputStreamReader reader = new InputStreamReader(stream, encoding);
        char[] array = new char[bytes.length];
        int used = reader.read(array, 0, array.length);
        checkBytes(array, 0, used, context.getConfiguration().getNameChecker());
        return new String(array, 0, used);
    }

    /**
     * Convert a string to a base64Binary value in a given encoding
     *
     * @param in       the input string
     * @param encoding the desired encoding
     * @return the base64Binary value that results from encoding the string as a sequence of octets in the
     *         given encoding.
     */

    public static Base64BinaryValue stringToBase64Binary(String in, String encoding)
            throws IOException {
        if (in == null) {
            return null;
        }
        if (encoding == null) {
            encoding = "UTF-8";
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream(in.length());
        OutputStreamWriter writer = new OutputStreamWriter(stream, encoding);
        writer.write(in);
        writer.close();
        byte[] bytes = stream.toByteArray();
        return octetsToBase64Binary(bytes);
    }

    /**
     * Convert a hexBinary value to a String, assuming a particular encoding
     *
     * @param context  the XPath dynamic context
     * @param in       the supplied hexBinary value
     * @param encoding the character encoding
     * @return the string that results from treating the hexBinary value as a sequence of octets
     *         that encode a string in the given encoding
     */

    public static String hexBinaryToString(XPathContext context, HexBinaryValue in, String encoding) throws Exception {
        if (in == null) {
            return null;
        }
        if (encoding == null) {
            encoding = "UTF-8";
        }
        byte[] bytes = in.getBinaryValue();
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        InputStreamReader reader = new InputStreamReader(stream, encoding);
        char[] array = new char[bytes.length];
        int used = reader.read(array, 0, array.length);
        checkBytes(array, 0, used, context.getConfiguration().getNameChecker());
        return new String(array, 0, used);
    }

    /**
     * Check that characters are valid XML characters (UTF-16 encoded)
     *
     * @param array   the array of characters
     * @param start   the position of the first significant character in the array
     * @param end     the position after the last significant character in the array
     * @param checker the NameChecker to be used (for XML 1.0 or XML 1.1 rules)
     * @throws XPathException if the string contains characters that are invalid in XML
     */

    private static void checkBytes(char[] array, int start, int end, NameChecker checker) throws XPathException {
        for (int c = start; c < end; c++) {
            int ch32 = array[c];
            if (UTF16.isHighSurrogate(ch32)) {
                char low = array[c++];
                ch32 = UTF16.combinePair((char)ch32, low);
            }
            if (!checker.isValidChar(ch32)) {
                XPathException err = new XPathException("The byte sequence contains a character not allowed by XML (hex " +
                        Integer.toHexString(ch32) + ')');
                err.setErrorCode("XTDE1180");
                throw err;
            }
        }
    }

    /**
     * Convert a string to a hexBinary value in a given encoding
     *
     * @param in       the input string
     * @param encoding the desired encoding
     * @return the hexBinary value that results from encoding the string as a sequence of octets in the
     *         given encoding.
     */

    public static HexBinaryValue stringToHexBinary(String in, String encoding) throws Exception {
        if (in == null) {
            return null;
        }
        if (encoding == null) {
            encoding = "UTF-8";
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream(in.length());
        OutputStreamWriter writer = new OutputStreamWriter(stream, encoding);
        writer.write(in);
        writer.close();
        byte[] bytes = stream.toByteArray();
        return octetsToHexBinary(bytes);
    }

    /**
     * Test whether a given integer is the codepoint of a valid XML character
     *
     * @param c  the XPath dynamic context
     * @param in the character to be tested
     * @return true if and only if the character is valid in (the relevant version of) XML
     */

    public static boolean validCharacter(XPathContext c, int in) {
        return c.getConfiguration().getNameChecker().isValidChar(in);
    }

    /**
     * Create a parentless namespace node. This function is useful in XQuery when namespaces need to be created
     * dynamically. The effect is the same as that of the xsl:namespace instruction in XSLT.
     *
     * @param context the dynamic evaluation context
     * @param prefix  the name of the namespace node
     * @param uri     the string value of the namespace node
     * @return the newly constructed namespace node
     */

    public static NodeInfo namespaceNode(XPathContext context, String prefix, String uri) throws XPathException {
        if (prefix == null) {
            prefix = "";
        } else if (!(prefix.length() == 0 || context.getConfiguration().getNameChecker().isValidNCName(prefix))) {
            throw new XPathException("Namespace prefix " + Err.wrap(prefix) + " is not a valid NCName");
        }
        if (uri == null || uri.length() == 0) {
            throw new XPathException("URI of namespace node must not be empty");
        }
        final NamePool namePool = context.getNamePool();
        Orphan node = new Orphan(context.getConfiguration());
        node.setNodeKind(Type.NAMESPACE);
        node.setNameCode(namePool.allocate("", "", prefix));
        node.setStringValue(uri);
        return node;
    }

    /**
     * Get a list of the names of the unparsed entities in a document
     * @param doc the document node of the document whose unparsed entities are required
     * @return an iterator over a sequence of strings containing the names of the unparsed entities
     */

    public static String[] unparsedEntities(DocumentInfo doc) throws XPathException {
        Iterator names = doc.getUnparsedEntityNames();
        int count = 0;
        while (names.hasNext()) {
            names.next();
            count++;
        }
        String[] ss = new String[count];
        names = doc.getUnparsedEntityNames();
        count = 0;
        while (names.hasNext()) {
            ss[count++] = (String)names.next();
        }
        return ss;
    }

    /**
     * Perform a parameterized deep-equals() test
     *
     * @param context   The evaluation context
     * @param arg1      The first sequence to be compared
     * @param arg2      The second sequence to be compared
     * @param collation The collation to be used (null if the default collation is to be used)
     * @param flags     A string whose characters select options that cause the comparison to vary from the
     *                  standard fn:deep-equals() function. The flags are:
     *                  <ul>
     *                  <li>N - take namespace nodes into account</li>
     *                  <li>J - join adjacent text nodes (e.g, nodes either side of a comment)
     *                  <li>A - compare type annotations</li>
     *                  <li>C - take comments into account</li>
     *                  <li>F - take namespace prefixes into account</li>
     *                  <li>P - take processing instructions into account</li>
     *                  <li>S - compare string values, not typed values</li>
     *                  <li>w - don't take whitespace-only text nodes into account</li>
     *                  </ul>
     * @return true if the sequences are deep equal, otherwise false
     */

    public static boolean deepEqual(XPathContext context,
                                    SequenceIterator arg1, SequenceIterator arg2,
                                    String collation, String flags) throws XPathException {
        if (flags.indexOf('!') >= 0) {
            // undocumented diagnostic option
            Properties indent = new Properties();
            indent.setProperty(OutputKeys.INDENT, "yes");
            System.err.println("DeepEqual: first argument:");
            QueryResult.serialize(QueryResult.wrap(arg1.getAnother(), context.getConfiguration()),
                    new StreamResult(System.err), indent);
            System.err.println("DeepEqual: second argument:");
            QueryResult.serialize(QueryResult.wrap(arg2.getAnother(), context.getConfiguration()),
                    new StreamResult(System.err), indent);
        }
        GenericAtomicComparer comparer;
        if (collation == null) {
            comparer = new GenericAtomicComparer(context.getDefaultCollation(), context);
        } else {
            comparer = new GenericAtomicComparer(context.getCollation(collation), context);
        }
        int flag = 0;
        if (flags.indexOf("N") >= 0) {
            flag |= DeepEqual.INCLUDE_NAMESPACES;
        }
        if (flags.indexOf("J") >= 0) {
            flag |= DeepEqual.JOIN_ADJACENT_TEXT_NODES;
        }
        if (flags.indexOf("C") >= 0) {
            flag |= DeepEqual.INCLUDE_COMMENTS;
        }
        if (flags.indexOf("P") >= 0) {
            flag |= DeepEqual.INCLUDE_PROCESSING_INSTRUCTIONS;
        }
        if (flags.indexOf("F") >= 0) {
            flag |= DeepEqual.INCLUDE_PREFIXES;
        }
        if (flags.indexOf("S") >= 0) {
            flag |= DeepEqual.COMPARE_STRING_VALUES;
        }
        if (flags.indexOf("A") >= 0) {
            flag |= DeepEqual.COMPARE_ANNOTATIONS;
        }
        if (flags.indexOf("w") >= 0) {
            flag |= DeepEqual.EXCLUDE_WHITESPACE_TEXT_NODES;
        }
        if (flags.indexOf("?") >= 0) {
            flag |= DeepEqual.WARNING_IF_FALSE;
        }
        return DeepEqual.deepEquals(arg1, arg2, comparer, context.getConfiguration(), flag);

    }

    /**
     * This function implements the <code>last-modified()</code> function without any argument. It returns
     * the modification time of the file containing the context node.
     *
     * @param c the dynamic evaluation context supplied by Saxon
     * @return file modification time as an xs:dateTime value, or an empty sequence if the context item is
     *         not a node or if the context node is not present in a local file
     * @throws XPathException XPath dynamic error reported back to Saxon
     */
    public static DateTimeValue lastModified(XPathContext c) throws XPathException {
        // Original author Zdenek Wagner [zdenek.wagner@gmail.com] contributed 2007-10-22
        Item item = c.getContextItem();
        if (item == null) {
            XPathException e = new XPathException("The context item for lastModified() is not set");
            e.setXPathContext(c);
            throw e;
        }
        if (item instanceof NodeInfo) {
            return lastModified(c, (NodeInfo)item);
        } else {
            return null;
        }
    }

    /**
     * This function implements the <code>last-modified(node)</code> function with one argument which
     * must be a node. It returns the modification time of the file containing the context node.
     *
     * @param node the node supplied by a user
     * @return file modification time as an xs:dateTime value, or an empty sequence if the supplied
     *         node is not present in a local file
     * @throws XPathException if an error occurs and the configuration option TRACE_EXTERNAL_FUNCTIONS is true
     */
    public static DateTimeValue lastModified(XPathContext context, NodeInfo node) throws XPathException {
        // Original author Zdenek Wagner [zdenek.wagner@gmail.com] contributed 2007-10-22
        // Rewritten by Michael Kay to use URL checking and connection code from UnparsedText.java
        return fileLastModified(context, node.getSystemId());
    }

    /**
     * This function determines the file modification time. It can be called from the stylesheet as file-timestamp(fn).
     * @param context the XPath dynamic evaluation context
     * @param fileURI the URI of a file. This must be an absolute URI to which Saxon can connect
     * @return file modification time as an xs:dateTime value or an empty sequence if the file is
     *         not found
     * @throws XPathException if an error occurs and the configuration option TRACE_EXTERNAL_FUNCTIONS is true
     */
    public static DateTimeValue fileLastModified(XPathContext context, String fileURI) throws XPathException {
        // Original author Zdenek Wagner [zdenek.wagner@gmail.com] contributed 2007-10-22
        // Rewritten by Michael Kay to take a URI and to use URL connection code from UnparsedText.java
        boolean debug = context.getConfiguration().isTraceExternalFunctions();
        URI absoluteURI;
        try {
            absoluteURI = new URI(fileURI);
        } catch (URISyntaxException e) {
            if (debug) {
                throw new XPathException(e);
            }
            return null;
        }
        if (!absoluteURI.isAbsolute()) {
            if (debug) {
                throw new XPathException("Supplied URI " + fileURI + " is not a valid absolute URI");
            }
            return null;
        }
        // The URL dereferencing classes throw all kinds of strange exceptions if given
        // ill-formed sequences of %hh escape characters. So we do a sanity check that the
        // escaping is well-formed according to UTF-8 rules
        try {
            EscapeURI.checkPercentEncoding(absoluteURI.toString());
        } catch (XPathException e) {
            if (debug) {
                throw e;
            }
            return null;
        }
        URL absoluteURL;
        try {
            absoluteURL = absoluteURI.toURL();
        } catch (MalformedURLException err) {
            if (debug) {
                throw new XPathException(err);
            }
            return null;
        }
        long lastMod;
        try {
            URLConnection connection = absoluteURL.openConnection();
            connection.setRequestProperty("Accept-Encoding","gzip");
            connection.connect();
            lastMod = connection.getLastModified();
        } catch (IOException e) {
            if (debug) {
                throw new XPathException(e);
            }
            return null;
        }

        if (lastMod == 0) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(lastMod);
        return new DateTimeValue(c, true);
    }

    /**
     * Determine whether a given date/time is in summer time (daylight savings time)
     * in a given region. This relies on the Java database of changes to daylight savings time.
     * Since summer time changes are set by civil authorities the information is not necessarily
     * reliable when applied to dates in the future.
     * @param context used to get the implicit timezone in the event that the supplied date/time
     * has no timezone
     * @param date the date/time in question. This should preferably include a timezone.
     * @param region either the two-letter ISO country code, or an Olsen timezone name such as
     * "America/New_York" or "Europe/Lisbon". If the country code denotes a country spanning several
     * timezones, such as the US, then one of them is chosen arbitrarily.
     * @return true if the date/time is known to be in summer time in the relevant country;
     * false if it is known not to be in summer time; null if there is no timezone or if no
     * information is available.
     */

    public static BooleanValue inSummerTime(XPathContext context, DateTimeValue date, String region) {
        if (!date.hasTimezone()) {
            try {
                date = (DateTimeValue)date.adjustTimezone(context.getImplicitTimezone());
            } catch (NoDynamicContextException err) {
                date = (DateTimeValue)date.adjustTimezone(0);
            }
        }
        Boolean b = NamedTimeZone.inSummerTime(date, region);
        return (b == null ? null : BooleanValue.get(b.booleanValue()));
    }

    /**
     * Compile a document containing a stylesheet module into a stylesheet that can be used to perform
     * transformations
     *
     * @param context the XPath dynamic evaluation context
     * @param doc     the document containing the stylesheet to be compiled
     * @return the compiled stylesheet
     */

    public static Templates compileStylesheet(XPathContext context, DocumentInfo doc) throws XPathException {
        if (doc == null) {
            return null;
        }
        try {
            TransformerFactoryImpl factory = new TransformerFactoryImpl(context.getConfiguration());
            return factory.newTemplates(doc);
        } catch (TransformerConfigurationException e) {
            throw XPathException.makeXPathException(e);
        }

    }

    /**
     * Run a transformation to convert an input tree to an output document
     *
     * @param context   The dynamic context
     * @param templates The compiled stylesheet
     * @param source    The initial context node representing the document to be transformed
     * @return the document that results from the transformation
     */

    public static DocumentInfo transform(XPathContext context, Templates templates, NodeInfo source) throws XPathException {
        if (templates == null) {
            return null;
        }
        if (source == null) {
            return null;
        }
        try {
            Transformer transformer = templates.newTransformer();
            TinyBuilder builder = new TinyBuilder();
            builder.setPipelineConfiguration(context.getController().makePipelineConfiguration());
            transformer.transform(source, builder);
            return (DocumentInfo)builder.getCurrentRoot();
        } catch (TransformerException e) {
            throw XPathException.makeXPathException(e);
        }
    }

    /**
     * Run a transformation to convert an input tree to an output document, supplying parameters to the
     * transformation.
     *
     * @param context   The dynamic context
     * @param templates The compiled stylesheet
     * @param source    The initial context node representing the document to be transformed
     * @param params    A sequence of nodes (typically element nodes) supplying values of parameters.
     *                  The name of the node should match the name of the parameter, the typed value of the node is
     *                  used as the value of the parameter.
     * @return the document that results from the transformation
     */

    public static DocumentInfo transform(XPathContext context, Templates templates,
                                         NodeInfo source, SequenceIterator params) throws XPathException {
        if (templates == null) {
            return null;
        }
        if (source == null) {
            return null;
        }
        try {
            Transformer transformer = templates.newTransformer();
            TinyBuilder builder = new TinyBuilder();
            builder.setPipelineConfiguration(context.getController().makePipelineConfiguration());
            while (true) {
                Item param = params.next();
                if (param == null) {
                    break;
                }
                if (param instanceof NodeInfo) {
                    switch (((NodeInfo)param).getNodeKind()) {
                    case Type.ELEMENT:
                    case Type.ATTRIBUTE:
                        setTransformerParameter(param, transformer, source);
                        break;
                    case Type.DOCUMENT:
                        AxisIterator kids = ((NodeInfo)param).iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);
                        while (true) {
                            NodeInfo kid = (NodeInfo)kids.next();
                            if (kid == null) {
                                break;
                            }
                            setTransformerParameter(kid, transformer, source);
                        }
                        break;
                    default:
                        throw new XPathException(
                                "Parameters passed to saxon:transform() must be element, attribute, or document nodes");
                    }

                } else {
                    throw new XPathException("Parameters passed to saxon:transform() must be nodes");
                }
            }
            transformer.transform(source, builder);
            return (DocumentInfo)builder.getCurrentRoot();
        } catch (TransformerException e) {
            throw XPathException.makeXPathException(e);
        }
    }

    private static void setTransformerParameter(Item param, Transformer transformer, NodeInfo source) throws XPathException {
        int fp = ((NodeInfo)param).getFingerprint();
        if (fp != -1) {
            Value val = ((NodeInfo)param).atomize();
            ((Controller)transformer).setParameter(
                    new StructuredQName(source.getNamePool(), fp), val);
        }
    }

    /**
     * Compile a string containing a source query
     * transformations
     *
     * @param context the XPath dynamic evaluation context
     * @param query   a string containing the query to be compiled
     * @return the compiled query
     */

    public static XQueryExpression compileQuery(XPathContext context, String query) throws XPathException {
        if (query == null) {
            return null;
        }
        StaticQueryContext sqc = new StaticQueryContext(context.getConfiguration());
        return sqc.compileQuery(query);

    }

    /**
     * Run a previously-compiled query. The initial context item for the query is taken from the context
     * in which the query is called (if there is one); no parameters are supplied
     *
     * @param context The dynamic context
     * @param query   The compiled query
     * @return the sequence representing the result of the query
     */

    public static SequenceIterator query(XPathContext context, XQueryExpression query)
            throws XPathException {
        if (query == null) {
            return null;
        }
        DynamicQueryContext dqc = new DynamicQueryContext(context.getConfiguration());
        Item c = context.getContextItem();
        if (c != null) {
            dqc.setContextItem(c);
        }
        return query.iterator(dqc);
    }


    /**
     * Run a previously-compiled query
     *
     * @param context The dynamic context
     * @param query   The compiled query
     * @param source  The initial context item for the query (may be null)
     * @return the sequence representing the result of the query
     */

    public static SequenceIterator query(XPathContext context, XQueryExpression query, Item source)
            throws XPathException {
        if (query == null) {
            return null;
        }
        DynamicQueryContext dqc = new DynamicQueryContext(context.getConfiguration());
        if (source != null) {
            dqc.setContextItem(source);
        }
        return query.iterator(dqc);
    }

    /**
     * Run a previously-compiled query, supplying parameters to the
     * transformation.
     *
     * @param context The dynamic context
     * @param query   The compiled query
     * @param source  The initial context node for the query (may be null)
     * @param params  A sequence of nodes (typically element nodes) supplying values of parameters.
     *                The name of the node should match the name of the parameter, the typed value of the node is
     *                used as the value of the parameter.
     * @return the results of the query (a sequence of items)
     */

    public static SequenceIterator query(XPathContext context, XQueryExpression query,
                                         Item source, SequenceIterator params) throws XPathException {
        if (query == null) {
            return null;
        }
        DynamicQueryContext dqc = new DynamicQueryContext(context.getConfiguration());
        if (source != null) {
            dqc.setContextItem(source);
        }
        NamePool pool = context.getConfiguration().getNamePool();
        while (true) {
            Item param = params.next();
            if (param == null) {
                break;
            }
            if (param instanceof NodeInfo) {
                switch (((NodeInfo)param).getNodeKind()) {
                case Type.ELEMENT:
                case Type.ATTRIBUTE:
                    Value val = ((NodeInfo)param).atomize();
                    dqc.setParameter(pool.getClarkName(((NodeInfo)param).getNameCode()), val);
                    break;
                case Type.DOCUMENT:
                    AxisIterator kids = ((NodeInfo)param).iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);
                    while (true) {
                        NodeInfo kid = (NodeInfo)kids.next();
                        if (kid == null) {
                            break;
                        }
                        Value val2 = ((NodeInfo)param).atomize();
                        dqc.setParameter(pool.getClarkName(kid.getNameCode()), val2);
                    }
                    break;
                default:
                    throw new XPathException(
                            "Parameters passed to saxon:query() must be element, attribute, or document nodes");
                }

            } else {
                throw new XPathException("Parameters passed to saxon:query() must be nodes");
            }
        }
        return query.iterator(dqc);
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
// Portions created by Zdenek Wagner [zdenek.wagner@gmail.com] are Copyright (C) Zdenek Wagner.
// All Rights Reserved.
//
// Contributor(s): none.
//
