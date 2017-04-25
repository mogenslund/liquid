(ns dk.salza.liq.coreutil)

(defn bump
  ([li index]
    (if (> (count li) index)
      (let [item (nth li index)]
        (conj (remove (fn [x] (= x item)) li) item))
      li))
  ([li keyw match]
    (let [parts (group-by #(= (% keyw) match) li)]
      (concat (parts true) (parts false)))))

(defn rotate
  "Take the first element of a list and
  inserts at the end."
  [li]
  (if (> 2 (count li))
    li
    (concat (rest li) (list (first li)))))

(defn remove-item
  ([li index]
    (if (> (count li) index)
      (let [item (nth li index)]
        (remove (fn [x] (= x item)) li))
      li))
  ([li keyw match]
    (let [parts (group-by #(= (% keyw) match) li)]
      (parts false))))


(defn doto-first
  [li fun & args]
  (conj (rest li) (apply fun (cons (first li) args))))

(defn get-match
  "In a list of maps returns the first map
  containing with specified keyword with 
  the specified match as value."
  [li keyw match]
  (first (filter #(= (% keyw) match) li)))