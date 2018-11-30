; eBooks Collection ebc - Log Window  
; Log Windows shows progress report for actions of ebc 

; Copyright (c) 2014 - 2016 Burkhardt Renz, THM. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.

(ns ebc.logwin
  (:require [seesaw.core :as sc]
            [seesaw.mig :as sm]
            [seesaw.widgets.log-window :as slw])
  (:import (javax.swing SwingUtilities)))

#_(set! *warn-on-reflection* true)

;; hides frame
(def hide-action
  (sc/action :handler (fn [e] (sc/hide! (sc/to-root e))) :name "Close"))

;; the output area
(def log-output-area
  (slw/log-window :auto-scroll? true ))

;; the content of the frame
(def content
  (sm/mig-panel :constraints ["insets 10", "[600]", "[468][36!]"]
                :items [[(sc/scrollable log-output-area
                                    :size [600 :by 440])   "cell 0 0, gapbottom 6"]
                        [(sc/button :id :close
                                    :action hide-action   
                                    :mnemonic \C
                                    :size [120 :by 36])    "cell 0 1, right" ]] ))

(def lw-frame
  (sc/frame :width 600 :height 562 
            :title "eBC Progress Log Window" 
            :content content
            :resizable? false
            :on-close :hide))

(defn init 
  "Initializes the logwin, returns frame."
  ([] (init [0,0]))
  ([pos]
  (sc/native!)
  (SwingUtilities/updateComponentTreeUI lw-frame)  
  (sc/pack! lw-frame)
  (sc/move! lw-frame :to pos)))

(defn show
  "Shows logwin frame."
  []
  (sc/invoke-later (sc/show! lw-frame)))
  
(defn hide
  "Hides logwin frame."
  []
  (sc/invoke-later (sc/hide! lw-frame)))
  
(defn msg 
  "Shows msg in the textarea of the frame."
  [msg]
  (sc/invoke-later (slw/log log-output-area (str msg "\n"))))

(defn clear
  "Clears the textarea of the frame."
  []
  (sc/invoke-later (slw/clear log-output-area)))

(defn disable
  "Disables closing the window"
  []
  (let [b (sc/select lw-frame [:#close])]
    (sc/config! b :enabled? false)
    (sc/config! lw-frame :on-close :nothing)))

(defn enable
  "Enables closing the window"
  []
  (let [b (sc/select lw-frame [:#close])]
    (sc/config! b :enabled? true)
    (sc/config! lw-frame :on-close :hide)))