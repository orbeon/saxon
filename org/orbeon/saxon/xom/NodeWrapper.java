package org.orbeon.saxon.xom;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.xpath.XPathException;
import nu.xom.*;

import java.util.HashMap;
import java.util.Iterator;

/**
 * A node in the XML parse tree representing an XML element, character content,
 * or attribute.
 * <P>
 * This is the implementation of the NodeInfo interface used as a wrapper for
 * XOM nodes.
 *
 * @author Michael H. Kay
 * @author Wolfgang Hoschek (ported org.orbeon.saxon.jdom to XOM)
 */

public class NodeWrapper implements NodeInfo, VirtualNode, SiblingCountingNode {

	protected Object node;

	protected short nodeKind;

	private NodeWrapper parent; // null means unknown

	protected DocumentWrapper docWrapper;

	protected int index; // -1 means unknown

	/**
	 * This constructor is protected: nodes should be created using the wrap
	 * factory method on the DocumentWrapper class
	 *
	 * @param node
	 *            The XOM node to be wrapped
	 * @param parent
	 *            The NodeWrapper that wraps the parent of this node
	 * @param index
	 *            Position of this node among its siblings
	 */
	protected NodeWrapper(Object node, NodeWrapper parent, int index) {
		this.node = node;
		this.parent = parent;
		this.index = index;
	}

	/**
	 * Factory method to wrap a XOM node with a wrapper that implements the
	 * Saxon NodeInfo interface.
	 *
	 * @param node
	 *            The XOM node
	 * @param docWrapper
	 *            The wrapper for the Document containing this node
	 * @return The new wrapper for the supplied node
	 */
	protected final NodeWrapper makeWrapper(Object node, DocumentWrapper docWrapper) {
		return makeWrapper(node, docWrapper, null, -1);
	}

	/**
	 * Factory method to wrap a XOM node with a wrapper that implements the
	 * Saxon NodeInfo interface.
	 *
	 * @param node
	 *            The XOM node
	 * @param docWrapper
	 *            The wrapper for the Document containing this node
	 * @param parent
	 *            The wrapper for the parent of the XOM node
	 * @param index
	 *            The position of this node relative to its siblings
	 * @return The new wrapper for the supplied node
	 */

