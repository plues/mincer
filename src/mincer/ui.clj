(ns mincer.ui
  (:gen-class)
  (:require
    [seesaw.core :refer [alert native! text button config!
                         frame invoke-later show! grid-panel label scrollable top-bottom-split]]
    [seesaw.chooser :refer [choose-file]]
    [seesaw.icon :refer [icon]]
    [clojure.java.io :refer [as-file copy]]
    [clojure.tools.logging :as log]
    [clojure.java.io :as io]
    [mincer.data :refer [persist]]
    [mincer.xml.modules :as modules]
    [mincer.xml.tree :as tree])
  (:import (org.xml.sax SAXParseException)
           (org.apache.log4j AppenderSkeleton LogManager Level)))

(def files (atom {:meta nil :source nil}))

(def textarea (text :multi-line? true :editable? false :wrap-lines? true))

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
        (alert info))
        (throw e))))

; button functions
(def save-button
  (button
    :id :save
    :text ::create
    :enabled? false
    :listen [:action (fn [e]
      (try
        (let [data (apply-with-error-handling modules/process (get @files :source))
              tree (apply-with-error-handling tree/process (get @files :meta))
              db (persist tree (:modules data) (:units data))
              file (choose-file :type :save)]
        (copy (as-file db) (as-file file))
        (log/info "Created database" (.getAbsolutePath file)))
      (catch SAXParseException e) ; handled before and ignored here
      (catch Exception e
        (invoke-later
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
            show!)))))]))

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
    :content (top-bottom-split
               (grid-panel
                 :rows 4
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
               (scrollable textarea))
    :on-close :exit
    :size [600 :by 700]
    :resizable? false))

(defn log-message [ev]
  (let [level (.getLevel ev)
        msg (.getMessage ev)])
  ; ignore message bellow INFO
  (when (.isGreaterOrEqual level Level/INFO)
    (invoke-later
      (.append textarea (str (.getMessage ev) "\n")))))

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
