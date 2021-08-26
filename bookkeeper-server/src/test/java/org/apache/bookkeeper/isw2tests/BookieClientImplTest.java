package org.apache.bookkeeper.isw2tests;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.bookkeeper.client.BookieWatcher;
import org.apache.bookkeeper.common.util.OrderedExecutor;
import org.apache.bookkeeper.conf.ClientConfiguration;
import org.apache.bookkeeper.net.BookieId;
import org.apache.bookkeeper.net.BookieSocketAddress;
import org.apache.bookkeeper.proto.BookieClient;
import org.apache.bookkeeper.proto.BookieClientImpl;
import org.apache.bookkeeper.stats.NullStatsLogger;
import org.apache.commons.lang.SystemUtils;
import org.junit.Before;
import org.junit.Test;

import io.netty.buffer.PooledByteBufAllocator;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Unit test for BookieClientImpl.
 */
public class BookieClientImplTest {
    private ClientConfiguration conf = new ClientConfiguration();
    private EventLoopGroup eventLoop;
    BookieWatcher watcher;
    OrderedExecutor executor;
    ScheduledExecutorService scheduler;
    BookieClient client;

    @Before
    public void setup() {
        executor = mock(OrderedExecutor.class);
        scheduler = mock(ScheduledExecutorService.class);
        watcher = mock(BookieWatcher.class);
        if (SystemUtils.IS_OS_LINUX) {
            try {
                eventLoop = new EpollEventLoopGroup();
            } catch (Throwable t) {
                // LOG.warn("Could not use Netty Epoll event loop for benchmark {}", t.getMessage());
                eventLoop = new NioEventLoopGroup();
            }
        } else {
            eventLoop = new NioEventLoopGroup();
        }
        try {
            client = new BookieClientImpl(conf, eventLoop, PooledByteBufAllocator.DEFAULT, executor, scheduler,
            NullStatsLogger.INSTANCE, BookieSocketAddress.LEGACY_BOOKIEID_RESOLVER);
        } catch (Exception e) {
            //TODO: handle exception
        }
    }

    @Test
    public void faultyBookiesShouldBeNull() throws IOException {       
        List<BookieId> temp = client.getFaultyBookies();
        assertEquals(temp.size(), 0);
    }
}
