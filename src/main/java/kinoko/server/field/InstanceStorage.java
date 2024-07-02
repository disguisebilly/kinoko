package kinoko.server.field;

import kinoko.provider.MapProvider;
import kinoko.provider.map.MapInfo;
import kinoko.server.node.ChannelServerNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class InstanceStorage {
    private static final AtomicInteger instanceIdCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, Instance> instanceMap = new ConcurrentHashMap<>();

    public Optional<Instance> createInstance(ChannelServerNode channelServerNode, List<Integer> mapIds, int returnMap, int timeLimit) {
        // Resolve map infos
        final List<MapInfo> mapInfos = new ArrayList<>();
        for (int mapId : mapIds) {
            final Optional<MapInfo> mapInfoResult = MapProvider.getMapInfo(mapId);
            if (mapInfoResult.isEmpty()) {
                return Optional.empty();
            }
            mapInfos.add(mapInfoResult.get());
        }
        // Create instance and field storage
        final Instance instance = new Instance(getNewInstanceId(), returnMap, channelServerNode, Instant.now().plus(timeLimit, ChronoUnit.SECONDS));
        final InstanceFieldStorage fieldStorage = InstanceFieldStorage.from(instance, mapInfos);
        instance.setFieldStorage(fieldStorage);
        instanceMap.put(instance.getInstanceId(), instance);
        return Optional.of(instance);
    }

    public boolean removeInstance(Instance instance) {
        assert instance.getUsers().isEmpty();
        instance.getFieldStorage().clear();
        return instanceMap.remove(instance.getInstanceId(), instance);
    }

    public int getNewInstanceId() {
        return instanceIdCounter.getAndIncrement();
    }
}