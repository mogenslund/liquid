(ns liq.extras.html-tool-test
  (:require [clojure.test :refer :all]
            [liq.modes.help-mode :as help-mode]
            [liq.buffer :as buffer]
            [liq.extras.html-tool :refer :all]))

(deftest to-html-test
  ""
  (is (= (count (to-html (buffer/buffer "aaa") (help-mode/mode :syntax))) 121))
  (is (to-html (buffer/buffer "\n") (help-mode/mode :syntax)))
  (is (to-html (buffer/buffer "a\n") (help-mode/mode :syntax)))
  (is (to-html (buffer/buffer "a\n\n\n") (help-mode/mode :syntax)))
  (is (= (count (to-html (buffer/buffer "") (help-mode/mode :syntax))) 118)))
