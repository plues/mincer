(ns mincer.ui
  (:gen-class)
  (:require
    [seesaw.core :refer [alert native! text text! button config!
                         frame invoke-later invoke-now invoke-soon
                         show! grid-panel label scrollable top-bottom-split]]
    [seesaw.chooser :refer [choose-file]]
    [seesaw.icon :refer [icon]]
    [clojure.java.io :refer [as-file copy]]
    [clojure.tools.logging :as log]
    [clojure.java.io :as io]
    [mincer.data :refer [persist]]
    [mincer.preferences :refer [load-prefs get-pref set-pref]]
    [mincer.xml.modules :as modules]
    [mincer.xml.tree :as tree])
  (:import (org.xml.sax SAXParseException)
           (org.apache.log4j AppenderSkeleton LogManager Level)))


; preferences
(def node-name "de/hhu/stups/mincer")
(def last-tree-directory "last-tree-dir")
(def last-data-directory "last-data-dir")
(def last-output-directory "last-output-dir")
(def prefs (load-prefs node-name))
;


(defn dirname [f] (-> f as-file .getCanonicalFile .getParent))

(def files (atom {:meta nil :source nil}))

(def textarea (text :multi-line? true :editable? false :wrap-lines? true))

(defn clear-textarea [] (text! textarea ""))

(def logo
  (icon
    (-> "mincer/logo.png" io/resource javax.imageio.ImageIO/read)))

(def meta-text
  (text :text (get @files :meta) :editable? false))

(def source-text
  (text :text (get @files :source) :editable? false))

(defn apply-with-error-handling [f file]
  (try
    (apply f [file])
    (catch SAXParseException e
      (let [msg (.getMessage e)
            line (.getLineNumber e)
            column (.getColumnNumber e)
            info (format "Error in file %s:\nline: %s, column: %s\nMessage: %s" file line column msg)]
        (log/info info)
        (invoke-soon (alert info)))
        (throw e))
    (catch Exception e
        (invoke-soon
          (->
            (frame
              :title "Error!"
              :content (scrollable
                (text
                  :text
                    (let [sw (new java.io.StringWriter)
                          pw (new java.io.PrintWriter sw)]
                      (.printStackTrace e pw)
                      (.toString sw))
                  :multi-line? true
                  :wrap-lines? true
                  :tab-size 4
                  :rows 20
                  :editable? false))
              :size [600 :by 600]
              :on-close :dispose)
            show!))
        (throw e))))


; heler functions
(def save-button)
(defn disable-save []
  (config! save-button :enabled? false))

(defn enable-save []
  (if (and (not (nil? (get @files :meta))) (not (nil? (get @files :source))))
    (config! save-button :enabled? true)
    (config! save-button :enabled? false)))

(defn check-text [id t]
  (if (not (nil? (get @files id)))
    (config! t :text (.getAbsolutePath (get @files id)))))

; event handler
(defn save-button-listener [e]
  (try
    (disable-save)
    (clear-textarea)
    (let [data @(future (apply-with-error-handling modules/process (get @files :source)))
          tree @(future (apply-with-error-handling tree/process (get @files :meta)))
          db (future (persist tree (:modules data) (:units data)))
          file (choose-file :type :save :dir (get-pref prefs last-output-directory))]
      @(future (copy (as-file @db) (as-file file)))
      (set-pref prefs last-output-directory (dirname file))
      (log/info "Created database" (.getAbsolutePath file)))
    ; Database creating failed
    (catch Exception e)
    (finally (enable-save))))

(defn meta-button-listener [e]
  (swap! files update-in [:meta]
         (fn [old new] new)
         (choose-file :type :open
                      :dir (get-pref prefs last-tree-directory)
                      :filters [[".xml" ["xml"]]]))
  (enable-save)
  (set-pref prefs last-tree-directory (dirname (get @files :meta)))
  (check-text :meta meta-text))

(defn source-button-listener [e]
  (swap! files update-in [:source]
         (fn [old new] new)
         (choose-file :type :open
                      :dir (get-pref prefs last-data-directory)
                      :filters [[".xml" ["xml"]]]))
  (enable-save)
  (set-pref prefs last-data-directory (dirname (get @files :source)))
  (check-text :source source-text))

; ui elements
(def save-button
  (button
    :id :save
    :text ::create
    :enabled? false
    :listen [:action save-button-listener]))


(def meta-button
  (button
    :id :meta
    :text ::meta
    :listen [:action meta-button-listener]))

(def source-button
  (button
    :id :source
    :text ::source
    :listen [:action source-button-listener]))

(def my-frame
  (frame
    :title ::frame-title,
    :content (top-bottom-split
               (grid-panel
                 :rows 4
                 :size [600 :by 320]
                 :columns 2
                 :hgap 10
                 :vgap 10
                 :items
                   [(label ::file-select)
                    (label :icon logo)
                    meta-button
                    meta-text
                    source-button
                    source-text
                    (label ::save)
                    save-button])
               ; bottom logging textarea
               (scrollable textarea :size [600 :by 250]))
    :on-close :exit
    :size [600 :by 570]
    :resizable? false))

(defn log-message [ev]
  (let [level (.getLevel ev)
        msg (.getMessage ev)]
  ; ignore message bellow INFO
  (when (.isGreaterOrEqual level Level/INFO)
    (invoke-later
      ; XXX considre printing messages with different colors based on the log level
      (.append textarea (str level ": " msg "\n"))))))

(defn get-logger [out-fn]
  (proxy [org.apache.log4j.AppenderSkeleton] []
    (append [event] (out-fn event))))

(defn setup-logger []
  (let [root (org.apache.log4j.LogManager/getRootLogger)]
    (.addAppender root (get-logger log-message))))

(defn start-ui []
  (native!)
  (setup-logger)
  (invoke-later
    (-> my-frame show!)))
