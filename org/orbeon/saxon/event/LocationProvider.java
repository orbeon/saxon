package net.sf.saxon.event;


/**
 * LocationProvider: this is a marker interface used to identify an object that
 * provides the location of elements in a source document or instructions in a stylesheet
 * or query. A locationProvider may be passed down the Receiver pipeline as part of the
 * PipelineConfiguration object; on the input pipeline, this will be a {@link SaxonLocator} object,
 * on the output pipeline, it will be a {@link net.sf.saxon.instruct.LocationMap}
 */

public interface LocationProvider {

    public String getSystemId(int locationId);

    public int getLineNumber(int locationId);
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
