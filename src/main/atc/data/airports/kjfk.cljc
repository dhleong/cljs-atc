(ns atc.data.airports.kjfk)

(def airport
 {:id "KJFK",
  :name "JOHN F KENNEDY INTL",
  :magnetic-north -13.0,
  :position [:N40*38'23.7400 :W073*46'43.2930 13.0],
  :runways
  [{:start-id "13R",
    :start-threshold [:N40*38'54.1020 :W073*49'00.1730],
    :end-id "31L",
    :end-threshold [:N40*37'40.7810 :W073*46'18.4130]}
   {:start-id "13L",
    :start-threshold [:N40*39'27.9533 :W073*47'24.8600],
    :end-id "31R",
    :end-threshold [:N40*38'37.4079 :W073*45'33.3832]}
   {:start-id "04R",
    :start-threshold [:N40*37'31.5320 :W073*46'13.2500],
    :end-id "22L",
    :end-threshold [:N40*38'42.8490 :W073*45'17.5090]}
   {:start-id "04L",
    :start-threshold [:N40*37'19.2759 :W073*47'08.1038],
    :end-id "22R",
    :end-threshold [:N40*39'01.8337 :W073*45'47.9596]}],
  :navaids
  [{:id "ACOVE",
    :position [:N42*14'05.220 :W074*01'54.590],
    :type :fix,
    :pronunciation "ac ove"}
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
    :pronunciation "cameron"}
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
    :name "kennebunk"}
   {:id "FZOOL",
    :position [:N41*19'35.110 :W073*16'59.580],
    :type :fix}
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
    :position [:N41*39'55.619 :W073*49'20.008],
    :type :vor/dme,
    :name "kingston"}
   {:id "JENNO",
    :position [:N41*09'10.530 :W075*19'53.070],
    :type :fix,
    :pronunciation "jeno"}
   {:id "JFK",
    :position [:N40*37'58.400 :W073*46'17.000],
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
    :type :fix}
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
    :pronunciation "wa vey"}
   {:id "WHITE",
    :position [:N40*00'24.320 :W074*15'04.610],
    :type :fix}]})
