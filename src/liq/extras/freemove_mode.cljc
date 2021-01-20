(ns liq.extras.freemove-mode
  (:require [clojure.string :as str]
            [liq.editor :as editor]
            [liq.buffer :as buffer]
            [liq.util :as util]))

;; (def state (atom {:block nil :overwritten nil})) -> Use ::block and ::overwritten in buf

;; !!!!!!! Save "baseline" buf. When move make new move from baseline buffer.


(defn buffer-data
  [buf]
  (str 
       "12345678901234567890" "\n"
       (buffer/text buf) "\n"
       ;(buffer/get-selected-text buf) "\n"
       (buf ::buffer/cursor) "\n"))

;; Beundry is after two spaces, after beginning of line and space or beginning of line
(defn beginning-of-boundry
  [buf]
  (loop [b buf]
    (cond (= (-> b ::buffer/cursor ::buffer/col) 1) (b ::buffer/cursor)
          (and (= (-> b ::buffer/cursor ::buffer/col) 2) (= (buffer/get-char b) \space)) (b ::buffer/cursor)
          (= (buffer/get-char (buffer/left b 1)) (buffer/get-char (buffer/left b 2)) \space) (b ::buffer/cursor)
          true (recur (buffer/left b)))))

(defn end-of-boundry
  [buf]
  (let [cols (buffer/col-count buf (-> buf ::buffer/cursor ::buffer/row))]
    (loop [b buf]
      (cond (= (-> b ::buffer/cursor ::buffer/col) cols) (b ::buffer/cursor)
            (and (= (-> b ::buffer/cursor ::buffer/col) (- cols 1)) (= (buffer/get-char b) \space)) (b ::buffer/cursor)
            (= (buffer/get-char (buffer/right b 1)) (buffer/get-char (buffer/right b 2)) \space) (b ::buffer/cursor)
            true (recur (buffer/right b))))))


(comment
  (beginning-of-boundry (buffer/right (buffer/buffer "aa  bbc d eeeee") 12))
  (beginning-of-boundry (buffer/right (buffer/buffer "aa bbc d eeeee") 12))
  (beginning-of-boundry (buffer/right (buffer/buffer " aa bbc d eeeee") 12))
  (beginning-of-boundry (buffer/right (buffer/buffer "  aa bbc d eeeee") 12))
  (end-of-boundry (buffer/right (buffer/buffer "aa bbc d eeeee  ee") 12)))

