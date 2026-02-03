package paxel.lintstone.impl;

import lombok.NonNull;
import paxel.lintstone.api.LintStoneEventHandler;
import paxel.lintstone.api.MessageAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * A specialized {@link MessageAccess} that records handlers instead of executing them immediately.
 */
class DecisionTreeBuilder implements MessageAccess {
    private final List<StaticDecisionTree.HandlerEntry<?>> handlers = new ArrayList<>();
    private LintStoneEventHandler<Object> otherwiseHandler;

    @Override
    public <T> @NonNull MessageAccess inCase(@NonNull Class<T> clazz, @NonNull LintStoneEventHandler<T> lintStoneEventHandler) {
        handlers.add(new StaticDecisionTree.HandlerEntry<>(clazz, lintStoneEventHandler));
        return this;
    }

    @Override
    public void otherwise(@NonNull LintStoneEventHandler<Object> catchAll) {
        this.otherwiseHandler = catchAll;
    }

    /**
     * Builds an optimized {@link DecisionTree} from the recorded handlers.
     *
     * @return the decision tree.
     */
    DecisionTree build() {
        return new StaticDecisionTree(handlers.toArray(new StaticDecisionTree.HandlerEntry[0]), otherwiseHandler);
    }
}
