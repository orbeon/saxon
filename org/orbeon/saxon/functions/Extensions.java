package org.orbeon.saxon.functions;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.TransformerFactoryImpl;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.query.QueryResult;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.charcode.UnicodeCharacterSet;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.GlobalOrderComparer;
import org.orbeon.saxon.sort.GenericAtomicComparer;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Properties;
import java.io.*;

/**
* This class implements functions that are supplied as standard with SAXON,
* but which are not defined in the XSLT or XPath specifications. <p>
*
* To invoke these functions, use a function call of the form prefix:name() where
* name is the method name, and prefix maps to a URI such as
* http://saxon.sf.net/org.orbeon.saxon.functions.Extensions (only the part
* of the URI after the last slash is important).
*/



public class Extensions  {

    // The class is never instantiated
    private Extensions() {}

    /**
    * Switch tracing off. Only works if tracing was enabled at compile time.
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
     * The function saxon:generate-id() is equivalent to the standard XSLT function generate-id().
     * It is provided as an extension function to make it available in non-XSLT environments, for example
     * in XQuery.
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
        if (node == null) {
            return -1;
        }
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
        if (doc == null) {
            return null;
        }
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

        if (e1 == null) {
            e1 = EmptyIterator.getInstance();
        }

        if (e2 == null) {
            e2 = EmptyIterator.getInstance();
        }

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
    * Total a stored expression over a set of nodes
    */

//    public static double sum (XPathContext context,
//                              SequenceIterator nsv,
//                              Evaluate.PreparedExpression pexpression) throws XPathException {
//
//        if (nsv == null) {
//            nsv = EmptyIterator.getInstance();
//        }
//        if (pexpression == null) {
//            return Double.NaN;
//        }
//        double total = 0.0;
//        XPathContext c = context.newMinorContext();
//        c.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
//        c.setCurrentIterator(nsv);
//        while (true) {
//            Item next = nsv.next();
//            if (next == null) break;
//            Item val = pexpression.expression.evaluateItem(c);
//            if (val instanceof NumericValue) {
//                DoubleValue v = (DoubleValue)((NumericValue)val).convert(Type.DOUBLE, context);
//                total += v.getDoubleValue();
//            } else {
//                DynamicError e = new DynamicError("expression in saxon:sum() must return numeric values");
//                e.setXPathContext(c);
//                throw e;
//            }
//        }
//        return total;
//    }

    /**
    * Get the maximum numeric value of a stored expression over a set of nodes
    */

//    public static double max (XPathContext context,
//                              SequenceIterator nsv,
//                              Evaluate.PreparedExpression pexpression) throws XPathException {
//
//        if (nsv == null) {
//            nsv = EmptyIterator.getInstance();
//        }
//        if (pexpression == null) {
//            return Double.NaN;
//        }
//
//        double max = Double.NEGATIVE_INFINITY;
//        XPathContext c = context.newMinorContext();
//        c.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
//        c.setCurrentIterator(nsv);
//        while (true) {
//            Item next = nsv.next();
//            if (next==null) break;
//            Item val = pexpression.expression.evaluateItem(c);
//            if (val instanceof NumericValue) {
//                DoubleValue v = (DoubleValue)((NumericValue)val).convert(Type.DOUBLE, context);
//                if (v.getDoubleValue()>max) max = v.getDoubleValue();
//            } else {
//                DynamicError e = new DynamicError("expression in saxon:max() must return numeric values");
//                e.setXPathContext(c);
//                throw e;
//            }
//        }
//        return max;
//    }

     /**
    * Get the minimum numeric value of a stored expression over a set of nodes
    */

//    public static double min (XPathContext context,
//                              SequenceIterator nsv,
//                              Evaluate.PreparedExpression pexpression) throws XPathException {
//
//        if (nsv == null) {
//            nsv = EmptyIterator.getInstance();
//        }
//        if (pexpression == null) {
//            return Double.NaN;
//        }
//
//        double min = Double.POSITIVE_INFINITY;
//        XPathContext c = context.newMinorContext();
//        c.setOriginatingConstructType(Location.SAXON_HIGHER_ORDER_EXTENSION_FUNCTION);
//        c.setCurrentIterator(nsv);
//        while (true) {
//            Item next = nsv.next();
//            if (next==null) break;
//            Item val = pexpression.expression.evaluateItem(c);
//            if (val instanceof NumericValue) {
//                DoubleValue v = (DoubleValue)((NumericValue)val).convert(Type.DOUBLE, context);
//                if (v.getDoubleValue()<min) min = v.getDoubleValue();
//            } else {
//                DynamicError e = new DynamicError("expression in saxon:min() must return numeric values");
//                e.setXPathContext(context);
//                throw e;
//            }
//        }
//        return min;
//    }

