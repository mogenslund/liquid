(ns dk.salza.liq.adapters.ghostadapter
  (:require [dk.salza.liq.util :as util]
            [dk.salza.liq.keys :as keys]
            [clojure.string :as str]))

(def display (atom '()))

(def input (atom nil))

(defn set-input
  [inp]
  (println "set-input" inp)
  (reset! input inp))

(defn get-display
  []
  (println "get-display")
  @display)

(defn rows
  []
  20)

(defn columns
  []
  80)

(defn wait-for-input
  []
  (loop []
    (println "Waiting")
    (if @input
      (let [res @input]
        (reset! input nil)
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

(def adapter {:init init
              :rows rows
              :columns columns
              :wait-for-input wait-for-input
              :print-lines print-lines
              :reset reset
              :quit quit})
