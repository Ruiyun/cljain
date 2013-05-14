(ns cljain.sip.message
  (:require [cljain.sip.core :refer [sip-factory]]
            [cljain.sip.address :as addr]
            [cljain.sip.header :as header])
  (:import [javax.sip SipFactory]
           [javax.sip.message Message MessageFactory Request Response]
           [javax.sip.header Header CSeqHeader]))

(def ^{:doc "The factory to create Request and Response."
       :tag MessageFactory
       :private true}
  factory (.createMessageFactory ^SipFactory sip-factory))

(defn request
  "Creates a new Request message of type specified by the method paramater,
  containing the URI of the Request, the mandatory headers of the message.
  This new Request does not contain a body."
  [method req-uri from call-id & more-headers]
  (let [headers (apply hash-map (mapcat #(vector (.getName ^Header %) %) (remove nil? (flatten more-headers))))
        cseq (get headers "CSeq" (header/cseq 1 method))
        to (get headers "To" (header/to (addr/address req-uri) nil))
        via (remove nil? [(get headers "Via")])
        max-forward (get headers "Max-Forwards" (header/max-forwards 70))
        request (.createRequest factory req-uri method call-id cseq from to via max-forward)
        remain-headers (remove #(#{"From" "Call-ID" "CSeq" "To" "Via" "Max-Forwards"} %) (keys headers))
        remain-headers (vals (select-keys headers remain-headers))]
    (doseq [h remain-headers] (.setHeader request h))
    request))

(defn response
  "Creates a new Response message of type specified by the status-code paramater,
  based on a specific Request message. This new Response does not contain a body.
  Only the required headers are copied from the Request."
  [status-code, ^Request reqest & more-headers]
  {:pre [(let [headers (remove nil? (flatten more-headers))]
           (or (= 0 (count headers)) (every? header/header? headers)))]}
  (let [response (.createResponse factory status-code reqest)
        headers (remove nil? (flatten more-headers))]
    (doseq [h headers] (.setHeader response h))
    response))

(defn reason
  "Gets the reason phrase of this Response message."
  [^Response response]
  (str (.getStatusCode response) \space (.getReasonPhrase response)))

(defn inc-sequence-number!
  "Increase the sequence number of a request's CSeq header."
  [^Request request]
  (let [sequence-number  (inc (.getSequenceNumber ^CSeqHeader (.getHeader request "CSeq")))
        new-cseq-header  (header/cseq sequence-number (.getMethod request))]
    (.setHeader request new-cseq-header)
    request))
