package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.IntArraySet;
import org.orbeon.saxon.tree.SystemIdMap;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.value.Whitespace;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * A data structure to hold the contents of a tree. As the name implies, this implementation
 * of the data model is optimized for size, and for speed of creation: it minimizes the number
 * of Java objects used.
 *
 * <p>It can be used to represent a tree that is rooted at a document node, or one that is rooted
 * at an element node.</p>
 */

public final class TinyTree {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private Configuration config;

    // List of top-level document nodes.
    private ArrayList documentList = new ArrayList(5);

    // The document number (really a tree number: it can identify a non-document root node
    protected int documentNumber;

    // the contents of the document

    protected LargeStringBuffer charBuffer;
    protected FastStringBuffer commentBuffer = null; // created when needed

    protected int numberOfNodes = 0;    // excluding attributes and namespaces

    // The following arrays contain one entry for each node other than attribute
    // and namespace nodes, arranged in document order.

    // nodeKind indicates the kind of node, e.g. element, text, or comment
    public byte[] nodeKind;

    // depth is the depth of the node in the hierarchy, i.e. the number of ancestors
    protected short[] depth;

    // next is the node number of the next sibling
    // - unless it points backwards, in which case it is the node number of the parent
    protected int[] next;

    // alpha holds a value that depends on the node kind. For text nodes, it is the offset
    // into the text buffer. For comments and processing instructions, it is the offset into
    // the comment buffer. For elements, it is the index of the first attribute node, or -1
    // if this element has no attributes.
    protected int[] alpha;

    // beta holds a value that depends on the node kind. For text nodes, it is the length
    // of the text. For comments and processing instructions, it is the length of the text.
    // For elements, it is the index of the first namespace node, or -1
    // if this element has no namespaces.
    protected int[] beta;

    // nameCode holds the name of the node, as an identifier resolved using the name pool
    protected int[] nameCode;

    // the prior array indexes preceding-siblings; it is constructed only when required
    protected int[] prior = null;

    // the typeCode array holds type codes for element nodes; it is constructed only
    // if at least one element has a type other than untyped, or has an IDREF property.
    // The array holds the type fingerprint, with bit TYPECODE_IDREF set if the value is an IDREF
    protected int[] typeCodeArray = null;

    private static final int TYPECODE_IDREF = 1<<29;

    // the owner array gives fast access from a node to its parent; it is constructed
    // only when required
    // protected int[] parentIndex = null;

    // the following arrays have one entry for each attribute.
    protected int numberOfAttributes = 0;

    // attParent is the index of the parent element node
    protected int[] attParent;

    // attCode is the nameCode representing the attribute name
    protected int[] attCode;

    // attValue is the string value of the attribute
    protected CharSequence[] attValue;

    // attTypeCode holds type annotations. The array is created only if any nodes have a type annotation
    // or are marked as IDREF/IDREFS attributes.  The bit TYPECODE_IDREF represents the is-idref property,
    // while IS_DTD_TYPE is set if the type is DTD-derived.
    protected int[] attTypeCode;

    // The following arrays have one entry for each namespace declaration
    protected int numberOfNamespaces = 0;

    // namespaceParent is the index of the element node owning the namespace declaration
    protected int[] namespaceParent;

    // namespaceCode is the namespace code used by the name pool: the top half is the prefix
    // code, the bottom half the URI code
    protected int[] namespaceCode;

    // an array holding the offsets of all the level-0 (root) nodes, so that the root of a given
    // node can be found efficiently
    private int[] rootIndex = new int[5];
    protected int rootIndexUsed = 0;

    private int[] lineNumbers = null;
    private int[] columnNumbers = null;
    private SystemIdMap systemIdMap = null;

    // a boolean that is set to true if the document declares a namespace other than the XML namespace
    protected boolean usesNamespaces = false;

    // We maintain statistics in static data, recording how large the trees created under this Java VM
    // turned out to be. These figures are then used when allocating space for new trees, on the assumption
    // that there is likely to be some uniformity. The statistics are initialized to an arbitrary value
    // so that they can be used every time including the first time. The count of how many trees have been
    // created so far is initialized artificially to 5, to provide some smoothing if the first real tree is
    // atypically large or small.

    private static int treesCreated = 5;
    private static double averageNodes = 4000.0;
    private static double averageAttributes = 100.0;
    private static double averageNamespaces = 20.0;
    private static double averageCharacters = 4000.0;

    /**
     * Create a TinyTree. The initial size is based on the average size of
     * trees previously built in this session
     */

    public TinyTree() {
        this((int)(averageNodes + 1),
                (int)(averageAttributes + 1),
                (int)(averageNamespaces + 1),
                (int)(averageCharacters + 1));
    }

    /**
     * Create a tree with a specified initial size
     * @param nodes the expected number of (non attribute or namespace) nodes
     * @param attributes the expected number of attributes
     * @param namespaces the expected number of namespace declarations
     * @param characters the expected number of characters in the document (in text nodes)
     */

