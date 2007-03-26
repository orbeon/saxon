package org.orbeon.saxon.xqj;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.value.SequenceType;

import org.orbeon.saxon.javax.xml.xquery.XQItemType;
import org.orbeon.saxon.javax.xml.xquery.XQSequenceType;

import org.orbeon.saxon.javax.xml.xquery.XQSequenceType;

/**
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: 14-May-2006
 * Time: 20:36:38
 * To change this template use File | Settings | File Templates.
 */
public class SaxonXQSequenceType implements XQSequenceType {

    SequenceType sequenceType;
    Configuration config;

    SaxonXQSequenceType(SequenceType sequenceType, Configuration config) {
        this.sequenceType = sequenceType;
        this.config = config;
    }

    public int getItemOccurrence() {
        int cardinality = sequenceType.getCardinality();
        switch (cardinality) {
            case StaticProperty.EXACTLY_ONE:
                return XQSequenceType.OCC_EXACTLY_ONE;
            case StaticProperty.ALLOWS_ZERO_OR_ONE:
                return XQSequenceType.OCC_ZERO_OR_ONE;
            case StaticProperty.ALLOWS_ONE_OR_MORE:
                return XQSequenceType.OCC_ONE_OR_MORE;
            case StaticProperty.ALLOWS_ZERO_OR_MORE:
                return XQSequenceType.OCC_ZERO_OR_MORE;
            default:
                return XQSequenceType.OCC_ZERO_OR_MORE;
        }
    }

    public XQItemType getItemType() {
        return new SaxonXQItemType(sequenceType.getPrimaryType(), config);
    }

    public String getString() {
        String s = sequenceType.getPrimaryType().toString(config.getNamePool());
        switch (sequenceType.getCardinality()) {
            case StaticProperty.EXACTLY_ONE:
                return s;
            case StaticProperty.ALLOWS_ZERO_OR_ONE:
                return s + "?";
            case StaticProperty.ALLOWS_ONE_OR_MORE:
                return s + "+";
            case StaticProperty.ALLOWS_ZERO_OR_MORE:
                return s + "*";
            default:
                return s;
        }
    }

    public String toString() {
        return getString();
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
// Contributor(s):
//