; eBooks Collection ebc - Constants 

; Copyright (c) 2014 - 2017 Burkhardt Renz, THM. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.

(ns ebc.consts
  (:require [clojure.edn :as edn]))

; version from project file
(defmacro proj-version []
  (some-> "project.clj" slurp edn/read-string (nth 2)))

(def ^:private rev (str (proj-version) " (2017-06-05)"))

(def
  ^{:doc "Revision of eBC
          :rev Revision
          :ver Version string
          :cpr Copyright string"}
  ebc-rev
  {:rev rev
   :ver (str "This is eBC (eBooks Collection) " rev)
   :cpr "Copyright (c) 2003 - 2017 by Burkhardt Renz, THM"})

(def 
  ^{:doc "About for gui"}
  about
  (clojure.string/join
    \newline
    ["<html><strong>eBooks Collection Toolbox</strong>"
     (str "eBC Rev " (:rev ebc-rev))
     (:cpr ebc-rev)
     "Project home: homepages.thm.de/~hg11260/ebc.html"
     ""
     "<html>Licensed under <strong>Eclipse Public License 1.0</strong><br />"
     "<html>Written in <strong>Clojure</strong> (clojure.org)<br />"
     ""
     "<html><strong>Libraries</strong> used:"
       "Seesaw (github.com/daveray/seesaw)"
       "Lucene (lucene.apache.org) with"
       "ZClucy (https://github.com/ceterumnet/clucy)"
       "Enlive (github.com/cgrand/enlive)"
       "Pantomime (github.com/michaelklishin/pantomime)"
       "Tika (tika.apache.org)"]))

(def
  ^{:doc "Patterns
         :folder  for ebc folders
         :cat     for category in folder name
         :subcat  for subcatogroy in folder name
         :topic   for topic in folder name
         :book    for ebc book
         :path    for ebc path 
         :scat    for category or subcategory in folder name"}
  ebc-pats
  {:folder #"^e[cst]_.*"
   :cat    #"^ec_([^/]*).*"
   :subcat #".*/es_([^/]*).*"
   :topic  #".*/et_([^/]*).*"
   :book   #"^e[bx]_.*"
   :path   #".*(ec_.*$)"
   :scat   #"^e[cs]_([^/]*).*"})
   
(def
  ^{:doc "Pattern for ebc filename: type - authors - title - ext"}
  ebc-name-pat
  #"^e([bx])_([^_]+)_([^_]+)\.(\w+)")

(def
  ^{:doc "Parts in ebc-name-pat"}
  ebc-name-parts
  {:path 0, :type 1, :authors 2, :title 3, :ext 4})
   
(def
  ^{:doc "Names of files
         :result html page for search results
         :home   html page for home page of directory
         :css    css for html pages
         :index  directory name for Lucene index"}
  ebc-file-names
  {:result "ebc-result.html"
   :home   "ebc-index.html"
   :css    "ebc.css"
   :index  "LuceneIdx"})

(def
  ^{:doc "Lucene configuration
         :def-fld  Default field for search
         :def-op   Default operator"}
  luc-config
  {:def-fld  :content
   :def-op   :and}) 
    
    