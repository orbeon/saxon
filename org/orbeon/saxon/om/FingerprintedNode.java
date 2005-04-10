package net.sf.saxon.om;

/**
 * This is a marker interface used to identify nodes that contain a namepool fingerprint. Although all nodes
 * are capable of returning a fingerprint, some (notably DOM, XOM, and JDOM nodes) need to calculate it on demand.
 * A node that implements this interface indicates that obtaining the fingerprint for use in name comparisons
 * is more efficient than using the URI and local name.
 */

public interface FingerprintedNode {}

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

