package cz.xefensor.retold.aender.generation;

import java.util.ArrayList;
import java.util.List;

/** Pure deterministic planning for conservative tunnels inside substantial parent islands. */
final class AenderCavePlanner {
    private static final double NODE_SPACING = 2.5D;
    private static final double ENTRANCE_TRANSITION = 0.20D;
    private static final int LARGE_ISLAND_PATH_ATTEMPTS = 8;

    private AenderCavePlanner() {
    }

    static List<Tunnel> tunnelsForIsland(AenderIslandSampler.Island island) {
        if (!supportsCaves(island)) {
            return List.of();
        }

        long caveSeed = mix64(island.seed() ^ 0xCA6E7A11E15L);
        boolean largeIsland = isLargeIsland(island);
        double chance = island.biome() == AenderBiomeKind.DESERT ? 0.72D : 0.80D;

        if (!largeIsland && unit(caveSeed ^ 0x01L) >= chance) {
            return List.of();
        }

        int count = 1;

        if (largeIsland && unit(caveSeed ^ 0x02L) < 0.68D) {
            count++;
        }
        if (largeIsland
                && Math.max(island.radiusX(), island.radiusZ()) >= 128.0D
                && unit(caveSeed ^ 0x03L) < 0.38D) {
            count++;
        }

        List<Tunnel> mainTunnels = new ArrayList<>(count);
        int attempts = largeIsland
                ? Math.max(LARGE_ISLAND_PATH_ATTEMPTS, count * 3)
                : count * 3;

        for (int index = 0; index < attempts && mainTunnels.size() < count; index++) {
            Tunnel tunnel = planTunnel(island, caveSeed, index);

            if (tunnel.nodes().size() >= 6) {
                mainTunnels.add(tunnel);
            }
        }

        List<Tunnel> network = new ArrayList<>(mainTunnels.size() * 2);

        for (Tunnel mainTunnel : mainTunnels) {
            network.add(mainTunnel);
            addBranches(island, mainTunnel, network);
        }

        return List.copyOf(network);
    }

    private static boolean isLargeIsland(AenderIslandSampler.Island island) {
        return Math.max(island.radiusX(), island.radiusZ()) >= 92.0D
                && island.height() >= 30.0D;
    }

    private static boolean supportsCaves(AenderIslandSampler.Island island) {
        boolean stableBody = switch (island.archetype()) {
            case ROUND, ELONGATED, PLATEAU, DUNES -> true;
            default -> false;
        };
        return stableBody
                && Math.min(island.radiusX(), island.radiusZ()) >= 48.0D
                && Math.max(island.radiusX(), island.radiusZ()) >= 68.0D
                && island.height() >= 24.0D;
    }

