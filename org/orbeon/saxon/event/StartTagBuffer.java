package net.sf.saxon.event;
import net.sf.saxon.om.*;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
  * StartTagBuffer is a ProxyReceiver that buffers attributes and namespace events within a start tag.
  * It maintains details of the namespace context, and a full set of attribute information, on behalf
  * of other filters that need access to namespace information or need to process attributes in arbitrary
  * order.
  */

public class StartTagBuffer extends ProxyReceiver implements NamespaceResolver
{

    // Details of the pending element event

    int elementNameCode;
    int elementTypeCode;
    int elementLocationId;
    int elementProperties;

    // Details of pending attribute events

    AttributeCollection bufferedAttributes;

    // We keep track of namespaces. The namespaces
    // vector holds a list of all namespaces currently declared (organised as pairs of entries,
    // prefix followed by URI). The stack contains an entry for each element currently open; the
    // value on the stack is an Integer giving the number of namespaces added to the main
    // namespace stack by that element.

    private int[] namespaces = new int[50];          // all namespace codes currently declared
    private int namespacesSize = 0;                  // all namespaces currently declared
    private int[] countStack = new int[50];
    private int depth = 0;

    // TODO: support namespace undeclarations

    public void setPipelineConfiguration(PipelineConfiguration config) {
        super.setPipelineConfiguration(config);
        bufferedAttributes = new AttributeCollection(getNamePool());
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
    }

    public void namespace(int namespaceCode, int properties) throws XPathException {
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

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        bufferedAttributes.addAttribute(nameCode, typeCode, value.toString(), locationId, properties);
    }

    /**
    * Add a namespace declaration to the stack
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
        super.startElement(elementNameCode, elementTypeCode, elementLocationId, elementProperties);
        for (int i=namespacesSize - countStack[depth-1]; i<namespacesSize; i++) {
            super.namespace(namespaces[i], 0);
            // TODO: remember the namespace event properties
        }
        for (int i=0; i<bufferedAttributes.getLength(); i++) {
            super.attribute(bufferedAttributes.getNameCode(i),
                    bufferedAttributes.getTypeAnnotation(i),
                    bufferedAttributes.getValue(i),
                    bufferedAttributes.getLocationId(i),
                    bufferedAttributes.getProperties(i));
        }
        super.startContent();
    }

    /**
    * endElement: Discard the namespaces declared on this element.
    */

    public void endElement () throws XPathException
    {
        int nscount = countStack[--depth];
        namespacesSize -= nscount;
        super.endElement();
    }

    /**
     * Get the name of the current element
     */

    public int getElementNameCode() {
        return elementNameCode;
    }

    /**
     * Get the value of the current attribute with a given nameCode
     * @return the attribute value, or null if the attribute is not present
     */

    public String getAttribute(int nameCode) {
        return bufferedAttributes.getValueByFingerprint(nameCode & 0xfffff);
    }

    /**
     * Get the URI code corresponding to a given prefix code, by searching the
     * in-scope namespaces. This is a service provided to subclasses.
     * @param prefixCode the 16-bit prefix code required
     * @return the 16-bit URI code, or -1 if the prefix is not found
     */

    protected short getURICode(short prefixCode) {
        for (int i=namespacesSize-1; i>=0; i--) {
        	if ((namespaces[i]>>16) == (prefixCode)) {
        		return (short)(namespaces[i]&0xffff);
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
        if ("".equals(prefix) && !useDefault) {
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
     * Use this NamespaceContext to resolve a lexical QName
     *
     * @param qname      the lexical QName; this must have already been lexically validated
     * @param useDefault true if the default namespace is to be used to resolve an unprefixed QName
     * @param pool       the NamePool to be used
     * @return the integer fingerprint that uniquely identifies this name
     * @throws net.sf.saxon.xpath.DynamicError
     *          if the string is not a valid lexical QName or
     *          if the namespace prefix has not been declared
     */

    public int getFingerprint(String qname, boolean useDefault, NamePool pool) throws DynamicError {
        try {
            String[] parts = Name.getQNameParts(qname);
            String uri = getURIForPrefix(parts[0], useDefault);
            return pool.allocate(parts[0], uri, parts[1]);
        } catch (QNameException e) {
            throw new DynamicError(e.getMessage());
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
