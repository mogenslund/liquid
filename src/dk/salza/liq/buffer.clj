(ns dk.salza.liq.buffer
  (:require [dk.salza.liq.slider :as slider]
            [dk.salza.liq.sliderutil :as sliderutil]
            ;[dk.salza.liq.mode :as mode]
            [dk.salza.liq.tools.fileutil :as fileutil]
            [dk.salza.liq.coreutil :refer :all]
            [clojure.string :as str]))

(defn create
  [name]
  {::name name
   ::slider (slider/create)
   ::slider-undo '()  ;; Conj slider into this when doing changes
   ::slider-stack '() ;; To use in connection with undo
   ::filename nil
   ::dirty false
   ::mem-col 0
   ::highlighter nil
   ::keymap {}
   ;::mode nil
   })

(defn create-from-file
  [path]
  (when-let [content (or (fileutil/read-file-to-list path) "")]
    {::name path
     ::slider (slider/create content)
     ::slider-undo '()  ;; Conj slider into this when doing changes
     ::slider-stack '() ;; To use in connection with undo
     ::filename path
     ::dirty false
     ::mem-col 0
     ::highlighter nil
     ::keymap {}
     ;::mode nil
     }))

(defn- doto-slider
  [buffer fun & args]
  (update-in buffer [::slider] #(apply fun (list* % args))))

(defn update-mem-col
  [buffer columns]
  (assoc buffer ::mem-col (slider/get-visual-column (buffer ::slider) columns)))

;(defn- doto-mode
;  [buffer fun & args]
;  (update-in buffer [::mode] #(apply fun (list* % args))))


(defn get-slider
  [buffer]
  (buffer ::slider))

(defn get-visible-content
  [buffer]
  (-> buffer ::slider slider/get-visible-content)) 

(defn set-undo-point
  [buffer]
  (let [newstack (conj (buffer ::slider-stack) (buffer ::slider))]
    (assoc buffer ::slider-stack newstack
                  ::slider-undo newstack)))

(defn undo
  [buffer]
  (if (empty? (buffer ::slider-undo))
    buffer
    (assoc buffer ::slider (-> buffer ::slider-undo first)
                  ::slider-stack (conj (buffer ::slider-stack) (-> buffer ::slider-undo first))
                  ::slider-undo (rest (buffer ::slider-undo)))))

;(defn get-mode [buffer] (buffer ::mode))
;(defn set-mode [buffer mode] (assoc buffer ::mode mode))
(defn set-highlighter [buffer highlighter] (assoc buffer ::highlighter highlighter))
(defn get-highlighter [buffer] (buffer ::highlighter))

(defn set-keymap [buffer keymap] (assoc buffer ::keymap keymap))
(defn get-keymap [buffer] (buffer ::keymap))


;(defn swap-actionmapping [buffer] (doto-mode buffer mode/swap-actionmapping))
(defn set-dirty ([buffer dirty] (if (buffer ::filename) (assoc buffer ::dirty dirty) buffer))
                ([buffer] (set-dirty buffer true)))
(defn get-dirty [buffer] (buffer ::dirty))
(defn get-filename [buffer] (buffer ::filename))
(defn get-name [buffer] (buffer ::name))

(defn forward-char [buffer amount] (doto-slider buffer slider/right amount))
(defn backward-char [buffer amount] (doto-slider buffer slider/left amount))
(defn forward-word [buffer] (doto-slider buffer #(-> % (slider/right-until #" ") (slider/right 1))))
(defn beginning-of-buffer [buffer] (doto-slider buffer slider/beginning))
(defn end-of-buffer [buffer] (doto-slider buffer slider/end))
(defn find-next [buffer search] (doto-slider buffer slider/find-next search))
(defn insert [buffer string] (set-dirty (doto-slider (set-undo-point buffer) slider/insert string)))
(defn delete [buffer amount] (set-dirty (doto-slider (set-undo-point buffer) slider/delete amount)))
(defn set-mark [buffer name] (doto-slider buffer slider/set-mark name))
(defn set-point [buffer point] (doto-slider buffer slider/set-point point))
(defn remove-mark [buffer name] (doto-slider buffer slider/remove-mark name))
(defn point-to-mark [buffer name] (doto-slider buffer slider/point-to-mark name))
(defn end-of-line [buffer] (doto-slider buffer slider/end-of-line))
(defn beginning-of-line [buffer] (doto-slider buffer slider/beginning-of-line))
(defn insert-line [buffer] (set-dirty (doto-slider (set-undo-point buffer) #(-> % slider/end-of-line (slider/insert "\n")))))
(defn delete-char [buffer] (if (slider/end? (buffer ::slider))
                               buffer
                               (set-dirty (doto-slider (set-undo-point buffer) #(-> % (slider/right 1) (slider/delete 1))))))
(defn clear [buffer] (set-dirty (doto-slider (set-undo-point buffer) slider/clear)))
(defn get-context [buffer] (sliderutil/get-context (get-slider buffer)))
(defn delete-selection [buffer] (doto-slider buffer slider/delete-region "selection"))
(defn delete-line [buffer] (doto-slider buffer slider/delete-line))
(defn select-sexp-at-point [buffer] (doto-slider buffer slider/select-sexp-at-point))


(defn forward-visual-line
  [buffer columns]
  (doto-slider buffer slider/forward-visual-column columns (buffer ::mem-col)))

(defn backward-visual-line
  [buffer columns]
  (doto-slider buffer slider/backward-visual-column columns (buffer ::mem-col)))

(defn get-region [buffer name] (slider/get-region (buffer ::slider) name))
(defn get-char [buffer] (slider/get-char (buffer ::slider)))
(defn get-point [buffer] (slider/get-point (buffer ::slider)))
(defn get-linenumber [buffer] (slider/get-linenumber (buffer ::slider)))
(defn get-mark [buffer name] (slider/get-mark (buffer ::slider) name))
(defn get-selection [buffer] (-> buffer (get-region "selection"))) ;(slider/get-region (buffer ::slider) "selection"))
(defn get-content [buffer] (slider/get-content (buffer ::slider)))
(defn sexp-at-point [buffer] (sliderutil/sexp-at-point (buffer ::slider)))
(defn get-line [buffer] (-> buffer beginning-of-line (set-mark "linestart")
                                   end-of-line (get-region "linestart")))

;(defn get-action1
;  [buffer keyw]
;  (when (-> buffer ::mode)
;    (-> buffer ::mode ::mode/actionmapping first keyw)))

(defn get-action
  [buffer keyw]
  (when (-> buffer ::keymap)
    (-> buffer ::keymap keyw)))

(defn end-of-buffer? [buffer] (slider/end? (buffer ::slider)))

(defn save-buffer
  [buffer]
  (when-let [filepath (get-filename buffer)]
    (fileutil/write-file filepath (get-content buffer)))
  (set-dirty buffer false))
  
(defn tmp-buffer
  [buffer columns]
  (doto-slider buffer slider/forward-line columns))