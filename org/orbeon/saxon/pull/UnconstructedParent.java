package org.orbeon.saxon.pull;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.StackFrame;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.XPathContextMajor;
import org.orbeon.saxon.instruct.ParentNodeConstructor;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.UncheckedXPathException;

/**
  * This class represents a virtual element node, the result of an element constructor that
  * (in general) hasn't been fully evaluated. This is similar to a Closure, except that it is
  * a NodeInfo rather than a Value. The object is capable of materializing the element if it
  * is actually needed, but the aim is to avoid materializing the element wherever possible,
  * at any rate not until its parent element is constructed so that this element can be built
  * in-situ rather than being built as a standalone element and then later copied.
 *
 * This class is not currently used for elements that require schema validation.
 */
public abstract class UnconstructedParent implements NodeInfo {

    protected ParentNodeConstructor instruction;
    protected XPathContextMajor savedXPathContext;
    protected NodeInfo node = null;

    /**
     * Create an UnconstructedParent node
     * @param instruction the instruction responsible for creating this element or document node
     * @param context the XPath dynamic context
     */

    protected UnconstructedParent(ParentNodeConstructor instruction, XPathContext context) {
        this.instruction = instruction;
        savedXPathContext = context.newContext();
        savedXPathContext.setOriginatingConstructType(Location.LAZY_EVALUATION);

        // Make a copy of all local variables. If the value of any local variable is a closure
        // whose depth exceeds a certain threshold, we evaluate the closure eagerly to avoid
        // creating deeply nested lists of Closures, which consume memory unnecessarily

        // We only copy the local variables if the expression has dependencies on local variables.
        // It would be even smarter to copy only those variables that we need; but that gives
        // diminishing returns.

        if ((instruction.getDependencies() & StaticProperty.DEPENDS_ON_LOCAL_VARIABLES) != 0) {
            StackFrame localStackFrame = context.getStackFrame();
            ValueRepresentation[] local = localStackFrame.getStackFrameValues();
            if (local != null) {
                ValueRepresentation[] savedStackFrame = new ValueRepresentation[local.length];
                System.arraycopy(local, 0, savedStackFrame, 0, local.length);
                savedXPathContext.setStackFrame(localStackFrame.getStackFrameMap(), savedStackFrame);
            }
        }

        // Make a copy of the context item
        SequenceIterator currentIterator = context.getCurrentIterator();
        if (currentIterator != null) {
            Item contextItem = currentIterator.current();
            UnfailingIterator single = SingletonIterator.makeIterator(contextItem);
            single.next();
            savedXPathContext.setCurrentIterator(single);
            // we don't save position() and last() because we have no way
            // of restoring them. Instead, we prevent lazy construction if there is a dependency
            // on position() or last()
        }
        savedXPathContext.setReceiver(new SequenceOutputter());
    }

    /**
     * Get the saved XPath dynamic context
     * @return the saved context
     */

    public XPathContext getXPathContext() {
        return savedXPathContext;
    }

    /**
     * Get the instruction responsible for creating this element or document node
     * @return the relevant instruction
     */

    public ParentNodeConstructor getInstruction() {
        return instruction;
    }

    /**
     * Get a PullProvider that delivers the stream of events corresponding to the tree rooted at this virtual node
     * @return the PullProvider for the events in the virtual tree
     */

    public PullProvider getPuller() {
        if (node == null) {
            VirtualTreeWalker walker = new VirtualTreeWalker(instruction, savedXPathContext);
            walker.setPipelineConfiguration(savedXPathContext.getController().makePipelineConfiguration());
            walker.setNameCode(getNameCode());
            return walker;
        } else {
            return TreeWalker.makeTreeWalker(node);
        }
    }

    /**
     * Method to construct the node when this is required.
     *
     * @throws XPathException if any failure occurs
     */
    void construct() throws XPathException {
        PipelineConfiguration pipe = savedXPathContext.getController().makePipelineConfiguration();
        PullProvider puller = getPuller();
        puller.setPipelineConfiguration(pipe);
        TinyBuilder builder = new TinyBuilder();
        builder.setPipelineConfiguration(pipe);
        builder.setSystemId(pipe.getLocationProvider().getSystemId(instruction.getLocationId()));
        builder.setBaseURI(pipe.getLocationProvider().getSystemId(instruction.getLocationId()));
        NamespaceReducer reducer = new NamespaceReducer();
        reducer.setUnderlyingReceiver(builder);
        reducer.setPipelineConfiguration(pipe);
        ComplexContentOutputter outputter = new ComplexContentOutputter();
        outputter.setReceiver(reducer);
        outputter.setPipelineConfiguration(pipe);
        //outputter.open();
        new PullPushCopier(puller, outputter).copy();
        //outputter.close();

        node = builder.getCurrentRoot();
    }

