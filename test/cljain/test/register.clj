(ns ^{:author "ruiyun"}
  cljain.test.register
  (:import [javax.sip.header WWWAuthenticateHeader AuthorizationHeader]
           [javax.sip.message Response]))

(org.apache.log4j.PropertyConfigurator/configure "log4j.properties")

(use 'cljain.dum)
(require '[cljain.sip.core :as sip]
  '[cljain.sip.address :as addr]
  '[cljain.sip.header :as header])

(defmethod handle-request :REGISTER [request transaction & _]
  (if (.getHeader request AuthorizationHeader/NAME)
    (send-response! Response/OK :in transaction)
    (send-response! Response/UNAUTHORIZED :in transaction
      :more-headers [(header/www-authenticate "Digest" "localhost" "aa2f052b75d9ed32"
                       :algorithm "MD5" :stale false)])))

(global-set-account :user "bob" :domain "localhost" :display-name "Bob" :password "123456")
;(sip/global-bind-sip-provider! (sip/sip-provider! "my-app" "localhost" 6060 "udp" :outbound-proxy "127.0.0.1:5060"))
(sip/global-bind-sip-provider! (sip/sip-provider! "my-app" "localhost" 5060 "udp"))
(sip/set-listener! (dum-listener))
(sip/start!)

(register-to (addr/address "sip:localhost") 40
  :on-success #(prn "success")
  :on-failure #(prn "failure")
  :on-refreshed #(prn "refreshed")
  :on-refresh-failed #(prn "refresh failed"))
