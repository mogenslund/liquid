(ns user
  (:require [dk.salza.liq.editor :as editor]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [dk.salza.liq.tools.cshell :refer :all]))
;;  (:use [dk.salza.liq.tools.cshell]))

(defmacro c
  "Allows syntac like:
  (c git status).
  without quotes etc."
  [& args]
  (future
    (doseq [l (apply cmdseq (map str args))]
      (editor/prompt-append l)))
  nil)
