(ns liq.modes.javascript-mode
  (:require [clojure.string :as str]
            [clojure.repl :as repl]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]
            [liq.util :as util]))

(def match
  {:keyword-begin #"(?<=(\s|\(|\[|\{)|^):[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+(?=(\s|\)|\]|\}|\,|$))"
   :keyword-end #".|$"
   :string-begin #"(?<!\\\\)(\")"
   :string-escape #"(\\\")"
   :string-end "\""
   :string1-begin #"(?<!\\\\)(')"
   :string1-escape #"(\\')"
   :string1-end "'"
   :comment-begin #"//"
   :comment-end #"$"
   :special-begin #"(var|function )"
   :green-begin "✔"
   :red-begin "✘"
   :yellow-begin "➜"
   :bold-begin #"(?<=\*)\w+"
   :bold-end #"\*"
   :definition-begin #"[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+"
   :definition-end #"."})

(def mode
  {:normal {"c" {"p" {"f" (fn [] (editor/message ((shell/sh "node" ((editor/current-buffer) ::buffer/filename)) :out)))
                      "å" (fn [] (editor/message ((shell/sh "node" ((editor/current-buffer) ::buffer/filename)) :out)))}
                 "å" (fn [] (editor/message ((shell/sh "node" ((editor/current-buffer) ::buffer/filename)) :out)))}
            "f5" (fn [] (editor/message ((shell/sh "node" ((editor/current-buffer) ::buffer/filename)) :out)))
            "½" (fn [] (editor/message (rand-int 1000)))}
   :syntax
    {:plain ; Context
      {:style :plain1 ; style
       :matchers {(match :string-begin) :string
                  (match :string1-begin) :string1
                  (match :keyword-begin) :keyword
                  (match :comment-begin) :comment
                  (match :green-begin) :green
                  (match :yellow-begin) :yellow
                  (match :red-begin) :red
                  (match :bold-begin) :bold
                  (match :special-begin) :special
                  #"[-a-zA-Z0-9]+\.txt" :topic
                  #"---.*---" :topic
                  #"===.*===" :topic}}
     :string
      {:style :string
       :matchers {(match :string-escape) :string
                  (match :string-end) :string-end}}
     :string-end
      {:style :string
       :matchers {#".|$|^" :plain}}

     :string1
      {:style :string
       :matchers {(match :string1-escape) :string1
                  (match :string1-end) :string1-end}}
     :string1-end
      {:style :string
       :matchers {#".|$|^" :plain}}

     :comment
      {:style :comment
       :matchers {(match :comment-end) :plain}}

     :keyword
      {:style :keyword
       :matchers {(match :keyword-end) :plain}}
      
     :special
      {:style :special
       :matchers {(match :definition-begin) :definition}}

     :green
      {:style :green
       :matchers {#".|$|^" :plain}}

     :yellow
      {:style :yellow
       :matchers {#".|$|^" :plain}}

     :red
      {:style :red
       :matchers {#".|$|^" :plain}}

     :bold
      {:style :green
       :matchers {(match :bold-end) :plain}}

     :definition
      {:style :definition
       :matchers {(match :definition-end) :plain}}

     :topic {:style :definition
             :matchers {#".|$" :plain}}}})

(defn load-mode
  []
  (editor/add-mode :javascript-mode mode)
  (swap! editor/state update ::editor/new-buffer-hooks conj
    (fn [buf]
      (if (and (buf ::buffer/filename) (re-matches #".*.js" (buf ::buffer/filename)))
        (update buf ::buffer/major-modes #(conj % :javascript-mode))
        buf))))