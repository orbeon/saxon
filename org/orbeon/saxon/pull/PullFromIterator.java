package org.orbeon.saxon.pull;

import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;

import javax.xml.transform.SourceLocator;

/**
 * This class delivers any XPath sequence through the pull interface. Atomic values
 * in the sequence are supplied unchanged, as are top-level text, comment, attribute,
 * namespace, and processing-instruction nodes. Elements and documents appearing in
 * the input sequence are supplied as a sequence of events that walks recursively
 * down the subtree rooted at that node. The input is supplied in the form of a
 * SequenceIterator.
 */

public class PullFromIterator implements PullProvider {

    private SequenceIterator base;
    private PullProvider treeWalker = null;
    private PipelineConfiguration pipe;
    private int currentEvent = START_OF_INPUT;

    public PullFromIterator(SequenceIterator base) {
        this.base = base;
    }

    /**
     * Set configuration information. This must only be called before any events
     * have been read. The returned value is a new PullProvider, which must be used
     * in place of the original provider to read all subsequent events: the effect
     * of calling the original provider is not defined. This mechanism allows
     * the provider to implement this method by inserting a filter between itself and
     * the client.
     */

    public PullProvider setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
        return this;
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
     *         {@link #END_OF_INPUT} is returned at the end of the sequence.
     */

    public int next() throws XPathException {
        if (treeWalker == null) {
            Item item = base.next();
            if (item == null) {
                currentEvent = END_OF_INPUT;
                return currentEvent;
            } else if (item instanceof AtomicValue) {
                currentEvent = ATOMIC_VALUE;
                return currentEvent;
            } else {
                switch (((NodeInfo)item).getNodeKind()) {
                    case Type.TEXT:
                        currentEvent = TEXT;
                        return currentEvent;

                    case Type.COMMENT:
                        currentEvent = COMMENT;
                        return currentEvent;

                    case Type.PROCESSING_INSTRUCTION:
                        currentEvent = PROCESSING_INSTRUCTION;
                        return currentEvent;

                    case Type.ATTRIBUTE:
                        currentEvent = ATTRIBUTE;
                        return currentEvent;

                    case Type.NAMESPACE:
                        currentEvent = NAMESPACE;
                        return currentEvent;

                    case Type.ELEMENT:
                    case Type.DOCUMENT:
                        treeWalker = TreeWalker.makeTreeWalker((NodeInfo)item);
                        currentEvent = treeWalker.next();
                        return currentEvent;

                    default:
                        throw new IllegalStateException();

                }
            }

        } else {
            // there is an active TreeWalker: just return its next event
            int event = treeWalker.next();
            if (event == END_OF_INPUT) {
                treeWalker = null;
                currentEvent = next();
            } else {
                currentEvent = event;
            }
            return currentEvent;

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
        if (treeWalker != null) {
            return treeWalker.getAttributes();
        } else {
            throw new IllegalStateException();
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
        if (treeWalker != null) {
            return treeWalker.getNamespaceDeclarations();
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Skip the current subtree. This method may be called only immediately after
     * a START_DOCUMENT or START_ELEMENT event. This call returns the matching
     * END_DOCUMENT or END_ELEMENT event; the next call on next() will return
     * the event following the END_DOCUMENT or END_ELEMENT.
     */

    public int skipToMatchingEnd() throws XPathException {
        if (treeWalker != null) {
            return treeWalker.skipToMatchingEnd();
        } else {
            throw new IllegalStateException();
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
        if (treeWalker != null) {
            treeWalker.close();
        }
    }

    /**
     * Get the nameCode identifying the name of the current node. This method
     * can be used after the {@link #START_ELEMENT}, {@link #PROCESSING_INSTRUCTION},
     * {@link #ATTRIBUTE}, or {@link #NAMESPACE} events.
     * If called at other times, the result is undefined and may result in an IllegalStateException.
     * If called when the current node is an unnamed namespace node (a node representing the default namespace)
     * the returned value is -1.
     *
     * @return the nameCode. The nameCode can be used to obtain the prefix, local name,
     *         and namespace URI from the name pool.
     */

    public int getNameCode() {
        if (treeWalker != null) {
            return treeWalker.getNameCode();
        } else {
            Item item = base.current();
            if (item instanceof NodeInfo) {
                return ((NodeInfo)item).getNameCode();
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Get the fingerprint of the name of the element. This is similar to the nameCode, except that
     * it does not contain any information about the prefix: so two elements with the same fingerprint
     * have the same name, excluding prefix. This method
     * can be used after the {@link #START_ELEMENT}, {@link #PROCESSING_INSTRUCTION},
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
     * Get the string value of the current element, text node, processing-instruction,
     * or top-level attribute or namespace node, or atomic value.
     * <p/>
     * <p>In other situations the result is undefined and may result in an IllegalStateException.</p>
     * <p/>
     * <p>If the most recent event was a {@link #START_ELEMENT}, this method causes the content
     * of the element to be read. The current event on completion of this method will be the
     * corresponding {@link #END_ELEMENT}. The next call of next() will return the event following
     * the END_ELEMENT event.</p>
     *
     * @return the String Value of the node in question, defined according to the rules in the
     *         XPath data model.
     */

    public CharSequence getStringValue() throws XPathException {
        if (treeWalker != null) {
            return treeWalker.getStringValue();
        } else {
            Item item = base.current();
            return item.getStringValueCS();
        }
    }

    /**
     * Get the type annotation of the current attribute or element node, or atomic value.
     * The result of this method is undefined unless the most recent event was START_ELEMENT,
     * ATTRIBUTE, or ATOMIC_VALUE.
     *
     * @return the type annotation. This code is the fingerprint of a type name, which may be
     *         resolved to a {@link org.orbeon.saxon.type.SchemaType} by access to the Configuration.
     */

    public int getTypeAnnotation() {
        if (treeWalker != null) {
            return treeWalker.getTypeAnnotation();
        } else {
            Item item = base.current();
            if (item instanceof NodeInfo) {
                return ((NodeInfo)item).getTypeAnnotation();
            } else {
                return ((AtomicType)((AtomicValue)item).getItemType()).getFingerprint();
            }
        }
    }

    /**
     * Get the location of the current event.
     * For an event stream representing a real document, the location information
     * should identify the location in the lexical XML source. For a constructed document, it should
     * identify the location in the query or stylesheet that caused the node to be created.
     * A value of null can be returned if no location information is available.
     */

    public SourceLocator getSourceLocator() {
        if (treeWalker != null) {
            return treeWalker.getSourceLocator();
        } else {
            return null;
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