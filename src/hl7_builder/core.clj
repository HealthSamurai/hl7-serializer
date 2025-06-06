(ns hl7-builder.core
  (:require [clojure.string :as str]))

(def default-delimiters
  {:field "|"
   :component "^"
   :repetition "~"
   :subcomponent "&"})

(defn trim-trailing [coll]
  (let [v (vec coll)]
    (loop [i (dec (count v))]
      (cond
        (neg? i) []
        (seq (nth v i)) (subvec v 0 (inc i))
        :else (recur (dec i))))))

(defn validate-map-keys [m is-top-level?]
  (let [keys (keys m)]
    (if is-top-level?
      (do
        (assert (contains? m 0) "Top-level segment must have key 0 (segment type)")
        (let [segment-type (get m 0)]
          (assert (and (some? segment-type)
                       (not (and (string? segment-type) (str/blank? segment-type))))
                  "Top-level segment key 0 must have a non-empty segment type"))
        (let [negative-keys (filter neg? keys)]
          (assert (empty? negative-keys)
                  "Top-level segment cannot have negative keys")))
      (let [non-positive-keys (filter #(<= % 0) keys)]
        (assert (empty? non-positive-keys)
                "Maps cannot have zero or negative keys")))))

(defn validate-nesting-level [level]
  (when (>= level 3)
    (throw (ex-info "HL7v2 does not support nesting beyond subcomponents" {}))))

(defn build-field [delimiters v level is-top-level?]
  (cond
    (string? v) v
    (number? v) (str v)
    (vector? v)
    (let [element-level (max 1 (dec level))]
      (->> v
           (map #(build-field delimiters % element-level false))
           (str/join (:repetition delimiters))))
    (map? v)
    (do
      (validate-map-keys v is-top-level?)
      (validate-nesting-level level)
      (let [joiner (case level
                     1 (:component delimiters)
                     2 (:subcomponent delimiters)
                     ;; Level 3+ would be sub-subcomponent (not supported)
                     (throw (ex-info "unexpected nesting level" {:level level})))
            max-pos (apply max (keys v))
            values (map #(build-field delimiters (get v %) (inc level) false)
                        (range 1 (inc max-pos)))
            trimmed (trim-trailing values)]
        (str/join joiner trimmed)))
    (nil? v) ""
    :else (str v)))

(defn build-segment-str
  ([m] (build-segment-str default-delimiters m))
  ([delimiters m]
   (validate-map-keys m true)
   (let [max-key (apply max (keys m))
         fields (map #(build-field delimiters (get m %) 1 false)  ; Start at level 1
                     (range (inc max-key)))
         trimmed-fields (trim-trailing fields)]
     (str/join (:field delimiters) trimmed-fields))))
