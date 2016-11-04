(ns dk.salza.liq.performance-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.util :as util]
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
