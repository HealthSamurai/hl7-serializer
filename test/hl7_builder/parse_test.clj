(ns hl7-builder.parse-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [hl7-builder.parse :as sut]))

(deftest test-parse-segment-str-basic
  (testing "Simple segment with basic fields"
    (is (= {0 "MSH" 1 "|" 2 "field1" 3 "field2"}
           (sut/parse-segment-str "MSH|field1|field2"))))

  (testing "Segment with only type"
    (is (= {0 "PID"}
           (sut/parse-segment-str "PID"))))

  (testing "Empty or nil input"
    (is (nil? (sut/parse-segment-str "")))
    (is (nil? (sut/parse-segment-str nil)))))

(deftest test-parse-segment-components
  (testing "Field with components"
    (is (= {0 "PID" 1 {1 "lastname" 2 "firstname"}}
           (sut/parse-segment-str "PID|lastname^firstname")))))

(deftest test-parse-segment-subcomponents
  (testing "Field with component that has subcomponents"
    (is (= {0 "PID" 1 {1 {1 "comp1" 2 "sub1"}}}
           (sut/parse-segment-str "PID|comp1&sub1")))))

(deftest test-parse-segment-str-repetitions
  (testing "Field with repetitions"
    (is (= {0 "PID" 1 ["value1" "value2"]}
           (sut/parse-segment-str "PID|value1~value2")))))
