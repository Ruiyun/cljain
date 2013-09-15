(ns cljain.test.register
  (:require [cljain.dum :refer :all]
            [cljain.sip.core :as sip]
            [cljain.sip.address :as addr]
            [cljain.sip.header :as header])
  (:import [javax.sip.header WWWAuthenticateHeader AuthorizationHeader]
           [javax.sip.message Response]))

(org.apache.log4j.PropertyConfigurator/configure "log4j.properties")

(defmethod handle-request :REGISTER [request transaction _]
  (if (.getHeader request AuthorizationHeader/NAME)
    (send-response! Response/OK :in transaction)
    (send-response! Response/UNAUTHORIZED :in transaction
                    :more-headers [(header/www-authenticate "Digest" "localhost" "aa2f052b75d9ed32"
                                                             :algorithm "MD5" :stale false)])))

(global-set-account {:user "bob" :domain "localhost" :display-name "Bob" :password "123456"})
(sip/global-bind-sip-provider! (sip/sip-provider! "my-app" "127.0.0.1" 5060 "udp"))
(sip/set-listener! (dum-listener))
(sip/start!)

(register-to! (addr/address "sip:127.0.0.1:5060") 40
              :on-success (fn [_] (println "success"))
              :on-failure (fn [_] (println "failure"))
              :on-refreshed (fn [_] (println "refreshed"))
              :on-refresh-failed (fn [_] (println "refresh failed")))
