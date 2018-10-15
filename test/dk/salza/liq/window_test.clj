(ns dk.salza.liq.window-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [dk.salza.liq.slider :as slider]
            [dk.salza.liq.buffer :as buffer]
            [dk.salza.liq.window :refer :all]))


;; visualize: aaa-aa-aaanaaaaaa-aa|aaa-¤aaan
;;            a = char
;;            - = space
;;            n = newline
;;            | = cursor
;;            ¤ = syntax hl

;; window:    columns = 7, rows = 4
;; sample:    aaaaaaa|aa-aa-aaaanaaaaa -> {::before ("a" "a" "a" "a" "a" "a "a") ::after ("a" "a" " "....}
;; rendered:  aaaaaaan|aa-aa-naaaanaaaaa
;;            #######
;;            aaaaaaa
;;            aa aa
;;            aaaa
;;            aaaaa

(defn create-buffer
  [before after]
  (-> (buffer/create "mybuffer")
      (buffer/apply-to-slider #(slider/insert % after))
      (buffer/apply-to-slider slider/beginning)
      (buffer/apply-to-slider #(slider/insert % before))))
  

(defn visualize
  [buffer]
  (-> (apply str (concat (reverse ((buffer/get-slider buffer) ::slider/before))
                         '("|")
                         ((buffer/get-slider buffer) ::slider/after)))
      (str/replace #"\n" "n")
      (str/replace #" " "-")))

(defn tmp-test
  []
  (let [buffer (create-buffer "aa aa\naaa aa" "aaa")]
    (println (visualize buffer))))

;(tmp-test)

;(deftest insert-token-test
;  (let [sl (-> (slider/create "abc\ndef") (slider/right 2))]
;    ;(println (-> sl (insert-token :xyz) slider/get-visible-content))
;    (testing "Insert token"
;      (is (= (-> sl (insert-token :xyz) slider/get-visible-content)
;             "ab¤c\ndef")))))

;(deftest render-test
;  (let [buf (-> (buffer/create "mybuffer") (buffer/insert "abc"))
;        window (create "mywindow" 1 1 4 6 "mybuffer")]     ; name top left rows columns buffername
;    (testing "Initial rendering"
;      (is (= ((first (render window buf)) :line)
;             (list "a" "b" "c" {:face :plain, :bgface :cursor2} " " {:face :plain, :bgface :plain})))
;      (is (= ((second (render window (buffer/insert buf "\n"))) :line)
;             (list {:face :plain, :bgface :cursor2} " " {:face :plain, :bgface :plain})))
;    )))