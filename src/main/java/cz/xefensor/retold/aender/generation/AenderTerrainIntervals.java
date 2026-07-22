package cz.xefensor.retold.aender.generation;

/**
 * In-place interval composition used by generator V2.
 */
final class AenderTerrainIntervals {
    private AenderTerrainIntervals() {
    }

    static int merge(int[] mins, int[] maxes, long[] seeds, int count) {
        sort(mins, maxes, seeds, count);

        int inputIndex = 0;
        int outputCount = 0;

        while (inputIndex < count) {
            int mergedMin = mins[inputIndex];
            int mergedMax = maxes[inputIndex];
            long surfaceSeed = seeds[inputIndex];
            inputIndex++;

            while (inputIndex < count && mins[inputIndex] <= mergedMax + 1) {
                int nextMax = maxes[inputIndex];
                long nextSeed = seeds[inputIndex];

                if (nextMax > mergedMax
                        || nextMax == mergedMax
                        && Long.compareUnsigned(nextSeed, surfaceSeed) < 0) {
                    mergedMax = nextMax;
                    surfaceSeed = nextSeed;
                }

                inputIndex++;
            }

            mins[outputCount] = mergedMin;
            maxes[outputCount] = mergedMax;
            seeds[outputCount] = surfaceSeed;
            outputCount++;
        }

        return outputCount;
    }

    private static void sort(int[] mins, int[] maxes, long[] seeds, int count) {
        for (int index = 1; index < count; index++) {
            int min = mins[index];
            int max = maxes[index];
            long seed = seeds[index];
            int insertionIndex = index - 1;

            while (insertionIndex >= 0
                    && compare(
                    mins[insertionIndex],
                    maxes[insertionIndex],
                    seeds[insertionIndex],
                    min,
                    max,
                    seed
            ) > 0) {
                mins[insertionIndex + 1] = mins[insertionIndex];
                maxes[insertionIndex + 1] = maxes[insertionIndex];
                seeds[insertionIndex + 1] = seeds[insertionIndex];
                insertionIndex--;
            }

            mins[insertionIndex + 1] = min;
            maxes[insertionIndex + 1] = max;
            seeds[insertionIndex + 1] = seed;
        }
    }

    private static int compare(
            int leftMin,
            int leftMax,
            long leftSeed,
            int rightMin,
            int rightMax,
            long rightSeed
    ) {
        int minComparison = Integer.compare(leftMin, rightMin);
        if (minComparison != 0) {
            return minComparison;
        }

        int maxComparison = Integer.compare(leftMax, rightMax);
        return maxComparison != 0 ? maxComparison : Long.compareUnsigned(leftSeed, rightSeed);
    }
}
