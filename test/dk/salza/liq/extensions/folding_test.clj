(ns dk.salza.liq.extensions.folding-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.extensions.folding :refer :all]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.helper :refer :all]))

(deftest get-headline-level-test
  (testing "Getting headline levels"
    (let [sl (create "## aaa\nbbb\n# c\n #d")]
      (is (= (get-headline-level sl) 2))
      (is (= (get-headline-level (-> sl (right 6))) 2))
      (is (= (get-headline-level (-> sl (right 7))) 0))
      (is (= (get-headline-level (-> sl (right 11))) 1))
      (is (= (get-headline-level (-> sl (right 15))) 0)))))