    public TinyTree(int nodes, int attributes, int namespaces, int characters) {
        //System.err.println("TinyTree.new() (initial size " + nodes + ", treesCreated = " + treesCreated + ")");
        nodeKind = new byte[nodes];
        depth = new short[nodes];
        next = new int[nodes];
        alpha = new int[nodes];
        beta = new int[nodes];
        nameCode = new int[nodes];

        numberOfAttributes = 0;
        attParent = new int[attributes];
        attCode = new int[attributes];
        attValue = new String[attributes];

        numberOfNamespaces = 0;
        namespaceParent = new int[namespaces];
        namespaceCode = new int[namespaces];

        charBuffer = new LargeStringBuffer(characters, 64000);
    }

    /**
     * Set the Configuration that contains this document
     * @param config the Saxon configuration
    */

    public void setConfiguration(Configuration config) {
        this.config = config;
		addNamespace(0, NamespaceConstant.XML_NAMESPACE_CODE);
    }

    /**
     * Get the configuration previously set using setConfiguration
     * @return the Saxon configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
	 * Get the name pool used for the names in this document
     * @return the name pool
	*/

	public NamePool getNamePool() {
		return config.getNamePool();
	}

    private void ensureNodeCapacity(short kind) {
        if (nodeKind.length < numberOfNodes+1) {
            //System.err.println("Number of nodes = " + numberOfNodes);
            int k = (kind == Type.STOPPER ? numberOfNodes+1 : numberOfNodes*2);

            byte[] nodeKind2 = new byte[k];
            int[] next2 = new int[k];
            short[] depth2 = new short[k];
            int[] alpha2 = new int[k];
            int[] beta2 = new int[k];
            int[] nameCode2 = new int[k];

            System.arraycopy(nodeKind, 0, nodeKind2, 0, numberOfNodes);
            System.arraycopy(next, 0, next2, 0, numberOfNodes);
            System.arraycopy(depth, 0, depth2, 0, numberOfNodes);
            System.arraycopy(alpha, 0, alpha2, 0, numberOfNodes);
            System.arraycopy(beta, 0, beta2, 0, numberOfNodes);
            System.arraycopy(nameCode, 0, nameCode2, 0, numberOfNodes);

            nodeKind = nodeKind2;
            next = next2;
            depth = depth2;
            alpha = alpha2;
            beta = beta2;
            nameCode = nameCode2;

            if (typeCodeArray != null) {
                int[] typeCodeArray2 = new int[k];
                System.arraycopy(typeCodeArray, 0, typeCodeArray2, 0, numberOfNodes);
                typeCodeArray = typeCodeArray2;
            }

            if (lineNumbers != null) {
                int[] lines2 = new int[k];
                System.arraycopy(lineNumbers, 0, lines2, 0, numberOfNodes);
                lineNumbers = lines2;
                int[] columns2 = new int[k];
                System.arraycopy(columnNumbers, 0, columns2, 0, numberOfNodes);
                columnNumbers = columns2;
            }
        }
    }

    private void ensureAttributeCapacity() {
        if (attParent.length < numberOfAttributes+1) {
            int k = numberOfAttributes*2;
            if (k==0) {
                k = 10;
            }

            int[] attParent2 = new int[k];
            int[] attCode2 = new int[k];
            String[] attValue2 = new String[k];

            System.arraycopy(attParent, 0, attParent2, 0, numberOfAttributes);
            System.arraycopy(attCode, 0, attCode2, 0, numberOfAttributes);
            System.arraycopy(attValue, 0, attValue2, 0, numberOfAttributes);

            attParent = attParent2;
            attCode = attCode2;
            attValue = attValue2;

            if (attTypeCode != null) {
                int[] attTypeCode2 = new int[k];
                System.arraycopy(attTypeCode, 0, attTypeCode2, 0, numberOfAttributes);
                attTypeCode = attTypeCode2;
            }
        }
    }

    private void ensureNamespaceCapacity() {
        if (namespaceParent.length < numberOfNamespaces+1) {
            int k = numberOfNamespaces*2;

            int[] namespaceParent2 = new int[k];
            int[] namespaceCode2 = new int[k];

            System.arraycopy(namespaceParent, 0, namespaceParent2, 0, numberOfNamespaces);
            System.arraycopy(namespaceCode, 0, namespaceCode2, 0, numberOfNamespaces);

            namespaceParent = namespaceParent2;
            namespaceCode = namespaceCode2;
        }
    }

    /**
     * Add a document node to the tree. The data structure can contain any number of document (or element) nodes
     * as top-level nodes. The document node is retained in the documentList list, and its offset in that list
     * is held in the alpha array for the relevant node number.
     * @param doc the document node to be added
     * @return the number of the node that was added
     */

    int addDocumentNode(TinyDocumentImpl doc) {
        documentList.add(doc);
        return addNode(Type.DOCUMENT, 0, documentList.size()-1, 0, -1);
    }