    /**
     * Method to construct the node when this is required.
     * <p>
     * Note that this may throw an UncheckedXPathException. This is because many of the methods on the
     * NodeInfo class are exception-free; we can't throw an XPathException on these interfaces, but may need
     * to in this case because lazy computation of expressions may throw errors.
     *
     * @throws UncheckedXPathException
     */

    void tryToConstruct() {
        try {
            construct();
        } catch (XPathException err) {
            throw new UncheckedXPathException(err);
        }
    }

    /**
     * Determine whether this is the same node as another node.
     * Note: a.isSameNodeInfo(b) if and only if generateId(a)==generateId(b).
     * This method has the same semantics as isSameNode() in DOM Level 3, but
     * works on Saxon NodeInfo objects rather than DOM Node objects.
     *
     * @param other the node to be compared with this node
     * @return true if this NodeInfo object and the supplied NodeInfo object represent
     *         the same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        return this == other ||
                !(other instanceof UnconstructedParent) &&
                        node != null &&
                        node.isSameNodeInfo(other);
    }

   /**
      * The equals() method compares nodes for identity. It is defined to give the same result
      * as isSameNodeInfo().
      * @param other the node to be compared with this node
      * @return true if this NodeInfo object and the supplied NodeInfo object represent
      *      the same node in the tree.
      * @since 8.7 Previously, the effect of the equals() method was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics. It is safer to use isSameNodeInfo() for this reason.
      * The equals() method has been defined because it is useful in contexts such as a Java Set or HashMap.
      */

     public boolean equals(Object other) {
       return other instanceof NodeInfo && isSameNodeInfo((NodeInfo)other);
   }

     /**
      * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
      * (represent the same node) then they must have the same hashCode()
      * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics.
      */

     public int hashCode() {
         if (node != null) {
             return node.hashCode();
         } else {
             return super.hashCode();
         }
     }

    /**
     * Get the System ID for the node (that is, the document URI of the containing document node).
     *
     * @return the System Identifier of the source document
     *         containing the node, or null if not known. Note this is not the
     *         same as the base URI: the base URI can be modified by xml:base, but
     *         the system ID cannot.
     */

    public String getSystemId() {
        return null;
//        if (node == null) {
//            tryToConstruct();
//        }
//        return node.getSystemId();
    }

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. This will be the same as the System ID unless xml:base has been used.
     *
     * @return the base URI of the node
     */

