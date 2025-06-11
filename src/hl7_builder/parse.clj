(ns hl7-builder.parse
  (:require
   [clojure.string :as str]
   [hl7-builder.delimiters :refer [default-delimiters]])
  (:import
   java.util.regex.Pattern))

(defn- parse-field [delimiters s level]
  (cond
    (str/blank? s) nil

    (and (= level 1) (str/includes? s (:component delimiters)))
    (let [comps (str/split s (re-pattern (Pattern/quote (:component delimiters))))
          indexed-components (keep-indexed (fn [idx comp]
                                             (when-not (str/blank? comp)
                                               [(inc idx) (parse-field delimiters comp 2)]))
                                           comps)]
      (if (seq indexed-components) (into {} indexed-components) s))

    (and (= level 1) (str/includes? s (:subcomponent delimiters)))
    {1 (parse-field delimiters s 2)}

    (and (= level 1) (str/includes? s (:repetition delimiters)))
    (let [reps (str/split s (re-pattern (Pattern/quote (:repetition delimiters))))]
      (mapv #(parse-field delimiters % 1) reps))

    (and (= level 2) (str/includes? s (:repetition delimiters)))
    (let [reps (str/split s (re-pattern (Pattern/quote (:repetition delimiters))))]
      (mapv #(parse-field delimiters % 2) reps))

    (and (= level 2) (str/includes? s (:subcomponent delimiters)))
    (let [subcomps (str/split s (re-pattern (Pattern/quote (:subcomponent delimiters))))
          indexed-subcomps (keep-indexed (fn [idx subcomp]
                                           (when-not (str/blank? subcomp)
                                             [(inc idx) subcomp]))
                                         subcomps)]
      (if (seq indexed-subcomps) (into {} indexed-subcomps) s))

    :else s))

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
                                       (let [parsed (parse-field delimiters field 1)]
                                         (when parsed
                                           [(+ idx 3) parsed])))  ; Start from msh3
                                     (drop 2 fields)))]           ; Drop msh1, msh2
         (into (sorted-map) (merge {0 "MSH" 1 msh1 2 msh2} parsed-fields)))
       ;; All other segments are parsed normally
       (let [fields (str/split s (re-pattern (Pattern/quote (:field delimiters))))
             indexed-fields (keep-indexed (fn [idx field]
                                            (let [parsed (parse-field delimiters field 1)]
                                              (when parsed
                                                [idx parsed])))
                                          fields)]
         (into {} indexed-fields))))))
