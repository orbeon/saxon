package net.sf.saxon.event;
import net.sf.saxon.expr.ExpressionLocation;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.QNameValue;

/**
 * This class is used for generating complex content, that is, the content of an
 * element or document node. It enforces the rules on the order of events within
 * complex content (attributes and namespaces must come first), and it implements
 * part of the namespace fixup rules, in particular, it ensures that there is a
 * namespace node for the namespace used in the element name and in each attribute
 * name.
 *
 * <p>The same ComplexContentOutputter may be used for generating an entire XML
 * document; it is not necessary to create a new outputter for each element node.</p>
 *
 * @author Michael H. Kay
 */



public class ComplexContentOutputter extends SequenceReceiver {

    private Receiver receiver;
            // the next receiver in the output pipeline

    private int pendingStartTag = -1;
    private boolean topLevel = true;
    private Boolean elementIsInNullNamespace;
    private int[] pendingAttCode = new int[20];
    private int[] pendingAttType = new int[20];
    private CharSequence[] pendingAttValue = new String[20];
    private int[] pendingAttLocation = new int[20];
    private int[] pendingAttProp = new int[20];
    private int pendingAttListSize = 0;

    private int[] pendingNSList = new int[20];
    private int pendingNSListSize = 0;

    private int currentSimpleType = -1;  // any other value means we are currently writing an
                                         // element of a particular simple type

    private boolean suppressAttributes = false;
                                        // set to true during error recovery only
    private int startElementProperties;
    private int startElementLocationId;

    public void setSystemId(String systemId) {}

    public String getSystemId() {
        return null;
    }

    /**
     * Set the receiver (to handle the next stage in the pipeline) directly
     */

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Start the output process
     */

    public void open() throws XPathException {
        receiver.open();
        previousAtomic = false;
    }

    /**
     * Start of a document node.
    */

