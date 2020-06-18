(ns liberator-hal.health-resource.core-test
  (:require
   [clojure.test :refer :all]

   [halboy.resource :as hal]
   [halboy.json :as hal-json]

   [pathological.files :as files]

   [ring.mock.request :as ring]
   [ring.middleware.keyword-params :as ring-keyword-params]
   [ring.middleware.params :as ring-params]

   [liberator-hal.health-resource.core :as health-resource]))

(def discovery-route ["/" :discovery])
(def health-route ["/health" :health])

(defn routes [extras]
  [""
   (concat
     [discovery-route
      health-route]
     extras)])

(defn dependencies
  ([] (dependencies []))
  ([extra-routes]
   {:routes (routes extra-routes)}))

(defn resource-handler
  ([dependencies] (resource-handler dependencies {}))
  ([dependencies options]
   (-> (health-resource/handler dependencies options)
     ring-keyword-params/wrap-keyword-params
     ring-params/wrap-params)))

(deftest has-status-200
  (let [handler (resource-handler (dependencies))
        request (ring/request :get "/")
        result (handler request)]
    (is (= (:status result) 200))))

(deftest includes-self-link
  (let [handler (resource-handler (dependencies))
        request (ring/request :get "http://localhost/health")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :self) "http://localhost/health"))))

(deftest includes-discovery-link
  (let [handler (resource-handler (dependencies))
        request (ring/request :get "http://localhost/health")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :discovery) "http://localhost/"))))

(deftest includes-version-attribute-when-version-file-path-provided
  (let [version "1.2.0+fc9c14c"
        version-file-path (files/write-lines
                            (files/create-temp-file "version" ".txt")
                            [version])
        handler (resource-handler (dependencies)
                  {:version-file-path (str version-file-path)})
        request (ring/request :get "http://localhost/health")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (files/delete version-file-path)

    (is (= (hal/get-property resource :version) version))))
