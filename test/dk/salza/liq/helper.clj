(ns dk.salza.liq.helper
  (:require [clojure.string :as str]))


(defn normalize-slider
  "Takes a slider an outputs the content as
  something like aaaanaa-¤aaaa-aana|aaaaa where
  a = char (chars are not translated to a)
  n = newline
  | = cursor position
  - = space
  ¤ = Syntax highlighting"
  [slider]
  (-> (apply str (concat (reverse (slider :dk.salza.liq.slider/before))
                         '("|")
                         (slider :dk.salza.liq.slider/after)))
      (str/replace #"\n" "n")
      (str/replace #" " "-")))