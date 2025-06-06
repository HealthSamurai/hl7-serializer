# HL7 Builder

[![Tests](https://github.com/HealthSamurai/hl7-serializer/actions/workflows/test.yml/badge.svg)](https://github.com/HealthSamurai/hl7-serializer/actions/workflows/test.yml)

Clojure library for building HL7v2 segments from structured data.

## Usage

```clojure
(require '[hl7-builder.core :as hl7])

;; Basic segment
(hl7/build-segment-str {0 "MSH" 1 "field1" 2 "field2"})
;; => "MSH|field1|field2"

;; Components and subcomponents
(hl7/build-segment-str {0 "PID" 1 {1 {1 "comp1" 2 "sub1"} 2 {1 "comp2" 2 "sub2"}}})
;; => "PID|comp1&sub1^comp2&sub2"

;; Repetitions
(hl7/build-segment-str {0 "PID" 1 ["value1" "value2" "value3"]})
;; => "PID|value1~value2~value3"

;; Custom delimiters
(hl7/build-segment-str {:field "#" :component "@" :repetition "%" :subcomponent "*"} 
                       {0 "MSH" 1 "field1"})
;; => "MSH#field1"
```

## Structure

- Top-level maps require key `0` for segment type
- Component maps use positive integer keys
- Vectors represent field repetitions
- Supports 3 nesting levels: fields (`|`)  components (`^`)  subcomponents (`&`)

## Tests

```bash
bb test
```

## License

Eclipse Public License
