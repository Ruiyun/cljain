(ns cljain.test.callee
  (:require [cljain.dum :refer :all]
            [cljain.sip.core :as sip]
            [cljain.sip.header :as header]
            [cljain.sip.address :as addr])
  (:import [org.apache.log4j PropertyConfigurator]))

(defn init []
  (global-set-account {:user "alice", :domain "home", :display-name "Alice", :password "thepwd"})
  (sip/global-bind-sip-provider! (sip/sip-provider! "callee" "127.0.0.1" 5060 "udp"))
  (sip/set-listener! (dum-listener))
  (sip/start!))

(defn release []
  (sip/stop-and-release!))

(defmethod handle-request :INVITE [request transaction _]
  (send-response! 200 :in transaction
                  :pack {:type :application
                         :sub-type :sdp
                         :content (.getBytes "v=0\r\no=..." "UTF-8")}
                  :more-headers [(header/contact (addr/address "sip:alice@127.0.0.1:5060"))]))

(defmethod handle-request :BYE [request transaction _]
  (send-response! 200 :in transaction))
