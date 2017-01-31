(ns dk.salza.liq.adapters.ghostadapter
  (:require [dk.salza.liq.util :as util]
            [dk.salza.liq.keys :as keys]
            [clojure.string :as str]))

(def display (atom '()))

(def input (atom (clojure.lang.PersistentQueue/EMPTY)))

(defn send-input
  [inp]
  ;(println "send-input" inp)
  (swap! input conj inp))

(defn get-display
  []
  ;(println "get-display")
  @display)

;(defn rows
;  []
;  20)

;(defn columns
;  []
;  80)

(defn wait-for-input
  []
  (loop []
    ;(println "Waiting")
    (if (not (empty? @input))
      (let [res (peek @input)]
        ;(println "read input" res)
        (swap! input pop)
        res)
      (do
        (Thread/sleep 100)
        (recur)))))

(defn print-lines
  [lines]
  (reset! display lines))

(defn reset
  []
  (reset! display '()))

(defn init
  []
  )

(defn quit
  []
  (System/exit 0))

(defn adapter
  [rows columns]
  {:init init
   :rows (fn [] rows)
   :columns (fn [] columns)
   :wait-for-input wait-for-input
   :print-lines print-lines
   :reset reset
   :quit quit})