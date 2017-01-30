(ns dk.salza.liq.core-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.adapters.ghostadapter :as ghostadapter]
            [dk.salza.liq.core :as core]))

(deftest a-test
  (testing "Nothing"
    (is (= 1 1))))

(deftest temporary
  (testing "Experiment with ghostadapter"
    (future (core/-main "--no-init-file" "--ghost"))
    ;(Thread/sleep 200) (ghostadapter/set-input :tab)
    (Thread/sleep 200) (ghostadapter/set-input :g)
    (Thread/sleep 200) (ghostadapter/set-input :g)
    (Thread/sleep 200) (ghostadapter/set-input :v)
    (Thread/sleep 200) (ghostadapter/set-input :G)
    (Thread/sleep 200) (ghostadapter/set-input :d)
    (Thread/sleep 200) (ghostadapter/set-input :d)
    (Thread/sleep 200) (ghostadapter/set-input :tab)
    (Thread/sleep 200) (ghostadapter/set-input :p)
    (Thread/sleep 200) (ghostadapter/set-input :p)
    (Thread/sleep 200) (ghostadapter/set-input :p)
    (Thread/sleep 200) (ghostadapter/set-input :p)
    (Thread/sleep 200)
    (println "DISPLAY" (ghostadapter/get-display))
    (Thread/sleep 200)
    (ghostadapter/set-input :C-q)
  ))