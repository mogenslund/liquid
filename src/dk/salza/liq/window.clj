(ns dk.salza.liq.window
  (:require [dk.salza.liq.buffer :as buffer]
            [dk.salza.liq.tools.fileutil :as futil])
  (:use [dk.salza.liq.slider :as slider :exclude [create]]))

(defn create
  [name top left rows columns buffername]
  {::name name
   ::top top
   ::left left
   ::rows rows
   ::columns columns
   ::buffername buffername})

(defn get-buffername
  [window]
  (window ::buffername))

(defn get-name
  [window]
  (window ::name))

(defn get-towid
  "Top-of-window id"
  [window buffer]
  (str (get-name window) "-" (get-buffername window)))

(defn resize-width
  [window delta]
  (if (< (+ (window ::columns) delta) 3)
    window
    (update window ::columns #(+ % delta))))

(defn resize-height
  [window delta]
  (if (< (+ (window ::rows) delta) 3)
    window
    (update window ::rows #(+ % delta))))