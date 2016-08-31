package com.github.tomakehurst.wiremock.extension.webhooks;

import com.github.tomakehurst.wiremock.AcceptanceTestBase;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.extension.webhooks.Webhooks.webhook;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

public class WebhooksAcceptanceTest extends AcceptanceTestBase {

    static Webhooks webhooks = new Webhooks();

    static CountDownLatch latch;

    WireMockTestClient client;

    @ClassRule
    public static WireMockRule wm = new WireMockRule(
        options()
            .dynamicPort()
            .extensions(webhooks));



    @BeforeClass
    public static void initClass() {
        stubFor(get(anyUrl())
            .willReturn(aResponse().withStatus(200)));

        wireMockServer.addMockServiceRequestListener(new RequestListener() {
            @Override
            public void requestReceived(Request request, Response response) {
                latch.countDown();
            }
        });
    }

    @Before
    public void init() {
        reset();
        latch = new CountDownLatch(1);
        client = new WireMockTestClient(wm.port());
    }

    @Test
    public void firesASingleWebhookWhenRequested() throws Exception {
        wm.stubFor(post(urlPathEqualTo("/something-async"))
            .willReturn(aResponse().withStatus(200))
            .withPostServeAction("webhook", webhook()
                .withMethod(POST)
                .withUrl("http://localhost:" + wireMockServer.port() + "/callback")
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"result\": \"SUCCESS\" }"))
        );

        verify(0, postRequestedFor(anyUrl()));

        client.post("/something-async", new StringEntity("", TEXT_PLAIN));

        latch.await(1, SECONDS);

        verify(1, postRequestedFor(urlEqualTo("/callback"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson("{ \"result\": \"SUCCESS\" }"))
        );
    }

}
