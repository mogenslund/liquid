(ns dk.salza.liq.adapters.ghostadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.window :as window]
            [clojure.string :as str]))

(defn send-input
  [inp]
  (editor/handle-input inp))

(defn get-display
  []
  (let [windows (reverse (editor/get-windows))
        buffers (map #(editor/get-buffer (window/get-buffername %)) windows)
        lineslist (doall (map #(window/render %1 %2) windows buffers))]
    lineslist))
