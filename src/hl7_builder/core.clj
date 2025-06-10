(ns hl7-builder.core
  (:require
   [clojure.string :as str]
   [hl7-builder.delimiters :refer [default-delimiters]]))

(defn- trim-trailing
  "Removes trailing empty elements from a vector"
  [v]
  (->> v
       reverse
       (drop-while empty?)
       reverse
       vec))

(defn- validate-map-keys [m level]
  (let [keys (keys m)]
    (if (= 0 level) ; 0 is only for segment level
      (do
        (assert (contains? m 0) "Top-level segment must have key 0 (segment type)")
        (let [segment-type (get m 0)]
          (assert (not (str/blank? segment-type))
                  "Top-level segment key 0 must have a non-empty segment type"))
        (let [negative-keys (filter neg? keys)]
          (assert (empty? negative-keys)
                  "Top-level segment cannot have negative keys")))
      (let [non-positive-keys (filter #(<= % 0) keys)]
        (assert (empty? non-positive-keys)
                "Maps cannot have zero or negative keys")))))

(defn- validate-nesting-level [level]
  (when (>= level 3)
    (throw (ex-info "HL7v2 does not support nesting beyond subcomponents" {}))))

(defn- build-field [delimiters v level]
  (cond
    (vector? v)
    (let [element-level (max 1 (dec level))]
      (->> v
           (map #(build-field delimiters % element-level))
           (str/join (:repetition delimiters))))
    (map? v)
    (do
      (validate-map-keys v level)
      (validate-nesting-level level)
      (let [joiner (case level
                     1 (:component delimiters)
                     2 (:subcomponent delimiters))
            upper-bound (apply max (keys v))
            values (vec (map #(build-field delimiters (get v %) (inc level))
                             (range 1 (inc upper-bound))))
            trimmed (trim-trailing values)] ; Trim empty componenets, subcomponents
        (str/join joiner trimmed)))
    (nil? v) ""
    :else (str v)))

(defn- build-msh-segment
  [delimiters m]
  (validate-map-keys m 0)
  (let [field-separator (if (contains? m 1)
                          (get m 1)
                          (:field delimiters))
        encoding-chars (if (contains? m 2)
                         (get m 2)
                         (str (:component delimiters)
                              (:repetition delimiters)
                              (:escape delimiters)
                              (:subcomponent delimiters)))
        max-key (apply max (keys m))
        fields-from-3 (when (> max-key 2)
                        (vec (map #(build-field delimiters (get m %) 1)
                                  (range 3 (inc max-key)))))
        trimmed-fields (trim-trailing (or fields-from-3 []))]
    (str "MSH" field-separator encoding-chars
         (when (seq trimmed-fields)
           (str field-separator (str/join field-separator trimmed-fields))))))

(defn build-segment-str
  ([m] (build-segment-str default-delimiters m))
  ([delimiters m]
   (if (= "MSH" (get m 0))
     (build-msh-segment delimiters m)
     (do
       (validate-map-keys m 0)
       (let [max-key (apply max (keys m))
             fields (vec (map #(build-field delimiters (get m %) 1)
                              (range (inc max-key))))
             trimmed-fields (trim-trailing fields)]
         (str/join (:field delimiters) trimmed-fields))))))
