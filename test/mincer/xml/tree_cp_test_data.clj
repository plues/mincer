(ns mincer.xml.tree-cp-test-data)

(def m-cp-tag {:attrs {:name "MV01: Makroökonomik" :pordnr "40553" :cp "6"} :content nil :tag :m})
(def m-cp-tag-2 {:attrs {:name "MV03: Mikroökonomik" :pordnr "40554" :cp "7" :pflicht "j"} :content nil :tag :m})

(def nested-l-cp-tag {:attrs {:max-cp "60" :min-cp "40" :name "Wahlpflichtmodule"}
                   :content [{:attrs {:max-cp "20" :min-cp "10" :name "Area 1"}
                              :content [m-cp-tag]
                              :tag :l}
                             {:attrs {:max-cp "30" :min-cp "5" :name "Area 2"}
                              :content [m-cp-tag-2 m-cp-tag]
                              :tag :l}
                             {:attrs {:max-cp "100" :min-cp "10" :name "Area 3"}
                              :content [m-cp-tag]
                              :tag :l}]
                   :tag :l})
(def l-cp-tag-missing-data {:attrs {:name "Wahlpflichtmodule"}
                   :content []
                   :tag :l})

(def b-cp-tag {:attrs {:abschl "bk"
                    :kzfa "H"
                    :name "Kernfach Philosophie"
                    :pversion "2011"
                    :cp "91"
                    :stg "phi"}
            :content [nested-l-cp-tag]
            :tag :b})

(def b-cp-tag-with-regeln {:attrs {:abschl "bk"
                                :kzfa "H"
                                :name "Kernfach Philosophie"
                                :pversion "2011"
                                :cp "42"
                                :stg "phi"}
                        :content [{:tag :regeln} {:tag :i} nested-l-cp-tag]
                        :tag :b})
(def modulbaum-cp-tag {:attrs nil :content  [b-cp-tag] :tag :ModulBaum})
