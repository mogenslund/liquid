(ns dk.salza.liq.core-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.adapters.ghostadapter :as ghostadapter]
            [dk.salza.liq.core :as core]))

(deftest a-test
  (testing "Nothing"
    (is (= 1 1))))

(defn- send-input
  [& syms]
  (doseq [s syms]
    (ghostadapter/send-input s)))

(deftest temporary
  (testing "Experiment with ghostadapter"
    (future (core/-main "--no-init-file" "--no-threads" "--ghost" "--rows=20" "--columns=80"))
    (send-input :g :g :v :G :d :d :tab :p :p :p :p :empty)
    (while (not (empty? @ghostadapter/input))
      (Thread/sleep 10))
    (println "DISPLAY" (ghostadapter/get-display))
    (ghostadapter/send-input :C-q)))