(ns dk.salza.liq.apps.keynavigateapp
  (:require [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.editor :as editor]
            [clojure.string :as str]))

(def ^:private state (atom {}))

(def sample {
  "m" {:info "Some commands"
       "e" {:info "Evaluation"
            "e" {:info "eval-last-sexp" :action editor/eval-last-sexp}
            "b" {:info "eval-buffer" :action editor/evaluate-file}}}
  })

(defn update-display
  []
  (let [ks (filter string? (keys @state))
        out (map #(str % ": " ((@state %) :info)) ks)]
    (editor/clear)
    (editor/insert (str/join "\n" out))))

(defn apply-key
  [c]
  (if (contains? @state c)
    (let [res (@state c)]
      (if (contains? res :action)
        (do
          (editor/previous-real-buffer-same-window) 
          ((res :action)))
        (do
          (reset! state res)
          (update-display))))
    (editor/previous-real-buffer-same-window)
  ))

(def ^:private keymap
  {:cursor-color :blue
   "C-g" editor/previous-real-buffer-same-window
   "esc" editor/previous-real-buffer-same-window
   ;"backspace" delete-char
   :selfinsert apply-key
   })

(defn run
  [annotated-keybindings]
  (reset! state annotated-keybindings)
  (editor/new-buffer "-keynavigateapp-")
  (editor/set-keymap keymap)
  (update-display))
