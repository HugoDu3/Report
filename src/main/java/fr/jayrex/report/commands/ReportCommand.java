package fr.jayrex.report.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.jayrex.report.Report;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class ReportCommand extends Command {

    private final Report plugin;
    private final Map<UUID, Long> cooldown;

    public ReportCommand(final Report plugin) {
        super("report", "report.use");
        this.plugin = plugin;
        cooldown = new HashMap<>();
    }

    public UUID getSenderUniqueId(@NonNull CommandSender sender) {
        return (sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId() : new UUID(0, 0));
    }

    @Override
    @SneakyThrows
    public void execute(final CommandSender sender, final String[] args) {

        List<String> toggledUUIDS = plugin.getData().getStringList("toggled");
        String senderUUIDS = getSenderUniqueId(sender).toString();

        if (args.length == 1 && args[0].equalsIgnoreCase("toggle") && sender.hasPermission("report.see")) {
            if (toggledUUIDS.contains(senderUUIDS)) {
                toggledUUIDS.remove(senderUUIDS);
                sender.sendMessage(new ComponentBuilder("Vous verrez à nouveau les messages de report.")
                        .color(ChatColor.GREEN).create());
            } else {
                toggledUUIDS.add(senderUUIDS);
                sender.sendMessage(new ComponentBuilder("Vous ne verrez plus les messages de report.")
                        .color(ChatColor.GREEN).create());
            }
            plugin.getData().set("toggled", toggledUUIDS);
            return;
        } else if (args.length < 2) {
            sender.sendMessage(new ComponentBuilder("Utilisation: /report <joueur> <raison>").color(ChatColor.RED).create());
            return;
        }

        if (cooldown.getOrDefault(getSenderUniqueId(sender), 0L) > System.currentTimeMillis()) {
            sender.sendMessage(new ComponentBuilder("Vous pourrez utilisez le report dans ").color(ChatColor.RED)
                    .append("" + getSeconds(getSenderUniqueId(sender))).color(ChatColor.GRAY).append(" secondes!")
                    .color(ChatColor.RED).create());
            return;
        }

        final String message = Stream.of(args).skip(1).collect(Collectors.joining(" "));

        final Collection<String> usernames = plugin.getProxy().getPlayers().stream().map(pi -> pi.getName())
                .collect(Collectors.toList());
        final String targetName = usernames.stream().filter(u -> u.equalsIgnoreCase(args[0])).findFirst().orElse(null);
        if (targetName == null) {
            sender.sendMessage(new ComponentBuilder("Erreur: Le joueur '" + args[0] + "' n'est pas en ligne.")
                    .color(ChatColor.RED).create());
            return;
        }

        final UUID targetUUID = plugin.getProxy().getPlayer(targetName).getUniqueId();
        final ServerInfo targetServer = plugin.getProxy().getPlayer(targetUUID).getServer().getInfo();

        final BaseComponent[] msgSubmitted = new ComponentBuilder("Vous avez émis un report contre ")
                .color(ChatColor.RED).append(targetName).color(ChatColor.GRAY).append(" pour: ").color(ChatColor.RED)
                .append(message).color(ChatColor.GRAY).create();
        sender.sendMessage(msgSubmitted);

        cooldown.put(getSenderUniqueId(sender),
                System.currentTimeMillis() + (plugin.getConfig().getInt("cooldown") * 1000));

        final BaseComponent[] msgReported = new ComponentBuilder(sender.getName()).color(ChatColor.GRAY)
                .append(" a report ").color(ChatColor.RED).append(targetName).color(ChatColor.GRAY).append(" pour ")
                .color(ChatColor.RED).append(message).color(ChatColor.GRAY).append(" Serveur: ").color(ChatColor.RED)
                .append(targetServer.getName()).color(ChatColor.GRAY).create();

        plugin.getProxy().getPlayers().stream().filter(p -> p.hasPermission("report.see"))
                .filter(p -> !toggledUUIDS.contains(p.getUniqueId().toString()))
                .forEach(p -> p.sendMessage(msgReported));

        final String webhookString = plugin.getConfig().getString("webhook_url", "");
        if (!webhookString.isEmpty()) {
            final WebhookClient client = new WebhookClientBuilder(webhookString).build();

            final WebhookMessage webmessage = new WebhookMessageBuilder()
                    .setContent(":bell: `" + sender.getName() + "` a report `" + targetName + "` pour `" + message
                            + "` Serveur: **" + targetServer.getName() + "**")
                    .setUsername("SpartaCube").build();
            client.send(webmessage).get();

            client.close();
        }

    }

    private long getSeconds(final UUID uuid) {
        final long time = cooldown.getOrDefault(uuid, 0L) - System.currentTimeMillis();
        return time < 0 ? 0 : time / 1000;
    }

}
