# HL7 Builder

[![Tests](https://github.com/HealthSamurai/hl7-serializer/actions/workflows/test.yml/badge.svg)](https://github.com/HealthSamurai/hl7-serializer/actions/workflows/test.yml)

Clojure library for building and parsing HL7v2 segments with structured data.

## Installation

Add to your `deps.edn`:

```clojure
{:deps {com.github.healthsamurai/hl7-serializer {:sha "<latest-commit-sha>"}}}
```

## Usage

### Building Segments

```clojure
(require '[hl7-builder.core :as hl7])

;; MSH segment
(hl7/build-segment-str {0 "MSH" 1 "|" 2 "^~\\&" 3 "SENDER" 4 "RECEIVER"})
;; => "MSH|^~\\&|SENDER|RECEIVER"

;; PID segment with name components
(hl7/build-segment-str {0 "PID" 5 {1 "Doe" 2 "John"}})
;; => "PID|||||Doe^John"

;; OBX segment with repetitions
(hl7/build-segment-str {0 "OBX" 1 "1" 2 "ST" 4 ["Result1" "Result2"]})
;; => "OBX|1|ST||Result1~Result2"
```

### Parsing Segments

```clojure
(require '[hl7-builder.parse :as parse])

;; Parse MSH segment
(parse/parse-segment-str "MSH|^~\\&|SENDER|RECEIVER")
;; => {0 "MSH" 1 "|" 2 "^~\\&" 3 "SENDER" 4 "RECEIVER"}

;; Parse PID segment
(parse/parse-segment-str "PID|||||Doe^John")
;; => {0 "PID" 5 {1 "Doe" 2 "John"}}

;; Parse OBX segment
(parse/parse-segment-str "OBX|1|ST||Result1~Result2")
;; => {0 "OBX" 1 "1" 2 "ST" 4 ["Result1" "Result2"]}
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