    /**
     * Add a node to the tree
     * @param kind          The kind of the node. This must be a document, element, text, comment,
     *                      or processing-instruction node (not an attribute or namespace)
     * @param depth         The depth in the tree
     * @param alpha         Pointer to attributes or text
     * @param beta          Pointer to namespaces or text
     * @param nameCode      The name of the node
     * @return the node number of the node that was added
     */
    int addNode(short kind, int depth, int alpha, int beta, int nameCode) {
        ensureNodeCapacity(kind);
        nodeKind[numberOfNodes] = (byte)kind;
        this.depth[numberOfNodes] = (short)depth;
        this.alpha[numberOfNodes] = alpha;
        this.beta[numberOfNodes] = beta;
        this.nameCode[numberOfNodes] = nameCode;
        next[numberOfNodes] = -1;      // safety precaution

        if (typeCodeArray != null) {
            typeCodeArray[numberOfNodes] = StandardNames.XS_UNTYPED;
        }

        if (numberOfNodes == 0) {
            documentNumber = config.getDocumentNumberAllocator().allocateDocumentNumber();
        }

        if (depth == 0 && kind != Type.STOPPER) {
            if (rootIndexUsed == rootIndex.length) {
                int[] r2 = new int[rootIndexUsed * 2];
                System.arraycopy(rootIndex, 0, r2, 0, rootIndexUsed);
                rootIndex = r2;
            }
            rootIndex[rootIndexUsed++] = numberOfNodes;
        }
        return numberOfNodes++;
    }

    /**
     * Append character data to the current text node
     * @param chars the character data to be appended
     */

    void appendChars(CharSequence chars) {
        charBuffer.append(chars);
    }

    /**
    * Condense the tree: release unused memory. This is done after the full tree has been built.
    * The method makes a pragmatic judgement as to whether it is worth reclaiming space; this is
    * only done when the constructed tree is very small compared with the space allocated.
    */

    void condense() {
        //System.err.println("TinyTree.condense() " + this + " roots " + rootIndexUsed + " nodes " + numberOfNodes + " capacity " + nodeKind.length);

        // If there are already two trees in this forest, the chances are that more will be added. In this
        // case we don't want to condense the arrays because we will only have to expand them again, which gets
        // increasingly expensive as they grow larger.
        if (rootIndexUsed > 1) {
            return;
        }
        if (numberOfNodes * 3 < nodeKind.length ||
                (nodeKind.length - numberOfNodes > 20000)) {

            //System.err.println("-- copying node arrays");
            int k = numberOfNodes + 1;

            byte[] nodeKind2 = new byte[k];
            int[] next2 = new int[k];
            short[] depth2 = new short[k];
            int[] alpha2 = new int[k];
            int[] beta2 = new int[k];
            int[] nameCode2 = new int[k];

            System.arraycopy(nodeKind, 0, nodeKind2, 0, numberOfNodes);
            System.arraycopy(next, 0, next2, 0, numberOfNodes);
            System.arraycopy(depth, 0, depth2, 0, numberOfNodes);
            System.arraycopy(alpha, 0, alpha2, 0, numberOfNodes);
            System.arraycopy(beta, 0, beta2, 0, numberOfNodes);
            System.arraycopy(nameCode, 0, nameCode2, 0, numberOfNodes);
            if (typeCodeArray != null) {
                int[] type2 = new int[k];
                System.arraycopy(typeCodeArray, 0, type2, 0, numberOfNodes);
                typeCodeArray = type2;
            }
            if (lineNumbers != null) {
                int[] lines2 = new int[k];
                System.arraycopy(lineNumbers, 0, lines2, 0, numberOfNodes);
                lineNumbers = lines2;
                int[] columns2 = new int[k];
                System.arraycopy(columnNumbers, 0, columns2, 0, numberOfNodes);
                columnNumbers = columns2;
            }

            nodeKind = nodeKind2;
            next = next2;
            depth = depth2;
            alpha = alpha2;
            beta = beta2;
            nameCode = nameCode2;
        }

        if ((numberOfAttributes * 3 < attParent.length) ||
                (attParent.length - numberOfAttributes > 1000)) {
            int k = numberOfAttributes;

            //System.err.println("-- copying attribute arrays");

            if (k==0) {
                attParent = IntArraySet.EMPTY_INT_ARRAY;
                attCode = IntArraySet.EMPTY_INT_ARRAY;
                attValue = EMPTY_STRING_ARRAY;
                attTypeCode = null;
            }

            int[] attParent2 = new int[k];
            int[] attCode2 = new int[k];
            String[] attValue2 = new String[k];

            System.arraycopy(attParent, 0, attParent2, 0, numberOfAttributes);
            System.arraycopy(attCode, 0, attCode2, 0, numberOfAttributes);
            System.arraycopy(attValue, 0, attValue2, 0, numberOfAttributes);

            attParent = attParent2;
            attCode = attCode2;
            attValue = attValue2;

            if (attTypeCode != null) {
                int[] attTypeCode2 = new int[k];
                System.arraycopy(attTypeCode, 0, attTypeCode2, 0, numberOfAttributes);
                attTypeCode = attTypeCode2;
            }
        }

        if (numberOfNamespaces * 3 < namespaceParent.length) {
            int k = numberOfNamespaces;
            int[] namespaceParent2 = new int[k];
            int[] namespaceCode2 = new int[k];

            //System.err.println("-- copying namespace arrays");

            System.arraycopy(namespaceParent, 0, namespaceParent2, 0, numberOfNamespaces);
            System.arraycopy(namespaceCode, 0, namespaceCode2, 0, numberOfNamespaces);

            namespaceParent = namespaceParent2;
            namespaceCode = namespaceCode2;
        }

        updateStatistics();
//        System.err.println("STATS: " + averageNodes + ", " + averageAttributes + ", "
//                + averageNamespaces + ", " + averageCharacters);

//        if (charBufferLength * 3 < charBuffer.length ||
//                charBuffer.length - charBufferLength > 10000) {
//            char[] c2 = new char[charBufferLength];
//            System.arraycopy(charBuffer,  0, c2, 0, charBufferLength);
//            charBuffer = c2;
//        }
    }

