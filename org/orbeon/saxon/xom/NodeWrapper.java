package net.sf.saxon.xom;

import net.sf.saxon.Configuration;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.UntypedAtomicValue;
import nu.xom.*;

/**
 * A node in the XML parse tree representing an XML element, character content,
 * or attribute.
 * <P>
 * This is the implementation of the NodeInfo interface used as a wrapper for
 * XOM nodes.
 *
 * @author Michael H. Kay
 * @author Wolfgang Hoschek (ported net.sf.saxon.jdom to XOM)
 */

public class NodeWrapper implements NodeInfo, VirtualNode, SiblingCountingNode {

	protected Node node;

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
	protected NodeWrapper(Node node, NodeWrapper parent, int index) {
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
	protected final NodeWrapper makeWrapper(Node node, DocumentWrapper docWrapper) {
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

	protected final NodeWrapper makeWrapper(Node node, DocumentWrapper docWrapper,
			NodeWrapper parent, int index) {

		short kind;
		if (node instanceof Element) {
			kind = Type.ELEMENT;
		} else if (node instanceof Attribute) {
			kind = Type.ATTRIBUTE;
		} else if (node instanceof Text) {
			kind = Type.TEXT;
		} else if (node instanceof Comment) {
			kind = Type.COMMENT;
		} else if (node instanceof ProcessingInstruction) {
			kind = Type.PROCESSING_INSTRUCTION;
		} else if (node instanceof Document) {
			return docWrapper;
		} else {
			throwIllegalNode(node); // moved out of fast path to enable better inlining
			return null; // keep compiler happy
		}

		NodeWrapper wrapper = new NodeWrapper(node, parent, index);
		wrapper.nodeKind = kind;
		wrapper.docWrapper = docWrapper;
		return wrapper;
	}

	private static void throwIllegalNode(Object node) {
		String str = node == null ?
				"NULL" :
				node.getClass() + " instance " + node.toString();
		throw new IllegalArgumentException("Bad node type in XOM! " + str);
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
				getStringValueCS()));
	}

    /**
	 * Get the type annotation of this node, if any. Returns -1 for kinds of
	 * nodes that have no annotation, and for elements annotated as untyped, and
	 * attributes annotated as untypedAtomic.
	 *
	 * @return -1 (there is no type annotation)
	 * @return the type annotation of the node.
	 * @see net.sf.saxon.type.Type
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
		if (!(other instanceof NodeWrapper)) {
            return false;
        }
		NodeWrapper ow = (NodeWrapper) other;
		return node.equals(ow.node);
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
		return node.getBaseURI();
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
        if (other instanceof SiblingCountingNode) {
		    return Navigator.compareOrder(this, (SiblingCountingNode) other);
        } else {
            // it must be a namespace node
            return -other.compareOrder(this);
        }
	}

	/**
	 * Return the string value of the node. The interpretation of this depends
	 * on the type of node. For an element it is the accumulated character
	 * content of the element, including descendant elements.
	 *
	 * @return the string value of the node
	 */

