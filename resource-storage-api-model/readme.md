# resource-storage-api-model

This model contains requests for storing, downloading and deleting resources. The base request type is `ResourcePayloadRequest` and there is a single processor in a `Reflex` app - `ResourcePayloadProcessor` - that simply resolves the correct `ResourceStorage` based on the type of the `ResourceSource` type (plus we can specify storage directory when storing a new one), and passes the request to it.

Note that this layer is strictly dealing with resource payload, i.e. the binary data, and thus works with `ResourceSource`s, rather than `Resource` objects. It does not offer any meta-data enriching (e.g. determining image dimensions).


## Comparison to `resource-api-model`

The `resource-api-model` with requests like `GetResource` is relevant in the context of an `IncrementalAccess`. These requests reference `Resource` objects, the processor (for existing resources) retrieves the corresponding `ResourceSource` and forwards each the request to the corresponding `ResourceStorageRequest` from our model.