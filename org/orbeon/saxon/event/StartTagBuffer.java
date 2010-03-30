package org.orbeon.saxon.event;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.tinytree.TinyBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * StartTagBuffer is a ProxyReceiver that buffers attributes and namespace events within a start tag.
 * It maintains details of the namespace context, and a full set of attribute information, on behalf
 * of other filters that need access to namespace information or need to process attributes in arbitrary
 * order.
 *
 * <p>StartTagBuffer also implements namespace fixup (the process of creating namespace nodes|bindings on behalf
 * of constructed element and attribute nodes). Although this would be done anyway, further down the pipeline,
 * it has to be done early in the case of a validating pipeline, because the namespace bindings must be created
 * before any namespace-sensitive attribute content is validated.</p>
 *
 * <p>The StartTagBuffer also allows error conditions to be buffered. This is because the XSIAttributeHandler
 * validates attributes such as xsi:type and xsi:nil before attempting to match its parent element against
 * a particle of its containing type. It is possible that the parent element will match a wildcard particle
 * with processContents="skip", in which case an invalid xsi:type or xsi:nil attribute is not an error.</p>
 */

public class StartTagBuffer extends ProxyReceiver implements NamespaceResolver {

    // Details of the pending element event

    int elementNameCode;
    int elementTypeCode;
    int elementLocationId;
    int elementProperties;

    // Details of pending attribute events

    AttributeCollectionImpl bufferedAttributes;
    private boolean acceptAttributes;
    private boolean inDocument;

    // We keep track of namespaces. The namespaces
    // array holds a list of all namespaces currently declared (organised as pairs of entries,
    // prefix followed by URI). The stack contains an entry for each element currently open; the
    // value on the stack is an Integer giving the number of namespaces added to the main
    // namespace stack by that element.

    private int[] namespaces = new int[50];          // all namespace codes currently declared
    private int namespacesSize = 0;                  // all namespaces currently declared
    private int[] countStack = new int[50];
    private int depth = 0;
    private int attCount = 0;

    // We can make an element node representing this start tag for use in evaluation xs:alternative
    // (conditional type assignment) expressions. Once this is done, we keep it.

    private NodeInfo elementNode;

