(ns mincer.module-combinations
  (:require [clojure.math.combinatorics :refer [subsets cartesian-product combinations]]
            [clojure.tools.logging :as log]
            [clojure.set :refer [difference]]))

(defn cp-sum [ms] (apply + (map :cp ms)))

(defn filter-by-cp [level]
  (let [modules           (set (:children level))
        mandatory-modules (filter :mandatory modules)
        elective-modules  (seq (difference modules mandatory-modules)) ; has to be a seq type for subsets below
        mandatory-cp      (cp-sum mandatory-modules)
        ; mandatory-cp plus the sum of the chosen elective cp must be between
        ; min-cp and max-cp for the current level
        filter-fn         (fn [sum] (<= (:min-cp level) (+ sum mandatory-cp) (:max-cp level)))]
    ; all combinations of mandatory-modules and a result of the filter below
    ; compose a valid solution
    (assert (not (nil? mandatory-cp)) "Expected mandatory-cp to not be nil")
    (map #(concat mandatory-modules %)
         ; find all subsets of elective-modules that satisfy filter-fn, i.e.
         ; sum of cp + mandatory-modules is in min-cp, max-cp for level
         (filter #(filter-fn (cp-sum %))
                 (subsets elective-modules)))))

(defn layer-filter-by-cp [layer ms]
  (and
    (pos? (count ms))
    (apply distinct? ms)
    (let [sum (cp-sum ms)]
      (case (:type layer)
        :course (= (:cp layer) sum)
        :level  (<= (:min-cp layer) sum (:max-cp layer))))))


(defn filter-by-count [level]
  (let [modules           (set (:children level))
        mandatory-modules (filter :mandatory modules)
        elective-modules  (seq (difference modules mandatory-modules))
        mandatory-count   (count mandatory-modules)
        min-count         (max (- (:min level) mandatory-count) 0) ; minimum number of modules left to choose
        max-count         (- (:max level) mandatory-count)] ; maximum number of modules left to choose
    (if (neg? max-count)
      ((log/error (str "Level " (:name level) " has more mandatory modules than max allowed for level"))
       (set [])) ; no solutions for this level
      (let [sizes             (range min-count (inc max-count))
            candidates        (mapcat #(combinations elective-modules %) sizes) ]
         ; all combinations of mandatory-modules and an element of candidates are valid solutions
         (map #(concat mandatory-modules %) candidates)))))

(defn layer-filter-by-count [level ms]
  (and
    (pos? (count ms))
    (apply distinct? ms)
    (case (:type level)
      :level (let [cc (count ms)] (<= (:min level) cc (:max level)))
      :course true)))


(declare traverse-level)

(defn filter-children [level level-filter-fn module-filter-fn]
  (let [children     (filter #(not (= (:type %) :minors))  (:children level))
        modules      (pmap #(traverse-level % level-filter-fn module-filter-fn) children) ; collect all lists of module combinations from sub-levels
        combinations (pmap flatten (apply cartesian-product modules))] ; build all combinations of possible choices
    (filter #(level-filter-fn level %) combinations)))

(defn traverse-level [level level-filter-fn module-filter-fn]
  (let [children (:children level)]
  (cond
    (zero? (count children )) '()
    (= :module (:type (first children))) (module-filter-fn level)
    ; inner node
    :else (filter-children level level-filter-fn module-filter-fn))))

(defn traverse-course-cp [course]
  (let [result (filter-children course layer-filter-by-cp filter-by-cp)]
    (log/debug (str "Done with " (:name course)))
    result))

(defn traverse-course-count [course]
  (let [result  (filter-children course layer-filter-by-count filter-by-count)]
    (log/debug (str "Done with " (:name course)))
    result))

(defn traverse-course [course]
  (set (map set ((if-not (nil? (:cp course))
     traverse-course-cp
     traverse-course-count) course))))
