package com.vapsa.fulbazo.ui.friends.group

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeFriendRepository
import com.vapsa.fulbazo.data.repository.FakeGroupRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GroupEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val groupRepository = FakeGroupRepository(latencyMillis = 0)

    private suspend fun viewModel(groupId: String? = null): GroupEditorViewModel {
        authRepository.login("demo@fulbazo.com", "demo123")
        return GroupEditorViewModel(
            groupId,
            authRepository,
            FakePlayerRepository(authRepository),
            FakeFriendRepository(latencyMillis = 0),
            groupRepository,
        )
    }

    @Test
    fun newGroup_requiresNameAndMembers() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.save()

        assertEquals(GroupNameError.REQUIRED, viewModel.uiState.value.nameError)
        assertTrue(viewModel.uiState.value.membersMissing)
    }

    @Test
    fun newGroup_isCreatedWithSelectedFriends() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onNameChange("  Los del sábado ")
        viewModel.onToggleMember("carlos")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDone)
        val created = groupRepository.observeGroups("demo").first().first { it.name == "Los del sábado" }
        assertEquals(listOf("carlos"), created.memberIds)
    }

    @Test
    fun existingGroup_isLoadedEditedAndDeleted() = runTest {
        val viewModel = viewModel("group-pichanga")
        advanceUntilIdle()
        assertEquals("Pichanga de los jueves", viewModel.uiState.value.name)
        assertEquals(setOf("carlos", "maria"), viewModel.uiState.value.memberIds)

        viewModel.onToggleMember("maria")
        viewModel.save()
        advanceUntilIdle()
        assertEquals(listOf("carlos"), groupRepository.observeGroups("demo").first().single().memberIds)

        viewModel.delete()
        advanceUntilIdle()
        assertTrue(groupRepository.observeGroups("demo").first().isEmpty())
    }
}
