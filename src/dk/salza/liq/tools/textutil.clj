(ns dk.salza.liq.tools.textutil
  (:require [clojure.string :as str]))

(defn calc-context
  "Calculates a context object like
  a filepath, url, checkbox, code
  collapse and returns the type and
  the matched string.
  Output like {:type :file :value /tmp/tmp.txt}"
  [text col]
  (let [wordbegin (+ (.lastIndexOf (subs text 0 col) " ") 1)
        wordend (if (= (.indexOf (subs text col) " ") -1)
                    (count text)
                    (+ (.indexOf (subs text col) " ") col))
        word (subs text wordbegin wordend)]
    (cond (re-matches #"https?://.*" word) {:type :url :value word}
          (re-matches #";?#" word) {:type :fold :value "fold"}
          (re-matches #"\(.*" word) {:type :function :value (str/replace word #"(\(|\))" "")}
          (re-matches #"/.*" word) {:type :file :value word}
          :else {:type :file :value word})))