	public String getStringValue() {
        return node.getValue();
	}

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return node.getValue();
    }

	/**
	 * Get name code. The name code is a coded form of the node name: two nodes
	 * with the same name code have the same namespace URI, the same local name,
	 * and the same prefix. By masking the name code with &0xfffff, you get a
	 * fingerprint: two nodes with the same fingerprint have the same local name
	 * and namespace URI.
	 *
	 * @see net.sf.saxon.om.NamePool#allocate allocate
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
		int nc = getNameCode();
		if (nc == -1) {
            return -1;
        }
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
				ParentNode p = node.getParent();
				if (p != null) parent = makeWrapper(p, docWrapper);
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

			default: {
				Node self = node;
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
			return makeAncestorIterator(false, nodeTest);

		case Axis.ANCESTOR_OR_SELF:
			return makeAncestorIterator(true, nodeTest);

		case Axis.ATTRIBUTE:
			if (nodeKind != Type.ELEMENT || ((Element) node).getAttributeCount() == 0)
				return EmptyIterator.getInstance();
			else
				return new AttributeAxisIterator(this, nodeTest);

		case Axis.CHILD:
			if (hasChildNodes())
				return new ChildAxisIterator(this, true, true, nodeTest);
			else
				return EmptyIterator.getInstance();

		case Axis.DESCENDANT:
			if (hasChildNodes()) {
				return new DescendantAxisIterator(this, false, false, nodeTest);
			} else {
				return EmptyIterator.getInstance();
			}

		case Axis.DESCENDANT_OR_SELF:
			if (nodeKind == Type.ATTRIBUTE || nodeKind == Type.NAMESPACE) {
				return makeSingleIterator(this, nodeTest);
            } else {
				return new DescendantAxisIterator(this, true, false, nodeTest);
            }

		case Axis.FOLLOWING:
			if (getParent() == null) {
				return EmptyIterator.getInstance();
            } else {
				if (nodeKind == Type.NAMESPACE) // avoids ClassCastException
					return new Navigator.AxisFilter(new Navigator.FollowingEnumeration(this), nodeTest);
				else
					return new DescendantAxisIterator(this, false, true, nodeTest);
			}

		case Axis.FOLLOWING_SIBLING:
			if (nodeKind == Type.ATTRIBUTE || nodeKind == Type.NAMESPACE) {
                return EmptyIterator.getInstance();
            }
			if (getParent() == null) {
				return EmptyIterator.getInstance();
            } else {
				return new ChildAxisIterator(this, false, true, nodeTest);
            }

		case Axis.NAMESPACE:
			if (nodeKind != Type.ELEMENT) {
				return EmptyIterator.getInstance();
            }
            return new NamespaceIterator(this, nodeTest);

		case Axis.PARENT:
			if (getParent() == null) {
				return EmptyIterator.getInstance();
            } else {
				return makeSingleIterator(parent, nodeTest);
            }

		case Axis.PRECEDING:
			return new Navigator.AxisFilter(
					new Navigator.PrecedingEnumeration(this, false), nodeTest);

		case Axis.PRECEDING_SIBLING:
			if (nodeKind == Type.ATTRIBUTE || nodeKind == Type.NAMESPACE) return EmptyIterator.getInstance();
			if (getParent() == null)
				return EmptyIterator.getInstance();
			else
				return new ChildAxisIterator(this, false, false, nodeTest);

		case Axis.SELF:
			return makeSingleIterator(this, nodeTest);

		case Axis.PRECEDING_OR_ANCESTOR:
			return new Navigator.AxisFilter(new Navigator.PrecedingEnumeration(
					this, true), nodeTest);

		default:
			throw new IllegalArgumentException("Unknown axis number " + axisNumber);
		}
	}

	/**
	 * There are always few ancestors, and even fewer that match the nodeTest.
	 * It's faster to materialize the matches in one swoop, rather than have a
	 * stateful AncestorEnumeration, except in rare cases where the otherwise
	 * lazy pipeline is shortcut. [thus says WH]
     *
     * MHK note: I'm not sure the ancestor::*[predicate][1] case is really "rare"...
	 */
	private AxisIterator makeAncestorIterator(boolean includeSelf, NodeTest nodeTest) {
		if (nodeTest == AnyNodeTest.getInstance()) nodeTest = null;
		int size = 0;
		Item[] ancestors = new Item[8];
		NodeInfo ancestor = includeSelf ? this : getParent();

		while (ancestor != null) { // walk the tree upwards towards the root
			if (nodeTest == null || nodeTest.matches(ancestor)) {
				if (size >= ancestors.length) { // grow list
					Item[] tmp = new Item[ancestors.length * 2];
					System.arraycopy(ancestors, 0, tmp, 0, size);
					ancestors = tmp;
				}
				ancestors[size++] = ancestor;
			}
			ancestor = ancestor.getParent();
		}

		if (size == 0) {
            return EmptyIterator.getInstance(); // common case
        }
		if (size == 1) {
            return SingletonIterator.makeIterator(ancestors[0]); // common case
        }
		return new ArrayIterator(ancestors, 0, size); // fast and implements ReversibleIterator
	}

	private static AxisIterator makeSingleIterator(NodeWrapper wrapper, NodeTest nodeTest) {
		if (nodeTest == AnyNodeTest.getInstance() || nodeTest.matches(wrapper))
			return SingletonIterator.makeIterator(wrapper);
		else
			return EmptyIterator.getInstance();
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
        return node.getChildCount() > 0;
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
     */

    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors)
        throws XPathException {
        Navigator.sendNamespaceDeclarations(this, out, includeAncestors);
    }

    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
     *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p/>
     *         <p>For a node other than an element, the method returns null.</p>
     */

    public int[] getDeclaredNamespaces(int[] buffer) {
        if (node instanceof Element) {
            Element elem = (Element)node;
            int size = elem.getNamespaceDeclarationCount();
            if (size == 0) {
                return EMPTY_NAMESPACE_LIST;
            }
            int[] result = (size <= buffer.length ? buffer : new int[size]);
            NamePool pool = getNamePool();
            for (int i=0; i < size; i++) {
                String prefix = elem.getNamespacePrefix(i);
                String uri = elem.getNamespaceURI(prefix);
                result[i] = pool.allocateNamespaceCode(prefix, uri);
            }
            if (size < result.length) {
                result[size] = -1;
            }
            return result;
        } else {
            return null;
        }
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
//				ix = start.getSiblingPosition();
				ix = par.indexOf(start.node);
				if (forwards) ix++;
			}
			cursor = ix;
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
			} while (nextChild instanceof DocType);
			// DocType is not an XPath node; can occur for /child::node()

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

	/**
	 * A bit of a misnomer; efficiently takes care of descendants,
	 * descentants-or-self as well as "following" axis.
	 * "includeSelf" must be false for the following axis.
	 * Uses simple and effective O(1) backtracking via indexOf().
	 */
    private final class DescendantAxisIterator implements AxisIterator {

		private NodeWrapper start;
		private boolean includeSelf;
		private boolean following;

		private Node anchor; // so we know where to stop the scan
		private Node currNode;
		private boolean moveToNextSibling;

		private NodeInfo current;
		private NodeTest nodeTest;
		private int position;

		private String testLocalName;
		private String testURI;

		public DescendantAxisIterator(NodeWrapper start, boolean includeSelf, boolean following, NodeTest test) {
			this.start = start;
			this.includeSelf = includeSelf;
			this.following = following;
			this.moveToNextSibling = following;

			if (!following) anchor = start.node;
			if (!includeSelf) currNode = start.node;

			if (test == AnyNodeTest.getInstance()) {
				test = null; // mark as AnyNodeTest
			}
			else if (test instanceof NameTest) {
				NameTest nt = (NameTest) test;
				if (nt.getPrimitiveType() == Type.ELEMENT) {
					// mark as element name test
					NamePool pool = getNamePool();
					this.testLocalName = pool.getLocalName(nt.getFingerprint());
					this.testURI = pool.getURI(nt.getFingerprint());
				}
			}
			else if (test instanceof NodeKindTest) {
				if (test.getPrimitiveType() == Type.ELEMENT) {
					// mark as element type test
					this.testLocalName = "";
					this.testURI = null;
				}
			}
			this.nodeTest = test;
			this.position = 0;
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
			if (currNode == null) { // if includeSelf
				currNode = start.node;
				return start;
			}

			int i;
			do {
				i = 0;
				Node p = currNode;

				if (p.getChildCount() == 0 || moveToNextSibling) { // move to next sibling

					moveToNextSibling = false; // do it just once
					while (true) {
						// if we've the reached the root, we're done scanning
						p = currNode.getParent();
						if (p == null) return null;

						// Note: correct even if currNode is an attribute.
						// Performance is particularly good with the O(1) patch
						// for XOM's ParentNode.indexOf()
						i = currNode.getParent().indexOf(currNode) + 1;

						if (i < p.getChildCount()) {
							break; // break out of while(true) loop; move to next sibling
						}
						else { // reached last sibling; move up
							currNode = p;
							// if we've come all the way back to the start anchor we're done
							if (p == anchor) return null;
						}
					}
				}
				currNode = p.getChild(i);
			} while (!conforms(currNode));

			// note the null here: makeNodeWrapper(parent, ...) is fast, so it
			// doesn't really matter that we don't keep a link to it.
			// In fact, it makes objects more short lived, easing pressure on
			// the VM allocator and collector for tenured heaps.
			return makeWrapper(currNode, docWrapper, null, i);
		}

		// avoids NodeWrapper allocation when there's clearly a mismatch (common case)
		private boolean conforms(Node node) {
			if (this.testLocalName != null) { // element test?
				if (!(node instanceof Element)) return false;
				if (this.testURI == null) return true; // pure element type test

				// element name test
				Element elem = (Element) node;
				return this.testLocalName.equals(elem.getLocalName()) &&
					this.testURI.equals(elem.getNamespaceURI());
			}
			else { // DocType is not an XPath node; can occur for /descendants::node()
				return !(node instanceof DocType);
			}
		}

		public Item current() {
			return current;
		}

		public int position() {
			return position;
		}

		public SequenceIterator getAnother() {
			return new DescendantAxisIterator(start, includeSelf, following, nodeTest);
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
