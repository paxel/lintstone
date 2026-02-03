package paxel.lintstone.impl;

import lombok.NonNull;
import paxel.lintstone.api.*;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This wraps the actual implementation of an Actor and makes sure that inside the actor system everything
 * is well synchronized.
 */
class Actor {

    private static final Logger LOG = Logger.getLogger(Actor.class.getName());

    private final @NonNull String name;

    private final @NonNull LintStoneActor actorInstance;
    private final @NonNull SequentialProcessor sequentialProcessor;
    private volatile boolean registered = true;
    private final @NonNull AtomicLong totalMessages = new AtomicLong();
    private final @NonNull AtomicLong totalReplies = new AtomicLong();
    private final @NonNull MessageContextFactory messageContextFactory;
    private final @NonNull Scheduler scheduler;
    private final int queueLimit;

    private final @NonNull ConcurrentLinkedQueue<MessageTask> taskPool = new ConcurrentLinkedQueue<>();

    Actor(@NonNull String name, @NonNull LintStoneActor actorInstance, @NonNull SequentialProcessor sequentialProcessor, @NonNull ActorSystem system, SelfUpdatingActorAccessor sender, @NonNull Scheduler scheduler, int queueLimit) {
        this.name = name;
        this.actorInstance = actorInstance;
        this.sequentialProcessor = sequentialProcessor;
        this.scheduler = scheduler;
        this.queueLimit = queueLimit;
        messageContextFactory = new MessageContextFactory(system, new SelfUpdatingActorAccessor(name, this, system, sender));
    }


    boolean isValid() {
        return registered;
    }

    void send(@NonNull Object message, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler) throws UnregisteredRecipientException {
        if (!registered) {
            throw new UnregisteredRecipientException("Actor " + name + " is not registered");
        }

        if (queueLimit > 0) {
            try {
                send(message, sender, replyHandler, queueLimit);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // If we are interrupted, we fallback to non-blocking add to avoid losing message if possible,
                // or just accept that we are interrupted. 
                // Given the API doesn't allow InterruptedException, this is a compromise.
            }
        }

        sequentialProcessor.add(createTask(message, sender, replyHandler));
        totalMessages.incrementAndGet();
    }

    void send(@NonNull Object message, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler, @NonNull Duration delay) throws UnregisteredRecipientException {
        scheduler.runLater(() -> {
            if (registered) {
                sequentialProcessor.add(createTask(message, sender, replyHandler));
                totalMessages.incrementAndGet();
            }
        }, delay);
    }


    void send(@NonNull Object message, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler, int blockThreshold) throws UnregisteredRecipientException, InterruptedException {
        if (!registered) {
            throw new UnregisteredRecipientException("Actor " + name + " is not registered");
        }

        MessageTask task = createTask(message, sender, replyHandler);
        if (!sequentialProcessor.addWithBackPressure(task, blockThreshold)) {
            taskPool.offer(task);
            throw new IllegalStateException("The sequential processor rejected the message.");
        }
        totalMessages.incrementAndGet();
    }

    private MessageTask createTask(@NonNull Object message, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler) {
        MessageTask task = taskPool.poll();
        if (task == null) {
            task = new MessageTask();
        }
        task.reset(message, sender, replyHandler);
        return task;
    }

    private class MessageTask implements Runnable {
        private @NonNull Object message;
        private SelfUpdatingActorAccessor sender;
        private ReplyHandler replyHandler;

        void reset(@NonNull Object message, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler) {
            this.message = message;
            this.sender = sender;
            this.replyHandler = replyHandler;
        }

        @Override
        public void run() {
            // create mec and delegate replies to our handleReply method
            MessageContext mec = messageContextFactory.create(message, (msg, self) -> Actor.this.handleReply(msg, self, sender, replyHandler));
            // process message
            try {
                actorInstance.newMessageEvent(mec);
            } catch (Exception e) {
                if (sender != null) {
                    sender.tell(new FailedMessage(message, e, name));
                } else {
                    LOG.log(Level.SEVERE, "While processing " + message + " on " + name + ":", e);
                }
                throw e;
            } finally {
                taskPool.offer(this);
            }
        }
    }

    /**
     * This method decides how to handle replys.
     *
     * @param reply        The msg that was replied
     * @param sender       The sender of the message we reply to
     * @param replyHandler The handler for the reply.
     *                     If this is null, we just send the msg to the sender.
     *                     The sender will receive this message in no relation to the previous msg he sent.
     *                     If the replyHandler is given, the relation between msg and reply is well-defined.
     *                     All reply during the handling of an ask are delegated to the replyHandler.
     */
    private void handleReply(@NonNull Object reply, @NonNull SelfUpdatingActorAccessor self, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler) {
        if (replyHandler == null) {
            // we don't have to handle this other than just sending it to the sender of the original message.
            Optional.ofNullable(sender)
                    .orElseThrow(() -> new NoSenderException("Message has no Sender"))
                    .send(reply, self);
        } else if (sender != null) {
            // we have a reply handler and a sender. so we want the sender to execute the result itself
            sender.run(replyHandler, reply);
        } else {
            // result handler without sender. this was asked from outside.
            // we could just execute the runnable here, but then the processing of the msg would be "interrupted" with the processing
            // of the reply. so we enqueue it in ourselves.
            self.run(replyHandler, reply);
        }
    }

    void unregisterGracefully() {
        registered = false;
        sequentialProcessor.unregisterGracefully();
    }

    void shutdown(boolean now) {
        sequentialProcessor.shutdown(now);
    }

    private final ConcurrentLinkedQueue<ReplyTask> replyTaskPool = new ConcurrentLinkedQueue<>();

    public void run(ReplyHandler replyHandler, @NonNull Object reply) {
        if (!registered) {
            throw new UnregisteredRecipientException("Actor " + name + " is not registered");
        }

        ReplyTask task = replyTaskPool.poll();
        if (task == null) {
            task = new ReplyTask();
        }
        task.reset(replyHandler, reply);
        sequentialProcessor.add(task);
        totalReplies.incrementAndGet();
    }

    private class ReplyTask implements Runnable {
        private ReplyHandler replyHandler;
        private @NonNull Object reply;

        void reset(ReplyHandler replyHandler, @NonNull Object reply) {
            this.replyHandler = replyHandler;
            this.reply = reply;
        }

        @Override
        public void run() {
            // we update the message context with the reply and give it to the reply handler
            MessageContext mec = messageContextFactory.create(reply, (msg, self) -> Actor.this.handleReply(msg, self, null, null));
            try {
                replyHandler.process(mec);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "While processing runnable on " + name + ":", e);
                throw e;
            } finally {
                replyTaskPool.offer(this);
            }
        }
    }

    @Override
    public String toString() {
        return "Actor{" +
                "name='" + name + '\'' +
                " registered='" + registered + '\'' +
                " total='" + totalMessages.get() + '\'' +
                " queued='" + sequentialProcessor.size() + '\'' +
                '}';
    }

    public long getTotalMessages() {
        return totalMessages.get();
    }

    public long getTotalReplies() {
        return totalReplies.get();
    }

    public int getQueued() {
        return sequentialProcessor.size();
    }
}
