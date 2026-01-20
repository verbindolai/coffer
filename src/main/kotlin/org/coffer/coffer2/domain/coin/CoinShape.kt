package org.coffer.coffer2.domain.coin

enum class CoinShape {
    /**
     * Standard circular/round coins (circularity > 0.85)
     */
    CIRCULAR,

    /**
     * Triangular coins (3 sides)
     */
    TRIANGULAR,

    /**
     * Square or rectangular coins (4 sides)
     */
    SQUARE,

    /**
     * Pentagon coins (5 sides)
     */
    PENTAGON,

    /**
     * Hexagon coins (6 sides)
     */
    HEXAGON,

    /**
     * Heptagon coins (7 sides) - e.g., UK 50 pence, 20 pence
     */
    HEPTAGON,

    /**
     * Octagon coins (8 sides)
     */
    OCTAGON,

    /**
     * Nonagon coins (9 sides)
     */
    NONAGON,

    /**
     * Decagon coins (10 sides)
     */
    DECAGON,

    /**
     * Undecagon coins (11 sides)
     */
    UNDECAGON,

    /**
     * Dodecagon coins (12 sides)
     */
    DODECAGON,

    /**
     * Tridecagon coins (13 sides)
     */
    TRIDECAGON,

    /**
     * Tetradecagon coins (14 sides)
     */
    TETRADECAGON,

    /**
     * Pentadecagon coins (15 sides)
     */
    PENTADECAGON,

    /**
     * Hexadecagon coins (16 sides)
     */
    HEXADECAGON,

    /**
     * Heptadecagon coins (17 sides)
     */
    HEPTADECAGON,

    /**
     * Octadecagon coins (18 sides)
     */
    OCTADECAGON,

    /**
     * Enneadecagon coins (19 sides)
     */
    ENNEADECAGON,

    /**
     * Icosagon coins (20 sides)
     */
    ICOSAGON,

    /**
     * Polygonal coins with irregular or high number of sides (>20)
     */
    POLYGONAL,

    /**
     * Coins with scalloped or wavy edges
     */
    SCALLOPED,

    /**
     * Irregular or unclassified shapes
     */
    IRREGULAR,

    /**
     * Shape has not been detected yet
     */
    UNKNOWN;

    companion object {
        fun fromNumistaString(value: String?): CoinShape {
            if (value.isNullOrBlank()) return CIRCULAR

            val normalized = value.lowercase().trim()

            return when {
                normalized.contains("round") || normalized.contains("circular") -> CIRCULAR
                normalized.contains("triangular") || normalized.contains("triangle") || normalized.contains("3-sided") -> TRIANGULAR
                normalized.contains("square") || normalized.contains("rectangular") || normalized.contains("4-sided") -> SQUARE
                normalized.contains("pentagonal") || normalized.contains("pentagon") || normalized.contains("5-sided") -> PENTAGON
                normalized.contains("hexagonal") || normalized.contains("hexagon") || normalized.contains("6-sided") -> HEXAGON
                normalized.contains("heptagonal") || normalized.contains("heptagon") || normalized.contains("7-sided") -> HEPTAGON
                normalized.contains("octagonal") || normalized.contains("octagon") || normalized.contains("8-sided") -> OCTAGON
                normalized.contains("nonagonal") || normalized.contains("nonagon") || normalized.contains("9-sided") -> NONAGON
                normalized.contains("decagonal") || normalized.contains("decagon") || normalized.contains("10-sided") -> DECAGON
                normalized.contains("undecagonal") || normalized.contains("undecagon") || normalized.contains("11-sided") -> UNDECAGON
                normalized.contains("dodecagonal") || normalized.contains("dodecagon") || normalized.contains("12-sided") -> DODECAGON
                normalized.contains("tridecagonal") || normalized.contains("tridecagon") || normalized.contains("13-sided") -> TRIDECAGON
                normalized.contains("tetradecagonal") || normalized.contains("tetradecagon") || normalized.contains("14-sided") -> TETRADECAGON
                normalized.contains("pentadecagonal") || normalized.contains("pentadecagon") || normalized.contains("15-sided") -> PENTADECAGON
                normalized.contains("hexadecagonal") || normalized.contains("hexadecagon") || normalized.contains("16-sided") -> HEXADECAGON
                normalized.contains("heptadecagonal") || normalized.contains("heptadecagon") || normalized.contains("17-sided") -> HEPTADECAGON
                normalized.contains("octadecagonal") || normalized.contains("octadecagon") || normalized.contains("18-sided") -> OCTADECAGON
                normalized.contains("enneadecagonal") || normalized.contains("enneadecagon") || normalized.contains("19-sided") -> ENNEADECAGON
                normalized.contains("icosagonal") || normalized.contains("icosagon") || normalized.contains("20-sided") -> ICOSAGON
                normalized.contains("scallop") -> SCALLOPED
                normalized.contains("irregular") -> IRREGULAR
                normalized.contains("polygon") -> POLYGONAL
                else -> CIRCULAR // Default to circular for unknown shapes
            }
        }
    }
}
