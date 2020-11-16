(ns liq.window-manager
  (:require [clojure.string :as str]
            [liq.util :as util]
            [liq.editor :as editor]
            [liq.buffer :as buffer]))

(defn window-detach
  []
  (editor/apply-to-buffer
    (fn [buf] (assoc-in buf [::buffer/window ::buffer/group] (util/counter-next)))))

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

(defn window-resize-horizontal
  [amount]
  (let [g (-> (editor/current-buffer) ::buffer/window ::buffer/group)]
    (editor/apply-to-all-buffers
      (fn [buf]
        (if (= (-> buf ::buffer/window ::buffer/group) g)
          (-> buf
            (update-in [::buffer/window ::buffer/cols] #(+ % amount))
            (update-in [::buffer/window ::buffer/left] #(+ % (if (= % 1) 0 (- amount)))))
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

(defn window-right
  []
  (let [buf (editor/current-buffer)
        bufb (first
               (filter #(and (> (-> % ::buffer/window ::buffer/left) (-> buf ::buffer/window ::buffer/left))
                             (not= (% ::buffer/name) "*minibuffer*")
                             (not= (% ::buffer/name) "*status-line*"))
                       (editor/all-buffers)))]
    (when bufb (editor/switch-to-buffer (bufb ::editor/id)))))

(defn window-left
  []
  (let [buf (editor/current-buffer)
        bufb (first
               (filter #(and (< (-> % ::buffer/window ::buffer/left) (-> buf ::buffer/window ::buffer/left))
                             (not= (% ::buffer/name) "*minibuffer*")
                             (not= (% ::buffer/name) "*status-line*"))
                       (editor/all-buffers)))]
    (when bufb (editor/switch-to-buffer (bufb ::editor/id)))))

(defn init
 []
 (editor/apply-to-buffer
  (fn [buf]
    (-> buf
        (buffer/set-normal-mode)
        (update ::buffer/major-modes #(conj % :window-arrange-mode))))))

(defn abort
  []
  (editor/apply-to-buffer
    (fn [buf]
      (update buf ::buffer/major-modes (fn [x] (filter #(not= % :window-arrange-mode) x))))))

(def window-arrange-mode
  {:normal {"esc" abort 
            "d" :window-detach
            "K" :window-smaller
            "J" :window-larger}
            
   :syntax
     {:plain
       {:style :green}}})
;; Move window hjkl
;; Resize window HJKL
;; Special functions to full, top-half, buttom-half, right-half, left-half. C-h, C-j, C-k, C-l
;; Detach d
;; Esc to exit mode



