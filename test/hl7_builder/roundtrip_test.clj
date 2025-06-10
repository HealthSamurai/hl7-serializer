(ns hl7-builder.roundtrip-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [hl7-builder.core :refer [build-segment-str]]
   [hl7-builder.parse :refer [parse-segment-str]]))

(deftest test-serialize-deserialize-isomorphism
  (testing "Simple segment conversion preserves data"
    (let [data {0 "PID" 1 "field1" 2 "field2"}
          serialized (build-segment-str data)
          parsed (parse-segment-str serialized)]
      (is (= data parsed))))

  (testing "Segment with components conversion preserves data"
    (let [data {0 "PID" 1 {1 "lastname" 2 "firstname"}}
          serialized (build-segment-str data)
          deserialized (parse-segment-str serialized)]
      (is (= data deserialized))))

  (testing "Segment with repetitions conversion preserves data"
    (let [data {0 "PID" 1 ["value1" "value2"]}
          serialized (build-segment-str data)
          deserialized (parse-segment-str serialized)]
      (is (= data deserialized))))

  (testing "Complex segment conversion preserves data"
    (let [data {0 "OBR" 1 "123" 2 {1 "order1" 2 {1 "comp1" 2 "sub1"}} 3 ["test1" "test2"]}
          serialized (build-segment-str data)
          deserialized (parse-segment-str serialized)]
      (is (= data deserialized))))

  (testing "MSH segment with default delimiters preserves data"
    (let [data {0 "MSH" 1 "|" 2 "^~\\&" 3 "SENDER" 4 "RECEIVER"}
          serialized (build-segment-str data)
          parsed (parse-segment-str serialized)]
      (is (= data parsed))))

  (testing "MSH segment with custom field separator preserves data"
    (let [data {0 "MSH" 1 "~" 2 "^|\\&" 3 "APP" 4 "FACILITY"}
          serialized (build-segment-str data)
          parsed (parse-segment-str serialized)]
      (is (= data parsed))))

  (testing "MSH segment with only required fields preserves data"
    (let [data {0 "MSH" 1 "|" 2 "^~\\&"}
          serialized (build-segment-str data)
          parsed (parse-segment-str serialized)]
      (is (= data parsed))))

  (testing "MSH segment without explicit delimiters uses defaults"
    (let [data {0 "MSH" 3 "APP" 4 "FACILITY" 5 "TIMESTAMP"}
          serialized (build-segment-str data)
          parsed (parse-segment-str serialized)]
      (is (= {0 "MSH" 1 "|" 2 "^~\\&" 3 "APP" 4 "FACILITY" 5 "TIMESTAMP"} parsed))))

  (testing "Complex MSH segment with components preserves data"
    (let [data {0 "MSH" 1 "|" 2 "^~\\&" 3 {1 "SENDER" 2 "NAMESPACE"} 4 "RECEIVER"}
          serialized (build-segment-str data)
          parsed (parse-segment-str serialized)]
      (is (= data parsed)))))