    /**
     * Set the type annotation of an element node
     * @param nodeNr the node whose type annotation is to be set
     * @param typeCode the type annotation
    */

    void setElementAnnotation(int nodeNr, int typeCode) {
        if (typeCode != StandardNames.XS_UNTYPED) {
            if (typeCodeArray == null) {
                typeCodeArray = new int[nodeKind.length];
                Arrays.fill(typeCodeArray, 0, nodeKind.length, StandardNames.XS_UNTYPED);
            }
            typeCodeArray[nodeNr] = typeCode;
        }
    }

    /**
     * Get the type annotation of a node. Applies only to document, element, text,
     * processing instruction, and comment nodes.
     * @param nodeNr the node whose type annotation is required
     * @return the fingerprint of the type annotation for elements and attributes, otherwise undefined.
    */

    public int getTypeAnnotation(int nodeNr) {
        if (typeCodeArray == null) {
            return StandardNames.XS_UNTYPED;
        }
        return typeCodeArray[nodeNr] & NamePool.FP_MASK;
    }

    /**
     * Get the node kind of a given node, which must be a document, element,
     * text, comment, or processing instruction node
     * @param nodeNr the node number
     * @return the node kind
     */

    public int getNodeKind(int nodeNr) {
        int kind = nodeKind[nodeNr];
        return (kind == Type.WHITESPACE_TEXT ? Type.TEXT : kind);
    }

    /**
     * Get the nameCode for a given node, which must be a document, element,
     * text, comment, or processing instruction node
     * @param nodeNr the node number
     * @return the name code
     */

    public int getNameCode(int nodeNr) {
        return nameCode[nodeNr];
    }

    /**
    * On demand, make an index for quick access to preceding-sibling nodes
    */

    void ensurePriorIndex() {
        // TODO: avoid rebuilding the whole index in the second case, i.e. with a forest
        if (prior==null || prior.length < numberOfNodes) {
            makePriorIndex();
        }
    }

    private synchronized void makePriorIndex() {
        prior = new int[numberOfNodes];
        Arrays.fill(prior, 0, numberOfNodes, -1);
        for (int i=0; i<numberOfNodes; i++) {
            int nextNode = next[i];
            if (nextNode > i) {
                prior[nextNode] = i;
            }
        }
    }

    /**
     * Add an attribute node to the tree
     * @param root the root of the tree to contain the attribute
     * @param parent the parent element of the new attribute
     * @param nameCode the name code of the attribute
     * @param typeCode the type annotation of the attribute
     * @param attValue the string value of the attribute
     * @param properties any special properties of the attribute (bit-significant)
     */


    void addAttribute(NodeInfo root, int parent, int nameCode, int typeCode, CharSequence attValue, int properties) {
        ensureAttributeCapacity();
        attParent[numberOfAttributes] = parent;
        attCode[numberOfAttributes] = nameCode;
        this.attValue[numberOfAttributes] = attValue;

        if (typeCode == -1) {
            // this shouldn't happen any more
            typeCode = StandardNames.XS_UNTYPED_ATOMIC;
        }

        if (typeCode != StandardNames.XS_UNTYPED_ATOMIC) {
            initializeAttributeTypeCodes();
        }

        if (attTypeCode != null) {
            attTypeCode[numberOfAttributes] = typeCode;
        }

        if (alpha[parent] == -1) {
            alpha[parent] = numberOfAttributes;
        }

        if (root instanceof TinyDocumentImpl) {
            boolean isID = false;
            if ((properties & ReceiverOptions.IS_ID) != 0) {
                isID = true;
            } else if ((nameCode & NamePool.FP_MASK) == StandardNames.XML_ID) {
                isID = true;
            } else if (config.getTypeHierarchy().isIdCode(typeCode)) {
                isID = true;
            }
            if (isID) {

                // The attribute is marked as being an ID. But we don't trust it - it
                // might come from a non-validating parser. Before adding it to the index, we
                // check that it really is an ID.

                String id = Whitespace.trim(attValue);

                // Make an exception to our usual policy of storing the original string value.
                // This is because xml:id processing applies whitespace trimming at an earlier stage
                this.attValue[numberOfAttributes] = id;
                
                if (root.getConfiguration().getNameChecker().isValidNCName(id)) {
                    NodeInfo e = getNode(parent);
                    ((TinyDocumentImpl)root).registerID(e, id);
                } else if (attTypeCode != null) {
                    attTypeCode[numberOfAttributes] = StandardNames.XS_UNTYPED_ATOMIC;
                }
            }
            if ((properties & ReceiverOptions.IS_IDREF) != 0) {
                initializeAttributeTypeCodes();
                attTypeCode[numberOfAttributes] = typeCode | TYPECODE_IDREF;
            }
        }

        // Note: IDREF attributes are not indexed at this stage; that happens only if and when
        // the idref() function is called.

        // Note that an attTypes array will be created for all attributes if any ID or IDREF is reported.

        numberOfAttributes++;
    }

