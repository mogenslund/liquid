(ns dk.salza.liq.syntaxhl.latexhl
  (:require [dk.salza.liq.slider :refer :all :rename {create create-slider}]))

(defn next-face
  [sl face]
  (let [ch (get-char sl)
        pch (-> sl left get-char)
        ppch (-> sl (left 2) get-char)] 
    (cond (= face :stringst) :string
          (= face :string)  (cond (and (= pch "\"") (= ppch "\\")) face
                                  (= pch "\"") :plain
                                  :else face)
          (= face :plain)   (cond (and (= ch "\"") (not= (get-char (right sl)) "\n")) :stringst
                                  (= ch ";") :comment
                                  (and (= ch "#") (or (= pch "\n") (= pch "") (= (get-point sl) 0))) :comment 
                                  (and (= pch "\n") (re-matches #"\\[a-zA-Z]+\{.*" (string-ahead sl 13))) :type1
                                  (and (= ch "\\") (re-matches #"\\[a-zA-Z]" (string-ahead sl 2))) :type3
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