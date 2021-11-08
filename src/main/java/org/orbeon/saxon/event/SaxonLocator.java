package org.orbeon.saxon.event;
import org.xml.sax.Locator;

import javax.xml.transform.SourceLocator;

/**
  * SaxonLocator: this interface exists to unify the SAX Locator and JAXP SourceLocator interfaces,
  * which are identical. It extends both interfaces. Therefore, anything
  * that implements SaxonLocator can be used both in SAX and in JAXP interfaces.
  */

public interface SaxonLocator extends Locator, SourceLocator, LocationProvider {}

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