    /**
    * Get the node with maximum numeric value of the string-value of each of a set of nodes
    */

    public static Value highest (SequenceIterator nsv) throws XPathException {
        return org.orbeon.saxon.exslt.Math.highest(nsv);
    }


    /**
    * Get the maximum numeric value of a stored expression over a set of nodes
    */

    public static SequenceIterator highest (XPathContext context,
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
            if (next==null) break;
            Item val = pexpression.expression.evaluateItem(c);
            if (val instanceof NumericValue) {
                DoubleValue v = (DoubleValue)((NumericValue)val).convert(Type.DOUBLE, context);
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
        return org.orbeon.saxon.exslt.Math.lowest(nsv);
    }

    /**
    * Get the node with minimum numeric value of a stored expression over a set of nodes
    */

    public static SequenceIterator lowest (XPathContext context,
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
            if (next==null) break;
            Item val = pexpression.expression.evaluateItem(c);
            if (val instanceof NumericValue) {
                DoubleValue v = (DoubleValue)((NumericValue)val).convert(Type.DOUBLE, context);
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
    * (in sequence order) that doesn't satisfy the expression.
    */

    public static SequenceIterator leading (XPathContext context,
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
        if (delim == null) {
            return new StringTokenIterator(s);
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
        if (node == null) {
            return null;
        }
        int code = node.getTypeAnnotation();
        if ((code & NodeInfo.IS_DTD_TYPE) != 0) {
            code = StandardNames.XDT_UNTYPED_ATOMIC;
        }
        if (code == -1) {
            int nodeKind = node.getNodeKind();
            if (nodeKind == Type.ELEMENT || nodeKind == Type.DOCUMENT) {
                return "untyped";
            } else {
                return "untypedAtomic";
            }
        }
        SchemaType type = context.getConfiguration().getSchemaType(code & 0xfffff);
        if (type==null) {
            // Anonymous types are not accessible by the namecode
            return context.getNamePool().getDisplayName(code);
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
     * Return the Controller object
     */

    public static Controller getController(XPathContext c) {
        return c.getController();
    }

    /**
     * Return the Configuration object
     */

    public static Configuration getConfiguration(XPathContext c) {
        return c.getConfiguration();
    }

	/**
	* Get a pseudo-attribute of a processing instruction. Return an empty string
	* if the pseudo-attribute is not present.
	* Character references and built-in entity references are expanded
	*/

	public static String getPseudoAttribute(XPathContext c, String name)
	throws XPathException {
        if (name == null) {
            return null;
        }
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
    public static SecondsDurationValue dayTimeDurationFromSeconds(BigDecimal arg) throws XPathException {
        return SecondsDurationValue.fromSeconds(arg);
    }

    /**
    * Get a yearMonthDuration value corresponding to a given number of months
    */
    // no longer documented in Saxon 8.1
    public static MonthDurationValue yearMonthDurationFromMonths(int arg) {
        return MonthDurationValue.fromMonths((int)arg);
    }

    /**
     * Perform decimal division to a user-specified precision
     */

    public static BigDecimal decimalDivide(BigDecimal arg1, BigDecimal arg2, int scale) {
        if (arg1 == null || arg2 == null) {
            return null;
        }
        return arg1.divide(arg2, scale, BigDecimal.ROUND_DOWN);
    }


    /**
     * Get the UTF-8 encoding of a string
     * @param in the supplied string
     * @return a sequence of integers, each in the range 0-255, representing the octets of the UTF-8
     * encoding of the given string
     */

    public static List stringToUtf8(String in) {
        if (in == null) {
            return Collections.EMPTY_LIST;
        }
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
        if (in == null) {
            return null;
        }
        return new Base64BinaryValue(in);
    }

    /**
     * Convert a sequence of integers in the range 0-255, representing a sequence of octets,
     * to a hexBinary value
     */

    public static HexBinaryValue octetsToHexBinary(byte[] in) {
        if (in == null) {
            return null;
        }
        return new HexBinaryValue(in);
    }

    /**
     * Convert a base64Binary value to a sequence of integers representing the octets contained in the value
     */

    public static byte[] base64BinaryToOctets(Base64BinaryValue in) {
        if (in == null) {
            return null;
        }
        return in.getBinaryValue();
    }

    /**
     * Convert a hexBinary value to a sequence of integers representing the octets contained in the value
     */

    public static byte[] hexBinaryToOctets(HexBinaryValue in) {
        if (in == null) {
            return null;
        }
        return in.getBinaryValue();
    }

    /**
     * Convert a base64Binary value to a String, assuming a particular encoding
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
     */

    public static Base64BinaryValue stringToBase64Binary(String in, String encoding)
            throws UnsupportedEncodingException, IOException {
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
     * Check that bytes are valid XML characters (UTF-16 encoded)
     */

    private static void checkBytes(char[] array, int start, int end, NameChecker checker) throws XPathException {
        for (int c=start; c<end; c++) {
            int ch32 = array[c];
            if (XMLChar.isHighSurrogate(ch32)) {
                char low = array[c++];
                ch32 = XMLChar.supplemental((char)ch32, low);
            }
            if (!checker.isValidChar(ch32)) {
                DynamicError err = new DynamicError(
                        "The byte sequence contains a character not allowed by XML (hex " +
                        Integer.toHexString(ch32) + ')');
                err.setErrorCode("XTDE1180");
                throw err;
            }
        }
    }

    /**
     * Convert a string to a hexBinary value in a given encoding
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
     */

    public static boolean validCharacter(XPathContext c, int in) {
        return c.getConfiguration().getNameChecker().isValidChar(in);
    }


    /**
     * Create a parentless namespace node. This function is useful in XQuery when namespaces need to be created
     * dynamically. The effect is the same as that of the xsl:namespace instruction in XSLT.
     */

    public static NodeInfo namespaceNode(XPathContext context, String prefix, String uri) throws XPathException {
        if (prefix == null) {
            prefix = "";
        }
        if (!("".equals(prefix) || context.getConfiguration().getNameChecker().isValidNCName(prefix))) {
            DynamicError err = new DynamicError("Namespace prefix " + Err.wrap(prefix) + " is not a valid NCName");
            throw err;
        }
        if (uri==null || "".equals(uri)) {
            DynamicError err = new DynamicError("URI of namespace node must not be empty");
            throw err;
        }
        final NamePool namePool = context.getNamePool();
        Orphan node = new Orphan(context.getConfiguration());
        node.setNodeKind(Type.NAMESPACE);
        node.setNameCode(namePool.allocate("", "", prefix));
        node.setStringValue(uri);
        return node;
    }

    /**
     * Perform a parameterized deep-equals() test
     * @param context The evaluation context
     * @param arg1 The first sequence to be compared
     * @param arg2 The second sequence to be compared
     * @param collation The collation to be used (null if the default collation is to be used)
     * @param flags A string whose characters select options that cause the comparison to vary from the
     * standard fn:deep-equals() function. The flags are:
     * <ul>
     * <li>N - take namespace nodes into account</li>
     * <li>A - compare type annotations</li>
     * <li>C - take comments into account</li>
     * <li>F - take namespace prefixes into account</li>
     * <li>P - take processing instructions into account</li>
     * <li>S - compare string values, not typed values</li>
     * <li>w - don't take whitespace-only text nodes into account</li>
     * </ul>
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
                    new StreamResult(System.err), indent, context.getConfiguration());
            System.err.println("DeepEqual: second argument:");
            QueryResult.serialize(QueryResult.wrap(arg2.getAnother(), context.getConfiguration()),
                    new StreamResult(System.err), indent, context.getConfiguration());
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
     * Compile a document containing a stylesheet module into a stylesheet that can be used to perform
     * transformations
     */

    public static Templates compileStylesheet(XPathContext context, DocumentInfo doc) throws XPathException {
        if (doc == null) {
            return null;
        }
        try {
            TransformerFactoryImpl factory = new TransformerFactoryImpl(context.getConfiguration());
            Templates templates = factory.newTemplates(doc);
            return templates;
        } catch (TransformerConfigurationException e) {
            throw DynamicError.makeDynamicError(e);
        }

    }

    /**
     * Run a transformation to convert an input tree to an output document
     * @param context The dynamic context
     * @param templates The compiled stylesheet
     * @param source The initial context node representing the document to be transformed
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
            throw DynamicError.makeDynamicError(e);
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