    public void startDocument(int properties) throws XPathException {
        receiver.startDocument(properties);
        previousAtomic = false;
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException { }


    /**
    * Produce text content output. <BR>
    * Special characters are escaped using XML/HTML conventions if the output format
    * requires it.
    * @param s The String to be output
    * @exception XPathException for any failure
    */

    public void characters(CharSequence s, int locationId, int properties) throws XPathException {
        previousAtomic = false;
        if (s==null) return;
        int len = s.length();
        if (len==0) return;
        if (pendingStartTag != -1) {
            startContent();
        }
        receiver.characters(s, locationId, properties);
    }

    /**
    * Output an element start tag. <br>
    * The actual output of the tag is deferred until all attributes have been output
    * using attribute().
    * @param nameCode The element name code
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        // System.err.println("StartElement " + nameCode);
        topLevel = false;
        if (nameCode == -1) {
            // This is used on error recovery when there is a bad element name. It is needed so that
            // the outputter can suppress any subsequent namespace and attribute nodes
            suppressAttributes = true;
            return;
        }
        suppressAttributes = false;
        if (pendingStartTag != -1) {
            startContent();
        }
        startElementProperties = properties;
        startElementLocationId = locationId;
        pendingAttListSize = 0;
        pendingNSListSize = 0;
        pendingStartTag = nameCode;
        elementIsInNullNamespace = null; // meaning not yet computed
        currentSimpleType = typeCode;
        previousAtomic = false;
    }


    /**
    * Output a namespace declaration. <br>
    * This is added to a list of pending namespaces for the current start tag.
    * If there is already another declaration of the same prefix, this one is
    * ignored, unless the REJECT_DUPLICATES flag is set, in which case this is an error.
    * Note that unlike SAX2 startPrefixMapping(), this call is made AFTER writing the start tag.
    * @param nscode The namespace code
    * @throws XPathException if there is no start tag to write to (created using writeStartTag),
    * or if character content has been written since the start tag was written.
    */

    public void namespace(int nscode, int properties)
    throws XPathException {

        if (suppressAttributes) return;

        // System.err.println("Write namespace prefix=" + (nscode>>16) + " uri=" + (nscode&0xffff));
        if (pendingStartTag==-1) {
            throw new NoOpenStartTagException(Type.NAMESPACE, topLevel);
        }

        // elimination of namespaces already present on an outer element of the
        // result tree is now done by the NamespaceReducer.

        // Handle declarations whose prefix is duplicated for this element.

        boolean rejectDuplicates = (properties & ReceiverOptions.REJECT_DUPLICATES) != 0;

        for (int i=0; i<pendingNSListSize; i++) {
            if (nscode == pendingNSList[i]) {
                // same prefix and URI: ignore this duplicate
                return;
            }
        	if ((nscode>>16) == (pendingNSList[i]>>16)) {
        	    if (rejectDuplicates) {
                    // In XSLT this error is recoverable. However, the recovery action is to take the
                    // last of the namespaces rather than the first. This recovery action gives us problems
                    // because we have already assumed that the first one will be generated, for example
                    // when checking any attributes in the sequence. So we make it a hard error.
        	        DynamicError err = new DynamicError("Cannot create two namespace nodes with the same name");
                    err.setErrorCode("XT0430");
                    throw err;
        	    } else {
        		    // same prefix, do a quick exit
        		    return;
        		}
        	}
        }

        // It is an error to output a namespace node for the default namespace if the element
        // itself is in the null namespace, as the resulting element could not be serialized

        if (((nscode>>16) == 0) && ((nscode&0xffff)!=0)) {
            if (elementIsInNullNamespace == null) {
                elementIsInNullNamespace = Boolean.valueOf(
                        getNamePool().getURI(pendingStartTag) == NamespaceConstant.NULL);
            }
            if (elementIsInNullNamespace.booleanValue()) {
                DynamicError err = new DynamicError(
                        "Cannot output a namespace node for the default namespace when the element is in no namespace");
                err.setErrorCode("XT0440");
                throw err;
            }
        }

        // if it's not a duplicate namespace, add it to the list for this start tag

        if (pendingNSListSize+1 > pendingNSList.length) {
            int[] newlist = new int[pendingNSListSize * 2];
            System.arraycopy(pendingNSList, 0, newlist, 0, pendingNSListSize);
            pendingNSList = newlist;
        }
        pendingNSList[pendingNSListSize++] = nscode;
        previousAtomic = false;
    }


    /**
    * Output an attribute value. <br>
    * This is added to a list of pending attributes for the current start tag, overwriting
    * any previous attribute with the same name. <br>
    * This method should NOT be used to output namespace declarations.<br>
    * @param nameCode The name of the attribute
    * @param value The value of the attribute
    * @param properties Bit fields containing properties of the attribute to be written
    * @throws XPathException if there is no start tag to write to (created using writeStartTag),
    * or if character content has been written since the start tag was written.
    */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        //System.err.println("Write attribute " + nameCode + "=" + value + " to Outputter " + this);

        if (suppressAttributes) return;

        if (pendingStartTag==-1) {
            DynamicError err = new NoOpenStartTagException(Type.ATTRIBUTE, topLevel);
            err.setLocator(new ExpressionLocation(
                    getPipelineConfiguration().getLocationProvider(),
                    locationId));
            throw err;
        }

        // if this is a duplicate attribute, overwrite the original, unless
        // the REJECT_DUPLICATES option is set.

        for (int a=0; a<pendingAttListSize; a++) {
            if ((pendingAttCode[a] & 0xfffff) == (nameCode & 0xfffff)) {
                if ((properties & ReceiverOptions.REJECT_DUPLICATES) == 0) {
                    pendingAttType[a] = typeCode;
                    pendingAttValue[a] = value;
                    pendingAttLocation[a] = locationId;
                    pendingAttProp[a] = properties;
                    return;
                } else {
                    DynamicError err = new DynamicError("Duplicate attribute: " +
                            getNamePool().getDisplayName(nameCode));
                    err.setErrorCode("XQ0025");
                    throw err;
                }
            }
        }

        // otherwise, add this one to the list

        if (pendingAttListSize >= pendingAttCode.length) {
            int[] attCode2 = new int[pendingAttListSize*2];
            int[] attType2 = new int[pendingAttListSize*2];
            String[] attValue2 = new String[pendingAttListSize*2];
            int[] attLoc2 = new int[pendingAttListSize*2];
            int[] attProp2 = new int[pendingAttListSize*2];
            System.arraycopy(pendingAttCode, 0, attCode2, 0, pendingAttListSize);
            System.arraycopy(pendingAttType, 0, attType2, 0, pendingAttListSize);
            System.arraycopy(pendingAttValue, 0, attValue2, 0, pendingAttListSize);
            System.arraycopy(pendingAttLocation, 0, attLoc2, 0, pendingAttListSize);
            System.arraycopy(pendingAttProp, 0, attProp2, 0, pendingAttListSize);
            pendingAttCode = attCode2;
            pendingAttType = attType2;
            pendingAttValue = attValue2;
            pendingAttLocation = attLoc2;
            pendingAttProp = attProp2;
        }

        pendingAttCode[pendingAttListSize] = nameCode;
        pendingAttType[pendingAttListSize] = typeCode;
        pendingAttValue[pendingAttListSize] = value;
        pendingAttLocation[pendingAttListSize] = locationId;
        pendingAttProp[pendingAttListSize] = properties;
        pendingAttListSize++;
        previousAtomic = false;
    }

	/**
	* Check that the prefix for an element or attribute is acceptable, allocating a substitute
	* prefix if not. The prefix is acceptable unless a namespace declaration has been
	* written that assignes this prefix to a different namespace URI. This method
	* also checks that the element or attribute namespace has been declared, and declares it
	* if not.
	*/

