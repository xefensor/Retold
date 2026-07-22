package cz.xefensor.retold.aender.generation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AenderTerrainIntervalsTest {
    @Test
    void mergesOverlappingAndAdjacentIslandsWithoutInputOrderBias() {
        int[] firstMins = {20, 0, 11};
        int[] firstMaxes = {30, 10, 19};
        long[] firstSeeds = {3L, 1L, 2L};
        int[] secondMins = {11, 20, 0};
        int[] secondMaxes = {19, 30, 10};
        long[] secondSeeds = {2L, 3L, 1L};

        int firstCount = AenderTerrainIntervals.merge(firstMins, firstMaxes, firstSeeds, 3);
        int secondCount = AenderTerrainIntervals.merge(secondMins, secondMaxes, secondSeeds, 3);

        assertEquals(1, firstCount);
        assertEquals(firstCount, secondCount);
        assertArrayEquals(new int[]{0}, copy(firstMins, firstCount));
        assertArrayEquals(new int[]{30}, copy(firstMaxes, firstCount));
        assertArrayEquals(copy(firstMins, firstCount), copy(secondMins, secondCount));
        assertArrayEquals(copy(firstMaxes, firstCount), copy(secondMaxes, secondCount));
        assertArrayEquals(copy(firstSeeds, firstCount), copy(secondSeeds, secondCount));
        assertEquals(3L, firstSeeds[0]);
    }

    @Test
    void preservesAirGapsBetweenSeparateIslands() {
        int[] mins = {20, 0};
        int[] maxes = {30, 10};
        long[] seeds = {2L, 1L};

        int count = AenderTerrainIntervals.merge(mins, maxes, seeds, 2);

        assertEquals(2, count);
        assertArrayEquals(new int[]{0, 20}, copy(mins, count));
        assertArrayEquals(new int[]{10, 30}, copy(maxes, count));
    }

    private static int[] copy(int[] values, int count) {
        int[] copy = new int[count];
        System.arraycopy(values, 0, copy, 0, count);
        return copy;
    }

    private static long[] copy(long[] values, int count) {
        long[] copy = new long[count];
        System.arraycopy(values, 0, copy, 0, count);
        return copy;
    }
}
