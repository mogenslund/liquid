(ns liq.extras.freemove-mode-test
  (:require [clojure.test :refer :all]
            [liq.buffer :as buffer]
            [liq.extras.freemove-mode :refer :all]))

(deftest trim-line-test
  ""
  (is (= (-> (buffer/buffer "aaa bbb") (trim-line 1) buffer/text) "aaa bbb"))
  (is (= (-> (buffer/buffer "aaa bbb  ") (trim-line 1) buffer/text) "aaa bbb"))
  (is (= (-> (buffer/buffer "  ") (trim-line 1) buffer/text) ""))
  (is (= (-> (buffer/buffer "") (trim-line 2) buffer/text) "")))

(deftest trim-buffer-test
  ""
  (is (= (-> (buffer/buffer "aaa bbb") trim-buffer buffer/text) "aaa bbb"))
  (is (= (-> (buffer/buffer "  aaa bbb  ") trim-buffer buffer/text) "  aaa bbb"))
  (is (= (-> (buffer/buffer "  aaa bbb  \n    \naa  ") trim-buffer buffer/text) "  aaa bbb\n\naa"))
  (is (= (-> (buffer/buffer "  aaa bbb  \n    \naa  ") trim-buffer buffer/end-of-buffer buffer/text) "  aaa bbb\n\naa"))
  (is (= (-> (buffer/buffer "  aaa bbb  \n    \naa  ") buffer/end-of-buffer trim-buffer buffer/text) "  aaa bbb\n\naa"))
  (is (= (-> (buffer/buffer "  aaa bbb  \n    \naa  ") buffer/end-of-buffer trim-buffer buffer/right buffer/text) "  aaa bbb\n\naa")))

(deftest beginning-of-boundry-test
  ""
  (is (= (-> (buffer/buffer "aa  bbbb ccc dd") (buffer/right 6) beginning-of-boundry ::buffer/col) 5))
  (is (= (-> (buffer/buffer "aa bbbb ccc dd") (buffer/right 6) beginning-of-boundry ::buffer/col) 1)))

(deftest move-region-tmp-test
  ""
  (is (= (buffer/text
           (move-region-tmp (-> (buffer/buffer "abc") (buffer/right))
                            [{::buffer/row 1 ::buffer/col 1} {::buffer/row 1 ::buffer/col 3}] 0 1))
         " abc")))

(deftest move-right-test
  ""
  (is (= (-> (buffer/buffer "abc") (buffer/right) move-right ::buffer/cursor ::buffer/col) 3))
  (is (= (-> (buffer/buffer "abc") (buffer/right) move-right buffer/text) " abc"))
  (is (= (-> (buffer/buffer "abc") (buffer/right) move-right move-right ::buffer/cursor ::buffer/col) 4))
  (is (= (-> (buffer/buffer "abc") (buffer/right) move-right move-right buffer/text) "  abc")))