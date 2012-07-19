(ns ^{:author "ruiyun"}
  cljain.sip
  (:use [potemkin])
  (:require [cljain.sip.address :as addr]
            [cljain.sip.header :as header]
            [cljain.sip.message :as msg]
            [cljain.sip.core :as core]))

;; add the #sip/address reader literals
;; usage: #sip/address "sip:localhost:5060;transport=udp"
(.bindRoot #'default-data-readers (assoc default-data-readers 'sip/address #'cljain.sip.address/str->address))

;;;

(import-fn addr/sip-uri)
(import-fn addr/tel-uri)
(import-fn addr/address)
(import-fn addr/sip-address)
(import-fn addr/uri?)
(import-fn addr/address?)

;;;

(import-fn msg/request)
(import-fn msg/response)
(import-fn msg/inc-sequence-number!)

;;;