    public String getBaseURI() {
        if (node == null) {
            // the base URI of a constructed parentless document or element node is the static base URI of the
            // instruction/expression that created it
            PipelineConfiguration pipe = savedXPathContext.getController().makePipelineConfiguration();
            return pipe.getLocationProvider().getSystemId(instruction.getLocationId());
        } else {
            return node.getBaseURI();
        }
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
     * Get column number
     * @return the column number of the node in its original source document; or -1 if not available
     */

    public int getColumnNumber() {
        return -1;
    }        

    /**
     * Determine the relative position of this node and another node, in document order.
     * The other node will always be in the same document.
     *
     * @param other The other node, whose position is to be compared with this
     *              node
     * @return -1 if this node precedes the other node, +1 if it follows the
     *         other node, or 0 if they are the same node. (In this case,
     *         isSameNode() will always return true, and the two nodes will
     *         produce the same result for generateId())
     */

    public int compareOrder(NodeInfo other) {
        if (node == null) {
            tryToConstruct();
        }
        return node.compareOrder(other);
    }

    /**
     * Return the string value of the node. The interpretation of this depends on the type
     * of node. For an element it is the accumulated character content of the element,
     * including descendant elements.
     *
     * @return the string value of the node
     */

    public String getStringValue() {
        return getStringValueCS().toString();
    }

    /**
     * Get fingerprint. The fingerprint is a coded form of the expanded name
     * of the node: two nodes
     * with the same name code have the same namespace URI and the same local name.
     * A fingerprint of -1 should be returned for a node with no name.
     *
     * @return an integer fingerprint; two nodes with the same fingerprint have
     *         the same expanded QName
     */

    public int getFingerprint() {
        int nc = getNameCode();
        if (nc == -1) {
            return -1;
        }
        return nc & NamePool.FP_MASK;
    }

    /**
     * Get the local part of the name of this node. This is the name after the ":" if any.
     *
     * @return the local part of the name. For an unnamed node, returns "". Unlike the DOM
     *         interface, this returns the full name in the case of a non-namespaced name.
     */

    public String getLocalPart() {
        return getNamePool().getLocalName(getNameCode());
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For an unnamed node,
     *         or for a node with an empty prefix, return an empty
     *         string.
     */

    public String getURI() {
        return getNamePool().getURI(getNameCode());
    }

    /**
     * Get the display name of this node. For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node. For a node with no name, return
     *         an empty string.
     */

    public String getDisplayName() {
        return getNamePool().getDisplayName(getNameCode());
    }

    /**
     * Get the prefix of the name of the node. This is defined only for elements and attributes.
     * If the node has no prefix, or for other kinds of node, return a zero-length string.
     *
     * @return The prefix of the name of the node.
     */

    public String getPrefix() {
        return getNamePool().getPrefix(getNameCode());
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return savedXPathContext.getConfiguration();
    }

    /**
     * Get the NamePool that holds the namecode for this node
     *
     * @return the namepool
     */

    public NamePool getNamePool() {
        return getConfiguration().getNamePool();
    }

    /**
     * Get the type annotation of this node, if any.
     * Returns -1 for kinds of nodes that have no annotation
     *
     * @return the type annotation of the node.
     * @see org.orbeon.saxon.type.Type
     */

    public int getTypeAnnotation() {
        return StandardNames.XS_UNTYPED;
    }

    /**
     * Get the NodeInfo object representing the parent of this node
     *
     * @return the parent of this node; null if this node has no parent
     */

    public NodeInfo getParent() {
        return null;
    }

    /**
     * Return an iteration over all the nodes reached by the given axis from this node
     *
     * @param axisNumber an integer identifying the axis; one of the constants
     *                   defined in class org.orbeon.saxon.om.Axis
     * @return an AxisIterator that scans the nodes reached by the axis in
     *         turn.
     * @throws UnsupportedOperationException if the namespace axis is
     *                                       requested and this axis is not supported for this implementation.
     * @see org.orbeon.saxon.om.Axis
     */

    public AxisIterator iterateAxis(byte axisNumber) {
        if (node == null) {
            tryToConstruct();
        }
        return node.iterateAxis(axisNumber);
    }

    /**
     * Return an iteration over all the nodes reached by the given axis from this node
     * that match a given NodeTest
     *
     * @param axisNumber an integer identifying the axis; one of the constants
     *                   defined in class org.orbeon.saxon.om.Axis
     * @param nodeTest   A pattern to be matched by the returned nodes; nodes
     *                   that do not match this pattern are not included in the result
     * @return a NodeEnumeration that scans the nodes reached by the axis in
     *         turn.
     * @throws UnsupportedOperationException if the namespace axis is
     *                                       requested and this axis is not supported for this implementation.
     * @see org.orbeon.saxon.om.Axis
     */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        if (node == null) {
            tryToConstruct();
        }
        return node.iterateAxis(axisNumber, nodeTest);
    }

    /**
     * Get the value of a given attribute of this node
     *
     * @param fingerprint The fingerprint of the attribute name
     * @return the attribute value if it exists or null if not
     */

    public String getAttributeValue(int fingerprint) {
        if (node == null) {
            tryToConstruct();
        }
        return node.getAttributeValue(fingerprint);
    }

    /**
     * Get the root node of the tree containing this node
     *
     * @return the NodeInfo representing the top-level ancestor of this node.
     *         This will not necessarily be a document node
     */

    public NodeInfo getRoot() {
        return this;
    }

    /**
     * Get the root node, if it is a document node.
     *
     * @return the DocumentInfo representing the containing document. If this
     *         node is part of a tree that does not have a document node as its
     *         root, return null.
     */

    public DocumentInfo getDocumentRoot() {
        return null;
    }

    /**
     * Determine whether the node has any children. <br />
     * Note: the result is equivalent to <br />
     * getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()
     *
     * @return True if the node has one or more children
     */

    public boolean hasChildNodes() {
        if (node == null) {
            tryToConstruct();
        }
        return node.hasChildNodes();
    }

    /**
     * Get a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @param buffer a buffer, into which will be placed
     *         a string that uniquely identifies this node, across all
     *         documents.
     */

    public void generateId(FastStringBuffer buffer) {
        if (node == null) {
            tryToConstruct();
        }
        node.generateId(buffer);
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        if (node == null) {
            tryToConstruct();
        }
        return node.getDocumentNumber();
    }

    /**
     * Copy this node to a given outputter
     *
     * @param out             the Receiver to which the node should be copied
     * @param whichNamespaces in the case of an element, controls
     *                        which namespace nodes should be copied. Values are {@link #NO_NAMESPACES},
     *                        {@link #LOCAL_NAMESPACES}, {@link #ALL_NAMESPACES}
     * @param copyAnnotations indicates whether the type annotations
     *                        of element and attribute nodes should be copied
     * @param locationId      If non-zero, identifies the location of the instruction
     *                        that requested this copy. If zero, indicates that the location information
     *                        for the original node is to be copied; in this case the Receiver must be
     *                        a LocationCopier
     * @throws org.orbeon.saxon.trans.XPathException
     *
     */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        if (node == null) {
            if (whichNamespaces == NodeInfo.ALL_NAMESPACES && copyAnnotations) {
                PullProvider pull = new VirtualTreeWalker(instruction, savedXPathContext);
                pull.setPipelineConfiguration(out.getPipelineConfiguration());
                PullPushCopier copier = new PullPushCopier(pull, out);
                copier.append();
                return;
            } else {
                construct();
            }
        }
        node.copy(out, whichNamespaces, copyAnnotations, locationId);
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
        if (node == null) {
            tryToConstruct();
        }
        return node.getDeclaredNamespaces(buffer);
    }

