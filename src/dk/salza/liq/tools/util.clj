(ns dk.salza.liq.tools.util
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.test :as test]))

(defn now
  "Current time in milliseconds"
  []
  (System/currentTimeMillis))

(defn cmd
  "Execute a native command.
  Adding :timeout 60 or similar as last command will
  add a timeout to the process."
  [& args]
  (let [builder (doto (ProcessBuilder. args)
                  (.redirectErrorStream true))
        process (.start builder)
                                        ;res (atom [])
        lineprocessor (future (doseq [line (line-seq (io/reader (.getInputStream process)))]
                                        ;(swap! res conj line)))
                                (println line)))
                                        ;(println "-" line)))
                                        ;(swap! dk.salza.qqrunner.core/state update :log #(conj (subvec % 1) line))))
        
        monitor (future (.waitFor process))
        starttime (quot (System/currentTimeMillis) 1000)]
    (try
      (while (and (not (future-done? monitor))
                  (< (- (quot (System/currentTimeMillis) 1000) starttime)))
        (Thread/sleep 1000))
      (catch Exception e
        (do (.destroy process)
            (println "Exception" (.getMessage e))
            (future-cancel monitor))))
    (when (not (future-done? monitor))
      (println "TimeoutException or Interrupted")
      (.destroy process))))

(def localclipboard (atom ""))

(defn clipboard-content
  []
  (if (java.awt.GraphicsEnvironment/isHeadless)
    @localclipboard
    (let [clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit))]
      (try
        (.getTransferData (.getContents clipboard nil) (java.awt.datatransfer.DataFlavor/stringFlavor))
        (catch java.lang.NullPointerException e "")))))

(defn set-clipboard-content
  [text]
  (reset! localclipboard text)
  (when (not (java.awt.GraphicsEnvironment/isHeadless))
    (let [clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit))]
      (.setContents clipboard (java.awt.datatransfer.StringSelection. text) nil))))

(defn update-map
  "Updates a map using update-in if the
  second arguments in pairs is a function
  and assoc-in if not."
  [m & kv]
  (if (empty? kv)
    m
    (let [key (first kv)
          val (second kv)]
      (apply 
       update-map
       (cons ((if (test/function? val) update-in assoc-in) m key val)
             (drop 2 kv))))))

(defn generateid
  "Generates a new id not in the existing
  list using parts of seed."
  [seed existing]
  (let [normalized (str/replace (str/replace seed #".*/" "") #"[^0-9a-zA-Z]" "-")]
    (if (not (some #{(keyword normalized)} existing))
      (keyword normalized)
      (loop [num 2]
        (if (not (some #{(keyword (str normalized num))} existing))
          (keyword (str normalized num))
          (recur (inc num)))))))

(defn read-lines-from-file
  [filepath]
  (vec (with-open [r (io/reader filepath)]
    (doall (line-seq r)))))

(defn save-lines-to-file
  [filepath lines]
  (with-open [wrtr (io/writer filepath)]
    (.write wrtr (clojure.string/join "\n" lines))))


(defn pretty-exception
  [e]
  (let [message (.getMessage e)
        stacklines (map str (.getStackTrace e))
        filtered (filter #(re-find (re-pattern #"dk\.salza") %) stacklines)
        shortened (map #(re-find (re-pattern "\\w*\\.clj:\\d+") %) filtered)]
    (str
     message "\n"
     (str/join "\n" shortened) "\n"
     (str/join "\n" filtered) "\n"
     (str/join "\n" stacklines))))

(defn browser
  [url]
  (cmd "firefox" "--new-tab" url))