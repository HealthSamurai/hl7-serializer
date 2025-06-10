(ns hl7-builder.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [hl7-builder.core :as sut]))

(deftest test-build-segment-str-basic
  (testing "Simple segment with default delimiters"
    (is (= "PID|field1|field2"
           (sut/build-segment-str {0 "PID" 1 "field1" 2 "field2"}))))

  (testing "Segment with only type"
    (is (= "PID"
           (sut/build-segment-str {0 "PID"}))))

  (testing "Segment with gaps (sparse fields)"
    (is (= "PID||field2"
           (sut/build-segment-str {0 "PID" 2 "field2"}))))

  (testing "Segment with trailing empty fields trimmed"
    (is (= "PID|field1"
           (sut/build-segment-str {0 "PID" 1 "field1" 2 nil 3 ""}))))

  (testing "Mixed data types"
    (is (= "PID|123|45.67|true"
           (sut/build-segment-str {0 "PID" 1 123 2 45.67 3 true})))))

(deftest test-build-segment-str-components
  (testing "Field with components"
    (is (= "PID|lastname^firstname^middlename"
           (sut/build-segment-str {0 "PID" 1 {1 "lastname" 2 "firstname" 3 "middlename"}}))))

  (testing "Field with single component containing subcomponents"
    (is (= "PID|comp1&sub1"
           (sut/build-segment-str {0 "PID" 1 {1 {1 "comp1" 2 "sub1"}}}))))

  (testing "Field with multiple components containing subcomponents"
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
           (sut/build-segment-str {0 "PID" 1 [{1 "comp1" 2 "comp2"} {1 "comp3" 2 "comp4"}]}))))

  (testing "Mixed empty and non-empty repetitions"
    (is (= "TEST|val1~~val3"
           (sut/build-segment-str {0 "TEST" 1 ["val1" nil "val3"]})))))

(deftest test-build-segment-str-msh-special-handling
  (testing "MSH segment with default delimiters"
    (is (= "MSH|^~\\&"
           (sut/build-segment-str {0 "MSH"}))))

  (testing "MSH segment with custom field separator"
    (is (= "MSH#^~\\&"
           (sut/build-segment-str {0 "MSH" 1 "#"}))))

  (testing "MSH segment with custom encoding characters"
    (is (= "MSH|@%\\*"
           (sut/build-segment-str {0 "MSH" 2 "@%\\*"}))))

  (testing "MSH segment with both custom separators"
    (is (= "MSH#@%\\*"
           (sut/build-segment-str {0 "MSH" 1 "#" 2 "@%\\*"}))))

  (testing "MSH segment with additional fields"
    (is (= "MSH|^~\\&|sending_app|sending_facility"
           (sut/build-segment-str {0 "MSH" 3 "sending_app" 4 "sending_facility"}))))

  (testing "MSH segment with complex fields"
    (is (= "MSH|^~\\&|app^version|facility^dept&subdept"
           (sut/build-segment-str {0 "MSH"
                                   3 {1 "app" 2 "version"}
                                   4 {1 "facility" 2 {1 "dept" 2 "subdept"}}}))))

  (testing "MSH segment with custom delimiters and fields"
    (let [custom-delims {:field "#" :component "@" :repetition "%" :subcomponent "*" :escape "\\"}]
      (is (= "MSH#@%\\*#app@version"
             (sut/build-segment-str custom-delims
                                    {0 "MSH" 3 {1 "app" 2 "version"}}))))))

(deftest test-build-segment-str-custom-delimiters
  (testing "Custom delimiters with complex structure"
    (let [custom-delims {:field "#" :component "@" :repetition "%" :subcomponent "*" :escape "\\"}]
      (is (= "PID#field1*comp1@sub1#field2%rep1%rep2"
             (sut/build-segment-str custom-delims
                                    {0 "PID"
                                     1 {1 {1 "field1" 2 "comp1"} 2 {1 "sub1"}}
                                     2 ["field2" "rep1" "rep2"]}))))))

(deftest test-build-segment-str-complex
  (testing "Complex nested structure"
    (is (= "OBR|123|order1^comp1&sub1^comp2|test1~test2^testcomp"
           (sut/build-segment-str {0 "OBR"
                                   1 123
                                   2 {1 "order1" 2 {1 "comp1" 2 "sub1"} 3 "comp2"}
                                   3 ["test1" {1 "test2" 2 "testcomp"}]}))))

  (testing "Real-world HL7-like PID segment"
    (is (= "PID||PATID1234^ID^MR^HOSPITAL^MR|DOE^JOHN^M||19800101|M"
           (sut/build-segment-str {0 "PID"
                                   2 {1 "PATID1234" 2 "ID" 3 "MR" 4 "HOSPITAL" 5 "MR"}
                                   3 {1 "DOE" 2 "JOHN" 3 "M"}
                                   5 "19800101"
                                   6 "M"}))))

  (testing "Complex structure with all element types"
    (is (= "OBX|1|TX|CODE^DISPLAY&SYSTEM^VERSION|1|result1&qualifier~result2|units|ref_range"
           (sut/build-segment-str {0 "OBX"
                                   1 1
                                   2 "TX"
                                   3 {1 "CODE" 2 {1 "DISPLAY" 2 "SYSTEM"} 3 "VERSION"}
                                   4 1
                                   5 [{1 {1 "result1" 2 "qualifier"}} "result2"]
                                   6 "units"
                                   7 "ref_range"})))))

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
                          (sut/build-segment-str {0 "PID" 1 {0 "bad" 1 "comp1"}})))
    (is (thrown-with-msg? AssertionError #"zero or negative keys"
                          (sut/build-segment-str {0 "PID" 1 {1 {0 "bad" 1 "sub1"}}}))))

  (testing "Deeply nested structure throws error"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"HL7v2 does not support nesting beyond subcomponents"
                          (sut/build-segment-str {0 "TEST"
                                                  1 {1 {1 {1 "too-deep"}}}})))))

(deftest test-build-segment-str-edge-cases
  (testing "Empty components and subcomponents are handled"
    (is (= "PID|comp1^^comp3"
           (sut/build-segment-str {0 "PID" 1 {1 "comp1" 2 nil 3 "comp3"}}))))

  (testing "Trailing empty components are trimmed"
    (is (= "PID|comp1^comp2"
           (sut/build-segment-str {0 "PID" 1 {1 "comp1" 2 "comp2" 3 nil 4 ""}}))))

  (testing "Subcomponents with gaps"
    (is (= "PID|sub1&&sub3"
           (sut/build-segment-str {0 "PID" 1 {1 {1 "sub1" 2 nil 3 "sub3"}}}))))

  (testing "Mixed nil and empty string handling"
    (is (= "PID|field1||field3"
           (sut/build-segment-str {0 "PID" 1 "field1" 2 nil 3 "field3" 4 ""}))))

  (testing "Large field numbers"
    (is (= "PID||||||||||field10"
           (sut/build-segment-str {0 "PID" 10 "field10"})))))