    private void initializeAttributeTypeCodes() {
        if (attTypeCode==null) {
            // this is the first typed attribute;
            // create an array for the types, and set all previous attributes to untyped
            attTypeCode = new int[attParent.length];
            Arrays.fill(attTypeCode, 0, numberOfAttributes, StandardNames.XS_UNTYPED_ATOMIC);
//            for (int i=0; i<numberOfAttributes; i++) {
//                attTypeCode[i] = StandardNames.XDT_UNTYPED_ATOMIC;
//            }
        }
    }

    /**
     * Index an element of type xs:ID
     * @param root the root node of the document
     * @param nodeNr the element of type xs:ID
     * @param checker checks names against XML 1.0 or XML 1.1 rules
     */

    public void indexIDElement(NodeInfo root, int nodeNr, NameChecker checker) {
        String id = Whitespace.trim(TinyParentNodeImpl.getStringValue(this, nodeNr));
        if (root.getNodeKind() == Type.DOCUMENT && checker.isValidNCName(id)) {
            NodeInfo e = getNode(nodeNr);
            ((TinyDocumentImpl)root).registerID(e, id);
        }
    }

    /**
     * Add a namespace node to the current element
     * @param parent the node number of the element
     * @param nscode namespace code identifying the prefix and uri
     */
    void addNamespace(int parent, int nscode ) {

        ensureNamespaceCapacity();
        namespaceParent[numberOfNamespaces] = parent;
        namespaceCode[numberOfNamespaces] = nscode;

        if (beta[parent] == -1) {
            beta[parent] = numberOfNamespaces;
        }
        numberOfNamespaces++;
        if (nscode != NamespaceConstant.XML_NAMESPACE_CODE) {
            usesNamespaces = true;
        }
    }

    /**
     * Get the node at a given position in the tree
     * @param nr the node number
     * @return the node at the given position
     */

    public final TinyNodeImpl getNode(int nr) {

        switch (nodeKind[nr]) {
            case Type.DOCUMENT:
                return (TinyDocumentImpl)documentList.get(alpha[nr]);
            case Type.ELEMENT:
                return new TinyElementImpl(this, nr);
            case Type.TEXT:
                return new TinyTextImpl(this, nr);
            case Type.WHITESPACE_TEXT:
                return new WhitespaceTextImpl(this, nr);
            case Type.COMMENT:
                return new TinyCommentImpl(this, nr);
            case Type.PROCESSING_INSTRUCTION:
                return new TinyProcInstImpl(this, nr);
            case Type.PARENT_POINTER:
                throw new IllegalArgumentException("Attempting to treat a parent pointer as a node");
        }

        return null;
    }

    /**
     * Get the typed value of a node whose type is known to be untypedAtomic.
     * The node must be a document, element, text,
     * comment, or processing-instruction node, and it must have no type annotation.
     * This method gets the typed value
     * of a numbered node without actually instantiating the NodeInfo object, as
     * a performance optimization.
     * @param nodeNr the node whose typed value is required
     * @return the atomic value of the node
     */