(defn move-and-space
  [buf trow tcol]
  (let [b1 (if (<= trow (buffer/line-count buf))
             (assoc buf ::buffer/cursor {::buffer/row (max trow 1) ::buffer/col 1})
             (nth (iterate buffer/append-line (buffer/end-of-buffer buf)) (- trow (buffer/line-count buf))))]
    (if (<= tcol (buffer/col-count b1 (max trow 1)))
      (assoc b1 ::buffer/cursor {::buffer/row (max trow 1) ::buffer/col tcol})
      (buffer/set-normal-mode
        (nth (iterate #(buffer/insert-char % \space) (buffer/insert-at-line-end b1))
             (- tcol (buffer/col-count b1 (max trow 1)) 1))))))

(comment
  (-> (buffer/buffer "    abc")
      buffer/insert-at-line-end
      (buffer/insert-char "-")
      buffer/text))

(defn dmove-and-space
  [buf drow dcol]
  (move-and-space buf (+ (-> buf ::buffer/cursor ::buffer/row) drow)
                      (+ (-> buf ::buffer/cursor ::buffer/col) dcol)))
        
(comment
  ((nth (iterate buffer/append-line (buffer/end-of-buffer (buffer/buffer "abc"))) 4) ::buffer/cursor)
  (-> (buffer/buffer "abc\naaa")
      (move-and-space 3 2)
      (buffer/insert-char ".")
      buffer/text)

  (-> (buffer/buffer "abc")
      buffer/right
      (dmove-and-space 1 0)
      (buffer/insert-char ".")
      buffer/text)

  (-> (buffer/buffer "ab  cd")
      buffer/right
      (dmove-and-space 0 5)
      (buffer/insert-char ".")
      buffer/text))




(defn move-region-tmp
  "Move region containing cursor to with cursor at p"
  ([buf r drow dcol]
   (let [b (or (buf ::tmp-buf) buf)
         r1 (or (buf ::tmp-region) r)
         drow1 (+ (or (buf ::tmp-drow) 0) drow)
         dcol1 (+ (or (buf ::tmp-dcol) 0) dcol)
         text (buffer/text b r1)
         ins-after (fn [buf] (if (< (-> buf ::buffer/cursor ::buffer/col) (buffer/col-count buf (-> buf ::buffer/cursor ::buffer/row)))
                               (buffer/set-insert-mode buf)
                               (buffer/right (buffer/set-insert-mode buf))))]
     (-> b
         (assoc ::buffer/cursor (buf ::buffer/cursor))
         (buffer/delete-region r1)
         buffer/set-insert-mode
         (buffer/insert-string (format (str "%" (count text) "s") ""))
         (assoc ::tmp-buf b
                ::tmp-region r1
                ::tmp-drow drow1
                ::tmp-dcol dcol1)
         (dmove-and-space drow1 dcol1)
         buffer/set-insert-mode
         (buffer/delete-char (count text))
         ;buffer/set-insert-mode
         ins-after
         (buffer/insert-string text)
         buffer/set-normal-mode
         (assoc ::buffer/cursor (buf ::buffer/cursor)))))
  ([buf drow dcol] (move-region-tmp buf [(beginning-of-boundry buf) (end-of-boundry buf)] drow dcol)))

(comment

  (-> (buffer/buffer "abcde")
      buffer/end-of-line
      (buffer/delete-char 2)
      (buffer/insert-string "hh")
      buffer-data)

  (-> (buffer/buffer "abcde")
      (buffer/right 2)
      (move-region-tmp 0 0)
      buffer-data)

  (-> (buffer/buffer "ab  cd")
      (move-region-tmp 0 5)
      buffer-data))


(defn ^:buffer move-right
  [buf]
  ;; Iteration one, assuming selection
  (-> buf
      (move-region-tmp 0 1)
      (update-in [::buffer/cursor ::buffer/col] inc)))

(defn ^:buffer move-left
  [buf]
  ;; Iteration one, assuming selection
  (-> buf
      (move-region-tmp 0 -1)
      (assoc-in [::buffer/cursor ::buffer/col] (max (- (-> buf ::buffer/cursor ::buffer/col) 1) 1))))

(defn ^:buffer move-down
  [buf]
  ;; Iteration one, assuming selection
  (-> buf
      (move-region-tmp 1 0)
      (update-in [::buffer/cursor ::buffer/row] inc)))

(defn ^:buffer move-up
  [buf]
  ;; Iteration one, assuming selection
  (-> buf
      (move-region-tmp -1 0)
      (assoc-in [::buffer/cursor ::buffer/row] (max (- (-> buf ::buffer/cursor ::buffer/row) 1) 1))))
        
(comment
  (format (str "%" 4 "s") "")
  (buffer/get-char (buffer/buffer " 12345678901234567890"))
  (count (buffer/text (buffer/buffer "12345678901234567890") [{::buffer/row 1 ::buffer/col 5} {::buffer/row 1 ::buffer/col 8}]))
  (-> (buffer/buffer "12345  abcde  567890\nhhh")
      (buffer/right 8)
      (move-region-tmp 0 1)
      (move-region-tmp 0 5)
      buffer-data)

  (-> (buffer/buffer "12345678901234567890")
      (buffer/right 5)
      move-right
      move-right
      buffer-data)

  (-> (buffer/buffer "abcde")
      (buffer/right 2)
      move-down
      buffer-data)

  (-> (buffer/buffer "abcde")
      (buffer/right 2)
      move-down
      move-up
      buffer-data)

  (-> (buffer/buffer "abcde")
      (buffer/right 2)
      (move-region-tmp 0 0)
      buffer-data))

 

(defn ^:buffer cursor-right
  [buf]
  (-> buf
      (dissoc ::tmp-buf ::tmp-region ::tmp-drow ::tmp-dcol)
      (dmove-and-space 0 1)))

(defn ^:buffer cursor-left
  [buf]
  (-> buf
      (dissoc ::tmp-buf ::tmp-region ::tmp-drow ::tmp-dcol)
      (dmove-and-space 0 -1)))

(defn ^:buffer cursor-down
  [buf]
  (-> buf
      (dissoc ::tmp-buf ::tmp-region ::tmp-drow ::tmp-dcol)
      (dmove-and-space 1 0)))

(defn ^:buffer cursor-up
  [buf]
  (-> buf
      (dissoc ::tmp-buf ::tmp-region ::tmp-drow ::tmp-dcol)
      (dmove-and-space -1 0)))


;(defn init
; []
; (editor/apply-to-buffer
;   (fn [buf]
;     (update buf ::buffer/major-modes conj :freemove-mode))))

(defn ^:buffer exit-mode
  [buf]
  (-> buf
      (dissoc ::tmp-buf ::tmp-region ::tmp-drow ::tmp-dcol)
      (update ::buffer/major-modes rest)))

(defn ^:buffer to-normal-mode
  [buf]
  (-> buf
      buffer/set-normal-mode
      buffer/left
      (dissoc ::tmp-buf ::tmp-region ::tmp-drow ::tmp-dcol)))

(defn handle-input
  [c]
  (if-let [f ({"esc" (fn [] (editor/apply-to-buffer to-normal-mode))}
              c)]
    (f)
    (editor/apply-to-buffer
      #(-> %
          (buffer/set-char (first c))
          (dmove-and-space 0 1)
          buffer/set-insert-mode
          (dissoc ::tmp-buf ::tmp-region ::tmp-drow ::tmp-dcol)))))
          
      
(def mode
  {:insert handle-input
   :normal {"esc" #'exit-mode
            "h" #'cursor-left
            "j" #'cursor-down
            "k" #'cursor-up
            "l" #'cursor-right
            "H" #'move-left
            "J" #'move-down
            "K" #'move-up
            "L" #'move-right}
   :visual {"H" #'buffer/left
            "L" #'move-right}})
;:init init})


;(defn load-mode
;  []
;  (editor/add-mode :freemove-mode mode))

(comment
  (load-mode)
  (editor/add-key-bindings :fundamental-mode :normal {"C-t" init}))

;; !! Reset overwritten when selection is different.
;; Move selection (reduced to first line). If no selection select block 
;; First time HJKL (capital), move block and overwrite. Save overwritten to state. Add spaces where block came from
;; Not first time HJKL move block and reestablish overwritten. Update overwritten with new 
;; Non HJKL: Clear moved block and overwritten
;; Normal typing should just overwrite

;; (-> (editor/current-buffer) ::buffer/major-modes)

;; PARTS
;; Pull out p1 p2 
;; Push in p1 txt
;;    aaaa  123456  aaaaaaaaaaa bbbbbbbbbbb cccccccccccccccccc ddddddddddd
