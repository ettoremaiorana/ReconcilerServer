package com.fourcasters.forec.reconciler.server.trades;

import com.fourcasters.forec.reconciler.EmailSender;
import com.fourcasters.forec.reconciler.server.*;
import com.fourcasters.forec.reconciler.server.trades.persist.TradePersister;
import com.fourcasters.forec.reconciler.server.trades.persist.TradeTaskFactory;
import com.fourcasters.forec.reconciler.server.trades.persist.TransactionManager;
import org.apache.logging.log4j.LogManager;
import org.zeromq.ZMQ;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TradeModule implements Module {
    private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(TradeModule.class);
    private final ApplicationInterface application;
    private final TradeReconcilerMessageSender reconcMessageSender;
    private final StrategiesTracker strategiesTracker;
    private final EmailSender emailSender;
    private final ZmqMessageHandlerFactory handlersFactory;
    private Consumer<TradeReconcilerMessageSender> openTradesSchedule;
    private ScheduledFuture<?> future;
    private TradePersister persister;
    private TradeEventCapturer tradeAppender;

    public TradeModule(ApplicationInterface application, ZMQ.Context ctx, ZmqMessageHandlerFactory handlersFactory) {
        this.application = application;
        this.reconcMessageSender = new TradeReconcilerMessageSender(ctx);
        this.strategiesTracker = new StrategiesTracker(new InitialStrategiesLoader());
        this.emailSender = new EmailSender();
        this.handlersFactory = handlersFactory;
    }

    @Override
    public void start() {
        persister  = new TradePersister(new TransactionManager(new TradeTaskFactory(application, reconcMessageSender), application), application);
        tradeAppender = new TradeEventCapturer(application, reconcMessageSender, strategiesTracker, emailSender);

        handlersFactory.registerMessageHandler(persister, "HISTORY");
        handlersFactory.registerMessageHandler(tradeAppender, "STATUS");

        openTradesSchedule = t-> {
            LOG.info("New scheduled task, asking for open trades");
            application.submit(
                    () -> t.askForOpenTrades("RECONC@ACTIVTRADES@EURUSD@1002")
            );
        };
        future = application.scheduleAtFixedRate(
                () -> openTradesSchedule.accept(reconcMessageSender), 120L, 5L, TimeUnit.MINUTES);

    }

    @Override
    public void stop() {
        if (future != null) {
            future.cancel(false);
        }
    }
}
