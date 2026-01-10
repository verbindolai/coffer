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
    UNKNOWN,
}
