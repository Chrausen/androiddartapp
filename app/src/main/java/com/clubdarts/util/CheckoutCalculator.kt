package com.clubdarts.util

import com.clubdarts.data.model.CheckoutRule

object CheckoutCalculator {

    private val doubleOutTable: Map<Int, String> = mapOf(
        2 to "D1",
        4 to "D2",
        6 to "D3",
        8 to "D4",
        10 to "D5",
        12 to "D6",
        14 to "D7",
        16 to "D8",
        18 to "D9",
        20 to "D10",
        22 to "D11",
        24 to "D12",
        26 to "D13",
        28 to "D14",
        30 to "D15",
        32 to "D16",
        34 to "D17",
        36 to "D18",
        38 to "D19",
        40 to "D20",
        50 to "Bull",
        // 2-dart finishes
        41 to "1 · D20",
        43 to "3 · D20",
        45 to "5 · D20",
        47 to "7 · D20",
        49 to "9 · D20",
        51 to "11 · D20",
        53 to "13 · D20",
        55 to "15 · D20",
        57 to "17 · D20",
        59 to "19 · D20",
        60 to "20 · D20",
        61 to "T15 · D8",
        62 to "T10 · D16",
        63 to "T13 · D12",
        64 to "T16 · D8",
        65 to "T15 · D10",
        66 to "T10 · D18",
        67 to "T17 · D8",
        68 to "T16 · D10",
        69 to "T11 · D18",
        70 to "T10 · D20",
        71 to "T11 · D19",
        72 to "T12 · D18",
        73 to "T13 · D17",
        74 to "T14 · D16",
        75 to "T15 · D15",
        76 to "T16 · D14",
        77 to "T15 · D16",
        78 to "T14 · D18",
        79 to "T13 · D20",
        80 to "T16 · D16",
        81 to "T19 · D12",
        82 to "T14 · D20",
        83 to "T17 · D16",
        84 to "T20 · D12",
        85 to "T15 · D20",
        86 to "T18 · D16",
        87 to "T17 · D18",
        88 to "T16 · D20",
        89 to "T19 · D16",
        90 to "T18 · D18",
        91 to "T17 · D20",
        92 to "T20 · D16",
        93 to "T19 · D18",
        94 to "T18 · D20",
        95 to "T19 · D19",
        96 to "T20 · D18",
        97 to "T19 · D20",
        98 to "T20 · D19",
        99 to "T19 · 2 · D20",
        100 to "T20 · D20",
        // 3-dart finishes
        101 to "T17 · T10 · D20",
        102 to "T20 · 10 · D16",
        103 to "T19 · T10 · D18",
        104 to "T18 · T10 · D20",
        105 to "T20 · T5 · D20",
        106 to "T20 · T10 · D18",
        107 to "T19 · T10 · D20",
        108 to "T20 · T16 · D18",
        109 to "T20 · T9 · D20",
        110 to "T20 · T10 · D20",
        111 to "T20 · T11 · D19",
        112 to "T20 · T12 · D16",
        113 to "T20 · T13 · D14",
        114 to "T20 · T14 · D12",
        115 to "T20 · T15 · D10",
        116 to "T20 · T16 · D8",
        117 to "T20 · T17 · D6",
        118 to "T20 · T18 · D4",
        119 to "T20 · T19 · D2",
        120 to "T20 · 20 · D20",
        121 to "T20 · T11 · D20",
        122 to "T18 · T18 · D14",
        123 to "T19 · T16 · D15",
        124 to "T20 · T16 · D12",
        125 to "T20 · T15 · D20",
        126 to "T19 · T19 · D13",
        127 to "T20 · T17 · D14",
        128 to "T18 · T18 · D20",
        129 to "T19 · T20 · D12",
        130 to "T20 · T18 · D12",
        131 to "T20 · T13 · D20",
        132 to "T20 · T16 · D20",
        133 to "T20 · T19 · D14",
        134 to "T20 · T14 · D20",
        135 to "T20 · T15 · D20",
        136 to "T20 · T20 · D8",
        137 to "T20 · T19 · D20",
        138 to "T20 · T18 · D20",
        139 to "T20 · T13 · D20",
        140 to "T20 · T20 · D10",
        141 to "T20 · T19 · D12",
        142 to "T20 · T14 · D20",
        143 to "T20 · T17 · D16",
        144 to "T20 · T20 · D12",
        145 to "T20 · T15 · D20",
        146 to "T20 · T18 · D16",
        147 to "T20 · T17 · D18",
        148 to "T20 · T16 · D20",
        149 to "T20 · T19 · D16",
        150 to "T20 · T18 · D18",
        151 to "T20 · T17 · D20",
        152 to "T20 · T20 · D16",
        153 to "T20 · T19 · D18",
        154 to "T20 · T18 · D20",
        155 to "T20 · T19 · D19",
        156 to "T20 · T20 · D18",
        157 to "T20 · T19 · D20",
        158 to "T20 · T20 · D19",
        160 to "T20 · T20 · D20",
        161 to "T20 · T17 · Bull",
        164 to "T20 · T18 · Bull",
        167 to "T20 · T19 · Bull",
        170 to "T20 · T20 · Bull"
    )

    /**
     * Returns a checkout suggestion for [score] under [rule], or null if none exists.
     * [maxDarts] limits the suggestion to paths that fit within the remaining darts
     * of the current visit (1–3). Suggestions with more steps than [maxDarts] are
     * suppressed so the hint always reflects what is still achievable this turn.
     */
    fun suggest(score: Int, rule: CheckoutRule, maxDarts: Int = 3): String? {
        if (score < 1 || score > 170) return null
        val raw = when (rule) {
            CheckoutRule.DOUBLE -> doubleOutTable[score]
                ?: if (score <= 40 && score % 2 == 0) "D${score / 2}"
                else if (score in 41..60) "${score - 40} · D20"
                else if (score % 2 == 1 && score in 3..21) "${score - 2} · D1"
                else if (score % 2 == 1 && score in 23..39) "${score - 20} · D10"
                else null
            CheckoutRule.STRAIGHT -> when {
                score == 25 -> "Bull (single)"
                score == 50 -> "Bull"
                score <= 20 -> "$score"
                else -> doubleOutTable[score]
            }
            CheckoutRule.TRIPLE -> when {
                score % 3 == 0 && score <= 60 -> "T${score / 3}"
                else -> doubleOutTable[score]
            }
        }
        // Drop the suggestion if it requires more darts than are left this visit.
        if (raw != null && raw.split(" · ").size > maxDarts) return null
        return raw
    }

    fun isCheckoutPossible(score: Int, rule: CheckoutRule): Boolean {
        if (score < 1) return false
        return when (rule) {
            CheckoutRule.STRAIGHT -> score <= 60  // single 20 or bull in 1 dart; up to 180 in 3 darts
            CheckoutRule.DOUBLE -> score <= 170 && score != 169 && score != 168 && score != 166 &&
                    score != 165 && score != 163 && score != 162 && score != 159
            CheckoutRule.TRIPLE -> score <= 180
        }
    }

    fun isValidCheckout(
        lastDartScore: Int,
        lastDartMult: Int,
        remainingAfter: Int,
        rule: CheckoutRule
    ): Boolean {
        if (remainingAfter != 0) return false
        return when (rule) {
            CheckoutRule.STRAIGHT -> lastDartScore > 0 || (lastDartScore == 25 && lastDartMult == 2)
            CheckoutRule.DOUBLE -> lastDartMult == 2 && lastDartScore > 0
            CheckoutRule.TRIPLE -> lastDartMult == 3 && lastDartScore > 0
        }
    }
}
