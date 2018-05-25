(ns dk.salza.liq.tools.cshell
  "A namespace for shell like utils
  in clojure style."
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.lang ProcessBuilder]
           [java.util.concurrent TimeUnit]))

(def current-dir (atom (System/getProperty "user.dir")))
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

(defn cd
  [d]
  (reset! current-dir d))

(defn mkdir
  [d]
  (.mkdirs (io/file @current-dir d)))

(defn cmdseq
  [& args]
  (let [builder (doto (ProcessBuilder. args)
                      (.directory (io/file @current-dir)))]
    (reset! current-process (.start builder))
    (line-seq (io/reader (.getInputStream @current-process)))))

(defn kill-process
  []
  (.destroy @current-process))

(defn cmd
  "Execute a native command.
  Adding :timeout 60 or similar as last command will
  add a timeout to the process."
  [& args]
  (let [[parameters options] (split-arguments args)
        builder (doto (ProcessBuilder. parameters)
                  (.redirectErrorStream true)
                  (.directory (io/file @current-dir)))
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
(defn expand-home
  [s]
  (if (clojure.string/starts-with? s "~")
    (clojure.string/replace-first s "~" (System/getProperty "user.home"))
    s))

(defn folder?
  [path]
  (.isDirectory (io/file (expand-home path))))

(defn file?
  [path]
  (.isFile (io/file (expand-home path))))

(defn size
  [s]
  (if (sequential? s)
    (map size s)
    (.length (io/file (expand-home s)))))

(defn ls
  "Outputs files and folders in a given
   dir into a vector."
  {:example "(ls \"/tmp\")"}
  ([dir]
   (into [] (map #(.getAbsolutePath %1) (.listFiles (io/file (expand-home dir))))))
  ([]
   (ls @current-dir)))

(defn lsr
  [dir]
  "Outputs files and folders in a given
   dir into a vector."
  {:example "(ls \"/tmp\")"}
  [dir]
  (map #(.getAbsolutePath %) (file-seq (io/file (expand-home dir)))))

(defn p
  "Prints a sequence in lines."
  [s]
  (if (sequential? s)
    (println (str/join "\n" s))
    (println s)))

(defn pp
  "Prints a sequence line by line."
  [s]
  (if (sequential? s)
    (doseq [l s] (println l))
    (println s)))

(defn rex
  "Takes a sequence and filters by given
  regular expression"
  [re s]
  (filter #(re-find (re-pattern re) %) (if (string? s) [s] s)))

(defn frex
  "Takes a sequence of files and filters by given
  regular expression match in content"
  [re s]
  (filter #(and (file? %) (re-find (re-pattern re) (slurp %))) (if (string? s) [s] s)))

(defn lrex
  "Returns sequence of lines in files matched
  by the regular expression."
  [re s]
  (apply concat
         (for [f (filter file? (if (string? s) [s] s))] ; The last part is to convert string to vector with string
           (filter #(re-find (re-pattern re) %)
                   (str/split-lines (slurp f))))))

(defn flrex
  "Returns sequence of [filepath line] vectors in files matched
  by the regular expression."
  [re s]
  (apply concat
         (for [f (filter file? (if (string? s) [s] s))] ; The last part is to convert string to vector with string
           (map #(do [f %]) (filter #(re-find (re-pattern re) %)
                    (str/split-lines (slurp f)))))))

(defn cp
  [source-path dest-path]
  (io/copy (io/file source-path) (io/file dest-path)))

(defn mv
  [source-path dest-path]
  (.renameTo (io/file source-path) (io/file dest-path)))

(defn rm
  [path]
  (io/delete-file path))

(defn pathsuggest
  "Given part of a path return
  sequence with suggestions."
  [partly]
  (let [fullpart (expand-home partly)
        folder (subs fullpart 0 (inc (.lastIndexOf fullpart "/")))]
    (map #(str % (when (folder? %) "/"))
         (->> folder ls (rex fullpart)))))
