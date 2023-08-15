package paxel.lintstone.impl;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import paxel.lintstone.api.LintStoneFailedMessage;

@ToString
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class FailedMessage implements LintStoneFailedMessage {

    private final Object message;
    private final Throwable cause;
    private final String actorName;


}
