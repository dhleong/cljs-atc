(ns atc.data.airports.kjfk)

(def airport
 {:magnetic-north -13.0,
  :departures
  {"DEEZZ5" {:path [{:fix "DEEZZ"} {:fix "HEERO"}]},
   "JFK" {:path [{:fix "JFK"}]},
   "SKORR5" {:path [{:fix "SKORR"}]}},
  :name "JOHN F KENNEDY INTL",
  :departure-fix-codes
  {"ARD" "A",
   "COATE" "O",
   "MERIT" "M",
   "WAVEY" "W",
   "CANDR" "C",
   "GREKI" "K",
   "BETTE" "E",
   "GAYEL" "G",
   "HAAYS" "H",
   "RBV" "R",
   "WHITE" "I",
   "DIXIE" "D",
   "SHIPP" "S",
   "BDR" "Z",
   "BAYYS" "B",
   "NEION" "N"},
  :departure-routes
  {"KDAY" {:fix "RBV", :route "KJFK RBV Q430 AIR APE DANEI3 KDAY"},
   "KDEN"
   {:fix "RBV",
    :route "KJFK RBV Q430 AIR J110 STL HYS J24 OATHE CLASH4 KDEN"},
   "KSLC"
   {:fix "GREKI",
    :route
    "KJFK GREKI JUDDS CAM NOVON BURWA OVORA VBI HML OGSIQ SWTHN JAC NORDK6 KSLC"},
   "KAUS"
   {:fix "WAVEY",
    :route
    "KJFK WAVEY EMJAY Q167 ZJAAY Q97 SAWED GUILD Q409 MRPIT CEELY Q172 YUTEE IRQ NOKIE MGMRY LCH LUKKN WLEEE7 KAUS"},
   "KBTV" {:fix "BDR", :route "KJFK BDR V487 BTV KBTV"},
   "KTYS"
   {:fix "RBV",
    :route "KJFK RBV Q430 BYRDD J48 CSN FANPO Q40 ALEAN VXV KTYS"},
   "KSAT"
   {:fix "RBV",
    :route
    "KJFK RBV Q430 BYRDD J48 CSN FANPO Q40 AEX IAH BRAUN3 KSAT"},
   "KATL"
   {:fix "CANDR",
    :route "KJFK DEEZZ5 CANDR J60 PSB HVQ HLRRY ONDRE1 KATL"},
   "KTOL" {:fix "CANDR", :route "KJFK DEEZZ5 CANDR J60 DJB KTOL"},
   "KSRQ"
   {:fix "RBV",
    :route
    "KJFK RBV Q430 COPES Q75 SLOJO Q103 PUPYY KYYUU LUBBR3 KSRQ"},
   "KSEA"
   {:fix "GREKI",
    :route
    "KJFK GREKI JUDDS CAM NOVON BURWA OVARA VBI HML LWT MLP GLASR2 KSEA"},
   "KMEM"
   {:fix "RBV",
    :route "KJFK RBV Q430 SAAME J6 HVQ Q68 RYANS BWG BLUZZ3 KMEM"},
   "KELM" {:fix "GAYEL", :route "KJFK GAYEL V374 CFB V270 ULW KELM"},
   "KMDW"
   {:fix "CANDR",
    :route "KJFK DEEZZ5 CANDR J60 DJB BAGEL PANGG5 KMDW"},
   "KSTL"
   {:fix "RBV", :route "KJFK RBV Q430 AIR J110 VHP AARCH2 KSTL"},
   "KDAB"
   {:fix "WAVEY",
    :route
    "KJFK WAVEY EMJAY Q167 ZJAAY Q97 SAWED MOXXY Q85 LPERD TTHOR3 KDAB"},
   "KBWI"
   {:fix "WAVEY",
    :route "KJFK WAVEY PLUME T320 WNSTN ATR LAFLN MIIDY2 KBWI"},
   "KCHA"
   {:fix "RBV",
    :route "KJFK RBV Q430 BYRDD J48 CSN FANPO Q40 JAARE GQO KCHA"},
   "KCHS"
   {:fix "WHITE",
    :route "KJFK WHITE Q409 CRPLR DFENC Q109 LAANA AMYLU AMYLU3 KCHS"},
   "KMYR"
   {:fix "WAVEY",
    :route "KJFK WAVEY EMJAY Q167 ZJAAY Q97 PAACK WYLMS KMYR"},
   "KCHO"
   {:fix "WAVEY",
    :route "KJFK WAVEY EMJAY Q167 ZJAAY ARICE JAMIE RIC KCHO"},
   "KHOU"
   {:fix "WAVEY",
    :route
    "KJFK WAVEY EMJAY Q167 ZJAAY Q97 SAWED GUILD Q409 MRPIT CEELY Q172 YUTEE IRQ THRSR SARKK AEX WAPPL6 KHOU"},
   "KOAK"
   {:fix "RBV",
    :route
    "KJFK RBV Q430 AIR J110 GCK J28 MLF ILC TATOO MONOH OAKES2 KOAK"},
   "KBUF" {:fix "COATE", :route "KJFK COATE LAAYK ULW BENEE KBUF"},
   "KONT"
   {:fix "WAVEY",
    :route
    "KJFK WAVEY EMJAY Q167 ZJAAY Q97 SAWED GUILD Q409 MRPIT CEELY Q172 YUTEE IRQ THRSR VLKNN Q30 IZAAC SUTTN J52 TXK SPS J72 ABQ J6 DRK DAFNY SCBBY2 KONT"},
   "KMHT"
   {:fix "MERIT", :route "KJFK MERIT HFD T315 GDM T314 MANCH KMHT"},
   "KLGB"
   {:fix "RBV",
    :route
    "KJFK RBV Q430 AIR J110 STL BUM ICT LBL CIM J134 DRK HIMDU DSNEE5 KLGB"},
   "CYMX"
   {:fix "GREKI",
    :route "KJFK GREKI JUDDS CAM PBERG LATTS EBDOT DUNUP CYMX"},
   "KMSY"
   {:fix "RBV",
    :route
    "KJFK RBV Q430 BYRDD J48 CSN FANPO Q40 NIOLA MEI RYTHM4 KMSY"},
   "KBHM"
   {:fix "RBV",
    :route "KJFK RBV Q430 BYRDD J48 CSN FANPO Q40 NIOLA DIODE KBHM"},
   "KFLL"
   {:fix "WAVEY",
    :route
    "KJFK WAVEY EMJAY Q167 ZJAAY KALDA Q133 CHIEZ Y291 MAJIK CUUDA2 KFLL"},
   "KPVD"
   {:fix "BAYYS", :route "KJFK BAYYS SEALL V188 GON V374 MINNK KPVD"},
   "KRSW"
   {:fix "RBV",
    :route "KJFK RBV Q430 COPES Q75 SLOJO Q103 CYNTA SHFTY5 KRSW"},
   "KCAE" {:fix "RBV", :route "KJFK RBV Q430 COPES Q75 SLOJO KCAE"},
   "KLAX"
   {:fix "GREKI",
    :route
    "KJFK GREKI JUDDS CAM NOVON BURWA OVORA VBI HML OGSIQ SWTHN DNW FFU J9 MLF WINEN Q73 HAKMN ANJLL4 KLAX"},
   "KGRR"
   {:fix "COATE", :route "KJFK COATE Q436 RAAKK Q438 RUBYY KGRR"},
   "CYOW"
   {:fix "HAAYS",
    :route "KJFK HAAYS V273 HNK LAMMS WAYGO ART DEANS1 CYOW"},
   "KBGR" {:fix "BDR", :route "KJFK BDR V487 CAM CON AUG KBGR"},
   "KEWB"
   {:fix "BAYYS", :route "KJFK BAYYS SEALL V188 GON V374 MINNK KEWB"},
   "KORD"
   {:fix "GREKI",
    :route
    "KJFK GREKI JUDDS CAM NOVON SIKBO NOSIK ZOHAN GRB SHIKY FYTTE7 KORD"},
   "KBCT"
   {:fix "SHIPP",
    :route "KJFK SHIPP Y488 STERN Y493 BAHAA DULEE CLMNT2 KBCT"},
   "KHYA"
   {:fix "BAYYS", :route "KJFK BAYYS SEALL V188 GON V374 MVY KHYA"},
   "CYVR"
   {:fix "GREKI",
    :route "KJFK GREKI JUDDS CAM Q822 HOZIR SAW HML YDC CYVR"},
   "KPHX"
   {:fix "RBV",
    :route
    "KJFK RBV Q430 AIR J110 STL BUM ICT LBL FTI J244 ZUN EAGUL6 KPHX"},
   "KPDX"
   {:fix "GREKI",
    :route
    "KJFK GREKI JUDDS CAM NOVON BURWA OVORA VBI HML LWT PDT JKNOX HHOOD4 KPDX"},
   "KSDF"
   {:fix "RBV",
    :route "KJFK RBV Q430 SAAME J6 HVQ Q68 UNCKL MAUDD6 KSDF"},
   "KEGE"
   {:fix "CANDR",
    :route "KJFK DEEZZ5 CANDR J60 HCT AKO AVVVS HUGGS RLG KEGE"},
   "KCVG"
   {:fix "COATE", :route "KJFK COATE LAAYK PSB CTW TIGRR3 KCVG"},
   "KDFW"
   {:fix "CANDR",
    :route
    "KJFK DEEZZ5 CANDR J60 JOT MZV IRK MCI ICT IRW HOFFF JOVEM5 KDFW"},
   "KPWK"
   {:fix "COATE",
    :route
    "KJFK COATE Q436 YARRK TRESL WODDS FRSST PMM FIYER OBK KPWK"},
   "KPIT" {:fix "RBV", :route "KJFK RBV T438 RAV PSB HAYNZ7 KPIT"},
   "KRIC" {:fix "WHITE", :route "KJFK WHITE Q409 TRPOD JAMIE KRIC"},
   "KBED" {:fix "MERIT", :route "KJFK MERIT HFD DREEM2 KBED"},
   "KDET"
   {:fix "GAYEL", :route "KJFK GAYEL Q818 WOZEE COLTS GIGGY2 KDET"},
   "KPIA" {:fix "RBV", :route "KJFK RBV Q430 AIR J110 VHP KPIA"},
   "KBGM" {:fix "HAAYS", :route "KJFK HAAYS HUO V252 CFB KBGM"},
   "KPTK"
   {:fix "GAYEL", :route "KJFK GAYEL Q818 WOZEE COLTS OKLND1 KPTK"},
   "KCMH"
   {:fix "COATE", :route "KJFK COATE LAAYK PSB AIR CLPRR2 KCMH"},
   "KBNA"
   {:fix "RBV",
    :route "KJFK RBV Q430 SAAME J6 HVQ Q68 YOCKY GROAT PASLY4 KBNA"},
   "KDCA"
   {:fix "DIXIE", :route "KJFK DIXIE V1 LEEAH CHOPS BILIT CAPKO KDCA"},
   "KOMA"
   {:fix "RBV",
    :route "KJFK RBV Q430 AIR J80 SPI IOW DSM LANTK2 KOMA"},
   "KSFO"
   {:fix "GREKI",
    :route
    "KJFK GREKI JUDDS CAM NOVON BURWA OVORA VBI HML GGW IDA BAM J32 LLC LEGGS BDEGA3 KSFO"},
   "KYIP"
   {:fix "GAYEL", :route "KJFK GAYEL Q818 WOZEE COLTS OKLND1 KYIP"},
   "KSAV" {:fix "WHITE", :route "KJFK WHITE Q409 SESUE SOOOP KSAV"},
   "KORF"
   {:fix "WHITE", :route "KJFK WHITE Q409 TRPOD JAMIE CCV KORF"},
   "KMIA"
   {:fix "WAVEY",
    :route
    "KJFK WAVEY EMJAY Q167 ZJAAY KALDA Q101 SKARP Y313 HOAGG BNFSH2 KMIA"},
   "KTPA"
   {:fix "RBV",
    :route "KJFK RBV Q430 COPES Q75 TEUFL BAAMF DADES1 KTPA"},
   "KJAX"
   {:fix "WHITE", :route "KJFK WHITE Q409 SESUE ESENT LUNNI1 KJAX"},
   "KMCI"
   {:fix "RBV", :route "KJFK RBV Q430 AIR J80 SPI EUING RUDDH3 KMCI"},
   "KCAK"
   {:fix "CANDR",
    :route "KJFK DEEZZ5 CANDR J60 PSB SOORD ZZIPS1 KCAK"},
   "KFXE"
   {:fix "SHIPP",
    :route "KJFK SHIPP Y488 STERN Y493 JENKS MAJIK WAMBI CPTAN2 KFXE"},
   "KCLT"
   {:fix "CANDR",
    :route "KJFK DEEZZ5 CANDR J60 PSB HVQ LNDIZ PARQR3 KCLT"},
   "KLAS"
   {:fix "GREKI",
    :route
    "KJFK GREKI JUDDS CAM NOVON BURWA OVORA VBI HML OGSIQ SWTHN DNW FFU J9 MLF STEWW CHOWW2 KLAS"},
   "KRDU"
   {:fix "WHITE",
    :route "KJFK WHITE Q409 VILLS NALES Q141 HOUKY TAQLE1 KRDU"},
   "KLWB"
   {:fix "RBV", :route "KJFK RBV Q430 BYRDD J48 MOL V140 COVEY KLWB"},
   "KROA" {:fix "RBV", :route "KJFK RBV Q430 BYRDD J48 MOL KROA"},
   "KPBI"
   {:fix "WAVEY",
    :route
    "KJFK WAVEY EMJAY Q167 ZJAAY Q131 ZILLS Y289 DULEE CLMNT2 KPBI"},
   "KFWA"
   {:fix "CANDR",
    :route "KJFK DEEZZ5 CANDR J60 DANNR RAV J64 FWA KFWA"},
   "KGSO"
   {:fix "WAVEY",
    :route "KJFK WAVEY EMJAY Q167 ZJAAY CRPLR COUPN RDU KGSO"},
   "KPWM" {:fix "BDR", :route "KJFK BDR V487 CAM CDOGG4 KPWM"},
   "KDSM"
   {:fix "CANDR", :route "KJFK DEEZZ5 CANDR J60 IOW J10 DSM KDSM"},
   "KMCO"
   {:fix "WHITE",
    :route
    "KJFK WHITE Q409 CRPLR EARZZ Q131 ZILLS Y289 BAHAA HIBAC ALYNA3 KMCO"},
   "KBOS"
   {:fix "SHIPP",
    :route "KJFK SHIPP Y487 ISLES ACK FERNZ OOSHN5 KBOS"},
   "KIND"
   {:fix "COATE",
    :route
    "KJFK COATE LAAYK ULW JHW ERI JFN DJB KLYNE RINTE SNKPT2 KIND"},
   "CYUL"
   {:fix "GREKI",
    :route "KJFK GREKI JUDDS CAM JASDU PBERG CARTR5 CYUL"},
   "KITH" {:fix "GAYEL", :route "KJFK GAYEL V374 CFB KITH"},
   "KALB" {:fix "HAAYS", :route "KJFK HAAYS V273 HNK KALB"},
   "CYYZ"
   {:fix "COATE", :route "KJFK COATE LAAYK ULW WOZEE LINNG3 CYYZ"},
   "KIAD"
   {:fix "DIXIE",
    :route "KJFK DIXIE V1 LEEAH T315 TAPPA THHMP CAVLR4 KIAD"},
   "KROC"
   {:fix "COATE", :route "KJFK COATE LAAYK CFB V252 GIBBE KROC"},
   "KAGC" {:fix "COATE", :route "KJFK COATE Q436 REBBL SLT REC KAGC"},
   "KDTW"
   {:fix "COATE", :route "KJFK COATE LAAYK ULW BUF DONEO TPGUN2 KDTW"},
   "KMVY"
   {:fix "BAYYS",
    :route "KJFK BAYYS T315 SEALL V188 GON V374 MVY KMVY"},
   "KSJC"
   {:fix "COATE",
    :route
    "KJFK COATE Q436 RAAKK Q440 HUFFR DKOTA DDY OCS DTA KNGRY RAZRR5 KSJC"},
   "KCLE"
   {:fix "RBV", :route "KJFK RBV T438 RAV PSB UPPRR TRYBE4 KCLE"},
   "KGSP"
   {:fix "RBV",
    :route "KJFK RBV Q430 COPES Q75 GVE FUBLL JUNNR3 KGSP"},
   "KMSP"
   {:fix "GAYEL",
    :route "KJFK GAYEL Q818 WOZEE NOSIK Q812 ZOHAN CEWDA MUSCL3 KMSP"},
   "KSAN"
   {:fix "GREKI",
    :route
    "KJFK GREKI JUDDS CAM NOVON BURWA OVORA VBI HML OGSIQ SWTHN DNW FFU J9 MLF WINEN Q73 LVELL LUCKI1 KSAN"},
   "KIAH"
   {:fix "RBV",
    :route
    "KJFK RBV Q430 BYRDD J48 CSN FANPO Q40 NIOLA MEI SWB ZEEKK2 KIAH"},
   "KMKE"
   {:fix "CANDR",
    :route
    "KJFK DEEZZ5 CANDR J60 DJB CRL PEGEE GETCH LYSTR SUDDS KMKE"},
   "KACK"
   {:fix "BAYYS", :route "KJFK BAYYS SEALL V188 GON DEEPO1 KACK"},
   "KSYR" {:fix "COATE", :route "KJFK COATE LAAYK CFB V29 SYR KSYR"}},
  :runways
  [{:start-id "13R",
    :start-threshold [:N40*38'54.1008 :W073*49'00.1730],
    :end-id "31L",
    :end-threshold [:N40*37'40.7799 :W073*46'18.4107]}
   {:start-id "13L",
    :start-threshold [:N40*39'27.9520 :W073*47'24.8606],
    :end-id "31R",
    :end-threshold [:N40*38'37.4085 :W073*45'33.3818]}
   {:start-id "04R",
    :start-threshold [:N40*37'31.5418 :W073*46'13.2441],
    :end-id "22L",
    :end-threshold [:N40*38'42.8531 :W073*45'17.5027]}
   {:start-id "04L",
    :start-threshold [:N40*37'19.2754 :W073*47'08.1029],
    :end-id "22R",
    :end-threshold [:N40*39'01.8338 :W073*45'47.9596]}],
  :navaids
  [{:id "ACOVE",
    :position [:N42*14'05.220 :W074*01'54.590],
    :type :fix,
    :pronunciation "a cove"}
   {:id "AGNEZ",
    :position [:N42*13'32.710 :W074*11'18.750],
    :type :fix,
    :pronunciation "agnes"}
   {:id "ALB",
    :position [:N42*44'50.210 :W073*48'11.462],
    :type :vortac,
    :name "albany"}
   {:id "ARD",
    :position [:N40*15'12.033 :W074*54'27.405],
    :type :vor/dme,
    :name "yardley"}
   {:id "ASPEN",
    :position [:N42*48'57.550 :W070*54'41.390],
    :type :fix}
   {:id "ATHOS",
    :position [:N42*14'49.490 :W073*48'43.560],
    :type :fix}
   {:id "BAYYS",
    :position [:N41*17'21.270 :W072*58'16.730],
    :type :fix,
    :pronunciation "bays"}
   {:id "BDR",
    :position [:N41*09'38.540 :W073*07'28.151],
    :type :vor/dme,
    :name "bridgeport"}
   {:id "BELTT",
    :position [:N41*03'48.610 :W072*59'13.520],
    :type :fix,
    :pronunciation "belt"}
   {:id "BETTE",
    :position [:N40*33'33.260 :W073*00'42.210],
    :type :fix}
   {:id "BOTON",
    :position [:N39*24'52.100 :W074*27'17.080],
    :type :fix,
    :pronunciation "bo ton"}
   {:id "CAMRN",
    :position [:N40*01'02.290 :W073*51'39.810],
    :type :fix,
    :pronunciation "cam rn"}
   {:id "CANDR",
    :position [:N40*58'15.550 :W074*57'35.380],
    :type :fix,
    :pronunciation "candor"}
   {:id "CAPIT",
    :position [:N40*45'37.990 :W073*37'49.460],
    :type :fix,
    :pronunciation "ca pit"}
   {:id "CCC",
    :position [:N40*55'46.628 :W072*47'55.890],
    :type :vor/dme,
    :name "calverton"}
   {:id "COATE",
    :position [:N41*08'10.420 :W074*41'42.600],
    :type :fix,
    :pronunciation "coat"}
   {:id "CODDI",
    :position [:N42*22'52.150 :W075*00'21.840],
    :type :fix,
    :pronunciation "codi"}
   {:id "DEEDE",
    :position [:N41*38'47.810 :W073*32'25.740],
    :type :fix,
    :pronunciation "deedee"}
   {:id "DEEZZ",
    :position [:N41*06'52.000 :W073*46'40.000],
    :type :fix,
    :pronunciation "de ezz"}
   {:id "DENNA",
    :position [:N41*13'59.790 :W073*11'37.940],
    :type :fix,
    :pronunciation "dena"}
   {:id "DIXIE",
    :position [:N40*05'57.720 :W074*09'52.170],
    :type :fix}
   {:id "DNY",
    :position [:N42*10'41.810 :W074*57'24.986],
    :type :vor/dme,
    :name "delancey"}
   {:id "DOORE",
    :position [:N41*01'41.440 :W074*22'03.730],
    :type :fix,
    :pronunciation "door"}
   {:id "DPK",
    :position [:N40*47'30.299 :W073*18'13.168],
    :type :vor/dme,
    :name "deer park"}
   {:id "ENE",
    :position [:N43*25'32.420 :W070*36'48.692],
    :type :vor/dme,
    :name "kennebunk",
    :pronunciation "kenney bunk"}
   {:id "FZOOL",
    :position [:N41*19'35.110 :W073*16'59.580],
    :type :fix,
    :pronunciation "fah zoo el"}
   {:id "GAMBY",
    :position [:N40*00'22.220 :W073*52'08.270],
    :type :fix,
    :pronunciation "gam bee"}
   {:id "GAYEL",
    :position [:N41*24'24.090 :W074*21'25.720],
    :type :fix,
    :pronunciation "gayle"}
   {:id "GREKI",
    :position [:N41*28'48.030 :W073*18'50.980],
    :type :fix,
    :pronunciation "gre ki"}
   {:id "GROUP",
    :position [:N42*33'50.100 :W073*48'23.330],
    :type :fix}
   {:id "HAAYS",
    :position [:N41*19'12.020 :W074*28'01.850],
    :type :fix,
    :pronunciation "hays"}
   {:id "HARTY",
    :position [:N41*04'16.280 :W075*05'23.620],
    :type :fix}
   {:id "HOGGS",
    :position [:N39*34'58.250 :W074*16'14.070],
    :type :fix,
    :pronunciation "hogs"}
   {:id "IGN",
    :position [:N41*39'55.626 :W073*49'20.061],
    :type :vor/dme,
    :name "kingston"}
   {:id "JENNO",
    :position [:N41*09'10.530 :W075*19'53.070],
    :type :fix,
    :pronunciation "jeno"}
   {:id "JFK",
    :position [:N40*37'58.382 :W073*46'17.010],
    :type :vor/dme,
    :name "kennedy"}
   {:id "KARRS",
    :position [:N39*50'27.140 :W073*59'09.560],
    :type :fix,
    :pronunciation "kars"}
   {:id "LENDY",
    :position [:N40*54'53.410 :W074*08'06.930],
    :type :fix,
    :pronunciation "len dee"}
   {:id "LGA",
    :position [:N40*47'01.376 :W073*52'06.962],
    :type :vor/dme,
    :name "la guardia"}
   {:id "LOLLY",
    :position [:N41*23'07.810 :W074*03'48.540],
    :type :fix}
   {:id "LOVES",
    :position [:N41*32'19.640 :W073*29'17.140],
    :type :fix}
   {:id "LVZ",
    :position [:N41*16'22.076 :W075*41'22.078],
    :type :vortac,
    :name "wilkes barre"}
   {:id "MERIT",
    :position [:N41*22'55.020 :W073*08'14.750],
    :type :fix}
   {:id "NEION",
    :position [:N41*13'41.210 :W074*34'50.780],
    :type :fix,
    :pronunciation "ne ion"}
   {:id "NESSI",
    :position [:N41*06'01.600 :W073*02'21.280],
    :type :fix,
    :pronunciation "ne ssi"}
   {:id "PANZE",
    :position [:N39*40'33.580 :W074*10'05.450],
    :type :fix,
    :pronunciation "pan ze"}
   {:id "PARCH",
    :position [:N41*05'57.220 :W072*07'14.660],
    :type :fix}
   {:id "PETER",
    :position [:N42*12'18.930 :W074*31'50.250],
    :type :fix}
   {:id "PVD",
    :position [:N41*43'27.633 :W071*25'46.708],
    :type :vor/dme,
    :name "providence"}
   {:id "PWL",
    :position [:N41*46'11.177 :W073*36'01.985],
    :type :vor/dme,
    :name "pawling"}
   {:id "RBV",
    :position [:N40*12'08.648 :W074*29'42.094],
    :type :vortac,
    :name "robbinsville",
    :pronunciation "robbins fill"}
   {:id "RKA",
    :position [:N42*27'58.790 :W075*14'21.221],
    :type :vor/dme,
    :name "rockdale"}
   {:id "RNGRR",
    :position [:N40*13'48.590 :W074*12'20.690],
    :type :fix,
    :pronunciation "ranger"}
   {:id "ROBER",
    :position [:N40*41'07.670 :W073*01'57.400],
    :type :fix}
   {:id "SEY",
    :position [:N41*10'02.770 :W071*34'33.910],
    :type :vor/dme,
    :name "sandy point"}
   {:id "SHIPP",
    :position [:N40*19'45.960 :W073*14'50.180],
    :type :fix}
   {:id "SIE",
    :position [:N39*05'43.832 :W074*48'01.238],
    :type :vortac,
    :name "sea isle"}
   {:id "STW",
    :position [:N40*59'44.951 :W074*52'08.509],
    :type :vor/dme,
    :name "stillwater"}
   {:id "TOWIN",
    :position [:N40*32'06.760 :W075*24'01.360],
    :type :fix,
    :pronunciation "to win"}
   {:id "TRAIT",
    :position [:N41*17'04.750 :W071*55'03.350],
    :type :fix}
   {:id "WAVEY",
    :position [:N40*14'04.500 :W073*23'39.760],
    :type :fix,
    :pronunciation "wave ee"}
   {:id "WHITE",
    :position [:N40*00'24.320 :W074*15'04.610],
    :type :fix}],
  :id "KJFK",
  :center-facilities
  [{:id "COLTS NECK",
    :position [:N40*18'42.000 :W074*09'37.000],
    :frequency "118.975"}
   {:id "SPARTA",
    :position [:N41*04'03.000 :W074*32'19.000],
    :frequency "133.15"}
   {:id "CALVERTON",
    :position [:N40*55'47.350 :W072*47'55.360],
    :frequency "124.525"}
   {:id "SHELTON",
    :position [:N41*19'37.340 :W073*06'55.390],
    :frequency "126.275"}],
  :position [:N40*38'23.7410 :W073*46'43.2920 13.0]})
