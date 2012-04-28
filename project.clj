(defproject cljain "0.3.0-SNAPSHOT"
  :description "Use JAIN-SIP by Clojure way."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [javax.sip/jain-sip-api "1.2.1.4"]
                 [javax.sip/jain-sip-ri "1.2.166"]
                 [log4j/log4j "1.2.16"]]
  :dev-dependencies [[lein-autodoc "0.9.0"]]
  :autodoc {:name "cljain", :page-title "cljain API Documentation"})