    /**
     * Set the pipeline configuration
     * @param pipe the pipeline configuration
     */

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        super.setPipelineConfiguration(pipe);
        bufferedAttributes = new AttributeCollectionImpl(pipe.getConfiguration());
    }

    /**
     * Start of a document node.
     */

    public void startDocument(int properties) throws XPathException {
        // a document node in the content sequence of an element is ignored. However, we need
        // to stop attributes being created within the document node.
        if (depth == 0) {
            depth++;
            super.startDocument(properties);
        }
        acceptAttributes = false;
        inDocument = true;      // we ought to clear this on endDocument, but it only affects diagnostics
    }


    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        if (depth == 1) {
            depth--;
            super.endDocument();
        }
    }

    /**
    * startElement
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {

        elementNameCode = nameCode;
        elementTypeCode = typeCode;
        elementLocationId = locationId;
        elementProperties = properties;

        bufferedAttributes.clear();

        // Record the current height of the namespace list so it can be reset at endElement time

        countStack[depth] = 0;
        if (++depth >= countStack.length) {
            int[] newstack = new int[depth*2];
            System.arraycopy(countStack, 0, newstack, 0, depth);
            countStack = newstack;
        }

        // Ensure that the element namespace is output, unless this is done
        // automatically by the caller (which is true, for example, for a literal
        // result element).

        acceptAttributes = true;
        inDocument = false;
        if ((properties & ReceiverOptions.NAMESPACE_OK) == 0) {
            namespace(getNamePool().allocateNamespaceCode(nameCode), 0);
        }
        attCount = 0;
        elementNode = null;
    }

    public void namespace(int namespaceCode, int properties) throws XPathException {

        if (!acceptAttributes) {
            throw NoOpenStartTagException.makeNoOpenStartTagException(
                    Type.NAMESPACE, getNamePool().getPrefixFromNamespaceCode(namespaceCode),
                    getPipelineConfiguration().getHostLanguage(),
                    inDocument, false);
        }

        // avoid duplicates
        for (int n=0; n<countStack[depth - 1]; n++) {
            if (namespaces[namespacesSize - 1 - n] == namespaceCode) {
                return;
            }
        }
        addToStack(namespaceCode);
        countStack[depth - 1]++;
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @param typeCode   The type of the attribute, as held in the name pool
     * @param properties Bit significant value. The following bits are defined:
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
            throws XPathException {

        if (!acceptAttributes) {
            throw NoOpenStartTagException.makeNoOpenStartTagException(
                    Type.ATTRIBUTE, getNamePool().getDisplayName(nameCode),
                    getPipelineConfiguration().getHostLanguage(),
                    inDocument, false);
        }

        // Perform namespace fixup for the attribute

        if (((properties & ReceiverOptions.NAMESPACE_OK) == 0) &&
                NamePool.getPrefixIndex(nameCode) != 0) {	// non-null prefix
            nameCode = checkProposedPrefix(nameCode, attCount++);
        }
        bufferedAttributes.addAttribute(nameCode, typeCode, value.toString(), locationId, properties);

        // Note: we're relying on the fact that AttributeCollection can hold two attributes of the same name
        // and maintain their order, because the check for duplicate attributes is not done until later in the
        // pipeline. We validate both the attributes (see Bugzilla #4600 which legitimizes this.)

    }

    /**
     * Add a namespace declaration (or undeclaration) to the stack
     * @param nscode the namepool namespace code for the declaration
    */

    private void addToStack(int nscode) {
		// expand the stack if necessary
        if (namespacesSize+1 >= namespaces.length) {
            int[] newlist = new int[namespacesSize*2];
            System.arraycopy(namespaces, 0, newlist, 0, namespacesSize);
            namespaces = newlist;
        }
        namespaces[namespacesSize++] = nscode;
    }

    /**
     * startContent: Add any namespace undeclarations needed to stop
     * namespaces being inherited from parent elements
     */

    public void startContent() throws XPathException {
        nextReceiver.startElement(elementNameCode, elementTypeCode, elementLocationId,
                elementProperties | ReceiverOptions.NAMESPACE_OK);
        declareNamespacesForStartElement();

        final int length = bufferedAttributes.getLength();
        for (int i=0; i<length; i++) {
            nextReceiver.attribute(bufferedAttributes.getNameCode(i),
                    bufferedAttributes.getTypeAnnotation(i),
                    bufferedAttributes.getValue(i),
                    bufferedAttributes.getLocationId(i),
                    bufferedAttributes.getProperties(i) | ReceiverOptions.NAMESPACE_OK);
        }
        acceptAttributes = false;
        nextReceiver.startContent();
    }

    protected void declareNamespacesForStartElement() throws XPathException {
        for (int i=namespacesSize - countStack[depth-1]; i<namespacesSize; i++) {
            nextReceiver.namespace(namespaces[i], 0);
        }
    }

    protected void declareAllNamespaces() throws XPathException {
        for (int i=0; i<namespacesSize; i++) {
            nextReceiver.namespace(namespaces[i], 0);
        }
    }

    /**
    * endElement: Discard the namespaces declared on this element.
    */

    public void endElement () throws XPathException {
        nextReceiver.endElement();
        undeclareNamespacesForElement();
    }

    protected void undeclareNamespacesForElement() {
        namespacesSize -= countStack[--depth];
    }

    /**
     * Get the name of the current element
     * @return the namepool namecode of the element
     */

    public int getElementNameCode() {
        return elementNameCode;
    }

    /**
     * Determine if the current element has any attributes
     * @return true if the element has one or more attributes
     */

    public boolean hasAttributes() {
        return bufferedAttributes.getLength() > 0;
    }

    /**
     * Get the value of the current attribute with a given nameCode
     * @param nameCode the name of the required attribute
     * @return the attribute value, or null if the attribute is not present
     */

    public String getAttribute(int nameCode) {
        return bufferedAttributes.getValueByFingerprint(nameCode & 0xfffff);
    }

    /**
     * Get the URI code corresponding to a given prefix code, by searching the
     * in-scope namespaces. This is a service provided to subclasses.
     * @param prefixCode the 16-bit prefix code required
     * @return the 16-bit URI code, or -1 if the prefix is not bound to any namespace
     */

    protected short getURICode(short prefixCode) {
        for (int i=namespacesSize-1; i>=0; i--) {
        	if ((namespaces[i]>>16) == (prefixCode)) {
                final short uriCode = (short)(namespaces[i]&0xffff);
                if (uriCode == 0) {
                    // we've found a namespace undeclaration, so it's as if the prefix weren't there at all
                    break;
                }
                return uriCode;
            }
        }
        if (prefixCode == 0) {
            return 0;   // by default, no prefix means no namespace URI
        } else {
            return -1;
        }
    }

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope.
     *
     * @param prefix     the namespace prefix
     * @param useDefault true if the default namespace is to be used when the
     *                   prefix is ""
     * @return the uri for the namespace, or null if the prefix is not in scope
     */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        NamePool pool = getNamePool();
        if ((prefix==null || prefix.length()==0) && !useDefault) {
            return "";
        } else if ("xml".equals(prefix)) {
            return NamespaceConstant.XML;
        } else {
            short prefixCode = pool.getCodeForPrefix(prefix);
            short uriCode = getURICode(prefixCode);
            if (uriCode == -1) {
                return null;
            }
            return pool.getURIFromURICode(uriCode);
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        NamePool pool = getNamePool();
        List prefixes = new ArrayList(namespacesSize);
        for (int i=namespacesSize-1; i>=0; i--) {
            String prefix = pool.getPrefixFromNamespaceCode(namespaces[i]);
            if (!prefixes.contains(prefix)) {
                prefixes.add(prefix);
            }
        }
        prefixes.add("xml");
        return prefixes.iterator();
    }

    /**
     * Check that the prefix for an element or attribute is acceptable, allocating a substitute
     * prefix if not. The prefix is acceptable unless a namespace declaration has been
     * written that assignes this prefix to a different namespace URI. This method
     * also checks that the element or attribute namespace has been declared, and declares it
     * if not.
     * @param nameCode the proposed element or attribute name
     * @param seq sequence number of attribute, used for generating distinctive prefixes
     * @return the actual allocated name, which may be different.
    */

    private int checkProposedPrefix(int nameCode, int seq) throws XPathException {
        NamePool namePool = getNamePool();
        int nscode = namePool.getNamespaceCode(nameCode);
        if (nscode == -1) {
            // avoid calling allocate where possible, because it's synchronized
            nscode = namePool.allocateNamespaceCode(nameCode);
        }
        int nsprefix = nscode>>16;

        short existingURICode = getURICode((short)nsprefix);
        if (existingURICode == -1) {
            // prefix has not been declared: declare it now (namespace fixup)
            namespace(nscode, 0);
            return nameCode;
        } else {
            if ((nscode & 0xffff) == existingURICode) {
                // prefix is already bound to this URI
                return nameCode;	// all is well
            } else {
                // conflict: prefix is currently bound to a different URI
                String prefix = getSubstitutePrefix(nscode, seq);

                int newCode = namePool.allocate(
                                    prefix,
                                    namePool.getURI(nameCode),
                                    namePool.getLocalName(nameCode));
                namespace(namePool.allocateNamespaceCode(newCode), 0);
                return newCode;
            }
        }
    }

    /**
     * It is possible for a single output element to use the same prefix to refer to different
     * namespaces. In this case we have to generate an alternative prefix for uniqueness. The
     * one we generate is based on the sequential position of the element/attribute: this is
     * designed to ensure both uniqueness (with a high probability) and repeatability
     * @param nscode the namespace code of the proposed element or attribute name
     * @param seq sequence number of attribute, used for generating distinctive prefixes
     * @return the actual allocated name, which may be different.
     */

    private String getSubstitutePrefix(int nscode, int seq) {
        String prefix = getNamePool().getPrefixFromNamespaceCode(nscode);
        return prefix + '_' + seq;
    }

    /**
     * Get an element node representing the element whose start tag this is, as required
     * for implementing conditional type assignment
     * @return an element node. This contains all the required namespaces and attributes, and has no children;
     * it is untyped, as are the attributes.
     */

    public NodeInfo getElementNode() throws XPathException {
        if (elementNode == null) {
            int len = bufferedAttributes.getLength();
            TinyBuilder builder = new TinyBuilder();
            builder.setSizeParameters(new int[]{2, len+2, namespacesSize+2, 16});
            builder.setPipelineConfiguration(getPipelineConfiguration());
            builder.open();
            builder.startElement(elementNameCode, StandardNames.XS_UNTYPED_ATOMIC, 0, 0);
            for (int i=0; i<namespacesSize; i++) {
                builder.namespace(namespaces[i], 0);
            }
            for (int i=0; i<len; i++) {
                builder.attribute(bufferedAttributes.getNameCode(i), StandardNames.XS_UNTYPED_ATOMIC,
                        bufferedAttributes.getValue(i), 0, 0);
            }
            builder.startContent();
            builder.endElement();
            elementNode = builder.getCurrentRoot();
        }
        return elementNode;
    }


}

// Copyright (c) 2004 - 2007 Saxonica Limited

