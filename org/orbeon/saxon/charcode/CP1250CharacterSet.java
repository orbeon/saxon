package net.sf.saxon.charcode;

/**
* This class defines properties of the cp1250 Central Europe character set,
 * as defined at <a ref="http://www.microsoft.com/globaldev/reference/sbcs/1250.htm">http://www.microsoft.com/globaldev/reference/sbcs/1250.htm</a>.
*/

public class CP1250CharacterSet implements CharacterSet{

    public static CP1250CharacterSet theInstance = null;

    private CP1250CharacterSet() {}

    public static CP1250CharacterSet getInstance() {
        if (theInstance == null) {
            init();
            theInstance = new CP1250CharacterSet();
        }
        return theInstance;
    }

    private static boolean[] c = null;

    private static void init() {

        c = new boolean[740];

        for (int i=0; i<127; i++) {
            c[i] = true;
        }
        for (int i=128; i<740; i++) {
            c[i] = false;
        }

        c[160] = true;
        c[164] = true;
        c[166] = true;
        c[167] = true;
        c[168] = true;
        c[169] = true;
        c[171] = true;
        c[172] = true;
        c[173] = true;
        c[174] = true;
        c[176] = true;
        c[177] = true;
        c[180] = true;
        c[181] = true;
        c[182] = true;
        c[183] = true;
        c[184] = true;
        c[187] = true;
        c[193] = true;
        c[194] = true;
        c[196] = true;
        c[199] = true;
        c[201] = true;
        c[203] = true;
        c[205] = true;
        c[206] = true;
        c[211] = true;
        c[212] = true;
        c[214] = true;
        c[215] = true;
        c[218] = true;
        c[220] = true;
        c[221] = true;
        c[223] = true;
        c[225] = true;
        c[226] = true;
        c[228] = true;
        c[231] = true;
        c[233] = true;
        c[235] = true;
        c[237] = true;
        c[238] = true;
        c[243] = true;
        c[244] = true;
        c[246] = true;
        c[247] = true;
        c[250] = true;
        c[252] = true;
        c[253] = true;
        c[258] = true;
        c[259] = true;
        c[260] = true;
        c[261] = true;
        c[262] = true;
        c[263] = true;
        c[268] = true;
        c[269] = true;
        c[270] = true;
        c[271] = true;
        c[272] = true;
        c[273] = true;
        c[280] = true;
        c[281] = true;
        c[282] = true;
        c[283] = true;
        c[313] = true;
        c[314] = true;
        c[317] = true;
        c[318] = true;
        c[321] = true;
        c[322] = true;
        c[323] = true;
        c[324] = true;
        c[327] = true;
        c[328] = true;
        c[336] = true;
        c[337] = true;
        c[340] = true;
        c[341] = true;
        c[344] = true;
        c[345] = true;
        c[346] = true;
        c[347] = true;
        c[350] = true;
        c[351] = true;
        c[352] = true;
        c[353] = true;
        c[354] = true;
        c[355] = true;
        c[356] = true;
        c[357] = true;
        c[366] = true;
        c[367] = true;
        c[368] = true;
        c[369] = true;
        c[377] = true;
        c[378] = true;
        c[379] = true;
        c[380] = true;
        c[381] = true;
        c[382] = true;
        c[711] = true;
        c[728] = true;
        c[729] = true;
        c[731] = true;
        c[733] = true;
        //c[8211] = true;
        //c[8212] = true;
        //c[8216] = true;
        //c[8217] = true;
        //c[8218] = true;
        //c[8220] = true;
        //c[8221] = true;
        //c[8222] = true;
        //c[8224] = true;
        //c[8225] = true;
        //c[8226] = true;
        //c[8230] = true;
        //c[8240] = true;
        //c[8249] = true;
        //c[8250] = true;
        //c[8364] = true;
        //c[8482] = true;
    }

    public final boolean inCharset(int ch) {
        return (ch < 740 && c[ch]) ||
		        ch==8211 ||
		        ch==8212 ||
		        ch==8216 ||
		        ch==8217 ||
		        ch==8218 ||
		        ch==8220 ||
		        ch==8221 ||
		        ch==8222 ||
		        ch==8224 ||
		        ch==8225 ||
		        ch==8226 ||
		        ch==8230 ||
		        ch==8240 ||
		        ch==8249 ||
		        ch==8250 ||
		        ch==8364 ||
		        ch==8482;
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
// The Initial Developer of the Original Code is Michael H. Kay using data supplied by Jirka Kosek [jirka@kosek.cz] and Unicode.org
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
