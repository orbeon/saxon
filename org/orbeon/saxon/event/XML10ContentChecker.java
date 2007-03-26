package org.orbeon.saxon.event;

import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.Name10Checker;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.XMLChar;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.ExpressionLocation;
import org.orbeon.saxon.sort.IntHashSet;

/**
 * This class is used on the serialization pipeline to check that the document conforms
 * to XML 1.0 rules. It is placed on the pipeline only when the configuration permits
 * XML 1.1 constructs, but the particular output document is being serialized as XML 1.0
 */

public class XML10ContentChecker extends ProxyReceiver {

    private NameChecker checker = Name10Checker.getInstance();
    private NamePool pool;
    private IntHashSet cache = new IntHashSet(100);

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        pool = pipe.getConfiguration().getNamePool();
        super.setPipelineConfiguration(pipe);
    }

    /**
     * Notify the start of an element
     *
     * @param nameCode   integer code identifying the name of the element within the name pool.
     * @param typeCode   integer code identifying the element's type within the name pool.
     * @param properties properties of the element node
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (!cache.contains(nameCode)) {
            if (!checker.isValidNCName(pool.getLocalName(nameCode))) {
                DynamicError err = new DynamicError("Invalid XML 1.0 element name " +
                        Err.wrap(pool.getLocalName(nameCode), Err.ELEMENT));
                err.setErrorCode("SERE0005");
                err.setLocator(new ExpressionLocation(getPipelineConfiguration().getLocationProvider(), locationId));
                throw err;
            }
            cache.add(nameCode);
        }
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
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
        if (!cache.contains(nameCode)) {
            if (!checker.isValidNCName(pool.getLocalName(nameCode))) {
                DynamicError err = new DynamicError("Invalid XML 1.0 attribute name " +
                        Err.wrap(pool.getLocalName(nameCode), Err.ATTRIBUTE));
                err.setErrorCode("SERE0005");
                err.setLocator(new ExpressionLocation(getPipelineConfiguration().getLocationProvider(), locationId));
                throw err;
            }
            cache.add(nameCode);
        }
        checkString(value, locationId);
        nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        checkString(chars, locationId);
        nextReceiver.characters(chars, locationId, properties);
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        checkString(chars, locationId);
        nextReceiver.comment(chars, locationId, properties);
    }

    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (!checker.isValidNCName(target)) {
            DynamicError err = new DynamicError("Invalid XML 1.0 processing instruction name " +
                    Err.wrap(target));
            err.setErrorCode("SERE0005");
            err.setLocator(new ExpressionLocation(getPipelineConfiguration().getLocationProvider(), locationId));
            throw err;
        }
        checkString(data, locationId);
        nextReceiver.processingInstruction(target, data, locationId, properties);
    }

     /**
     * Check that a string consists of valid XML 1.0 characters (UTF-16 encoded)
     */

    private void checkString(CharSequence in, int locationId) throws XPathException {
         final int len = in.length();
         for (int c=0; c<len; c++) {
            int ch32 = in.charAt(c);
            if (XMLChar.isHighSurrogate(ch32)) {
                char low = in.charAt(++c);
                ch32 = XMLChar.supplemental((char)ch32, low);
            }
            if (!XMLChar.isValid(ch32)) {
                DynamicError err = new DynamicError(
                        "The result tree contains a character not allowed by XML 1.0 (hex " +
                        Integer.toHexString(ch32) + ')');
                err.setErrorCode("SERE0006");
                err.setLocator(new ExpressionLocation(getPipelineConfiguration().getLocationProvider(), locationId));
                throw err;
            }
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
// The Initial Developer of the Original Code is Michael H. Kay. The detectEncoding() method includes
// code fragments taken from the AElfred XML Parser developed by David Megginson.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

