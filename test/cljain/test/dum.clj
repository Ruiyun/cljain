(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.test.dum
  (:require [cljain.sip.core :as core]
            [cljain.sip.header :as header]
            [cljain.sip.address :as addr]
            [cljain.sip.dialog :as dlg]
            [cljain.sip.message :as msg]
            [cljain.sip.transaction :as trans]
            [cljain.dum :as dum]))

(org.apache.log4j.PropertyConfigurator/configure "log4j.properties")
(core/global-bind-sip-provider! (core/sip-provider! "test" "127.0.0.1" 5060 "udp"))
(dum/initialize! :user "reuiyun" :domain "notbook" :display-name "Ruiyun Wen")
(core/start!)

;(def bob (cljain.sip.address/address (cljain.sip.address/sip-uri "127.0.0.1" :port 5070 :user "bob") "Bob henry"))
;(cljain.dum/send-request! :INVITE :to bob :pack {:type :application :sub-type :sdp :content "Welcome message"}
;  :on-success (fn [a b c] (prn "success!" (dlg/send-ack! b (dlg/ack! b 1))))
;  :on-failure (fn [a b c] (prn "faliure!" a))
;  :on-timeout (fn [a] (prn "tiemout" a)))

(dum/register-to (addr/address (addr/sip-uri "127.0.0.1" :port 5070)) 15)