    private static Tunnel planTunnel(
            AenderIslandSampler.Island island,
            long caveSeed,
            int index
    ) {
        long tunnelSeed = mix64(caveSeed ^ (long) index * 0x9E3779B97F4A7C15L);
        double angle = unit(tunnelSeed ^ 0x11L) * Math.PI * 2.0D;
        double directionX = Math.cos(angle);
        double directionZ = Math.sin(angle);
        double perpendicularX = -directionZ;
        double perpendicularZ = directionX;
        double radiusAlong = radiusAlong(island, directionX, directionZ);
        double perpendicularRadius = radiusAlong(island, perpendicularX, perpendicularZ);
        double perpendicularOffset = (unit(tunnelSeed ^ 0x12L) - 0.5D)
                * perpendicularRadius
                * 0.28D;
        EntranceKind startEntrance = entranceKind(tunnelSeed);
        EntranceKind endEntrance = unit(tunnelSeed ^ 0x32L) < 0.55D
                ? outsideEntranceKind(tunnelSeed ^ 0x33L)
                : EntranceKind.NONE;
        double startReach = startEntrance == EntranceKind.SIDE
                ? findEdgeReach(
                        island,
                        directionX,
                        directionZ,
                        perpendicularX,
                        perpendicularZ,
                        perpendicularOffset,
                        radiusAlong,
                        -1.0D
                )
                : radiusAlong * (0.42D + unit(tunnelSeed ^ 0x13L) * 0.18D);
        double endReach = endEntrance == EntranceKind.SIDE
                ? findEdgeReach(
                        island,
                        directionX,
                        directionZ,
                        perpendicularX,
                        perpendicularZ,
                        perpendicularOffset,
                        radiusAlong,
                        1.0D
                )
                : radiusAlong * (0.44D + unit(tunnelSeed ^ 0x14L) * 0.20D);
        double length = startReach + endReach;
        int nodeCount = Math.max(6, (int) Math.ceil(length / NODE_SPACING) + 1);
        double endVerticalTarget = endVerticalTarget(
                island,
                tunnelSeed,
                endEntrance,
                directionX,
                directionZ,
                perpendicularX,
                perpendicularZ,
                perpendicularOffset,
                endReach,
                nodeCount
        );

        if (Double.isInfinite(endVerticalTarget)) {
            return new Tunnel(
                    tunnelSeed,
                    TunnelKind.MAIN,
                    startEntrance,
                    endEntrance,
                    List.of()
            );
        }

        double firstTurn = (unit(tunnelSeed ^ 0x15L) - 0.5D) * perpendicularRadius * 1.15D;
        double secondTurn = (unit(tunnelSeed ^ 0x16L) - 0.5D) * perpendicularRadius * 1.15D;
        double localMeander = (unit(tunnelSeed ^ 0x1AL) - 0.5D)
                * perpendicularRadius
                * 0.20D;
        double verticalAmplitude = island.biome() == AenderBiomeKind.DESERT
                ? 1.0D + unit(tunnelSeed ^ 0x17L) * 1.8D
                : 2.0D + unit(tunnelSeed ^ 0x17L) * 3.5D;
        int chamberIndex = unit(tunnelSeed ^ 0x18L) < 0.34D
                ? Math.max(2, (int) (nodeCount * (0.34D + unit(tunnelSeed ^ 0x19L) * 0.32D)))
                : -1;
        List<Node> nodes = new ArrayList<>(nodeCount);
        Double previousY = null;

        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            double progress = nodeIndex / (double) (nodeCount - 1);
            double along = -startReach + length * progress;
            double bend = perpendicularOffset
                    + cubicTurn(progress, firstTurn, secondTurn)
                    + Math.sin(Math.PI * 3.0D * progress) * localMeander;
            double x = island.centerX() + directionX * along + perpendicularX * bend;
            double z = island.centerZ() + directionZ * along + perpendicularZ * bend;
            int blockX = (int) Math.round(x);
            int blockZ = (int) Math.round(z);
            AenderIslandSampler.Island.Column column = island.columnAt(blockX, blockZ);

            if (column.empty()) {
                return new Tunnel(
                        tunnelSeed,
                        TunnelKind.MAIN,
                        startEntrance,
                        endEntrance,
                        List.of()
                );
            }

            long nodeSeed = mix64(tunnelSeed ^ (long) nodeIndex * 0xC2B2AE3D27D4EB4FL);
            double horizontalRadius = 2.25D + unit(nodeSeed ^ 0x21L) * 1.65D;
            double verticalRadius = 1.75D + unit(nodeSeed ^ 0x22L) * 1.35D;

            if (Math.abs(nodeIndex - chamberIndex) <= 1) {
                double chamberStrength = nodeIndex == chamberIndex ? 1.0D : 0.55D;
                horizontalRadius += chamberStrength * (1.8D + unit(tunnelSeed ^ 0x23L) * 1.4D);
                verticalRadius += chamberStrength * (0.9D + unit(tunnelSeed ^ 0x24L) * 0.9D);
            }

            EntranceKind nodeEntrance = EntranceKind.NONE;

            if (progress <= ENTRANCE_TRANSITION && startEntrance != EntranceKind.NONE) {
                nodeEntrance = startEntrance;
                horizontalRadius = Math.min(horizontalRadius, 2.75D);
                verticalRadius = Math.min(verticalRadius, 2.45D);
            } else if (progress >= 1.0D - ENTRANCE_TRANSITION
                    && endEntrance != EntranceKind.NONE) {
                nodeEntrance = endEntrance;
                horizontalRadius = Math.min(horizontalRadius, 2.75D);
                verticalRadius = Math.min(verticalRadius, 2.45D);
            }

            int thickness = column.maxY() - column.minY() + 1;
            double normalY = column.minY()
                    + thickness * (island.biome() == AenderBiomeKind.DESERT ? 0.43D : 0.40D)
                    + Math.sin(progress * Math.PI * 2.0D + unit(tunnelSeed ^ 0x25L) * Math.PI)
                    * verticalAmplitude;
            double minY = column.minY() + verticalRadius + 3.0D;
            double maxY = column.maxY() - verticalRadius - 5.0D;

            if (maxY < minY && nodeEntrance == EntranceKind.NONE) {
                return new Tunnel(
                        tunnelSeed,
                        TunnelKind.MAIN,
                        startEntrance,
                        endEntrance,
                        List.of()
                );
            }

            double y = maxY >= minY
                    ? clamp(normalY, minY, maxY)
                    : (column.minY() + column.maxY()) * 0.5D;

            double endpointProgress = progress <= ENTRANCE_TRANSITION
                    ? progress
                    : 1.0D - progress;

            if (nodeEntrance == EntranceKind.UNDERSIDE) {
                double blend = smoothStep(0.0D, ENTRANCE_TRANSITION, endpointProgress);
                double entranceY = column.minY() + verticalRadius * 0.30D;
                y = lerp(entranceY, y, blend);
            } else if (nodeEntrance == EntranceKind.SURFACE) {
                double blend = smoothStep(0.0D, ENTRANCE_TRANSITION, endpointProgress);
                double entranceY = column.maxY() - verticalRadius * 0.30D;
                y = lerp(entranceY, y, blend);
            }

            double allowedMinY = switch (nodeEntrance) {
                case NONE -> minY;
                case SIDE -> column.minY() + 2.0D;
                case UNDERSIDE -> column.minY() - verticalRadius;
                case SURFACE -> column.minY() + 4.0D;
            };
            double allowedMaxY = switch (nodeEntrance) {
                case NONE -> maxY;
                case SIDE -> column.maxY() - 3.0D;
                case UNDERSIDE -> column.maxY() - 4.0D;
                case SURFACE -> column.maxY() + verticalRadius;
            };

            if (!Double.isNaN(endVerticalTarget)
                    && progress >= 1.0D - ENTRANCE_TRANSITION) {
                int remainingNodes = nodeCount - 1 - nodeIndex;
                double reachableDistance = remainingNodes * 2.0D;
                allowedMinY = Math.max(allowedMinY, endVerticalTarget - reachableDistance);
                allowedMaxY = Math.min(allowedMaxY, endVerticalTarget + reachableDistance);
                y = clamp(y, allowedMinY, allowedMaxY);
            }

            if (allowedMaxY < allowedMinY) {
                return new Tunnel(
                        tunnelSeed,
                        TunnelKind.MAIN,
                        startEntrance,
                        endEntrance,
                        List.of()
                );
            }

            if (previousY != null) {
                double smoothedMinY = Math.max(allowedMinY, previousY - 2.0D);
                double smoothedMaxY = Math.min(allowedMaxY, previousY + 2.0D);

                if (smoothedMaxY < smoothedMinY) {
                    return new Tunnel(
                            tunnelSeed,
                            TunnelKind.MAIN,
                            startEntrance,
                            endEntrance,
                            List.of()
                    );
                }

                y = clamp(y, smoothedMinY, smoothedMaxY);
            }

            nodes.add(new Node(x, y, z, horizontalRadius, verticalRadius, nodeEntrance, nodeSeed));
            previousY = y;
        }

