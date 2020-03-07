(ns liq.extras.snake-mode
  (:require [clojure.string :as str]
            [liq.editor :as editor]
            [liq.buffer :as buffer]
            [liq.util :as util]))

(def state (atom
  {:next-move :right
   :cols 30
   :rows 20
   :history (list {::buffer/row 10 ::buffer/col 18} {::buffer/row 10 ::buffer/col 18})
   :length 2}))

(defn place-dot
  []
  (let [buf (editor/get-buffer "*snake*")]
    (loop [p {::buffer/row (+ (rand-int (@state :rows)) 2) ::buffer/col (+ (rand-int (@state :cols)) 2)}]
      (if (= (buffer/get-char buf p) \space)
        (editor/apply-to-buffer "*snake*" #(buffer/set-char % p \0))
        (recur {::buffer/row (+ (rand-int (@state :rows)) 2) ::buffer/col (+ (rand-int (@state :cols)) 2)})))))

(def initial-screen
  (str
    (str/join (repeat (+ (@state :cols) 2) "█")) "\n"
    (str/join "\n" (repeat (@state :rows) (str "█" (str/join (repeat (@state :cols) " ")) "█"))) "\n"
    (str/join (repeat (+ (@state :cols) 2) "█"))
    "\nLevel: 0"))

(defn reset-view
  []
  (editor/apply-to-buffer "*snake*"
    (fn [buf] (-> buf
                  buffer/clear
                  (buffer/insert-string initial-screen)
                  (assoc ::buffer/cursor {::buffer/row 10 ::buffer/col 15}))))
  (place-dot)
  (swap! state assoc :next-move :right :length 2))

(defn game-over-view
  []
  (swap! state assoc :next-move nil)
  (editor/apply-to-buffer "*snake*"
    (fn [buf] (-> buf
                  (buffer/delete-region [{::buffer/row 10 ::buffer/col 10} {::buffer/row 10 ::buffer/col 20}])
                  (assoc ::buffer/cursor {::buffer/row 10 ::buffer/col 10})
                  (buffer/insert-string " GAME OVER ")))))

(defn move
  [dir]
  (swap! state update :history conj ((editor/get-buffer "*snake*") ::buffer/cursor))
  (let [tail-point (nth (@state :history) (@state :length))]
    (editor/apply-to-buffer "*snake*"
      #(-> %
           (buffer/set-char \█)
           (buffer/set-style :red)
           (buffer/set-char (tail-point ::buffer/row) (tail-point ::buffer/col) \space))))
  (cond (= dir :right) (editor/apply-to-buffer "*snake*" buffer/right) 
        (= dir :left) (editor/apply-to-buffer "*snake*" buffer/left)
        (= dir :up) (editor/apply-to-buffer "*snake*" buffer/up)
        (= dir :down) (editor/apply-to-buffer "*snake*" buffer/down)))

(defn run
 []
 (let [id (editor/get-buffer-id-by-name "*snake*")]
   (if id
     (editor/switch-to-buffer id)
     (editor/new-buffer "" {:major-modes (list :snake-mode) :name "*snake*"}))
   (reset-view)
   ;; Game loop
   (future 
     (loop []
       (Thread/sleep 150)
       (when (@state :next-move)
         (move (@state :next-move))
         (let [buf (editor/get-buffer "*snake*")
               c (buffer/get-char buf)
               p (buf ::buffer/cursor)]
           (cond (= c \█) (swap! state assoc :next-move nil)
                 (= c \0)
                   (do (swap! state update :length inc)
                       (editor/apply-to-buffer "*snake*"
                         #(-> %
                              (buffer/delete-line (+ (@state :rows) 3))
                              (buffer/append-buffer (buffer/buffer (str "\nLevel: " (- (@state :length) 2))))
                              (assoc ::buffer/cursor p)))
                       (place-dot))))
         (editor/paint-buffer "*snake*")
         (recur)))
     (game-over-view))))

(def mode
  {:insert {"esc" (fn [] (editor/apply-to-buffer buffer/set-normal-mode))}
   :normal {"esc" #(do (swap! state assoc :next-move nil) (editor/previous-regular-buffer))
            "h" #(swap! state assoc :next-move :left)
            "k" #(swap! state assoc :next-move :up)
            "j" #(swap! state assoc :next-move :down)
            "l" #(swap! state assoc :next-move :right)
            "C-b" #(swap! state assoc :next-move :left)
            "C-p" #(swap! state assoc :next-move :up)
            "C-n" #(swap! state assoc :next-move :down)
            "C-f" #(swap! state assoc :next-move :right)
            "left" #(swap! state assoc :next-move :left)
            "up" #(swap! state assoc :next-move :up)
            "down" #(swap! state assoc :next-move :down)
            "right" #(swap! state assoc :next-move :right)
            ":" (fn [] (swap! state assoc :next-move nil)
                       (editor/switch-to-buffer "*minibuffer*")
                       (editor/apply-to-buffer #(-> % buffer/clear (buffer/insert-char \:))))}
   :init run})

