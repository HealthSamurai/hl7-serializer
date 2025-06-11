(ns hl7-builder.parse
  (:require
   [clojure.string :as str]
   [hl7-builder.delimiters :refer [default-delimiters]])
  (:import
   java.util.regex.Pattern))

(defn- parse-subcomponents [delimiters string]
  (if (str/includes? string (:subcomponent delimiters))
    (let [subcomps (str/split string (re-pattern (Pattern/quote (:subcomponent delimiters))))
          indexed-subcomps (keep-indexed (fn [idx subcomp]
                                           (when-not (str/blank? subcomp)
                                             [(inc idx) subcomp]))
                                         subcomps)]
      (if (seq indexed-subcomps) (into {} indexed-subcomps) string))
    string))

(defn- parse-field [delimiters string]
  (cond
    (str/blank? string) nil

    (str/includes? string (:repetition delimiters))
    (let [reps (str/split string (re-pattern (Pattern/quote (:repetition delimiters))))]
      (mapv #(parse-field delimiters %) reps))

    (str/includes? string (:component delimiters))
    (let [comps (str/split string (re-pattern (Pattern/quote (:component delimiters))))
          indexed-components (keep-indexed (fn [idx comp]
                                             (when-not (str/blank? comp)
                                               [(inc idx) (parse-subcomponents delimiters comp)]))
                                           comps)]
      (if (seq indexed-components) (into {} indexed-components) string))

    (str/includes? string (:subcomponent delimiters))
    ;; When field contains only subcomponents (no components), wrap in map with key 1
    ;; to maintain consistent component-like structure for single component fields
    {1 (parse-subcomponents delimiters string)}

    :else string))

(defn parse-segment-str
  ([s] (parse-segment-str default-delimiters s))
  ([delimiters s]
   (when-not (str/blank? s)
     (if (and (>= (count s) 4) (= (subs s 0 3) "MSH"))
       (let [field-sep (subs s 3 4)
             fields (str/split s (re-pattern (Pattern/quote field-sep)))
             msh1 field-sep      ; Have to take it manually becuse its separator itself
             msh2 (get fields 1) ; Have to handle it manually because it contains delimiters
             ;; The rest parsed normally starting from msh3
             parsed-fields (into {} (keep-indexed
                                     (fn [idx field]
                                       (let [parsed (parse-field delimiters field)]
                                         (when parsed
                                           [(+ idx 3) parsed])))  ; Start from msh3
                                     (drop 2 fields)))]           ; Drop msh1, msh2
         (into (sorted-map) (merge {0 "MSH" 1 msh1 2 msh2} parsed-fields)))
       ;; All other segments are parsed normally
       (let [fields (str/split s (re-pattern (Pattern/quote (:field delimiters))))
             indexed-fields (keep-indexed (fn [idx field]
                                            (let [parsed (parse-field delimiters field)]
                                              (when parsed
                                                [idx parsed])))
                                          fields)]
         (into {} indexed-fields))))))
