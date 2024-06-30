package kinoko.util.tool;

import kinoko.provider.*;
import kinoko.provider.mob.MobTemplate;
import kinoko.provider.reward.Reward;
import kinoko.provider.wz.*;
import kinoko.provider.wz.property.WzListProperty;
import kinoko.server.ServerConstants;
import kinoko.world.item.ItemConstants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

final class MonsterBookExtractor extends RewardExtractor {
    private static final Map<Integer, List<Integer>> monsterBookRewards = new HashMap<>(); // mob id -> item ids

    public static void main(String[] args) throws IOException {
        ItemProvider.initialize();
        MobProvider.initialize();
        QuestProvider.initialize();
        StringProvider.initialize();

        // Load monster book rewards
        try (final WzReader reader = WzReader.build(StringProvider.STRING_WZ, new WzReaderConfig(WzConstants.WZ_GMS_IV, ServerConstants.GAME_VERSION))) {
            final WzPackage wzPackage = reader.readPackage();
            loadMonsterBookRewards(wzPackage);
        } catch (IOException | ProviderError e) {
            throw new IllegalArgumentException("Exception caught while loading String.wz", e);
        }

        // Load BMS rewards
        final WzImage rewardImage = readImage(RewardExtractor.REWARD_IMG);
        final Map<Integer, List<Reward>> mobRewards = loadRewards(rewardImage, "m", false);

        // Create YAML
        for (var entry : monsterBookRewards.entrySet()) {
            final int mobId = entry.getKey();

            final Optional<MobTemplate> mobTemplateResult = MobProvider.getMobTemplate(mobId);
            if (mobTemplateResult.isEmpty()) {
                throw new IllegalStateException("Could not resolve mob template for mob ID : " + mobId);
            }
            final MobTemplate mobTemplate = mobTemplateResult.get();

            final List<Reward> bmsRewards = mobRewards.getOrDefault(mobId, List.of());
            final Path filePath = Path.of(RewardProvider.REWARD_DATA.toString(), String.format("%d.yaml", mobId));
            try (BufferedWriter bw = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                bw.write(String.format("# %s (%d)\n\n", StringProvider.getMobName(mobId), mobId));
                bw.write("rewards:\n");
                for (int itemId : entry.getValue().stream()
                        .sorted(Comparator.comparingInt(Integer::intValue)).toList()) {
                    int min = 1;
                    int max = 1;
                    double prob = 0;
                    if (ItemConstants.isEquip(itemId)) {
                        if (mobTemplate.isBoss()) {
                            prob = 0.0002;
                        } else {
                            prob = 0.0001;
                        }
                    } else if (ItemConstants.isConsume(itemId)) {
                        if (itemId / 10000 == 204) {
                            if (itemId == 2049100) {
                                // chaos scroll
                                if (mobTemplate.isBoss()) {
                                    prob = 0.000100;
                                } else {
                                    prob = 0.000001;
                                }
                            } else {
                                // scroll
                                if (mobTemplate.isBoss()) {
                                    prob = 0.010000;
                                } else {
                                    prob = 0.000100;
                                }
                            }
                        } else if (itemId / 10000 == 200 || itemId / 10000 == 201 || itemId / 10000 == 202 || itemId / 10000 == 205) {
                            // potion
                            if (mobTemplate.isBoss()) {
                                prob = 0.100000;
                            } else {
                                if (itemId == 2000004 || itemId == 2000005) {
                                    prob = 0.001000; // elixir / power elixir
                                } else {
                                    prob = 0.010000;
                                }
                            }
                        } else if (itemId / 10000 == 206) {
                            min = 10;
                            max = 20;
                            prob = 0.008000; // arrows
                        } else if (itemId / 10000 == 238) {
                            prob = 0.02; // monster book card
                        } else if (ItemConstants.isRechargeableItem(itemId)) {
                            prob = 0.0004;
                        } else if (itemId / 10000 == 228) {
                            prob = 0.200000; // skill book
                        } else if (itemId / 10000 == 229) {
                            // mastery book
                            if (mobTemplate.isBoss()) {
                                prob = 0.100000;
                            } else {
                                prob = 0.000100;
                            }
                        }
                    } else if (itemId / 1000 == 4000) {
                        if (itemId == 4000021) {
                            prob = 0.040000; // Leather
                        } else {
                            prob = 0.400000; // mob ETC
                        }
                    } else if (itemId / 10000 == 401 || itemId / 10000 == 402) {
                        prob = 0.002000; // ore
                    } else if (itemId / 1000 == 4004) {
                        prob = 0.001000; // crystal ore
                    } else if (itemId / 10000 == 403) {
                        prob = 0.050000; // monster card / omok piece / quest item
                    } else if (itemId / 10000 == 413 || itemId / 1000 == 4007) {
                        prob = 0.000300; // production stims || magic powder
                    } else if (itemId / 1000 == 4006) {
                        prob = 0.000700; // the magic rock / summoning rock
                    } else if (itemId / 1000 == 4003) {
                        prob = 0.040000; // stiff/ soft feather
                    }
                    bw.write(String.format("  - [ %d, %d, %d, %f ] # %s\n", itemId, min, max, prob, StringProvider.getItemName(itemId)));
                }
                for (Reward reward : bmsRewards) {
                    if (reward.isQuest()) {
                        bw.write(String.format("  - [ %d, %d, %d, %f, %d ] # %s\n", reward.getItemId(), reward.getMin(), reward.getMax(), reward.getProb(), reward.getQuestId(), StringProvider.getItemName(reward.getItemId())));
                    }
                }
            }
        }
    }

    private static void loadMonsterBookRewards(WzPackage source) throws ProviderError {
        if (!(source.getDirectory().getImages().get("MonsterBook.img") instanceof WzImage monsterBookImage)) {
            throw new ProviderError("Could not resolve String.wz/MonsterBook.img");
        }
        for (var entry : monsterBookImage.getProperty().getItems().entrySet()) {
            final int mobId = WzProvider.getInteger(entry.getKey());
            if (!(entry.getValue() instanceof WzListProperty entryProp) ||
                    !(entryProp.get("reward") instanceof WzListProperty rewardProp)) {
                throw new ProviderError("Could not resolve monster book info");
            }
            final List<Integer> rewards = new ArrayList<>();
            for (var rewardEntry : rewardProp.getItems().entrySet()) {
                final int itemId = WzProvider.getInteger(rewardEntry.getValue());
                rewards.add(itemId);
            }
            monsterBookRewards.put(mobId, Collections.unmodifiableList(rewards));
        }
    }
}