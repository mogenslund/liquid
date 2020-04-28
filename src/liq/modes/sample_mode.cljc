(ns liq.modes.sample-mode
  "This sample mode can be used as template for modes, especially completion modes.
  To be used for a real purpose it needs a bit more detail, to cover edge cases.
  To use this simple mode, first require the namespace with:
 
      (ns user (:require [liq.modes.sample-mode :as sample-mode]))

  Then load the mode into the editor with:

      (sample-mode/load-mode)

  This will remap the tab key in fundamental-modes insert mode to some useless
  completion.

  Press tab several times and then enter to choose a word ending."
  (:require [clojure.string :as str]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]
            [liq.util :as util]))

(def samplestate (atom {}))

(defn choices
  [text]
  (str "sandwich\npie\ncrumple\ncheese\narrow\nshirt"))

(defn run
  []
  (let [buf (editor/current-buffer)
        w (-> buf buffer/left buffer/word)]
    (reset! samplestate {:bufferid (buf ::editor/id) :word w})
    (editor/new-buffer 
      (choices w)
      {:major-modes (list :sample-mode)
            :name "*sample*"
            :top (+ (-> buf ::buffer/window ::buffer/top) (-> buf ::buffer/cursor ::buffer/row) -1)
            :left (+ (-> buf ::buffer/window ::buffer/left) (-> buf ::buffer/cursor ::buffer/col) -1)
            :rows 5
            :cols 30})
    (editor/apply-to-buffer
      "*sample*"
      #(-> %
           buffer/end-of-line
           buffer/set-selection
           buffer/beginning-of-line))))

(defn abort
  []
  (editor/message (str "Sample " (rand-int 100) @samplestate))
  (editor/kill-buffer "*sample*")
  (editor/switch-to-buffer (@samplestate :bufferid)))

(defn execute
  []
  (let [hit (buffer/get-selected-text (editor/get-buffer "*sample*"))]
    (abort)
    (editor/apply-to-buffer
      #(buffer/insert-string % hit))))
  
(defn next-choice
  []
  (editor/apply-to-buffer
    "*sample*"
    #(-> %
         buffer/down
         buffer/end-of-line
         buffer/set-selection
         buffer/beginning-of-line)))

(def mode
  {:normal {"esc" abort 
            "\t" next-choice
            "\n" execute}
   :syntax
     {:plain
       {:style :green}}})

(defn load-mode
  []
  (swap! editor/state
    #(-> %
         (update ::editor/modes assoc :sample-mode mode)
         (assoc-in [::editor/modes :fundamental-mode :insert "\t"] run))))
