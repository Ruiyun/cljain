(defproject cljain "0.6.0-beta1"
  :description "Enjoy JAIN-SIP in Clojure's way."
  :dependencies [[org.clojure/tools.logging "0.2.6"]
                 [ruiyun/tools.timer "1.0.1"]
                 [javax.sip/jain-sip-api "1.2.1.4"]
                 [javax.sip/jain-sip-ri "1.2.166"]
                 [log4j/log4j "1.2.16"]]
  :lein-release {:deploy-via :clojars}
  :global-vars {*warn-on-reflection* true
                *assert* true}
  :min-lein-version "2.0.0"
  :url "https://github.com/Ruiyun/cljain"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]]
                   :plugins [[codox "0.6.4"]]}})
