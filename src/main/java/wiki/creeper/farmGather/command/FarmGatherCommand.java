package wiki.creeper.farmGather.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import wiki.creeper.farmGather.FarmGather;
import wiki.creeper.farmGather.player.HoeSkillType;
import wiki.creeper.farmGather.player.PlayerProfile;
import wiki.creeper.farmGather.util.ItemUtil;
import wiki.creeper.farmGather.util.Text;

public class FarmGatherCommand implements CommandExecutor, TabCompleter {
    private final FarmGather plugin;

    public FarmGatherCommand(FarmGather plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean isAdmin = sender.hasPermission("farmgather.admin");

        switch (sub) {
            case "reload" -> {
                if (requireAdmin(sender, isAdmin)) {
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(Text.colorize("&a설정을 리로드했습니다."));
            }
            case "givehoe" -> {
                if (requireAdmin(sender, isAdmin)) {
                    return true;
                }
                handleGiveHoe(sender, args);
            }
            case "givebasic" -> {
                if (requireAdmin(sender, isAdmin)) {
                    return true;
                }
                handleGiveBasic(sender, args);
            }
            case "xp" -> {
                if (requireAdmin(sender, isAdmin)) {
                    return true;
                }
                handleXp(sender, args);
            }
            case "level" -> {
                if (requireAdmin(sender, isAdmin)) {
                    return true;
                }
                handleLevel(sender, args);
            }
            case "stats" -> handleStats(sender, args, isAdmin);
            case "world" -> {
                if (requireAdmin(sender, isAdmin)) {
                    return true;
                }
                handleWorld(sender, args);
            }
            default -> sender.sendMessage(Text.colorize("&c알 수 없는 하위 명령입니다. /fg help 를 확인하세요."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean isAdmin = sender.hasPermission("farmgather.admin");
        if (args.length == 1) {
            if (isAdmin) {
                return List.of("reload", "givehoe", "givebasic", "xp", "level", "stats", "world");
            }
            return List.of("stats");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("givehoe") && isAdmin) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("givehoe") && isAdmin) {
            return Arrays.stream(HoeSkillType.values())
                    .map(type -> type.name().toLowerCase(Locale.ROOT))
                    .toList();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("givehoe") && isAdmin) {
            return List.of("1", "2", "3", "4", "5");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("givebasic") && isAdmin) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }
        if ((args[0].equalsIgnoreCase("xp") || args[0].equalsIgnoreCase("level")) && isAdmin) {
            if (args.length == 2) {
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
            }
            if (args.length == 3) {
                return args[0].equalsIgnoreCase("xp")
                        ? List.of("add", "get")
                        : List.of("set", "add", "get");
            }
        }
        if (args[0].equalsIgnoreCase("world") && isAdmin) {
            if (args.length == 2) {
                return List.of("reset");
            }
            if (args.length == 3) {
                return List.of("now");
            }
        }
        if (args[0].equalsIgnoreCase("stats")) {
            if (args.length == 2 && isAdmin) {
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
            }
        }
        return List.of();
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Text.colorize("&6[FarmGather] 사용 가능한 명령어"));
        sender.sendMessage(Text.colorize("&e/fg stats [player] &7- 현재 레벨/경험치를 확인합니다."));
        sender.sendMessage(Text.colorize("&e/fg givebasic <player> &7- 기본 채집 호미 지급 (관리자)"));
        sender.sendMessage(Text.colorize("&e/fg givehoe <player> <skill> <level> &7- 스킬 호미 지급 (관리자)"));
        sender.sendMessage(Text.colorize("&e/fg xp <player> <add|get> [amount] &7- 경험치 관리 (관리자)"));
        sender.sendMessage(Text.colorize("&e/fg level <player> <set|add|get> [값] &7- 레벨 관리 (관리자)"));
        sender.sendMessage(Text.colorize("&e/fg world reset now &7- 채집 월드를 즉시 리셋 (관리자)"));
        sender.sendMessage(Text.colorize("&e/fg reload &7- 설정을 다시 불러옵니다. (관리자)"));
    }

    private boolean requireAdmin(CommandSender sender, boolean isAdmin) {
        if (!isAdmin) {
            sender.sendMessage(Text.colorize("&c권한이 없습니다."));
            return true;
        }
        return false;
    }

    private void handleGiveHoe(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Text.colorize("&c사용법: /fg givehoe <player> <skill> <level>"));
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Text.colorize("&c해당 플레이어를 찾을 수 없습니다."));
            return;
        }

        HoeSkillType type;
        try {
            type = HoeSkillType.fromKey(args[2]);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Text.colorize("&c알 수 없는 스킬입니다. 사용 가능: sweep, focus, doubletap, shears"));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Text.colorize("&c레벨은 1~5 사이의 숫자여야 합니다."));
            return;
        }

        if (level < 1 || level > 5) {
            sender.sendMessage(Text.colorize("&c레벨은 1~5 사이만 허용됩니다."));
            return;
        }

        ItemStack hoe = new ItemStack(Material.DIAMOND_HOE);
        ItemUtil.applyHoeSkill(hoe, type, level, plugin, target.getUniqueId());

        Map<Integer, ItemStack> leftovers = target.getInventory().addItem(hoe);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(stack -> target.getWorld().dropItemNaturally(target.getLocation(), stack));
        }

        String skillName = type.name().toLowerCase(Locale.ROOT);
        target.sendMessage(Text.colorize(String.format("&a%s Lv.%d 호미를 받았습니다!", skillName, level)));
        if (sender != target) {
            sender.sendMessage(Text.colorize(String.format("&a%s님에게 %s Lv.%d 호미를 지급했습니다.", target.getName(), skillName, level)));
        }
    }

    private void handleGiveBasic(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.colorize("&c사용법: /fg givebasic <player>"));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Text.colorize("&c해당 플레이어를 찾을 수 없습니다."));
            return;
        }
        ItemStack hoe = ItemUtil.createBasicHoe(plugin, target.getUniqueId());
        Map<Integer, ItemStack> leftovers = target.getInventory().addItem(hoe);
        leftovers.values().forEach(stack -> target.getWorld().dropItemNaturally(target.getLocation(), stack));
        target.sendMessage(Text.colorize("&a기본 채집 호미를 받았습니다."));
        if (sender != target) {
            sender.sendMessage(Text.colorize(String.format("&a%s님에게 기본 호미를 지급했습니다.", target.getName())));
        }
    }

    private void handleXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.colorize("&c사용법: /fg xp <player> <add|get> [양]"));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Text.colorize("&c해당 플레이어를 찾을 수 없습니다."));
            return;
        }
        PlayerProfile profile = plugin.getProfileManager().getProfile(target);
        if (profile == null) {
            sender.sendMessage(Text.colorize("&c플레이어 데이터가 아직 로드되지 않았습니다."));
            return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "get" -> sender.sendMessage(Text.colorize(String.format("&e%s: Lv.%d, XP %.2f", target.getName(), profile.getLevel(), profile.getXp())));
            case "add" -> {
                if (args.length < 4) {
                    sender.sendMessage(Text.colorize("&c지급할 경험치 양을 입력하세요."));
                    return;
                }
                double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(Text.colorize("&c숫자만 입력할 수 있습니다."));
                    return;
                }
                plugin.getProgressionService().addXp(profile, amount);
                sender.sendMessage(Text.colorize(String.format("&a%s의 경험치를 %.2f 만큼 추가했습니다. (현재 Lv.%d, XP %.2f)",
                        target.getName(), amount, profile.getLevel(), profile.getXp())));
                target.sendMessage(Text.colorize(String.format("&a관리자로부터 경험치 %.2f 을(를) 받았습니다.", amount)));
                flushProfileOrWarn(sender, target.getUniqueId());
            }
            default -> sender.sendMessage(Text.colorize("&c사용법: /fg xp <player> <add|get> [양]"));
        }
    }

    private void handleLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.colorize("&c사용법: /fg level <player> <set|add|get> [값]"));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Text.colorize("&c해당 플레이어를 찾을 수 없습니다."));
            return;
        }
        PlayerProfile profile = plugin.getProfileManager().getProfile(target);
        if (profile == null) {
            sender.sendMessage(Text.colorize("&c플레이어 데이터가 아직 로드되지 않았습니다."));
            return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        int cap = plugin.getProgressionService().getConfig().cap();
        switch (action) {
            case "get" -> sender.sendMessage(Text.colorize(String.format("&e%s: Lv.%d, XP %.2f", target.getName(), profile.getLevel(), profile.getXp())));
            case "set", "add" -> {
                if (args.length < 4) {
                    sender.sendMessage(Text.colorize("&c값을 입력하세요."));
                    return;
                }
                int value;
                try {
                    value = Integer.parseInt(args[3]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(Text.colorize("&c정수를 입력하세요."));
                    return;
                }
                int newLevel = profile.getLevel();
                if (action.equals("set")) {
                    newLevel = Math.max(1, Math.min(cap, value));
                } else {
                    newLevel = Math.max(1, Math.min(cap, profile.getLevel() + value));
                }
                profile.setLevel(newLevel);
                profile.setXp(0);
                sender.sendMessage(Text.colorize(String.format("&a%s의 레벨을 %d로 설정했습니다.", target.getName(), newLevel)));
                target.sendMessage(Text.colorize(String.format("&a관리자에 의해 레벨이 %d로 변경되었습니다.", newLevel)));
                flushProfileOrWarn(sender, target.getUniqueId());
            }
            default -> sender.sendMessage(Text.colorize("&c사용법: /fg level <player> <set|add|get> [값]"));
        }
    }

    private void handleStats(CommandSender sender, String[] args, boolean isAdmin) {
        if (!isAdmin && !sender.hasPermission("farmgather.stats")) {
            sender.sendMessage(Text.colorize("&c권한이 없습니다."));
            return;
        }
        Player target;
        if (args.length >= 2) {
            if (!isAdmin) {
                sender.sendMessage(Text.colorize("&c다른 플레이어의 정보를 보려면 관리자 권한이 필요합니다."));
                return;
            }
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Text.colorize("&c해당 플레이어를 찾을 수 없습니다."));
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Text.colorize("&c콘솔에서는 대상 플레이어를 지정해야 합니다."));
                return;
            }
            target = player;
        }

        PlayerProfile profile = plugin.getProfileManager().getProfile(target);
        if (profile == null) {
            sender.sendMessage(Text.colorize("&c플레이어 데이터가 아직 로드되지 않았습니다."));
            return;
        }

        double xpToNext;
        try {
            xpToNext = plugin.getProgressionService().getXpToNextLevel(profile.getLevel());
        } catch (IllegalStateException ex) {
            xpToNext = Double.POSITIVE_INFINITY;
        }
        String xpDisplay = Double.isInfinite(xpToNext) ? "MAX" : String.format("%.2f", xpToNext);
        sender.sendMessage(Text.colorize(String.format("&a%s &7- Lv.%d, XP %.2f, 다음 레벨까지 %s", target.getName(), profile.getLevel(), profile.getXp(), xpDisplay)));
        if (sender != target) {
            target.sendMessage(Text.colorize("&a현재 레벨/경험치 정보를 확인했습니다."));
        }
    }

    private void handleWorld(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("reset") || !args[2].equalsIgnoreCase("now")) {
            sender.sendMessage(Text.colorize("&c사용법: /fg world reset now"));
            return;
        }
        sender.sendMessage(Text.colorize("&e채집 월드 리셋을 시작합니다..."));
        plugin.getWorldResetService().resetNow(sender);
    }

    private void flushProfileOrWarn(CommandSender sender, UUID uuid) {
        try {
            plugin.getProfileManager().flush(uuid).join();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to persist FarmGather profile", ex);
            sender.sendMessage(Text.colorize("&c데이터 저장 중 오류가 발생했습니다. 콘솔 로그를 확인하세요."));
        }
    }
}
