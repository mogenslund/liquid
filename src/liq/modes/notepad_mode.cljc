(ns liq.modes.notepad-mode
  (:require [clojure.string :as str]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer :refer [delete-region shrink-region set-insert-mode]]
            [liq.util :as util]))

(defn unselect-move
  [fun buf]
  (-> buf
      buffer/remove-selection
      fun))

(defn select-move
  [fun buf]
  (let [buf1 (if (buffer/get-selection buf)
               buf
               (buffer/set-selection buf))]
    (fun buf1)))

(defn notepad-copy
  [buf]
  (let [p (buffer/get-selection buf)
        text (buffer/get-selected-text buf)]
    (when p
      (util/set-clipboard-content text false))
    buf))

(defn notepad-paste
  [buf]
  (let [text (util/clipboard-content)] 
    (if text
      (-> buf
          (buffer/insert-string text)
          (buffer/right (count text))
          buffer/set-insert-mode)
      buf)))

(defn notepad-cut
  [buf]
  (-> buf
      notepad-copy 
      buffer/delete
      buffer/set-insert-mode))

(def mode
  {:insert {"C-c" #(editor/apply-to-buffer notepad-copy)
            "C-x" #(editor/apply-to-buffer notepad-cut)
            "C-v" #(editor/apply-to-buffer notepad-paste)
            "C-s" :w
            "C-o" :Ex
            "left" :unselect-left
            "S-left" :select-left
            "right" :unselect-right
            "S-right" :select-right
            "up" :unselect-up
            "S-up" :select-up
            "down" :unselect-down
            "S-down" :select-down
            "f5" :eval-sexp-at-point}
            
   :normal {}}) 
             
(defn load-notepad-mode
  []
  (editor/set-command :unselect-left #(editor/apply-to-buffer (partial unselect-move buffer/left))) 
  (editor/set-command :select-left #(editor/apply-to-buffer (partial select-move buffer/left))) 
  (editor/set-command :unselect-right #(editor/apply-to-buffer (partial unselect-move buffer/right))) 
  (editor/set-command :select-right #(editor/apply-to-buffer (partial select-move buffer/right))) 
  (editor/set-command :unselect-up #(editor/apply-to-buffer (partial unselect-move buffer/up))) 
  (editor/set-command :select-up #(editor/apply-to-buffer (partial select-move buffer/up)))
  (editor/set-command :unselect-down #(editor/apply-to-buffer (partial unselect-move buffer/down))) 
  (editor/set-command :select-down #(editor/apply-to-buffer (partial select-move buffer/down))) 
  (editor/add-mode :notepad-mode mode)
  (editor/add-new-buffer-hook (fn
                                [buf]
                                (if (= (subs (str (buf ::buffer/name) " ") 0 1) "*")
                                  buf
                                  (update buf ::buffer/major-modes conj :notepad-mode)))))
  