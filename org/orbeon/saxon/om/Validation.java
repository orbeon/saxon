package net.sf.saxon.om;


/**
* This class contains constants and static methods to manipulate the validation
* property of a type.
*/

public final class Validation {

    public static final int INVALID = -1;

    public static final int STRICT = 1;
    public static final int LAX = 2;
    public static final int PRESERVE = 3;
    public static final int STRIP = 4;
    public static final int SKIP = 4;   // synonym provided for the XQuery API

    public static final int DEFAULT = 0;

    public static final int VALIDATION_MODE_MASK = 0xff;

    public static final int VALIDATE_OUTPUT = 0x10000;

    /**
     * This class is never instantiated
     */

    private Validation() {
    }

    public static int getCode(String value) {
        if (value.equals("strict")) {
            return STRICT;
        } else if (value.equals("lax")) {
            return LAX;
        } else if (value.equals("preserve")) {
            return PRESERVE;
        } else if (value.equals("strip")) {
            return STRIP;
        } else {
            return INVALID;
        }
    }

    public static String toString(int value) {
        switch(value & VALIDATION_MODE_MASK) {
            case STRICT: return "strict";
            case LAX: return "lax";
            case PRESERVE: return "preserve";
            case STRIP: return "skip";  // for XQuery
            default: return "invalid";
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
