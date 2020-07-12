(ns rbg.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [rbg.core :as rbg]))

(deftest hsl-str-validation
  (testing "hsl-str validation"
    (testing "throws"
      (are [h s l] (thrown? AssertionError (rbg/hsl-str h s l))
        360 0 0
        0 101 0
        0 0 101))
    (testing "succeeds"
      (are [h s l] (string? (rbg/hsl-str h s l))
        0 0 0
        359 100 100
        237 50 87))))

(deftest key->str-test
  (is (= (rbg/key->str :adam) "adam")
      "':' char is not included in string result."))

(deftest rule->css-validation-test
  (is (thrown? AssertionError (rbg/rule->css [:p]))))

(deftest my-css-test
  (is (= (rbg/my-css [:tag {:attr "value"}])
         "tag {\n  attr: value;\n}"))
  (is (= (rbg/my-css (list [:tag1 {:a "val-a"}]
                           [:tag2 {:b "val-b"}]))
         "tag1 {\n  a: val-a;\n}\n\ntag2 {\n  b: val-b;\n}")))

(deftest gen-bg-validation-test
  (testing "gen-bg input validation"
    (is (thrown? AssertionError (rbg/gen-bg 10 10 1)) 
        "fails with sc < 5")
    (is (rbg/hiccup? (rbg/gen-bg 5 5 5))
        "succeeds with sc >= 5")))
