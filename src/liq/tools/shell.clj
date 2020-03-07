(ns liq.tools.shell
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.lang ProcessBuilder]
           [java.util.concurrent TimeUnit]))

(def current-process (atom nil))

(defn datetimestamp
  "Todays date in yyyy-mm-dd-HH-mm-ss format"
  []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd-HH-mm-ss")
           (.getTime (java.util.Calendar/getInstance))))

(defn today
  []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd")
           (.getTime (java.util.Calendar/getInstance))))

(defn- split-arguments
  "Splits arguments/vector into list of parameters
  and a map, if there are keywords.
  (split-arguments [1 2 3 :timeout 10]) -> [[1 2 3] {:timeout 10}]"
  [args]
  (apply #(vector %1 (apply hash-map %2)) (split-with #(not (keyword? %)) args)))

(defn- take-realized
  [coll]
  (if-not (instance? clojure.lang.IPending coll)
    (cons (first coll) (take-realized (rest coll)))
    (when (realized? coll)
      (cons (first coll) (take-realized (rest coll))))))

(defn cmdseq
  [folder & args]
  (let [builder (doto (ProcessBuilder. args)
                      (.directory (io/file folder)))]
    (reset! current-process (.start builder))
    (line-seq (io/reader (.getInputStream @current-process)))))

(defn kill-process
  []
  (.destroy @current-process))

(defn cmd
  "Execute a native command.
  Adding :timeout 60 or similar as last command will
  add a timeout to the process."
  [folder & args]
  (let [[parameters options] (split-arguments args)
        builder (doto (ProcessBuilder. parameters)
                  (.redirectErrorStream true)
                  (.directory (io/file folder)))
        process (.start builder)]
    (if (if (options :timeout)
          (.waitFor process (options :timeout) TimeUnit/SECONDS)
          (.waitFor process))
      (str/join "\n" (doall (line-seq (io/reader (.getInputStream process)))))
      (str (str/join "\n" (take-realized (line-seq (io/reader (.getInputStream process)))))
           "\nTimeoutException"))))

(defn remote-cmd
  "Executes a command on remote server and returns
   the result."
  [server username password & args]
  (apply cmd (concat ["sshpass" "-p" password
        "ssh" "-o" "UserKnownHostsFile=/dev/null" "-o" "StrictHostKeyChecking=no"
        (str username "@" server)]
                     args)))

(comment
  (cmd "/tmp" "/home/sosdamgx/m/bin/mcountdown" "3")
  (doseq [x (cmdseq "/tmp" "/home/sosdamgx/m/bin/mcountdown" "3")] (println x)))