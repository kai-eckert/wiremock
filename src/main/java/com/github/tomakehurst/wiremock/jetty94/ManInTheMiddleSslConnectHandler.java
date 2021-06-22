package com.github.tomakehurst.wiremock.jetty94;

import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Promise;

import javax.servlet.http.HttpServletRequest;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import static com.google.common.base.MoreObjects.firstNonNull;

public class ManInTheMiddleSslConnectHandler extends ConnectHandler {

    private final ServerConnector mitmProxyConnector;

    public ManInTheMiddleSslConnectHandler(ServerConnector mitmProxyConnector) {
        this.mitmProxyConnector = mitmProxyConnector;
    }

    @Override
    protected void connectToServer(HttpServletRequest request, String ignoredHost, int ignoredPort, Promise<SocketChannel> promise) {
        SocketChannel channel = null;
        try
        {
            channel = SocketChannel.open();
            channel.socket().setTcpNoDelay(true);
            channel.configureBlocking(false);

            String host = firstNonNull(mitmProxyConnector.getHost(), "localhost");
            int port = mitmProxyConnector.getLocalPort();
            InetSocketAddress address = newConnectAddress(host, port);

            channel.connect(address);
            promise.succeeded(channel);
        }
        catch (Throwable x)
        {
            close(channel);
            promise.failed(x);
        }
    }

    private void close(Closeable closeable)
    {
        try
        {
            if (closeable != null)
                closeable.close();
        }
        catch (Throwable x)
        {
            LOG.ignore(x);
        }
    }
}
