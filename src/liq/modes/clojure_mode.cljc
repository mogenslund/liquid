(ns liq.modes.clojure-mode
  (:require [clojure.string :as str]
            [liq.modes.fundamental-mode :as fundamental-mode]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]
            [liq.util :as util]))

(def match
  {:keyword-begin #"(?<=(\s|\(|\[|\{)|^):[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+(?=(\s|\)|\]|\}|\,|$))"
   :keyword-end #".|$"
   :string-begin #"(?<!\\\\)(\")"
   :string-escape #"(\\\")"
   :string-end "\""
   :comment-begin #"(?<!\\\\);.*$|^#+ .*$"
   :comment-end #"$"
   :special-begin #"(?<=\()(ns |def(n|n-|test|record|protocol|macro)? )"
   :green-begin "✔"
   :red-begin "✘"
   :yellow-begin "➜"
   :bold-begin #"(?<=\*)\w+"
   :bold-end #"\*"
   :definition-begin #"[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+"
   :definition-end #"."})

(def mode
   {:syntax
     {:plain ; Context
       {:style :plain1 ; style
        :matchers {(match :string-begin) :string
                   (match :keyword-begin) :keyword
                   (match :comment-begin) :comment
                   (match :green-begin) :green
                   (match :yellow-begin) :yellow
                   (match :red-begin) :red
                   (match :bold-begin) :bold
                   (match :special-begin) :special}}
      :string
       {:style :string
        :matchers {(match :string-escape) :string
                   (match :string-end) :string-end}}
      :string-end
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
        :matchers {(match :definition-end) :plain}}}})