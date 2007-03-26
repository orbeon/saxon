package org.orbeon.saxon.event;
import org.orbeon.saxon.codenorm.Normalizer;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.value.Whitespace;

/**
 * UnicodeNormalizer: This ProxyReceiver performs unicode normalization on the contents
 * of attribute and text nodes.
 *
 * @author Michael Kay
*/


public class UnicodeNormalizer extends ProxyReceiver {

    private Normalizer normalizer;

    public UnicodeNormalizer(String form) throws XPathException {
        byte fb;
        if (form.equals("NFC")) {
            fb = Normalizer.C;
        } else if (form.equals("NFD")) {
            fb = Normalizer.D;
        } else if (form.equals("NFKC")) {
            fb = Normalizer.KC;
        } else if (form.equals("NFKD")) {
            fb = Normalizer.KD;
        } else {
            DynamicError err = new DynamicError("Unknown normalization form " + form);
            err.setErrorCode("SESU0011");
            throw err;
        }

        normalizer = new Normalizer(fb);
    }

    /**
     * Output an attribute
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
            throws XPathException {
        nextReceiver.attribute(nameCode, typeCode, normalizer.normalize(value), locationId, properties);
    }

    /**
    * Output character data
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (Whitespace.isWhite(chars)) {
            nextReceiver.characters(chars, locationId, properties);
        } else {
            nextReceiver.characters(normalizer.normalize(chars), locationId, properties);
        }
    }

};

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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

