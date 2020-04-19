(ns liq.extras.markdownfolds
  (:require [clojure.string :as str]
            [liq.editor :as editor]
            [liq.buffer :as buffer]
            [liq.util :as util]))

(defn get-headline-level
  ([buf row]
   (let [line (buffer/line buf row)]
     (cond (re-find #"^ [✔☐➜✘] " line) 10  
           (re-find #"^.def?n" line) 10  
           true (count (re-find #"^[#]+(?= )" line)))))
  ([buf]
   (get-headline-level buf (-> buf ::buffer/cursor ::buffer/row))))

(defn get-level-end
  ([buf row]
   (let [l (get-headline-level buf row)
         lc (buffer/line-count buf)]
     (if (= l 0)
       row
       (loop [r (inc row)]
         (cond (> r lc) lc
               (<= 1 (get-headline-level buf r) l) (dec r)
               true (recur (inc r)))))))
  ([buf]
   (get-level-end buf (-> buf ::buffer/cursor ::buffer/row))))

(defn hide-lines-below
  ([buf p n]
   (update buf ::buffer/hidden-lines assoc (inc (p ::buffer/row)) (+ (p ::buffer/row) n)))
  ([buf n]
   (hide-lines-below buf (-> buf ::buffer/cursor) n)))

(defn show-lines-below
 ([buf p]
  (update buf ::buffer/hidden-lines dissoc (inc (p ::buffer/row))))
 ([buf]
  (show-lines-below buf (buf ::buffer/cursor))))

(defn hide-level
  ([buf p]
    (let [row (p ::buffer/row)]
      (update buf ::buffer/hidden-lines assoc (inc row) (get-level-end buf row))))
  ([buf]
    (hide-level buf (buf ::buffer/cursor))))

(defn hide-all-levels
  [buf]
  (loop [b buf row 1]
    (if (> row (buffer/line-count b))
      b
      (recur (hide-level b {::buffer/row row ::buffer/col 1}) (inc row)))))

(defn show-all-levels
  [buf]
  (assoc buf ::buffer/hidden-lines {}))

(defn hide-levels-between
  [buf row0 row1]
  (loop [b buf row row0]
    (if (> row row1)
      b
      (recur (hide-level b {::buffer/row row ::buffer/col 1}) (inc row)))))

(defn show-levels-between
  [buf row0 row1]
  (loop [b buf row row0]
    (if (> row row1)
      b
      (recur (update b ::buffer/hidden-lines dissoc row) (inc row)))))

(defn cycle-level-fold
  [buf]
  (let [r0 (-> buf ::buffer/cursor ::buffer/row)
        r1 (get-level-end buf)]
    (cond (buffer/row-hidden? buf (inc r0))
            (-> buf show-lines-below (hide-levels-between (inc r0) r1))
          (= (buffer/visible-rows-count buf (inc r0) r1) (- r1 r0))
            (hide-levels-between buf r0 r1)
          true (show-levels-between buf (inc r0) r1))))

(defn toggle-show-lines-below
  [buf]
  (if (not= (buffer/next-visible-row buf) (inc (-> buf ::buffer/cursor ::buffer/row)))
    (show-lines-below buf)
    (hide-lines-below buf (- (get-level-end buf) (-> buf ::buffer/cursor ::buffer/row)))))

(defn load-markdownfolds
  []
  (editor/add-key-bindings
    :fundamental-mode
    :normal {"+" {"+" (fn [] (editor/apply-to-buffer cycle-level-fold))
                  "0" (fn [] (editor/apply-to-buffer show-all-levels))
                  "a" (fn [] (editor/apply-to-buffer hide-all-levels))
                  "t" (fn [] (editor/apply-to-buffer hide-level))
                  "-" (fn [] (editor/apply-to-buffer #(show-lines-below %)))}}
  ))
