; eBooks Collection ebc - Synchronisation of the Lucene index
; Updates the Lucene index of the eBooks Collection with the
; changes of the collection since the last update or index

; Copyright (c) 2014 Burkhardt Renz, THM. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.

(ns ebc.update
  (:require [ebc.consts :as c]
            [ebc.util :refer :all]
            [ebc.index :as i]
            [clojure.string :as str]
            [clojure.set :as set]
            [clucy.core :as luc]
            [clojure.java.io :as io])
  (:import (org.apache.lucene.index IndexReader DirectoryReader)
           (org.apache.lucene.store Directory)))

#_(set! *warn-on-reflection* true)

;; Find all ebooks that has been modified or added since a given timestamp
;; Search for the latest date of ebooks in the index
;; Search for the files in the collection with newer date

(defn
  last-sync-date
  "Searches the latest sync date of the ebooks collection in basedir."
  [basedir]
  (let [idx-path (str basedir "/" (:index c/ebc-file-names))
        index (luc/disk-index idx-path)]
    (binding [luc/*analyzer* i/ebc-analyzer]
      (first (map #(:date %) (luc/search index "*:*" 1 :sort-by "date desc"))))))


;; Find the obsolete ebooks in the index
;; ipaths = current paths in index
;; cpaths = current paths in collection
;; opaths = obsolete paths in index = ipaths - cpaths
;; npaths = new paths in collection = cpaths - ipaths

;; helper func (better to be found in the clucy library)
(defn
  no-ldocs
  "Number of ldocs in the given Lucene index."
  [index]
  (let [^IndexReader reader (DirectoryReader/open ^Directory index)]
    (.numDocs reader)))

(defn
  ipaths
  "Set of the current paths in the index at basedir."
  [basedir]
  (let [idx-path (str basedir "/" (:index c/ebc-file-names))
        index (luc/disk-index idx-path)]
    (binding [luc/*analyzer* i/ebc-analyzer]
      (into #{} (map #(:path %) (luc/search index "*:*" (no-ldocs index)))))))

(defn
  cpaths
  "Set of the current paths in the collection at basedir"
  [basedir]
  (into #{} (map :path (books basedir))))

(defn
  opaths
  "Set of obsolete paths in the index at basedir"
  [ipath-set cpath-set]
  (set/difference ipath-set cpath-set))

(defn
  npaths
  "Set of new paths in the collection at basedir"
  [ipath-set cpath-set]
  (set/difference cpath-set ipath-set))

;; Find the new books in the collection
(defn 
  nbooks
  "Seq of new books in the collection at basedir"
  [basedir npath-set]
   (filter #(contains? npath-set (:path %)) (books basedir)))
  
;; Find the modified books in the collection
(defn
  mbooks
  "Seq of modified books in the collection at basedir"
  [basedir npath-set]
  (let [last-date (last-sync-date basedir)
        cand (books basedir #(newer? % last-date))]
  (filter #(not (contains? npath-set (:path %))) cand)))

;; Synchronisation
;; Step 1: Delete the obsolete books
;; Step 2: Add the new books
;; Step 3: Upsert modified books

(defn-
  log-path [path]
    (i/log-msg path)
    path)

(defn update-index
  "Updates the index in the basedir by deleting obsolete books and
   upserting modified or new books."
  [basedir report-fn]
  (report-fn (:ver c/ebc-rev))
  (i/init-log-agent (str "Updating the index for " basedir " ...") report-fn)
  (try
	  (let [idx-path (str basedir "/" (:index c/ebc-file-names))
	        index (luc/disk-index idx-path i/ldoc-schema)
	        ipath-set (ipaths basedir)
	        cpath-set (cpaths basedir)
	        npath-set (npaths ipath-set cpath-set)]  
	    (binding [luc/*analyzer* i/ebc-analyzer luc/*content*  false]
	      (do
		      ; Step 1
		      (i/log-msg "Deleting obsolete index entries...")
		      (apply luc/delete-document index (map #(hash-map :path %)
		                                           (map log-path (opaths ipath-set cpath-set))))
		      ; Step 2
		      (i/log-msg "Adding new index entries...")
		      (apply luc/add index (i/ldocs (nbooks basedir npath-set)))
		      
		      ; Step 3
		      (i/log-msg "Updating the index with modified books...")
		      (apply luc/upsert index (i/ldocs (mbooks basedir npath-set)))
		      
		      ; Step 4
	        (i/log-msg "Done."))))
   (catch Exception e 
     (i/log-msg "Could not update the Lucene Index.")
     (i/log-msg (.getMessage e)))))
