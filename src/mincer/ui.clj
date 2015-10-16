(ns mincer.ui
  (:gen-class)
  (:require
    [seesaw.core :refer [text button config! frame invoke-later show! grid-panel]]
    [seesaw.chooser :refer [choose-file]]
    [clojure.java.io :refer [as-file copy]]
    [mincer.data :refer [persist]]
    [mincer.xml.modules :as modules]
    [mincer.xml.tree :as tree]))

(def files (atom {:meta nil :source nil}))

; here instead of slurp:
; do sth with the files and write it out
(defn my-writer [file]
  (let [data (modules/process (get @files :source))
        tree (tree/process (get @files :meta))
        db (persist tree (:modules data) (:units data))]
    (copy (as-file db) (as-file file)))
  (println "Finished!"))

(defn my-text [t]
  (text :text t
        :editable? false))

(def save-button
  (button
    :id :save
    :text ::create
    :enabled? false
    :listen [:action (fn [e]
      (my-writer
        (choose-file
          :type :save
          :filters [[".sql" ["sql"]]])))]))

(defn enable-save []
  (if (and (not (nil? (get @files :meta))) (not (nil? (get @files :source))))
    (config! save-button :enabled? true)
    (config! save-button :enabled? false)))

(defn open-button [id]
    (button
      :id id
      :text ::open
      :listen [:action (fn [e]
        (swap! files update-in [id] (fn [old new] new)
          (choose-file
            :type :open
            :filters [[".xml" ["xml"]]]))
        (enable-save))]))

(def my-frame
  (frame
    :title ::frame-title,
    :content (grid-panel
                :rows 3
                :columns 2
                :hgap 10
                :vgap 10
                :items
                  [(my-text ::file-select)
                   save-button
                   (my-text ::meta)
                   (open-button :meta)
                   (my-text ::source)
                   (open-button :source)])
    :on-close :exit
    :size [450 :by 300]
    :resizable? false))

(defn start-ui []
  (invoke-later
    (-> my-frame show!)))
