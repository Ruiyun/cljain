(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.message
  (:use cljain.util
        [cljain.core :only [sip-factory]])
  (:require [cljain.address :as addr]
            [cljain.header :as header])
  (:import [javax.sip SipFactory]
           [javax.sip.message Message MessageFactory Request]))

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :private true}
  factory (.createMessageFactory sip-factory))

(defn request
  "place doc string here"
  {:added "0.2.0"}
  [method req-uri from call-id & more-headers]
  (let [headers (apply hash-map (mapcat #(vector (.getName %) %) more-headers))
        c-seq (get headers "CSeq" (header/c-seq 1 method))
        to (get headers "To" (header/to (addr/address req-uri) nil))
        via [(get headers "Via")]
        max-forward (get headers "Max-Forwards" (header/max-forwards 70))
        request (.createRequest factory req-uri method call-id c-seq from to via max-forward)
        remain-headers (remove #(in? % ["From" "Call-ID" "CSeq" "To" "Via" "Max-Forwards"]) (keys headers))
        remain-headers (vals (select-keys headers remain-headers))]
    (doseq [h remain-headers] (.setHeader request h))
    request))

(defn response
  "place doc string here"
  {:added "0.2.0"}
  [status-code, ^Request reqest]
  (.createResponse factory status-code reqest))


(defn set-header!
  "place doc string here"
  {:added "0.2.0"}
  [^Message message, header]
  (.setHeader message header))

(defn add-header!
  "place doc string here"
  {:added "0.2.0"}
  [^Message message, header]
  (.addHeader message header))

(defn remove-header!
  "place doc string here"
  {:added "0.2.0"}
  [^Message message, header-name]
  (.removeHeader message header-name))


