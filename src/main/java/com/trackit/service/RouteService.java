package com.trackit.service;

import com.trackit.dto.Dtos.*;
import com.trackit.model.Route;
import com.trackit.model.Stop;
import com.trackit.repo.RouteRepository;
import com.trackit.repo.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routes;
    private final StopRepository stops;
    private final Map<Long, List<Stop>> stopCache = new ConcurrentHashMap<>();

    /** Cached because it is read on every GPS update. */
    public List<Stop> stopsOf(Long routeId) {
        return stopCache.computeIfAbsent(routeId, stops::findByRouteIdOrderBySeq);
    }

    public List<RouteView> all() {
        return routes.findAll().stream().map(r -> new RouteView(r.getId(), r.getName(),
                stopsOf(r.getId()).stream().map(s -> new StopView(s.getId(), s.getSeq(), s.getName(), s.getLat(), s.getLng())).toList()
        )).toList();
    }

    @Transactional
    public RouteView create(CreateRoute req) {
        Route r = routes.save(new Route(req.name()));
        List<StopInput> in = req.stops();
        stops.saveAll(IntStream.range(0, in.size())
                .mapToObj(i -> new Stop(r.getId(), i, in.get(i).name(), in.get(i).lat(), in.get(i).lng())).toList());
        stopCache.remove(r.getId());
        return all().stream().filter(v -> v.id().equals(r.getId())).findFirst().orElseThrow();
    }
}
