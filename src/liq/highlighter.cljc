(ns liq.highlighter
  (:require [clojure.string :as str]
            [liq.buffer :as buffer]))

(defn first-match  
  [s match]
  (if (string? match)
    (when-let [m (str/index-of s match)]
      {:pos (inc m) :match match})
    (let [m (re-find match s)]
      (cond (= m "") {:pos (inc (count s)) :match ""}
            (string? m) (first-match s m) 
            (vector? m) (first-match s (first m))))))

(defn highlight-row
  [buf hl row initial-context]
  (loop [b buf col 1 context initial-context]
    (let [line (buffer/line b row col)
          hit (first (sort-by :pos
                       (filter :pos
                         (for [[reg con] (-> hl context :matchers)]
                           (assoc (first-match line reg) :context con)))))
          col1 (if (and hit (hit :pos)) (+ (hit :pos) col -1) (+ (buffer/col-count b row) 1))
          next-context (if (and hit (hit :pos)) (hit :context) context)
          match (or (and hit (hit :match)) "")
          colcount (buffer/col-count b row)]
      (cond (> col colcount) (buffer/set-style b row (max (min col colcount) 1) (-> hl context :style))
             true (recur (-> b (buffer/set-style row col (min col1 colcount) (-> hl context :style))
                               (buffer/set-style row col1 (min (+ col1 (dec (count match))) colcount) (-> hl next-context :style)))
                        (+ col1 (count match))
                        next-context)))))

(defn highlight
  ([buf hl]
   (loop [b buf row 1 col 1 context :plain]
     (let [line (buffer/line b row col)
           hit (first (sort-by :pos
                        (filter :pos
                          (for [[reg con] (-> hl context :matchers)]
                            (assoc (first-match line reg) :context con)))))
           col1 (if (and hit (hit :pos)) (+ (hit :pos) col -1) (+ (buffer/col-count b row) 1))
           next-context (if (and hit (hit :pos)) (hit :context) context)
           match (or (and hit (hit :match)) "")
           colcount (buffer/col-count b row)]
       (cond (> row (buffer/line-count b)) b 
             (> col colcount) (recur (buffer/set-style b row (max (min col colcount) 1) (-> hl context :style)) (inc row) 1 next-context)
             true (recur (-> b (buffer/set-style row col (min col1 colcount) (-> hl context :style))
                               (buffer/set-style row col1 (min (+ col1 (dec (count match))) colcount) (-> hl next-context :style)))
                         row
                         (+ col1 (count match))
                         next-context)))))
  ([buf hl row]
   (if (= row 1)
     (highlight-row buf hl 1 :plain)
     (let [b1 (-> buf buffer/up buffer/end-of-line)
           style (buffer/get-style b1)
           c (buffer/get-char b1)]
       (if (and (= style :string) (not= c \"))
         (highlight-row buf hl row :string)
         (highlight-row buf hl row :plain))))))
         

      
(comment
  (def buf1 (buffer/buffer "Some :keyw and \"text\" and a :keyword ; My \"text\" ; comment and\nSome more text\nHere is also a comment"))

  (nil :pos)

  (first (sort-by :pos
                       (filter :pos
                         (for [[reg con] (-> hl :plain1 :matchers)]
                           (assoc (first-match "lj" reg) :context con)))))
  (sort-by :pos (filter :pos
                        (for [[reg con] (-> hl :plain :matchers)]
                          (assoc (first-match "Some :keyw and" reg) :context con))))
  (sort-by :pos (filter :pos
                        (for [[reg con] (-> hl :plain :matchers)]
                          (assoc (first-match "Some keyw and" reg) :context con))))

  (def match-keys
    {:match-keyword-begin #"(?<=(\s|\(|\[|\{)):[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+(?=(\s|\)|\]|\}|\,|$))"
     :match-keyword-end #".|$"
     :match-string-begin #"(?<!\\\\)(\")"
     :match-string-escape #"(\\\")"
     :match-string-end #"(\")"
     :match-comment-begin #"(?<!\\\\);"
     :match-comment-end #"$"})

  (def hl1
     {:plain ; Context
       {:style :plain1 ; style
        :matchers {(match-keys :match-string-begin) :string
                   (match-keys :match-keyword-begin) :keyword
                   (match-keys :match-comment-begin) :comment}}
      :string
       {:style :string
        :matchers {(match-keys :match-string-escape) :string
                   (match-keys :match-string-end) :string-end}}
      :string-end
       {:style :string
        :matchers {#".|$" :plain}}
      :comment
       {:style :comment
        :matchers {(match-keys :match-comment-end) :plain}}
      :keyword
       {:style :keyword
        :matchers {(match-keys :match-keyword-end) :plain}}})

  (first-match #"$" "abc")
  (pr-str (re-find #"$" "abc"))
  (highlight (buffer/buffer "\"a\"") hl1)
  (highlight (buffer/buffer "a :bbb ccc") hl1 2)
  (first-match "a :bbb" #"(?<=(\s|\(|\[|\{)):[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+(?=(\s|\)|\]|\}|\,|$))")

  (let [buf (highlight (buffer/buffer "aa \"\"\na") hl1)]
    (map #(buffer/get-style buf 1 %) (range 1 (inc (buffer/col-count buf 1)))))
  (let [buf (highlight (buffer/buffer "a :a\na") hl1)]
    (map #(buffer/get-style buf 1 %) (range 1 (inc (buffer/col-count buf 1)))))
  (highlight buf1 hl1))