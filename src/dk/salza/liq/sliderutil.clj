(ns dk.salza.liq.sliderutil
  (:require [clojure.string :as str])
  (:use [dk.salza.liq.slider]))


(defn sexp-at-point-deprecated
  [sl]
  (when-let [sl0 (left-until sl #"\(")]
    (loop [s (-> sl0 (set-mark "sexpstart") (right 1)) level 1]
      (let [ch (get-char s)
            nextlevel (cond (= ch "(") (inc level)
                            (= ch ")") (dec level)
                            :else level)]
        (cond (= nextlevel 0) (get-region (right s 1) "sexpstart")
              (end? s) nil
              :else (recur (right s 1) nextlevel))))))

(defn sexp-at-point
  [sl]
  (let [sl0 (-> sl (mark-paren-start "paren-start") (mark-paren-end "paren-end"))]
    (if (and (get-mark sl0 "paren-start") (get-mark sl0 "paren-end"))
      (-> sl0 (point-to-mark "paren-start") (get-region "paren-end"))
      nil)))


(defn get-context
  "Calculates a context object like
  a filepath, url, checkbox, code
  collapse and returns the type and
  the matched string.
  Output like {:type :file :value /tmp/tmp.txt}"
  [sl]
  (let [sl0 (-> sl (left-until #"[^ \n\"]")
                   (left-until #"[ \n\"]")
                   (right-until #"[\w(\[/]")
                   (set-mark "contextstart")) ;(right 1) (set-mark "contextstart")) (re-find #"\w" "  x")
        sl1 (-> sl0 (right-until #"[ |\n|\"]"))
        word (get-region sl1 "contextstart")]
    (cond (re-matches #"https?://.*" word) {:type :url :value word}
          (re-matches #";?#" word) {:type :fold :value "fold"}
          (re-matches #"\(.*" word) {:type :function :value (str/replace word #"(\(|\))" "")}
          (re-matches #"[-a-z0-9\.]+/[-a-z0-9]+" word) {:type :function :value (str/replace word #"(\(|\))" "")}
          (re-matches #"/.*" word) {:type :file :value word}
          (re-matches #".*(data:image/png;base64,.*)" word) {:type :base64image :value (re-find #"(?<=base64,)[^\)]*" word)}
          :else {:type :file :value word})))