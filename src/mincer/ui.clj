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

(defn any? [x y] (not (nil? (some #{x} y))))

(defn dirname [f] (-> f as-file .getCanonicalFile .getParent))

(def tree-file (atom nil))
(def data-file (atom nil))

(def textarea (text :multi-line? true :editable? false :wrap-lines? true))

(defn clear-textarea [] (text! textarea ""))

(def logo
  (icon
    (-> "mincer/logo.png" io/resource javax.imageio.ImageIO/read)))

(def meta-text
  (text :text @tree-file :editable? false))

(def source-text
  (text :text @data-file :editable? false))

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
      :error)
    (catch IllegalArgumentException e
      (invoke-soon (alert (.getMessage e)))
      :error)
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
      :error)))


; heler functions
(defn any-file-chosen? [] (boolean (or @tree-file @data-file)))

(declare save-button)
(defn disable-save []
  (config! save-button :enabled? false))

(defn enable-save []
  (config! save-button :enabled? (boolean (and @tree-file @data-file))))

(defn check-text [id t]
  (if @id
    (config! t :text (.getAbsolutePath @id))))

(defn save-db [db]
  (if-let [file (choose-file :type :save
                             :dir (get-pref prefs last-output-directory))]
    (do
      @(future (copy (as-file db) (as-file file)))
      (set-pref prefs last-output-directory (dirname file))
      (log/info "Created database" (.getAbsolutePath file)))
    (log/info "User canceled operation")))

; event handler
(defn save-button-listener [e]
  (disable-save)
  (clear-textarea)
  (let [data (future (apply-with-error-handling modules/process @data-file))
        tree (future (apply-with-error-handling tree/process @tree-file))]
    (if-not (any? :error [@data @tree])
      (save-db
        (persist @tree (:modules @data) (:units @data)))))
  (enable-save))

(defn meta-button-listener [e]
  (choose-file :type :open
               :dir (if-not (any-file-chosen?) (get-pref prefs last-tree-directory))
               :filters [[".xml" ["xml"]]]
               :success-fn (fn [fc file]
                             (swap! tree-file (constantly file))
                             (set-pref prefs last-tree-directory (dirname file))))
  (enable-save)
  (check-text tree-file meta-text))

(defn source-button-listener [e]
  (choose-file :type :open
               :dir (if-not (any-file-chosen?) (get-pref prefs last-data-directory))
               :filters [[".xml" ["xml"]]]
               :success-fn  (fn [fc file]
                              (swap! data-file (constantly file))
                              (set-pref prefs last-data-directory (dirname file))))
  (enable-save)
  (check-text data-file source-text))

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
  (invoke-now
    (show! my-frame)))
