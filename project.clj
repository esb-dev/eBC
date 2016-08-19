; eBooks Collection ebc - Project definition 

;   Copyright (c) 2014 - 2016 Burkhardt Renz, THM. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.

(defproject ebc "4.5.1"
  :description "eBooks Collection Toolbox"
  :url "http://homepages.thm.de/~hg11260/ebc.html"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]  
                 [org.clojure/tools.cli "0.3.5"]
                 [seesaw "1.4.5"]
                 [ceterumnet-zclucy "0.9.4"]
                 [enlive "1.1.6"]
                 [com.novemberain/pantomime "2.8.0"]]
  :main ebc.main
  :profiles {:uberjar  {:aot :all}}
  :uberjar-name "ebc.jar")
