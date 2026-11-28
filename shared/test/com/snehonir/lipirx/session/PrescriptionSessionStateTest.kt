package com.snehonir.lipirx.session

import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.agent.PipelineEvent
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrescriptionSessionStateTest {

    @Test
    fun testActiveScanResultsIsolationFromHistoryRecord() {
        val sessionState = PrescriptionSessionState()

        val activeMedicines = listOf(
            MedicineItem(medicineName = "Napa Extra", strengthValue = "500mg", schedule = "1+0+1")
        )
        sessionState.setResults(activeMedicines)

        assertEquals(activeMedicines, sessionState.session.value.activeScanMedicines.toList())
        assertEquals(activeMedicines, sessionState.session.value.medicines.toList())
        assertNull(sessionState.session.value.selectedHistoryItem)

        // Open history record
        val historyRecord = PrescriptionHistoryEntity(id = 100L, scannedAt = 1000L, medicineSummary = "History Napa (Paracetamol)")
        val historyMedicines = listOf(
            MedicineItem(medicineName = "History Napa", genericName = "Paracetamol")
        )
        sessionState.openHistoryRecord(historyRecord, historyMedicines)

        assertEquals(historyRecord, sessionState.session.value.selectedHistoryItem)
        assertEquals(historyMedicines, sessionState.session.value.medicines.toList())
        assertEquals(activeMedicines, sessionState.session.value.activeScanMedicines.toList(), "Active scan medicines must remain untouched when viewing history")

        // Clear selected history record
        sessionState.clearHistoryRecord()
        assertNull(sessionState.session.value.selectedHistoryItem)
        assertEquals(activeMedicines, sessionState.session.value.medicines.toList(), "Current medicines must revert to active scan items after closing history")
    }

    @Test
    fun testBackgroundStreamingAndCompletionIsolationWhenHistoryIsOpened() {
        val sessionState = PrescriptionSessionState()

        val initialScanMedicines = listOf(
            MedicineItem(medicineName = "Streaming Item 1", strengthValue = "10mg")
        )
        sessionState.setLoadingState(isLoading = true)
        sessionState.applyProgress(PipelineEvent.ItemsUpdated(initialScanMedicines))

        // Open history while background analysis is running
        val historyRecord = PrescriptionHistoryEntity(id = 200L, scannedAt = 2000L, medicineSummary = "Old Record")
        val historyMedicines = listOf(
            MedicineItem(medicineName = "History Record Item")
        )
        sessionState.openHistoryRecord(historyRecord, historyMedicines)

        assertEquals(historyMedicines, sessionState.session.value.medicines.toList())
        assertEquals(initialScanMedicines, sessionState.session.value.activeScanMedicines.toList())

        // Incoming background streaming update
        val updatedBackgroundScanMedicines = listOf(
            MedicineItem(medicineName = "Streaming Item 1", strengthValue = "10mg"),
            MedicineItem(medicineName = "Streaming Item 2", strengthValue = "20mg")
        )
        sessionState.applyProgress(PipelineEvent.ItemsUpdated(updatedBackgroundScanMedicines))

        // History view medicines must stay isolated
        assertEquals(historyMedicines, sessionState.session.value.medicines.toList(), "History items must not be overwritten by background streaming updates")
        assertEquals(updatedBackgroundScanMedicines, sessionState.session.value.activeScanMedicines.toList(), "Background active scan items must update")

        // Background analysis completes
        val finalScanMedicines = listOf(
            MedicineItem(medicineName = "Streaming Item 1", strengthValue = "10mg"),
            MedicineItem(medicineName = "Streaming Item 2", strengthValue = "20mg"),
            MedicineItem(medicineName = "Streaming Item 3", strengthValue = "30mg")
        )
        sessionState.setResults(finalScanMedicines)

        assertEquals(historyMedicines, sessionState.session.value.medicines.toList(), "History items must not be overwritten by background setResults")
        assertEquals(finalScanMedicines, sessionState.session.value.activeScanMedicines.toList(), "Background active scan items must hold final results")

        // User closes history
        sessionState.clearHistoryRecord()
        assertEquals(finalScanMedicines, sessionState.session.value.medicines.toList(), "Closing history must restore the completed active scan items")
    }

    @Test
    fun testStartPipelineAnalysisResetsStaleMedicines() {
        val sessionState = PrescriptionSessionState()
        val oldMedicines = listOf(
            MedicineItem(medicineName = "Old Napa", strengthValue = "500mg")
        )
        sessionState.setResults(oldMedicines)
        assertEquals(oldMedicines, sessionState.session.value.medicines.toList())

        sessionState.startPipelineAnalysis { }

        assertTrue(sessionState.session.value.isLoading)
        assertTrue(sessionState.session.value.medicines.isEmpty(), "Medicines must be cleared when starting new pipeline analysis")
        assertTrue(sessionState.session.value.activeScanMedicines.isEmpty(), "Active scan medicines must be cleared when starting new pipeline analysis")
    }

    @Test
    fun testUpdateMedicineListInActiveScan() {
        val sessionState = PrescriptionSessionState()
        val initialMedicines = listOf(
            MedicineItem(medicineName = "Seclo 20", strengthValue = "20mg", schedule = "1+0+1")
        )
        sessionState.setResults(initialMedicines)

        val updatedMedicines = listOf(
            initialMedicines[0].copy(needsReview = true, instructions = "Take before meal")
        )
        sessionState.updateMedicineList(updatedMedicines)

        assertEquals(updatedMedicines, sessionState.session.value.activeScanMedicines.toList())
    }

    @Test
    fun testResetSession() {
        val sessionState = PrescriptionSessionState()
        val dummyBytes = byteArrayOf(1, 2, 3, 4, 5)
        sessionState.setImageBytes(dummyBytes)

        assertEquals(dummyBytes, sessionState.session.value.imageByteArray)
        sessionState.resetSession()
        assertNull(sessionState.session.value.imageByteArray)
    }
}
