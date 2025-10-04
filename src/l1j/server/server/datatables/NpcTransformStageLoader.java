package l1j.server.server.datatables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.server.templates.L1NpcTransformationStage;

public class NpcTransformStageLoader {

        private static final Logger _log = LoggerFactory.getLogger(NpcTransformStageLoader.class);

        private static final String DATA_PATH = "data/npc_transform_stages.csv";

        private final Map<Integer, List<L1NpcTransformationStage>> _stageMap;

        private NpcTransformStageLoader() {
                _stageMap = load();
        }

        private static class LoaderHolder {
                private static final NpcTransformStageLoader INSTANCE = new NpcTransformStageLoader();
        }

        public static NpcTransformStageLoader getInstance() {
                return LoaderHolder.INSTANCE;
        }

        public List<L1NpcTransformationStage> getStages(int npcId) {
                List<L1NpcTransformationStage> stages = _stageMap.get(npcId);
                if (stages == null) {
                        return Collections.emptyList();
                }
                return stages;
        }

        private Map<Integer, List<L1NpcTransformationStage>> load() {
                Map<Integer, List<L1NpcTransformationStage>> stageMap = new HashMap<>();
                File file = new File(DATA_PATH);
                if (!file.exists()) {
                        _log.info("NPC transform stage definition file not found: {}", file.getAbsolutePath());
                        return stageMap;
                }

                try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                if (line.isEmpty() || line.startsWith("#")) {
                                        continue;
                                }

                                String[] tokens = line.split(",");
                                if (tokens.length < 3) {
                                        _log.warn("Skipping malformed transform stage entry: {}", line);
                                        continue;
                                }

                                try {
                                        int npcId = Integer.parseInt(tokens[0].trim());
                                        int hpPercent = Integer.parseInt(tokens[1].trim());
                                        int transformId = Integer.parseInt(tokens[2].trim());

                                        List<L1NpcTransformationStage> stages = stageMap.get(npcId);
                                        if (stages == null) {
                                                stages = new ArrayList<>();
                                                stageMap.put(npcId, stages);
                                        }
                                        stages.add(new L1NpcTransformationStage(hpPercent, transformId));
                                } catch (NumberFormatException e) {
                                        _log.warn("Skipping transform stage entry due to number format: {}", line, e);
                                }
                        }
                } catch (Exception e) {
                        _log.error("Failed to load NPC transformation stages", e);
                }

                for (List<L1NpcTransformationStage> stages : stageMap.values()) {
                        stages.sort((a, b) -> Integer.compare(b.getHpPercent(), a.getHpPercent()));
                }

                return stageMap;
        }
}
