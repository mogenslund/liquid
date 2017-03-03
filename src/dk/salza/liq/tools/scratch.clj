(ns dk.salza.liq.tools.scratch
  (:require [clojure.zip :as zip]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;;;; Also do slider with marks. Marks should not be of fixed type. Probably not needed.
;;;; It should be possible just to drag out the slider, if the marks are not needed.
;;;; Slider should just automatically have marks: ['("a") '("b") {:mark1 10 :mymark 20}]
;;

;(println (str/replace (slurp "https://en.wikipedia.org/wiki/Test_method") #"<[^>]*>" ""))
(defn www
  []
  (-> "https://en.wikipedia.org/wiki/Turtle"
      slurp
      (str/split #"<body[^>]*>" 2)
      (second)
      (str/replace #"<br[^>]*>" "\n")
      (str/replace #"<h1[^>]*>" "\n# ")
      (str/replace #"<h2[^>]*>" "\n## ")
      (str/replace #"<h3[^>]*>" "\n### ")
      (str/replace #"<h4[^>]*>" "\n##### ")
      (str/replace #"<h5[^>]*>" "\n###### ")
      (str/replace #"</h1[^>]*>" "\n")
      (str/replace #"</h2[^>]*>" "\n")
      (str/replace #"</h3[^>]*>" "\n")
      (str/replace #"</h4[^>]*>" "\n")
      (str/replace #"</h5[^>]*>" "\n")
      (str/replace #"<li[^>]*>" " * ")
      (str/replace #"<[^>]*>" "")
      (str/replace #"\n\n\n+" "\n\n")
      println))

(defn filter-headlines
  []
  (let [content "abcde\n# Headline 1\n(defn some-fun\nabcd\ndeft\n## Headline2"
        lines (->> content (str/split-lines) (filter #(re-find #"^(\(defn|#)" %)))]
    (doseq [l lines]
      (println "!!" l))))

(filter-headlines)
