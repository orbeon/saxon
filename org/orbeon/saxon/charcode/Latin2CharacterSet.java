package net.sf.saxon.charcode;

/**
* This class defines properties of the ISO-8859-2 character set
*/

public class Latin2CharacterSet implements CharacterSet {

    private static Latin2CharacterSet theInstance = null;

    private Latin2CharacterSet() {}

    public static Latin2CharacterSet getInstance() {
        if (theInstance == null) {
            init();
            theInstance = new Latin2CharacterSet();
        }
        return theInstance;
    }


    private static boolean[] c = null;

    private static void init() {

        c = new boolean[750];

        for (int i=0; i<127; i++) {
            c[i] = true;
        }
        for (int i=128; i<750; i++) {
            c[i] = false;
        }

        c[	160	] = true;
        c[	164	] = true;
        c[	167	] = true;
        c[	168	] = true;
        c[	173	] = true;
        c[	176	] = true;
        c[	180	] = true;
        c[	184	] = true;
        c[	193	] = true;
        c[	194	] = true;
        c[	196	] = true;
        c[	199	] = true;
        c[	201	] = true;
        c[	203	] = true;
        c[	205	] = true;
        c[	206	] = true;
        c[	211	] = true;
        c[	212	] = true;
        c[	214	] = true;
        c[	215	] = true;
        c[	218	] = true;
        c[	220	] = true;
        c[	221	] = true;
        c[	223	] = true;
        c[	225	] = true;
        c[	226	] = true;
        c[	228	] = true;
        c[	231	] = true;
        c[	233	] = true;
        c[	235	] = true;
        c[	237	] = true;
        c[	238	] = true;
        c[	243	] = true;
        c[	244	] = true;
        c[	246	] = true;
        c[	247	] = true;
        c[	250	] = true;
        c[	252	] = true;
        c[	253	] = true;
        c[	258	] = true;
        c[	259	] = true;
        c[	260	] = true;
        c[	261	] = true;
        c[	262	] = true;
        c[	263	] = true;
        c[	268	] = true;
        c[	269	] = true;
        c[	270	] = true;
        c[	271	] = true;
        c[	272	] = true;
        c[	273	] = true;
        c[	280	] = true;
        c[	281	] = true;
        c[	282	] = true;
        c[	283	] = true;
        c[	313	] = true;
        c[	314	] = true;
        c[	317	] = true;
        c[	318	] = true;
        c[	321	] = true;
        c[	322	] = true;
        c[	323	] = true;
        c[	324	] = true;
        c[	327	] = true;
        c[	328	] = true;
        c[	336	] = true;
        c[	337	] = true;
        c[	340	] = true;
        c[	341	] = true;
        c[	344	] = true;
        c[	345	] = true;
        c[	346	] = true;
        c[	347	] = true;
        c[	350	] = true;
        c[	351	] = true;
        c[	352	] = true;
        c[	353	] = true;
        c[	354	] = true;
        c[	355	] = true;
        c[	356	] = true;
        c[	357	] = true;
        c[	366	] = true;
        c[	367	] = true;
        c[	368	] = true;
        c[	369	] = true;
        c[	377	] = true;
        c[	378	] = true;
        c[	379	] = true;
        c[	380	] = true;
        c[	381	] = true;
        c[	382	] = true;
        c[	711	] = true;
        c[	728	] = true;
        c[	729	] = true;
        c[	731	] = true;
        c[	733	] = true;

    }

    public final boolean inCharset(int ch) {
        return (ch < 750 && c[ch]);
    }

}

// Data from Han The Thanh [thanh@informatics.muni.cz]

//	Latin2	UTF8-1	UTF8-2	Name		Unicode
//	160	194	160	nobreakspace	160
//	164	194	164	currency	164
//	167	194	167	section		167
//	168	194	168	diaeresis	168
//	173	194	173	hyphen		173
//	176	194	176	degree		176
//	180	194	180	acute		180
//	184	194	184	cedilla		184
//	193	195	129	Aacute		193
//	194	195	130	Acircumflex	194
//	196	195	132	Adiaeresis	196
//	199	195	135	Ccedilla	199
//	201	195	137	Eacute		201
//	203	195	139	Ediaeresis	203
//	205	195	141	Iacute		205
//	206	195	142	Icircumflex	206
//	211	195	147	Oacute		211
//	212	195	148	Ocircumflex	212
//	214	195	150	Odiaeresis	214
//	215	195	151	multiply	215
//	218	195	154	Uacute		218
//	220	195	156	Udiaeresis	220
//	221	195	157	Yacute		221
//	223	195	159	ssharp		223
//	225	195	161	aacute		225
//	226	195	162	acircumflex	226
//	228	195	164	adiaeresis	228
//	231	195	167	ccedilla	231
//	233	195	169	eacute		233
//	235	195	171	ediaeresis	235
//	237	195	173	iacute		237
//	238	195	174	icircumflex	238
//	243	195	179	oacute		243
//	244	195	180	ocircumflex	244
//	246	195	182	odiaeresis	246
//	247	195	183	division	247
//	250	195	186	uacute		250
//	252	195	188	udiaeresis	252
//	253	195	189	yacute		253
//	195	196	130	Abreve		258
//	227	196	131	abreve		259
//	161	196	132	Aogonek		260
//	177	196	133	aogonek		261
//	198	196	134	Cacute		262
//	230	196	135	cacute		263
//	200	196	140	Ccaron		268
//	232	196	141	ccaron		269
//	207	196	142	Dcaron		270
//	239	196	143	dcaron		271
//	208	196	144	Eth		272
//	240	196	145	eth		273
//	202	196	152	Eogonek		280
//	234	196	153	eogonek		281
//	204	196	154	Ecaron		282
//	236	196	155	ecaron		283
//	197	196	185	Lacute		313
//	229	196	186	lacute		314
//	165	196	189	Lcaron		317
//	181	196	190	lcaron		318
//	163	197	129	Lstroke		321
//	179	197	130	lstroke		322
//	209	197	131	Nacute		323
//	241	197	132	nacute		324
//	210	197	135	Ncaron		327
//	242	197	136	ncaron		328
//	213	197	144	Odoubleacute	336
//	245	197	145	odoubleacute	337
//	192	197	148	Racute		340
//	224	197	149	racute		341
//	216	197	152	Rcaron		344
//	248	197	153	rcaron		345
//	166	197	154	Sacute		346
//	182	197	155	sacute		347
//	170	197	158	Scedilla	350
//	186	197	159	scedilla	351
//	169	197	160	Scaron		352
//	185	197	161	scaron		353
//	222	197	162	Tcedilla	354
//	254	197	163	tcedilla	355
//	171	197	164	Tcaron		356
//	187	197	165	tcaron		357
//	217	197	174	Uring		366
//	249	197	175	uring		367
//	219	197	176	Udoubleacute	368
//	251	197	177	udoubleacute	369
//	172	197	185	Zacute		377
//	188	197	186	zacute		378
//	175	197	187	Zabovedot	379
//	191	197	188	zabovedot	380
//	174	197	189	Zcaron		381
//	190	197	190	zcaron		382
//	183	203	135	caron		711
//	162	203	152	breve		728
//	255	203	153	abovedot	729
//	178	203	155	ogonek		731
//	189	203	157	doubleacute	733

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
// The Initial Developer of the Original Code is Michael H. Kay using data supplied by Han The Thanh [thanh@informatics.muni.cz]
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
