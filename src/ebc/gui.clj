; eBooks Collection ebc - GUI  
; GUI for the ebooks collection toolbox.

; Copyright (c) 2014 Burkhardt Renz, THM. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.

(ns ebc.gui
  (:require [ebc.consts :as c]
            [ebc.util :refer :all]
            [ebc.index :as index]
            [ebc.update :as update]
            [ebc.directory :as dir]
            [ebc.logwin :as lw]
            [ebc.html :as html]
            [clojure.java.browse :as b]
            [clojure.string :as str]
            [seesaw.core :as sc]
            [seesaw.chooser :as sch]
            [seesaw.mig  :as sm])
  (:import (javax.swing JFrame)))

#_(set! *warn-on-reflection* true)

;; Read ebook collection basedirs from env, they are separated by ';'
(def ebc-basedirs
  (if-let [ebc-env-entry (System/getenv c/ebc-env-entry)]
    (str/split ebc-env-entry #";")
    []))

;; Disable and enable buttons during long running tasks
(defn- disable
  [root]
  (doseq [id [[:#dir] [:#check] [:#index] [:#update] [:#browse] [:#search] [:#exit]]]
          (sc/config! (sc/select root id) :enabled? false))
  (sc/config! root :on-close :nothing)
  (lw/disable))

(defn- enable
  [root]
  (doseq [id [[:#dir] [:#check] [:#index] [:#update] [:#browse] [:#search] [:#exit]]]
          (sc/config! (sc/select root id) :enabled? true))
  (sc/config! root :on-close :exit)
  (lw/enable))

;; Actions for the gui
(defn choose-basedir [event]
  (sch/choose-file :type :open 
                   :selection-mode :dirs-only
                   :success-fn (fn [ev basedir]
                                 (let [cb (sc/select (sc/to-root event) [:#ebc])
                                       basedir' (clojure.string/replace basedir "\\" "/")]
                                   (sc/selection! cb basedir')))))

(def choose-action
  (sc/action :handler (fn [e] (choose-basedir e)) :name "Choose eBooks Collection"))

(defn dir [event]
  (let [root    (sc/to-root event)
        basedir (norm-path (sc/text (sc/select root [:#ebc])))]
    (future
      (sc/invoke-later (disable root))
      (lw/clear)
      (lw/show)
      (dir/make-directory basedir lw/msg)
      (sc/invoke-later (enable root))))) 

(def dir-action
  (sc/action :handler (fn [e] (dir e)) :name "Directory" :mnemonic \D))

(defn check [event]
  (let [root    (sc/to-root event)
        basedir (norm-path (sc/text (sc/select root [:#ebc])))]
    (future
      (sc/invoke-later (disable root))
      (lw/clear)
      (lw/show)
      (dir/do-check basedir lw/msg)
      (sc/invoke-later (enable root))))) 


(def check-action
  (sc/action :handler (fn [e] (check e)) :name "Check" :mnemonic \C))

(defn index [event]
  (let [root    (sc/to-root event)
        basedir (norm-path (sc/text (sc/select root [:#ebc])))]
    (future
      (sc/invoke-later (disable root))
      (lw/clear)
      (lw/show)
      (index/make-index basedir lw/msg)
      (sc/invoke-later (enable root))))) 

(def index-action
  (sc/action :handler (fn [e] (index e)) :name "Index" :mnemonic \I))

(defn update [event]
  (let [root    (sc/to-root event)
        basedir (norm-path (sc/text (sc/select root [:#ebc])))]
    (future
      (sc/invoke-later (disable root))
      (lw/clear)
      (lw/show)
      (update/update-index basedir lw/msg)
      (sc/invoke-later (enable root))))) 

(def update-action
  (sc/action :handler (fn [e] (update e)) :name "Update" :mnemonic \U))

(defn about-dlg []
    (-> (sc/dialog :title "About eBC" :size [400 :by 360] :content c/about)
        (sc/pack!)
        (sc/show!)))

(def about-action 
  (sc/action :handler (fn [e] (about-dlg)) :name "About" :mnemonic \A))

(defn browse [event]
  (let [root    (sc/to-root event)
        basedir (norm-path (sc/text (sc/select root [:#ebc])))
        path    (str basedir "/" (:home c/ebc-file-names))]
    (try
      (b/browse-url path)
     (catch Exception e (sc/alert (.getMessage e))))))

(def browse-action
  (sc/action :handler (fn [e] (browse e)) :name "Browse" :mnemonic \B))

(defn search [event]
   (let [root (sc/to-root event)
         basedir (str/replace (sc/text (sc/select root [:#ebc])) "\\" "/")
         result-path (str basedir "/" (:result c/ebc-file-names))
         search-crit (sc/text (sc/select root [:#scr]))
         results (try 
                   (index/sresults basedir search-crit)
                   (catch Exception e (sc/alert (.getMessage e))))
         result-page (html/results "Search results" (dir/navdata (dir/subjects basedir)) results)]
       (if results
         (do
          (let [name (:css c/ebc-file-names)]
            (copy-resource name (str basedir "/" name)))
           (html/spit-page result-path result-page)
           (try
             (b/browse-url result-path)
             (catch Exception e (sc/alert (.getMessage e))))))))

(def search-action
  (sc/action :handler (fn [e] (search e)) :name "Search" :mnemonic \S))

(def exit-action
  (sc/action :handler (fn [e] (System/exit 0)) :name "Exit" :mnemonic \E))


;; Frame with the help of MigLayout
(defn frame-content []
  (sm/mig-panel :constraints ["insets 24 18 24 18", "[132!][132!][132!][132!]", "[36!][36!][36!][36!][36!]"]
    :items [
      [ (sc/button :action choose-action :size [260 :by 26])     "cell 0 0 2 1"]
      [ (sc/combobox :id :ebc :editable? true
                     :model ebc-basedirs :size [260 :by 26])     "cell 2 0 2 1"] 
      [ (sc/label  :text "Search criteria")                      "cell 1 1, align right, gapright 12"]
      [ (sc/text   :id :scr :text "" :size [260 :by 26])         "cell 2 1 2 1"] 
      [ (sc/button :id :check :action check-action 
                   :size [120 :by 36])                           "cell 0 3" ]
      [ (sc/button :id :dir :action dir-action  
                   :size [120 :by 36])                           "cell 1 3" ]
      [ (sc/button :id :browse :action browse-action
                   :size [120 :by 36])                           "cell 2 3" ]
      [ (sc/button :id :search :action search-action 
                   :size [120 :by 36])                           "cell 3 3" ]
      [ (sc/button :action about-action :size [120 :by 36])      "cell 0 4" ]
      [ (sc/button :id :index :action index-action 
                   :size [120 :by 36])                           "cell 1 4" ]
      [ (sc/button :id :update :action update-action 
                   :size [120 :by 36])                           "cell 2 4" ]
      [ (sc/button :id :exit :action exit-action 
                   :size [120 :by 36])                           "cell 3 4" ]
      ]
      ))

(defn gui []
  (let [f (sc/frame :title "The eBooks Collection Toolbox" 
                    :resizable? false 
                    :content (frame-content)
                    :on-close :exit)]
    (do
      (sc/native!)
      (javax.swing.SwingUtilities/updateComponentTreeUI f)  
      (doto (.getRootPane ^JFrame f) (.setDefaultButton (sc/select f [:#search])))
      ;; is there a more seesaw-like way to do this?
      (sc/pack! f)
      (lw/init [250, 75])
      (sc/move! f :to [50, 50])
      (sc/show! f)
      (sc/request-focus! (sc/select f [:#scr])))))
