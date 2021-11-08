package org.orbeon.saxon.tinytree;

import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pull.PullProvider;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.Configuration;

import javax.xml.transform.SourceLocator;
import java.util.List;

/**
 * This implementation of the Saxon pull interface starts from a document, element,
 * text, comment, or processing-instruction node in a TinyTree,
 * and returns the events corresponding to that node and its descendants (including
 * their attributes and namespaces). The class performs the same function as
 * the general-purpose {@link org.orbeon.saxon.pull.TreeWalker} class, but is
 * specialized to exploit the TinyTree data structure: in particular, it never
 * materializes any Node objects.
 */

public class TinyTreeWalker implements PullProvider, SourceLocator {

    private int startNode;
    private int currentNode;
    private int currentEvent;
    private TinyTree tree;
    private PipelineConfiguration pipe;
    private NamespaceDeclarationsImpl nsDeclarations;
    private int[] nsBuffer = new int[10];

    /**
     * Create a TinyTreeWalker to return events associated with a tree or subtree
     * @param startNode the root of the tree or subtree. Must be a document, element, text,
     * comment, or processing-instruction node.
     * @throws IllegalArgumentException if the start node is an attribute or namespace node.
     */

    public TinyTreeWalker(TinyNodeImpl startNode) {
        int kind = startNode.getNodeKind();
        if (kind == Type.ATTRIBUTE || kind == Type.NAMESPACE) {
            throw new IllegalArgumentException("TinyTreeWalker cannot start at an attribute or namespace node");
        }
        this.startNode = startNode.nodeNr;
        tree = startNode.tree;
        nsDeclarations = new NamespaceDeclarationsImpl();
        nsDeclarations.setNamePool(startNode.getNamePool());
    }

