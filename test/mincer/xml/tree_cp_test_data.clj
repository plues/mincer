(ns mincer.xml.tree-cp-test-data)

(def m-cp-tag {:attrs {:name "MV01: Makroökonomik" :pordnr "40553" :cp "6"} :content nil :tag :m})
(def m-cp-tag-2 {:attrs {:name "MV03: Mikroökonomik" :pordnr "40554" :cp "7" :pflicht "j"} :content nil :tag :m})

; (def m-cp-tag {:attrs  {:name "Logik I" :pflicht "j" :pordnr "29381"} :content nil :tag :m})
; (def m-tag-2 {:attrs {:name "Grundlagen" :pordnr "29380"} :content nil :tag :m})
; (def nested-l-tag {:attrs {:max "6" :min "4" :name "Basiswahlpflichtmodule"}
;                    :content [{:attrs {:max "2" :min "1" :name "Theoretische Philosophie"}
;                               :content [m-tag]
;                               :tag :l}
;                              {:attrs {:max "2" :min "1" :TM "TM" :name "Praktische Philosophie"}
;                               :content [m-tag m-tag-2]
;                               :tag :l}
;                              {:attrs {:max "4" :min "2" :ART "ART" :name "Geschichte der Philosophie"}
;                               :content [m-tag]
;                               :tag :l}]
;                    :tag :l})

; (def b-tag {:attrs {:abschl "bk"
;                     :kzfa "H"
;                     :name "Kernfach Philosophie"
;                     :pversion "2011"
;                     :stg "phi"}
;             :content [nested-l-tag]
;             :tag :b})

; (def b-tag-with-regeln {:attrs {:abschl "bk"
;                                 :kzfa "H"
;                                 :name "Kernfach Philosophie"
;                                 :pversion "2011"
;                                 :stg "phi"}
;                         :content [{:tag :regeln} {:tag :i} nested-l-tag]
;                         :tag :b})
; (def modulbaum-tag {:attrs nil :content  [b-tag] :tag :ModulBaum})