    AtomicValue getAtomizedValueOfUntypedNode(int nodeNr) {
        switch (nodeKind[nodeNr]) {
            case Type.ELEMENT:
            case Type.DOCUMENT:
                int level = depth[nodeNr];
                int next = nodeNr+1;

                // we optimize two special cases: firstly, where the node has no children, and secondly,
                // where it has a single text node as a child.

                if (depth[next] <= level) {
                    return UntypedAtomicValue.ZERO_LENGTH_UNTYPED;
                } else if (nodeKind[next] == Type.TEXT && depth[next+1] <= level) {
                    int length = beta[next];
                    int start = alpha[next];
                    return new UntypedAtomicValue(charBuffer.subSequence(start, start+length));
                } else if (nodeKind[next] == Type.WHITESPACE_TEXT && depth[next+1] <= level) {
                    return new UntypedAtomicValue(WhitespaceTextImpl.getStringValue(this, next));
                }

                // Now handle the general case

                FastStringBuffer sb = null;
                while (next < numberOfNodes && depth[next] > level) {
                    if (nodeKind[next]==Type.TEXT) {
                        if (sb==null) {
                            sb = new FastStringBuffer(1024);
                        }
                        sb.append(TinyTextImpl.getStringValue(this, next));
                    } else if (nodeKind[next]==Type.WHITESPACE_TEXT) {
                        if (sb==null) {
                            sb = new FastStringBuffer(1024);
                        }
                        WhitespaceTextImpl.appendStringValue(this, next, sb);
                    }
                    next++;
                }
                if (sb==null) {
                    return UntypedAtomicValue.ZERO_LENGTH_UNTYPED;
                } else {
                    return new UntypedAtomicValue(sb.condense());
                }

            case Type.TEXT:
                return new UntypedAtomicValue(TinyTextImpl.getStringValue(this, nodeNr));
            case Type.WHITESPACE_TEXT:
                return new UntypedAtomicValue(WhitespaceTextImpl.getStringValue(this, nodeNr));
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                int start2 = alpha[nodeNr];
                int len2 = beta[nodeNr];
                if (len2==0) return UntypedAtomicValue.ZERO_LENGTH_UNTYPED;
                char[] dest = new char[len2];
                commentBuffer.getChars(start2, start2+len2, dest, 0);
                return new StringValue(new CharSlice(dest, 0, len2));
            default:
                throw new IllegalStateException("Unknown node kind");
        }
    }

    /**
     * Make a (transient) attribute node from the array of attributes
     * @param nr the node number of the attribute
     * @return an attribute node
    */

    TinyAttributeImpl getAttributeNode(int nr) {
        return new TinyAttributeImpl(this, nr);
    }

    /**
     * Get the type annotation of an attribute node.
     * The bit {@link NodeInfo#IS_DTD_TYPE} (1<<30) will be set in the case of an attribute node if the type annotation
     * is one of ID, IDREF, or IDREFS and this is derived from DTD rather than schema validation.
     * @param nr the node number of the attribute
     * @return Type.UNTYPED_ATOMIC if there is no annotation
    */

    int getAttributeAnnotation(int nr) {
        if (attTypeCode == null) {
            return StandardNames.XS_UNTYPED_ATOMIC;
        } else {
            return attTypeCode[nr] & (NamePool.FP_MASK | NodeInfo.IS_DTD_TYPE);
        }
    }

    /**
     * Determine whether an attribute is an IDREF/IDREFS attribute. (The represents the
     * is-idref property in the data model)
     * @param nr the node number of the attribute
     * @return true if this is an IDREF/IDREFS attribute
     */

    public boolean isIdAttribute(int nr) {
        if (attTypeCode == null) {
            return false;
        }
        int tc = attTypeCode[nr];
        tc &= NamePool.FP_MASK;
        if (tc == StandardNames.XS_UNTYPED_ATOMIC) {
            return false;
        } else if (tc == StandardNames.XS_ID) {
            return true;
        } else if (tc < 1024) {
            return false;
        } else {
            final SchemaType type = getConfiguration().getSchemaType(tc);
            final TypeHierarchy th = getConfiguration().getTypeHierarchy();
            if (type.isAtomicType()) {
                return th.isSubType((AtomicType)type, BuiltInAtomicType.ID);
            } else if (type instanceof ListType) {
                // TODO: rules for ID's in list and union types
                SimpleType itemType = ((ListType)type).getItemType();
                return itemType.isAtomicType() &&
                        th.isSubType((AtomicType)itemType, BuiltInAtomicType.ID);
            }
        }
        return false;
    }


    /**
     * Determine whether an attribute is an IDREF/IDREFS attribute. (The represents the
     * is-idref property in the data model)
     * @param nr the node number of the attribute
     * @return true if this is an IDREF/IDREFS attribute
     */

    public boolean isIdrefAttribute(int nr) {
        if (attTypeCode == null) {
            return false;
        }
        int tc = attTypeCode[nr];
        if ((attTypeCode[nr] & TYPECODE_IDREF) != 0) {
            return true;
        }
        tc &= NamePool.FP_MASK;
        if (tc == StandardNames.XS_UNTYPED_ATOMIC) {
            return false;
        } else if (tc == StandardNames.XS_IDREF) {
            return true;
        } else if (tc == StandardNames.XS_IDREFS) {
            return true;
        } else if (tc < 1024) {
            return false;
        } else {
            final SchemaType type = getConfiguration().getSchemaType(tc);
            final TypeHierarchy th = getConfiguration().getTypeHierarchy();
            if (type.isAtomicType()) {
                return th.isSubType((AtomicType)type, BuiltInAtomicType.IDREF);
            } else if (type instanceof ListType) {
                SimpleType itemType = ((ListType)type).getItemType();
                return itemType.isAtomicType() &&
                        th.isSubType((AtomicType)itemType, BuiltInAtomicType.IDREF);
            }
        }
        return false;
    }

    

    /**
     * Ask whether an element is an IDREF/IDREFS element. (The represents the
     * is-idref property in the data model)
     * @param nr the element node whose is-idref property is required
     * @return true if the node has the is-idref property
     */

