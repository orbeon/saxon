package net.sf.saxon.om;

import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ReversibleIterator;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.EscapeURI;
import net.sf.saxon.pattern.*;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceExtent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


/**
 * The Navigator class provides helper classes for navigating a tree, irrespective
 * of its implementation
 *
 * @author Michael H. Kay
 */



public final class Navigator {

    // Class is never instantiated
    private Navigator() {
    }

    /**
     * Determine if a string is all-whitespace
     *
     * @param content the string to be tested
     * @return true if the supplied string contains no non-whitespace
     *     characters
     */

    public static final boolean isWhite(CharSequence content) {
        for (int i=0; i<content.length();) {
            // all valid XML 1.0 whitespace characters, and only whitespace characters, are <= 0x20
            // TODO: needs updating to support XML 1.1
            if (content.charAt(i++) > 32) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the string value of an attribute of a given element, given the URI and
     * local part of the attribute name.
     * @return the attribute value, or null if the attribute is not present
     */

    public static String getAttributeValue(NodeInfo element, String uri, String localName) {
        int fingerprint = element.getNamePool().allocate("", uri, localName);
        return element.getAttributeValue(fingerprint);
    }

    /**
     * Helper method to get the base URI of an element node
     */

    public static String getBaseURI(NodeInfo element) {
        String xmlBase = element.getAttributeValue(StandardNames.XML_BASE);
        if (xmlBase!=null) {
            String escaped = EscapeURI.escape(xmlBase, false).toString();
            URI escapedURI;
            try {
                escapedURI = new URI(escaped);
                if (!escapedURI.isAbsolute()) {
                    NodeInfo parent = element.getParent();
                    if (parent==null) {
                        return escapedURI.toString();
                    }
                    String startSystemId = element.getSystemId();
                    String parentSystemId = parent.getSystemId();
                    if (startSystemId.equals(parentSystemId)) {
                        URI base = new URI(element.getParent().getBaseURI());
                        escapedURI = base.resolve(escapedURI);
                    } else {
                        URI base = new URI(startSystemId);
                        escapedURI = base.resolve(escapedURI);
                    }
                }
            } catch (URISyntaxException e) {
                // xml:base is an invalid URI. Just return it as is: the operation that needs the base URI
                // will probably fail as a result.
                return xmlBase;
            }
            return escapedURI.toString();
        }
        String startSystemId = element.getSystemId();
        NodeInfo parent = element.getParent();
        if (parent==null) {
            return startSystemId;
        }
        String parentSystemId = parent.getSystemId();
        if (startSystemId.equals(parentSystemId)) {
            return parent.getBaseURI();
        } else {
            return startSystemId;
        }
    }

    /**
     * Output all namespace nodes associated with this element. Does nothing if
     * the node is not an element. This is a helper method to allow the method
     * {@link NodeInfo#sendNamespaceDeclarations(net.sf.saxon.event.Receiver, boolean)} to be
     * implemented if {@link NodeInfo#getDeclaredNamespaces(int[])} is available.
     * @param out The relevant outputter
     * @param includeAncestors True if namespaces declared on ancestor elements must be output
     */

    public static void sendNamespaceDeclarations(NodeInfo node, Receiver out, boolean includeAncestors)
        throws XPathException {
        if (node.getNodeKind() == Type.ELEMENT) {
            int[] codes;
            if (includeAncestors) {
                codes = new NamespaceIterator(node, null).getInScopeNamespaceCodes();
            } else {
                codes = node.getDeclaredNamespaces(NodeInfo.EMPTY_NAMESPACE_LIST);
            }
            for (int i=0; i<codes.length; i++) {
                if (codes[i] == -1) break;
                out.namespace(codes[i], 0);
            }
        }
    }

    /**
     * Get an absolute XPath expression that identifies a given node within its document
     *
     * @param node the node whose path is required. If null is supplied,
     * an empty string is returned - this fact is used in making a recursive call
     * for a parentless node.
     * @return a path expression that can be used to retrieve the node
     */

    public static String getPath(NodeInfo node) {
        if (node==null) {
            return "";
        }
        String pre;
        NodeInfo parent = node.getParent();
        // System.err.println("node = " + node + " parent = " + parent);

        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                return "/";
            case Type.ELEMENT:
                if (parent==null) {
                    return node.getDisplayName();
                } else {
                    pre = getPath(parent);
                    if (pre.equals("/")) {
                        return '/' + node.getDisplayName();
                    } else {
                        return pre + '/' + node.getDisplayName() + '[' + getNumberSimple(node) + ']';
                    }
                }
            case Type.ATTRIBUTE:
                return getPath(parent) + "/@" + node.getDisplayName();
            case Type.TEXT:
                pre = getPath(parent);
                return (pre.equals("/") ? "" : pre) +
                        "/text()[" + getNumberSimple(node) + ']';
            case Type.COMMENT:
                pre = getPath(parent);
                return (pre.equals("/") ? "" : pre) +
                    "/comment()[" + getNumberSimple(node) + ']';
            case Type.PROCESSING_INSTRUCTION:
                pre = getPath(parent);
                return (pre.equals("/") ? "" : pre) +
                    "/processing-instruction()[" + getNumberSimple(node) + ']';
            case Type.NAMESPACE:
                String test = node.getLocalPart();
                if (test.equals("")) {
                    // default namespace: need a node-test that selects unnamed nodes only
                    test="*[not(local-name()]";
                }
                return getPath(parent)+ "/namespace::" + test;
            default:
                return "";
        }
    }

    /**
     * Get simple node number. This is defined as one plus the number of previous siblings of the
     * same node type and name. It is not accessible directly in XSL.
     *
     * @param node The node whose number is required
     * @param context Used for remembering previous result, for
     *     performance
     * @exception XPathException if any error occurs
     * @return the node number, as defined above
     */

    public static int getNumberSimple(NodeInfo node, XPathContext context) throws XPathException {

        //checkNumberable(node);

        int fingerprint = node.getFingerprint();
        NodeTest same;

        if (fingerprint==-1) {
            same = NodeKindTest.makeNodeKindTest(node.getNodeKind());
        } else {
            same = new NameTest(node);
        }

        SequenceIterator preceding = node.iterateAxis(Axis.PRECEDING_SIBLING, same);

        int i=1;
        while (true) {
            NodeInfo prev = (NodeInfo)preceding.next();
            if (prev == null) {
                break;
            }

            Controller controller = context.getController();
            int memo = controller.getRememberedNumber(prev);
            if (memo>0) {
                memo += i;
                controller.setRememberedNumber(node, memo);
                return memo;
            }

            i++;
        }

        context.getController().setRememberedNumber(node, i);
        return i;
    }

    /**
     * Get simple node number. This is defined as one plus the number of previous siblings of the
     * same node type and name. It is not accessible directly in XSL. This version doesn't require
     * the controller, and therefore doesn't remember previous results. It is used only by getPath().
     *
     * @param node the node whose number is required
     * @return the node number, as defined above
     */

    private static int getNumberSimple(NodeInfo node) {

        int fingerprint = node.getFingerprint();
        NodeTest same;

        if (fingerprint==-1) {
            same = NodeKindTest.makeNodeKindTest(node.getNodeKind());
        } else {
            same = new NameTest(node);
        }

        AxisIterator preceding = node.iterateAxis(Axis.PRECEDING_SIBLING, same);

        int i=1;
        while (preceding.next() != null) {
            i++;
        }

        return i;
    }

    /**
     * Get node number (level="single"). If the current node matches the supplied pattern, the returned
     * number is one plus the number of previous siblings that match the pattern. Otherwise,
     * return the element number of the nearest ancestor that matches the supplied pattern.
     *
     * @param node the current node, the one whose node number is required
     * @param count Pattern that identifies which nodes should be
     *     counted. Default (null) is the element name if the current node is
     *      an element, or "node()" otherwise.
     * @param from Pattern that specifies where counting starts from.
     *     Default (null) is the root node. (This parameter does not seem
     *     useful but is included for the sake of XSLT conformance.)
     * @param context the dynamic context of the transformation, used if
     *     the patterns reference context values (e.g. variables)
     * @exception XPathException when any error occurs in processing
     * @return the node number established as follows: go to the nearest
     *     ancestor-or-self that matches the 'count' pattern and that is a
     *     descendant of the nearest ancestor that matches the 'from' pattern.
     *      Return one plus the nunber of preceding siblings of that ancestor
     *      that match the 'count' pattern. If there is no such ancestor,
     *     return 0.
     */

    public static int getNumberSingle(NodeInfo node, Pattern count,
                    Pattern from, XPathContext context) throws XPathException {

//        checkNumberable(node);

        if (count==null && from==null) {
            return getNumberSimple(node, context);
        }

        boolean knownToMatch = false;
        if (count==null) {
            if (node.getFingerprint()==-1) {	// unnamed node
                count = new NodeTestPattern(NodeKindTest.makeNodeKindTest(node.getNodeKind()));
            } else {
                count = new NodeTestPattern(new NameTest(node));
            }
            knownToMatch = true;
        }

        NodeInfo target = node;
        while (!(knownToMatch || count.matches(target, context))) {
            target = target.getParent();
            if (target==null) {
                return 0;
            }
            if (from!=null && from.matches(target, context)) {
                return 0;
            }
        }

        // we've found the ancestor to count from

        SequenceIterator preceding =
            target.iterateAxis(Axis.PRECEDING_SIBLING, count.getNodeTest());
                        // pass the filter condition down to the axis enumeration where possible
        boolean alreadyChecked = (count instanceof NodeTestPattern);
        int i = 1;
        while (true) {
            NodeInfo p = (NodeInfo)preceding.next();
            if (p == null) {
                return i;
            }
            if (alreadyChecked || count.matches(p, context)) {
                i++;
            }
        }
    }

    /**
     * Get node number (level="any").
     * Return one plus the number of previous nodes in the
     * document that match the supplied pattern
     *
     * @exception net.sf.saxon.trans.XPathException
     * @param inst Identifies the xsl:number expression; this is relevant
     *     when the function is memoised to support repeated use of the same
     *     instruction to number multiple nodes
     * @param node The node being numbered
     * @param count Pattern that identifies which nodes should be
     *     counted. Default (null) is the element name if the current node is
     *      an element, or "node()" otherwise.
     * @param from Pattern that specifies where counting starts from.
     *     Default (null) is the root node. Only nodes after the first (most
     *     recent) node that matches the 'from' pattern are counted.
     * @param context The dynamic context for the transformation
     * @param hasVariablesInPatterns if the count or from patterns
     *     contain variables, then it's not safe to get the answer by adding
     *     one to the number of the most recent node that matches
     * @return one plus the number of nodes that precede the current node,
     *     that match the count pattern, and that follow the first node that
     *     matches the from pattern if specified.
     */

    public static int getNumberAny(Expression inst, NodeInfo node, Pattern count,
                    Pattern from, XPathContext context, boolean hasVariablesInPatterns) throws XPathException {

        NodeInfo memoNode = null;
        int memoNumber = 0;
        Controller controller = context.getController();
        boolean memoise = (!hasVariablesInPatterns && count!=null);
        if (memoise) {
            Object[] memo = (Object[])controller.getUserData(inst, "xsl:number");
            if (memo != null) {
                memoNode = (NodeInfo)memo[0];
                memoNumber = ((Integer)memo[1]).intValue();
            }
        }

        int num = 0;
        if (count==null) {
            if (node.getFingerprint()==-1) {	// unnamed node
                count = new NodeTestPattern(NodeKindTest.makeNodeKindTest(node.getNodeKind()));
            } else {
                count = new NodeTestPattern(new NameTest(node));
            }
            num = 1;
        } else if (count.matches(node, context)) {
            num = 1;
        }

        // We use a special axis invented for the purpose: the union of the preceding and
        // ancestor axes, but in reverse document order

        // Pass part of the filtering down to the axis iterator if possible
        NodeTest filter;
        if (from==null) {
            filter = count.getNodeTest();
        } else if (from.getNodeKind()==Type.ELEMENT && count.getNodeKind()==Type.ELEMENT) {
            filter = NodeKindTest.ELEMENT;
        } else {
            filter = AnyNodeTest.getInstance();
        }

        SequenceIterator preceding =
            node.iterateAxis(Axis.PRECEDING_OR_ANCESTOR, filter);

        while (true) {
            NodeInfo prev = (NodeInfo)preceding.next();
            if (prev == null) {
                break;
            }
            if (from!=null && from.matches(prev, context)) {
                return num;
            }
            if (count.matches(prev, context)) {
                if (num==1 && memoNode!=null && prev.isSameNodeInfo(memoNode)) {
                    num = memoNumber + 1;
                    break;
                }
                num++;
            }
        }
        if (memoise) {
            Object[] memo = new Object[2];
            memo[0] = node;
            memo[1] = new Integer(num);
            controller.setUserData(inst, "xsl:number", memo);
        }
        return num;
    }

    /**
     * Get node number (level="multiple").
     * Return a vector giving the hierarchic position of this node. See the XSLT spec for details.
     *
     * @exception XPathException
     * @param node The node to be numbered
     * @param count Pattern that identifies which nodes (ancestors and
     *      their previous siblings) should be counted. Default (null) is the
     *      element name if the current node is an element, or "node()"
     *     otherwise.
     * @param from Pattern that specifies where counting starts from.
     *     Default (null) is the root node. Only nodes below the first (most
     *     recent) node that matches the 'from' pattern are counted.
     * @param context The dynamic context for the transformation
     * @return a vector containing for each ancestor-or-self that matches the
     *      count pattern and that is below the nearest node that matches the
     *      from pattern, an Integer which is one greater than the number of
     *     previous siblings that match the count pattern.
     */

    public static List getNumberMulti(NodeInfo node, Pattern count,
                    Pattern from, XPathContext context) throws XPathException {

        //checkNumberable(node);

        ArrayList v = new ArrayList(5);

        if (count==null) {
            if (node.getFingerprint()==-1) {    // unnamed node
                count = new NodeTestPattern(NodeKindTest.makeNodeKindTest(node.getNodeKind()));
            } else {
                count = new NodeTestPattern(new NameTest(node));
            }
        }

        NodeInfo curr = node;

        while(true) {
            if (count.matches(curr, context)) {
                int num = getNumberSingle(curr, count, null, context);
                v.add(0, new Long(num));
            }
            curr = curr.getParent();
            if (curr==null) break;
            if (from!=null && from.matches(curr, context)) break;
        }

        return v;
    }

     /**
     * Generic (model-independent) implementation of deep copy algorithm for nodes.
     * This is available for use by any node implementations that choose to use it.
      * @param node The node to be copied
      * @param out The receiver to which events will be sent
      * @param namePool Namepool holding the name codes (used only to resolve namespace
      *          codes)
      * @param whichNamespaces Indicates which namespace nodes for an element should
      *          be copied
      * @param copyAnnotations Indicates whether type annotations should be copied
      * @throws XPathException on any failure reported by the Receiver
     */

    public static void copy(NodeInfo node,
                            Receiver out,
                            NamePool namePool,
                            int whichNamespaces,
                            boolean copyAnnotations, int locationId) throws XPathException {

        switch (node.getNodeKind()) {
            case Type.DOCUMENT: {
                out.startDocument(0);
                AxisIterator children0 = node.iterateAxis(Axis.CHILD, new AnyNodeTest());
                while (true) {
                    NodeInfo child = (NodeInfo)children0.next();
                    if (child == null) {
                        break;
                    }
                    child.copy(out, whichNamespaces, copyAnnotations, locationId);
                }
                out.endDocument();
                break;
            }
            case Type.ELEMENT: {
                out.startElement(node.getNameCode(),
                        copyAnnotations? node.getTypeAnnotation(): -1,
                        0, 0);

                // output the namespaces

                if (whichNamespaces != NodeInfo.NO_NAMESPACES) {
                    node.sendNamespaceDeclarations(out, true);
                }

                // output the attributes

                AxisIterator attributes = node.iterateAxis(Axis.ATTRIBUTE, new AnyNodeTest());
                while (true) {
                    NodeInfo att = (NodeInfo)attributes.next();
                    if (att == null) {
                        break;
                    }
                    att.copy(out, whichNamespaces, copyAnnotations, locationId);
                }

                // notify the start of content

                out.startContent();

                // output the children

                AxisIterator children = node.iterateAxis(Axis.CHILD, new AnyNodeTest());
                while (true) {
                    NodeInfo child = (NodeInfo)children.next();
                    if (child == null) {
                        break;
                    }
                    child.copy(out, whichNamespaces, copyAnnotations, locationId);
                }

                // finally the end tag

                out.endElement();
                return;
            }
            case Type.ATTRIBUTE: {
                out.attribute(node.getNameCode(),
                              copyAnnotations? node.getTypeAnnotation(): -1,
                              node.getStringValueCS(), 0, 0);
                return;
            }
            case Type.TEXT: {
                out.characters(node.getStringValueCS(), 0, 0);
                return;
            }
            case Type.COMMENT: {
                out.comment(node.getStringValueCS(), 0, 0);
                return;
            }
            case Type.PROCESSING_INSTRUCTION: {
                out.processingInstruction(node.getLocalPart(), node.getStringValueCS(), 0, 0);
                return;
            }
            case Type.NAMESPACE: {
                out.namespace(namePool.allocateNamespaceCode(node.getLocalPart(), node.getStringValue()),0);
                return;
            }
            default:

        }
    }

    /**
    * Generic (model-independent) method to determine the relative position of two
    * node in document order. The nodes must be in the same tree.
    * @param first The first node
    * @param second The second node, whose position is to be compared with the first node
    * @return -1 if this node precedes the other node, +1 if it follows the other
    * node, or 0 if they are the same node. (In this case, isSameNode() will always
    * return true, and the two nodes will produce the same result for generateId())
    */

    public static int compareOrder(SiblingCountingNode first, SiblingCountingNode second) {
        NodeInfo ow = second;

        // are they the same node?
        if (first.isSameNodeInfo(second)) {
            return 0;
        }

        NodeInfo firstParent = first.getParent();
        if (firstParent == null) {
            // first node is the root
            return -1;
        }

        NodeInfo secondParent = second.getParent();
        if (secondParent == null) {
            // second node is the root
            return +1;
        }

        // do they have the same parent (common case)?
        if (firstParent.isSameNodeInfo(secondParent)) {
            int cat1 = nodeCategories[first.getNodeKind()];
            int cat2 = nodeCategories[second.getNodeKind()];
            if (cat1 == cat2) {
                return first.getSiblingPosition() - second.getSiblingPosition();
            } else {
                return cat1 - cat2;
            }
        }

        // find the depths of both nodes in the tree
        int depth1 = 0;
        int depth2 = 0;
        NodeInfo p1 = first;
        NodeInfo p2 = second;
        while (p1 != null) {
            depth1++;
            p1 = p1.getParent();
        }
        while (p2 != null) {
            depth2++;
            p2 = p2.getParent();
        }
        // move up one branch of the tree so we have two nodes on the same level

        p1 = first;
        while (depth1>depth2) {
            p1 = p1.getParent();
            if (p1.isSameNodeInfo(second)) {
                return +1;
            }
            depth1--;
        }

        p2 = ow;
        while (depth2>depth1) {
            p2 = p2.getParent();
            if (p2.isSameNodeInfo(first)) {
                return -1;
            }
            depth2--;
        }

        // now move up both branches in sync until we find a common parent
        while (true) {
            NodeInfo par1 = p1.getParent();
            NodeInfo par2 = p2.getParent();
            if (par1==null || par2==null) {
                throw new NullPointerException("DOM/JDOM tree compare - internal error");
            }
            if (par1.isSameNodeInfo(par2)) {
                return ((SiblingCountingNode)p1).getSiblingPosition() -
                        ((SiblingCountingNode)p2).getSiblingPosition();
            }
            p1 = par1;
            p2 = par2;
        }
    }

    /**
     * Classify node kinds into categories for sorting into document order:
     * 0 = document, 1 = namespace, 2 = attribute, 3 = (element, text, comment, pi)
     */

    private static int[] nodeCategories = {
        -1, //0 = not used
        3,  //1 = element
        2,  //2 = attribute
        3,  //3 = text
        -1, -1, -1, //4,5,6 = not used
        3,  //7 = processing-instruction
        3,  //8 = comment
        0,  //9 = document
        -1, -1, -1, //10,11,12 = not used
        1   //13 = namespace
    };

    /**
    * Get a character string that uniquely identifies this node and that collates nodes
    * into document order
    * @return a string. The string is always interned so keys can be compared using "==".
    */

    public static String getSequentialKey(SiblingCountingNode node) {
        // This was designed so it could be used for sorting nodes into document
        // order, but is not currently used that way.
        StringBuffer key = new StringBuffer(12);
        int num = node.getDocumentNumber();
        while(node != null && !(node instanceof DocumentInfo)) {
            key.insert(0, alphaKey(node.getSiblingPosition()));
            node = (SiblingCountingNode)node.getParent();
        }
        key.insert(0, "w" + num);
        return key.toString().intern();
    }

    /**
    * Construct an alphabetic key from an positive integer; the key collates in the same sequence
    * as the integer
    * @param value The positive integer key value (negative values are treated as zero).
    */

    public static String alphaKey(int value) {
        if (value<1) return "a";
        if (value<10) return "b" + value;
        if (value<100) return "c" + value;
        if (value<1000) return "d" + value;
        if (value<10000) return "e" + value;
        if (value<100000) return "f" + value;
        if (value<1000000) return "g" + value;
        if (value<10000000) return "h" + value;
        if (value<100000000) return "i" + value;
        if (value<1000000000) return "j" + value;
        return "k" + value;
    }


    ///////////////////////////////////////////////////////////////////////////////
    // Helper classes to support axis iteration
    ///////////////////////////////////////////////////////////////////////////////

    /**
     * AxisFilter is an iterator that applies a NodeTest filter to
     * the nodes returned by an underlying AxisIterator.
     */

    public static class AxisFilter extends AxisIteratorImpl {
        private AxisIterator base;
        private NodeTest nodeTest;
        //private int last = -1;

        /**                             S
         * Construct a AxisFilter
         * @param base the underlying iterator that returns all the nodes on
         * a required axis. This must not be an atomizing iterator!
         * @param test a NodeTest that is applied to each node returned by the
         * underlying AxisIterator; only those nodes that pass the NodeTest are
         * returned by the AxisFilter
         */

        public AxisFilter(AxisIterator base, NodeTest test) {
            this.base = base;
            this.nodeTest = test;
            position = 0;
        }

    	public Item next() {
            while (true) {
    	        current = base.next();
                if (current == null) {
                    return null;
                }
                if (nodeTest.matches((NodeInfo)current)) {
                    position++;
                    return current;
                }
    	    }
        }

    	public SequenceIterator getAnother() {
    	    return new AxisFilter((AxisIterator)base.getAnother(), nodeTest);
    	}
	}

    /**
     * BaseEnumeration is an abstract implementation of an AxisIterator, it
     * simplifies the implementation of the underlying AxisIterator by requiring
     * it to provide only two methods: advance(), and getAnother()
     */

    public static abstract class BaseEnumeration extends AxisIteratorImpl {

        public final Item next() {
            advance();
            return current;
        }

        /**
         * The advance() method must be provided in each concrete implementation.
         * It must leave the variable current set to the next node to be returned in the
         * iteration, or to null if there are no more nodes to be returned.
         */

        public abstract void advance();

	    public abstract SequenceIterator getAnother();

	}

    /**
     * General-purpose implementation of the ancestor and ancestor-or-self axes
     */

    public static final class AncestorEnumeration extends BaseEnumeration {

        private boolean includeSelf;
        private boolean atStart;
        private NodeInfo start;

        public AncestorEnumeration(NodeInfo start, boolean includeSelf) {
            this.start = start;
            this.includeSelf = includeSelf;
            this.current = start;
            atStart = true;
        }

        public void advance() {
            if (atStart) {
                atStart = false;
                if (includeSelf) {
                    return;
                }
            }
            current = ((NodeInfo)current).getParent();
        }

        public SequenceIterator getAnother() {
            return new AncestorEnumeration(start, includeSelf);
        }

    } // end of class AncestorEnumeration

    /**
     * General-purpose implementation of the descendant and descendant-or-self axes,
     * in terms of the child axis.
    * But it also has the option to return the descendants in reverse document order;
    * this is used when evaluating the preceding axis. Note that the includeSelf option
    * should not be used when scanning in reverse order, as the self node will always be
    * returned first.
    */

    public static final class DescendantEnumeration extends BaseEnumeration {

        private AxisIterator children = null;
        private AxisIterator descendants = null;
        private NodeInfo start;
        private boolean includeSelf;
        private boolean forwards;
        private boolean atEnd = false;

        public DescendantEnumeration(NodeInfo start,
                                 boolean includeSelf, boolean forwards) {
            this.start = start;
            this.includeSelf = includeSelf;
            this.forwards = forwards;
        }

        public void advance() {
            if (descendants!=null) {
                Item nextd = descendants.next();
                if (nextd != null) {
                    current = nextd;
                    return;
                } else {
                    descendants = null;
                }
            }
            if (children!=null) {
                NodeInfo n = (NodeInfo)children.next();
                if (n != null) {
                    if (n.hasChildNodes()) {
                        if (forwards) {
                            descendants = new DescendantEnumeration(n,
                                             false, forwards);
                            current = n;
                        } else {
                            descendants = new DescendantEnumeration(n, true, forwards);
                            advance();
                        }
                    } else {
                        current = n;
                    }
                } else {
                    if (forwards || !includeSelf) {
                        current = null;
                    } else {
                        atEnd = true;
                        children = null;
                        current = start;
                    }
                }
            } else if (atEnd) {
                // we're just finishing a backwards scan
                current = null;
            } else {
                // we're just starting...
                if (start.hasChildNodes()) {
                    //children = new NodeWrapper.ChildEnumeration(start, true, forwards);
                    children = start.iterateAxis(Axis.CHILD);
                    if (!forwards) {
                        if (children instanceof ReversibleIterator) {
                            children = (AxisIterator)((ReversibleIterator)children).getReverseIterator();
                        } else {
                            try {
                                children = new SequenceExtent(start.iterateAxis(Axis.CHILD)).reverseIterate();
                            } catch (XPathException e) {
                                throw new AssertionError(
                                        "Internal error in Navigator#descendantEnumeration: " + e.getMessage());
                                // shouldn't happen.
                            }
                        }
                    }
                } else {
                    children = EmptyIterator.getInstance();
                }
                if (forwards && includeSelf) {
                    current = start;
                } else {
                    advance();
                }
            }
        }

        public SequenceIterator getAnother() {
            return new DescendantEnumeration(start, includeSelf, forwards);
        }

    } // end of class DescendantEnumeration

    /**
     * General purpose implementation of the following axis, in terms of the
     * ancestor, child, and following-sibling axes
     */

    public static final class FollowingEnumeration extends BaseEnumeration {
        private NodeInfo start;
        private AxisIterator ancestorEnum = null;
        private AxisIterator siblingEnum = null;
        private AxisIterator descendEnum = null;

        public FollowingEnumeration(NodeInfo start) {
            this.start = start;
            ancestorEnum = new AncestorEnumeration(start, false);
            switch (start.getNodeKind()) {
                case Type.ELEMENT:
                case Type.TEXT:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                    //siblingEnum = new NodeWrapper.ChildEnumeration(start, false, true);
                        // gets following siblings
                    siblingEnum = start.iterateAxis(Axis.FOLLOWING_SIBLING);
                    break;
                case Type.ATTRIBUTE:
                case Type.NAMESPACE:
                    //siblingEnum = new NodeWrapper.ChildEnumeration((NodeWrapper)start.getParent(), true, true);
                        // gets children of the attribute's parent node
                    NodeInfo parent = start.getParent();
                    if (parent == null) {
                        siblingEnum = EmptyIterator.getInstance();
                    } else {
                        siblingEnum = parent.iterateAxis(Axis.CHILD);
                    }
                    break;
                default:
                    siblingEnum = EmptyIterator.getInstance();
            }
            //advance();
        }

        public void advance() {
            if (descendEnum!=null) {
                Item nextd = descendEnum.next();
                if (nextd != null) {
                    current = nextd;
                    return;
                } else {
                    descendEnum = null;
                }
            }
            if (siblingEnum!=null) {
                Item nexts = siblingEnum.next();
                if (nexts != null) {
                    current = nexts;
                    NodeInfo n = (NodeInfo)current;
                    if (n.hasChildNodes()) {
                        descendEnum = new DescendantEnumeration(n, false, true);
                    } else {
                        descendEnum = null;
                    }
                    return;
                } else {
                    descendEnum = null;
                    siblingEnum = null;
                }
            }
            Item nexta = ancestorEnum.next();
            if (nexta != null) {
                current = nexta;
                NodeInfo n = (NodeInfo)current;
                if (n.getNodeKind() == Type.DOCUMENT) {
                    siblingEnum = EmptyIterator.getInstance();
                } else {
                    //siblingEnum = new NodeWrapper.ChildEnumeration(next, false, true);
                    siblingEnum = n.iterateAxis(Axis.FOLLOWING_SIBLING);
                }
                advance();
            } else {
                current = null;
            }
        }

        public SequenceIterator getAnother() {
            return new FollowingEnumeration(start);
        }

    } // end of class FollowingEnumeration

    public static final class PrecedingEnumeration extends BaseEnumeration {

        private NodeInfo start;
        private AxisIterator ancestorEnum = null;
        private AxisIterator siblingEnum = null;
        private AxisIterator descendEnum = null;
        private boolean includeAncestors;

        public PrecedingEnumeration(NodeInfo start, boolean includeAncestors) {
            this.start = start;
            this.includeAncestors = includeAncestors;
            ancestorEnum = new AncestorEnumeration(start, false);
            switch (start.getNodeKind()) {
                case Type.ELEMENT:
                case Type.TEXT:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                    // get preceding-sibling enumeration
                    //siblingEnum = new NodeWrapper.ChildEnumeration(start, false, false);
                    siblingEnum = start.iterateAxis(Axis.PRECEDING_SIBLING);
                    break;
                default:
                    siblingEnum = EmptyIterator.getInstance();
            }
            //advance();
        }

        public void advance() {
            if (descendEnum!=null) {
                Item nextd = descendEnum.next();
                if (nextd != null) {
                    current = nextd;
                    return;
                } else {
                    descendEnum = null;
                }
            }
            if (siblingEnum!=null) {
                Item nexts = siblingEnum.next();
                if (nexts != null) {
                    NodeInfo sib = (NodeInfo)nexts;
                    if (sib.hasChildNodes()) {
                        descendEnum = new DescendantEnumeration(sib, true, false);
                        advance();
                    } else {
                        descendEnum = null;
                        current = sib;
                    }
                    return;
                } else {
                    descendEnum = null;
                    siblingEnum = null;
                }
            }
            Item nexta = ancestorEnum.next();
            if (nexta != null) {
                current = nexta;
                NodeInfo n = (NodeInfo)current;
                if (n.getNodeKind() == Type.DOCUMENT) {
                    siblingEnum = EmptyIterator.getInstance();
                } else {
                    siblingEnum = n.iterateAxis(Axis.PRECEDING_SIBLING);
                }
                if (!includeAncestors) {
                    advance();
                }
            } else {
                current = null;
            }
        }

        public SequenceIterator getAnother() {
            return new PrecedingEnumeration(start, includeAncestors);
        }

    } // end of class PrecedingEnumeration


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
