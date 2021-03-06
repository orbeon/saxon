package org.orbeon.saxon.pull;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.sort.IntArraySet;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.DocumentInstr;
import org.orbeon.saxon.instruct.ElementCreator;
import org.orbeon.saxon.instruct.ParentNodeConstructor;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;

import javax.xml.transform.SourceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * A virtual tree walker provides a sequence of pull events describing the structure and content of a tree
 * that is conceptually being constructed by expressions in a query or stylesheet; in fact the tree is
 * not necessarily constructed in memory, and exists only as this stream of pull events.
 * <p>
 * The tree is physically constructed if operations are requested that depend on the identity of the nodes
 * in the tree, or that navigate within the tree. Operations such as copying or atomizing the tree can be done
 * directly, without building it in memory. (Note however that if such operations are done more than once, the
 * underlying instructions may be evaluated repeatedly.)
 */

public class VirtualTreeWalker implements PullProvider, NamespaceDeclarations {

    private PipelineConfiguration pipe;
    private int currentEvent = START_OF_INPUT;
    private int nameCode;
    private int nextNameCode;
    private ParentNodeConstructor instruction;
    private XPathContext context;
    private Stack constructorStack = new Stack();
    private Stack iteratorStack = new Stack();
    private PullProvider subordinateTreeWalker = null;
                // The subordinateTreeWalker is used if the tree construction expression pulls in references
                // to document or element nodes in an existing source document. When this happens, tree walking
                // events generated by walking the source document are copied into to the stream of events
                // generated by this class.
    private boolean alreadyRead = false;
    private boolean allowAttributes = false;

    private int stripDepth = -1;
                // If this is >0, it indicates that instructions on the constructor stack with depth < stripDepth
                // specify validation=preserve, while those at greater depth effectively specify validation=strip
                // (It doesn't matter if they actually say validation=preserve, the stripping takes priority)

    private AttributeCollectionImpl attributes;
    private boolean foundAttributes;
    private int[] activeNamespaces;
    private ArrayList additionalNamespaces = new ArrayList(10);  // array of namespace nodes

    /**
     * Create a VirtualTreeWalker to navigate the tree constructed by evaluating a given instruction
     * in a given dyamic context
     * @param instruction the instruction (this will always be an instruction that creates element or
     * document nodes)
     * @param context the dynamic evaluation context
     */

