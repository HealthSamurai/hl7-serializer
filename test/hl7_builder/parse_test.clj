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
           (sut/parse-segment-str "PID|value1~value2"))))

  (testing "Field with repetitions containing components"
    (is (= {0 "PID"
            1 "1"
            3 [{1 "M000001531" 5 "MR" 6 "BMH"}
               {1 "T1-20250118014500" 5 "PI" 6 "BMH"}
               {1 "T00001540" 5 "EMR" 6 "BMH"}
               {1 "05ADD0D4-5723-4399-8184-059AC0B2C8ED" 5 "PT" 6 "BMH"}]}
           (sut/parse-segment-str "PID|1||M000001531^^^^MR^BMH~T1-20250118014500^^^^PI^BMH~T00001540^^^^EMR^BMH~05ADD0D4-5723-4399-8184-059AC0B2C8ED^^^^PT^BMH")))))
