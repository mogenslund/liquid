(ns liq.window-manager
  (:require [clojure.string :as str]
            [liq.util :as util]
            [liq.editor :as editor]
            [liq.buffer :as buffer]))

(defn window-detach
  []
  (editor/apply-to-buffer
    (fn [buf] (assoc-in buf [::buffer/window ::buffer/group] (util/counter-next)))))

(defn window-frame-calc
  [left right top bottom]
  (let [totalrows ((editor/get-window) ::buffer/rows)
        totalcols ((editor/get-window) ::buffer/cols)
        abs-left (min (dec totalcols) (max 1 (if (integer? left) left (inc (int (* left totalcols))))))
        abs-right (max (inc abs-left) (min totalcols (if (integer? right) right (int (* right totalcols)))))
        abs-cols (- abs-right abs-left -1)
        abs-top (min (dec totalrows) (max 1 (if (integer? top) top (inc (int (* top totalrows))))))
        abs-bottom (max (inc abs-top) (min totalrows (if (integer? bottom) bottom (int (* bottom totalrows)))))
        abs-rows (- abs-bottom abs-top -1)]
    {:left abs-left
     :top abs-top
     :rows abs-rows
     :cols abs-cols}))
;; 1 or larger means absolute, 0-1 means relative.
;; One window with 0.25 as end and anthor with 0.25 as start should line up.
;; Can be 0.1 0.25 0.45 0.67

(defn window-set
  [left right top bottom]
  (let [g (-> (editor/current-buffer) ::buffer/window ::buffer/group)
        w (window-frame-calc left right top bottom)]
    (editor/apply-to-all-buffers
      (fn [buf]
        (if (= (-> buf ::buffer/window ::buffer/group) g)
          (-> buf
            (assoc-in [::buffer/window ::buffer/left] (w :left))
            (assoc-in [::buffer/window ::buffer/top] (w :top))
            (assoc-in [::buffer/window ::buffer/rows] (w :rows))
            (assoc-in [::buffer/window ::buffer/cols] (w :cols)))
          buf)))
    (editor/paint-all-buffer-groups)))

;; (window-set 0.0 0.5 0.0 0.5)
    

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
;; Preset rotation: Upper 1/4, 1/3, 1/2, 2/3, 3/4 1
;; Detach d
;; Esc to exit mode



