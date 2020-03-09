(ns liq.modes.help-mode
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]
            [liq.util :as util]))

(defn load-topic
  [topic]
  (if-let [id (editor/get-buffer-id-by-name (str "*Help - " topic "*"))]
    (switch-to-buffer id)
    (editor/new-buffer (slurp (if (re-find #"/" topic) topic (io/resource (str "help/" topic))))
                       {:major-modes (list :help-mode) :name (str "*Help - " topic "*")})))
 

; (slurp (io/resource (str "help/" topic)))
; (slurp (io/resource "help/index.txt"))

(defn load-topic-at-point
  []
  (let [buf (editor/current-buffer)
        parent (re-find #"/.*/" (buf ::buffer/name))
        topic (str parent (re-find #"[^:\(\)\[\]\{\}]+" (buffer/get-word buf)))]
    (when (re-matches #".*\.txt" topic)
      (load-topic topic))))

(defn run
  ([topic] (load-topic topic))
  ([] (run "index.txt")))

(def mode
  {:insert {"esc" (fn [] (apply-to-buffer #(-> % (assoc ::buffer/mode :normal) buffer/left)))}
   :normal {"q" editor/previous-buffer
            "C- " #(((editor/get-mode :buffer-chooser-mode) :init))
            "\n" load-topic-at-point
            "h" :left 
            "j" :down
            "k" :up
            "l" :right
            "C-b" :left
            "C-n" :down
            "C-p" :up
            "C-f" :right
            "left" :left 
            "down" :down 
            "up" :up 
            "right" :right
            "0" #(apply-to-buffer buffer/beginning-of-line)
            "$" #(apply-to-buffer buffer/end-of-line)
            "g" {"g" #(editor/apply-to-buffer buffer/beginning-of-buffer)}
            "G" #(apply-to-buffer buffer/end-of-buffer)
            "/" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \/))))
            ":" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \:))))}
    :visual {"esc" #(apply-to-buffer buffer/set-normal-mode)}
    :init run
    :syntax
     {:plain ; Context
       {:style :plain ; style
        :matchers {#"[-a-zA-Z0-9]+\.txt" :topic
                   #"---.*---" :topic
                   #"===.*===" :topic}}
      :topic
       {:style :definition
        :matchers {#".|$" :plain}}}})
