package paxel.lintstone.api.actors;

import paxel.lintstone.api.ActorSettings;
import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneActorAccessor;
import paxel.lintstone.api.LintStoneMessageEventContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SortNodeActor implements LintStoneActor {
    private final String name;
    private Long value;
    private LintStoneActorAccessor left;
    private LintStoneActorAccessor right;

    public SortNodeActor(String name) {
        this.name = name;
    }

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec.inCase(Long.class, this::add)
                .inCase(String.class, this::get)
                .otherwise(this::err);
    }

    private void err(Object o, LintStoneMessageEventContext lintStoneMessageEventContext) {
        System.err.printf("Unsupported in " + this + " message %s: <%s>%n", o.getClass(), o);
    }

    private void get(String ignore, LintStoneMessageEventContext lintStoneMessageEventContext) {
        Optional.ofNullable(left)
                // ask left side for values, if exists
                .map(a -> a.<List<Long>>ask(ignore))
                .orElse(CompletableFuture.completedFuture(Collections.emptyList()))
                .thenCompose(leftValues ->
                        Optional.ofNullable(right)
                                // ask right side for values, if exists
                                .map(b -> b.<List<Long>>ask(ignore))
                                .orElse(CompletableFuture.completedFuture(Collections.emptyList()))
                                .thenApply(rightValues -> {
                                    // create sorted list of values under this actor
                                    List<Long> longs = sumList(leftValues, value, rightValues);
                                    lintStoneMessageEventContext.reply(longs);
                                    // kill self
                                    lintStoneMessageEventContext.unregister();
                                    return longs;
                                }));
    }

    private List<Long> sumList(List<Long> leftValues, Long value, List<Long> rightValues) {
        ArrayList<Long> longs = new ArrayList<>(leftValues);
        longs.add(value);
        longs.addAll(rightValues);
        return longs;
    }

    private void add(Long value, LintStoneMessageEventContext lintStoneMessageEventContext) {
        if (this.value == null || this.value.equals(value))
            // first value that we receive // same value received
            this.value = value;
        else if (this.value > value) {
            // lesser than us. delegate to left actor
            if (this.left == null)
                this.left = lintStoneMessageEventContext.registerActor(name + "<", () -> new SortNodeActor(name + "<"),value, ActorSettings.DEFAULT);
            else
                left.tell(value);
        } else {
            // bigger than us. delegate to right actor
            if (this.right == null)
                this.right = lintStoneMessageEventContext.registerActor(name + ">", () -> new SortNodeActor(name + ">"), value, ActorSettings.DEFAULT);
            else
                right.tell(value);
        }
    }

    @Override
    public String toString() {
        return "SortNodeActor{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", left=" + left +
                ", right=" + right +
                '}';
    }
}
