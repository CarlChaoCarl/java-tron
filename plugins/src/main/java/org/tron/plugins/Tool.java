package org.tron.plugins;

import picocli.CommandLine;

@CommandLine.Command(name = "tool",
    mixinStandardHelpOptions = true,
    version = "tool command 1.0",
    description = "An rich command set that provides high-level operations  for tools.",
    subcommands = {CommandLine.HelpCommand.class,
        BlockTransAccount.class,
        AccountBalanceDiff.class,
        AccountPermissionDiff.class
    },
    commandListHeading = "%nCommands:%n%nThe most commonly used db commands are:%n"
)
public class Tool {
}
