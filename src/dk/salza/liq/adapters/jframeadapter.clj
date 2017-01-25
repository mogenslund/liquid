(ns dk.salza.liq.adapters.jframeadapter
  (:require [dk.salza.liq.util :as util]
            [clojure.string :as str]))


(def pane (atom nil))
(def last-key (atom nil))
(def tmp-keys (atom ""))
(def g (atom nil))

(def crow (atom 1))
(def ccolumn (atom 1))
(def ccolor (atom (java.awt.Color. 255 100 0)))
(def cbgcolor (atom (java.awt.Color. 20 20 20)))

(defn event2keyword
  [e]
  (let [ch (str (.getKeyChar e))
        code (re-find #"(?<=primaryLevelUnicode=)\d+" (.paramString e))]
    (println "----" ch)
    (println "KEYCHAR:   " ch)
    (println "CTRL:      " (.isControlDown e))
    (println "CODE:      " code)
    (println "PARAM STR: " (.paramString e)); ,primaryLevelUnicode=108,
    (cond (.isControlDown e) (cond (= code "102") :C-f
                                   (= code "103") :C-g
                                   (= code "106") :C-j
                                   (= code "107") :C-k
                                   (= code "108") :C-l
                                   (= code "111") :C-o
                                   (= code "113") :C-q
                                   (= code "115") :C-s
                                   (= code "116") :C-t
                                   (= code "32") :C-space
                                   :else :unknown)
          (= ch "\t") :tab
          (= ch " ") :space
          (= ch "\n") :enter
          (= ch "!") :exclamation
          (= ch "\"") :quote
          (= ch "#") :hash
          (= ch "$") :dollar
          (= ch "%") :percent
          :else (keyword (str (.getKeyChar e))))))

(defn jframeinit
  []
  (let [;editorpane (doto (javax.swing.JEditorPane.)
        ;             (.setContentType "text/html")
        ;             (.setEditable false)
                     ;(.setFocusTraversalKeysEnabled false)
        ;             (.setText "something <b>default</b>")
        ;             (.addKeyListener (proxy [java.awt.event.KeyListener] []
        ;               (keyPressed [e] (do))
        ;               (keyReleased [e] (do))
        ;               (keyTyped [e] (reset! last-key (event2keyword e))))))
        panel (doto (javax.swing.JPanel.)
                ;(.setFocusTraversalKeysEnabled false)
                (.addKeyListener (proxy [java.awt.event.KeyListener] []
                   (keyPressed [e] (println "a"))
                   (keyReleased [e] (println "b"))
                   (keyTyped [e] (reset! last-key (event2keyword e))))))
                ]
    (doto (javax.swing.JFrame. "λiquid")
      ;(.add editorpane)
      (.add panel)
      (.setSize 800 600)
      (.show))
    (while (nil? (.getGraphics panel))
      (Thread/sleep 50))
    (.requestFocus panel)
    (reset! g (.getGraphics panel))))
    ;(reset! pane editorpane)))

(defn jframerows
  []
  20)

(defn jframecolumns
  []
  80)

(defn plot
  [letter]
  (let [h 16
        w 8
        x (* (- @crow 1) h)
        y (* (- @ccolumn 1) w)]
    (.setColor @g @cbgcolor)
    (.fillRect @g y x w h)
    (.setColor @g @ccolor)
    (.drawString @g letter y (+ x h -3))
    (swap! ccolumn inc)))

(def old-lines (atom {}))

(defn jframeprint-lines
  [lines]
  (.setFont @g (java.awt.Font. "monospaced" java.awt.Font/PLAIN 12)); 
  (doseq [line lines]
    (let [row (line :row)
          column (line :column)
          content (line :line)
          key (str "k" row "-" column)
          oldcontent (@old-lines key)
          gr @g] 
    (when (not= oldcontent content)
      (let [diff (max 1 (- (count (filter #(and (string? %) (not= % "")) oldcontent))
                           (count (filter #(and (string? %) (not= % "")) content))))
            padding (format (str "%" diff "s") " ")]
        (reset! crow row)
        (reset! ccolumn column)
        (doseq [ch (line :line)]
          (if (string? ch)
            (if (= ch "\t") (plot "-") (plot ch))
            ;(do
            ;  (cond (= (ch :face) :string) (print "\033[38;5;131m")
            ;        (= (ch :face) :comment) (print "\033[38;5;105m")
            ;        (= (ch :face) :type1) (print "\033[38;5;11m") ; defn
            ;        (= (ch :face) :type2) (print "\033[38;5;40m") ; function
            ;        (= (ch :face) :type3) (print "\033[38;5;117m") ; keyword
            ;        :else (print "\033[0;37m"))
            ;  (cond (= (ch :bgface) :cursor1) (print "\033[42m")
            ;        (= (ch :bgface) :cursor2) (print "\033[44m")
            ;        (= (ch :bgface) :selection) (print "\033[48;5;52m")
            ;        (= (ch :bgface) :statusline) (print "\033[48;5;235m")
            ;        :else (print "\033[49m"))
            ;)))
        ;(if (= row (count lines))
        ;  (print (str "  " padding))
        ;  (print (str "\033[0;37m\033[49m" padding))))
      (swap! old-lines assoc key content))
    )))))



  ;; (doseq [[x y] (map vector (range 3) (range 3))] (println x "," y))

;  (doseq [line lines]
;    (let [row (line :row)
;          column (line :column)
;          content (line :line)] 
;        (doseq [ch (line :line)]

;  (draw-letter @g "a")
;  (draw-letter @g "b")
;  (draw-letter @g "c")
;  (draw-letter @g 2 20 "a" (java.awt.Color. 255 100 0) (java.awt.Color. 0 0 0))
;  (draw-letter @g 2 22 "b" (java.awt.Color. 255 100 0) (java.awt.Color. 0 0 0))
;  (draw-letter @g 2 24 "c" (java.awt.Color. 255 100 0) (java.awt.Color. 0 0 0))
;  (draw-letter @g 3 20 "d" (java.awt.Color. 155 0 0) (java.awt.Color. 0 0 0))
;  (draw-letter @g 3 21 "e" (java.awt.Color. 155 100 0) (java.awt.Color. 0 0 0))
  )
  ;(.setText @pane (str/join "<br>\n" (doall (map #(apply str (% :line)) lines)))))
  ;(.setText @pane (str "abc<b>test</b>" @tmp-keys)))

(defn jframewait-for-input
  []
  (while (not @last-key)
    (Thread/sleep 10))
  (let [res @last-key]
    (println "---")
    (reset! last-key nil)
    (swap! tmp-keys #(str % "-" res))
    res))
    ;:k))

(defn jframequit
  []
  (System/exit 0))

(def adapter {:init jframeinit
              :rows jframerows
              :columns jframecolumns
              :wait-for-input jframewait-for-input
              :printlines jframeprint-lines
              ;:reset jframereset
              :quit jframequit})

