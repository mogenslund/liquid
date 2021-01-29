(ns liq.extras.txtplot
  (:require [clojure.string :as str]))

(defn txtbitmap
  [points]
  (let [maxx (quot (+ (apply max (map first points)) 2) 2)
        maxy (quot (+ (apply max (map second points)) 2) 2)
        v0 (apply vector (map #(or [0 0 0 0] %) (range maxx)))
        bitmap0 (apply vector (map #(or v0 %) (range maxy)))
        ;; p -> p0, v0 : [2 1] -> [1 0], [0 0 1 0]  (quot 2 2)
        csplit (fn [p] [[(quot (first p) 2) (quot (second p) 2)]
                        (cond (and (even? (first p)) (even? (second p))) [1 0 0 0]
                              (and (odd? (first p)) (even? (second p))) [0 1 0 0]
                              (and (even? (first p)) (odd? (second p))) [0 0 1 0]
                              (and (odd? (first p)) (odd? (second p))) [0 0 0 1])])
        ;; [0 0 1 1] [0 1 1 0] -> [0 1 1 1]
        ccombine (fn [p1 p2] (apply vector (map max p1 p2))) 
        chararray [" " "▗" "▖" "▄" "▝" "▐" "▞" "▟" "▘" "▚" "▌" "▙" "▀" "▜" "▛" "█"]
        tochar (fn [v] (chararray (+ (* (v 0) 8) (* (v 1) 4) (* (v 2) 2) (* (v 3) 1))))]
    (str/join "\n"
      (map #(str/join (map tochar %))
        (loop [bitmap bitmap0 remaining points]
          (if (empty? remaining)
            bitmap
            (let [[p0 v0] (csplit (first remaining))
                  bitmap1 (update-in bitmap [(second p0) (first p0)] #(ccombine % v0))]
              (recur bitmap1 (rest remaining)))))))))

(defn txtplot
  [xres yres points]
  (let [xmin (apply min (map first points))
        xmax (apply max (map first points))
        ymin (apply min (map second points))
        ymax (apply max (map second points))
        mapx (fn [x] (Math/round (+ 0.0 (/ (* (- x xmin) xres) (- xmax xmin)))))
        mapy (fn [y] (Math/round (+ 0.0 (- yres (/ (* (- y ymin) yres) (- ymax ymin))))))
        mapxy (fn [p] [(mapx (first p)) (mapy (second p))])]
    (txtbitmap (apply vector (map mapxy points)))))

(defn box
  [p1 p2]
  (let [rx (range (min (p1 0) (p2 0)) (max (p1 0) (p2 0)))
        ry (range (min (p1 1) (p2 1)) (max (p1 1) (p2 1)))]
    (for [x rx y ry] [x y])))

(defn txtbar
  [barwidth barsep values]
  (let [intvalues (map int values)
        ma (apply max intvalues)
        mi (apply min intvalues)
        height (- ma (max 0 mi))]
        ;pixvalues (map #(quot (* height %) ma) values)]
    (txtbitmap
      (apply concat
             (map-indexed
               (fn [idx v]
                   (box [(* (+ barwidth barsep) idx) height] [(+ (* (+ barwidth barsep) idx) barwidth) (- height v)]))
               intvalues)))))

;; Esamples
(comment

  ;; Basic samples
  (txtbitmap [[0 0] [10 1] [10 2] [4 5] [6 5]])
  (txtplot 100 200 [[-100 2] [-5 3] [0 0] [3 -15]])
  (txtbar 2 6 [2 9 8 1 4 5 0 2 12 9 9 3])
  (txtbar 10 4 [2 9 8 1 4 5 0 2 12 9 9 3])
  (txtbar 4 6 [-3 -2 -1 0 1 2 3 4])

  ;; Sinus curve
  (let [xs (range -10 10 0.04)
        points (map #(vector % (Math/sin %)) xs)]
    (txtplot 200 50 points))

  ;; Sierpinski fractal
  (let [sierpinski [[0.5 0 0 0.5 0 0 0.33]
                    [0.5 0 0 0.5 0 0.5 0.33]
                    [0.5 0 0 0.5 0.5 0 0.34]]
        nextpoint (fn [data [x0 y0]]
                    (let [r (rand)
                          index (loop [i 0 accum ((data 0) 6)]
                                  (if (> accum r)
                                    i
                                    (recur (+ i 1) (+ accum ((data (+ i 1)) 6)))))
                          x (+ (* ((data index) 0) x0) (* ((data index) 1) y0) ((data index) 4))
                          y (+ (* ((data index) 2) x0) (* ((data index) 3) y0) ((data index) 5))]
                      [x y]))
        points (take 25000 (iterate #(nextpoint sierpinski %) [0 0]))]
   (txtplot 108 54 points)))  
 
