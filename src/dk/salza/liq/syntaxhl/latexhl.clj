(ns dk.salza.liq.syntaxhl.latexhl
  (:use [dk.salza.liq.slider :as slider :exclude [create]]))

(defn get-ahead
  [sl n]
  (apply str (map get-char (take n (iterate #(right % 1) sl)))))

(defn next-face
  [sl face]
  (let [ch (look-ahead sl 0)
        pch (look-behind sl 1)
        ppch (look-behind sl 2)]
    (cond (= face :string)  (cond (and (= pch "\"") (= ppch "\\")) face
                                  (and (= pch "\"") (string? (-> sl (left 2) (get-char)))) :plain
                                  (and (= pch "\"") (re-matches #"[^#\( \[{\n]" ppch)) :plain
                                  (and (= pch "\"") (re-matches #"[\)\]}]" (or ch " "))) :plain
                                  :else face)
          (= face :plain)   (cond (and (= ch "\"") (re-matches #"[#\( \[{\n]" pch)) :string
                                  (= ch ";") :comment
                                  (and (= ch "#") (or (= pch "\n") (= pch "") (= (get-point sl) 0))) :comment 
                                  (and (= pch "\n") (re-matches #"\\[a-zA-Z]+\{.*" (get-ahead sl 13))) :type1
                                  (and (= ch "\\") (re-matches #"\\[a-zA-Z]" (get-ahead sl 2))) :type3
                                  :else face)
          (= face :type1)   (cond (re-matches #"\{" ch) :type2
                                  :else face)
          (= face :type2)   (cond (= pch "}") :plain
                                  :else face)
          (= face :type3)   (cond (re-matches #"[^a-zA-Z]" ch) :plain
                                  :else face)
          (= face :comment) (cond (= ch "\n") :plain
                                  :else face)
                            :else face)))