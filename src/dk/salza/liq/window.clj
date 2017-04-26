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
  [window buffer]
  (str (get-name window) "-" (get-buffername window)))