(ns dk.salza.liq.apps.mdbrowserapp
  (:require [clojure.java.io :as io]
            [dk.salza.liq.buffer :as buffer]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.editor :as editor]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.extensions.headlinenavigator]
            [dk.salza.liq.extensions.linenavigator]
            [dk.salza.liq.syntaxhl.clojuremdhl :as clojuremdhl]
            [dk.salza.liq.logging :as logging]
            [dk.salza.liq.coreutil :refer :all]))

(def navigate (atom nil))

(def keymap
  {:cursor-color :blue
   :M editor/prompt-to-tmp
   :enter #(@navigate)
   :tab #(editor/find-next "[")
   :space #(editor/forward-page)
   :right editor/forward-char
   :left editor/backward-char
   :up editor/backward-line
   :down editor/forward-line
   :C-s #(promptapp/run editor/find-next '("SEARCH"))
   :v editor/selection-toggle
   :g {:g editor/beginning-of-buffer
       :t editor/top-align-page
       :n editor/top-next-headline
       :c #(editor/prompt-append (str "--" (editor/get-context) "--"))
       :i dk.salza.liq.extensions.headlinenavigator/run
       :l dk.salza.liq.extensions.linenavigator/run}
   :dash editor/top-next-headline
   :C-g editor/escape
   :esc editor/escape
   :e editor/eval-last-sexp
   :E editor/evaluate-file
   :C-e editor/evaluate-file-raw
   :l editor/forward-char
   :j editor/backward-char
   :i editor/backward-line
   :k editor/forward-line
   :J editor/beginning-of-line
   :G editor/end-of-buffer
   :L editor/end-of-line
   :m editor/previous-real-buffer 
   :n editor/find-next
   :O editor/context-action
   :w editor/forward-word
   :1 editor/highlight-sexp-at-point
   :2 editor/select-sexp-at-point
   :y {:y #(do (or (editor/copy-selection) (editor/copy-line)) (editor/selection-cancel))}
   :C-w editor/kill-buffer
   :C-t (fn [] (editor/tmp-test))
   })

(defn markdown-link
  "Takes a slider and tries to resolve the context
  in relation to markdown."
  [sl0]
  (let [
    link
      (loop [state {:sl sl0 :link1 "" :link2 "" :state 1}]
        (let [sl (state :sl)
              st (state :state)
              c (get-char sl)]
          (cond (= st 1) (cond (or (= c "\n") (beginning? sl)) ((get-context sl0) :value)
                               (= c "[") (recur (assoc state :state 2 :sl (right sl 1)))
                               true (recur (assoc state :sl (left sl 1))))
                (= st 2) (cond (or (= c "\n") (end? sl)) ((get-context sl0) :value)
                               (= c "]") (recur (assoc state :state 3 :sl (right sl 1)))
                               true (recur (assoc state :link1 (str (state :link1) c) :sl (right sl 1))))
                (= st 3) (cond (= c "(") (recur (assoc state :state 4 :sl (right sl 1)))
                               (= c "[") (recur (assoc state :state 5 :sl (right sl 1)))
                               true (state :link1))
                (= st 4) (cond (or (= c "\n") (end? sl)) (state :link1) 
                               (= c ")") (state :link2)
                               true (recur (assoc state :link2 (str (state :link2) c) :sl (right sl 1))))
                (= st 5) (cond (or (= c "\n") (end? sl)) (state :link1) 
                               (= c "]") (state :link2)
                               true (recur (assoc state :link2 (str (state :link2) c)
                                                        :sl (right sl 1)))))))]
    (or (re-find (re-pattern (str "(?<=\\[" link "\\]: )[^\n]+")) (get-content sl0)) link)))


;; Links:
;; [direct.md]
;;
;; [My Link][1]
;; [1]: Path to link

;; :enter should open link in mdbrowserapp
;; :tab Move cursor to next link
(defn run
  "TODO: Implementation not finished yet.
  App to browse files written in markdown.
  TODO: Support browsing webpages by converting to
  markdown."
  [filepath]
  (if (editor/get-buffer filepath)
    (editor/switch-to-buffer filepath)
    (do
      (editor/create-buffer-from-file filepath)
      (editor/set-keymap keymap)
      (editor/set-highlighter clojuremdhl/next-face))))

(reset! navigate
  (fn []
    (let [sl (buffer/get-slider (editor/current-buffer))
          link (markdown-link sl)]
      (logging/log "MDBROWSERLINK" link)
      (if (.isAbsolute (io/file link))
        (run link)
        (run (str (io/file (editor/get-folder) link)))))))