        return new Tunnel(
                tunnelSeed,
                TunnelKind.MAIN,
                startEntrance,
                endEntrance,
                List.copyOf(nodes)
        );
    }

    private static void addBranches(
            AenderIslandSampler.Island island,
            Tunnel mainTunnel,
            List<Tunnel> network
    ) {
        int branchCount = unit(mainTunnel.seed() ^ 0xB2A6C101L) < 0.72D ? 1 : 0;

        if (Math.max(island.radiusX(), island.radiusZ()) >= 128.0D
                && unit(mainTunnel.seed() ^ 0xB2A6C102L) < 0.34D) {
            branchCount++;
        }

        for (int branchIndex = 0; branchIndex < branchCount; branchIndex++) {
            for (int attempt = 0; attempt < 3; attempt++) {
                Tunnel branch = planBranch(island, mainTunnel, branchIndex, attempt);

                if (branch.nodes().size() >= 7) {
                    network.add(branch);
                    break;
                }
            }
        }
    }

    private static Tunnel planBranch(
            AenderIslandSampler.Island island,
            Tunnel mainTunnel,
            int branchIndex,
            int attempt
    ) {
        long branchSeed = mix64(
                mainTunnel.seed()
                        ^ (long) branchIndex * 0xD1342543DE82EF95L
                        ^ (long) attempt * 0xC6BC279692B5CC83L
                        ^ 0xB2A6C11L
        );
        List<Node> parentNodes = mainTunnel.nodes();
        double junctionProgress = 0.28D + unit(branchSeed ^ 0x41L) * 0.44D;
        int junctionIndex = Math.max(
                2,
                Math.min(
                        parentNodes.size() - 3,
                        (int) Math.round(junctionProgress * (parentNodes.size() - 1))
                )
        );
        Node junction = parentNodes.get(junctionIndex);
        Node before = parentNodes.get(junctionIndex - 2);
        Node after = parentNodes.get(junctionIndex + 2);
        double tangentX = after.x() - before.x();
        double tangentZ = after.z() - before.z();
        double tangentLength = Math.sqrt(tangentX * tangentX + tangentZ * tangentZ);

        if (tangentLength < 0.001D) {
            return emptyBranch(branchSeed);
        }

        tangentX /= tangentLength;
        tangentZ /= tangentLength;
        double side = unit(branchSeed ^ 0x42L) < 0.5D ? -1.0D : 1.0D;
        double departureAngle = side * (0.72D + unit(branchSeed ^ 0x43L) * 0.72D);
        double directionX = tangentX * Math.cos(departureAngle)
                - tangentZ * Math.sin(departureAngle);
        double directionZ = tangentX * Math.sin(departureAngle)
                + tangentZ * Math.cos(departureAngle);
        double perpendicularX = -directionZ;
        double perpendicularZ = directionX;
        double length = 18.0D + unit(branchSeed ^ 0x44L) * 24.0D;
        int nodeCount = Math.max(7, (int) Math.ceil(length / NODE_SPACING) + 1);
        double turn = (unit(branchSeed ^ 0x45L) - 0.5D) * length * 0.48D;
        boolean endChamber = unit(branchSeed ^ 0x46L) < 0.48D;
        List<Node> nodes = new ArrayList<>(nodeCount);
        double previousY = junction.y();

        for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
            double progress = nodeIndex / (double) (nodeCount - 1);
            double distance = length * progress;
            double bend = Math.sin(Math.PI * progress) * turn;
            double x = junction.x() + directionX * distance + perpendicularX * bend;
            double z = junction.z() + directionZ * distance + perpendicularZ * bend;
            AenderIslandSampler.Island.Column column = island.columnAt(
                    (int) Math.round(x),
                    (int) Math.round(z)
            );

            if (column.empty()) {
                return emptyBranch(branchSeed);
            }

            long nodeSeed = mix64(branchSeed ^ (long) nodeIndex * 0xC2B2AE3D27D4EB4FL);
            double horizontalRadius = 2.1D + unit(nodeSeed ^ 0x47L) * 1.3D;
            double verticalRadius = 1.7D + unit(nodeSeed ^ 0x48L) * 1.1D;

            if (endChamber && nodeIndex >= nodeCount - 2) {
                double chamberStrength = nodeIndex == nodeCount - 1 ? 1.0D : 0.55D;
                horizontalRadius += chamberStrength * (1.4D + unit(branchSeed ^ 0x49L) * 1.2D);
                verticalRadius += chamberStrength * (0.7D + unit(branchSeed ^ 0x4AL) * 0.8D);
            }

            double minY = column.minY() + verticalRadius + 3.0D;
            double maxY = column.maxY() - verticalRadius - 5.0D;

            if (maxY < minY) {
                return emptyBranch(branchSeed);
            }

            double targetY = nodeIndex == 0
                    ? junction.y()
                    : column.minY()
                            + (column.maxY() - column.minY() + 1) * 0.42D
                            + Math.sin(progress * Math.PI * 1.5D) * 2.0D;
            double y = clamp(
                    targetY,
                    Math.max(minY, previousY - 1.6D),
                    Math.min(maxY, previousY + 1.6D)
            );

            if (y < minY || y > maxY) {
                return emptyBranch(branchSeed);
            }

            nodes.add(new Node(
                    x,
                    y,
                    z,
                    horizontalRadius,
                    verticalRadius,
                    EntranceKind.NONE,
                    nodeSeed
            ));
            previousY = y;
        }

        return new Tunnel(
                branchSeed,
                TunnelKind.BRANCH,
                EntranceKind.NONE,
                EntranceKind.NONE,
                List.copyOf(nodes)
        );
    }

    private static Tunnel emptyBranch(long seed) {
        return new Tunnel(
                seed,
                TunnelKind.BRANCH,
                EntranceKind.NONE,
                EntranceKind.NONE,
                List.of()
        );
    }

    private static double endVerticalTarget(
            AenderIslandSampler.Island island,
            long tunnelSeed,
            EntranceKind entrance,
            double directionX,
            double directionZ,
            double perpendicularX,
            double perpendicularZ,
            double perpendicularOffset,
            double endReach,
            int nodeCount
    ) {
        if (entrance != EntranceKind.SURFACE && entrance != EntranceKind.UNDERSIDE) {
            return Double.NaN;
        }

        int x = (int) Math.round(
                island.centerX() + directionX * endReach + perpendicularX * perpendicularOffset
        );
        int z = (int) Math.round(
                island.centerZ() + directionZ * endReach + perpendicularZ * perpendicularOffset
        );
        AenderIslandSampler.Island.Column column = island.columnAt(x, z);

        if (column.empty()) {
            return Double.POSITIVE_INFINITY;
        }

        long nodeSeed = mix64(tunnelSeed ^ (long) (nodeCount - 1) * 0xC2B2AE3D27D4EB4FL);
        double verticalRadius = Math.min(
                1.75D + unit(nodeSeed ^ 0x22L) * 1.35D,
                2.45D
        );
        return entrance == EntranceKind.SURFACE
                ? column.maxY() - verticalRadius * 0.30D
                : column.minY() + verticalRadius * 0.30D;
    }

    private static EntranceKind entranceKind(long seed) {
        double roll = unit(seed ^ 0x31L);

        if (roll < 0.29D) {
            return EntranceKind.SIDE;
        }
        if (roll < 0.53D) {
            return EntranceKind.UNDERSIDE;
        }
        if (roll < 0.72D) {
            return EntranceKind.SURFACE;
        }
        return EntranceKind.NONE;
    }

    private static EntranceKind outsideEntranceKind(long seed) {
        double roll = unit(seed ^ 0x34L);

        if (roll < 0.40D) {
            return EntranceKind.SIDE;
        }
        if (roll < 0.74D) {
            return EntranceKind.UNDERSIDE;
        }
        return EntranceKind.SURFACE;
    }

    private static double findEdgeReach(
            AenderIslandSampler.Island island,
            double directionX,
            double directionZ,
            double perpendicularX,
            double perpendicularZ,
            double perpendicularOffset,
            double radiusAlong,
            double directionSign
    ) {
        double lastSolid = radiusAlong * 0.58D;
        int emptySteps = 0;

        for (double distance = radiusAlong * 0.40D;
                distance <= radiusAlong * 1.55D;
                distance += 2.0D) {
            int x = (int) Math.round(
                    island.centerX()
                            + directionSign * directionX * distance
                            + perpendicularX * perpendicularOffset
            );
            int z = (int) Math.round(
                    island.centerZ()
                            + directionSign * directionZ * distance
                            + perpendicularZ * perpendicularOffset
            );
            AenderIslandSampler.Island.Column column = island.columnAt(x, z);

            if (!column.empty() && column.maxY() - column.minY() >= 8) {
                lastSolid = distance;
                emptySteps = 0;
            } else if (++emptySteps >= 3) {
                break;
            }
        }

        return lastSolid;
    }

    private static double radiusAlong(
            AenderIslandSampler.Island island,
            double directionX,
            double directionZ
    ) {
        double scaledX = directionX / island.radiusX();
        double scaledZ = directionZ / island.radiusZ();
        return 1.0D / Math.sqrt(scaledX * scaledX + scaledZ * scaledZ);
    }

    private static double smoothStep(double edge0, double edge1, double value) {
        double progress = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return progress * progress * (3.0D - 2.0D * progress);
    }

    private static double cubicTurn(
            double progress,
            double firstControl,
            double secondControl
    ) {
        double inverse = 1.0D - progress;
        return 3.0D * inverse * inverse * progress * firstControl
                + 3.0D * inverse * progress * progress * secondControl;
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double unit(long seed) {
        return (mix64(seed) >>> 11) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    enum EntranceKind {
        NONE,
        SIDE,
        UNDERSIDE,
        SURFACE
    }

    enum TunnelKind {
        MAIN,
        BRANCH
    }

    record Tunnel(
            long seed,
            TunnelKind kind,
            EntranceKind startEntrance,
            EntranceKind endEntrance,
            List<Node> nodes
    ) {
    }

    record Node(
            double x,
            double y,
            double z,
            double horizontalRadius,
            double verticalRadius,
            EntranceKind entrance,
            long seed
    ) {
    }
}