	protected final NodeWrapper makeWrapper(Object node, DocumentWrapper docWrapper,
			NodeWrapper parent, int index) {

		short kind;
		if (node instanceof Element) {
			kind = Type.ELEMENT;
		} else if (node instanceof Attribute) {
			kind = Type.ATTRIBUTE;
		} else if (node instanceof Text) {
			kind = Type.TEXT;
		} else if (node instanceof Namespace) {
			kind = Type.NAMESPACE;
		} else if (node instanceof Comment) {
			kind = Type.COMMENT;
		} else if (node instanceof ProcessingInstruction) {
			kind = Type.PROCESSING_INSTRUCTION;
		} else if (node instanceof Document) {
			return docWrapper;
		} else {
			throw new IllegalArgumentException("Bad node type in XOM! "
					+ node.getClass() + " instance " + node.toString());
		}

		NodeWrapper wrapper = new NodeWrapper(node, parent, index);
		wrapper.nodeKind = kind;
		wrapper.docWrapper = docWrapper;
		return wrapper;
	}

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return docWrapper.getConfiguration();
    }

	/**
	 * Get the underlying XOM node, to implement the VirtualNode interface
	 */

	public Object getUnderlyingNode() {
		return node;
	}

	/**
	 * Get the name pool for this node
	 *
	 * @return the NamePool
	 */

	public NamePool getNamePool() {
		return docWrapper.getNamePool();
	}

	/**
	 * Return the type of node.
	 *
	 * @return one of the values Node.ELEMENT, Node.TEXT, Node.ATTRIBUTE, etc.
	 */

	public int getNodeKind() {
		return nodeKind;
	}

	/**
	 * Get the typed value of the item
	 */

	public SequenceIterator getTypedValue() {
		return SingletonIterator.makeIterator(new UntypedAtomicValue(
				getStringValue()));
	}

    /**
	 * Get the type annotation of this node, if any. Returns -1 for kinds of
	 * nodes that have no annotation, and for elements annotated as untyped, and
	 * attributes annotated as untypedAtomic.
	 *
	 * @return -1 (there is no type annotation)
	 * @return the type annotation of the node.
	 * @see org.orbeon.saxon.type.Type
	 */

	public int getTypeAnnotation() {
		return -1;
	}

	/**
	 * Determine whether this is the same node as another node. <br />
	 * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
	 *
	 * @return true if this Node object and the supplied Node object represent
	 *         the same node in the tree.
	 */

	public boolean isSameNodeInfo(NodeInfo other) {
		if (!(other instanceof NodeWrapper)) { return false; }
		NodeWrapper ow = (NodeWrapper) other;
//		if (getNodeKind() == Type.NAMESPACE) {
//			if (ow.getNodeKind() != Type.NAMESPACE) return false;
//			if (parent != ow.parent) return false;
//			return ((Namespace) node).getPrefix().equals(((Namespace) ow.node).getPrefix());
//			// how come the prefix matters but the URI shouldn't matter?
//			// Well it makes test idkey131 pass, and probably only works because the
//			// method is used in a certain contexts where this is the expected behaviour...
//		}
		return node.equals(ow.node);
//         return node == ow.node; // In XOM equals() always means identity
	}

	/**
	 * Get the System ID for the node.
	 *
	 * @return the System Identifier of the entity in the source document
	 *         containing the node, or null if not known. Note this is not the
	 *         same as the base URI: the base URI can be modified by xml:base,
	 *         but the system ID cannot.
	 */

	public String getSystemId() {
		return docWrapper.baseURI;
	}

	public void setSystemId(String uri) {
		docWrapper.baseURI = uri;
	}

	/**
	 * Get the Base URI for the node, that is, the URI used for resolving a
	 * relative URI contained in the node.
	 */

	public String getBaseURI() {
		if (getNodeKind() == Type.NAMESPACE) return null;
		return ((Node) node).getBaseURI();
	}

	/**
	 * Get line number
	 *
	 * @return the line number of the node in its original source document; or
	 *         -1 if not available
	 */

	public int getLineNumber() {
		return -1;
	}

	/**
	 * Determine the relative position of this node and another node, in
	 * document order. The other node will always be in the same document.
	 *
	 * @param other
	 *            The other node, whose position is to be compared with this
	 *            node
	 * @return -1 if this node precedes the other node, +1 if it follows the
	 *         other node, or 0 if they are the same node. (In this case,
	 *         isSameNode() will always return true, and the two nodes will
	 *         produce the same result for generateId())
	 */

	public int compareOrder(NodeInfo other) {
		return Navigator.compareOrder(this, (SiblingCountingNode) other);
	}

	/**
	 * Return the string value of the node. The interpretation of this depends
	 * on the type of node. For an element it is the accumulated character
	 * content of the element, including descendant elements.
	 *
	 * @return the string value of the node
	 */

	public String getStringValue() {
		if (node instanceof Node) {
			return ((Node) node).getValue();
		} else {
			return ((Namespace) node).getURI();
		}
	}

	/**
	 * Get name code. The name code is a coded form of the node name: two nodes
	 * with the same name code have the same namespace URI, the same local name,
	 * and the same prefix. By masking the name code with &0xfffff, you get a
	 * fingerprint: two nodes with the same fingerprint have the same local name
	 * and namespace URI.
	 *
	 * @see org.orbeon.saxon.om.NamePool#allocate allocate
	 */

	public int getNameCode() {
		switch (nodeKind) {
			case Type.ELEMENT:
			case Type.ATTRIBUTE:
			case Type.PROCESSING_INSTRUCTION:
			case Type.NAMESPACE:
				return docWrapper.getNamePool().allocate(getPrefix(), getURI(),
						getLocalPart());
			default:
				return -1;
		}
	}

	/**
	 * Get fingerprint. The fingerprint is a coded form of the expanded name of
	 * the node: two nodes with the same name code have the same namespace URI
	 * and the same local name. A fingerprint of -1 should be returned for a
	 * node with no name.
	 */

	public int getFingerprint() {
		// same as in org.orbeon.saxon.dom.NodeWrapper as advised by Michael Kay
		int nc = getNameCode();
		if (nc == -1) { return -1; }
		return nc & 0xfffff;
	}

	/**
	 * Get the local part of the name of this node. This is the name after the
	 * ":" if any.
	 *
	 * @return the local part of the name. For an unnamed node, returns "".
	 */

	public String getLocalPart() {
		switch (nodeKind) {
			case Type.ELEMENT:
				return ((Element) node).getLocalName();
			case Type.ATTRIBUTE:
				return ((Attribute) node).getLocalName();
			case Type.TEXT:
			case Type.COMMENT:
			case Type.DOCUMENT:
				return "";
			case Type.PROCESSING_INSTRUCTION:
				return ((ProcessingInstruction) node).getTarget();
			case Type.NAMESPACE:
				return ((Namespace) node).getPrefix();
			default:
				return null;
		}
	}

	/**
	 * Get the prefix part of the name of this node. This is the name before the
	 * ":" if any. (Note, this method isn't required as part of the NodeInfo
	 * interface.)
	 *
	 * @return the prefix part of the name. For an unnamed node, return an empty
	 *         string.
	 */

	public String getPrefix() {
		switch (nodeKind) {
			case Type.ELEMENT:
				return ((Element) node).getNamespacePrefix();
			case Type.ATTRIBUTE:
				return ((Attribute) node).getNamespacePrefix();
			default:
				return "";
		}
	}

    /**
	 * Get the URI part of the name of this node. This is the URI corresponding
	 * to the prefix, or the URI of the default namespace if appropriate.
	 *
	 * @return The URI of the namespace of this node. For an unnamed node, or
	 *         for a node with an empty prefix, return an empty string.
	 */

	public String getURI() {
		switch (nodeKind) {
			case Type.ELEMENT:
				return ((Element) node).getNamespaceURI();
			case Type.ATTRIBUTE:
				return ((Attribute) node).getNamespaceURI();
			default:
				return "";
		}
	}

	/**
	 * Get the display name of this node. For elements and attributes this is
	 * [prefix:]localname. For unnamed nodes, it is an empty string.
	 *
	 * @return The display name of this node. For a node with no name, return an
	 *         empty string.
	 */

	public String getDisplayName() {
		switch (nodeKind) {
			case Type.ELEMENT:
				return ((Element) node).getQualifiedName();
			case Type.ATTRIBUTE:
				return ((Attribute) node).getQualifiedName();
			case Type.PROCESSING_INSTRUCTION:
			case Type.NAMESPACE:
				return getLocalPart();
			default:
				return "";
		}
	}

	/**
	 * Get the NodeInfo object representing the parent of this node
	 */

	public NodeInfo getParent() {
		if (parent == null) {
			if (node instanceof Node) {
				ParentNode p = ((Node) node).getParent();
				if (p != null) parent = makeWrapper(p, docWrapper);
			} else if (node instanceof Namespace) {
				throw new UnsupportedOperationException(
						"Cannot find parent of XOM namespace node");
			} else {
				throw new IllegalStateException("Unknown XOM node type "
						+ node.getClass());
			}
		}
		return parent;
	}

	/**
	 * Get the index position of this node among its siblings (starting from 0)
	 */

	public int getSiblingPosition() {
		if (index != -1) return index;
		switch (nodeKind) {
			case Type.ATTRIBUTE: {
				Attribute att = (Attribute) node;
				Element p = (Element) att.getParent();
				if (p == null) return 0;
                for (int i=p.getAttributeCount(); --i >= 0;) {
					if (p.getAttribute(i) == att) {
						index = i;
						return i;
					}
				}
				throw new IllegalStateException("XOM node not linked to parent node");
			}
			case Type.NAMESPACE: {
				//getParent(); // this will throw an UnsupportedOperationException
				if (parent == null) throw new IllegalStateException("namespace has no parent");
				Namespace ns = (Namespace) node;
				Element elem = (Element) parent.node;
				int size = elem.getNamespaceDeclarationCount();
				for (int i=0; i < size; i++) {
					String prefix = elem.getNamespacePrefix(i);
					if (prefix.equals(ns.getPrefix())) {
						String uri = elem.getNamespaceURI(prefix);
						if (uri.equals(ns.getURI())) {
							index = i;
							return i;
						}
					}
				}
				throw new IllegalStateException("XOM node not linked to parent node");
			}
			default: {
				Node self = (Node) node;
				ParentNode p = self.getParent();
				int i = (p == null ? 0 : p.indexOf(self));
				if (i == -1) throw new IllegalStateException("XOM node not linked to parent node");
				index = i;
				return index;
			}
		}
	}

	/**
	 * Return an iteration over the nodes reached by the given axis from this
	 * node
	 *
	 * @param axisNumber
	 *            the axis to be used
	 * @return a SequenceIterator that scans the nodes reached by the axis in
	 *         turn.
	 */

	public AxisIterator iterateAxis(byte axisNumber) {
		return iterateAxis(axisNumber, AnyNodeTest.getInstance());
	}

	/**
	 * Return an iteration over the nodes reached by the given axis from this
	 * node
	 *
	 * @param axisNumber
	 *            the axis to be used
	 * @param nodeTest
	 *            A pattern to be matched by the returned nodes
	 * @return a SequenceIterator that scans the nodes reached by the axis in
	 *         turn.
	 */

	public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
		switch (axisNumber) {
		case Axis.ANCESTOR:
			if (nodeKind == Type.DOCUMENT) return EmptyIterator.getInstance();
			return new Navigator.AxisFilter(new Navigator.AncestorEnumeration(
					this, false), nodeTest);

		case Axis.ANCESTOR_OR_SELF:
			if (nodeKind == Type.DOCUMENT) return EmptyIterator.getInstance();
			return new Navigator.AxisFilter(new Navigator.AncestorEnumeration(
					this, true), nodeTest);

		case Axis.ATTRIBUTE:
			if (nodeKind != Type.ELEMENT || ((Element)node).getAttributeCount() == 0) {
                return EmptyIterator.getInstance();
            }
			return new AttributeAxisIterator(this, nodeTest);

		case Axis.CHILD:
			if (hasChildNodes()) {
				return new ChildAxisIterator(this,
						true, true, nodeTest);
			} else {
				return EmptyIterator.getInstance();
			}

		case Axis.DESCENDANT:
			if (hasChildNodes()) {
				return new Navigator.AxisFilter(
						new Navigator.DescendantEnumeration(this, false, true),
						nodeTest);
			} else {
				return EmptyIterator.getInstance();
			}

		case Axis.DESCENDANT_OR_SELF:
			return new Navigator.AxisFilter(
					new Navigator.DescendantEnumeration(this, true, true),
					nodeTest);

		case Axis.FOLLOWING:
			return new Navigator.AxisFilter(new Navigator.FollowingEnumeration(
					this), nodeTest);

		case Axis.FOLLOWING_SIBLING:
			switch (nodeKind) {
				case Type.DOCUMENT:
				case Type.ATTRIBUTE:
				case Type.NAMESPACE:
					return EmptyIterator.getInstance();
				default:
					return new ChildAxisIterator(this,
							false, true, nodeTest);
			}

		case Axis.NAMESPACE:
			if (nodeKind != Type.ELEMENT) { return EmptyIterator.getInstance(); }
			return new Navigator.AxisFilter(new NamespaceEnumeration(this),
					nodeTest);

		case Axis.PARENT:
			getParent();
			if (parent == null) return EmptyIterator.getInstance();
			if (nodeTest.matches(parent)) { return SingletonIterator
					.makeIterator(parent); }
			return EmptyIterator.getInstance();

		case Axis.PRECEDING:
			return new Navigator.AxisFilter(new Navigator.PrecedingEnumeration(
					this, false), nodeTest);

		case Axis.PRECEDING_SIBLING:
			switch (nodeKind) {
				case Type.DOCUMENT:
				case Type.ATTRIBUTE:
				case Type.NAMESPACE:
					return EmptyIterator.getInstance();
				default:
					return new ChildAxisIterator(this,
							false, false, nodeTest);
			}

		case Axis.SELF:
			if (nodeTest.matches(this)) {
				return SingletonIterator.makeIterator(this);
			}
			return EmptyIterator.getInstance();

		case Axis.PRECEDING_OR_ANCESTOR:
			return new Navigator.AxisFilter(new Navigator.PrecedingEnumeration(
					this, true), nodeTest);

		default:
			throw new IllegalArgumentException("Unknown axis number " + axisNumber);
		}
	}

	/**
	 * Get the value of a given attribute of this node
	 *
	 * @param fingerprint
	 *            The fingerprint of the attribute name
	 * @return the attribute value if it exists or null if not
	 */

	public String getAttributeValue(int fingerprint) {
		if (nodeKind == Type.ELEMENT) {
			NamePool pool = docWrapper.getNamePool();
			String localName = pool.getLocalName(fingerprint);
			String uri = pool.getURI(fingerprint);
			Attribute att = ((Element) node).getAttribute(localName, uri);
			if (att != null) return att.getValue();
		}
		return null;
	}

	/**
	 * Get the root node - always a document node with this tree implementation
	 *
	 * @return the NodeInfo representing the containing document
	 */

	public NodeInfo getRoot() {
		return docWrapper;
	}

	/**
	 * Get the root (document) node
	 *
	 * @return the DocumentInfo representing the containing document
	 */

	public DocumentInfo getDocumentRoot() {
		return docWrapper;
	}

	/**
	 * Determine whether the node has any children. <br />
	 * Note: the result is equivalent to <br />
	 * getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()
	 */

	public boolean hasChildNodes() {
        return ((Node)node).getChildCount() > 0;
	}

	/**
	 * Get a character string that uniquely identifies this node. Note:
	 * a.isSameNode(b) if and only if generateId(a)==generateId(b)
	 *
	 * @return a string that uniquely identifies this node, across all documents
	 */

	public String generateId() {
		return Navigator.getSequentialKey(this);
	}

	/**
	 * Get the document number of the document containing this node. For a
	 * free-standing orphan node, just return the hashcode.
	 */

	public int getDocumentNumber() {
		return getDocumentRoot().getDocumentNumber();
	}

	/**
	 * Copy this node to a given outputter (deep copy)
	 */

	public void copy(Receiver out, int whichNamespaces,
			boolean copyAnnotations, int locationId) throws XPathException {
		Navigator.copy(this, out, docWrapper.getNamePool(), whichNamespaces,
				copyAnnotations, locationId);
	}

	/**
	 * Output all namespace nodes associated with this element. Does nothing if
	 * the node is not an element.
	 *
	 * @param out
	 *            The relevant outputter
	 * @param includeAncestors
	 *            True if namespaces declared on ancestor elements must be
	 *            output; false if it is known that these are already on the
	 *            result tree
	 */

	public void outputNamespaceNodes(Receiver out, boolean includeAncestors)
			throws XPathException {
		if (nodeKind == Type.ELEMENT) {
			NamePool pool = docWrapper.getNamePool();
			AxisIterator iter = iterateAxis(Axis.NAMESPACE);
			while (true) {
				NodeWrapper wrapper = (NodeWrapper) iter.next();
				if (wrapper == null) {
					break;
				}
				if (!includeAncestors && !wrapper.getParent().isSameNodeInfo(this)) {
					break;
				}
				Namespace ns = (Namespace) wrapper.node;
				int nscode = pool.allocateNamespaceCode(ns.getPrefix(), ns
						.getURI());
				out.namespace(nscode, 0);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////////
	// XOM has no explicit namespace class, so we define our own helper:
	///////////////////////////////////////////////////////////////////////////////
	private static final class Namespace {
//		public static final Namespace XML_NAMESPACE =
//			new Namespace("xml", "http://www.w3.org/XML/1998/namespace");
		private String prefix; // The prefix mapped to this namespace
		private String uri;    // The URI for this namespace

		public Namespace(String prefix, String uri) {
			this.prefix = prefix;
			this.uri = uri;
		}
		public String getPrefix() { return prefix; }
		public String getURI() { return uri; }
	}

	///////////////////////////////////////////////////////////////////////////////
	// Axis enumeration classes
	///////////////////////////////////////////////////////////////////////////////

	private final class AttributeAxisIterator implements AxisIterator {

		private NodeWrapper start;

		private NodeInfo current;
		private int cursor;

		private NodeTest nodeTest;
		private int position;

		public AttributeAxisIterator(NodeWrapper start, NodeTest test) {
			// use lazy instead of eager materialization (performance)
			this.start = start;
			if (test == AnyNodeTest.getInstance()) test = null;
			this.nodeTest = test;
			this.position = 0;
			this.cursor = 0;
		}

		public Item next() {
			NodeInfo curr;
			do { // until we find a match
				curr = advance();
			}
			while (curr != null && nodeTest != null && (! nodeTest.matches(curr)));

			if (curr != null) position++;
			current = curr;
			return curr;
		}

		private NodeInfo advance() {
			Element elem = (Element) start.node;
			if (cursor == elem.getAttributeCount()) return null;
			NodeInfo curr = makeWrapper(elem.getAttribute(cursor), docWrapper, start, cursor);
			cursor++;
			return curr;
		}

		public Item current() {
			return current;
		}

		public int position() {
			return position;
		}

		public SequenceIterator getAnother() {
			return new AttributeAxisIterator(start, nodeTest);
		}

	} // end of class AttributeAxisIterator
	private final class NamespaceEnumeration extends Navigator.BaseEnumeration {

		private Iterator prefixes;

		private int ix = 0;
//		private int size;

		private NodeWrapper start;
//		private NodeWrapper currElem;
//		private ArrayList prefixHistory = new ArrayList(1);

		public NamespaceEnumeration(NodeWrapper start) {
			this.start = start;
//			Element elem = (Element) start.node;
//			size = elem.getNamespaceDeclarationCount();
//			this.currElem = start;

			// build the complete list of namespaces
			// avoiding eager materialization seems too difficult for the moment...
			NodeWrapper curr = start;
			HashMap nslist = new HashMap(10);
			do {
				Element elem = (Element) curr.node;
				int size = elem.getNamespaceDeclarationCount();
				for (int i=0; i < size; i++) {
					String prefix = elem.getNamespacePrefix(i);
					String uri = elem.getNamespaceURI(prefix);
					if (!(prefix.length() == 0 && uri.length() == 0)) {
						if (!nslist.containsKey(prefix)) {
							nslist.put(prefix, new Namespace(prefix, uri));
						}
					}
				}
				curr = (NodeWrapper) curr.getParent();
			} while (curr.getNodeKind() == Type.ELEMENT);
			nslist.put("xml", new Namespace("xml", "http://www.w3.org/XML/1998/namespace"));
			//nslist.put("xml", Namespace.XML_NAMESPACE);
			prefixes = nslist.values().iterator();
		}

		public SequenceIterator getAnother() {
			return new NamespaceEnumeration(start);
		}

		public void advance() {
			if (prefixes.hasNext()) {
				Namespace ns = (Namespace) prefixes.next();
				current = makeWrapper(ns, docWrapper, start, ix++);
			} else {
				current = null;
			}
		}

//		public void advance() {
//			if (ix < 0) { // iterator exhausted
//				current = null;
//				return;
//			}
//			if (ix < size) {
//				// iterate over namespaces of currElem
//				Element elem = (Element) currElem.node;
//				String prefix = elem.getNamespacePrefix(ix++);
//				if (	prefixHistory.contains(prefix)) { // eliminate duplicates
//					advance();
//					return;
//				}
//				String uri = elem.getNamespaceURI(prefix);
//				if (prefix.length() == 0 && uri.length() == 0) { // skip it
//					advance();
//					return;
//				}
//				Namespace ns = new Namespace(prefix, uri);
//				current = makeWrapper(ns, docWrapper, start, ix);
//				prefixHistory.add(prefix);
//			} else {
//				currElem = (NodeWrapper) currElem.getParent();
//				if (currElem != null && currElem.getNodeKind() == Type.ELEMENT) {
//					// iterate over namespaces of parent
//					size = ((Element) currElem.node).getNamespaceDeclarationCount();
//					ix = 0;
//					advance();
//					return;
//				}
//				else {
//					// manually add the required XML Namespace
//					current = makeWrapper(Namespace.XML_NAMESPACE, docWrapper, start, ix);
//					ix = -1;
//					currElem = null;
//					prefixHistory = null;
//				}
//			}
//		}

		// NB: namespace nodes in the XOM implementation do not support all
		// XPath functions, for example namespace nodes have no parent.

	} // end of class NamespaceEnumeration
	/**
	 * The class ChildAxisIterator handles not only the child axis, but also the
	 * following-sibling and preceding-sibling axes. It can also iterate the
	 * children of the start node in reverse order, something that is needed to
	 * support the preceding and preceding-or-ancestor axes (the latter being
	 * used by xsl:number)
	 */
	private final class ChildAxisIterator implements AxisIterator {

		private NodeWrapper start;
		private NodeWrapper commonParent;
		private int ix;
		private boolean downwards; // iterate children of start node (not siblings)
		private boolean forwards; // iterate in document order (not reverse order)

		private NodeInfo current;
		private ParentNode par;
		private int cursor;

		private NodeTest nodeTest;
		private int position;

		private ChildAxisIterator(NodeWrapper start, boolean downwards, boolean forwards, NodeTest test) {
			this.start = start;
			this.downwards = downwards;
			this.forwards = forwards;

			if (test == AnyNodeTest.getInstance()) test = null;
			this.nodeTest = test;
			this.position = 0;

			if (downwards)
				commonParent = start;
			else
				commonParent = (NodeWrapper) start.getParent();

			par = (ParentNode) commonParent.node;
			if (downwards) {
				ix = (forwards ? 0 : par.getChildCount());
			} else {
				// find the start node among the list of siblings
				ix = start.getSiblingPosition();
				if (forwards) ix++;
			}
			cursor = ix;
//			children = new ParentNodeIterator(par, ix);
			if (!downwards && !forwards) ix--;
		}

		public Item next() {
			NodeInfo curr;
			do { // until we find a match
				curr = advance();
			}
			while (curr != null && nodeTest != null && (! nodeTest.matches(curr)));

			if (curr != null) position++;
			current = curr;
			return curr;
		}

		private NodeInfo advance() {
			Node nextChild;
			do {
				if (forwards) {
					if (cursor == par.getChildCount()) return null;
					nextChild = par.getChild(cursor++);
				} else { // backwards
					if (cursor == 0) return null;
					nextChild = par.getChild(--cursor);
				}
			}
			while (nextChild instanceof DocType);

			NodeInfo curr = makeWrapper(nextChild, docWrapper, commonParent, ix);
			ix += (forwards ? 1 : -1);
			return curr;
		}

		public Item current() {
			return current;
		}

		public int position() {
			return position;
		}

		public SequenceIterator getAnother() {
			return new ChildAxisIterator(start, downwards, forwards, nodeTest);
		}

	}


}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay, with extensive
// rewriting by Wolfgang Hoschek
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//
