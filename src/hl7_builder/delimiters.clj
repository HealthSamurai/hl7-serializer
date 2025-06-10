(ns hl7-builder.delimiters)

(def default-delimiters
  {:field "|"
   :component "^"
   :repetition "~"
   :subcomponent "&"
   :escape "\\"})
