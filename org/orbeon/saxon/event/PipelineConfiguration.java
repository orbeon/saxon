package net.sf.saxon.event;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;

/**
 * A PipelineConfiguration sets options that apply to all the operations in a pipeline.
 * Unlike the global Configuration, these options are always local to a process.
 */

public class PipelineConfiguration {

    private Configuration config;
    private LocationProvider locationProvider;
    private ErrorListener errorListener;
    private URIResolver uriResolver;
    private Controller controller;

    public PipelineConfiguration() {}

    public PipelineConfiguration(PipelineConfiguration p) {
        config = p.config;
        locationProvider = p.locationProvider;
        errorListener = p.errorListener;
        uriResolver = p.uriResolver;
        controller = p.controller;
    }

    public Configuration getConfiguration() {
        return config;
    }

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    public LocationProvider getLocationProvider() {
        return locationProvider;
    }

    public void setLocationProvider(LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public URIResolver getURIResolver() {
        return uriResolver;
    }

    public void setURIResolver(URIResolver uriResolver) {
        this.uriResolver = uriResolver;
    }

     public void setController(Controller controller) {
         this.controller = controller;
     }

    /**
     * Get the controller associated with this pipelineConfiguration
     * @return the controller if it is known; otherwise null.
     */

    public Controller getController() {
        return controller;
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

