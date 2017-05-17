(ns dk.salza.liq.renderer-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.renderer :refer :all]
            [dk.salza.liq.helper :refer :all]
            [dk.salza.liq.window :as window]
            [dk.salza.liq.editor :as editor]))

(defn get-first-line
  "Helper returns first rendered line"
  []
  ;(doseq [l (first (render-screen))]
  ;  (println l))
  (clojure.string/join (filter #(not (map? %)) ((first (first (render-screen))) :line))))

(defn reset-editor
  "Resets the editor and inserts an empty buffer with 6 real lines"
  []
  (editor/reset)
  (editor/add-window (window/create "win1" 1 1 6 40 "buffer1"))
  (editor/new-buffer "buffer1"))

;; lein test :only dk.salza.liq.renderer-test/basic-renderer-test
(deftest basic-renderer-test
  (testing "Creating editor and simple buffer insert"
    (reset-editor)
    (editor/insert "1abc\n2abc\n3abc\n4abc\n5abc\n6abc\n7abc")
    (editor/beginning-of-buffer)
    (is (= (get-first-line) "1abc"))
    (dotimes [n 5] (editor/forward-line))
    (is (= (get-first-line) "1abc"))
    (editor/forward-line)
    (is (= (get-first-line) "6abc"))
    ))