    public VirtualTreeWalker(ParentNodeConstructor instruction, XPathContext context) {
        this.instruction = instruction;
        this.context = context;
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
     * @return the pipeline configuration
     */

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    /**
     * Get the namepool
     * @return the NamePool
     */

    public NamePool getNamePool() {
        return pipe.getConfiguration().getNamePool();
    }

    /**
     * Get the next event
     *
     * @return an integer code indicating the type of event. The code
     *         {@link #END_OF_INPUT} is returned at the end of the sequence.
     */

    public int next() throws XPathException {

        try {

            // First see if we are currently walking some other tree that has been logically
            // copied into this tree.

            if (subordinateTreeWalker != null) {
                currentEvent = subordinateTreeWalker.next();
                if (currentEvent == END_OF_INPUT) {
                    subordinateTreeWalker = null;
                    return next();
                }
                return currentEvent;
            }

            // On the first call, produce a START_ELEMENT or START_DOCUMENT event depending on the instruction

            if (currentEvent == START_OF_INPUT) {
                constructorStack.push(instruction);
                if (stripDepth < 0 && instruction.getValidationMode() == Validation.STRIP) {
                     stripDepth = constructorStack.size();
                }
                SequenceIterator content = instruction.getContentExpression().iterate(context);
                iteratorStack.push(content);
                if (instruction instanceof DocumentInstr) {
                    currentEvent = START_DOCUMENT;
                    nameCode = -1;
                } else {
                    currentEvent = START_ELEMENT;
                    nameCode = ((ElementCreator)instruction).getNameCode(context);
                    allowAttributes = true;
                    // look ahead to generate all the attributes and namespaces of the element
                    processAttributesAndNamespaces((ElementCreator)instruction, content);
                    // remember that we've read one event too many
                    allowAttributes = false;
                    alreadyRead = true;
                }
                return currentEvent;
            }

            if (iteratorStack.isEmpty()) {
                // if we're at the top level, see if we've just started, or are about to finish
                if (currentEvent == START_DOCUMENT || currentEvent == START_ELEMENT) {
                    // we've just started: start processing the content of the instruction
                    SequenceIterator iter = instruction.getContentExpression().iterate(context);
                    constructorStack.push(instruction);
                    if (stripDepth < 0 && instruction.getValidationMode() == Validation.STRIP) {
                        stripDepth = constructorStack.size();
                    }
                    iteratorStack.push(iter);
                } else if (currentEvent == END_DOCUMENT || currentEvent == END_ELEMENT) {
                    // we're about to finish
                    currentEvent = END_OF_INPUT;
                    return currentEvent;
                } else {
                    // we're going to finish soon, but must first generate the last END_DOCUMENT or END_ELEMENT event
                    currentEvent =
                            ((instruction instanceof DocumentInstr) ? END_DOCUMENT : END_ELEMENT);
                    return currentEvent;
                }
            }

            // Read the next item from the current content iterator

            SequenceIterator iterator = ((SequenceIterator)iteratorStack.peek());
            if (alreadyRead) {
                Item item = iterator.current();
                alreadyRead = false;
                nameCode = nextNameCode;
                return processItem(iterator, item);
            } else {
                return processItem(iterator, iterator.next());
            }
        } catch (XPathException e) {
            // report any dynamic errors unless already reported
            context.getController().reportFatalError(e);
            throw e;
        }
    }

    private FastStringBuffer textNodeBuffer = new FastStringBuffer(100);
    private CharSequence currentTextValue = null;

    /**
     * Process an item in the content of an element or document node
     * @param iterator the iterator over the contents of the element or document
     * @param item the current item to be processed, or null if the end of the content
     * iterator has been reached
     * @return the next event code
     * @throws XPathException if a dynamic error occurs
     */

    private int processItem(SequenceIterator iterator, Item item) throws XPathException {
        if (item == null) {
            // we've reached the end of the children
            if (stripDepth == constructorStack.size()) {
                stripDepth = -1;
            }
            ParentNodeConstructor inst = (ParentNodeConstructor)constructorStack.pop();
            if (inst instanceof DocumentInstr) {
                iteratorStack.pop();
                if (iteratorStack.isEmpty()) {
                    currentEvent = END_DOCUMENT;
                    nameCode = -1;
                    return currentEvent;
                }
                // skip the END_DOCUMENT event for a nested document node
                return next();
            } else {
                currentEvent = END_ELEMENT;
                nameCode = -1;
                iteratorStack.pop();
                return currentEvent;
            }


        } else if (item instanceof UnconstructedParent) {
            // this represents a nested element or document node constructor
            UnconstructedParent parent = (UnconstructedParent)item;
            ParentNodeConstructor inst = parent.getInstruction();

            constructorStack.push(inst);
            if (stripDepth < 0 && inst.getValidationMode() == Validation.STRIP) {
                 stripDepth = constructorStack.size();
            }
            SequenceIterator content = inst.getContentExpression().iterate(parent.getXPathContext());
            if (inst instanceof DocumentInstr) {
                iteratorStack.push(content);
                // skip the START_DOCUMENT event
                return next();
            } else {
                currentEvent = START_ELEMENT;
                nameCode = ((UnconstructedElement)item).getNameCode();
                processAttributesAndNamespaces((ElementCreator)inst, content);
                alreadyRead = true;
                iteratorStack.push(content);
                return currentEvent;
            }

        } else if (item instanceof AtomicValue) {
            currentTextValue = textNodeBuffer;
            textNodeBuffer.setLength(0);
            textNodeBuffer.append(item.getStringValueCS());
            while (true) {
                Item next = iterator.next();
                if (next instanceof AtomicValue) {
                    textNodeBuffer.append(' ');
                    textNodeBuffer.append(next.getStringValueCS());
                    //continue;
                } else {
                    currentEvent = TEXT;
                    nameCode = -1;
                    alreadyRead = true;
                    return currentEvent;
                }
            }

        } else {
            nameCode = ((NodeInfo)item).getNameCode();
            switch (((NodeInfo)item).getNodeKind()) {
                case Type.TEXT:
                    currentEvent = TEXT;
                    currentTextValue = item.getStringValueCS();
                    return currentEvent;

                case Type.COMMENT:
                    currentEvent = COMMENT;
                    return currentEvent;

                case Type.PROCESSING_INSTRUCTION:
                    currentEvent = PROCESSING_INSTRUCTION;
                    return currentEvent;

                case Type.ATTRIBUTE:
                    if (!allowAttributes) {
                        XPathException de;
                        if (constructorStack.peek() instanceof DocumentInstr) {
                            de = new XPathException(
                                    "Attributes cannot be attached to a document node");
                            if (pipe.getHostLanguage() == Configuration.XQUERY) {
                                de.setErrorCode("XQTY0004");
                            } else {
                                de.setErrorCode("XTDE0420");
                            }
                        } else {
                            de = new XPathException(
                                    "Attributes in the content of an element must come before the child nodes");
                            if (pipe.getHostLanguage() == Configuration.XQUERY) {
                                de.setErrorCode("XQDY0024");
                            } else {
                                de.setErrorCode("XTDE0410");
                            }
                        }
                        de.setXPathContext(context);
                        de.setLocator(getSourceLocator());
                        throw de;
                    }
                    currentEvent = ATTRIBUTE;
                    return currentEvent;

                case Type.NAMESPACE:
                    if (!allowAttributes) {
                        XPathException de = new XPathException("Namespace nodes in the content of an element must come before the child nodes");
                        de.setErrorCode("XTDE0410");
                        de.setXPathContext(context);
                        de.setLocator(getSourceLocator());
                        throw de;
                    }
                    currentEvent = NAMESPACE;
                    return currentEvent;

                case Type.ELEMENT:
                    subordinateTreeWalker = TreeWalker.makeTreeWalker((NodeInfo)item);
                    subordinateTreeWalker.setPipelineConfiguration(pipe);
                    currentEvent = subordinateTreeWalker.next();
                    nameCode = subordinateTreeWalker.getNameCode();
                    return currentEvent;

                case Type.DOCUMENT:
                    subordinateTreeWalker = TreeWalker.makeTreeWalker((NodeInfo)item);
                    subordinateTreeWalker.setPipelineConfiguration(pipe);
                    subordinateTreeWalker = new DocumentEventIgnorer(subordinateTreeWalker);
                    subordinateTreeWalker.setPipelineConfiguration(pipe);
                    currentEvent = subordinateTreeWalker.next();
                    nameCode = -1;
                    return currentEvent;

                default:
                    throw new IllegalStateException();

            }
        }
    }

    /**
     * Following a START_ELEMENT event, evaluate the contents of the element to obtain all attributes and namespaces.
     * This process stops when the first event other than an attribute or namespace is read. We then remember the
     * extra event, which will be the next event returned in the normal sequence. Note that the relative order
     * of attributes and namespaces is undefined.
     * @param inst The instruction that creates the element node
     * @param content Iterator over the expression that generates the attributes, namespaces, and content of the
     * element
     * @throws XPathException if any dynamic error occurs
     */
    private void processAttributesAndNamespaces(ElementCreator inst, SequenceIterator content) throws XPathException {
        foundAttributes = false;
        additionalNamespaces.clear();
        activeNamespaces = inst.getActiveNamespaces();
        if (activeNamespaces == null) {
            activeNamespaces = IntArraySet.EMPTY_INT_ARRAY;
        }

        // if the namespace of the element name itself is not one of the active namespaces, make sure
        // a namespace node is created for it

//        NamePool pool = context.getNamePool();
//        int nscode = pool.allocateNamespaceCode(nameCode);
//        boolean found = false;
//        for (int i=0; i<activeNamespaces.length; i++) {
//            if (activeNamespaces[i] == -1) {
//                break;
//            }
//            if (activeNamespaces[i] == nscode) {
//                found = true;
//                break;
//            }
//        }
//        if (!found) {
//            Orphan namespace = new Orphan(context.getConfiguration());
//            namespace.setNodeKind(Type.NAMESPACE);
//            namespace.setNameCode(pool.allocate("", "", pool.getPrefix(nameCode)));
//            namespace.setStringValue(pool.getURI(nameCode));
//            additionalNamespaces.add(namespace);
//        }

        boolean preserve = (stripDepth<0);
        while (true) {
            Item next = content.next();
            if (next == null) {
                break;
            } else if (next instanceof NodeInfo) {
                NodeInfo node = (NodeInfo)next;
                int kind = node.getNodeKind();
                if (kind == Type.ATTRIBUTE) {
                    if (!foundAttributes) {
                        if (attributes == null) {
                            attributes = new AttributeCollectionImpl(context.getConfiguration());
                        }
                        attributes.clear();
                        foundAttributes = true;
                    }
                    int index = attributes.getIndexByFingerprint(node.getFingerprint());
                    if (index >= 0) {
                        // Attribute already exists. In XQuery this is an error. In XSLT, the last attribute wins
                        if (context.getController().getExecutable().getHostLanguage() == Configuration.XSLT) {
                            attributes.setAttribute(index,
                                    node.getNameCode(),
                                    preserve ? node.getTypeAnnotation() : StandardNames.XS_UNTYPED_ATOMIC,
                                    node.getStringValue(), 0, 0);
                        } else {
                            XPathException de = new XPathException("The attributes of an element must have distinct names");
                            de.setErrorCode("XQDY0025");
                            de.setXPathContext(context);
                            de.setLocator(getSourceLocator());
                            throw de;
                        }
                    } else {
                        attributes.addAttribute(
                            node.getNameCode(),
                            preserve ? node.getTypeAnnotation() : StandardNames.XS_UNTYPED_ATOMIC,
                            node.getStringValue(),
                            0, 0);
                    }
                    // if the namespace of the attribute name itself has not already been declared, make sure
                    // a namespace node is created for it

//                    int anc = node.getNameCode();
//                    if ((anc >> 20) != 0) {
//                        // the attribute name is prefixed
//
//                        int anscode = pool.allocateNamespaceCode(anc);
//                        boolean afound = false;
//                        for (int i=0; i<activeNamespaces.length; i++) {
//                            if (activeNamespaces[i] == -1) {
//                                break;
//                            }
//                            if (activeNamespaces[i] == anscode) {
//                                afound = true;
//                                break;
//                            }
//                            // don't bother searching the additionalNamespaces, we'll just declare it twice
//                        }
//
//                        if (!afound) {
//                            Orphan namespace = new Orphan(context.getConfiguration());
//                            namespace.setNodeKind(Type.NAMESPACE);
//                            namespace.setNameCode(pool.allocate("", "", pool.getPrefix(anc)));
//                            namespace.setStringValue(pool.getURI(anc));
//                            additionalNamespaces.add(namespace);
//                        }
//                    }

                } else if (kind == Type.NAMESPACE) {
                    additionalNamespaces.add(node);
                } else if (kind == Type.TEXT && node.getStringValue().length() == 0) {
                    //continue;   // ignore zero-length text nodes
                } else {
                    nextNameCode = ((NodeInfo)next).getNameCode();
                    break;
                }
            } else {
                break;
            }
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
        if (subordinateTreeWalker != null) {
            return subordinateTreeWalker.getAttributes();
        } else {
            if (foundAttributes) {
                return attributes;
            } else {
                return AttributeCollectionImpl.EMPTY_ATTRIBUTE_COLLECTION;
            }
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
        if (subordinateTreeWalker != null) {
            return subordinateTreeWalker.getNamespaceDeclarations();
        } else {
            return this;
        }
    }

    /**
     * Skip the current subtree. This method may be called only immediately after
     * a START_DOCUMENT or START_ELEMENT event. This call returns the matching
     * END_DOCUMENT or END_ELEMENT event; the next call on next() will return
     * the event following the END_DOCUMENT or END_ELEMENT.
     * @throws IllegalStateException if the method is called at any time other than
     * immediately after a START_DOCUMENT or START_ELEMENT event.
     */

    public int skipToMatchingEnd() throws XPathException {
        if (currentEvent != START_DOCUMENT && currentEvent != START_ELEMENT) {
            throw new IllegalStateException();
        }
        if (subordinateTreeWalker != null) {
            return subordinateTreeWalker.skipToMatchingEnd();
        } else {
            SequenceIterator content = (SequenceIterator)iteratorStack.peek();
            if (alreadyRead) {
                alreadyRead = false;
            }
            while (true) {
                Item next = content.next();
                if (next == null) {
                    break;
                }
            }
            return (currentEvent == START_DOCUMENT ? END_DOCUMENT : END_ELEMENT);
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
        if (subordinateTreeWalker != null) {
            subordinateTreeWalker.close();
        } else {
            // do nothing
        }
    }

    /**
     * Set the initial nameCode
     * @param nameCode the nameCode of the node at the root of the tree being walked
     */

    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    /**
     * Get the nameCode identifying the name of the current node. This method
     * can be used after the {@link #START_ELEMENT}, {@link #END_ELEMENT}, {@link #PROCESSING_INSTRUCTION},
     * {@link #ATTRIBUTE}, or {@link #NAMESPACE} events. With some PullProvider implementations,
     * <b>but not this one</b>, it can also be used after {@link #END_ELEMENT}.
     * If called at other times, the result is undefined and may result in an IllegalStateException.
     * If called when the current node is an unnamed namespace node (a node representing the default namespace)
     * the returned value is -1.
     *
     * @return the nameCode. The nameCode can be used to obtain the prefix, local name,
     *         and namespace URI from the name pool.
     */

    public int getNameCode() {
        if (subordinateTreeWalker != null) {
            return subordinateTreeWalker.getNameCode();
        }
        return nameCode;
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
        if (subordinateTreeWalker != null) {
            return subordinateTreeWalker.getStringValue();
        } else if (currentEvent == TEXT) {
            return currentTextValue;
        } else if (currentEvent != START_ELEMENT && currentEvent != START_DOCUMENT) {
            SequenceIterator content = (SequenceIterator)iteratorStack.peek();
            if (content.current() == null) {
                return "";
            }
            return content.current().getStringValue();
        } else {
            FastStringBuffer sb = new FastStringBuffer(100);
            SequenceIterator content = (SequenceIterator)iteratorStack.peek();
            boolean previousAtomic = false;
            if (alreadyRead) {
                Item current = content.current();
                if (current == null) {
                    return "";
                }
                processText(current, sb);
                previousAtomic = (current instanceof AtomicValue);
                alreadyRead = false;
            }
            while (true) {
                Item next = content.next();
                if (next == null) {
                    break;
                }
                boolean atomic = (next instanceof AtomicValue);
                if (atomic && previousAtomic) {
                    sb.append(' ');
                }
                processText(next, sb);
                previousAtomic = atomic;
            }
            return sb;
        }
    }

    /**
     * Add the string value of a child item to a string buffer that is used to accumulate the
     * string value of a document or element node
     * @param item the child item
     * @param sb the string buffer where the content is accumulated
     */

    private void processText(Item item, FastStringBuffer sb) {
        if (item instanceof UnconstructedParent) {
            sb.append(item.getStringValueCS());
        } else if (item instanceof AtomicValue) {
            sb.append(item.getStringValueCS());
        } else {
            NodeInfo node = (NodeInfo)item;
            switch (node.getNodeKind()) {
                case Type.DOCUMENT:
                case Type.ELEMENT:
                case Type.TEXT:
                    sb.append(node.getStringValueCS());
                default:
                    // do nothing
            }
        }
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
     * ATTRIBUTE, or ATOMIC_VALUE.
     *
     * @return the type annotation. This code is the fingerprint of a type name, which may be
     *         resolved to a {@link org.orbeon.saxon.type.SchemaType} by access to the Configuration.
     */

    public int getTypeAnnotation() {
        if (subordinateTreeWalker != null && stripDepth < 0) {
            return subordinateTreeWalker.getTypeAnnotation();
        } else {
            return -1;
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
        return instruction;
    }

    /**
     * Get the number of declarations (and undeclarations) in this list.
     */

    public int getNumberOfNamespaces() {
        return activeNamespaces.length + additionalNamespaces.size();
    }

    /**
     * Get the prefix of the n'th declaration (or undeclaration) in the list,
     * counting from zero.
     *
     * @param index the index identifying which declaration is required.
     * @return the namespace prefix. For a declaration or undeclaration of the
     *         default namespace, this is the zero-length string.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */

    public String getPrefix(int index) {
        if (index < activeNamespaces.length) {
            return getNamePool().getPrefixFromNamespaceCode(activeNamespaces[index]);
        } else {
            return ((NodeInfo)additionalNamespaces.get(index - activeNamespaces.length)).getLocalPart();
        }
    }

    /**
     * Get the namespace URI of the n'th declaration (or undeclaration) in the list,
     * counting from zero.
     *
     * @param index the index identifying which declaration is required.
     * @return the namespace URI. For a namespace undeclaration, this is the
     *         zero-length string.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */

    public String getURI(int index) {
        if (index < activeNamespaces.length) {
            return getNamePool().getURIFromNamespaceCode(activeNamespaces[index]);
        } else {
            return ((NodeInfo)additionalNamespaces.get(index - activeNamespaces.length)).getStringValue();
        }
    }

    /**
     * Get the n'th declaration in the list in the form of a namespace code. Namespace
     * codes can be translated into a prefix and URI by means of methods in the
     * NamePool
     *
     * @param index the index identifying which declaration is required.
     * @return the namespace code. This is an integer whose upper half indicates
     *         the prefix (0 represents the default namespace), and whose lower half indicates
     *         the URI (0 represents an undeclaration).
     * @throws IndexOutOfBoundsException if the index is out of range.
     * @see org.orbeon.saxon.om.NamePool#getPrefixFromNamespaceCode(int)
     * @see org.orbeon.saxon.om.NamePool#getURIFromNamespaceCode(int)
     */

    public int getNamespaceCode(int index) {
        if (index < activeNamespaces.length) {
            return activeNamespaces[index];
        } else {
            return getNamePool().allocateNamespaceCode(getPrefix(index), getURI(index));
        }
    }

    /**
     * Get all the namespace codes, as an array.
     *
     * @param buffer a sacrificial array that the method is free to use to contain the result.
     *               May be null.
     * @return an integer array containing namespace codes. The array may be filled completely
     *         with namespace codes, or it may be incompletely filled, in which case a -1 integer acts
     *         as a terminator.
     */

    public int[] getNamespaceCodes(int[] buffer) {
        if (buffer.length < getNumberOfNamespaces()) {
            buffer = new int[getNumberOfNamespaces()];
        } else {
            buffer[getNumberOfNamespaces()] = -1;
        }
        for (int i=0; i<getNumberOfNamespaces(); i++) {
            buffer[i] = getNamespaceCode(i);
        }
        return buffer;
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

