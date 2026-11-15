package com.snehonir.lipirx.domain.eval

import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem

object RxGoldenDataset {

    val testSuites: List<RxEvalTestCase> = listOf(

        // Test Case 1: Standard Clean Prescription (Baseline)
        RxEvalTestCase(
            id = "TC_001_Standard_Clean",
            description = "Standard clear prescription with two common brands and explicit dosages.",
            input = TranscriptionInput(
                imageByteArray = ByteArray(0),
                selectedModel = AiProvider.Gemini.defaultModel,
                provider = AiProvider.Gemini
            ),
            initialItems = listOf(
                MedicineItem(medicineName = "Tab Napa Extra 500mg", schedule = "1+0+1", units = 10, unitType = "tablets"),
                MedicineItem(medicineName = "Cap Seclo 20mg", schedule = "1+0+1", units = 14, unitType = "capsules")
            ),
            expectedItemsCount = 2,
            expectedMedicines = listOf(
                ExpectedMedicine(brandName = "Napa Extra", expectedStrength = "500mg", expectedGeneric = "Paracetamol"),
                ExpectedMedicine(brandName = "Seclo", expectedStrength = "20mg", expectedGeneric = "Omeprazole")
            ),
            maxAllowedTurns = 5,
            allowPharmacistSuspension = true
        ),

        // Test Case 2: Messy Handwriting & Shorthand Abbreviations
        RxEvalTestCase(
            id = "TC_002_Messy_Abbreviations",
            description = "Cursive handwriting with Latin medical shorthand (e.g. '1-0-1' and 'bid'). Tests Vision and Entity Parser resilience.",
            input = TranscriptionInput(
                imageByteArray = ByteArray(0),
                selectedModel = AiProvider.Gemini.defaultModel,
                provider = AiProvider.Gemini
            ),
            initialItems = listOf(
                MedicineItem(medicineName = "Cap Monas 10mg", schedule = "1-0-1 (b.i.d.)", units = 14, unitType = "capsules")
            ),
            expectedItemsCount = 1,
            expectedMedicines = listOf(
                ExpectedMedicine(brandName = "Monas", expectedStrength = "10mg", expectedGeneric = "Montelukast")
            ),
            maxAllowedTurns = 5,
            allowPharmacistSuspension = true
        ),

        // Test Case 3: High-Risk Interaction / Safety Audit Trigger
        RxEvalTestCase(
            id = "TC_003_Safety_Conflict_Check",
            description = "Prescription containing NSAID and PPI requiring clinical safety auditor verification.",
            input = TranscriptionInput(
                imageByteArray = ByteArray(0),
                selectedModel = AiProvider.Gemini.defaultModel,
                provider = AiProvider.Gemini
            ),
            initialItems = listOf(
                MedicineItem(medicineName = "Tab Ketorol 10mg", schedule = "1+0+1", units = 10, unitType = "tablets"),
                MedicineItem(medicineName = "Cap Maxpro 20mg", schedule = "1+0+0", units = 14, unitType = "capsules")
            ),
            expectedItemsCount = 2,
            expectedMedicines = listOf(
                ExpectedMedicine(brandName = "Ketorol", expectedStrength = "10mg", expectedGeneric = "Ketorolac"),
                ExpectedMedicine(brandName = "Maxpro", expectedStrength = "20mg", expectedGeneric = "Esomeprazole")
            ),
            maxAllowedTurns = 6,
            allowPharmacistSuspension = true
        ),

        // Test Case 4: Ambiguous Brand Name Requiring Generic Substitution Search
        RxEvalTestCase(
            id = "TC_004_Obscure_Brand_Lookup",
            description = "Uncommon regional brand name requiring the Generic Substitute agent to query local BD generic database.",
            input = TranscriptionInput(
                imageByteArray = ByteArray(0),
                selectedModel = AiProvider.Gemini.defaultModel,
                provider = AiProvider.Gemini
            ),
            initialItems = listOf(
                MedicineItem(medicineName = "Tab Zimax 500mg", schedule = "1+0+0", units = 3, unitType = "tablets")
            ),
            expectedItemsCount = 1,
            expectedMedicines = listOf(
                ExpectedMedicine(brandName = "Zimax", expectedStrength = "500mg", expectedGeneric = "Azithromycin")
            ),
            maxAllowedTurns = 5,
            allowPharmacistSuspension = true
        )
    )
}
