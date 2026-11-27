// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.web.endpoints.test;

import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.junit.Test;
import hiconic.platform.reflex.web_server.processing.ResponseCompressionHandler;
import hiconic.rx.web.server.model.config.HttpResponseCompression;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;

public class ResponseCompressionTest {
    private static final byte[] TEXT = "Olá Welt — structured response.\n".repeat(1024).getBytes(StandardCharsets.UTF_8);
    private static final HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
    private static byte[] unzip(byte[] data) throws IOException {try (var input = new GZIPInputStream(new ByteArrayInputStream(data))) {return input.readAllBytes();}}
    private static byte[] zip(byte[] data) throws IOException {var output = new ByteArrayOutputStream();try (var gzip = new GZIPOutputStream(output)) {gzip.write(data);}return output.toByteArray();}
    private static HttpHandler content(String type, byte[] data, Map<String,String> headers) {
        return exchange -> {if(type!=null)exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,type);headers.forEach((name,value)->exchange.getResponseHeaders().put(new io.undertow.util.HttpString(name),value));exchange.setResponseContentLength(data.length);exchange.getResponseSender().send(ByteBuffer.wrap(data));};
    }
    private static class Server implements AutoCloseable {
        final Undertow server;final URI uri;
        Server(HttpResponseCompression config,HttpHandler next) {server=Undertow.builder().setIoThreads(1).setWorkerThreads(2).addHttpListener(0,"127.0.0.1").setHandler(ResponseCompressionHandler.wrap(next,config)).build();server.start();uri=URI.create("http://127.0.0.1:"+((InetSocketAddress)server.getListenerInfo().get(0).getAddress()).getPort()+"/");}
        HttpResponse<byte[]> get(String encoding) throws Exception {var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5));if(encoding!=null)builder.header("Accept-Encoding",encoding);return CLIENT.send(builder.build(),HttpResponse.BodyHandlers.ofByteArray());}
        public void close(){server.stop();}
    }
    @Test public void defaultCompressesLargeTextAndPreservesRepresentation() throws Exception {
        try(var server=new Server(null,content("text/plain; charset=UTF-8",TEXT,Map.of("ETag","\"source-hash\"","Vary","Origin")))){
            var response=server.get("gzip");assertEquals(200,response.statusCode());assertEquals("gzip",response.headers().firstValue("Content-Encoding").orElse(null));assertArrayEquals(TEXT,unzip(response.body()));assertTrue(response.body().length<TEXT.length/4);assertEquals("W/\"source-hash\"",response.headers().firstValue("ETag").orElse(null));
            assertTrue(String.join(",",response.headers().allValues("Vary")).contains("Origin"));assertTrue(String.join(",",response.headers().allValues("Vary")).contains("Accept-Encoding"));
            var identity=server.get(null);assertArrayEquals(TEXT,identity.body());assertEquals("\"source-hash\"",identity.headers().firstValue("ETag").orElse(null));assertFalse(identity.headers().firstValue("Content-Encoding").orElse("").equals("gzip"));
        }
    }
    @Test public void knownLengthThresholdIsInclusiveAndConfigurable() throws Exception {
        for(int length:List.of(2047,2048))try(var server=new Server(null,content("application/json",new byte[length],Map.of()))){var response=server.get("gzip");assertEquals(length>=2048,response.headers().firstValue("Content-Encoding").orElse("").equals("gzip"));}
        var config=HttpResponseCompression.T.create();config.setMinimumResponseBytes(0);
        try(var server=new Server(config,content("text/plain",new byte[3],Map.of()))){assertEquals("gzip",server.get("gzip").headers().firstValue("Content-Encoding").orElse(null));}
    }
    @Test public void nativeNegotiationHonorsQValuesAndIdentity() throws Exception {
        try(var server=new Server(null,content("application/xml",TEXT,Map.of()))){
            for(String encoding:List.of("gzip;q=0","gzip;q=0.5, identity;q=1","identity"))assertArrayEquals(TEXT,server.get(encoding).body());
            assertArrayEquals(TEXT,unzip(server.get("gzip;q=1, identity;q=0").body()));
            assertEquals(406,server.get("br, identity;q=0").statusCode());
        }
    }
    @Test public void defaultTypesIncludeModelsAndRpcButExcludePackedBinaryAndSse() throws Exception {
        for(String type:List.of("application/problem+json","application/custom+xml","application/javascript","application/yaml","image/svg+xml","gm/bin","multipart/chunked","multipart/form-data; boundary=example"))
            try(var server=new Server(null,content(type,TEXT,Map.of()))){assertEquals(type,"gzip",server.get("gzip").headers().firstValue("Content-Encoding").orElse(null));}
        for(String type:List.of("application/java-archive","application/zip","application/gzip","image/png","application/octet-stream","text/event-stream; charset=UTF-8"))
            try(var server=new Server(null,content(type,TEXT,Map.of()))){assertArrayEquals(type,TEXT,server.get("gzip").body());}
    }
    @Test public void customTypesReplaceDefaultsAndDisabledMeansUnchangedTransport() throws Exception {
        var config=HttpResponseCompression.T.create();config.setMimeTypes(Set.of("application/x-special"));config.setLevel(3);
        try(var server=new Server(config,content("application/x-special",TEXT,Map.of()))){assertArrayEquals(TEXT,unzip(server.get("gzip").body()));}
        try(var server=new Server(config,content("text/plain",TEXT,Map.of()))){assertArrayEquals(TEXT,server.get("gzip").body());}
        config.setEnabled(false);
        try(var server=new Server(config,content("application/x-special",TEXT,Map.of("Vary","Origin")))){var response=server.get("gzip");assertArrayEquals(TEXT,response.body());assertEquals(List.of("Origin"),response.headers().allValues("Vary"));}
    }
    @Test public void rangesNoTransformAndPreEncodedResponsesArePreserved() throws Exception {
        try(var server=new Server(null,exchange->{exchange.setStatusCode(206);content("text/plain",TEXT,Map.of("Content-Range","bytes 0-10/100")).handleRequest(exchange);})){assertArrayEquals(TEXT,server.get("gzip").body());}
        try(var server=new Server(null,content("text/plain",TEXT,Map.of()))){var response=CLIENT.send(HttpRequest.newBuilder(server.uri).header("Accept-Encoding","gzip").header("Range","bytes=0-10").build(),HttpResponse.BodyHandlers.ofByteArray());assertArrayEquals(TEXT,response.body());}
        try(var server=new Server(null,content("text/plain",TEXT,Map.of("Cache-Control","private, No-Transform")))){assertArrayEquals(TEXT,server.get("gzip").body());}
        byte[] compressed=zip(TEXT);
        try(var server=new Server(null,content("text/plain",compressed,Map.of("Content-Encoding","gzip","ETag","\"compressed-hash\"","Vary","*")))){var response=server.get("gzip");assertArrayEquals(compressed,response.body());assertEquals("\"compressed-hash\"",response.headers().firstValue("ETag").orElse(null));assertEquals(List.of("*"),response.headers().allValues("Vary"));}
    }
    @Test public void headAndBodylessStatusesNeverEmitAnEncodedBody() throws Exception {
        try(var server=new Server(null,content("text/plain",TEXT,Map.of("ETag","\"source\"")))){var response=CLIENT.send(HttpRequest.newBuilder(server.uri).header("Accept-Encoding","gzip").method("HEAD",HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(0,response.body().length);assertEquals("gzip",response.headers().firstValue("Content-Encoding").orElse(null));assertEquals("W/\"source\"",response.headers().firstValue("ETag").orElse(null));assertTrue(response.headers().firstValue("Content-Length").isEmpty());}
        for(int status:List.of(204,205,304))try(var server=new Server(null,exchange->{exchange.setStatusCode(status);exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,"text/plain");exchange.endExchange();})){var response=server.get("gzip");assertEquals(status,response.statusCode());assertEquals(0,response.body().length);assertFalse(response.headers().firstValue("Content-Encoding").orElse("").equals("gzip"));}
    }
    @Test public void unknownLengthFlushIsObservableBeforeTheResponseFinishes() throws Exception {
        var release=new CountDownLatch(1);
        try(var server=new Server(null,exchange->exchange.dispatch(()->{try{exchange.startBlocking();exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,"multipart/chunked");var output=exchange.getOutputStream();output.write(TEXT);output.flush();if(!release.await(5,TimeUnit.SECONDS))throw new IOException("Client never received the flushed prefix");output.write("tail".getBytes(StandardCharsets.UTF_8));output.close();}catch(Exception failure){throw new RuntimeException(failure);}}))){
            var future=CompletableFuture.supplyAsync(()->{try{var response=CLIENT.send(HttpRequest.newBuilder(server.uri).header("Accept-Encoding","gzip").timeout(Duration.ofSeconds(4)).build(),HttpResponse.BodyHandlers.ofInputStream());return new GZIPInputStream(response.body());}catch(Exception failure){throw new CompletionException(failure);}});
            try{var input=future.get(3,TimeUnit.SECONDS);var prefix=CompletableFuture.supplyAsync(()->{try{return input.readNBytes(32);}catch(IOException failure){throw new UncheckedIOException(failure);}});assertArrayEquals(Arrays.copyOf(TEXT,32),prefix.get(2,TimeUnit.SECONDS));release.countDown();try(input){assertEquals(TEXT.length-32+4,input.readAllBytes().length);}}
            finally{release.countDown();}
        }
    }
    @Test public void invalidConfigurationFailsAtStartup() {
        var config=HttpResponseCompression.T.create();config.setMinimumResponseBytes(-1);assertThrows(IllegalArgumentException.class,()->ResponseCompressionHandler.wrap(exchange->{},config));config.setMinimumResponseBytes(0);config.setLevel(10);assertThrows(IllegalArgumentException.class,()->ResponseCompressionHandler.wrap(exchange->{},config));config.setLevel(1);config.setMimeTypes(Set.of("broken"));assertThrows(IllegalArgumentException.class,()->ResponseCompressionHandler.wrap(exchange->{},config));
    }
}
