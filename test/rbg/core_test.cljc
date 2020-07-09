(ns rbg.core-test
  (:require [clojure.test :refer :all]
            [rbg.core :as rbg]))

(is (= 5 (+ 2 3)))

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

(deftest gen-bg-validation-test
  (testing "gen-bg input validation"
    (is (thrown? AssertionError (rbg/gen-bg 10 10 1)) 
        "fails with sc < 5")
    (is (rbg/hiccup? (rbg/gen-bg 5 5 5))
        "succeeds with sc >= 5")))
