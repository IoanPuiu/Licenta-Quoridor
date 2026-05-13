package AI_prototip;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameStateGraphTest {

    @Test
    public void generatePawnMoveGraphBuildsExpectedHardcodedAdjacency() {
        GameState gameState = new GameState(new int[]{
                40, 31, 10, 10, 0,
                38, 75, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        });

        Map<Integer, Set<Integer>> expectedGraph = Map.ofEntries(
                entry(0, Set.of(1, 9)),
                entry(1, Set.of(0, 2, 10)),
                entry(2, Set.of(1, 3, 11)),
                entry(3, Set.of(2, 4, 12)),
                entry(4, Set.of(3, 5, 13)),
                entry(5, Set.of(4, 6, 14)),
                entry(6, Set.of(5, 7, 15)),
                entry(7, Set.of(6, 8, 16)),
                entry(8, Set.of(7, 17)),
                entry(9, Set.of(0, 10, 18)),
                entry(10, Set.of(1, 9, 11, 19)),
                entry(11, Set.of(2, 10, 12, 20)),
                entry(12, Set.of(3, 11, 13, 21)),
                entry(13, Set.of(4, 12, 14, 22)),
                entry(14, Set.of(5, 13, 15, 23)),
                entry(15, Set.of(6, 14, 16, 24)),
                entry(16, Set.of(7, 15, 17, 25)),
                entry(17, Set.of(8, 16, 26)),
                entry(18, Set.of(9, 19, 27)),
                entry(19, Set.of(10, 18, 20, 28)),
                entry(20, Set.of(11, 19, 21, 29)),
                entry(21, Set.of(12, 20, 22)),
                entry(22, Set.of(13, 21, 23)),
                entry(23, Set.of(14, 22, 24, 32)),
                entry(24, Set.of(15, 23, 25, 33)),
                entry(25, Set.of(16, 24, 26, 34)),
                entry(26, Set.of(17, 25, 35)),
                entry(27, Set.of(18, 28, 36)),
                entry(28, Set.of(19, 27, 29, 37)),
                entry(29, Set.of(20, 28, 30, 38)),
                entry(30, Set.of(29, 31, 39)),
                entry(31, Set.of(30, 32, 40)),
                entry(32, Set.of(23, 31, 33, 41)),
                entry(33, Set.of(24, 32, 34, 42)),
                entry(34, Set.of(25, 33, 35, 43)),
                entry(35, Set.of(26, 34, 44)),
                entry(36, Set.of(27, 37, 45)),
                entry(37, Set.of(28, 36, 38, 46)),
                entry(38, Set.of(29, 37, 39, 47)),
                entry(39, Set.of(30, 38, 40, 48)),
                entry(40, Set.of(31, 39, 41, 49)),
                entry(41, Set.of(32, 40, 50)),
                entry(42, Set.of(33, 43, 51)),
                entry(43, Set.of(34, 42, 44, 52)),
                entry(44, Set.of(35, 43, 53)),
                entry(45, Set.of(36, 46, 54)),
                entry(46, Set.of(37, 45, 47, 55)),
                entry(47, Set.of(38, 46, 48, 56)),
                entry(48, Set.of(39, 47, 49, 57)),
                entry(49, Set.of(40, 48, 50, 58)),
                entry(50, Set.of(41, 49, 59)),
                entry(51, Set.of(42, 52, 60)),
                entry(52, Set.of(43, 51, 53, 61)),
                entry(53, Set.of(44, 52, 62)),
                entry(54, Set.of(45, 55, 63)),
                entry(55, Set.of(46, 54, 56, 64)),
                entry(56, Set.of(47, 55, 57, 65)),
                entry(57, Set.of(48, 56, 58, 66)),
                entry(58, Set.of(49, 57, 59, 67)),
                entry(59, Set.of(50, 58, 60, 68)),
                entry(60, Set.of(51, 59, 61, 69)),
                entry(61, Set.of(52, 60, 62, 70)),
                entry(62, Set.of(53, 61, 71)),
                entry(63, Set.of(54, 64, 72)),
                entry(64, Set.of(55, 63, 65, 73)),
                entry(65, Set.of(56, 64, 66, 74)),
                entry(66, Set.of(57, 65, 67, 75)),
                entry(67, Set.of(58, 66, 68, 76)),
                entry(68, Set.of(59, 67, 69, 77)),
                entry(69, Set.of(60, 68, 70, 78)),
                entry(70, Set.of(61, 69, 71, 79)),
                entry(71, Set.of(62, 70, 80)),
                entry(72, Set.of(63, 73)),
                entry(73, Set.of(64, 72, 74)),
                entry(74, Set.of(65, 73, 75)),
                entry(75, Set.of(66, 74, 76)),
                entry(76, Set.of(67, 75, 77)),
                entry(77, Set.of(68, 76, 78)),
                entry(78, Set.of(69, 77, 79)),
                entry(79, Set.of(70, 78, 80)),
                entry(80, Set.of(71, 79))
        );

        assertEquals(expectedGraph, gameState.generatePawnMoveGraph());
    }
}
