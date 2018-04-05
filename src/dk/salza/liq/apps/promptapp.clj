(ns dk.salza.liq.apps.promptapp
  (:require [dk.salza.liq.editor :as editor]
            [clojure.string :as str]))

(def state (atom {}))

(defn escape
  []
  (editor/set-keymap (@state :old-keymap))
  (editor/other-window))

(defn process-line
  []
  (let [line (editor/get-line)
        value (subs line (+ (count (nth (@state :parameterlabels) (@state :linenr))) 2))]   ; (pr-str (subs "abcdef" 6))
    (swap! state update :values conj value)
    (if (= (count (@state :parameterlabels)) (count (@state :values)))
        (do
          (escape)
          (editor/eval-safe #(apply (@state :function) (reverse (@state :values)))))
        (do
          (swap! state update :linenr inc)
          (editor/forward-line)
          (editor/end-of-line)))))

(def keymap
  {:cursor-color :green
   "right" editor/forward-char
   "left" editor/backward-char
   " " #(editor/insert " ")
   "\n" process-line
   "backspace" editor/delete
   "C-g" escape
   :selfinsert editor/insert})


(defn run
  [fun parameterlabels]
  (editor/switch-to-buffer "-prompt-")
  (swap! state assoc :old-keymap (editor/get-keymap)
                     :function fun
                     :parameterlabels parameterlabels
                     :linenr 0
                     :values '())
  (editor/set-keymap keymap) 
  (editor/end-of-buffer)
  (editor/insert (str "\n" (str/join "\n" (map #(str % ": ") parameterlabels))))
  (editor/beginning-of-line)
  (doseq [x (rest parameterlabels)]
    (editor/backward-line))
  (editor/end-of-line))