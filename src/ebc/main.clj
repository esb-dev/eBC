; eBooks Collection ebc - main
; Starts the GUI as well as the cli tools.

; Copyright (c) 2014 - 2015 Burkhardt Renz, THM. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.

(ns ebc.main
  (:require [ebc.consts :as c]
            [ebc.directory :refer :all]
            [ebc.index :refer :all]
            [ebc.update :refer :all]
            [ebc.gui :refer :all]
            [clojure.tools.cli :as cli])
 (:gen-class))

#_(set! *warn-on-reflection* true)

;; ## Starting point for the functionality of the ebc ebCollection
;;    tool set

;; Command line handling

(def 
  ^{:doc "Command line options."}
  cli-opts
  [["-v" "--version"]
   ["-h" "--help"] 
   ["-b" "--basedir BD" "(optional) base directory" :default "."]
   ["-c" "--check"      "checks filenames"]
   ["-d" "--directory"  "creates the HTML eBook directory"]
   ["-i" "--index"      "indexes the eBooks Collection"]
   ["-u" "--update'"     "updates the index"]])

(def usage
   (clojure.string/join
     \newline
     [(:ver c/ebc-rev)
        ""
        "Usage: ebc              starts the GUI for searching"
        "       ebc [-b basedir] -c checks filenames in the eBooks collection"
        "       ebc [-b basedir] -d creates HTML directory for the eBooks Collection"
        "       ebc [-b basedir] -i indexes the eBooks Collection"
        "       ebc [-b basedir] -u updates the index"
        "       ebc -h help"
        "       ebc -v version"]))
  
(defn- exit [status msg]
  (println msg)
  (System/exit status))

;; Main function

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-opts)]
    (let [basedir (:basedir options)]
      (cond
        errors                (exit 1 usage)
        (:version options)    (exit 0 (:ver c/ebc-rev))
        (:help options)       (exit 0 usage)
        (:check options)      (do (do-check basedir println) (exit 0 "bye"))
        (:index options)      (do (make-index basedir println) (exit 0 "bye"))
        (:update options)     (do (update-index basedir println) (exit 0 "bye"))
        (:directory options)  (do (make-directory basedir println) (exit 0 "bye"))
        :else                 (gui)
        ))))