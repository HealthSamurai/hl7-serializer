(ns hl7-builder.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [hl7-builder.core :as sut]))

(deftest test-build-segment-str-basic
  (testing "Simple segment with default delimiters"
    (is (= "MSH|field1|field2"
           (sut/build-segment-str {0 "MSH" 1 "field1" 2 "field2"}))))

  (testing "Segment with only type"
    (is (= "PID"
           (sut/build-segment-str {0 "PID"}))))

  (testing "Segment with gaps (sparse fields)"
    (is (= "MSH||field2"
           (sut/build-segment-str {0 "MSH" 2 "field2"}))))

  (testing "Segment with trailing empty fields (not trimmed at top level)"
    (is (= "MSH|field1"
           (sut/build-segment-str {0 "MSH" 1 "field1" 2 nil 3 ""}))))

  (testing "Mixed data types"
    (is (= "PID|123|45.67|true"
           (sut/build-segment-str {0 "PID" 1 123 2 45.67 3 true})))))

(deftest test-build-segment-str-components
  (testing "Field with components"
    (is (= "PID|lastname^firstname^middlename"
           (sut/build-segment-str {0 "PID" 1 {1 "lastname" 2 "firstname" 3 "middlename"}}))))

  (testing "Field with subcomponents"
    (is (= "PID|comp1&sub1^comp2&sub2"
           (sut/build-segment-str {0 "PID" 1 {1 {1 "comp1" 2 "sub1"} 2 {1 "comp2" 2 "sub2"}}}))))

  (testing "Components with trailing empty elements trimmed"
    (is (= "PID|comp1^comp2"
           (sut/build-segment-str {0 "PID" 1 {1 "comp1" 2 "comp2" 3 nil 4 ""}})))))

(deftest test-build-segment-str-repetitions
  (testing "Field with repetitions"
    (is (= "PID|value1~value2~value3"
           (sut/build-segment-str {0 "PID" 1 ["value1" "value2" "value3"]}))))

  (testing "Empty repetition vector"
    (is (= "PID"
           (sut/build-segment-str {0 "PID" 1 []}))))

  (testing "Repetitions with components"
    (is (= "PID|comp1^comp2~comp3^comp4"
           (sut/build-segment-str {0 "PID" 1 [{1 "comp1" 2 "comp2"} {1 "comp3" 2 "comp4"}]})))))


(deftest test-build-segment-str-custom-delimiters
  (testing "Custom delimiters"
    (let [custom-delims {:field "#" :component "@" :repetition "%" :subcomponent "*"}]
      (is (= "MSH#field1*comp1@sub1#field2%rep1%rep2"
             (sut/build-segment-str custom-delims
                                    {0 "MSH"
                                     1 {1 {1 "field1" 2 "comp1"} 2 {1 "sub1"}}
                                     2 ["field2" "rep1" "rep2"]}))))))

(deftest test-build-segment-str-complex
  (testing "Complex nested structure"
    (is (= "OBR|123|order1^comp1&sub1^comp2|test1~test2^testcomp"
           (sut/build-segment-str {0 "OBR"
                                   1 123
                                   2 {1 "order1" 2 {1 "comp1" 2 "sub1"} 3 "comp2"}
                                   3 ["test1" {1 "test2" 2 "testcomp"}]}))))

  (testing "Real-world HL7-like segment"
    (is (= "PID||PATID1234^ID^MR^HOSPITAL^MR|DOE^JOHN^M||19800101|M"
           (sut/build-segment-str {0 "PID"
                                   2 {1 "PATID1234" 2 "ID" 3 "MR" 4 "HOSPITAL" 5 "MR"}
                                   3 {1 "DOE" 2 "JOHN" 3 "M"}
                                   5 "19800101"
                                   6 "M"})))))

(deftest test-build-segment-str-validation
  (testing "Missing segment type throws error"
    (is (thrown-with-msg? AssertionError #"Top-level segment must have key 0"
                          (sut/build-segment-str {1 "field1"}))))

  (testing "Empty segment type throws error"
    (is (thrown-with-msg? AssertionError #"non-empty segment type"
                          (sut/build-segment-str {0 "" 1 "field1"})))
    (is (thrown-with-msg? AssertionError #"non-empty segment type"
                          (sut/build-segment-str {0 nil 1 "field1"}))))

  (testing "Negative keys throw error"
    (is (thrown-with-msg? AssertionError #"cannot have negative keys"
                          (sut/build-segment-str {0 "MSH" -1 "bad" 1 "field1"}))))

  (testing "Component maps with invalid keys throw error"
    (is (thrown-with-msg? AssertionError #"zero or negative keys"
                          (sut/build-segment-str {0 "MSH" 1 {0 "bad" 1 "comp1"}})))))

(deftest test-edge-cases
  (testing "Large field numbers"
    (is (= "MSH||||||||||field10"
           (sut/build-segment-str {0 "MSH" 10 "field10"}))))

  (testing "Deeply nested structure"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"HL7v2 does not support nesting beyond subcomponents"
           (sut/build-segment-str {0 "TEST"
                                   1 {1 {1 "comp1" 2 "sub1"}
                                      2 {1 "comp2" 2 "sub2" 3 {1 "subsub1"}}}})))))

  (testing "Mixed empty and non-empty repetitions"
    (is (= "TEST|val1~~val3"
           (sut/build-segment-str {0 "TEST" 1 ["val1" nil "val3"]}))))
