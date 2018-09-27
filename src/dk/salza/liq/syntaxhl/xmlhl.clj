(ns dk.salza.liq.syntaxhl.xmlhl
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
                                  (and (= ch "#") (or (= pch "\n") (= (get-point sl) 0))) :comment 
                                  (= ch "<") :type1
                                  (= ch ">") :type1
                                  (and (= ch ":") (re-matches #"[\( \[{\n]" pch)) :type3
                                  :else face)
          (= face :type1)   (cond (= pch "<") :type2
                                  (and (= pch ">") (= ch "<")) :type1
                                  :else :plain)
          (= face :type2)   (cond (= ch " ") :plain
                                  (= ch ">") :type1
                                  :else face)
          (= face :type3)   (cond (re-matches #"[\)\]}\s]" (or ch " ")) :plain
                                  :else face)
          (= face :comment) (cond (= ch "\n") :plain
                                  :else face)
                            :else face)))