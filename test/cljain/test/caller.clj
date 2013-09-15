(ns cljain.test.caller
  (:require [cljain.dum :refer :all]
            [cljain.sip.core :as sip]
            [cljain.sip.address :as addr]
            [cljain.sip.header :as header]
            [cljain.sip.message :as msg])
  (:import [org.apache.log4j PropertyConfigurator]))

(defn init []
  (PropertyConfigurator/configure "log4j.properties")

  (global-set-account {:user "bob", :domain "office", :display-name "Bob", :password "thepwd"})
  (sip/global-bind-sip-provider! (sip/sip-provider! "caller" "127.0.0.1" 6060 "udp"))
  (sip/set-listener! (dum-listener))
  (sip/start!))

(defn release []
  (sip/stop-and-release!))

(def ^:private dlg (atom nil))

(defn begin []
  (send-request! :INVITE :to (addr/address "sip:alice@home")
                 :pack {:type :application
                        :sub-type :sdp
                        :content (.getBytes "v=0\r\no=..." "UTF-8")}
                 :more-headers [(header/contact (addr/address "sip:bob@127.0.0.1:6060"))
                                (header/route (addr/address "<sip:alice@127.0.0.1:5060;transport=UDP;lr>"))]
                 :on-success (fn [& {:keys [dialog]}]
                               (let [ack (.createAck dialog 1)]
                                 (swap! dlg (constantly dialog))
                                 (.sendAck dialog ack)))
                 :on-failure (fn [& {rsp :response}]
                               (println "oops," (msg/reason rsp)))
                 :on-timeout (fn [& _]
                               (println "Timeout, try it later."))))

(defn end []
  (let [bye (.createRequest @dlg "BYE")
        t (sip/new-client-transcation! bye)]
    (.sendRequest @dlg t)))
