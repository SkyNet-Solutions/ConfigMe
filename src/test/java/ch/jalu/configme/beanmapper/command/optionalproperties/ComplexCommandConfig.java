package ch.jalu.configme.beanmapper.command.optionalproperties;

import java.util.Map;

/**
 * Like {@link ch.jalu.configme.beanmapper.command.CommandConfig} but for {@link ComplexCommand}.
 */
public class ComplexCommandConfig {

    private Map<String, ComplexCommand> commands;
    private int duration;

    public Map<String, ComplexCommand> getCommands() {
        return commands;
    }

    public void setCommands(Map<String, ComplexCommand> commands) {
        this.commands = commands;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}

