package org.orbeon.saxon.event;


/**
 * LocationProvider: this interface represents an object that
 * provides the location of elements in a source document or instructions in a stylesheet
 * or query. A locationProvider may be passed down the Receiver pipeline as part of the
 * PipelineConfiguration object; on the input pipeline, this will be a {@link SaxonLocator} object,
 * on the output pipeline, it will be a {@link org.orbeon.saxon.instruct.LocationMap}
 * <p/>
 * A LocationProvider that represents locations in the source document from which the events
 * are derived (as distinct from locations in a query or stylesheet of the instructions causing the
 * events) will also implement the marker interface {@link org.orbeon.saxon.event.SourceLocationProvider}
 */

public interface LocationProvider {

    /**
     * Get the URI of the document or module containing a particular location
     * @param locationId identifier of the location in question (as passed down the Receiver pipeline)
     * @return the URI of the document or module.
     */

    public String getSystemId(long locationId);

    /**
     * Get the line number within the document or module containing a particular location
     * @param locationId identifier of the location in question (as passed down the Receiver pipeline)
     * @return the line number within the document or module.
     */

    public int getLineNumber(long locationId);

    /**
     * Get the column number within the document or module containing a particular location
     * @param locationId identifier of the location in question (as passed down the Receiver pipeline)
     * @return the column number within the document or module, or -1 if this is not available
     */

    public int getColumnNumber(long locationId);


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
