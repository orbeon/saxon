package net.sf.saxon;


/**
  * FeatureKeys defines a set of constants, names of Saxon configuration
  * options which can be supplied to the TransformerFactoryImpl interface
  *
  * @author Michael H. Kay
  */


public class FeatureKeys {

	/**
	* ALLOW_EXTERNAL_FUNCTIONS must be a Boolean; it determines whether calls to external functions are allowed
	*/

	public final static String ALLOW_EXTERNAL_FUNCTIONS =
	        "http://saxon.sf.net/feature/allow-external-functions";

	/**
	* TRACE_EXTERNAL_FUNCTIONS must be a Boolean; it determines whether the loading and binding of extension
     * functions is traced
	*/

	public final static String TRACE_EXTERNAL_FUNCTIONS =
	        "http://saxon.sf.net/feature/trace-external-functions";

	/**
	* TIMING must be an Boolean; it determines whether basic timing information is output to System.err
	*/

	public final static String TIMING =
	        "http://saxon.sf.net/feature/timing";

	/**
	* TREE_MODEL must be an Integer: Builder.STANDARD_TREE or Builder.TINY_TREE
	*/

	public final static String TREE_MODEL =
	        "http://saxon.sf.net/feature/treeModel";

	/**
	* TRACE_LISTENER must be a class that implements net.sf.saxon.trace.TraceListener
	*/

	public final static String TRACE_LISTENER =
	        "http://saxon.sf.net/feature/traceListener";

	/**
	* LINE_NUMBERING must be a Boolean(); it determines whether line numbers are maintained for the
     * source document
	*/

	public final static String LINE_NUMBERING =
	        "http://saxon.sf.net/feature/linenumbering";

	/**
	* RECOVERY_POLICY must be an Integer: Controller.RECOVER_SILENTLY,
	* Controller.RECOVER_WITH_WARNINGS, or Controller.DO_NOT_RECOVER
	*/

	public final static String RECOVERY_POLICY =
	        "http://saxon.sf.net/feature/recoveryPolicy";

	/**
	* MESSAGE_EMITTER_CLASS must be the class name of an Emitter
	*/

	public final static String MESSAGE_EMITTER_CLASS =
	        "http://saxon.sf.net/feature/messageEmitterClass";

    /**
    * SOURCE_PARSER_CLASS must be the full class name of an XMLReader
    */

    public final static String SOURCE_PARSER_CLASS =
            "http://saxon.sf.net/feature/sourceParserClass";

    /**
    * STYLE_PARSER_CLASS must be an XMLReader
    */

    public final static String STYLE_PARSER_CLASS =
            "http://saxon.sf.net/feature/styleParserClass";

    /**
    * NAME_POOL must be an instance of net.sf.saxon.om.NamePool
    */

    public final static String NAME_POOL =
            "http://saxon.sf.net/feature/namePool";

    /**
    * OUTPUT_URI_RESOLVER must be an instance of net.sf.saxon.OutputURIResolver
    */

    public final static String OUTPUT_URI_RESOLVER =
            "http://saxon.sf.net/feature/outputURIResolver";

	/**
	* DTD_VALIDATION must be a Boolean. This determines whether source documents should be
	* parsed with DTD-validation enabled.
	*/

	public final static String DTD_VALIDATION =
	        "http://saxon.sf.net/feature/validation";

	/**
	* SCHEMA_VALIDATION must be a Boolean. This determines whether source documents should be
	* parsed with schema-validation enabled.
	*/

	public final static String SCHEMA_VALIDATION =
	        "http://saxon.sf.net/feature/schema-validation";

    /**
    * VALIDATION_WARNINGS must be a Boolean. This determines whether validation errors in result
    * documents should be treated as fatal. By default they are fatal; with this option set, they
    * are treated as warnings.
    */

    public final static String VALIDATION_WARNINGS =
            "http://saxon.sf.net/feature/validation-warnings";


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
// The Original Code is: all this file, other than fragments copied from the SAX distribution
// made available by David Megginson, and the line marked PB-SYNC.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// The line marked PB-SYNC is by Peter Bryant (pbryant@bigfoot.com). All Rights Reserved.
//
// Contributor(s): Michael Kay, Peter Bryant, David Megginson
//