    /**
     * Set the system identifier for this Source.
     * <p/>
     * <p>The system identifier is optional if the source does not
     * get its data from a URL, but it may still be useful to provide one.
     * The application can use a system identifier, for example, to resolve
     * relative URIs and to include in error messages and warnings.</p>
     *
     * @param systemId The system identifier as a URL string.
     */
    public void setSystemId(String systemId) {
        //
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        if (node == null) {
            //construct();
            try {
                PullProvider puller = getPuller();
                puller.next();  // assert: it's a START_DOCUMENT or START_ELEMENT
                return puller.getStringValue();
            } catch (XPathException e) {
                throw new UncheckedXPathException(e);
            }
        }
        return node.getStringValueCS();
    }

    /**
     * Get the typed value of the item
     *
     * @return the typed value of the item. In general this will be a sequence
     * @throws org.orbeon.saxon.trans.XPathException
     *          where no typed value is available, e.g. for
     *          an element with complex content
     */

    public SequenceIterator getTypedValue() throws XPathException {
        if (node == null) {
            construct();
        }
        return node.getTypedValue();
    }

    /**
     * Get the typed value. The result of this method will always be consistent with the method
     * {@link org.orbeon.saxon.om.Item#getTypedValue()}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     *
     * @return the typed value. If requireSingleton is set to true, the result will always be an
     *         AtomicValue. In other cases it may be a Value representing a sequence whose items are atomic
     *         values.
     * @since 8.5
     */

    public Value atomize() throws XPathException {
        if (node == null) {
            construct();
        }
        return node.atomize();
    }

    /**
     * Determine whether this node has the is-id property
     *
     * @return true if the node is an ID
     */

    public boolean isId() {
        return false;
    }

    /**
     * Determine whether this node has the is-idref property
     *
     * @return true if the node is an IDREF or IDREFS element or attribute
     */

    public boolean isIdref() {
        return false;
    }

    /**
     * Determine whether the node has the is-nilled property
     *
     * @return true if the node has the is-nilled property
     */

    public boolean isNilled() {
        return false;
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
