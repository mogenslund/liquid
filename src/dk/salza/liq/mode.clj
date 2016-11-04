(ns dk.salza.liq.mode
  (:require [dk.salza.liq.coreutil :refer :all]))

(defn create
  [name]
  {::name name
   ::actionmapping '({} {}) ; use bump function to bring another to front
   ::syntax-highlighter identity ; Function that takes a line and applies highlight
   })

(defn swap-actionmapping
  [mode]
  (update mode ::actionmapping bump 1))

(defn set-action
  [mode keyw fun]
  (update mode ::actionmapping doto-first assoc keyw fun))

(defn set-actions
  [mode actions]
  (update mode ::actionmapping doto-first merge actions))

(defn get-action
  [mode keyw]
  (-> mode first ::actionmapping keyw))

(defn set-highlighter
  [mode highlighter]
  (assoc-in mode [::syntax-highlighter] highlighter))

(defn highlight
  [mode line]
  ((mode ::syntax-highlighter) line))
