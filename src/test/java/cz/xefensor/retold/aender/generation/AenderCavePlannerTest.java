package cz.xefensor.retold.aender.generation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AenderCavePlannerTest {
    @Test
    void tunnelsAreDeterministicContinuousAndRemainInSubstantialStone() {
        int testedTunnels = 0;

        for (long seed = 0; seed < 96; seed++) {
            AenderIslandSampler.Island island = island(seed, AenderIslandArchetype.ROUND);
            List<AenderCavePlanner.Tunnel> first = AenderCavePlanner.tunnelsForIsland(island);
            List<AenderCavePlanner.Tunnel> second = AenderCavePlanner.tunnelsForIsland(island);
            assertEquals(first, second, "The same island must plan identical cave tunnels");

            for (AenderCavePlanner.Tunnel tunnel : first) {
                assertTrue(tunnel.nodes().size() >= 6);
                AenderCavePlanner.Node previous = null;

                for (AenderCavePlanner.Node node : tunnel.nodes()) {
                    AenderIslandSampler.Island.Column column = island.columnAt(
                            (int) Math.round(node.x()),
                            (int) Math.round(node.z())
                    );
                    assertFalse(column.empty(), "Every cave node must remain inside its parent island");

                    if (node.entrance() == AenderCavePlanner.EntranceKind.NONE) {
                        assertTrue(node.y() - node.verticalRadius() >= column.minY() + 2.0D);
                        assertTrue(node.y() + node.verticalRadius() <= column.maxY() - 4.0D);
                    }

                    if (previous != null) {
                        double distance = Math.sqrt(
                                square(node.x() - previous.x())
                                        + square(node.y() - previous.y())
                                        + square(node.z() - previous.z())
                        );
                        assertTrue(distance <= 7.0D, "Adjacent nodes must overlap into one tunnel");
                    }

                    previous = node;
                }

                testedTunnels++;
            }
        }

        assertTrue(testedTunnels > 40, "The continuity check needs a useful tunnel sample");
    }

    @Test
    void largeEligibleIslandsAlwaysReceiveAtLeastOneTunnel() {
        for (AenderIslandArchetype archetype : List.of(
                AenderIslandArchetype.ROUND,
                AenderIslandArchetype.ELONGATED,
                AenderIslandArchetype.PLATEAU,
                AenderIslandArchetype.DUNES
        )) {
            for (long seed = 0; seed < 256; seed++) {
                assertFalse(
                        AenderCavePlanner.tunnelsForIsland(island(seed, archetype)).isEmpty(),
                        "Every large stable island must receive a valid tunnel"
                );
            }
        }
    }

    @Test
    void tunnelsCanConnectAtBothEndsAcrossAllApprovedEntranceKinds() {
        int multipleTunnelIslands = 0;
        int doubleEndedTunnels = 0;
        int sideEntrances = 0;
        int undersideEntrances = 0;
        int surfaceEntrances = 0;

        for (long seed = 0; seed < 384; seed++) {
            List<AenderCavePlanner.Tunnel> tunnels = AenderCavePlanner.tunnelsForIsland(
                    island(seed, AenderIslandArchetype.ROUND)
            );

            long mainTunnelCount = tunnels.stream()
                    .filter(tunnel -> tunnel.kind() == AenderCavePlanner.TunnelKind.MAIN)
                    .count();

            if (mainTunnelCount >= 2) {
                multipleTunnelIslands++;
            }

            for (AenderCavePlanner.Tunnel tunnel : tunnels) {
                if (tunnel.startEntrance() != AenderCavePlanner.EntranceKind.NONE
                        && tunnel.endEntrance() != AenderCavePlanner.EntranceKind.NONE) {
                    doubleEndedTunnels++;
                }

                sideEntrances += countEntranceKind(tunnel, AenderCavePlanner.EntranceKind.SIDE);
                undersideEntrances += countEntranceKind(
                        tunnel,
                        AenderCavePlanner.EntranceKind.UNDERSIDE
                );
                surfaceEntrances += countEntranceKind(
                        tunnel,
                        AenderCavePlanner.EntranceKind.SURFACE
                );

                assertEquals(tunnel.startEntrance(), tunnel.nodes().getFirst().entrance());
                assertEquals(tunnel.endEntrance(), tunnel.nodes().getLast().entrance());
                assertVerticalEntranceReachesShell(
                        island(seed, AenderIslandArchetype.ROUND),
                        tunnel.nodes().getFirst(),
                        tunnel.startEntrance()
                );
                assertVerticalEntranceReachesShell(
                        island(seed, AenderIslandArchetype.ROUND),
                        tunnel.nodes().getLast(),
                        tunnel.endEntrance()
                );
            }
        }

        assertTrue(multipleTunnelIslands > 180);
        assertTrue(doubleEndedTunnels > 180);
        assertTrue(sideEntrances > 250);
        assertTrue(undersideEntrances > 200);
        assertTrue(surfaceEntrances > 100);
    }

    @Test
    void caveNetworksContainTurningMainPassagesAndConnectedBranches() {
        int branchedIslands = 0;
        int branchPaths = 0;
        int visiblyTurningMainPaths = 0;

        for (long seed = 0; seed < 384; seed++) {
            List<AenderCavePlanner.Tunnel> tunnels = AenderCavePlanner.tunnelsForIsland(
                    island(seed, AenderIslandArchetype.ROUND)
            );
            List<AenderCavePlanner.Tunnel> mainTunnels = tunnels.stream()
                    .filter(tunnel -> tunnel.kind() == AenderCavePlanner.TunnelKind.MAIN)
                    .toList();
            List<AenderCavePlanner.Tunnel> branches = tunnels.stream()
                    .filter(tunnel -> tunnel.kind() == AenderCavePlanner.TunnelKind.BRANCH)
                    .toList();

            if (!branches.isEmpty()) {
                branchedIslands++;
            }

            for (AenderCavePlanner.Tunnel mainTunnel : mainTunnels) {
                if (maximumHorizontalLineDeviation(mainTunnel.nodes()) >= 4.0D) {
                    visiblyTurningMainPaths++;
                }
            }

            for (AenderCavePlanner.Tunnel branch : branches) {
                branchPaths++;
                AenderCavePlanner.Node junction = branch.nodes().getFirst();
                double nearestMainNode = mainTunnels.stream()
                        .flatMap(tunnel -> tunnel.nodes().stream())
                        .mapToDouble(node -> distance(junction, node))
                        .min()
                        .orElse(Double.POSITIVE_INFINITY);

                assertTrue(
                        nearestMainNode <= 2.0D,
                        "Every branch must begin inside a main passage"
                );
                assertTrue(branch.nodes().size() >= 7);
                assertEquals(AenderCavePlanner.EntranceKind.NONE, branch.startEntrance());
                assertEquals(AenderCavePlanner.EntranceKind.NONE, branch.endEntrance());
            }
        }

        assertTrue(branchedIslands > 180);
        assertTrue(branchPaths > 240);
        assertTrue(visiblyTurningMainPaths > 250);
    }

    @Test
    void fragileAndAlreadyOpenArchetypesDoNotReceiveCaves() {
        for (AenderIslandArchetype archetype : List.of(
                AenderIslandArchetype.CRESCENT,
                AenderIslandArchetype.SPLIT,
                AenderIslandArchetype.TWIN,
                AenderIslandArchetype.ERODED_MESA,
                AenderIslandArchetype.SHELF
        )) {
            for (long seed = 0; seed < 32; seed++) {
                assertTrue(AenderCavePlanner.tunnelsForIsland(island(seed, archetype)).isEmpty());
            }
        }
    }

    private static AenderIslandSampler.Island island(
            long seed,
            AenderIslandArchetype archetype
    ) {
        AenderBiomeKind biome = archetype.isDesertShape()
                ? AenderBiomeKind.DESERT
                : AenderBiomeKind.PLAINS;
        return new AenderIslandSampler.Island(
                0,
                128,
                0,
                104.0D,
                82.0D,
                38.0D,
                seed,
                biome,
                archetype,
                AenderUndersideProfile.ROOTED
        );
    }

    private static double square(double value) {
        return value * value;
    }

    private static double distance(
            AenderCavePlanner.Node first,
            AenderCavePlanner.Node second
    ) {
        return Math.sqrt(
                square(first.x() - second.x())
                        + square(first.y() - second.y())
                        + square(first.z() - second.z())
        );
    }

    private static double maximumHorizontalLineDeviation(
            List<AenderCavePlanner.Node> nodes
    ) {
        AenderCavePlanner.Node start = nodes.getFirst();
        AenderCavePlanner.Node end = nodes.getLast();
        double lineX = end.x() - start.x();
        double lineZ = end.z() - start.z();
        double lineLength = Math.sqrt(lineX * lineX + lineZ * lineZ);
        double maximum = 0.0D;

        for (AenderCavePlanner.Node node : nodes) {
            double deviation = Math.abs(
                    lineZ * (node.x() - start.x()) - lineX * (node.z() - start.z())
            ) / lineLength;
            maximum = Math.max(maximum, deviation);
        }

        return maximum;
    }

    private static int countEntranceKind(
            AenderCavePlanner.Tunnel tunnel,
            AenderCavePlanner.EntranceKind kind
    ) {
        int count = 0;

        if (tunnel.startEntrance() == kind) {
            count++;
        }
        if (tunnel.endEntrance() == kind) {
            count++;
        }

        return count;
    }

    private static void assertVerticalEntranceReachesShell(
            AenderIslandSampler.Island island,
            AenderCavePlanner.Node node,
            AenderCavePlanner.EntranceKind entrance
    ) {
        AenderIslandSampler.Island.Column column = island.columnAt(
                (int) Math.round(node.x()),
                (int) Math.round(node.z())
        );

        if (entrance == AenderCavePlanner.EntranceKind.SURFACE) {
            assertTrue(
                    node.y() + node.verticalRadius() >= column.maxY() + 0.5D,
                    () -> "Surface endpoint stopped below cap: " + node + " in " + column
            );
        } else if (entrance == AenderCavePlanner.EntranceKind.UNDERSIDE) {
            assertTrue(
                    node.y() - node.verticalRadius() <= column.minY() + 0.5D,
                    () -> "Underside endpoint stopped above shell: " + node + " in " + column
            );
        }
    }
}
