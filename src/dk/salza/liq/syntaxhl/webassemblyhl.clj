(ns dk.salza.liq.syntaxhl.webassemblyhl
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