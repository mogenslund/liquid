(ns dk.salza.liq.performance-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.helper :refer :all]))

(deftest slider-performance-test
  (testing "Performance of slider"
    (let [sl (create)
          t0 (util/now)
          sl0 (nth (iterate #(insert % "a") sl) 120000)
          sl1 (nth (iterate #(left % 1) sl0) 30000)
          sl2 (nth (iterate #(insert % "b\n") sl1) 50000)
          sl3 (beginning sl2)
          sl4 (nth (iterate #(forward-line % 10) sl3) 1000)]
      (is (< (- (util/now) t0) 1000))))) ; Expected 625 ms

(deftest slider-performance-test-visual
  (testing "Performance of slider with forward-visual-line"
    (let [sl (create)
          t0 (util/now)
          sl0 (nth (iterate #(insert % "a") sl) 120000)
          sl1 (nth (iterate #(left % 1) sl0) 30000)
          sl2 (nth (iterate #(insert % "b\n") sl1) 50000)
          sl3 (beginning sl2)
          sl4 (nth (iterate #(forward-visual-column % 10 1) sl3) 200)]
      (is (< (- (util/now) t0) 1500))))) ; Expected 1100 ms