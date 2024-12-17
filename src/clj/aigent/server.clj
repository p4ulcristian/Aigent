(ns aigent.server
    (:require
     [aigent.handler :refer [app]]
     [config.core :refer [env]]
     [clj-http.client :as client]
     [cheshire.core :as json]
     [ring.adapter.jetty :refer [run-jetty]])
    (:gen-class))

(defn handle-line [line]
   (let [data-str (subs line 6)
         json-str (json/parse-string data-str)
         edn-str  (read-string (pr-str json-str))
         choices  (get edn-str "choices")
         content  (get-in (first choices) ["delta" "content"])]
     (println content)))


(def example-messages
  [{:role "system"
    :content "Always answer in rhymes."}
   {:role "user"
    :content "Introduce yourself."}])

(defn chat-request [messages]
  (let [url "http://192.168.1.117:1234/v1/chat/completions"
        payload {:model "lmstudio-community/Meta-Llama-3.1-8B-Instruct-GGUF"
                 :messages messages
                 :temperature 0.7
                 :max_tokens -1
                 :stream true}
        headers {"Content-Type" "application/json"}
        response (client/post url
                              {:body (json/generate-string payload)
                               :headers headers
                               :as :stream})]
    (with-open [rdr (clojure.java.io/reader (:body response))]
      (doseq [line (line-seq rdr)] 
        
        (when-not (or (= 0 (count line))
                      (= "data: [DONE]" line))
          (handle-line line))))))


(chat-request example-messages)

(defn -main [& args]
  (let [port (or (env :port) 3000)]
    (run-jetty #'app {:port port :join? false})))
