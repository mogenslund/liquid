(ns liq.extras.cool-stuff
  (:require [clojure.string :as str]
            [liq.editor :as editor]
            [liq.buffer :as buffer]
            [liq.util :as util])
  (:import [java.net Socket]
           [java.io BufferedInputStream BufferedOutputStream
                    BufferedReader InputStreamReader]
           [java.nio.charset Charset]))

;; clj -J-Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}"
(def socket (atom nil))

(defn jack-in
  ([port]
   (let [s (try (Socket. "localhost" (util/int-value port)) (catch Exception e nil))]
     (when s
       (reset! socket {:socket s
                       :in (BufferedInputStream. (.getInputStream s))
                       :out (BufferedOutputStream. (.getOutputStream s))
                       :reader (BufferedReader. (InputStreamReader. (BufferedInputStream. (.getInputStream s))))})
       (future
         (loop []
           (when @socket
             (let [l (.readLine (@socket :reader))]
               (when l
                 (editor/message l :append true)
                 (recur)))))))))
  ([] (jack-in "5555")))

(defn jack-out
  []
  (when (and @socket (@socket :socket))
    (when (.isConnected (@socket :socket))
      (.close (@socket :socket)))
  (reset! socket nil)))

(defn send-to-repl
  [text]
  (when (and @socket (@socket :socket) (.isConnected (@socket :socket)))
    (.write (@socket :out) (.getBytes (str text "\n") (Charset/forName "UTF-8")))
    (.flush (@socket :out))))

(defn send-sexp-at-point-to-repl 
  [buf]
  (let [sexp (if (= (buf ::buffer/mode) :visual)
               (buffer/get-selected-text buf)
               (buffer/sexp-at-point buf))]
    (send-to-repl sexp)))

(defn send-current-buffer-to-repl
  []
  (-> (editor/current-buffer)
      (buffer/text)
      (send-to-repl)))

(defn output-split
  ([n]
   (editor/set-setting :auto-switch-to-output false)
   (let [w (editor/get-window)
         rows (w ::buffer/rows)
         cols (w ::buffer/cols w)
         sp (min (if (re-matches #"\d+" n) (Integer/parseInt n) 5) (- rows 3))]
     (editor/set-setting :auto-switch-to-output false)
     (editor/switch-to-buffer "scratch")
     (editor/apply-to-buffer "output" #(assoc % ::buffer/window {::buffer/top (- rows sp) ::buffer/left 1 ::buffer/rows sp ::buffer/cols cols}))
     (editor/apply-to-buffer "scratch" #(assoc % ::buffer/window {::buffer/top 1 ::buffer/left 1 ::buffer/rows (- rows sp 2) ::buffer/cols cols}))
     (editor/new-buffer "-----------------------------" {:name "*delimeter*" :top (- rows sp 1) :left 1 :rows 1 :cols cols})
     (editor/switch-to-buffer "scratch")
     (editor/paint-buffer)))
  ([] (output-split "5")))

(defn load-cool-stuff
  []
  (swap! editor/state assoc-in [::editor/commands :jack-in] jack-in)
  (swap! editor/state assoc-in [::editor/commands :jack-out] jack-out) 
  (swap! editor/state assoc-in [::editor/commands :output-split] output-split) 
  (editor/add-key-bindings :clojure-mode :normal
    {"f4" #(send-current-buffer-to-repl)
     "f5" #(send-sexp-at-point-to-repl (editor/current-buffer))
     "f6" jack-out})
  (editor/add-key-bindings :clojure-mode :visual
    {"f4" #(send-current-buffer-to-repl)
     "f5" #(send-sexp-at-point-to-repl (editor/current-buffer))}))
