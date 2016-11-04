(ns dk.salza.liq.apis)

(defprotocol Adapter
  (init [this])
  (rows [this])
  (columns [this])
  (wait-for-input [this])
  (print-lines [this lines])
  (reset [this])
  (quit [this]))

(defprotocol Mode
  (run [this]))