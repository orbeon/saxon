package net.sf.saxon.charcode;

/**
This package defines pluggable character set CP852
*/

public class CP852CharacterSet implements PluggableCharacterSet {

    public static CP852CharacterSet theInstance = null;

    private CP852CharacterSet() {}

    public static CP852CharacterSet getInstance() {
        if (theInstance == null) {
            init();
            theInstance = new CP852CharacterSet();
        }
        return theInstance;
    }

    private static boolean c[] = null;

    private static void init () {
        c = new boolean[400];
        for (int i=0; i<127; i++) {
            c[i] = true;
        }
        for (int i=127; i<400; i++) {
            c[i] = false;
    }

    c[167] = true;
    c[171] = true;
    c[172] = true;
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
}

    public final boolean inCharset(int ch) {
        return ch < 400 && c[ch];
    }

    public final String getEncodingName() {
        return "cp852";
    }
}

/*
(C) Z. Wagner -- Ice Bear Soft, 11 Oct 2001
http://icebearsoft.euweb.cz

This package is free software. Its use and distribution should follow
the Library General Public Licence (see http://www.gnu.org). Since this licence
is void in the Czech Republic, the users may opt to use a modified version
available from http://www.zastudena.cz

The character mapping was obtained by conversion a character table of all non-US characters from
CP852 into UNICODE entities using a simple stylesheet and saxon with the following attribute in
xsl:output

saxon:character-representation="dec;dec"

The class was tested by reverse conversion of the generated table to native representation as well
as by transformation of several texts which use Czech and Slovak accented characters.
*/
