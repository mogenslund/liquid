(ns liq.extras.markdownfolds-test
  (:require [clojure.test :refer :all]
            [liq.buffer :as buffer]
            [liq.extras.markdownfolds :refer :all]))

(deftest get-headline-level-test
  ""
  (is (= (get-headline-level (buffer/buffer "# abc") 1) 1))
  (is (= (get-headline-level (buffer/buffer "## abc") 1) 2))
  (is (= (get-headline-level (buffer/buffer "abc\n## abc") 2) 2))
  (is (= (get-headline-level (buffer/buffer "\n## abc") 2) 2))
  (is (= (get-headline-level (buffer/buffer "\n") 2) 0))
  (is (= (get-headline-level (buffer/buffer "abc") 1) 0))
  (is (= (get-headline-level (buffer/buffer "") 1) 0))
  (is (= (get-headline-level (buffer/buffer "abc") 2) 0))
  (is (= (get-headline-level (buffer/buffer "abc") 0) 0)))

(deftest get-level-end-test
  ""
  (is (= (get-level-end (buffer/buffer "# abc\nabc\n# def") 1) 2))
  (is (= (get-level-end (buffer/buffer "# abc\nabc") 1) 2))
  (is (= (get-level-end (buffer/buffer "# abc\n# abc") 1) 1))
  (is (= (get-level-end (buffer/buffer "# abc\n# abc\nabc") 1) 1))
  (is (= (get-level-end (buffer/buffer "# abc\n\n# abc") 1) 2))
  (is (= (get-level-end (buffer/buffer "# abc") 1) 1))
  (is (= (get-level-end (buffer/buffer "# aaa\n# aaa\n## bb\n123\n# ccc\n# ddd") 3) 4)))

(deftest hide-level-test
  ""
  (let [buf (buffer/buffer "# abc\ndef")]
    (is (= (buf ::buffer/hidden-lines) {}))
    (is (= (-> buf buffer/left hide-level ::buffer/hidden-lines) {2 2}))))