    public boolean isIdrefElement(int nr) {
        if (typeCodeArray == null) {
            return false;
        }
        int tc = typeCodeArray[nr];
        return (tc & TYPECODE_IDREF) != 0 ||
                getConfiguration().getTypeHierarchy().isIdrefsCode(tc & NamePool.FP_MASK);
    }


    /**
    * Set the system id of an element in the document. This identifies the external entity containing
     * the node - this is not necessarily the same as the base URI.
     * @param seq the node number
     * @param uri the system ID
    */

    void setSystemId(int seq, String uri) {
        if (uri==null) {
            uri = "";
        }
        if (systemIdMap==null) {
            systemIdMap = new SystemIdMap();
        }
        systemIdMap.setSystemId(seq, uri);
    }


    /**
     * Get the system id of an element in the document
     * @param seq the node number of the element node
     * @return the system id (base URI) of the element
    */

    String getSystemId(int seq) {
        if (systemIdMap==null) {
            return null;
        }
        return systemIdMap.getSystemId(seq);
    }

    /**
     * Get the root node for a given node
     * @param nodeNr the node number of the given node
     * @return the node number of the root of the tree containing the given node
     */

    int getRootNode(int nodeNr) {
        for (int i=rootIndexUsed-1; i>=0; i--) {
            if (rootIndex[i] <= nodeNr) {
                return rootIndex[i];
            }
        }
        return 0;
    }

    /**
    * Set line numbering on
    */

    public void setLineNumbering() {
        lineNumbers = new int[nodeKind.length];
        Arrays.fill(lineNumbers, -1);
        columnNumbers = new int[nodeKind.length];
        Arrays.fill(columnNumbers, -1);
    }

    /**
     * Set the line number for a node. Ignored if line numbering is off.
     * @param sequence the node number
     * @param line the line number to be set for the  node
     * @param column the column number for the node
    */

    void setLineNumber(int sequence, int line, int column) {
        if (lineNumbers != null) {
            lineNumbers[sequence] = line;
            columnNumbers[sequence] = column;
        }
    }

    /**
     * Get the line number for a node.
     * @param sequence the node number
     * @return the line number of the node. Return -1 if line numbering is off.
    */

    int getLineNumber(int sequence) {
        if (lineNumbers != null) {
            // find the nearest preceding node that has a known line number, and return it
            for (int i=sequence; i>=0; i--) {
                int c = lineNumbers[sequence];
                if (c > 0) {
                    return c;
                }
            }
        }
        return -1;
    }

    /**
     * Get the column number for a node.
     * @param sequence the node number
     * @return the line number of the node. Return -1 if line numbering is off.
    */

    int getColumnNumber(int sequence) {
        if (columnNumbers != null) {
            // find the nearest preceding node that has a known column number, and return it
            for (int i=sequence; i>=0; i--) {
                int c = columnNumbers[sequence];
                if (c > 0) {
                    return c;
                }
            }
        }
        return -1;
    }

    /**
     * Get the document number (actually, the tree number)
     * @return the unique number of this TinyTree structure
     */

    public int getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Ask whether a given node is nilled
     * @param nodeNr the node in question
     * @return true if the node has the nilled property
     */

    public boolean isNilled(int nodeNr) {
        return (typeCodeArray != null && (typeCodeArray[nodeNr] & NodeInfo.IS_NILLED) != 0);
    }

	/**
	* Produce diagnostic print of main tree arrays
	*/

	public void diagnosticDump() {
        NamePool pool = config.getNamePool();
		System.err.println("    node    type   depth    next   alpha    beta    name");
		for (int i=0; i<numberOfNodes; i++) {
			System.err.println(n8(i) + n8(nodeKind[i]) + n8(depth[i]) + n8(next[i]) +
									 n8(alpha[i]) + n8(beta[i]) + n8(nameCode[i]) +
                                     (nameCode[i]== -1 ? "" : " " + pool.getDisplayName(nameCode[i])));
		}
		System.err.println("    attr  parent    name    value");
		for (int i=0; i<numberOfAttributes; i++) {
		    System.err.println(n8(i) + n8(attParent[i]) + n8(attCode[i]) + "    " + attValue[i]);
		}
		System.err.println("      ns  parent  prefix     uri");
		for (int i=0; i<numberOfNamespaces; i++) {
		    System.err.println(n8(i) + n8(namespaceParent[i]) + n8(namespaceCode[i]>>16) + n8(namespaceCode[i]&0xffff));
		}
	}

    /**
     * Create diagnostic dump of the tree containing a particular node.
     * Designed to be called as an extension function for diagnostics.
     * @param node the node in question
     */

    public static synchronized void diagnosticDump(NodeInfo node) {
        if (node instanceof TinyNodeImpl) {
            TinyTree tree = ((TinyNodeImpl)node).tree;
            System.err.println("Tree containing node " + ((TinyNodeImpl)node).nodeNr);
            tree.diagnosticDump();
        } else {
            System.err.println("Node is not in a TinyTree");
        }
    }

