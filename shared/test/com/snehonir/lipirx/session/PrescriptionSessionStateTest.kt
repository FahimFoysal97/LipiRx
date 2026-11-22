package com.snehonir.lipirx.session

import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