    /**
     * Set configuration information. This must only be called before any events
     * have been read.
     */

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
    }

    /**
     * Get configuration information.
     */

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    /**
     * Get the next event
     *
     * @return an integer code indicating the type of event. The code
     *         {@link #END_OF_INPUT} is returned if there are no more events to return.
     */

    public int next() throws XPathException {
        switch(currentEvent) {
            case START_OF_INPUT:
                currentNode = startNode;
                switch (tree.nodeKind[currentNode]) {
                    case Type.DOCUMENT:
                        currentEvent = START_DOCUMENT;
                        break;
                    case Type.ELEMENT:
                        currentEvent = START_ELEMENT;
                        break;
                    case Type.TEXT:
                    case Type.WHITESPACE_TEXT:
                        currentEvent = TEXT;
                        break;
                    case Type.COMMENT:
                        currentEvent = COMMENT;
                        break;
                    case Type.PROCESSING_INSTRUCTION:
                        currentEvent = PROCESSING_INSTRUCTION;
                        break;
                    case Type.PARENT_POINTER:
                        throw new IllegalStateException("Current node is a parent-pointer pseudo-node");
                }
                return currentEvent;

            case START_DOCUMENT:
            case START_ELEMENT:

                if (tree.depth[currentNode+1] > tree.depth[currentNode]) {
                    // the current element or document has children: move to the first child
                    switch (tree.nodeKind[++currentNode]) {
                        case Type.ELEMENT:
                            currentEvent = START_ELEMENT;
                            break;
                        case Type.TEXT:
                        case Type.WHITESPACE_TEXT:
                            currentEvent = TEXT;
                            break;
                        case Type.COMMENT:
                            currentEvent = COMMENT;
                            break;
                        case Type.PROCESSING_INSTRUCTION:
                            currentEvent = PROCESSING_INSTRUCTION;
                            break;
                        case Type.PARENT_POINTER:
                            throw new IllegalStateException("First child node must not be a parent-pointer pseudo-node");
                    }
                    return currentEvent;
                } else {
                    if (currentEvent == START_DOCUMENT) {
                        currentEvent = END_DOCUMENT;
                    } else {
                        currentEvent = END_ELEMENT;
                    }
                    return currentEvent;
                }
            case END_ELEMENT:
            case TEXT:
            case COMMENT:
            case PROCESSING_INSTRUCTION:
                if (currentNode == startNode) {
                    currentEvent = END_OF_INPUT;
                    return currentEvent;
                }
                int next = tree.next[currentNode];
                if (next > currentNode) {
                    // this node has a following sibling
                    currentNode = tree.next[currentNode];
                    do {
                        switch (tree.nodeKind[currentNode]) {
                            case Type.ELEMENT:
                                currentEvent = START_ELEMENT;
                                break;
                            case Type.TEXT:
                            case Type.WHITESPACE_TEXT:
                                currentEvent = TEXT;
                                break;
                            case Type.COMMENT:
                                currentEvent = COMMENT;
                                break;
                            case Type.PROCESSING_INSTRUCTION:
                                currentEvent = PROCESSING_INSTRUCTION;
                                break;
                            case Type.PARENT_POINTER:
                                // skip this pseudo-node
                                currentEvent = -1;
                                currentNode++;
                                break;
                        }
                    } while (currentEvent == -1);
                    return currentEvent;
                } else {
                    // return to the parent element or document
                    currentNode = next;
                    if (currentNode == -1) {
                        // indicates we were at the END_ELEMENT of a parentless element node
                        currentEvent = END_OF_INPUT;
                        return currentEvent;
                    }
                    switch (tree.nodeKind[currentNode]) {
                        case Type.ELEMENT:
                            currentEvent = END_ELEMENT;
                            break;
                        case Type.DOCUMENT:
                            currentEvent = END_DOCUMENT;
                            break;
                    }
                    return currentEvent;
                }

            case ATTRIBUTE:
            case NAMESPACE:
            case END_DOCUMENT:
                currentEvent = END_OF_INPUT;
                return currentEvent;

            case END_OF_INPUT:
                throw new IllegalStateException("Cannot call next() when input is exhausted");

            default:
                throw new IllegalStateException("Unrecognized event " + currentEvent);

        }
    }

    /**
     * Get the event most recently returned by next(), or by other calls that change
     * the position, for example getStringValue() and skipToMatchingEnd(). This
     * method does not change the position of the PullProvider.
     *
     * @return the current event
     */

    public int current() {
        return currentEvent;
    }

    /**
     * Get the attributes associated with the current element. This method must
     * be called only after a START_ELEMENT event has been notified. The contents
     * of the returned AttributeCollection are guaranteed to remain unchanged
     * until the next START_ELEMENT event, but may be modified thereafter. The object
     * should not be modified by the client.
     * <p/>
     * <p>Attributes may be read before or after reading the namespaces of an element,
     * but must not be read after the first child node has been read, or after calling
     * one of the methods skipToEnd(), getStringValue(), or getTypedValue().</p>
     *
     * @return an AttributeCollection representing the attributes of the element
     *         that has just been notified.
     */

    public AttributeCollection getAttributes() throws XPathException {
        if (tree.nodeKind[currentNode] == Type.ELEMENT) {
            if (tree.alpha[currentNode] == -1) {
                return AttributeCollectionImpl.EMPTY_ATTRIBUTE_COLLECTION;
            }
            return new TinyAttributeCollection(tree, currentNode);
        } else {
            throw new IllegalStateException("getAttributes() called when current event is not ELEMENT_START");
        }
    }

    /**
     * Get the namespace declarations associated with the current element. This method must
     * be called only after a START_ELEMENT event has been notified. In the case of a top-level
     * START_ELEMENT event (that is, an element that either has no parent node, or whose parent
     * is not included in the sequence being read), the NamespaceDeclarations object returned
     * will contain a namespace declaration for each namespace that is in-scope for this element
     * node. In the case of a non-top-level element, the NamespaceDeclarations will contain
     * a set of namespace declarations and undeclarations, representing the differences between
     * this element and its parent.
     * <p/>
     * <p>It is permissible for this method to return namespace declarations that are redundant.</p>
     * <p/>
     * <p>The NamespaceDeclarations object is guaranteed to remain unchanged until the next START_ELEMENT
     * event, but may then be overwritten. The object should not be modified by the client.</p>
     * <p/>
     * <p>Namespaces may be read before or after reading the attributes of an element,
     * but must not be read after the first child node has been read, or after calling
     * one of the methods skipToEnd(), getStringValue(), or getTypedValue().</p>*
     */

    public NamespaceDeclarations getNamespaceDeclarations() throws XPathException {
        if (tree.nodeKind[currentNode] == Type.ELEMENT) {
            int[] decl;
            if (currentNode == startNode) {
                // get all inscope namespaces for a top-level element in the sequence.
                decl = TinyElementImpl.getInScopeNamespaces(tree, currentNode, nsBuffer);
            } else {
                // only namespace declarations (and undeclarations) on this element are required
                decl = TinyElementImpl.getDeclaredNamespaces(tree, currentNode, nsBuffer);
            }
            nsDeclarations.setNamespaceCodes(decl);
            return nsDeclarations;
        }
        throw new IllegalStateException("getNamespaceDeclarations() called when current event is not START_ELEMENT");
    }

    /**
     * Skip the current subtree. This method may be called only immediately after
     * a START_DOCUMENT or START_ELEMENT event. This call returns the matching
     * END_DOCUMENT or END_ELEMENT event; the next call on next() will return
     * the event following the END_DOCUMENT or END_ELEMENT.
     */

    public int skipToMatchingEnd() throws XPathException {
        // For this implementation, we simply leave the current node unchanged, and change
        // the current event
        switch (currentEvent) {
            case START_DOCUMENT:
                currentEvent = END_DOCUMENT;
                return currentEvent;
            case START_ELEMENT:
                currentEvent = END_ELEMENT;
                return currentEvent;
            default:
                throw new IllegalStateException(
                        "Cannot call skipToMatchingEnd() except when at start of element or document");

        }
    }

    /**
     * Close the event reader. This indicates that no further events are required.
     * It is not necessary to close an event reader after {@link #END_OF_INPUT} has
     * been reported, but it is recommended to close it if reading terminates
     * prematurely. Once an event reader has been closed, the effect of further
     * calls on next() is undefined.
     */

    public void close() {
        // no action
    }

    /**
     * Get the namePool used to lookup all name codes and namespace codes
     *
     * @return the namePool
     */

    public NamePool getNamePool() {
        return pipe.getConfiguration().getNamePool();
    }

    /**
     * Get the nameCode identifying the name of the current node. This method
     * can be used after the {@link #START_ELEMENT}, {@link #PROCESSING_INSTRUCTION},
     * {@link #ATTRIBUTE}, or {@link #NAMESPACE} events. With some PullProvider implementations,
     * including this one, it can also be used after {@link #END_ELEMENT}.
     * If called at other times, the result is undefined and may result in an IllegalStateException.
     * If called when the current node is an unnamed namespace node (a node representing the default namespace)
     * the returned value is -1.
     * @return the nameCode. The nameCode can be used to obtain the prefix, local name,
     * and namespace URI from the name pool.
     */

    public int getNameCode() {
        switch (currentEvent) {
            case START_ELEMENT:
            case PROCESSING_INSTRUCTION:
            case END_ELEMENT:
                return tree.nameCode[currentNode];
            default:
                throw new IllegalStateException("getNameCode() called when its value is undefined");
        }
    }

    /**
     * Get the fingerprint of the name of the element. This is similar to the nameCode, except that
     * it does not contain any information about the prefix: so two elements with the same fingerprint
     * have the same name, excluding prefix. This method
     * can be used after the {@link #START_ELEMENT}, {@link #END_ELEMENT}, {@link #PROCESSING_INSTRUCTION},
     * {@link #ATTRIBUTE}, or {@link #NAMESPACE} events.
     * If called at other times, the result is undefined and may result in an IllegalStateException.
     * If called when the current node is an unnamed namespace node (a node representing the default namespace)
     * the returned value is -1.
     *
     * @return the fingerprint. The fingerprint can be used to obtain the local name
     *         and namespace URI from the name pool.
     */

    public int getFingerprint() {
        int nc = getNameCode();
        if (nc == -1) {
            return -1;
        } else {
            return nc & NamePool.FP_MASK;
        }
    }

    /**
     * Get the string value of the current attribute, text node, processing-instruction,
     * or atomic value.
     * This method cannot be used to obtain the string value of an element, or of a namespace
     * node. If the most recent event was anything other than {@link #START_ELEMENT}, {@link #TEXT},
     * {@link #PROCESSING_INSTRUCTION}, or {@link #ATOMIC_VALUE}, the result is undefined.
     */

    public CharSequence getStringValue() throws XPathException {
        switch (tree.nodeKind[currentNode]) {
            case Type.TEXT:
                return TinyTextImpl.getStringValue(tree, currentNode);
            case Type.WHITESPACE_TEXT:
                return WhitespaceTextImpl.getStringValue(tree, currentNode);
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                // sufficiently rare that instantiating the node is OK
                return tree.getNode(currentNode).getStringValue();
            case Type.ELEMENT:
                currentEvent = END_ELEMENT;
                return TinyParentNodeImpl.getStringValue(tree, currentNode);
            case Type.PARENT_POINTER:
                throw new IllegalStateException("Trying to get string value of a parent-pointer pseudo node");
        }
        return null;
    }

    /**
     * Get an atomic value. This call may be used only when the last event reported was
     * ATOMIC_VALUE. This indicates that the PullProvider is reading a sequence that contains
     * a free-standing atomic value; it is never used when reading the content of a node.
     */

    public AtomicValue getAtomicValue() {
        throw new IllegalStateException();
    }

    /**
     * Get the type annotation of the current attribute or element node, or atomic value.
     * The result of this method is undefined unless the most recent event was START_ELEMENT,
     * START_CONTENT, ATTRIBUTE, or ATOMIC_VALUE.
     *
     * @return the type code. This code is the fingerprint of a type name, which may be
     *         resolved to a {@link org.orbeon.saxon.type.SchemaType} by access to the Configuration.
     */

    public int getTypeAnnotation() {
        if (tree.nodeKind[currentNode] != Type.ELEMENT) {
            throw new IllegalStateException("getTypeAnnotation() called when current event is not ELEMENT_START or ");
        }
        if (tree.typeCodeArray == null) {
            return StandardNames.XS_UNTYPED;
        }
        return tree.typeCodeArray[currentNode] & NamePool.FP_MASK;
    }

    /**
     * Get the location of the current event.
     * For an event stream representing a real document, the location information
     * should identify the location in the lexical XML source. For a constructed document, it should
     * identify the location in the query or stylesheet that caused the node to be created.
     * A value of null can be returned if no location information is available.
     */

    public SourceLocator getSourceLocator() {
        return this;
    }

    /**
     * Return the public identifier for the current document event.
     * <p/>
     * <p>The return value is the public identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     *
     * @return A string containing the public identifier, or
     *         null if none is available.
     * @see #getSystemId
     */
    public String getPublicId() {
        return null;
    }

    /**
     * Return the system identifier for the current document event.
     * <p/>
     * <p>The return value is the system identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     * <p/>
     * <p>If the system identifier is a URL, the parser must resolve it
     * fully before passing it to the application.</p>
     *
     * @return A string containing the system identifier, or null
     *         if none is available.
     * @see #getPublicId
     */
    public String getSystemId() {
        return tree.getSystemId(currentNode);
    }

    /**
     * Return the line number where the current document event ends.
     * <p/>
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     * <p/>
     * <p>The return value is an approximation of the line number
     * in the document entity or external parsed entity where the
     * markup that triggered the event appears.</p>
     *
     * @return The line number, or -1 if none is available.
     * @see #getColumnNumber
     */
    public int getLineNumber() {
        return tree.getLineNumber(currentNode);
    }

    /**
     * Return the character position where the current document event ends.
     * <p/>
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     * <p/>
     * <p>The return value is an approximation of the column number
     * in the document entity or external parsed entity where the
     * markup that triggered the event appears.</p>
     *
     * @return The column number, or -1 if none is available.
     * @see #getLineNumber
     */
    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get a list of unparsed entities.
     *
     * @return a list of unparsed entities, or null if the information is not available, or
     *         an empty list if there are no unparsed entities.
     */

    public List getUnparsedEntities() {
        return null;
    }

  public static void main(String[] args) throws Exception {

    Controller controller = new Controller(new Configuration());
    TinyBuilder tb = (TinyBuilder)controller.makeBuilder();
    tb.open();
    NamePool p = tb.getConfiguration().getNamePool();
    int code = p.allocate("a", "b", "c");
    tb.startElement(code, -1, -1, -1);
    tb.endElement();
    tb.startDocument(-1);
    tb.startElement(code, -1, -1, -1);
    tb.endElement();
    tb.endDocument();
    tb.close();
    TinyTree tt = tb.getTree();
    NodeInfo node = tb.getCurrentRoot();

    TinyTreeWalker walker = new TinyTreeWalker((TinyNodeImpl)node);
    System.out.println(walker.next());
    System.out.println(walker.next());
    System.out.println(walker.next());
    System.out.println(walker.next());
    System.out.println(walker.next());
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