(ns dk.salza.liq.helper
  (:require [clojure.string :as str]
            [dk.salza.liq.slider :refer :all]))


(defn normalize-slider
  "Takes a slider an outputs the content as
  something like aaaanaa-¤aaaa-aana|aaaaa where
  a = char (chars are not translated to a)
  n = newline
  | = cursor position
  - = space
  ¤ = Syntax highlighting"
  [sl]
  (-> (apply str (concat (-> sl before get-content reverse)
                         '("|")
                         (-> sl after get-content)))
      (str/replace #"\n" "n")
      (str/replace #" " "-")))