	private int checkProposedPrefix(int nameCode, int seq) throws XPathException {
        NamePool namePool = getNamePool();
        int nscode = namePool.getNamespaceCode(nameCode);
        if (nscode == -1) {
            // avoid calling allocate where possible, because it's synchronized
		    nscode = namePool.allocateNamespaceCode(nameCode);
        }
		int nsprefix = nscode>>16;

        for (int i=0; i<pendingNSListSize; i++) {
        	if (nsprefix == (pendingNSList[i]>>16)) {
        		// same prefix
        		if ((nscode & 0xffff) == (pendingNSList[i] & 0xffff)) {
        			// same URI
        			return nameCode;	// all is well
        		} else {
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
        // no declaration of this prefix: declare it now
        namespace(nscode, 0);
        return nameCode;
    }

    /**
    * It is possible for a single output element to use the same prefix to refer to different
    * namespaces. In this case we have to generate an alternative prefix for uniqueness. The
    * one we generate is based on the sequential position of the element/attribute: this is
    * designed to ensure both uniqueness (with a high probability) and repeatability
    */

    private String getSubstitutePrefix(int nscode, int seq) {
    	String prefix = getNamePool().getPrefixFromNamespaceCode(nscode);
        return prefix + '_' + seq;
    }

    /**
    * Output an element end tag.
    */

    public void endElement() throws XPathException {
        //System.err.println("Write end tag " + this + " : " + name);
        if (pendingStartTag != -1) {
            startContent();
        }

        // write the end tag

        receiver.endElement();
        previousAtomic = false;
    }

    /**
    * Write a comment
    */

    public void comment(CharSequence comment, int locationId, int properties) throws XPathException {
        if (pendingStartTag != -1) {
            startContent();
        }
        receiver.comment(comment, locationId, properties);
        previousAtomic = false;
    }

    /**
    * Write a processing instruction
    */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (pendingStartTag != -1) {
            startContent();
        }
        receiver.processingInstruction(target, data, locationId, properties);
        previousAtomic = false;
    }

    /**
    * Append an arbitrary item (node or atomic value) to the output
     * @param item the item to be appended
     * @param locationId the location of the calling instruction, for diagnostics
     * @param copyNamespaces if the item is an element node, this indicates whether its namespaces
     */

    public void append(Item item, int locationId, int copyNamespaces) throws XPathException {
        if (item == null) {
            return;
        } else if (item instanceof AtomicValue) {
            if (previousAtomic) {
                characters(" ", locationId, 0);
            }
            if (item instanceof QNameValue) {
                DynamicError err = new DynamicError("Cannot add a QName value to the content of an element");
                err.setErrorCode("XT0380");
                throw err;
            }
            characters(item.getStringValueCS(), locationId, 0);
            previousAtomic = true;
        } else if (item instanceof DocumentInfo) {
            SequenceIterator iter = ((DocumentInfo)item).iterateAxis(Axis.CHILD);
            while (true) {
                Item it = iter.next();
                if (it == null) break;
                append(it, locationId, copyNamespaces);
            }
        } else {
            ((NodeInfo)item).copy(this, copyNamespaces, true, locationId);
            previousAtomic = false;
        }
    }


    /**
    * Close the output
    */

    public void close() throws XPathException {
        // System.err.println("Close " + this + " using emitter " + emitter.getClass());
        receiver.close();
        previousAtomic = false;
    }

    /**
    * Flush out a pending start tag
    */

    public void startContent() throws XPathException {

        if (pendingStartTag == -1) {
            // this can happen if the method is called from outside,
            // e.g. from a SequenceOutputter earlier in the pipeline
            return;
        }

        int elcode = checkProposedPrefix(pendingStartTag, 0);
        receiver.startElement(elcode, currentSimpleType,
                startElementLocationId, startElementProperties | ReceiverOptions.NAMESPACE_OK);

        for (int a=0; a<pendingAttListSize; a++) {
            int attcode = pendingAttCode[a];
            if ((attcode>>20 & 0xff) != 0) {	// non-null prefix
                pendingAttCode[a] = checkProposedPrefix(attcode, a+1);
            }
        }

        for (int n=0; n<pendingNSListSize; n++) {
            receiver.namespace(pendingNSList[n], 0);
        }

        for (int a=0; a<pendingAttListSize; a++) {
            receiver.attribute( pendingAttCode[a],
                                pendingAttType[a],
                                pendingAttValue[a],
                                pendingAttLocation[a],
                                pendingAttProp[a]);
        }

        receiver.startContent();

        pendingAttListSize = 0;
        pendingNSListSize = 0;
        pendingStartTag = -1;
        previousAtomic = false;
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
