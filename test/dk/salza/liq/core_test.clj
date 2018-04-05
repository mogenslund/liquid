(ns dk.salza.liq.core-test
  (:require [clojure.test :refer :all]
            [dk.salza.liq.adapters.ghostadapter :as ghostadapter]
            [clojure.string :as str]
            [dk.salza.liq.core :as core]))

(defn- send-input
  [& syms]
  (doseq [s syms]
    (Thread/sleep 20)
    (if (re-matches #"(C-)?(M-)?\w" s)
      (ghostadapter/send-input s)
      (doseq [c s] (ghostadapter/send-input (str c))))))

(defn- short-screen-notation
  "This function takes the lines which are created
  by window/render and comprimises (with a little loss) the
  content to a smaller string.
  The point is just to have something simple to check, when
  checking the rendered window in a test check.
  The output is a string with normal text, where special codes
  are just small substring of length 3, starting with ¤.
  For example a linebreak will is \"¤BR\" and plain text
  with cursor1 background is \"¤PG\"."
  [lines]
  (str/join "¤BR"
    (for [line lines]
      (reduce str (format "¤%02d" (line :column)) 
        (for [c (line :line)]
          (str
            (if (map? c)
              (str "¤"
                   (cond (= (c :face) :plain) "P"
                         (= (c :face) :type1) "1"
                         (= (c :face) :type2) "2"
                         (= (c :face) :type3) "3"
                         (= (c :face) :comment) "C"
                         (= (c :face) :string) "S"
                         :else "?")
                   (cond (= (c :bgface) :plain) "P"
                         (= (c :bgface) :cursor1) "G"
                         (= (c :bgface) :cursor2) "B"
                         (= (c :bgface) :selection) "S"
                         (= (c :bgface) :statusline) "L"
                         :else "?")
                    (or (c :char) ""))
                 c)))))))

(defn- screen-check
  "Takes some input, as a list of text snippets and
  keywords corresponding to input characers, and a
  string corresponding to some subcontent of the expected
  screenoutput comprised with the short-screen-notation.
  For example:
  (screen-check [:enter \"abc\" :tab] \"¤BRabc¤PB \")
  will be a success."
  [input expected]
  (let [program (future (core/-main "--no-init-file" "--no-threads" "--ghost" "--rows=20" "--columns=190"))]
    (Thread/sleep 100)
    (send-input "ggvGdd" "\t") ; Clearing screen. Ready to type
    (apply send-input input)
    (Thread/sleep 100)
    ;(while (not (empty? @ghostadapter/input)) (Thread/sleep 10))
    (let [windowcontent (apply concat (ghostadapter/get-display))]
;      (println "DISPLAY E:" expected)
;      (println "DISPLAY 1:" (str/replace (short-screen-notation windowcontent) #"BR" "\n"))
      (is (.contains (short-screen-notation windowcontent) expected)))))

(deftest defn-highlight
  (testing "Checking highlight of defn"
    (screen-check [" (defn myfun" "\n" " []" "\n" " (do))"]
                  "¤44 (¤1Pdefn¤2P myfun¤BR¤44¤PP []¤BR¤44 (do))"))) 

(deftest reproduce-findfile-slash
  (testing "Reproduce error when typing /a in findfile mode"
    (screen-check ["C-o" "C-f" "/" "a"]
                  "findfile")))

; (deftest temporary
;   (testing "Experiment with ghostadapter"
;     (future (core/-main "--no-init-file" "--no-threads" "--ghost" "--rows=20" "--columns=90"))
;     (send-input "ggvGdd" :tab "(defn aaa")
;     (while (not (empty? @ghostadapter/input))
;       (Thread/sleep 10))
;     (let [windowcontent (ghostadapter/get-display)]
;       (println windowcontent)
;       (println "DISPLAY:" (short-screen-notation windowcontent)))
;     (ghostadapter/send-input :C-q)))