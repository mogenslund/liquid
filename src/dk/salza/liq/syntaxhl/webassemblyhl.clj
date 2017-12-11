(ns dk.salza.liq.syntaxhl.webassemblyhl
  (:use [dk.salza.liq.slider :as slider :exclude [create]]))

(defn next-face
  [sl face]
  (let [ch (look-ahead sl 0)
        pch (or (look-behind sl 1) " ")
        ppch (or (look-behind sl 2) " ")]
    (cond (= face :string)  (cond (and (= pch "\"") (= ppch "\\")) face
                                  (and (= pch "\"") (string? (-> sl (left 2) (get-char)))) :plain
                                  (and (= pch "\"") (re-matches #"[^#\( \[{\n]" ppch)) :plain
                                  (and (= pch "\"") (re-matches #"[\)\]}]" (or ch " "))) :plain
                                  :else face)
          (= face :plain)   (cond (and (= ch "\"") (re-matches #"[#\( \[{\n]" pch)) :string
                                  (= ch ";") :comment
                                  (and (= ch "#") (or (= pch "\n") (= pch "") (= (get-point sl) 0))) :comment 
                                  (and (= pch "(") (re-find #"func|export" (string-ahead sl 13))) :type1
                                  (and (= pch "(") (re-find #"module|memory|param|result" (string-ahead sl 13))) :type3
                                  (and (= pch ":") (re-matches #"[\( \[{\n]" pch)) :type3
                                  :else face)
          (= face :type1)   (cond (= ch " ") :type2
                                  :else face)
          (= face :type2)   (cond (or (= ch " ") (= ch ")")) :plain
                                  :else face)
          (= face :type3)   (cond (re-matches #"[\)\]}\s]" (or ch " ")) :plain
                                  :else face)
          (= face :comment) (cond (= ch "\n") :plain
                                  :else face)
                            :else face)))