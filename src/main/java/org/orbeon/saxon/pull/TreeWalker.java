package org.orbeon.saxon.pull;

import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.tinytree.TinyNodeImpl;
import org.orbeon.saxon.tinytree.TinyTreeWalker;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;

import javax.xml.transform.SourceLocator;
import java.util.Stack;
import java.util.List;

/**
 * This implementation of the Saxon pull interface starts from any NodeInfo,
 * and returns the events corresponding to that node and its descendants (including
 * their attributes and namespaces). This works with any tree model: alternative
 * implementations may be available that take advantage of particular implementations
 * of the tree model.
 */

public class TreeWalker implements PullProvider, SourceLocator {

    private NodeInfo startNode;
    private NodeInfo currentNode;
    private int currentEvent;
    private Stack iteratorStack = new Stack();
    private PipelineConfiguration pipe;

    /**
     * Factory method to get a tree walker starting an a given node
     * @param startNode the start node
     * @return a PullProvider that delivers events associated with the subtree starting at the given node
     */

    public static PullProvider makeTreeWalker(NodeInfo startNode) {
        if (startNode instanceof UnconstructedParent) {
            return ((UnconstructedParent)startNode).getPuller();
        }
        if (startNode instanceof TinyNodeImpl) {
            switch (startNode.getNodeKind()) {
                case Type.DOCUMENT:
                case Type.ELEMENT:
                    return new TinyTreeWalker((TinyNodeImpl)startNode);
                default:
                    return new PullFromIterator(SingletonIterator.makeIterator(startNode));
            }

        } else {
            return new TreeWalker(startNode);
        }
    }

    /**
     * Private constructor: the class should be instantiated using the static factory method
     * @param startNode the root node of the subtree to be walked
     */

    private TreeWalker(NodeInfo startNode) {
        this.startNode = startNode;
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
                switch (currentNode.getNodeKind()) {
                    case Type.DOCUMENT:
                        currentEvent = START_DOCUMENT;
                        break;
                    case Type.ELEMENT:
                        currentEvent = START_ELEMENT;
                        break;
                    case Type.TEXT:
                        currentEvent = TEXT;
                        break;
                    case Type.COMMENT:
                        currentEvent = COMMENT;
                        break;
                    case Type.PROCESSING_INSTRUCTION:
                        currentEvent = PROCESSING_INSTRUCTION;
                        break;
                    case Type.ATTRIBUTE:
                        currentEvent = ATTRIBUTE;
                        break;
                    case Type.NAMESPACE:
                        currentEvent = NAMESPACE;
                        break;
                }
                return currentEvent;

            case START_DOCUMENT:
            case START_ELEMENT:
                AxisIterator kids = currentNode.iterateAxis(Axis.CHILD);
                iteratorStack.push(kids);

                currentNode = (NodeInfo)kids.next();
                if (currentNode != null) {
                    switch (currentNode.getNodeKind()) {
                        case Type.ELEMENT:
                            currentEvent = START_ELEMENT;
                            break;
                        case Type.TEXT:
                            currentEvent = TEXT;
                            break;
                        case Type.COMMENT:
                            currentEvent = COMMENT;
                            break;
                        case Type.PROCESSING_INSTRUCTION:
                            currentEvent = PROCESSING_INSTRUCTION;
                            break;
                    }
                    return currentEvent;
                } else {
                    iteratorStack.pop();
                    if (iteratorStack.isEmpty()) {
                        currentNode = startNode;
                    }
                    if (currentEvent == START_DOCUMENT) {
                        currentEvent = END_DOCUMENT;
                    } else {
                        currentEvent = END_ELEMENT;
                    }
                    return currentEvent;
                }
            case TEXT:
            case COMMENT:
            case PROCESSING_INSTRUCTION:
            case END_ELEMENT:
                if (iteratorStack.isEmpty()) {
                    if (currentNode == startNode) {
                        currentNode = null;
                        currentEvent = END_OF_INPUT;
                    } else {
                        currentNode = startNode;
                        if (currentNode.getNodeKind() == Type.ELEMENT) {
                            currentEvent = END_ELEMENT;
                        } else {
                            currentEvent = END_DOCUMENT;
                        }
                    }
                    return currentEvent;
                }

                AxisIterator siblings = (AxisIterator)iteratorStack.peek();
                currentNode = (NodeInfo)siblings.next();
                if (currentNode == null) {
                    iteratorStack.pop();
                    if (iteratorStack.isEmpty()) {
                        currentNode = startNode;
                        if (currentNode.getNodeKind() == Type.ELEMENT) {
                            currentEvent = END_ELEMENT;
                        } else {
                            currentEvent = END_DOCUMENT;
                        }
                        return currentEvent;
                    }
                    AxisIterator uncles = (AxisIterator)iteratorStack.peek();
                    currentNode = (NodeInfo)uncles.current();
                    if (currentNode.getNodeKind() == Type.DOCUMENT) {
                        currentEvent = END_DOCUMENT;
                    } else {
                        currentEvent = END_ELEMENT;
                    }
                    return currentEvent;
                } else {
                    switch (currentNode.getNodeKind()) {
                        case Type.ELEMENT:
                            currentEvent = START_ELEMENT;
                            break;
                        case Type.TEXT:
                            currentEvent = TEXT;
                            break;
                        case Type.COMMENT:
                            currentEvent = COMMENT;
                            break;
                        case Type.PROCESSING_INSTRUCTION:
                            currentEvent = PROCESSING_INSTRUCTION;
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
        if (currentNode.getNodeKind() == Type.ELEMENT) {
            AttributeCollectionImpl atts = new AttributeCollectionImpl(startNode.getConfiguration());
            SequenceIterator iter = currentNode.iterateAxis(Axis.ATTRIBUTE);
            while (true) {
                NodeInfo node = (NodeInfo)iter.next();
                if (node == null) {
                    break;
                }
                atts.addAttribute(node.getNameCode(), node.getTypeAnnotation(), node.getStringValue(), 0, 0);
            }
            return atts;
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
        if (currentNode.getNodeKind() == Type.ELEMENT) {
            if (iteratorStack.isEmpty()) {
                // get all inscope namespaces for a top-level element in the sequence.
                int[] codes = NamespaceIterator.getInScopeNamespaceCodes(currentNode);
                return new NamespaceDeclarationsImpl(getNamePool(), codes);
            } else {
                // only namespace declarations (and undeclarations) on this element are required
                return new NamespaceDeclarationsImpl(getNamePool(), currentNode.getDeclaredNamespaces(nsBuffer));
            }
        }
        throw new IllegalStateException("getNamespaceDeclarations() called when current event is not ELEMENT_START");
    }

    private int[] nsBuffer = new int[10];

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
        return currentNode.getNameCode();
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
        return currentNode.getFingerprint();
    }

    /**
     * Get the string value of the current attribute, text node, processing-instruction,
     * or atomic value.
     * This method cannot be used to obtain the string value of an element, or of a namespace
     * node. If the most recent event was anything other than {@link #START_ELEMENT}, {@link #TEXT},
     * {@link #PROCESSING_INSTRUCTION}, or {@link #ATOMIC_VALUE}, the result is undefined.
     */

    public CharSequence getStringValue() throws XPathException {
        if (currentNode.getNodeKind() == Type.ELEMENT) {
            skipToMatchingEnd();
        }
        return currentNode.getStringValueCS();
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
        return currentNode.getTypeAnnotation();
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
        return currentNode.getSystemId();
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
        return currentNode.getLineNumber();
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