    /**
     * Output a number as a string of 8 characters
     * @param val the number
     * @return the string representation of the number, aligned in a fixed width field
    */

    private String n8(int val) {
        String s = "        " + val;
        return s.substring(s.length()-8);
    }

    /**
     * Output a statistical summary to System.err
     */

    public void showSize() {
        System.err.println("Tree size: " + numberOfNodes + " nodes, " + charBuffer.length() + " characters, " +
                                numberOfAttributes + " attributes");
    }

    /**
     * Update the statistics held in static data. We don't bother to sychronize, on the basis that it doesn't
     * matter if the stats are wrong.
     */

    private void updateStatistics() {
        int n0 = treesCreated;
        int n1 = treesCreated + 1;
        treesCreated = n1;
        averageNodes = ((averageNodes * n0) + numberOfNodes) / n1;
        if (averageNodes < 10.0) {
            averageNodes = 10.0;
        }
        averageAttributes = ((averageAttributes * n0) + numberOfAttributes) / n1;
        if (averageAttributes < 10.0) {
            averageAttributes = 10.0;
        }
        averageNamespaces = ((averageNamespaces * n0) + numberOfNamespaces) / n1;
        if (averageNamespaces < 5.0) {
            averageNamespaces = 5.0;
        }
        averageCharacters = ((averageCharacters * n0) + charBuffer.length()) / n1;
        if (averageCharacters < 100.0) {
            averageCharacters = 100.0;
        }

    }

    /**
     * Get the number of nodes in the tree, excluding attributes and namespace nodes
     * @return the number of nodes.
     */

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    /**
     * Get the number of attributes in the tree
     * @return  the number of attributes
     */

    public int getNumberOfAttributes() {
        return numberOfAttributes;
    }

    /**
     * Get the number of namespace declarations in the tree
     * @return  the number of namespace declarations
     */

    public int getNumberOfNamespaces() {
        return numberOfNamespaces;
    }

    /**
     * Get the array holding node kind information
     * @return an array of bytes, byte N is the node kind of node number N
     */

    public byte[] getNodeKindArray() {
        return nodeKind;
    }

    /**
     * Get the array holding node depth information
     * @return an array of shorts, byte N is the node depth of node number N
     */

    public short[] getNodeDepthArray() {
        return depth;
    }

    /**
     * Get the array holding node name information
     * @return an array of integers, integer N is the name code of node number N
     */

    public int[] getNameCodeArray() {
        return nameCode;
    }

    /**
     * Get the array holding node type information
     * @return an array of integers, integer N is the type code of node number N
     */

    public int[] getTypeCodeArray() {
        return typeCodeArray;
    }

    /**
     * Get the array holding next-sibling pointers
     * @return an array of integers, integer N is the next-sibling pointer for node number N
     */

    public int[] getNextPointerArray() {
        return next;
    }

    /**
     * Get the array holding alpha information
     * @return an array of integers, whose meaning depends on the node kind. For elements it is a pointer
     * to the first attribute, for text, comment, and processing instruction nodes it is a pointer to the content
     */

    public int[] getAlphaArray() {
        return alpha;
    }

    /**
     * Get the array holding beta information
     * @return an array of integers, whose meaning depends on the node kind. For elements it is a pointer
     * to the first namespace declaration
     */

    public int[] getBetaArray() {
        return beta;
    }

    /**
     * Get the character buffer used to hold all the text data of the document
     * @return  the character buffer
     */

    public CharSequence getCharacterBuffer() {
        //return new CharSlice(charBuffer, 0, charBufferLength);
        return charBuffer;
    }

    /**
     * Get the character buffer used to hold all the comment data of the document
     * @return  the character buffer used for comments
     */

    public CharSequence getCommentBuffer() {
        return commentBuffer;
    }

    /**
     * Get the array used to hold the name codes of all attributes
     * @return an integer array; the Nth integer holds the attribute name code of attribute N
     */

    public int[] getAttributeNameCodeArray() {
        return attCode;
    }

    /**
     * Get the array used to hold the type codes of all attributes
     * @return an integer array; the Nth integer holds the attribute type code of attribute N
     */

    public int[] getAttributeTypeCodeArray() {
        return attTypeCode;
    }

    /**
     * Get the array used to hold the parent pointers of all attributes
     * @return an integer array; the Nth integer holds the pointer to the parent element of attribute N
     */

    public int[] getAttributeParentArray() {
        return attParent;
    }

    /**
     * Get the array used to hold the name codes of all attributes
     * @return an array of strings; the Nth string holds the string value of attribute N
     */

    public CharSequence[] getAttributeValueArray() {
        return attValue;
    }

    /**
     * Get the array used to hold the namespace codes of namespace declarations
     * @return an array of integer namespace codes
     */

    public int[] getNamespaceCodeArray() {
        return namespaceCode;
    }

    /**
     * Get the array used to hold the parent pointers of all namespace declarations
     * @return an integer array; the Nth integer holds the pointer to the parent element of namespace N
     */

    public int[] getNamespaceParentArray() {
        return namespaceParent;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//
