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

(defn my-writer [file]
  (let [data (modules/process (get @files :source))
        tree (tree/process (get @files :meta))
        db (persist tree (:modules data) (:units data))]
    (copy (as-file db) (as-file file))
    (println "Created database" file)))

(defn my-text [t]
  (text :text t
        :editable? false))

(def meta-text
  (text :text (get @files :meta) :editable? false))

(def source-text
  (text :text (get @files :source) :editable? false))

; button functions
(def save-button
  (button
    :id :save
    :text ::create
    :enabled? false
    :listen [:action (fn [e]
      (try
        (my-writer
          (choose-file
            :type :save
            :filters [[".sqlite3" ["sqlite3"]]]))
      (catch Exception e
        (invoke-later
          (show!
            (frame
              :title "Error!"
              :content (.getMessage e)
              :on-close :exit
              :size [450 :by 300]
              :resizable? false))))))]))

(defn enable-save []
  (if (and (not (nil? (get @files :meta))) (not (nil? (get @files :source))))
    (config! save-button :enabled? true)
    (config! save-button :enabled? false)))

(defn check-text [id t]
  (if (not (nil? (get @files id)))
    (config! t :text (.getAbsolutePath (get @files id)))))

(def meta-button
  (button
    :id :meta
    :text ::meta
    :listen [:action (fn [e]
      (swap! files update-in [:meta] (fn [old new] new)
        (choose-file
          :type :open
          :filters [[".xml" ["xml"]]]))
      (enable-save)
      (check-text :meta meta-text))]))

(def source-button
  (button
    :id :source
    :text ::source
    :listen [:action (fn [e]
      (swap! files update-in [:source] (fn [old new] new)
        (choose-file
          :type :open
          :filters [[".xml" ["xml"]]]))
      (enable-save)
      (check-text :source source-text))]))

(def my-frame
  (frame
    :title ::frame-title,
    :content (grid-panel
                :rows 4
                :columns 2
                :hgap 10
                :vgap 10
                :items
                  [(my-text ::file-select)
                   (text "")
                   meta-button
                   meta-text
                   source-button
                   source-text
                   (my-text ::save)
                   save-button])
    :on-close :exit
    :size [600 :by 300]
    :resizable? false))

(defn start-ui []
  (invoke-later
    (-> my-frame show!)))
