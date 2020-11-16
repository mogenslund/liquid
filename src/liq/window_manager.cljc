(ns liq.window-manager
  (:require [clojure.string :as str]
            [liq.util :as util]
            [liq.editor :as editor]
            [liq.buffer :as buffer]))

(defn window-resize-vertical
  [amount]
  (let [g (-> (editor/current-buffer) ::buffer/window ::buffer/group)]
    (editor/apply-to-all-buffers
      (fn [buf]
        (if (= (-> buf ::buffer/window ::buffer/group) g)
          (-> buf
            (update-in [::buffer/window ::buffer/rows] #(+ % amount))
            (update-in [::buffer/window ::buffer/top] #(+ % (if (= % 1) 0 (- amount)))))
          buf)))
    (editor/paint-all-buffer-groups)))

(defn window-below
  []
  (let [buf (editor/current-buffer)
        bufb (first
               (filter #(and (> (-> % ::buffer/window ::buffer/top) (-> buf ::buffer/window ::buffer/top))
                             (not= (% ::buffer/name) "*minibuffer*")
                             (not= (% ::buffer/name) "*status-line*"))
                       (editor/all-buffers)))]
    (when bufb (editor/switch-to-buffer (bufb ::editor/id)))))

(defn window-above
  []
  (let [buf (editor/current-buffer)
        bufb (first
               (filter #(and (< (-> % ::buffer/window ::buffer/top) (-> buf ::buffer/window ::buffer/top))
                             (not= (% ::buffer/name) "*minibuffer*")
                             (not= (% ::buffer/name) "*status-line*"))
                       (editor/all-buffers)))]
    (when bufb (editor/switch-to-buffer (bufb ::editor/id)))))
 