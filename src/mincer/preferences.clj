(ns mincer.preferences
  (:gen-class)
  (:import (java.util.prefs Preferences)))

(defn load-prefs [node-name] (.node (Preferences/userRoot) node-name))
(defn set-pref [prefs key value] (.put prefs key value))
(defn get-pref [prefs key] (.get prefs key nil))
