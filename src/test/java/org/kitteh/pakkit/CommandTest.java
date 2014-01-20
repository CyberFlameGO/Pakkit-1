package org.kitteh.pakkit;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.bukkit.command.CommandSender;
import org.junit.Assert;
import org.junit.Test;
import org.kitteh.pakkit.Command.Args;
import org.kitteh.pakkit.Command.SubCommand;

public final class CommandTest {
    @Test
    public void annotations() {
        for (Method method : Command.class.getDeclaredMethods()) {
            SubCommand sub = method.getAnnotation(SubCommand.class);
            if (sub == null) {
                continue;
            }
            String n = method.getName() + ": ";
            Assert.assertTrue(n + "NULL ARGS", sub.arg() != null);
            Assert.assertTrue(n + "STATIC", !Modifier.isStatic(method.getModifiers()));
            Class<?>[] params = method.getParameterTypes();
            Assert.assertTrue(n + "DOES NOT TAKE SENDER,ARGS", params.length == 2 && params[0].equals(CommandSender.class) && params[1].equals(Args.class));
        }
    }
}
