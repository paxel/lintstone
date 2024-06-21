package paxel.lintstone.api;

@FunctionalInterface
public interface ErrorHandler {
    boolean handleError(Object a);
}
